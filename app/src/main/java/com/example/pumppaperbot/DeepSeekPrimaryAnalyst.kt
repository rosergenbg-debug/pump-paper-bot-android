package com.example.pumppaperbot

import android.content.Context
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class DeepSeekPrimaryState(
    val day: String = "",
    val lastAttempt: Long = 0L,
    val lastSuccess: Long = 0L,
    val model: String = "",
    val action: String = "WAIT",
    val direction: Int = 0,
    val danger: Int = 0,
    val confidence: Int = 0,
    val summary: String = "Ожидает первый рыночный кадр",
    val successfulToday: Int = 0,
    val failedToday: Int = 0,
    val promptTokensToday: Int = 0,
    val completionTokensToday: Int = 0,
    val lastInputReadiness: Int = 0,
    val lastLocalBuySignal: Boolean = false,
    val lastLocalSellSignal: Boolean = false,
    val evidence: List<String> = emptyList(),
    val risks: List<String> = emptyList(),
    val error: String = ""
) {
    fun toJson() = JSONObject()
        .put("day", day)
        .put("lastAttempt", lastAttempt)
        .put("lastSuccess", lastSuccess)
        .put("model", model)
        .put("action", action)
        .put("direction", direction)
        .put("danger", danger)
        .put("confidence", confidence)
        .put("summary", summary)
        .put("successfulToday", successfulToday)
        .put("failedToday", failedToday)
        .put("promptTokensToday", promptTokensToday)
        .put("completionTokensToday", completionTokensToday)
        .put("lastInputReadiness", lastInputReadiness)
        .put("lastLocalBuySignal", lastLocalBuySignal)
        .put("lastLocalSellSignal", lastLocalSellSignal)
        .put("evidence", JSONArray(evidence))
        .put("risks", JSONArray(risks))
        .put("error", error)

    companion object {
        fun fromJson(json: JSONObject) = DeepSeekPrimaryState(
            day = json.optString("day"),
            lastAttempt = json.optLong("lastAttempt"),
            lastSuccess = json.optLong("lastSuccess"),
            model = json.optString("model"),
            action = json.optString("action", "WAIT"),
            direction = json.optInt("direction").coerceIn(-100, 100),
            danger = json.optInt("danger").coerceIn(0, 10),
            confidence = json.optInt("confidence").coerceIn(0, 100),
            summary = json.optString("summary", "Ожидает первый рыночный кадр"),
            successfulToday = json.optInt("successfulToday").coerceAtLeast(0),
            failedToday = json.optInt("failedToday").coerceAtLeast(0),
            promptTokensToday = json.optInt("promptTokensToday").coerceAtLeast(0),
            completionTokensToday = json.optInt("completionTokensToday").coerceAtLeast(0),
            lastInputReadiness = json.optInt("lastInputReadiness").coerceIn(-100, 100),
            lastLocalBuySignal = json.optBoolean("lastLocalBuySignal"),
            lastLocalSellSignal = json.optBoolean("lastLocalSellSignal"),
            evidence = json.optJSONArray("evidence")?.let { array ->
                List(array.length()) { array.optString(it).take(240) }.filter { it.isNotBlank() }
            }.orEmpty(),
            risks = json.optJSONArray("risks")?.let { array ->
                List(array.length()) { array.optString(it).take(240) }.filter { it.isNotBlank() }
            }.orEmpty(),
            error = json.optString("error")
        )
    }
}

object DeepSeekPrimaryStore {
    private const val PREFS = "deepseek_primary_v41"
    private const val KEY_STATE = "state"
    private val utc = TimeZone.getTimeZone("UTC")

    fun dayKey(now: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = utc
    }.format(Date(now))

