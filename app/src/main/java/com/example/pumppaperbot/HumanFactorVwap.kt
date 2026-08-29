package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.max

data class HumanFactorTrade(
    val time: Long,
    val action: String,
    val price: Double,
    val amount: Double,
    val fee: Double,
    val pnlEur: Double,
    val reason: String
) {
    fun json() = JSONObject()
        .put("time", time)
        .put("action", action)
        .put("price", price)
        .put("amount", amount)
        .put("fee", fee)
        .put("pnl", pnlEur)
        .put("reason", reason)

    companion object {
        fun from(j: JSONObject) = HumanFactorTrade(
            j.optLong("time"),
            j.optString("action"),
            j.optDouble("price"),
            j.optDouble("amount"),
            j.optDouble("fee"),
            j.optDouble("pnl"),
            j.optString("reason")
        )
    }
}

data class HumanFactorState(
    val cash: Double = 1000.0,
    val coins: Double = 0.0,
    val entryPrice: Double = 0.0,
    val entryAt: Long = 0L,
    val targetVwap: Double = 0.0,
    val targetPrice: Double = 0.0,
    val readiness: Int = 0,
    val pending: Boolean = false,
    val candidateId: Long = 0L,
    val reason: String = "Ожидаем минутные данные",
    val lastAlertBand: Int = 0,
    val lastAlertAt: Long = 0L,
    val updatedAt: Long = 0L,
    val trades: List<HumanFactorTrade> = emptyList()
) {
    val inPosition get() = coins > 0.0
    fun value(price: Double) = cash + coins * max(price, 0.0)
}

internal object HumanFactorVwapPolicy {
    const val READY = 98

    fun evaluate(c: List<PumpCandle>): Triple<Int, Double, String> {
        if (c.size < 61) return Triple(0, 0.0, "Нужно не менее 60 закрытых минут")
        val rows = c.dropLast(1).takeLast(60)
        val x = rows.last()
        val prev = rows[rows.lastIndex - 1]
        val quote = rows.sumOf { it.quoteVolume }
        if (quote <= 0.0) return Triple(0, 0.0, "Нет quote volume")
        val vwap = rows.sumOf { ((it.high + it.low + it.close) / 3.0) * it.quoteVolume } / quote
        val deviation = (x.close / vwap - 1.0) * 100.0
        val buy = if (x.volume > 0.0) x.takerBuyVolume / x.volume else 0.0
        val prevBuy = if (prev.volume > 0.0) prev.takerBuyVolume / prev.volume else 0.0
        val distance = (((-deviation) / 0.40) * 55.0).toInt().coerceIn(0, 55)
        val green = if (x.close > x.open) 15 else 0
        val share = ((buy - .40) / .10 * 20).toInt().coerceIn(0, 20)
        val repair = if (buy > prevBuy) 10 else 0
        val score = (distance + green + share + repair).coerceIn(0, 100)
        val exact = deviation <= -.40 && x.close > x.open && buy >= .50 && buy > prevBuy
        val final = if (exact) 100 else score.coerceAtMost(99)
        return Triple(
            final,
            vwap,
            String.format(
                Locale.GERMANY,
                "VWAP €%.8f • отклонение %+.2f%% • BUY %.0f%%",
                vwap,
                deviation,
                buy * 100.0
            )
        )
    }
}

/** Human-confirmed T32 entry, automatic +2.0% NET exit in V6.5. */
object HumanFactorStore {
    private const val PREFS = "human_factor_vwap_v630"
    private const val KEY = "state"
    const val TARGET_NET_PERCENT = 2.0

    fun state(context: Context): HumanFactorState = runCatching {
        val j = JSONObject(context.getSharedPreferences(PREFS, 0).getString(KEY, "{}") ?: "{}")
        HumanFactorState(
            cash = j.optDouble("cash", 1000.0),
            coins = j.optDouble("coins"),
            entryPrice = j.optDouble("entryPrice"),
            entryAt = j.optLong("entryAt"),
            targetVwap = j.optDouble("targetVwap"),
            targetPrice = j.optDouble("targetPrice"),
            readiness = j.optInt("readiness"),
            pending = j.optBoolean("pending"),
            candidateId = j.optLong("candidateId"),
            reason = j.optString("reason", "Ожидание"),
            lastAlertBand = j.optInt("lastAlertBand"),
            lastAlertAt = j.optLong("lastAlertAt"),
            updatedAt = j.optLong("updatedAt"),
            trades = (j.optJSONArray("trades") ?: JSONArray()).let { array ->
                (0 until array.length()).map { HumanFactorTrade.from(array.getJSONObject(it)) }
            }
        )
    }.getOrDefault(HumanFactorState())

