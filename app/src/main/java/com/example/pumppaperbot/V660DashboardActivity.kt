package com.example.pumppaperbot

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale

/** Clean V6.6 owner surface. Legacy screens remain in the APK only as dormant history/tools. */
class V660DashboardActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var monitorButton: Button
    private lateinit var readinessBar: ProgressBar
    private lateinit var readinessText: TextView
    private lateinit var contextText: TextView
    private lateinit var coreText: TextView
    private lateinit var btcText: TextView
    private lateinit var solText: TextView
    private lateinit var humanText: TextView
    private lateinit var approveButton: Button
    private lateinit var rejectButton: Button
    private lateinit var alertButton: Button

    private val refresh = object : Runnable {
        override fun run() {
            render()
            handler.postDelayed(this, 2_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(18))
            setBackgroundColor(Color.parseColor("#0D1117"))
        }
        root.addView(label("PUMP V6.6", 28, "#F0F6FC", true))
        root.addView(label("3 AUTO + HUMAN • X-алгоритм • PAPER ONLY", 15, "#7EE787", true))
        root.addView(label("Новые счета стартуют с €1000 и нулевой историей. Старые V6.5 стратегии не запускаются фоновым сервисом.", 13, "#8B949E", false))

        monitorButton = button("", "#238636").apply {
            setOnClickListener {
                if (PumpBotEngine.isRunning(this@V660DashboardActivity)) {
                    PumpBotEngine.setRunning(this@V660DashboardActivity, false)
                    stopService(Intent(this@V660DashboardActivity, PumpSignalService::class.java))
                    HumanFactorAlarmV650.cancel(this@V660DashboardActivity)
                } else {
                    PumpBotEngine.setRunning(this@V660DashboardActivity, true)
                    val intent = Intent(this@V660DashboardActivity, PumpSignalService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ContextCompat.startForegroundService(this@V660DashboardActivity, intent)
                    else startService(intent)
                }
                render()
            }
        }
        root.addView(monitorButton, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(10) })

        root.addView(label("ЖИВАЯ ГОТОВНОСТЬ К ВХОДУ", 17, "#F0F6FC", true))
        readinessBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressTintList = ColorStateList.valueOf(Color.parseColor("#58A6FF"))
            progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor("#30363D"))
        }
        root.addView(readinessBar, LinearLayout.LayoutParams(-1, dp(24)))
        readinessText = label("0 / 100", 30, "#58A6FF", true).apply { gravity = Gravity.CENTER_HORIZONTAL }
        root.addView(readinessText)
        contextText = label("", 14, "#C9D1D9", false)
        root.addView(contextText)
        root.addView(label("Шкала не является отдельным BUY-сигналом: она показывает, насколько рынок постепенно приближается к точному T32/X setup. Автовход всё равно требует закрытую свечу и жёсткие условия.", 12, "#8B949E", false))

        coreText = card(root, "AUTO CORE")
        btcText = card(root, "AUTO BTC GUARD")
        solText = card(root, "AUTO SOL/BTC SELECT")
        humanText = card(root, "HUMAN SELECT")

        val humanButtons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        approveButton = button("ВОЙТИ HUMAN", "#238636").apply {
            setOnClickListener {
                HumanFactorStore.approve(this@V660DashboardActivity)
                render()
            }
        }
        rejectButton = button("ОТКЛОНИТЬ", "#8E1519").apply {
            setOnClickListener {
                HumanFactorStore.reject(this@V660DashboardActivity)
                render()
            }
        }
        humanButtons.addView(approveButton, LinearLayout.LayoutParams(0, dp(58), 1f))
        humanButtons.addView(rejectButton, LinearLayout.LayoutParams(0, dp(58), 1f).apply { leftMargin = dp(8) })
        root.addView(humanButtons, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(8) })

        alertButton = button("ЗВОНКИ И РАСПИСАНИЕ", "#1F6FEB").apply {
            setOnClickListener { startActivity(Intent(this@V660DashboardActivity, AlertSettingsActivity::class.java)) }
        }
        root.addView(alertButton, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(12) })
        root.addView(button("4 ГРАФИКА • СРАВНИТЬ СЧЕТА", "#8250DF").apply {
            setOnClickListener { startActivity(Intent(this@V660DashboardActivity, CompetitionActivity::class.java)) }
        }, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(8) })
        root.addView(button("РЫНОК / СТАРЫЙ ЭКРАН (ТОЛЬКО ПРОСМОТР)", "#30363D").apply {
            setOnClickListener { startActivity(Intent(this@V660DashboardActivity, MainActivity::class.java)) }
        }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(8) })
        root.addView(label("AUTO: TP +2,5% NET • STOP −1,2% NET • TIME 120m • max 2 входа/UTC сутки. HUMAN: вход только после вашей кнопки, выход автоматический по тем же правилам.", 13, "#F0B72F", true))

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(refresh)
        handler.post(refresh)
    }

    override fun onPause() {
        handler.removeCallbacks(refresh)
        super.onPause()
    }

    private fun render() {
        val running = PumpBotEngine.isRunning(this)
        monitorButton.text = if (running) "МОНИТОР V6.6: ВКЛЮЧЕН • НАЖАТЬ STOP" else "МОНИТОР V6.6: ВЫКЛЮЧЕН • НАЖАТЬ START"
        monitorButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (running) "#238636" else "#8E1519"))

        val setup = runCatching { T32V660Policy.evaluate(this) }.getOrNull()
        val score = setup?.readiness ?: 0
        readinessBar.progress = score
        readinessText.text = "$score / 100"
        readinessText.setTextColor(Color.parseColor(when {
            score >= 90 -> "#FF7B72"
            score >= 80 -> "#F0B72F"
            score >= 60 -> "#D2A8FF"
            else -> "#58A6FF"
        }))
        contextText.text = setup?.reason ?: "Нет готовых минутных данных"

        val now = System.currentTimeMillis()
        val venue = BitpandaFusionStore.state(this)
        val fallback = PaperExecutionPolicy.displayPrice(PumpBotEngine.snapshot(this), now)
        val price = venue.bid.takeIf { venue.fresh(now) } ?: fallback
        renderAuto(coreText, V660CoreStore.state(this), price)
        renderAuto(btcText, V660BtcGuardStore.state(this), price)
        renderAuto(solText, V660SolSelectStore.state(this), price)

        val human = HumanFactorStore.state(this)
        humanText.text = String.format(
            Locale.GERMANY,
            "HUMAN SELECT\n€%.2f • %+.2f%% • %s • готовность %d/100\n%s",
            human.value(price),
            (human.value(price) / 1000.0 - 1.0) * 100.0,
            if (human.inPosition) "В PUMP" else "В ЕВРО",
            human.readiness,
            human.reason.take(240)
        )
        approveButton.isEnabled = human.pending && !human.inPosition
        approveButton.alpha = if (approveButton.isEnabled) 1f else 0.42f
        rejectButton.isEnabled = human.pending
        rejectButton.alpha = if (rejectButton.isEnabled) 1f else 0.42f

        val enabled = ResearchModePolicy.alertsEnabled(this)
        alertButton.text = if (enabled) "ЗВОНКИ: ВКЛ • ${AlertSchedule.statusText(this)}" else "ЗВОНКИ: ВЫКЛ • НАЖАТЬ НАСТРОИТЬ"
        alertButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (enabled) "#1F6FEB" else "#8E1519"))
    }

    private fun renderAuto(view: TextView, state: T32V660AutoState, price: Double) {
        val value = state.value(price)
        view.text = String.format(
            Locale.GERMANY,
            "%s\n€%.2f • %+.2f%% • %s • готовность %d/100\n%s",
            view.tag as String,
            value,
            (value / 1000.0 - 1.0) * 100.0,
            if (state.inPosition) "В PUMP" else "В ЕВРО",
            state.readiness,
            state.reason.take(240)
        )
    }

    private fun card(parent: LinearLayout, title: String): TextView {
        val view = label(title, 15, "#F0F6FC", true).apply {
            tag = title
            setBackgroundColor(Color.parseColor("#161B22"))
            setPadding(dp(10), dp(9), dp(10), dp(9))
        }
        parent.addView(view, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        return view
    }

    private fun button(text: String, color: String) = Button(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor(color))
        textSize = 13f
        isAllCaps = false
        gravity = Gravity.CENTER
    }

    private fun label(text: String, size: Int, color: String, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(Color.parseColor(color))
        setPadding(0, dp(5), 0, dp(5))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
