package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * V6.5 cost model shared by the four T32 experiments.
 * Fee is charged on executed notional on BUY and SELL: 0.21% per side.
 */
internal object T32CostPolicyV650 {
    const val FEE_RATE = 0.0021
    const val STOP_NET_PERCENT = -0.80
    const val MAX_HOLD_MILLIS = 90L * 60L * 1_000L

    data class BuyFill(val coins: Double, val feeEur: Double, val notionalEur: Double)

    fun buyAllCash(cashEur: Double, ask: Double): BuyFill {
        if (cashEur <= 0.0 || ask <= 0.0) return BuyFill(0.0, 0.0, 0.0)
        val notional = cashEur / (1.0 + FEE_RATE)
        val fee = notional * FEE_RATE
        return BuyFill(notional / ask, fee, notional)
    }

    fun sellFee(coins: Double, bid: Double): Double = (coins * bid).coerceAtLeast(0.0) * FEE_RATE

    fun netPercent(entryPrice: Double, exitPrice: Double): Double {
        if (entryPrice <= 0.0 || exitPrice <= 0.0) return 0.0
        return ((exitPrice * (1.0 - FEE_RATE)) / (entryPrice * (1.0 + FEE_RATE)) - 1.0) * 100.0
    }

    fun targetPrice(entryPrice: Double, targetNetPercent: Double): Double {
        if (entryPrice <= 0.0) return 0.0
        val target = 1.0 + targetNetPercent / 100.0
        return entryPrice * (1.0 + FEE_RATE) * target / (1.0 - FEE_RATE)
    }

    fun entryCost(coins: Double, entryPrice: Double): Double = coins * entryPrice * (1.0 + FEE_RATE)
}

internal object HumanFactorAlertPolicyV650 {
    const val REPEAT_MILLIS = 60_000L
    fun shouldRing(pending: Boolean, lastAlertAt: Long, now: Long): Boolean =
        pending && (lastAlertAt <= 0L || now - lastAlertAt >= REPEAT_MILLIS)
}

