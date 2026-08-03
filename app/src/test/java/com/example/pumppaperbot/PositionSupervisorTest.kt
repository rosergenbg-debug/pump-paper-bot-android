package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PositionSupervisorTest {
    @Test
    fun openSergePositionBypassesRoutineDeepSeekCostCeiling() {
        assertTrue(PositionSupervisorPolicy.paidCheckAllowed(true, 99.0))
        assertFalse(PositionSupervisorPolicy.paidCheckAllowed(false, 0.50))
    }

    @Test
    fun stateJsonRoundTripPreservesExitBaselineAndScale() {
        val original = PositionSupervisionState(
            positionEntryTime = 100L,
            lastAttempt = 200L,
            lastSuccess = 190L,
            model = PositionSupervisorPolicy.PRO_MODEL,
            action = "EXIT",
            exitAdvised = true,
            exitAdvisedAt = 180L,
            exitBaselinePrice = 0.0025,
            exitBaselineDirection = -62,
            exitBaselineRsi = 38.0,
            exitBaselineDanger = 7,
            conditionDelta = -7,
            dangerLevel = 9,
            summary = "Давление продавцов усиливается",
            promptTokens = 123,
            completionTokens = 45
        )

        val restored = PositionSupervisionState.fromJson(original.toJson())

        assertEquals(original, restored)
    }

    @Test
    fun scalesAreClampedWhenReadingOldOrInvalidData() {
        val restored = PositionSupervisionState.fromJson(
            org.json.JSONObject()
                .put("conditionDelta", -99)
                .put("dangerLevel", 50)
        )

        assertEquals(-10, restored.conditionDelta)
        assertEquals(10, restored.dangerLevel)
    }

    @Test
    fun statusDistinguishesDeteriorationImprovementAndCancellation() {
        val exit = PositionSupervisionState(
            lastSuccess = 1L,
            exitAdvised = true,
            conditionDelta = -6,
            dangerLevel = 8,
            summary = "хуже"
        )
        assertTrue(PositionSupervisorPolicy.statusText(exit).contains("ухудшается"))
        assertTrue(PositionSupervisorPolicy.statusText(exit.copy(conditionDelta = 4)).contains("улучшается"))
        assertTrue(PositionSupervisorPolicy.statusText(exit.copy(action = "CANCEL_EXIT")).contains("ОТМЕНА ВЫХОДА"))
        assertFalse(PositionSupervisorPolicy.statusText(exit.copy(exitAdvised = false)).contains("ВЫХОД РЕКОМЕНДОВАН"))
    }

    @Test
    fun dispatcherUsesProForNewOrCriticalPositionAndFlashForRoutineChecks() {
        val now = 10_000_000L
        val state = PositionSupervisionState(
            positionEntryTime = 100L,
            lastAttempt = now - PositionSupervisorPolicy.FLASH_INTERVAL
        )
        assertEquals(
            PositionSupervisorPolicy.PRO_MODEL,
            PositionSupervisorPolicy.chooseModelForPosition(
                state, true, 200L, critical = false, forceCritical = false, now = now
            )
        )
        assertEquals(
            PositionSupervisorPolicy.PRO_MODEL,
            PositionSupervisorPolicy.chooseModelForPosition(
                state, true, 100L, critical = true, forceCritical = false, now = now
            )
        )
        assertEquals(
            PositionSupervisorPolicy.FLASH_MODEL,
            PositionSupervisorPolicy.chooseModelForPosition(
                state, true, 100L, critical = false, forceCritical = false, now = now
            )
        )
        assertEquals(
            null,
            PositionSupervisorPolicy.chooseModelForPosition(
                state.copy(lastAttempt = now), true, 100L,
                critical = false, forceCritical = false, now = now
            )
        )
    }
}
