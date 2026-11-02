package com.example.pumppaperbot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FusionFlowStatusTest {
    private val now = 1_200_000L

    @Test fun doesNotHideCauseWhenWebsocketIsConnecting() {
        val info = FusionFlowViewModel.from(
            MicroImpulseSnapshot(connected = false, phase = "CONNECTING"), now
        )
        assertFalse(info.fresh)
        assertTrue(info.status.contains("Подключение"))
    }

    @Test fun websocketFailuresExposeUsefulReason() {
        val info = FusionFlowViewModel.from(
            MicroImpulseSnapshot(connected = false, phase = "RECONNECTING", error = "connection refused"), now
        )
        assertFalse(info.fresh)
        assertTrue(info.status.contains("connection refused"))
    }

    @Test fun olderTradesRemainDistinctFromNetworkOutage() {
        val info = FusionFlowViewModel.from(
            MicroImpulseSnapshot(connected = true, phase = "CALM", updatedAt = now - 70_000L), now
        )
        assertFalse(info.fresh)
        assertTrue(info.status.contains("не обновлялись"))
    }

    @Test fun recentTradesShowLiveFlow() {
        val info = FusionFlowViewModel.from(
            MicroImpulseSnapshot(connected = true, updatedAt = now - 4_000L, flowHistorySeconds = 540L), now
        )
        assertTrue(info.fresh)
        assertTrue(info.status.contains("Живой поток"))
    }
}
