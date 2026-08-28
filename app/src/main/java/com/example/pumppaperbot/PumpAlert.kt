package com.example.pumppaperbot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object PumpAlert {
    private data class SoundTestConfig(
        val channelId: String,
        val notificationId: Int,
        val title: String,
        val color: Int
    )

    private const val monitorChannelId = "pump_rsi_risk_monitor"
    private const val signalChannelId = "pump_rsi_risk_signals_v50"
    private const val rapidDropChannelId = "pump_rapid_drop_v50"
    private const val eventRadarChannelId = "pump_event_radar_v50"
    private const val appTradeChannelId = "pump_app_trades_v50"
    private const val geminiTradeChannelId = "pump_deepseek_trades_v50"
    private const val geminiExitExperimentChannelId = "pump_deepseek_experiment_v50"
    private const val positionSupervisorChannelId = "pump_position_supervisor_v50"
    private const val silentAlertChannelId = "pump_silent_updates_v50"
    private const val deepSeekCostChannelId = "pump_deepseek_cost_v414"
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
    private const val positionSupervisorNotificationId = 3511
    private const val personalGuardNotificationId = 3512
    private const val entryReminderAppId = 3514
    private const val entryReminderDeepSeekId = 3515
    private const val entryReminderExperimentId = 3516
    private const val geminiPositionAdvisorNotificationId = 3517
    private const val deepSeekActionLevelNotificationId = 3518
    private const val deepSeekCostNotificationId = 3519
    private const val appSoundTestNotificationId = 3520
    private const val deepSeekSoundTestNotificationId = 3521
    private const val experimentSoundTestNotificationId = 3522
    private const val sergeSoundTestNotificationId = 3523
    private const val fastPositionWarningNotificationId = 3524
    private val rapidDropVibration = longArrayOf(0, 1000, 180, 1000, 180, 1600)

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val sound = AlertSoundPreferences.uri(context)
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
            "DeepSeek: исполненные покупки и продажи",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Отдельный громкий звонок после каждой виртуальной сделки DeepSeek"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 700, 250, 700, 250, 1100)
            setSound(sound, attrs)
        }
        val geminiExitExperiment = NotificationChannel(
            geminiExitExperimentChannelId,
            "DeepSeek‑эксперимент: входы и ранние выходы",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Отдельный громкий звонок DeepSeek‑эксперимента с ранним входом и рыночным выходом"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 180, 500, 180, 500, 180, 1100)
            setSound(sound, attrs)
        }
        val positionSupervisor = NotificationChannel(
            positionSupervisorChannelId,
            "Серж: сопровождение открытой позиции",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Выход, ухудшение, улучшение и отмена выхода по открытой позиции Сержа"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 900, 180, 900, 180, 1300)
            setSound(sound, attrs)
        }
        val deepSeekCost = NotificationChannel(
            deepSeekCostChannelId,
            "Расходы DeepSeek",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Одно информационное предупреждение, когда оценка расходов DeepSeek за UTC-сутки превышает примерно 5 евро"
            enableVibration(true)
        }
        val silentAlerts = NotificationChannel(
            silentAlertChannelId,
            "PUMP сообщения без звонка",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Подготовительные сообщения и виртуальные сделки вне выбранного звукового времени"
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(monitor)
        manager.createNotificationChannel(signal)
        manager.createNotificationChannel(rapidDrop)
        manager.createNotificationChannel(eventRadar)
        manager.createNotificationChannel(appTrades)
        manager.createNotificationChannel(geminiTrades)
        manager.createNotificationChannel(geminiExitExperiment)
        manager.createNotificationChannel(positionSupervisor)
        manager.createNotificationChannel(deepSeekCost)
        manager.createNotificationChannel(silentAlerts)
    }

    fun recreateSelectableChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        listOf(
            signalChannelId, rapidDropChannelId, eventRadarChannelId, appTradeChannelId, geminiTradeChannelId,
            geminiExitExperimentChannelId, positionSupervisorChannelId
        ).forEach(manager::deleteNotificationChannel)
        ensureChannels(context)
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
            "поздний вход ${snapshot.lateEntryRisk}/100. Цена €${formatPrice(PaperExecutionPolicy.displayPrice(snapshot))}"
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
        if (!ResearchModePolicy.userAlertsAllowed(context)) return
        requireTradeNotificationsAvailable(context)
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
            .setSound(AlertSoundPreferences.uri(context))
            .build()
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(signalNotificationId, notification)
        vibrate(context)
        if (snapshot.waitMode == "BUY" && snapshot.readinessScore >= 100) {
            EntryAlertReminderStore.arm(
                context,
                source = "APP",
                signalId = "APP:${snapshot.lastCandle}:${snapshot.strategyMode}",
                signalAt = snapshot.lastSync,
                initialPrice = PaperExecutionPolicy.displayPrice(snapshot)
            )
        }
    }

    fun showRapidDrop(context: Context, snapshot: LiveSnapshot) {
        ensureChannels(context)
        if (!ResearchModePolicy.userAlertsAllowed(context)) return
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
        val urgentPersonalExit = snapshot.waitMode == "SELL"
        val loud = AlertDeliveryPolicy.shouldRing(
            preparatoryAllowed = AlertSchedule.isAllowedNow(context),
            executedTradeAllowed = AlertSchedule.isExecutedTradeAllowedNow(context),
            executedTrade = false,
            urgentPersonalExit = urgentPersonalExit
        )
        val notification = NotificationCompat.Builder(
            context,
            if (loud) rapidDropChannelId else silentAlertChannelId
        )
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(if (loud) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_STATUS)
            .setColor(0xFFDA3633.toInt())
            .setVibrate(if (loud) rapidDropVibration else longArrayOf(0))
            .setSound(if (loud) AlertSoundPreferences.uri(context) else null)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(rapidDropNotificationId, notification)
        if (loud) vibrate(context, rapidDropVibration)
    }

    fun showEventRadar(context: Context, state: EventRadarState, snapshot: LiveSnapshot) {
        ensureChannels(context)
        if (!ResearchModePolicy.userAlertsAllowed(context)) return
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
            "BUY" -> "APP ВОШЁЛ В PUMP/EUR"
            StrategyV2.ACTION_SELL_HALF -> "APP ЧАСТИЧНО ВЫШЕЛ ИЗ PUMP/EUR"
            else -> "APP ВЫШЕЛ ИЗ PUMP/EUR"
        }
        val text = if (buy) {
            String.format(
                java.util.Locale.GERMANY,
                "APP вложил виртуальные €%.2f по цене €%.8f. Комиссия €%.2f. Причина входа: %s",
                trade.amount * trade.price + trade.fee,
                trade.price,
                trade.fee,
                trade.reason
            )
        } else {
            String.format(
                java.util.Locale.GERMANY,
                "APP продал по цене €%.8f. Результат сделки %+.2f €. Комиссия €%.2f. Причина выхода: %s",
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
        if (!ResearchModePolicy.userAlertsAllowed(context)) return
        if (!VirtualTradeAlertPolicy.shouldNotify(
                trade.action,
                PumpBotEngine.snapshot(context).waitMode == "SELL"
            )) return
        showTradeNotification(
            context,
            appTradeChannelId,
            if (buy) appBuyNotificationId else appSellNotificationId,
            title,
            text,
            if (buy) 0xFF238636.toInt() else 0xFFDA3633.toInt(),
            executedTrade = true
        )
        if (buy) {
            EntryAlertReminderStore.arm(
                context, "APP", "APP_TRADE:${trade.time}:${trade.candleTime}",
                trade.time, trade.price
            )
        }
    }

    fun showGeminiTrade(context: Context, trade: GeminiPaperTrade) {
        ensureChannels(context)
        val buy = trade.action == "BUY"
        val text = if (buy) {
            String.format(
                java.util.Locale.GERMANY,
                "DeepSeek вложил виртуальные €%.2f по цене €%.8f. Комиссия €%.2f. " +
                    "Оценка %+d/100, уверенность %d/100. Причина входа: %s",
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
                "DeepSeek продал всю виртуальную позицию по цене €%.8f. " +
                    "Результат %+.2f €, комиссия €%.2f. Причина выхода: %s",
                trade.price,
                trade.pnlEur,
                trade.fee,
                trade.reason
            )
        }
        SignalAttributionStore.record(
            context,
            "DEEPSEEK",
            if (buy) "ВХОД" else "ВЫХОД",
            trade.reason,
            trade.time,
            executedTrade = true
        )
        if (!ResearchModePolicy.userAlertsAllowed(context)) return
        if (!VirtualTradeAlertPolicy.shouldNotify(
                trade.action,
                PumpBotEngine.snapshot(context).waitMode == "SELL"
            )) return
        showTradeNotification(
            context,
            geminiTradeChannelId,
            if (buy) geminiBuyNotificationId else geminiSellNotificationId,
            if (buy) "DEEPSEEK ВОШЁЛ В PUMP/EUR" else "DEEPSEEK ВЫШЕЛ ИЗ PUMP/EUR",
            text,
            if (buy) 0xFF7C3AED.toInt() else 0xFFDA3633.toInt(),
            executedTrade = true
        )
        if (buy) {
            EntryAlertReminderStore.arm(
                context, "DEEPSEEK", "DEEPSEEK_TRADE:${trade.decisionId}",
                trade.time, trade.price
            )
        }
    }

    fun showGeminiExitExperimentTrade(context: Context, trade: GeminiPaperTrade) {
        ensureChannels(context)
        val buy = trade.action == "BUY"
        val text = if (buy) {
            String.format(
                java.util.Locale.GERMANY,
                "Эксперимент вошёл по цене €%.8f и вложил виртуальные €%.2f. Причина входа: %s",
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
            "DEEPSEEK‑ЭКСПЕРИМЕНТ",
            if (buy) "ВХОД" else "ВЫХОД",
            trade.reason,
            trade.time,
            executedTrade = true
        )
        if (!ResearchModePolicy.userAlertsAllowed(context)) return
        if (!VirtualTradeAlertPolicy.shouldNotify(
                trade.action,
                PumpBotEngine.snapshot(context).waitMode == "SELL"
            )) return
        showTradeNotification(
            context,
            geminiExitExperimentChannelId,
            if (buy) geminiExperimentBuyNotificationId else geminiExperimentSellNotificationId,
            if (buy) "DEEPSEEK‑ЭКСПЕРИМЕНТ ВОШЁЛ" else "DEEPSEEK‑ЭКСПЕРИМЕНТ ВЫШЕЛ",
            text,
            if (buy) 0xFFD29922.toInt() else 0xFFFF7B72.toInt(),
            executedTrade = true
        )
        if (buy) {
            EntryAlertReminderStore.arm(
                context, "DEEPSEEK‑ЭКСПЕРИМЕНТ", "EXPERIMENT_TRADE:${trade.decisionId}",
                trade.time, trade.price
            )
        }
    }

    fun showEntryReminder(
        context: Context,
        reminder: EntryAlertReminder,
        currentPrice: Double
    ) {
        ensureChannels(context)
        if (!ResearchModePolicy.userAlertsAllowed(context)) return
        val (channel, id) = when (reminder.source) {
            "APP" -> appTradeChannelId to entryReminderAppId
            "DEEPSEEK" -> geminiTradeChannelId to entryReminderDeepSeekId
            else -> geminiExitExperimentChannelId to entryReminderExperimentId
        }
        val change = if (reminder.initialPrice > 0.0 && currentPrice > 0.0) {
            (currentPrice / reminder.initialPrice - 1.0) * 100.0
        } else 0.0
        val text = String.format(
            java.util.Locale.GERMANY,
            "Вы ещё не подтвердили покупку. Сильный вход %s остаётся свежим; цена €%.8f (%+.2f%% от первого звонка). Не догоняйте цену выше защитного допуска.",
            reminder.source,
            currentPrice,
            change
        )
        showTradeNotification(
            context,
            channel,
            id,
            "ПОВТОР ВХОДА • ${reminder.source}",
            text,
            0xFFFFC107.toInt()
        )
    }

    fun showDeepSeekActionLevel(
        context: Context,
        level: DeepSeekActionLevel,
        state: DeepSeekPrimaryState
    ) {
        ensureChannels(context)
        val title = if (level.level >= DeepSeekActionLevelPolicy.READY_LEVEL) {
            "APP + DEEPSEEK: ВХОД ПОДТВЕРЖДАЕТСЯ • ${level.level}/10"
        } else {
            "APP + DEEPSEEK: ЖЁЛТЫЙ СИГНАЛ • ${level.level}/10"
        }
        val evidence = state.evidence.take(2).joinToString("; ")
        val text = buildString {
            append(level.detail)
            append(" Причина: ")
            append(if (evidence.isNotBlank()) evidence else state.summary)
            append(". Решение о покупке остаётся за вами.")
        }
        SignalAttributionStore.record(
            context,
            "APP + DEEPSEEK",
            if (level.level >= DeepSeekActionLevelPolicy.READY_LEVEL) {
                "ПОДТВЕРЖДЕНИЕ ВХОДА ${level.level}/10"
            } else {
                "ЖЁЛТЫЙ СИГНАЛ ВХОДА ${level.level}/10"
            },
            text,
            state.lastSuccess,
            executedTrade = false
        )
        if (!ResearchModePolicy.userAlertsAllowed(context)) return
        showTradeNotification(
            context,
            signalChannelId,
            deepSeekActionLevelNotificationId,
            title,
            text,
            if (level.level >= DeepSeekActionLevelPolicy.READY_LEVEL) {
                0xFF238636.toInt()
            } else {
                0xFFF0B72F.toInt()
            }
        )
    }

    fun showPersonalPositionGuard(context: Context, reason: String) {
        ensureChannels(context)
        if (!ResearchModePolicy.userAlertsAllowed(context)) return
        showTradeNotification(
            context,
            positionSupervisorChannelId,
            personalGuardNotificationId,
            "СЕРЖ: ЖИВАЯ ПРОВЕРКА ВЫХОДА",
            "$reason. DeepSeek и Gemini получают усиленную проверку. Решение о продаже остаётся за вами.",
            0xFFDA3633.toInt(),
            alwaysLoud = true
        )
    }

    fun showFastPositionWarning(context: Context, decision: FastPositionWarningDecision) {
        ensureChannels(context)
        if (!ResearchModePolicy.userAlertsAllowed(context)) return
        val shock = ShockReboundStore.state(context)
        val title = if (decision.band >= FastPositionWarningPolicy.CRITICAL) {
            "СЕРЖ: КРИТИЧНО • ПРОВЕРЬ ВЫХОД"
        } else {
            "СЕРЖ: БУДЬ ГОТОВ К ВЫХОДУ"
        }
        val ratio = decision.pressure.activityRatio60sTo5m?.let {
            String.format(java.util.Locale.GERMANY, "×%.2f", it)
        } ?: "ещё без 5м фона"
        val text = String.format(
            java.util.Locale.GERMANY,
            "%s. Провал за локальные 3м %.2f%%, отскок %.2f%%, BUY за 60с %.0f%%, темп денег %s. Это предупреждение, не автоматическая продажа.",
            decision.reason,
            shock.drawdown3mPercent,
            shock.rebound3mPercent,
            decision.pressure.buyerShare60s,
            ratio
        )
        showTradeNotification(
            context,
            positionSupervisorChannelId,
            fastPositionWarningNotificationId,
            title,
            text,
            if (decision.band >= FastPositionWarningPolicy.CRITICAL) 0xFFDA3633.toInt() else 0xFFFFC107.toInt(),
            alwaysLoud = true
        )
    }

    fun showPositionSupervision(context: Context, state: PositionSupervisionState) {
        ensureChannels(context)
        if (!ResearchModePolicy.userAlertsAllowed(context)) return
        requireTradeNotificationsAvailable(context)
        val title = when {
            state.action == "CANCEL_EXIT" -> "ОТМЕНА ВЫХОДА — ПРОДОЛЖАЕМ"
            state.dangerLevel >= PositionAlertPolicy.CRITICAL_LEVEL ->
                "КРИТИЧНО • ПРОВЕРЬ ВЫХОД ${state.dangerLevel}/10"
            !state.exitAdvised && state.dangerLevel >= PositionAlertPolicy.PREPARE_LEVEL ->
                "БУДЬ ГОТОВ К ВЫХОДУ ${state.dangerLevel}/10"
            state.exitAdvised && state.conditionDelta < 0 ->
                "СИТУАЦИЯ УХУДШАЕТСЯ ${state.conditionDelta}/−10"
            state.exitAdvised && state.conditionDelta > 0 ->
                "СИТУАЦИЯ УЛУЧШАЕТСЯ +${state.conditionDelta}/+10"
            else -> "DEEPSEEK РЕКОМЕНДУЕТ ВЫХОД"
        }
        val text = PositionSupervisorPolicy.statusText(state) +
            "\nМодель: ${state.model}. Решение о продаже остаётся за вами."
        val urgentExit = state.exitAdvised || state.dangerLevel >= PositionAlertPolicy.PREPARE_LEVEL
        val loud = urgentExit
        val notification = NotificationCompat.Builder(
            context,
            if (loud) positionSupervisorChannelId else silentAlertChannelId
        )
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(if (loud) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_STATUS)
            .setColor(if (state.action == "CANCEL_EXIT") 0xFF238636.toInt() else 0xFFDA3633.toInt())
            .setVibrate(if (loud) longArrayOf(0, 900, 180, 900, 180, 1300) else longArrayOf(0))
            .setSound(if (loud) AlertSoundPreferences.uri(context) else null)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(positionSupervisorNotificationId, notification)
        if (loud) vibrate(context, longArrayOf(0, 900, 180, 900, 180, 1300))
    }

    fun showGeminiPositionAdvisor(context: Context, state: GeminiPositionAdvisorState) {
        ensureChannels(context)
        if (!ResearchModePolicy.userAlertsAllowed(context)) return
        val sources = state.sources.take(2).joinToString("; ")
        val text = buildString {
            append(GeminiPositionAdvisorPolicy.statusText(state))
            if (sources.isNotBlank()) append("\nПроверенный фон: $sources")
            append("\nGemini ничего не продаёт автоматически; решение остаётся за вами.")
        }
        showTradeNotification(
            context,
            positionSupervisorChannelId,
            geminiPositionAdvisorNotificationId,
            if (state.action == "EXIT" && state.dangerLevel >= 9) {
                "GEMINI: КРИТИЧЕСКИЙ ВЫХОД"
            } else if (state.action == "EXIT") {
                "GEMINI РЕКОМЕНДУЕТ ВЫХОД"
            } else {
                "GEMINI: УСИЛЕННОЕ НАБЛЮДЕНИЕ"
            },
            text,
            0xFFFF7B72.toInt(),
            alwaysLoud = state.action == "EXIT",
            scheduledSound = false
        )
    }

    fun clearPersonalPositionAlerts(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(positionSupervisorNotificationId)
        manager.cancel(personalGuardNotificationId)
        manager.cancel(geminiPositionAdvisorNotificationId)
    }

    fun silenceUserAlerts(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        listOf(
            signalNotificationId,
            rapidDropNotificationId,
            eventRadarNotificationId,
            geminiBuyNotificationId,
            appBuyNotificationId,
            appSellNotificationId,
            geminiSellNotificationId,
            geminiExperimentBuyNotificationId,
            geminiExperimentSellNotificationId,
            positionSupervisorNotificationId,
            personalGuardNotificationId,
            entryReminderAppId,
            entryReminderDeepSeekId,
            entryReminderExperimentId,
            geminiPositionAdvisorNotificationId,
            deepSeekActionLevelNotificationId,
            appSoundTestNotificationId,
            deepSeekSoundTestNotificationId,
            experimentSoundTestNotificationId,
            sergeSoundTestNotificationId,
            fastPositionWarningNotificationId
        ).forEach(manager::cancel)
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.cancel()
    }

    enum class SoundTestTarget { APP, DEEPSEEK, EXPERIMENT, SERGE }

    fun showSoundTest(context: Context, target: SoundTestTarget) {
        if (!ResearchModePolicy.soundAllowed(context)) return
        ensureChannels(context)
        requireTradeNotificationsAvailable(context)
        val config = when (target) {
            SoundTestTarget.APP -> SoundTestConfig(appTradeChannelId, appSoundTestNotificationId, "ТЕСТ ЗВОНКА • APP", 0xFF238636.toInt())
            SoundTestTarget.DEEPSEEK -> SoundTestConfig(geminiTradeChannelId, deepSeekSoundTestNotificationId, "ТЕСТ ЗВОНКА • DEEPSEEK", 0xFF7C3AED.toInt())
            SoundTestTarget.EXPERIMENT -> SoundTestConfig(geminiExitExperimentChannelId, experimentSoundTestNotificationId, "ТЕСТ ЗВОНКА • ЭКСПЕРИМЕНТ", 0xFFD29922.toInt())
            SoundTestTarget.SERGE -> SoundTestConfig(positionSupervisorChannelId, sergeSoundTestNotificationId, "ТЕСТ ЗВОНКА • СЕРЖ", 0xFFDA3633.toInt())
        }
        val notification = NotificationCompat.Builder(context, config.channelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(config.title)
            .setContentText("Проверка выбранной мелодии и отдельного звукового канала V5.0.")
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(config.color)
            .setVibrate(longArrayOf(0, 700, 250, 700, 250, 1100))
            .setSound(AlertSoundPreferences.uri(context))
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(config.notificationId, notification)
        vibrate(context)
    }

    fun showDeepSeekCostWarning(context: Context, estimatedCostUsd: Double) {
        ensureChannels(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return
        val text = String.format(
            java.util.Locale.GERMANY,
            "Оценка расходов сегодня: $%.2f (примерно порог €5). Это только уведомление: анализ DeepSeek не остановлен.",
            estimatedCostUsd
        )
        val notification = NotificationCompat.Builder(
            context,
            if (ResearchModePolicy.soundAllowed(context)) deepSeekCostChannelId else silentAlertChannelId
        )
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("DeepSeek: расходы превысили примерно €5")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setColor(0xFFF0B72F.toInt())
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(deepSeekCostNotificationId, notification)
    }

    private fun showTradeNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        text: String,
        color: Int,
        alwaysLoud: Boolean = false,
        scheduledSound: Boolean = true,
        executedTrade: Boolean = false
    ) {
        requireTradeNotificationsAvailable(context)
        val loud = AlertDeliveryPolicy.shouldRing(
            preparatoryAllowed = scheduledSound && AlertSchedule.isAllowedNow(context),
            executedTradeAllowed = scheduledSound && AlertSchedule.isExecutedTradeAllowedNow(context),
            executedTrade = executedTrade,
            urgentPersonalExit = alwaysLoud
        ) && ResearchModePolicy.soundAllowed(context)
        val notification = NotificationCompat.Builder(context, if (loud) channelId else silentAlertChannelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(if (loud) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_STATUS)
            .setColor(color)
            .setVibrate(if (loud) longArrayOf(0, 700, 250, 700, 250, 1100) else longArrayOf(0))
            .setSound(if (loud) AlertSoundPreferences.uri(context) else null)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(notificationId, notification)
        if (loud) vibrate(context)
    }

    fun showHumanFactor(context: Context, entry: Boolean, detail: String) {
        ensureChannels(context)
        if (!ResearchModePolicy.userAlertsAllowed(context)) return
        showTradeNotification(
            context, appTradeChannelId, if (entry) 64032 else 64033,
            if (entry) "ЧЕЛОВЕЧЕСКИЙ ФАКТОР • ПРОВЕРЬТЕ ВХОД" else "ЧЕЛОВЕЧЕСКИЙ ФАКТОР • ВЫХОД",
            detail, if (entry) 0xFF238636.toInt() else 0xFFDA3633.toInt(),
            alwaysLoud = !entry, executedTrade = false
        )
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
        if (!ResearchModePolicy.soundAllowed(context)) return
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
