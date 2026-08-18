package com.example.pumppaperbot

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

class BuyerBreathActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var headline: TextView
    private lateinit var gauge: BuyerBreathGaugeView
    private lateinit var details: TextView
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
            setPadding(dp(14), dp(12), dp(14), dp(28))
            setBackgroundColor(Color.parseColor("#0D1117"))
        }
        content.addView(button("← НАЗАД").apply { setOnClickListener { finish() } }, params(dp(48)))
        content.addView(label("ДЫХАНИЕ РЫНКА", 26, "#F0F6FC", true), params(-2, dp(9)))
        content.addView(label(
            "Живой цикл покупательского напора: начало, разгон, зрелость, выдыхание и захват продавцами. Работает до и после «Я купил».",
            14, "#8B949E", false
        ), params(-2, dp(5)))
        headline = panel(20, true)
        content.addView(headline, params(-2, dp(10)))
        gauge = BuyerBreathGaugeView(this)
        content.addView(gauge, params(dp(500), dp(8)))
        details = panel(15, false).apply { setTextIsSelectable(true) }
        content.addView(details, params(-2, dp(8)))
        content.addView(label(
            "Важно: высокий процент покупок означает сторону агрессора, но не гарантирует рост. Если цена не отвечает, модуль показывает поглощение. Окончательный EXIT требует правил защиты позиции; резкий шок обрабатывается отдельно.",
            13, "#FFD866", false
        ), params(-2, dp(10)))
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
        val breathing = LiveMarketBreathingStore.snapshot(this, System.currentTimeMillis())
        val cycle = breathing.buyerBreath
        headline.text = buildString {
            append(cycle.title)
            append("\n")
            append(if (cycle.fresh) "Уверенность данных ${cycle.confidence}/100" else "Поток устарел — ждём подключение")
            append(" • история ${breathing.historyMinutes} мин")
        }
        val colors = when (cycle.phase) {
            BuyerBreathPhase.IGNITION, BuyerBreathPhase.EXPANSION -> "#7EE787" to "#14351F"
            BuyerBreathPhase.MATURE, BuyerBreathPhase.QUIET -> "#FFD866" to "#3A300F"
            BuyerBreathPhase.EXHAUSTION, BuyerBreathPhase.SELLER_TAKEOVER, BuyerBreathPhase.SHOCK ->
                "#FF7B72" to "#3A171A"
            BuyerBreathPhase.STALE -> "#8B949E" to "#161B22"
        }
        headline.setTextColor(Color.parseColor(colors.first))
        headline.setBackgroundColor(Color.parseColor(colors.second))
        gauge.setData(cycle, breathing.horizons)
        details.text = BuyerBreathText.detailed(cycle)
    }

    private fun panel(size: Int, bold: Boolean) = label("", size, "#C9D1D9", bold).apply {
        setBackgroundColor(Color.parseColor("#161B22"))
        setPadding(dp(12), dp(12), dp(12), dp(12))
        setLineSpacing(0f, 1.12f)
    }

    private fun label(text: String, size: Int, color: String, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(Color.parseColor(color))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun button(text: String) = Button(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor("#30363D"))
        textSize = 13f
        isAllCaps = false
    }

    private fun params(height: Int, top: Int = 0) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        if (height == -2) LinearLayout.LayoutParams.WRAP_CONTENT else height
    ).apply { topMargin = top }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

