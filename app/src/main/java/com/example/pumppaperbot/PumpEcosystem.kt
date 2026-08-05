package com.example.pumppaperbot

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class PumpEcosystemSnapshot(
    val updatedAt: Long = 0L,
    val migrations1h: Int = 0,
    val migrations6h: Int = 0,
    val migrations24h: Int = 0,
    val migrationAcceleration: Double? = null,
    val dexVolume24hUsd: Double? = null,
    val dexVolumeChange1dPercent: Double? = null,
    val revenue24hUsd: Double? = null,
    val revenueChange1dPercent: Double? = null,
    val buyback24hUsd: Double? = null,
    val buybackChange1dPercent: Double? = null,
    val burnedPump24h: Double? = null,
    val score: Int? = null,
    val dataQuality: Int = 0,
    val sourceStatus: String = "ДАННЫЕ ЕЩЁ НЕ ПОЛУЧЕНЫ",
    val error: String = ""
) {
    fun fresh(now: Long = System.currentTimeMillis()): Boolean =
        updatedAt > 0L && now >= updatedAt && now - updatedAt <= MAX_AGE_MILLIS

    fun toPromptJson(now: Long = System.currentTimeMillis()): JSONObject = JSONObject()
        .put("updated_at", updatedAt)
        .put("age_seconds", if (updatedAt > 0L && now >= updatedAt) (now - updatedAt) / 1000L else JSONObject.NULL)
        .put("fresh", fresh(now))
        .put("data_quality_0_100", dataQuality)
        .put("ecosystem_score_minus100_plus100", score ?: JSONObject.NULL)
        .put("migrations_1h", migrations1h)
        .put("migrations_6h", migrations6h)
        .put("migrations_24h", migrations24h)
        .put("boost_trigger_migrations_1h", migrations1h)
        .put("boost_trigger_migrations_24h", migrations24h)
        .put("boost_note", "миграция считается подтверждённым триггером BOOST; объём BOOST не выдумывается без отдельного подтверждённого события")
        .put("migration_acceleration", migrationAcceleration ?: JSONObject.NULL)
        .put("dex_volume_24h_usd", dexVolume24hUsd ?: JSONObject.NULL)
        .put("dex_volume_change_1d_pct", dexVolumeChange1dPercent ?: JSONObject.NULL)
        .put("protocol_revenue_24h_usd", revenue24hUsd ?: JSONObject.NULL)
        .put("protocol_revenue_change_1d_pct", revenueChange1dPercent ?: JSONObject.NULL)
        .put("pump_buyback_24h_usd", buyback24hUsd ?: JSONObject.NULL)
        .put("pump_buyback_change_1d_pct", buybackChange1dPercent ?: JSONObject.NULL)
        .put("pump_burned_24h", burnedPump24h ?: JSONObject.NULL)
        .put("source_status", sourceStatus)
        .put("role", "внутренний фундаментальный фон; самостоятельно не исполняет и не запрещает сделку")

    companion object {
        const val MAX_AGE_MILLIS = 30L * 60L * 1000L
    }
}

internal object PumpEcosystemAnalyzer {
    fun analyze(
        now: Long,
        migrationTimes: List<Long>,
        dexVolume24hUsd: Double?,
        dexChange1d: Double?,
        revenue24hUsd: Double?,
        revenueChange1d: Double?,
        buyback24hUsd: Double?,
        buybackChange1d: Double?,
        burnedPump24h: Double?,
        successfulSources: Int,
        sourceStatus: String,
        error: String = ""
    ): PumpEcosystemSnapshot {
        val valid = migrationTimes.filter { it in (now - DAY_MILLIS)..now }
        val one = valid.count { it >= now - HOUR_MILLIS }
        val six = valid.count { it >= now - 6L * HOUR_MILLIS }
        val acceleration = if (six > 0) one * 6.0 / six else null
        val components = buildList {
            acceleration?.let { add(((it - 1.0) * 45.0).coerceIn(-70.0, 70.0)) }
            dexChange1d?.takeIf(Double::isFinite)?.let { add((it * 2.5).coerceIn(-80.0, 80.0)) }
            revenueChange1d?.takeIf(Double::isFinite)?.let { add((it * 2.0).coerceIn(-80.0, 80.0)) }
            buybackChange1d?.takeIf(Double::isFinite)?.let { add((it * 1.5).coerceIn(-70.0, 70.0)) }
            if (buyback24hUsd != null && buyback24hUsd > 0.0) add(10.0)
            if (burnedPump24h != null && burnedPump24h > 0.0) add(10.0)
        }
        val score = components.takeIf { it.isNotEmpty() }
            ?.average()?.roundToInt()?.coerceIn(-100, 100)
        val quality = (successfulSources * 25).coerceIn(0, 100)
        return PumpEcosystemSnapshot(
            updatedAt = now,
            migrations1h = one,
            migrations6h = six,
            migrations24h = valid.size,
            migrationAcceleration = acceleration,
            dexVolume24hUsd = dexVolume24hUsd,
            dexVolumeChange1dPercent = dexChange1d,
            revenue24hUsd = revenue24hUsd,
            revenueChange1dPercent = revenueChange1d,
            buyback24hUsd = buyback24hUsd,
            buybackChange1dPercent = buybackChange1d,
            burnedPump24h = burnedPump24h,
            score = score,
            dataQuality = quality,
            sourceStatus = sourceStatus,
            error = error.take(240)
        )
    }

