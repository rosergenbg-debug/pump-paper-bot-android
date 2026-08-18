package com.example.pumppaperbot

import java.util.Locale
import kotlin.math.abs

enum class BtcPumpRegimeType {
    BTC_ACCELERATION,
    BTC_COOLING_AFTER_IMPULSE,
    JOINT_RISK_OFF,
    PUMP_INDEPENDENT_STRENGTH,
    MIXED_OR_SIDEWAYS,
    INSUFFICIENT_DATA
}

data class BtcPumpRegimeInput(
    val pump1hPercent: Double? = null,
    val pump3hPercent: Double? = null,
    val pump6hPercent: Double? = null,
    val btc1hPercent: Double? = null,
    val btc3hPercent: Double? = null,
    val btc6hPercent: Double? = null,
    val pumpBuyerPercent60s: Double? = null,
    val pumpBuyerPercent5m: Double? = null,
    val btcBuyerPercent60s: Double? = null,
    val btcChange60sPercent: Double? = null,
    val breathingScore: Int? = null
)

data class BtcPumpRegimeSnapshot(
    val type: BtcPumpRegimeType,
    val title: String,
    val explanation: String,
    val confidence: Int,
    val exitRiskAdjustment: Int
)

/**
 * Interprets BTC as a changing market regime, not as a permanent inverse PUMP rule.
 * The thresholds are deliberately broad: the current order flow and APP confirmation
 * remain more important than a historical relationship.
 */
object BtcPumpRegimePolicy {
    const val RESEARCH_NOTE =
        "Фон V5.3: 4 088 общих часовых наблюдений PUMP/BTC за 01.03–18.08.2026; это контекст, не приказ."

    fun classify(input: BtcPumpRegimeInput): BtcPumpRegimeSnapshot {
        val hasHourly = listOf(
            input.pump1hPercent, input.pump3hPercent, input.pump6hPercent,
            input.btc1hPercent, input.btc3hPercent, input.btc6hPercent
        ).any { it != null }
        if (!hasHourly) return BtcPumpRegimeSnapshot(
            BtcPumpRegimeType.INSUFFICIENT_DATA,
            "BTC/PUMP: накапливаем данные",
            "Свежих часовых рядов пока недостаточно. Решение принимается по APP, цене и потоку сделок.",
            20,
            0
        )

        val pump1h = input.pump1hPercent
        val pump3h = input.pump3hPercent
        val btc1h = input.btc1hPercent
        val btc3h = input.btc3hPercent
        val btc6h = input.btc6hPercent
        val relative1h = if (pump1h != null && btc1h != null) pump1h - btc1h else null
        val relative3h = if (pump3h != null && btc3h != null) pump3h - btc3h else null
        val btcLiveWeak = (input.btcBuyerPercent60s ?: 50.0) < 47.0 ||
            (input.btcChange60sPercent ?: 0.0) <= -0.10
        val flowSupportsPump = (input.pumpBuyerPercent60s ?: 50.0) >= 52.0 ||
            (input.pumpBuyerPercent5m ?: 50.0) >= 51.0 ||
            (input.breathingScore ?: 0) >= 15

        if (btc1h != null && btc3h != null && pump3h != null &&
            btc1h <= -0.50 && btc3h <= -0.90 && pump3h <= -0.80
        ) return BtcPumpRegimeSnapshot(
            BtcPumpRegimeType.JOINT_RISK_OFF,
            "BTC/PUMP: совместное снижение",
            "Bitcoin и PUMP слабеют вместе. Риск повышен, но выход подтверждается только текущими продажами, APP или устойчивой слабостью 15/30/60 минут.",
            if (btcLiveWeak) 90 else 78,
            if (btcLiveWeak) 2 else 1
        )

        if (btc6h != null && btc1h != null && btc6h >= 0.80 && abs(btc1h) <= 0.25 &&
            (btc3h == null || btc3h < 0.60)
        ) return BtcPumpRegimeSnapshot(
            BtcPumpRegimeType.BTC_COOLING_AFTER_IMPULSE,
            "BTC/PUMP: Bitcoin остывает после импульса",
            "После быстрого роста BTC у PUMP в выборке чаще был откат, а автоматический «догон» не подтвердился. Держим только при собственной силе PUMP и покупательском потоке; не выходим по одному факту боковика BTC.",
            78,
            1
        )

        if (relative1h != null && relative3h != null && pump1h != null &&
            relative1h >= 0.45 && relative3h >= 0.80 && pump1h > 0.0 && flowSupportsPump
        ) return BtcPumpRegimeSnapshot(
            BtcPumpRegimeType.PUMP_INDEPENDENT_STRENGTH,
            "BTC/PUMP: PUMP сильнее рынка",
            "PUMP опережает Bitcoin на 1 и 3 часах, а свежий поток не противоречит движению. Это поддерживает удержание, пока покупатели и цена не развернутся.",
            82,
            -1
        )

        if (btc1h != null && btc3h != null &&
            (btc1h >= 0.40 && btc3h >= 0.80 || btc1h >= 0.15 && btc3h >= 1.20)
        ) return BtcPumpRegimeSnapshot(
            BtcPumpRegimeType.BTC_ACCELERATION,
            "BTC/PUMP: Bitcoin ускоряется",
            "В исследовании PUMP обычно двигался с Bitcoin в тот же час. Не предполагаем обратную связь и не гонимся за ценой: после завершения импульса отдельно проверяем риск отката PUMP.",
            84,
            0
        )

        return BtcPumpRegimeSnapshot(
            BtcPumpRegimeType.MIXED_OR_SIDEWAYS,
            "BTC/PUMP: смешанный режим",
            "У Bitcoin нет достаточно сильного режима. Главные данные сейчас — собственный тренд PUMP, исполненные сделки, устойчивость 15/30/60 минут и подтверждение APP.",
            60,
            0
        )
    }
}

