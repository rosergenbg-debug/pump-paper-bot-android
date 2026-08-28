package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

/**
 * V6.6 focused paper network derived from protected X checkpoints.
 * No real orders. Three independent portfolios share one economic core and vary only entry context.
 */
internal object T32CostPolicyV660 {
    const val FEE_RATE = 0.0021
    const val TARGET_NET_PERCENT = 2.5
    const val STOP_NET_PERCENT = -1.2
    const val MAX_HOLD_MILLIS = 120L * 60L * 1_000L
    const val MAX_ENTRIES_PER_UTC_DAY = 2
    const val LIMIT_DISCOUNT = 0.001
    const val LIMIT_TTL_MILLIS = 140_000L

    fun buyAllCash(cashEur: Double, ask: Double): T32CostPolicyV650.BuyFill =
        T32CostPolicyV650.buyAllCash(cashEur, ask)

    fun sellFee(coins: Double, bid: Double): Double = coins * bid * FEE_RATE

    fun netPercent(entryPrice: Double, exitPrice: Double): Double {
        if (entryPrice <= 0.0 || exitPrice <= 0.0) return 0.0
        return ((exitPrice * (1.0 - FEE_RATE)) / (entryPrice * (1.0 + FEE_RATE)) - 1.0) * 100.0
    }

    fun targetPrice(entryPrice: Double): Double =
        entryPrice * (1.0 + FEE_RATE) * (1.0 + TARGET_NET_PERCENT / 100.0) / (1.0 - FEE_RATE)

    fun entryCost(coins: Double, entryPrice: Double): Double = coins * entryPrice * (1.0 + FEE_RATE)
}

internal data class T32V660Setup(
    val readiness: Int,
    val exactCore: Boolean,
    val vwap: Double,
    val signalClose: Double,
    val deviationPercent: Double,
    val buyShare: Double,
    val buyShareDelta: Double,
    val drawdown12hPercent: Double?,
    val belowFour12h: Boolean,
    val btcStrongUpRecent: Boolean,
    val btcHourlyReturns: List<Double>,
    val solBtcRelativeLag6: Double?,
    val reason: String
)

internal object T32V660Policy {
    const val MANUAL_ALERT_SCORE = 80
    const val MANUAL_RESET_SCORE = 65

    private data class MarketPoint(val closeTime: Long, val high: Double, val close: Double)

