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
    val reason: String = "V6.6 • ждём рынок",
    val lastAlertBand: Int = 0,
    val lastAlertAt: Long = 0L,
    val updatedAt: Long = 0L,
    val trades: List<HumanFactorTrade> = emptyList()
) {
    val inPosition get() = coins > 0.0
    fun value(price: Double) = cash + coins * max(price, 0.0)
}

/** Compatibility wrapper: V6.6 display score is deliberately continuous, not a 0->100 binary jump. */
internal object HumanFactorVwapPolicy {
    const val READY = T32V660Policy.MANUAL_ALERT_SCORE

    fun evaluate(c: List<PumpCandle>): Triple<Int, Double, String> {
        if (c.size < 2) return Triple(0, 0.0, "Ждём минутные данные")
        val rows = c.takeLast(60)
        val q = rows.sumOf { it.quoteVolume }
        if (q <= 0.0) return Triple(0, 0.0, "Нет quote volume")
        val vwap = rows.sumOf { ((it.high + it.low + it.close) / 3.0) * it.quoteVolume } / q
        val x = rows.last()
        val buy = if (x.volume > 0.0) x.takerBuyVolume / x.volume else 0.0
        val deviation = (x.close / vwap - 1.0) * 100.0
        val score = (((-deviation + 0.25) / 0.65) * 60.0 + ((buy - 0.40) / 0.10) * 40.0)
            .toInt().coerceIn(0, 100)
        return Triple(score, vwap, String.format(Locale.GERMANY, "VWAP %+.2f%% • BUY %.0f%%", deviation, buy * 100.0))
    }
}

/**
 * V6.6 human branch.
 * Entry is always owner-confirmed; exit is automatic on the same economic core as the three autos.
 * Opportunity monitoring continues while a position is open, but a second BUY is never allowed.
 */
object HumanFactorStore {
    private const val PREFS = "human_factor_v660_fresh"
    private const val KEY = "state"
    const val TARGET_NET_PERCENT = T32CostPolicyV660.TARGET_NET_PERCENT

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
            reason = j.optString("reason", "V6.6 • ожидание"),
            lastAlertBand = j.optInt("lastAlertBand"),
            lastAlertAt = j.optLong("lastAlertAt"),
            updatedAt = j.optLong("updatedAt"),
            trades = (j.optJSONArray("trades") ?: JSONArray()).let { a ->
                (0 until a.length()).map { HumanFactorTrade.from(a.getJSONObject(it)) }
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
        val venue = BitpandaFusionStore.state(context)
        val fallback = PaperExecutionPolicy.displayPrice(PumpBotEngine.snapshot(context), now)
        val bid = venue.bid.takeIf { venue.fresh(now) } ?: fallback
        val setup = T32V660Policy.evaluate(context)

        if (state.inPosition) {
            val target = state.targetPrice.takeIf { it > 0.0 } ?: T32CostPolicyV660.targetPrice(state.entryPrice)
            val net = T32CostPolicyV660.netPercent(state.entryPrice, bid)
            val hitTarget = bid >= target
            val hitStop = net <= T32CostPolicyV660.STOP_NET_PERCENT
            val hitTime = now - state.entryAt >= T32CostPolicyV660.MAX_HOLD_MILLIS
            if (hitTarget || hitStop || hitTime) {
                val gross = state.coins * bid
                val fee = T32CostPolicyV660.sellFee(state.coins, bid)
                val pnl = gross - fee - T32CostPolicyV660.entryCost(state.coins, state.entryPrice)
                val why = when {
                    hitTarget -> "TP +2,5% NET"
                    hitStop -> "STOP -1,2% NET"
                    else -> "TIME 120 МИН"
                }
                state = state.copy(
                    cash = gross - fee,
                    coins = 0.0,
                    entryPrice = 0.0,
                    entryAt = 0L,
                    targetPrice = 0.0,
                    pending = false,
                    readiness = setup.readiness,
                    reason = "HUMAN AUTO EXIT • $why • ${setup.reason}",
                    updatedAt = now,
                    trades = state.trades + HumanFactorTrade(now, "SELL", bid, state.coins, fee, pnl, "HUMAN_V660;$why")
                )
                save(context, state)
                UnifiedResearchLog.record(context, "V660_HUMAN", "SELL", "bid=$bid; net=$net; pnl=$pnl; $why", now)
                PumpAlert.showHumanFactor(context, false, state.reason)
                return state
            }
        }

        val suppressed = state.candidateId == -1L || state.candidateId == -2L
        val resetCandidate = setup.readiness < T32V660Policy.MANUAL_RESET_SCORE || !setup.belowFour12h
        val candidateId = when {
            resetCandidate -> 0L
            !suppressed && setup.readiness >= T32V660Policy.MANUAL_ALERT_SCORE && state.candidateId == 0L -> now
            else -> state.candidateId
        }
        val effectiveSuppressed = candidateId == -1L || candidateId == -2L
        val pending = setup.readiness >= T32V660Policy.MANUAL_ALERT_SCORE && setup.belowFour12h && !effectiveSuppressed
        val shouldRing = HumanFactorAlertPolicyV650.shouldRing(pending, state.lastAlertAt, now)
        val band = when {
            setup.readiness >= 95 -> 95
            setup.readiness >= 90 -> 90
            setup.readiness >= 80 -> 80
            setup.readiness >= 65 -> 65
            else -> 0
        }

        val positionText = if (state.inPosition) {
            val net = T32CostPolicyV660.netPercent(state.entryPrice, bid)
            String.format(Locale.GERMANY, "ПОЗИЦИЯ NET %+.2f%% • ", net)
        } else ""
        val suppressText = when (candidateId) {
            -1L -> "Отклонено до распада ситуации • "
            -2L -> "Уже подтверждено; ждём новую ситуацию • "
            else -> ""
        }
        state = state.copy(
            readiness = setup.readiness,
            pending = pending,
            candidateId = candidateId,
            targetVwap = setup.vwap,
            targetPrice = if (state.inPosition) state.targetPrice.takeIf { it > 0.0 } ?: T32CostPolicyV660.targetPrice(state.entryPrice) else 0.0,
            reason = "$positionText$suppressText${setup.reason}",
            lastAlertBand = band,
            lastAlertAt = if (shouldRing) now else if (!pending) 0L else state.lastAlertAt,
            updatedAt = now
        )
        save(context, state)

        if (shouldRing) {
            val detail = buildString {
                append("V6.6 HUMAN • готовность ${setup.readiness}/100. ")
                if (state.inPosition) append("Позиция уже открыта; это новая возможность, второй BUY недоступен. ")
                else append("Решение за вами: ВОЙТИ или ОТКЛОНИТЬ. ")
                append("При входе AUTO TP +2,5% / STOP -1,2% / TIME 120m. ${setup.reason}")
            }
            HumanFactorAlarmV650.ring(context, detail)
            UnifiedResearchLog.record(context, "V660_HUMAN", "ALERT", detail, now)
        } else if (!pending) {
            HumanFactorAlarmV650.cancel(context)
        }
        UnifiedResearchLog.record(context, "V660_HUMAN", if (pending) "PENDING" else "CYCLE", state.reason, now)
        return state
    }

