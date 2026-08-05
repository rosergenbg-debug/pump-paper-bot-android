package com.example.pumppaperbot

import android.content.Context
import org.json.JSONObject

/**
 * Adds freshness-labelled public Binance data to DeepSeek prompts without
 * changing StrategyV2, which intentionally continues to trade on closed bars.
 */
object DeepSeekFreshMarketContext {
    const val LIVE_PRICE_MAX_AGE = PaperExecutionPolicy.MAX_LIVE_PRICE_AGE_MILLIS
    const val MICRO_MAX_AGE = 45L * 1000L
    const val FIVE_MINUTE_MAX_AGE = 10L * 60L * 1000L

    fun analysisPrice(snapshot: LiveSnapshot, now: Long = System.currentTimeMillis()): Double {
        return snapshot.livePrice?.takeIf {
            it > 0.0 && isFresh(snapshot.livePriceAt, now, LIVE_PRICE_MAX_AGE)
        } ?: snapshot.lastPrice
    }

    fun append(
        context: Context,
        frame: JSONObject,
        snapshot: LiveSnapshot,
        now: Long = System.currentTimeMillis()
    ): JSONObject {
        val livePriceFresh = snapshot.livePrice != null && snapshot.livePrice > 0.0 &&
            isFresh(snapshot.livePriceAt, now, LIVE_PRICE_MAX_AGE)
        val impulse = ImpulseRadarStore.state(context)
        val impulseFresh = impulse.candleTime > 0L &&
            isFresh(impulse.candleTime, now, FIVE_MINUTE_MAX_AGE)
        val micro = MicroImpulseStore.state(context)
        val microFresh = micro.connected && micro.updatedAt > 0L &&
            isFresh(micro.updatedAt, now, MICRO_MAX_AGE)
        val breathing = LiveMarketBreathingStore.snapshot(context, now)

        frame
            .put("current_price_eur", analysisPrice(snapshot, now))
            .put("current_price_source", if (livePriceFresh) "live_spot_tickers" else "last_closed_30m_candle")
            .put("live_price_age_seconds", ageSeconds(snapshot.livePriceAt, now))
            .put("market_snapshot_age_seconds", ageSeconds(snapshot.lastSync, now))
            .put("closed_30m_candle_age_seconds", ageSeconds(snapshot.lastCandle, now))
            .put("spot_order_book_depth_levels", 20)
            .put("spot_order_book_age_seconds", ageSeconds(snapshot.lastSync, now))
            .put("spot_order_book_bid_notional_usdt", snapshot.bookBidNotional ?: JSONObject.NULL)
            .put("spot_order_book_ask_notional_usdt", snapshot.bookAskNotional ?: JSONObject.NULL)
            .put("funding_source", "latest_settled_funding_rate")
            .put("hourly_flow_source", "closed_30m_klines_aligned_to_last_full_hour")
            .put("live_market_breathing", breathing.toJson())
            .put("five_minute_flow", JSONObject()
                .put("fresh", impulseFresh)
                .put("candle_close_at", impulse.candleTime)
                .put("age_seconds", ageSeconds(impulse.candleTime, now))
                .put("readiness_shadow_only", if (impulseFresh) impulse.readiness else JSONObject.NULL)
                .put("volume_vs_36bar_median", freshNumber(impulse.volumeRatio, impulseFresh))
                .put("spot_taker_buy_pct", freshNumber(impulse.spotTakerRatio?.times(100.0), impulseFresh))
                .put("futures_taker_buy_pct", freshNumber(impulse.futuresTakerRatio?.times(100.0), impulseFresh))
                .put("pump_return_15m_pct", freshNumber(impulse.return15m?.times(100.0), impulseFresh))
                .put("pump_return_60m_pct", freshNumber(impulse.return60m?.times(100.0), impulseFresh))
                .put("relative_strength_15m_pct", freshNumber(impulse.relativeStrength15m?.times(100.0), impulseFresh))
                .put("open_interest_change_10m_pct", freshNumber(impulse.openInterestChange10m?.times(100.0), impulseFresh))
                .put("breakout_60m", if (impulseFresh) impulse.breakout60m else JSONObject.NULL)
                .put("status", impulse.status.take(240))
                .put("error", impulse.error.take(240)))
            .put("real_time_spot_flow", JSONObject()
                .put("fresh", microFresh)
                .put("connected", micro.connected)
                .put("updated_at", micro.updatedAt)
                .put("age_seconds", ageSeconds(micro.updatedAt, now))
                .put("phase", if (microFresh) micro.phase else "STALE_OR_OFFLINE")
                .put("price_usdt", freshNumber(micro.priceUsdt, microFresh))
                .put("trades_5s", if (microFresh) micro.trades5s else JSONObject.NULL)
                .put("trades_60s", if (microFresh) micro.trades60s else JSONObject.NULL)
                .put("trade_acceleration", freshNumber(micro.tradeAcceleration, microFresh))
                .put("aggressive_buy_5s_pct", freshNumber(micro.aggressiveBuyPercent5s, microFresh))
                .put("aggressive_buy_15s_pct", freshNumber(micro.aggressiveBuyPercent15s, microFresh))
                .put("aggressive_buy_60s_pct", freshNumber(micro.aggressiveBuyPercent60s, microFresh))
                .put("aggressive_buy_5m_pct", freshNumber(micro.aggressiveBuyPercent5m, microFresh))
                .put("aggressive_buy_notional_60s_usdt", freshNumber(micro.buyNotional60s, microFresh))
                .put("aggressive_sell_notional_60s_usdt", freshNumber(micro.sellNotional60s, microFresh))
                .put("price_change_60s_pct", freshNumber(micro.priceChange60sPercent, microFresh))
                .put("best_bid_ask_spread_pct", freshNumber(micro.spreadPercent, microFresh))
                .put("top_of_book_imbalance", freshNumber(micro.topBookImbalance, microFresh))
                .put("error", micro.error.take(240)))
            .put("real_time_bitcoin_flow", JSONObject()
                .put("fresh", microFresh && micro.bitcoinPriceUsdt > 0.0)
                .put("price_usdt", freshNumber(micro.bitcoinPriceUsdt, microFresh))
                .put("aggressive_buy_15s_pct", freshNumber(micro.bitcoinAggressiveBuyPercent15s, microFresh))
                .put("aggressive_buy_60s_pct", freshNumber(micro.bitcoinAggressiveBuyPercent60s, microFresh))
                .put("price_change_60s_pct", freshNumber(micro.bitcoinPriceChange60sPercent, microFresh)))
        return frame
    }

    internal fun isFresh(at: Long, now: Long, maxAge: Long): Boolean =
        at > 0L && now >= at && now - at <= maxAge

    internal fun ageSeconds(at: Long, now: Long): Any =
        if (at > 0L && now >= at) (now - at) / 1000L else JSONObject.NULL

    private fun freshNumber(value: Double?, fresh: Boolean): Any =
        if (fresh && value != null && value.isFinite()) value else JSONObject.NULL
}
