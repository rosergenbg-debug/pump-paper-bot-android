package com.example.pumppaperbot

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSeekStructuredResponseTest {
    @Test fun `complete JSON response is accepted with usage`() {
        val raw = envelope(
            content = JSONObject().put("action", "WATCH").put("summary", "рынок ждёт").toString(),
            finishReason = "stop",
            promptTokens = 1200,
            completionTokens = 340
        )

        val parsed = DeepSeekResponseParser.parse(raw)

        assertEquals("WATCH", parsed.json.getString("action"))
        assertEquals(1200, parsed.promptTokens)
        assertEquals(340, parsed.completionTokens)
    }

    @Test fun `length response is rejected with tokens preserved for repair`() {
        val error = runCatching {
            DeepSeekResponseParser.parse(envelope("{\"action\":\"WAT", "length", 1300, 1600))
        }.exceptionOrNull() as DeepSeekStructuredException

        assertEquals("length", error.finishReason)
        assertEquals(1300, error.promptTokens)
        assertEquals(1600, error.completionTokens)
        assertTrue(DeepSeekRepairPolicy.shouldRetry(error))
    }

    @Test fun `empty and malformed content are repairable but HTTP errors are not`() {
        val empty = runCatching {
            DeepSeekResponseParser.parse(envelope("", "stop", 500, 20))
        }.exceptionOrNull() as DeepSeekStructuredException
        val malformed = runCatching {
            DeepSeekResponseParser.parse(envelope("not-json", "stop", 500, 20))
        }.exceptionOrNull() as DeepSeekStructuredException

        assertTrue(DeepSeekRepairPolicy.shouldRetry(empty))
        assertTrue(DeepSeekRepairPolicy.shouldRetry(malformed))
        assertFalse(DeepSeekRepairPolicy.shouldRetry(DeepSeekStructuredException(429, message = "rate")))
    }

    @Test fun `content filter is never retried`() {
        val error = runCatching {
            DeepSeekResponseParser.parse(envelope("", "content_filter", 500, 10))
        }.exceptionOrNull() as DeepSeekStructuredException

        assertFalse(DeepSeekRepairPolicy.shouldRetry(error))
    }

    @Test fun `cost estimate uses model-specific conservative cache-miss prices`() {
        val flash = ApiUsageEvent("DEEPSEEK", "TEST", "deepseek-v4-flash", "OK", 1L, promptTokens = 1_000_000, outputTokens = 1_000_000)
        val pro = flash.copy(model = "deepseek-v4-pro")

        assertEquals(0.42, DeepSeekCostPolicy.estimateUsd(flash), 0.000001)
        assertEquals(1.305, DeepSeekCostPolicy.estimateUsd(pro), 0.000001)
    }

    @Test fun `signal becomes stale after twelve minutes`() {
        val state = DeepSeekPrimaryState(lastSuccess = 1_000L)

        assertTrue(DeepSeekPrimaryPolicy.isFreshSignal(state, 1_000L + DeepSeekPrimaryPolicy.SIGNAL_MAX_AGE))
        assertFalse(DeepSeekPrimaryPolicy.isFreshSignal(state, 1_001L + DeepSeekPrimaryPolicy.SIGNAL_MAX_AGE))
    }

    private fun envelope(
        content: String,
        finishReason: String,
        promptTokens: Int,
        completionTokens: Int
    ): String = JSONObject()
        .put("choices", JSONArray().put(JSONObject()
            .put("finish_reason", finishReason)
            .put("message", JSONObject().put("content", content))))
        .put("usage", JSONObject()
            .put("prompt_tokens", promptTokens)
            .put("completion_tokens", completionTokens))
        .toString()
}
