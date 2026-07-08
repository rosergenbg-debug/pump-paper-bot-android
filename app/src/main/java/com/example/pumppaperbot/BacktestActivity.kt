package com.example.pumppaperbot

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BacktestActivity : AppCompatActivity() {
    private var startTime: Long = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(180)
    private var selectedCoinIndex = 0
    private var selectedRisk = 35.0
    private val coinButtons = ArrayList<Button>()
    private lateinit var risk30Button: Button
    private lateinit var risk35Button: Button
    private lateinit var dateButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(Color.parseColor("#0D1117"))
        }

        val topRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val mainButton = button("< Главное меню", "#30363D")
        mainButton.setOnClickListener { finish() }
        topRow.addView(mainButton, LinearLayout.LayoutParams(-1, dp(44)))
        root.addView(topRow)

        root.addView(label("ПРОВЕРКА НАЗАД", 24, "#F0F6FC", true))
        root.addView(label("Выберите монету, риск RSI 30/35 и дату старта.", 13, "#8B949E", false))

        root.addView(label("Монета", 16, "#F0F6FC", true))
        addCoinGrid(root)

        root.addView(label("Риск стратегии", 16, "#F0F6FC", true))
        val riskRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        risk30Button = button("RSI 30\nОсторожно", "#30363D")
        risk35Button = button("RSI 35\nАктивно", "#238636")
        risk30Button.setOnClickListener {
            selectedRisk = 30.0
            updateRiskButtons()
        }
        risk35Button.setOnClickListener {
            selectedRisk = 35.0
            updateRiskButtons()
        }
        riskRow.addView(risk30Button, LinearLayout.LayoutParams(0, dp(54), 1f))
        riskRow.addView(risk35Button, LinearLayout.LayoutParams(0, dp(54), 1f).apply { leftMargin = dp(8) })
        root.addView(riskRow)

        dateButton = button("СТАРТ: ${PumpBotEngine.formatDate(startTime)}", "#30363D")
        dateButton.setOnClickListener { pickDate() }
        root.addView(dateButton, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(8) })

        val run = button("ЗАПУСТИТЬ ПРОВЕРКУ", "#238636")
        run.setOnClickListener { openResultScreen() }
        root.addView(run, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(8) })

        setContentView(root)
        updateCoinButtons()
        updateRiskButtons()
    }

    private fun addCoinGrid(root: LinearLayout) {
        PumpBotEngine.coinOptions.chunked(2).forEachIndexed { rowIndex, rowCoins ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowCoins.forEach { coin ->
                val globalIndex = PumpBotEngine.coinOptions.indexOf(coin)
                val coinButton = button("${coin.name}\n${coin.symbol}", "#30363D")
                coinButton.setOnClickListener {
                    selectedCoinIndex = globalIndex
                    updateCoinButtons()
                }
                coinButtons.add(coinButton)
                val params = LinearLayout.LayoutParams(0, dp(54), 1f)
                if (row.childCount > 0) params.leftMargin = dp(8)
                row.addView(coinButton, params)
            }
            if (rowCoins.size == 1) {
                row.addView(View(this), LinearLayout.LayoutParams(0, dp(54), 1f).apply { leftMargin = dp(8) })
            }
            val params = LinearLayout.LayoutParams(-1, dp(54))
            if (rowIndex > 0) params.topMargin = dp(5)
            root.addView(row, params)
        }
    }

    private fun pickDate() {
        val cal = Calendar.getInstance().apply { timeInMillis = startTime }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val picked = Calendar.getInstance()
                picked.set(year, month, day, 0, 0, 0)
                picked.set(Calendar.MILLISECOND, 0)
                startTime = picked.timeInMillis
                dateButton.text = "СТАРТ: ${PumpBotEngine.formatDate(startTime)}"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun openResultScreen() {
        val intent = Intent(this, BacktestResultActivity::class.java)
            .putExtra(BacktestResultActivity.extraCoinIndex, selectedCoinIndex)
            .putExtra(BacktestResultActivity.extraRisk, selectedRisk)
            .putExtra(BacktestResultActivity.extraStartTime, startTime)
        startActivity(intent)
    }

    private fun updateCoinButtons() {
        coinButtons.forEachIndexed { index, button ->
            button.setBackgroundColor(Color.parseColor(if (index == selectedCoinIndex) "#1F6FEB" else "#30363D"))
        }
    }

    private fun updateRiskButtons() {
        risk30Button.setBackgroundColor(Color.parseColor(if (selectedRisk <= 30.0) "#238636" else "#30363D"))
        risk35Button.setBackgroundColor(Color.parseColor(if (selectedRisk > 30.0) "#238636" else "#30363D"))
    }

    private fun button(text: String, color: String): Button {
        return Button(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor(color))
            textSize = 12f
            isAllCaps = false
        }
    }

    private fun label(text: String, size: Int, color: String, bold: Boolean): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size.toFloat()
            setTextColor(Color.parseColor(color))
            setPadding(0, dp(5), 0, dp(5))
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
