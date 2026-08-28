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
 * V6.6 owner alarm. Master switch and AlertSchedule are the single source of truth:
 * OFF = silence; WORK/DAILY = configured window; ALWAYS = 24/7.
 */
object HumanFactorAlarmV650 {
    private const val CHANNEL_ID = "pump_human_factor_v660"
    private const val NOTIFICATION_ID = 66032
    private const val NOTIFICATION_TIMEOUT_MILLIS = 150_000L
    private val handler = Handler(Looper.getMainLooper())

    @Volatile private var activeRingtone: Ringtone? = null
    @Volatile private var repeatContext: Context? = null
    @Volatile private var repeatDetail: String? = null

    private val repeatRunnable = object : Runnable {
        override fun run() {
            val context = repeatContext ?: return
            val detail = repeatDetail ?: return
            if (!allowed(context)) {
                cancel(context)
                return
            }
            issue(context, detail, ignoreSchedule = false)
            handler.postDelayed(this, HumanFactorAlertPolicyV650.REPEAT_MILLIS)
        }
    }

    fun ring(context: Context, detail: String) {
        val app = context.applicationContext
        if (!allowed(app)) {
            cancel(app)
            return
        }
        repeatContext = app
        repeatDetail = detail
        handler.removeCallbacks(repeatRunnable)
        issue(app, detail, ignoreSchedule = false)
        handler.postDelayed(repeatRunnable, HumanFactorAlertPolicyV650.REPEAT_MILLIS)
    }

    /** Manual settings test: bypasses the clock/day schedule, but never bypasses the master OFF switch. */
    fun testOnce(context: Context): Boolean {
        val app = context.applicationContext
        if (!ResearchModePolicy.alertsEnabled(app)) return false
        issue(app, "ТЕСТ V6.6 HUMAN • если слышите звонок и вибрацию, канал ручного входа работает.", ignoreSchedule = true)
        return true
    }

    fun cancel(context: Context) {
        repeatContext = null
        repeatDetail = null
        handler.removeCallbacks(repeatRunnable)
        runCatching { NotificationManagerCompat.from(context.applicationContext).cancel(NOTIFICATION_ID) }
        runCatching { activeRingtone?.stop() }
        activeRingtone = null
    }

    private fun allowed(context: Context): Boolean =
        ResearchModePolicy.alertsEnabled(context) && AlertSchedule.isAllowedNow(context)

    @SuppressLint("MissingPermission")
    private fun issue(context: Context, detail: String, ignoreSchedule: Boolean) {
        if (!ResearchModePolicy.alertsEnabled(context)) return
        if (!ignoreSchedule && !AlertSchedule.isAllowedNow(context)) return
        ensureChannel(context)
        val open = Intent(context, V660DashboardActivity::class.java).apply {
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
            .setContentTitle("PUMP V6.6 • НУЖНО РЕШЕНИЕ")
            .setContentText(detail.take(220))
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail.take(900)))
            .setContentIntent(pending)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(!ignoreSchedule)
            .setAutoCancel(ignoreSchedule)
            .setOnlyAlertOnce(false)
            .setTimeoutAfter(NOTIFICATION_TIMEOUT_MILLIS)
            .setVibrate(longArrayOf(0L, 500L, 180L, 500L, 180L, 900L))
            .build()

        runCatching { NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID) }
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
        vibrateStrong(context, ignoreSchedule)
        playShortAlarm(context, ignoreSchedule)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val sound = AlertSoundPreferences.uri(context)
        val audio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "PUMP V6.6 — ручной вход",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Повторяющийся сигнал V6.6 по общей кнопке звонков и расписанию"
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 500L, 180L, 500L, 180L, 900L)
                setSound(sound, audio)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
        )
    }

    @Suppress("DEPRECATION")
    private fun vibrateStrong(context: Context, ignoreSchedule: Boolean) {
        if (!ResearchModePolicy.alertsEnabled(context)) return
        if (!ignoreSchedule && !AlertSchedule.isAllowedNow(context)) return
        val pattern = longArrayOf(0L, 500L, 180L, 500L, 180L, 900L)
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            else vibrator.vibrate(pattern, -1)
        }
    }

    private fun playShortAlarm(context: Context, ignoreSchedule: Boolean) {
        if (!ResearchModePolicy.alertsEnabled(context)) return
        if (!ignoreSchedule && !AlertSchedule.isAllowedNow(context)) return
        runCatching { activeRingtone?.stop() }
        val uri = AlertSoundPreferences.uri(context)
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
