package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max

data class AppPaperTrade(
    val time: Long,
    val candleTime: Long,
    val action: String,
    val price: Double,
    val amount: Double,
    val fee: Double,
    val pnlEur: Double,
    val reason: String
) {
    fun toJson(): JSONObject = JSONObject()
        .put("time", time)
        .put("candleTime", candleTime)
        .put("action", action)
        .put("price", price)
        .put("amount", amount)
        .put("fee", fee)
        .put("pnlEur", pnlEur)
        .put("reason", reason)

    companion object {
        fun fromJson(value: JSONObject) = AppPaperTrade(
            time = value.optLong("time"),
            candleTime = value.optLong("candleTime"),
            action = value.optString("action"),
            price = value.optDouble("price"),
            amount = value.optDouble("amount"),
            fee = value.optDouble("fee"),
            pnlEur = value.optDouble("pnlEur"),
            reason = value.optString("reason")
        )
    }
}

data class AppPaperDecision(
    val time: Long,
    val candleTime: Long,
    val action: String,
    val price: Double,
    val reason: String,
    val positionAfter: Boolean,
    val valueAfter: Double
) {
    fun toJson(): JSONObject = JSONObject()
        .put("time", time)
        .put("candleTime", candleTime)
        .put("action", action)
        .put("price", price)
        .put("reason", reason)
        .put("positionAfter", positionAfter)
        .put("valueAfter", valueAfter)

    companion object {
        fun fromJson(value: JSONObject) = AppPaperDecision(
            time = value.optLong("time"),
            candleTime = value.optLong("candleTime"),
            action = value.optString("action", "WAIT"),
            price = value.optDouble("price"),
            reason = value.optString("reason"),
            positionAfter = value.optBoolean("positionAfter"),
            valueAfter = value.optDouble("valueAfter", AppPaperPortfolio.START_BALANCE)
        )
    }
}

data class AppPaperPortfolio(
    val cashEur: Double = START_BALANCE,
    val pumpAmount: Double = 0.0,
    val entryPrice: Double = 0.0,
    val entryTime: Long = 0L,
    val strategyMode: String = StrategyV2.MODE_NONE,
    val entryAtr: Double = 0.0,
    val invalidationPrice: Double = 0.0,
    val highestClose: Double = 0.0,
    val partialTaken: Boolean = false,
    val partialCandle: Long = 0L,
    val lastCandleTime: Long = 0L,
    val totalFeesEur: Double = 0.0,
    val peakValueEur: Double = START_BALANCE,
    val maxDrawdownPercent: Double = 0.0,
    val trades: List<AppPaperTrade> = emptyList(),
    val decisions: List<AppPaperDecision> = emptyList()
) {
    val inPosition: Boolean get() = pumpAmount > 0.0
    fun value(price: Double): Double = cashEur + pumpAmount * max(0.0, price)
    fun profit(price: Double): Double = value(price) - START_BALANCE
    fun profitPercent(price: Double): Double = profit(price) / START_BALANCE * 100.0
    val closedTrades: Int get() = trades.count { it.action == "SELL" }
    val winningTrades: Int get() = trades.count { it.action == "SELL" && it.pnlEur > 0.0 }
    val winRatePercent: Double
        get() = if (closedTrades == 0) 0.0 else winningTrades.toDouble() / closedTrades * 100.0

    companion object {
        const val START_BALANCE = 1000.0
    }
}

/**
 * Independent virtual account for the built-in StrategyV2 algorithm.
 * It never reads or modifies the user's manual position or the Gemini portfolio.
 */
object AppPaperTrader {
    const val FEE_RATE = 0.0015
    internal const val DECISION_RETENTION_MILLIS = 183L * 24L * 60L * 60L * 1000L
    private const val MAX_DECISIONS = 9_000

