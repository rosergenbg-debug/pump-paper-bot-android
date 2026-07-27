package com.example.pumppaperbot

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GeminiExperimentActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val clock = object : Runnable {
        override fun run() {
            render()
            handler.postDelayed(this, 1000L)
        }
    }
    private lateinit var root: LinearLayout
    private lateinit var toggle: Button
    private lateinit var runNow: Button
    private lateinit var status: TextView
    private lateinit var portfolio: TextView
    private lateinit var allocation: TextView
    private lateinit var cashBar: View
    private lateinit var investedBar: View
    private lateinit var statistics: TextView
    private lateinit var currentPosition: TextView
    private lateinit var positionPnl: TextView
    private lateinit var positionChart: GeminiPositionChartView
    private lateinit var microImpulse: TextView
    private lateinit var lastOperation: TextView
    private lateinit var lastDecision: TextView
    private lateinit var activityHistory: TextView
    private lateinit var activityToggle: Button
    private lateinit var history: TextView
    private lateinit var trades: TextView
    private var activityExpanded = false
    private var renderedActivityCount = -1
    private var renderedActivityLastAt = Long.MIN_VALUE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0D1117"))
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(28))
        }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(button("← НАЗАД", "#30363D").apply {
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(-1, dp(48)))
        root.addView(label("V3.14 • ВИРТУАЛЬНЫЙ ПОРТФЕЛЬ GEMINI", 24, "#F0F6FC", true))
        root.addView(label(
            "Отдельный виртуальный счёт со стартом €1 000. Gemini самостоятельно принимает решения, " +
                "а приложение показывает деньги, вложение в PUMP, результат и операции. " +
                "Основную стратегию и реальные деньги этот модуль не меняет.",
            14, "#C9D1D9", false, 8
        ))

        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        toggle = button("ВКЛЮЧИТЬ", "#7C3AED")
        runNow = button("ПРОВЕРИТЬ СЕЙЧАС", "#1F6FEB")
        buttons.addView(toggle, LinearLayout.LayoutParams(0, dp(56), 1f))
        buttons.addView(runNow, LinearLayout.LayoutParams(0, dp(56), 1f).apply {
            leftMargin = dp(8)
        })
        root.addView(buttons, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(12) })

        root.addView(button("КЛЮЧ GEMINI И РАДАР НОВОСТЕЙ", "#30363D").apply {
            setOnClickListener {
                startActivity(Intent(this@GeminiExperimentActivity, EventRadarActivity::class.java))
            }
        }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(8) })

        val portfolioPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#211A36"))
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        portfolio = label("", 22, "#F0F6FC", true)
        allocation = label("", 14, "#C9D1D9", false, 8)
        val allocationBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#30363D"))
        }
        cashBar = View(this).apply { setBackgroundColor(Color.parseColor("#58A6FF")) }
        investedBar = View(this).apply { setBackgroundColor(Color.parseColor("#A371F7")) }
        allocationBar.addView(cashBar, LinearLayout.LayoutParams(0, dp(12), 1f))
        allocationBar.addView(investedBar, LinearLayout.LayoutParams(0, dp(12), 0.001f))
        portfolioPanel.addView(portfolio)
        portfolioPanel.addView(allocationBar, LinearLayout.LayoutParams(-1, dp(12)).apply {
            topMargin = dp(12)
        })
        portfolioPanel.addView(allocation)

        status = card("#172033")
        statistics = card("#161B22")
        currentPosition = card("#172033")
        positionPnl = label("", 36, "#79C0FF", true).apply {
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#101820"))
            setPadding(dp(12), dp(14), dp(12), dp(8))
        }
        positionChart = GeminiPositionChartView(this)
        microImpulse = card("#101820")
        lastOperation = card("#161B22")
        lastDecision = card("#172033")
        activityHistory = card("#101820")
        activityToggle = button("ПОКАЗАТЬ ЖУРНАЛ", "#30363D")
        history = card("#161B22")
        trades = card("#161B22")

        root.addView(label("ВИРТУАЛЬНЫЕ ДЕНЬГИ GEMINI", 17, "#D2A8FF", true, 16))
        root.addView(portfolioPanel, cardParams(6))
        root.addView(lastOperation, cardParams())
        root.addView(label("ГРАФИК ОТКРЫТОЙ СДЕЛКИ", 17, "#D2A8FF", true, 16))
        root.addView(positionPnl, cardParams(6))
        root.addView(positionChart, LinearLayout.LayoutParams(-1, dp(220)))
        root.addView(currentPosition, cardParams(6))
        root.addView(label("MICRO IMPULSE • ТЕНЕВОЙ РЕЖИМ", 17, "#F0B72F", true, 16))
        root.addView(microImpulse, cardParams(6))
        root.addView(lastDecision, cardParams())
        root.addView(statistics, cardParams())
        root.addView(status, cardParams())

        root.addView(label("ВИРТУАЛЬНЫЕ ОПЕРАЦИИ", 17, "#D2A8FF", true, 16))
        root.addView(trades, cardParams(6))
        root.addView(label("ИСТОРИЯ РЕШЕНИЙ", 17, "#79C0FF", true, 16))
        root.addView(history, cardParams(6))

        root.addView(label("ЧЕСТНЫЙ КОНТРОЛЬ ИДЕИ", 17, "#F0B72F", true, 16))
        root.addView(label(
            "Повторный шестимесячный тест часовых рыночных признаков без подмешивания недоступного архива новостей не подтвердил цель. " +
                "На validation поймано 24,1% подъёмов PUMP свыше 3% (7 из 29), на закрытом holdout — 27,3% (12 из 44), результат после комиссий −14,76%. " +
                "Чтобы искусственно превысить 50% на validation, потребовалось 6,1 сигнала в сутки и 208 ложных из 223. " +
                "Поэтому Gemini работает только как живой отдельный эксперимент и сам накапливает проверяемую статистику.",
            14, "#F0B72F", false, 6
        ))

        root.addView(label("ТЕХНИЧЕСКИЙ ЖУРНАЛ • 24 ЧАСА", 17, "#7EE787", true, 16))
        root.addView(activityToggle, LinearLayout.LayoutParams(-1, dp(48)).apply {
            topMargin = dp(6)
        })
        activityHistory.visibility = View.GONE
        root.addView(activityHistory, cardParams(6))
        activityToggle.setOnClickListener {
            activityExpanded = !activityExpanded
            activityHistory.visibility = if (activityExpanded) View.VISIBLE else View.GONE
            render()
        }

        root.addView(button("СБРОСИТЬ ТОЛЬКО GEMINI‑ЭКСПЕРИМЕНТ", "#8E1519").apply {
            setOnClickListener {
                AlertDialog.Builder(this@GeminiExperimentActivity)
                    .setTitle("Сбросить виртуальный счёт Gemini?")
                    .setMessage("Удалятся только решения, виртуальные сделки и статистика Gemini. Основная стратегия не изменится.")
                    .setNegativeButton("ОТМЕНА", null)
                    .setPositiveButton("СБРОСИТЬ") { _, _ ->
                        GeminiPaperStore.reset(this@GeminiExperimentActivity)
                        render()
                    }
                    .show()
            }
        }, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(18) })

        toggle.setOnClickListener {
            val enabled = GeminiPaperStore.state(this).enabled
            GeminiPaperStore.setEnabled(this, !enabled)
            render()
        }
        runNow.setOnClickListener { runCheck() }
        render()
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(clock)
        handler.post(clock)
    }

    override fun onPause() {
        handler.removeCallbacks(clock)
        super.onPause()
    }

    private fun runCheck() {
        runNow.isEnabled = false
        runNow.text = "ПРОВЕРЯЮ…"
        status.text = "Обновляю рынок и новости, затем проверяю новый закрытый час…"
        Thread cycle@{
            val source = "РУЧНАЯ ПРОВЕРКА ИЗ ОКНА GEMINI"
            if (!GeminiCycleGuard.tryEnter()) {
                GeminiPaperStore.recordActivity(
                    this,
                    "ЦИКЛ",
                    "WAIT",
                    "$source: предыдущая проверка ещё выполняется"
                )
                runOnUiThread {
                    runNow.isEnabled = true
                    runNow.text = "ПРОВЕРИТЬ СЕЙЧАС"
                    render()
                }
                return@cycle
            }
            val startedAt = System.currentTimeMillis()
            val interval = if (PumpBotEngine.snapshot(this).running) {
                TimeUnit.MINUTES.toMillis(2)
            } else {
                TimeUnit.MINUTES.toMillis(15)
            }
            GeminiPaperStore.beginCycle(this, source, interval, startedAt)
            try {
                val result = runCatching {
                    MarketSyncClient().sync(this)
                    EventRadarClient().sync(this, force = true)
                    GeminiPaperStore.markDataReady(this, source, startedAt)
                    GeminiExperimentClient().sync(this, force = true, source = source)
                }
                val finishedAt = System.currentTimeMillis()
                result.fold(
                    onSuccess = {
                        GeminiPaperStore.finishCycle(
                            this,
                            source,
                            startedAt,
                            finishedAt + interval,
                            "ручная проверка завершена; Gemini: ${it.status}",
                            finishedAt
                        )
                    },
                    onFailure = {
                        GeminiPaperStore.saveFailure(
                            this,
                            it.message ?: "Ошибка обновления",
                            finishedAt
                        )
                        GeminiPaperStore.failCycle(
                            this,
                            source,
                            startedAt,
                            finishedAt + interval,
                            it.message ?: it.javaClass.simpleName,
                            finishedAt
                        )
                    }
                )
                runOnUiThread {
                    runNow.isEnabled = true
                    runNow.text = "ПРОВЕРИТЬ СЕЙЧАС"
                    render()
                }
            } finally {
                GeminiCycleGuard.exit()
            }
        }.start()
    }

    private fun render() {
        val state = GeminiPaperStore.state(this, includeActivity = true)
        val now = System.currentTimeMillis()
        val visibleStatus = GeminiHourlyRetryPolicy.visibleStatus(state, now)
        val budget = GeminiRequestBudget.state(this, now)
        val snapshot = PumpBotEngine.snapshot(this)
        val p = state.portfolio
        val displayPrice = if (snapshot.lastPrice > 0.0) snapshot.lastPrice else p.entryPrice
        val value = p.value(displayPrice)
        toggle.text = if (state.enabled) "ВЫКЛЮЧИТЬ" else "ВКЛЮЧИТЬ"
        toggle.setBackgroundColor(Color.parseColor(if (state.enabled) "#7C3AED" else "#6E7681"))
        val keySource = when {
            EventRadarStore.hasCustomApiKey(this) -> "сохранённый личный ключ"
            EmbeddedGeminiKey.value.isNotBlank() -> "встроенный ключ"
            else -> "ключ не настроен"
        }
        status.text = buildString {
            append("СТАТУС: $visibleStatus")
            append("\nТекущая стадия: ${state.phase}")
            append("\nКлюч: $keySource")
            val activeModel = state.activeModel.ifBlank { state.model }
            if (activeModel.isNotBlank()) append("\nМодель: $activeModel")
            if (state.lastAttempt > 0L) {
                append(
                    "\nПоследнее обращение: ${PumpBotEngine.formatTime(state.lastAttempt)}" +
                        " • попытка ${state.attemptsThisHour.coerceAtMost(3)}/3"
                )
            }
            if (state.lastCycleStarted > 0L) {
                append("\nПоследний цикл: ${activityTime(state.lastCycleStarted)}")
                if (state.cycleSource.isNotBlank()) append(" • ${state.cycleSource}")
            }
            if (state.lastDataReady > 0L) {
                append(
                    "\nДанные собраны: ${activityTime(state.lastDataReady)}" +
                        " • ${formatDuration(state.dataDurationMillis)}"
                )
            }
            if (state.lastSuccess > 0L) append("\nПоследний успешный ответ: ${PumpBotEngine.formatTime(state.lastSuccess)}")
            val nextAt = maxOf(
                GeminiHourlyRetryPolicy.nextVisibleActionAt(state, now),
                budget.nextAllowedAt
            )
            when {
                !state.enabled -> append("\nСледующий запрос: выключен")
                visibleStatus == "НЕТ КЛЮЧА GEMINI" ->
                    append("\nСледующий запрос: сначала сохраните ключ Gemini")
                nextAt <= 0L -> append("\nСейчас выполняется сетевой запрос • предел 85 секунд")
                state.lastFailure >= state.lastSuccess &&
                    state.attemptsThisHour in 1 until GeminiHourlyRetryPolicy.MAX_AUTOMATIC_ATTEMPTS_PER_HOUR ->
                    append("\nПовтор разрешён после ${PumpBotEngine.formatTime(nextAt)}")
                else -> append("\nНовый прогноз после ${PumpBotEngine.formatTime(nextAt)}")
            }
            if (state.nextCheckAt > 0L && state.enabled) {
                append(
                    "\nСледующая проверка цикла: ${activityTime(state.nextCheckAt)}" +
                        " • через ${countdown(state.nextCheckAt, now)}"
                )
            }
            append("\nКонтроль жизни: ${cycleHealth(state, snapshot.running, now)}")
            append("\nЦикл: рынок ~2 мин • RSS-новости ~10 мин • Gemini API после закрытия часа")
            append(
                "\nОбщий бюджет Gemini: ${budget.usedToday}/${GeminiRequestBudget.MAX_REQUESTS_PER_DAY}" +
                    " • осталось ${budget.remainingToday}"
            )
            append("\nЧасовой контур: ${state.requestsToday} запросов • ${state.totalTokensToday} токенов")
            if (state.error.isNotBlank()) append("\nОшибка: ${state.error}")
        }
        status.setTextColor(Color.parseColor(
            when {
                visibleStatus.startsWith("ОШИБКА") ||
                    visibleStatus.startsWith("НЕТ КЛЮЧА") -> "#FF7B72"
                visibleStatus == "РАБОТАЕТ" -> "#7EE787"
                else -> "#79C0FF"
            }
        ))

        val activity24h = state.activity.filter { it.at >= now - ACTIVITY_WINDOW_MILLIS }
        val activityLastAt = activity24h.lastOrNull()?.at ?: 0L
        activityToggle.text = if (activityExpanded) {
            "СКРЫТЬ ЖУРНАЛ (${activity24h.size})"
        } else {
            "ПОКАЗАТЬ ЖУРНАЛ ЗА 24 ЧАСА (${activity24h.size})"
        }
        if (renderedActivityCount != state.activity.size || renderedActivityLastAt != activityLastAt) {
            val shownActivity = activity24h.asReversed()
            activityHistory.text = if (shownActivity.isEmpty()) {
                "За последние 24 часа событий нет."
            } else {
                buildString {
                    append("События только за последние 24 часа: ${shownActivity.size}")
                    append("\n\n")
                    append(shownActivity.joinToString("\n\n") { event ->
                        buildString {
                            append("${activityIcon(event.result)} ${activityTime(event.at)} • ${event.stage}")
                            if (event.model.isNotBlank()) append(" • ${event.model}")
                            if (event.attempt > 0) append(" • попытка ${event.attempt}/3")
                            if (event.durationMillis > 0L) append(" • ${formatDuration(event.durationMillis)}")
                            append("\n${event.detail}")
                        }
                    })
                }
            }
            renderedActivityCount = activity24h.size
            renderedActivityLastAt = activityLastAt
        }

        val investedEur = if (p.inPosition && displayPrice > 0.0) {
            p.pumpAmount * displayPrice
        } else {
            0.0
        }
        val cashEur = p.cashEur.coerceAtLeast(0.0)
        val allocationTotal = (cashEur + investedEur).coerceAtLeast(0.01)
        cashBar.layoutParams = LinearLayout.LayoutParams(
            0,
            dp(12),
            (cashEur / allocationTotal).toFloat().coerceAtLeast(0.001f)
        )
        investedBar.layoutParams = LinearLayout.LayoutParams(
            0,
            dp(12),
            (investedEur / allocationTotal).toFloat().coerceAtLeast(0.001f)
        )
        val position = if (p.inPosition) {
            String.format(
                Locale.GERMANY,
                "В PUMP: %.2f монет • вход €%.8f",
                p.pumpAmount,
                p.entryPrice
            )
        } else {
            String.format(Locale.GERMANY, "В НАЛИЧНЫХ: €%.2f", p.cashEur)
        }
        portfolio.text = String.format(
            Locale.GERMANY,
            "ОБЩАЯ СТОИМОСТЬ\n€%.2f\n\nСтарт €1 000,00  •  результат %+.2f%%\n%s",
            value,
            p.profitPercent(displayPrice),
            position
        )
        allocation.text = String.format(
            Locale.GERMANY,
            "СИНИЙ — СВОБОДНЫЕ EUR  €%.2f\nФИОЛЕТОВЫЙ — ВЛОЖЕНО В PUMP  €%.2f\nКомиссии за всё время €%.2f",
            cashEur,
            investedEur,
            p.totalFeesEur
        )
        portfolio.setTextColor(Color.parseColor(
            if (p.profit(displayPrice) >= 0.0) "#D2A8FF" else "#FF7B72"
        ))

        val latestTrade = p.trades.lastOrNull()
        lastOperation.text = when (latestTrade?.action) {
            "BUY" -> String.format(
                Locale.GERMANY,
                "● СЕЙЧАС: КУПЛЕНО PUMP\n%s • вложено €%.2f\nЦена покупки €%.8f • количество %.2f PUMP",
                PumpBotEngine.formatTime(latestTrade.time),
                latestTrade.amount * latestTrade.price + latestTrade.fee,
                latestTrade.price,
                latestTrade.amount
            )
            "SELL" -> String.format(
                Locale.GERMANY,
                "● СЕЙЧАС: ПРОДАНО — ДЕНЬГИ В EUR\n%s • получено €%.2f\nРезультат сделки %+.2f €",
                PumpBotEngine.formatTime(latestTrade.time),
                latestTrade.amount * latestTrade.price - latestTrade.fee,
                latestTrade.pnlEur
            )
            else -> "● СЕЙЧАС: ОЖИДАНИЕ\nПокупок и продаж Gemini пока не было. Все €1 000,00 находятся в EUR."
        }
        lastOperation.setTextColor(Color.parseColor(
            when (latestTrade?.action) {
                "BUY" -> "#D2A8FF"
                "SELL" -> if (latestTrade.pnlEur >= 0.0) "#7EE787" else "#FF7B72"
                else -> "#79C0FF"
            }
        ))

        val activeBuy = if (p.inPosition) p.trades.lastOrNull { it.action == "BUY" } else null
        if (p.inPosition && activeBuy != null && displayPrice > 0.0) {
            val positionCost = activeBuy.amount * activeBuy.price + activeBuy.fee
            val livePnlEur = investedEur - positionCost
            val livePnlPercent = if (positionCost > 0.0) livePnlEur / positionCost * 100.0 else 0.0
            currentPosition.text = String.format(
                Locale.GERMANY,
                "ОТКРЫТАЯ ПОЗИЦИЯ — РЕЗУЛЬТАТ ПРЯМО СЕЙЧАС\n" +
                    "Вложено €%.2f  →  сейчас стоит €%.2f\n" +
                    "ТЕКУЩИЙ РЕЗУЛЬТАТ  %+.2f €  (%+.2f%%)\n" +
                    "Покупка €%.8f  •  текущая цена €%.8f",
                positionCost,
                investedEur,
                livePnlEur,
                livePnlPercent,
                activeBuy.price,
                displayPrice
            )
            val liveColor = if (livePnlEur >= 0.0) "#7EE787" else "#FF7B72"
            currentPosition.setTextColor(Color.parseColor(liveColor))
            positionPnl.text = String.format(
                Locale.GERMANY,
                "%+.2f%%\n%+.2f €",
                livePnlPercent,
                livePnlEur
            )
            positionPnl.setTextColor(Color.parseColor(liveColor))
        } else {
            currentPosition.text = String.format(
                Locale.GERMANY,
                "ОТКРЫТОЙ ПОЗИЦИИ НЕТ\nВсе деньги сейчас в EUR: €%.2f\nПлавающий результат: €0,00",
                cashEur
            )
            currentPosition.setTextColor(Color.parseColor("#79C0FF"))
            positionPnl.text = "ПОЗИЦИЯ НЕ ОТКРЫТА"
            positionPnl.setTextColor(Color.parseColor("#79C0FF"))
        }
        positionChart.setPosition(
            candles = snapshot.chart.candles,
            entryTime = activeBuy?.time ?: 0L,
            entry = activeBuy?.price ?: 0.0,
            current = displayPrice,
            active = p.inPosition
        )

        val micro = MicroImpulseStore.state(this)
        microImpulse.text = buildString {
            append("СОСТОЯНИЕ: ${micro.phase}  •  оценка ${micro.score}/100")
            append("\nПоток: ${if (micro.connected) "подключён" else "переподключение"}")
            if (micro.updatedAt > 0L) append("  •  обновлено ${activityTime(micro.updatedAt)}")
            append(String.format(
                Locale.GERMANY,
                "\nСделки 5 с: %d  •  ускорение ×%.2f  •  покупки %.1f%%",
                micro.trades5s,
                micro.tradeAcceleration,
                micro.aggressiveBuyPercent5s
            ))
            append(String.format(
                Locale.GERMANY,
                "\nПокупки 15 с: %.1f%%  •  цена 60 с: %+.3f%%",
                micro.aggressiveBuyPercent15s,
                micro.priceChange60sPercent
            ))
            micro.spreadPercent?.let {
                append(String.format(Locale.GERMANY, "  •  spread %.4f%%", it))
            }
            micro.topBookImbalance?.let {
                append(String.format(Locale.GERMANY, "\nВерх стакана: %+.2f", it))
            }
            if (micro.error.isNotBlank()) append("\nОшибка: ${micro.error}")
            append("\nНаблюдает PUMP/USDT по секундам. Не покупает, не продаёт и не влияет на Gemini.")
        }
        microImpulse.setTextColor(Color.parseColor(
            when (micro.phase) {
                "IGNITION" -> "#FF7B72"
                "CONFIRMATION" -> "#7EE787"
                "PRESSURE" -> "#F0B72F"
                else -> "#79C0FF"
            }
        ))

        statistics.text = String.format(
            Locale.GERMANY,
            "ОБЩИЙ ИТОГ ВСЕЙ ТОРГОВЛИ\nРезультат %+.2f € (%+.2f%%) • стоимость €%.2f\n" +
                "Закрытых сделок %d • прибыльных %d • win rate %.1f%%\n" +
                "Макс. живая просадка %.2f%% • точность направления %.1f%% (%d прогнозов)\n" +
                "Подъёмы >3%%: поймано %d из %d • %.1f%%",
            p.profit(displayPrice),
            p.profitPercent(displayPrice),
            value,
            p.closedTrades,
            p.winningTrades,
            p.winRatePercent,
            p.causalMaxDrawdownPercent,
            p.directionAccuracyPercent,
            p.evaluatedHours,
            p.capturedSurges,
            p.surgeOpportunities,
            p.surgeCapturePercent
        )

        val last = p.decisions.lastOrNull()
        lastDecision.text = if (last == null) {
            "ПОСЛЕДНЕЕ РЕШЕНИЕ\nПока нет. Нужны рыночные данные, ключ Gemini и новый полностью закрытый час."
        } else {
            buildString {
                append("ПОСЛЕДНЕЕ РЕШЕНИЕ: ${actionRu(last.requestedAction)}")
                append("\n${PumpBotEngine.formatTime(last.decidedAt)} • направление ${signed(last.directionScore)}/100 • уверенность ${last.confidence}/100")
                append("\n${last.execution} • горизонт ${last.horizonHours} ч • счёт €${String.format(Locale.GERMANY, "%.2f", last.portfolioValueAfter)}")
                if (last.evaluationVersion >= GeminiHourlyDecision.CAUSAL_EVALUATION_VERSION) {
                    append("\nИсполнение после ответа: ${PumpBotEngine.formatTime(last.executionQuoteAt)} • €${String.format(Locale.GERMANY, "%.8f", last.price)}")
                }
                append("\n${last.reason}")
                if (last.risks.isNotEmpty()) append("\nРиски: ${last.risks.joinToString("; ")}")
            }
        }

        history.text = p.decisions.takeLast(24).asReversed().joinToString("\n\n") { decision ->
            buildString {
                append("${PumpBotEngine.formatTime(decision.decidedAt)} • ${actionRu(decision.requestedAction)}")
                append(" • ${signed(decision.directionScore)}/100 • увер. ${decision.confidence} • ${decision.horizonHours} ч")
                decision.evaluatedReturnPercent?.let {
                    append(" • итог горизонта ${String.format(Locale.GERMANY, "%+.2f%%", it)}")
                }
                decision.peakReturnPercent?.let {
                    append(" • максимум ${String.format(Locale.GERMANY, "%+.2f%%", it)}")
                }
                append("\n${decision.reason.take(220)}")
            }
        }.ifBlank { "Решений пока нет." }

        trades.text = p.trades.takeLast(30).asReversed().joinToString("\n\n") { trade ->
            buildString {
                append("${PumpBotEngine.formatTime(trade.time)} • ${actionRu(trade.action)}")
                append(" • €${String.format(Locale.GERMANY, "%.8f", trade.price)}")
                append(" • комиссия €${String.format(Locale.GERMANY, "%.2f", trade.fee)}")
                if (trade.action == "SELL") {
                    append(" • итог ${String.format(Locale.GERMANY, "%+.2f €", trade.pnlEur)}")
                }
                append("\n${trade.reason.take(220)}")
            }
        }.ifBlank { "Виртуальных операций пока нет." }
    }

    private fun actionRu(value: String): String = when (value) {
        "BUY" -> "КУПИТЬ"
        "SELL" -> "ПРОДАТЬ"
        else -> "ДЕРЖАТЬ / ЖДАТЬ"
    }

    private fun signed(value: Int): String = if (value >= 0) "+$value" else value.toString()

    private fun cycleHealth(
        state: GeminiExperimentState,
        monitorRunning: Boolean,
        now: Long
    ): String = when {
        !state.enabled -> "Gemini выключен"
        state.lastCycleStarted <= 0L -> "цикл ещё ни разу не запускался"
        state.lastCycleStarted > state.lastCycleFinished &&
            now - state.lastCycleStarted > TimeUnit.MINUTES.toMillis(3) ->
            "ВНИМАНИЕ: текущий цикл длится больше 3 минут"
        monitorRunning && state.nextCheckAt > 0L &&
            now - state.nextCheckAt > TimeUnit.MINUTES.toMillis(3) ->
            "ВНИМАНИЕ: ожидаемая проверка опаздывает больше чем на 3 минуты"
        state.lastCycleStarted > state.lastCycleFinished ->
            "цикл выполняется, ${formatDuration(now - state.lastCycleStarted)}"
        else -> "цикл отвечает и завершил последнюю проверку"
    }

    private fun activityIcon(result: String): String = when (result) {
        "OK" -> "✓"
        "ERROR" -> "!"
        "START" -> "▶"
        else -> "•"
    }

    private fun activityTime(value: Long): String =
        SimpleDateFormat("dd.MM HH:mm:ss", Locale.GERMANY).format(Date(value))

    private fun formatDuration(value: Long): String {
        val seconds = value.coerceAtLeast(0L) / 1000.0
        return if (seconds < 60.0) {
            String.format(Locale.GERMANY, "%.1f с", seconds)
        } else {
            String.format(Locale.GERMANY, "%.1f мин", seconds / 60.0)
        }
    }

    private fun countdown(target: Long, now: Long): String {
        val remaining = (target - now).coerceAtLeast(0L)
        val minutes = remaining / 60_000L
        val seconds = (remaining % 60_000L) / 1000L
        return String.format(Locale.GERMANY, "%02d:%02d", minutes, seconds)
    }

    private fun card(color: String): TextView = label("", 14, "#C9D1D9", false).apply {
        setBackgroundColor(Color.parseColor(color))
        setPadding(dp(12), dp(12), dp(12), dp(12))
    }

    private fun cardParams(top: Int = 10) = LinearLayout.LayoutParams(-1, -2).apply {
        topMargin = dp(top)
    }

    private fun label(
        text: String,
        size: Int,
        color: String,
        bold: Boolean,
        top: Int = 0
    ): TextView = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(Color.parseColor(color))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        if (top > 0) setPadding(0, dp(top), 0, 0)
        setLineSpacing(0f, 1.08f)
    }

    private fun button(text: String, color: String): Button = Button(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        textSize = 12f
        gravity = Gravity.CENTER
        isAllCaps = false
        setBackgroundColor(Color.parseColor(color))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private val ACTIVITY_WINDOW_MILLIS = TimeUnit.HOURS.toMillis(24)
    }
}
