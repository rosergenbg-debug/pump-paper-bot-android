package com.example.pumppaperbot

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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
import kotlin.math.roundToInt

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
    private lateinit var flowWaveChart: FlowClockView
    private lateinit var largeFlow: TextView
    private lateinit var liquidityRelease: TextView
    private lateinit var breathTiming: TextView
    private lateinit var entryAudit: TextView
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
        content.addView(breathingChart, params(dp(380), dp(6)))
        content.addView(label(
            "ЧАСЫ РЫНОЧНОГО ПОТОКА",
            18,
            "#F0F6FC",
            true
        ), params(-2, dp(10)))
        content.addView(label(
            "Четыре отдельные дуги не накладываются друг на друга: 5 мин быстро реагируют, 15 мин показывают основное движение, 30 и 60 мин подтверждают фон. Маркеры плавно перетекают между продавцами и покупателями.",
            12,
            "#8B949E",
            false
        ), params(-2, dp(3)))
        flowWaveChart = FlowClockView(this)
        content.addView(flowWaveChart, params(dp(430), dp(6)))
        breathTiming = panel(14, true)
        content.addView(breathTiming, params(-2, dp(6)))
        largeFlow = panel(14, true)
        content.addView(largeFlow, params(-2, dp(6)))
        liquidityRelease = panel(14, true)
        content.addView(liquidityRelease, params(-2, dp(6)))
        entryAudit = panel(14, false)
        content.addView(entryAudit, params(-2, dp(6)))
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
        val capitalFlow = CapitalFlowProxyPolicy.evaluate(impulse, breathing, now)
        val audit = EntryOpportunityAuditStore.latest(this)
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
        flowWaveChart.setData(breathing)
        breathTiming.text = ContinuousFlowWaveText.describe(breathing)
        largeFlow.text = LargeFlowFingerprintText.describe(micro.largeFlow)
        liquidityRelease.text = LiquidityReleaseShadowText.describe(LiquidityReleaseShadowStore.latest(this))
        entryAudit.text = buildString {
            append("ПОЧЕМУ СИСТЕМЫ ВОШЛИ ИЛИ НЕ ВОШЛИ")
            if (audit.at <= 0L || audit.participants.isEmpty()) {
                append("\nЖурнал причин появится после следующего полного цикла проверки.")
            } else {
                audit.participants.forEach { participant ->
                    append("\n\n${participant.participant}: ${participant.state}")
                    if (participant.confirmationsRequired > 0) {
                        append(" • подтверждения ${participant.confirmations}/${participant.confirmationsRequired}")
                    }
                    append("\n${participant.reason}")
                }
                append("\n\nЭто фактическое состояние защит в момент последнего цикла, а не догадка задним числом.")
            }
        }

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
            append("\nВремя: ${breathing.buyerBreath.timing.status}")
            append("\n\nКРУПНЫЙ ПОТОК — ОЦЕНКА МЕХАНИЗМА, НЕ ЛИЧНОСТИ")
            append("\n${capitalFlow.title} • ${signed(capitalFlow.score)}/100 • уверенность ${capitalFlow.confidence}/100")
            append("\n${capitalFlow.explanation}")
            append("\n${capitalFlow.identityNote}")
            append("\nНакоплено ${breathing.historyMinutes} мин.; данные старше 24 часов удаляются.")
            if (!breathing.fresh) append("\nВнимание: сглаженное дыхание устарело и не используется DeepSeek.")
            append("\n\nСвежесть: микросделки ${age(micro.updatedAt, now)} сек • 5‑мин данные ${age(impulse.candleTime, now)} сек")
            if (!microFresh) append("\nВнимание: живой поток сейчас устарел или переподключается.")
        }
        deepSeek.text = buildString {
            val entryCoach = DeepSeekEntryCoachStore.state(this@CriticalOverviewActivity)
            val entryTuning = DeepSeekEntryCoachStore.tuning(this@CriticalOverviewActivity)
            append("DEEPSEEK • ${level.level}/10 • ${level.label}\n${level.detail}")
            append("\n\nПредложение: ${primary.proposedAction} • итог: ${primary.action}")
            append("\nИсполнение: ${primary.executionStatus}")
            append("\nПроверка: ${primary.verificationSummary}")
            append("\n\n${primary.summary}")
            if (primary.evidence.isNotEmpty()) append("\nФакты: ${primary.evidence.joinToString("; ")}")
            if (primary.risks.isNotEmpty()) append("\nРиски: ${primary.risks.joinToString("; ")}")
            append("\n\nПРЕДВХОДНЫЙ AI-КОНТРОЛЬ: ${entryCoach.status} • ${entryCoach.verdict}")
            append(" • ${entryCoach.confidence}/100 • ${entryCoach.stage}")
            append("\n${entryCoach.reason}")
            if (entryCoach.status == "PAUSED_BALANCE") {
                append("\nDEEPSEEK НЕ ТРАТИТ ЗАПРОСЫ: API сообщил о недостатке средств; следующая проба не раньше паузы.")
            }
            append("\nЭкономный режим: до 6 предвходных запросов/сутки, не чаще одного за 15 минут, без платного автоповтора.")
            append("\n8 мягких регуляторов, ревизия ${entryTuning.revision}: ${entryTuning.compact()}")
            val tuningTrial = DeepSeekTuningTrialStore.state(this@CriticalOverviewActivity)
            append("\nАВТОЮСТИРОВКА V5.36: ${tuningTrial.status} • ${tuningTrial.observedTrades}/${DeepSeekAdaptiveTuningPolicy.STANDARD_TRIAL_TRADES} сделок")
            append("\n${tuningTrial.reason}")
            append("\nDeepSeek не может отменять запрет по устаревшим данным, спреду, поздней фазе или давлению продавцов.")
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
            "РАСХОД DEEPSEEK СЕГОДНЯ: $%.4f\nЭкономный режим V5.35: обычный обзор ограничен, предвходный контролёр вызывается только для готового кандидата.",
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

object ContinuousFlowWaveText {
    fun describe(snapshot: LiveMarketBreathingSnapshot): String = buildString {
        val wave = snapshot.flowWave
        val latest = wave.latest
        append("СЕЙЧАС: ").append(wave.state)
        latest?.let {
            append("\n5м ").append(signed(it.score5m))
            append("\n15м ").append(signed(it.score15m))
            append(" • 30м ").append(signed(it.score30m))
            append(" • 1ч ").append(signed(it.score60m))
        }
        append("\nЧТО ДЕЛАТЬ: ").append(wave.guidance)
        append("\nЗелёные/красные метки — зоны проверки разворота потока, не автоматические BUY/EXIT.")
        if (wave.staleSeconds > LiveMarketBreathingAnalyzer.MAX_LIVE_AGE_MILLIS / 1_000L) {
            append("\nПоток не обновлялся ").append(wave.staleSeconds).append(" сек.; история сохранена, новые решения заблокированы.")
        }
    }

    private fun signed(value: Int) = if (value >= 0) "+$value" else value.toString()
}

object LargeFlowFingerprintText {
    fun describe(value: LargeFlowFingerprint): String = buildString {
        append("КРУПНЫЙ ПОТОК • ВИДИМЫЙ ОТПЕЧАТОК\n")
        append(value.title).append(" • уверенность ").append(value.confidence).append("/100")
        append(String.format(Locale.GERMANY, "\nДинамический порог крупной заявки: $%,.0f", value.thresholdUsdt))
        append(String.format(Locale.GERMANY, "\nКрупные покупки за 5 мин: $%,.0f • продажи: $%,.0f", value.largeBuyUsdt, value.largeSellUsdt))
        append(String.format(Locale.GERMANY, "\nКрупнейшая покупка: $%,.0f • продажа: $%,.0f", value.largestBuyUsdt, value.largestSellUsdt))
        append("\nПочерк: ").append(value.fingerprint)
        append("\n").append(value.explanation)
        append("\nВажно: биржа не показывает имя владельца. Похожая сумма и ритм — косвенный признак алгоритма, а не доказательство, что это один человек или фирма.")
    }
}

class FlowClockView(context: android.content.Context) : View(context) {
    private val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#101820") }
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#30363D"); style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F0F6FC"); textSize = sp(11f); isFakeBoldText = true
    }
    private val detail = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#8B949E"); textSize = sp(9f) }
    private val red = Color.parseColor("#F85149")
    private val green = Color.parseColor("#3FB950")
    private val neutral = Color.parseColor("#8B949E")
    private val ringColors = intArrayOf(
        Color.parseColor("#FF8B3D"), Color.parseColor("#FFD866"),
        Color.parseColor("#58A6FF"), Color.parseColor("#BC8CFF")
    )
    private var snapshot = LiveMarketBreathingSnapshot()
    private val displayed = FloatArray(4)
    private val targets = FloatArray(4)
    private var initialized = false

    fun setData(snapshot: LiveMarketBreathingSnapshot) {
        this.snapshot = snapshot
        snapshot.flowWave.latest?.let {
            val next = floatArrayOf(it.score5m.toFloat(), it.score15m.toFloat(), it.score30m.toFloat(), it.score60m.toFloat())
            next.copyInto(targets)
            if (!initialized) { next.copyInto(displayed); initialized = true }
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(background.color)
        var moving = false
        for (i in displayed.indices) {
            val delta = targets[i] - displayed[i]
            if (kotlin.math.abs(delta) > 0.15f) { displayed[i] += delta * 0.14f; moving = true }
        }
        val cx = width / 2f
        val cy = dp(245f)
        val maxRadius = minOf(width / 2f - dp(24f), dp(190f))
        val radii = floatArrayOf(maxRadius, maxRadius - dp(28f), maxRadius - dp(56f), maxRadius - dp(84f))
        detail.textAlign = Paint.Align.LEFT
        detail.color = red; canvas.drawText("ПРОДАВЦЫ", dp(12f), dp(25f), detail)
        detail.textAlign = Paint.Align.CENTER; detail.color = neutral; canvas.drawText("РАВНОВЕСИЕ", cx, dp(25f), detail)
        detail.textAlign = Paint.Align.RIGHT; detail.color = green; canvas.drawText("ПОКУПАТЕЛИ", width - dp(12f), dp(25f), detail)
        val names = arrayOf("5 МИН", "15 МИН • ОСНОВНОЙ", "30 МИН", "60 МИН")
        radii.forEachIndexed { index, radius ->
            track.strokeWidth = dp(if (index == 1) 11f else 8f)
            track.color = Color.parseColor("#30363D")
            val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            canvas.drawArc(oval, 180f, 180f, false, track)
            val score = displayed[index].coerceIn(-100f, 100f)
            val sweep = (score + 100f) / 200f * 180f
            track.color = if (score > 5f) green else if (score < -5f) red else neutral
            canvas.drawArc(oval, 270f, sweep - 90f, false, track)
            val angle = Math.toRadians((180f + sweep).toDouble())
            val x = cx + kotlin.math.cos(angle).toFloat() * radius
            val y = cy + kotlin.math.sin(angle).toFloat() * radius
            val marker = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ringColors[index]; style = Paint.Style.FILL }
            canvas.drawCircle(x, y, dp(if (index == 1) 7f else 6f), marker)
        }
        detail.textAlign = Paint.Align.CENTER; detail.color = neutral
        canvas.drawText("−100", dp(24f), cy + dp(17f), detail)
        canvas.drawText("0", cx, cy - maxRadius - dp(8f), detail)
        canvas.drawText("+100", width - dp(24f), cy + dp(17f), detail)
        val rowTop = dp(278f)
        names.forEachIndexed { index, name ->
            val y = rowTop + index * dp(32f)
            val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ringColors[index] }
            canvas.drawCircle(dp(20f), y - dp(4f), dp(4f), dot)
            label.textAlign = Paint.Align.LEFT; label.color = Color.parseColor("#F0F6FC")
            canvas.drawText(name, dp(32f), y, label)
            label.textAlign = Paint.Align.RIGHT
            label.color = if (displayed[index] > 5) green else if (displayed[index] < -5) red else neutral
            canvas.drawText("${signed(displayed[index].roundToInt())} • ${phase(displayed[index].roundToInt())}", width - dp(12f), y, label)
        }
        detail.textAlign = Paint.Align.CENTER; detail.color = Color.parseColor("#8B949E")
        val freshness = if (snapshot.fresh) "ЖИВОЙ ПОТОК" else "ДАННЫЕ УСТАРЕЛИ — НОВЫЕ РЕШЕНИЯ ЗАБЛОКИРОВАНЫ"
        canvas.drawText(freshness, cx, height - dp(12f), detail)
        if (moving) postInvalidateOnAnimation()
    }

    private fun phase(score: Int) = when {
        score >= 35 -> "СИЛЬНЫЕ ПОКУПКИ"
        score >= 8 -> "ПОКУПКИ"
        score <= -35 -> "ПРОДАВЦЫ ДАВЯТ"
        score <= -8 -> "ПРОДАЖИ"
        else -> "РАВНОВЕСИЕ"
    }

    private fun signed(value: Int) = if (value >= 0) "+$value" else value.toString()
    private fun dp(value: Float) = value * resources.displayMetrics.density
    private fun sp(value: Float) = value * resources.displayMetrics.scaledDensity
}
