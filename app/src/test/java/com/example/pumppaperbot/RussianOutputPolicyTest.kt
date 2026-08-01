package com.example.pumppaperbot

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RussianOutputPolicyTest {
    @Test fun `russian text is preserved and chinese text is hidden`() {
        assertFalse(RussianOutputPolicy.containsHan("Рынок снижается, ожидаем подтверждения."))
        assertTrue(RussianOutputPolicy.containsHan("市场信号混杂"))
        assertEquals(
            "Текст скрыт: модель вернула ответ не на русском языке",
            RussianOutputPolicy.visible("市场信号混杂")
        )
    }

    @Test fun `legacy api event cannot show stored chinese response`() {
        val restored = ApiUsageEvent.fromJson(JSONObject()
            .put("provider", "DEEPSEEK")
            .put("circuit", "ОСНОВНОЙ РЫНОК")
            .put("model", "deepseek-v4-flash")
            .put("status", "OK")
            .put("at", 1L)
            .put("detail", "短期反弹但趋势仍弱"))

        assertFalse(RussianOutputPolicy.containsHan(restored.detail))
        assertTrue(restored.detail.contains("не на русском"))
    }

    @Test fun `gemini hourly parser rejects chinese user-visible fields`() {
        val content = JSONObject()
            .put("action", "HOLD")
            .put("direction", 0)
            .put("confidence", 50)
            .put("horizon_hours", 2)
            .put("reason_ru", "市场信号混杂")
            .put("risks", JSONArray().put("Нет подтверждения"))
        val response = JSONObject()
            .put("candidates", JSONArray().put(JSONObject()
                .put("finishReason", "STOP")
                .put("content", JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", content.toString())
                )))))

        val error = runCatching {
            GeminiHourlyResponseParser.parse(response.toString(), "gemini-3.6-flash")
        }.exceptionOrNull()

        assertTrue(error is GeminiApiException)
        assertTrue(error?.message.orEmpty().contains("не на русском"))
    }
}