    fun state(context: Context, now: Long = System.currentTimeMillis()): DeepSeekPrimaryState {
        val stored = runCatching {
            DeepSeekPrimaryState.fromJson(JSONObject(
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(KEY_STATE, "{}").orEmpty()
            ))
        }.getOrDefault(DeepSeekPrimaryState())
        val today = dayKey(now)
        return if (stored.day == today) stored else stored.copy(
            day = today,
            successfulToday = 0,
            failedToday = 0,
            promptTokensToday = 0,
            completionTokensToday = 0
        ).also { save(context, it) }
    }

    fun save(context: Context, state: DeepSeekPrimaryState) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_STATE, state.toJson().toString()).apply()
    }
}

object DeepSeekPrimaryPolicy {
    const val INTERVAL = 5L * 60L * 1000L
    const val SIGNAL_MAX_AGE = 12L * 60L * 1000L

    fun isFreshSignal(state: DeepSeekPrimaryState, now: Long = System.currentTimeMillis()): Boolean =
        state.lastSuccess > 0L && now >= state.lastSuccess && now - state.lastSuccess <= SIGNAL_MAX_AGE

    fun chooseModel(snapshot: LiveSnapshot, force: Boolean, materialChange: Boolean): String {
        val critical = snapshot.buySignal || snapshot.sellSignal || snapshot.rapidDrop.active ||
            kotlin.math.abs(snapshot.readinessScore) >= 95 || kotlin.math.abs(snapshot.directionScore) >= 75 ||
            (snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0 && snapshot.directionScore <= -55)
        return if (force || (materialChange && critical)) {
            PositionSupervisorPolicy.PRO_MODEL
        } else {
            PositionSupervisorPolicy.FLASH_MODEL
        }
    }

    fun shouldRun(
        state: DeepSeekPrimaryState,
        hasMarketData: Boolean,
        force: Boolean,
        now: Long,
        materialChange: Boolean = false
    ): Boolean = hasMarketData && (
        force || state.lastAttempt <= 0L || materialChange || now - state.lastAttempt >= INTERVAL
    )

    fun compactStatus(
        state: DeepSeekPrimaryState,
        configured: Boolean,
        now: Long = System.currentTimeMillis()
    ): String = when {
        !configured -> "DEEPSEEK • ОСНОВНОЙ • ключ не введён"
        state.lastSuccess <= 0L && state.error.isNotBlank() ->
            "DEEPSEEK • ОСНОВНОЙ • ошибка: ${state.error}\nЗапросы сегодня: 0 успешно • ${state.failedToday} ошибок"
        state.lastSuccess <= 0L -> "DEEPSEEK • ОСНОВНОЙ • ожидает первый анализ"
        else -> buildString {
            append("DEEPSEEK • ОСНОВНОЙ • ${shortModel(state.model)} • ")
            append(if (isFreshSignal(state, now)) state.action else "РЕЗУЛЬТАТ УСТАРЕЛ")
            append("\n${state.summary}")
            append("\nСегодня: ${state.successfulToday} успешно • ${state.failedToday} ошибок")
            append(" • последний ${PumpBotEngine.formatTime(state.lastSuccess)}")
            if (state.error.isNotBlank()) append("\nПоследняя ошибка: ${state.error}")
        }
    }

    private fun shortModel(model: String) = when {
        model.contains("pro", ignoreCase = true) -> "PRO"
        model.contains("flash", ignoreCase = true) -> "FLASH"
        model.isBlank() -> "FLASH"
        else -> model.uppercase(Locale.ROOT)
    }
}

private data class DeepSeekPrimaryResult(
    val action: String,
    val direction: Int,
    val danger: Int,
    val confidence: Int,
    val summary: String,
    val evidence: List<String>,
    val risks: List<String>,
    val promptTokens: Int,
    val completionTokens: Int,
    val repaired: Boolean
)

