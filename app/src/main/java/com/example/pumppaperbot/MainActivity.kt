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
    private var tvMode4h: TextView? = null
    private var tvMode2h: TextView? = null
    private var tvSelectedStatsTitle: TextView? = null
    private var tvSelectedStatsSummary: TextView? = null
    private var tvSelectedStatsDetails: TextView? = null
    private var tvSelectedStatsTrades: TextView? = null
    private var chartPrimary: StrategyChartView? = null
    private var chartExperimental: StrategyChartView? = null
    private var btnStart: Button? = null
    private var btnStop: Button? = null
    private var btnReset: Button? = null
    private var btnCheck: Button? = null
    private var btnBacktest: Button? = null
    private var selectedStats = "primary"

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
        tvMode4h = findViewById(R.id.tvMode4h)
        tvMode2h = findViewById(R.id.tvMode2h)
        tvSelectedStatsTitle = findViewById(R.id.tvSelectedStatsTitle)
        tvSelectedStatsSummary = findViewById(R.id.tvSelectedStatsSummary)
        tvSelectedStatsDetails = findViewById(R.id.tvSelectedStatsDetails)
        tvSelectedStatsTrades = findViewById(R.id.tvSelectedStatsTrades)
        chartPrimary = findViewById(R.id.chartPrimary)
        chartExperimental = findViewById(R.id.chartExperimental)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnReset = findViewById(R.id.btnReset)
        btnCheck = findViewById(R.id.btnCheck)
        btnBacktest = findViewById(R.id.btnBacktest)

        tvMode4h?.setOnClickListener {
            selectedStats = "primary"
            updateUi()
        }
        tvMode2h?.setOnClickListener {
            selectedStats = "experiment"
            updateUi()
        }
        btnStart?.setOnClickListener { startBot() }
        btnCheck?.setOnClickListener { checkNow() }
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

        chartPrimary?.setData("30m RSI", snapshot.primaryChart)
        chartExperimental?.setData("2h SUPER", snapshot.experimentalChart)
        renderSelectedStats(snapshot)
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

    private fun renderSelectedStats(snapshot: BotSnapshot) {
        val result = if (selectedStats == "experiment") snapshot.experimental else snapshot.primary
        val selectedColor = Color.parseColor("#1F6FEB")
        val idleColor = Color.parseColor("#30363D")
        tvMode4h?.setBackgroundColor(if (selectedStats == "primary") selectedColor else idleColor)
        tvMode2h?.setBackgroundColor(if (selectedStats == "experiment") selectedColor else idleColor)

        tvSelectedStatsTitle?.text = if (selectedStats == "experiment") "2h SUPER STATISTICS" else "30m RSI STATISTICS"
        tvSelectedStatsSummary?.text = String.format(
            Locale.US,
            "Started: %s | Invested: %.2f USDT | Now: %.2f USDT | P/L: %+.2f USDT (%+.2f%%)",
            PumpBotEngine.formatTime(snapshot.startedAt),
            PumpBotEngine.startBalance,
            result.equity,
            result.profit,
            result.profitPercent
        )
        tvSelectedStatsSummary?.setTextColor(if (result.profit >= 0.0) Color.parseColor("#32C789") else Color.parseColor("#FF4D6D"))
        tvSelectedStatsDetails?.text = String.format(
            Locale.US,
            "Trades: %d | BUY %d / SELL %d | Fees paid: %.2f USDT | Fee: %.2f%%",
            result.tradeCount,
            result.state.buys,
            result.state.sells,
            result.totalFees,
            PumpBotEngine.feeRate * 100.0
        )
        tvSelectedStatsTrades?.text = selectedTradeRows(result)
    }

    private fun selectedTradeRows(result: StrategyResult): String {
        val trades = result.state.trades.takeLast(5).reversed()
        if (trades.isEmpty()) return "No trades yet."
        return trades.joinToString("\n----------------\n") {
            val pnl = if (it.action == "SELL") String.format(Locale.US, " | P/L %+.2f", it.pnl) else ""
            String.format(
                Locale.US,
                "%s %s | price %.8f | amount %.2f | fee %.2f | balance %.2f%s",
                PumpBotEngine.formatTime(it.time),
                it.action,
                it.price,
                it.amount,
                it.fee,
                it.equity,
                pnl
            )
        }
    }

    private fun formatPrice(value: Double): String {
        return if (value > 0.0) String.format(Locale.US, "%.8f", value) else "-"
    }
}
