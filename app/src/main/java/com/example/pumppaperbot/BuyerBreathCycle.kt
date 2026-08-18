package com.example.pumppaperbot

import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class BuyerBreathPhase {
    STALE,
    QUIET,
    IGNITION,
    EXPANSION,
    MATURE,
    EXHAUSTION,
    SELLER_TAKEOVER,
    SHOCK
}

data class BuyerBreathSnapshot(
    val phase: BuyerBreathPhase = BuyerBreathPhase.STALE,
    val title: String = "НЕТ СВЕЖЕГО ПОТОКА",
    val fresh: Boolean = false,
    val pressureScore: Int? = null,
    val efficiencyScore: Int? = null,
    val absorptionRisk: Int = 0,
    val confidence: Int = 0,
    val ageMinutes: Int = 0,
    val buyerPercent5m: Double? = null,
    val buyerPercent15m: Double? = null,
    val priceChange5mPercent: Double? = null,
    val priceChange15mPercent: Double? = null,
    val activityRatio: Double? = null,
    val moveSincePhaseStartPercent: Double? = null,
    val explanation: String = "Ждём живые сделки.",
    val actionHint: String = "Решение по устаревшему потоку не принимается.",
    val watchFor: String = "Свежий поток PUMP и реакцию цены.",
    val historicalReference: String = RESEARCH_REFERENCE
) {
    fun toJson(): JSONObject = JSONObject()
        .put("phase", phase.name)
        .put("title", title)
        .put("fresh", fresh)
        .put("pressure_score", pressureScore ?: JSONObject.NULL)
        .put("price_response_efficiency", efficiencyScore ?: JSONObject.NULL)
        .put("absorption_risk", absorptionRisk)
        .put("confidence", confidence)
        .put("phase_age_minutes", ageMinutes)
        .put("buyer_pct_5m", buyerPercent5m ?: JSONObject.NULL)
        .put("buyer_pct_15m", buyerPercent15m ?: JSONObject.NULL)
        .put("price_change_5m_pct", priceChange5mPercent ?: JSONObject.NULL)
        .put("price_change_15m_pct", priceChange15mPercent ?: JSONObject.NULL)
        .put("activity_vs_baseline", activityRatio ?: JSONObject.NULL)
        .put("move_since_phase_start_pct", moveSincePhaseStartPercent ?: JSONObject.NULL)
        .put("explanation", explanation)
        .put("action_hint", actionHint)
        .put("watch_for", watchFor)
        .put("historical_reference", historicalReference)

    companion object {
        const val RESEARCH_REFERENCE =
            "Фон V5.4: 26 496 закрытых 5‑минутных свечей PUMP/BTC за 18.05–18.08.2026; это диапазон, не обещание."
    }
}

/**
 * Detects the lifecycle of aggressive buying without treating buy share as a price forecast.
 * Every feature is causal and uses only samples available at the analysis time.
 */
object BuyerBreathCycleAnalyzer {
    const val HISTORICAL_PUMP_SHOCK_5M_PERCENT = 1.5475
    const val HISTORICAL_BTC_SHOCK_5M_PERCENT = 0.5600

