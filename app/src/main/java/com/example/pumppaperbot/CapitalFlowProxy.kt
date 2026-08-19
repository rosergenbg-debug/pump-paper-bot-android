package com.example.pumppaperbot

import org.json.JSONObject
import kotlin.math.abs

/**
 * Behavioural estimate of capital flow. Public CEX feeds do not expose a trader,
 * company or country, so this policy deliberately classifies the *mechanism* of
 * a move rather than inventing an identity for it.
 */
enum class CapitalFlowMode {
    NEW_LONGS,
    SHORT_COVERING,
    ACCUMULATION,
    DISTRIBUTION,
    NEW_SHORTS,
    DELEVERAGING,
    MIXED,
    UNAVAILABLE
}

data class CapitalFlowProxy(
    val mode: CapitalFlowMode = CapitalFlowMode.UNAVAILABLE,
    val score: Int = 0,
    val confidence: Int = 0,
    val title: String = "НЕДОСТАТОЧНО ДАННЫХ",
    val explanation: String = "Ждём синхронные цену, сделки и открытый интерес.",
    val identityNote: String = "Биржевой поток анонимен: фирма, страна и владелец заявки не видны."
) {
    fun toJson() = JSONObject()
        .put("mode", mode.name)
        .put("score", score)
        .put("confidence", confidence)
        .put("title", title)
        .put("explanation", explanation)
        .put("identity_note", identityNote)

    companion object {
        fun fromJson(value: JSONObject?) = if (value == null) CapitalFlowProxy() else CapitalFlowProxy(
            mode = runCatching { CapitalFlowMode.valueOf(value.optString("mode")) }
                .getOrDefault(CapitalFlowMode.UNAVAILABLE),
            score = value.optInt("score").coerceIn(-100, 100),
            confidence = value.optInt("confidence").coerceIn(0, 100),
            title = value.optString("title", "НЕДОСТАТОЧНО ДАННЫХ"),
            explanation = value.optString("explanation", "Ждём синхронные цену, сделки и открытый интерес."),
            identityNote = value.optString(
                "identity_note",
                "Биржевой поток анонимен: фирма, страна и владелец заявки не видны."
            )
        )
    }
}

object CapitalFlowProxyPolicy {
    fun evaluate(
        impulse: ImpulseSnapshot,
        breathing: LiveMarketBreathingSnapshot,
        now: Long = System.currentTimeMillis()
    ): CapitalFlowProxy {
        val impulseFresh = impulse.candleTime > 0L && now >= impulse.candleTime &&
            now - impulse.candleTime <= DeepSeekFreshMarketContext.FIVE_MINUTE_MAX_AGE
        val latest = breathing.flowWave.latest.takeIf {
            breathing.fresh || breathing.flowWave.staleSeconds <=
                LiveMarketBreathingAnalyzer.MAX_LIVE_AGE_MILLIS / 1_000L
        }
        val spot = impulse.spotTakerRatio.takeIf { impulseFresh }
        val futures = impulse.futuresTakerRatio.takeIf { impulseFresh }
        val price15 = impulse.return15m.takeIf { impulseFresh }
        val price60 = impulse.return60m.takeIf { impulseFresh }
        val oi = impulse.openInterestChange10m.takeIf { impulseFresh }
        val available = listOf(spot, futures, price15, price60, oi).count { it != null } +
            if (latest != null) 1 else 0
        if (available < 3) return CapitalFlowProxy()

        val tradePressure = listOfNotNull(spot, futures)
            .map { ((it - 0.5) * 200.0).coerceIn(-100.0, 100.0) }
            .averageOrNull() ?: latest?.score15m?.toDouble() ?: 0.0
        val wave = latest?.let { (it.score15m * 0.6 + it.score30m * 0.4) } ?: 0.0
        val price = ((price15 ?: price60 ?: 0.0) * 2_000.0).coerceIn(-100.0, 100.0)
        val openInterest = ((oi ?: 0.0) * 5_000.0).coerceIn(-100.0, 100.0)
        val score = (tradePressure * 0.38 + wave * 0.27 + price * 0.20 + openInterest * 0.15)
            .toInt().coerceIn(-100, 100)
        val confidence = (35 + available * 8 +
            (if (spot != null && futures != null) 8 else 0) +
            (if (oi != null) 9 else 0)).coerceIn(0, 100)

        val buys = tradePressure >= 8 || wave >= 12
        val sells = tradePressure <= -8 || wave <= -12
        val up = (price15 ?: price60 ?: 0.0) >= 0.003
        val down = (price15 ?: price60 ?: 0.0) <= -0.003
        val oiUp = (oi ?: 0.0) >= 0.003
        val oiDown = (oi ?: 0.0) <= -0.003
        val absorbed = breathing.buyerBreath.absorptionRisk >= 62 ||
            (buys && abs(price) < 8 && (breathing.buyerBreath.efficiencyScore ?: 0) < 8)

        return when {
            up && buys && oiUp -> result(
                CapitalFlowMode.NEW_LONGS, score, confidence,
                "НОВЫЕ ЛОНГИ ПОДДЕРЖИВАЮТ РОСТ",
                "Цена, агрессивные покупки и OI растут вместе: движение похоже на приток нового плечевого капитала."
            )
            up && oiDown -> result(
                CapitalFlowMode.SHORT_COVERING, score, confidence,
                "РОСТ ЧАСТИЧНО ПОХОЖ НА ЗАКРЫТИЕ ШОРТОВ",
                "Цена растёт, но OI снижается: часть импульса могла возникнуть из выкупа закрываемых шортов, а не из устойчивого нового спроса."
            )
            buys && absorbed -> result(
                CapitalFlowMode.ACCUMULATION, score, confidence,
                "ПОКУПКИ ПОГЛОЩАЮТСЯ",
                "Покупки заметны, но цена отвечает слабо. Это может быть накопление либо крупный продавец; нужен пробой и удержание цены."
            )
            down && sells && oiUp -> result(
                CapitalFlowMode.NEW_SHORTS, score, confidence,
                "НОВЫЕ ШОРТЫ УСИЛИВАЮТ СНИЖЕНИЕ",
                "Цена и поток направлены вниз при росте OI: похоже на набор новых коротких позиций."
            )
            down && oiDown -> result(
                CapitalFlowMode.DELEVERAGING, score, confidence,
                "СБРОС ПЛЕЧА / ЗАКРЫТИЕ ПОЗИЦИЙ",
                "Цена и OI падают вместе: рынок сокращает позиции; это не обязательно новый направленный продавец."
            )
            sells && breathing.buyerBreath.absorptionRisk >= 55 -> result(
                CapitalFlowMode.DISTRIBUTION, score, confidence,
                "РАСПРЕДЕЛЕНИЕ В ПОЛЬЗУ ПРОДАВЦОВ",
                "Продажи усиливаются, а прежний покупательский поток теряет эффективность."
            )
            else -> result(
                CapitalFlowMode.MIXED, score, confidence,
                "ПОТОК СМЕШАННЫЙ — КРУПНЫЙ ИГРОК НЕ ПОДТВЕРЖДЁН",
                "Цена, сделки и OI пока не дают одного устойчивого механизма движения."
            )
        }
    }

    private fun result(
        mode: CapitalFlowMode,
        score: Int,
        confidence: Int,
        title: String,
        explanation: String
    ) = CapitalFlowProxy(mode, score, confidence, title, explanation)

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()
}
