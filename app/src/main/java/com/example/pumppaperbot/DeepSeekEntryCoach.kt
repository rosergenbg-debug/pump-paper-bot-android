package com.example.pumppaperbot

import android.content.Context
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

/**
 * Eight deliberately narrow, persisted V5.34 entry regulators.
 *
 * They can shape only soft candidate/confirmation behaviour. They cannot disable stale-data,
 * spread, seller-takeover, late-phase or execution-price safety vetoes. Automatic adjustment is
 * limited to one small step per UTC day and requires enough completed paper trades.
 */
data class DeepSeekEntryTuning(
    val pm2ScoreOffset: Int = 0,
    val pm3ScoreOffset: Int = 0,
    val retestScoreOffset: Int = 0,
    val safeScoreOffset: Int = 0,
    val decelerationGap: Int = 12,
    val chaseTighteningBps: Int = 0,
    val confirmationExtraSeconds: Int = 0,
    val absorptionTightening: Int = 0,
    val revision: Int = 0,
    val lastAdjustedAt: Long = 0L,
    val lastAdjustment: String = ""
) {
    fun scoreOffset(mode: PumpProfitModeV526): Int = when (mode) {
        PumpProfitModeV526.PUMP_2 -> pm2ScoreOffset
        PumpProfitModeV526.PUMP_3 -> pm3ScoreOffset
        PumpProfitModeV526.PUMP_RETEST -> retestScoreOffset
        PumpProfitModeV526.PUMP_SAFE -> safeScoreOffset
    }

    fun toJson() = JSONObject()
        .put("pm2ScoreOffset", pm2ScoreOffset)
        .put("pm3ScoreOffset", pm3ScoreOffset)
        .put("retestScoreOffset", retestScoreOffset)
        .put("safeScoreOffset", safeScoreOffset)
        .put("decelerationGap", decelerationGap)
        .put("chaseTighteningBps", chaseTighteningBps)
        .put("confirmationExtraSeconds", confirmationExtraSeconds)
        .put("absorptionTightening", absorptionTightening)
        .put("revision", revision)
        .put("lastAdjustedAt", lastAdjustedAt)
        .put("lastAdjustment", lastAdjustment)

    fun compact(): String =
        "PM2=$pm2ScoreOffset PM3=$pm3ScoreOffset RETEST=$retestScoreOffset SAFE=$safeScoreOffset; " +
            "торможение=$decelerationGap; погоня=−$chaseTighteningBps б.п.; " +
            "подтверждение=+${confirmationExtraSeconds}с; поглощение=−$absorptionTightening"

    companion object {
        fun fromJson(json: JSONObject) = DeepSeekEntryTuning(
            pm2ScoreOffset = json.optInt("pm2ScoreOffset").coerceIn(-4, 6),
            pm3ScoreOffset = json.optInt("pm3ScoreOffset").coerceIn(-4, 6),
            retestScoreOffset = json.optInt("retestScoreOffset").coerceIn(-4, 6),
            safeScoreOffset = json.optInt("safeScoreOffset").coerceIn(-4, 6),
            decelerationGap = json.optInt("decelerationGap", 12).coerceIn(8, 20),
            chaseTighteningBps = json.optInt("chaseTighteningBps").coerceIn(0, 30),
            confirmationExtraSeconds = json.optInt("confirmationExtraSeconds").coerceIn(0, 20),
            absorptionTightening = json.optInt("absorptionTightening").coerceIn(0, 12),
            revision = json.optInt("revision").coerceAtLeast(0),
            lastAdjustedAt = json.optLong("lastAdjustedAt"),
            lastAdjustment = RussianOutputPolicy.visible(json.optString("lastAdjustment")).take(500)
        )
    }
}

