package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max

data class PumpVariantSyncResult(
    val portfolio: FusionSimPortfolio,
    val status: String,
    val tradeNetPercent: Double
)

private enum class PumpVariantKindV610 { RETEST, SAFE }

private data class RetestStateV610(
    val armedAt: Long = 0L,
    val anchorAsk: Double = 0.0,
    val lowAsk: Double = 0.0,
    val pulledBack: Boolean = false
) {
    val armed: Boolean get() = armedAt > 0L && anchorAsk > 0.0
    fun toJson() = JSONObject()
        .put("armedAt", armedAt)
        .put("anchorAsk", anchorAsk)
        .put("lowAsk", lowAsk)
        .put("pulledBack", pulledBack)

    companion object {
        fun fromJson(j: JSONObject) = RetestStateV610(
            armedAt = j.optLong("armedAt"),
            anchorAsk = j.optDouble("anchorAsk"),
            lowAsk = j.optDouble("lowAsk"),
            pulledBack = j.optBoolean("pulledBack")
        )
    }
}

private data class SafeStateV610(
    val candidateAt: Long = 0L,
    val anchorAsk: Double = 0.0,
    val confirmations: Int = 0
) {
    val armed: Boolean get() = candidateAt > 0L && anchorAsk > 0.0
    fun toJson() = JSONObject()
        .put("candidateAt", candidateAt)
        .put("anchorAsk", anchorAsk)
        .put("confirmations", confirmations)

    companion object {
        fun fromJson(j: JSONObject) = SafeStateV610(
            candidateAt = j.optLong("candidateAt"),
            anchorAsk = j.optDouble("anchorAsk"),
            confirmations = j.optInt("confirmations").coerceIn(0, 3)
        )
    }
}

private data class PumpVariantConfigV610(
    val kind: PumpVariantKindV610,
    val account: String,
    val label: String,
    val prefs: String,
    val mode: PumpProfitModeV526,
    val targetNet: Double,
    val stopNet: Double
)

/**
 * V6.1 rebuilds the two variant PM entry blocks instead of stacking another exception on top of
 * the generic impulse engine. Existing portfolio/state preference names are retained so balances,
 * history and compatible-update continuity survive the rewrite.
 */
private class PumpVariantStoreV610(private val config: PumpVariantConfigV610) {
    private val portfolioKey = "portfolio"
    private val backupKey = "portfolio_backup"
    private val stabilityKey = "stability"
    private val statusKey = "last_status"
    private val statusAtKey = "last_status_at"
    // Keep the historical key so a previously armed V5.x retest can be read safely.
    private val retestKey = "retest_state"
    private val safeKey = "safe_entry_state_v610"

    fun state(context: Context): FusionSimPortfolio {
        val p = prefs(context)
        return parse(p.getString(portfolioKey, null))
            ?: parse(p.getString(backupKey, null))
            ?: FusionSimPortfolio()
    }

    fun status(context: Context): String = prefs(context)
        .getString(statusKey, "${config.label} • ждём первый профильный вход").orEmpty()

    fun statusAt(context: Context): Long = prefs(context).getLong(statusAtKey, 0L)
    fun toJson(value: FusionSimPortfolio): JSONObject = FusionSimStore.toJson(value)

    fun netValue(context: Context, now: Long = System.currentTimeMillis()): Double {
        val value = state(context)
        val market = BitpandaFusionStore.state(context)
        val bid = market.bid.takeIf { market.fresh(now) } ?: value.entryPrice
        return netValue(value, bid, market.feeRate)
    }

    fun tradeNet(context: Context, now: Long = System.currentTimeMillis()): Double {
        val value = state(context)
        val market = BitpandaFusionStore.state(context)
        return tradeNet(value, market.bid.takeIf { market.fresh(now) } ?: 0.0, market.feeRate)
    }

    fun fastTracking(context: Context, now: Long = System.currentTimeMillis()): Boolean = when (config.kind) {
        PumpVariantKindV610.RETEST -> retest(context).let { it.armed && now - it.armedAt <= RETEST_WINDOW_MILLIS }
        PumpVariantKindV610.SAFE -> safe(context).let { it.armed && now - it.candidateAt <= SAFE_CANDIDATE_TTL_MILLIS }
    }

