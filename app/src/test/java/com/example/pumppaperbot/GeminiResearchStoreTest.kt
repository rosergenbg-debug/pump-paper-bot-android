package com.example.pumppaperbot

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class GeminiResearchStoreTest {
    @Test fun `technical activity retention is exactly twenty four hours`() {
        assertEquals(
            TimeUnit.HOURS.toMillis(24),
            GeminiResearchStore.ACTIVITY_RETENTION_MILLIS
        )
    }
}
