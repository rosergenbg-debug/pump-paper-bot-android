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
    const val PRIORITY_INTERVAL_MILLIS = 3L * 60L * 1000L
    const val NORMAL_INTERVAL_MILLIS = 120_000L

    fun plan(portfolio: FusionSimPortfolio): FusionPriorityPlan = if (portfolio.inPosition) {
        FusionPriorityPlan(
            active = true,
            forcePro = false,
            intervalMillis = PRIORITY_INTERVAL_MILLIS,
            label = "FUSION POSITION • ЛОКАЛЬНЫЙ КОНТРОЛЬ • DEEPSIG FLASH • 3 МИН"
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

/** V5.11 keeps the exact upper-bar hypotheses but adds execution smoothing downstream. */
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
                "FLOW V5.11: верхние сглаженные столбики сейчас/5м/15м/30м одновременно выше нуля",
                frame.instant, frame.score5m, frame.score15m, frame.score20m, frame.score30m
            )
            inPosition && frame.exitSignal -> FusionFlowDecision(
                "EXIT",
                "FLOW V5.11: сейчас/5м/15м/20м ниже нуля; выход сначала вооружается и ждёт фактическое движение цены вниз",
                frame.instant, frame.score5m, frame.score15m, frame.score20m, frame.score30m
            )
            else -> null
        }
    }
}

