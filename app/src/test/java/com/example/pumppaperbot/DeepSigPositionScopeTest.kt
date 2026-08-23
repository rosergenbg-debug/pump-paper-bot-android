package com.example.pumppaperbot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSigPositionScopeTest {
    @Test fun `open Fusion does not block a flat DeepSig from BUY`() {
        val scope = DeepSigPositionScope(
            deepSigPositionOpen = false,
            fusionPositionOpen = true
        )

        assertFalse(scope.actionPositionOpen)
        assertTrue(scope.fusionContextOnly)
        assertTrue("BUY" in scope.allowedActions)
        assertFalse("EXIT" in scope.allowedActions)
    }

    @Test fun `DeepSig own open position enables EXIT regardless of Fusion`() {
        val scope = DeepSigPositionScope(
            deepSigPositionOpen = true,
            fusionPositionOpen = true
        )

        assertTrue(scope.actionPositionOpen)
        assertTrue("EXIT" in scope.allowedActions)
        assertFalse("BUY" in scope.allowedActions)
    }
}
