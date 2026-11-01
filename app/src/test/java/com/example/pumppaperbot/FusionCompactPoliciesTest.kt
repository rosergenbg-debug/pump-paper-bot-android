package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FusionCompactPoliciesTest {
    @Test fun equalBuySellQuoteStartsWithHalfPercentLoss() {
        assertEquals(-0.4987531172069879, FusionPositionFees.pnlPercent(1.0, 1.0), 0.000001)
    }

    @Test fun risingMarketUsesBidAndDeductsBothFees() {
        assertEquals(0.0, FusionPositionFees.pnlPercent(1.0, 1.0 / ((1 - .0025) / (1 + .0025))), 1e-8)
    }

    @Test fun staleTakerFlowNeverPretendsToBeLive() {
        val now = 1_000_000L
        val micro = MicroImpulseSnapshot(
            connected = true, updatedAt = now - 45_000L,
            buyNotional5m = 500.0, sellNotional5m = 1500.0, flowHistorySeconds = 60L
        )
        val model = FusionFlowViewModel.from(micro, now)
        assertFalse(model.fresh)
        assertEquals(-1000.0, model.rows[1].netUsdt, 0.00001)
    }

    @Test fun freshFlowWarnsAboutPartialCoverage() {
        val now = 1_000_000L
        val model = FusionFlowViewModel.from(MicroImpulseSnapshot(
            connected = true, updatedAt = now - 2_000L, flowHistorySeconds = 180L
        ), now)
        assertTrue(model.fresh)
        assertTrue(model.status.contains("набирается"))
    }
}
