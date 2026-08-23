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

private data class RetestState(
    val armedAt: Long = 0L,
    val anchorAsk: Double = 0.0,
    val lowAsk: Double = 0.0,
    val pulledBack: Boolean = false
) {
    val armed: Boolean get() = armedAt > 0L && anchorAsk > 0.0
    fun toJson() = JSONObject().put("armedAt", armedAt).put("anchorAsk", anchorAsk)
        .put("lowAsk", lowAsk).put("pulledBack", pulledBack)
    companion object {
        fun fromJson(j: JSONObject) = RetestState(
            j.optLong("armedAt"), j.optDouble("anchorAsk"), j.optDouble("lowAsk"),
            j.optBoolean("pulledBack")
        )
    }
}

private data class PumpVariantConfig(
    val account: String,
    val label: String,
    val prefs: String,
    val mode: PumpProfitModeV526,
    val targetNet: Double,
    val stopNet: Double,
    val retest: Boolean = false,
    val requireAppEvidence: Boolean = false
)

private class PumpVariantStore(private val config: PumpVariantConfig) {
    private val portfolioKey = "portfolio"
    private val backupKey = "portfolio_backup"
    private val stabilityKey = "stability"
    private val statusKey = "last_status"
    private val statusAtKey = "last_status_at"
    private val retestKey = "retest_state"

    fun state(context: Context): FusionSimPortfolio {
        val p = prefs(context)
        return parse(p.getString(portfolioKey, null))
            ?: parse(p.getString(backupKey, null))
            ?: FusionSimPortfolio()
    }

    fun status(context: Context): String = prefs(context)
        .getString(statusKey, "${config.label} • ждём первый подтверждённый вход").orEmpty()

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
            val entry = PumpProfitEngineV526.evaluateEntry(config.mode, previous, observation, now)
            if (entry.action != "BUY") {
                if (config.retest && retest(context).armed && !PumpProfitEngineV526.isFastCandidate(config.mode, observation)) {
                    saveRetest(context, RetestState())
                }
                return finish(context, marked, entry.nextState, entry.reason, 0.0)
            }
            if (config.requireAppEvidence && !appEvidence(context)) {
                return finish(
                    context, marked, entry.nextState,
                    "SAFE WAIT: быстрый поток подтверждён, но локальная APP-модель ещё не видит закрытый pullback/reclaim; Pro не вызывается",
                    0.0
                )
            }
            if (config.retest) {
                val decision = evaluateRetest(context, observation, market.ask, now)
                if (!decision.first) return finish(context, marked, entry.nextState, decision.second, 0.0)
            }
            return buy(context, marked, entry.nextState, market, now,
                if (config.retest) "RETEST BUY: откат и возврат покупателей подтверждены; ${entry.reason}" else entry.reason)
        }

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

    private fun evaluateRetest(
        context: Context,
        observation: SharedFusionEntryObservation,
        ask: Double,
        now: Long
    ): Pair<Boolean, String> {
        val old = retest(context)
        if (!old.armed) {
            saveRetest(context, RetestState(now, ask, ask, false))
            return false to "RETEST ARMED: ранний импульс найден; до 5 минут ждём откат 0,25–0,60% и возврат +0,12%, не покупаем вершину"
        }
        if (now - old.armedAt > 5L * 60L * 1000L || ask > old.anchorAsk * 1.004) {
            saveRetest(context, RetestState())
            return false to "RETEST CANCEL: окно 5 минут закончилось или цена ушла вверх без безопасного отката"
        }
        val low = minOf(old.lowAsk.takeIf { it > 0.0 } ?: ask, ask)
        val pullback = old.pulledBack || low <= old.anchorAsk * 0.9975
        val tooDeep = low < old.anchorAsk * 0.994
        val rebound = pullback && ask >= low * 1.0012
        val flowOkay = PumpProfitEngineV526.isFastCandidate(config.mode, observation)
        val next = old.copy(lowAsk = low, pulledBack = pullback)
        if (tooDeep || !flowOkay) {
            saveRetest(context, RetestState())
            return false to "RETEST CANCEL: откат глубже 0,60% или покупательский поток потерял качество"
        }
        saveRetest(context, if (rebound) RetestState() else next)
        return if (rebound) true to "RETEST CONFIRMED" else false to
            "RETEST WAIT: минимум ${signed((low / old.anchorAsk - 1.0) * 100.0)}%; нужен откат ≥0,25% и отскок +0,12%"
    }

    private fun appEvidence(context: Context): Boolean = runCatching {
        val candles = PumpBotEngine.snapshot(context).chart.candles
        if (candles.size < 50) false else {
            val decision = ResearchDecisionEngine.evaluate(candles)
            decision.status == ResearchSignalStatus.SHADOW_CANDIDATE ||
                decision.status == ResearchSignalStatus.ACTIONABLE
        }
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
            cashEur = 0.0, pumpAmount = amount, entryPrice = market.ask, entryCostEur = allocation,
            lastDecisionId = now, totalFeesEur = value.totalFeesEur + fee,
            trades = (value.trades + trade).takeLast(5_000),
            decisions = (value.decisions + decision).takeLast(9_000)
        )
        val nextState = state.copy(entryStreak = 0, entryCandidateAt = 0L, peakBid = market.bid,
            profitDefenseArmed = false, cooldownUntil = 0L)
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
        val next = value.copy(cashEur = cash, pumpAmount = 0.0, entryPrice = 0.0, entryCostEur = 0.0,
            lastDecisionId = now, totalFeesEur = value.totalFeesEur + fee, peakValueEur = peak,
            maxDrawdownPercent = max(value.maxDrawdownPercent, drawdown),
            trades = (value.trades + trade).takeLast(5_000), decisions = (value.decisions + decision).takeLast(9_000))
        val protective = reason.contains("HARD_STOP") || reason.contains("EARLY_RISK_EXIT")
        val cooldown = FusionStabilityPolicy.cooldownAfterExit(evaluated.copy(cooldownUntil = previous.cooldownUntil), pnl, protective, now)
        val status = "SELL ${signed(percent)}% NET: $reason"
        save(context, next, cooldown, status)
        UnifiedResearchLog.record(context, config.account, "SELL", status, now)
        return PumpVariantSyncResult(next, status, percent)
    }

    private fun finish(context: Context, value: FusionSimPortfolio, state: FusionStabilityState, text: String, net: Double): PumpVariantSyncResult {
        save(context, value, state, text)
        return PumpVariantSyncResult(value, text, net)
    }

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

    private fun retest(context: Context): RetestState = runCatching {
        RetestState.fromJson(JSONObject(prefs(context).getString(retestKey, null).orEmpty()))
    }.getOrDefault(RetestState())

    private fun saveRetest(context: Context, value: RetestState) {
        prefs(context).edit().putString(retestKey, value.toJson().toString()).apply()
    }

    private fun save(context: Context, value: FusionSimPortfolio, state: FusionStabilityState, status: String) {
        val raw = FusionSimStore.toJson(value).toString()
        prefs(context).edit().putString(portfolioKey, raw).putString(backupKey, raw)
            .putString(stabilityKey, state.toJson().toString()).putString(statusKey, status.take(1200))
            .putLong(statusAtKey, System.currentTimeMillis()).commit()
    }

    private fun parse(raw: String?): FusionSimPortfolio? = if (raw.isNullOrBlank()) null else runCatching {
        val j = JSONObject(raw)
        val trades = j.optJSONArray("trades") ?: JSONArray()
        val decisions = j.optJSONArray("decisions") ?: JSONArray()
        FusionSimPortfolio(
            cashEur = j.optDouble("cashEur", FusionSimPortfolio.START_BALANCE), pumpAmount = j.optDouble("pumpAmount"),
            entryPrice = j.optDouble("entryPrice"), entryCostEur = j.optDouble("entryCostEur"),
            lastDecisionId = j.optLong("lastDecisionId"), totalFeesEur = j.optDouble("totalFeesEur"),
            peakValueEur = j.optDouble("peakValueEur", FusionSimPortfolio.START_BALANCE),
            maxDrawdownPercent = j.optDouble("maxDrawdownPercent"),
            trades = (0 until trades.length()).mapNotNull { trades.optJSONObject(it)?.let(FusionSimTrade::fromJson) },
            decisions = (0 until decisions.length()).mapNotNull { decisions.optJSONObject(it)?.let(FusionSimDecision::fromJson) }
        )
    }.getOrNull()

    private fun prefs(context: Context) = context.getSharedPreferences(config.prefs, Context.MODE_PRIVATE)
    private fun signed(value: Double) = String.format(java.util.Locale.GERMANY, "%+.2f", value)
}

