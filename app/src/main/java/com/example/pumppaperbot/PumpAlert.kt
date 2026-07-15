package com.example.pumppaperbot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

object PumpAlert {
    private const val monitorChannelId = "pump_rsi_risk_monitor"
    private const val signalChannelId = "pump_rsi_risk_signals"
    private const val monitorNotificationId = 3501
    private const val signalNotificationId = 3502

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val monitor = NotificationChannel(
            monitorChannelId,
            "PUMP RSI монитор",
            NotificationManager.IMPORTANCE_LOW
        )
        val signal = NotificationChannel(
            signalChannelId,
            "PUMP RSI сигналы покупки и продажи",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 700, 250, 700, 250, 1100)
            setSound(sound, attrs)
        }
        manager.createNotificationChannel(monitor)
        manager.createNotificationChannel(signal)
    }

    fun monitorNotification(context: Context, text: String) =
        NotificationCompat.Builder(context, monitorChannelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("PUMP RSI монитор работает")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(openAppIntent(context))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    fun showSignal(context: Context, snapshot: LiveSnapshot) {
        ensureChannels(context)
        val score = snapshot.readinessScore
        val delayed = AlertSchedule.hasDelayedPossible(context)
        val title = when {
            delayed && AlertSchedule.pendingDirection(context) == "BUY" -> "PUMP/EUR: НОЧНОЙ СИГНАЛ — ВХОД ЕЩЁ ВОЗМОЖЕН"
            delayed -> "PUMP/EUR: НОЧНОЙ СИГНАЛ — ПРОВЕРЬТЕ ВЫХОД"
            score == 99 -> "PUMP/EUR: ГОТОВНОСТЬ К ПОКУПКЕ 99/100"
            score == -99 -> "PUMP/EUR: ГОТОВНОСТЬ К ПРОДАЖЕ 99/100"
            snapshot.signalAction == "BUY" -> "PUMP/EUR: +100 — ПОКУПАТЬ"
            snapshot.signalAction == StrategyV2.ACTION_SELL_HALF -> "PUMP/EUR: −100 — ПРОДАТЬ 50%"
            else -> "PUMP/EUR: −100 — ПРОДАВАТЬ"
        }
        val preparation = if (delayed) {
            "${AlertSchedule.delayedNotificationText(context)} "
        } else if (kotlin.math.abs(score) == 99) {
            "Приготовьтесь и ждите 100. Это готовность условий, не вероятность прибыли. "
        } else ""
        val text = "$preparation${snapshot.signalReason}. Цена €${formatPrice(snapshot.lastPrice)}"
        val notification = NotificationCompat.Builder(context, signalChannelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 700, 250, 700, 250, 1100))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .build()
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(signalNotificationId, notification)
        vibrate(context)
    }

    fun monitorId(): Int = monitorNotificationId

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        return PendingIntent.getActivity(
            context,
            35,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 250, 700, 250, 1100), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 700, 250, 700, 250, 1100), -1)
        }
    }

    private fun formatPrice(value: Double): String {
        return if (value > 0.0) String.format(java.util.Locale.US, "%.8f", value) else "-"
    }
}
