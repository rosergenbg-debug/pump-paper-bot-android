package com.example.pumppaperbot

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class DeepSeekStructuredResult(
    val json: JSONObject,
    val promptTokens: Int,
    val completionTokens: Int,
    val finishReason: String,
    val repaired: Boolean
)

class DeepSeekStructuredException(
    val httpCode: Int = 0,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val finishReason: String = "",
    message: String
) : RuntimeException(message)

internal data class DeepSeekEnvelope(
    val json: JSONObject,
    val promptTokens: Int,
    val completionTokens: Int,
    val finishReason: String
)

internal object DeepSeekResponseParser {
    fun parse(raw: String): DeepSeekEnvelope {
        if (raw.isBlank()) throw DeepSeekStructuredException(message = "DeepSeek вернул пустой HTTP-ответ")
        val root = runCatching { JSONObject(raw) }.getOrElse {
            throw DeepSeekStructuredException(message = "Ответ DeepSeek не является JSON-конвертом")
        }
        val usage = root.optJSONObject("usage")
        val promptTokens = usage?.optInt("prompt_tokens") ?: 0
        val completionTokens = usage?.optInt("completion_tokens") ?: 0
        val choice = root.optJSONArray("choices")?.optJSONObject(0)
            ?: throw DeepSeekStructuredException(
                promptTokens = promptTokens,
                completionTokens = completionTokens,
                message = "DeepSeek не вернул choices[0]"
            )
        val finishReason = choice.optString("finish_reason")
        val content = choice.optJSONObject("message")?.optString("content").orEmpty().trim()
        if (finishReason != "stop") {
            throw DeepSeekStructuredException(
                promptTokens = promptTokens,
                completionTokens = completionTokens,
                finishReason = finishReason,
                message = when (finishReason) {
                    "length" -> "Ответ DeepSeek оборван по лимиту токенов"
                    "insufficient_system_resource" -> "DeepSeek прервал ответ из-за нагрузки сервера"
                    "content_filter" -> "Ответ DeepSeek остановлен фильтром"
                    else -> "DeepSeek завершил ответ: ${finishReason.ifBlank { "неизвестная причина" }}"
                }
            )
        }
        if (content.isBlank()) {
            throw DeepSeekStructuredException(
                promptTokens = promptTokens,
                completionTokens = completionTokens,
                finishReason = finishReason,
                message = "DeepSeek вернул пустое поле content"
            )
        }
        val json = runCatching { JSONObject(content) }.getOrElse { parseError ->
            throw DeepSeekStructuredException(
                promptTokens = promptTokens,
                completionTokens = completionTokens,
                finishReason = finishReason,
                message = "DeepSeek вернул повреждённый JSON: ${parseError.message.orEmpty().take(120)}"
            )
        }
        return DeepSeekEnvelope(json, promptTokens, completionTokens, finishReason)
    }
}

object DeepSeekRepairPolicy {
    fun shouldRetry(error: DeepSeekStructuredException): Boolean =
        error.httpCode == 0 && error.finishReason != "content_filter"
}

class DeepSeekStructuredClient(private val http: OkHttpClient) {
    fun request(
        apiKey: String,
        model: String,
        system: String,
        frame: JSONObject,
        reasoningEffort: String,
        maxTokens: Int,
        validate: (JSONObject) -> String? = { null },
        onRepairStart: (String) -> Unit = {}
    ): DeepSeekStructuredResult {
        val first = try {
            validateEnvelope(executeOnce(
                apiKey = apiKey,
                model = model,
                messages = baseMessages(system, frame),
                thinking = true,
                reasoningEffort = reasoningEffort,
                maxTokens = maxTokens
            ), validate)
        } catch (error: DeepSeekStructuredException) {
            if (!DeepSeekRepairPolicy.shouldRetry(error)) throw error
            onRepairStart(error.message.orEmpty())
            val repaired = try {
                validateEnvelope(executeOnce(
                    apiKey = apiKey,
                    model = model,
                    messages = repairMessages(system, frame),
                    thinking = false,
                    reasoningEffort = "low",
                    maxTokens = REPAIR_MAX_TOKENS
                ), validate)
            } catch (repairError: DeepSeekStructuredException) {
                throw DeepSeekStructuredException(
                    httpCode = repairError.httpCode,
                    promptTokens = error.promptTokens + repairError.promptTokens,
                    completionTokens = error.completionTokens + repairError.completionTokens,
                    finishReason = repairError.finishReason.ifBlank { error.finishReason },
                    message = "Повтор не восстановил ответ: ${repairError.message.orEmpty()}"
                )
            }
            return DeepSeekStructuredResult(
                json = repaired.json,
                promptTokens = error.promptTokens + repaired.promptTokens,
                completionTokens = error.completionTokens + repaired.completionTokens,
                finishReason = repaired.finishReason,
                repaired = true
            )
        }
        return DeepSeekStructuredResult(
            json = first.json,
            promptTokens = first.promptTokens,
            completionTokens = first.completionTokens,
            finishReason = first.finishReason,
            repaired = false
        )
    }

    private fun validateEnvelope(
        envelope: DeepSeekEnvelope,
        validate: (JSONObject) -> String?
    ): DeepSeekEnvelope {
        val problem = validate(envelope.json)?.takeIf { it.isNotBlank() } ?: return envelope
        throw DeepSeekStructuredException(
            promptTokens = envelope.promptTokens,
            completionTokens = envelope.completionTokens,
            finishReason = envelope.finishReason,
            message = "DeepSeek вернул неполную схему: ${problem.take(180)}"
        )
    }

    private fun executeOnce(
        apiKey: String,
        model: String,
        messages: JSONArray,
        thinking: Boolean,
        reasoningEffort: String,
        maxTokens: Int
    ): DeepSeekEnvelope {
        val body = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("thinking", JSONObject().put("type", if (thinking) "enabled" else "disabled"))
            .put("reasoning_effort", reasoningEffort)
            .put("response_format", JSONObject().put("type", "json_object"))
            .put("max_tokens", maxTokens)
            .put("user_id", "pumpsignal_android")
        val request = Request.Builder()
            .url("https://api.deepseek.com/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = runCatching {
                    JSONObject(raw).optJSONObject("error")?.optString("message")
                }.getOrNull().orEmpty().ifBlank { "DeepSeek HTTP ${response.code}" }
                throw DeepSeekStructuredException(httpCode = response.code, message = message.take(300))
            }
            return DeepSeekResponseParser.parse(raw)
        }
    }

    private fun baseMessages(system: String, frame: JSONObject) = JSONArray()
        .put(JSONObject().put("role", "system").put("content", system))
        .put(JSONObject().put("role", "user").put("content", frame.toString()))

    private fun repairMessages(system: String, frame: JSONObject) = baseMessages(system, frame)
        .put(JSONObject().put("role", "user").put(
            "content",
            "Верни немедленно только короткий итоговый JSON по указанной схеме. Без рассуждений и Markdown."
        ))

    companion object {
        const val REPAIR_MAX_TOKENS = 550
    }
}