    fun fastCandidate(
        context: Context,
        observation: SharedFusionEntryObservation,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        if (fastTracking(context, now)) return true
        return when (config.kind) {
            PumpVariantKindV610.RETEST -> PumpVariantEntryPolicyV610.retestSeed(observation).let {
                !it.hardVeto && (it.allowed || it.nearCandidate)
            }
            PumpVariantKindV610.SAFE -> PumpVariantEntryPolicyV610.safeContinuation(
                observation, appSupport(context)
            ).let { !it.hardVeto && (it.allowed || it.nearCandidate) }
        }
    }

    @Synchronized
    fun sync(context: Context, now: Long = System.currentTimeMillis()): PumpVariantSyncResult {
        val market = BitpandaFusionStore.state(context)
        val current = state(context)
        val previous = stability(context)
        if (!market.fresh(now) || market.bid <= 0.0 || market.ask <= 0.0) {
            return finish(context, current, previous, "WAIT: ${config.label} ждёт свежий Bitpanda bid/ask", 0.0)
        }
        val observation = SharedFusionEntryObservationStore.snapshot(context, now)
        val marked = mark(current, market.bid, market.feeRate)
        if (!marked.inPosition) {
            if (previous.cooldownUntil > now) {
                val left = ((previous.cooldownUntil - now + 999L) / 1000L).coerceAtLeast(1L)
                return finish(context, marked, previous, "${config.label} COOLDOWN: ещё ${left}с после собственного выхода", 0.0)
            }
            return when (config.kind) {
                PumpVariantKindV610.RETEST -> syncRetestEntry(context, marked, previous, market, observation, now)
                PumpVariantKindV610.SAFE -> syncSafeEntry(context, marked, previous, market, observation, now)
            }
        }

        resetEntryState(context)
        val lastBuy = marked.trades.asReversed().firstOrNull { it.action == "BUY" }
        val age = lastBuy?.let { (now - it.time).coerceAtLeast(0L) } ?: Long.MAX_VALUE
        val exit = PumpProfitEngineV526.evaluatePosition(
            config.mode, marked, previous, observation, market.bid, market.feeRate, age
        )
        if (exit.action != "EXIT") {
            return finish(context, marked, exit.nextState, exit.reason.orEmpty(), exit.tradeNetPercent)
        }
        return sell(context, marked, previous, exit.nextState, market, now, exit.reason.orEmpty())
    }

