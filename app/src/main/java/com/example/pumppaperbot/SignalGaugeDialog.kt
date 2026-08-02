package com.example.pumppaperbot

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.abs

object SignalGaugeDialog {
    fun show(context: Context, snapshot: LiveSnapshot) {
        val now = System.currentTimeMillis()
        val radar = EventRadarStore.state(context)
        val deepSeek = DeepSeekPrimaryStore.state(context)
        val internalScore = snapshot.directionScore
        val information = radar.informationAdjustment(now)
        val totalScore = radar.combinedDirection(internalScore, now)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 18), dp(context, 10), dp(context, 18), dp(context, 8))
            setBackgroundColor(Color.parseColor("#161B22"))
        }
        root.addView(TextView(context).apply {
            text = "ДВЕ ЖИВЫЕ ШКАЛЫ"
            textSize = 20f
            setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
        })
        root.addView(DualSignalGaugeView(context).apply {
            setScores(
                appInternal = internalScore,
                appAdjustment = information,
                appTotal = totalScore,
                geminiScore = deepSeek.direction.takeIf { deepSeek.lastSuccess > 0L },
                geminiConfidence = deepSeek.confidence
            )
        }, LinearLayout.LayoutParams(-1, dp(context, 390)))

        root.addView(section(context, "#172033", buildString {
            append("APP • ИТОГ ${signed(totalScore)}/100\n")
            append(directionText(totalScore))
            append("\n\nКто считает: приложение по текущему рынку PUMP/BTC/SOL, потоку сделок, активности и риску позднего входа.")
            append("\nОснова приложения: ${signed(internalScore)}/100")
            append("\nПоправка Gemini по свежим новостям: ${signed(information)}")
            append("\nИтог на шкале: ${signed(internalScore)} ${operation(information)} = ${signed(totalScore)}/100")
            append("\n\nПочему сейчас так: ${snapshot.breathingState}. ${snapshot.breathingExplanation}")
            append("\nАктивность ${snapshot.energyScore}/100 • сжатие ${snapshot.compressionScore}/100")
            append("\nСогласованность ${snapshot.breathingConfidence}/100 • поздний вход ${snapshot.lateEntryRisk}/100")
            if (information == 0) {
                append("\nНовостная поправка равна нулю: нет свежего подтверждённого события, которое сейчас допускается добавить.")
            } else {
                append("\nНовостная поправка ограничена диапазоном ±12 и со временем уменьшается до нуля.")
            }
            if (radar.gemini.outputSummary.isNotBlank()) {
                append("\nОснование новостного Gemini: ${radar.gemini.outputSummary.take(500)}")
            }
        }))

        root.addView(section(context, "#211A36", buildString {
            append("DEEPSEEK • ОСНОВНОЙ АНАЛИТИК\n")
            if (deepSeek.lastSuccess <= 0L) {
                append("Текущего числа на шкале нет. ${deepSeek.error.ifBlank { "Ожидается первый рыночный анализ." }}")
            } else {
                append("Решение: ${actionRu(deepSeek.action)}")
                append(" • направление ${signed(deepSeek.direction)}/100")
                append(" • уверенность ${deepSeek.confidence}/100 • опасность ${deepSeek.danger}/10")
                append("\nКогда: ${PumpBotEngine.formatTime(deepSeek.lastSuccess)} • ${deepSeek.model}")
                append("\nПочему: ${deepSeek.summary}")
                if (deepSeek.evidence.isNotEmpty()) append("\nФакты: ${deepSeek.evidence.joinToString("; ")}")
                if (deepSeek.risks.isNotEmpty()) append("\nРиски: ${deepSeek.risks.joinToString("; ")}")
            }
            append("\n\nКто считает: DeepSeek Flash по полному рыночному кадру каждые 2 минуты и при существенном изменении сигнала; BUY/EXIT проходит отдельную усиленную проверку.")
            append("\nОн даёт независимый аналитический сигнал и не меняет сделки APP автоматически.")
        }))

        root.addView(section(context, "#101820", buildString {
            append("КАК ЧИТАТЬ ДВЕ ШКАЛЫ\n")
            append("APP обновляется по живому рынку и показывает общий фон приложения с небольшой новостной поправкой.")
            append("\nDEEPSEEK регулярно выдаёт отдельное решение по тому же рынку.")
            append("\nПоэтому значения могут расходиться — это два независимых взгляда, а не ошибка.")
            append("\n\nРадар: ${radar.sourceCount}/${EventRadarClient.totalSources} источников • ${radar.parsedEntries} сообщений")
            append("\nНовостной Gemini: ${if (radar.aiEnabled) radar.gemini.status else "ВЫКЛЮЧЁН"}")
            append("\n\nСиний маркер APP — расчёт приложения без новостей. Фиолетовый — итог APP после новостной поправки. Оранжевый — самостоятельное решение DeepSeek.")
            append("\nЭто шкалы направления, а не вероятность прибыли и не автоматический приказ купить.")
        }))
        AlertDialog.Builder(context)
            .setView(ScrollView(context).apply { addView(root) })
            .setPositiveButton("ЗАКРЫТЬ", null)
            .show()
    }

    private fun section(context: Context, color: String, value: String) = TextView(context).apply {
        text = value
        textSize = 15f
        setTextColor(Color.parseColor("#C9D1D9"))
        setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12))
        setBackgroundColor(Color.parseColor(color))
    }.also {
        it.layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(context, 8)
        }
    }

    private fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    private fun signed(value: Int): String = if (value >= 0) "+$value" else "−${abs(value)}"

    private fun operation(value: Int): String =
        if (value >= 0) "+ $value" else "− ${abs(value)}"

    private fun directionText(value: Int): String = when {
        value >= 25 -> "Общий поток приложения направлен вверх."
        value > 0 -> "У приложения слабый перевес вверх."
        value <= -25 -> "Общий поток приложения направлен вниз."
        value < 0 -> "У приложения слабый перевес вниз."
        else -> "Общий поток приложения нейтрален."
    }

    private fun actionRu(value: String): String = when (value) {
        "BUY" -> "КУПИТЬ"
        "SELL", "EXIT" -> "ПРОДАТЬ / ВЫЙТИ"
        "WATCH" -> "НАБЛЮДАТЬ"
        else -> "ДЕРЖАТЬ / ЖДАТЬ"
    }
}

