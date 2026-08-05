package com.example.pumppaperbot

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

object AlertSoundPreferences {
    private const val PREFS = "pump_alert_sound_v417"
    private const val KEY_URI = "uri"

    fun uri(context: Context): Uri = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY_URI, null)
        ?.let(Uri::parse)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ?: Uri.parse("content://settings/system/alarm_alert")

    fun title(context: Context): String = runCatching {
        RingtoneManager.getRingtone(context, uri(context))?.getTitle(context)
    }.getOrNull().orEmpty().ifBlank { "Стандартная тревога Android" }

    fun save(context: Context, uri: Uri) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_URI, uri.toString())
            .commit()
        PumpAlert.recreateSelectableChannels(context)
    }
}
