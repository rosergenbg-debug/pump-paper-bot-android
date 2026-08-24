package com.example.pumppaperbot

import kotlin.math.max

data class CapitalParticipationDecision(val allowed: Boolean, val reason: String)

/**
 * Public-tape evidence only. It never identifies a person or company. A large turnover is
 * deliberately insufficient: entry requires a repeated aggressive BUY footprint, supportive
 * top-20 book and later price acceptance.
 */
object CapitalParticipationGate {
    private const val MIN_5M_NOTIONAL = 250_000.0
    private const val MIN_ACCELERATION = 1.15
    private const val MIN_BUY_SELL_RATIO = 1.12
    private const val MIN_LARGE_BUY_CONFIDENCE = 50
    private const val MIN_BOOK_NOTIONAL = 40_000.0
    private const val MIN_BOOK_BID_ASK_RATIO = 1.05
    private const val MAX_BOOK_SPREAD_PERCENT = 0.30
    const val MIN_ACCEPTANCE_PERCENT = 0.10
    const val MAX_ACCEPTANCE_CHASE_PERCENT = 0.75

    fun evaluate(
        observation: SharedFusionEntryObservation,
        minFiveMinuteNotional: Double = MIN_5M_NOTIONAL,
        minAcceleration: Double = MIN_ACCELERATION,
        minBuySellRatio: Double = MIN_BUY_SELL_RATIO
    ): CapitalParticipationDecision {
        val micro = observation.micro ?: return deny("нет свежей ленты реальных сделок")
        if (micro.flowHistorySeconds < 15L * 60L) return deny("ждём 15 минут истории сделок")
        val buy5 = micro.buyNotional5m.coerceAtLeast(0.0)
        val sell5 = micro.sellNotional5m.coerceAtLeast(0.0)
        val total5 = buy5 + sell5
        if (total5 < minFiveMinuteNotional) {
            return deny("оборот 5м ${money(total5)} < ${money(minFiveMinuteNotional)}")
        }
        val total15 = (micro.buyNotional15m + micro.sellNotional15m).coerceAtLeast(total5)
        val priorFive = (total15 - total5).coerceAtLeast(0.0) / 2.0
        if (priorFive <= 0.0) return deny("нет базы предыдущих 10 минут")
        val acceleration = total5 / priorFive
        if (acceleration < minAcceleration) {
            return deny("оборот не ускорился: ${fmt(acceleration)}x < ${fmt(minAcceleration)}x")
        }
        val sideRatio = buy5 / sell5.coerceAtLeast(1.0)
        if (sideRatio < minBuySellRatio) {
            return deny("BUY/SELL лишь ${fmt(sideRatio)}x")
        }

        val large = micro.largeFlow
        val largeBuySeries = large.mode == LargeFlowMode.BUY_SERIES &&
            large.confidence >= MIN_LARGE_BUY_CONFIDENCE &&
            large.largeBuyUsdt > large.largeSellUsdt * 1.20
        if (!largeBuySeries) {
            return deny("нет устойчивой серии крупных BUY (режим ${large.mode}, уверенность ${large.confidence}/100)")
        }

        if (observation.capitalFlow.mode in setOf(
                CapitalFlowMode.ACCUMULATION,
                CapitalFlowMode.DISTRIBUTION,
                CapitalFlowMode.NEW_SHORTS,
                CapitalFlowMode.DELEVERAGING
            )) {
            return deny("механизм потока ${observation.capitalFlow.mode}: деньги пока не двигают цену вверх")
        }

        val bidBook = observation.bookBidNotional ?: return deny("нет свежей глубины bid")
        val askBook = observation.bookAskNotional ?: return deny("нет свежей глубины ask")
        val bookTotal = bidBook.coerceAtLeast(0.0) + askBook.coerceAtLeast(0.0)
        if (bookTotal < MIN_BOOK_NOTIONAL) return deny("стакан слишком пустой: ${money(bookTotal)}")
        val bookRatio = bidBook / askBook.coerceAtLeast(1.0)
        if (bookRatio < MIN_BOOK_BID_ASK_RATIO) {
            return deny("стакан не поддерживает рост: bid/ask ${fmt(bookRatio)}x")
        }
        val spread = observation.bookSpreadPercent
        if (spread != null && spread > MAX_BOOK_SPREAD_PERCENT) {
            return deny("спред ${fmt(spread)}% слишком широк")
        }
        if (observation.executionAsk <= 0.0) return deny("нет свежего исполнимого ask Bitpanda")

        return CapitalParticipationDecision(
            true,
            "5м ${money(total5)}, ускорение ${fmt(acceleration)}x, BUY/SELL ${fmt(sideRatio)}x, " +
                "крупные BUY ${money(large.largeBuyUsdt)}, стакан bid/ask ${fmt(bookRatio)}x"
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