    fun evaluate(context: Context): T32V660Setup {
        val minute = ChartSpeedStore.candles(context, ChartInterval.ONE_MINUTE)
        if (minute.size < 61) return empty("Ждём не менее 60 минутных свечей")
        val displayRows = minute.takeLast(60)
        val closedRows = minute.dropLast(1).takeLast(60)
        if (closedRows.size < 60 || displayRows.size < 2) return empty("Минутная история ещё неполная")

        val live = displayRows.last()
        val livePrev = displayRows[displayRows.lastIndex - 1]
        val closed = closedRows.last()
        val closedPrev = closedRows[closedRows.lastIndex - 1]

        fun vwap(rows: List<PumpCandle>): Double {
            val q = rows.sumOf { it.quoteVolume }
            if (q <= 0.0) return 0.0
            return rows.sumOf { ((it.high + it.low + it.close) / 3.0) * it.quoteVolume } / q
        }
        fun buyShare(x: PumpCandle): Double = if (x.volume > 0.0) x.takerBuyVolume / x.volume else 0.0

        val displayVwap = vwap(displayRows)
        val closedVwap = vwap(closedRows)
        if (displayVwap <= 0.0 || closedVwap <= 0.0) return empty("Нет quote volume для VWAP")

        val displayDeviation = (live.close / displayVwap - 1.0) * 100.0
        val displayBuy = buyShare(live)
        val displayPrevBuy = buyShare(livePrev)
        val buyDelta = displayBuy - displayPrevBuy
        val closedDeviation = (closed.close / closedVwap - 1.0) * 100.0
        val closedBuy = buyShare(closed)
        val closedPrevBuy = buyShare(closedPrev)

        val payloads = PumpBotEngine.savedMarketPayloads(context)
        val pump30 = parse(payloads.pumpJson)
        val btc30 = parse(payloads.btcJson)
        val sol30 = parse(payloads.solJson)
        val lastPump = pump30.lastOrNull()
        val peak12h = pump30.takeLast(24).maxOfOrNull { it.high }
        val drawdown12h = if (lastPump != null && peak12h != null && peak12h > 0.0) {
            (lastPump.close / peak12h - 1.0) * 100.0
        } else null
        val belowFour = drawdown12h?.let { it <= -4.0 } ?: false

        val btcReturns = (0..2).mapNotNull { hourReturnEndingLag(btc30, it) }
        val btcStrongUp = btcReturns.any { it >= 0.50 }
        val btcLag6 = hourReturnEndingLag(btc30, 6)
        val solLag6 = hourReturnEndingLag(sol30, 6)
        val rel6 = if (btcLag6 != null && solLag6 != null) solLag6 - btcLag6 else null

        val distance = progress(-displayDeviation, -0.25, 0.40, 35)
        val depth = progress(-(drawdown12h ?: 0.0), 0.5, 4.0, 25)
        val share = progress(displayBuy, 0.40, 0.50, 20)
        val bodyPct = if (live.open > 0.0) (live.close / live.open - 1.0) * 100.0 else 0.0
        val recovery = progress(bodyPct, -0.20, 0.15, 10)
        val acceleration = progress(buyDelta, -0.05, 0.03, 10)
        val readiness = (distance + depth + share + recovery + acceleration).coerceIn(0, 100)

        val exactT32 = closedDeviation <= -0.40 &&
            closed.close > closed.open &&
            closedBuy >= 0.50 &&
            closedBuy > closedPrevBuy
        val exactCore = exactT32 && belowFour

        val reason = buildString {
            append(String.format(Locale.GERMANY, "готовность %d/100 • VWAP %+.2f%% • BUY %.0f%%", readiness, displayDeviation, displayBuy * 100.0))
            append(drawdown12h?.let { String.format(Locale.GERMANY, " • 12h %.2f%%", it) } ?: " • 12h ?")
            if (btcReturns.isNotEmpty()) append(String.format(Locale.GERMANY, " • BTC1h %+.2f%%", btcReturns.first()))
            if (btcStrongUp) append(" • BTC STRONG-UP")
            append(rel6?.let { String.format(Locale.GERMANY, " • SOL-BTC L6 %+.2f п.п.", it) } ?: " • SOL-BTC L6 ?")
            if (exactCore) append(" • CORE ГОТОВ")
        }

        return T32V660Setup(
            readiness = readiness,
            exactCore = exactCore,
            vwap = displayVwap,
            signalClose = closed.close,
            deviationPercent = displayDeviation,
            buyShare = displayBuy,
            buyShareDelta = buyDelta,
            drawdown12hPercent = drawdown12h,
            belowFour12h = belowFour,
            btcStrongUpRecent = btcStrongUp,
            btcHourlyReturns = btcReturns,
            solBtcRelativeLag6 = rel6,
            reason = reason
        )
    }

    private fun progress(value: Double, low: Double, high: Double, weight: Int): Int {
        if (high <= low) return 0
        return (((value - low) / (high - low)) * weight).toInt().coerceIn(0, weight)
    }

