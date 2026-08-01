package com.example.pumppaperbot

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class DeepSeekPrimaryState(
    val day: String = "",
    val lastAttempt: Long = 0L,
    val lastSuccess: Long = 0L,
    val model: String = "",
    val action: String = "WAIT",
    val direction: Int = 0,
    val danger: Int = 0,
    val confidence: Int = 0,
    val summary: String = "Ожидает первый рыночный кадр",
    val successfulToday: Int = 0,
    val failedToday: Int = 0,
    val promptTokensToday: Int = 0,
    val completionTokensToday: Int = 0,
    val error: String = ""
) {
    fun toJson() = JSONObject()
        .put("day", day)
        .put("lastAttempt", lastAttempt)
        .put("lastSuccess", lastSuccess)
        .put("model", model)
        .put("action", action)
        .put("direction", direction)
        .put("danger", danger)
        .put("confidence", confidence)
        .put("summary", summary)
        .put("successfulToday", successfulToday)
        .put("failedToday", failedToday)
        .put("promptTokensToday", promptTokensToday)
        .put("completionTokensToday", completionTokensToday)
        .put("error", error)

    companion object {
        fun fromJson(json: JSONObject) = DeepSeekPrimaryState(
            day = json.optString("day"),
            lastAttempt = json.optLong("lastAttempt"),
            lastSuccess = json.optLong("lastSuccess"),
            model = json.optString("model"),
            action = json.optString("action", "WAIT"),
            direction = json.optInt("direction").coerceIn(-100, 100),
            danger = json.optInt("danger").coerceIn(0, 10),
            confidence = json.optInt("confidence").coerceIn(0, 100),
            summary = json.optString("summary", "Ожидает первый рыночный кадр"),
            successfulToday = json.optInt("successfulToday").coerceAtLeast(0),
            failedToday = json.optInt("failedToday").coerceAtLeast(0),
            promptTokensToday = json.optInt("promptTokensToday").coerceAtLeast(0),
            completionTokensToday = json.optInt("completionTokensToday").coerceAtLeast(0),
            error = json.optString("error")
        )
    }
}

object DeepSeekPrimaryStore {
    private const val PREFS = "deepseek_primary_v41"
    private const val KEY_STATE = "state"
    private val utc = TimeZone.getTimeZone("UTC")

    fun dayKey(now: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = utc
    }.format(Date(now))

    fun state(context: Context, now: Long = System.currentTimeMillis()): DeepSeekPrimaryState {
        val stored = runCatching {
            DeepSeekPrimaryState.fromJson(JSONObject(
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(KEY_STATE, "{}").orEmpty()
            ))
        }.getOrDefault(DeepSeekPrimaryState())
        val today = dayKey(now)
        return if (stored.day == today) stored else stored.copy(
            day = today,
            successfulToday = 0,
            failedToday = 0,
            promptTokensToday = 0,
            completionTokensToday = 0
        ).also { save(context, it) }
    }

    fun save(context: Context, state: DeepSeekPrimaryState) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_STATE, state.toJson().toString()).apply()
    }
}

object DeepSeekPrimaryPolicy {
    const val INTERVAL = 10L * 60L * 1000L

    fun shouldRun(state: DeepSeekPrimaryState, hasMarketData: Boolean, force: Boolean, now: Long): Boolean =
        hasMarketData && (force || state.lastAttempt <= 0L || now - state.lastAttempt >= INTERVAL)

    fun compactStatus(state: DeepSeekPrimaryState, configured: Boolean): String = when {
        !configured -> "DEEPSEEK • ОСНОВНОЙ • ключ не введён"
        state.lastSuccess <= 0L && state.error.isNotBlank() ->
            "DEEPSEEK • ОСНОВНОЙ • ошибка: ${state.error}\nЗапросы сегодня: 0 успешно • ${state.failedToday} ошибок"
        state.lastSuccess <= 0L -> "DEEPSEEK • ОСНОВНОЙ • ожидает первый анализ"
        else -> buildString {
            append("DEEPSEEK • ОСНОВНОЙ • ${shortModel(state.model)} • ${state.action}")
            append("\n${state.summary}")
            append("\nСегодня: ${state.successfulToday} успешно • ${state.failedToday} ошибок")
            append(" • последний ${PumpBotEngine.formatTime(state.lastSuccess)}")
            if (state.error.isNotBlank()) append("\nПоследняя ошибка: ${state.error}")
        }
    }

    private fun shortModel(model: String) = when {
        model.contains("pro", ignoreCase = true) -> "PRO"
        model.contains("flash", ignoreCase = true) -> "FLASH"
        model.isBlank() -> "FLASH"
        else -> model.uppercase(Locale.ROOT)
    }
}

private data class DeepSeekPrimaryResult(
    val action: String,
    val direction: Int,
    val danger: Int,
    val confidence: Int,
    val summary: String,
    val promptTokens: Int,
    val completionTokens: Int
)

