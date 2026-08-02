package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class EntryAlertReminder(
    val source: String,
    val signalId: String,
    val signalAt: Long,
    val initialPrice: Double,
    val lastAlertAt: Long,
    val repeats: Int
) {
    fun toJson(): JSONObject = JSONObject()
        .put("source", source)
        .put("signalId", signalId)
        .put("signalAt", signalAt)
        .put("initialPrice", initialPrice)
        .put("lastAlertAt", lastAlertAt)
        .put("repeats", repeats)

    companion object {
        fun fromJson(json: JSONObject) = EntryAlertReminder(
            source = json.optString("source"),
            signalId = json.optString("signalId"),
            signalAt = json.optLong("signalAt"),
            initialPrice = json.optDouble("initialPrice"),
            lastAlertAt = json.optLong("lastAlertAt"),
            repeats = json.optInt("repeats").coerceAtLeast(0)
        )
    }
}

internal object EntryAlertReminderPolicy {
    const val REPEAT_INTERVAL_MILLIS = 6L * 60L * 1000L
    const val MAX_SIGNAL_AGE_MILLIS = 20L * 60L * 1000L
    const val MAX_REPEATS = 2
    const val MAX_CHASE_PERCENT = 1.5

    fun shouldKeep(reminder: EntryAlertReminder, currentPrice: Double, now: Long): Boolean {
        if (reminder.signalAt <= 0L || now < reminder.signalAt) return false
        if (now - reminder.signalAt > MAX_SIGNAL_AGE_MILLIS) return false
        if (reminder.repeats >= MAX_REPEATS) return false
        if (reminder.initialPrice > 0.0 && currentPrice > 0.0 &&
            (currentPrice / reminder.initialPrice - 1.0) * 100.0 > MAX_CHASE_PERCENT
        ) return false
        return true
    }

    fun isDue(reminder: EntryAlertReminder, now: Long): Boolean =
        now >= reminder.lastAlertAt && now - reminder.lastAlertAt >= REPEAT_INTERVAL_MILLIS
}

object EntryAlertReminderStore {
    private const val PREFS = "entry_alert_reminders_v49"
    private const val KEY_REMINDERS = "reminders"

    @Synchronized
    fun arm(
        context: Context,
        source: String,
        signalId: String,
        signalAt: Long,
        initialPrice: Double,
        alertedAt: Long = System.currentTimeMillis()
    ) {
        if (PumpBotEngine.snapshot(context).waitMode == "SELL") return
        val current = read(context).toMutableList()
        val previous = current.indexOfFirst { it.source == source }
        val reminder = EntryAlertReminder(
            source = source,
            signalId = signalId,
            signalAt = signalAt,
            initialPrice = initialPrice,
            lastAlertAt = alertedAt,
            repeats = 0
        )
        if (previous >= 0 && current[previous].signalId == signalId) return
        if (previous >= 0) current[previous] = reminder else current += reminder
        save(context, current)
    }

    @Synchronized
    fun flush(context: Context, now: Long = System.currentTimeMillis()) {
        val snapshot = PumpBotEngine.snapshot(context)
        if (snapshot.waitMode == "SELL") {
            clear(context)
            return
        }
        val price = PaperExecutionPolicy.displayPrice(snapshot, now)
        val kept = ArrayList<EntryAlertReminder>()
        read(context).forEach { reminder ->
            if (!EntryAlertReminderPolicy.shouldKeep(reminder, price, now)) return@forEach
            if (!EntryAlertReminderPolicy.isDue(reminder, now)) {
                kept += reminder
                return@forEach
            }
            val delivered = runCatching {
                PumpAlert.showEntryReminder(context, reminder, price)
            }.isSuccess
            kept += if (delivered) {
                reminder.copy(lastAlertAt = now, repeats = reminder.repeats + 1)
            } else {
                reminder
            }
        }
        save(context, kept.filter {
            EntryAlertReminderPolicy.shouldKeep(it, price, now)
        })
    }

    @Synchronized
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun read(context: Context): List<EntryAlertReminder> = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_REMINDERS, "[]").orEmpty()
        val json = JSONArray(raw)
        (0 until json.length()).mapNotNull { json.optJSONObject(it)?.let(EntryAlertReminder::fromJson) }
            .filter { it.source.isNotBlank() && it.signalId.isNotBlank() }
    }.getOrDefault(emptyList())

    private fun save(context: Context, reminders: List<EntryAlertReminder>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_REMINDERS, JSONArray(reminders.map { it.toJson() }).toString())
            .commit()
    }
}
