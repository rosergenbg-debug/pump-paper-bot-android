package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalPositionAdvisorTest {
    @Test
    fun `btc acceleration is treated as same direction context, not inverse rule`() {
        val regime = BtcPumpRegimePolicy.classify(BtcPumpRegimeInput(
            pump1hPercent = 1.1,
            pump3hPercent = 1.8,
            btc1hPercent = 0.6,
            btc3hPercent = 1.1,
            btc6hPercent = 1.3
        ))

        assertEquals(BtcPumpRegimeType.BTC_ACCELERATION, regime.type)
        assertTrue(regime.explanation.contains("тот же час"))
        assertFalse(regime.explanation.contains("PUMP падает"))
    }

    @Test
    fun `cooling after btc impulse does not promise a pump catch up`() {
        val regime = BtcPumpRegimePolicy.classify(BtcPumpRegimeInput(
            pump1hPercent = -0.2,
            pump3hPercent = -0.4,
            btc1hPercent = 0.05,
            btc3hPercent = 0.2,
            btc6hPercent = 1.4
        ))

        assertEquals(BtcPumpRegimeType.BTC_COOLING_AFTER_IMPULSE, regime.type)
        assertTrue(regime.explanation.contains("догон"))
        assertTrue(regime.explanation.contains("не подтвердился"))
    }

    @Test
    fun `independent pump strength needs supporting live flow`() {
        val withoutFlow = BtcPumpRegimePolicy.classify(BtcPumpRegimeInput(
            pump1hPercent = 0.9,
            pump3hPercent = 1.6,
            btc1hPercent = 0.1,
            btc3hPercent = 0.2,
            btc6hPercent = 0.3,
            pumpBuyerPercent60s = 47.0,
            pumpBuyerPercent5m = 48.0,
            breathingScore = -10
        ))
        val withFlow = BtcPumpRegimePolicy.classify(BtcPumpRegimeInput(
            pump1hPercent = 0.9,
            pump3hPercent = 1.6,
            btc1hPercent = 0.1,
            btc3hPercent = 0.2,
            btc6hPercent = 0.3,
            pumpBuyerPercent60s = 56.0,
            pumpBuyerPercent5m = 53.0,
            breathingScore = 20
        ))

        assertFalse(withoutFlow.type == BtcPumpRegimeType.PUMP_INDEPENDENT_STRENGTH)
        assertEquals(BtcPumpRegimeType.PUMP_INDEPENDENT_STRENGTH, withFlow.type)
    }

    @Test
    fun `ordinary two percent drawdown stays hold without confirmed exit`() {
        val view = PersonalPositionAdvisorPolicy.render(
            state = PositionSupervisionState(
                lastSuccess = 1L,
                action = "HOLD",
                exitAdvised = false,
                dangerLevel = 4,
                pnlPercent = -2.0,
                summary = "Обычная волатильность, продавцы не удержали давление",
                trendStatus = "Боковик с попыткой восстановления",
                riskStatus = "Средний риск без подтверждённого разворота",
                nearTermScenario = "Вероятна консолидация; выход только при новой волне продаж"
            ),
            regime = mixedRegime(),
            supportPlan = plan(-2.0),
            intervalMinutes = 3
        )

        assertEquals(PersonalAdvisorSeverity.CALM, view.severity)
        assertTrue(view.text.contains("СЕЙЧАС: ДЕРЖАТЬ"))
        assertFalse(view.text.contains("ВЫХОД ПОДТВЕРЖДЁН"))
    }

    @Test
    fun `confirmed critical exit is always direct and red`() {
        val view = PersonalPositionAdvisorPolicy.render(
            state = PositionSupervisionState(
                lastSuccess = 1L,
                action = "EXIT",
                exitAdvised = true,
                dangerLevel = 10,
                summary = "APP и текущие продажи подтверждают продолжающийся разворот"
            ),
            regime = mixedRegime(),
            supportPlan = plan(-4.5),
            intervalMinutes = 1
        )

        assertEquals(PersonalAdvisorSeverity.EXIT, view.severity)
        assertTrue(view.text.contains("ВЫХОД ПОДТВЕРЖДЁН"))
        assertTrue(view.text.contains("без задержки"))
    }

    private fun mixedRegime() = BtcPumpRegimeSnapshot(
        BtcPumpRegimeType.MIXED_OR_SIDEWAYS,
        "BTC/PUMP: смешанный режим",
        "Решение определяется текущими данными PUMP.",
        60,
        0
    )

    private fun plan(pnl: Double) = PositionSupportPlan(
        tier = "ОБЫЧНЫЙ КОНТРОЛЬ",
        intervalMillis = PositionSupervisorPolicy.FLASH_INTERVAL,
        model = PositionSupervisorPolicy.FLASH_MODEL,
        maxReasoning = false,
        pnlPercent = pnl,
        peakPnlPercent = 0.0,
        pullbackPercent = 2.0,
        trigger = "тест"
    )
}