    private fun syncRetestEntry(
        context: Context,
        value: FusionSimPortfolio,
        previous: FusionStabilityState,
        market: FusionMarketSnapshot,
        observation: SharedFusionEntryObservation,
        now: Long
    ): PumpVariantSyncResult {
        val old = retest(context)
        if (!old.armed) {
            val seed = PumpVariantEntryPolicyV610.retestSeed(observation)
            if (!seed.allowed) {
                return finish(
                    context, value, resetEntry(previous),
                    "V610 RETEST WAIT: ${seed.score}/${seed.threshold}; ${seed.reason}", 0.0
                )
            }
            saveRetest(context, RetestStateV610(now, market.ask, market.ask, false))
            return finish(
                context, value, resetEntry(previous),
                "V610 RETEST ARMED: ранний импульс ${seed.score}/${seed.threshold}; теперь до 8 минут ждём нормальный откат и возврат покупателей", 0.0
            )
        }

        if (now - old.armedAt > RETEST_WINDOW_MILLIS) {
            saveRetest(context, RetestStateV610())
            return finish(context, value, resetEntry(previous), "V610 RETEST CANCEL: окно 8 минут закончилось", 0.0)
        }
        if (!old.pulledBack && market.ask > old.anchorAsk * 1.008) {
            saveRetest(context, RetestStateV610())
            return finish(
                context, value, resetEntry(previous),
                "V610 RETEST CANCEL: цена ушла выше +0,80% без ретеста; погоню оставляем другим профилям", 0.0
            )
        }

        val low = minOf(old.lowAsk.takeIf { it > 0.0 } ?: market.ask, market.ask)
        val pullback = ((1.0 - low / old.anchorAsk) * 100.0).coerceAtLeast(0.0)
        val pulledBack = old.pulledBack || pullback >= RETEST_MIN_PULLBACK_PERCENT
        val rebound = if (low > 0.0) ((market.ask / low - 1.0) * 100.0).coerceAtLeast(0.0) else 0.0
        if (pullback > RETEST_MAX_PULLBACK_PERCENT) {
            saveRetest(context, RetestStateV610())
            return finish(
                context, value, resetEntry(previous),
                "V610 RETEST CANCEL: откат ${signed(-pullback)}% стал слишком глубоким", 0.0
            )
        }

        val nextRetest = old.copy(lowAsk = low, pulledBack = pulledBack)
        saveRetest(context, nextRetest)
        if (!pulledBack) {
            return finish(
                context, value, resetEntry(previous),
                "V610 RETEST TRACKING: минимум ${signed(-pullback)}%; нужен откат хотя бы −${fmt(RETEST_MIN_PULLBACK_PERCENT)}%", 0.0
            )
        }

        val assessment = PumpVariantEntryPolicyV610.retestRebound(observation, pullback, rebound)
        if (!assessment.allowed) {
            if (assessment.hardVeto) saveRetest(context, RetestStateV610())
            return finish(
                context, value, resetEntry(previous),
                "V610 RETEST REBOUND WAIT: ${assessment.score}/${assessment.threshold}; ${assessment.reason}", 0.0
            )
        }

        val ai = DeepSeekEntryCoach.review(
            context, config.mode, observation, assessment.score, assessment.threshold, now
        )
        if (!ai.allowed && !assessment.strongLocal) {
            return finish(
                context, value, resetEntry(previous),
                "V610 RETEST AI SOFT WAIT: ${ai.reason}; профиль остаётся вооружён, локальный rebound не сбрасывается", 0.0
            )
        }
        saveRetest(context, RetestStateV610())
        val reason = "V610 RETEST BUY: pullback/rebound подтверждены; ${assessment.reason}; " +
            if (assessment.strongLocal && !ai.allowed) "сильный локальный rebound не зависит от доступности AI" else ai.reason
        return buy(context, value, resetEntry(previous), market, now, reason)
    }

    private fun syncSafeEntry(
        context: Context,
        value: FusionSimPortfolio,
        previous: FusionStabilityState,
        market: FusionMarketSnapshot,
        observation: SharedFusionEntryObservation,
        now: Long
    ): PumpVariantSyncResult {
        val app = appSupport(context)
        val assessment = PumpVariantEntryPolicyV610.safeContinuation(observation, app)
        val old = safe(context)
        if (!assessment.allowed) {
            val keep = !assessment.hardVeto && assessment.nearCandidate && old.armed &&
                now - old.candidateAt <= SAFE_CANDIDATE_TTL_MILLIS
            if (!keep) saveSafe(context, SafeStateV610())
            return finish(
                context, value, resetEntry(previous),
                "V610 SAFE WAIT: ${assessment.score}/${assessment.threshold}${if (app) " + APP" else ""}; ${assessment.reason}", 0.0
            )
        }

        val candidateAt = if (old.armed) old.candidateAt else now
        val anchor = if (old.armed) old.anchorAsk else market.ask
        val confirmations = if (old.armed) (old.confirmations + 1).coerceAtMost(3) else 1
        val next = SafeStateV610(candidateAt, anchor, confirmations)
        saveSafe(context, next)
        val elapsed = (now - candidateAt).coerceAtLeast(0L)
        val priceMove = if (anchor > 0.0) (market.ask / anchor - 1.0) * 100.0 else 0.0
        val priceAccepted = priceMove in -0.20..0.38
        if (confirmations < 2 || elapsed < SAFE_CONFIRM_MILLIS || !priceAccepted) {
            return finish(
                context, value, resetEntry(previous),
                "V610 SAFE ARMED ${confirmations}/2: ${assessment.score}/${assessment.threshold}; " +
                    "${elapsed / 1000L}s/${SAFE_CONFIRM_MILLIS / 1000L}s; цена ${signed(priceMove)}% " +
                    if (priceAccepted) "допустима" else "вне безопасного диапазона",
                0.0
            )
        }

        val ai = DeepSeekEntryCoach.review(
            context, config.mode, observation, assessment.score, assessment.threshold, now
        )
        if (!ai.allowed && !assessment.strongLocal) {
            return finish(
                context, value, resetEntry(previous),
                "V610 SAFE AI SOFT WAIT: ${ai.reason}; локальный кандидат сохраняется", 0.0
            )
        }
        saveSafe(context, SafeStateV610())
        val reason = "V610 SAFE BUY: стабильное continuation подтверждено${if (app) " + APP support" else ""}; " +
            "${assessment.reason}; " +
            if (assessment.strongLocal && !ai.allowed) "сильный локальный SAFE не зависит от доступности AI" else ai.reason
        return buy(context, value, resetEntry(previous), market, now, reason)
    }