    private fun parse(json: String): List<MarketPoint> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val root = JSONArray(json)
            (0 until root.length()).mapNotNull { i ->
                root.optJSONArray(i)?.let { row ->
                    val close = row.optDouble(4, Double.NaN)
                    val high = row.optDouble(2, Double.NaN)
                    val time = row.optLong(6, 0L)
                    if (close.isFinite() && high.isFinite() && close > 0.0 && high > 0.0 && time > 0L) MarketPoint(time, high, close) else null
                }
            }.sortedBy { it.closeTime }
        }.getOrDefault(emptyList())
    }

    private fun hourReturnEndingLag(rows: List<MarketPoint>, lagHours: Int): Double? {
        if (rows.size < 4) return null
        val end = rows.lastIndex - lagHours * 2
        val start = end - 2
        if (start < 0 || end !in rows.indices) return null
        val a = rows[start].close
        val b = rows[end].close
        if (a <= 0.0 || b <= 0.0) return null
        return (b / a - 1.0) * 100.0
    }

    private fun empty(reason: String) = T32V660Setup(
        readiness = 0,
        exactCore = false,
        vwap = 0.0,
        signalClose = 0.0,
        deviationPercent = 0.0,
        buyShare = 0.0,
        buyShareDelta = 0.0,
        drawdown12hPercent = null,
        belowFour12h = false,
        btcStrongUpRecent = false,
        btcHourlyReturns = emptyList(),
        solBtcRelativeLag6 = null,
        reason = reason
    )
}

internal enum class T32V660Profile(val title: String, val agent: String) {
    CORE("AUTO CORE", "V660_CORE"),
    BTC_GUARD("AUTO BTC GUARD", "V660_BTC_GUARD"),
    SOL_SELECT("AUTO SOL/BTC SELECT", "V660_SOL_SELECT");

    fun allows(setup: T32V660Setup): Boolean = when (this) {
        CORE -> true
        BTC_GUARD -> !setup.btcStrongUpRecent
        SOL_SELECT -> (setup.solBtcRelativeLag6 ?: Double.NEGATIVE_INFINITY) >= 0.40
    }

    fun gateText(setup: T32V660Setup): String = when (this) {
        CORE -> "CORE"
        BTC_GUARD -> if (setup.btcStrongUpRecent) "BLOCK: BTC STRONG-UP 1–3h" else "BTC GUARD OK"
        SOL_SELECT -> setup.solBtcRelativeLag6?.let {
            String.format(Locale.GERMANY, "SOL-BTC L6 %+.2f п.п. (нужно +0,40)", it)
        } ?: "SOL-BTC L6: нет данных"
    }
}

data class T32V660AutoState(
    val cash: Double = 1000.0,
    val coins: Double = 0.0,
    val entryPrice: Double = 0.0,
    val entryAt: Long = 0L,
    val targetPrice: Double = 0.0,
    val readiness: Int = 0,
    val reason: String = "Новый счёт V6.6 • ожидание",
    val pendingLimit: Double = 0.0,
    val pendingUntil: Long = 0L,
    val dayKey: String = "",
    val entriesToday: Int = 0,
    val updatedAt: Long = 0L,
    val trades: List<HumanFactorTrade> = emptyList()
) {
    val inPosition get() = coins > 0.0
    fun value(price: Double) = cash + coins * max(price, 0.0)
}

