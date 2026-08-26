package com.example.pumppaperbot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class V6OutcomeCausalityTest {
    @Test fun `book obtained before origin cannot be reused as future outcome`() {
        val market = FusionMarketSnapshot(
            connected = true,
            bid = 1.01,
            ask = 1.02,
            lastSuccess = 9_000L
        )
        val frame = V6OutcomeCausalityPolicy.futureFrame(
            originAt = 10_000L,
            market = market,
            now = 40_000L
        )
        assertEquals(0L, frame.observedAt)
        assertNull(frame.bid)
    }

    @Test fun `future outcome uses actual Fusion observation timestamp not evaluation clock`() {
        val market = FusionMarketSnapshot(
            connected = true,
            bid = 1.03,
            ask = 1.04,
            lastSuccess = 42_000L
        )
        val frame = V6OutcomeCausalityPolicy.futureFrame(
            originAt = 10_000L,
            market = market,
            now = 50_000L
        )
        assertEquals(42_000L, frame.observedAt)
        assertEquals(1.03, frame.bid!!, 0.0000001)
    }

    @Test fun `future outcome rejects observation timestamp from the future clock`() {
        val market = FusionMarketSnapshot(
            connected = true,
            bid = 1.03,
            ask = 1.04,
            lastSuccess = 60_000L
        )
        val frame = V6OutcomeCausalityPolicy.futureFrame(
            originAt = 10_000L,
            market = market,
            now = 50_000L
        )
        assertEquals(0L, frame.observedAt)
        assertNull(frame.bid)
    }
}
