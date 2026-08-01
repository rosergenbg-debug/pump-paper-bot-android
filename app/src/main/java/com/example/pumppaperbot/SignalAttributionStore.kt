package com.example.pumppaperbot

import android.content.Context
import org.json.JSONObject

data class SignalAttribution(
    val source: String,
    val kind: String,
    val reason: String,
    val at: Long,
    val executedTrade: Boolean
)

/** Keeps the last meaningful signal visible after the notification is dismissed. */
object SignalAttributionStore {
    private const val PREFS = "latest_signal_attribution_v320"
    private const val KEY_LATEST = "latest"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun record(
        context: Context,
        source: String,
        kind: String,
        reason: String,
        at: Long = System.currentTimeMillis(),
        executedTrade: Boolean = false
    ) {
        val cleanReason = reason.trim().ifBlank { "Причина не записана" }.take(1_500)
        val value = JSONObject()
            .put("source", source.trim().take(80))
            .put("kind", kind.trim().take(120))
            .put("reason", cleanReason)
            .put("at", at)
            .put("executedTrade", executedTrade)
        prefs(context).edit().putString(KEY_LATEST, value.toString()).apply()
    }

    @Synchronized
    fun latest(context: Context): SignalAttribution? {
        val raw = prefs(context).getString(KEY_LATEST, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            SignalAttribution(
                source = json.optString("source"),
                kind = json.optString("kind"),
                reason = json.optString("reason"),
                at = json.optLong("at"),
                executedTrade = json.optBoolean("executedTrade")
            )
        }.getOrNull()?.takeIf { it.source.isNotBlank() && it.kind.isNotBlank() }
    }
}