data class DeepSeekEntryCoachState(
    val status: String = "IDLE",
    val verdict: String = "NONE",
    val stage: String = "UNKNOWN",
    val confidence: Int = 0,
    val reason: String = "DeepSeek ещё не проверял локальный кандидат.",
    val requestedAt: Long = 0L,
    val completedAt: Long = 0L,
    val expiresAt: Long = 0L,
    val referencePrice: Double = 0.0,
    val referenceInstant: Int = 0,
    val referencePhase: String = "",
    val proposalKey: String = "none",
    val proposalDelta: Int = 0,
    val proposalApplied: Boolean = false,
    val retryAfterAt: Long = 0L,
    val error: String = ""
) {
    fun toJson() = JSONObject()
        .put("status", status).put("verdict", verdict).put("stage", stage)
        .put("confidence", confidence).put("reason", reason)
        .put("requestedAt", requestedAt).put("completedAt", completedAt).put("expiresAt", expiresAt)
        .put("referencePrice", referencePrice).put("referenceInstant", referenceInstant)
        .put("referencePhase", referencePhase).put("proposalKey", proposalKey)
        .put("proposalDelta", proposalDelta).put("proposalApplied", proposalApplied)
        .put("retryAfterAt", retryAfterAt).put("error", error)

    companion object {
        fun fromJson(json: JSONObject) = DeepSeekEntryCoachState(
            status = json.optString("status", "IDLE"),
            verdict = json.optString("verdict", "NONE"),
            stage = json.optString("stage", "UNKNOWN"),
            confidence = json.optInt("confidence").coerceIn(0, 100),
            reason = RussianOutputPolicy.visible(json.optString("reason")).take(500),
            requestedAt = json.optLong("requestedAt"), completedAt = json.optLong("completedAt"),
            expiresAt = json.optLong("expiresAt"), referencePrice = json.optDouble("referencePrice"),
            referenceInstant = json.optInt("referenceInstant"), referencePhase = json.optString("referencePhase"),
            proposalKey = json.optString("proposalKey", "none"),
            proposalDelta = json.optInt("proposalDelta").coerceIn(-1, 1),
            proposalApplied = json.optBoolean("proposalApplied"),
            retryAfterAt = json.optLong("retryAfterAt").coerceAtLeast(0L),
            error = RussianOutputPolicy.visible(json.optString("error")).take(300)
        )
    }
}

object DeepSeekEntryCoachStore {
    private const val PREFS = "deepseek_entry_coach_v534"
    private const val STATE = "state"
    private const val TUNING = "tuning"
    private const val REQUEST_DAY = "request_day"
    private const val REQUEST_COUNT = "request_count"
    private const val LAST_REQUEST = "last_request"

    fun state(context: Context): DeepSeekEntryCoachState = runCatching {
        DeepSeekEntryCoachState.fromJson(JSONObject(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(STATE, null).orEmpty()))
    }.getOrDefault(DeepSeekEntryCoachState())

    fun tuning(context: Context): DeepSeekEntryTuning = runCatching {
        DeepSeekEntryTuning.fromJson(JSONObject(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(TUNING, null).orEmpty()))
    }.getOrDefault(DeepSeekEntryTuning())

    fun saveState(context: Context, value: DeepSeekEntryCoachState) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(STATE, value.toJson().toString()).apply()
    }

    fun saveTuning(context: Context, value: DeepSeekEntryTuning) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(TUNING, value.toJson().toString()).apply()
    }

    @Synchronized
    fun tryConsumeRequest(context: Context, now: Long, maxPerDay: Int, minInterval: Long): Boolean {
        val day = now / (24L * 60L * 60L * 1000L)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val storedDay = prefs.getLong(REQUEST_DAY, -1L)
        val count = if (storedDay == day) prefs.getInt(REQUEST_COUNT, 0) else 0
        val last = prefs.getLong(LAST_REQUEST, 0L)
        if (count >= maxPerDay || (last > 0L && now - last < minInterval)) return false
        return prefs.edit().putLong(REQUEST_DAY, day).putInt(REQUEST_COUNT, count + 1)
            .putLong(LAST_REQUEST, now).commit()
    }

    fun exportJson(context: Context) = JSONObject()
        .put("state", state(context).toJson())
        .put("tuning", tuning(context).toJson())
        .put("coachBudget", JSONObject()
            .put("maxRequestsPerUtcDay", DeepSeekEntryCoach.MAX_REQUESTS_PER_UTC_DAY)
            .put("minimumIntervalMinutes", DeepSeekEntryCoach.MIN_REQUEST_INTERVAL / 60_000L)
            .put("compatibleVerdictMinutes", DeepSeekEntryCoachPolicy.VERDICT_TTL / 60_000L)
            .put("automaticRepairRequest", false))
        .put("automaticAuthority", "ONE_BOUNDED_SOFT_STEP_PER_UTC_DAY_WITH_TRIAL_AND_ROLLBACK")
        .put("adaptiveTrial", DeepSeekTuningTrialStore.state(context).toJson())
        .put("canOverrideHardVeto", false)
}

