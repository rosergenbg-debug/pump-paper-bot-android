package com.example.pumppaperbot

import android.content.Context
import org.json.JSONObject
import kotlin.math.max

/**
 * V5.19 fast local shock/rebound lane.
 *
 * Three minutes is context only. Entry is driven by 15-second observations of price recovery,
 * aggressive taker flow, activity and top-of-book pressure. The lane remains paper-only and
 * never sends a real exchange order.
 */
data class ShockReboundObservation(
    val at: Long,
    val price: Double,
    val drawdown3mPercent: Double,
    val rebound3mPercent: Double,
    val change15sPercent: Double,
    val change60sPercent: Double,
    val buyer5sPercent: Double,
    val buyer15sPercent: Double,
    val buyer60sPercent: Double,
    val tradeAcceleration: Double,
    val moneyActivityRatio: Double?,
    val bookImbalance: Double?
)

data class ShockReboundState(
    val active: Boolean = false,
    val ready: Boolean = false,
    val failed: Boolean = false,
    val candidateAt: Long = 0L,
    val confirmations: Int = 0,
    val lastObservedAt: Long = 0L,
    val drawdown3mPercent: Double = 0.0,
    val rebound3mPercent: Double = 0.0,
    val lastPrice: Double = 0.0,
    val reason: String = "Спокойный рынок"
) {
    fun fresh(now: Long): Boolean = lastObservedAt > 0L && now - lastObservedAt in 0..FRESH_MILLIS

    fun toJson(): JSONObject = JSONObject()
        .put("active", active)
        .put("ready", ready)
        .put("failed", failed)
        .put("candidateAt", candidateAt)
        .put("confirmations", confirmations)
        .put("lastObservedAt", lastObservedAt)
        .put("drawdown3mPercent", drawdown3mPercent)
        .put("rebound3mPercent", rebound3mPercent)
        .put("lastPrice", lastPrice)
        .put("reason", reason)

    companion object {
        const val FRESH_MILLIS = 45_000L
        fun fromJson(j: JSONObject) = ShockReboundState(
            active = j.optBoolean("active"),
            ready = j.optBoolean("ready"),
            failed = j.optBoolean("failed"),
            candidateAt = j.optLong("candidateAt"),
            confirmations = j.optInt("confirmations"),
            lastObservedAt = j.optLong("lastObservedAt"),
            drawdown3mPercent = j.optDouble("drawdown3mPercent"),
            rebound3mPercent = j.optDouble("rebound3mPercent"),
            lastPrice = j.optDouble("lastPrice"),
            reason = j.optString("reason", "Спокойный рынок")
        )
    }
}

data class FastMoneyPressure(
    val activityRatio60sTo5m: Double?,
    val buyerShare60s: Double,
    val buyerShare5m: Double,
    val heavyBuying: Boolean,
    val heavySelling: Boolean,
    val absorptionPossible: Boolean
)

object FastMoneyPressurePolicy {
    fun evaluate(micro: MicroImpulseSnapshot): FastMoneyPressure {
        val turnover60 = micro.buyNotional60s + micro.sellNotional60s
        val turnover5m = micro.buyNotional5m + micro.sellNotional5m
        val ratio = if (micro.flowHistorySeconds >= 240L && turnover5m > 0.0) {
            turnover60 / (turnover5m / 5.0)
        } else null
        val buy60 = micro.aggressiveBuyPercent60s
        val buy5m = micro.aggressiveBuyPercent5m
        val accelerated = (ratio ?: 1.0) >= 1.20
        val heavyBuying = micro.connected && accelerated &&
            ((buy60 >= 56.0 && micro.priceChange60sPercent >= 0.05) || buy5m >= 57.0)
        val heavySelling = micro.connected && accelerated &&
            ((buy60 <= 44.0 && micro.priceChange60sPercent <= -0.08) ||
                (buy5m <= 46.0 && micro.priceChange60sPercent <= -0.12))
        val absorptionPossible = micro.connected && accelerated && buy60 <= 44.0 &&
            micro.priceChange60sPercent > -0.08
        return FastMoneyPressure(ratio, buy60, buy5m, heavyBuying, heavySelling, absorptionPossible)
    }
}

