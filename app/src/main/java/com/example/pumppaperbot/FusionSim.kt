package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.pow

data class FusionSimTrade(
    val time: Long,
    val decisionId: Long,
    val action: String,
    val price: Double,
    val amount: Double,
    val feeEur: Double,
    val pnlEur: Double,
    val reason: String
) {
    fun toJson(): JSONObject = JSONObject().put("time", time).put("decisionId", decisionId)
        .put("action", action).put("price", price).put("amount", amount)
        .put("feeEur", feeEur).put("pnlEur", pnlEur).put("reason", reason)

    companion object {
        fun fromJson(j: JSONObject) = FusionSimTrade(
            j.optLong("time"), j.optLong("decisionId"), j.optString("action"),
            j.optDouble("price"), j.optDouble("amount"), j.optDouble("feeEur"),
            j.optDouble("pnlEur"), j.optString("reason")
        )
    }
}

data class FusionSimDecision(
    val time: Long,
    val decisionId: Long,
    val requestedAction: String,
    val result: String,
    val venuePrice: Double,
    val reason: String
) {
    fun toJson(): JSONObject = JSONObject().put("time", time).put("decisionId", decisionId)
        .put("requestedAction", requestedAction).put("result", result)
        .put("venuePrice", venuePrice).put("reason", reason)

    companion object {
        fun fromJson(j: JSONObject) = FusionSimDecision(
            j.optLong("time"), j.optLong("decisionId"), j.optString("requestedAction"),
            j.optString("result"), j.optDouble("venuePrice"), j.optString("reason")
        )
    }
}

data class FusionSimPortfolio(
    val cashEur: Double = START_BALANCE,
    val pumpAmount: Double = 0.0,
    val entryPrice: Double = 0.0,
    val entryCostEur: Double = 0.0,
    val lastDecisionId: Long = 0L,
    val totalFeesEur: Double = 0.0,
    val peakValueEur: Double = START_BALANCE,
    val maxDrawdownPercent: Double = 0.0,
    val trades: List<FusionSimTrade> = emptyList(),
    val decisions: List<FusionSimDecision> = emptyList()
) {
    val inPosition: Boolean get() = pumpAmount > 0.0
    fun value(price: Double): Double = cashEur + pumpAmount * max(0.0, price)
    fun profit(price: Double): Double = value(price) - START_BALANCE
    companion object { const val START_BALANCE = 1000.0 }
}

data class FusionPriorityPlan(
    val active: Boolean,
    val forcePro: Boolean,
    val intervalMillis: Long,
    val label: String
)

data class FusionPriorityMetrics(
    val markPriceEur: Double,
    val estimatedExitFeeEur: Double,
    val netLiquidationValueEur: Double,
    val netPnlEur: Double,
    val netPnlPercent: Double,
    val pullbackFromPeakPercent: Double,
    val venueFresh: Boolean
)

object FusionPriorityPolicy {
    const val PRIORITY_INTERVAL_MILLIS = 5L * 60L * 1000L
    const val NORMAL_INTERVAL_MILLIS = 120_000L

    fun plan(portfolio: FusionSimPortfolio): FusionPriorityPlan = if (portfolio.inPosition) {
        FusionPriorityPlan(
            active = true,
            forcePro = false,
            intervalMillis = PRIORITY_INTERVAL_MILLIS,
            label = "FUSION POSITION • ЛОКАЛЬНЫЙ КОНТРОЛЬ • ПЛАТНЫЙ DEEPSIG НЕ УСКОРЯЕТСЯ"
        )
    } else {
        FusionPriorityPlan(
            active = false,
            forcePro = false,
            intervalMillis = NORMAL_INTERVAL_MILLIS,
            label = "FUSION POSITION • НЕТ"
        )
    }

