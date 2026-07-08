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
        btnStart = findViewById(R.id.btnStart)
        btnCheck = findViewById(R.id.btnCheck)
        btnStop = findViewById(R.id.btnStop)
        btnReset = findViewById(R.id.btnReset)
        btnManual = findViewById(R.id.btnManual)
        btnToggleMode = findViewById(R.id.btnToggleMode)
        btnBacktest = findViewById(R.id.btnBacktest)

        PumpBotEngine.ensureInitialized(this)
        requestNotificationPermission()

        btnStart?.setOnClickListener { startMonitor() }
        btnCheck?.setOnClickListener { checkNow() }
        btnStop?.setOnClickListener {
            confirm("Stop monitor?", "PUMP checks and alarms will stop.") {
                stopMonitor()
            }
        }
        btnReset?.setOnClickListener {
            confirm("Reset state?", "This clears wait mode, entry price and saved chart data.") {
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
        val serviceIntent = Intent(this, PumpSignalService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
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
            confirm("Confirm BUY?", "The app will remember this price and start waiting for SELL.") {
                PumpBotEngine.confirmBought(this)
                updateUi()
            }
        } else {
            confirm("Confirm SELL?", "The app will clear the entry price and start waiting for BUY.") {
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
            .setPositiveButton("Confirm") { _, _ -> action() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateUi() {
        val snapshot = PumpBotEngine.snapshot(this)
        val active = snapshot.signalAction == snapshot.waitMode &&
            (snapshot.signalAction == "BUY" || snapshot.signalAction == "SELL")
        val status = if (snapshot.running) "RUNNING" else "STOPPED"
        tvStatus?.text = "$status | Last sync: ${PumpBotEngine.formatTime(snapshot.lastSync)} | Checks every 2 min while monitor is running"
        tvMode?.text = if (snapshot.waitMode == "BUY") "MODE: waiting for BUY" else "MODE: waiting for SELL"
        tvMode?.setTextColor(if (active) Color.WHITE else Color.parseColor("#C9D1D9"))
        tvMode?.setBackgroundColor(if (active) Color.parseColor("#DA3633") else Color.parseColor("#30363D"))

        renderSignalBox(tvBuySignal, "BUY", snapshot.buySignal, snapshot.waitMode == "BUY")
        renderSignalBox(tvSellSignal, "SELL", snapshot.sellSignal, snapshot.waitMode == "SELL")

        tvPrice?.text = String.format(
            Locale.US,
            "PUMP price %.8f | RSI %.1f | EMA200 %.8f | Candle %s",
            snapshot.lastPrice,
            snapshot.lastRsi,
            snapshot.lastEma200,
            PumpBotEngine.formatTime(snapshot.lastCandle)
        )
        tvReason?.text = snapshot.signalReason
        tvReason?.setTextColor(if (active) Color.parseColor("#FF4D6D") else Color.parseColor("#8B949E"))

        tvPosition?.text = if (snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0) {
            String.format(Locale.US, "Manual position: bought around %.8f | highest %.8f", snapshot.entryPrice, snapshot.highestClose)
        } else {
            "Manual position: none"
        }

        btnStart?.isEnabled = !snapshot.running
        btnStart?.alpha = if (snapshot.running) 0.45f else 1f
        btnStop?.isEnabled = snapshot.running
        btnStop?.alpha = if (snapshot.running) 1f else 0.65f
        btnManual?.text = if (snapshot.waitMode == "BUY") "I BOUGHT - WAIT SELL" else "I SOLD - WAIT BUY"
        btnToggleMode?.text = if (snapshot.waitMode == "BUY") "SWITCH TO SELL" else "SWITCH TO BUY"
        chart?.setData("PUMP RSI35", snapshot.chart)
    }

    private fun renderSignalBox(view: TextView?, label: String, signal: Boolean, selectedMode: Boolean) {
        val color = when {
            signal && label == "BUY" -> "#238636"
            signal && label == "SELL" -> "#DA3633"
            selectedMode -> "#30363D"
            else -> "#161B22"
        }
        view?.setBackgroundColor(Color.parseColor(color))
        view?.text = if (signal) "$label NOW" else "$label idle"
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