class DeepSeekPrimaryAnalyst {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(75, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun sync(
        context: Context,
        force: Boolean = false,
        now: Long = System.currentTimeMillis()
    ): DeepSeekPrimaryState {
        val snapshot = PumpBotEngine.snapshot(context)
        val previous = DeepSeekPrimaryStore.state(context, now)
        val materialChange = previous.lastAttempt > 0L && (
            kotlin.math.abs(snapshot.readinessScore - previous.lastInputReadiness) >= 15 ||
                snapshot.buySignal != previous.lastLocalBuySignal ||
                snapshot.sellSignal != previous.lastLocalSellSignal
            )
        if (!DeepSeekPrimaryPolicy.shouldRun(
                previous, snapshot.lastPrice > 0.0, force, now, materialChange
            )) return previous
        val key = DeepSeekSecureKeyStore.read(context)
        if (key.isBlank()) return previous.copy(
            lastAttempt = now,
            error = "API-ключ DeepSeek не введён"
        ).also { DeepSeekPrimaryStore.save(context, it) }

        val requestedModel = DeepSeekPrimaryPolicy.chooseModel(snapshot, force, materialChange)

        DeepSeekPrimaryStore.save(context, previous.copy(
            lastAttempt = now,
            model = requestedModel,
            lastInputReadiness = snapshot.readinessScore,
            lastLocalBuySignal = snapshot.buySignal,
            lastLocalSellSignal = snapshot.sellSignal,
            error = ""
        ))
        val started = System.currentTimeMillis()
        ApiUsageLogStore.record(context, ApiUsageEvent(
            provider = "DEEPSEEK", circuit = "ОСНОВНОЙ РЫНОК",
            model = requestedModel, status = "START", at = started,
            detail = when {
                force -> "ручная усиленная проверка"
                materialChange -> "существенно изменился рыночный сигнал"
                else -> "плановый анализ"
            }
        ))
        return runCatching { analyze(context, key, requestedModel, snapshot, EventRadarStore.state(context)) }.fold(
            onSuccess = { result ->
                ApiUsageLogStore.record(context, ApiUsageEvent(
                    provider = "DEEPSEEK", circuit = "ОСНОВНОЙ РЫНОК",
                    model = requestedModel, status = "OK", at = System.currentTimeMillis(),
                    durationMillis = System.currentTimeMillis() - started,
                    promptTokens = result.promptTokens, outputTokens = result.completionTokens,
                    detail = if (result.repaired) "Ответ восстановлен коротким повтором • ${result.summary}" else result.summary
                ))
                previous.copy(
                    lastAttempt = now,
                    lastSuccess = now,
                    model = requestedModel,
                    action = result.action,
                    direction = result.direction,
                    danger = result.danger,
                    confidence = result.confidence,
                    summary = result.summary,
                    successfulToday = previous.successfulToday + 1,
                    promptTokensToday = previous.promptTokensToday + result.promptTokens,
                    completionTokensToday = previous.completionTokensToday + result.completionTokens,
                    lastInputReadiness = snapshot.readinessScore,
                    lastLocalBuySignal = snapshot.buySignal,
                    lastLocalSellSignal = snapshot.sellSignal,
                    evidence = result.evidence,
                    risks = result.risks,
                    error = ""
                ).also { DeepSeekPrimaryStore.save(context, it) }
            },
            onFailure = { error ->
                val structured = error as? DeepSeekStructuredException
                ApiUsageLogStore.record(context, ApiUsageEvent(
                    provider = "DEEPSEEK", circuit = "ОСНОВНОЙ РЫНОК",
                    model = requestedModel, status = "ERROR", at = System.currentTimeMillis(),
                    durationMillis = System.currentTimeMillis() - started,
                    promptTokens = structured?.promptTokens ?: 0,
                    outputTokens = structured?.completionTokens ?: 0,
                    detail = buildString {
                        append(error.message.orEmpty().take(240))
                        structured?.finishReason?.takeIf { it.isNotBlank() }?.let { append(" • finish_reason=$it") }
                    }
                ))
                previous.copy(
                    lastAttempt = now,
                    model = requestedModel,
                    lastInputReadiness = snapshot.readinessScore,
                    lastLocalBuySignal = snapshot.buySignal,
                    lastLocalSellSignal = snapshot.sellSignal,
                    failedToday = previous.failedToday + 1,
                    error = error.message.orEmpty().take(240)
                ).also { DeepSeekPrimaryStore.save(context, it) }
            }
        )
    }

    private fun analyze(
        context: Context,
        apiKey: String,
        model: String,
        snapshot: LiveSnapshot,
        radar: EventRadarState
    ): DeepSeekPrimaryResult {
        val now = System.currentTimeMillis()
        val latestNews = radar.recent.take(5).map { event ->
            JSONObject()
                .put("source", event.source)
                .put("title", event.title.take(220))
                .put("importance", event.importance)
                .put("direction", event.directionScore)
                .put("published_at", event.publishedAt)
        }
        val hourly = GeminiMarketFrame.from(context)
        val frame = JSONObject()
            .put("symbol", "PUMP/EUR")
            .put("closed_30m_price_eur", snapshot.lastPrice)
            .put("rsi", snapshot.lastRsi)
            .put("ema_200", snapshot.lastEma200)
            .put("funding_rate", snapshot.fundingRate)
            .put("direction_score", snapshot.directionScore)
            .put("readiness_score", snapshot.readinessScore)
            .put("market_confidence", snapshot.breathingConfidence)
            .put("energy_score", snapshot.energyScore)
            .put("book_imbalance", snapshot.bookImbalance ?: JSONObject.NULL)
            .put("spread_percent", snapshot.spreadPercent ?: JSONObject.NULL)
            .put("open_interest_contracts", snapshot.openInterest ?: JSONObject.NULL)
            .put("open_interest_change_since_previous_sync_pct", snapshot.openInterestChangePercent ?: JSONObject.NULL)
            .put("rapid_drop_active", snapshot.rapidDrop.active)
            .put("local_buy_signal", snapshot.buySignal)
            .put("local_sell_signal", snapshot.sellSignal)
            .put("local_reason", snapshot.signalReason.take(600))
            .put("user_position_open", snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0)
            .put("hourly_context_age_seconds", hourly?.let {
                DeepSeekFreshMarketContext.ageSeconds(it.candleTime, now)
            } ?: JSONObject.NULL)
            .put("hourly_pump_change_1h_pct", hourly?.pump1hPercent ?: JSONObject.NULL)
            .put("hourly_pump_change_3h_pct", hourly?.pump3hPercent ?: JSONObject.NULL)
            .put("hourly_pump_change_6h_pct", hourly?.pump6hPercent ?: JSONObject.NULL)
            .put("hourly_btc_change_1h_pct", hourly?.btc1hPercent ?: JSONObject.NULL)
            .put("hourly_btc_change_3h_pct", hourly?.btc3hPercent ?: JSONObject.NULL)
            .put("hourly_sol_change_1h_pct", hourly?.sol1hPercent ?: JSONObject.NULL)
            .put("hourly_sol_change_3h_pct", hourly?.sol3hPercent ?: JSONObject.NULL)
            .put("hourly_spot_taker_buy_pct", hourly?.spotTakerBuyPercent ?: JSONObject.NULL)
            .put("hourly_futures_taker_buy_pct", hourly?.futuresTakerBuyPercent ?: JSONObject.NULL)
            .put("hourly_spot_cvd_proxy_pct", hourly?.spotCvdPercent ?: JSONObject.NULL)
            .put("hourly_futures_cvd_proxy_pct", hourly?.futuresCvdPercent ?: JSONObject.NULL)
            .put("premium_last_full_hour_pct", hourly?.premiumPercent ?: JSONObject.NULL)
            .put("news", JSONArray(latestNews))
        DeepSeekFreshMarketContext.append(context, frame, snapshot, now)
        val positionOpen = snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0
        val allowedActions = if (positionOpen) setOf("BUY", "HOLD", "WATCH", "EXIT") else setOf("BUY", "HOLD", "WATCH")
        val system = """
            Ты основной независимый аналитик PumpSignal для PUMP/EUR. Оцени цену на горизонте 1–6 часов.
            Сначала сопоставь PUMP 1ч/3ч/6ч, BTC и SOL, spot/futures taker flow, CVD, funding,
            premium, стакан, open interest, RSI, локальную StrategyV2 и свежие новости.
            Поля real_time_spot_flow — анонимные исполненные spot-сделки и лучший bid/ask, а не личности трейдеров.
            Поля five_minute_flow — последние закрытые 5-минутные spot/futures данные. Часовые CVD являются
            прокси по taker-volume закрытых свечей, а funding_rate — последняя рассчитанная ставка, не прогноз.
            Всегда учитывай age_seconds и fresh. Просроченные или null-поля не используй как текущий факт.
            Краткий real-time всплеск используй как подтверждение либо предупреждение, но не как самостоятельный BUY.
            Не считай один индикатор или один заголовок достаточным основанием. Не догоняй уже перегретую цену.
            BUY допустим только при подтверждении минимум двумя независимыми группами данных и отсутствии
            late-entry/rapid-drop запрета. EXIT допустим только при открытой позиции и согласованном ухудшении.
            Отделяй факты из кадра от предположений. Не подменяй StrategyV2 и не обещай прибыль.
            Верни только JSON:
            action BUY, HOLD, WATCH или EXIT; direction целое -100..100; danger целое 0..10;
            confidence целое 0..100; summary одно короткое конкретное предложение по-русски;
            evidence массив из 2–4 коротких фактов; risks массив из 1–3 условий, которые опровергнут вывод.
            Если пользователь не в позиции, EXIT не используй. Если данных недостаточно, выбери WATCH.
        """.trimIndent()
        val response = DeepSeekStructuredClient(http).request(
            apiKey = apiKey,
            model = model,
            system = system,
            frame = frame,
            reasoningEffort = if (model == PositionSupervisorPolicy.PRO_MODEL) "high" else "low",
            maxTokens = if (model == PositionSupervisorPolicy.PRO_MODEL) 3200 else 1600,
            validate = { json ->
                when {
                    json.optString("action").uppercase(Locale.ROOT) !in allowedActions -> "action отсутствует или недопустим"
                    !json.has("direction") -> "нет direction"
                    !json.has("danger") -> "нет danger"
                    !json.has("confidence") -> "нет confidence"
                    json.optString("summary").isBlank() -> "нет summary"
                    else -> null
                }
            },
            onRepairStart = { reason ->
                ApiUsageLogStore.record(context, ApiUsageEvent(
                    provider = "DEEPSEEK", circuit = "ВОССТАНОВЛЕНИЕ РЫНКА",
                    model = model, status = "RETRY", at = System.currentTimeMillis(),
                    detail = reason.take(260)
                ))
            }
        )
        val json = response.json
        val action = json.optString("action", "WATCH").uppercase(Locale.ROOT)
            .takeIf { it in allowedActions } ?: "WATCH"
        return DeepSeekPrimaryResult(
            action = action,
            direction = json.optInt("direction").coerceIn(-100, 100),
            danger = json.optInt("danger").coerceIn(0, 10),
            confidence = json.optInt("confidence").coerceIn(0, 100),
            summary = json.optString("summary", "DeepSeek не дал пояснение").take(400),
            evidence = json.optJSONArray("evidence")?.let { array ->
                List(array.length().coerceAtMost(4)) { array.optString(it).take(240) }
                    .filter { it.isNotBlank() }
            }.orEmpty(),
            risks = json.optJSONArray("risks")?.let { array ->
                List(array.length().coerceAtMost(3)) { array.optString(it).take(240) }
                    .filter { it.isNotBlank() }
            }.orEmpty(),
            promptTokens = response.promptTokens,
            completionTokens = response.completionTokens,
            repaired = response.repaired
        )
    }
}
