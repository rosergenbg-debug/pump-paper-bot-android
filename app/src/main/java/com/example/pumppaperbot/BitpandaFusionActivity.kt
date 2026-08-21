package com.example.pumppaperbot

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import java.util.concurrent.Executors

class BitpandaFusionActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private lateinit var keyStatus: TextView
    private lateinit var input: EditText
    private lateinit var editor: LinearLayout
    private lateinit var keyActions: LinearLayout
    private lateinit var status: TextView
    private lateinit var account: TextView
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(28))
            setBackgroundColor(Color.parseColor("#0D1117"))
        }
        content.addView(button("← НАЗАД", "#30363D").apply { setOnClickListener { finish() } }, params(50))
        content.addView(label("BITPANDA FUSION • READ-ONLY", 24, "#F0F6FC", true))
        content.addView(label(
            "V${BuildConfig.VERSION_NAME} использует ключ только для чтения стакана PUMP-EUR. Реальные заявки, отмена заявок, переводы и вывод средств в приложении не реализованы. Создавайте ключ только со scope Read — без Trade и Transfer.",
            14, "#C9D1D9", false
        ))
        keyStatus = label("", 15, "#C9D1D9", true)
        content.addView(keyStatus, params(48, 8))
        input = EditText(this).apply {
            hint = "Вставьте Bitpanda Fusion API-ключ (только Read)"
            setHintTextColor(Color.parseColor("#8B949E")); setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#161B22"))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(dp(10), 0, dp(10), 0)
        }
        editor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(input, params(58, 4))
            addView(button("СОХРАНИТЬ ЗАЩИЩЁННЫЙ КЛЮЧ", "#238636").apply { setOnClickListener { saveKey() } }, params(54, 4))
        }
        content.addView(editor)
        keyActions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(button("ИЗМЕНИТЬ", "#1F6FEB").apply { setOnClickListener { editor.visibility = View.VISIBLE; keyActions.visibility = View.GONE } }, LinearLayout.LayoutParams(0, dp(54), 1f))
            addView(button("УДАЛИТЬ", "#DA3633").apply { setOnClickListener { confirmDelete() } }, LinearLayout.LayoutParams(0, dp(54), 1f).apply { leftMargin = dp(8) })
        }
        content.addView(keyActions, params(54, 4))
        content.addView(button("ПРОВЕРИТЬ READ-ONLY СЕЙЧАС", "#7C3AED").apply { setOnClickListener { testNow() } }, params(58, 8))
        progress = ProgressBar(this).apply { visibility = View.GONE }
        content.addView(progress, params(36, 2))
        status = panel(); content.addView(status, params(-2, 8))
        content.addView(label("DEEPSIG FUSION • ВИРТУАЛЬНЫЙ СЧЁТ", 19, "#F0F6FC", true))
        account = panel(); content.addView(account, params(-2, 5))
        content.addView(button("ЭКСПОРТ ЕДИНОГО ЛОГА", "#1F6FEB").apply {
            setOnClickListener { runCatching { UnifiedResearchLog.share(this@BitpandaFusionActivity) }
                .onFailure { Toast.makeText(this@BitpandaFusionActivity, it.message ?: "Ошибка экспорта", Toast.LENGTH_LONG).show() } }
        }, params(56, 8))
        content.addView(button("СБРОСИТЬ ТОЛЬКО FUSIONSIM", "#8B1E1E").apply { setOnClickListener { confirmReset() } }, params(54, 4))
        content.addView(label(
            "Fusion остаётся полностью виртуальным. В V${BuildConfig.VERSION_NAME} симулятор считает 0,25% на входе и 0,25% на выходе; реальные торговые команды Bitpanda не реализованы.",
            13, "#8B949E", false
        ))
        setContentView(ScrollView(this).apply { addView(content) })
        updateUi()
    }

    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    private fun saveKey() {
        val value = input.text.toString().trim()
        if (value.isBlank()) return
        if (!BitpandaFusionSecureKeyStore.save(this, value)) {
            Toast.makeText(this, "Android не смог защитить ключ", Toast.LENGTH_LONG).show(); return
        }
        input.setText(""); updateUi(); testNow()
    }

    private fun confirmDelete() = AlertDialog.Builder(this)
        .setTitle("Удалить ключ Bitpanda?")
        .setMessage("Доступ к котировкам прекратится. Виртуальный счёт и его история сохранятся.")
        .setPositiveButton("Удалить") { _, _ ->
            BitpandaFusionSecureKeyStore.save(this, ""); BitpandaFusionStore.clear(this); updateUi()
        }.setNegativeButton("Отмена", null).show()

    private fun confirmReset() = AlertDialog.Builder(this)
        .setTitle("Сбросить FusionSim?")
        .setMessage("Будут удалены только виртуальные деньги, сделки, решения и состояние динамического stop FusionSim. Остальные счета не изменятся.")
        .setPositiveButton("Сбросить") { _, _ -> FusionSimStore.reset(this); updateUi() }
        .setNegativeButton("Отмена", null).show()

    private fun testNow() {
        if (BitpandaFusionSecureKeyStore.read(this).isBlank()) {
            Toast.makeText(this, "Сначала введите read-only ключ", Toast.LENGTH_LONG).show(); return
        }
        progress.visibility = View.VISIBLE
        executor.execute {
            BitpandaFusionClient().sync(this, force = true)
            main.post { progress.visibility = View.GONE; updateUi() }
        }
    }

    private fun updateUi() {
        val configured = BitpandaFusionSecureKeyStore.read(this).isNotBlank()
        editor.visibility = if (configured) View.GONE else View.VISIBLE
        keyActions.visibility = if (configured) View.VISIBLE else View.GONE
        keyStatus.text = if (configured) "Ключ защищён Android Keystore • значение скрыто" else "Ключ ещё не введён"
        val s = BitpandaFusionStore.state(this)
        val imbalance = if (s.bidDepthEur + s.askDepthEur > 0.0) {
            (s.bidDepthEur - s.askDepthEur) / (s.bidDepthEur + s.askDepthEur) * 100.0
        } else 0.0
        status.text = if (!configured) {
            "Статус: ждём ключ Read\nСимулятор остаётся в EUR и не торгует."
        } else if (s.connected) {
            "Статус: READ-ONLY ПОДКЛЮЧЁН\nПара: ${s.pair}\nBid: ${money(s.bid)} • Ask: ${money(s.ask)}\n" +
                "Спред: ${String.format(Locale.US, "%.3f", s.spreadPercent)}%\n" +
                "Баланс глубины: ${String.format(Locale.US, "%+.1f", imbalance)}%\n" +
                "Рекомендация исполнения: ${venueAdvice(s.spreadPercent, imbalance)}\n" +
                "Торговые права: НЕ ИСПОЛЬЗУЮТСЯ"
        } else "Статус: НЕТ СВЕЖИХ ДАННЫХ\n${s.error.ifBlank { "Нажмите проверку API" }}"

        val p = FusionSimStore.state(this)
        val guard = FusionSimStore.stability(this)
        val mark = if (s.fresh()) s.bid else s.mid
        val priority = FusionPriorityPolicy.plan(p)
        val metrics = FusionPriorityPolicy.metrics(p, mark, s.feeRate, s.fresh())
        val breathing = LiveMarketBreathingStore.snapshot(this)
        val frame = FusionFlowPolicy.frame(breathing)
        val flowNow = if (frame == null) {
            "мгн/5/15/20/30: —"
        } else {
            "мгн ${signed(frame.instant)} • 5м ${signed(frame.score5m)} • 15м ${signed(frame.score15m)} • " +
                "20м ${signed(frame.score20m)} • 30м ${signed(frame.score30m)}"
        }
        val activeStop = if (p.inPosition) {
            FusionRiskPolicy.activeStopPrice(p.entryPrice, guard.peakBid, s.feeRate)
        } else 0.0
        val stopText = if (p.inPosition && activeStop > 0.0) {
            "${money(activeStop)} • пик bid ${money(guard.peakBid)}"
        } else "не активен до следующего BUY"
        val smoothing = when {
            guard.exitArmed -> "EXIT ВООРУЖЁН • ждём фактическое движение bid вниз"
            guard.entryStreak > 0 -> "BUY-КАНДИДАТ ${guard.entryStreak}/${FusionStabilityPolicy.ENTRY_CONFIRMATIONS}"
            else -> "спокойное наблюдение"
        }

        account.text = "Старт: €1 000,00\nEUR: ${eur(p.cashEur)}\nPUMP: ${String.format(Locale.US, "%.4f", p.pumpAmount)}\n" +
            "Режим: ${if (priority.active) priority.label else "обычное автономное наблюдение"}\n" +
            "Чистая стоимость при виртуальном выходе: ${eur(metrics.netLiquidationValueEur)}\n" +
            "Чистый результат: ${signedEur(metrics.netPnlEur)}\n" +
            "Цена результата: ${if (metrics.venueFresh) "свежий Fusion bid" else "справочная, исполнение заблокировано"}\n" +
            "Откат от пика: ${String.format(Locale.US, "%.2f", metrics.pullbackFromPeakPercent)}%\n" +
            "Комиссия симуляции: 0,25% вход + 0,25% выход • уплачено: ${eur(p.totalFeesEur)}\n" +
            "Динамический STOP: $stopText\n" +
            "Сглаживание: $smoothing\n" +
            "Потоки верхнего графика: $flowNow\n" +
            "BUY: мгн/5/15/30 > 0; сильное согласование входит сразу, слабое пересечение нуля подтверждается ещё раз. " +
            "EXIT: мгн/5/15/20 < 0 сначала вооружает выход; при боковике ждём, при реальном движении вниз продаём. " +
            "Начальный виртуальный stop −1,5%; после пика +0,60% он поднимается к безубытку и затем следует за максимумом."
    }

    private fun venueAdvice(spread: Double, imbalance: Double): String = when {
        spread > 1.0 -> "широкий спред — flow-сигнал может дать невыгодное виртуальное исполнение"
        imbalance < -35.0 -> "в ask-глубине перевес; вход требует дополнительного подтверждения"
        imbalance > 35.0 -> "в bid-глубине перевес; это подтверждение, но не самостоятельный BUY"
        else -> "стакан сбалансирован; направление задаёт отдельная flow-логика FusionSim"
    }

    private fun eur(v: Double) = String.format(Locale.GERMANY, "€%,.2f", v)
    private fun signedEur(v: Double) = String.format(Locale.GERMANY, "%+,.2f €", v)
    private fun signed(v: Int) = if (v >= 0) "+$v" else v.toString()
    private fun money(v: Double) = String.format(Locale.US, "€%.6f", v)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun params(height: Int, top: Int = 0) = LinearLayout.LayoutParams(-1, if (height < 0) -2 else dp(height)).apply { topMargin = dp(top) }
    private fun label(text: String, size: Int, color: String, bold: Boolean) = TextView(this).apply {
        this.text = text; textSize = size.toFloat(); setTextColor(Color.parseColor(color));
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD); setPadding(dp(4), dp(8), dp(4), dp(8))
    }
    private fun panel() = label("", 14, "#C9D1D9", false).apply { setBackgroundColor(Color.parseColor("#161B22")); setPadding(dp(12), dp(12), dp(12), dp(12)) }
    private fun button(text: String, color: String) = Button(this).apply {
        this.text = text; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor(color)); isAllCaps = false
    }
}