    private fun save(context: Context, state: HumanFactorState) {
        val j = JSONObject()
            .put("cash", state.cash)
            .put("coins", state.coins)
            .put("entryPrice", state.entryPrice)
            .put("entryAt", state.entryAt)
            .put("targetVwap", state.targetVwap)
            .put("targetPrice", state.targetPrice)
            .put("readiness", state.readiness)
            .put("pending", state.pending)
            .put("candidateId", state.candidateId)
            .put("reason", state.reason)
            .put("lastAlertBand", state.lastAlertBand)
            .put("lastAlertAt", state.lastAlertAt)
            .put("updatedAt", state.updatedAt)
            .put("trades", JSONArray(state.trades.takeLast(300).map { it.json() }))
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY, j.toString()).commit()
    }

    @Synchronized
    fun sync(context: Context, now: Long = System.currentTimeMillis()): HumanFactorState {
        var state = state(context)
        val market = BitpandaFusionStore.state(context)
        val fallbackPrice = PaperExecutionPolicy.displayPrice(PumpBotEngine.snapshot(context), now)
        val bid = market.bid.takeIf { market.fresh(now) } ?: fallbackPrice

        if (state.inPosition) {
            HumanFactorAlarmV650.cancel(context)
            val target = state.targetPrice.takeIf { it > 0.0 }
                ?: T32CostPolicyV650.targetPrice(state.entryPrice, TARGET_NET_PERCENT)
            val net = T32CostPolicyV650.netPercent(state.entryPrice, bid)
            val hitTarget = bid >= target
            val hitStop = net <= T32CostPolicyV650.STOP_NET_PERCENT
            val hitTime = now - state.entryAt >= T32CostPolicyV650.MAX_HOLD_MILLIS
            if (hitTarget || hitStop || hitTime) {
                val gross = state.coins * bid
                val fee = T32CostPolicyV650.sellFee(state.coins, bid)
                val pnl = gross - fee - T32CostPolicyV650.entryCost(state.coins, state.entryPrice)
                val exitReason = when {
                    hitTarget -> "TP +2,0% NET"
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
                    pending = false,
                    candidateId = 0L,
                    reason = "HUMAN AUTO EXIT • $exitReason",
                    lastAlertBand = 0,
                    lastAlertAt = 0L,
                    updatedAt = now,
                    trades = state.trades + HumanFactorTrade(
                        now,
                        "SELL",
                        bid,
                        state.coins,
                        fee,
                        pnl,
                        "HUMAN_2P0;$exitReason"
                    )
                )
                save(context, state)
                UnifiedResearchLog.record(
                    context,
                    "T32_HUMAN_2P0",
                    "SELL",
                    "price=$bid; net=$net; pnlEur=$pnl; $exitReason",
                    now
                )
                PumpAlert.showHumanFactor(context, false, state.reason)
                return state
            }
            val marked = state.copy(
                targetPrice = target,
                reason = String.format(
                    Locale.GERMANY,
                    "ПОЗИЦИЯ • NET %+.2f%% • AUTO TP €%.8f (+2,0%% NET)",
                    net,
                    target
                ),
                updatedAt = now
            )
            save(context, marked)
            UnifiedResearchLog.record(context, "T32_HUMAN_2P0", "IN_POSITION", marked.reason, now)
            return marked
        }

        val (score, vwap, marketReason) = HumanFactorVwapPolicy.evaluate(
            ChartSpeedStore.candles(context, ChartInterval.ONE_MINUTE)
        )
        val rejected = state.candidateId == -1L
        val pending = score >= HumanFactorVwapPolicy.READY && !rejected
        val candidateId = when {
            score < 90 -> 0L
            pending && (!state.pending || state.candidateId == 0L) -> now
            else -> state.candidateId
        }
        val band = when {
            rejected -> 0
            score >= 98 -> 100
            score >= 90 -> 90
            else -> 0
        }
        val shouldRing = HumanFactorAlertPolicyV650.shouldRing(pending, state.lastAlertAt, now)
        val displayReason = if (rejected) {
            "Вход отклонён; ждём распада текущей ситуации"
        } else {
            marketReason
        }
        state = state.copy(
            readiness = score,
            pending = pending,
            candidateId = candidateId,
            targetVwap = vwap,
            targetPrice = 0.0,
            reason = displayReason,
            lastAlertBand = band,
            lastAlertAt = if (shouldRing) now else if (!pending) 0L else state.lastAlertAt,
            updatedAt = now
        )
        save(context, state)

        if (shouldRing) {
            val detail = "Готовность $score/100. Нажмите ВОЙТИ или ОТКЛОНИТЬ. При входе AUTO TP = +2,0% NET. $marketReason"
            HumanFactorAlarmV650.ring(context, detail)
            UnifiedResearchLog.record(context, "T32_HUMAN_2P0", "ALERT", detail, now)
        } else if (!pending) {
            HumanFactorAlarmV650.cancel(context)
        }
        UnifiedResearchLog.record(
            context,
            "T32_HUMAN_2P0",
            if (pending) "PENDING" else "CYCLE",
            "readiness=$score; rejected=$rejected; $displayReason",
            now
        )
        return state
    }

    @Synchronized
    fun approve(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val state = state(context)
        if (!state.pending || state.readiness < HumanFactorVwapPolicy.READY || state.inPosition) return false
        val market = BitpandaFusionStore.state(context)
        val ask = market.ask.takeIf { market.fresh(now) } ?: return false
        val fill = T32CostPolicyV650.buyAllCash(state.cash, ask)
        if (fill.coins <= 0.0) return false
        val target = T32CostPolicyV650.targetPrice(ask, TARGET_NET_PERCENT)
        save(
            context,
            state.copy(
                cash = 0.0,
                coins = fill.coins,
                entryPrice = ask,
                entryAt = now,
                targetPrice = target,
                pending = false,
                readiness = 0,
                candidateId = 0L,
                reason = String.format(
                    Locale.GERMANY,
                    "ПОКУПКА ПОДТВЕРЖДЕНА • AUTO TP €%.8f = +2,0%% NET",
                    target
                ),
                lastAlertBand = 0,
                lastAlertAt = 0L,
                updatedAt = now,
                trades = state.trades + HumanFactorTrade(
                    now,
                    "BUY",
                    ask,
                    fill.coins,
                    fill.feeEur,
                    0.0,
                    "HUMAN_APPROVED_2P0_NET"
                )
            )
        )
        HumanFactorAlarmV650.cancel(context)
        UnifiedResearchLog.record(
            context,
            "T32_HUMAN_2P0",
            "BUY",
            "ask=$ask; target=$target; targetNet=2.0%; fee=${fill.feeEur}; HUMAN_APPROVED",
            now
        )
        return true
    }

    @Synchronized
    fun reject(context: Context, now: Long = System.currentTimeMillis()) {
        val state = state(context)
        save(
            context,
            state.copy(
                pending = false,
                candidateId = -1L,
                reason = "Вход отклонён человеком; ждём нового рыночного setup",
                lastAlertBand = 0,
                lastAlertAt = 0L,
                updatedAt = now
            )
        )
        HumanFactorAlarmV650.cancel(context)
        UnifiedResearchLog.record(
            context,
            "T32_HUMAN_2P0",
            "REJECT",
            "Вход отклонён человеком; candidate=${state.candidateId}; readiness=${state.readiness}",
            now
        )
    }
}