class DeepSeekPrimaryAnalyst {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(75, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun sync(
        context: Context,
        force: Boolean = false,
        now: Long = System.currentTimeMillis()
    ): DeepSeekPrimaryState {
        val snapshot = PumpBotEngine.snapshot(context)
        val previous = DeepSeekPrimaryStore.state(context, now)
        if (!DeepSeekPrimaryPolicy.shouldRun(previous, snapshot.lastPrice > 0.0, force, now)) return previous
        val key = DeepSeekSecureKeyStore.read(context)
        if (key.isBlank()) return previous.copy(
            lastAttempt = now,
            error = "API-ключ DeepSeek не введён"
        ).also { DeepSeekPrimaryStore.save(context, it) }

        DeepSeekPrimaryStore.save(context, previous.copy(
            lastAttempt = now,
            model = PositionSupervisorPolicy.FLASH_MODEL,
            error = ""
        ))
        return runCatching { analyze(key, snapshot, EventRadarStore.state(context)) }.fold(
            onSuccess = { result ->
                previous.copy(
                    lastAttempt = now,
                    lastSuccess = now,
                    model = PositionSupervisorPolicy.FLASH_MODEL,
                    action = result.action,
                    direction = result.direction,
                    danger = result.danger,
                    confidence = result.confidence,
                    summary = result.summary,
                    successfulToday = previous.successfulToday + 1,
                    promptTokensToday = previous.promptTokensToday + result.promptTokens,
                    completionTokensToday = previous.completionTokensToday + result.completionTokens,
                    error = ""
                ).also { DeepSeekPrimaryStore.save(context, it) }
            },
            onFailure = { error ->
                previous.copy(
                    lastAttempt = now,
                    model = PositionSupervisorPolicy.FLASH_MODEL,
                    failedToday = previous.failedToday + 1,
                    error = error.message.orEmpty().take(240)
                ).also { DeepSeekPrimaryStore.save(context, it) }
            }
        )
    }

    private fun analyze(apiKey: String, snapshot: LiveSnapshot, radar: EventRadarState): DeepSeekPrimaryResult {
        val latestNews = radar.recent.take(5).map { event ->
            JSONObject()
                .put("source", event.source)
                .put("title", event.title.take(220))
                .put("importance", event.importance)
                .put("direction", event.directionScore)
                .put("published_at", event.publishedAt)
        }
        val frame = JSONObject()
            .put("symbol", "PUMP/EUR")
            .put("price_eur", snapshot.lastPrice)
            .put("rsi", snapshot.lastRsi)
            .put("ema_200", snapshot.lastEma200)
            .put("funding_rate", snapshot.fundingRate)
            .put("direction_score", snapshot.directionScore)
            .put("readiness_score", snapshot.readinessScore)
            .put("market_confidence", snapshot.breathingConfidence)
            .put("energy_score", snapshot.energyScore)
            .put("book_imbalance", snapshot.bookImbalance ?: JSONObject.NULL)
            .put("spread_percent", snapshot.spreadPercent ?: JSONObject.NULL)
            .put("open_interest_change_percent", snapshot.openInterestChangePercent ?: JSONObject.NULL)
            .put("rapid_drop_active", snapshot.rapidDrop.active)
            .put("local_buy_signal", snapshot.buySignal)
            .put("local_sell_signal", snapshot.sellSignal)
            .put("local_reason", snapshot.signalReason.take(600))
            .put("user_position_open", snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0)
            .put("news", JSONArray(latestNews))
        val system = """
            Ты основной независимый аналитик PumpSignal. Анализируй весь рыночный кадр и свежие новости.
            Не подменяй локальную стратегию и не обещай прибыль. Верни только JSON:
            action BUY, HOLD, WATCH или EXIT; direction целое -100..100; danger целое 0..10;
            confidence целое 0..100; summary одно короткое конкретное предложение по-русски.
            Если пользователь не в позиции, EXIT не используй. Если данных недостаточно, выбери WATCH.
        """.trimIndent()
        val body = JSONObject()
            .put("model", PositionSupervisorPolicy.FLASH_MODEL)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", system))
                .put(JSONObject().put("role", "user").put("content", frame.toString())))
            .put("thinking", JSONObject().put("type", "enabled"))
            .put("reasoning_effort", "low")
            .put("response_format", JSONObject().put("type", "json_object"))
            .put("max_tokens", 650)
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
                error(message.take(240))
            }
            val root = JSONObject(raw)
            val content = root.optJSONArray("choices")?.optJSONObject(0)
                ?.optJSONObject("message")?.optString("content").orEmpty()
            val json = JSONObject(content)
            val usage = root.optJSONObject("usage")
            val positionOpen = snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0
            val allowed = if (positionOpen) setOf("BUY", "HOLD", "WATCH", "EXIT") else setOf("BUY", "HOLD", "WATCH")
            val action = json.optString("action", "WATCH").uppercase(Locale.ROOT)
                .takeIf { it in allowed } ?: "WATCH"
            return DeepSeekPrimaryResult(
                action = action,
                direction = json.optInt("direction").coerceIn(-100, 100),
                danger = json.optInt("danger").coerceIn(0, 10),
                confidence = json.optInt("confidence").coerceIn(0, 100),
                summary = json.optString("summary", "DeepSeek не дал пояснение").take(400),
                promptTokens = usage?.optInt("prompt_tokens") ?: 0,
                completionTokens = usage?.optInt("completion_tokens") ?: 0
            )
        }
    }
}