    @Synchronized
    fun approve(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val state = state(context)
        if (!state.pending || state.readiness < T32V660Policy.MANUAL_ALERT_SCORE || state.inPosition) return false
        val venue = BitpandaFusionStore.state(context)
        val ask = venue.ask.takeIf { venue.fresh(now) } ?: return false
        val fill = T32CostPolicyV660.buyAllCash(state.cash, ask)
        if (fill.coins <= 0.0) return false
        val target = T32CostPolicyV660.targetPrice(ask)
        save(context, state.copy(
            cash = 0.0,
            coins = fill.coins,
            entryPrice = ask,
            entryAt = now,
            targetPrice = target,
            pending = false,
            candidateId = -2L,
            reason = String.format(Locale.GERMANY, "HUMAN BUY • AUTO TP €%.8f = +2,5%% NET • STOP -1,2%% • 120m", target),
            lastAlertBand = 0,
            lastAlertAt = 0L,
            updatedAt = now,
            trades = state.trades + HumanFactorTrade(now, "BUY", ask, fill.coins, fill.feeEur, 0.0, "HUMAN_APPROVED_V660")
        ))
        HumanFactorAlarmV650.cancel(context)
        UnifiedResearchLog.record(context, "V660_HUMAN", "BUY", "ask=$ask; target=$target; HUMAN_APPROVED", now)
        return true
    }

    @Synchronized
    fun reject(context: Context, now: Long = System.currentTimeMillis()) {
        val state = state(context)
        save(context, state.copy(
            pending = false,
            candidateId = -1L,
            reason = "Вход отклонён; ждём распада этой ситуации и нового набора готовности",
            lastAlertBand = 0,
            lastAlertAt = 0L,
            updatedAt = now
        ))
        HumanFactorAlarmV650.cancel(context)
        UnifiedResearchLog.record(context, "V660_HUMAN", "REJECT", "owner rejected current setup", now)
    }
}