enum class PersonalAdvisorSeverity { CALM, WATCH, EXIT }

data class PersonalAdvisorView(
    val text: String,
    val severity: PersonalAdvisorSeverity
)

object PersonalPositionAdvisorPolicy {
    fun render(
        state: PositionSupervisionState,
        regime: BtcPumpRegimeSnapshot,
        supportPlan: PositionSupportPlan,
        intervalMinutes: Long,
        buyerBreath: BuyerBreathSnapshot = BuyerBreathSnapshot()
    ): PersonalAdvisorView {
        val hasAnswer = state.lastSuccess > 0L
        val severity = when {
            state.exitAdvised -> PersonalAdvisorSeverity.EXIT
            state.dangerLevel >= 7 -> PersonalAdvisorSeverity.WATCH
            else -> PersonalAdvisorSeverity.CALM
        }
        val command = when {
            state.exitAdvised && state.dangerLevel >= 9 ->
                "ВЫХОД ПОДТВЕРЖДЁН — действуйте без задержки"
            state.exitAdvised ->
                "ПОДГОТОВЬТЕ ВЫХОД — разворот подтверждён"
            state.action == "CANCEL_EXIT" ->
                "ДЕРЖАТЬ — прежняя причина выхода снята"
            state.dangerLevel >= 7 ->
                "ДЕРЖАТЬ ПОД КОНТРОЛЕМ — выход ещё не подтверждён"
            else -> "ДЕРЖАТЬ — подтверждённой причины выходить нет"
        }
        val urgency = when {
            state.exitAdvised && state.dangerLevel >= 9 -> "критическая, решение уже подтверждено"
            state.exitAdvised -> "высокая, не ждать обычного цикла"
            state.dangerLevel >= 7 -> "повышенная, перепроверка до $intervalMinutes мин"
            else -> "обычная, следующая проверка до $intervalMinutes мин"
        }
        val pnl = formatSigned(state.pnlPercent)
        val peak = formatSigned(state.peakPnlPercent)
        val pullback = String.format(Locale.GERMANY, "%.2f%%", state.pullbackPercent)
        val reason = when {
            !hasAnswer && state.error.isNotBlank() ->
                "DeepSeek временно недоступен: ${state.error}. Локальная защита позиции продолжает работать."
            !hasAnswer -> state.summary
            else -> state.summary
        }
        val trend = state.trendStatus.ifBlank { "Тенденция ещё не оценена" }
        val scenario = state.nearTermScenario.ifBlank {
            "Ждём первый сценарий на 30–90 минут; текущий контроль не остановлен."
        }
        val risk = state.riskStatus.ifBlank {
            if (state.dangerLevel >= 7) "Риск повышен, но нужен подтверждённый разворот." else
                "Обычная волатильность допустима; одна просадка не является командой выхода."
        }
        return PersonalAdvisorView(
            severity = severity,
            text = buildString {
                append("МОЙ СОВЕТНИК • ").append(supportPlan.tier)
                append("\n\nСЕЙЧАС: ").append(command)
                append("\nСрочность: ").append(urgency)
                append("\nРезультат: ").append(pnl)
                    .append(" • пик ").append(peak).append(" • откат ").append(pullback)
                append("\n\nТЕНДЕНЦИЯ: ").append(trend)
                append("\nРИСК: ").append(state.dangerLevel).append("/10 • ").append(risk)
                append("\nПОЧЕМУ: ").append(reason)
                append("\n\nВПЕРЁД НА 30–90 МИН: ").append(scenario)
                append("\nЧТО ИЗМЕНИТ РЕШЕНИЕ: ").append(state.watchFor)
                append("\n\nСТАКАН: ").append(state.bookStatus)
                append("\nСДЕЛКИ: ").append(state.flowStatus)
                append("\nBITCOIN: ").append(state.bitcoinStatus)
                append("\n").append(regime.title).append(" — ").append(regime.explanation)
                append("\n").append(BtcPumpRegimePolicy.RESEARCH_NOTE)
                append("\n\nДЫХАНИЕ РЫНКА: ").append(buyerBreath.title)
                append(" • напор ").append(buyerBreath.pressureScore?.let { if (it >= 0) "+$it" else "$it" } ?: "—")
                append(" • эффективность ").append(buyerBreath.efficiencyScore?.let { if (it >= 0) "+$it" else "$it" } ?: "—")
                append(" • поглощение ").append(buyerBreath.absorptionRisk).append("/100")
                append("\n").append(buyerBreath.actionHint)
                append("\nСледить: ").append(buyerBreath.watchFor)
            }
        )
    }

    private fun formatSigned(value: Double): String =
        String.format(Locale.GERMANY, "%+.2f%%", value)
}
