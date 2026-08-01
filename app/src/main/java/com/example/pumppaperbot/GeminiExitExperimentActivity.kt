package com.example.pumppaperbot

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class GeminiExitExperimentActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var account: TextView
    private lateinit var evidence: TextView
    private lateinit var history: TextView
    private val refresh = object : Runnable {
        override fun run() {
            render()
            handler.postDelayed(this, 5_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(16))
            setBackgroundColor(Color.parseColor("#0D1117"))
        }
        root.addView(Button(this).apply {
            text = "←  GEMINI‑ЭКСПЕРИМЕНТ"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#9A6700"))
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(-1, dp(48)))
        root.addView(label(
            "Вход: копия Gemini или ранний подтверждённый сигнал. Выход: по подтверждённому ослаблению рынка.",
            14,
            "#C9D1D9",
            false
        ), params(8))
        account = panel(22)
        evidence = panel(15)
        history = panel(14)
        root.addView(account, params(10))
        root.addView(evidence, params(8))
        root.addView(Button(this).apply {
            text = "СРАВНИТЬ ВСЕХ ЧЕТЫРЁХ"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1F6FEB"))
            setOnClickListener {
                startActivity(Intent(this@GeminiExitExperimentActivity, CompetitionActivity::class.java))
            }
        }, params(8).apply { height = dp(50) })
        root.addView(history, params(8))
        setContentView(ScrollView(this).apply { addView(root) })
        render()
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
        val price = PumpBotEngine.snapshot(this).lastPrice
        val state = GeminiExitExperimentStore.state(this)
        if (state == null) {
            account.text = "Эксперимент запустится при следующем цикле мониторинга"
            evidence.text = "Пока нет первой контрольной точки"
            history.text = "Сделок эксперимента пока нет"
            return
        }
        val p = state.portfolio
        account.text = String.format(
            Locale.GERMANY,
            "€%.2f   %+.2f%%\n%s",
            p.value(price),
            p.profitPercent(price),
            if (p.inPosition) "В PUMP • вход €%.8f" else "В ЕВРО • анализируем ранний вход"
        )
        account.setTextColor(Color.parseColor(if (p.profitPercent(price) >= 0.0) "#3FB950" else "#FF7B72"))
        evidence.text = buildString {
            append("ПОСЛЕДНЯЯ ПРОВЕРКА ${if (state.lastPhase == "ENTRY") "ВХОДА" else "ВЫХОДА"}: ${state.lastSignal}\n")
            if (state.lastPhase == "ENTRY") {
                append("Подтверждение ${state.lastScore}/9 • независимых групп ${state.lastGroups}/6")
            } else {
                append("Опасность ${state.lastScore}/11 • независимых групп ${state.lastGroups}/7")
            }
            if (state.lastPhase == "EXIT" && state.dangerStreak > 0) append(" • подтверждение ${state.dangerStreak}/2")
            if (state.lastPhase == "EXIT" && state.adaptivePullbackPercent > 0.0) {
                append(String.format(Locale.GERMANY, "\nДопустимый шум сейчас около %.2f%%", state.adaptivePullbackPercent))
            }
            append("\n${state.lastReason}")
        }
        val recent = p.trades.takeLast(12).reversed()
        history.text = if (recent.isEmpty()) {
            "СДЕЛКИ ЭКСПЕРИМЕНТА\nПока нет сделок"
        } else {
            buildString {
                append("СДЕЛКИ ЭКСПЕРИМЕНТА")
                recent.forEach { trade ->
                    append("\n\n${trade.action} • ${PumpBotEngine.formatTime(trade.time)}")
                    append(String.format(Locale.GERMANY, " • €%.8f", trade.price))
                    if (trade.action == "SELL") append(String.format(Locale.GERMANY, " • %+.2f €", trade.pnlEur))
                    append("\n${trade.reason}")
                }
            }
        }
    }

    private fun panel(size: Int) = label("", size, "#F0F6FC", true).apply {
        setBackgroundColor(Color.parseColor("#161B22"))
        setPadding(dp(12), dp(12), dp(12), dp(12))
    }

    private fun label(value: String, size: Int, color: String, bold: Boolean) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(Color.parseColor(color))
        gravity = Gravity.CENTER_VERTICAL
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun params(top: Int) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
