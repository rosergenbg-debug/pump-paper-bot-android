package com.example.pumppaperbot

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

enum class CriticalOverviewBand { RED, YELLOW, GREEN }

data class CriticalOverviewMetric(
    val key: String,
    val title: String,
    val score: Int?,
    val detail: String
)

data class CriticalOverviewModel(
    val positionOpen: Boolean,
    val overallScore: Int,
    val band: CriticalOverviewBand,
    val headline: String,
    val metrics: List<CriticalOverviewMetric>
)

data class CriticalOverviewEvidence(
    val positionOpen: Boolean,
    val actionLevel: Int,
    val directionScore: Int,
    val hardEntryVeto: Boolean,
    val rapidDrop: Boolean,
    val bookImbalance: Double?,
    val pumpBuyerPercent60s: Double?,
    val pumpPriceChange60sPercent: Double?,
    val spotTakerRatio: Double?,
    val futuresTakerRatio: Double?,
    val bitcoinBuyerPercent60s: Double?,
    val bitcoinPriceChange60sPercent: Double?,
    val openInterestChangePercent: Double?
)

object CriticalOverviewPolicy {
    fun evaluate(e: CriticalOverviewEvidence): CriticalOverviewModel {
        val actionScore = if (e.positionOpen) {
            ((5.5 - e.actionLevel.coerceIn(1, 10)) * 22.2).roundToInt()
        } else {
            ((e.actionLevel.coerceIn(1, 10) - 5.5) * 22.2).roundToInt()
        }.coerceIn(-100, 100)
        val bookScore = e.bookImbalance?.takeIf(Double::isFinite)
            ?.times(100.0)?.roundToInt()?.coerceIn(-100, 100)
        val pumpScore = combinedFlowScore(
            e.pumpBuyerPercent60s,
            e.pumpPriceChange60sPercent,
            buyerWeight = 0.72
        )
        val spotScore = takerScore(e.spotTakerRatio)
        val futuresScore = takerScore(e.futuresTakerRatio)
        val bitcoinScore = combinedFlowScore(
            e.bitcoinBuyerPercent60s,
            e.bitcoinPriceChange60sPercent,
            buyerWeight = 0.65
        )
        val openInterestScore = openInterestScore(
            e.openInterestChangePercent,
            e.pumpPriceChange60sPercent
        )
        val directionScore = e.directionScore.coerceIn(-100, 100)

        val weighted = listOf(
            actionScore to 0.25,
            directionScore to 0.14,
            bookScore to 0.13,
            pumpScore to 0.20,
            spotScore to 0.09,
            futuresScore to 0.09,
            bitcoinScore to 0.10,
            openInterestScore to 0.05
        )
        val available = weighted.filter { it.first != null }
        var overall = if (available.isEmpty()) 0 else (
            available.sumOf { (score, weight) -> score!!.toDouble() * weight } /
                available.sumOf { it.second }
            ).roundToInt().coerceIn(-100, 100)
        if (!e.positionOpen && e.hardEntryVeto) overall = minOf(overall, -85)
        if (e.rapidDrop) overall = minOf(overall, if (e.positionOpen) -75 else -92)
        if (e.positionOpen && e.actionLevel >= 8) overall = minOf(overall, -55)
        if (!e.positionOpen && e.actionLevel <= 2) overall = minOf(overall, -55)

        val band = when {
            overall >= 25 -> CriticalOverviewBand.GREEN
            overall <= -25 -> CriticalOverviewBand.RED
            else -> CriticalOverviewBand.YELLOW
        }
        val headline = if (e.positionOpen) {
            when (band) {
                CriticalOverviewBand.GREEN -> "УСЛОВИЯ ПОДДЕРЖИВАЮТ УДЕРЖАНИЕ"
                CriticalOverviewBand.YELLOW -> "СИТУАЦИЯ СМЕШАННАЯ — БЫТЬ НАЧЕКУ"
                CriticalOverviewBand.RED -> "СИТУАЦИЯ УХУДШАЕТСЯ — ПРОВЕРИТЬ ВЫХОД"
            }
        } else {
            when (band) {
                CriticalOverviewBand.GREEN -> "ФОН УЛУЧШАЕТСЯ — ПРОВЕРИТЬ ВХОД"
                CriticalOverviewBand.YELLOW -> "СМЕШАННЫЙ ФОН — ПОДГОТОВИТЬСЯ"
                CriticalOverviewBand.RED -> "ВХОД НЕ ПОДТВЕРЖДЁН"
            }
        }
        return CriticalOverviewModel(
            positionOpen = e.positionOpen,
            overallScore = overall,
            band = band,
            headline = headline,
            metrics = listOf(
                CriticalOverviewMetric("action", "DEEPSEEK / ЗАЩИТА", actionScore, "уровень ${e.actionLevel.coerceIn(1, 10)}/10"),
                CriticalOverviewMetric("direction", "ОБЩЕЕ НАПРАВЛЕНИЕ", directionScore, "локальный фон ${signed(directionScore)}/100"),
                CriticalOverviewMetric("book", "СТАКАН • 20 УРОВНЕЙ", bookScore, percentDetail(e.bookImbalance?.times(100.0))),
                CriticalOverviewMetric("pump", "СДЕЛКИ PUMP • 60 СЕК.", pumpScore, flowDetail(e.pumpBuyerPercent60s, e.pumpPriceChange60sPercent)),
                CriticalOverviewMetric("spot", "SPOT-ПОТОК • 5 МИН.", spotScore, ratioDetail(e.spotTakerRatio)),
                CriticalOverviewMetric("futures", "FUTURES-ПОТОК • 5 МИН.", futuresScore, ratioDetail(e.futuresTakerRatio)),
                CriticalOverviewMetric("bitcoin", "BITCOIN • 60 СЕК.", bitcoinScore, flowDetail(e.bitcoinBuyerPercent60s, e.bitcoinPriceChange60sPercent)),
                CriticalOverviewMetric("oi", "ОТКРЫТЫЙ ИНТЕРЕС", openInterestScore, percentDetail(e.openInterestChangePercent))
            )
        )
    }

