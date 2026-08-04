package com.example.pumppaperbot

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
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

class ApiCenterActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private lateinit var provider: String
    private lateinit var keyStatus: TextView
    private lateinit var editor: LinearLayout
    private lateinit var input: EditText
    private lateinit var addKey: Button
    private lateinit var keyActions: LinearLayout
    private lateinit var providerToggle: Button
    private lateinit var liveStatus: TextView
    private lateinit var usage: TextView
    private lateinit var log: TextView
    private lateinit var progress: ProgressBar
    private val refresh = object : Runnable {
        override fun run() {
            updateUi()
            main.postDelayed(this, 5_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        provider = intent.getStringExtra(EXTRA_PROVIDER)?.uppercase(Locale.ROOT)
            ?.takeIf { it == DEEPSEEK || it == GEMINI } ?: DEEPSEEK
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(24))
            setBackgroundColor(Color.parseColor("#0D1117"))
        }
        content.addView(button("← НАЗАД", "#30363D").apply { setOnClickListener { finish() } }, params(dp(50)))
        content.addView(label("$provider • ЦЕНТР API", 25, "#F0F6FC", true))
        content.addView(label(roleText(), 14, "#C9D1D9", false))

        keyStatus = label("", 15, "#C9D1D9", true)
        content.addView(keyStatus, params(-2, dp(6)))
        createKeyControls(content)

        providerToggle = button("", if (provider == GEMINI) "#7C3AED" else "#238636").apply {
            visibility = View.GONE
        }
        content.addView(providerToggle, params(dp(54), dp(8)))

        content.addView(button("ПРОВЕРИТЬ API СЕЙЧАС", if (provider == DEEPSEEK) "#238636" else "#7C3AED").apply {
            setOnClickListener { testNow() }
        }, params(dp(58), dp(8)))
        content.addView(button("СКОПИРОВАТЬ ДИАГНОСТИКУ", "#1F6FEB").apply {
            setOnClickListener { copyDiagnostics() }
        }, params(dp(54), dp(4)))
        content.addView(button("ПОДЕЛИТЬСЯ ДИАГНОСТИКОЙ", "#1F6FEB").apply {
            setOnClickListener { shareDiagnostics() }
        }, params(dp(54), dp(4)))
        progress = ProgressBar(this).apply { visibility = View.GONE }
        content.addView(progress, params(dp(36), dp(2)))

        liveStatus = panel()
        content.addView(liveStatus, params(-2, dp(8)))
        usage = panel()
        content.addView(usage, params(-2, dp(8)))
        content.addView(label("ПОСЛЕДНИЕ ОБРАЩЕНИЯ", 19, "#F0F6FC", true))
        content.addView(label("Новые записи сверху. События старых версий сохранены для истории и помечены отдельно.", 13, "#8B949E", false))
        log = panel()
        content.addView(log, params(-2, dp(4)))
        setContentView(ScrollView(this).apply { addView(content) })
        updateUi()
    }

    override fun onResume() {
        super.onResume()
        main.post(refresh)
    }

    override fun onPause() {
        main.removeCallbacks(refresh)
        super.onPause()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun createKeyControls(content: LinearLayout) {
        input = EditText(this).apply {
            hint = "Вставьте личный $provider API-ключ"
            setHintTextColor(Color.parseColor("#8B949E"))
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#161B22"))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(dp(10), 0, dp(10), 0)
        }
        editor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(input, params(dp(58), dp(4)))
            addView(button("СОХРАНИТЬ API-КЛЮЧ", "#238636").apply {
                setOnClickListener { saveKey() }
            }, params(dp(54), dp(4)))
        }
        content.addView(editor)
        addKey = button("ВВЕСТИ API-КЛЮЧ", "#1F6FEB").apply {
            setOnClickListener {
                visibility = View.GONE
                editor.visibility = View.VISIBLE
                input.requestFocus()
            }
        }
        content.addView(addKey, params(dp(54), dp(4)))
        keyActions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(button("ИЗМЕНИТЬ API", "#1F6FEB").apply {
                setOnClickListener {
                    keyActions.visibility = View.GONE
                    editor.visibility = View.VISIBLE
                    input.setText("")
                    input.requestFocus()
                }
            }, LinearLayout.LayoutParams(0, dp(54), 1f))
            addView(button("УДАЛИТЬ", "#DA3633").apply {
                setOnClickListener { confirmDelete() }
            }, LinearLayout.LayoutParams(0, dp(54), 1f).apply { leftMargin = dp(8) })
        }
        content.addView(keyActions, params(dp(54), dp(4)))
    }

    private fun saveKey() {
        val clean = input.text.toString().trim()
        if (clean.isBlank()) {
            keyStatus.text = "Пустое поле не изменило сохранённый ключ"
            return
        }
        if (provider == DEEPSEEK) {
            if (!DeepSeekSecureKeyStore.save(this, clean)) {
                keyStatus.text = "Android не смог защитить ключ"
                return
            }
        } else {
            EventRadarStore.saveApiKey(this, clean)
            EventRadarStore.setUseAi(this, false)
            GeminiPaperStore.retireLegacyPendingDecision(this)
        }
        input.setText("")
        updateUi()
        testNow()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Удалить ключ $provider?")
            .setMessage("Новые обращения к $provider прекратятся. Рыночная история, сделки и остальные настройки сохранятся.")
            .setPositiveButton("Удалить") { _, _ ->
                if (provider == DEEPSEEK) {
                    DeepSeekSecureKeyStore.save(this, "")
                    DeepSeekConnectionStore.clear(this)
                } else {
                    EventRadarStore.saveApiKey(this, "")
                    EventRadarStore.setUseAi(this, false)
                    GeminiPaperStore.retireLegacyPendingDecision(this)
                }
                input.setText("")
                updateUi()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun testNow() {
        if (!configured()) {
            keyStatus.text = "Сначала введите API-ключ"
            return
        }
        progress.visibility = View.VISIBLE
        liveStatus.text = "$provider: выполняется контрольный запрос и расширенная самодиагностика…"
        executor.execute {
            val started = System.currentTimeMillis()
            runCatching {
                if (provider == DEEPSEEK) {
                    val key = DeepSeekSecureKeyStore.read(this)
                    DeepSeekKeyVerifier().verify(this, key)
                    DeepSeekPrimaryAnalyst().sync(this, force = true)
                } else {
                    EventRadarClient().testGemini(this)
                }
            }
            ProviderSelfDiagnostics.run(this, provider, started)
            main.post {
                progress.visibility = View.GONE
                updateUi()
            }
        }
    }

    private fun updateUi() {
        val configured = configured()
        editor.visibility = View.GONE
        addKey.visibility = if (configured) View.GONE else View.VISIBLE
        keyActions.visibility = if (configured) View.VISIBLE else View.GONE
        keyStatus.text = if (configured) "$provider • API-ключ защищён и сохранён" else "$provider • API-ключ не введён"
        renderStatus()
        renderUsage()
        renderLog()
    }

    private fun renderStatus() {
        if (provider == DEEPSEEK) {
            val state = DeepSeekPrimaryStore.state(this)
            val position = PositionSupervisorStore.state(this)
            val connection = DeepSeekConnectionStore.state(this)
            val actionLevel = DeepSeekActionLevelPolicy.fromMarket(
                PumpBotEngine.snapshot(this),
                state,
                MicroImpulseStore.state(this)
            )
            liveStatus.text = buildString {
                append("РОЛЬ: ОСНОВНОЙ АНАЛИТИК\n")
                append("Подключение: ")
                append(if (connection.lastSuccess > 0L && connection.error.isBlank()) "РАБОТАЕТ" else if (connection.error.isNotBlank()) "ОШИБКА: ${connection.error}" else "ожидает проверки")
                append("\nОсновной рынок: ${state.action} • направление ${signed(state.direction)}/100 • уверенность ${state.confidence}% • вход ${actionLevel.level}/10 • опасность ${state.danger}/10")
                append("\n${state.summary}")
                append("\nАктуальность сигнала: ${if (DeepSeekPrimaryPolicy.isFreshSignal(state)) "СВЕЖИЙ" else "УСТАРЕЛ — не используется на шкале"}")
                if (state.evidence.isNotEmpty()) append("\nФакты: ${state.evidence.joinToString("; ")}")
                if (state.risks.isNotEmpty()) append("\nРиски: ${state.risks.joinToString("; ")}")
                val interval = if (actionLevel.intensive) {
                    DeepSeekActionLevelPolicy.INTENSIVE_INTERVAL_MILLIS
                } else DeepSeekPrimaryPolicy.INTERVAL
                val next = state.lastAttempt.takeIf { it > 0L }?.plus(interval) ?: 0L
                append("\nПоследний ответ: ${time(state.lastSuccess)} • следующий плановый: ${time(next)}")
                append("\nПозиция Сержа: ${PositionSupervisorPolicy.statusText(position)}")
            }
        } else {
            val radar = EventRadarStore.state(this)
            val budget = GeminiRequestBudget.state(this)
            val position = GeminiPositionAdvisorStore.state(this)
            liveStatus.text = buildString {
                append("РОЛЬ: ВТОРОЕ МНЕНИЕ БЕЗ ТОРГОВЫХ ПРАВ\n")
                append("Рынок/новости вручную; позиция Сержа контролируется автоматически по квоте")
                append("\nПоследняя ручная проверка: ${radar.gemini.status}")
                append("\nПозиция Сержа: ${GeminiPositionAdvisorPolicy.statusText(position)}")
                append("\nДоступно по наблюдаемой квоте: ${budget.remainingToday}")
                append("\nСброс квоты: ${time(budget.dayResetsAt)}")
                val error = position.error.ifBlank { radar.gemini.error }
                if (error.isNotBlank()) append("\nПоследняя ошибка: $error")
                append("\nGemini не меняет ни один виртуальный счёт и не продаёт реальную позицию.")
            }
        }
    }

    private fun renderUsage() {
        val summary = ApiUsageLogStore.summary(this, provider, appVersion = BuildConfig.VERSION_NAME)
        val allVersions = ApiUsageLogStore.summary(this, provider)
        val olderRequests = (allVersions.requestsToday - summary.requestsToday).coerceAtLeast(0)
        val olderErrors = (allVersions.errorsToday - summary.errorsToday).coerceAtLeast(0)
        usage.text = buildString {
            append("ФАКТИЧЕСКАЯ НАГРУЗКА • V${BuildConfig.VERSION_NAME}\n")
            append("За 60 секунд: ${summary.requestsLastMinute} запросов • ${(summary.requestsLastMinute / 60.0).format(3)} запр./сек")
            append("\nЗа час: ${summary.requestsLastHour} • сегодня отправлено: ${summary.requestsToday}")
            append("\nУспешно: ${summary.successesToday} • ошибок: ${summary.errorsToday}")
            append(" • восстановлений: ${summary.retriesToday}")
            append("\nТокены ответов журнала: ${summary.promptTokensToday} вход + ${summary.outputTokensToday} выход")
            if (provider == DEEPSEEK) {
                val budgetCost = DeepSeekDailyBudgetStore.costUsd(this@ApiCenterActivity)
                val projected = budgetCost * 30.0
                append("\nОценка без скидки кэша: $${budgetCost.format(4)} сегодня")
                append(" • $${projected.format(2)} за 30 таких дней")
                append("\nОстановка по стоимости отключена: DeepSeek продолжает анализ.")
                append(" Предупреждение один раз в сутки после ≈€${DeepSeekCostWarningPolicy.DAILY_WARNING_USD.format(0)}")
            }
            if (olderRequests > 0 || olderErrors > 0) {
                append("\nСтарые версии сегодня: $olderRequests запросов • $olderErrors ошибок")
                append(" (не входят в показатели V${BuildConfig.VERSION_NAME})")
            }
        }
    }

    private fun renderLog() {
        val events = ApiUsageLogStore.list(this, provider).takeLast(20).asReversed()
        log.text = if (events.isEmpty()) "После первого реального обращения здесь появится точный журнал." else
            events.joinToString("\n\n") {
                val version = it.appVersion.takeIf(String::isNotBlank)?.let { value -> "V$value" }
                    ?: "СТАРАЯ ВЕРСИЯ"
                "${time(it.at)} • $version • ${it.circuit} • ${shortModel(it.model)} • ${it.status}\n" +
                    "${if (it.durationMillis > 0L) "${it.durationMillis / 1000.0} сек • " else ""}${it.detail}"
            }
    }

    private fun configured(): Boolean = if (provider == DEEPSEEK) {
        DeepSeekSecureKeyStore.read(this).isNotBlank()
    } else EventRadarStore.apiKey(this).isNotBlank()

    private fun shareDiagnostics() {
        val report = ProviderDiagnostics.report(this, provider)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "PumpSignal V${BuildConfig.VERSION_NAME} • диагностика $provider")
            putExtra(Intent.EXTRA_TEXT, report)
        }
        startActivity(Intent.createChooser(intent, "Отправить диагностику"))
    }

    private fun copyDiagnostics() {
        val report = ProviderDiagnostics.report(this, provider)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Диагностика PumpSignal $provider", report))
        Toast.makeText(this, "Диагностика скопирована. Вставь её в ChatGPT.", Toast.LENGTH_LONG).show()
    }

    private fun roleText(): String = if (provider == DEEPSEEK) {
        "Flash проверяет весь рынок каждые 2 минуты и при существенном изменении сигнала. Перед BUY/EXIT отдельная усиленная проверка подтверждает или отклоняет сделку. Pro подключается сразу после «Я купил» и при опасности."
    } else {
        "Gemini даёт независимое второе мнение. 12 из 25 обращений доступны обычно, ещё 13 остаются в резерве до открытия позиции."
    }

    private fun panel() = label("", 14, "#C9D1D9", false).apply {
        setBackgroundColor(Color.parseColor("#161B22"))
        setPadding(dp(10), dp(10), dp(10), dp(10))
        setTextIsSelectable(true)
    }

    private fun shortModel(model: String): String = when {
        model.contains("pro", true) -> "PRO"
        model.contains("flash", true) -> "FLASH"
        model.isBlank() -> "—"
        else -> model
    }

    private fun time(value: Long): String = if (value <= 0L) "—" else PumpBotEngine.formatTime(value)
    private fun signed(value: Int) = if (value >= 0) "+$value" else "−${kotlin.math.abs(value)}"
    private fun Double.format(digits: Int) = String.format(Locale.GERMANY, "%.${digits}f", this)
    private fun params(height: Int, top: Int = 0) = LinearLayout.LayoutParams(-1, height).apply { topMargin = top }
    private fun button(text: String, color: String) = Button(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        backgroundTintList = ColorStateList.valueOf(Color.parseColor(color))
        textSize = 13f
        isAllCaps = false
    }
    private fun label(text: String, size: Int, color: String, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(Color.parseColor(color))
        setPadding(0, dp(7), 0, dp(7))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_PROVIDER = "provider"
        const val DEEPSEEK = "DEEPSEEK"
        const val GEMINI = "GEMINI"
    }
}
