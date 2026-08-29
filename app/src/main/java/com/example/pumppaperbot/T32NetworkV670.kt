package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min

/** Economic assumptions shared by the two V6.7 X-derived paper accounts. */
internal object T32CostPolicyV670 {
    const val FEE_RATE = 0.0021
    const val STOP_NET_PERCENT = -1.2
    const val ECONOMY_TARGET_NET_PERCENT = 2.5
    const val ECONOMY_MAX_HOLD_MILLIS = 120L * 60L * 1_000L
    const val X52_MAX_HOLD_MILLIS = 90L * 60L * 1_000L
    const val MAX_ENTRIES_PER_UTC_DAY = 2
    const val LIMIT_DISCOUNT = 0.001
    const val LIMIT_TTL_MILLIS = 2L * 60L * 1_000L

    fun buyAllCash(cashEur: Double, askEur: Double): T32CostPolicyV650.BuyFill =
        T32CostPolicyV650.buyAllCash(cashEur, askEur)

    fun sellFee(coins: Double, bidEur: Double): Double = coins * bidEur * FEE_RATE

    fun entryCost(coins: Double, entryEur: Double): Double = coins * entryEur * (1.0 + FEE_RATE)

    fun netPercent(entryEur: Double, exitEur: Double): Double {
        if (entryEur <= 0.0 || exitEur <= 0.0) return 0.0
        return ((exitEur * (1.0 - FEE_RATE)) / (entryEur * (1.0 + FEE_RATE)) - 1.0) * 100.0
    }

    fun economyTargetPrice(entryEur: Double): Double =
        entryEur * (1.0 + FEE_RATE) * (1.0 + ECONOMY_TARGET_NET_PERCENT / 100.0) / (1.0 - FEE_RATE)
}

internal data class T32V670Setup(
    val readiness: Int,
    val exactCore: Boolean,
    val signalOpenTime: Long,
    val signalCloseTime: Long,
    val signalCloseUsdt: Double,
    val signalVwapUsdt: Double,
    val liveOpenTime: Long,
    val liveOpenUsdt: Double,
    val liveHighUsdt: Double,
    val liveLowUsdt: Double,
    val liveCloseUsdt: Double,
    val drawdown12hPercent: Double?,
    val solBtcRelativeLag6: Double?,
    val reason: String
)

/**
 * Exact causal entry lineage used by X: raw PUMP/USDT, 60 closed 1m candles and a 12h
 * rolling high made only from candles already closed at the signal. Live data is used only
 * for readiness and realistic pending-limit observation, never to create the closed signal.
 */
internal object T32V670Policy {
    private const val REQUIRED_CLOSED_MINUTES = 720

