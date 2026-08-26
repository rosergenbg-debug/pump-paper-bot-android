package com.example.pumppaperbot

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Compact text evidence for V6.0. Raw samples stay on-device for seven days; exports are split
 * well below the known ~2 MB chat upload problem. No API key or secret is ever included.
 */
object V6ScalpReportStore {
    private const val DIRECTORY = "v6_scalp_execution_reports"
    private const val RETENTION_MILLIS = 7L * 24L * 60L * 60L * 1_000L
    private const val MAX_EXPORT_PART_BYTES = 900_000
    private const val MAX_TRADE_ROWS = 500
    private const val DEFAULT_WINDOW_HOURS = 24
    private val utc = TimeZone.getTimeZone("UTC")
    private val lock = Any()

    private const val SAMPLE_COLUMNS =
        "time_ms\ttrigger\tagreement\tscore\tfee_bp_side\tspread_bp\tprobe_eur\tbuy_slip_bp\t" +
            "sell_slip_bp\tcost_floor_bp\timb3\timb5\tmicro_bias_bp\tbid5_change_pct\t" +
            "ask5_change_pct\tbuy15_pct\tbuy60_pct\taccel\tprice60_pct\tinstant\tflow5\tflow15\tflow30\tbid\task\tfee_tier\treason"

    fun append(context: Context, snapshot: ScalpExecutionSnapshotV600) {
        if (snapshot.at <= 0L) return
        synchronized(lock) {
            // First close/update older causal observations with this strictly later market frame,
            // then register the current frame as a new pending origin.
            V6ScalpOutcomeStore.observe(
                context,
                snapshot,
                BitpandaFusionStore.state(context),
                snapshot.at
            )
            val dir = File(context.filesDir, DIRECTORY).apply { mkdirs() }
            File(dir, "v6-${day(snapshot.at)}.tsv").appendText(line(snapshot) + "\n", Charsets.UTF_8)
            val cutoff = snapshot.at - RETENTION_MILLIS
            dir.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
        }
    }

    fun exportRecent(
        context: Context,
        windowHours: Int = DEFAULT_WINDOW_HOURS,
        now: Long = System.currentTimeMillis()
    ): List<File> = synchronized(lock) {
        val hours = windowHours.coerceIn(1, 168)
        val cutoff = now - hours * 60L * 60L * 1_000L
        val samples = readLines(context, cutoff)
        val outcomes = V6ScalpOutcomeStore.readLines(context, cutoff)
        val dataRows = ArrayList<String>(samples.size + outcomes.size).apply {
            samples.forEach { add("SAMPLE\t$it") }
            outcomes.forEach { add("OUTCOME\t$it") }
        }
        val summary = summaryText(context, cutoff, now, hours, samples.size, outcomes.size)
        val parts = split(summary, dataRows, MAX_EXPORT_PART_BYTES)
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        dir.listFiles()?.filter { it.name.contains("V6-Scalp-${hours}h-") }?.forEach { it.delete() }
        parts.mapIndexed { index, text ->
            File(
                dir,
                "PumpSignal-V${BuildConfig.VERSION_NAME}-V6-Scalp-${hours}h-${stamp(now)}-part" +
                    "%02d-of-%02d.txt".format(Locale.US, index + 1, parts.size)
            ).apply { writeText(text, Charsets.UTF_8) }
        }
    }