private class T32ProfitVariantEngineV650(
    private val prefsName: String,
    private val agent: String,
    private val targetNetPercent: Double
) {
    private val key = "state"

    fun state(context: Context): HumanFactorState = runCatching {
        val raw = context.getSharedPreferences(prefsName, 0).getString(key, "{}") ?: "{}"
        stateFromJson(JSONObject(raw))
    }.getOrDefault(HumanFactorState())

    private fun stateFromJson(j: JSONObject): HumanFactorState = HumanFactorState(
        cash = j.optDouble("cash", 1000.0),
        coins = j.optDouble("coins"),
        entryPrice = j.optDouble("entryPrice"),
        entryAt = j.optLong("entryAt"),
        targetVwap = j.optDouble("targetVwap"),
        targetPrice = j.optDouble("targetPrice"),
        readiness = j.optInt("readiness"),
        pending = false,
        candidateId = 0L,
        reason = j.optString("reason", "Ожидание T32"),
        lastAlertBand = 0,
        lastAlertAt = 0L,
        updatedAt = j.optLong("updatedAt"),
        trades = (j.optJSONArray("trades") ?: JSONArray()).let { array ->
            (0 until array.length()).map { HumanFactorTrade.from(array.getJSONObject(it)) }
        }
    )

    private fun save(context: Context, state: HumanFactorState) {
        val j = JSONObject()
            .put("cash", state.cash)
            .put("coins", state.coins)
            .put("entryPrice", state.entryPrice)
            .put("entryAt", state.entryAt)
            .put("targetVwap", state.targetVwap)
            .put("targetPrice", state.targetPrice)
            .put("readiness", state.readiness)
            .put("reason", state.reason)
            .put("updatedAt", state.updatedAt)
            .put("trades", JSONArray(state.trades.takeLast(300).map { it.json() }))
        context.getSharedPreferences(prefsName, 0).edit().putString(key, j.toString()).commit()
    }

    @Synchronized
    fun sync(context: Context, now: Long = System.currentTimeMillis()): HumanFactorState {
        var state = state(context)
        val market = BitpandaFusionStore.state(context)
        val fallbackPrice = PaperExecutionPolicy.displayPrice(PumpBotEngine.snapshot(context), now)
        val bid = market.bid.takeIf { market.fresh(now) } ?: fallbackPrice

        if (state.inPosition) {
            val target = state.targetPrice.takeIf { it > 0.0 }
                ?: T32CostPolicyV650.targetPrice(state.entryPrice, targetNetPercent)
            val net = T32CostPolicyV650.netPercent(state.entryPrice, bid)
            val hitTarget = bid >= target
            val hitStop = net <= T32CostPolicyV650.STOP_NET_PERCENT
            val hitTime = now - state.entryAt >= T32CostPolicyV650.MAX_HOLD_MILLIS
            if (hitTarget || hitStop || hitTime) {
                val gross = state.coins * bid
                val fee = T32CostPolicyV650.sellFee(state.coins, bid)
                val pnl = gross - fee - T32CostPolicyV650.entryCost(state.coins, state.entryPrice)
                val exitReason = when {
                    hitTarget -> String.format(Locale.GERMANY, "TP +%.1f%% NET", targetNetPercent)
                    hitStop -> "STOP -0,80% NET"
                    else -> "90 МИН"
                }
                state = state.copy(
                    cash = gross - fee,
                    coins = 0.0,
                    entryPrice = 0.0,
                    entryAt = 0L,
                    targetVwap = 0.0,
                    targetPrice = 0.0,
                    readiness = 0,
                    reason = "AUTO EXIT • $exitReason",
                    updatedAt = now,
                    trades = state.trades + HumanFactorTrade(
                        now, "SELL", bid, state.coins, fee, pnl,
                        "T32_${targetNetPercent}_NET;$exitReason"
                    )
                )
                save(context, state)
                UnifiedResearchLog.record(context, agent, "SELL", "price=$bid; net=$net; pnlEur=$pnl; $exitReason", now)
                return state
            }
            val marked = state.copy(
                targetPrice = target,
                reason = String.format(
                    Locale.GERMANY,
                    "ПОЗИЦИЯ • NET %+.2f%% • TP €%.8f (+%.1f%% NET)",
                    net, target, targetNetPercent
                ),
                updatedAt = now
            )
            save(context, marked)
            UnifiedResearchLog.record(context, agent, "IN_POSITION", marked.reason, now)
            return marked
        }

        val (score, vwap, reason) = HumanFactorVwapPolicy.evaluate(
            ChartSpeedStore.candles(context, ChartInterval.ONE_MINUTE)
        )
        if (score >= 100) {
            val ask = market.ask.takeIf { market.fresh(now) }
            if (ask != null) {
                val fill = T32CostPolicyV650.buyAllCash(state.cash, ask)
                val target = T32CostPolicyV650.targetPrice(ask, targetNetPercent)
                state = state.copy(
                    cash = 0.0,
                    coins = fill.coins,
                    entryPrice = ask,
                    entryAt = now,
                    targetVwap = vwap,
                    targetPrice = target,
                    readiness = 0,
                    reason = String.format(
                        Locale.GERMANY,
                        "AUTO BUY • TP €%.8f = +%.1f%% NET • %s",
                        target, targetNetPercent, reason
                    ),
                    updatedAt = now,
                    trades = state.trades + HumanFactorTrade(
                        now, "BUY", ask, fill.coins, fill.feeEur, 0.0,
                        "T32_${targetNetPercent}_NET"
                    )
                )
                save(context, state)
                UnifiedResearchLog.record(
                    context, agent, "BUY",
                    "ask=$ask; target=$target; targetNet=$targetNetPercent%; fee=${fill.feeEur}; $reason", now
                )
                return state
            }
        }

        state = state.copy(
            readiness = score,
            targetVwap = vwap,
            targetPrice = 0.0,
            reason = reason,
            updatedAt = now
        )
        save(context, state)
        UnifiedResearchLog.record(context, agent, "CYCLE", "readiness=$score; $reason", now)
        return state
    }
}

object T32Net15Store {
    const val TARGET_NET_PERCENT = 1.5
    private val engine = T32ProfitVariantEngineV650("t32_net_15_v650", "T32_NET_1P5", TARGET_NET_PERCENT)
    fun state(context: Context): HumanFactorState = engine.state(context)
    fun sync(context: Context, now: Long = System.currentTimeMillis()): HumanFactorState = engine.sync(context, now)
}

object T32Net20Store {
    const val TARGET_NET_PERCENT = 2.0
    private val engine = T32ProfitVariantEngineV650("t32_net_20_v650", "T32_NET_2P0", TARGET_NET_PERCENT)
    fun state(context: Context): HumanFactorState = engine.state(context)
    fun sync(context: Context, now: Long = System.currentTimeMillis()): HumanFactorState = engine.sync(context, now)
}

object T32ProfitVariantsV650 {
    fun syncAll(context: Context, now: Long = System.currentTimeMillis()) {
        T32Net15Store.sync(context, now)
        T32Net20Store.sync(context, now)
    }
}