    fun evaluate(context: Context, now: Long = System.currentTimeMillis()): T32V670Setup {
        val raw = ChartSpeedStore.pumpUsdtCandles(context, ChartInterval.ONE_MINUTE)
            .sortedBy { it.openTime }
        val closed = raw.filter { it.closeTime < now }
        if (closed.size < REQUIRED_CLOSED_MINUTES) {
            return empty("Ждём 720 закрытых минут PUMP/USDT (${closed.size}/720)")
        }

        val signal = closed.last()
        val previous = closed[closed.lastIndex - 1]
        val vwapRows = closed.takeLast(60)
        val quote = vwapRows.sumOf { it.quoteVolume }
        if (quote <= 0.0) return empty("Нет quote volume для причинного VWAP")
        val signalVwap = vwapRows.sumOf {
            ((it.high + it.low + it.close) / 3.0) * it.quoteVolume
        } / quote
        val signalBuy = buyShare(signal)
        val previousBuy = buyShare(previous)
        val signalDeviation = (signal.close / signalVwap - 1.0) * 100.0
        val peak12h = closed.takeLast(REQUIRED_CLOSED_MINUTES).maxOf { it.high }
        val drawdown12h = (signal.close / peak12h - 1.0) * 100.0
        val exactCore = isExactCore(
            deviationPercent = signalDeviation,
            green = signal.close > signal.open,
            buyShare = signalBuy,
            previousBuyShare = previousBuy,
            drawdown12hPercent = drawdown12h
        )

        val live = raw.lastOrNull()?.takeIf { it.openTime >= signal.openTime } ?: signal
        val liveRows = raw.takeLast(60)
        val liveQuote = liveRows.sumOf { it.quoteVolume }
        val liveVwap = if (liveQuote > 0.0) liveRows.sumOf {
            ((it.high + it.low + it.close) / 3.0) * it.quoteVolume
        } / liveQuote else signalVwap
        val liveDeviation = (live.close / liveVwap - 1.0) * 100.0
        val liveBuy = buyShare(live)
        val livePrevious = raw.getOrNull(raw.lastIndex - 1) ?: previous
        val liveDelta = liveBuy - buyShare(livePrevious)
        val bodyPercent = if (live.open > 0.0) (live.close / live.open - 1.0) * 100.0 else 0.0
        val readiness = (
            progress(-liveDeviation, 0.25, 0.40, 35) +
                progress(-drawdown12h, 0.50, 4.0, 25) +
                progress(liveBuy, 0.40, 0.50, 20) +
                progress(bodyPercent, -0.20, 0.15, 10) +
                progress(liveDelta, -0.05, 0.03, 10)
            ).coerceIn(0, 100)

        val payloads = PumpBotEngine.savedMarketPayloads(context)
        val btc = PumpBotEngine.parseCandles(payloads.btcJson).sortedBy { it.closeTime }
        val sol = PumpBotEngine.parseCandles(payloads.solJson).sortedBy { it.closeTime }
        val btcLag6 = hourReturnEndingLag(btc, 6)
        val solLag6 = hourReturnEndingLag(sol, 6)
        val relativeLag6 = if (btcLag6 != null && solLag6 != null) solLag6 - btcLag6 else null

        val reason = buildString {
            append(String.format(Locale.GERMANY, "готовность %d/100 • VWAP %+.2f%% • BUY %.0f%%", readiness, liveDeviation, liveBuy * 100.0))
            append(String.format(Locale.GERMANY, " • 12h %.2f%%", drawdown12h))
            append(relativeLag6?.let {
                String.format(Locale.GERMANY, " • SOL-BTC L6 %+.2f п.п.", it)
            } ?: " • SOL-BTC L6 ?")
            if (exactCore) append(" • X CORE ГОТОВ")
        }

        return T32V670Setup(
            readiness = readiness,
            exactCore = exactCore,
            signalOpenTime = signal.openTime,
            signalCloseTime = signal.closeTime,
            signalCloseUsdt = signal.close,
            signalVwapUsdt = signalVwap,
            liveOpenTime = live.openTime,
            liveOpenUsdt = live.open,
            liveHighUsdt = live.high,
            liveLowUsdt = live.low,
            liveCloseUsdt = live.close,
            drawdown12hPercent = drawdown12h,
            solBtcRelativeLag6 = relativeLag6,
            reason = reason
        )
    }

    private fun buyShare(candle: PumpCandle): Double =
        when {
            candle.quoteVolume > 0.0 && candle.takerBuyQuoteVolume > 0.0 ->
                candle.takerBuyQuoteVolume / candle.quoteVolume
            candle.volume > 0.0 -> candle.takerBuyVolume / candle.volume
            else -> 0.0
        }

    fun isExactCore(
        deviationPercent: Double,
        green: Boolean,
        buyShare: Double,
        previousBuyShare: Double,
        drawdown12hPercent: Double
    ): Boolean = deviationPercent <= -0.40 &&
        green &&
        buyShare >= 0.50 &&
        buyShare > previousBuyShare &&
        drawdown12hPercent <= -4.0

    private fun progress(value: Double, low: Double, high: Double, weight: Int): Int {
        if (high <= low) return 0
        return (((value - low) / (high - low)) * weight).toInt().coerceIn(0, weight)
    }

    private fun hourReturnEndingLag(rows: List<PumpCandle>, lagHours: Int): Double? {
        if (rows.size < 4) return null
        val end = rows.lastIndex - lagHours * 2
        val start = end - 2
        if (start < 0 || end !in rows.indices) return null
        val first = rows[start].close
        val last = rows[end].close
        if (first <= 0.0 || last <= 0.0) return null
        return (last / first - 1.0) * 100.0
    }

    private fun empty(reason: String) = T32V670Setup(
        readiness = 0,
        exactCore = false,
        signalOpenTime = 0L,
        signalCloseTime = 0L,
        signalCloseUsdt = 0.0,
        signalVwapUsdt = 0.0,
        liveOpenTime = 0L,
        liveOpenUsdt = 0.0,
        liveHighUsdt = 0.0,
        liveLowUsdt = 0.0,
        liveCloseUsdt = 0.0,
        drawdown12hPercent = null,
        solBtcRelativeLag6 = null,
        reason = reason
    )
}