private class T32V660AutoEngine(
    private val prefsName: String,
    private val profile: T32V660Profile
) {
    private val key = "state"

    fun state(context: Context): T32V660AutoState = runCatching {
        val j = JSONObject(context.getSharedPreferences(prefsName, 0).getString(key, "{}") ?: "{}")
        T32V660AutoState(
            cash = j.optDouble("cash", 1000.0),
            coins = j.optDouble("coins"),
            entryPrice = j.optDouble("entryPrice"),
            entryAt = j.optLong("entryAt"),
            targetPrice = j.optDouble("targetPrice"),
            readiness = j.optInt("readiness"),
            reason = j.optString("reason", "Новый счёт V6.6 • ожидание"),
            pendingLimit = j.optDouble("pendingLimit"),
            pendingUntil = j.optLong("pendingUntil"),
            dayKey = j.optString("dayKey", ""),
            entriesToday = j.optInt("entriesToday"),
            updatedAt = j.optLong("updatedAt"),
            trades = (j.optJSONArray("trades") ?: JSONArray()).let { a ->
                (0 until a.length()).map { HumanFactorTrade.from(a.getJSONObject(it)) }
            }
        )
    }.getOrDefault(T32V660AutoState())

    private fun save(context: Context, s: T32V660AutoState) {
        val j = JSONObject()
            .put("cash", s.cash)
            .put("coins", s.coins)
            .put("entryPrice", s.entryPrice)
            .put("entryAt", s.entryAt)
            .put("targetPrice", s.targetPrice)
            .put("readiness", s.readiness)
            .put("reason", s.reason)
            .put("pendingLimit", s.pendingLimit)
            .put("pendingUntil", s.pendingUntil)
            .put("dayKey", s.dayKey)
            .put("entriesToday", s.entriesToday)
            .put("updatedAt", s.updatedAt)
            .put("trades", JSONArray(s.trades.takeLast(300).map { it.json() }))
        context.getSharedPreferences(prefsName, 0).edit().putString(key, j.toString()).commit()
    }

    @Synchronized
    fun sync(context: Context, now: Long = System.currentTimeMillis()): T32V660AutoState {
        var s = state(context)
        val today = utcDay(now)
        if (s.dayKey != today) s = s.copy(dayKey = today, entriesToday = 0)

        val venue = BitpandaFusionStore.state(context)
        val fallback = PaperExecutionPolicy.displayPrice(PumpBotEngine.snapshot(context), now)
        val bid = venue.bid.takeIf { venue.fresh(now) } ?: fallback
        val ask = venue.ask.takeIf { venue.fresh(now) }

        if (s.inPosition) {
            val target = s.targetPrice.takeIf { it > 0.0 } ?: T32CostPolicyV660.targetPrice(s.entryPrice)
            val net = T32CostPolicyV660.netPercent(s.entryPrice, bid)
            val hitTarget = bid >= target
            val hitStop = net <= T32CostPolicyV660.STOP_NET_PERCENT
            val hitTime = now - s.entryAt >= T32CostPolicyV660.MAX_HOLD_MILLIS
            if (hitTarget || hitStop || hitTime) {
                val gross = s.coins * bid
                val fee = T32CostPolicyV660.sellFee(s.coins, bid)
                val pnl = gross - fee - T32CostPolicyV660.entryCost(s.coins, s.entryPrice)
                val why = when {
                    hitTarget -> "TP +2,5% NET"
                    hitStop -> "STOP -1,2% NET"
                    else -> "TIME 120 МИН"
                }
                s = s.copy(
                    cash = gross - fee,
                    coins = 0.0,
                    entryPrice = 0.0,
                    entryAt = 0L,
                    targetPrice = 0.0,
                    reason = "AUTO EXIT • $why",
                    updatedAt = now,
                    trades = s.trades + HumanFactorTrade(now, "SELL", bid, s.coins, fee, pnl, "${profile.name};$why")
                )
                save(context, s)
                UnifiedResearchLog.record(context, profile.agent, "SELL", "bid=$bid; net=$net; pnl=$pnl; $why", now)
                return s
            }
            val setup = T32V660Policy.evaluate(context)
            s = s.copy(
                readiness = setup.readiness,
                targetPrice = target,
                reason = String.format(Locale.GERMANY, "ПОЗИЦИЯ • NET %+.2f%% • TP +2,5%% • новая готовность %d/100", net, setup.readiness),
                updatedAt = now
            )
            save(context, s)
            return s
        }

        val setup = T32V660Policy.evaluate(context)
        if (s.pendingUntil > 0L && now > s.pendingUntil) s = s.copy(pendingLimit = 0.0, pendingUntil = 0L)

        val dailyBlocked = s.entriesToday >= T32CostPolicyV660.MAX_ENTRIES_PER_UTC_DAY
        val profileAllowed = profile.allows(setup)

        if (!dailyBlocked && profileAllowed && s.pendingLimit > 0.0 && now <= s.pendingUntil && ask != null && ask <= s.pendingLimit) {
            val fill = T32CostPolicyV660.buyAllCash(s.cash, ask)
            if (fill.coins > 0.0) {
                val target = T32CostPolicyV660.targetPrice(ask)
                s = s.copy(
                    cash = 0.0,
                    coins = fill.coins,
                    entryPrice = ask,
                    entryAt = now,
                    targetPrice = target,
                    pendingLimit = 0.0,
                    pendingUntil = 0L,
                    entriesToday = s.entriesToday + 1,
                    readiness = setup.readiness,
                    reason = "AUTO BUY • ${profile.gateText(setup)} • TP +2,5 / SL -1,2 / 120m",
                    updatedAt = now,
                    trades = s.trades + HumanFactorTrade(now, "BUY", ask, fill.coins, fill.feeEur, 0.0, profile.name)
                )
                save(context, s)
                UnifiedResearchLog.record(context, profile.agent, "BUY", "ask=$ask; limitFill=true; ${setup.reason}", now)
                return s
            }
        }

        if (!dailyBlocked && profileAllowed && setup.exactCore && setup.signalClose > 0.0 && s.pendingLimit <= 0.0) {
            val limit = setup.signalClose * (1.0 - T32CostPolicyV660.LIMIT_DISCOUNT)
            s = s.copy(pendingLimit = limit, pendingUntil = now + T32CostPolicyV660.LIMIT_TTL_MILLIS)
            if (ask != null && ask <= limit) {
                save(context, s)
                return sync(context, now)
            }
        }

        val status = when {
            dailyBlocked -> "ЛИМИТ 2 ВХОДА/СУТКИ"
            !profileAllowed -> profile.gateText(setup)
            s.pendingLimit > 0.0 -> String.format(Locale.GERMANY, "LIMIT €%.8f • ждём до 2 мин", s.pendingLimit)
            else -> profile.gateText(setup)
        }
        s = s.copy(readiness = setup.readiness, reason = "$status • ${setup.reason}", updatedAt = now)
        save(context, s)
        UnifiedResearchLog.record(context, profile.agent, "CYCLE", s.reason, now)
        return s
    }

    private fun utcDay(now: Long): String = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(now))
}

