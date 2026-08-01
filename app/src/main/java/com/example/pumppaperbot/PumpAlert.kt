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
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object PumpAlert {
    private const val monitorChannelId = "pump_rsi_risk_monitor"
    private const val signalChannelId = "pump_rsi_risk_signals"
    private const val rapidDropChannelId = "pump_rapid_drop_v26"
    private const val eventRadarChannelId = "pump_event_radar_v3"
    private const val appTradeChannelId = "pump_app_trades_v319"
    private const val geminiTradeChannelId = "pump_gemini_trades_v319"
    private const val geminiExitExperimentChannelId = "pump_gemini_exit_experiment_v319"
    private const val monitorNotificationId = 3501
    private const val signalNotificationId = 3502
    private const val rapidDropNotificationId = 3503
    private const val eventRadarNotificationId = 3504
    private const val geminiBuyNotificationId = 3505
    private const val appBuyNotificationId = 3506
    private const val appSellNotificationId = 3507
    private const val geminiSellNotificationId = 3508
    private const val geminiExperimentBuyNotificationId = 3509
    private const val geminiExperimentSellNotificationId = 3510
    private val rapidDropVibration = longArrayOf(0, 1000, 180, 1000, 180, 1600)

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
        val rapidDrop = NotificationChannel(
            rapidDropChannelId,
            "PUMP аварийное падение 25%+",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Отдельная тревога при падении PUMP/EUR на 25% и больше от максимума последних 24 часов"
            enableVibration(true)
            vibrationPattern = rapidDropVibration
            setSound(sound, attrs)
        }
        val eventRadar = NotificationChannel(
            eventRadarChannelId,
            "PUMP V3 важные внешние события",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Предупреждение о важном официальном событии; это не самостоятельная команда купить или продать"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 180, 500)
            setSound(sound, attrs)
        }
        val appTrades = NotificationChannel(
            appTradeChannelId,
            "APP: исполненные покупки и продажи",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Отдельный громкий звонок после каждой виртуальной сделки стратегии APP"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 700, 250, 700, 250, 1100)
            setSound(sound, attrs)
        }
        val geminiTrades = NotificationChannel(
            geminiTradeChannelId,
            "Gemini: исполненные покупки и продажи",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Отдельный громкий звонок после каждой виртуальной сделки Gemini"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 700, 250, 700, 250, 1100)
            setSound(sound, attrs)
        }
        val geminiExitExperiment = NotificationChannel(
            geminiExitExperimentChannelId,
            "Gemini‑эксперимент: входы и ранние выходы",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Отдельный громкий звонок Gemini‑эксперимента с ранним входом и рыночным выходом"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 180, 500, 180, 500, 180, 1100)
            setSound(sound, attrs)
        }
        manager.createNotificationChannel(monitor)
        manager.createNotificationChannel(signal)
        manager.createNotificationChannel(rapidDrop)
        manager.createNotificationChannel(eventRadar)
        manager.createNotificationChannel(appTrades)
        manager.createNotificationChannel(geminiTrades)
        manager.createNotificationChannel(geminiExitExperiment)
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
            score == -99 -> "СРОЧНО ПРОВЕРЬТЕ ВЫХОД ИЗ PUMP/EUR"
            snapshot.signalAction == "BUY" -> "PUMP/EUR: +100 — ПОКУПАТЬ"
            snapshot.signalAction == StrategyV2.ACTION_SELL_HALF && snapshot.strategyMode == StrategyV2.MODE_EXHAUSTION -> "PUMP/EUR: −100 — ПРОДАТЬ 40%"
            snapshot.signalAction == StrategyV2.ACTION_SELL_HALF -> "PUMP/EUR: −100 — ПРОДАТЬ 50%"
            else -> "СРОЧНО ВЫЙТИ ИЗ PUMP/EUR"
        }
        val preparation = if (delayed) {
            "${AlertSchedule.delayedNotificationText(context)} "
        } else if (kotlin.math.abs(score) == 99) {
            "Приготовьтесь и ждите 100. Это готовность условий, не вероятность прибыли. "
        } else ""
        val text = "$preparation${snapshot.signalReason}. Дыхание: ${snapshot.breathingState}; " +
            "поток ${if (snapshot.directionScore >= 0) "+" else ""}${snapshot.directionScore}/100; " +
            "поздний вход ${snapshot.lateEntryRisk}/100. Цена €${formatPrice(snapshot.lastPrice)}"
        SignalAttributionStore.record(
            context = context,
            source = "APP",
            kind = when {
                kotlin.math.abs(score) == 99 -> "ГОТОВНОСТЬ ${kotlin.math.abs(score)}/100"
                snapshot.signalAction == "BUY" -> "СИГНАЛ ВХОДА"
                else -> "СИГНАЛ ВЫХОДА"
            },
            reason = "$preparation${snapshot.signalReason}",
            at = snapshot.lastSync,
            executedTrade = false
        )
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

    fun showRapidDrop(context: Context, snapshot: LiveSnapshot) {
        ensureChannels(context)
        val drop = snapshot.rapidDrop
        if (!drop.active) return
        val title = String.format(
            java.util.Locale.GERMANY,
            "PUMP/EUR: РЕЗКОЕ ПАДЕНИЕ −%.1f%%",
            drop.dropPercent
        )
        val action = when {
            snapshot.waitMode == "SELL" -> "ОТКРЫТА ПОЗИЦИЯ: срочно проверьте цену, стоп и возможность выхода."
            drop.recoveryConfirmed -> String.format(
                java.util.Locale.GERMANY,
                "Есть отскок +%.1f%% от минимума, но покупка разрешена только после обычного подтверждения 99/100.",
                drop.reboundPercent
            )
            else -> "Падение ещё не остановлено. Не покупать автоматически; ждём разворот, покупателей и закрытую свечу."
        }
        val text = String.format(
            java.util.Locale.GERMANY,
            "%s Максимум €%.8f, сейчас €%.8f, движение заняло около %d мин.",
            action,
            drop.peakPrice,
            drop.currentPrice,
            drop.windowMinutes
        )
        val notification = NotificationCompat.Builder(context, rapidDropChannelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(0xFFDA3633.toInt())
            .setVibrate(rapidDropVibration)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(rapidDropNotificationId, notification)
        vibrate(context, rapidDropVibration)
    }

    fun showEventRadar(context: Context, state: EventRadarState, snapshot: LiveSnapshot) {
        ensureChannels(context)
        val event = state.alertCandidate ?: return
        val direction = when {
            event.directionScore >= 20 -> "ВОЗМОЖНОЕ ДАВЛЕНИЕ ВВЕРХ"
            event.directionScore <= -20 -> "ВОЗМОЖНОЕ ДАВЛЕНИЕ ВНИЗ"
            else -> "НАПРАВЛЕНИЕ НЕЯСНО"
        }
        val title = "V3 ${event.source}: $direction"
        val text = "Важность ${event.importance}/100. ${event.title}. " +
            "${state.confirmation(snapshot.directionScore, snapshot.breathingConfidence, event)}. " +
            "Это предупреждение для проверки, не приказ купить или продать."
        val notification = NotificationCompat.Builder(context, eventRadarChannelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setColor(0xFF7C3AED.toInt())
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(eventRadarNotificationId, notification)
        vibrate(context, longArrayOf(0, 500, 180, 500))
    }

    fun showAppTrade(context: Context, trade: AppPaperTrade) {
        ensureChannels(context)
        val buy = trade.action == "BUY"
        val title = when (trade.action) {
            "BUY" -> "APP: ВХОД В PUMP/EUR"
            StrategyV2.ACTION_SELL_HALF -> "APP: ЧАСТИЧНЫЙ ВЫХОД ИЗ PUMP/EUR"
            else -> "APP: ВЫХОД ИЗ PUMP/EUR"
        }
        val text = if (buy) {
            String.format(
                java.util.Locale.GERMANY,
                "APP вложил виртуальные €%.2f по цене €%.8f. Комиссия €%.2f. %s",
                trade.amount * trade.price + trade.fee,
                trade.price,
                trade.fee,
                trade.reason
            )
        } else {
            String.format(
                java.util.Locale.GERMANY,
                "APP продал по цене €%.8f. Результат сделки %+.2f €. Комиссия €%.2f. %s",
                trade.price,
                trade.pnlEur,
                trade.fee,
                trade.reason
            )
        }
        SignalAttributionStore.record(
            context,
            "APP",
            if (buy) "ВХОД" else "ВЫХОД",
            trade.reason,
            trade.time,
            executedTrade = true
        )
        showTradeNotification(
            context,
            appTradeChannelId,
            if (buy) appBuyNotificationId else appSellNotificationId,
            title,
            text,
            if (buy) 0xFF238636.toInt() else 0xFFDA3633.toInt()
        )
    }

    fun showGeminiTrade(context: Context, trade: GeminiPaperTrade) {
        ensureChannels(context)
        val buy = trade.action == "BUY"
        val text = if (buy) {
            String.format(
                java.util.Locale.GERMANY,
                "Gemini вложила виртуальные €%.2f по цене €%.8f. Комиссия €%.2f. " +
                    "Оценка %+d/100, уверенность %d/100. %s",
                trade.amount * trade.price + trade.fee,
                trade.price,
                trade.fee,
                trade.score,
                trade.confidence,
                trade.reason
            )
        } else {
            String.format(
                java.util.Locale.GERMANY,
                "Gemini продала всю виртуальную позицию по цене €%.8f. " +
                    "Результат %+.2f €, комиссия €%.2f. %s",
                trade.price,
                trade.pnlEur,
                trade.fee,
                trade.reason
            )
        }
        SignalAttributionStore.record(
            context,
            "GEMINI",
            if (buy) "ВХОД" else "ВЫХОД",
            trade.reason,
            trade.time,
            executedTrade = true
        )
        showTradeNotification(
            context,
            geminiTradeChannelId,
            if (buy) geminiBuyNotificationId else geminiSellNotificationId,
            if (buy) "GEMINI: ВХОД В PUMP/EUR" else "GEMINI: ВЫХОД ИЗ PUMP/EUR",
            text,
            if (buy) 0xFF7C3AED.toInt() else 0xFFDA3633.toInt()
        )
    }

    fun showGeminiExitExperimentTrade(context: Context, trade: GeminiPaperTrade) {
        ensureChannels(context)
        val buy = trade.action == "BUY"
        val text = if (buy) {
            String.format(
                java.util.Locale.GERMANY,
                "Эксперимент вошёл по цене €%.8f и вложил виртуальные €%.2f. %s",
                trade.price,
                trade.amount * trade.price + trade.fee,
                trade.reason
            )
        } else {
            String.format(
                java.util.Locale.GERMANY,
                "Эксперимент вышел по цене €%.8f. Результат %+.2f €. Причина: %s",
                trade.price,
                trade.pnlEur,
                trade.reason
            )
        }
        SignalAttributionStore.record(
            context,
            "GEMINI‑ЭКСПЕРИМЕНТ",
            if (buy) "ВХОД" else "ВЫХОД",
            trade.reason,
            trade.time,
            executedTrade = true
        )
        showTradeNotification(
            context,
            geminiExitExperimentChannelId,
            if (buy) geminiExperimentBuyNotificationId else geminiExperimentSellNotificationId,
            if (buy) "GEMINI‑ЭКСПЕРИМЕНТ: ВХОД" else "GEMINI‑ЭКСПЕРИМЕНТ: ВЫХОД",
            text,
            if (buy) 0xFFD29922.toInt() else 0xFFFF7B72.toInt()
        )
    }

    private fun showTradeNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        text: String,
        color: Int
    ) {
        requireTradeNotificationsAvailable(context)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(color)
            .setVibrate(longArrayOf(0, 700, 250, 700, 250, 1100))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(notificationId, notification)
        vibrate(context)
    }

    private fun requireTradeNotificationsAvailable(context: Context) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            error("уведомления приложения отключены в Android")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            error("Android не дал разрешение на уведомления")
        }
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

    private fun vibrate(
        context: Context,
        pattern: LongArray = longArrayOf(0, 700, 250, 700, 250, 1100)
    ) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    private fun formatPrice(value: Double): String {
        return if (value > 0.0) String.format(java.util.Locale.US, "%.8f", value) else "-"
    }
}
