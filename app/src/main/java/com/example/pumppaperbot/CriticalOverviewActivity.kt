package com.example.pumppaperbot

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.math.max

data class CriticalOverviewHistoryPoint(
    val at: Long,
    val pump: Int?,
    val book: Int?,
    val bitcoin: Int?
)

class CriticalOverviewActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val history = ArrayDeque<CriticalOverviewHistoryPoint>()
    private lateinit var status: TextView
    private lateinit var chart: CriticalOverviewChartView
    private lateinit var breathingChart: MarketBreathingChartView
    private lateinit var facts: TextView
    private lateinit var deepSeek: TextView
    private lateinit var cost: TextView
    private val refresh = object : Runnable {
        override fun run() {
            render()
            handler.postDelayed(this, 2_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(24))
            setBackgroundColor(Color.parseColor("#0D1117"))
        }
        content.addView(button("← НАЗАД", "#30363D").apply { setOnClickListener { finish() } }, params(dp(48)))
        content.addView(label("КРИТИЧЕСКИЙ ОБЗОР", 25, "#F0F6FC", true), params(-2, dp(8)))
        content.addView(label(
            "Живые факты: стакан, покупки/продажи PUMP, spot/futures, Bitcoin и открытый интерес. Экран обновляется каждые 2 секунды и сам не создаёт платных запросов.",
            13,
            "#8B949E",
            false
        ))
        status = panel(20, true)
        content.addView(status, params(-2, dp(9)))
        chart = CriticalOverviewChartView(this)
        content.addView(chart, params(dp(610), dp(6)))
        content.addView(label(
            "ДЫХАНИЕ РЫНКА • ИСТОРИЯ ДО 24 ЧАСОВ",
            18,
            "#F0F6FC",
            true
        ), params(-2, dp(10)))
        breathingChart = MarketBreathingChartView(this)
        content.addView(breathingChart, params(dp(330), dp(6)))
        facts = panel(14, false)
        content.addView(facts, params(-2, dp(8)))
        deepSeek = panel(14, false)
        content.addView(deepSeek, params(-2, dp(8)))
        cost = panel(13, true)
        content.addView(cost, params(-2, dp(8)))
        setContentView(ScrollView(this).apply { addView(content) })
    }

    override fun onResume() {
        super.onResume()
        handler.post(refresh)
    }

    override fun onPause() {
        handler.removeCallbacks(refresh)
        super.onPause()
    }

    private fun render() {
        val now = System.currentTimeMillis()
        val snapshot = PumpBotEngine.snapshot(this)
        val micro = MicroImpulseStore.state(this)
        val impulse = ImpulseRadarStore.state(this)
        val primary = DeepSeekPrimaryStore.state(this, now)
        val breathing = LiveMarketBreathingStore.snapshot(this, now)
        val positionOpen = snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0
        val level = if (positionOpen) {
            DeepSeekActionLevelPolicy.fromPosition(
                snapshot,
                PositionSupervisorStore.state(this),
                PersonalPositionGuardStore.state(this),
                micro,
                now
            )
        } else {
            DeepSeekActionLevelPolicy.fromMarket(snapshot, primary, micro, now)
        }
        val microFresh = micro.connected && DeepSeekFreshMarketContext.isFresh(
            micro.updatedAt,
            now,
            DeepSeekFreshMarketContext.MICRO_MAX_AGE
        )
        val impulseFresh = DeepSeekFreshMarketContext.isFresh(
            impulse.candleTime,
            now,
            20L * 60L * 1_000L
        )
        val model = CriticalOverviewPolicy.evaluate(CriticalOverviewEvidence(
            positionOpen = positionOpen,
            actionLevel = level.level,
            directionScore = snapshot.directionScore,
            hardEntryVeto = snapshot.rapidDrop.active && !snapshot.rapidDrop.recoveryConfirmed,
            rapidDrop = snapshot.rapidDrop.active,
            bookImbalance = if (microFresh) micro.topBookImbalance ?: snapshot.bookImbalance else snapshot.bookImbalance,
            pumpBuyerPercent60s = micro.aggressiveBuyPercent60s.takeIf { microFresh },
            pumpPriceChange60sPercent = micro.priceChange60sPercent.takeIf { microFresh },
            spotTakerRatio = impulse.spotTakerRatio.takeIf { impulseFresh },
            futuresTakerRatio = impulse.futuresTakerRatio.takeIf { impulseFresh },
            bitcoinBuyerPercent60s = micro.bitcoinAggressiveBuyPercent60s.takeIf { microFresh },
            bitcoinPriceChange60sPercent = micro.bitcoinPriceChange60sPercent.takeIf { microFresh },
            openInterestChangePercent = (impulse.openInterestChange10m.takeIf { impulseFresh }
                ?: snapshot.openInterestChangePercent)
        ))
        val colors = when (model.band) {
            CriticalOverviewBand.RED -> "#FF7B72" to "#3A171A"
            CriticalOverviewBand.YELLOW -> "#FFD866" to "#3A300F"
            CriticalOverviewBand.GREEN -> "#7EE787" to "#15351F"
        }
        status.text = "${model.headline}\nИТОГ ${signed(model.overallScore)}/100 • ${if (positionOpen) "ПОЗИЦИЯ ОТКРЫТА" else "ОЖИДАНИЕ ВХОДА"}"
        status.setTextColor(Color.parseColor(colors.first))
        status.setBackgroundColor(Color.parseColor(colors.second))

        val metric = model.metrics.associateBy { it.key }
        history.addLast(CriticalOverviewHistoryPoint(
            at = now,
            pump = metric["pump"]?.score,
            book = metric["book"]?.score,
            bitcoin = metric["bitcoin"]?.score
        ))
        while (history.size > 120) history.removeFirst()
        chart.setData(model, history.toList())
        breathingChart.setData(breathing)

        facts.text = buildString {
            append("ЧТО ПРОИСХОДИТ СЕЙЧАС\n")
            model.metrics.forEach { item ->
                append("\n${item.title}: ${item.score?.let(::signed) ?: "—"}/100 • ${item.detail}")
            }
            val bid = snapshot.bookBidNotional
            val ask = snapshot.bookAskNotional
            if (bid != null && ask != null) {
                append(String.format(Locale.GERMANY, "\nСтакан 20 уровней: bid $%,.0f • ask $%,.0f", bid, ask))
            }
            if (microFresh) {
                append(String.format(
                    Locale.GERMANY,
                    "\nPUMP 60 сек.: покупок $%,.0f • продаж $%,.0f • сделок %d",
                    micro.buyNotional60s,
                    micro.sellNotional60s,
                    micro.trades60s
                ))
            }
            append("\n\nДЫХАНИЕ: ${breathing.regime}")
            append("\nОбычный DeepSeek: ${breathing.normalScore?.let(::signed) ?: "—"}/100")
            append(" • эксперимент: ${breathing.experimentScore?.let(::signed) ?: "—"}/100")
            append(" • мгновенно: ${breathing.instantScore?.let(::signed) ?: "—"}/100")
            append("\nЦикл покупок: ${breathing.buyerBreath.title}")
            append(" • напор ${breathing.buyerBreath.pressureScore?.let(::signed) ?: "—"}")
            append(" • эффективность ${breathing.buyerBreath.efficiencyScore?.let(::signed) ?: "—"}")
            append(" • поглощение ${breathing.buyerBreath.absorptionRisk}/100")
            append("\n${breathing.buyerBreath.actionHint}")
            append("\nНакоплено ${breathing.historyMinutes} мин.; данные старше 24 часов удаляются.")
            if (!breathing.fresh) append("\nВнимание: сглаженное дыхание устарело и не используется DeepSeek.")
            append("\n\nСвежесть: микросделки ${age(micro.updatedAt, now)} сек • 5‑мин данные ${age(impulse.candleTime, now)} сек")
            if (!microFresh) append("\nВнимание: живой поток сейчас устарел или переподключается.")
        }
        deepSeek.text = buildString {
            append("DEEPSEEK • ${level.level}/10 • ${level.label}\n${level.detail}")
            append("\n\nПредложение: ${primary.proposedAction} • итог: ${primary.action}")
            append("\nИсполнение: ${primary.executionStatus}")
            append("\nПроверка: ${primary.verificationSummary}")
            append("\n\n${primary.summary}")
            if (primary.evidence.isNotEmpty()) append("\nФакты: ${primary.evidence.joinToString("; ")}")
            if (primary.risks.isNotEmpty()) append("\nРиски: ${primary.risks.joinToString("; ")}")
            if (positionOpen) {
                val supervisor = PositionSupervisorStore.state(this@CriticalOverviewActivity)
                append("\n\nСопровождение позиции: ${supervisor.summary}")
                append("\nСтакан: ${supervisor.bookStatus}")
                append("\nСделки: ${supervisor.flowStatus}")
                append("\nBitcoin: ${supervisor.bitcoinStatus}")
                append("\nСледить за: ${supervisor.watchFor}")
            }
        }
        val estimated = DeepSeekDailyBudgetStore.costUsd(this, now)
        cost.text = String.format(
            Locale.GERMANY,
            "РАСХОД DEEPSEEK СЕГОДНЯ: $%.4f\nБлокировки нет. После примерно €5 приложение один раз предупредит, но продолжит анализ.",
            estimated
        )
        cost.setTextColor(Color.parseColor(
            if (DeepSeekCostWarningPolicy.warningReached(estimated)) "#FFD866" else "#8B949E"
        ))
    }

    private fun panel(size: Int, bold: Boolean) = label("", size, "#C9D1D9", bold).apply {
        setBackgroundColor(Color.parseColor("#161B22"))
        setPadding(dp(10), dp(10), dp(10), dp(10))
    }

    private fun label(text: String, size: Int, color: String, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(Color.parseColor(color))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun button(text: String, color: String) = Button(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor(color))
        textSize = 13f
        isAllCaps = false
    }

    private fun params(height: Int, top: Int = 0) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        if (height == -2) LinearLayout.LayoutParams.WRAP_CONTENT else height
    ).apply { topMargin = top }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun age(at: Long, now: Long) = if (at <= 0L || now < at) "—" else ((now - at) / 1_000L).toString()
    private fun signed(value: Int) = if (value >= 0) "+$value" else value.toString()
}

