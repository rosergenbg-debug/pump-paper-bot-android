package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSeekResearchCardPolicyTest {
    @Test
    fun lowReadinessSaysDoNotEnterAndExplainsNoTrade() {
        val card = DeepSeekResearchCardPolicy.render(
            DeepSeekPrimaryState(
                action = "WATCH",
                proposedAction = "WATCH",
                executionStatus = "СДЕЛКА НЕ ЗАПРАШИВАЛАСЬ",
                entryReadiness = 1
            ),
            inPosition = false,
            fresh = true
        )

        assertEquals(DeepSeekResearchCardTone.SAFE, card.tone)
        assertTrue(card.text.contains("СЕЙЧАС: НЕ ВХОДИТЬ"))
        assertTrue(card.text.contains("Сигнал BUY не сформирован"))
        assertFalse(card.text.contains("КАНДИДАТ ВХОДА"))
        assertFalse(card.text.contains("СДЕЛКА НЕ ЗАПРАШИВАЛАСЬ"))
    }

    @Test
    fun proposedButRejectedBuyIsNotPresentedAsEntry() {
        val card = DeepSeekResearchCardPolicy.render(
            DeepSeekPrimaryState(
                action = "WATCH",
                proposedAction = "BUY",
                executionStatus = "ОТКЛОНЕНО ПРОВЕРКОЙ",
                entryReadiness = 8
            ),
            inPosition = false,
            fresh = true
        )

        assertEquals(DeepSeekResearchCardTone.WATCH, card.tone)
        assertTrue(card.text.contains("BUY рассматривался, но проверка его не подтвердила"))
        assertFalse(card.text.contains("ВИРТУАЛЬНЫЙ ВХОД ПОДТВЕРЖДЁН"))
    }

    @Test
    fun approvedBuyIsExplicitlyVirtual() {
        val card = DeepSeekResearchCardPolicy.render(
            DeepSeekPrimaryState(
                action = "BUY",
                proposedAction = "BUY",
                executionStatus = "ОДОБРЕНО К ИСПОЛНЕНИЮ",
                entryReadiness = 9
            ),
            inPosition = false,
            fresh = true
        )

        assertEquals(DeepSeekResearchCardTone.READY, card.tone)
        assertTrue(card.text.contains("ВИРТУАЛЬНЫЙ ВХОД ПОДТВЕРЖДЁН"))
        assertTrue(card.text.contains("реальные деньги не используются"))
    }

    @Test
    fun positionHoldDoesNotReactToNoiseAsAnExit() {
        val card = DeepSeekResearchCardPolicy.render(
            DeepSeekPrimaryState(
                action = "HOLD",
                proposedAction = "HOLD",
                executionStatus = "СДЕЛКА НЕ ЗАПРАШИВАЛАСЬ",
                danger = 2
            ),
            inPosition = true,
            fresh = true
        )

        assertEquals(DeepSeekResearchCardTone.SAFE, card.tone)
        assertTrue(card.text.contains("УДЕРЖИВАТЬ И НАБЛЮДАТЬ"))
        assertTrue(card.text.contains("обычный рыночный шум"))
    }
}