object DeepSeekEntryTuningPolicy {
    const val MIN_COMPLETED_TRADES = 8
    const val ADJUSTMENT_INTERVAL = 24L * 60L * 60L * 1000L
    val keys = setOf(
        "pm2_score_offset", "pm3_score_offset", "retest_score_offset", "safe_score_offset",
        "deceleration_gap", "chase_tightening", "confirmation_extra", "absorption_tightening"
    )

    data class Outcome(val tuning: DeepSeekEntryTuning, val applied: Boolean, val reason: String)

    fun apply(
        current: DeepSeekEntryTuning,
        key: String,
        delta: Int,
        confidence: Int,
        completedTrades: Int,
        now: Long,
        reason: String
    ): Outcome {
        if (key !in keys || delta == 0) return Outcome(current, false, "модель не предложила допустимое изменение")
        if (confidence < 85) return Outcome(current, false, "уверенность юстировки ниже 85/100")
        if (completedTrades < MIN_COMPLETED_TRADES) return Outcome(current, false, "нужно минимум $MIN_COMPLETED_TRADES закрытых paper-сделок")
        if (current.lastAdjustedAt > 0L && now - current.lastAdjustedAt < ADJUSTMENT_INTERVAL) {
            return Outcome(current, false, "разрешена только одна маленькая юстировка за 24 часа")
        }
        val step = delta.coerceIn(-1, 1)
        val changed = when (key) {
            "pm2_score_offset" -> current.copy(pm2ScoreOffset = (current.pm2ScoreOffset + step).coerceIn(-4, 6))
            "pm3_score_offset" -> current.copy(pm3ScoreOffset = (current.pm3ScoreOffset + step).coerceIn(-4, 6))
            "retest_score_offset" -> current.copy(retestScoreOffset = (current.retestScoreOffset + step).coerceIn(-4, 6))
            "safe_score_offset" -> current.copy(safeScoreOffset = (current.safeScoreOffset + step).coerceIn(-4, 6))
            "deceleration_gap" -> current.copy(decelerationGap = (current.decelerationGap + step).coerceIn(8, 20))
            // These three controls may be tightened or restored toward zero, never loosened beyond baseline.
            "chase_tightening" -> current.copy(chaseTighteningBps = (current.chaseTighteningBps + step * 2).coerceIn(0, 30))
            "confirmation_extra" -> current.copy(confirmationExtraSeconds = (current.confirmationExtraSeconds + step * 2).coerceIn(0, 20))
            "absorption_tightening" -> current.copy(absorptionTightening = (current.absorptionTightening + step).coerceIn(0, 12))
            else -> current
        }
        if (changed == current) return Outcome(current, false, "регулятор уже находится на безопасной границе")
        val summary = "$key ${if (step > 0) "+" else ""}$step: ${reason.take(300)}"
        return Outcome(changed.copy(
            revision = current.revision + 1,
            lastAdjustedAt = now,
            lastAdjustment = summary
        ), true, summary)
    }
}

data class DeepSeekEntryCoachGate(val allowed: Boolean, val reason: String)