object V660CoreStore {
    private val engine = T32V660AutoEngine("v660_auto_core_fresh", T32V660Profile.CORE)
    fun state(context: Context) = engine.state(context)
    fun sync(context: Context, now: Long = System.currentTimeMillis()) = engine.sync(context, now)
}

object V660BtcGuardStore {
    private val engine = T32V660AutoEngine("v660_auto_btc_guard_fresh", T32V660Profile.BTC_GUARD)
    fun state(context: Context) = engine.state(context)
    fun sync(context: Context, now: Long = System.currentTimeMillis()) = engine.sync(context, now)
}

object V660SolSelectStore {
    private val engine = T32V660AutoEngine("v660_auto_sol_select_fresh", T32V660Profile.SOL_SELECT)
    fun state(context: Context) = engine.state(context)
    fun sync(context: Context, now: Long = System.currentTimeMillis()) = engine.sync(context, now)
}

object T32NetworkV660 {
    fun syncAll(context: Context, now: Long = System.currentTimeMillis()) {
        listOf<Pair<String, () -> Unit>>(
            "V660_CORE" to { V660CoreStore.sync(context, now) },
            "V660_BTC_GUARD" to { V660BtcGuardStore.sync(context, now) },
            "V660_SOL_SELECT" to { V660SolSelectStore.sync(context, now) }
        ).forEach { (agent, action) ->
            runCatching(action).onFailure {
                UnifiedResearchLog.record(context, agent, "ERROR", "${it.javaClass.simpleName}: ${it.message.orEmpty().take(180)}", now)
            }
        }
    }
}