    fun apply(
        current: AppPaperPortfolio,
        evaluation: AppPaperEvaluation,
        now: Long = System.currentTimeMillis()
    ): AppPaperPortfolio {
        if (evaluation.candleTime <= 0L ||
            evaluation.price <= 0.0 ||
            evaluation.candleTime <= current.lastCandleTime
        ) return current

        var cash = current.cashEur
        var amount = current.pumpAmount
        var entryPrice = current.entryPrice
        var entryTime = current.entryTime
        var strategyMode = current.strategyMode
        var entryAtr = current.entryAtr
        var invalidationPrice = current.invalidationPrice
        var highestClose = max(current.highestClose, evaluation.highestClose)
        var partialTaken = current.partialTaken
        var partialCandle = current.partialCandle
        var fees = current.totalFeesEur
        var trades = current.trades
        val action = evaluation.action.uppercase()

        when {
            action == "BUY" && !current.inPosition && cash > 0.01 -> {
                val allocation = cash
                val fee = allocation * FEE_RATE
                amount = (allocation - fee) / evaluation.price
                cash = 0.0
                entryPrice = evaluation.price
                entryTime = evaluation.candleTime
                strategyMode = evaluation.strategyMode
                entryAtr = evaluation.entryAtr
                invalidationPrice = evaluation.invalidationPrice
                highestClose = evaluation.price
                partialTaken = false
                partialCandle = 0L
                fees += fee
                trades = addTrade(
                    trades,
                    AppPaperTrade(
                        now, evaluation.candleTime, "BUY", evaluation.price,
                        amount, fee, 0.0, evaluation.reason
                    )
                )
            }
            action == StrategyV2.ACTION_SELL_HALF && current.inPosition && !current.partialTaken -> {
                val soldAmount = amount / 2.0
                val gross = soldAmount * evaluation.price
                val fee = gross * FEE_RATE
                val matchingBuyFee = trades.lastOrNull { it.action == "BUY" }?.fee ?: 0.0
                val allocatedBuyFee = matchingBuyFee / 2.0
                val pnl = gross - fee - soldAmount * entryPrice - allocatedBuyFee
                cash += gross - fee
                amount -= soldAmount
                partialTaken = true
                partialCandle = evaluation.candleTime
                fees += fee
                trades = addTrade(
                    trades,
                    AppPaperTrade(
                        now, evaluation.candleTime, "SELL_HALF", evaluation.price,
                        soldAmount, fee, pnl, evaluation.reason
                    )
                )
            }
            action == StrategyV2.ACTION_SELL && current.inPosition -> {
                val soldAmount = amount
                val gross = soldAmount * evaluation.price
                val fee = gross * FEE_RATE
                val matchingBuyFee = trades.lastOrNull { it.action == "BUY" }?.fee ?: 0.0
                val remainingBuyFee = if (partialTaken) matchingBuyFee / 2.0 else matchingBuyFee
                val partialPnl = trades.asReversed()
                    .takeWhile { it.action != "BUY" }
                    .filter { it.action == "SELL_HALF" }
                    .sumOf { it.pnlEur }
                val pnl = gross - fee - soldAmount * entryPrice - remainingBuyFee + partialPnl
                cash += gross - fee
                amount = 0.0
                entryPrice = 0.0
                entryTime = 0L
                strategyMode = StrategyV2.MODE_NONE
                entryAtr = 0.0
                invalidationPrice = 0.0
                highestClose = 0.0
                partialTaken = false
                partialCandle = 0L
                fees += fee
                trades = addTrade(
                    trades,
                    AppPaperTrade(
                        now, evaluation.candleTime, "SELL", evaluation.price,
                        soldAmount, fee, pnl, evaluation.reason
                    )
                )
            }
        }

        val value = cash + amount * evaluation.price
        val peak = max(current.peakValueEur, value)
        val drawdown = if (peak > 0.0) (1.0 - value / peak) * 100.0 else 0.0
        val cutoff = now - DECISION_RETENTION_MILLIS
        val decision = AppPaperDecision(
            now,
            evaluation.candleTime,
            action,
            evaluation.price,
            evaluation.reason.take(600),
            amount > 0.0,
            value
        )
        return current.copy(
            cashEur = cash,
            pumpAmount = amount,
            entryPrice = entryPrice,
            entryTime = entryTime,
            strategyMode = strategyMode,
            entryAtr = entryAtr,
            invalidationPrice = invalidationPrice,
            highestClose = highestClose,
            partialTaken = partialTaken,
            partialCandle = partialCandle,
            lastCandleTime = evaluation.candleTime,
            totalFeesEur = fees,
            peakValueEur = peak,
            maxDrawdownPercent = max(current.maxDrawdownPercent, drawdown),
            trades = trades,
            decisions = (current.decisions.filter { it.time >= cutoff } + decision)
                .takeLast(MAX_DECISIONS)
        )
    }

