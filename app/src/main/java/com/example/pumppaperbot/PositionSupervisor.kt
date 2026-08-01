package com.example.pumppaperbot

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class PositionSupervisionState(
    val positionEntryTime: Long = 0L,
    val lastAttempt: Long = 0L,
    val lastSuccess: Long = 0L,
    val model: String = "",
    val action: String = "WAIT",
    val exitAdvised: Boolean = false,
    val exitAdvisedAt: Long = 0L,
    val exitBaselinePrice: Double = 0.0,
    val exitBaselineDirection: Int = 0,
    val exitBaselineRsi: Double = 0.0,
    val exitBaselineDanger: Int = 0,
    val conditionDelta: Int = 0,
    val dangerLevel: Int = 0,
    val summary: String = "Ожидает открытия позиции",
    val error: String = "",
    val promptTokens: Int = 0,
    val completionTokens: Int = 0
) {
    fun toJson(): JSONObject = JSONObject()
        .put("positionEntryTime", positionEntryTime)
        .put("lastAttempt", lastAttempt)
        .put("lastSuccess", lastSuccess)
        .put("model", model)
        .put("action", action)
        .put("exitAdvised", exitAdvised)
        .put("exitAdvisedAt", exitAdvisedAt)
        .put("exitBaselinePrice", exitBaselinePrice)
        .put("exitBaselineDirection", exitBaselineDirection)
        .put("exitBaselineRsi", exitBaselineRsi)
        .put("exitBaselineDanger", exitBaselineDanger)
        .put("conditionDelta", conditionDelta)
        .put("dangerLevel", dangerLevel)
        .put("summary", summary)
        .put("error", error)
        .put("promptTokens", promptTokens)
        .put("completionTokens", completionTokens)

    companion object {
        fun fromJson(json: JSONObject) = PositionSupervisionState(
            positionEntryTime = json.optLong("positionEntryTime"),
            lastAttempt = json.optLong("lastAttempt"),
            lastSuccess = json.optLong("lastSuccess"),
            model = json.optString("model"),
            action = json.optString("action", "WAIT"),
            exitAdvised = json.optBoolean("exitAdvised"),
            exitAdvisedAt = json.optLong("exitAdvisedAt"),
            exitBaselinePrice = json.optDouble("exitBaselinePrice", 0.0),
            exitBaselineDirection = json.optInt("exitBaselineDirection"),
            exitBaselineRsi = json.optDouble("exitBaselineRsi", 0.0),
            exitBaselineDanger = json.optInt("exitBaselineDanger").coerceIn(0, 10),
            conditionDelta = json.optInt("conditionDelta").coerceIn(-10, 10),
            dangerLevel = json.optInt("dangerLevel").coerceIn(0, 10),
            summary = json.optString("summary", "Ожидает открытия позиции"),
            error = json.optString("error"),
            promptTokens = json.optInt("promptTokens"),
            completionTokens = json.optInt("completionTokens")
        )
    }
}

object PositionSupervisorPolicy {
    const val FLASH_MODEL = "deepseek-v4-flash"
    const val PRO_MODEL = "deepseek-v4-pro"
    const val FLASH_INTERVAL = 15L * 60L * 1000L
    const val PRO_RECHECK_INTERVAL = 5L * 60L * 1000L

    fun chooseModel(
        state: PositionSupervisionState,
        snapshot: LiveSnapshot,
        forceCritical: Boolean,
        now: Long
    ): String? {
        val critical = snapshot.sellSignal || snapshot.rapidDrop.active ||
            snapshot.directionScore <= -65 || state.exitAdvised || state.dangerLevel >= 6
        return chooseModelForPosition(
            state = state,
            positionOpen = snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0,
            entryTime = snapshot.entryTime,
            critical = critical,
            forceCritical = forceCritical,
            now = now
        )
    }

    internal fun chooseModelForPosition(
        state: PositionSupervisionState,
        positionOpen: Boolean,
        entryTime: Long,
        critical: Boolean,
        forceCritical: Boolean,
        now: Long
    ): String? {
        if (!positionOpen) return null
        if (forceCritical || state.positionEntryTime != entryTime) return PRO_MODEL
        val interval = if (critical) PRO_RECHECK_INTERVAL else FLASH_INTERVAL
        if (now - state.lastAttempt < interval) return null
        return if (critical) PRO_MODEL else FLASH_MODEL
    }

    fun statusText(state: PositionSupervisionState): String = when {
        state.lastSuccess <= 0L && state.error.isNotBlank() -> "DeepSeek: ${state.error}"
        state.lastSuccess <= 0L -> state.summary
        state.action == "CANCEL_EXIT" -> "ОТМЕНА ВЫХОДА • продолжаем наблюдение\n${state.summary}"
        state.exitAdvised && state.conditionDelta < 0 ->
            "ВЫХОД РЕКОМЕНДОВАН • ситуация ухудшается ${state.conditionDelta}/−10 • опасность ${state.dangerLevel}/10\n${state.summary}"
        state.exitAdvised && state.conditionDelta > 0 ->
            "ВЫХОД РЕКОМЕНДОВАН • ситуация улучшается +${state.conditionDelta}/+10 • опасность ${state.dangerLevel}/10\n${state.summary}"
        state.exitAdvised -> "ВЫХОД РЕКОМЕНДОВАН • контрольная точка 0 • опасность ${state.dangerLevel}/10\n${state.summary}"
        else -> "ПОЗИЦИЮ ДЕРЖИМ • опасность ${state.dangerLevel}/10\n${state.summary}"
    }
}

