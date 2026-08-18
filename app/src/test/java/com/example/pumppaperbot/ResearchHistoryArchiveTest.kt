package com.example.pumppaperbot

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResearchHistoryArchiveTest {
    @Test
    fun summarizesLegacyAccountsWithoutMergingBalances() {
        fun portfolio(trades: Int, decisions: Int): JSONObject = JSONObject()
            .put("cashEur", 777.0)
            .put("trades", JSONArray().apply {
                repeat(trades) { index ->
                    put(JSONObject().put("time", 1_000L + index).put("action", "BUY"))
                }
            })
            .put("decisions", JSONArray().apply {
                repeat(decisions) { index ->
                    put(JSONObject().put("decidedAt", 2_000L + index).put("requestedAction", "HOLD"))
                }
            })
        val root = JSONObject().put("accounts", JSONObject()
            .put("APP", portfolio(2, 3))
            .put("DeepSig", portfolio(4, 5))
            .put("DeepSigX", JSONObject().put("portfolio", portfolio(1, 2).toString())))

        val summary = ResearchHistoryArchive.summarize(root)

        assertEquals(7, summary.tradeCount)
        assertEquals(10, summary.decisionCount)
        assertTrue(summary.hasHistory)
        assertTrue(summary.compactText().contains("АРХИВ ДО V5 СОХРАНЁН"))
    }
}