/** Original autonomous T32 control: same VWAP exit logic, V6.5 fee model = 0.21% per side. */
object Vwap3265AutoStore {
    private const val PREFS = "vwap_3265_auto_v630"
    private const val KEY = "state"

    fun state(context: Context): HumanFactorState = runCatching {
        val j = JSONObject(context.getSharedPreferences(PREFS, 0).getString(KEY, "{}") ?: "{}")
        HumanFactorState(
            cash = j.optDouble("cash", 1000.0),
            coins = j.optDouble("coins"),
            entryPrice = j.optDouble("entryPrice"),
            entryAt = j.optLong("entryAt"),
            targetVwap = j.optDouble("targetVwap"),
            targetPrice = 0.0,
            readiness = j.optInt("readiness"),
            pending = false,
            candidateId = 0L,
            reason = j.optString("reason", "Ожидание"),
            lastAlertBand = 0,
            lastAlertAt = 0L,
            updatedAt = j.optLong("updatedAt"),
            trades = (j.optJSONArray("trades") ?: JSONArray()).let { array ->
                (0 until array.length()).map { HumanFactorTrade.from(array.getJSONObject(it)) }
            }
        )
    }.getOrDefault(HumanFactorState())

    private fun save(context: Context, state: HumanFactorState) {
        val j = JSONObject()
            .put("cash", state.cash)
            .put("coins", state.coins)
            .put("entryPrice", state.entryPrice)
            .put("entryAt", state.entryAt)
            .put("targetVwap", state.targetVwap)
            .put("readiness", state.readiness)
            .put("reason", state.reason)
            .put("updatedAt", state.updatedAt)
            .put("trades", JSONArray(state.trades.takeLast(300).map { it.json() }))
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY, j.toString()).commit()
    }

    @Synchronized
    fun sync(context: Context, now: Long = System.currentTimeMillis()): HumanFactorState {
        var state = state(context)
        val market = BitpandaFusionStore.state(context)
        val fallbackPrice = PaperExecutionPolicy.displayPrice(PumpBotEngine.snapshot(context), now)
        val bid = market.bid.takeIf { market.fresh(now) } ?: fallbackPrice

        if (state.inPosition) {
            val net = T32CostPolicyV650.netPercent(state.entryPrice, bid)
            val hitVwap = bid >= state.targetVwap
            val hitStop = net <= T32CostPolicyV650.STOP_NET_PERCENT
            val hitTime = now - state.entryAt >= T32CostPolicyV650.MAX_HOLD_MILLIS
            if (hitVwap || hitStop || hitTime) {
                val gross = state.coins * bid
                val fee = T32CostPolicyV650.sellFee(state.coins, bid)
                val pnl = gross - fee - T32CostPolicyV650.entryCost(state.coins, state.entryPrice)
                val exitReason = when {
                    hitVwap -> "VWAP"
                    hitStop -> "STOP -0,80% NET"
                    else -> "90 МИН"
                }
                state = state.copy(
                    cash = gross - fee,
                    coins = 0.0,
                    entryPrice = 0.0,
                    entryAt = 0L,
                    targetVwap = 0.0,
                    readiness = 0,
                    reason = "T32 ORIGINAL EXIT • $exitReason",
                    updatedAt = now,
                    trades = state.trades + HumanFactorTrade(
                        now,
                        "SELL",
                        bid,
                        state.coins,
                        fee,
                        pnl,
                        "T32_ORIGINAL;$exitReason"
                    )
                )
                save(context, state)
                UnifiedResearchLog.record(
                    context,
                    "T32_ORIGINAL",
                    "SELL",
                    "price=$bid; net=$net; pnlEur=$pnl; $exitReason",
                    now
                )
                return state
            }
            val marked = state.copy(
                reason = String.format(
                    Locale.GERMANY,
                    "ПОЗИЦИЯ • NET %+.2f%% • VWAP €%.8f",
                    net,
                    state.targetVwap
                ),
                updatedAt = now
            )
            save(context, marked)
            UnifiedResearchLog.record(context, "T32_ORIGINAL", "IN_POSITION", marked.reason, now)
            return marked
        }

        val (score, vwap, reason) = HumanFactorVwapPolicy.evaluate(
            ChartSpeedStore.candles(context, ChartInterval.ONE_MINUTE)
        )
        if (score >= 100) {
            val ask = market.ask.takeIf { market.fresh(now) }
            if (ask != null) {
                val fill = T32CostPolicyV650.buyAllCash(state.cash, ask)
                state = state.copy(
                    cash = 0.0,
                    coins = fill.coins,
                    entryPrice = ask,
                    entryAt = now,
                    targetVwap = vwap,
                    readiness = 0,
                    reason = "T32 ORIGINAL BUY • $reason",
                    updatedAt = now,
                    trades = state.trades + HumanFactorTrade(
                        now,
                        "BUY",
                        ask,
                        fill.coins,
                        fill.feeEur,
                        0.0,
                        "T32_ORIGINAL_VWAP"
                    )
                )
                save(context, state)
                UnifiedResearchLog.record(
                    context,
                    "T32_ORIGINAL",
                    "BUY",
                    "ask=$ask; targetVwap=$vwap; fee=${fill.feeEur}; $reason",
                    now
                )
                return state
            }
        }

        state = state.copy(
            readiness = score,
            targetVwap = vwap,
            reason = reason,
            updatedAt = now
        )
        save(context, state)
        UnifiedResearchLog.record(context, "T32_ORIGINAL", "CYCLE", "readiness=$score; $reason", now)
        return state
    }
}
