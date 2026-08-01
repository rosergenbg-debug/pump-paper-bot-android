package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ApiUsageEvent(
    val provider: String,
    val circuit: String,
    val model: String,
    val status: String,
    val at: Long,
    val durationMillis: Long = 0L,
    val promptTokens: Int = 0,
    val outputTokens: Int = 0,
    val detail: String = ""
) {
    fun toJson() = JSONObject()
        .put("provider", provider)
        .put("circuit", circuit)
        .put("model", model)
        .put("status", status)
        .put("at", at)
        .put("durationMillis", durationMillis)
        .put("promptTokens", promptTokens)
        .put("outputTokens", outputTokens)
        .put("detail", detail)

    companion object {
        fun fromJson(json: JSONObject) = ApiUsageEvent(
            provider = json.optString("provider"),
            circuit = json.optString("circuit"),
            model = json.optString("model"),
            status = json.optString("status"),
            at = json.optLong("at"),
            durationMillis = json.optLong("durationMillis"),
            promptTokens = json.optInt("promptTokens").coerceAtLeast(0),
            outputTokens = json.optInt("outputTokens").coerceAtLeast(0),
            detail = json.optString("detail").take(500)
        )
    }
}

data class ApiUsageSummary(
    val requestsLastMinute: Int,
    val requestsLastHour: Int,
    val requestsToday: Int,
    val successesToday: Int,
    val errorsToday: Int,
    val retriesToday: Int,
    val promptTokensToday: Int,
    val outputTokensToday: Int,
    val estimatedCostUsdToday: Double
)

object ApiUsageLogStore {
    private const val PREFS = "api_usage_log_v42"
    private const val KEY_EVENTS = "events"
    private const val MAX_EVENTS = 120
    private val lock = Any()

    fun record(context: Context, event: ApiUsageEvent) = synchronized(lock) {
        val updated = (list(context) + event)
            .filter { event.at - it.at <= 7L * 24L * 60L * 60L * 1000L }
            .takeLast(MAX_EVENTS)
        val json = JSONArray()
        updated.forEach { json.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_EVENTS, json.toString()).apply()
    }

    fun list(context: Context, provider: String? = null): List<ApiUsageEvent> = synchronized(lock) {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_EVENTS, "[]").orEmpty()
        val parsed = runCatching {
            val json = JSONArray(raw)
            List(json.length()) { index -> ApiUsageEvent.fromJson(json.getJSONObject(index)) }
        }.getOrDefault(emptyList())
        if (provider == null) parsed else parsed.filter { it.provider.equals(provider, true) }
    }

    fun summary(context: Context, provider: String, now: Long = System.currentTimeMillis()): ApiUsageSummary {
        val events = list(context, provider)
        val todayStart = now - now % (24L * 60L * 60L * 1000L)
        val requests = events.filter { it.status == "START" || it.status == "RETRY" }
        val today = events.filter { it.at >= todayStart }
        val terminal = today.filter { it.status == "OK" || it.status == "ERROR" }
        return ApiUsageSummary(
            requestsLastMinute = requests.count { now - it.at <= 60_000L },
            requestsLastHour = requests.count { now - it.at <= 3_600_000L },
            requestsToday = requests.count { it.at >= todayStart },
            successesToday = today.count { it.status == "OK" },
            errorsToday = today.count { it.status == "ERROR" },
            retriesToday = today.count { it.status == "RETRY" },
            promptTokensToday = terminal.sumOf { it.promptTokens },
            outputTokensToday = terminal.sumOf { it.outputTokens },
            estimatedCostUsdToday = terminal.sumOf { DeepSeekCostPolicy.estimateUsd(it) }
        )
    }
}

object DeepSeekCostPolicy {
    private const val FLASH_INPUT_PER_MILLION = 0.14
    private const val FLASH_OUTPUT_PER_MILLION = 0.28
    private const val PRO_INPUT_PER_MILLION = 0.435
    private const val PRO_OUTPUT_PER_MILLION = 0.87