    fun metrics(
        portfolio: FusionSimPortfolio,
        markPriceEur: Double,
        feeRate: Double,
        venueFresh: Boolean
    ): FusionPriorityMetrics {
        val mark = markPriceEur.coerceAtLeast(0.0)
        val exitFee = if (portfolio.inPosition) {
            portfolio.pumpAmount * mark * feeRate.coerceIn(0.0, 0.02)
        } else 0.0
        val liquidation = portfolio.cashEur + portfolio.pumpAmount * mark - exitFee
        val pnl = liquidation - FusionSimPortfolio.START_BALANCE
        val peak = max(portfolio.peakValueEur, FusionSimPortfolio.START_BALANCE)
        val pullback = if (peak > 0.0) {
            ((peak - liquidation) / peak * 100.0).coerceAtLeast(0.0)
        } else 0.0
        return FusionPriorityMetrics(
            markPriceEur = mark,
            estimatedExitFeeEur = exitFee,
            netLiquidationValueEur = liquidation,
            netPnlEur = pnl,
            netPnlPercent = pnl / FusionSimPortfolio.START_BALANCE * 100.0,
            pullbackFromPeakPercent = pullback,
            venueFresh = venueFresh
        )
    }
}

data class FusionFlowFrame(
    val instant: Int,
    val score5m: Int,
    val score15m: Int,
    val score20m: Int,
    val score30m: Int
) {
    val buySignal: Boolean get() = instant > 0 && score5m > 0 && score15m > 0 && score30m > 0
    val exitSignal: Boolean get() = instant < 0 && score5m < 0 && score15m < 0 && score20m < 0
    val strongBuy: Boolean get() = instant >= 8 && score5m >= 6 && score15m >= 5 && score30m >= 3
    val exitRecovery: Boolean get() = score5m >= 5 || score15m >= 5 || score20m >= 5

    /** Earlier warning: the medium horizons are no longer just noisy around zero. */
    val deteriorationSignal: Boolean get() {
        val mediumNegative = listOf(score5m, score15m, score20m).count { it <= -8 } >= 2
        val mediumCoreWeak = score15m <= -8 || score20m <= -8
        return mediumNegative && mediumCoreWeak
    }

    /** Full system exit still uses current/5/15/20, but ignores tiny -1/-1/-1/-1 chatter. */
    val meaningfulExitSignal: Boolean get() = exitSignal && (
        instant <= -4 || score5m <= -3 || score15m <= -2 || score20m <= -2
    )

    val severeExitSignal: Boolean get() = instant <= -10 && score5m <= -7 && score15m <= -5 && score20m <= -4
}

data class FusionFlowDecision(
    val action: String,
    val reason: String,
    val instant: Int,
    val score5m: Int,
    val score15m: Int,
    val score20m: Int,
    val score30m: Int
)

object FusionFlowPolicy {
    fun frame(breathing: LiveMarketBreathingSnapshot): FusionFlowFrame? {
        if (!breathing.fresh) return null
        val instant = breathing.instantScore ?: return null
        fun upperBar(minutes: Int): Int? = breathing.horizons
            .firstOrNull { it.minutes == minutes }
            ?.score
        return FusionFlowFrame(
            instant = instant,
            score5m = upperBar(5) ?: return null,
            score15m = upperBar(15) ?: return null,
            score20m = upperBar(20) ?: return null,
            score30m = upperBar(30) ?: return null
        )
    }

    fun decide(
        inPosition: Boolean,
        breathing: LiveMarketBreathingSnapshot
    ): FusionFlowDecision? {
        val frame = frame(breathing) ?: return null
        return when {
            !inPosition && frame.buySignal -> FusionFlowDecision(
                "BUY",
                "FLOW V5.16: сейчас/5м/15м/30м выше нуля; исполнение пройдёт anti-churn подтверждение",
                frame.instant, frame.score5m, frame.score15m, frame.score20m, frame.score30m
            )
            inPosition && frame.meaningfulExitSignal -> FusionFlowDecision(
                "EXIT",
                "FLOW V5.16: сейчас/5м/15м/20м устойчиво отрицательны; выход проходит подтверждение цены/времени",
                frame.instant, frame.score5m, frame.score15m, frame.score20m, frame.score30m
            )
            else -> null
        }
    }
}