object PositionSupervisorStore {
    private const val PREFS = "position_supervisor_v4"
    private const val KEY_STATE = "state"

    fun state(context: Context): PositionSupervisionState = runCatching {
        PositionSupervisionState.fromJson(JSONObject(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_STATE, "{}").orEmpty()
        ))
    }.getOrDefault(PositionSupervisionState())

    fun save(context: Context, state: PositionSupervisionState) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_STATE, state.toJson().toString()).apply()
    }

    fun clearPosition(context: Context) = save(context, PositionSupervisionState())
}

private data class SupervisorApiResult(
    val action: String,
    val conditionDelta: Int,
    val dangerLevel: Int,
    val summary: String,
    val promptTokens: Int,
    val completionTokens: Int
)

private class DeepSeekApiException(val httpCode: Int, message: String) : RuntimeException(message)

class PositionSupervisorClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun sync(
        context: Context,
        forceCritical: Boolean = false,
        now: Long = System.currentTimeMillis()
    ): PositionSupervisionState {
        val snapshot = PumpBotEngine.snapshot(context)
        if (snapshot.waitMode != "SELL" || snapshot.entryPrice <= 0.0) {
            PositionSupervisorStore.clearPosition(context)
            return PositionSupervisorStore.state(context)
        }
        val stored = PositionSupervisorStore.state(context)
        val previous = if (stored.positionEntryTime == 0L || stored.positionEntryTime == snapshot.entryTime) {
            stored
        } else {
            PositionSupervisionState(
                positionEntryTime = snapshot.entryTime,
                summary = "Новая позиция открыта • запускается DeepSeek Pro"
            )
        }
        val model = PositionSupervisorPolicy.chooseModel(previous, snapshot, forceCritical, now)
            ?: return previous
        val key = DeepSeekSecureKeyStore.read(context)
        if (key.isBlank()) {
            return previous.copy(
                positionEntryTime = snapshot.entryTime,
                lastAttempt = now,
                error = "API-ключ DeepSeek не введён",
                summary = "Локальная стратегия продолжает следить за позицией"
            ).also { PositionSupervisorStore.save(context, it) }
        }
        PositionSupervisorStore.save(context, previous.copy(
            positionEntryTime = snapshot.entryTime,
            lastAttempt = now,
            model = model,
            error = ""
        ))
        return runCatching {
            var usedModel = model
            val result = try {
                analyze(key, model, snapshot, previous)
            } catch (error: DeepSeekApiException) {
                if (model != PositionSupervisorPolicy.PRO_MODEL ||
                    error.httpCode !in setOf(400, 404, 422)
                ) throw error
                usedModel = PositionSupervisorPolicy.FLASH_MODEL
                analyze(key, usedModel, snapshot, previous, criticalReasoning = true)
            }
            usedModel to result
        }.fold(
            onSuccess = { (usedModel, result) ->
                val firstExit = result.action == "EXIT" && !previous.exitAdvised
                val cancelExit = result.action == "CANCEL_EXIT" && previous.exitAdvised
                val stillExit = when (result.action) {
                    "EXIT" -> true
                    "CANCEL_EXIT" -> false
                    else -> previous.exitAdvised
                }
                val updated = previous.copy(
                    positionEntryTime = snapshot.entryTime,
                    lastAttempt = now,
                    lastSuccess = now,
                    model = usedModel,
                    action = result.action,
                    exitAdvised = stillExit,
                    exitAdvisedAt = when {
                        firstExit -> now
                        stillExit -> previous.exitAdvisedAt
                        else -> 0L
                    },
                    exitBaselinePrice = when {
                        firstExit -> snapshot.lastPrice
                        stillExit -> previous.exitBaselinePrice
                        else -> 0.0
                    },
                    exitBaselineDirection = when {
                        firstExit -> snapshot.directionScore
                        stillExit -> previous.exitBaselineDirection
                        else -> 0
                    },
                    exitBaselineRsi = when {
                        firstExit -> snapshot.lastRsi
                        stillExit -> previous.exitBaselineRsi
                        else -> 0.0
                    },
                    exitBaselineDanger = when {
                        firstExit -> result.dangerLevel
                        stillExit -> previous.exitBaselineDanger
                        else -> 0
                    },
                    conditionDelta = if (firstExit) 0 else result.conditionDelta,
                    dangerLevel = result.dangerLevel,
                    summary = result.summary,
                    error = "",
                    promptTokens = previous.promptTokens + result.promptTokens,
                    completionTokens = previous.completionTokens + result.completionTokens
                )
                PositionSupervisorStore.save(context, updated)
                if (firstExit || cancelExit ||
                    (stillExit && result.conditionDelta != previous.conditionDelta) ||
                    (stillExit && result.dangerLevel > previous.dangerLevel)
                ) {
                    runCatching { PumpAlert.showPositionSupervision(context, updated) }
                }
                updated
            },
            onFailure = { error ->
                previous.copy(
                    positionEntryTime = snapshot.entryTime,
                    lastAttempt = now,
                    model = model,
                    error = error.message.orEmpty().take(300)
                ).also { PositionSupervisorStore.save(context, it) }
            }
        )
    }

    private fun analyze(
        apiKey: String,
        model: String,
        snapshot: LiveSnapshot,
        previous: PositionSupervisionState,
        criticalReasoning: Boolean = false
    ): SupervisorApiResult {
        val frame = JSONObject()
            .put("symbol", "PUMP/EUR")
            .put("entry_price_eur", snapshot.entryPrice)
            .put("current_price_eur", snapshot.lastPrice)
            .put("highest_price_since_entry_eur", snapshot.highestClose)
            .put("pnl_percent", (snapshot.lastPrice / snapshot.entryPrice - 1.0) * 100.0)
            .put("rsi", snapshot.lastRsi)
            .put("funding_rate", snapshot.fundingRate)
            .put("direction_score", snapshot.directionScore)
            .put("market_confidence", snapshot.breathingConfidence)
            .put("energy_score", snapshot.energyScore)
            .put("book_imbalance", snapshot.bookImbalance ?: JSONObject.NULL)
            .put("spread_percent", snapshot.spreadPercent ?: JSONObject.NULL)
            .put("open_interest_change_percent", snapshot.openInterestChangePercent ?: JSONObject.NULL)
            .put("rapid_drop_active", snapshot.rapidDrop.active)
            .put("local_exit_signal", snapshot.sellSignal)
            .put("local_reason", snapshot.signalReason.take(600))
            .put("previous_exit_advised", previous.exitAdvised)
            .put("previous_condition_delta", previous.conditionDelta)
            .put("previous_danger_level", previous.dangerLevel)
            .put("previous_summary", previous.summary.take(500))
            .put("first_exit_baseline", JSONObject()
                .put("exists", previous.exitAdvisedAt > 0L)
                .put("time", previous.exitAdvisedAt)
                .put("price_eur", previous.exitBaselinePrice)
                .put("direction_score", previous.exitBaselineDirection)
                .put("rsi", previous.exitBaselineRsi)
                .put("danger_level", previous.exitBaselineDanger)
            )
        val system = """
            Ты сопровождаешь уже открытую пользователем позицию PUMP/EUR. Не решай вопрос входа.
            Главная задача — вовремя заметить ухудшение и выход, но не создавать ложную тревогу по одному индикатору.
            Верни только JSON: action HOLD, EXIT или CANCEL_EXIT; condition_delta целое от -10 до +10;
            danger_level целое от 0 до 10; summary кратко по-русски.
            condition_delta сравнивает ситуацию с моментом первого EXIT: отрицательное означает ухудшение,
            положительное — улучшение. CANCEL_EXIT допустим только если прежняя причина выхода действительно исчезла.
            Если previous_exit_advised=true, возвращай EXIT до тех пор, пока отмена не стала обоснованной;
            HOLD после уже выданного выхода не используй.
            danger_level 10 означает критическую угрозу позиции. Это аналитический сигнал, не гарантия результата.
        """.trimIndent()
        val body = JSONObject()
            .put("model", model)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", system))
                .put(JSONObject().put("role", "user").put("content", frame.toString())))
            .put("thinking", JSONObject().put("type", "enabled").put(
                "reasoning_effort", if (criticalReasoning || model == PositionSupervisorPolicy.PRO_MODEL) "max" else "low"
            ))
            .put("response_format", JSONObject().put("type", "json_object"))
            .put("max_tokens", if (model == PositionSupervisorPolicy.PRO_MODEL) 1200 else 500)
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
                throw DeepSeekApiException(response.code, message.take(300))
            }
            val root = JSONObject(raw)
            val content = root.optJSONArray("choices")?.optJSONObject(0)
                ?.optJSONObject("message")?.optString("content").orEmpty()
            val json = JSONObject(content)
            val usage = root.optJSONObject("usage")
            val action = json.optString("action", "HOLD").uppercase()
                .takeIf { it in setOf("HOLD", "EXIT", "CANCEL_EXIT") } ?: "HOLD"
            return SupervisorApiResult(
                action = action,
                conditionDelta = json.optInt("condition_delta").coerceIn(-10, 10),
                dangerLevel = json.optInt("danger_level").coerceIn(0, 10),
                summary = json.optString("summary", "DeepSeek не дал пояснение").take(500),
                promptTokens = usage?.optInt("prompt_tokens") ?: 0,
                completionTokens = usage?.optInt("completion_tokens") ?: 0
            )
        }
    }
}
