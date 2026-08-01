package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max

data class UserPaperTrade(
    val time: Long,
    val action: String,
    val price: Double,
    val amount: Double,
    val fee: Double,
    val pnlEur: Double
) {
    fun toJson(): JSONObject = JSONObject()
        .put("time", time)
        .put("action", action)
        .put("price", price)
        .put("amount", amount)
        .put("fee", fee)
        .put("pnlEur", pnlEur)

    companion object {
        fun fromJson(value: JSONObject) = UserPaperTrade(
            time = value.optLong("time"),
            action = value.optString("action"),
            price = value.optDouble("price"),
            amount = value.optDouble("amount"),
            fee = value.optDouble("fee"),
            pnlEur = value.optDouble("pnlEur")
        )
    }
}

data class UserPaperPortfolio(
    val cashEur: Double = START_BALANCE,
    val pumpAmount: Double = 0.0,
    val entryPrice: Double = 0.0,
    val entryTime: Long = 0L,
    val entryFeeEur: Double = 0.0,
    val totalFeesEur: Double = 0.0,
    val peakValueEur: Double = START_BALANCE,
    val maxDrawdownPercent: Double = 0.0,
    val trades: List<UserPaperTrade> = emptyList()
) {
    val inPosition: Boolean get() = pumpAmount > 0.0
    fun value(price: Double): Double = cashEur + pumpAmount * max(0.0, price)
    fun profit(price: Double): Double = value(price) - START_BALANCE
    fun profitPercent(price: Double): Double = profit(price) / START_BALANCE * 100.0

    companion object {
        const val START_BALANCE = 1000.0
    }
}

/** Virtual €1,000 account controlled only by the user's manual BUY/SELL buttons. */
object UserPaperTrader {
    const val FEE_RATE = 0.0015

    fun buy(
        current: UserPaperPortfolio,
        price: Double,
        at: Long
    ): UserPaperPortfolio {
        if (current.inPosition || current.cashEur <= 0.01 || price <= 0.0 || !price.isFinite()) {
            return current
        }
        val fee = current.cashEur * FEE_RATE
        val amount = (current.cashEur - fee) / price
        return updateRisk(
            current.copy(
                cashEur = 0.0,
                pumpAmount = amount,
                entryPrice = price,
                entryTime = at,
                entryFeeEur = fee,
                totalFeesEur = current.totalFeesEur + fee,
                trades = current.trades + UserPaperTrade(
                    at, "BUY", price, amount, fee, 0.0
                )
            ),
            price
        )
    }

    fun sell(
        current: UserPaperPortfolio,
        price: Double,
        at: Long
    ): UserPaperPortfolio {
        if (!current.inPosition || price <= 0.0 || !price.isFinite()) return current
        val gross = current.pumpAmount * price
        val fee = gross * FEE_RATE
        val pnl = gross - fee - current.pumpAmount * current.entryPrice - current.entryFeeEur
        return updateRisk(
            current.copy(
                cashEur = current.cashEur + gross - fee,
                pumpAmount = 0.0,
                entryPrice = 0.0,
                entryTime = 0L,
                entryFeeEur = 0.0,
                totalFeesEur = current.totalFeesEur + fee,
                trades = current.trades + UserPaperTrade(
                    at, "SELL", price, current.pumpAmount, fee, pnl
                )
            ),
            price
        )
    }

    fun markToMarket(current: UserPaperPortfolio, price: Double): UserPaperPortfolio =
        updateRisk(current, price)

    private fun updateRisk(
        current: UserPaperPortfolio,
        price: Double
    ): UserPaperPortfolio {
        if (price <= 0.0 || !price.isFinite()) return current
        val value = current.value(price)
        val peak = max(current.peakValueEur, value)
        val drawdown = if (peak > 0.0) (1.0 - value / peak) * 100.0 else 0.0
        return current.copy(
            peakValueEur = peak,
            maxDrawdownPercent = max(current.maxDrawdownPercent, drawdown)
        )
    }
}

