package com.example.pumppaperbot

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class GeminiBudgetState(
    val usedToday: Int,
    val remainingToday: Int,
    val nextAllowedAt: Long,
    val dayResetsAt: Long,
    val rateLimitStrikes: Int
)

internal data class GeminiBudgetPermit(
    val allowed: Boolean,
    val state: GeminiBudgetState,
    val reason: String = ""
)

internal class GeminiRequestBlockedException(
    val nextAllowedAt: Long,
    message: String
) : IllegalStateException(message)

/**
 * One persisted request budget shared by the hourly and news Gemini circuits.
 * The day follows the quota reset timezone used by Gemini API projects.
 */
object GeminiRequestBudget {
    const val MAX_REQUESTS_PER_DAY = 20
    const val NORMAL_REQUESTS_PER_DAY = 10

    private const val PREFS = "gemini_request_budget_v37"
    private const val KEY_DAY = "pacific_day"
    private const val KEY_REQUESTS = "requests"
    private const val KEY_BACKOFF_UNTIL = "backoff_until"
    private const val KEY_RATE_LIMIT_STRIKES = "rate_limit_strikes"
    private val lock = Any()
    private val pacificTimeZone: TimeZone = TimeZone.getTimeZone("America/Los_Angeles")

    fun state(
        context: Context,
        now: Long = System.currentTimeMillis()
    ): GeminiBudgetState = synchronized(lock) {
        resetDayIfNeeded(context, now)
        stateLocked(context, now)
    }

    internal fun tryAcquire(
        context: Context,
        now: Long = System.currentTimeMillis()
    ): GeminiBudgetPermit = synchronized(lock) {
        resetDayIfNeeded(context, now)
        val current = stateLocked(context, now)
        val positionOpen = PumpBotEngine.snapshot(context).let {
            it.waitMode == "SELL" && it.entryPrice > 0.0
        }
        val activeLimit = activeLimit(positionOpen)
        if (current.usedToday >= activeLimit) {
            return@synchronized GeminiBudgetPermit(
                false,
                current,
                if (positionOpen) {
                    "Достигнут общий предел $MAX_REQUESTS_PER_DAY Gemini API-запросов за сутки"
                } else {
                    "Обычные 50% квоты Gemini использованы; ${MAX_REQUESTS_PER_DAY - NORMAL_REQUESTS_PER_DAY} запросов сохранены для открытой позиции"
                }
            )
        }
        if (now < current.nextAllowedAt) {
            return@synchronized GeminiBudgetPermit(
                false,
                current,
                "Gemini API на паузе после ограничения частоты"
            )
        }
        val prefs = prefs(context)
        prefs.edit().putInt(KEY_REQUESTS, current.usedToday + 1).commit()
        GeminiBudgetPermit(true, stateLocked(context, now))
    }

    internal fun requirePermit(
        context: Context,
        now: Long = System.currentTimeMillis()
    ): GeminiBudgetState {
        val permit = tryAcquire(context, now)
        if (!permit.allowed) {
            throw GeminiRequestBlockedException(
                permit.state.nextAllowedAt.takeIf { it > now } ?: permit.state.dayResetsAt,
                permit.reason
            )
        }
        return permit.state
    }

    internal fun recordRateLimit(
        context: Context,
        retryAfterSeconds: Long? = null,
        now: Long = System.currentTimeMillis()
    ): GeminiBudgetState = synchronized(lock) {
        resetDayIfNeeded(context, now)
        val prefs = prefs(context)
        val strikes = (prefs.getInt(KEY_RATE_LIMIT_STRIKES, 0) + 1).coerceAtMost(3)
        val fallbackDelay = when (strikes) {
            1 -> 5L * 60L * 1000L
            2 -> 15L * 60L * 1000L
            else -> 45L * 60L * 1000L
        }
        val serverDelay = retryAfterSeconds
            ?.coerceIn(1L, 6L * 60L * 60L)
            ?.times(1000L)
            ?: 0L
        val until = now + maxOf(fallbackDelay, serverDelay)
        prefs.edit()
            .putInt(KEY_RATE_LIMIT_STRIKES, strikes)
            .putLong(KEY_BACKOFF_UNTIL, until)
            .commit()
        stateLocked(context, now)
    }

    internal fun recordSuccess(context: Context) = synchronized(lock) {
        prefs(context).edit()
            .putInt(KEY_RATE_LIMIT_STRIKES, 0)
            .putLong(KEY_BACKOFF_UNTIL, 0L)
            .apply()
    }

    internal fun pacificDayKey(now: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = pacificTimeZone
        }.format(Date(now))

    internal fun nextPacificReset(now: Long): Long {
        val calendar = Calendar.getInstance(pacificTimeZone).apply {
            timeInMillis = now
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    internal fun activeLimit(positionOpen: Boolean): Int =
        if (positionOpen) MAX_REQUESTS_PER_DAY else NORMAL_REQUESTS_PER_DAY

    private fun stateLocked(context: Context, now: Long): GeminiBudgetState {
        val prefs = prefs(context)
        val used = prefs.getInt(KEY_REQUESTS, 0).coerceAtLeast(0)
        val positionOpen = PumpBotEngine.snapshot(context).let {
            it.waitMode == "SELL" && it.entryPrice > 0.0
        }
        return GeminiBudgetState(
            usedToday = used,
            remainingToday = (activeLimit(positionOpen) - used).coerceAtLeast(0),
            nextAllowedAt = prefs.getLong(KEY_BACKOFF_UNTIL, 0L),
            dayResetsAt = nextPacificReset(now),
            rateLimitStrikes = prefs.getInt(KEY_RATE_LIMIT_STRIKES, 0)
        )
    }

    private fun resetDayIfNeeded(context: Context, now: Long) {
        val prefs = prefs(context)
        val day = pacificDayKey(now)
        if (prefs.getString(KEY_DAY, "") == day) return
        prefs.edit()
            .putString(KEY_DAY, day)
            .putInt(KEY_REQUESTS, 0)
            .putLong(KEY_BACKOFF_UNTIL, 0L)
            .putInt(KEY_RATE_LIMIT_STRIKES, 0)
            .commit()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
