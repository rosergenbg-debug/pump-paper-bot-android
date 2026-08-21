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
import kotlin.math.abs

data class DeepSeekPrimaryState(
    val day: String = "",
    val lastAttempt: Long = 0L,
    val lastSuccess: Long = 0L,
    val model: String = "",
    val action: String = "WAIT",
    val modelIntent: String = "WATCH",
    val proposedAction: String = "WAIT",
    val executionStatus: String = "ОЖИДАНИЕ",
    val verificationSummary: String = "проверка сделки не требовалась",
    val direction: Int = 0,
    val danger: Int = 0,
    val confidence: Int = 0,
    val entryReadiness: Int = 1,
    val summary: String = "Ожидает первый рыночный кадр",
    val shortScenario: String = "Краткосрочный сценарий ещё не рассчитан",
    val longScenario: String = "Долгосрочный сценарий ещё не рассчитан",
    val invalidation: String = "Условие отмены ещё не рассчитано",
    val uncertainty: String = "Неопределённость ещё не рассчитана",
    val successfulToday: Int = 0,
    val failedToday: Int = 0,
    val promptTokensToday: Int = 0,
    val completionTokensToday: Int = 0,
    val estimatedCostUsdToday: Double = 0.0,
    val lastInputReadiness: Int = 0,
    val lastLocalBuySignal: Boolean = false,
    val lastLocalSellSignal: Boolean = false,
    val lastBreathingScore: Int = 0,
    val lastEcosystemScore: Int = 0,
    val independentEntryConfirmStreak: Int = 0,
    val independentExitConfirmStreak: Int = 0,
    val lastPersistenceEvaluationAt: Long = 0L,
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
        .put("modelIntent", modelIntent)
        .put("proposedAction", proposedAction)
        .put("executionStatus", executionStatus)
        .put("verificationSummary", verificationSummary)
        .put("direction", direction)
        .put("danger", danger)
        .put("confidence", confidence)
        .put("entryReadiness", entryReadiness)
        .put("summary", summary)
        .put("shortScenario", shortScenario)
        .put("longScenario", longScenario)
        .put("invalidation", invalidation)
        .put("uncertainty", uncertainty)
        .put("successfulToday", successfulToday)
        .put("failedToday", failedToday)
        .put("promptTokensToday", promptTokensToday)
        .put("completionTokensToday", completionTokensToday)
        .put("estimatedCostUsdToday", estimatedCostUsdToday)
        .put("lastInputReadiness", lastInputReadiness)
        .put("lastLocalBuySignal", lastLocalBuySignal)
        .put("lastLocalSellSignal", lastLocalSellSignal)
        .put("lastBreathingScore", lastBreathingScore)
        .put("lastEcosystemScore", lastEcosystemScore)
        .put("independentEntryConfirmStreak", independentEntryConfirmStreak)
        .put("independentExitConfirmStreak", independentExitConfirmStreak)
        .put("lastPersistenceEvaluationAt", lastPersistenceEvaluationAt)
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
            modelIntent = json.optString("modelIntent", json.optString("proposedAction", json.optString("action", "WATCH"))),
            proposedAction = json.optString("proposedAction", json.optString("action", "WAIT")),
            executionStatus = RussianOutputPolicy.visible(json.optString("executionStatus", "ОЖИДАНИЕ")),
            verificationSummary = RussianOutputPolicy.visible(
                json.optString("verificationSummary", "проверка сделки не требовалась")
            ),
            direction = json.optInt("direction").coerceIn(-100, 100),
            danger = json.optInt("danger").coerceIn(0, 10),
            confidence = json.optInt("confidence").coerceIn(0, 100),
            entryReadiness = json.optInt(
                "entryReadiness",
                (json.optInt("direction").coerceAtLeast(0) / 10).coerceAtLeast(1)
            ).coerceIn(1, 10),
            summary = RussianOutputPolicy.visible(json.optString("summary", "Ожидает первый рыночный кадр")),
            shortScenario = RussianOutputPolicy.visible(json.optString("shortScenario", "Краткосрочный сценарий ещё не рассчитан")),
            longScenario = RussianOutputPolicy.visible(json.optString("longScenario", "Долгосрочный сценарий ещё не рассчитан")),
            invalidation = RussianOutputPolicy.visible(json.optString("invalidation", "Условие отмены ещё не рассчитано")),
            uncertainty = RussianOutputPolicy.visible(json.optString("uncertainty", "Неопределённость ещё не рассчитана")),
            successfulToday = json.optInt("successfulToday").coerceAtLeast(0),
            failedToday = json.optInt("failedToday").coerceAtLeast(0),
            promptTokensToday = json.optInt("promptTokensToday").coerceAtLeast(0),
            completionTokensToday = json.optInt("completionTokensToday").coerceAtLeast(0),
            estimatedCostUsdToday = json.optDouble("estimatedCostUsdToday")
                .takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0,
            lastInputReadiness = json.optInt("lastInputReadiness").coerceIn(-100, 100),
            lastLocalBuySignal = json.optBoolean("lastLocalBuySignal"),
            lastLocalSellSignal = json.optBoolean("lastLocalSellSignal"),
            lastBreathingScore = json.optInt("lastBreathingScore").coerceIn(-100, 100),
            lastEcosystemScore = json.optInt("lastEcosystemScore").coerceIn(-100, 100),
            independentEntryConfirmStreak = json.optInt("independentEntryConfirmStreak").coerceIn(0, 2),
            independentExitConfirmStreak = json.optInt("independentExitConfirmStreak").coerceIn(0, 2),
            lastPersistenceEvaluationAt = json.optLong("lastPersistenceEvaluationAt").coerceAtLeast(0L),
            evidence = json.optJSONArray("evidence")?.let { array ->
                List(array.length()) { RussianOutputPolicy.visible(array.optString(it)).take(240) }.filter { it.isNotBlank() }
            }.orEmpty(),
            risks = json.optJSONArray("risks")?.let { array ->
                List(array.length()) { RussianOutputPolicy.visible(array.optString(it)).take(240) }.filter { it.isNotBlank() }
            }.orEmpty(),
            error = RussianOutputPolicy.visible(json.optString("error"))
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
            completionTokensToday = 0,
            estimatedCostUsdToday = 0.0
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

    fun chooseModel(
        snapshot: LiveSnapshot,
        force: Boolean,
        materialChange: Boolean,
        actionLevel: DeepSeekActionLevel = DeepSeekActionLevel(
            DeepSeekActionPhase.ENTRY,
            1,
            DeepSeekActionBand.RED,
            "НЕ ВХОДИТЬ",
            "Ожидается свежая оценка"
        ),
        analyticalConflict: Boolean = false
    ): String {
        val critical = snapshot.buySignal || snapshot.sellSignal || snapshot.rapidDrop.active ||
            kotlin.math.abs(snapshot.readinessScore) >= 95 || kotlin.math.abs(snapshot.directionScore) >= 75 ||
            (snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0 && snapshot.directionScore <= -55)
        return if (force || actionLevel.proPreferred || analyticalConflict || (materialChange && critical)) {
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
        materialChange: Boolean = false,
        intervalMillis: Long = INTERVAL
    ): Boolean = hasMarketData && (
        force || state.lastAttempt <= 0L || materialChange || now - state.lastAttempt >= intervalMillis
    )

    fun compactStatus(
        state: DeepSeekPrimaryState,
        configured: Boolean,
        now: Long = System.currentTimeMillis()
    ): String = when {
        !configured -> "DEEPSIG • НЕЗАВИСИМЫЙ ТЕСТ • ключ DeepSeek не введён"
        state.lastSuccess <= 0L && state.error.isNotBlank() ->
            "DEEPSIG • НЕЗАВИСИМЫЙ ТЕСТ • ошибка: ${state.error}\nЗапросы сегодня: 0 успешно • ${state.failedToday} ошибок"
        state.lastSuccess <= 0L -> "DEEPSIG • НЕЗАВИСИМЫЙ ТЕСТ • ожидает первый анализ"
        else -> buildString {
            append("DEEPSIG • ${shortModel(state.model)} • ")
            append(if (isFreshSignal(state, now)) state.action else "РЕЗУЛЬТАТ УСТАРЕЛ")
            append("\n${state.summary}")
            if (state.proposedAction != state.action) {
                append("\nПочему без сделки: ${state.verificationSummary}")
            }
            append("\nБлижайшее: ${state.shortScenario}")
            append("\nПересмотр: ${state.invalidation}")
            append("\nОбновлено ${PumpBotEngine.formatTime(state.lastSuccess)}")
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

internal object DeepSeekTradeVerificationPolicy {
    fun finalAction(proposedAction: String, approved: Boolean?, positionOpen: Boolean): String = when {
        approved == null || approved -> proposedAction
        positionOpen -> "HOLD"
        else -> "WATCH"
    }

    fun acceptedDirection(proposedAction: String, approved: Boolean?, direction: Int): Int =
        direction.coerceIn(-100, 100)

    fun acceptedConfidence(proposedAction: String, approved: Boolean?, confidence: Int): Int =
        confidence.coerceIn(0, 100)
}

internal object DeepSeekTradeIntentPolicy {
    fun normalize(
        modelAction: String,
        positionOpen: Boolean,
        entryReadiness: Int,
        direction: Int,
        confidence: Int,
        hardVeto: Boolean,
        locallyConfirmed: Boolean
    ): String {
        if (positionOpen || modelAction == "BUY" || hardVeto) return modelAction
        return if (modelAction in setOf("WATCH", "HOLD") && entryReadiness >= 9 &&
            direction >= 50 && confidence >= 60 && locallyConfirmed
        ) "BUY" else modelAction
    }
}

private data class DeepSeekPrimaryResult(
    val action: String,
    val modelIntent: String,
    val proposedAction: String,
    val executionStatus: String,
    val direction: Int,
    val danger: Int,
    val confidence: Int,
    val entryReadiness: Int,
    val summary: String,
    val shortScenario: String,
    val longScenario: String,
    val invalidation: String,
    val uncertainty: String,
    val evidence: List<String>,
    val risks: List<String>,
    val independentEntryConfirmStreak: Int,
    val independentExitConfirmStreak: Int,
    val lastPersistenceEvaluationAt: Long,
    val promptTokens: Int,
    val completionTokens: Int,
    val verificationPromptTokens: Int = 0,
    val verificationCompletionTokens: Int = 0,
    val verificationSummary: String = "не требовалась",
    val repaired: Boolean,
    val finishReason: String
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
        val fusionPriority = FusionPriorityPolicy.plan(FusionSimStore.state(context))
        val forceProModel = force || fusionPriority.forcePro
        val micro = MicroImpulseStore.state(context)
        val breathing = LiveMarketBreathingStore.snapshot(context, now)
        val ecosystem = PumpEcosystemStore.state(context)
        val featureKey = EvidenceFeatureKey.from(snapshot, breathing.normalScore, ecosystem)
        val memoryPrompt = DeepSeekEvidenceMemory.promptSummary(context, featureKey, now)
        val memorySupplied = listOf("promoted_patterns", "background_patterns").any { key ->
            (memoryPrompt.optJSONArray(key)?.length() ?: 0) > 0
        }
        val ecosystemScore = ecosystem.score ?: 0
        val analyticalConflict = ecosystem.fresh(now) && ecosystem.dataQuality >= 50 &&
            breathing.normalScore != null && abs(ecosystemScore) >= 20 && abs(breathing.normalScore) >= 20 &&
            ecosystemScore * breathing.normalScore < 0
        val previousActionLevel = DeepSeekActionLevelPolicy.fromMarket(
            snapshot, previous, micro, now, breathing
        )
        val materialChange = previous.lastAttempt > 0L && (
            kotlin.math.abs(snapshot.readinessScore - previous.lastInputReadiness) >= 15 ||
                kotlin.math.abs((breathing.normalScore ?: 0) - previous.lastBreathingScore) >= 12 ||
                kotlin.math.abs(ecosystemScore - previous.lastEcosystemScore) >= 20 ||
                snapshot.buySignal != previous.lastLocalBuySignal ||
                snapshot.sellSignal != previous.lastLocalSellSignal
            )
        val adaptiveInterval = if (fusionPriority.active) {
            fusionPriority.intervalMillis
        } else if (snapshot.waitMode == "BUY" && (
                previousActionLevel.intensive || kotlin.math.abs(breathing.normalScore ?: 0) >= 20
            )) {
            DeepSeekActionLevelPolicy.INTENSIVE_INTERVAL_MILLIS
        } else DeepSeekPrimaryPolicy.INTERVAL
        if (!DeepSeekPrimaryPolicy.shouldRun(
                previous, DeepSeekFreshMarketContext.analysisPrice(snapshot, now) > 0.0,
                force, now, materialChange, adaptiveInterval
            )) return previous
        val key = DeepSeekSecureKeyStore.read(context)
        if (key.isBlank()) return previous.copy(
            lastAttempt = now,
            error = "API-ключ DeepSeek не введён"
        ).also { DeepSeekPrimaryStore.save(context, it) }

        val requestedModel = DeepSeekPrimaryPolicy.chooseModel(
            snapshot, forceProModel, materialChange, previousActionLevel,
            analyticalConflict = analyticalConflict
        )

        DeepSeekPrimaryStore.save(context, previous.copy(
            lastAttempt = now,
            model = requestedModel,
            lastInputReadiness = snapshot.readinessScore,
            lastLocalBuySignal = snapshot.buySignal,
            lastLocalSellSignal = snapshot.sellSignal,
            lastBreathingScore = breathing.normalScore ?: 0,
            lastEcosystemScore = ecosystemScore,
            error = ""
        ))
        val started = System.currentTimeMillis()
        ApiUsageLogStore.record(context, ApiUsageEvent(
            provider = "DEEPSEEK", circuit = "ОСНОВНОЙ РЫНОК",
            model = requestedModel, status = "START", at = started,
            detail = buildString {
                append(when {
                fusionPriority.active -> "Fusion-позиция: локальная защита постоянно, DeepSig по экономичному интервалу"
                force -> "ручная усиленная проверка"
                materialChange -> "существенно изменился рыночный сигнал"
                else -> "плановый анализ"
                })
                append(" • readiness=${snapshot.readinessScore} direction=${snapshot.directionScore}")
                append(" breathing=${breathing.normalScore ?: 0}")
                append(" ecosystem=$ecosystemScore quality=${ecosystem.dataQuality}")
                append(" price=${snapshot.livePrice?.takeIf { it > 0.0 } ?: snapshot.lastPrice}")
            }
        ))
        return runCatching {
            analyze(
                context, key, requestedModel, snapshot, EventRadarStore.state(context), ecosystem,
                memoryPrompt, previous
            )
        }.fold(
            onSuccess = { result ->
                val completedAt = System.currentTimeMillis()
                ApiUsageLogStore.record(context, ApiUsageEvent(
                    provider = "DEEPSEEK", circuit = "ОСНОВНОЙ РЫНОК",
                    model = requestedModel, status = "OK", at = completedAt,
                    durationMillis = completedAt - started,
                    promptTokens = result.promptTokens, outputTokens = result.completionTokens,
                    detail = buildString {
                        append("finish=${result.finishReason} • action=${result.action} • ")
                        if (result.repaired) append("ответ восстановлен коротким повтором • ")
                        append(result.summary)
                        if (result.evidence.isNotEmpty()) append(" • факты: ${result.evidence.joinToString("; ")}")
                        if (result.risks.isNotEmpty()) append(" • риски: ${result.risks.joinToString("; ")}")
                        if (result.verificationSummary != "не требовалась") {
                            append(" • усиленная проверка: ${result.verificationSummary}")
                        }
                    }.take(500)
                ))
                val requestCost = DeepSeekCostPolicy.estimateUsd(
                    requestedModel, result.promptTokens, result.completionTokens
                ) + DeepSeekCostPolicy.estimateUsd(
                    PositionSupervisorPolicy.FLASH_MODEL,
                    result.verificationPromptTokens,
                    result.verificationCompletionTokens
                )
                previous.copy(
                    lastAttempt = now,
                    lastSuccess = completedAt,
                    model = requestedModel,
                    action = result.action,
                    modelIntent = result.modelIntent,
                    proposedAction = result.proposedAction,
                    executionStatus = result.executionStatus,
                    verificationSummary = result.verificationSummary,
                    direction = result.direction,
                    danger = result.danger,
                    confidence = result.confidence,
                    entryReadiness = result.entryReadiness,
                    summary = result.summary,
                    shortScenario = result.shortScenario,
                    longScenario = result.longScenario,
                    invalidation = result.invalidation,
                    uncertainty = result.uncertainty,
                    successfulToday = previous.successfulToday + 1,
                    promptTokensToday = previous.promptTokensToday + result.promptTokens +
                        result.verificationPromptTokens,
                    completionTokensToday = previous.completionTokensToday + result.completionTokens +
                        result.verificationCompletionTokens,
                    estimatedCostUsdToday = previous.estimatedCostUsdToday + requestCost,
                    lastInputReadiness = snapshot.readinessScore,
                    lastLocalBuySignal = snapshot.buySignal,
                    lastLocalSellSignal = snapshot.sellSignal,
                    lastBreathingScore = breathing.normalScore ?: 0,
                    lastEcosystemScore = ecosystemScore,
                    independentEntryConfirmStreak = result.independentEntryConfirmStreak,
                    independentExitConfirmStreak = result.independentExitConfirmStreak,
                    lastPersistenceEvaluationAt = result.lastPersistenceEvaluationAt,
                    evidence = result.evidence,
                    risks = result.risks,
                    error = ""
                ).also { updated ->
                    DeepSeekPrimaryStore.save(context, updated)
                    DeepSeekActionLevelAlertStore.sync(
                        context,
                        DeepSeekActionLevelPolicy.fromMarket(
                            snapshot, updated, MicroImpulseStore.state(context), completedAt,
                            LiveMarketBreathingStore.snapshot(context, completedAt)
                        ),
                        updated
                    )
                    DeepSeekEvidenceMemory.recordPrediction(
                        context = context,
                        observedAt = completedAt,
                        price = DeepSeekFreshMarketContext.analysisPrice(snapshot, completedAt),
                        directionScore = result.direction,
                        baselineDirectionScore = snapshot.directionScore,
                        confidence = result.confidence,
                        action = result.action,
                        featureKey = featureKey,
                        breathingScore = breathing.normalScore,
                        ecosystem = ecosystem,
                        summary = result.summary,
                        memorySupplied = memorySupplied
                    )
                }
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
                val failedCost = DeepSeekCostPolicy.estimateUsd(
                    requestedModel,
                    structured?.promptTokens ?: 0,
                    structured?.completionTokens ?: 0
                )
                previous.copy(
                    lastAttempt = now,
                    model = requestedModel,
                    lastInputReadiness = snapshot.readinessScore,
                    lastLocalBuySignal = snapshot.buySignal,
                    lastLocalSellSignal = snapshot.sellSignal,
                    lastBreathingScore = breathing.normalScore ?: 0,
                    lastEcosystemScore = ecosystemScore,
                    failedToday = previous.failedToday + 1,
                    promptTokensToday = previous.promptTokensToday + (structured?.promptTokens ?: 0),
                    completionTokensToday = previous.completionTokensToday + (structured?.completionTokens ?: 0),
                    estimatedCostUsdToday = previous.estimatedCostUsdToday + failedCost,
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
        radar: EventRadarState,
        ecosystem: PumpEcosystemSnapshot,
        memoryPrompt: JSONObject,
        previousState: DeepSeekPrimaryState
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
        val fusion = BitpandaFusionStore.state(context)
        val fusionSim = FusionSimStore.state(context)
        val fusionPriority = FusionPriorityPolicy.plan(fusionSim)
        val aiPaperPortfolio = GeminiPaperStore.state(context).portfolio
        val aiPaperPositionOpen = aiPaperPortfolio.inPosition
        val managedVirtualPositionOpen = aiPaperPositionOpen || fusionSim.inPosition
        val fusionVenueFresh = fusion.fresh(now)
        val fusionMark = if (fusionVenueFresh) fusion.bid else {
            DeepSeekFreshMarketContext.analysisPrice(snapshot, now)
        }
        val fusionMetrics = FusionPriorityPolicy.metrics(
            fusionSim, fusionMark, fusion.feeRate, fusionVenueFresh
        )
        val fusionEntryTime = fusionSim.trades.lastOrNull { it.action == "BUY" }?.time ?: 0L
        val frame = JSONObject()
            .put("symbol", "PUMP/EUR")
            .put("closed_30m_price_eur", snapshot.lastPrice)
            .put("closed_30m_strategy_role", "независимый количественный контекст; не торговый приказ")
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
            .put("strategy_30m_late_entry_flag_display_only_for_deepseek", snapshot.lateEntryBlocked)
            .put("user_position_open", snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0)
            .put("deepseek_paper_position_open", aiPaperPositionOpen)
            .put("managed_virtual_position_open", managedVirtualPositionOpen)
            .put("fusion_priority_position", JSONObject()
                .put("position_open", fusionSim.inPosition)
                .put("maximum_control_active", fusionPriority.active)
                .put("control_interval_seconds", fusionPriority.intervalMillis / 1000L)
                .put("forced_model", if (fusionPriority.forcePro) PositionSupervisorPolicy.PRO_MODEL else JSONObject.NULL)
                .put("entry_time", fusionEntryTime.takeIf { it > 0L } ?: JSONObject.NULL)
                .put("entry_ask_eur", fusionSim.entryPrice.takeIf { fusionSim.inPosition } ?: JSONObject.NULL)
                .put("entry_cost_eur", fusionSim.entryCostEur.takeIf { fusionSim.inPosition } ?: JSONObject.NULL)
                .put("pump_amount", fusionSim.pumpAmount.takeIf { fusionSim.inPosition } ?: JSONObject.NULL)
                .put("current_bid_fresh", fusionVenueFresh)
                .put("current_bid_eur", fusion.bid.takeIf { fusionVenueFresh } ?: JSONObject.NULL)
                .put("reference_mark_eur", fusionMetrics.markPriceEur.takeIf { it > 0.0 } ?: JSONObject.NULL)
                .put("mark_source", if (fusionVenueFresh) "FUSION_BID" else "BINANCE_REFERENCE_NOT_EXECUTABLE")
                .put("estimated_exit_fee_eur", fusionMetrics.estimatedExitFeeEur.takeIf { fusionSim.inPosition } ?: JSONObject.NULL)
                .put("net_liquidation_value_eur", fusionMetrics.netLiquidationValueEur.takeIf { fusionSim.inPosition } ?: JSONObject.NULL)
                .put("net_pnl_eur", fusionMetrics.netPnlEur.takeIf { fusionSim.inPosition } ?: JSONObject.NULL)
                .put("net_pnl_percent", fusionMetrics.netPnlPercent.takeIf { fusionSim.inPosition } ?: JSONObject.NULL)
                .put("peak_value_eur", fusionSim.peakValueEur.takeIf { fusionSim.inPosition } ?: JSONObject.NULL)
                .put("pullback_from_peak_percent", fusionMetrics.pullbackFromPeakPercent.takeIf { fusionSim.inPosition } ?: JSONObject.NULL)
                .put("separate_from_serge", true)
                .put("real_orders", false))
            .put("bitpanda_fusion_read_only", JSONObject()
                .put("configured", fusion.configured)
                .put("fresh", fusion.fresh(now))
                .put("age_seconds", fusion.lastSuccess.takeIf { it > 0L }?.let { (now - it).coerceAtLeast(0L) / 1000L } ?: JSONObject.NULL)
                .put("pair", fusion.pair)
                .put("best_bid_eur", fusion.bid.takeIf { it > 0.0 } ?: JSONObject.NULL)
                .put("best_ask_eur", fusion.ask.takeIf { it > 0.0 } ?: JSONObject.NULL)
                .put("mid_eur", fusion.mid.takeIf { it > 0.0 } ?: JSONObject.NULL)
                .put("spread_percent", fusion.spreadPercent.takeIf { fusion.connected } ?: JSONObject.NULL)
                .put("bid_depth_eur", fusion.bidDepthEur.takeIf { fusion.connected } ?: JSONObject.NULL)
                .put("ask_depth_eur", fusion.askDepthEur.takeIf { fusion.connected } ?: JSONObject.NULL)
                .put("role", "фактическая площадка исполнения для отдельной paper-симуляции; не приказ и не разрешение сделки"))
            .put("hourly_context_age_seconds", hourly?.let {
                DeepSeekFreshMarketContext.ageSeconds(it.candleTime, now)
            } ?: JSONObject.NULL)
            .put("hourly_pump_change_1h_pct", hourly?.pump1hPercent ?: JSONObject.NULL)
            .put("hourly_pump_change_3h_pct", hourly?.pump3hPercent ?: JSONObject.NULL)
            .put("hourly_pump_change_6h_pct", hourly?.pump6hPercent ?: JSONObject.NULL)
            .put("hourly_btc_change_1h_pct", hourly?.btc1hPercent ?: JSONObject.NULL)
            .put("hourly_btc_change_3h_pct", hourly?.btc3hPercent ?: JSONObject.NULL)
            .put("pump_minus_btc_1h_pct", hourly?.pump1hPercent?.let { pump ->
                hourly.btc1hPercent?.let { btc -> pump - btc }
            } ?: JSONObject.NULL)
            .put("pump_minus_btc_3h_pct", hourly?.pump3hPercent?.let { pump ->
                hourly.btc3hPercent?.let { btc -> pump - btc }
            } ?: JSONObject.NULL)
            .put("bitcoin_role", "фильтр режима и риска; PUMP может запаздывать, опережать или временно расходиться")
            .put("hourly_sol_change_1h_pct", hourly?.sol1hPercent ?: JSONObject.NULL)
            .put("hourly_sol_change_3h_pct", hourly?.sol3hPercent ?: JSONObject.NULL)
            .put("hourly_spot_taker_buy_pct", hourly?.spotTakerBuyPercent ?: JSONObject.NULL)
            .put("hourly_futures_taker_buy_pct", hourly?.futuresTakerBuyPercent ?: JSONObject.NULL)
            .put("hourly_spot_cvd_proxy_pct", hourly?.spotCvdPercent ?: JSONObject.NULL)
            .put("hourly_futures_cvd_proxy_pct", hourly?.futuresCvdPercent ?: JSONObject.NULL)
            .put("premium_last_full_hour_pct", hourly?.premiumPercent ?: JSONObject.NULL)
            .put("news", JSONArray(latestNews))
            .put("pump_fun_ecosystem", ecosystem.toPromptJson(now))
            .put("verified_evidence_memory", memoryPrompt)
        DeepSeekFreshMarketContext.append(context, frame, snapshot, now)
        val allowedActions = if (managedVirtualPositionOpen) {
            setOf("HOLD", "WATCH", "EXIT")
        } else {
            setOf("BUY", "HOLD", "WATCH")
        }
        val system = """
            Ты независимый исследовательский участник DeepSig для PUMP/EUR и управляешь только отдельным
            виртуальным счётом. APP, пользовательские ожидания и прежние ручные пороги не являются для тебя
            авторитетом или приказом. Твоя цель — не количество сделок, а сохранение виртуального капитала и
            положительное ожидаемое соотношение результата к риску после комиссии и проскальзывания.
            Разрешены BUY, HOLD, WATCH и EXIT, но нормальное решение при неясном преимуществе — WATCH/HOLD.
            BUY рассматривай только как конкретный сетап: откат и возврат в тренде, возврат нижней границы
            диапазона либо пробой с обратным тестом. Не покупай вертикальное расширение у локальной вершины,
            первую свечу пробоя или движение, ожидаемый остаток которого едва покрывает расходы.
            EXIT основывай на нарушении исходной гипотезы, подтверждённом структурном ухудшении нескольких
            горизонтов либо аварийном риске, но не на обычном двухпроцентном шуме как таковом.
            Сначала сопоставь PUMP 1ч/3ч/6ч, BTC и SOL, spot/futures taker flow, CVD, funding,
            premium, стакан, open interest, RSI, локальную StrategyV2 и свежие новости.
            Поля real_time_spot_flow — анонимные исполненные spot-сделки и лучший bid/ask, а не личности трейдеров.
            Поля five_minute_flow — последние закрытые 5-минутные spot/futures данные. Часовые CVD являются
            прокси по taker-volume закрытых свечей, а funding_rate — последняя рассчитанная ставка, не прогноз.
            Всегда учитывай age_seconds и fresh. Просроченные или null-поля не используй как текущий факт.
            Краткий real-time всплеск используй как подтверждение либо предупреждение, но не как самостоятельный BUY
            или EXIT. После короткой просадки отдельно проверь возврат покупателей на 5/15 минутах и удержание цены.
            Возможный сбор стопов допускается только как гипотеза при быстром поглощении продаж и возврате потока;
            не выдавай намерения участников за установленный факт.
            live_market_breathing содержит устойчивое направление на 5/15/30/60/360 минутах, непрерывную
            continuous_flow_wave с отдельными 5/15/20/30/60/180/360-минутными слоями и сырой instant_score.
            anonymous_large_order_fingerprint показывает публичные крупные taker-заявки и вероятные серии частей.
            Это вероятностный отпечаток: никогда не утверждай, что известны конкретный владелец, фирма, страна,
            его прибыль или повторный вход того же счёта. Покупки без реакции цены трактуй как возможное поглощение.
            Опирайся прежде всего на normal_deepseek_score,
            плавные 15/30/60/180/360-минутные волны и их согласованность, а не на исчезновение одного тика.
            capital_flow_proxy различает вероятный набор новых лонгов, закрытие шортов, поглощение и сокращение
            плеча по совместному поведению цены, taker-flow и OI. Это механизм, не личность: биржа не раскрывает
            фирму, страну или владельца заявки, поэтому не называй конкретного «умного игрока».
            buyer_breath_cycle внутри него разделяет QUIET/IGNITION/EXPANSION/MATURE/EXHAUSTION/
            SELLER_TAKEOVER/SHOCK. Высокий aggressive buy не гарантирует рост: отрицательная
            price_response_efficiency и высокий absorption_risk означают, что покупки могут поглощаться.
            Фаза помогает оценить возраст и качество импульса, но не создаёт сделку одна. Исторические диапазоны
            не являются целью цены; через 60 минут ранний buy-всплеск сам по себе не имел устойчивого преимущества.
            Локальные APP-поля — лишь ещё один вычисленный контекст: не копируй их решение и не позволяй им
            создавать или запрещать твою сделку. Rapid drop и многоуровневая слабость являются самостоятельным
            риском независимо от мнения APP.
            Внутри незакрытой 30-минутной свечи BUY разрешён при устойчивом 5/15-минутном дыхании, подтверждении
            исполненными покупками/5-минутным потоком и отсутствии реального разворота или rapid drop.
            Не считай один индикатор или один заголовок достаточным основанием. Не догоняй уже перегретую цену.
            Bitcoin — фильтр рыночного режима, а не команда PUMP двигаться синхронно каждую минуту. Различай
            краткое расхождение, запаздывание PUMP и общий разворот. Одиночная минутная слабость BTC не запрещает
            BUY, если PUMP сохраняет устойчивые 5/15-минутные покупки, CVD и относительную силу. Но совместная
            слабость BTC и PUMP на нескольких горизонтах остаётся серьёзным риском.
            pump_fun_ecosystem — внутренний фундаментальный фон Pump.fun: миграции, объём, доход, выкуп и сжигание.
            Учитывай качество и возраст; null не превращай в ноль. Этот слой не имеет самостоятельной торговой власти.
            verified_evidence_memory содержит только замороженные до результата закономерности. Повышай вес только
            записей promoted_patterns; background_patterns используй как слабый фон. Память не отменяет свежесть,
            rapid drop, риск-контроль и обязательную независимую проверку сделки.
            bitpanda_fusion_read_only — независимый read-only стакан площадки будущего исполнения. Используй только
            свежие bid/ask, спред и глубину как проверку исполнимости и риска проскальзывания. Широкий спред или
            сильный ask-перевес снижают качество входа. Сам по себе bid-перевес не создаёт BUY. Просроченный,
            отсутствующий или ошибочный Fusion-кадр не является положительным подтверждением.
            fusion_priority_position — отдельная виртуальная позиция исполнения на Bitpanda Fusion. Когда
            maximum_control_active=true, считай её высшим приоритетом среди виртуальных исследований: контролируй
            исходную гипотезу, свежие продажи, flow/CVD, 5/15/30/60 минут, BTC/SOL, rapid drop, фактический bid/ask,
            спред, комиссию, чистый PnL, достигнутый пик и откат от пика. Снижение на 2% само по себе не EXIT.
            Если открыт только FusionSim, решение EXIT относится только к нему и будет виртуально исполнено по
            свежему bid. Эта позиция не является позицией Сержа, не меняет его PnL и не нажимает его кнопки.
            При просроченном Fusion-стакане продолжай оценивать рыночный риск, но не выдумывай цену исполнения:
            приложение само откажется виртуально закрывать позицию без свежего bid.
            BUY допустим только при подтверждении минимум двумя независимыми группами данных. Самостоятельный
            BUY DeepSig будет исполнен приложением лишь после двух отдельных последовательных AI-оценок.
            Не запрещай BUY
            механически из-за уже растущей 30-минутной свечи: отличай устойчивое продолжение от выдохшегося рывка
            по 5/15-минутному дыханию. Rapid drop без восстановления остаётся запретом.
            EXIT допустим только при открытой позиции и согласованном ухудшении нескольких горизонтов.
            Обычный EXIT требует одновременной устойчивой слабости 15/30/60 минут и свежих продаж и будет
            исполнен лишь после двух отдельных AI-оценок.
            Одна красная свеча, краткий сброс цены, стенка стакана либо слабая минута Bitcoin не являются EXIT.
            Ты управляешь виртуальным счётом DeepSig и приоритетно сопровождаешь связанную FusionSim-позицию.
            managed_virtual_position_open означает, что хотя бы одна из этих позиций открыта. BUY разрешён лишь
            когда обе закрыты; EXIT закрывает открытый DeepSig и/или FusionSim по правилам соответствующего
            виртуального исполнения. Не меняй счёт APP или Сержа.
            Отделяй факты из кадра от предположений. Null и просроченное означают отсутствие доказательства,
            а не нейтральный или положительный факт. Не обещай прибыль и не изображай confidence вероятностью.
            Верни только JSON:
            action BUY, HOLD, WATCH или EXIT; direction целое -100..100; danger целое 0..10;
            confidence целое 0..100; entry_readiness целое 1..10, где 1 означает «не входить»,
            5–6 — наблюдение, 7–8 — близкий жёлтый вход, 9–10 — подтверждённый зелёный вход.
            Если счёт в евро, entry_readiness 9–10, подтверждены минимум две независимые группы и нет rapid drop,
            action обязан быть BUY, а не WATCH. Если подтверждения недостаточно, не завышай entry_readiness;
            summary одно короткое конкретное предложение о происходящем сейчас;
            short_scenario один короткий наиболее вероятный сценарий на 15–60 минут;
            long_scenario один короткий наиболее вероятный сценарий на 3–24 часа;
            invalidation одно конкретное наблюдаемое условие отмены сценария;
            uncertainty одно короткое пояснение главной неопределённости и уверенности;
            evidence массив из 2–4 коротких фактов; risks массив из 1–3 условий, которые опровергнут вывод.
            Все текстовые значения без исключения пиши только на русском языке. Китайские иероглифы запрещены.
            Если managed_virtual_position_open=false, EXIT не используй; если true, BUY не используй.
            Если данных недостаточно, выбери WATCH.
        """.trimIndent()
        val response = DeepSeekStructuredClient(http).request(
            apiKey = apiKey,
            model = model,
            system = system,
            frame = frame,
            reasoningEffort = if (model == PositionSupervisorPolicy.PRO_MODEL) "high" else "low",
            maxTokens = if (model == PositionSupervisorPolicy.PRO_MODEL) 2400 else 1200,
            validate = { json ->
                when {
                    json.optString("action").uppercase(Locale.ROOT) !in allowedActions -> "action отсутствует или недопустим"
                    !json.has("direction") -> "нет direction"
                    !json.has("danger") -> "нет danger"
                    !json.has("confidence") -> "нет confidence"
                    json.optString("summary").isBlank() -> "нет summary"
                    json.optString("short_scenario").isBlank() -> "нет short_scenario"
                    json.optString("long_scenario").isBlank() -> "нет long_scenario"
                    json.optString("invalidation").isBlank() -> "нет invalidation"
                    json.optString("uncertainty").isBlank() -> "нет uncertainty"
                    RussianOutputPolicy.validate(
                        json.optString("summary"),
                        json.optString("short_scenario"),
                        json.optString("long_scenario"),
                        json.optString("invalidation"),
                        json.optString("uncertainty"),
                        json.optJSONArray("evidence")?.toString().orEmpty(),
                        json.optJSONArray("risks")?.toString().orEmpty()
                    ) != null -> RussianOutputPolicy.validate(
                        json.optString("summary"),
                        json.optString("short_scenario"),
                        json.optString("long_scenario"),
                        json.optString("invalidation"),
                        json.optString("uncertainty"),
                        json.optJSONArray("evidence")?.toString().orEmpty(),
                        json.optJSONArray("risks")?.toString().orEmpty()
                    )
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
        val modelAction = json.optString("action", "WATCH").uppercase(Locale.ROOT)
            .takeIf { it in allowedActions } ?: "WATCH"
        val modelEntryReadiness = json.optInt(
            "entry_readiness",
            (json.optInt("direction").coerceAtLeast(0) / 10).coerceAtLeast(1)
        ).coerceIn(1, 10)
        val breathing = LiveMarketBreathingStore.snapshot(context, now)
        fun breathingScore(minutes: Int) = breathing.horizons
            .firstOrNull { it.minutes == minutes }?.score ?: 0
        val micro = MicroImpulseStore.state(context)
        val microFresh = micro.connected && DeepSeekFreshMarketContext.isFresh(
            micro.updatedAt, now, DeepSeekFreshMarketContext.MICRO_MAX_AGE
        )
        val entryFusion = AppLedHybridPolicy.entry(AppLedEntryEvidence(
            aiFresh = true,
            aiAction = modelAction,
            aiDirection = json.optInt("direction"),
            aiConfidence = json.optInt("confidence"),
            aiReadiness = modelEntryReadiness,
            appReadiness = 0,
            appBuySignal = false,
            appSellSignal = false,
            hardVeto = snapshot.rapidDrop.active && !snapshot.rapidDrop.recoveryConfirmed,
            microFresh = microFresh,
            pumpBuyerPercent60s = micro.aggressiveBuyPercent60s,
            pumpChange60sPercent = micro.priceChange60sPercent,
            bitcoinBuyerPercent60s = micro.bitcoinAggressiveBuyPercent60s,
            bitcoinChange60sPercent = micro.bitcoinPriceChange60sPercent,
            breathing5m = breathingScore(5),
            breathing15m = breathingScore(15),
            breathing30m = breathingScore(30),
            breathing60m = breathingScore(60)
        ))
        val currentPrice = DeepSeekFreshMarketContext.analysisPrice(snapshot, now)
        val activeBuyAt = if (fusionSim.inPosition) {
            fusionEntryTime.takeIf { it > 0L } ?: now
        } else aiPaperPortfolio.trades.lastOrNull { it.action == "BUY" }?.time ?: now
        val managedReturn = if (fusionSim.inPosition) {
            fusionMetrics.netPnlPercent
        } else if (aiPaperPortfolio.inPosition && aiPaperPortfolio.entryPrice > 0.0 && currentPrice > 0.0) {
            (currentPrice / aiPaperPortfolio.entryPrice - 1.0) * 100.0
        } else 0.0
        val exitFusion = AppLedHybridPolicy.exit(AppLedExitEvidence(
            modelRequestsExit = modelAction == "EXIT",
            appExitSignal = false,
            rapidDropUnrecovered = snapshot.rapidDrop.active && !snapshot.rapidDrop.recoveryConfirmed,
            currentReturnPercent = managedReturn,
            positionAgeMillis = (now - activeBuyAt).coerceAtLeast(0L),
            microFresh = microFresh,
            pumpBuyerPercent15s = micro.aggressiveBuyPercent15s,
            pumpBuyerPercent60s = micro.aggressiveBuyPercent60s,
            pumpBuyerPercent5m = micro.aggressiveBuyPercent5m,
            pumpChange60sPercent = micro.priceChange60sPercent,
            breathing5m = breathingScore(5),
            breathing15m = breathingScore(15),
            breathing30m = breathingScore(30),
            breathing60m = breathingScore(60)
        ))
        val persistence = DeepSeekPersistencePolicy.update(
            previousEntryStreak = if (managedVirtualPositionOpen) 0 else previousState.independentEntryConfirmStreak,
            previousExitStreak = if (managedVirtualPositionOpen) previousState.independentExitConfirmStreak else 0,
            previousEvaluationAt = previousState.lastPersistenceEvaluationAt,
            independentEntrySetup = !managedVirtualPositionOpen && entryFusion.independentDeepSeekSetup,
            independentExitSetup = managedVirtualPositionOpen && exitFusion.independentDeepSeekSetup,
            now = now
        )
        val proposedAction = if (managedVirtualPositionOpen) {
            if (exitFusion.emergency || persistence.confirmIndependentExit
            ) "EXIT" else "HOLD"
        } else {
            if (persistence.confirmIndependentBuy) "BUY" else "WATCH"
        }
        val entryReadiness = if (managedVirtualPositionOpen) modelEntryReadiness else when {
            persistence.confirmIndependentBuy -> maxOf(9, entryFusion.level)
            entryFusion.independentDeepSeekSetup -> maxOf(8, entryFusion.level)
            else -> entryFusion.level
        }
        val verification = if (proposedAction == "BUY" || proposedAction == "EXIT") {
            verifyTradeDecision(
                context, apiKey, frame, json, proposedAction, managedVirtualPositionOpen,
                forcePro = fusionPriority.active && exitFusion.emergency &&
                    snapshot.rapidDrop.active && !snapshot.rapidDrop.recoveryConfirmed
            )
        } else null
        val action = DeepSeekTradeVerificationPolicy.finalAction(
            proposedAction, verification?.approved, managedVirtualPositionOpen
        )
        val executionStatus = when {
            verification == null -> "СДЕЛКА НЕ ЗАПРАШИВАЛАСЬ"
            verification.approved -> "ОДОБРЕНО К ИСПОЛНЕНИЮ"
            else -> "ОТКЛОНЕНО ПРОВЕРКОЙ"
        }
        val summary = if (verification != null && !verification.approved) {
            "Сделка отклонена усиленной проверкой: ${verification.summary}"
        } else if (!managedVirtualPositionOpen && entryFusion.independentDeepSeekSetup &&
            !persistence.confirmIndependentBuy
        ) {
            "DeepSig самостоятельно подтвердил вход 1/2; ждём следующую отдельную оценку: ${entryFusion.reason}"
        } else if (managedVirtualPositionOpen && exitFusion.independentDeepSeekSetup &&
            !persistence.confirmIndependentExit
        ) {
            "DeepSig самостоятельно подтвердил риск 1/2; позиция пока удерживается: ${exitFusion.reason}"
        } else if (!managedVirtualPositionOpen && proposedAction != modelAction) {
            "DeepSig ещё не получил два независимых подтверждения: ${entryFusion.reason}"
        } else if (managedVirtualPositionOpen && modelAction == "EXIT" && !exitFusion.allowExit) {
            "DeepSig удерживает позицию: ${exitFusion.reason}"
        } else if (fusionPriority.active) {
            "Fusion под локальным контролем; DeepSig проверяет ключевые изменения: ${json.optString("summary", "позиция удерживается")}"
        } else {
            json.optString("summary", "DeepSig не дал пояснение")
        }
        return DeepSeekPrimaryResult(
            action = action,
            modelIntent = modelAction,
            proposedAction = proposedAction,
            executionStatus = executionStatus,
            direction = DeepSeekTradeVerificationPolicy.acceptedDirection(
                proposedAction, verification?.approved, json.optInt("direction")
            ),
            danger = if (managedVirtualPositionOpen && modelAction == "EXIT" && !exitFusion.allowExit) {
                minOf(json.optInt("danger").coerceIn(0, 10), exitFusion.dangerCap)
            } else json.optInt("danger").coerceIn(0, 10),
            confidence = DeepSeekTradeVerificationPolicy.acceptedConfidence(
                proposedAction, verification?.approved, json.optInt("confidence")
            ),
            entryReadiness = entryReadiness,
            summary = summary.take(400),
            shortScenario = json.optString("short_scenario", "Краткосрочно требуется наблюдение").take(300),
            longScenario = json.optString("long_scenario", "Долгосрочно данных пока недостаточно").take(300),
            invalidation = json.optString("invalidation", "Свежие данные опровергнут текущий сценарий").take(300),
            uncertainty = json.optString("uncertainty", "Сохраняется рыночная неопределённость").take(300),
            evidence = json.optJSONArray("evidence")?.let { array ->
                List(array.length().coerceAtMost(4)) { array.optString(it).take(240) }
                    .filter { it.isNotBlank() }
            }.orEmpty(),
            risks = json.optJSONArray("risks")?.let { array ->
                List(array.length().coerceAtMost(3)) { array.optString(it).take(240) }
                    .filter { it.isNotBlank() }
            }.orEmpty(),
            independentEntryConfirmStreak = persistence.entryStreak,
            independentExitConfirmStreak = persistence.exitStreak,
            lastPersistenceEvaluationAt = persistence.lastEvaluationAt,
            promptTokens = response.promptTokens,
            completionTokens = response.completionTokens,
            verificationPromptTokens = verification?.promptTokens ?: 0,
            verificationCompletionTokens = verification?.completionTokens ?: 0,
            verificationSummary = verification?.summary ?: "не требовалась",
            repaired = response.repaired,
            finishReason = response.finishReason
        )
    }

    private data class TradeVerification(
        val approved: Boolean,
        val summary: String,
        val promptTokens: Int,
        val completionTokens: Int
    )

    private fun verifyTradeDecision(
        context: Context,
        apiKey: String,
        frame: JSONObject,
        proposal: JSONObject,
        proposedAction: String,
        positionOpen: Boolean,
        forcePro: Boolean = false
    ): TradeVerification {
        val model = if (forcePro) PositionSupervisorPolicy.PRO_MODEL else PositionSupervisorPolicy.FLASH_MODEL
        val started = System.currentTimeMillis()
        ApiUsageLogStore.record(context, ApiUsageEvent(
            provider = "DEEPSEEK", circuit = "ПРОВЕРКА СДЕЛКИ",
            model = model, status = "START", at = started,
            detail = if (forcePro) {
                "Fusion-позиция: аварийная Pro-проверка $proposedAction перед виртуальным исполнением"
            } else "экономичная независимая Flash-проверка $proposedAction перед исполнением"
        ))
        val verificationFrame = JSONObject(frame.toString())
            .put("proposed_decision", JSONObject(proposal.toString()))
            .put("position_open", positionOpen)
        val system = """
            Ты второй строгий контролёр независимого виртуального участника DeepSig. Перепроверь предложенный
            $proposedAction по тому же свежему рыночному кадру. APP, пользовательские пожелания и ручные пороги
            не являются подтверждением и не имеют права одобрить или запретить эту сделку. Сначала проверь
            live_market_breathing: DeepSig использует
            normal_deepseek_score и согласованность 5/15/30/60/180/360 минут, а не одиночный instant_score.
            Проверь buyer_breath_cycle: IGNITION/EXPANSION годятся только при положительной эффективности цены;
            EXHAUSTION требует подтверждения, SELLER_TAKEOVER усиливает многогрупповой EXIT, SHOCK включает
            аварийный режим. Высокий процент покупок без реакции цены считай возможным поглощением, а не силой.
            Закрытая 30-минутная StrategyV2 — только один количественный контекст. Не отклоняй устойчивый
            внутрисвечный BUY только из-за незакрытой свечи или уже начавшегося роста, но отклоняй погоню за
            вертикальным расширением без возврата/ретеста и без достаточного остатка движения после расходов.
            Ищи настоящий выдох движения, противоречие spot/futures, слабость BTC/SOL, rapid drop и устаревшие данные.
            Не отклоняй BUY только из-за одиночной минутной слабости Bitcoin: PUMP может запаздывать или временно
            расходиться. Считай BTC запретом лишь при устойчивой слабости нескольких горизонтов вместе с потерей
            покупательского потока/относительной силы самого PUMP.
            Одобряй BUY только при конкретном сетапе, согласованном 5/15-минутном направлении и подтверждении
            минимум двумя независимыми группами: исполненный spot/futures поток, CVD, структура цены/ретест,
            стакан либо относительная сила к рынку. Одобряй EXIT при нарушении гипотезы или согласованном
            ухудшении 15/30/60 минут вместе со свежими продажами. Само снижение на 2% не является причиной EXIT.
            Одиночный короткий тик, стенка стакана, гипотеза о сборе стопов или слабая минута Bitcoin — отказ EXIT.
            Null и просроченные поля не являются положительным доказательством. Confidence — качество имеющихся
            свидетельств, а не обещанная вероятность прибыли.
            Если fusion_priority_position.maximum_control_active=true, перепроверь именно цену входа Fusion,
            свежий bid/ask, спред, комиссию, чистый PnL и откат от пика. Два процента снижения сами по себе не
            являются EXIT. При одобренном EXIT приложение закроет только открытые виртуальные позиции; позиция
            Сержа и его кнопки полностью отделены.
            Верни только JSON: approved boolean; summary короткая причина; evidence массив до 3 фактов;
            risks массив до 3 рисков. Все текстовые поля пиши только по-русски, без китайских иероглифов.
        """.trimIndent()
        return runCatching {
            val response = DeepSeekStructuredClient(http).request(
                apiKey = apiKey,
                model = model,
                system = system,
                frame = verificationFrame,
                reasoningEffort = if (forcePro) "high" else "low",
                maxTokens = if (forcePro) 900 else 650,
                validate = { result ->
                    when {
                        !result.has("approved") -> "нет approved"
                        result.optString("summary").isBlank() -> "нет summary"
                        RussianOutputPolicy.validate(
                            result.optString("summary"),
                            result.optJSONArray("evidence")?.toString().orEmpty(),
                            result.optJSONArray("risks")?.toString().orEmpty()
                        ) != null -> RussianOutputPolicy.validate(
                            result.optString("summary"),
                            result.optJSONArray("evidence")?.toString().orEmpty(),
                            result.optJSONArray("risks")?.toString().orEmpty()
                        )
                        else -> null
                    }
                },
                onRepairStart = { reason ->
                    ApiUsageLogStore.record(context, ApiUsageEvent(
                        provider = "DEEPSEEK", circuit = "ВОССТАНОВЛЕНИЕ ПРОВЕРКИ СДЕЛКИ",
                        model = model, status = "RETRY", at = System.currentTimeMillis(),
                        detail = reason.take(260)
                    ))
                }
            )
            val result = TradeVerification(
                approved = response.json.optBoolean("approved"),
                summary = response.json.optString("summary", "решение не подтверждено").take(300),
                promptTokens = response.promptTokens,
                completionTokens = response.completionTokens
            )
            val completed = System.currentTimeMillis()
            ApiUsageLogStore.record(context, ApiUsageEvent(
                provider = "DEEPSEEK", circuit = "ПРОВЕРКА СДЕЛКИ",
                model = model, status = "OK", at = completed,
                durationMillis = completed - started,
                promptTokens = result.promptTokens,
                outputTokens = result.completionTokens,
                detail = "${if (result.approved) "ОДОБРЕНО" else "ОТКЛОНЕНО"}: ${result.summary}"
            ))
            result
        }.getOrElse { error ->
            val structured = error as? DeepSeekStructuredException
            val failed = System.currentTimeMillis()
            ApiUsageLogStore.record(context, ApiUsageEvent(
                provider = "DEEPSEEK", circuit = "ПРОВЕРКА СДЕЛКИ",
                model = model, status = "ERROR", at = failed,
                durationMillis = failed - started,
                promptTokens = structured?.promptTokens ?: 0,
                outputTokens = structured?.completionTokens ?: 0,
                detail = "сделка безопасно отклонена: ${error.message.orEmpty().take(240)}"
            ))
            TradeVerification(
                approved = false,
                summary = "усиленная проверка не завершилась; сделка безопасно отклонена",
                promptTokens = structured?.promptTokens ?: 0,
                completionTokens = structured?.completionTokens ?: 0
            )
        }
    }
}