    private fun combinedFlowScore(buyers: Double?, priceChange: Double?, buyerWeight: Double): Int? {
        val buyerScore = buyers?.takeIf(Double::isFinite)?.let { ((it - 50.0) * 2.0).coerceIn(-100.0, 100.0) }
        val moveScore = priceChange?.takeIf(Double::isFinite)?.let { (it * 140.0).coerceIn(-100.0, 100.0) }
        return when {
            buyerScore != null && moveScore != null ->
                (buyerScore * buyerWeight + moveScore * (1.0 - buyerWeight)).roundToInt()
            buyerScore != null -> buyerScore.roundToInt()
            moveScore != null -> moveScore.roundToInt()
            else -> null
        }?.coerceIn(-100, 100)
    }

    private fun takerScore(ratio: Double?): Int? = ratio?.takeIf(Double::isFinite)
        ?.let { ((it - 0.5) * 200.0).roundToInt().coerceIn(-100, 100) }

    private fun openInterestScore(change: Double?, pumpChange: Double?): Int? {
        val oi = change?.takeIf(Double::isFinite) ?: return null
        val move = pumpChange?.takeIf(Double::isFinite) ?: return 0
        if (abs(move) < 0.01) return 0
        return (abs(oi) * 20.0 * move.sign).roundToInt().coerceIn(-100, 100)
    }

    private fun signed(value: Int) = if (value >= 0) "+$value" else value.toString()
    private fun percentDetail(value: Double?): String = value?.takeIf(Double::isFinite)?.let {
        String.format(java.util.Locale.GERMANY, "%+.2f%%", it)
    } ?: "данных пока нет"
    private fun ratioDetail(value: Double?): String = value?.takeIf(Double::isFinite)?.let {
        String.format(java.util.Locale.GERMANY, "покупатели %.1f%%", it * 100.0)
    } ?: "данных пока нет"
    private fun flowDetail(buyers: Double?, change: Double?): String {
        if (buyers?.isFinite() != true && change?.isFinite() != true) return "данных пока нет"
        return String.format(
            java.util.Locale.GERMANY,
            "покупатели %.1f%% • цена %+.3f%%",
            buyers ?: 50.0,
            change ?: 0.0
        )
    }
}