object UserPaperStore {
    private const val PREFS = "user_paper_v318"
    private const val KEY_PORTFOLIO = "portfolio"
    private const val KEY_INITIALIZED = "initialized"

    @Synchronized
    fun state(context: Context): UserPaperPortfolio {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            val migrated = migrate(ManualPositionStore.trades(context))
            save(context, migrated)
            prefs.edit().putBoolean(KEY_INITIALIZED, true).commit()
            return migrated
        }
        return load(prefs.getString(KEY_PORTFOLIO, null))
    }

    @Synchronized
    fun recordBuy(context: Context, price: Double, at: Long) {
        save(context, UserPaperTrader.buy(state(context), price, at))
    }

    @Synchronized
    fun recordSell(context: Context, price: Double, at: Long) {
        save(context, UserPaperTrader.sell(state(context), price, at))
    }

    @Synchronized
    fun markToMarket(context: Context, price: Double): UserPaperPortfolio {
        val current = state(context)
        val updated = UserPaperTrader.markToMarket(current, price)
        if (updated != current) save(context, updated)
        return updated
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Synchronized
    fun discardOpenPosition(context: Context) {
        val current = state(context)
        if (!current.inPosition) return
        val retained = current.trades.dropLastWhile { it.action == "BUY" }
        var rebuilt = UserPaperPortfolio()
        retained.forEach { trade ->
            rebuilt = when (trade.action) {
                "BUY" -> UserPaperTrader.buy(rebuilt, trade.price, trade.time)
                "SELL" -> UserPaperTrader.sell(rebuilt, trade.price, trade.time)
                else -> rebuilt
            }
        }
        save(context, rebuilt)
    }

    private fun migrate(trades: List<ManualTrade>): UserPaperPortfolio {
        var result = UserPaperPortfolio()
        trades.sortedBy { it.boughtAt }.forEach { trade ->
            result = UserPaperTrader.buy(result, trade.buyPrice, trade.boughtAt)
            if (trade.closed) {
                result = UserPaperTrader.sell(result, trade.sellPrice, trade.soldAt)
            }
        }
        return result
    }

    private fun save(context: Context, value: UserPaperPortfolio) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PORTFOLIO, toJson(value).toString())
            .putBoolean(KEY_INITIALIZED, true)
            .commit()
    }

    private fun load(raw: String?): UserPaperPortfolio {
        if (raw.isNullOrBlank()) return UserPaperPortfolio()
        return runCatching {
            val json = JSONObject(raw)
            val tradesJson = json.optJSONArray("trades") ?: JSONArray()
            UserPaperPortfolio(
                cashEur = json.optDouble("cashEur", UserPaperPortfolio.START_BALANCE),
                pumpAmount = json.optDouble("pumpAmount"),
                entryPrice = json.optDouble("entryPrice"),
                entryTime = json.optLong("entryTime"),
                entryFeeEur = json.optDouble("entryFeeEur"),
                totalFeesEur = json.optDouble("totalFeesEur"),
                peakValueEur = json.optDouble("peakValueEur", UserPaperPortfolio.START_BALANCE),
                maxDrawdownPercent = json.optDouble("maxDrawdownPercent"),
                trades = (0 until tradesJson.length()).mapNotNull {
                    tradesJson.optJSONObject(it)?.let(UserPaperTrade::fromJson)
                }
            )
        }.getOrDefault(UserPaperPortfolio())
    }

    private fun toJson(value: UserPaperPortfolio): JSONObject = JSONObject()
        .put("cashEur", value.cashEur)
        .put("pumpAmount", value.pumpAmount)
        .put("entryPrice", value.entryPrice)
        .put("entryTime", value.entryTime)
        .put("entryFeeEur", value.entryFeeEur)
        .put("totalFeesEur", value.totalFeesEur)
        .put("peakValueEur", value.peakValueEur)
        .put("maxDrawdownPercent", value.maxDrawdownPercent)
        .put("trades", JSONArray(value.trades.map { it.toJson() }))
}
