package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max

/**
 * V5.21 Pump Machine.
 *
 * Paper-only participant replacing the old DEEPSIG paper execution slot.
 * Entry timing and normal system exits intentionally mirror Fusion flow/stability rules.
 * Risk contract is intentionally different and simple:
 *   - hard +3.00% NET per-trade take profit;
 *   - hard -1.50% NET per-trade stop loss;
 *   - a confirmed Fusion system exit or failed shock rebound may exit earlier.
 *
 * Net means after the simulated buy fee, sell fee and executable Bitpanda bid/ask spread.
 * No real order path exists in this class.
 */
data class PumpMachineSyncResult(
    val portfolio: FusionSimPortfolio,
    val status: String,
    val tradeNetPercent: Double
)

data class PumpMachineDecision(
    val action: String?,
    val nextState: FusionStabilityState,
    val reason: String,
    val tradeNetPercent: Double
)

object PumpMachinePolicy {
    const val TAKE_PROFIT_NET_PERCENT = 3.00
    const val STOP_LOSS_NET_PERCENT = -1.50

    fun tradeNetPercent(
        portfolio: FusionSimPortfolio,
        bid: Double,
        feeRate: Double
    ): Double {
        if (!portfolio.inPosition || portfolio.entryCostEur <= 0.0 || bid <= 0.0) return 0.0
        val fee = feeRate.coerceIn(0.0, 0.02)
        val netExit = portfolio.pumpAmount * bid * (1.0 - fee)
        return (netExit / portfolio.entryCostEur - 1.0) * 100.0
    }

    fun netLiquidationValue(
        portfolio: FusionSimPortfolio,
        bid: Double,
        feeRate: Double
    ): Double {
        if (!portfolio.inPosition) return portfolio.cashEur
        if (bid <= 0.0) return portfolio.cashEur + portfolio.pumpAmount * max(portfolio.entryPrice, 0.0)
        val fee = feeRate.coerceIn(0.0, 0.02)
        return portfolio.cashEur + portfolio.pumpAmount * bid * (1.0 - fee)
    }

