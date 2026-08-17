package com.example.pumppaperbot

import android.app.AlertDialog
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

class AppPaperActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            render()
            handler.postDelayed(this, 5_000L)
        }
    }
    private lateinit var appCard: TextView
    private lateinit var geminiCard: TextView
    private lateinit var experimentCard: TextView
    private lateinit var userCard: TextView
    private lateinit var comparison: TextView
    private lateinit var appTrades: TextView
    private lateinit var appDecisions: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0D1117"))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(28))
        }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(button("← НАЗАД", "#30363D").apply {
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(-1, dp(48)))
        root.addView(label("APP • DEEPSIG • DEEPSIGX", 22, "#F0F6FC", true, 12))
        root.addView(label(
            "Три автономные тестовые системы с отдельными виртуальными счетами по €1 000. Счёт Сержа показан отдельно для ручного контроля. Реальные биржевые заявки приложение не исполняет; звонки управляются общей кнопкой.",
            14, "#C9D1D9", false, 6
        ))

        val accounts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val firstRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val secondRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        appCard = card("#132A20")
        geminiCard = card("#211A36")
        experimentCard = card("#33270D")
        userCard = card("#172033")
        firstRow.addView(appCard, LinearLayout.LayoutParams(0, dp(156), 1f))
        firstRow.addView(geminiCard, LinearLayout.LayoutParams(0, dp(156), 1f).apply {
            leftMargin = dp(8)
        })
        secondRow.addView(experimentCard, LinearLayout.LayoutParams(0, dp(156), 1f))
        secondRow.addView(userCard, LinearLayout.LayoutParams(0, dp(156), 1f).apply {
            leftMargin = dp(8)
        })
        accounts.addView(firstRow)
        accounts.addView(secondRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        root.addView(accounts, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        comparison = card("#172033")
        root.addView(comparison, cardParams(10))
        root.addView(button("ОТКРЫТЬ ПОДРОБНОСТИ DEEPSIG / DEEPSIGX", "#7C3AED").apply {
            setOnClickListener {
                startActivity(android.content.Intent(this@AppPaperActivity, GeminiExperimentActivity::class.java))
            }
        }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(8) })

        root.addView(label("СДЕЛКИ APP • ХРАНЯТСЯ ДО РУЧНОГО СБРОСА", 16, "#7EE787", true, 18))
        appTrades = card("#161B22")
        root.addView(appTrades, cardParams(6))
        root.addView(label("РЕШЕНИЯ APP • ПОСЛЕДНИЕ 6 МЕСЯЦЕВ", 16, "#79C0FF", true, 18))
        appDecisions = card("#161B22")
        root.addView(appDecisions, cardParams(6))

        root.addView(button("СБРОСИТЬ ТОЛЬКО ВИРТУАЛЬНЫЙ СЧЁТ APP", "#8E1519").apply {
            setOnClickListener {
                AlertDialog.Builder(this@AppPaperActivity)
                    .setTitle("Начать счёт App заново с €1 000?")
                    .setMessage("Удалятся сделки и решения только виртуального APP. DeepSig, DeepSigX и счёт Сержа не изменятся.")
                    .setNegativeButton("ОТМЕНА", null)
                    .setPositiveButton("СБРОСИТЬ") { _, _ ->
                        AppPaperStore.reset(this@AppPaperActivity)
                        render()
                    }
                    .show()
            }
        }, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(18) })
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
        val app = AppPaperStore.state(this)
        val geminiState = GeminiPaperStore.state(this)
        val gemini = geminiState.portfolio
        val experiment = GeminiExitExperimentStore.state(this)?.portfolio ?: GeminiPaperPortfolio()
        val user = UserPaperStore.markToMarket(this, price)
        val appValue = app.value(price)
        val geminiValue = gemini.value(price)
        val experimentValue = experiment.value(price)
        val userValue = user.value(price)

        appCard.text = accountText(
            "APP",
            appValue,
            app.profitPercent(price),
            app.inPosition,
            app.entryPrice,
            app.trades.lastOrNull()?.action,
            app.trades.lastOrNull()?.time
        )
        geminiCard.text = accountText(
            "DEEPSIG",
            geminiValue,
            gemini.profitPercent(price),
            gemini.inPosition,
            gemini.entryPrice,
            gemini.trades.lastOrNull()?.action,
            gemini.trades.lastOrNull()?.time
        )
        experimentCard.text = accountText(
            "DEEPSIGX",
            experimentValue,
            experiment.profitPercent(price),
            experiment.inPosition,
            experiment.entryPrice,
            experiment.trades.lastOrNull()?.action,
            experiment.trades.lastOrNull()?.time
        )
        userCard.text = accountText(
            "СЕРЖ",
            userValue,
            user.profitPercent(price),
            user.inPosition,
            user.entryPrice,
            user.trades.lastOrNull()?.action,
            user.trades.lastOrNull()?.time
        )
        appCard.setTextColor(Color.parseColor(if (app.profit(price) >= 0.0) "#7EE787" else "#FF7B72"))
        geminiCard.setTextColor(Color.parseColor(if (gemini.profit(price) >= 0.0) "#D2A8FF" else "#FF7B72"))
        experimentCard.setTextColor(Color.parseColor(if (experiment.profit(price) >= 0.0) "#F2CC60" else "#FF7B72"))
        userCard.setTextColor(Color.parseColor(if (user.profit(price) >= 0.0) "#79C0FF" else "#FF7B72"))

        val ranking = listOf(
            "APP" to appValue,
            "DEEPSIG" to geminiValue,
            "DEEPSIGX" to experimentValue
        )
            .sortedByDescending { it.second }
        val leader = "Сейчас впереди ${ranking.first().first} • €${money(ranking.first().second)}"
        comparison.text = buildString {
            append("СРАВНЕНИЕ В МОМЕНТЕ\n$leader")
            append("\nAPP: ${positionWord(app.inPosition)}")
            append("  •  DeepSig: ${positionWord(gemini.inPosition)}")
            append("  •  DeepSigX: ${positionWord(experiment.inPosition)}")
            append("\nСерж (вне рейтинга): ${positionWord(user.inPosition)}")
            val appLast = app.trades.lastOrNull()
            val geminiLast = gemini.trades.lastOrNull()
            val experimentLast = experiment.trades.lastOrNull()
            val userLast = user.trades.lastOrNull()
            append("\nПоследнее действие APP: ${tradeWord(appLast?.action)} ${time(appLast?.time)}")
            append("\nПоследнее действие DeepSig: ${tradeWord(geminiLast?.action)} ${time(geminiLast?.time)}")
            append("\nПоследнее действие DeepSigX: ${tradeWord(experimentLast?.action)} ${time(experimentLast?.time)}")
            append("\nПоследнее действие Сержа: ${tradeWord(userLast?.action)} ${time(userLast?.time)}")
            append("\n\nAPP — воспроизводимая причинная V5‑база: режим рынка, pullback/reclaim/retest, зона входа, ATR‑инвалидация и издержки. DeepSig — независимое предложение модели, которое исполняется только после повторного подтверждения и отдельной проверки риска; APP ему не командует. DeepSigX — независимый быстрый количественный эксперимент по дыханию, потоку, CVD и структуре; он не зеркалит сделки двух других систем. Все три работают только с виртуальными деньгами и не звонят.")
        }

        appTrades.text = app.trades.takeLast(40).asReversed().joinToString("\n\n") { trade ->
            buildString {
                append("${PumpBotEngine.formatDate(trade.time)} • ${tradeWord(trade.action)}")
                append("\n€${String.format(Locale.GERMANY, "%.8f", trade.price)}")
                append(" • комиссия €${money(trade.fee)}")
                if (trade.action.startsWith("SELL")) append(" • результат ${signedMoney(trade.pnlEur)}")
                append("\n${trade.reason.take(220)}")
            }
        }.ifBlank { "App получил €1 000 и ждёт первого подтверждённого входа." }

        appDecisions.text = app.decisions.takeLast(48).asReversed().joinToString("\n\n") { decision ->
            "${PumpBotEngine.formatDate(decision.time)} • ${tradeWord(decision.action)} • " +
                "${positionWord(decision.positionAfter)} • €${money(decision.valueAfter)}\n" +
                decision.reason.take(220)
        }.ifBlank { "Решений пока нет. Первое появится после новой закрытой 30‑минутной свечи." }
    }

    private fun accountText(
        name: String,
        value: Double,
        profitPercent: Double,
        inPosition: Boolean,
        entryPrice: Double,
        lastAction: String?,
        lastActionAt: Long?
    ): String = buildString {
        append(name)
        append("\n€${money(value)}")
        append("\n${String.format(Locale.GERMANY, "%+.2f%%", profitPercent)}")
        append("\n\n${positionWord(inPosition)}")
        if (inPosition && entryPrice > 0.0) {
            append("\nвход €${String.format(Locale.GERMANY, "%.8f", entryPrice)}")
        }
        append("\n${tradeWord(lastAction)} ${time(lastActionAt)}")
    }

    private fun positionWord(inPosition: Boolean) = if (inPosition) "В PUMP" else "В ЕВРО"

    private fun tradeWord(action: String?): String = when (action) {
        "BUY" -> "КУПИЛ"
        "SELL_HALF" -> "ПРОДАЛ 50%"
        "SELL" -> "ПРОДАЛ"
        "HOLD", "WAIT" -> "ЖДЁТ"
        null -> "ЕЩЁ БЕЗ СДЕЛОК"
        else -> action
    }

    private fun time(value: Long?): String =
        value?.takeIf { it > 0L }?.let(PumpBotEngine::formatTime).orEmpty()

    private fun money(value: Double) = String.format(Locale.GERMANY, "%.2f", value)
    private fun signedMoney(value: Double) = String.format(Locale.GERMANY, "%+.2f €", value)

    private fun label(
        text: String,
        size: Int,
        color: String,
        bold: Boolean,
        top: Int = 0
    ) = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(Color.parseColor(color))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        if (top > 0) setPadding(0, dp(top), 0, 0)
    }

    private fun card(color: String) = TextView(this).apply {
        setBackgroundColor(Color.parseColor(color))
        setTextColor(Color.parseColor("#C9D1D9"))
        textSize = 14f
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), dp(12), dp(12), dp(12))
    }

    private fun button(text: String, color: String) = Button(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor(color))
        textSize = 12f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun cardParams(top: Int = 8) =
        LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top) }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
