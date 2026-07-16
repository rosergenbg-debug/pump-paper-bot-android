package com.example.pumppaperbot

import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class AlertSettingsActivity : AppCompatActivity() {
    private lateinit var workButton: Button
    private lateinit var alwaysButton: Button
    private lateinit var startButton: Button
    private lateinit var endButton: Button
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundColor(Color.parseColor("#0D1117"))
        }
        root.addView(button("← НАЗАД", "#30363D").apply { setOnClickListener { finish() } }, LinearLayout.LayoutParams(-1, dp(48)))
        root.addView(label("ВРЕМЯ ЗВОНКА", 25, "#F0F6FC", true))
        root.addView(label("Звук и вибрация срабатывают при готовности 99 или 100 только если поздний вход не запрещён и данные достаточно согласованы.", 15, "#C9D1D9", true))

        val modes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        workButton = button("РАБОЧИЙ РЕЖИМ", "#30363D").apply {
            setOnClickListener { AlertSchedule.setMode(this@AlertSettingsActivity, AlertSchedule.MODE_WORK); updateUi() }
        }
        alwaysButton = button("24 ЧАСА", "#30363D").apply {
            setOnClickListener { AlertSchedule.setMode(this@AlertSettingsActivity, AlertSchedule.MODE_ALWAYS); updateUi() }
        }
        modes.addView(workButton, LinearLayout.LayoutParams(0, dp(58), 1f))
        modes.addView(alwaysButton, LinearLayout.LayoutParams(0, dp(58), 1f).apply { leftMargin = dp(8) })
        root.addView(modes, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(14) })

        root.addView(label("Рабочие дни: понедельник, вторник, четверг, пятница. В эти дни ночью звонка нет. В среду, субботу и воскресенье звонок разрешён круглосуточно.", 14, "#8B949E", false))
        root.addView(label("Разрешённое время в рабочие дни", 17, "#F0F6FC", true))
        val hours = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        startButton = button("С 06:00", "#238636").apply { setOnClickListener { pickTime(true) } }
        endButton = button("ДО 23:00", "#B62324").apply { setOnClickListener { pickTime(false) } }
        hours.addView(startButton, LinearLayout.LayoutParams(0, dp(58), 1f))
        hours.addView(endButton, LinearLayout.LayoutParams(0, dp(58), 1f).apply { leftMargin = dp(8) })
        root.addView(hours, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(8) })

        root.addView(label("Для отпуска или праздника включите «24 часа». Потом верните «Рабочий режим».", 14, "#F0B72F", true))
        status = label("", 15, "#58A6FF", true)
        root.addView(status)
        root.addView(label("Если сигнал возник в запрещённое время, приложение сохранит цену и время. После 06:00 оно сообщит: вход ещё возможен или уже пропущен.", 14, "#C9D1D9", false))
        setContentView(root)
        updateUi()
    }

    private fun pickTime(start: Boolean) {
        val value = if (start) AlertSchedule.startMinutes(this) else AlertSchedule.endMinutes(this)
        TimePickerDialog(this, { _, hour, minute ->
            val newValue = hour * 60 + minute
            if (start) {
                AlertSchedule.setHours(this, newValue, AlertSchedule.endMinutes(this))
            } else {
                AlertSchedule.setHours(this, AlertSchedule.startMinutes(this), newValue)
            }
            updateUi()
        }, value / 60, value % 60, true).show()
    }

    private fun updateUi() {
        val always = AlertSchedule.mode(this) == AlertSchedule.MODE_ALWAYS
        workButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (!always) "#238636" else "#30363D"))
        alwaysButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (always) "#1F6FEB" else "#30363D"))
        startButton.text = "С ${format(AlertSchedule.startMinutes(this))}"
        endButton.text = "ДО ${format(AlertSchedule.endMinutes(this))}"
        startButton.isEnabled = !always
        endButton.isEnabled = !always
        startButton.alpha = if (always) 0.5f else 1f
        endButton.alpha = if (always) 0.5f else 1f
        status.text = AlertSchedule.statusText(this)
    }

    private fun format(value: Int): String = String.format(Locale.GERMANY, "%02d:%02d", value / 60, value % 60)

    private fun button(text: String, color: String) = Button(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor(color))
        textSize = 13f
        isAllCaps = false
        setPadding(dp(4), 0, dp(4), 0)
    }

    private fun label(text: String, size: Int, color: String, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(Color.parseColor(color))
        setPadding(0, dp(7), 0, dp(7))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
