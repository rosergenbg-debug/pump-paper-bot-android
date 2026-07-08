package com.example.pumppaperbot

import android.app.AlertDialog
import android.content.Intent
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

    private var tvGlobalStatus: TextView? = null
    private var tvPrimaryTitle: TextView? = null
    private var tvPrimaryBalance: TextView? = null
    private var tvPrimaryMeta: TextView? = null
    private var tvPrimaryDecision: TextView? = null
    private var tvExperimentalTitle: TextView? = null
    private var tvExperimentalBalance: TextView? = null
    private var tvExperimentalMeta: TextView? = null
    private var tvExperimentalDecision: TextView? = null
    private var tvTradeLog: TextView? = null
    private var chartPrimary: StrategyChartView? = null
    private var chartExperimental: StrategyChartView? = null
    private var btnStart: Button? = null
    private var btnStop: Button? = null
    private var btnReset: Button? = null
    private var btnCheck: Button? = null
    private var btnStats: Button? = null
    private var btnBacktest: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvGlobalStatus = findViewById(R.id.tvGlobalStatus)
        tvPrimaryTitle = findViewById(R.id.tvPrimaryTitle)
        tvPrimaryBalance = findViewById(R.id.tvPrimaryBalance)
        tvPrimaryMeta = findViewById(R.id.tvPrimaryMeta)
        tvPrimaryDecision = findViewById(R.id.tvPrimaryDecision)
        tvExperimentalTitle = findViewById(R.id.tvExperimentalTitle)
        tvExperimentalBalance = findViewById(R.id.tvExperimentalBalance)
        tvExperimentalMeta = findViewById(R.id.tvExperimentalMeta)
        tvExperimentalDecision = findViewById(R.id.tvExperimentalDecision)
        tvTradeLog = findViewById(R.id.tvTradeLog)
        chartPrimary = findViewById(R.id.chartPrimary)
        chartExperimental = findViewById(R.id.chartExperimental)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnReset = findViewById(R.id.btnReset)
        btnCheck = findViewById(R.id.btnCheck)
        btnStats = findViewById(R.id.btnStats)
        btnBacktest = findViewById(R.id.btnBacktest)

        btnStart?.setOnClickListener { startBot() }
        btnCheck?.setOnClickListener { checkNow() }
        btnStats?.setOnClickListener { startActivity(Intent(this, StatsActivity::class.java)) }
        btnBacktest?.setOnClickListener { startActivity(Intent(this, BacktestActivity::class.java)) }
        btnStop?.setOnClickListener {
            confirm("Stop simulation?", "Virtual trading will pause. Current balances and positions stay saved.") {
                stopBot()
            }
        }
        btnReset?.setOnClickListener {
            confirm("Reset everything?", "Both virtual accounts, positions and trade logs will be cleared.") {
                resetBot()
            }
        }

        PumpBotEngine.ensureInitialized(this)
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
        checkNow()
    }

    private fun checkNow() {
        WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<PumpBotWorker>().build())
        handler.postDelayed({ updateUi() }, 2500)
        handler.postDelayed({ updateUi() }, 6000)
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
        val updated = PumpBotEngine.formatTime(snapshot.lastSync)
        tvGlobalStatus?.text = if (snapshot.running) {
            "Status: running. Last sync: $updated. Android checks about every 15 min; missed candles are backfilled."
        } else {
            "Status: stopped. Last sync: $updated. CHECK refreshes charts without virtual trades."
        }
        btnStart?.isEnabled = !snapshot.running
        btnStart?.alpha = if (snapshot.running) 0.45f else 1f
        btnStop?.isEnabled = snapshot.running
        btnStop?.alpha = if (snapshot.running) 1f else 0.55f

        renderStrategy(
            result = snapshot.primary,
            titleView = tvPrimaryTitle,
            balanceView = tvPrimaryBalance,
            metaView = tvPrimaryMeta,
            decisionView = tvPrimaryDecision
        )
        renderStrategy(
            result = snapshot.experimental,
            titleView = tvExperimentalTitle,
            balanceView = tvExperimentalBalance,
            metaView = tvExperimentalMeta,
            decisionView = tvExperimentalDecision
        )

        chartPrimary?.setData("PRIMARY 4H", snapshot.primaryChart)
        chartExperimental?.setData("EXPERIMENT 2H", snapshot.experimentalChart)
        tvTradeLog?.text = tradeLog(snapshot)
    }

    private fun renderStrategy(
        result: StrategyResult,
        titleView: TextView?,
        balanceView: TextView?,
        metaView: TextView?,
        decisionView: TextView?
    ) {
        val state = result.state
        titleView?.text = "${state.title}  ${state.buys} BUY / ${state.sells} SELL"
        balanceView?.text = String.format(Locale.US, "%.2f USDT  (%+.2f%%)", result.equity, result.profitPercent)
        balanceView?.setTextColor(if (result.profit >= 0.0) Color.parseColor("#32C789") else Color.parseColor("#FF4D6D"))
        val position = if (state.coins > 0.0) {
            String.format(Locale.US, "Position: %.2f PUMP @ %.8f", state.coins, state.entryPrice)
        } else {
            "Position: none"
        }
        metaView?.text = "$position | Last price: ${formatPrice(state.lastPrice)}"
        decisionView?.text = "${state.lastAction} | ${PumpBotEngine.formatTime(state.lastUpdated)} | ${state.lastReason}"
    }

    private fun tradeLog(snapshot: BotSnapshot): String {
        val rows = mutableListOf<String>()
        rows.add("Recent trades")
        rows.addAll(snapshot.primary.state.trades.takeLast(6).reversed().map {
            "4H ${PumpBotEngine.formatTime(it.time)} ${it.action} ${formatPrice(it.price)} equity ${String.format(Locale.US, "%.2f", it.equity)}"
        })
        rows.addAll(snapshot.experimental.state.trades.takeLast(6).reversed().map {
            "2H ${PumpBotEngine.formatTime(it.time)} ${it.action} ${formatPrice(it.price)} equity ${String.format(Locale.US, "%.2f", it.equity)}"
        })
        return if (rows.size == 1) "Trade log is empty." else rows.joinToString("\n")
    }

    private fun formatPrice(value: Double): String {
        return if (value > 0.0) String.format(Locale.US, "%.8f", value) else "-"
    }
}
