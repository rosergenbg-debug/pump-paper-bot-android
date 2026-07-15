package com.example.pumppaperbot

import android.app.DatePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BacktestActivity : AppCompatActivity() {
    private var startTime: Long = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(365)
    private lateinit var dateButton: Button
    private lateinit var cautiousButton: Button
    private lateinit var aggressiveButton: Button
    private var aggressive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aggressive = PumpBotEngine.isAggressive(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(Color.parseColor("#0D1117"))
        }
        root.addView(button("< Главное меню", "#30363D").apply { setOnClickListener { finish() } }, LinearLayout.LayoutParams(-1, dp(44)))
        root.addView(label("ПРОВЕРКА НАЗАД PUMP/EUR", 24, "#F0F6FC", true))
        root.addView(label("Выберите тот же профиль, который хотите использовать в live. Проверяются PUMP, BTC и funding.", 14, "#8B949E", false))
        root.addView(label("Комиссия 0,15% на вход и 0,15% на выход. Проскальзывание 0,05%. Стоп цены 4,4%.", 14, "#C9D1D9", false))
        root.addView(label("Другие монеты отключены: этот V2-алгоритм подтвердился только на истории PUMP.", 14, "#F0B72F", true))

        val profileRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        cautiousButton = button("ОСТОРОЖНЫЙ\n1 вход • +8%", "#30363D").apply {
            setOnClickListener { aggressive = false; renderProfiles() }
        }
        aggressiveButton = button("АГРЕССИВНЫЙ\n2 входа • 50%", "#30363D").apply {
            setOnClickListener { aggressive = true; renderProfiles() }
        }
        profileRow.addView(cautiousButton, LinearLayout.LayoutParams(0, dp(58), 1f))
        profileRow.addView(aggressiveButton, LinearLayout.LayoutParams(0, dp(58), 1f).apply { leftMargin = dp(8) })
        root.addView(profileRow, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(10) })
        renderProfiles()

        dateButton = button("СТАРТ: ${PumpBotEngine.formatDate(startTime)}", "#30363D").apply { setOnClickListener { pickDate() } }
        root.addView(dateButton, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(14) })
        root.addView(button("ЗАПУСТИТЬ V2 BACKTEST", "#238636").apply {
            setOnClickListener {
                startActivity(
                    Intent(this@BacktestActivity, BacktestResultActivity::class.java)
                        .putExtra(BacktestResultActivity.extraStartTime, startTime)
                        .putExtra(BacktestResultActivity.extraAggressive, aggressive)
                )
            }
        }, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(10) })
        setContentView(root)
    }

    private fun renderProfiles() {
        cautiousButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (!aggressive) "#238636" else "#30363D"))
        aggressiveButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (aggressive) "#B62324" else "#30363D"))
        cautiousButton.alpha = if (!aggressive) 1f else 0.72f
        aggressiveButton.alpha = if (aggressive) 1f else 0.72f
    }

    private fun pickDate() {
        val cal = Calendar.getInstance().apply { timeInMillis = startTime }
        DatePickerDialog(this, { _, year, month, day ->
            startTime = Calendar.getInstance().apply {
                set(year, month, day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            dateButton.text = "СТАРТ: ${PumpBotEngine.formatDate(startTime)}"
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun button(text: String, color: String) = Button(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor(color))
        textSize = 13f
        isAllCaps = false
    }

    private fun label(text: String, size: Int, color: String, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(Color.parseColor(color))
        setPadding(0, dp(6), 0, dp(6))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
