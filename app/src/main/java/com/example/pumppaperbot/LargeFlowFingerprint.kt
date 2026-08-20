package com.example.pumppaperbot

import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max

enum class LargeFlowMode { BUY_SERIES, SELL_SERIES, BUY_ABSORBED, SELL_ABSORBED, MIXED, WARMING_UP }

/**
 * A public-tape footprint, never a trader identity. Binance aggTrade groups fills
 * belonging to one taker order, so a MicroTrade is useful as one visible order;
 * repeated similar orders are only a probable sliced metaorder.
 */
data class LargeFlowFingerprint(
    val mode: LargeFlowMode = LargeFlowMode.WARMING_UP,
    val confidence: Int = 0,
    val thresholdUsdt: Double = 0.0,
    val largestBuyUsdt: Double = 0.0,
    val largestSellUsdt: Double = 0.0,
    val largeBuyUsdt: Double = 0.0,
    val largeSellUsdt: Double = 0.0,
    val buySlices: Int = 0,
    val sellSlices: Int = 0,
    val buySeriesSeconds: Int = 0,
    val sellSeriesSeconds: Int = 0,
    val title: String = "НАКАПЛИВАЕМ ЛЕНТУ КРУПНЫХ ЗАЯВОК",
    val explanation: String = "Нужно несколько минут живых сделок для динамического порога.",
    val fingerprint: String = "—"
) {
    fun toJson() = JSONObject()
        .put("mode", mode.name)
        .put("confidence", confidence)
        .put("dynamic_large_order_usdt", thresholdUsdt)
        .put("largest_buy_order_usdt", largestBuyUsdt)
        .put("largest_sell_order_usdt", largestSellUsdt)
        .put("large_buy_notional_5m", largeBuyUsdt)
        .put("large_sell_notional_5m", largeSellUsdt)
        .put("probable_buy_slices", buySlices)
        .put("probable_sell_slices", sellSlices)
        .put("buy_series_seconds", buySeriesSeconds)
        .put("sell_series_seconds", sellSeriesSeconds)
        .put("title", title)
        .put("explanation", explanation)
        .put("anonymous_pattern", fingerprint)
        .put("identity_limit", "Это сходный алгоритмический почерк, не доказательство одного владельца.")

    companion object {
        fun fromJson(value: JSONObject?): LargeFlowFingerprint {
            if (value == null) return LargeFlowFingerprint()
            return LargeFlowFingerprint(
                mode = runCatching { LargeFlowMode.valueOf(value.optString("mode")) }
                    .getOrDefault(LargeFlowMode.WARMING_UP),
                confidence = value.optInt("confidence").coerceIn(0, 100),
                thresholdUsdt = value.optDouble("dynamic_large_order_usdt"),
                largestBuyUsdt = value.optDouble("largest_buy_order_usdt"),
                largestSellUsdt = value.optDouble("largest_sell_order_usdt"),
                largeBuyUsdt = value.optDouble("large_buy_notional_5m"),
                largeSellUsdt = value.optDouble("large_sell_notional_5m"),
                buySlices = value.optInt("probable_buy_slices"),
                sellSlices = value.optInt("probable_sell_slices"),
                buySeriesSeconds = value.optInt("buy_series_seconds"),
                sellSeriesSeconds = value.optInt("sell_series_seconds"),
                title = value.optString("title", "НАКАПЛИВАЕМ ЛЕНТУ КРУПНЫХ ЗАЯВОК"),
                explanation = value.optString("explanation", "Нужно несколько минут живых сделок для динамического порога."),
                fingerprint = value.optString("anonymous_pattern", "—")
            )
        }
    }
}