    private const val HOUR_MILLIS = 60L * 60L * 1000L
    private const val DAY_MILLIS = 24L * HOUR_MILLIS
}

object PumpEcosystemStore {
    private const val PREFS = "pump_ecosystem_v416"
    private const val KEY_STATE = "state"
    private const val KEY_MIGRATIONS = "migrations"
    private const val KEY_LAST_SYNC = "last_sync"
    const val SYNC_INTERVAL_MILLIS = 10L * 60L * 1000L

    fun state(context: Context): PumpEcosystemSnapshot = runCatching {
        val json = JSONObject(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_STATE, "{}").orEmpty())
        PumpEcosystemSnapshot(
            updatedAt = json.optLong("updatedAt"),
            migrations1h = json.optInt("migrations1h"),
            migrations6h = json.optInt("migrations6h"),
            migrations24h = json.optInt("migrations24h"),
            migrationAcceleration = json.nullableDouble("migrationAcceleration"),
            dexVolume24hUsd = json.nullableDouble("dexVolume24hUsd"),
            dexVolumeChange1dPercent = json.nullableDouble("dexVolumeChange1dPercent"),
            revenue24hUsd = json.nullableDouble("revenue24hUsd"),
            revenueChange1dPercent = json.nullableDouble("revenueChange1dPercent"),
            buyback24hUsd = json.nullableDouble("buyback24hUsd"),
            buybackChange1dPercent = json.nullableDouble("buybackChange1dPercent"),
            burnedPump24h = json.nullableDouble("burnedPump24h"),
            score = json.optInt("score").takeIf { json.has("score") && !json.isNull("score") },
            dataQuality = json.optInt("dataQuality"),
            sourceStatus = json.optString("sourceStatus", "ДАННЫЕ ЕЩЁ НЕ ПОЛУЧЕНЫ"),
            error = json.optString("error")
        )
    }.getOrDefault(PumpEcosystemSnapshot())

    fun shouldSync(context: Context, now: Long): Boolean =
        now - context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_LAST_SYNC, 0L) >=
            SYNC_INTERVAL_MILLIS

    fun markAttempt(context: Context, now: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_LAST_SYNC, now).apply()
    }

    fun save(context: Context, value: PumpEcosystemSnapshot) {
        val json = JSONObject()
            .put("updatedAt", value.updatedAt)
            .put("migrations1h", value.migrations1h)
            .put("migrations6h", value.migrations6h)
            .put("migrations24h", value.migrations24h)
            .putNullable("migrationAcceleration", value.migrationAcceleration)
            .putNullable("dexVolume24hUsd", value.dexVolume24hUsd)
            .putNullable("dexVolumeChange1dPercent", value.dexVolumeChange1dPercent)
            .putNullable("revenue24hUsd", value.revenue24hUsd)
            .putNullable("revenueChange1dPercent", value.revenueChange1dPercent)
            .putNullable("buyback24hUsd", value.buyback24hUsd)
            .putNullable("buybackChange1dPercent", value.buybackChange1dPercent)
            .putNullable("burnedPump24h", value.burnedPump24h)
            .putNullable("score", value.score)
            .put("dataQuality", value.dataQuality)
            .put("sourceStatus", value.sourceStatus)
            .put("error", value.error)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_STATE, json.toString()).apply()
    }

    fun migrationEvents(context: Context, now: Long): List<String> {
        val array = runCatching { JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MIGRATIONS, "[]")) }.getOrDefault(JSONArray())
        return List(array.length()) { array.optString(it) }
            .filter { event -> event.substringBefore(':').toLongOrNull()?.let {
                it in (now - 24L * 60L * 60L * 1000L)..now
            } == true }
            .distinct().sortedBy { it.substringBefore(':').toLongOrNull() }
    }

    fun saveMigrationEvents(context: Context, values: List<String>, now: Long) {
        val retained = values.filter { event -> event.substringBefore(':').toLongOrNull()?.let {
            it in (now - 24L * 60L * 60L * 1000L)..now
        } == true }.distinct().sortedBy { it.substringBefore(':').toLongOrNull() }.takeLast(5_000)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_MIGRATIONS, JSONArray(retained).toString()).apply()
    }

    private fun JSONObject.nullableDouble(key: String): Double? =
        if (!has(key) || isNull(key)) null else optDouble(key).takeIf(Double::isFinite)

    private fun JSONObject.putNullable(key: String, value: Any?): JSONObject =
        put(key, value ?: JSONObject.NULL)
}

class PumpEcosystemClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    fun sync(context: Context, now: Long = System.currentTimeMillis()): PumpEcosystemSnapshot {
        if (!PumpEcosystemStore.shouldSync(context, now)) return PumpEcosystemStore.state(context)
        PumpEcosystemStore.markAttempt(context, now)
        val errors = mutableListOf<String>()
        var successfulSources = 0

        val migrations = runCatching { fetchMigrationEvents(now) }.onSuccess { successfulSources++ }
            .onFailure { errors += "миграции: ${it.message.orEmpty().take(80)}" }
            .getOrDefault(emptyList())
        val mergedMigrations = (PumpEcosystemStore.migrationEvents(context, now) + migrations).distinct()
        PumpEcosystemStore.saveMigrationEvents(context, mergedMigrations, now)

        val dex = runCatching { fetchSummary(DEX_URLS) }.onSuccess { successfulSources++ }
            .onFailure { errors += "объём: ${it.message.orEmpty().take(80)}" }.getOrNull()
        val fees = runCatching { fetchSummary(FEE_URLS) }.onSuccess { successfulSources++ }
            .onFailure { errors += "доход: ${it.message.orEmpty().take(80)}" }.getOrNull()
        val buybacks = runCatching { fetchBuybacks() }.onSuccess { successfulSources++ }
            .onFailure { errors += "выкуп: ${it.message.orEmpty().take(80)}" }.getOrNull()

        if (successfulSources == 0) {
            return PumpEcosystemStore.state(context).copy(
                sourceStatus = "0/4 ИСТОЧНИКА ДОСТУПНЫ — СОХРАНЁН ПОСЛЕДНИЙ КАДР",
                error = errors.joinToString("; ").take(240)
            ).also { PumpEcosystemStore.save(context, it) }
        }

