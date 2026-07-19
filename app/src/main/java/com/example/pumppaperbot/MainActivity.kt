package com.example.pumppaperbot

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val refreshUi = object : Runnable {
        override fun run() {
            updateUi()
            handler.postDelayed(this, 5000)
        }
    }

    private var tvStatus: TextView? = null
    private var tvBuySignal: TextView? = null
    private var tvSellSignal: TextView? = null
    private var tvMode: TextView? = null
    private var tvReadiness: TextView? = null
    private var tvRapidDrop: TextView? = null
    private var tvBreathingState: TextView? = null
    private var tvEnergy: TextView? = null
    private var tvDirection: TextView? = null
    private var tvConfidence: TextView? = null
    private var tvLateRisk: TextView? = null
    private var tvMicrostructure: TextView? = null
    private var tvPrice: TextView? = null
    private var tvReason: TextView? = null
    private var tvPosition: TextView? = null
    private var tvAlertStatus: TextView? = null
    private var chart: StrategyChartView? = null
    private var btnRisk30: Button? = null
    private var btnRisk35: Button? = null
    private var btnStart: Button? = null
    private var btnCheck: Button? = null
    private var btnStop: Button? = null
    private var btnReset: Button? = null
    private var btnManual: Button? = null
    private var btnToggleMode: Button? = null
    private var btnBacktest: Button? = null
    private var btnAlertSettings: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvBuySignal = findViewById(R.id.tvBuySignal)
        tvSellSignal = findViewById(R.id.tvSellSignal)
        tvMode = findViewById(R.id.tvMode)
        tvReadiness = findViewById(R.id.tvReadiness)
        tvRapidDrop = findViewById(R.id.tvRapidDrop)
        tvBreathingState = findViewById(R.id.tvBreathingState)
        tvEnergy = findViewById(R.id.tvEnergy)
        tvDirection = findViewById(R.id.tvDirection)
        tvConfidence = findViewById(R.id.tvConfidence)
        tvLateRisk = findViewById(R.id.tvLateRisk)
        tvMicrostructure = findViewById(R.id.tvMicrostructure)
        tvPrice = findViewById(R.id.tvPrice)
        tvReason = findViewById(R.id.tvReason)
        tvPosition = findViewById(R.id.tvPosition)
        tvAlertStatus = findViewById(R.id.tvAlertStatus)
        chart = findViewById(R.id.chart)
        btnRisk30 = findViewById(R.id.btnRisk30)
        btnRisk35 = findViewById(R.id.btnRisk35)
        btnStart = findViewById(R.id.btnStart)
        btnCheck = findViewById(R.id.btnCheck)
        btnStop = findViewById(R.id.btnStop)
        btnReset = findViewById(R.id.btnReset)
        btnManual = findViewById(R.id.btnManual)
        btnToggleMode = findViewById(R.id.btnToggleMode)
        btnBacktest = findViewById(R.id.btnBacktest)
        btnAlertSettings = findViewById(R.id.btnAlertSettings)

        PumpBotEngine.ensureInitialized(this)
        requestNotificationPermission()

        btnRisk30?.setOnClickListener {
            PumpBotEngine.setAggressive(this, false)
            updateUi()
            checkNow()
        }
        btnRisk35?.setOnClickListener {
            PumpBotEngine.setAggressive(this, true)
            updateUi()
            checkNow()
        }
        btnStart?.setOnClickListener { startMonitor() }
        btnCheck?.setOnClickListener { checkNow() }
        btnStop?.setOnClickListener {
            confirm("Остановить монитор?", "Проверка PUMP и звуковые сигналы будут остановлены.") {
                stopMonitor()
            }
        }
        btnReset?.setOnClickListener {
            confirm("Сбросить состояние?", "Очистится режим ожидания, цена входа и сохраненные данные графика.") {
                resetAll()
            }
        }
        btnManual?.setOnClickListener { confirmManualAction() }
        btnToggleMode?.setOnClickListener { showSignalInfo() }
        btnBacktest?.setOnClickListener { startActivity(Intent(this, BacktestActivity::class.java)) }
        btnAlertSettings?.setOnClickListener { startActivity(Intent(this, AlertSettingsActivity::class.java)) }
        chart?.setOnClickListener { startActivity(Intent(this, ChartDetailActivity::class.java)) }

        updateUi()
        checkNow()
    }

    override fun onResume() {
        super.onResume()
        updateUi()
        handler.post(refreshUi)
    }

    override fun onPause() {
        handler.removeCallbacks(refreshUi)
        super.onPause()
    }

    private fun startMonitor() {
        PumpBotEngine.setRunning(this, true)
        PumpAlert.ensureChannels(this)
        ContextCompat.startForegroundService(this, Intent(this, PumpSignalService::class.java))
        val request = PeriodicWorkRequestBuilder<PumpBotWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PumpBotEngine.uniqueWorkName,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
        updateUi()
    }

    private fun stopMonitor() {
        PumpBotEngine.setRunning(this, false)
        stopService(Intent(this, PumpSignalService::class.java))
        WorkManager.getInstance(this).cancelUniqueWork(PumpBotEngine.uniqueWorkName)
        updateUi()
    }

    private fun resetAll() {
        stopService(Intent(this, PumpSignalService::class.java))
        WorkManager.getInstance(this).cancelUniqueWork(PumpBotEngine.uniqueWorkName)
        PumpBotEngine.reset(this)
        updateUi()
        checkNow()
    }

    private fun checkNow() {
        WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<PumpBotWorker>().build())
        handler.postDelayed({ updateUi() }, 2000)
        handler.postDelayed({ updateUi() }, 6000)
    }

    private fun confirmManualAction() {
        val snapshot = PumpBotEngine.snapshot(this)
        if (snapshot.waitMode == "BUY") {
            confirm("Подтвердить покупку?", "Приложение запомнит цену PUMP/EUR и режим ${snapshot.strategyMode}.") {
                PumpBotEngine.confirmBought(this)
                updateUi()
            }
        } else if (snapshot.signalAction == StrategyV2.ACTION_SELL_HALF && !snapshot.partialTaken) {
            val percent = if (snapshot.strategyMode == StrategyV2.MODE_EXHAUSTION) 40 else 50
            val remains = 100 - percent
            confirm("Подтвердить продажу $percent%?", "Оставшиеся $remains% будут защищены трейлингом 4%.") {
                PumpBotEngine.confirmPartialSold(this)
                updateUi()
            }
        } else {
            confirm("Подтвердить продажу?", "Приложение очистит цену входа и начнет ждать сигнал на покупку.") {
                PumpBotEngine.confirmSold(this)
                updateUi()
            }
        }
    }

    private fun showSignalInfo() {
        val snapshot = PumpBotEngine.snapshot(this)
        val profile = if (snapshot.aggressive) {
            "Активный: четыре этапа, допускает вход после восстановления до −3% от максимума"
        } else {
            "Осторожный: четыре этапа, вход после серии падений только рядом с дном (до −6%)"
        }
        val details = if (snapshot.waitMode == "BUY") {
            "Базовый тренд: ${snapshot.trendReadiness}/100\n" +
                "Серия падений + разворот + покупатели + рынок: ${snapshot.shockReadiness}/100\n"
        } else {
            "Готовность к продаже: ${kotlin.math.abs(snapshot.readinessScore)}/100\n"
        }
        AlertDialog.Builder(this)
            .setTitle("Как рассчитан сигнал")
            .setMessage(
                "$profile\n\n$details\n${snapshot.signalReason}\n\n" +
                    "Дыхание: ${snapshot.breathingState}.\n${snapshot.breathingExplanation}\n\n" +
                    "АКТИВНОСТЬ показывает силу текущего расширения, но не направление. ПОТОК показывает согласованное направление цены и taker-покупок. СОГЛАСОВАНО — качество и согласие доступных данных, а не вероятность прибыли. ПОЗДНИЙ ВХОД показывает риск покупки после уже прошедшего импульса.\n\n" +
                    "Новая защита PUMP работает самостоятельно: высокий риск позднего входа блокирует покупку даже тогда, когда BTC и SOL не растут. Старый общерыночный фильтр остаётся дополнительной страховкой.\n\n" +
                    "Недельный ритм показывается только как предупреждение: выходные обычно тише, понедельник 06–12 склонен к откату, четверг исторически слабее. Календарь сам не создаёт и не отменяет сделку.\n\n" +
                    "При падении PUMP/EUR на 25% и больше от максимума последних 24 часов включается отдельная аварийная тревога. Она не является командой купить: до подтверждённого отскока обычный сигнал покупки блокируется.\n\n" +
                    "95–98 — только отображение приближения без звонка. 99 — звук и вибрация только при допустимом риске позднего входа и достаточной согласованности данных. " +
                    "100 — условия стратегии полностью выполнены. " +
                    "Это готовность правил, а не вероятность прибыли."
            )
            .setPositiveButton("Понятно", null)
            .show()
    }

    private fun confirm(title: String, message: String, action: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Подтвердить") { _, _ -> action() }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateUi() {
        val snapshot = PumpBotEngine.snapshot(this)
        tvStatus?.text = if (snapshot.running) {
            "Монитор включён • обновлено ${PumpBotEngine.formatTime(snapshot.lastSync)}"
        } else {
            "Монитор остановлен • последнее обновление ${PumpBotEngine.formatTime(snapshot.lastSync)}"
        }
        tvMode?.text = if (snapshot.rapidDrop.active) {
            String.format(Locale.GERMANY, "АВАРИЙНОЕ ПАДЕНИЕ −%.1f%% — ПРОВЕРЬТЕ РЫНОК", snapshot.rapidDrop.dropPercent)
        } else if (snapshot.lateEntryBlocked && snapshot.waitMode == "BUY") {
            "ВХОД ЗАБЛОКИРОВАН — ЦЕНА УЖЕ ВЫСОКО"
        } else if (snapshot.marketGateActive && snapshot.waitMode == "BUY") {
            "РЫНОК ПЕРЕГРЕТ — НЕ ДОГОНЯЕМ ЦЕНУ"
        } else if (snapshot.waitMode == "BUY") {
            "ЖДЁМ НОВЫЙ ВХОД СНИЗУ"
        } else {
            "МОНЕТА КУПЛЕНА — ЖДЁМ ВЫХОД"
        }
        tvMode?.setTextColor(Color.parseColor("#F0F6FC"))
        tvMode?.setBackgroundColor(
            Color.parseColor(if (snapshot.rapidDrop.active || snapshot.marketGateActive || snapshot.lateEntryBlocked) "#9E2A2B" else "#30363D")
        )

        renderRapidDrop(snapshot)
        renderReadiness(snapshot)
        renderBreathing(snapshot)

        renderStrategyButtons(snapshot.aggressive)
        renderSignalBox(tvBuySignal, "BUY", snapshot.buySignal, snapshot.waitMode == "BUY")
        renderSignalBox(tvSellSignal, "SELL", snapshot.sellSignal, snapshot.waitMode == "SELL")

        tvPrice?.text = String.format(
            Locale.US,
            "Цена €%.8f • свеча %s\nRSI %.1f • EMA200 %.8f • funding %+.5f%%",
            snapshot.lastPrice,
            PumpBotEngine.formatTime(snapshot.lastCandle),
            snapshot.lastRsi,
            snapshot.lastEma200,
            snapshot.fundingRate * 100.0
        )
        tvReason?.text = if (snapshot.weekRhythm.caution) {
            "${snapshot.signalReason}\n${snapshot.weekRhythm.title}. ${snapshot.weekRhythm.explanation}"
        } else {
            snapshot.signalReason
        }
        tvReason?.setTextColor(
            when {
                snapshot.marketGateActive -> Color.parseColor("#F0B72F")
                snapshot.signalReason.startsWith("СЕЙЧАС НЕ ПОКУПАТЬ") -> Color.parseColor("#FF7B72")
                snapshot.readinessScore >= 95 -> Color.parseColor("#7EE787")
                snapshot.readinessScore <= -95 -> Color.parseColor("#FF7B72")
                else -> Color.parseColor("#C9D1D9")
            }
        )

        tvPosition?.text = if (snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0) {
            String.format(
                Locale.US,
                "Позиция: %s | вход %.8f EUR | максимум %.8f | частично продано: %s",
                friendlyMode(snapshot.strategyMode),
                snapshot.entryPrice,
                snapshot.highestClose,
                if (snapshot.partialTaken) "да" else "нет"
            )
        } else {
            "Сделка: не открыта"
        }
        tvAlertStatus?.text = AlertSchedule.statusText(this)

        btnStart?.isEnabled = !snapshot.running
        btnStart?.alpha = if (snapshot.running) 0.45f else 1f
        btnStop?.isEnabled = snapshot.running
        btnStop?.alpha = if (snapshot.running) 1f else 0.65f
        btnManual?.text = when {
            snapshot.waitMode == "BUY" -> "Я КУПИЛ — ЖДУ ПРОДАЖУ"
            snapshot.signalAction == StrategyV2.ACTION_SELL_HALF && !snapshot.partialTaken -> {
                if (snapshot.strategyMode == StrategyV2.MODE_EXHAUSTION) "Я ПРОДАЛ 40% — ВЕСТИ 60%" else "Я ПРОДАЛ 50% — ВЕСТИ ОСТАТОК"
            }
            else -> "Я ПРОДАЛ — ЖДУ ПОКУПКУ"
        }
        btnToggleMode?.text = "ПОЧЕМУ ТАКОЙ СИГНАЛ?"
        chart?.setData("PUMP/EUR • ДЫХАНИЕ РЫНКА", snapshot.chart)
    }

    private fun renderRapidDrop(snapshot: LiveSnapshot) {
        val drop = snapshot.rapidDrop
        if (!drop.active) {
            tvRapidDrop?.visibility = View.GONE
            return
        }
        tvRapidDrop?.visibility = View.VISIBLE
        val action = when {
            snapshot.waitMode == "SELL" -> "ПОЗИЦИЯ ОТКРЫТА — СРОЧНО ПРОВЕРЬТЕ СТОП И ЦЕНУ ВЫХОДА"
            drop.recoveryConfirmed -> String.format(
                Locale.GERMANY,
                "ОТСКОК +%.1f%% ЕСТЬ, НО ЖДЁМ ОБЫЧНЫЙ СИГНАЛ 99/100",
                drop.reboundPercent
            )
            else -> "ПАДЕНИЕ НЕ ОСТАНОВИЛОСЬ — НЕ ПОКУПАТЬ АВТОМАТИЧЕСКИ"
        }
        tvRapidDrop?.text = String.format(
            Locale.GERMANY,
            "РЕЗКОЕ ПАДЕНИЕ −%.1f%%\nмаксимум €%.8f → сейчас €%.8f\n%s",
            drop.dropPercent,
            drop.peakPrice,
            drop.currentPrice,
            action
        )
    }

    private fun renderBreathing(snapshot: LiveSnapshot) {
        tvBreathingState?.text = "ДЫХАНИЕ: ${snapshot.breathingState}\n${snapshot.marketRelation}"
        tvBreathingState?.setTextColor(
            Color.parseColor(
                when {
                    snapshot.lateEntryBlocked -> "#FF7B72"
                    snapshot.directionScore >= 25 -> "#7EE787"
                    snapshot.directionScore <= -25 -> "#FF7B72"
                    else -> "#79C0FF"
                }
            )
        )
        tvEnergy?.text = "АКТИВНОСТЬ\n${snapshot.energyScore}/100\nсжатие ${snapshot.compressionScore}"
        val direction = if (snapshot.directionScore >= 0) "+${snapshot.directionScore}" else "−${kotlin.math.abs(snapshot.directionScore)}"
        tvDirection?.text = "ПОТОК\n$direction/100\n${if (snapshot.directionScore >= 0) "вверх" else "вниз"}"
        tvDirection?.setTextColor(Color.parseColor(if (snapshot.directionScore >= 20) "#7EE787" else if (snapshot.directionScore <= -20) "#FF7B72" else "#C9D1D9"))
        tvConfidence?.text = "СОГЛАСОВАНО\n${snapshot.breathingConfidence}/100\nне шанс прибыли"
        tvLateRisk?.text = "ПОЗДНИЙ ВХОД\n${snapshot.lateEntryRisk}/100\n${if (snapshot.lateEntryBlocked) "ЗАПРЕЩЁН" else "допустимо"}"
        tvLateRisk?.setTextColor(Color.parseColor(if (snapshot.lateEntryBlocked) "#FF7B72" else if (snapshot.lateEntryRisk >= 45) "#F0B72F" else "#7EE787"))

        val book = snapshot.bookImbalance?.let {
            val side = if (it >= 0.0) "покупатели" else "продавцы"
            "стакан: $side ${String.format(Locale.GERMAN, "%+.0f%%", it * 100.0)}"
        } ?: "стакан: нет данных"
        val spread = snapshot.spreadPercent?.let { "spread ${String.format(Locale.GERMAN, "%.3f%%", it)}" } ?: "spread —"
        val oi = snapshot.openInterestChangePercent?.let { "OI ${String.format(Locale.GERMAN, "%+.2f%%", it)} с прошлой проверки" }
            ?: snapshot.openInterest?.let { "OI собирается для сравнения" }
            ?: "OI: нет данных"
        tvMicrostructure?.text = "$book • $spread • $oi\nСнимок стакана — дополнительное наблюдение, не самостоятельный приказ купить."
    }

    private fun renderReadiness(snapshot: LiveSnapshot) {
        val score = snapshot.readinessScore
        if (snapshot.rapidDrop.active && snapshot.waitMode == "BUY" && !snapshot.rapidDrop.recoveryConfirmed) {
            tvReadiness?.text = "АВАРИЙНЫЙ РЕЖИМ\nПОКУПКА ЕЩЁ НЕ ПОДТВЕРЖДЕНА\nждём остановку падения"
            tvReadiness?.setTextColor(Color.parseColor("#FFFFFF"))
            tvReadiness?.setBackgroundColor(Color.parseColor("#4A1418"))
            return
        }
        if (snapshot.lateEntryBlocked && snapshot.waitMode == "BUY") {
            tvReadiness?.text = "ПОКУПКА ЗАПРЕЩЕНА\nРИСК ПОЗДНЕГО ВХОДА ${snapshot.lateEntryRisk}/100\nждём новый вход снизу"
            tvReadiness?.setTextColor(Color.parseColor("#FF7B72"))
            tvReadiness?.setBackgroundColor(Color.parseColor("#321A1D"))
            return
        }
        if (snapshot.marketGateActive) {
            tvReadiness?.text = "ПАУЗА ПОКУПКИ\nPUMP + BTC + SOL резко выросли за 1 час\nждём новую закрытую свечу"
            tvReadiness?.setTextColor(Color.parseColor("#F0B72F"))
            tvReadiness?.setBackgroundColor(Color.parseColor("#2D240F"))
            return
        }
        tvReadiness?.text = when {
            score >= 100 -> "+100  ПОКУПАТЬ\nУСЛОВИЯ ПОДТВЕРЖДЕНЫ"
            score == 99 -> "+99  ЗВОНОК: ПРИГОТОВИТЬСЯ\nдо покупки остался 1 балл"
            score >= 95 -> "+$score  СИГНАЛ ПРИБЛИЖАЕТСЯ\nзвонок будет на 99"
            score <= -100 -> "−100  ПРОДАВАТЬ\nУСЛОВИЯ ПОДТВЕРЖДЕНЫ"
            score == -99 -> "−99  ЗВОНОК: ПРИГОТОВИТЬСЯ\nдо продажи остался 1 балл"
            score <= -95 -> "−${kotlin.math.abs(score)}  СИГНАЛ ПРИБЛИЖАЕТСЯ\nзвонок будет на −99"
            score < 0 -> "ПРОДАЖА НЕ ПОДТВЕРЖДЕНА\nготовность ${kotlin.math.abs(score)}/100"
            else -> "ПОКУПКА НЕ ПОДТВЕРЖДЕНА\nготовность $score/100"
        }
        tvReadiness?.setTextColor(
            when {
                score >= 95 -> Color.parseColor("#7EE787")
                score <= -95 -> Color.parseColor("#FF7B72")
                else -> Color.parseColor("#F0B72F")
            }
        )
        tvReadiness?.setBackgroundColor(if (kotlin.math.abs(score) >= 95) Color.parseColor("#202A22") else Color.parseColor("#161B22"))
    }

    private fun renderStrategyButtons(aggressive: Boolean) {
        btnRisk30?.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (!aggressive) "#238636" else "#30363D"))
        btnRisk35?.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (aggressive) "#B62324" else "#30363D"))
        btnRisk30?.alpha = if (!aggressive) 1f else 0.72f
        btnRisk35?.alpha = if (aggressive) 1f else 0.72f
        btnRisk30?.text = "ОСТОРОЖНЫЙ\nБЛИЖЕ КО ДНУ\nМЕНЬШЕ ВХОДОВ"
        btnRisk35?.text = "АКТИВНЫЙ\nЛОВИТ РАЗВОРОТ\nБОЛЬШЕ ВХОДОВ"
    }

    private fun friendlyMode(mode: String): String {
        return when (mode) {
            StrategyV2.MODE_EXHAUSTION -> "4-этапный разворот"
            StrategyV2.MODE_SHOCK -> "импульс после падения"
            StrategyV2.MODE_TREND -> "трендовый вход"
            else -> "ожидание"
        }
    }

    private fun renderSignalBox(view: TextView?, label: String, signal: Boolean, selectedMode: Boolean) {
        val color = when {
            signal && label == "BUY" -> "#238636"
            signal && label == "SELL" -> "#DA3633"
            selectedMode -> "#30363D"
            else -> "#161B22"
        }
        view?.setBackgroundColor(Color.parseColor(color))
        val display = if (label == "BUY") "Покупка" else "Продажа"
        view?.text = if (signal) "$display: СИГНАЛ" else "$display: нет"
        view?.setTextColor(if (signal) Color.WHITE else Color.parseColor("#8B949E"))
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 350)
        }
    }
}