    fun shareRecent24h(context: Context) {
        val files = exportRecent(context, DEFAULT_WINDOW_HOURS)
        val uris = ArrayList(files.map {
            FileProvider.getUriForFile(context, "${context.packageName}.files", it)
        })
        val send = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/plain"
            if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris.single())
            else putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "PumpSignal V${BuildConfig.VERSION_NAME} — V6 scalp report 24h (${uris.size} файл.)"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Отправить V6 отчёт за 24 часа"))
    }

    private fun summaryText(
        context: Context,
        cutoff: Long,
        now: Long,
        hours: Int,
        sampleCount: Int,
        outcomeCount: Int
    ): String {
        runCatching { ResearchPerformanceLedger.capture(context) }
        val current = ScalpExecutionIntelligenceStoreV600.current(context)
        val fusion = BitpandaFusionStore.state(context)
        val market = PumpBotEngine.snapshot(context)
        val price = PaperExecutionPolicy.displayPrice(market, now)
        val pm1 = PumpMachine2Store.state(context)
        val pm2 = PumpMachineStore.state(context)
        val pm3 = PumpMachineRetestStore.state(context)
        val pm4 = PumpMachineSafeStore.state(context)
        val fusionSim = FusionSimStore.state(context)
        val ledger = runCatching { ResearchPerformanceLedger.exportJson(context, 5_000) }.getOrNull()
        val recentTrades = ArrayList<String>()
        val events = ledger?.optJSONArray("eventsNewestFirst") ?: JSONArray()
        for (index in 0 until events.length()) {
            if (recentTrades.size >= MAX_TRADE_ROWS) break
            val event = events.optJSONObject(index) ?: continue
            if (event.optLong("time") < cutoff || event.optString("kind") != "TRADE") continue
            recentTrades += buildString {
                append(event.optLong("time")); append('\t')
                append(clean(event.optString("account"))); append('\t')
                append(clean(event.optString("action"))); append('\t')
                append(fmt(event.optDouble("price"))); append('\t')
                append(fmtSigned(event.optDouble("pnlEur"))); append('\t')
                append(clean(event.optString("reason")).take(260))
            }
        }
        val fusionMark = fusion.bid.takeIf { fusion.fresh(now) } ?: price
        return buildString {
            appendLine("PUMP / PumpBot V6.0 SCALP EXECUTION REPORT")
            appendLine("schema=pump-v6-scalp-report-v1")
            appendLine("appVersion=${BuildConfig.VERSION_NAME}")
            appendLine("generatedAt=$now")
            appendLine("windowHours=$hours")
            appendLine("cutoffAt=$cutoff")
            appendLine("sampleCount=$sampleCount")
            appendLine("forwardOutcomeRows=$outcomeCount")
            appendLine("format=UTF-8 TAB-SEPARATED TEXT")
            appendLine("maxPartBytes=$MAX_EXPORT_PART_BYTES")
            appendLine("safety=SHADOW_ONLY; REAL_ORDERS=false; CONTAINS_API_KEYS=false")
            appendLine("IMPORTANT=V6 does not allow or veto any V5 trade in this release")
            appendLine("COST_FLOOR=observed round-trip fee + spread + depth slippage + safety buffer; NOT a profit forecast")
            appendLine("EXECUTION_PROBE_EUR=${ScalpExecutionPolicyV600.EXECUTION_PROBE_EUR}; diagnostic depth probe, not position sizing")
            appendLine("FORWARD_OUTCOMES=30/60/120/300s use later Bitpanda bid; late observations are marked MISSED, never backfilled with a wrong timestamp")
            appendLine()
            appendLine("[CURRENT_EXECUTION_SHADOW]")
            appendLine(current.compactText().replace('\n', ' '))
            appendLine(
                "v5ControlFee=${fmt(fusion.feeRate * 100.0)}% (${clean(fusion.feeTier)}); " +
                    "observedAccountFee=${fusion.observedAccountFeeRate?.let { fmt(it * 100.0) + "%" } ?: "NA"}; " +
                    "observedTier=${clean(fusion.observedAccountFeeTier ?: "NA")}; " +
                    "tradedVolume30d=${fusion.tradedVolume30dEur?.let(::fmt) ?: "NA"}"
            )
            appendLine("fusionFresh=${fusion.fresh(now)} bid=${fmt(fusion.bid)} ask=${fmt(fusion.ask)} depthBid=${fmt(fusion.bidDepthEur)} depthAsk=${fmt(fusion.askDepthEur)}")
            appendLine()
            appendLine("[CURRENT_ACCOUNTS]")
            appendLine("PM1_2pct\tvalue=${fmt(PumpMachine2Policy.netLiquidationValue(pm1, fusionMark, fusion.feeRate))}\tinPosition=${pm1.inPosition}\ttrades=${pm1.trades.size}\tstatus=${clean(PumpMachine2Store.lastStatus(context)).take(300)}")
            appendLine("PM2_3pct\tvalue=${fmt(PumpMachinePolicy.netLiquidationValue(pm2, fusionMark, fusion.feeRate))}\tinPosition=${pm2.inPosition}\ttrades=${pm2.trades.size}\tstatus=${clean(PumpMachineStore.lastStatus(context)).take(300)}")
            appendLine("PM3_RETEST\tvalue=${fmt(PumpMachineRetestStore.netValue(context, now))}\tinPosition=${pm3.inPosition}\ttrades=${pm3.trades.size}\tstatus=${clean(PumpMachineRetestStore.lastStatus(context)).take(300)}")
            appendLine("PM4_SAFE\tvalue=${fmt(PumpMachineSafeStore.netValue(context, now))}\tinPosition=${pm4.inPosition}\ttrades=${pm4.trades.size}\tstatus=${clean(PumpMachineSafeStore.lastStatus(context)).take(300)}")
            appendLine("FUSION\tvalue=${fmt(fusionSim.value(fusionMark))}\tinPosition=${fusionSim.inPosition}\ttrades=${fusionSim.trades.size}")
            val coach = DeepSeekEntryCoachStore.state(context)
            val tuning = DeepSeekEntryCoachStore.tuning(context)
            appendLine("DEEPSEEK_COACH\tstatus=${clean(coach.status)}\tverdict=${clean(coach.verdict)}\tprofile=${clean(coach.candidateProfile)}\ttuningRevision=${tuning.revision}")
            appendLine()
            appendLine("[TRADES_LAST_${hours}H]")
            appendLine("time_ms\taccount\taction\tprice\tpnl_eur\treason")
            if (recentTrades.isEmpty()) appendLine("NONE") else recentTrades.forEach(::appendLine)
            if (recentTrades.size >= MAX_TRADE_ROWS) appendLine("TRADES_TRUNCATED_AT=$MAX_TRADE_ROWS")
            appendLine()
            appendLine("[DATA_ROWS]")
            appendLine("SAMPLE_COLUMNS=$SAMPLE_COLUMNS")
            appendLine("OUTCOME_COLUMNS=${V6ScalpOutcomeStore.COLUMNS}")
            appendLine("Each row begins with SAMPLE or OUTCOME before the corresponding columns.")
        }
    }

    private fun readLines(context: Context, cutoff: Long): List<String> {
        val result = ArrayList<String>()
        File(context.filesDir, DIRECTORY).listFiles()
            ?.filter { it.name.endsWith(".tsv") && it.lastModified() >= cutoff }
            ?.sortedBy { it.name }
            ?.forEach { file ->
                file.useLines(Charsets.UTF_8) { lines -> lines.forEach { raw ->
                    val at = raw.substringBefore('\t').toLongOrNull() ?: return@forEach
                    if (at >= cutoff) result += raw
                } }
            }
        return result.sortedBy { it.substringBefore('\t').toLongOrNull() ?: 0L }
    }

    internal fun split(summary: String, lines: List<String>, maxBytes: Int): List<String> {
        require(maxBytes > summary.toByteArray(Charsets.UTF_8).size + 1_024) {
            "Лимит отчёта меньше заголовка"
        }
        val groups = ArrayList<MutableList<String>>()
        var current = ArrayList<String>()
        fun payload(rows: List<String>, part: Int = 99, count: Int = 99): String = buildString {
            append(summary)
            appendLine("parts=$part/$count")
            appendLine("rowsInPart=${rows.size}; totalRows=${lines.size}; uploadAllParts=${count > 1}")
            rows.forEach { appendLine(it) }
        }
        lines.forEach { row ->
            val candidate = ArrayList(current).apply { add(row) }
            if (payload(candidate).toByteArray(Charsets.UTF_8).size <= maxBytes) {
                current = candidate
            } else {
                require(current.isNotEmpty()) { "Одна строка V6 отчёта превысила лимит" }
                groups += current
                current = arrayListOf(row)
            }
        }
        if (current.isNotEmpty() || groups.isEmpty()) groups += current
        return groups.mapIndexed { index, rows ->
            payload(rows, index + 1, groups.size).also {
                require(it.toByteArray(Charsets.UTF_8).size <= maxBytes) { "Часть V6 отчёта превысила лимит" }
            }
        }
    }

    private fun line(value: ScalpExecutionSnapshotV600): String = listOf(
        value.at.toString(),
        clean(value.trigger),
        clean(value.agreement),
        value.executionScore.toString(),
        fmt(value.feeBpsPerSide),
        fmt(value.spreadBps),
        fmt(value.probeNotionalEur),
        nullable(value.buySlippageBps),
        nullable(value.sellSlippageBps),
        nullable(value.costFloorBps),
        nullable(value.imbalance3),
        nullable(value.imbalance5),
        nullable(value.micropriceBiasBps),
        nullable(value.bidDepthChangePercent),
        nullable(value.askDepthChangePercent),
        fmt(value.aggressiveBuy15s),
        fmt(value.aggressiveBuy60s),
        fmt(value.tradeAcceleration),
        fmtSigned(value.priceChange60sPercent),
        value.instantScore?.toString() ?: "NA",
        value.flow5m?.toString() ?: "NA",
        value.flow15m?.toString() ?: "NA",
        value.flow30m?.toString() ?: "NA",
        fmt(value.bid),
        fmt(value.ask),
        clean(value.feeTier),
        clean(value.reason).take(300)
    ).joinToString("\t")

    private fun splitClean(value: String): String = value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')
    private fun clean(value: String): String = splitClean(value)
        .replace(Regex("(?i)(x-api-key|authorization|api[_ -]?key)\\s*[:=]?\\s*[^,;\\s]+"), "$1=[HIDDEN]")
        .trim()

    private fun nullable(value: Double?): String = value?.takeIf(Double::isFinite)?.let(::fmt) ?: "NA"
    private fun fmt(value: Double): String = if (value.isFinite()) String.format(Locale.US, "%.6f", value) else "NA"
    private fun fmtSigned(value: Double): String = if (value.isFinite()) String.format(Locale.US, "%+.6f", value) else "NA"
    private fun day(now: Long) = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utc }.format(Date(now))
    private fun stamp(now: Long) = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).apply { timeZone = utc }.format(Date(now))
}
