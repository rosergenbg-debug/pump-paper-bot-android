package com.example.pumppaperbot

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Human Factor is the only T32 branch that requires the owner to act before a paper BUY.
 * Its entry alarm is intentionally independent from ordinary preparatory-alert schedules.
 * While the setup stays pending this object repeats the audible/vibration attempt every minute,
 * even between the slower full trading cycles. Android DND or OS-level restrictions can still
 * override sound, so notification sound, direct alarm sound and vibration are all attempted.
 */
object HumanFactorAlarmV650 {
    private const val CHANNEL_ID = "pump_human_factor_v650"
    private const val NOTIFICATION_ID = 65032
    private const val NOTIFICATION_TIMEOUT_MILLIS = 150_000L
    private val handler = Handler(Looper.getMainLooper())

    @Volatile private var activeRingtone: Ringtone? = null
    @Volatile private var repeatContext: Context? = null
    @Volatile private var repeatDetail: String? = null

    private val repeatRunnable = object : Runnable {
        override fun run() {
            val context = repeatContext ?: return
            val detail = repeatDetail ?: return
            issue(context, detail)
            handler.postDelayed(this, HumanFactorAlertPolicyV650.REPEAT_MILLIS)
        }
    }

    fun ring(context: Context, detail: String) {
        val app = context.applicationContext
        repeatContext = app
        repeatDetail = detail
        handler.removeCallbacks(repeatRunnable)
        issue(app, detail)
        handler.postDelayed(repeatRunnable, HumanFactorAlertPolicyV650.REPEAT_MILLIS)
    }

    fun cancel(context: Context) {
        repeatContext = null
        repeatDetail = null
        handler.removeCallbacks(repeatRunnable)
        runCatching { NotificationManagerCompat.from(context.applicationContext).cancel(NOTIFICATION_ID) }
        runCatching { activeRingtone?.stop() }
        activeRingtone = null
    }

    @SuppressLint("MissingPermission")
    private fun issue(context: Context, detail: String) {
        ensureChannel(context)
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("ЧЕЛОВЕЧЕСКИЙ ФАКТОР • НУЖНО РЕШЕНИЕ")
            .setContentText(detail.take(220))
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail.take(800)))
            .setContentIntent(pending)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(false)
            .setTimeoutAfter(NOTIFICATION_TIMEOUT_MILLIS)
            .setVibrate(longArrayOf(0L, 500L, 180L, 500L, 180L, 900L))
            .build()

        // POST_NOTIFICATIONS is requested by the app on Android 13+. If the user denies it,
        // NotificationManagerCompat may be blocked by the OS; the direct vibration/ringtone
        // attempts below are intentionally kept as a second delivery path for this paper alert.
        runCatching { NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID) }
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
        vibrateStrong(context)
        playShortAlarm(context)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Human Factor — обязательный вход",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Повторяющийся сигнал, пока Human Factor ждёт решения ВОЙТИ/ОТКЛОНИТЬ"
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 500L, 180L, 500L, 180L, 900L)
                setSound(sound, audio)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
        )
    }

    @Suppress("DEPRECATION")
    private fun vibrateStrong(context: Context) {
        val pattern = longArrayOf(0L, 500L, 180L, 500L, 180L, 900L)
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                vibrator.vibrate(pattern, -1)
            }
        }
    }

    private fun playShortAlarm(context: Context) {
        runCatching { activeRingtone?.stop() }
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: return
        runCatching {
            val ringtone = RingtoneManager.getRingtone(context, uri) ?: return@runCatching
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            activeRingtone = ringtone
            ringtone.play()
            handler.postDelayed({
                if (activeRingtone === ringtone) {
                    runCatching { ringtone.stop() }
                    activeRingtone = null
                }
            }, 8_000L)
        }
    }
}
