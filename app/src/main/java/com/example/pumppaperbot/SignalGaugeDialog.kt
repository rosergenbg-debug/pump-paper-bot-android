package com.example.pumppaperbot

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

object SignalGaugeDialog {
    fun show(context: Context, snapshot: LiveSnapshot) {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 18), dp(context, 10), dp(context, 18), dp(context, 8))
            setBackgroundColor(Color.parseColor("#161B22"))
        }
        root.addView(TextView(context).apply {
            text = "КРУПНАЯ ШКАЛА СИГНАЛА"
            textSize = 20f
            setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
        })
        root.addView(LargeSignalGaugeView(context).apply { setSnapshot(snapshot) }, LinearLayout.LayoutParams(-1, dp(context, 390)))
        val score = snapshot.readinessScore
        val direction = when {
            score >= 99 -> "Сигнал почти подтверждён: покупка"
            score > 0 -> "Тенденция ближе к покупке: +$score/100"
            score <= -99 -> "Сигнал почти подтверждён: продажа"
            score < 0 -> "Тенденция ближе к продаже: −${abs(score)}/100"
            else -> "Нейтрально: подтверждённого направления нет"
        }
        root.addView(TextView(context).apply {
            text = buildString {
                append(direction)
                if (snapshot.waitMode == "BUY") {
                    append("\nОсторожный: ${snapshot.trendReadiness}/100")
                    if (snapshot.aggressive) append(" • после шока: ${snapshot.shockReadiness}/100")
                }
                append("\n\n+100 и −100 — полное выполнение правил стратегии, а не гарантия результата.")
            }
            textSize = 15f
            setTextColor(Color.parseColor("#C9D1D9"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, dp(context, 4), 0, dp(context, 8))
        })
        AlertDialog.Builder(context)
            .setView(root)
            .setPositiveButton("ЗАКРЫТЬ", null)
            .show()
    }

    private fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}

private class LargeSignalGaugeView(context: Context) : View(context) {
    private val green = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#238636") }
    private val red = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#B62324") }
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#30363D") }
    private val blue = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2F81F7") }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C9D1D9")
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }
    private var score = 0

    init {
        contentDescription = "Крупная шкала готовности от плюс ста для покупки до минус ста для продажи"
    }

    fun setSnapshot(snapshot: LiveSnapshot) {
        score = snapshot.readinessScore.coerceIn(-100, 100)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val top = 58f
        val bottom = height - 58f
        val middle = (top + bottom) / 2f
        val half = (bottom - top) / 2f
        val bar = RectF(centerX - 36f, top, centerX + 36f, bottom)
        canvas.drawRoundRect(bar, 24f, 24f, track)
        canvas.drawRoundRect(RectF(centerX - 36f, top, centerX + 36f, middle), 24f, 24f, green)
        canvas.drawRoundRect(RectF(centerX - 36f, middle, centerX + 36f, bottom), 24f, 24f, red)
        canvas.drawRect(centerX - 36f, middle - 24f, centerX + 36f, middle + 24f, track)

        val markerY = if (score >= 0) middle - half * score / 100f else middle + half * abs(score) / 100f
        canvas.drawCircle(centerX, markerY, 24f, blue)
        canvas.drawLine(centerX - 76f, markerY, centerX + 76f, markerY, blue.apply { strokeWidth = 8f })

        canvas.drawText("+100", centerX, 35f, text)
        canvas.drawText("ПОКУПКА", centerX + 125f, top + 12f, small)
        canvas.drawText("0", centerX + 72f, middle + 8f, small)
        canvas.drawText("−100", centerX, height - 20f, text)
        canvas.drawText("ПРОДАЖА", centerX + 125f, bottom, small)
        val current = if (score >= 0) "+$score" else "−${abs(score)}"
        canvas.drawText(current, centerX - 105f, markerY + 10f, text)
    }
}