object LargeFlowFingerprintPolicy {
    fun evaluate(trades: List<MicroTrade>, now: Long, currentPrice: Double): LargeFlowFingerprint {
        val recent = trades.filter { it.at in (now - 5L * 60L * 1_000L)..now }
            .sortedBy { it.at }
        if (recent.size < 30) return LargeFlowFingerprint()
        val notionals = recent.map { it.notional }.sorted()
        val median = percentile(notionals, 0.50)
        val p97 = percentile(notionals, 0.97)
        val total = notionals.sum()
        // Dynamic to PUMP liquidity. The floor prevents ordinary retail prints
        // from being labelled as institutional on a quiet tape.
        val threshold = max(10_000.0, max(median * 8.0, p97)).coerceAtMost(max(10_000.0, total * 0.20))
        val large = recent.filter { it.notional >= threshold }
        val buy = large.filter { it.aggressiveBuy }
        val sell = large.filterNot { it.aggressiveBuy }
        val buyCluster = bestCluster(recent, true, threshold)
        val sellCluster = bestCluster(recent, false, threshold)
        val startPrice = recent.first().price
        val responsePercent = if (startPrice > 0.0 && currentPrice > 0.0) {
            (currentPrice / startPrice - 1.0) * 100.0
        } else 0.0
        val buyTotal = buy.sumOf { it.notional }
        val sellTotal = sell.sumOf { it.notional }
        val buyDominant = buyTotal > sellTotal * 1.35 || buyCluster.notional > sellCluster.notional * 1.35
        val sellDominant = sellTotal > buyTotal * 1.35 || sellCluster.notional > buyCluster.notional * 1.35
        val buyAbsorbed = buyDominant && responsePercent < 0.04
        val sellAbsorbed = sellDominant && responsePercent > -0.04
        val mode = when {
            buyAbsorbed -> LargeFlowMode.BUY_ABSORBED
            sellAbsorbed -> LargeFlowMode.SELL_ABSORBED
            buyDominant -> LargeFlowMode.BUY_SERIES
            sellDominant -> LargeFlowMode.SELL_SERIES
            else -> LargeFlowMode.MIXED
        }
        val activeCluster = if (buyDominant) buyCluster else sellCluster
        val confidence = (35 + large.size.coerceAtMost(8) * 4 +
            activeCluster.count.coerceAtMost(10) * 3 +
            if (abs(responsePercent) >= 0.10) 10 else 0).coerceIn(0, 92)
        val title = when (mode) {
            LargeFlowMode.BUY_SERIES -> "ВИДНА СЕРИЯ КРУПНЫХ ПОКУПОК"
            LargeFlowMode.SELL_SERIES -> "ВИДНА СЕРИЯ КРУПНЫХ ПРОДАЖ"
            LargeFlowMode.BUY_ABSORBED -> "КРУПНЫЕ ПОКУПКИ ПОКА ПОГЛОЩАЮТСЯ"
            LargeFlowMode.SELL_ABSORBED -> "КРУПНЫЕ ПРОДАЖИ ПОКА ВЫКУПАЮТСЯ"
            LargeFlowMode.MIXED -> "КРУПНЫЙ ПОТОК СМЕШАННЫЙ"
            LargeFlowMode.WARMING_UP -> "НАКАПЛИВАЕМ ЛЕНТУ КРУПНЫХ ЗАЯВОК"
        }
        val explanation = when (mode) {
            LargeFlowMode.BUY_SERIES -> "Большие taker-покупки и похожие части преобладают; цена отвечает вверх. Это зона проверки притока, не автоматический вход."
            LargeFlowMode.SELL_SERIES -> "Большие taker-продажи и похожие части преобладают; цена отвечает вниз. Новому входу нужна повторная проверка."
            LargeFlowMode.BUY_ABSORBED -> "Покупатель тратит заметно, но цена почти не растёт: сверху может стоять крупный продавец. Высокий объём сам по себе не BUY."
            LargeFlowMode.SELL_ABSORBED -> "Продажи заметны, но цена держится: их может поглощать скрытый спрос. Нужны продолжение и стакан."
            LargeFlowMode.MIXED -> "Есть крупные отпечатки с обеих сторон, устойчивого знающего потока пока нет."
            LargeFlowMode.WARMING_UP -> "Нужно несколько минут живых сделок для динамического порога."
        }
        val rounded = activeCluster.typicalUsdt
        val pattern = if (activeCluster.count >= 4 && rounded > 0.0) {
            "${if (buyDominant) "BUY" else "SELL"}-серия: ~${money(rounded)} × ${activeCluster.count} частей"
        } else if (large.isNotEmpty()) {
            "единичные заявки выше ${money(threshold)}"
        } else "крупных заявок выше динамического порога нет"
        return LargeFlowFingerprint(
            mode = mode,
            confidence = confidence,
            thresholdUsdt = threshold,
            largestBuyUsdt = buy.maxOfOrNull { it.notional } ?: 0.0,
            largestSellUsdt = sell.maxOfOrNull { it.notional } ?: 0.0,
            largeBuyUsdt = buyTotal,
            largeSellUsdt = sellTotal,
            buySlices = buyCluster.count,
            sellSlices = sellCluster.count,
            buySeriesSeconds = buyCluster.seconds,
            sellSeriesSeconds = sellCluster.seconds,
            title = title,
            explanation = explanation,
            fingerprint = pattern
        )
    }

    private fun bestCluster(all: List<MicroTrade>, buy: Boolean, threshold: Double): Cluster {
        val side = all.filter { it.aggressiveBuy == buy }
        var best = Cluster()
        for (start in side.indices) {
            val seed = side[start]
            val selected = side.drop(start).takeWhile { it.at - seed.at <= 180_000L }
                .filter { trade ->
                    val ratio = trade.notional / seed.notional.coerceAtLeast(1.0)
                    ratio in 0.72..1.38
                }
            val notional = selected.sumOf { it.notional }
            if (selected.size >= 4 && notional >= threshold && notional > best.notional) {
                best = Cluster(
                    count = selected.size,
                    notional = notional,
                    seconds = ((selected.last().at - selected.first().at) / 1_000L).toInt(),
                    typicalUsdt = selected.map { it.notional }.sorted().let { percentile(it, 0.50) }
                )
            }
        }
        return best
    }

    private fun percentile(values: List<Double>, p: Double): Double {
        if (values.isEmpty()) return 0.0
        val index = ((values.size - 1) * p).toInt().coerceIn(0, values.lastIndex)
        return values[index]
    }

    private fun money(value: Double): String = when {
        value >= 1_000_000 -> String.format(java.util.Locale.US, "$%.2fm", value / 1_000_000.0)
        value >= 1_000 -> String.format(java.util.Locale.US, "$%.0fk", value / 1_000.0)
        else -> String.format(java.util.Locale.US, "$%.0f", value)
    }

    private data class Cluster(
        val count: Int = 0,
        val notional: Double = 0.0,
        val seconds: Int = 0,
        val typicalUsdt: Double = 0.0
    )
}