    fun estimateUsd(event: ApiUsageEvent): Double {
        if (!event.provider.equals("DEEPSEEK", true)) return 0.0
        val pro = event.model.contains("pro", true)
        val inputRate = if (pro) PRO_INPUT_PER_MILLION else FLASH_INPUT_PER_MILLION
        val outputRate = if (pro) PRO_OUTPUT_PER_MILLION else FLASH_OUTPUT_PER_MILLION
        return event.promptTokens * inputRate / 1_000_000.0 +
            event.outputTokens * outputRate / 1_000_000.0
    }
}

object DeepSeekDiagnostics {
    fun report(context: Context, now: Long = System.currentTimeMillis()): String {
        val primary = DeepSeekPrimaryStore.state(context, now)
        val position = PositionSupervisorStore.state(context)
        val snapshot = PumpBotEngine.snapshot(context)
        val impulse = ImpulseRadarStore.state(context)
        val micro = MicroImpulseStore.state(context)
        val usage = ApiUsageLogStore.summary(context, "DEEPSEEK", now)
        val events = ApiUsageLogStore.list(context, "DEEPSEEK").takeLast(60).asReversed()
        return buildString {
            appendLine("PumpSignal V${BuildConfig.VERSION_NAME} • DeepSeek diagnostics")
            appendLine("Generated: ${stamp(now)}")
            appendLine("Package: ${BuildConfig.APPLICATION_ID}")
            appendLine()
            appendLine("PRIMARY")
            appendLine("model=${primary.model} action=${primary.action} direction=${primary.direction} confidence=${primary.confidence} danger=${primary.danger}")
            appendLine("lastAttempt=${stamp(primary.lastAttempt)} lastSuccess=${stamp(primary.lastSuccess)} fresh=${DeepSeekPrimaryPolicy.isFreshSignal(primary, now)}")
            appendLine("summary=${primary.summary.take(500)}")
            appendLine("error=${primary.error.ifBlank { "none" }.take(500)}")
            appendLine()
            appendLine("POSITION")
            appendLine("open=${snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0} model=${position.model} action=${position.action} danger=${position.dangerLevel}")
            appendLine("lastAttempt=${stamp(position.lastAttempt)} lastSuccess=${stamp(position.lastSuccess)} error=${position.error.ifBlank { "none" }.take(500)}")
            appendLine()
            appendLine("MARKET FRESHNESS")
            appendLine("marketSyncAgeSec=${age(snapshot.lastSync, now)} livePriceAgeSec=${age(snapshot.livePriceAt, now)} closed30mAgeSec=${age(snapshot.lastCandle, now)}")
            appendLine("microConnected=${micro.connected} microAgeSec=${age(micro.updatedAt, now)} microFresh=${micro.connected && DeepSeekFreshMarketContext.isFresh(micro.updatedAt, now, DeepSeekFreshMarketContext.MICRO_MAX_AGE)} microError=${micro.error.take(240)}")
            appendLine("fiveMinuteAgeSec=${age(impulse.candleTime, now)} fiveMinuteFresh=${DeepSeekFreshMarketContext.isFresh(impulse.candleTime, now, DeepSeekFreshMarketContext.FIVE_MINUTE_MAX_AGE)} fiveMinuteError=${impulse.error.take(240)}")
            appendLine()
            appendLine("USAGE TODAY")
            appendLine("httpRequests=${usage.requestsToday} ok=${usage.successesToday} errors=${usage.errorsToday} repairs=${usage.retriesToday}")
            appendLine("tokens=${usage.promptTokensToday} input + ${usage.outputTokensToday} output estimatedCostUsd=${"%.5f".format(Locale.US, usage.estimatedCostUsdToday)}")
            appendLine()
            appendLine("RECENT API EVENTS (newest first)")
            events.forEach { event ->
                appendLine("${stamp(event.at)} | ${event.circuit} | ${event.model} | ${event.status} | ${event.durationMillis}ms | ${event.promptTokens}+${event.outputTokens} | ${event.detail.replace('\n', ' ').take(500)}")
            }
            appendLine()
            append("API keys and request payloads are intentionally excluded.")
        }
    }

    private fun age(at: Long, now: Long): String =
        if (at <= 0L || now < at) "unknown" else ((now - at) / 1000L).toString()

    private fun stamp(at: Long): String = if (at <= 0L) "never" else
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date(at))
}