data class FusionStabilityState(
    val entryStreak: Int = 0,
    val exitStreak: Int = 0,
    val exitArmedAt: Long = 0L,
    val exitArmedBid: Double = 0.0,
    val peakBid: Double = 0.0
) {
    val exitArmed: Boolean get() = exitArmedAt > 0L && exitArmedBid > 0.0

    fun toJson(): JSONObject = JSONObject()
        .put("entryStreak", entryStreak)
        .put("exitStreak", exitStreak)
        .put("exitArmedAt", exitArmedAt)
        .put("exitArmedBid", exitArmedBid)
        .put("peakBid", peakBid)

    companion object {
        fun fromJson(j: JSONObject) = FusionStabilityState(
            entryStreak = j.optInt("entryStreak"),
            exitStreak = j.optInt("exitStreak"),
            exitArmedAt = j.optLong("exitArmedAt"),
            exitArmedBid = j.optDouble("exitArmedBid"),
            peakBid = j.optDouble("peakBid")
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
    const val INITIAL_STOP_PERCENT = 1.50
    const val PROFIT_LOCK_TRIGGER_PERCENT = 0.60
    const val TRAIL_FROM_PEAK_PERCENT = 0.60
    const val MIN_NET_LOCK_PERCENT = 0.02

    fun breakEvenGrossPercent(feeRate: Double): Double {
        val fee = feeRate.coerceIn(0.0, 0.02)
        val grossFactor = 1.0 / (1.0 - fee).pow(2.0)
        return (grossFactor - 1.0) * 100.0
    }

    fun activeStopPrice(
        entryPrice: Double,
        peakBid: Double,
        feeRate: Double
    ): Double {
        if (entryPrice <= 0.0) return 0.0
        val initial = entryPrice * (1.0 - INITIAL_STOP_PERCENT / 100.0)
        val peak = max(entryPrice, peakBid)
        val peakGainPercent = (peak / entryPrice - 1.0) * 100.0
        if (peakGainPercent < PROFIT_LOCK_TRIGGER_PERCENT) return initial

        val profitFloorPercent = breakEvenGrossPercent(feeRate) + MIN_NET_LOCK_PERCENT
        val profitFloor = entryPrice * (1.0 + profitFloorPercent / 100.0)
        val trailing = peak * (1.0 - TRAIL_FROM_PEAK_PERCENT / 100.0)
        return max(initial, max(profitFloor, trailing))
    }
}

/**
 * V5.11 anti-chatter layer.
 * - Entry still requires current/5/15/30 > 0. A clearly strong alignment enters immediately;
 *   a weak just-above-zero alignment must survive two evaluations.
 * - Exit still starts from current/5/15/20 < 0, but it is armed first. If price is sideways or
 *   rising, the position is held. A later real decline executes the exit.
 * - Virtual protective/trailing stop bypasses the delay on a genuine sharp drop.
 */
object FusionStabilityPolicy {
    const val ENTRY_CONFIRMATIONS = 2
    const val EXIT_ARM_TTL_MILLIS = 6L * 60L * 1000L
    const val REQUIRED_DOWN_FROM_ARM_PERCENT = 0.12
    const val REQUIRED_PULLBACK_FROM_PEAK_PERCENT = 0.35

    fun evaluate(
        inPosition: Boolean,
        entryPrice: Double,
        previous: FusionStabilityState,
        frame: FusionFlowFrame?,
        bid: Double,
        feeRate: Double,
        now: Long
    ): FusionStabilityDecision {
        if (bid <= 0.0) return FusionStabilityDecision(null, previous, 0.0, "Нет свежего bid")

        if (!inPosition) {
            val buy = frame?.buySignal == true
            val streak = if (buy) (previous.entryStreak + 1).coerceAtMost(ENTRY_CONFIRMATIONS) else 0
            val next = FusionStabilityState(entryStreak = streak)
            return when {
                frame?.strongBuy == true -> FusionStabilityDecision(
                    "BUY", next, 0.0,
                    "Сильное согласование сейчас/5/15/30 прошло зону шума — BUY без лишней задержки"
                )
                buy && streak >= ENTRY_CONFIRMATIONS -> FusionStabilityDecision(
                    "BUY", next, 0.0,
                    "Слабое пересечение нуля удержалось два цикла — BUY подтверждён"
                )
                buy -> FusionStabilityDecision(
                    null, next, 0.0,
                    "BUY-кандидат вооружён: ждём ещё одно подтверждение, чтобы не реагировать на микрофлуктуацию"
                )
                else -> FusionStabilityDecision(null, next, 0.0, "Условия BUY ещё не собраны")
            }
        }

        val peak = max(max(previous.peakBid, bid), entryPrice)
        val activeStop = FusionRiskPolicy.activeStopPrice(entryPrice, peak, feeRate)
        if (activeStop > 0.0 && bid <= activeStop) {
            return FusionStabilityDecision(
                "EXIT",
                previous.copy(entryStreak = 0, peakBid = peak),
                activeStop,
                "Защитный виртуальный STOP: bid дошёл до динамического порога"
            )
        }

        val rawExit = frame?.exitSignal == true
        val recovered = frame?.exitRecovery == true
        val armedStillFresh = previous.exitArmed && now - previous.exitArmedAt in 0..EXIT_ARM_TTL_MILLIS

        if (recovered) {
            return FusionStabilityDecision(
                null,
                FusionStabilityState(peakBid = peak),
                activeStop,
                "Выход снят: 5/15/20 минут показали заметное восстановление"
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
        val streak = if (rawExit) (previous.exitStreak + 1).coerceAtMost(99) else if (armedStillFresh) previous.exitStreak else 0
        val armed = armedAt > 0L && armedBid > 0.0
        val downFromArm = if (armed && armedBid > 0.0) {
            ((armedBid - bid) / armedBid * 100.0).coerceAtLeast(0.0)
        } else 0.0
        val pullbackFromPeak = if (peak > 0.0) {
            ((peak - bid) / peak * 100.0).coerceAtLeast(0.0)
        } else 0.0
        val next = FusionStabilityState(
            entryStreak = 0,
            exitStreak = streak,
            exitArmedAt = armedAt,
            exitArmedBid = armedBid,
            peakBid = peak
        )

        val actualDecline = downFromArm >= REQUIRED_DOWN_FROM_ARM_PERCENT ||
            pullbackFromPeak >= REQUIRED_PULLBACK_FROM_PEAK_PERCENT
        if (armed && actualDecline && (rawExit || armedStillFresh)) {
            return FusionStabilityDecision(
                "EXIT", next, activeStop,
                "EXIT подтверждён ценой: сигнал был вооружён, затем bid реально пошёл вниз " +
                    String.format(java.util.Locale.US, "(от arm %.2f%%, от пика %.2f%%)", downFromArm, pullbackFromPeak)
            )
        }
        if (rawExit) {
            return FusionStabilityDecision(
                null, next, activeStop,
                "EXIT вооружён, но цена пока не падает — держим позицию и ждём подтверждение движения вниз"
            )
        }
        if (armedStillFresh) {
            return FusionStabilityDecision(
                null, next, activeStop,
                "EXIT остаётся вооружён на короткое окно; боковик сам по себе продажу не запускает"
            )
        }
        return FusionStabilityDecision(
            null,
            FusionStabilityState(peakBid = peak),
            activeStop,
            "Позиция удерживается; условий выхода нет"
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
        val breathing = LiveMarketBreathingStore.snapshot(context, now)
        val frame = FusionFlowPolicy.frame(breathing)
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
        val plan = FusionStabilityPolicy.evaluate(
            inPosition = tracked.inPosition,
            entryPrice = tracked.entryPrice,
            previous = previousStability,
            frame = frame,
            bid = market.bid,
            feeRate = market.feeRate,
            now = now
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
                FusionStabilityState(peakBid = market.bid)
            } else if (tracked.inPosition && !next.inPosition) {
                FusionStabilityState()
            } else plan.nextState
            saveStability(context, afterTradeState)
            UnifiedResearchLog.record(context, "FUSION_SIM", "OK", next.decisions.last().result)
            when {
                !tracked.inPosition && next.inPosition -> UnifiedResearchLog.record(
                    context,
                    "FUSION_PRIORITY",
                    "START",
                    "Виртуальный BUY исполнен по ask; комиссия 0,25% учтена; локальный контроль остаётся непрерывным, DeepSig опрашивается каждые 3 минуты"
                )
                tracked.inPosition && !next.inPosition -> UnifiedResearchLog.record(
                    context,
                    "FUSION_PRIORITY",
                    "STOP",
                    "Виртуальный EXIT исполнен по bid; контроль Fusion возвращён в обычный режим"
                )
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