    fun analyze(
        samples: List<LiveBreathingSample>,
        horizons: List<LiveBreathingHorizon>,
        fresh: Boolean
    ): BuyerBreathSnapshot {
        if (samples.size < 2 || !fresh) return BuyerBreathSnapshot()
        val ordered = samples.sortedBy { it.at }
        val latest = ordered.last()
        fun selected(minutes: Int, until: Long = latest.at): List<LiveBreathingSample> =
            ordered.filter { it.at > until - minutes * 60_000L && it.at <= until }
        fun buyer(minutes: Int, until: Long = latest.at): Double? = median(selected(minutes, until).map {
            val total = it.pumpBuyNotional60s + it.pumpSellNotional60s
            if (total > 0.0) it.pumpBuyNotional60s / total * 100.0 else it.pumpBuyerPercent
        })
        fun priceChange(minutes: Int): Double? {
            val window = selected(minutes)
            val first = window.firstOrNull()?.priceUsdt ?: return null
            val last = window.lastOrNull()?.priceUsdt ?: return null
            return if (first > 0.0 && last > 0.0) (last / first - 1.0) * 100.0 else null
        }

        val buy5 = buyer(5) ?: latest.pumpBuyerPercent
        val buy15 = buyer(15) ?: buy5
        val previous5 = buyer(5, latest.at - 5L * 60_000L) ?: buy15
        val buySlope = buy5 - previous5
        val price5 = priceChange(5) ?: latest.pumpChange60sPercent
        val price15 = priceChange(15) ?: price5
        val btcWindow5 = selected(5).filter { it.bitcoinPriceUsdt > 0.0 }
        val btcPrice5 = if (btcWindow5.size >= 2) {
            (btcWindow5.last().bitcoinPriceUsdt / btcWindow5.first().bitcoinPriceUsdt - 1.0) * 100.0
        } else null
        val recentActivity = median(selected(3).map { it.pumpBuyNotional60s + it.pumpSellNotional60s }
            .filter { it > 0.0 })
        val baselineActivity = median(ordered.filter {
            it.at <= latest.at - 5L * 60_000L && it.at >= latest.at - 35L * 60_000L
        }.map { it.pumpBuyNotional60s + it.pumpSellNotional60s }.filter { it > 0.0 })
        val activityRatio = if (recentActivity != null && baselineActivity != null && baselineActivity > 0.0) {
            recentActivity / baselineActivity
        } else null
        val acceleration = median(selected(3).map { it.tradeAcceleration }.filter { it > 0.0 })
        val book = median(selected(5).mapNotNull { it.bookImbalance }) ?: 0.0
        val persistence = selected(15).let { window ->
            if (window.isEmpty()) 0 else window.count { it.pumpBuyerPercent >= 50.0 } * 100 / window.size
        }
        val buyerScore = ((buy15 - 50.0) * 5.0).coerceIn(-100.0, 100.0)
        val priceScore = (price15 * 90.0).coerceIn(-100.0, 100.0)
        val activityScore = activityRatio?.let { ((it - 1.0) * 45.0).coerceIn(-100.0, 100.0) } ?: 0.0
        val pressure = (buyerScore * 0.44 + priceScore * 0.31 + activityScore * 0.15 +
            (book * 100.0).coerceIn(-100.0, 100.0) * 0.10).roundToInt().coerceIn(-100, 100)
        val efficiency = when {
            buy15 >= 53.0 && price15 <= 0.0 ->
                (priceScore - (buy15 - 53.0) * 3.0).roundToInt().coerceIn(-100, 100)
            buy15 <= 47.0 && price15 >= 0.0 ->
                (priceScore + (47.0 - buy15) * 2.0).roundToInt().coerceIn(-100, 100)
            else -> priceScore.roundToInt().coerceIn(-100, 100)
        }
        val recentHigh = selected(60).maxOfOrNull { it.priceUsdt } ?: latest.priceUsdt
        val pullback = if (recentHigh > 0.0) (recentHigh - latest.priceUsdt) / recentHigh * 100.0 else 0.0
        val absorption = (
            (if (buy15 >= 55.0) 18.0 + (buy15 - 55.0) * 2.5 else 0.0) +
                (if (buy15 >= 53.0 && price15 <= 0.0) 38.0 else 0.0) +
                (if (buySlope <= -5.0) 15.0 else 0.0) +
                (if (book <= -0.08) 12.0 else 0.0) +
                (if ((activityRatio ?: 1.0) >= 1.2 && price15 <= 0.0) 12.0 else 0.0)
            ).roundToInt().coerceIn(0, 100)
        val horizon5 = horizons.firstOrNull { it.minutes == 5 }?.score ?: 0
        val horizon15 = horizons.firstOrNull { it.minutes == 15 }?.score ?: 0
        val shock = abs(price5) >= HISTORICAL_PUMP_SHOCK_5M_PERCENT ||
            abs(latest.pumpChange60sPercent) >= 0.80 ||
            (btcPrice5 != null && abs(btcPrice5) >= HISTORICAL_BTC_SHOCK_5M_PERCENT)
        val sellerTakeover = buy5 <= 47.0 && buy15 <= 49.0 && price5 <= -0.08 &&
            price15 < 0.0 && horizon5 < 0
        val exhaustion = absorption >= 65 ||
            (buySlope <= -7.0 && buy5 <= 54.0 && price5 <= -0.05 && pullback >= 0.30) ||
            (buy15 <= 51.0 && price15 < 0.0 && horizon5 <= -10 && horizon15 > -20)
        val expansion = buy5 >= 55.0 && buy15 >= 54.0 && price5 >= 0.04 && price15 >= 0.08 &&
            persistence >= 60 && ((activityRatio ?: 1.15) >= 1.10 || (acceleration ?: 1.3) >= 1.30)
        val ignition = buy5 >= 55.0 && buySlope >= 3.0 && price5 >= 0.03 &&
            ((activityRatio ?: 1.20) >= 1.20 || (acceleration ?: 1.3) >= 1.30)
        val mature = buy15 >= 52.0 && price15 > 0.08 && horizon15 > 0 &&
            (buySlope < 3.0 || (activityRatio ?: 1.0) < 1.10)

        val phase = when {
            shock -> BuyerBreathPhase.SHOCK
            sellerTakeover -> BuyerBreathPhase.SELLER_TAKEOVER
            exhaustion -> BuyerBreathPhase.EXHAUSTION
            expansion -> BuyerBreathPhase.EXPANSION
            ignition -> BuyerBreathPhase.IGNITION
            mature -> BuyerBreathPhase.MATURE
            else -> BuyerBreathPhase.QUIET
        }
        val agePredicate: (LiveBreathingSample) -> Boolean = when (phase) {
            BuyerBreathPhase.IGNITION, BuyerBreathPhase.EXPANSION, BuyerBreathPhase.MATURE ->
                { it.pumpBuyerPercent >= 52.0 }
            BuyerBreathPhase.EXHAUSTION -> { it.pumpBuyerPercent in 48.0..58.0 }
            BuyerBreathPhase.SELLER_TAKEOVER -> { it.pumpBuyerPercent <= 49.0 }
            else -> { _ -> true }
        }
        val phaseStart = ordered.asReversed().takeWhile(agePredicate).lastOrNull() ?: latest
        val age = ((latest.at - phaseStart.at).coerceAtLeast(0L) / 60_000L).toInt().coerceAtMost(360)
        val moveSinceStart = phaseStart.priceUsdt.takeIf { it > 0.0 }?.let {
            (latest.priceUsdt / it - 1.0) * 100.0
        }
        val confidence = (35 + minOf(35, selected(15).size / 4) +
            (if (activityRatio != null) 15 else 0) + (if (horizons.count { it.score != null } >= 2) 15 else 0))
            .coerceIn(0, 100)
        return presentation(
            phase, pressure, efficiency, absorption, confidence, age, buy5, buy15,
            price5, price15, activityRatio, moveSinceStart
        )
    }

