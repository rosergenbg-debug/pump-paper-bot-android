package com.example.pumppaperbot

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private var tvPrice: TextView? = null
    private var tvReason: TextView? = null
    private var tvPosition: TextView? = null
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvBuySignal = findViewById(R.id.tvBuySignal)
        tvSellSignal = findViewById(R.id.tvSellSignal)
        tvMode = findViewById(R.id.tvMode)
        tvPrice = findViewById(R.id.tvPrice)
        tvReason = findViewById(R.id.tvReason)
        tvPosition = findViewById(R.id.tvPosition)
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

        PumpBotEngine.ensureInitialized(this)
        requestNotificationPermission()

        btnRisk30?.isEnabled = false
        btnRisk35?.isEnabled = false
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
        btnToggleMode?.setOnClickListener { toggleMode() }
        btnBacktest?.setOnClickListener { startActivity(Intent(this, BacktestActivity::class.java)) }

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
            confirm("Подтвердить продажу 50%?", "Оставшаяся половина будет защищена безубытком и trailing-stop 4%.") {
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

    private fun toggleMode() {
        val snapshot = PumpBotEngine.snapshot(this)
        val next = if (snapshot.waitMode == "BUY") "SELL" else "BUY"
        PumpBotEngine.setWaitMode(this, next)
        updateUi()
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
        val active = (snapshot.waitMode == "BUY" && snapshot.signalAction == "BUY") ||
            (snapshot.waitMode == "SELL" &&
                (snapshot.signalAction == "SELL" || snapshot.signalAction == StrategyV2.ACTION_SELL_HALF))
        val status = if (snapshot.running) "РАБОТАЕТ" else "ОСТАНОВЛЕНО"
        tvStatus?.text = "$status | V2 1.3 | Проверка: ${PumpBotEngine.formatTime(snapshot.lastSync)}"
        tvMode?.text = if (snapshot.waitMode == "BUY") "Режим: жду покупку" else "Режим: жду продажу"
        tvMode?.setTextColor(if (active) Color.WHITE else Color.parseColor("#C9D1D9"))
        tvMode?.setBackgroundColor(if (active) Color.parseColor("#DA3633") else Color.parseColor("#30363D"))

        renderStrategyButtons()
        renderSignalBox(tvBuySignal, "BUY", snapshot.buySignal, snapshot.waitMode == "BUY")
        renderSignalBox(tvSellSignal, "SELL", snapshot.sellSignal, snapshot.waitMode == "SELL")

        tvPrice?.text = String.format(
            Locale.US,
            "PUMP/EUR %.8f | RSI %.1f | EMA200 %.8f | funding %+.5f%% | %s",
            snapshot.lastPrice,
            snapshot.lastRsi,
            snapshot.lastEma200,
            snapshot.fundingRate * 100.0,
            PumpBotEngine.formatTime(snapshot.lastCandle)
        )
        tvReason?.text = snapshot.signalReason
        tvReason?.setTextColor(if (active) Color.parseColor("#FF4D6D") else Color.parseColor("#8B949E"))

        tvPosition?.text = if (snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0) {
            String.format(
                Locale.US,
                "Позиция: %s | вход %.8f EUR | максимум %.8f | 50%% продано: %s",
                snapshot.strategyMode,
                snapshot.entryPrice,
                snapshot.highestClose,
                if (snapshot.partialTaken) "да" else "нет"
            )
        } else {
            "Позиция: нет"
        }

        btnStart?.isEnabled = !snapshot.running
        btnStart?.alpha = if (snapshot.running) 0.45f else 1f
        btnStop?.isEnabled = snapshot.running
        btnStop?.alpha = if (snapshot.running) 1f else 0.65f
        btnManual?.text = when {
            snapshot.waitMode == "BUY" -> "Я КУПИЛ - ЗАПОМНИТЬ ВХОД"
            snapshot.signalAction == StrategyV2.ACTION_SELL_HALF && !snapshot.partialTaken -> "Я ПРОДАЛ 50% - ВЕСТИ ОСТАТОК"
            else -> "Я ПРОДАЛ ВСЁ - ЖДАТЬ ПОКУПКУ"
        }
        btnToggleMode?.text = if (snapshot.waitMode == "BUY") "ЖДАТЬ ПРОДАЖУ" else "ЖДАТЬ ПОКУПКУ"
        chart?.setData("PUMP/EUR V2", snapshot.chart)
    }

    private fun renderStrategyButtons() {
        btnRisk30?.setBackgroundColor(Color.parseColor("#1F6FEB"))
        btnRisk35?.setBackgroundColor(Color.parseColor("#8957E5"))
        btnRisk30?.text = "V2 ТРЕНД\nцель +8%"
        btnRisk35?.text = "V2 ШОК\n50% при +6%"
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
        view?.text = if (signal) "$display СЕЙЧАС" else "$display: нет"
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
