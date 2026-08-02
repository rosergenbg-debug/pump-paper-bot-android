package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class ApiUsageEvent(
    val provider: String,
    val circuit: String,
    val model: String,
    val status: String,
    val at: Long,
    val durationMillis: Long = 0L,
    val promptTokens: Int = 0,
    val outputTokens: Int = 0,
    val detail: String = "",
    val appVersion: String = BuildConfig.VERSION_NAME
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
        .put("appVersion", appVersion)

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
            detail = RussianOutputPolicy.visible(json.optString("detail")).take(500),
            appVersion = json.optString("appVersion")
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
        DeepSeekDailyBudgetStore.record(context, event)
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

    fun summary(
        context: Context,
        provider: String,
        now: Long = System.currentTimeMillis(),
        appVersion: String? = null
    ): ApiUsageSummary {
        val events = list(context, provider).let { source ->
            if (appVersion == null) source else source.filter { it.appVersion == appVersion }
        }
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

object DeepSeekDailyBudgetStore {
    private const val PREFS = "deepseek_daily_budget_v48"
    private val utc = TimeZone.getTimeZone("UTC")

    fun costUsd(context: Context, now: Long = System.currentTimeMillis()): Double {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = dayKey(now)
        if (prefs.getString("day", "") != today) {
            prefs.edit().putString("day", today).putLong("cost_bits", 0L).commit()
            return 0.0
        }
        return Double.fromBits(prefs.getLong("cost_bits", 0L))
            .takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
    }

    fun record(context: Context, event: ApiUsageEvent) {
        if (!event.provider.equals("DEEPSEEK", true) ||
            (event.status != "OK" && event.status != "ERROR")) return
        val added = DeepSeekCostPolicy.estimateUsd(event)
        if (added <= 0.0) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = dayKey(event.at)
        synchronized(this) {
            val current = if (prefs.getString("day", "") == today) {
                Double.fromBits(prefs.getLong("cost_bits", 0L))
                    .takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
            } else 0.0
            prefs.edit()
                .putString("day", today)
                .putLong("cost_bits", (current + added).toBits())
                .commit()
        }
    }

    private fun dayKey(now: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = utc
    }.format(Date(now))
}

object DeepSeekCostPolicy {
    private const val FLASH_INPUT_PER_MILLION = 0.14
    private const val FLASH_OUTPUT_PER_MILLION = 0.28
    private const val PRO_INPUT_PER_MILLION = 0.435
    private const val PRO_OUTPUT_PER_MILLION = 0.87

    fun estimateUsd(event: ApiUsageEvent): Double {
        if (!event.provider.equals("DEEPSEEK", true)) return 0.0
        return estimateUsd(event.model, event.promptTokens, event.outputTokens)
    }

    fun estimateUsd(model: String, promptTokens: Int, outputTokens: Int): Double {
        val pro = model.contains("pro", true)
        val inputRate = if (pro) PRO_INPUT_PER_MILLION else FLASH_INPUT_PER_MILLION
        val outputRate = if (pro) PRO_OUTPUT_PER_MILLION else FLASH_OUTPUT_PER_MILLION
        return promptTokens.coerceAtLeast(0) * inputRate / 1_000_000.0 +
            outputTokens.coerceAtLeast(0) * outputRate / 1_000_000.0
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
        val currentUsage = ApiUsageLogStore.summary(context, "DEEPSEEK", now, BuildConfig.VERSION_NAME)
        val events = ApiUsageLogStore.list(context, "DEEPSEEK").takeLast(60).asReversed()
        val budgetCost = DeepSeekDailyBudgetStore.costUsd(context, now)
        return buildString {
            appendLine("PumpSignal V${BuildConfig.VERSION_NAME} • Диагностика DeepSeek")
            appendLine("Создано: ${stamp(now)}")
            appendLine("Пакет: ${BuildConfig.APPLICATION_ID}")
            appendLine()
            appendLine("ОСНОВНОЙ КОНТУР")
            appendLine("модель=${primary.model} действие=${primary.action} направление=${primary.direction} уверенность=${primary.confidence} опасность=${primary.danger}")
            appendLine("последняяПопытка=${stamp(primary.lastAttempt)} последнийУспех=${stamp(primary.lastSuccess)} свежий=${DeepSeekPrimaryPolicy.isFreshSignal(primary, now)}")
            appendLine("вывод=${primary.summary.take(500)}")
            appendLine("факты=${primary.evidence.joinToString(" | ").ifBlank { "нет" }.take(1000)}")
            appendLine("риски=${primary.risks.joinToString(" | ").ifBlank { "нет" }.take(1000)}")
            appendLine("ошибка=${primary.error.ifBlank { "нет" }.take(500)}")
            appendLine()
            appendLine("ПОЗИЦИЯ СЕРЖА")
            appendLine("открыта=${snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0} модель=${position.model} действие=${position.action} опасность=${position.dangerLevel}")
            appendLine("последняяПопытка=${stamp(position.lastAttempt)} последнийУспех=${stamp(position.lastSuccess)} ошибка=${position.error.ifBlank { "нет" }.take(500)}")
            appendLine()
            appendLine("СВЕЖЕСТЬ РЫНКА")
            appendLine("синхронизацияСек=${age(snapshot.lastSync, now)} живаяЦенаСек=${age(snapshot.livePriceAt, now)} свеча30мСек=${age(snapshot.lastCandle, now)}")
            appendLine("микропотокПодключён=${micro.connected} возрастМикропотокаСек=${age(micro.updatedAt, now)} микропотокСвежий=${micro.connected && DeepSeekFreshMarketContext.isFresh(micro.updatedAt, now, DeepSeekFreshMarketContext.MICRO_MAX_AGE)} ошибкаМикропотока=${micro.error.take(240)}")
            appendLine("слой5мВозрастСек=${age(impulse.candleTime, now)} слой5мСвежий=${DeepSeekFreshMarketContext.isFresh(impulse.candleTime, now, DeepSeekFreshMarketContext.FIVE_MINUTE_MAX_AGE)} ошибка5м=${impulse.error.take(240)}")
            appendLine()
            appendLine("ИСПОЛЬЗОВАНИЕ СЕГОДНЯ")
            appendLine("запросы=${usage.requestsToday} успешно=${usage.successesToday} ошибки=${usage.errorsToday} восстановления=${usage.retriesToday}")
            appendLine("токены=${usage.promptTokensToday} вход + ${usage.outputTokensToday} выход оценкаСтоимостиUSD=${"%.5f".format(Locale.US, usage.estimatedCostUsdToday)}")
            appendLine("защитныйСчётчикUSD=${"%.5f".format(Locale.US, budgetCost)} лимитUSD=${"%.2f".format(Locale.US, DeepSeekPrimaryPolicy.DAILY_COST_LIMIT_USD)}")
            appendLine("текущаяВерсия=${BuildConfig.VERSION_NAME} запросы=${currentUsage.requestsToday} успешно=${currentUsage.successesToday} ошибки=${currentUsage.errorsToday} восстановления=${currentUsage.retriesToday}")
            appendLine()
            appendLine("ПОСЛЕДНИЕ СОБЫТИЯ API (новые сверху)")
            events.forEach { event ->
                appendLine("${stamp(event.at)} | версия=${event.appVersion.ifBlank { "старая" }} | ${event.circuit} | ${event.model} | ${event.status} | ${event.durationMillis}мс | ${event.promptTokens}+${event.outputTokens} | ${event.detail.replace('\n', ' ').take(500)}")
            }
            appendLine()
            append("API-ключи, данные авторизации и полные запросы намеренно исключены.")
        }
    }

    private fun age(at: Long, now: Long): String =
        if (at <= 0L || now < at) "unknown" else ((now - at) / 1000L).toString()

    private fun stamp(at: Long): String = if (at <= 0L) "never" else
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date(at))
}