    private fun presentation(
        phase: BuyerBreathPhase,
        pressure: Int,
        efficiency: Int,
        absorption: Int,
        confidence: Int,
        age: Int,
        buy5: Double,
        buy15: Double,
        price5: Double,
        price15: Double,
        activityRatio: Double?,
        move: Double?
    ): BuyerBreathSnapshot {
        val title: String
        val explanation: String
        val action: String
        val watch: String
        val historical: String
        when (phase) {
            BuyerBreathPhase.IGNITION -> {
                title = "НАЧАЛО ВДОХА"
                explanation = "Доля и интенсивность покупок растут, цена начала отвечать. Это ранний признак, ещё не гарантированный разгон."
                action = "Не гнаться за одним всплеском: проверить, удержится ли напор 5–15 минут и не появится ли поглощение."
                watch = "Покупки ≥55%, активность ≥1,2× и положительная реакция цены; отмена — возврат ниже 50% без роста."
                historical = "1 087 ранних признаков: локальный максимум обычно через 5–50 мин; дополнительный максимум +0,11…+1,16%. Через 60 мин положительный итог был лишь в 47,7% случаев."
            }
            BuyerBreathPhase.EXPANSION -> {
                title = "ВДОХ • РАЗГОН"
                explanation = "Покупатели сохраняют перевес, объём повышен и цена пока эффективно следует за потоком."
                action = "Импульс жив, но новая покупка после вертикального рывка опасна. Открытую позицию держать, пока эффективность не ломается."
                watch = "Замедление покупок на 7+ п.п., остановка цены при высоком buy% или переход 5/15 минут в продажи."
                historical = "Спокойный цикл обычно длился 20–65 мин, медиана 35 мин. Диапазон описывает прошлые циклы, а не оставшуюся прибыль."
            }
            BuyerBreathPhase.MATURE -> {
                title = "ЗРЕЛЫЙ ИМПУЛЬС"
                explanation = "Рост ещё держится, но ускорение покупок уже не раннее. Риск догонять цену выше, чем в начале вдоха."
                action = "Открытую позицию можно сопровождать; для нового входа ждать откат/ретест, а не покупать только из-за высокого процента."
                watch = "Сохраняется ли рост цены на единицу потока; высокая доля покупок без нового максимума означает выдыхание."
                historical = "В исследовании поток чаще достигал локального максимума раньше цены; поэтому высокий buy% ближе к пику не был самостоятельным входом."
            }
            BuyerBreathPhase.EXHAUSTION -> {
                title = "ВЫДЫХАНИЕ • ПОГЛОЩЕНИЕ"
                explanation = "Покупки ещё видны, но хуже двигают цену, либо их напор заметно снизился. Возможен крупный продавец, поглощающий спрос."
                action = "Не выходить по одному индикатору. Подготовить выход и ждать подтверждение продажами, APP или слабостью 15/30/60 минут."
                watch = "Покупки ниже 48–49% вместе с падением цены подтверждают разворот; возврат цены и buy% снимает тревогу."
                historical = "Простой маркер выдоха сам по себе не дал устойчивого направления вперёд; поэтому V5.4 требует подтверждения несколькими группами."
            }
            BuyerBreathPhase.SELLER_TAKEOVER -> {
                title = "ПРОДАВЦЫ ПЕРЕХВАТЫВАЮТ"
                explanation = "На 5 и 15 минутах доминируют агрессивные продажи, а цена подтверждает давление вниз."
                action = "Для открытой позиции срочно перепроверить выход. Окончательная команда остаётся за защитой позиции и аварийными правилами."
                watch = "Возврат покупателей выше 52–55% с ростом цены отменяет захват; продолжение продаж усиливает риск."
                historical = "Это подтверждение текущего состояния, не точный прогноз следующего часа. Стакан и BTC используются только как дополнительные подтверждения."
            }
            BuyerBreathPhase.SHOCK -> {
                title = "РЕЗКИЙ ОБРЫВ / ШОК"
                explanation = "Движение вышло за спокойный исторический режим; обычная форма вдоха сейчас ненадёжна."
                action = "При открытой позиции действует аварийная защита. Не усреднять и не ждать обычного цикла без стабилизации."
                watch = "Снижение минутной скорости, восстановление покупателей и удержание цены минимум два контрольных цикла."
                historical = "Порог спокойного режима: |PUMP 5 мин| 1,55% или |BTC 5 мин| 0,56% (верхние 0,5% трёхмесячной выборки)."
            }
            BuyerBreathPhase.QUIET -> {
                title = "ЗАТИШЬЕ • ЖДЁМ ВНИЗУ"
                explanation = "Устойчивого покупательского вдоха или подтверждённого давления продавцов сейчас нет."
                action = "Наблюдать. Не входить только из-за одиночных 60-секундных 60–70% покупок."
                watch = "Одновременный рост доли покупок, объёма и цены даст начало вдоха; слабая реакция цены покажет поглощение."
                historical = "Трёхмесячное исследование показало: сама доля покупок не предсказывает итог через час. Важна реакция цены и устойчивость."
            }
            BuyerBreathPhase.STALE -> error("STALE is returned before presentation")
        }
        return BuyerBreathSnapshot(
            phase = phase,
            title = title,
            fresh = true,
            pressureScore = pressure,
            efficiencyScore = efficiency,
            absorptionRisk = absorption,
            confidence = confidence,
            ageMinutes = age,
            buyerPercent5m = buy5,
            buyerPercent15m = buy15,
            priceChange5mPercent = price5,
            priceChange15mPercent = price15,
            activityRatio = activityRatio,
            moveSincePhaseStartPercent = move,
            explanation = explanation,
            actionHint = action,
            watchFor = watch,
            historicalReference = historical
        )
    }