object DeepSeekEntryCoachPolicy {
    const val VERDICT_TTL = 10L * 60L * 1000L
    const val PENDING_TTL = 90_000L
    const val ORDINARY_ERROR_BACKOFF = 30L * 60L * 1000L
    const val BALANCE_ERROR_BACKOFF = 6L * 60L * 60L * 1000L

    fun isBalanceError(message: String): Boolean {
        val value = message.lowercase()
        return "insufficient balance" in value || "недостаточно средств" in value ||
            "balance is insufficient" in value
    }

    fun errorBackoff(message: String): Long =
        if (isBalanceError(message)) BALANCE_ERROR_BACKOFF else ORDINARY_ERROR_BACKOFF

    fun strictFallback(
        mode: PumpProfitModeV526,
        observation: SharedFusionEntryObservation,
        localScore: Int,
        localThreshold: Int
    ): Boolean {
        val frame = observation.frame ?: return false
        val breath = observation.breathing?.buyerBreath ?: return false
        val extra = if (mode == PumpProfitModeV526.PUMP_SAFE) 8 else 10
        return breath.phase in setOf(BuyerBreathPhase.IGNITION, BuyerBreathPhase.EXPANSION) &&
            frame.instant + observation.entryTuning.decelerationGap >= frame.score5m &&
            breath.absorptionRisk <= 55 &&
            (breath.moveSincePhaseStartPercent ?: 0.0) <= 0.55 &&
            localScore >= localThreshold + extra
    }

    fun compatible(state: DeepSeekEntryCoachState, observation: SharedFusionEntryObservation, now: Long): Boolean {
        val frame = observation.frame ?: return false
        val breath = observation.breathing?.buyerBreath ?: return false
        if (state.completedAt <= 0L || state.expiresAt < now) return false
        if (state.referencePhase != breath.phase.name) return false
        if (abs(frame.instant - state.referenceInstant) > 18) return false
        if (state.referencePrice > 0.0 && observation.executionAsk > 0.0 &&
            abs(observation.executionAsk / state.referencePrice - 1.0) * 100.0 > 0.35) return false
        return true
    }
}