    private fun appSupport(context: Context): Boolean = runCatching {
        val evaluation = PumpBotEngine.evaluateAppPaper(context, AppPaperStore.state(context))
        evaluation.candleTime > 0L &&
            !evaluation.action.equals(StrategyV2.ACTION_SELL, ignoreCase = true) &&
            !evaluation.action.equals(StrategyV2.ACTION_SELL_HALF, ignoreCase = true) &&
            (evaluation.action.equals("BUY", ignoreCase = true) || evaluation.readinessScore >= 85)
    }.getOrDefault(false)

    private fun buy(
        context: Context,
        value: FusionSimPortfolio,
        state: FusionStabilityState,
        market: FusionMarketSnapshot,
        now: Long,
        reason: String
    ): PumpVariantSyncResult {
        val allocation = value.cashEur
        val fee = allocation * market.feeRate
        val amount = (allocation - fee) / market.ask
        val trade = FusionSimTrade(now, now, "BUY", market.ask, amount, fee, 0.0, reason)
        val decision = FusionSimDecision(now, now, "BUY", "${config.label} BUY • paper-only", market.ask, reason)
        val next = value.copy(
            cashEur = 0.0,
            pumpAmount = amount,
            entryPrice = market.ask,
            entryCostEur = allocation,
            lastDecisionId = now,
            totalFeesEur = value.totalFeesEur + fee,
            trades = (value.trades + trade).takeLast(5_000),
            decisions = (value.decisions + decision).takeLast(9_000)
        )
        val nextState = state.copy(
            entryStreak = 0,
            entryCandidateAt = 0L,
            entryAnchorAsk = 0.0,
            peakBid = market.bid,
            profitDefenseArmed = false,
            cooldownUntil = 0L
        )
        val status = "BUY: $reason • цель ${signed(config.targetNet)}% NET • стоп ${signed(config.stopNet)}% NET"
        save(context, next, nextState, status)
        UnifiedResearchLog.record(context, config.account, "BUY", status, now)
        return PumpVariantSyncResult(next, status, 0.0)
    }

    private fun sell(
        context: Context,
        value: FusionSimPortfolio,
        previous: FusionStabilityState,
        evaluated: FusionStabilityState,
        market: FusionMarketSnapshot,
        now: Long,
        reason: String
    ): PumpVariantSyncResult {
        val gross = value.pumpAmount * market.bid
        val fee = gross * market.feeRate
        val net = gross - fee
        val pnl = net - value.entryCostEur
        val percent = if (value.entryCostEur > 0.0) pnl / value.entryCostEur * 100.0 else 0.0
        val trade = FusionSimTrade(now, now, "SELL", market.bid, value.pumpAmount, fee, pnl, reason)
        val decision = FusionSimDecision(now, now, "EXIT", "${config.label} SELL • ${signed(percent)}% NET", market.bid, reason)
        val cash = value.cashEur + net
        val peak = max(value.peakValueEur, cash)
        val drawdown = if (peak > 0.0) ((peak - cash) / peak * 100.0).coerceAtLeast(0.0) else 0.0
        val next = value.copy(
            cashEur = cash,
            pumpAmount = 0.0,
            entryPrice = 0.0,
            entryCostEur = 0.0,
            lastDecisionId = now,
            totalFeesEur = value.totalFeesEur + fee,
            peakValueEur = peak,
            maxDrawdownPercent = max(value.maxDrawdownPercent, drawdown),
            trades = (value.trades + trade).takeLast(5_000),
            decisions = (value.decisions + decision).takeLast(9_000)
        )
        val protective = reason.contains("HARD_STOP") || reason.contains("EARLY_RISK_EXIT")
        val cooldown = FusionStabilityPolicy.cooldownAfterExit(
            evaluated.copy(cooldownUntil = previous.cooldownUntil), pnl, protective, now
        )
        val status = "SELL ${signed(percent)}% NET: $reason"
        save(context, next, cooldown, status)
        UnifiedResearchLog.record(context, config.account, "SELL", status, now)
        return PumpVariantSyncResult(next, status, percent)
    }

