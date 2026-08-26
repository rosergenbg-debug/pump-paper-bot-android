package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min

/**
 * Causal forward evaluation for V6.0 shadow observations.
 *
 * A V6 signal is useless unless we later know what happened. Pending observations are evaluated
 * only with future Bitpanda bid snapshots. Horizons that were missed by too much are recorded as
 * MISSED instead of pretending a late price was the requested 30/60/120/300-second outcome.
 */
data class V6PendingOutcome(
    val originAt: Long,
    val trigger: String,
    val agreement: String,
    val score: Int,
    val entryAsk: Double,
    val feeRate: Double,
    val costFloorBps: Double?,
    val initialBid: Double,
    val bestBid: Double,
    val worstBid: Double,
    val completedMask: Int = 0
) {
    fun toJson() = JSONObject()
        .put("originAt", originAt)
        .put("trigger", trigger)
        .put("agreement", agreement)
        .put("score", score)
        .put("entryAsk", entryAsk)
        .put("feeRate", feeRate)
        .put("costFloorBps", costFloorBps ?: JSONObject.NULL)
        .put("initialBid", initialBid)
        .put("bestBid", bestBid)
        .put("worstBid", worstBid)
        .put("completedMask", completedMask)

    companion object {
        fun fromJson(j: JSONObject): V6PendingOutcome? {
            val origin = j.optLong("originAt")
            val ask = j.optDouble("entryAsk")
            if (origin <= 0L || ask <= 0.0) return null
            return V6PendingOutcome(
                originAt = origin,
                trigger = j.optString("trigger").take(120),
                agreement = j.optString("agreement").take(40),
                score = j.optInt("score").coerceIn(0, 100),
                entryAsk = ask,
                feeRate = j.optDouble("feeRate", FusionTradingCosts.FEE_RATE).coerceIn(0.0, 0.05),
                costFloorBps = if (!j.has("costFloorBps") || j.isNull("costFloorBps")) null
                    else j.optDouble("costFloorBps").takeIf(Double::isFinite),
                initialBid = j.optDouble("initialBid"),
                bestBid = j.optDouble("bestBid"),
                worstBid = j.optDouble("worstBid"),
                completedMask = j.optInt("completedMask")
            )
        }
    }
}

object V6ScalpOutcomeStore {
    private const val PREFS = "v6_scalp_forward_outcomes_v600"
    private const val KEY_PENDING = "pending"
    private const val DIRECTORY = "v6_scalp_forward_outcomes"
    private const val RETENTION_MILLIS = 7L * 24L * 60L * 60L * 1_000L
    private const val MAX_PENDING = 120
    private val utc = TimeZone.getTimeZone("UTC")
    private val lock = Any()

    private data class Horizon(val seconds: Int, val toleranceSeconds: Int, val bit: Int)
    private val horizons = listOf(
        Horizon(30, 25, 1),
        Horizon(60, 35, 2),
        Horizon(120, 50, 4),
        Horizon(300, 100, 8)
    )
    private val allMask = horizons.fold(0) { acc, h -> acc or h.bit }

    const val COLUMNS =
        "origin_ms\thorizon_s\tobserved_ms\tdelay_s\tstatus\ttrigger\tagreement\tscore\tentry_ask\t" +
            "future_bid\tgross_pct\tnet_after_fees_pct\tsampled_mfe_pct\tsampled_mae_pct\tfee_bp_side\tcost_floor_bp"

    @Synchronized
    fun observe(
        context: Context,
        snapshot: ScalpExecutionSnapshotV600,
        market: FusionMarketSnapshot,
        now: Long = System.currentTimeMillis()
    ) = synchronized(lock) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pending = load(prefs.getString(KEY_PENDING, "[]").orEmpty())
        val venueFresh = market.fresh(now) && market.bid > 0.0 && market.ask > 0.0
        val currentBid = market.bid.takeIf { venueFresh }
        val next = ArrayList<V6PendingOutcome>()

        pending.forEach { original ->
            var item = if (currentBid != null) original.copy(
                bestBid = if (original.bestBid > 0.0) max(original.bestBid, currentBid) else currentBid,
                worstBid = if (original.worstBid > 0.0) min(original.worstBid, currentBid) else currentBid
            ) else original
            var mask = item.completedMask
            val elapsedSeconds = ((now - item.originAt).coerceAtLeast(0L) / 1_000L).toInt()
            horizons.forEach { horizon ->
                if (mask and horizon.bit != 0 || elapsedSeconds < horizon.seconds) return@forEach
                val delay = elapsedSeconds - horizon.seconds
                when {
                    delay <= horizon.toleranceSeconds && currentBid != null -> {
                        appendOutcome(context, item, horizon, now, delay, "OK", currentBid)
                        mask = mask or horizon.bit
                    }
                    delay > horizon.toleranceSeconds -> {
                        appendOutcome(context, item, horizon, now, delay, "MISSED", null)
                        mask = mask or horizon.bit
                    }
                }
            }
            item = item.copy(completedMask = mask)
            if (mask != allMask && now - item.originAt <= 10L * 60L * 1_000L) next += item
        }