object ShockReboundPolicy {
    const val SHOCK_DRAWDOWN_3M_PERCENT = 2.50
    const val SHOCK_CHANGE_60S_PERCENT = -1.50
    const val MIN_REBOUND_PERCENT = 0.70
    const val REQUIRED_CONFIRMATIONS = 2
    const val MIN_CONFIRMATION_MILLIS = 15_000L
    const val ACTIVE_TTL_MILLIS = 4L * 60L * 1000L

    fun update(previous: ShockReboundState, o: ShockReboundObservation): ShockReboundState {
        val shockNow = o.drawdown3mPercent >= SHOCK_DRAWDOWN_3M_PERCENT ||
            o.change60sPercent <= SHOCK_CHANGE_60S_PERCENT
        val previousStillActive = previous.active && previous.lastObservedAt > 0L &&
            o.at - previous.lastObservedAt in 0..ACTIVE_TTL_MILLIS
        val active = shockNow || previousStillActive
        if (!active) return ShockReboundState(lastObservedAt = o.at, lastPrice = o.price)

        val bookOkay = (o.bookImbalance ?: 0.0) > -0.18
        val activityOkay = (o.moneyActivityRatio ?: o.tradeAcceleration) >= 1.15 || o.tradeAcceleration >= 1.45
        val buyersReturning = o.buyer5sPercent >= 58.0 && o.buyer15sPercent >= 55.0 &&
            o.buyer60sPercent >= 48.0
        val priceTurning = o.rebound3mPercent >= MIN_REBOUND_PERCENT &&
            o.change15sPercent >= 0.12 && o.change60sPercent > -0.80
        val candidate = active && buyersReturning && priceTurning && activityOkay && bookOkay

        val failed = previous.ready && (
            (o.change15sPercent <= -0.35 && o.buyer15sPercent <= 43.0 && o.rebound3mPercent <= 0.30) ||
                (o.change60sPercent <= -1.80 && o.buyer15sPercent <= 45.0)
            )

        if (failed) {
            return previous.copy(
                active = true,
                ready = false,
                failed = true,
                confirmations = 0,
                candidateAt = 0L,
                lastObservedAt = o.at,
                drawdown3mPercent = o.drawdown3mPercent,
                rebound3mPercent = o.rebound3mPercent,
                lastPrice = o.price,
                reason = "SHOCK_REBOUND_FAILED: отскок сорвался, быстрые продавцы снова контролируют цену"
            )
        }

        if (!candidate) {
            return previous.copy(
                active = true,
                ready = false,
                failed = false,
                confirmations = 0,
                candidateAt = 0L,
                lastObservedAt = o.at,
                drawdown3mPercent = o.drawdown3mPercent,
                rebound3mPercent = o.rebound3mPercent,
                lastPrice = o.price,
                reason = "SHOCK_ARMED: сильный провал есть, но дно ещё не подтверждено"
            )
        }

        val candidateAt = if (previous.confirmations > 0 && previous.candidateAt > 0L) previous.candidateAt else o.at
        val confirmations = (previous.confirmations + 1).coerceAtMost(REQUIRED_CONFIRMATIONS)
        val confirmedByTime = o.at - candidateAt >= MIN_CONFIRMATION_MILLIS
        val ready = confirmations >= REQUIRED_CONFIRMATIONS && confirmedByTime
        return previous.copy(
            active = true,
            ready = ready,
            failed = false,
            candidateAt = candidateAt,
            confirmations = confirmations,
            lastObservedAt = o.at,
            drawdown3mPercent = o.drawdown3mPercent,
            rebound3mPercent = o.rebound3mPercent,
            lastPrice = o.price,
            reason = if (ready) {
                "SHOCK_REBOUND_READY: два 15-секундных подтверждения, покупатели вернулись и цена оттолкнулась от 3-минутного минимума"
            } else {
                "SHOCK_REBOUND_CONFIRMING: первое подтверждение отскока, ждём ещё один 15-секундный кадр"
            }
        )
    }
}

object ShockReboundStore {
    private const val PREFS = "shock_rebound_v519"
    private const val KEY = "state"

    @Synchronized
    fun observe(context: Context, observation: ShockReboundObservation): ShockReboundState {
        val next = ShockReboundPolicy.update(state(context), observation)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, next.toJson().toString()).apply()
        return next
    }

    fun state(context: Context): ShockReboundState {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        return if (raw.isNullOrBlank()) ShockReboundState() else runCatching {
            ShockReboundState.fromJson(JSONObject(raw))
        }.getOrDefault(ShockReboundState())
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
