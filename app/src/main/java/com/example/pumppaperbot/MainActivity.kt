package com.example.pumppaperbot

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val refreshUi = object : Runnable {
        override fun run() {
            updateUi()
            handler.postDelayed(this, 10_000)
        }
    }

    private var tvStatus: TextView? = null
    private var tvBalance: TextView? = null
    private var tvProfit: TextView? = null
    private var tvPosition: TextView? = null
    private var tvPrice: TextView? = null
    private var tvTrades: TextView? = null
    private var tvLastDecision: TextView? = null
    private var tvTradeLog: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvBalance = findViewById(R.id.tvBalance)
        tvProfit = findViewById(R.id.tvProfit)
        tvPosition = findViewById(R.id.tvPosition)
        tvPrice = findViewById(R.id.tvPrice)
        tvTrades = findViewById(R.id.tvTrades)
        tvLastDecision = findViewById(R.id.tvLastDecision)
        tvTradeLog = findViewById(R.id.tvTradeLog)

        findViewById<Button>(R.id.btnStart).setOnClickListener { startBot() }
        findViewById<Button>(R.id.btnStop).setOnClickListener { stopBot() }
        findViewById<Button>(R.id.btnReset).setOnClickListener { resetBot() }
        findViewById<Button>(R.id.btnCheck).setOnClickListener { checkNow() }

        PumpBotEngine.ensureInitialized(this)
        updateUi()
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

    private fun startBot() {
        PumpBotEngine.setRunning(this, true)
        val request = PeriodicWorkRequestBuilder<PumpBotWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PumpBotEngine.uniqueWorkName,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
        checkNow()
    }

    private fun stopBot() {
        PumpBotEngine.setRunning(this, false)
        WorkManager.getInstance(this).cancelUniqueWork(PumpBotEngine.uniqueWorkName)
        updateUi()
    }

    private fun resetBot() {
        PumpBotEngine.reset(this)
        WorkManager.getInstance(this).cancelUniqueWork(PumpBotEngine.uniqueWorkName)
        updateUi()
    }

    private fun checkNow() {
        WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<PumpBotWorker>().build())
        handler.postDelayed({ updateUi() }, 3000)
        updateUi()
    }

    private fun updateUi() {
        val state = PumpBotEngine.load(this)
        val result = PumpBotEngine.result(state)
        val updated = if (state.lastUpdated > 0L) {
            SimpleDateFormat("dd.MM HH:mm", Locale.GERMAN).format(Date(state.lastUpdated))
        } else {
            "-"
        }

        tvStatus?.text = if (state.running) {
            "Status: running, checks about every 15 minutes"
        } else {
            "Status: stopped"
        }
        tvBalance?.text = String.format(Locale.US, "Balance: %.2f USDT", result.equity)
        tvProfit?.text = String.format(Locale.US, "Profit/Loss: %+.2f USDT (%+.2f%%)", result.profit, result.profitPercent)
        tvProfit?.setTextColor(if (result.profit >= 0.0) Color.parseColor("#32C789") else Color.parseColor("#FF4D6D"))
        tvPosition?.text = if (state.coins > 0.0) {
            String.format(Locale.US, "Position: %.2f PUMP @ %.8f", state.coins, state.entryPrice)
        } else {
            "Position: none"
        }
        tvPrice?.text = if (state.lastPrice > 0.0) {
            String.format(Locale.US, "Last PUMP price: %.8f USDT", state.lastPrice)
        } else {
            "Last PUMP price: -"
        }
        tvTrades?.text = "Trades: BUY ${state.buys} / SELL ${state.sells}"
        tvLastDecision?.text = "Last decision: ${state.lastAction} | $updated | ${state.lastReason}"
        tvTradeLog?.text = state.tradeLog.ifBlank { "Trade log is empty." }
    }
}