        if (venueFresh && snapshot.at > 0L && snapshot.ask > 0.0 &&
            next.none { it.originAt == snapshot.at }
        ) {
            next += V6PendingOutcome(
                originAt = snapshot.at,
                trigger = snapshot.trigger,
                agreement = snapshot.agreement,
                score = snapshot.executionScore,
                entryAsk = snapshot.ask,
                feeRate = (snapshot.feeBpsPerSide / 10_000.0).coerceIn(0.0, 0.05),
                costFloorBps = snapshot.costFloorBps,
                initialBid = snapshot.bid,
                bestBid = snapshot.bid,
                worstBid = snapshot.bid
            )
        }

        val retained = next.sortedBy { it.originAt }.takeLast(MAX_PENDING)
        prefs.edit().putString(KEY_PENDING, JSONArray(retained.map { it.toJson() }).toString()).apply()
        cleanup(context, now)
    }

    fun readLines(context: Context, cutoff: Long): List<String> = synchronized(lock) {
        val out = ArrayList<String>()
        File(context.filesDir, DIRECTORY).listFiles()
            ?.filter { it.name.endsWith(".tsv") && it.lastModified() >= cutoff }
            ?.sortedBy { it.name }
            ?.forEach { file ->
                file.useLines(Charsets.UTF_8) { lines -> lines.forEach { raw ->
                    val origin = raw.substringBefore('\t').toLongOrNull() ?: return@forEach
                    if (origin >= cutoff) out += raw
                } }
            }
        out.sortedBy { it.substringBefore('\t').toLongOrNull() ?: 0L }
    }

    private fun appendOutcome(
        context: Context,
        item: V6PendingOutcome,
        horizon: Horizon,
        observedAt: Long,
        delaySeconds: Int,
        status: String,
        futureBid: Double?
    ) {
        val gross = futureBid?.takeIf { it > 0.0 }?.let { (it / item.entryAsk - 1.0) * 100.0 }
        val net = futureBid?.takeIf { it > 0.0 }?.let {
            ((it / item.entryAsk) * (1.0 - item.feeRate) * (1.0 - item.feeRate) - 1.0) * 100.0
        }
        val mfe = item.bestBid.takeIf { it > 0.0 }?.let { (it / item.entryAsk - 1.0) * 100.0 }
        val mae = item.worstBid.takeIf { it > 0.0 }?.let { (it / item.entryAsk - 1.0) * 100.0 }
        val row = listOf(
            item.originAt.toString(),
            horizon.seconds.toString(),
            observedAt.toString(),
            delaySeconds.toString(),
            status,
            clean(item.trigger),
            clean(item.agreement),
            item.score.toString(),
            fmt(item.entryAsk),
            nullable(futureBid),
            nullable(gross),
            nullable(net),
            nullable(mfe),
            nullable(mae),
            fmt(item.feeRate * 10_000.0),
            nullable(item.costFloorBps)
        ).joinToString("\t")
        val dir = File(context.filesDir, DIRECTORY).apply { mkdirs() }
        File(dir, "outcome-${day(item.originAt)}.tsv").appendText(row + "\n", Charsets.UTF_8)
    }

    private fun load(raw: String): List<V6PendingOutcome> = runCatching {
        val a = JSONArray(raw)
        (0 until a.length()).mapNotNull { index ->
            a.optJSONObject(index)?.let(V6PendingOutcome::fromJson)
        }
    }.getOrDefault(emptyList())

    private fun cleanup(context: Context, now: Long) {
        val cutoff = now - RETENTION_MILLIS
        File(context.filesDir, DIRECTORY).listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
    }

    private fun clean(value: String) = value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').take(160)
    private fun nullable(value: Double?): String = value?.takeIf(Double::isFinite)?.let(::fmt) ?: "NA"
    private fun fmt(value: Double): String = if (value.isFinite()) String.format(Locale.US, "%.8f", value) else "NA"
    private fun day(now: Long) = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utc }.format(Date(now))
}