    private fun finish(
        context: Context,
        value: FusionSimPortfolio,
        state: FusionStabilityState,
        text: String,
        net: Double
    ): PumpVariantSyncResult {
        save(context, value, state, text)
        return PumpVariantSyncResult(value, text, net)
    }

    private fun resetEntry(previous: FusionStabilityState) = previous.copy(
        entryStreak = 0,
        entryCandidateAt = 0L,
        entryAnchorAsk = 0.0,
        exitStreak = 0,
        exitArmedAt = 0L,
        exitArmedBid = 0.0,
        peakBid = 0.0,
        profitDefenseArmed = false
    )

    private fun mark(value: FusionSimPortfolio, bid: Double, feeRate: Double): FusionSimPortfolio {
        val liquidation = netValue(value, bid, feeRate)
        val peak = max(value.peakValueEur, liquidation)
        val drawdown = if (peak > 0.0) ((peak - liquidation) / peak * 100.0).coerceAtLeast(0.0) else 0.0
        return value.copy(peakValueEur = peak, maxDrawdownPercent = max(value.maxDrawdownPercent, drawdown))
    }

    private fun tradeNet(value: FusionSimPortfolio, bid: Double, feeRate: Double): Double {
        if (!value.inPosition || value.entryCostEur <= 0.0 || bid <= 0.0) return 0.0
        return (value.pumpAmount * bid * (1.0 - feeRate) / value.entryCostEur - 1.0) * 100.0
    }

    private fun netValue(value: FusionSimPortfolio, bid: Double, feeRate: Double): Double =
        if (!value.inPosition) value.cashEur else value.cashEur + value.pumpAmount * bid * (1.0 - feeRate)

    private fun stability(context: Context): FusionStabilityState = runCatching {
        FusionStabilityState.fromJson(JSONObject(prefs(context).getString(stabilityKey, null).orEmpty()))
    }.getOrDefault(FusionStabilityState())

    private fun retest(context: Context): RetestStateV610 = runCatching {
        RetestStateV610.fromJson(JSONObject(prefs(context).getString(retestKey, null).orEmpty()))
    }.getOrDefault(RetestStateV610())

    private fun safe(context: Context): SafeStateV610 = runCatching {
        SafeStateV610.fromJson(JSONObject(prefs(context).getString(safeKey, null).orEmpty()))
    }.getOrDefault(SafeStateV610())

    private fun saveRetest(context: Context, value: RetestStateV610) {
        prefs(context).edit().putString(retestKey, value.toJson().toString()).apply()
    }

    private fun saveSafe(context: Context, value: SafeStateV610) {
        prefs(context).edit().putString(safeKey, value.toJson().toString()).apply()
    }

    private fun resetEntryState(context: Context) {
        saveRetest(context, RetestStateV610())
        saveSafe(context, SafeStateV610())
    }

    private fun save(context: Context, value: FusionSimPortfolio, state: FusionStabilityState, status: String) {
        val raw = FusionSimStore.toJson(value).toString()
        prefs(context).edit()
            .putString(portfolioKey, raw)
            .putString(backupKey, raw)
            .putString(stabilityKey, state.toJson().toString())
            .putString(statusKey, status.take(1200))
            .putLong(statusAtKey, System.currentTimeMillis())
            .commit()
    }