    private fun median(values: List<Double>): Double? {
        val sorted = values.filter(Double::isFinite).sorted()
        if (sorted.isEmpty()) return null
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2.0
    }
}

object BuyerBreathText {
    fun compact(snapshot: BuyerBreathSnapshot): String = buildString {
        append("ДЫХАНИЕ РЫНКА • ").append(snapshot.title)
        append("\nНапор ").append(signed(snapshot.pressureScore))
            .append(" • эффективность ").append(signed(snapshot.efficiencyScore))
            .append(" • поглощение ").append(snapshot.absorptionRisk).append("/100")
        if (snapshot.fresh) append("\n").append(snapshot.actionHint) else append("\nЖдём свежий поток сделок.")
    }

    fun detailed(snapshot: BuyerBreathSnapshot): String = buildString {
        append(snapshot.title).append(" • фаза ").append(snapshot.ageMinutes).append(" мин")
        append("\nНапор: ").append(signed(snapshot.pressureScore)).append("/100")
        append(" • эффективность цены: ").append(signed(snapshot.efficiencyScore)).append("/100")
        append(" • поглощение: ").append(snapshot.absorptionRisk).append("/100")
        append(String.format(Locale.GERMANY, "\nПокупки: 5 мин %.1f%% • 15 мин %.1f%%", snapshot.buyerPercent5m ?: 50.0, snapshot.buyerPercent15m ?: 50.0))
        append(String.format(Locale.GERMANY, "\nЦена: 5 мин %+.3f%% • 15 мин %+.3f%%", snapshot.priceChange5mPercent ?: 0.0, snapshot.priceChange15mPercent ?: 0.0))
        snapshot.activityRatio?.let { append(String.format(Locale.GERMANY, " • активность %.2f×", it)) }
        snapshot.moveSincePhaseStartPercent?.let { append(String.format(Locale.GERMANY, "\nОт начала текущей фазы: %+.3f%%", it)) }
        append("\n\nЧТО ЭТО ЗНАЧИТ: ").append(snapshot.explanation)
        append("\nЧТО ДЕЛАТЬ: ").append(snapshot.actionHint)
        append("\nЧТО СЛЕДИТЬ: ").append(snapshot.watchFor)
        append("\n\nИСТОРИЧЕСКИЙ ОРИЕНТИР: ").append(snapshot.historicalReference)
        append("\n").append(BuyerBreathSnapshot.RESEARCH_REFERENCE)
    }

    private fun signed(value: Int?): String = value?.let { if (it >= 0) "+$it" else "$it" } ?: "—"
}
