package com.example.pumppaperbot

import android.content.Context
import java.util.concurrent.TimeUnit

/**
 * Activity visibility and cooldown shared with the existing foreground monitor.
 * No API keys are stored here. All timestamps are wall-clock milliseconds.
 */
internal object FusionRefreshState {
    const val FOREGROUND_INTERVAL_MS = 15_000L
    const val BACKGROUND_INTERVAL_MS = 60_000L
    private const val PREFS = "fusion_refresh_v1"
    private const val KEY_VISIBLE = "activity_visible"
    private const val KEY_TOUCH = "activity_heartbeat"
    private const val KEY_THROTTLE = "retry_not_before"
    private const val KEY_FAILURES = "consecutive_http_429"

    fun foreground(context: Context, visible: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_VISIBLE, visible)
            .putLong(KEY_TOUCH, if (visible) System.currentTimeMillis() else 0L)
            .apply()
    }

    fun heartbeat(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_TOUCH, System.currentTimeMillis()).apply()
    }

    fun isForeground(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val touch = p.getLong(KEY_TOUCH, 0L)
        return p.getBoolean(KEY_VISIBLE, false) && touch in 1L..now &&
            now - touch < 35_000L
    }

    fun canRequest(context: Context, now: Long = System.currentTimeMillis()) =
        now >= context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_THROTTLE, 0L)

    fun retryDelaySeconds(context: Context, now: Long = System.currentTimeMillis()): Long =
        ((context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_THROTTLE, 0L) - now).coerceAtLeast(0L) + 999L) / 1000L

    fun blocked(context: Context, code: Int, retryAfterSeconds: Long? = null) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val failures = (p.getInt(KEY_FAILURES, 0) + 1).coerceAtMost(8)
        val seconds = when (code) {
            429 -> maxOf(retryAfterSeconds ?: 0L, (60L * (1 shl failures)).coerceAtMost(900L))
            401, 403 -> TimeUnit.MINUTES.toSeconds(10)
            500, 502, 503, 504 -> 60L
            else -> 30L
        }
        p.edit()
            .putInt(KEY_FAILURES, if (code == 429) failures else 0)
            .putLong(KEY_THROTTLE, System.currentTimeMillis() + seconds * 1000L)
            .apply()
    }

    fun success(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_FAILURES, 0)
            .remove(KEY_THROTTLE)
            .apply()
    }
}