private class DualSignalGaugeView(context: Context) : View(context) {
    private val green = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#238636") }
    private val red = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#B62324") }
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#30363D") }
    private val blue = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2F81F7") }
    private val purple = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#A371F7") }
    private val orange = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F0883E") }
    private val connector = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A371F7")
        strokeWidth = dp(3f)
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = sp(17f)
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C9D1D9")
        textSize = sp(12f)
        textAlign = Paint.Align.CENTER
    }
    private var appInternal = 0
    private var appAdjustment = 0
    private var appTotal = 0
    private var geminiScore: Int? = null
    private var geminiConfidence = 0

    init {
        contentDescription = "Две шкалы от плюс ста до минус ста: итог приложения и самостоятельное решение DeepSeek"
    }

    fun setScores(
        appInternal: Int,
        appAdjustment: Int,
        appTotal: Int,
        geminiScore: Int?,
        geminiConfidence: Int
    ) {
        this.appInternal = appInternal.coerceIn(-100, 100)
        this.appAdjustment = appAdjustment.coerceIn(-12, 12)
        this.appTotal = appTotal.coerceIn(-100, 100)
        this.geminiScore = geminiScore?.coerceIn(-100, 100)
        this.geminiConfidence = geminiConfidence.coerceIn(0, 100)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val appX = width * 0.28f
        val geminiX = width * 0.72f
        val top = dp(62f)
        val bottom = height - dp(48f)
        val middle = (top + bottom) / 2f
        val half = (bottom - top) / 2f
        val barHalf = dp(18f)

        fun drawTrack(centerX: Float) {
            val bar = RectF(centerX - barHalf, top, centerX + barHalf, bottom)
            canvas.drawRoundRect(bar, dp(12f), dp(12f), track)
            canvas.drawRoundRect(
                RectF(centerX - barHalf, top, centerX + barHalf, middle),
                dp(12f),
                dp(12f),
                green
            )
            canvas.drawRoundRect(
                RectF(centerX - barHalf, middle, centerX + barHalf, bottom),
                dp(12f),
                dp(12f),
                red
            )
            canvas.drawRect(
                centerX - barHalf,
                middle - dp(9f),
                centerX + barHalf,
                middle + dp(9f),
                track
            )
        }

        fun markerY(value: Int): Float = if (value >= 0) {
            middle - half * value / 100f
        } else {
            middle + half * abs(value) / 100f
        }

        drawTrack(appX)
        drawTrack(geminiX)
        canvas.drawText("APP", appX, dp(20f), text)
        canvas.drawText("DEEPSEEK", geminiX, dp(20f), text)
        canvas.drawText("итог ${signed(appTotal)}", appX, dp(41f), small)
        canvas.drawText(
            geminiScore?.let { signed(it) } ?: "нет свежего",
            geminiX,
            dp(41f),
            small
        )

        val internalY = markerY(appInternal)
        val totalY = markerY(appTotal)
        blue.strokeWidth = dp(4f)
        purple.strokeWidth = dp(4f)
        orange.strokeWidth = dp(4f)
        canvas.drawLine(appX - dp(43f), internalY, appX - dp(4f), internalY, blue)
        canvas.drawCircle(appX - dp(10f), internalY, dp(8f), blue)
        canvas.drawLine(appX + dp(4f), totalY, appX + dp(43f), totalY, purple)
        canvas.drawCircle(appX + dp(10f), totalY, dp(8f), purple)
        if (internalY != totalY) {
            canvas.drawLine(appX + dp(39f), internalY, appX + dp(39f), totalY, connector)
        }

        geminiScore?.let { score ->
            val scoreY = markerY(score)
            canvas.drawLine(geminiX - dp(43f), scoreY, geminiX + dp(43f), scoreY, orange)
            canvas.drawCircle(geminiX, scoreY, dp(9f), orange)
        } ?: run {
            canvas.drawText("—", geminiX, middle + dp(6f), text)
        }

        listOf(appX, geminiX).forEach { centerX ->
            canvas.drawText("+100", centerX, top - dp(9f), small)
            canvas.drawText("0", centerX + dp(31f), middle + dp(4f), small)
            canvas.drawText("−100", centerX, bottom + dp(20f), small)
        }
        canvas.drawText("новости ${signed(appAdjustment)}", appX, height - dp(6f), small)
        canvas.drawText(
            if (geminiScore == null) "решение устарело" else "уверенность $geminiConfidence%",
            geminiX,
            height - dp(6f),
            small
        )
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity
    private fun signed(value: Int): String = if (value >= 0) "+$value" else "−${abs(value)}"
}