data class FusionStabilityState(
    val entryStreak: Int = 0,
    val entryCandidateAt: Long = 0L,
    val exitStreak: Int = 0,
    val exitArmedAt: Long = 0L,
    val exitArmedBid: Double = 0.0,
    val peakBid: Double = 0.0,
    val profitDefenseArmed: Boolean = false,
    val cooldownUntil: Long = 0L,
    val lastExitAt: Long = 0L,
    val lastLossExitAt: Long = 0L,
    val lossExitStreak: Int = 0
) {
    val exitArmed: Boolean get() = exitArmedAt > 0L && exitArmedBid > 0.0

    fun toJson(): JSONObject = JSONObject()
        .put("entryStreak", entryStreak)
        .put("entryCandidateAt", entryCandidateAt)
        .put("exitStreak", exitStreak)
        .put("exitArmedAt", exitArmedAt)
        .put("exitArmedBid", exitArmedBid)
        .put("peakBid", peakBid)
        .put("profitDefenseArmed", profitDefenseArmed)
        .put("cooldownUntil", cooldownUntil)
        .put("lastExitAt", lastExitAt)
        .put("lastLossExitAt", lastLossExitAt)
        .put("lossExitStreak", lossExitStreak)

    companion object {
        fun fromJson(j: JSONObject) = FusionStabilityState(
            entryStreak = j.optInt("entryStreak"),
            entryCandidateAt = j.optLong("entryCandidateAt"),
            exitStreak = j.optInt("exitStreak"),
            exitArmedAt = j.optLong("exitArmedAt"),
            exitArmedBid = j.optDouble("exitArmedBid"),
            peakBid = j.optDouble("peakBid"),
            profitDefenseArmed = j.optBoolean("profitDefenseArmed"),
            cooldownUntil = j.optLong("cooldownUntil"),
            lastExitAt = j.optLong("lastExitAt"),
            lastLossExitAt = j.optLong("lastLossExitAt"),
            lossExitStreak = j.optInt("lossExitStreak")
        )
    }
}

data class FusionStabilityDecision(
    val action: String?,
    val nextState: FusionStabilityState,
    val activeStopPrice: Double,
    val reason: String
)

/** Pure virtual stop/trailing calculations. No exchange order path exists here. */
object FusionRiskPolicy {
    const val STRUCTURAL_TRAIL_PERCENT = 1.75
    const val PROFIT_DEFENSE_TRAIL_PERCENT = 1.00
    const val PROFIT_DEFENSE_EXTRA_CUSHION_PERCENT = 1.15

    fun breakEvenGrossPercent(feeRate: Double): Double {
        val fee = feeRate.coerceIn(0.0, 0.02)
        val grossFactor = 1.0 / (1.0 - fee).pow(2.0)
        return (grossFactor - 1.0) * 100.0
    }

    fun peakGainPercent(entryPrice: Double, peakBid: Double): Double = if (entryPrice > 0.0) {
        (max(entryPrice, peakBid) / entryPrice - 1.0) * 100.0
    } else 0.0

    fun profitDefenseEligible(entryPrice: Double, peakBid: Double, feeRate: Double): Boolean {
        val required = breakEvenGrossPercent(feeRate) + PROFIT_DEFENSE_EXTRA_CUSHION_PERCENT
        return peakGainPercent(entryPrice, peakBid) >= required
    }

    fun activeStopPrice(
        entryPrice: Double,
        peakBid: Double,
        feeRate: Double,
        profitDefenseArmed: Boolean = false
    ): Double {
        if (entryPrice <= 0.0) return 0.0
        val peak = max(entryPrice, peakBid)
        val structural = peak * (1.0 - STRUCTURAL_TRAIL_PERCENT / 100.0)
        if (!profitDefenseArmed) return structural
        val defense = peak * (1.0 - PROFIT_DEFENSE_TRAIL_PERCENT / 100.0)
        return max(structural, defense)
    }
}

/**
 * V5.16 anti-churn state machine.
 *
 * Two independent exits are kept intentionally:
 * 1) HARD_TRAILING_STOP / PROFIT_DEFENSE_STOP protects capital and already earned profit.
 * 2) SYSTEM_EXIT uses current/5/15/20 flow deterioration plus real price confirmation.
 *
 * Profit defense does NOT tighten merely because a small profit appeared. It arms only after
 * enough fee-covered upside exists AND the flow columns materially deteriorate. Once armed,
 * the stop never loosens during that position.
 */