class BuyerBreathGaugeView(context: android.content.Context) : View(context) {
    private val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#101820") }
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#30363D") }
    private val green = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#238636") }
    private val red = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#B62324") }
    private val yellow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#9A6700") }
    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = sp(11f); isFakeBoldText = true
    }
    private val muted = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#8B949E"); textSize = sp(9f) }
    private var cycle = BuyerBreathSnapshot()
    private var horizons: List<LiveBreathingHorizon> = emptyList()

    fun setData(cycle: BuyerBreathSnapshot, horizons: List<LiveBreathingHorizon>) {
        this.cycle = cycle
        this.horizons = horizons
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), background)
        val left = dp(14f)
        val right = width - dp(14f)
        val center = (left + right) / 2f
        canvas.drawText("+ ПОКУПКИ", left, dp(24f), white)
        white.textAlign = Paint.Align.RIGHT
        canvas.drawText("ПРОДАЖИ −", right, dp(24f), white)
        white.textAlign = Paint.Align.LEFT
        drawSignedBar(canvas, "НАПОР", cycle.pressureScore, dp(48f), left, right, center)
        drawSignedBar(canvas, "ЭФФЕКТИВНОСТЬ ЦЕНЫ", cycle.efficiencyScore, dp(96f), left, right, center)
        drawAbsorption(canvas, dp(144f), left, right)
        drawTimeline(canvas, dp(205f), left, right)
        var y = dp(284f)
        listOf(5, 15, 30, 60).forEach { minutes ->
            val score = horizons.firstOrNull { it.minutes == minutes }?.score
            drawSignedBar(canvas, if (minutes == 60) "1 ЧАС" else "$minutes МИН.", score, y, left, right, center)
            y += dp(46f)
        }
    }

    private fun drawSignedBar(canvas: Canvas, title: String, score: Int?, y: Float, left: Float, right: Float, center: Float) {
        canvas.drawText(title, left, y, white)
        white.textAlign = Paint.Align.RIGHT
        canvas.drawText(score?.let { if (it >= 0) "+$it" else "$it" } ?: "—", right, y, white)
        white.textAlign = Paint.Align.LEFT
        val top = y + dp(8f)
        val bottom = top + dp(15f)
        canvas.drawRect(left, top, right, bottom, grid)
        canvas.drawRect(center - dp(1f), top - dp(3f), center + dp(1f), bottom + dp(3f), muted)
        score?.let {
            // Serge requested the positive/buyer side on the left and the negative/seller side on the right.
            val extent = (right - left) / 2f * abs(it.coerceIn(-100, 100)) / 100f
            if (it >= 0) canvas.drawRect(center - extent, top, center, bottom, green)
            else canvas.drawRect(center, top, center + extent, bottom, red)
        }
    }

    private fun drawAbsorption(canvas: Canvas, y: Float, left: Float, right: Float) {
        canvas.drawText("ПОГЛОЩЕНИЕ ПОКУПОК", left, y, white)
        white.textAlign = Paint.Align.RIGHT
        canvas.drawText("${cycle.absorptionRisk}/100", right, y, white)
        white.textAlign = Paint.Align.LEFT
        val top = y + dp(8f)
        val bottom = top + dp(15f)
        canvas.drawRect(left, top, right, bottom, grid)
        val end = left + (right - left) * cycle.absorptionRisk.coerceIn(0, 100) / 100f
        canvas.drawRect(left, top, end, bottom, when {
            cycle.absorptionRisk >= 65 -> red
            cycle.absorptionRisk >= 35 -> yellow
            else -> green
        })
    }

    private fun drawTimeline(canvas: Canvas, y: Float, left: Float, right: Float) {
        canvas.drawText("ЦИКЛ", left, y, white)
        val phases = listOf(
            BuyerBreathPhase.QUIET to "ПОКОЙ",
            BuyerBreathPhase.IGNITION to "НАЧАЛО",
            BuyerBreathPhase.EXPANSION to "ВДОХ",
            BuyerBreathPhase.MATURE to "ЗРЕЛОСТЬ",
            BuyerBreathPhase.EXHAUSTION to "ВЫДОХ",
            BuyerBreathPhase.SELLER_TAKEOVER to "ПРОДАЖИ"
        )
        val lineY = y + dp(28f)
        canvas.drawRect(left, lineY - dp(1f), right, lineY + dp(1f), grid)
        phases.forEachIndexed { index, (phase, label) ->
            val x = left + (right - left) * index / (phases.size - 1).toFloat()
            val active = cycle.phase == phase || cycle.phase == BuyerBreathPhase.SHOCK && phase == BuyerBreathPhase.SELLER_TAKEOVER
            canvas.drawCircle(x, lineY, dp(if (active) 8f else 5f), if (active) {
                when (phase) {
                    BuyerBreathPhase.IGNITION, BuyerBreathPhase.EXPANSION -> green
                    BuyerBreathPhase.MATURE, BuyerBreathPhase.QUIET -> yellow
                    else -> red
                }
            } else grid)
            muted.textAlign = when (index) {
                0 -> Paint.Align.LEFT
                phases.lastIndex -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            canvas.drawText(label, x, lineY + dp(20f), muted)
        }
        muted.textAlign = Paint.Align.LEFT
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density
    private fun sp(value: Float) = value * resources.displayMetrics.scaledDensity
}
