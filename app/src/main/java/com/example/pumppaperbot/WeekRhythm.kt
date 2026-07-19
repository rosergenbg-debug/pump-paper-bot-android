package com.example.pumppaperbot

import java.util.Calendar
import java.util.TimeZone

data class WeekRhythmContext(
    val code: String,
    val title: String,
    val explanation: String,
    val caution: Boolean
)

/**
 * Calendar context derived from the historical PUMP/EUR study.  It never
 * creates or cancels a trade by itself because the direction effect was not
 * sufficiently stable out of sample.
 */
object WeekRhythm {
    private val berlin = TimeZone.getTimeZone("Europe/Berlin")

    fun at(time: Long): WeekRhythmContext {
        if (time <= 0L) return neutral()
        val calendar = Calendar.getInstance(berlin).apply { timeInMillis = time }
        val day = calendar.get(Calendar.DAY_OF_WEEK)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return when {
            day == Calendar.SATURDAY || day == Calendar.SUNDAY -> WeekRhythmContext(
                "WEEKEND_QUIET",
                "ВЫХОДНЫЕ: РЫНОК ОБЫЧНО ТИШЕ",
                "Исторически объём PUMP был примерно на 34% ниже. Календарь не запрещает вход, но нужен подтверждённый объём.",
                true
            )
            day == Calendar.MONDAY && hour in 6..11 -> WeekRhythmContext(
                "MONDAY_EXHALE",
                "ПОНЕДЕЛЬНИК 06–12: ВОЗМОЖЕН УТРЕННИЙ ВЫДОХ",
                "Утренний откат встречался чаще обычного. Не догоняем рост и ждём закрытую свечу разворота.",
                true
            )
            day == Calendar.THURSDAY -> WeekRhythmContext(
                "THURSDAY_WEAK",
                "ЧЕТВЕРГ: ИСТОРИЧЕСКИ БОЛЕЕ СЛАБЫЙ ДЕНЬ",
                "Это только дополнительная осторожность, а не самостоятельный запрет покупки.",
                true
            )
            else -> neutral()
        }
    }

    private fun neutral() = WeekRhythmContext(
        "NEUTRAL",
        "НЕДЕЛЬНЫЙ РИТМ: НЕЙТРАЛЬНО",
        "Решение принимается по цене, объёму, потоку и состоянию BTC/SOL, а не по календарю.",
        false
    )
}