    fun evaluate(
        portfolio: FusionSimPortfolio,
        previous: FusionStabilityState,
        frame: FusionFlowFrame?,
        bid: Double,
        feeRate: Double,
        now: Long,
        shockReady: Boolean,
        shockFailed: Boolean,
        shockEntry: Boolean,
        positionAgeMillis: Long
    ): PumpMachineDecision {
        if (bid <= 0.0) {
            return PumpMachineDecision(null, previous, "Нет свежего bid для Pump Machine", 0.0)
        }

        if (!portfolio.inPosition) {
            if (previous.cooldownUntil > now) {
                val leftSeconds = ((previous.cooldownUntil - now + 999L) / 1000L).coerceAtLeast(1L)
                return PumpMachineDecision(
                    null,
                    previous.copy(
                        entryStreak = 0,
                        entryCandidateAt = 0L,
                        exitStreak = 0,
                        exitArmedAt = 0L,
                        exitArmedBid = 0.0,
                        peakBid = 0.0,
                        profitDefenseArmed = false
                    ),
                    "COOLDOWN: Pump Machine ждёт ещё ${leftSeconds}с после предыдущего выхода",
                    0.0
                )
            }

            // Same fast shock lane as Fusion V5.19. ShockReboundStore itself already requires
            // two confirmed fast observations, so this does not buy a falling knife.
            if (shockReady) {
                return PumpMachineDecision(
                    "BUY",
                    previous.copy(
                        entryStreak = 0,
                        entryCandidateAt = 0L,
                        exitStreak = 0,
                        exitArmedAt = 0L,
                        exitArmedBid = 0.0,
                        peakBid = 0.0,
                        profitDefenseArmed = false,
                        cooldownUntil = 0L
                    ),
                    "SHOCK_REBOUND_ENTRY: подтверждённый быстрый отскок по тому же контуру, что Fusion",
                    0.0
                )
            }

            val buy = frame?.buySignal == true
            if (!buy) {
                return PumpMachineDecision(
                    null,
                    previous.copy(
                        entryStreak = 0,
                        entryCandidateAt = 0L,
                        exitStreak = 0,
                        exitArmedAt = 0L,
                        exitArmedBid = 0.0,
                        peakBid = 0.0,
                        profitDefenseArmed = false,
                        cooldownUntil = 0L
                    ),
                    "WAIT: условия Fusion BUY сейчас/5/15/30 ещё не собраны",
                    0.0
                )
            }

            val candidateAt = if (previous.entryStreak > 0 && previous.entryCandidateAt > 0L) {
                previous.entryCandidateAt
            } else now
            val streak = (previous.entryStreak + 1)
                .coerceAtMost(FusionStabilityPolicy.ENTRY_CONFIRMATIONS)
            val confirmedByTime = now - candidateAt >= FusionStabilityPolicy.ENTRY_CONFIRM_MIN_MILLIS
            val next = previous.copy(
                entryStreak = streak,
                entryCandidateAt = candidateAt,
                exitStreak = 0,
                exitArmedAt = 0L,
                exitArmedBid = 0.0,
                peakBid = 0.0,
                profitDefenseArmed = false,
                cooldownUntil = 0L
            )
            return if (
                streak >= FusionStabilityPolicy.ENTRY_CONFIRMATIONS && confirmedByTime
            ) {
                PumpMachineDecision(
                    "BUY",
                    next,
                    "ENTRY_CONFIRMED: тот же Fusion-вход — сейчас/5/15/30 подтверждены минимум двумя наблюдениями и 60с",
                    0.0
                )
            } else {
                PumpMachineDecision(
                    null,
                    next,
                    if (frame?.strongBuy == true) {
                        "ENTRY_ARMED_STRONG: сильный Fusion BUY, но Pump Machine всё равно ждёт подтверждение во времени"
                    } else {
                        "ENTRY_ARMED: положительный Fusion-поток подтверждается перед BUY"
                    },
                    0.0
                )
            }
        }

        val tradeNet = tradeNetPercent(portfolio, bid, feeRate)
        val peak = max(max(previous.peakBid, bid), portfolio.entryPrice)
        val base = previous.copy(
            entryStreak = 0,
            entryCandidateAt = 0L,
            peakBid = peak,
            // Pump Machine deliberately has no Fusion trailing/profit-defense.
            // Its hard risk limits are the net +3.00 / -1.50 contract below.
            profitDefenseArmed = false,
            cooldownUntil = 0L
        )

        if (tradeNet >= TAKE_PROFIT_NET_PERCENT) {
            return PumpMachineDecision(
                "EXIT",
                base,
                "TAKE_PROFIT_3_NET: чистая прибыль сделки достигла ${fmt(tradeNet)}%; цель Pump Machine +3,00% выполнена",
                tradeNet
            )
        }

        if (tradeNet <= STOP_LOSS_NET_PERCENT) {
            return PumpMachineDecision(
                "EXIT",
                base,
                "STOP_LOSS_1_5_NET: чистый результат сделки ${fmt(tradeNet)}%; жёсткий лимит Pump Machine −1,50%",
                tradeNet
            )
        }

        if (
            shockEntry && shockFailed &&
            positionAgeMillis >= FusionStabilityPolicy.SHOCK_FAILURE_MIN_AGE_MILLIS
        ) {
            return PumpMachineDecision(
                "EXIT",
                base,
                "SHOCK_REBOUND_FAILED: быстрый отскок сорвался; выходим по тому же аварийному правилу Fusion",
                tradeNet
            )
        }

        // From here down this is the Fusion SYSTEM_EXIT state machine without Fusion's
        // 1.75%/1.00% trailing. The user's +3 / -1.5 contract replaces those two stops.
        val rawExit = frame?.meaningfulExitSignal == true
        val severeExit = frame?.severeExitSignal == true
        val recovered = frame?.exitRecovery == true
        val armedStillFresh = previous.exitArmed &&
            now - previous.exitArmedAt in 0..FusionStabilityPolicy.EXIT_ARM_TTL_MILLIS

        if (recovered) {
            return PumpMachineDecision(
                null,
                base.copy(exitStreak = 0, exitArmedAt = 0L, exitArmedBid = 0.0),
                "HOLD: 5/15/20 восстановились; системный EXIT снят",
                tradeNet
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
        val next = base.copy(
            exitStreak = streak,
            exitArmedAt = armedAt,
            exitArmedBid = armedBid
        )

        val actualDecline =
            downFromArm >= FusionStabilityPolicy.REQUIRED_DOWN_FROM_ARM_PERCENT ||
                pullbackFromPeak >= FusionStabilityPolicy.REQUIRED_PULLBACK_FROM_PEAK_PERCENT
        val exitConfirmed = severeExit || streak >= 2
        val holdLimit = if (shockEntry) {
            FusionStabilityPolicy.SHOCK_MIN_HOLD_MILLIS
        } else {
            FusionStabilityPolicy.MIN_HOLD_MILLIS
        }
        val holdLockActive = positionAgeMillis < holdLimit

        if (armed && actualDecline && exitConfirmed && (!holdLockActive || severeExit)) {
            return PumpMachineDecision(
                "EXIT",
                next,
                "SYSTEM_EXIT: Fusion сейчас/5/15/20 подтвердили давление вниз и bid реально пошёл вниз " +
                    "(от arm ${fmt(downFromArm)}%, от пика ${fmt(pullbackFromPeak)}%)",
                tradeNet
            )
        }
        if (rawExit && holdLockActive && !severeExit) {
            val left = ((holdLimit - positionAgeMillis).coerceAtLeast(0L) / 1000L)
            return PumpMachineDecision(
                null,
                next,
                "HOLD_LOCK: системный EXIT пока ждёт; минимальное удержание ещё ${left}с; +3% TP и −1,5% SL действуют сразу",
                tradeNet
            )
        }
        if (rawExit) {
            return PumpMachineDecision(
                null,
                next,
                if (exitConfirmed) {
                    "EXIT_ARMED: Fusion-выход подтверждён, ждём фактическое снижение bid"
                } else {
                    "EXIT_ARMED: первое устойчивое отрицательное Fusion-наблюдение; ждём второе"
                },
                tradeNet
            )
        }
        if (armedStillFresh) {
            return PumpMachineDecision(
                null,
                next,
                "EXIT_ARMED: окно выхода остаётся активным; боковик сам по себе продажу не запускает",
                tradeNet
            )
        }

        return PumpMachineDecision(
            null,
            base.copy(exitStreak = 0, exitArmedAt = 0L, exitArmedBid = 0.0),
            "HOLD: Pump Machine ждёт либо Fusion EXIT, либо +3,00% net TP, либо −1,50% net SL",
            tradeNet
        )
    }

    private fun fmt(value: Double): String =
        String.format(java.util.Locale.GERMANY, "%+.2f", value)
}

object PumpMachineStore {
    private const val PREFS = "pump_machine_paper_v521"
    private const val PORTFOLIO = "portfolio"
    private const val STABILITY = "stability"
    private const val LAST_STATUS = "last_status"
    private const val MAX_TRADES = 5_000
    private const val MAX_DECISIONS = 9_000

    fun state(context: Context): FusionSimPortfolio {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(PORTFOLIO, null)
        return runCatching { portfolioFromJson(JSONObject(raw.orEmpty())) }
            .getOrDefault(FusionSimPortfolio())
    }

    fun lastStatus(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(LAST_STATUS, "PUMP MACHINE • ждём первый Fusion-сигнал")
            .orEmpty()

    fun toJson(value: FusionSimPortfolio): JSONObject = portfolioToJson(value)

    fun netValue(context: Context, now: Long = System.currentTimeMillis()): Double {
        val portfolio = state(context)
        val market = BitpandaFusionStore.state(context)
        val bid = market.bid.takeIf { market.fresh(now) } ?: portfolio.entryPrice
        return PumpMachinePolicy.netLiquidationValue(portfolio, bid, market.feeRate)
    }

    fun tradeNetPercent(context: Context, now: Long = System.currentTimeMillis()): Double {
        val portfolio = state(context)
        if (!portfolio.inPosition) return 0.0
        val market = BitpandaFusionStore.state(context)
        val bid = market.bid.takeIf { market.fresh(now) } ?: return 0.0
        return PumpMachinePolicy.tradeNetPercent(portfolio, bid, market.feeRate)
    }

    @Synchronized
    fun sync(context: Context, now: Long = System.currentTimeMillis()): PumpMachineSyncResult {
        val market = BitpandaFusionStore.state(context)
        val current = state(context)
        val previousStability = stability(context)
        if (!market.fresh(now) || market.bid <= 0.0 || market.ask <= 0.0) {
            val status = "WAIT: Pump Machine ждёт свежий read-only Bitpanda bid/ask"
            saveStatus(context, status)
            return PumpMachineSyncResult(current, status, 0.0)
        }

        val breathing = LiveMarketBreathingStore.snapshot(context, now)
        val frame = FusionFlowPolicy.frame(breathing)
        val shock = ShockReboundStore.state(context)
        val shockFresh = shock.fresh(now)
        val lastBuy = current.trades.asReversed().firstOrNull { it.action == "BUY" }
        val shockEntry = current.inPosition && lastBuy?.reason.orEmpty().contains("SHOCK_REBOUND_ENTRY")
        val positionAge = if (current.inPosition && lastBuy != null) {
            (now - lastBuy.time).coerceAtLeast(0L)
        } else Long.MAX_VALUE

        val plan = PumpMachinePolicy.evaluate(
            portfolio = current,
            previous = previousStability,
            frame = frame,
            bid = market.bid,
            feeRate = market.feeRate,
            now = now,
            shockReady = !current.inPosition && shockFresh && shock.ready,
            shockFailed = shockFresh && shock.failed,
            shockEntry = shockEntry,
            positionAgeMillis = positionAge
        )

        val marked = mark(current, market.bid, market.feeRate)
        if (plan.action == null) {
            savePortfolio(context, marked)
            saveStability(context, plan.nextState)
            saveStatus(context, plan.reason)
            return PumpMachineSyncResult(marked, plan.reason, plan.tradeNetPercent)
        }

        val decisionId = now
        return when (plan.action) {
            "BUY" -> {
                if (marked.inPosition || marked.cashEur <= 0.01) {
                    saveStability(context, plan.nextState)
                    val status = "WAIT: Pump Machine уже находится в позиции"
                    saveStatus(context, status)
                    PumpMachineSyncResult(marked, status, 0.0)
                } else {
                    val allocation = marked.cashEur
                    val buyFee = allocation * market.feeRate
                    val amount = (allocation - buyFee) / market.ask
                    val trade = FusionSimTrade(
                        time = now,
                        decisionId = decisionId,
                        action = "BUY",
                        price = market.ask,
                        amount = amount,
                        feeEur = buyFee,
                        pnlEur = 0.0,
                        reason = plan.reason
                    )
                    val decision = FusionSimDecision(
                        time = now,
                        decisionId = decisionId,
                        requestedAction = "BUY",
                        result = "PUMP MACHINE BUY • paper-only",
                        venuePrice = market.ask,
                        reason = plan.reason
                    )
                    val next = marked.copy(
                        cashEur = 0.0,
                        pumpAmount = amount,
                        entryPrice = market.ask,
                        entryCostEur = allocation,
                        lastDecisionId = decisionId,
                        totalFeesEur = marked.totalFeesEur + buyFee,
                        trades = (marked.trades + trade).takeLast(MAX_TRADES),
                        decisions = (marked.decisions + decision).takeLast(MAX_DECISIONS)
                    )
                    val entryState = plan.nextState.copy(
                        entryStreak = 0,
                        entryCandidateAt = 0L,
                        exitStreak = 0,
                        exitArmedAt = 0L,
                        exitArmedBid = 0.0,
                        peakBid = market.bid,
                        profitDefenseArmed = false,
                        cooldownUntil = 0L
                    )
                    savePortfolio(context, next)
                    saveStability(context, entryState)
                    val status = "BUY: ${plan.reason} • TP +3,00% net • SL −1,50% net"
                    saveStatus(context, status)
                    UnifiedResearchLog.record(context, "PUMP_MACHINE", "BUY", status, now)
                    PumpMachineSyncResult(next, status, 0.0)
                }
            }
            "EXIT" -> {
                if (!marked.inPosition) {
                    saveStability(context, plan.nextState)
                    val status = "WAIT: Pump Machine уже вне позиции"
                    saveStatus(context, status)
                    PumpMachineSyncResult(marked, status, 0.0)
                } else {
                    val soldAmount = marked.pumpAmount
                    val gross = soldAmount * market.bid
                    val sellFee = gross * market.feeRate
                    val net = gross - sellFee
                    val pnl = net - marked.entryCostEur
                    val tradeNet = if (marked.entryCostEur > 0.0) {
                        pnl / marked.entryCostEur * 100.0
                    } else 0.0
                    val trade = FusionSimTrade(
                        time = now,
                        decisionId = decisionId,
                        action = "SELL",
                        price = market.bid,
                        amount = soldAmount,
                        feeEur = sellFee,
                        pnlEur = pnl,
                        reason = plan.reason
                    )
                    val decision = FusionSimDecision(
                        time = now,
                        decisionId = decisionId,
                        requestedAction = "EXIT",
                        result = "PUMP MACHINE SELL • ${String.format(java.util.Locale.GERMANY, "%+.2f%% net", tradeNet)}",
                        venuePrice = market.bid,
                        reason = plan.reason
                    )
                    val nextCash = marked.cashEur + net
                    val nextPeak = max(marked.peakValueEur, nextCash)
                    val drawdown = if (nextPeak > 0.0) {
                        ((nextPeak - nextCash) / nextPeak * 100.0).coerceAtLeast(0.0)
                    } else 0.0
                    val next = marked.copy(
                        cashEur = nextCash,
                        pumpAmount = 0.0,
                        entryPrice = 0.0,
                        entryCostEur = 0.0,
                        lastDecisionId = decisionId,
                        totalFeesEur = marked.totalFeesEur + sellFee,
                        peakValueEur = nextPeak,
                        maxDrawdownPercent = max(marked.maxDrawdownPercent, drawdown),
                        trades = (marked.trades + trade).takeLast(MAX_TRADES),
                        decisions = (marked.decisions + decision).takeLast(MAX_DECISIONS)
                    )
                    val protectiveStop = plan.reason.startsWith("STOP_LOSS_1_5_NET")
                    val exitState = FusionStabilityPolicy.cooldownAfterExit(
                        previous = previousStability,
                        exitPnlEur = pnl,
                        wasProtectiveStop = protectiveStop,
                        now = now
                    )
                    savePortfolio(context, next)
                    saveStability(context, exitState)
                    val status = "SELL ${String.format(java.util.Locale.GERMANY, "%+.2f%% net", tradeNet)}: ${plan.reason}"
                    saveStatus(context, status)
                    UnifiedResearchLog.record(context, "PUMP_MACHINE", "SELL", status, now)
                    PumpMachineSyncResult(next, status, tradeNet)
                }
            }
            else -> PumpMachineSyncResult(marked, plan.reason, plan.tradeNetPercent)
        }
    }

    private fun mark(
        value: FusionSimPortfolio,
        bid: Double,
        feeRate: Double
    ): FusionSimPortfolio {
        val liquidation = PumpMachinePolicy.netLiquidationValue(value, bid, feeRate)
        val peak = max(value.peakValueEur, liquidation)
        val drawdown = if (peak > 0.0) {
            ((peak - liquidation) / peak * 100.0).coerceAtLeast(0.0)
        } else 0.0
        return value.copy(
            peakValueEur = peak,
            maxDrawdownPercent = max(value.maxDrawdownPercent, drawdown)
        )
    }

    private fun stability(context: Context): FusionStabilityState {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(STABILITY, null)
        return runCatching { FusionStabilityState.fromJson(JSONObject(raw.orEmpty())) }
            .getOrDefault(FusionStabilityState())
    }

    private fun savePortfolio(context: Context, value: FusionSimPortfolio) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(PORTFOLIO, portfolioToJson(value).toString())
            .apply()
    }

    private fun saveStability(context: Context, value: FusionStabilityState) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(STABILITY, value.toJson().toString())
            .apply()
    }

    private fun saveStatus(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(LAST_STATUS, value.take(1200))
            .apply()
    }

    private fun portfolioToJson(value: FusionSimPortfolio): JSONObject = JSONObject()
        .put("cashEur", value.cashEur)
        .put("pumpAmount", value.pumpAmount)
        .put("entryPrice", value.entryPrice)
        .put("entryCostEur", value.entryCostEur)
        .put("lastDecisionId", value.lastDecisionId)
        .put("totalFeesEur", value.totalFeesEur)
        .put("peakValueEur", value.peakValueEur)
        .put("maxDrawdownPercent", value.maxDrawdownPercent)
        .put("trades", JSONArray(value.trades.map { it.toJson() }))
        .put("decisions", JSONArray(value.decisions.map { it.toJson() }))

    private fun portfolioFromJson(value: JSONObject): FusionSimPortfolio {
        val tradesJson = value.optJSONArray("trades") ?: JSONArray()
        val decisionsJson = value.optJSONArray("decisions") ?: JSONArray()
        val trades = (0 until tradesJson.length()).mapNotNull {
            tradesJson.optJSONObject(it)?.let(FusionSimTrade::fromJson)
        }
        val decisions = (0 until decisionsJson.length()).mapNotNull {
            decisionsJson.optJSONObject(it)?.let(FusionSimDecision::fromJson)
        }
        return FusionSimPortfolio(
            cashEur = value.optDouble("cashEur", FusionSimPortfolio.START_BALANCE),
            pumpAmount = value.optDouble("pumpAmount"),
            entryPrice = value.optDouble("entryPrice"),
            entryCostEur = value.optDouble("entryCostEur"),
            lastDecisionId = value.optLong("lastDecisionId"),
            totalFeesEur = value.optDouble("totalFeesEur"),
            peakValueEur = value.optDouble("peakValueEur", FusionSimPortfolio.START_BALANCE),
            maxDrawdownPercent = value.optDouble("maxDrawdownPercent"),
            trades = trades.takeLast(MAX_TRADES),
            decisions = decisions.takeLast(MAX_DECISIONS)
        )
    }
}