class CriticalOverviewChartView(context: android.content.Context) : View(context) {
    private val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#101820") }
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#30363D"); strokeWidth = dp(1f) }
    private val positive = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#238636") }
    private val negative = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#B62324") }
    private val neutral = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6E7681") }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = sp(11f); isFakeBoldText = true }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#8B949E"); textSize = sp(9f) }
    private val pumpLine = linePaint("#7EE787")
    private val bookLine = linePaint("#79C0FF")
    private val bitcoinLine = linePaint("#FFD866")
    private var model: CriticalOverviewModel? = null
    private var history: List<CriticalOverviewHistoryPoint> = emptyList()

    fun setData(model: CriticalOverviewModel, history: List<CriticalOverviewHistoryPoint>) {
        this.model = model
        this.history = history
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(background.color)
        val current = model ?: return
        val left = dp(12f)
        val right = width - dp(12f)
        val center = (left + right) / 2f
        canvas.drawText("КРАСНЫЙ  ←  УХУДШЕНИЕ", left, dp(18f), small)
        small.textAlign = Paint.Align.RIGHT
        canvas.drawText("УЛУЧШЕНИЕ  →  ЗЕЛЁНЫЙ", right, dp(18f), small)
        small.textAlign = Paint.Align.LEFT

        var y = dp(42f)
        current.metrics.forEach { metric ->
            canvas.drawText(metric.title, left, y, text)
            text.textAlign = Paint.Align.RIGHT
            canvas.drawText(metric.score?.let { if (it >= 0) "+$it" else "$it" } ?: "—", right, y, text)
            text.textAlign = Paint.Align.LEFT
            val barTop = y + dp(7f)
            val barBottom = barTop + dp(13f)
            canvas.drawRect(left, barTop, center, barBottom, Paint(negative).apply { alpha = 55 })
            canvas.drawRect(center, barTop, right, barBottom, Paint(positive).apply { alpha = 55 })
            canvas.drawLine(center, barTop - dp(2f), center, barBottom + dp(2f), grid)
            metric.score?.let { score ->
                val end = center + (right - left) / 2f * score / 100f
                canvas.drawRect(minOf(center, end), barTop, maxOf(center, end), barBottom, if (score >= 0) positive else negative)
            } ?: canvas.drawCircle(center, (barTop + barBottom) / 2f, dp(3f), neutral)
            small.textAlign = Paint.Align.RIGHT
            canvas.drawText(metric.detail, right, barBottom + dp(11f), small)
            small.textAlign = Paint.Align.LEFT
            y += dp(57f)
        }

        val plotTop = y + dp(6f)
        val plotBottom = height - dp(24f)
        canvas.drawText("СЫРОЙ МГНОВЕННЫЙ ПОТОК ЭТОГО ЭКРАНА", left, plotTop - dp(7f), text)
        repeat(3) { index ->
            val gy = plotTop + (plotBottom - plotTop) * index / 2f
            canvas.drawLine(left, gy, right, gy, grid)
        }
        small.color = pumpLine.color
        canvas.drawText("PUMP", left, height - dp(7f), small)
        small.color = bookLine.color
        canvas.drawText("СТАКАН", left + dp(57f), height - dp(7f), small)
        small.color = bitcoinLine.color
        canvas.drawText("BTC", left + dp(128f), height - dp(7f), small)
        small.color = Color.parseColor("#8B949E")
        if (history.size < 2 || plotBottom <= plotTop) {
            canvas.drawText("Накапливаем точки каждые 2 секунды", left, (plotTop + plotBottom) / 2f, small)
            return
        }
        drawHistory(canvas, history.map { it.pump }, pumpLine, left, right, plotTop, plotBottom)
        drawHistory(canvas, history.map { it.book }, bookLine, left, right, plotTop, plotBottom)
        drawHistory(canvas, history.map { it.bitcoin }, bitcoinLine, left, right, plotTop, plotBottom)
    }

    private fun drawHistory(
        canvas: Canvas,
        values: List<Int?>,
        paint: Paint,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float
    ) {
        val path = Path()
        var started = false
        values.forEachIndexed { index, value ->
            if (value == null) {
                started = false
            } else {
                val x = left + (right - left) * index / max(1, values.lastIndex).toFloat()
                val y = bottom - (value.coerceIn(-100, 100) + 100f) / 200f * (bottom - top)
                if (!started) path.moveTo(x, y) else path.lineTo(x, y)
                started = true
            }
        }
        canvas.drawPath(path, paint)
    }

    private fun linePaint(color: String) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = Color.parseColor(color)
        strokeWidth = dp(2f)
        style = Paint.Style.STROKE
        isFakeBoldText = true
        textSize = sp(9f)
    }
    private fun dp(value: Float) = value * resources.displayMetrics.density
    private fun sp(value: Float) = value * resources.displayMetrics.scaledDensity
}