internal enum class T32V670Profile(val title: String, val agent: String) {
    ECONOMY("AUTO X ECONOMY", "V670_X_ECONOMY"),
    X52_SELECT("AUTO X52 SELECT", "V670_X52_SELECT");

    fun allowsSignal(setup: T32V670Setup): Boolean = when (this) {
        ECONOMY -> true
        X52_SELECT -> (setup.solBtcRelativeLag6 ?: Double.NEGATIVE_INFINITY) >= 0.40
    }

    fun gateText(setup: T32V670Setup): String = when (this) {
        ECONOMY -> "X CORE"
        X52_SELECT -> setup.solBtcRelativeLag6?.let {
            String.format(Locale.GERMANY, "SOL-BTC L6 %+.2f п.п. (нужно +0,40)", it)
        } ?: "SOL-BTC L6: нет данных"
    }
}

data class T32V670AutoState(
    val cash: Double = 1000.0,
    val coins: Double = 0.0,
    val entryPriceEur: Double = 0.0,
    val entryAt: Long = 0L,
    val entrySignalMinute: Long = 0L,
    val targetPriceEur: Double = 0.0,
    val targetSignalUsdt: Double = 0.0,
    val readiness: Int = 0,
    val reason: String = "V6.7 • ожидание",
    val pendingLimitUsdt: Double = 0.0,
    val pendingVwapUsdt: Double = 0.0,
    val pendingSignalAt: Long = 0L,
    val pendingUntil: Long = 0L,
    val dayKey: String = "",
    val entriesToday: Int = 0,
    val updatedAt: Long = 0L,
    val trades: List<HumanFactorTrade> = emptyList()
) {
    val inPosition get() = coins > 0.0
    fun value(markEur: Double) = cash + coins * max(markEur, 0.0)
}