object FusionStabilityPolicy {
    const val ENTRY_CONFIRMATIONS = 2
    const val ENTRY_CONFIRM_MIN_MILLIS = 60L * 1000L
    const val MIN_HOLD_MILLIS = 10L * 60L * 1000L
    const val SHOCK_MIN_HOLD_MILLIS = 2L * 60L * 1000L
    const val SHOCK_FAILURE_MIN_AGE_MILLIS = 15_000L
    const val EXIT_ARM_TTL_MILLIS = 8L * 60L * 1000L
    const val REQUIRED_DOWN_FROM_ARM_PERCENT = 0.20
    const val REQUIRED_PULLBACK_FROM_PEAK_PERCENT = 0.50
    const val NORMAL_REENTRY_COOLDOWN_MILLIS = 15L * 60L * 1000L
    const val STOP_REENTRY_COOLDOWN_MILLIS = 20L * 60L * 1000L
    const val DOUBLE_LOSS_COOLDOWN_MILLIS = 30L * 60L * 1000L
    const val LOSS_STREAK_WINDOW_MILLIS = 60L * 60L * 1000L

    fun evaluate(
        inPosition: Boolean,
        entryPrice: Double,
        previous: FusionStabilityState,
        frame: FusionFlowFrame?,
        bid: Double,
        feeRate: Double,
        now: Long,
        positionAgeMillis: Long = Long.MAX_VALUE,
        shockReady: Boolean = false,
        shockFailed: Boolean = false,
        shockEntry: Boolean = false,
        entryObservation: SharedFusionEntryObservation? = null
    ): FusionStabilityDecision {
        if (bid <= 0.0) return FusionStabilityDecision(null, previous, 0.0, "Нет свежего bid")

        if (!inPosition) {
            val observation = entryObservation ?: SharedFusionEntryObservation(
                frame = frame,
                shockReady = shockReady,
                sampledAt = now,
                sampleBucket = now / 15_000L
            )
            val shared = SharedFusionEntryPolicy.evaluate(previous, observation, now)
            return FusionStabilityDecision(shared.action, shared.nextState, 0.0, shared.reason)
        }

        val peak = max(max(previous.peakBid, bid), entryPrice)
        val deterioration = frame?.deteriorationSignal == true
        val defenseEligible = FusionRiskPolicy.profitDefenseEligible(entryPrice, peak, feeRate)
        val defenseArmed = previous.profitDefenseArmed || (deterioration && defenseEligible)
        val activeStop = FusionRiskPolicy.activeStopPrice(
            entryPrice = entryPrice,
            peakBid = peak,
            feeRate = feeRate,
            profitDefenseArmed = defenseArmed
        )

        val basePositionState = previous.copy(
            entryStreak = 0,
            entryCandidateAt = 0L,
            peakBid = peak,
            profitDefenseArmed = defenseArmed,
            cooldownUntil = 0L
        )

        if (shockEntry && shockFailed && positionAgeMillis >= SHOCK_FAILURE_MIN_AGE_MILLIS) {
            return FusionStabilityDecision(
                "EXIT", basePositionState, activeStop,
                "SHOCK_REBOUND_FAILED: быстрый отскок после провала сорвался; продавцы вернули контроль, paper-позицию закрываем без ожидания медленных горизонтов"
            )
        }

        if (activeStop > 0.0 && bid <= activeStop) {
            val tag = if (defenseArmed) "PROFIT_DEFENSE_STOP" else "HARD_TRAILING_STOP"
            val trail = if (defenseArmed) FusionRiskPolicy.PROFIT_DEFENSE_TRAIL_PERCENT else FusionRiskPolicy.STRUCTURAL_TRAIL_PERCENT
            return FusionStabilityDecision(
                "EXIT",
                basePositionState,
                activeStop,
                "$tag: bid дошёл до защитного trailing-порога; дистанция от пика ${String.format(java.util.Locale.US, "%.2f", trail)}%"
            )
        }

        val rawExit = frame?.meaningfulExitSignal == true
        val severeExit = frame?.severeExitSignal == true
        val recovered = frame?.exitRecovery == true
        val armedStillFresh = previous.exitArmed && now - previous.exitArmedAt in 0..EXIT_ARM_TTL_MILLIS

        if (recovered) {
            return FusionStabilityDecision(
                null,
                basePositionState.copy(exitStreak = 0, exitArmedAt = 0L, exitArmedBid = 0.0),
                activeStop,
                if (defenseArmed) {
                    "HOLD: поток восстановился; системный EXIT снят, но уже поднятая защита прибыли не опускается"
                } else {
                    "HOLD: выход снят — 5/15/20 минут показали заметное восстановление"
                }
            )
        }

        val armedAt = when {
            rawExit && !armedStillFresh -> now
            armedStillFresh -> previous.exitArmedAt
            else -> 0L
        }
        val armedBid = when {
            rawExit && !armedStillFresh -> bid
            armedStillFresh -> previous.exitArmedBid
            else -> 0.0
        }
        val streak = if (rawExit) {
            (previous.exitStreak + 1).coerceAtMost(99)
        } else if (armedStillFresh) {
            previous.exitStreak
        } else 0
        val armed = armedAt > 0L && armedBid > 0.0
        val downFromArm = if (armed) {
            ((armedBid - bid) / armedBid * 100.0).coerceAtLeast(0.0)
        } else 0.0
        val pullbackFromPeak = if (peak > 0.0) {
            ((peak - bid) / peak * 100.0).coerceAtLeast(0.0)
        } else 0.0
        val next = basePositionState.copy(
            exitStreak = streak,
            exitArmedAt = armedAt,
            exitArmedBid = armedBid
        )

        val actualDecline = downFromArm >= REQUIRED_DOWN_FROM_ARM_PERCENT ||
            pullbackFromPeak >= REQUIRED_PULLBACK_FROM_PEAK_PERCENT
        val exitConfirmed = severeExit || streak >= 2
        val holdLimit = if (shockEntry) SHOCK_MIN_HOLD_MILLIS else MIN_HOLD_MILLIS
        val holdLockActive = positionAgeMillis < holdLimit

        if (armed && actualDecline && exitConfirmed && (!holdLockActive || severeExit)) {
            return FusionStabilityDecision(
                "EXIT", next, activeStop,
                "SYSTEM_EXIT: сейчас/5/15/20 подтвердили отрицательное давление, затем bid реально пошёл вниз " +
                    String.format(java.util.Locale.US, "(от arm %.2f%%, от пика %.2f%%)", downFromArm, pullbackFromPeak)
            )
        }
        if (rawExit && holdLockActive && !severeExit) {
            val left = ((holdLimit - positionAgeMillis).coerceAtLeast(0L) / 1000L)
            return FusionStabilityDecision(
                null, next, activeStop,
                "HOLD_LOCK: обычный системный EXIT пока не исполняем; минимальное удержание ещё ${left}с, аварийный stop остаётся активным"
            )
        }
        if (rawExit) {
            return FusionStabilityDecision(
                null, next, activeStop,
                if (exitConfirmed) {
                    "EXIT_ARMED: поток подтвердился, но ждём реальное снижение цены вместо продажи в боковике"
                } else {
                    "EXIT_ARMED: первое устойчивое отрицательное наблюдение; ждём второе подтверждение"
                }
            )
        }
        if (armedStillFresh) {
            return FusionStabilityDecision(
                null, next, activeStop,
                "EXIT_ARMED: короткое окно остаётся активным; боковик сам по себе продажу не запускает"
            )
        }
        return FusionStabilityDecision(
            null,
            basePositionState.copy(exitStreak = 0, exitArmedAt = 0L, exitArmedBid = 0.0),
            activeStop,
            when {
                defenseArmed -> "PROFIT_DEFENSE: поток ухудшился после достаточного навара; stop подтянут до 1% от пика и больше не опускается"
                deterioration && !defenseEligible -> "HOLD: поток ухудшается, но навар ещё недостаточен для tight profit-defense; работает базовый trailing 1,75%"
                else -> "HOLD: позиция удерживается; работает базовый trailing 1,75% от пика"
            }
        )
    }