    private fun parse(raw: String?): FusionSimPortfolio? = if (raw.isNullOrBlank()) null else runCatching {
        val j = JSONObject(raw)
        val trades = j.optJSONArray("trades") ?: JSONArray()
        val decisions = j.optJSONArray("decisions") ?: JSONArray()
        FusionSimPortfolio(
            cashEur = j.optDouble("cashEur", FusionSimPortfolio.START_BALANCE),
            pumpAmount = j.optDouble("pumpAmount"),
            entryPrice = j.optDouble("entryPrice"),
            entryCostEur = j.optDouble("entryCostEur"),
            lastDecisionId = j.optLong("lastDecisionId"),
            totalFeesEur = j.optDouble("totalFeesEur"),
            peakValueEur = j.optDouble("peakValueEur", FusionSimPortfolio.START_BALANCE),
            maxDrawdownPercent = j.optDouble("maxDrawdownPercent"),
            trades = (0 until trades.length()).mapNotNull { trades.optJSONObject(it)?.let(FusionSimTrade::fromJson) },
            decisions = (0 until decisions.length()).mapNotNull { decisions.optJSONObject(it)?.let(FusionSimDecision::fromJson) }
        )
    }.getOrNull()

    private fun prefs(context: Context) = context.getSharedPreferences(config.prefs, Context.MODE_PRIVATE)
    private fun signed(value: Double) = String.format(java.util.Locale.GERMANY, "%+.2f", value)
    private fun fmt(value: Double) = String.format(java.util.Locale.GERMANY, "%.2f", value)

    private companion object {
        const val RETEST_WINDOW_MILLIS = 8L * 60L * 1000L
        const val RETEST_MIN_PULLBACK_PERCENT = 0.18
        const val RETEST_MAX_PULLBACK_PERCENT = 0.90
        const val SAFE_CONFIRM_MILLIS = 30_000L
        const val SAFE_CANDIDATE_TTL_MILLIS = 2L * 60L * 1000L
    }
}

object PumpMachineRetestStore {
    private val store = PumpVariantStoreV610(PumpVariantConfigV610(
        kind = PumpVariantKindV610.RETEST,
        account = "PUMP_MACHINE_RETEST",
        label = "PUMP MACHINE RETEST",
        prefs = "pump_machine_retest_paper_v529",
        mode = PumpProfitModeV526.PUMP_RETEST,
        targetNet = 2.00,
        stopNet = -1.10
    ))

    fun state(c: Context) = store.state(c)
    fun lastStatus(c: Context) = store.status(c)
    fun lastStatusAt(c: Context) = store.statusAt(c)
    fun sync(c: Context, now: Long = System.currentTimeMillis()) = store.sync(c, now)
    fun netValue(c: Context, now: Long = System.currentTimeMillis()) = store.netValue(c, now)
    fun tradeNetPercent(c: Context, now: Long = System.currentTimeMillis()) = store.tradeNet(c, now)
    fun toJson(v: FusionSimPortfolio) = store.toJson(v)
    fun fastTracking(c: Context, now: Long = System.currentTimeMillis()) = store.fastTracking(c, now)
    fun fastCandidate(c: Context, observation: SharedFusionEntryObservation, now: Long = System.currentTimeMillis()) =
        store.fastCandidate(c, observation, now)
}

object PumpMachineSafeStore {
    private val store = PumpVariantStoreV610(PumpVariantConfigV610(
        kind = PumpVariantKindV610.SAFE,
        account = "PUMP_MACHINE_SAFE",
        label = "PUMP MACHINE SAFE",
        prefs = "pump_machine_safe_paper_v529",
        mode = PumpProfitModeV526.PUMP_SAFE,
        targetNet = 1.15,
        stopNet = -0.75
    ))

    fun state(c: Context) = store.state(c)
    fun lastStatus(c: Context) = store.status(c)
    fun lastStatusAt(c: Context) = store.statusAt(c)
    fun sync(c: Context, now: Long = System.currentTimeMillis()) = store.sync(c, now)
    fun netValue(c: Context, now: Long = System.currentTimeMillis()) = store.netValue(c, now)
    fun tradeNetPercent(c: Context, now: Long = System.currentTimeMillis()) = store.tradeNet(c, now)
    fun toJson(v: FusionSimPortfolio) = store.toJson(v)
    fun fastTracking(c: Context, now: Long = System.currentTimeMillis()) = store.fastTracking(c, now)
    fun fastCandidate(c: Context, observation: SharedFusionEntryObservation, now: Long = System.currentTimeMillis()) =
        store.fastCandidate(c, observation, now)
}
