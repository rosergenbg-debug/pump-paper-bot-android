package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ApiUsageEvent(
    val provider: String,
    val circuit: String,
    val model: String,
    val status: String,
    val at: Long,
    val durationMillis: Long = 0L,
    val promptTokens: Int = 0,
    val outputTokens: Int = 0,
    val detail: String = ""
) {
    fun toJson() = JSONObject()
        .put("provider", provider)
        .put("circuit", circuit)
        .put("model", model)
        .put("status", status)
        .put("at", at)
        .put("durationMillis", durationMillis)
        .put("promptTokens", promptTokens)
        .put("outputTokens", outputTokens)
        .put("detail", detail)

    companion object {
        fun fromJson(json: JSONObject) = ApiUsageEvent(
            provider = json.optString("provider"),
            circuit = json.optString("circuit"),
            model = json.optString("model"),
            status = json.optString("status"),
            at = json.optLong("at"),
            durationMillis = json.optLong("durationMillis"),
            promptTokens = json.optInt("promptTokens").coerceAtLeast(0),
            outputTokens = json.optInt("outputTokens").coerceAtLeast(0),
            detail = json.optString("detail").take(500)
        )
    }
}

data class ApiUsageSummary(
    val requestsLastMinute: Int,
    val requestsLastHour: Int,
    val requestsToday: Int,
    val successesToday: Int,
    val errorsToday: Int,
    val promptTokensToday: Int,
    val outputTokensToday: Int
)

object ApiUsageLogStore {
    private const val PREFS = "api_usage_log_v42"
    private const val KEY_EVENTS = "events"
    private const val MAX_EVENTS = 120
    private val lock = Any()

    fun record(context: Context, event: ApiUsageEvent) = synchronized(lock) {
        val updated = (list(context) + event)
            .filter { event.at - it.at <= 7L * 24L * 60L * 60L * 1000L }
            .takeLast(MAX_EVENTS)
        val json = JSONArray()
        updated.forEach { json.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_EVENTS, json.toString()).apply()
    }

    fun list(context: Context, provider: String? = null): List<ApiUsageEvent> = synchronized(lock) {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_EVENTS, "[]").orEmpty()
        val parsed = runCatching {
            val json = JSONArray(raw)
            List(json.length()) { index -> ApiUsageEvent.fromJson(json.getJSONObject(index)) }
        }.getOrDefault(emptyList())
        if (provider == null) parsed else parsed.filter { it.provider.equals(provider, true) }
    }

    fun summary(context: Context, provider: String, now: Long = System.currentTimeMillis()): ApiUsageSummary {
        val events = list(context, provider)
        val todayStart = now - now % (24L * 60L * 60L * 1000L)
        val requests = events.filter { it.status == "START" }
        val today = events.filter { it.at >= todayStart }
        return ApiUsageSummary(
            requestsLastMinute = requests.count { now - it.at <= 60_000L },
            requestsLastHour = requests.count { now - it.at <= 3_600_000L },
            requestsToday = requests.count { it.at >= todayStart },
            successesToday = today.count { it.status == "OK" },
            errorsToday = today.count { it.status == "ERROR" },
            promptTokensToday = today.filter { it.status == "OK" }.sumOf { it.promptTokens },
            outputTokensToday = today.filter { it.status == "OK" }.sumOf { it.outputTokens }
        )
    }
}