    fun cooldownAfterExit(
        previous: FusionStabilityState,
        exitPnlEur: Double,
        wasProtectiveStop: Boolean,
        now: Long
    ): FusionStabilityState {
        val loss = exitPnlEur < 0.0
        val recentPriorLoss = loss && previous.lastLossExitAt > 0L &&
            now - previous.lastLossExitAt in 0..LOSS_STREAK_WINDOW_MILLIS
        val lossStreak = when {
            !loss -> 0
            recentPriorLoss -> (previous.lossExitStreak + 1).coerceAtMost(99)
            else -> 1
        }
        val cooldown = when {
            loss && lossStreak >= 2 -> DOUBLE_LOSS_COOLDOWN_MILLIS
            wasProtectiveStop -> STOP_REENTRY_COOLDOWN_MILLIS
            else -> NORMAL_REENTRY_COOLDOWN_MILLIS
        }
        return FusionStabilityState(
            cooldownUntil = now + cooldown,
            lastExitAt = now,
            lastLossExitAt = if (loss) now else previous.lastLossExitAt,
            lossExitStreak = lossStreak
        )
    }
}

internal object FusionSimTrader {
    fun apply(
        current: FusionSimPortfolio,
        decisionId: Long,
        requestedAction: String,
        bid: Double,
        ask: Double,
        feeRate: Double,
        reason: String,
        now: Long
    ): FusionSimPortfolio {
        if (decisionId <= current.lastDecisionId || bid <= 0.0 || ask < bid) return current
        val action = requestedAction.uppercase()
        var cash = current.cashEur
        var amount = current.pumpAmount
        var entry = current.entryPrice
        var entryCost = current.entryCostEur
        var fees = current.totalFeesEur
        var trades = current.trades
        var result = "НАБЛЮДЕНИЕ"
        var venuePrice = (bid + ask) / 2.0
        when {
            action == "BUY" && !current.inPosition && cash > 0.01 -> {
                venuePrice = ask
                val fee = cash * feeRate.coerceIn(0.0, 0.02)
                amount = (cash - fee) / ask
                entryCost = cash
                cash = 0.0
                entry = ask
                fees += fee
                result = "ВИРТУАЛЬНО КУПИЛ ПО ASK"
                trades = (trades + FusionSimTrade(now, decisionId, "BUY", ask, amount, fee, 0.0, reason)).takeLast(5000)
            }
            (action == "SELL" || action == "EXIT") && current.inPosition -> {
                venuePrice = bid
                val gross = amount * bid
                val fee = gross * feeRate.coerceIn(0.0, 0.02)
                val pnl = gross - fee - entryCost
                cash = gross - fee
                fees += fee
                result = "ВИРТУАЛЬНО ПРОДАЛ ПО BID"
                trades = (trades + FusionSimTrade(now, decisionId, "SELL", bid, amount, fee, pnl, reason)).takeLast(5000)
                amount = 0.0
                entry = 0.0
                entryCost = 0.0
            }
            action == "BUY" -> result = "BUY ПРОПУЩЕН: ПОЗИЦИЯ УЖЕ ОТКРЫТА"
            action == "SELL" || action == "EXIT" -> result = "SELL ПРОПУЩЕН: ПОЗИЦИИ НЕТ"
        }
        val value = cash + amount * bid
        val peak = max(current.peakValueEur, value)
        val drawdown = if (peak > 0.0) (1.0 - value / peak) * 100.0 else 0.0
        val decision = FusionSimDecision(now, decisionId, action, result, venuePrice, reason)
        return current.copy(
            cashEur = cash, pumpAmount = amount, entryPrice = entry, entryCostEur = entryCost,
            lastDecisionId = decisionId, totalFeesEur = fees, peakValueEur = peak,
            maxDrawdownPercent = max(current.maxDrawdownPercent, drawdown), trades = trades,
            decisions = (current.decisions + decision).takeLast(9000)
        )
    }
}

