package com.example.pumppaperbot

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ResearchLogCompactionPolicyTest {
    @Test
    fun `changing mark values do not make duplicate cycle states unique`() {
        val first = "МОНИТОР; value=1000.12; tradeNet=0.21; trades=2; V526 PM3 COOLDOWN: ещё 519с"
        val second = "МОНИТОР; value=1001.44; tradeNet=0.34; trades=2; V526 PM3 COOLDOWN: ещё 385с"
        assertEquals(ResearchLogCompactionPolicy.semantic(first), ResearchLogCompactionPolicy.semantic(second))
    }

    @Test
    fun `real gate transition remains visible`() {
        val quiet = "МОНИТОР; value=1000; V526_PM3_NO_FOMO: фаза QUIET"
        val armed = "МОНИТОР; value=1000; RETEST ARMED: ждём откат"
        assertNotEquals(ResearchLogCompactionPolicy.semantic(quiet), ResearchLogCompactionPolicy.semantic(armed))
    }

    @Test
    fun `compact records repeat count without losing last detail`() {
        val events = listOf(
            JSONObject().put("time", 1_000L).put("agent", "PM").put("result", "CYCLE")
                .put("detail", "MONITOR; value=1000; WAIT"),
            JSONObject().put("time", 2_000L).put("agent", "PM").put("result", "CYCLE")
                .put("detail", "MONITOR; value=1001; WAIT")
        )
        val compact = ResearchLogCompactionPolicy.compact(events, 15L * 60L * 1000L)
        assertEquals(1, compact.size)
        assertEquals(2, compact.single().optInt("repeatCount"))
        assertEquals(2_000L, compact.single().optLong("lastTime"))
    }

    @Test
    fun `trade and error events are never compacted`() {
        val trades = listOf(
            JSONObject().put("time", 1_000L).put("agent", "PM").put("result", "SELL")
                .put("detail", "SELL +2.00% NET"),
            JSONObject().put("time", 2_000L).put("agent", "PM").put("result", "SELL")
                .put("detail", "SELL +2.00% NET")
        )
        assertEquals(2, ResearchLogCompactionPolicy.compact(trades, 15L * 60L * 1000L).size)
    }
}