private class T32V670AutoEngine(
    private val prefsName: String,
    private val profile: T32V670Profile
) {
    private val key = "state"

    fun state(context: Context): T32V670AutoState = runCatching {
        val j = JSONObject(context.getSharedPreferences(prefsName, 0).getString(key, "{}") ?: "{}")
        T32V670AutoState(
            cash = j.optDouble("cash", 1000.0),
            coins = j.optDouble("coins"),
            entryPriceEur = j.optDouble("entryPriceEur"),
            entryAt = j.optLong("entryAt"),
            entrySignalMinute = j.optLong("entrySignalMinute"),
            targetPriceEur = j.optDouble("targetPriceEur"),
            targetSignalUsdt = j.optDouble("targetSignalUsdt"),
            readiness = j.optInt("readiness"),
            reason = j.optString("reason", "V6.7 • ожидание"),
            pendingLimitUsdt = j.optDouble("pendingLimitUsdt"),
            pendingVwapUsdt = j.optDouble("pendingVwapUsdt"),
            pendingSignalAt = j.optLong("pendingSignalAt"),
            pendingUntil = j.optLong("pendingUntil"),
            dayKey = j.optString("dayKey", ""),
            entriesToday = j.optInt("entriesToday"),
            updatedAt = j.optLong("updatedAt"),
            trades = (j.optJSONArray("trades") ?: JSONArray()).let { array ->
                (0 until array.length()).map { HumanFactorTrade.from(array.getJSONObject(it)) }
            }
        )
    }.getOrDefault(T32V670AutoState())

    private fun save(context: Context, state: T32V670AutoState) {
        val j = JSONObject()
            .put("cash", state.cash)
            .put("coins", state.coins)
            .put("entryPriceEur", state.entryPriceEur)
            .put("entryAt", state.entryAt)
            .put("entrySignalMinute", state.entrySignalMinute)
            .put("targetPriceEur", state.targetPriceEur)
            .put("targetSignalUsdt", state.targetSignalUsdt)
            .put("readiness", state.readiness)
            .put("reason", state.reason)
            .put("pendingLimitUsdt", state.pendingLimitUsdt)
            .put("pendingVwapUsdt", state.pendingVwapUsdt)
            .put("pendingSignalAt", state.pendingSignalAt)
            .put("pendingUntil", state.pendingUntil)
            .put("dayKey", state.dayKey)
            .put("entriesToday", state.entriesToday)
            .put("updatedAt", state.updatedAt)
            .put("trades", JSONArray(state.trades.takeLast(300).map { it.json() }))
        context.getSharedPreferences(prefsName, 0).edit().putString(key, j.toString()).commit()
    }

    @Synchronized
    fun sync(context: Context, now: Long = System.currentTimeMillis()): T32V670AutoState {
        var state = state(context)
        val today = utcDay(now)
        if (state.dayKey != today) state = state.copy(dayKey = today, entriesToday = 0)

        val setup = T32V670Policy.evaluate(context, now)
        val venue = BitpandaFusionStore.state(context)
        val freshBid = venue.bid.takeIf { venue.fresh(now) && it > 0.0 }
        val freshAsk = venue.ask.takeIf { venue.fresh(now) && it > 0.0 }

        if (state.inPosition) {
            if (freshBid == null) {
                state = state.copy(readiness = setup.readiness, reason = "ПОЗИЦИЯ • ждём свежий исполнимый bid • ${setup.reason}", updatedAt = now)
                save(context, state)
                return state
            }
            val net = T32CostPolicyV670.netPercent(state.entryPriceEur, freshBid)
            val hitStop = net <= T32CostPolicyV670.STOP_NET_PERCENT
            val hitTarget = when (profile) {
                T32V670Profile.ECONOMY -> freshBid >= state.targetPriceEur
                T32V670Profile.X52_SELECT -> dynamicTargetReached(state, setup)
            }
            val maxHold = when (profile) {
                T32V670Profile.ECONOMY -> T32CostPolicyV670.ECONOMY_MAX_HOLD_MILLIS
                T32V670Profile.X52_SELECT -> T32CostPolicyV670.X52_MAX_HOLD_MILLIS
            }
            val hitTime = now - state.entryAt >= maxHold
            // Conservative ordering matches replay: STOP wins when target and stop are both observable.
            if (hitStop || hitTarget || hitTime) {
                val gross = state.coins * freshBid
                val fee = T32CostPolicyV670.sellFee(state.coins, freshBid)
                val pnl = gross - fee - T32CostPolicyV670.entryCost(state.coins, state.entryPriceEur)
                val why = when {
                    hitStop -> "STOP -1,2% NET"
                    hitTarget && profile == T32V670Profile.ECONOMY -> "TP +2,5% NET"
                    hitTarget -> "X52 VWAP"
                    profile == T32V670Profile.ECONOMY -> "TIME 120 МИН"
                    else -> "TIME 90 МИН"
                }
                state = state.copy(
                    cash = gross - fee,
                    coins = 0.0,
                    entryPriceEur = 0.0,
                    entryAt = 0L,
                    entrySignalMinute = 0L,
                    targetPriceEur = 0.0,
                    targetSignalUsdt = 0.0,
                    readiness = setup.readiness,
                    reason = "AUTO EXIT • $why",
                    updatedAt = now,
                    trades = state.trades + HumanFactorTrade(now, "SELL", freshBid, state.coins, fee, pnl, "${profile.name};$why")
                )
                save(context, state)
                UnifiedResearchLog.record(context, profile.agent, "SELL", "bid=$freshBid; net=$net; pnl=$pnl; $why", now)
                return state
            }
            state = state.copy(
                readiness = setup.readiness,
                reason = String.format(Locale.GERMANY, "ПОЗИЦИЯ • NET %+.2f%% • %s", net, if (profile == T32V670Profile.ECONOMY) "TP +2,5%" else "X52 → VWAP"),
                updatedAt = now
            )
            save(context, state)
            return state
        }

        if (state.pendingUntil > 0L && now > state.pendingUntil) state = clearPending(state)
        val dailyBlocked = state.entriesToday >= T32CostPolicyV670.MAX_ENTRIES_PER_UTC_DAY

        val pendingLive = state.pendingLimitUsdt > 0.0 && now <= state.pendingUntil
        val limitTouched = pendingLive && setup.liveLowUsdt > 0.0 && setup.liveLowUsdt <= state.pendingLimitUsdt
        if (!dailyBlocked && limitTouched && freshAsk != null) {
            val fill = T32CostPolicyV670.buyAllCash(state.cash, freshAsk)
            if (fill.coins > 0.0) {
                val signalFillUsdt = min(state.pendingLimitUsdt, setup.liveOpenUsdt.takeIf { it > 0.0 } ?: state.pendingLimitUsdt)
                val targetEur = if (profile == T32V670Profile.ECONOMY) T32CostPolicyV670.economyTargetPrice(freshAsk) else 0.0
                val targetUsdt = if (profile == T32V670Profile.X52_SELECT) max(signalFillUsdt * 1.001, state.pendingVwapUsdt) else 0.0
                state = clearPending(state).copy(
                    cash = 0.0,
                    coins = fill.coins,
                    entryPriceEur = freshAsk,
                    entryAt = now,
                    entrySignalMinute = setup.liveOpenTime,
                    targetPriceEur = targetEur,
                    targetSignalUsdt = targetUsdt,
                    entriesToday = state.entriesToday + 1,
                    readiness = setup.readiness,
                    reason = if (profile == T32V670Profile.ECONOMY) "AUTO BUY • TP +2,5 / SL -1,2 / 120m" else "AUTO BUY • X52 VWAP / SL -1,2 / 90m",
                    updatedAt = now,
                    trades = state.trades + HumanFactorTrade(now, "BUY", freshAsk, fill.coins, fill.feeEur, 0.0, profile.name)
                )
                save(context, state)
                UnifiedResearchLog.record(context, profile.agent, "BUY", "askEur=$freshAsk; signalFillUsdt=$signalFillUsdt; targetUsdt=$targetUsdt", now)
                return state
            }
        }

        val mayCreate = !dailyBlocked &&
            state.pendingLimitUsdt <= 0.0 &&
            setup.exactCore &&
            profile.allowsSignal(setup) &&
            setup.signalCloseUsdt > 0.0 &&
            setup.signalCloseTime > 0L &&
            now <= setup.signalCloseTime + T32CostPolicyV670.LIMIT_TTL_MILLIS
        if (mayCreate) {
            state = state.copy(
                pendingLimitUsdt = setup.signalCloseUsdt * (1.0 - T32CostPolicyV670.LIMIT_DISCOUNT),
                pendingVwapUsdt = setup.signalVwapUsdt,
                pendingSignalAt = setup.signalOpenTime,
                pendingUntil = setup.signalCloseTime + T32CostPolicyV670.LIMIT_TTL_MILLIS
            )
        }

        val status = when {
            dailyBlocked -> "ЛИМИТ 2 ВХОДА/UTC-СУТКИ"
            state.pendingLimitUsdt > 0.0 -> String.format(Locale.GERMANY, "LIMIT PUMP/USDT %.8f • TTL 2m", state.pendingLimitUsdt)
            !profile.allowsSignal(setup) -> profile.gateText(setup)
            else -> profile.gateText(setup)
        }
        state = state.copy(readiness = setup.readiness, reason = "$status • ${setup.reason}", updatedAt = now)
        save(context, state)
        UnifiedResearchLog.record(context, profile.agent, "CYCLE", state.reason, now)
        return state
    }

    private fun dynamicTargetReached(state: T32V670AutoState, setup: T32V670Setup): Boolean {
        if (state.targetSignalUsdt <= 0.0 || setup.liveCloseUsdt <= 0.0) return false
        return if (setup.liveOpenTime > state.entrySignalMinute) {
            setup.liveHighUsdt >= state.targetSignalUsdt
        } else {
            setup.liveCloseUsdt >= state.targetSignalUsdt
        }
    }

    private fun clearPending(state: T32V670AutoState) = state.copy(
        pendingLimitUsdt = 0.0,
        pendingVwapUsdt = 0.0,
        pendingSignalAt = 0L,
        pendingUntil = 0L
    )

    private fun utcDay(now: Long): String = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(now))
}

object V670EconomyStore {
    private val engine = T32V670AutoEngine("v670_x_economy", T32V670Profile.ECONOMY)
    fun state(context: Context) = engine.state(context)
    fun sync(context: Context, now: Long = System.currentTimeMillis()) = engine.sync(context, now)
}

object V670X52SelectStore {
    private val engine = T32V670AutoEngine("v670_x52_select", T32V670Profile.X52_SELECT)
    fun state(context: Context) = engine.state(context)
    fun sync(context: Context, now: Long = System.currentTimeMillis()) = engine.sync(context, now)
}

object T32NetworkV670 {
    fun syncAll(context: Context, now: Long = System.currentTimeMillis()) {
        listOf<Pair<String, () -> Unit>>(
            "V670_X_ECONOMY" to { V670EconomyStore.sync(context, now) },
            "V670_X52_SELECT" to { V670X52SelectStore.sync(context, now) }
        ).forEach { (agent, action) ->
            runCatching(action).onFailure {
                UnifiedResearchLog.record(context, agent, "ERROR", "${it.javaClass.simpleName}: ${it.message.orEmpty().take(180)}", now)
            }
        }
    }
}