object FusionSimStore {
    private const val PREFS = "fusion_sim_paper_v51"
    private const val PORTFOLIO = "portfolio"
    private const val BACKUP = "portfolio_backup"
    private const val ACTIVATED = "activated"
    private const val ACTIVATION_WATERMARK = "activation_watermark"
    private const val STABILITY = "v511_stability"

    fun state(context: Context): FusionSimPortfolio {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        parse(p.getString(PORTFOLIO, null))?.let { return it }
        val recovered = parse(p.getString(BACKUP, null))
        if (recovered != null) { save(context, recovered); return recovered }
        return FusionSimPortfolio()
    }

    fun stability(context: Context): FusionStabilityState {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(STABILITY, null)
        return if (raw.isNullOrBlank()) FusionStabilityState() else runCatching {
            FusionStabilityState.fromJson(JSONObject(raw))
        }.getOrDefault(FusionStabilityState())
    }

    private fun saveStability(context: Context, value: FusionStabilityState) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(STABILITY, value.toJson().toString()).apply()
    }

    fun activate(context: Context, existingDeepSeekDecision: Long) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.getBoolean(ACTIVATED, false)) {
            p.edit().putBoolean(ACTIVATED, true)
                .putLong(ACTIVATION_WATERMARK, existingDeepSeekDecision.coerceAtLeast(0L)).commit()
            UnifiedResearchLog.record(context, "FUSION_SIM", "START", "Активирован без исполнения старых решений DeepSig")
        }
    }

    @Synchronized
    fun sync(context: Context, deepSeek: DeepSeekPrimaryState, now: Long = System.currentTimeMillis()): FusionSimPortfolio {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.getBoolean(ACTIVATED, false)) activate(context, deepSeek.lastSuccess)
        val current = state(context)
        val entryObservation = SharedFusionEntryObservationStore.snapshot(context, now)
        val frame = entryObservation.frame
        val market = BitpandaFusionStore.state(context)
        if (!market.fresh(now)) {
            if (frame != null && (frame.buySignal || frame.exitSignal)) {
                UnifiedResearchLog.record(context, "FUSION_SIM", "WAIT", "Flow-сигнал не исполнен: нет свежего read-only стакана Bitpanda")
            }
            return current
        }
        val tracked = if (current.inPosition) {
            current.copy(peakValueEur = max(current.peakValueEur, current.value(market.bid)))
        } else current
        if (tracked != current) save(context, tracked)

        val previousStability = stability(context)
        val lastBuyAt = if (tracked.inPosition) {
            tracked.trades.asReversed().firstOrNull { it.action == "BUY" }?.time ?: 0L
        } else 0L
        val positionAgeMillis = if (tracked.inPosition && lastBuyAt > 0L) {
            (now - lastBuyAt).coerceAtLeast(0L)
        } else Long.MAX_VALUE
        val shock = ShockReboundStore.state(context)
        val shockFresh = shock.fresh(now)
        val lastBuyReason = tracked.trades.asReversed().firstOrNull { it.action == "BUY" }?.reason.orEmpty()
        val shockEntry = tracked.inPosition && lastBuyReason.contains("SHOCK_REBOUND_ENTRY")
        val plan = FusionStabilityPolicy.evaluate(
            inPosition = tracked.inPosition,
            entryPrice = tracked.entryPrice,
            previous = previousStability,
            frame = frame,
            bid = market.bid,
            feeRate = market.feeRate,
            now = now,
            positionAgeMillis = positionAgeMillis,
            shockReady = !tracked.inPosition && shockFresh && shock.ready,
            shockFailed = shockFresh && shock.failed,
            shockEntry = shockEntry,
            entryObservation = entryObservation
        )
        if (plan.nextState != previousStability) saveStability(context, plan.nextState)
        val action = plan.action ?: return tracked

        val decisionId = max(now, tracked.lastDecisionId + 1L)
        val values = frame?.let {
            "значения мгн/5/15/20/30: ${it.instant}/${it.score5m}/${it.score15m}/${it.score20m}/${it.score30m}"
        } ?: "flow-значения неполные"
        val stopText = if (plan.activeStopPrice > 0.0) {
            String.format(java.util.Locale.US, "; активный stop %.8f", plan.activeStopPrice)
        } else ""
        val reason = "${plan.reason}; $values$stopText"
        val next = FusionSimTrader.apply(
            tracked, decisionId, action, market.bid, market.ask,
            market.feeRate, reason, now
        )
        if (next != tracked) {
            save(context, next)
            val afterTradeState = if (!tracked.inPosition && next.inPosition) {
                previousStability.copy(
                    entryStreak = 0,
                    entryCandidateAt = 0L,
                    exitStreak = 0,
                    exitArmedAt = 0L,
                    exitArmedBid = 0.0,
                    peakBid = market.bid,
                    profitDefenseArmed = false,
                    cooldownUntil = 0L
                )
            } else if (tracked.inPosition && !next.inPosition) {
                val exitPnl = next.trades.lastOrNull { it.action == "SELL" }?.pnlEur ?: 0.0
                val protective = plan.reason.startsWith("HARD_TRAILING_STOP") ||
                    plan.reason.startsWith("PROFIT_DEFENSE_STOP")
                FusionStabilityPolicy.cooldownAfterExit(
                    previous = previousStability,
                    exitPnlEur = exitPnl,
                    wasProtectiveStop = protective,
                    now = now
                )
            } else plan.nextState
            saveStability(context, afterTradeState)
            UnifiedResearchLog.record(context, "FUSION_SIM", "OK", next.decisions.last().result)
            when {
                !tracked.inPosition && next.inPosition -> UnifiedResearchLog.record(
                    context,
                    "FUSION_PRIORITY",
                    "START",
                    if (reason.contains("SHOCK_REBOUND_ENTRY")) {
                        "Виртуальный BUY исполнен по ask после подтверждённого быстрого отскока; комиссия 0,25% учтена; реальных ордеров нет"
                    } else {
                        "Виртуальный BUY исполнен по ask; комиссия 0,25% учтена; обычный вход подтверждён во времени и не ловит один зелёный тик"
                    }
                )
                tracked.inPosition && !next.inPosition -> {
                    val cooldownSeconds = ((afterTradeState.cooldownUntil - now).coerceAtLeast(0L) / 1000L)
                    UnifiedResearchLog.record(
                        context,
                        "FUSION_PRIORITY",
                        "STOP",
                        "Виртуальный EXIT исполнен по bid; повторный BUY заблокирован на ${cooldownSeconds}с anti-churn cooldown"
                    )
                }
            }
        }
        return next
    }

    fun reset(context: Context) {
        runCatching { ResearchPerformanceLedger.capture(context) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }

    fun toJson(value: FusionSimPortfolio): JSONObject = JSONObject()
        .put("cashEur", value.cashEur).put("pumpAmount", value.pumpAmount)
        .put("entryPrice", value.entryPrice).put("entryCostEur", value.entryCostEur)
        .put("lastDecisionId", value.lastDecisionId).put("totalFeesEur", value.totalFeesEur)
        .put("peakValueEur", value.peakValueEur).put("maxDrawdownPercent", value.maxDrawdownPercent)
        .put("trades", JSONArray(value.trades.map { it.toJson() }))
        .put("decisions", JSONArray(value.decisions.map { it.toJson() }))

    private fun save(context: Context, value: FusionSimPortfolio) {
        val raw = toJson(value).toString()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(PORTFOLIO, raw).putString(BACKUP, raw).commit()
    }

    private fun parse(raw: String?): FusionSimPortfolio? = if (raw.isNullOrBlank()) null else runCatching {
        val j = JSONObject(raw)
        val trades = j.optJSONArray("trades") ?: JSONArray()
        val decisions = j.optJSONArray("decisions") ?: JSONArray()
        FusionSimPortfolio(
            cashEur = j.optDouble("cashEur", 1000.0), pumpAmount = j.optDouble("pumpAmount"),
            entryPrice = j.optDouble("entryPrice"), entryCostEur = j.optDouble("entryCostEur"),
            lastDecisionId = j.optLong("lastDecisionId"), totalFeesEur = j.optDouble("totalFeesEur"),
            peakValueEur = j.optDouble("peakValueEur", 1000.0),
            maxDrawdownPercent = j.optDouble("maxDrawdownPercent"),
            trades = (0 until trades.length()).mapNotNull { trades.optJSONObject(it)?.let(FusionSimTrade::fromJson) },
            decisions = (0 until decisions.length()).mapNotNull { decisions.optJSONObject(it)?.let(FusionSimDecision::fromJson) }
        )
    }.getOrNull()
}