class MarketBreathingChartView(context: android.content.Context) : View(context) {
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#30363D") }
    private val positive = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#238636") }
    private val negative = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#B62324") }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C9D1D9")
        textSize = sp(11f)
        isFakeBoldText = true
    }
    private val detail = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8B949E")
        textSize = sp(9f)
    }
    private var snapshot = LiveMarketBreathingSnapshot()

    fun setData(snapshot: LiveMarketBreathingSnapshot) {
        this.snapshot = snapshot
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#101820"))
        val left = dp(12f)
        val right = width - dp(12f)
        val center = (left + right) / 2f
        val rows = listOf(
            Triple("СЕЙЧАС", snapshot.instantScore, null),
            *snapshot.horizons.map { horizon ->
                Triple(
                    when (horizon.minutes) {
                        60 -> "1 ЧАС"
                        360 -> "6 ЧАСОВ"
                        else -> "${horizon.minutes} МИН."
                    },
                    horizon.score,
                    horizon
                )
            }.toTypedArray()
        )
        var y = dp(31f)
        rows.forEach { (title, score, horizon) ->
            canvas.drawText(title, left, y, label)
            label.textAlign = Paint.Align.RIGHT
            canvas.drawText(score?.let { if (it >= 0) "+$it" else "$it" } ?: "—", right, y, label)
            label.textAlign = Paint.Align.LEFT
            val top = y + dp(7f)
            val bottom = top + dp(12f)
            canvas.drawRect(left, top, right, bottom, grid)
            canvas.drawLine(center, top - dp(2f), center, bottom + dp(2f), detail)
            score?.let {
                val end = center + (right - left) / 2f * it.coerceIn(-100, 100) / 100f
                canvas.drawRect(minOf(center, end), top, maxOf(center, end), bottom, if (it >= 0) positive else negative)
            }
            horizon?.let {
                detail.textAlign = Paint.Align.RIGHT
                canvas.drawText(
                    "устойчивость ${it.persistencePercent}% • ${it.samples} точек",
                    right,
                    bottom + dp(11f),
                    detail
                )
                detail.textAlign = Paint.Align.LEFT
            }
            y += dp(50f)
        }
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density
    private fun sp(value: Float) = value * resources.displayMetrics.scaledDensity
}