object DeepSeekEntryCoach {
    private const val CIRCUIT = "ПРЕДВХОДНЫЙ КОНТРОЛЬ"
    const val MAX_REQUESTS_PER_UTC_DAY = 6
    const val MIN_REQUEST_INTERVAL = 15L * 60L * 1000L
    private val running = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor()
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS).build()

    fun review(
        context: Context,
        mode: PumpProfitModeV526,
        observation: SharedFusionEntryObservation,
        localScore: Int,
        localThreshold: Int,
        now: Long
    ): DeepSeekEntryCoachGate {
        DeepSeekAdaptiveTuningGuard.reconcile(context, recentClosedOutcomes(context), now)?.let { reason ->
            UnifiedResearchLog.record(context, "DEEPSEEK_ENTRY_TUNING", "TRIAL", reason, now)
        }
        val strict = DeepSeekEntryCoachPolicy.strictFallback(mode, observation, localScore, localThreshold)
        val key = DeepSeekSecureKeyStore.read(context)
        if (key.isBlank()) return DeepSeekEntryCoachGate(
            strict,
            if (strict) "DeepSeek недоступен: разрешён только усиленный ранний локальный вход"
            else "AI WAIT: ключ DeepSeek не введён и усиленный локальный резерв не подтверждён"
        )
        val state = DeepSeekEntryCoachStore.state(context)
        if (DeepSeekEntryCoachPolicy.compatible(state, observation, now)) {
            return when (state.verdict) {
                "APPROVE" -> {
                    val reliable = state.confidence >= 60 && state.stage in setOf("STARTING", "CONTINUING")
                    DeepSeekEntryCoachGate(
                        reliable || strict,
                        if (reliable) "DeepSeek APPROVE ${state.confidence}/100: ${state.reason}"
                        else if (strict) "DeepSeek неуверен, но усиленный ранний локальный вход подтверждён"
                        else "AI WAIT: APPROVE недостаточно надёжен (${state.confidence}/100, ${state.stage})"
                    )
                }
                "REJECT" -> {
                    val decisive = state.confidence >= 70 || state.stage == "LATE"
                    DeepSeekEntryCoachGate(
                        !decisive && strict,
                        if (decisive) "DeepSeek REJECT ${state.confidence}/100: ${state.reason}"
                        else if (strict) "Слабый AI REJECT не отменяет усиленный ранний локальный вход"
                        else "DeepSeek REJECT ${state.confidence}/100: ${state.reason}"
                    )
                }
                else -> DeepSeekEntryCoachGate(
                    strict,
                    if (strict) "DeepSeek WAIT, но усиленный ранний локальный вход подтверждён"
                    else "DeepSeek WAIT ${state.confidence}/100: ${state.reason}"
                )
            }
        }
        if (state.status == "PENDING" && now - state.requestedAt < DeepSeekEntryCoachPolicy.PENDING_TTL) {
            return DeepSeekEntryCoachGate(false, "AI WAIT: DeepSeek сейчас проверяет последние 5 минут")
        }
        if (state.retryAfterAt > now) {
            val minutes = ((state.retryAfterAt - now + 59_999L) / 60_000L).coerceAtLeast(1L)
            return DeepSeekEntryCoachGate(
                strict,
                if (strict) "DeepSeek на паузе после ошибки ещё ~$minutes мин.; разрешён усиленный локальный резерв"
                else "AI WAIT: после ошибки DeepSeek не повторяет платные запросы ещё ~$minutes мин."
            )
        }
        val acquired = running.compareAndSet(false, true)
        if (acquired && DeepSeekEntryCoachStore.tryConsumeRequest(
                context, now, MAX_REQUESTS_PER_UTC_DAY, MIN_REQUEST_INTERVAL
            )) {
            val pending = state.copy(
                status = "PENDING", verdict = "NONE", confidence = 0,
                reason = "Проверяем, начинается ли движение или локальный импульс уже выдохся.",
                requestedAt = now, expiresAt = 0L, retryAfterAt = 0L, error = ""
            )
            DeepSeekEntryCoachStore.saveState(context, pending)
            executor.execute {
                try {
                    request(context.applicationContext, key, mode, observation, localScore, localThreshold, now)
                } finally {
                    running.set(false)
                }
            }
            return DeepSeekEntryCoachGate(false, "AI WAIT: запущена одна короткая проверка последних 5 минут")
        }
        if (acquired) running.set(false)
        return DeepSeekEntryCoachGate(
            strict,
            if (strict) "Лимит/пауза DeepSeek: разрешён усиленный ранний локальный резерв"
            else "AI WAIT: свежего решения нет; DeepSeek экономит запросы, локальный резерв недостаточно сильный"
        )
    }

    private fun request(
        context: Context,
        apiKey: String,
        mode: PumpProfitModeV526,
        observation: SharedFusionEntryObservation,
        localScore: Int,
        localThreshold: Int,
        requestedAt: Long
    ) {
        val model = PositionSupervisorPolicy.FLASH_MODEL
        val frame = requestFrame(context, mode, observation, localScore, localThreshold)
        ApiUsageLogStore.record(context, ApiUsageEvent(
            provider = "DEEPSEEK", circuit = CIRCUIT, model = model, status = "START", at = requestedAt,
            detail = "локальный кандидат $localScore/$localThreshold; анализ последних 5 минут"
        ))
        UnifiedResearchLog.record(context, "DEEPSEEK_ENTRY_COACH", "START", "кандидат $localScore/$localThreshold", requestedAt)
        val started = System.currentTimeMillis()
        runCatching {
            DeepSeekStructuredClient(http).request(
                apiKey = apiKey,
                model = model,
                system = systemPrompt(),
                frame = frame,
                reasoningEffort = "low",
                maxTokens = 360,
                validate = ::validate,
                onRepairStart = { detail ->
                    ApiUsageLogStore.record(context, ApiUsageEvent(
                        provider = "DEEPSEEK", circuit = CIRCUIT, model = model,
                        status = "RETRY", at = System.currentTimeMillis(), detail = detail.take(240)
                    ))
                },
                allowRepair = false
            )
        }.fold(onSuccess = { result ->
            val now = System.currentTimeMillis()
            val json = result.json
            val verdict = json.optString("verdict").uppercase()
            val confidence = json.optInt("confidence").coerceIn(0, 100)
            val reason = RussianOutputPolicy.visible(json.optString("reason_ru")).take(500)
            val stage = json.optString("stage").uppercase()
            val proposalKey = json.optString("adjust_key", "none")
            val proposalDelta = json.optInt("adjust_delta").coerceIn(-1, 1)
            val outcomes = recentClosedOutcomes(context)
            val currentTuning = DeepSeekEntryCoachStore.tuning(context)
            val tuningResult = if (DeepSeekTuningTrialStore.state(context).active) {
                DeepSeekEntryTuningPolicy.Outcome(
                    currentTuning, false,
                    "предыдущая юстировка ещё проверяется; второе изменение запрещено"
                )
            } else {
                DeepSeekEntryTuningPolicy.apply(
                    currentTuning, proposalKey, proposalDelta, confidence,
                    outcomes.length(), now, RussianOutputPolicy.visible(json.optString("adjust_reason_ru"))
                )
            }
            if (tuningResult.applied) {
                DeepSeekEntryCoachStore.saveTuning(context, tuningResult.tuning)
                DeepSeekAdaptiveTuningGuard.startTrial(
                    context, currentTuning, proposalKey, outcomes, now
                )
                UnifiedResearchLog.record(context, "DEEPSEEK_ENTRY_TUNING", "ADJUST_TRIAL", tuningResult.reason, now)
            } else if (proposalKey != "none") {
                UnifiedResearchLog.record(context, "DEEPSEEK_ENTRY_TUNING", "PROPOSAL", tuningResult.reason, now)
            }
            val micro = observation.micro ?: MicroImpulseSnapshot()
            val phase = observation.breathing?.buyerBreath?.phase?.name.orEmpty()
            val state = DeepSeekEntryCoachState(
                status = "READY", verdict = verdict, stage = stage, confidence = confidence, reason = reason,
                requestedAt = requestedAt, completedAt = now, expiresAt = now + DeepSeekEntryCoachPolicy.VERDICT_TTL,
                referencePrice = observation.executionAsk, referenceInstant = observation.frame?.instant ?: micro.score,
                referencePhase = phase, proposalKey = proposalKey, proposalDelta = proposalDelta,
                proposalApplied = tuningResult.applied, retryAfterAt = 0L
            )
            DeepSeekEntryCoachStore.saveState(context, state)
            ApiUsageLogStore.record(context, ApiUsageEvent(
                provider = "DEEPSEEK", circuit = CIRCUIT, model = model, status = "OK", at = now,
                durationMillis = now - started, promptTokens = result.promptTokens,
                outputTokens = result.completionTokens,
                detail = "$verdict $confidence/100 • $stage • $reason".take(500)
            ))
            UnifiedResearchLog.record(
                context, "DEEPSEEK_ENTRY_COACH", verdict,
                "$confidence/100 • $stage • $reason • tuningApplied=${tuningResult.applied}", now
            )
        }, onFailure = { error ->
            val now = System.currentTimeMillis()
            val structured = error as? DeepSeekStructuredException
            val detail = error.message.orEmpty().ifBlank { error.javaClass.simpleName }.take(300)
            val balanceError = DeepSeekEntryCoachPolicy.isBalanceError(detail)
            val retryAfterAt = now + DeepSeekEntryCoachPolicy.errorBackoff(detail)
            DeepSeekEntryCoachStore.saveState(context, DeepSeekEntryCoachStore.state(context).copy(
                status = if (balanceError) "PAUSED_BALANCE" else "ERROR",
                verdict = "NONE", completedAt = now, expiresAt = 0L,
                retryAfterAt = retryAfterAt, error = detail,
                reason = if (balanceError) {
                    "DeepSeek остановлен на 6 часов: API сообщил о недостатке средств. Локальный резерв продолжает работать."
                } else {
                    "DeepSeek-проверка не завершилась; повтор отложен на 30 минут, действует усиленный локальный резерв."
                }
            ))
            ApiUsageLogStore.record(context, ApiUsageEvent(
                provider = "DEEPSEEK", circuit = CIRCUIT, model = model, status = "ERROR", at = now,
                durationMillis = now - started, promptTokens = structured?.promptTokens ?: 0,
                outputTokens = structured?.completionTokens ?: 0, detail = detail
            ))
            UnifiedResearchLog.record(context, "DEEPSEEK_ENTRY_COACH", "ERROR", detail, now)
        })
    }

    private fun requestFrame(
        context: Context,
        mode: PumpProfitModeV526,
        observation: SharedFusionEntryObservation,
        localScore: Int,
        localThreshold: Int
    ): JSONObject {
        val frame = observation.frame ?: FusionFlowFrame(0, 0, 0, 0, 0)
        val micro = observation.micro ?: MicroImpulseSnapshot()
        val breath = observation.breathing?.buyerBreath ?: BuyerBreathSnapshot()
        val tuning = DeepSeekEntryCoachStore.tuning(context)
        return JSONObject()
            .put("task", "PRE_ENTRY_LAST_5_MINUTES")
            .put("paper_only", true)
            .put("candidate_profile", mode.name)
            .put("local_candidate", JSONObject().put("score", localScore).put("threshold", localThreshold))
            .put("flow", JSONObject()
                .put("instant", frame.instant).put("m5", frame.score5m).put("m15", frame.score15m)
                .put("m20", frame.score20m).put("m30", frame.score30m))
            .put("executed_money", JSONObject()
                .put("buy_60s", micro.buyNotional60s).put("sell_60s", micro.sellNotional60s)
                .put("buy_5m", micro.buyNotional5m).put("sell_5m", micro.sellNotional5m)
                .put("buy_15m", micro.buyNotional15m).put("sell_15m", micro.sellNotional15m)
                .put("buy_pct_5s", micro.aggressiveBuyPercent5s)
                .put("buy_pct_15s", micro.aggressiveBuyPercent15s)
                .put("buy_pct_60s", micro.aggressiveBuyPercent60s)
                .put("buy_pct_5m", micro.aggressiveBuyPercent5m)
                .put("trade_acceleration", micro.tradeAcceleration)
                .put("price_change_60s_pct", micro.priceChange60sPercent)
                .put("trades_60s", micro.trades60s))
            .put("breath", JSONObject()
                .put("phase", breath.phase.name).put("phase_age_min", breath.ageMinutes)
                .put("pressure", breath.pressureScore ?: JSONObject.NULL)
                .put("efficiency", breath.efficiencyScore ?: JSONObject.NULL)
                .put("absorption", breath.absorptionRisk)
                .put("activity_ratio", breath.activityRatio ?: JSONObject.NULL)
                .put("move_since_phase_start_pct", breath.moveSincePhaseStartPercent ?: JSONObject.NULL)
                .put("price_change_5m_pct", breath.priceChange5mPercent ?: JSONObject.NULL))
            .put("execution", JSONObject()
                .put("ask_eur", observation.executionAsk)
                .put("spread_pct", observation.bookSpreadPercent ?: micro.spreadPercent ?: JSONObject.NULL)
                .put("bid_notional", observation.bookBidNotional ?: JSONObject.NULL)
                .put("ask_notional", observation.bookAskNotional ?: JSONObject.NULL))
            .put("bitcoin", JSONObject()
                .put("buy_pct_15s", micro.bitcoinAggressiveBuyPercent15s)
                .put("buy_pct_60s", micro.bitcoinAggressiveBuyPercent60s)
                .put("price_change_60s_pct", micro.bitcoinPriceChange60sPercent))
            .put("current_tuning", tuning.toJson())
            .put("recent_closed_paper_trades", recentClosedOutcomes(context))
    }

    private fun recentClosedOutcomes(context: Context): JSONArray {
        data class Row(val agent: String, val trade: FusionSimTrade, val entryReason: String)
        val rows = listOf(
            "PM3" to PumpMachineStore.state(context).trades,
            "PM2" to PumpMachine2Store.state(context).trades,
            "RETEST" to PumpMachineRetestStore.state(context).trades,
            "SAFE" to PumpMachineSafeStore.state(context).trades
        ).flatMap { (agent, trades) ->
            var entryReason = ""
            trades.sortedBy { it.time }.mapNotNull { trade ->
                when (trade.action) {
                    "BUY" -> {
                        entryReason = trade.reason
                        null
                    }
                    "SELL" -> Row(agent, trade, entryReason).also { entryReason = "" }
                    else -> null
                }
            }
        }
            .sortedBy { it.trade.time }.takeLast(24)
        return JSONArray(rows.map { row ->
            JSONObject().put("agent", row.agent).put("time", row.trade.time)
                .put("pnl_eur", row.trade.pnlEur)
                .put("entry_reason", row.entryReason.take(140))
                .put("exit_reason", row.trade.reason.take(120))
        })
    }

    private fun validate(json: JSONObject): String? {
        val verdict = json.optString("verdict").uppercase()
        if (verdict !in setOf("APPROVE", "WAIT", "REJECT")) return "verdict должен быть APPROVE, WAIT или REJECT"
        if (!json.has("confidence") || json.optInt("confidence") !in 0..100) return "нет confidence 0..100"
        val stage = json.optString("stage").uppercase()
        if (stage !in setOf("STARTING", "CONTINUING", "LATE", "NOISE")) return "неверный stage"
        val reason = json.optString("reason_ru")
        if (reason.isBlank()) return "нет короткой причины на русском"
        RussianOutputPolicy.validate(reason, json.optString("adjust_reason_ru"))?.let { return it }
        val key = json.optString("adjust_key", "none")
        if (key != "none" && key !in DeepSeekEntryTuningPolicy.keys) return "неизвестный adjust_key"
        if (json.optInt("adjust_delta") !in -1..1) return "adjust_delta должен быть -1, 0 или 1"
        return null
    }

    private fun systemPrompt() = """
        Ты короткий предвходный контролёр paper-only системы PUMP/EUR. Оцени не то, были ли покупки
        сильными, а осталось ли вероятное продолжение после комиссии и спреда. Особое внимание:
        ускорение мгновенного и 5-минутного потока, ослабление продаж, реакция цены, поглощение,
        возраст фазы и расстояние уже пройденного движения. Положительные медленные окна при падающем
        мгновенном потоке — признак запоздания. APPROVE давай только для STARTING/CONTINUING, REJECT —
        для LATE/явного выдоха, WAIT — для шума или противоречий. Не пытайся обходить локальные veto.
        Верни только короткий JSON на русском без Markdown и без скрытых рассуждений:
        {"verdict":"APPROVE|WAIT|REJECT","confidence":0,"stage":"STARTING|CONTINUING|LATE|NOISE",
        "reason_ru":"одно проверяемое объяснение","adjust_key":"none или один разрешённый ключ",
        "adjust_delta":-1,"adjust_reason_ru":"кратко"}.
        Юстировку предлагай только если recent_closed_paper_trades содержит минимум 8 сделок и видна
        повторяющаяся ошибка; иначе adjust_key=none и adjust_delta=0. Не обещай прибыль.
    """.trimIndent()
}