object PumpMachineRetestStore {
    private val store = PumpVariantStore(PumpVariantConfig(
        "PUMP_MACHINE_RETEST", "PUMP MACHINE RETEST", "pump_machine_retest_paper_v529",
        PumpProfitModeV526.PUMP_RETEST, 2.00, -1.10, retest = true
    ))
    fun state(c: Context) = store.state(c)
    fun lastStatus(c: Context) = store.status(c)
    fun lastStatusAt(c: Context) = store.statusAt(c)
    fun sync(c: Context, now: Long = System.currentTimeMillis()) = store.sync(c, now)
    fun netValue(c: Context, now: Long = System.currentTimeMillis()) = store.netValue(c, now)
    fun tradeNetPercent(c: Context, now: Long = System.currentTimeMillis()) = store.tradeNet(c, now)
    fun toJson(v: FusionSimPortfolio) = store.toJson(v)
}

object PumpMachineSafeStore {
    private val store = PumpVariantStore(PumpVariantConfig(
        "PUMP_MACHINE_SAFE", "PUMP MACHINE SAFE", "pump_machine_safe_paper_v529",
        PumpProfitModeV526.PUMP_SAFE, 1.15, -0.75, requireAppEvidence = true
    ))
    fun state(c: Context) = store.state(c)
    fun lastStatus(c: Context) = store.status(c)
    fun lastStatusAt(c: Context) = store.statusAt(c)
    fun sync(c: Context, now: Long = System.currentTimeMillis()) = store.sync(c, now)
    fun netValue(c: Context, now: Long = System.currentTimeMillis()) = store.netValue(c, now)
    fun tradeNetPercent(c: Context, now: Long = System.currentTimeMillis()) = store.tradeNet(c, now)
    fun toJson(v: FusionSimPortfolio) = store.toJson(v)
}
