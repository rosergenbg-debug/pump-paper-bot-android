package com.example.pumppaperbot

import kotlin.math.max

data class CapitalParticipationDecision(val allowed: Boolean, val reason: String)

/**
 * V6.1 relative capital-participation evidence for the standalone Fusion strategy.
 *
 * V5.x required an absolute $250k/5m turnover AND a recognised large-BUY series AND a bid-heavy
 * book. That combination was useful as research evidence but too close to an "ideal candidate"
 * lock: it could ignore a genuine move simply because the absolute tape scale was lower. V6.1
 * keeps data/execution safety hard, but treats large-flow fingerprints and mildly ask-heavy books
 * as evidence rather than mandatory permission.
 */
object CapitalParticipationGate {
    private const val MIN_DATA_5M_NOTIONAL = 25_000.0
    private const val MIN_ACCELERATION = 1.05
    private const val MIN_BUY_SELL_RATIO = 1.05
    private const val MIN_LARGE_BUY_CONFIDENCE = 50
    private const val MIN_BOOK_NOTIONAL = 20_000.0
    private const val MIN_BOOK_BID_ASK_RATIO = 0.95
    private const val MAX_BOOK_SPREAD_PERCENT = 0.30
    const val MIN_ACCEPTANCE_PERCENT = 0.08
    const val MAX_ACCEPTANCE_CHASE_PERCENT = 0.85

    fun evaluate(
        observation: SharedFusionEntryObservation,
        minFiveMinuteNotional: Double = MIN_DATA_5M_NOTIONAL,
        minAcceleration: Double = MIN_ACCELERATION,
        minBuySellRatio: Double = MIN_BUY_SELL_RATIO
    ): CapitalParticipationDecision {
        val micro = observation.micro ?: return deny("нет свежей ленты реальных сделок")
        if (micro.flowHistorySeconds < 5L * 60L) return deny("ждём хотя бы 5 минут истории сделок")
        val buy5 = micro.buyNotional5m.coerceAtLeast(0.0)
        val sell5 = micro.sellNotional5m.coerceAtLeast(0.0)
        val total5 = buy5 + sell5
        if (total5 < minFiveMinuteNotional) {
            return deny("лента слишком тонкая для Fusion: 5м ${money(total5)} < ${money(minFiveMinuteNotional)}")
        }

        val total15 = (micro.buyNotional15m + micro.sellNotional15m).coerceAtLeast(total5)
        val priorFive = (total15 - total5).coerceAtLeast(0.0) / 2.0
        val acceleration = if (priorFive > 0.0) total5 / priorFive else micro.tradeAcceleration.coerceAtLeast(0.0)
        if (acceleration > 0.0 && acceleration < minAcceleration) {
            return deny("оборот не ускоряется: ${fmt(acceleration)}x < ${fmt(minAcceleration)}x")
        }
        val sideRatio = buy5 / sell5.coerceAtLeast(1.0)
        if (sideRatio < minBuySellRatio && micro.aggressiveBuyPercent60s < 54.0) {
            return deny("покупатели пока не получили перевес: BUY/SELL ${fmt(sideRatio)}x, 60с ${fmt(micro.aggressiveBuyPercent60s)}%")
        }

        if (observation.capitalFlow.mode in setOf(
                CapitalFlowMode.DISTRIBUTION,
                CapitalFlowMode.NEW_SHORTS,
                CapitalFlowMode.DELEVERAGING
            )) {
            return deny("механизм потока ${observation.capitalFlow.mode}: капитал сейчас не подтверждает рост")
        }
        if (observation.capitalFlow.mode == CapitalFlowMode.ACCUMULATION &&
            micro.priceChange60sPercent < 0.08 && sideRatio < 1.15
        ) {
            return deny("покупки пока поглощаются: ждём реальную реакцию цены")
        }

        val bidBook = observation.bookBidNotional ?: return deny("нет свежей глубины bid")
        val askBook = observation.bookAskNotional ?: return deny("нет свежей глубины ask")
        val bookTotal = bidBook.coerceAtLeast(0.0) + askBook.coerceAtLeast(0.0)
        if (bookTotal < MIN_BOOK_NOTIONAL) return deny("стакан слишком пустой: ${money(bookTotal)}")
        val bookRatio = bidBook / askBook.coerceAtLeast(1.0)
        if (bookRatio < MIN_BOOK_BID_ASK_RATIO && micro.aggressiveBuyPercent15s < 58.0) {
            return deny("стакан заметно против роста: bid/ask ${fmt(bookRatio)}x без сильного taker-buy")
        }
        val spread = observation.bookSpreadPercent
        if (spread != null && spread > MAX_BOOK_SPREAD_PERCENT) {
            return deny("спред ${fmt(spread)}% слишком широк")
        }
        if (observation.executionAsk <= 0.0) return deny("нет свежего исполнимого ask Bitpanda")

        val large = micro.largeFlow
        val largeBuySeries = large.mode == LargeFlowMode.BUY_SERIES &&
            large.confidence >= MIN_LARGE_BUY_CONFIDENCE &&
            large.largeBuyUsdt > large.largeSellUsdt * 1.20
        val largeText = if (largeBuySeries) {
            "крупные BUY подтверждают ${money(large.largeBuyUsdt)}"
        } else {
            "крупная серия не обязательна (${large.mode}, ${large.confidence}/100)"
        }
        return CapitalParticipationDecision(
            true,
            "5м ${money(total5)}, ускорение ${fmt(acceleration)}x, BUY/SELL ${fmt(sideRatio)}x, " +
                "$largeText, стакан bid/ask ${fmt(bookRatio)}x"
        )
    }

    fun priceAcceptance(anchorAsk: Double, currentAsk: Double): CapitalParticipationDecision {
        if (anchorAsk <= 0.0 || currentAsk <= 0.0) return deny("нет цены для проверки принятия")
        val change = (currentAsk / anchorAsk - 1.0) * 100.0
        return when {
            change < MIN_ACCEPTANCE_PERCENT -> deny("цена ещё не приняла капитал: ${signed(change)}%")
            change > MAX_ACCEPTANCE_CHASE_PERCENT -> deny("цена уже ушла на ${signed(change)}% — не догоняем")
            else -> CapitalParticipationDecision(true, "принятие цены ${signed(change)}%")
        }
    }

    private fun deny(reason: String) = CapitalParticipationDecision(false, reason)
    private fun fmt(value: Double) = String.format(java.util.Locale.GERMANY, "%.2f", value)
    private fun signed(value: Double) = String.format(java.util.Locale.GERMANY, "%+.2f", value)
    private fun money(value: Double): String = when {
        value >= 1_000_000.0 -> String.format(java.util.Locale.US, "$%.2fm", value / 1_000_000.0)
        value >= 1_000.0 -> String.format(java.util.Locale.US, "$%.0fk", value / 1_000.0)
        else -> String.format(java.util.Locale.US, "$%.0f", max(0.0, value))
    }
}
