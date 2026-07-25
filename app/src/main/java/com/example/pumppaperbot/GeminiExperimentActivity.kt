package com.example.pumppaperbot

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class GeminiExperimentActivity : AppCompatActivity() {
    private lateinit var root: LinearLayout
    private lateinit var toggle: Button
    private lateinit var runNow: Button
    private lateinit var status: TextView
    private lateinit var portfolio: TextView
    private lateinit var statistics: TextView
    private lateinit var lastDecision: TextView
    private lateinit var history: TextView
    private lateinit var trades: TextView

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
        root.addView(label("V3.4 • ОТДЕЛЬНЫЙ GEMINI‑ЭКСПЕРИМЕНТ", 24, "#F0F6FC", true))
        root.addView(label(
            "Раз в новый закрытый час Gemini самостоятельно выбирает КУПИТЬ, ДЕРЖАТЬ или ПРОДАТЬ для виртуальных 1 000 €. " +
                "Он видит PUMP/BTC/SOL, spot/futures‑поток, funding, premium, стакан/OI и свежие новости. " +
                "Основную стратегию и реальные деньги этот модуль не меняет.",
            14, "#C9D1D9", false, 8
        ))

        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        toggle = button("ВКЛЮЧИТЬ", "#7C3AED")
        runNow = button("ПРОВЕРИТЬ ЧАС", "#1F6FEB")
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

        status = card("#172033")
        portfolio = card("#211A36")
        statistics = card("#161B22")
        lastDecision = card("#172033")
        history = card("#161B22")
        trades = card("#161B22")
        root.addView(status, cardParams())
        root.addView(portfolio, cardParams())
        root.addView(statistics, cardParams())
        root.addView(lastDecision, cardParams())

        root.addView(label("ИСТОРИЯ РЕШЕНИЙ", 17, "#79C0FF", true, 16))
        root.addView(history, cardParams(6))
        root.addView(label("ВИРТУАЛЬНЫЕ ОПЕРАЦИИ", 17, "#D2A8FF", true, 16))
        root.addView(trades, cardParams(6))

        root.addView(label("ЧЕСТНЫЙ КОНТРОЛЬ ИДЕИ", 17, "#F0B72F", true, 16))
        root.addView(label(
            "Повторный шестимесячный тест часовых рыночных признаков без подмешивания недоступного архива новостей не подтвердил цель. " +
                "На validation поймано 24,1% подъёмов PUMP свыше 3% (7 из 29), на закрытом holdout — 27,3% (12 из 44), результат после комиссий −14,76%. " +
                "Чтобы искусственно превысить 50% на validation, потребовалось 6,1 сигнала в сутки и 208 ложных из 223. " +
                "Поэтому Gemini работает только как живой отдельный эксперимент и сам накапливает проверяемую статистику.",
            14, "#F0B72F", false, 6
        ))

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
        render()
    }

    private fun runCheck() {
        runNow.isEnabled = false
        runNow.text = "ПРОВЕРЯЮ…"
        status.text = "Обновляю рынок и новости, затем проверяю новый закрытый час…"
        Thread {
            val result = runCatching {
                MarketSyncClient().sync(this)
                EventRadarClient().sync(this, force = true)
                GeminiExperimentClient().sync(this, force = true)
            }
            runOnUiThread {
                if (result.isFailure) {
                    GeminiPaperStore.saveFailure(
                        this,
                        result.exceptionOrNull()?.message ?: "Ошибка обновления"
                    )
                }
                runNow.isEnabled = true
                runNow.text = "ПРОВЕРИТЬ ЧАС"
                render()
            }
        }.start()
    }

    private fun render() {
        val state = GeminiPaperStore.state(this)
        val snapshot = PumpBotEngine.snapshot(this)
        val p = state.portfolio
        val value = p.value(snapshot.lastPrice)
        toggle.text = if (state.enabled) "ВЫКЛЮЧИТЬ" else "ВКЛЮЧИТЬ"
        toggle.setBackgroundColor(Color.parseColor(if (state.enabled) "#7C3AED" else "#6E7681"))
        val keySource = when {
            EventRadarStore.hasCustomApiKey(this) -> "сохранённый личный ключ"
            EmbeddedGeminiKey.value.isNotBlank() -> "встроенный ключ"
            else -> "ключ не настроен"
        }
        status.text = buildString {
            append("СТАТУС: ${state.status}")
            append("\nКлюч: $keySource")
            if (state.lastSuccess > 0L) append("\nПоследний ответ: ${PumpBotEngine.formatTime(state.lastSuccess)} • ${state.model}")
            append("\nСегодня: ${state.requestsToday} запросов • ${state.totalTokensToday} токенов")
            if (state.error.isNotBlank()) append("\nОшибка: ${state.error}")
        }
        status.setTextColor(Color.parseColor(
            when (state.status) {
                "РАБОТАЕТ" -> "#7EE787"
                "ОШИБКА", "НЕТ КЛЮЧА GEMINI" -> "#FF7B72"
                else -> "#79C0FF"
            }
        ))

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
            "ВИРТУАЛЬНЫЙ СЧЁТ\nСтарт €1 000,00 • сейчас €%.2f\nРезультат %+.2f%% • комиссии €%.2f\n%s",
            value,
            p.profitPercent(snapshot.lastPrice),
            p.totalFeesEur,
            position
        )
        portfolio.setTextColor(Color.parseColor(
            if (p.profit(snapshot.lastPrice) >= 0.0) "#D2A8FF" else "#FF7B72"
        ))
        statistics.text = String.format(
            Locale.GERMANY,
            "ЖИВАЯ СТАТИСТИКА\nЗакрытых сделок %d • прибыльных %d • win rate %.1f%%\n" +
                "Макс. просадка %.2f%% • точность направления %.1f%% (%d часов)\n" +
                "Подъёмы >3%%: поймано %d из %d • %.1f%%",
            p.closedTrades,
            p.winningTrades,
            p.winRatePercent,
            p.maxDrawdownPercent,
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
                append("\n${last.execution} • счёт €${String.format(Locale.GERMANY, "%.2f", last.portfolioValueAfter)}")
                append("\n${last.reason}")
                if (last.risks.isNotEmpty()) append("\nРиски: ${last.risks.joinToString("; ")}")
            }
        }

        history.text = p.decisions.takeLast(24).asReversed().joinToString("\n\n") { decision ->
            buildString {
                append("${PumpBotEngine.formatTime(decision.decidedAt)} • ${actionRu(decision.requestedAction)}")
                append(" • ${signed(decision.directionScore)}/100 • увер. ${decision.confidence}")
                decision.evaluatedReturnPercent?.let {
                    append(" • следующий час ${String.format(Locale.GERMANY, "%+.2f%%", it)}")
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
}
