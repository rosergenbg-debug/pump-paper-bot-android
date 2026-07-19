package com.example.pumppaperbot

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
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
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import java.util.concurrent.Executors

class EventRadarActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private lateinit var enabledButton: Button
    private lateinit var aiButton: Button
    private lateinit var keyInput: EditText
    private lateinit var status: TextView
    private lateinit var events: TextView
    private lateinit var details: TextView
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(20))
            setBackgroundColor(Color.parseColor("#0D1117"))
        }
        content.addView(button("← НАЗАД", "#30363D").apply { setOnClickListener { finish() } }, params(dp(50)))
        content.addView(label("V3.1 • ИНТЕРНЕТ + GEMINI", 25, "#F0F6FC", true))
        content.addView(label(
            "Читает небольшие обновления ФРС, ЕЦБ, SEC и BLS, затем Gemini сопоставляет важное событие с текущим дыханием PUMP, Bitcoin и Solana. Этот слой объясняет фон, но не создаёт сделку самостоятельно.",
            15, "#C9D1D9", false
        ))

        enabledButton = button("", "#30363D").apply {
            setOnClickListener {
                EventRadarStore.setEnabled(this@EventRadarActivity, !EventRadarStore.isEnabled(this@EventRadarActivity))
                updateUi()
            }
        }
        content.addView(enabledButton, params(dp(58), dp(10)))

        status = label("", 15, "#79C0FF", true).apply {
            setBackgroundColor(Color.parseColor("#161B22"))
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        content.addView(status, params(-2, dp(8)))

        val checkButton = button("ПРОВЕРИТЬ ОФИЦИАЛЬНЫЕ ЛЕНТЫ СЕЙЧАС", "#1F6FEB").apply {
            setOnClickListener { syncNow() }
        }
        content.addView(checkButton, params(dp(58), dp(8)))
        progress = ProgressBar(this).apply { visibility = View.GONE }
        content.addView(progress, LinearLayout.LayoutParams(-1, dp(38)))

        content.addView(label("GEMINI УЖЕ ПОДКЛЮЧЁН", 20, "#F0F6FC", true))
        content.addView(label(
            "В этой личной сборке ключ уже встроен. Поле ниже нужно только если вы захотите временно проверить другой ключ.",
            14, "#C9D1D9", false
        ))
        keyInput = EditText(this).apply {
            hint = "Встроенный ключ активен • здесь можно указать другой"
            setHintTextColor(Color.parseColor("#8B949E"))
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#161B22"))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(dp(10), 0, dp(10), 0)
            setText("")
        }
        content.addView(keyInput, params(dp(58), dp(6)))

        val keyButtons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        keyButtons.addView(button("СОХРАНИТЬ ДРУГОЙ", "#238636").apply {
            setOnClickListener {
                EventRadarStore.saveApiKey(this@EventRadarActivity, keyInput.text.toString())
                EventRadarStore.setUseAi(this@EventRadarActivity, true)
                keyInput.setText("")
                updateUi()
            }
        }, LinearLayout.LayoutParams(0, dp(56), 1f))
        keyButtons.addView(button("ВЕРНУТЬ ВСТРОЕННЫЙ", "#30363D").apply {
            setOnClickListener {
                keyInput.setText("")
                EventRadarStore.saveApiKey(this@EventRadarActivity, "")
                EventRadarStore.setUseAi(this@EventRadarActivity, true)
                updateUi()
            }
        }, LinearLayout.LayoutParams(0, dp(56), 1f).apply { leftMargin = dp(8) })
        content.addView(keyButtons, params(dp(56), dp(8)))

        aiButton = button("", "#30363D").apply {
            setOnClickListener {
                if (EventRadarStore.apiKey(this@EventRadarActivity).isNotBlank()) {
                    EventRadarStore.setUseAi(this@EventRadarActivity, !EventRadarStore.useAi(this@EventRadarActivity))
                    updateUi()
                } else {
                    status.text = "Сначала вставьте и сохраните бесплатный ключ Gemini. Без него радар продолжает работать по прозрачным правилам."
                }
            }
        }
        content.addView(aiButton, params(dp(58), dp(8)))

        content.addView(button("ПРОВЕРИТЬ GEMINI — ПОЛУЧИТЬ ЖИВОЙ ОТВЕТ", "#7C3AED").apply {
            setOnClickListener { testGeminiNow() }
        }, params(dp(58), dp(8)))

        details = label("", 14, "#C9D1D9", false).apply {
            setBackgroundColor(Color.parseColor("#0B1320"))
            setPadding(dp(10), dp(10), dp(10), dp(10))
            visibility = View.GONE
        }
        content.addView(button("ПОКАЗАТЬ, ЧТО РЕАЛЬНО ПОЛУЧЕНО", "#1F6FEB").apply {
            setOnClickListener {
                details.visibility = if (details.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                updateDetails(EventRadarStore.state(this@EventRadarActivity))
            }
        }, params(dp(58), dp(8)))
        content.addView(details, params(-2, dp(4)))

        content.addView(label("ПОСЛЕДНИЕ СОБЫТИЯ", 20, "#F0F6FC", true))
        events = label("Пока событий нет", 14, "#C9D1D9", false).apply {
            setBackgroundColor(Color.parseColor("#161B22"))
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        content.addView(events, params(-2, dp(4)))
        events.setOnClickListener {
            EventRadarStore.state(this).latest?.link?.takeIf { it.startsWith("http") }?.let { link ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            }
        }

        setContentView(ScrollView(this).apply { addView(content) })
        updateUi()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun syncNow() {
        progress.visibility = View.VISIBLE
        status.text = "Проверяю четыре официальные ленты…"
        executor.execute {
            val state = runCatching { EventRadarClient().sync(this, force = true) }
                .getOrElse {
                    EventRadarStore.saveFailure(this, it.message ?: "ошибка проверки")
                    EventRadarStore.state(this)
                }
            main.post {
                progress.visibility = View.GONE
                updateUi(state)
            }
        }
    }

    private fun testGeminiNow() {
        progress.visibility = View.VISIBLE
        status.text = "Отправляю реальный запрос в Gemini и жду JSON-ответ…"
        executor.execute {
            val state = EventRadarClient().testGemini(this)
            main.post {
                progress.visibility = View.GONE
                updateUi(state)
                details.visibility = View.VISIBLE
                updateDetails(state)
            }
        }
    }

    private fun updateUi(state: EventRadarState = EventRadarStore.state(this)) {
        enabledButton.text = if (state.enabled) "РАДАР ВКЛЮЧЁН" else "РАДАР ВЫКЛЮЧЕН"
        enabledButton.backgroundTintList = ColorStateList.valueOf(
            Color.parseColor(if (state.enabled) "#238636" else "#30363D")
        )
        aiButton.text = when {
            !state.aiConfigured -> "GEMINI: КЛЮЧ НЕ НАЙДЕН"
            state.aiEnabled -> "GEMINI ВКЛЮЧЁН"
            else -> "GEMINI СОХРАНЁН, НО ВЫКЛЮЧЕН"
        }
        aiButton.backgroundTintList = ColorStateList.valueOf(
            Color.parseColor(if (state.aiEnabled && state.aiConfigured) "#7C3AED" else "#30363D")
        )
        val latest = state.latest
        status.text = when {
            !state.enabled -> "Радар выключен. Торговая часть продолжает работать как раньше."
            state.lastSuccess <= 0L -> "Радар готов. Нажмите проверку или запустите основной монитор."
            latest == null -> "Интернет: ${state.sourceCount}/4 источников • разобрано ${state.parsedEntries} сообщений."
            else -> {
                val direction = signed(latest.directionScore)
                "Интернет: ${state.sourceCount}/4 • ${formatBytes(state.fetchBytes)} • разобрано ${state.parsedEntries} • новых ${state.newEvents}\n" +
                    "${latest.source}: важность ${latest.importance}/100 • влияние $direction/100\n" +
                    "${if (latest.aiAnalyzed) "оценено Gemini + правилами" else "оценено прозрачными правилами"}"
            }
        }
        if (state.error.isNotBlank()) status.append("\nНе все источники ответили: ${state.error}")
        val gemini = state.gemini
        status.append("\nGEMINI: ${gemini.status}")
        if (gemini.lastSuccess > 0L) {
            status.append(" • HTTP ${gemini.httpCode} • ${gemini.totalTokensToday} токенов сегодня")
        }
        if (gemini.error.isNotBlank()) status.append("\nОшибка Gemini: ${gemini.error}")
        if (gemini.lastAutoNote.isNotBlank()) status.append("\n${gemini.lastAutoNote}")
        status.append("\nТрафик V3 сегодня: ${EventRadarStore.trafficText(this)}")

        events.text = if (state.recent.isEmpty()) {
            "Пока событий нет. После первой проверки старая лента будет показана, но не вызовет ложный звонок."
        } else {
            state.recent.take(8).mapIndexed { index, event ->
                val marker = when {
                    event.directionScore >= 20 -> "↑"
                    event.directionScore <= -20 -> "↓"
                    else -> "•"
                }
                "${index + 1}. $marker ${event.source} • ${PumpBotEngine.formatTime(event.publishedAt)}\n" +
                    "${event.title}\nВажность ${event.importance}/100 • влияние ${signed(event.directionScore)}/100 • уверенность ${event.confidence}/100"
            }.joinToString("\n\n") + "\n\nКоснитесь списка, чтобы открыть источник самого свежего события."
        }
        if (details.visibility == View.VISIBLE) updateDetails(state)
    }

    private fun updateDetails(state: EventRadarState) {
        val sourceLines = if (state.sourceChecks.isEmpty()) {
            "Источники ещё не проверялись"
        } else state.sourceChecks.joinToString("\n") { check ->
            val result = when {
                check.error.isNotBlank() -> "ошибка: ${check.error}"
                check.cacheHit -> "HTTP 304 • без повторной загрузки"
                else -> "HTTP ${check.httpCode} • ${formatBytes(check.downloadedBytes)} • сообщений ${check.parsedEntries}"
            }
            "${check.source}: $result"
        }
        val gemini = state.gemini
        val web = if (gemini.webReferenceTitles.isEmpty()) "дополнительных ссылок Google Search не вернул" else {
            gemini.webReferenceTitles.joinToString("\n") { "• $it" }
        }
        val latestTitles = state.recent.take(5).joinToString("\n") {
            "• ${it.source}: ${it.title.take(180)}"
        }.ifBlank { "• сообщений пока нет" }
        details.text = buildString {
            append("ФАКТИЧЕСКАЯ ПРОВЕРКА ИНТЕРНЕТА\n$sourceLines")
            append("\n\nПОСЛЕДНИЕ ЗАГРУЖЕННЫЕ ЗАГОЛОВКИ\n$latestTitles")
            append("\n\nФАКТИЧЕСКИЙ ОТВЕТ GEMINI\n")
            append("Статус: ${gemini.status} • HTTP ${gemini.httpCode} • модель ${gemini.model.ifBlank { "—" }}")
            append("\nОтправлено: ${gemini.inputTitle.ifBlank { "—" }}")
            append("\nПолучено: ${gemini.outputSummary.ifBlank { "—" }}")
            append("\nОценка: ${signed(gemini.directionScore)}/100 • важность ${gemini.importance}/100 • уверенность ${gemini.confidence}/100")
            append("\nСегодня: ${gemini.requestsToday} запросов • ${gemini.promptTokensToday} входных + ${gemini.outputTokensToday} выходных = ${gemini.totalTokensToday} токенов")
            append("\nВнешние ссылки Gemini: ${gemini.webReferences}\n$web")
        }
    }

    private fun formatBytes(bytes: Int): String = when {
        bytes >= 1024 * 1024 -> String.format(Locale.GERMANY, "%.2f МБ", bytes / 1024.0 / 1024.0)
        bytes >= 1024 -> String.format(Locale.GERMANY, "%.1f КБ", bytes / 1024.0)
        else -> "$bytes Б"
    }

    private fun signed(value: Int): String = if (value >= 0) "+$value" else "−${kotlin.math.abs(value)}"

    private fun params(height: Int, top: Int = 0) = LinearLayout.LayoutParams(-1, height).apply { topMargin = top }

    private fun button(text: String, color: String) = Button(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        backgroundTintList = ColorStateList.valueOf(Color.parseColor(color))
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