        val snapshot = PumpEcosystemAnalyzer.analyze(
            now = now,
            migrationTimes = mergedMigrations.mapNotNull { it.substringBefore(':').toLongOrNull() },
            dexVolume24hUsd = dex?.first,
            dexChange1d = dex?.second,
            revenue24hUsd = fees?.first,
            revenueChange1d = fees?.second,
            buyback24hUsd = buybacks?.buybackUsd,
            buybackChange1d = buybacks?.changePercent,
            burnedPump24h = buybacks?.burnedPump,
            successfulSources = successfulSources,
            sourceStatus = "$successfulSources/4 ИСТОЧНИКА ДОСТУПНЫ",
            error = errors.joinToString("; ")
        )
        PumpEcosystemStore.save(context, snapshot)
        return snapshot
    }

    private fun fetchMigrationEvents(now: Long): List<String> {
        val body = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 1)
            .put("method", "getSignaturesForAddress")
            .put("params", JSONArray().put(PUMP_MIGRATOR).put(JSONObject().put("limit", 500)))
        val root = postJson(SOLANA_RPC, body)
        root.optJSONObject("error")?.let { throw IllegalStateException(it.optString("message", "Solana RPC error")) }
        val array = root.optJSONArray("result") ?: throw IllegalStateException("нет списка миграций")
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val at = item.optLong("blockTime").takeIf { it > 0L }?.times(1000L)
                ?: return@mapNotNull null
            val signature = item.optString("signature").takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            "$at:$signature"
        }.filter { event -> event.substringBefore(':').toLongOrNull()?.let {
            it in (now - 24L * 60L * 60L * 1000L)..now
        } == true }
    }

    private fun fetchSummary(urls: List<String>): Pair<Double?, Double?> {
        var lastError: Throwable? = null
        urls.forEach { url ->
            runCatching {
                val root = getJson(url)
                val total24h = root.finiteDouble("total24h") ?: root.finiteDouble("total24hUsd")
                val change = root.finiteDouble("change_1d") ?: root.finiteDouble("change1d")
                if (total24h == null && change == null) throw IllegalStateException("поля 24ч отсутствуют")
                return total24h to change
            }.onFailure { lastError = it }
        }
        throw lastError ?: IllegalStateException("источник недоступен")
    }

    private data class BuybackFacts(val buybackUsd: Double?, val changePercent: Double?, val burnedPump: Double?)

    private fun fetchBuybacks(): BuybackFacts {
        val root = getJson(BUYBACK_URL)
        val daily = root.optJSONObject("dailyBuybacks") ?: root.optJSONObject("daily_buybacks")
            ?: throw IllegalStateException("dailyBuybacks отсутствует")
        val dates = daily.keys().asSequence().toList().sorted()
        if (dates.isEmpty()) throw IllegalStateException("история выкупов пуста")
        fun value(date: String): Triple<Double?, Double?, Double?> {
            val raw = daily.opt(date)
            if (raw is Number) return Triple(raw.toDouble(), null, null)
            val json = raw as? JSONObject ?: return Triple(null, null, null)
            val usd = json.firstFinite("buybackUsd", "buyback_usd", "usd", "amountUsd", "totalUsd")
            val burned = json.firstFinite("pumpBurned", "pump_burned", "burnedPump", "pumpAmount")
            return Triple(usd, burned, json.firstFinite("sol", "buybackSol"))
        }
        val latest = value(dates.last())
        val previous = dates.dropLast(1).lastOrNull()?.let(::value)
        val change = if (latest.first != null && previous?.first != null && previous.first!! > 0.0) {
            (latest.first!! / previous.first!! - 1.0) * 100.0
        } else null
        val rootBurned = root.firstFinite("burnedPump24h", "pumpBurned24h", "burned_24h")
        return BuybackFacts(latest.first, change, latest.second ?: rootBurned)
    }

    private fun getJson(url: String): JSONObject {
        val request = Request.Builder().url(url).header("User-Agent", "PumpSignal/${BuildConfig.VERSION_NAME}").build()
        return http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
            JSONObject(response.body?.string().orEmpty())
        }
    }

    private fun postJson(url: String, json: JSONObject): JSONObject {
        val request = Request.Builder().url(url)
            .post(json.toString().toRequestBody(JSON_MEDIA))
            .header("User-Agent", "PumpSignal/${BuildConfig.VERSION_NAME}").build()
        return http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
            JSONObject(response.body?.string().orEmpty())
        }
    }

    private fun JSONObject.finiteDouble(key: String): Double? =
        if (!has(key) || isNull(key)) null else optDouble(key).takeIf(Double::isFinite)

    private fun JSONObject.firstFinite(vararg keys: String): Double? =
        keys.firstNotNullOfOrNull { finiteDouble(it) }

    private companion object {
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
        const val SOLANA_RPC = "https://api.mainnet-beta.solana.com"
        const val PUMP_MIGRATOR = "39azUYFWPz3VHgKCf3VChUwbpURdCHRxjWVowf5jUJjg"
        const val BUYBACK_URL = "https://fees.pump.fun/api/buybacks"
        val DEX_URLS = listOf(
            "https://api.llama.fi/summary/dexs/pump.fun",
            "https://api.llama.fi/summary/dexs/pump-fun",
            "https://api.llama.fi/summary/dexs/pumpfun"
        )
        val FEE_URLS = listOf(
            "https://api.llama.fi/summary/fees/pump.fun?dataType=dailyRevenue",
            "https://api.llama.fi/summary/fees/pump-fun?dataType=dailyRevenue",
            "https://api.llama.fi/summary/fees/pumpfun?dataType=dailyRevenue"
        )
    }
}
