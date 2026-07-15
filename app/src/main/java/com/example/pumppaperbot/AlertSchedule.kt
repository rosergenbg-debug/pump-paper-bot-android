package com.example.pumppaperbot

import android.content.Context
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class DelayedSignalState { NONE, POSSIBLE, MISSED }

object AlertSchedule {
    const val MODE_WORK = "WORK"
    const val MODE_ALWAYS = "ALWAYS"
    private const val prefsName = "PumpAlertScheduleV1"
    private const val keyMode = "mode"
    private const val keyStart = "start_minutes"
    private const val keyEnd = "end_minutes"
    private const val keyPendingTime = "pending_time"
    private const val keyPendingPrice = "pending_price"
    private const val keyPendingDirection = "pending_direction"
    private const val keyMessage = "message"
    private const val defaultStart = 6 * 60
    private const val defaultEnd = 23 * 60
    private val workDays = setOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY
    )

    private fun prefs(context: Context) = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun mode(context: Context): String = prefs(context).getString(keyMode, MODE_WORK) ?: MODE_WORK
    fun startMinutes(context: Context): Int = prefs(context).getInt(keyStart, defaultStart)
    fun endMinutes(context: Context): Int = prefs(context).getInt(keyEnd, defaultEnd)

    fun setMode(context: Context, value: String) {
        prefs(context).edit().putString(keyMode, if (value == MODE_ALWAYS) MODE_ALWAYS else MODE_WORK).apply()
    }

    fun setHours(context: Context, start: Int, end: Int) {
        prefs(context).edit()
            .putInt(keyStart, start.coerceIn(0, 1439))
            .putInt(keyEnd, end.coerceIn(0, 1439))
            .apply()
    }

    fun isAllowedNow(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        if (mode(context) == MODE_ALWAYS) return true
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        if (calendar.get(Calendar.DAY_OF_WEEK) !in workDays) return true
        val minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val start = startMinutes(context)
        val end = endMinutes(context)
        return if (start <= end) minutes in start until end else minutes >= start || minutes < end
    }

    fun rememberBlocked(context: Context, snapshot: LiveSnapshot) {
        val p = prefs(context)
        if (p.getLong(keyPendingTime, 0L) > 0L) return
        val direction = if (snapshot.waitMode == "SELL" || snapshot.readinessScore < 0) "SELL" else "BUY"
        val directionText = if (direction == "BUY") "на покупку" else "на продажу"
        p.edit()
            .putLong(keyPendingTime, System.currentTimeMillis())
            .putLong(keyPendingPrice, java.lang.Double.doubleToRawLongBits(snapshot.lastPrice))
            .putString(keyPendingDirection, direction)
            .putString(
                keyMessage,
                "Сигнал $directionText сохранён ночью в ${PumpBotEngine.formatTime(System.currentTimeMillis())}. Утром проверим, остаётся ли действие возможным."
            )
            .apply()
    }

    fun resolvePending(context: Context, snapshot: LiveSnapshot): DelayedSignalState {
        val p = prefs(context)
        val time = p.getLong(keyPendingTime, 0L)
        if (time <= 0L || !isAllowedNow(context)) return DelayedSignalState.NONE
        val direction = p.getString(keyPendingDirection, "BUY") ?: "BUY"
        val priceBits = p.getLong(keyPendingPrice, java.lang.Double.doubleToRawLongBits(0.0))
        val price = java.lang.Double.longBitsToDouble(priceBits)
        val age = System.currentTimeMillis() - time
        val change = if (price > 0.0 && snapshot.lastPrice > 0.0) snapshot.lastPrice / price - 1.0 else 0.0
        val possible = if (direction == "BUY") {
            age <= TimeUnit.HOURS.toMillis(3) && snapshot.waitMode == "BUY" && change in -0.02..0.015
        } else {
            snapshot.waitMode == "SELL"
        }
        return if (possible) {
            p.edit().putString(
                keyMessage,
                String.format(
                    Locale.US,
                    "Ночной сигнал: %s ещё возможен. Цена изменилась на %+.2f%%. Проверьте график перед действием.",
                    if (direction == "BUY") "вход" else "выход",
                    change * 100.0
                )
            ).apply()
            DelayedSignalState.POSSIBLE
        } else {
            p.edit()
                .putLong(keyPendingTime, 0L)
                .putString(
                    keyMessage,
                    String.format(
                        Locale.US,
                        "Ночной %s пропущен: прошло %.1f ч, цена изменилась на %+.2f%%. Движение не догоняем.",
                        if (direction == "BUY") "вход" else "выход",
                        age.toDouble() / TimeUnit.HOURS.toMillis(1),
                        change * 100.0
                    )
                )
                .apply()
            DelayedSignalState.MISSED
        }
    }

    fun hasDelayedPossible(context: Context): Boolean {
        return prefs(context).getLong(keyPendingTime, 0L) > 0L && isAllowedNow(context)
    }

    fun pendingDirection(context: Context): String = prefs(context).getString(keyPendingDirection, "BUY") ?: "BUY"
    fun pendingTime(context: Context): Long = prefs(context).getLong(keyPendingTime, 0L)

    fun markDelivered(context: Context) {
        val p = prefs(context)
        val direction = p.getString(keyPendingDirection, "BUY") ?: "BUY"
        p.edit()
            .putLong(keyPendingTime, 0L)
            .putString(keyMessage, "Отложенный ночной сигнал передан: ${if (direction == "BUY") "вход ещё был возможен" else "проверьте выход"}.")
            .apply()
    }

    fun alertKeySuffix(context: Context): String {
        val pending = pendingTime(context)
        return if (pending > 0L && isAllowedNow(context)) ":DELAYED:$pending" else ""
    }

    fun message(context: Context): String = prefs(context).getString(keyMessage, "Ночных пропущенных сигналов нет.")
        ?: "Ночных пропущенных сигналов нет."

    fun statusText(context: Context): String {
        val schedule = if (mode(context) == MODE_ALWAYS) {
            "Звонок: 24 часа"
        } else {
            "Звонок: Пн, Вт, Чт, Пт ${formatMinutes(startMinutes(context))}–${formatMinutes(endMinutes(context))}; Ср, Сб, Вс — 24 часа"
        }
        return "$schedule\n${message(context)}"
    }

    fun delayedNotificationText(context: Context): String = message(context)

    private fun formatMinutes(value: Int): String = String.format(Locale.GERMANY, "%02d:%02d", value / 60, value % 60)
}