    private fun addTrade(
        previous: List<AppPaperTrade>,
        trade: AppPaperTrade
    ): List<AppPaperTrade> = previous + trade
}

internal object AppTradeAlertPolicy {
    fun newlyExecutedTrades(
        before: AppPaperPortfolio,
        after: AppPaperPortfolio
    ): List<AppPaperTrade> {
        if (after.trades.size <= before.trades.size) return emptyList()
        return after.trades.drop(before.trades.size)
    }
}

data class AppPaperSyncResult(
    val portfolio: AppPaperPortfolio,
    val tradeAlerted: Boolean
)

object AppPaperStore {
    // New competition epoch. V4.22 remains untouched in app_paper_v317.
    private const val PREFS = "app_paper_v5_research"
    private const val KEY_PORTFOLIO = "portfolio"
    private const val KEY_PORTFOLIO_BACKUP = "portfolio_backup_v322"
    private const val KEY_PENDING_ALERTS = "pending_trade_alerts_v322"
    private const val KEY_STORAGE_ERROR = "portfolio_storage_error_v322"

    fun state(context: Context): AppPaperPortfolio {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = p.getString(KEY_PORTFOLIO, null)
        loadOrNull(raw)?.let { return it }
        val recovered = loadOrNull(p.getString(KEY_PORTFOLIO_BACKUP, null))
        if (recovered != null) {
            p.edit()
                .putString(KEY_PORTFOLIO, toJson(recovered).toString())
                .remove(KEY_STORAGE_ERROR)
                .commit()
            return recovered
        }
        if (!raw.isNullOrBlank()) {
            p.edit().putString(
                KEY_STORAGE_ERROR,
                "Повреждены основные данные APP; торговля остановлена до восстановления или сброса"
            ).commit()
        }
        return AppPaperPortfolio()
    }

    @Synchronized
    fun sync(context: Context): AppPaperPortfolio {
        return syncWithAlerts(context).portfolio
    }

    @Synchronized
    fun syncWithAlerts(context: Context): AppPaperSyncResult {
        flushPendingAlerts(context)
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = state(context)
        check(p.getString(KEY_STORAGE_ERROR, "").isNullOrBlank()) {
            p.getString(KEY_STORAGE_ERROR, "Ошибка хранилища APP").orEmpty()
        }
        val rawEvaluation = PumpBotEngine.evaluateAppPaper(context, current)
        val evaluation = if (rawEvaluation.candleTime > current.lastCandleTime) {
            if (PaperExecutionPolicy.isTradeAction(rawEvaluation.action)) {
                val quote = GeminiExecutionQuoteClient().fetch()
                PaperExecutionPolicy.prepareAppEvaluation(
                    rawEvaluation,
                    quote.priceEur,
                    quote.receivedAt
                )
            } else {
                rawEvaluation.copy(
                    price = PaperExecutionPolicy.displayPrice(PumpBotEngine.snapshot(context))
                        .takeIf { it > 0.0 } ?: rawEvaluation.price
                )
            }
        } else {
            rawEvaluation
        }
        val next = AppPaperTrader.apply(current, evaluation)
        val trades = AppTradeAlertPolicy.newlyExecutedTrades(current, next)
        if (next != current || trades.isNotEmpty()) {
            save(context, next, (pendingAlerts(context) + trades).distinctBy(::alertId))
        }
        flushPendingAlerts(context)
        return AppPaperSyncResult(next, trades.isNotEmpty())
    }

    fun reset(context: Context) {
        runCatching { ResearchPerformanceLedger.capture(context) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun save(
        context: Context,
        value: AppPaperPortfolio,
        pending: List<AppPaperTrade> = pendingAlerts(context)
    ) {
        val raw = toJson(value).toString()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PORTFOLIO, raw)
            .putString(KEY_PORTFOLIO_BACKUP, raw)
            .putString(KEY_PENDING_ALERTS, JSONArray(pending.map { it.toJson() }).toString())
            .remove(KEY_STORAGE_ERROR)
            .commit()
    }

    private fun loadOrNull(raw: String?): AppPaperPortfolio? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(raw)
            val tradesJson = json.optJSONArray("trades") ?: JSONArray()
            val decisionsJson = json.optJSONArray("decisions") ?: JSONArray()
            AppPaperPortfolio(
                cashEur = json.optDouble("cashEur", AppPaperPortfolio.START_BALANCE),
                pumpAmount = json.optDouble("pumpAmount"),
                entryPrice = json.optDouble("entryPrice"),
                entryTime = json.optLong("entryTime"),
                strategyMode = json.optString("strategyMode", StrategyV2.MODE_NONE),
                entryAtr = json.optDouble("entryAtr"),
                invalidationPrice = json.optDouble("invalidationPrice"),
                highestClose = json.optDouble("highestClose"),
                partialTaken = json.optBoolean("partialTaken"),
                partialCandle = json.optLong("partialCandle"),
                lastCandleTime = json.optLong("lastCandleTime"),
                totalFeesEur = json.optDouble("totalFeesEur"),
                peakValueEur = json.optDouble("peakValueEur", AppPaperPortfolio.START_BALANCE),
                maxDrawdownPercent = json.optDouble("maxDrawdownPercent"),
                trades = (0 until tradesJson.length()).mapNotNull {
                    tradesJson.optJSONObject(it)?.let(AppPaperTrade::fromJson)
                },
                decisions = (0 until decisionsJson.length()).mapNotNull {
                    decisionsJson.optJSONObject(it)?.let(AppPaperDecision::fromJson)
                }
            )
        }.getOrNull()
    }

    private fun pendingAlerts(context: Context): List<AppPaperTrade> = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PENDING_ALERTS, "[]").orEmpty()
        val json = JSONArray(raw)
        (0 until json.length()).mapNotNull { index ->
            json.optJSONObject(index)?.let(AppPaperTrade::fromJson)
        }
    }.getOrDefault(emptyList())

    private fun flushPendingAlerts(context: Context) {
        val pending = pendingAlerts(context)
        if (pending.isEmpty()) return
        var delivered = 0
        for (trade in pending) {
            val success = runCatching { PumpAlert.showAppTrade(context, trade) }.isSuccess
            if (!success) break
            delivered++
        }
        if (delivered > 0) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(
                    KEY_PENDING_ALERTS,
                    JSONArray(pending.drop(delivered).map { it.toJson() }).toString()
                )
                .commit()
        }
    }

    private fun alertId(trade: AppPaperTrade): String =
        "${trade.time}:${trade.candleTime}:${trade.action}"

    private fun toJson(value: AppPaperPortfolio): JSONObject = JSONObject()
        .put("cashEur", value.cashEur)
        .put("pumpAmount", value.pumpAmount)
        .put("entryPrice", value.entryPrice)
        .put("entryTime", value.entryTime)
        .put("strategyMode", value.strategyMode)
        .put("entryAtr", value.entryAtr)
        .put("invalidationPrice", value.invalidationPrice)
        .put("highestClose", value.highestClose)
        .put("partialTaken", value.partialTaken)
        .put("partialCandle", value.partialCandle)
        .put("lastCandleTime", value.lastCandleTime)
        .put("totalFeesEur", value.totalFeesEur)
        .put("peakValueEur", value.peakValueEur)
        .put("maxDrawdownPercent", value.maxDrawdownPercent)
        .put("trades", JSONArray(value.trades.map { it.toJson() }))
        .put("decisions", JSONArray(value.decisions.map { it.toJson() }))
}
