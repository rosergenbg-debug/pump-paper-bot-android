package com.example.pumppaperbot

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

data class EventFeedSource(val name: String, val url: String)

private data class FeedFetchResult(
    val events: List<RawMarketEvent>,
    val check: EventSourceCheck
)

private class FeedHttpException(val httpCode: Int, message: String) : IllegalStateException(message)

class EventRadarClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    private val sources = listOf(
        EventFeedSource("ФРС", "https://www.federalreserve.gov/feeds/press_monetary.xml"),
        EventFeedSource("ЕЦБ", "https://www.ecb.europa.eu/rss/press.html"),
        EventFeedSource("SEC", "https://www.sec.gov/news/pressreleases.rss"),
        EventFeedSource("BLS", "https://www.bls.gov/feed/bls_latest.rss"),
        EventFeedSource("PUMP НОВОСТИ", "https://news.google.com/rss/search?q=%22PUMP%20token%22%20OR%20%22pump.fun%22%20when%3A7d&hl=en-US&gl=US&ceid=US%3Aen"),
        EventFeedSource("SOL НОВОСТИ", "https://news.google.com/rss/search?q=Solana%20crypto%20when%3A3d&hl=en-US&gl=US&ceid=US%3Aen"),
        EventFeedSource("BTC НОВОСТИ", "https://news.google.com/rss/search?q=Bitcoin%20crypto%20when%3A3d&hl=en-US&gl=US&ceid=US%3Aen")
    )

    fun sync(context: Context, force: Boolean = false): EventRadarState {
        if (!EventRadarStore.isEnabled(context)) return EventRadarStore.state(context)
        if (!force && !EventRadarStore.shouldSync(context)) return EventRadarStore.state(context)
        EventRadarStore.markAttempt(context)
        val collected = ArrayList<MarketEvent>()
        val errors = ArrayList<String>()
        val checks = ArrayList<EventSourceCheck>()
        sources.forEach { source ->
            runCatching {
                fetchSource(context, source)
            }.onSuccess { result ->
                checks += result.check
                collected += result.events.map { EventRadarClassifier.classify(it) }
            }.onFailure {
                val message = it.message ?: "ошибка"
                val code = (it as? FeedHttpException)?.httpCode ?: 0
                errors += "${source.name}: $message"
                checks += EventSourceCheck(
                    source = source.name,
                    httpCode = code,
                    cacheHit = false,
                    downloadedBytes = 0,
                    parsedEntries = 0,
                    checkedAt = System.currentTimeMillis(),
                    error = message
                )
            }
        }

        if (checks.none { it.successful }) {
            EventRadarStore.saveFetchFailure(context, checks, errors.joinToString("; "))
            return EventRadarStore.state(context)
        }

        val enriched = maybeUseAi(context, collected)
        EventRadarStore.saveSync(context, enriched, checks, errors.joinToString("; "))
        return EventRadarStore.state(context)
    }

    fun testGemini(context: Context): EventRadarState {
        val key = EventRadarStore.apiKey(context)
        if (key.isBlank()) {
            EventRadarStore.saveGeminiFailure(context, 0, "Ключ Gemini не найден")
            return EventRadarStore.state(context)
        }
        val state = EventRadarStore.state(context)
        val storedEvent = state.latest
        val event = storedEvent ?: EventRadarClassifier.classify(
            RawMarketEvent(
                source = "ТЕСТ",
                sourceUrl = "",
                title = "Текущий новостной фон Bitcoin, Solana и PUMP",
                summary = "Проверь свежие публичные факты, отдели их от предположений и сопоставь с рыночным снимком. Не давай торговый совет.",
                link = "",
                publishedAt = System.currentTimeMillis()
            )
        )
        EventRadarStore.markGeminiAttempt(context, event.title, "Ручная проверка API и смыслового анализа")
        runCatching {
            GeminiEventInterpreter(client).analyze(
                apiKey = key,
                event = event,
                market = PumpBotEngine.snapshot(context),
                recent = state.recent.take(20),
                useGoogleSearch = true,
                detailed = true
            )
        }.onSuccess { result ->
            EventRadarStore.saveGeminiSuccess(
                context = context,
                event = result.event,
                httpCode = result.httpCode,
                model = result.model,
                promptTokens = result.promptTokens,
                outputTokens = result.outputTokens,
                totalTokens = result.totalTokens,
                webTitles = result.webTitles,
                detailedAnalysis = result.detailedAnalysis,
                evidence = result.evidence,
                risks = result.risks,
                horizonHours = result.horizonHours,
                saveEvent = storedEvent != null
            )
            EventRadarStore.setUseAi(context, true)
        }.onFailure { error ->
            EventRadarStore.saveGeminiFailure(
                context,
                (error as? GeminiApiException)?.httpCode ?: 0,
                error.message ?: "Gemini не ответил"
            )
        }
        return EventRadarStore.state(context)
    }

    private fun fetchSource(context: Context, source: EventFeedSource): FeedFetchResult {
        val checkedAt = System.currentTimeMillis()
        val builder = Request.Builder()
            .url(source.url)
            .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
            .header("User-Agent", "PumpSignalAndroid/${PumpBotEngine.appVersionName} personal research")
        EventRadarStore.etag(context, source.name).takeIf { it.isNotBlank() }?.let {
            builder.header("If-None-Match", it)
        }
        EventRadarStore.lastModified(context, source.name).takeIf { it.isNotBlank() }?.let {
            builder.header("If-Modified-Since", it)
        }
        val request = builder.build()
        client.newCall(request).execute().use { response ->
            if (response.code == 304) return FeedFetchResult(
                emptyList(),
                EventSourceCheck(source.name, 304, true, 0, 0, checkedAt)
            )
            if (!response.isSuccessful) throw FeedHttpException(response.code, "HTTP ${response.code}")
            val remaining = EventRadarStore.remainingTrafficBytes(context)
            if (remaining < minimumUsefulResponseBytes) error("дневной лимит трафика V3 исчерпан")
            val allowed = minOf(maxFeedBytes.toLong(), remaining).toInt()
            val declared = response.body?.contentLength() ?: -1L
            if (declared > allowed) error("лента превышает лимит ${allowed / 1024} КБ")
            val bytes = response.body?.byteStream()?.use { input ->
                val output = ByteArrayOutputStream(minOf(allowed, 64 * 1024))
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > allowed) {
                        EventRadarStore.recordTrafficBytes(context, total.toLong())
                        error("лента превышает лимит ${allowed / 1024} КБ")
                    }
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            } ?: ByteArray(0)
            EventRadarStore.recordTrafficBytes(context, bytes.size.toLong())
            val parsed = if (bytes.isEmpty()) emptyList() else {
                EventFeedParser.parse(bytes, source, System.currentTimeMillis())
            }
            EventRadarStore.saveHttpValidators(
                context,
                source.name,
                response.header("ETag"),
                response.header("Last-Modified")
            )
            return FeedFetchResult(
                parsed,
                EventSourceCheck(source.name, response.code, false, bytes.size, parsed.size, checkedAt)
            )
        }
    }

    private fun maybeUseAi(context: Context, events: List<MarketEvent>): List<MarketEvent> {
        val key = EventRadarStore.apiKey(context)
        if (!EventRadarStore.useAi(context) || key.isBlank()) {
            EventRadarStore.markGeminiSkipped(context, "Gemini выключен; официальные ленты проверены правилами")
            return events
        }
        val candidate = events
            .filter { it.importance >= 45 }
            .maxByOrNull { it.publishedAt }
        if (candidate == null) {
            EventRadarStore.markGeminiSkipped(
                context,
                if (events.isEmpty()) "Ленты не изменились (HTTP 304): нового текста для Gemini нет"
                else "Новые сообщения есть, но их важность ниже 45/100"
            )
            return events
        }
        EventRadarStore.markGeminiAttempt(context, candidate.title, "Автоматический анализ нового важного сообщения")
        val result = runCatching {
            GeminiEventInterpreter(client).analyze(
                apiKey = key,
                event = candidate,
                market = PumpBotEngine.snapshot(context),
                recent = events.take(6),
                useGoogleSearch = false,
                detailed = false
            )
        }.onFailure { error ->
            EventRadarStore.saveGeminiFailure(
                context,
                (error as? GeminiApiException)?.httpCode ?: 0,
                error.message ?: "Gemini не ответил"
            )
        }.getOrNull() ?: return events
        EventRadarStore.saveGeminiSuccess(
            context,
            result.event,
            result.httpCode,
            result.model,
            result.promptTokens,
            result.outputTokens,
            result.totalTokens,
            result.webTitles,
            result.detailedAnalysis,
            result.evidence,
            result.risks,
            result.horizonHours,
            saveEvent = false
        )
        return events.map { if (it.id == candidate.id) result.event else it }
    }

    companion object {
        const val totalSources = 7
        const val maxFeedBytes = 768 * 1024
        const val minimumUsefulResponseBytes = 16 * 1024L
    }
}

internal object EventFeedParser {
    fun parse(bytes: ByteArray, source: EventFeedSource, now: Long): List<RawMarketEvent> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { isXIncludeAware = false }
            runCatching { isExpandEntityReferences = false }
        }
        val document = factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
        val entries = descendants(document.documentElement)
            .filter { it.localNameOrNode() == "item" || it.localNameOrNode() == "entry" }
        return entries.mapNotNull { element ->
            val title = childText(element, "title").cleanText()
            if (title.isBlank()) return@mapNotNull null
            val summary = sequenceOf("description", "summary", "content")
                .map { childText(element, it).cleanText() }
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
            val rawLink = linkOf(element)
            val link = resolveLink(source.url, rawLink)
            val dateText = sequenceOf("pubDate", "published", "updated", "date")
                .map { childText(element, it).trim() }
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
            val published = parseDate(dateText) ?: now
            if (published < now - 14L * 24L * 60L * 60L * 1000L) return@mapNotNull null
            RawMarketEvent(source.name, source.url, title, summary, link, published)
        }.distinctBy { "${it.link}|${it.title}" }.take(25)
    }

    private fun descendants(root: Element): List<Element> {
        val result = ArrayList<Element>()
        fun visit(node: Node) {
            if (node is Element) result += node
            val children = node.childNodes
            for (index in 0 until children.length) visit(children.item(index))
        }
        visit(root)
        return result
    }

    private fun childText(parent: Element, name: String): String {
        val children = parent.childNodes
        for (index in 0 until children.length) {
            val child = children.item(index)
            if (child is Element && child.localNameOrNode().equals(name, ignoreCase = true)) {
                return child.textContent.orEmpty()
            }
        }
        return ""
    }

    private fun linkOf(parent: Element): String {
        val children = parent.childNodes
        for (index in 0 until children.length) {
            val child = children.item(index)
            if (child is Element && child.localNameOrNode().equals("link", ignoreCase = true)) {
                val href = child.getAttribute("href").trim()
                if (href.isNotBlank()) return href
                val value = child.textContent.orEmpty().trim()
                if (value.isNotBlank()) return value
            }
        }
        return childText(parent, "guid").trim()
    }

    private fun Element.localNameOrNode(): String = (localName ?: nodeName).substringAfter(':')

    private fun String.cleanText(): String = this
        .replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun resolveLink(base: String, value: String): String = runCatching {
        if (value.isBlank()) base else URI(base).resolve(value).toString()
    }.getOrDefault(value.ifBlank { base })

    private fun parseDate(value: String): Long? {
        if (value.isBlank()) return null
        val patterns = listOf(
            "EEE, dd MMM yyyy HH:mm:ss z",
            "EEE, dd MMM yyyy HH:mm z",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd"
        )
        return patterns.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = true
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(value)?.time
            }.getOrNull()
        }
    }
}

internal class GeminiApiException(val httpCode: Int, message: String) : IllegalStateException(message)

internal data class GeminiAnalysisResult(
    val event: MarketEvent,
    val httpCode: Int,
    val model: String,
    val promptTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
    val webTitles: List<String>,
    val detailedAnalysis: String,
    val evidence: List<String>,
    val risks: List<String>,
    val horizonHours: Int
)

internal object GeminiResponseParser {
    fun parse(responseText: String, event: MarketEvent, httpCode: Int = 200): GeminiAnalysisResult {
        val root = JSONObject(responseText)
        val candidate = root.optJSONArray("candidates")?.optJSONObject(0)
            ?: throw GeminiApiException(httpCode, "Gemini не вернул вариант ответа")
        val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
        val text = buildString {
            if (parts != null) for (index in 0 until parts.length()) {
                append(parts.optJSONObject(index)?.optString("text").orEmpty())
            }
        }.trim()
        if (text.isBlank()) {
            val finish = candidate.optString("finishReason", "без причины")
            val usage = root.optJSONObject("usageMetadata")
            val thoughts = usage?.optInt("thoughtsTokenCount") ?: 0
            throw GeminiApiException(
                httpCode,
                "Gemini завершил ответ: $finish; внутренний анализ $thoughts токенов, текста нет"
            )
        }
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace < 0 || lastBrace <= firstBrace) {
            throw GeminiApiException(httpCode, "Ответ Gemini не содержит JSON")
        }
        val json = JSONObject(text.substring(firstBrace, lastBrace + 1))
        val usage = root.optJSONObject("usageMetadata")
        val chunks = candidate.optJSONObject("groundingMetadata")?.optJSONArray("groundingChunks")
        val webTitles = buildList {
            if (chunks != null) for (index in 0 until chunks.length()) {
                val web = chunks.optJSONObject(index)?.optJSONObject("web") ?: continue
                val label = web.optString("title").ifBlank { web.optString("uri") }.trim()
                if (label.isNotBlank()) add(label.take(160))
            }
        }.distinct().take(8)
        val enriched = event.copy(
            directionScore = json.optInt("direction", event.directionScore).coerceIn(-100, 100),
            importance = json.optInt("importance", event.importance).coerceIn(0, 100),
            confidence = json.optInt("confidence", event.confidence).coerceIn(0, 100),
            category = json.optString("category", event.category).take(60),
            explanation = json.optString("summary_ru", event.explanation).take(500),
            aiAnalyzed = true
        )
        fun stringList(name: String): List<String> {
            val array = json.optJSONArray(name) ?: return emptyList()
            return (0 until array.length()).mapNotNull { array.optString(it).trim().takeIf(String::isNotBlank) }
        }
        return GeminiAnalysisResult(
            event = enriched,
            httpCode = httpCode,
            model = root.optString("modelVersion", "gemini-3-flash-preview"),
            promptTokens = usage?.optInt("promptTokenCount") ?: 0,
            outputTokens = usage?.optInt("candidatesTokenCount") ?: 0,
            totalTokens = usage?.optInt("totalTokenCount") ?: 0,
            webTitles = webTitles,
            detailedAnalysis = json.optString("detailed_analysis_ru").take(24_000),
            evidence = stringList("evidence").take(30),
            risks = stringList("risks").take(20),
            horizonHours = json.optInt("horizon_hours").coerceIn(0, 168)
        )
    }
}

internal class GeminiEventInterpreter(private val client: OkHttpClient) {
    fun analyze(
        apiKey: String,
        event: MarketEvent,
        market: LiveSnapshot,
        recent: List<MarketEvent>,
        useGoogleSearch: Boolean,
        detailed: Boolean
    ): GeminiAnalysisResult {
        val recentContext = recent.take(if (detailed) 20 else 8).joinToString("\n") {
            "- ${it.source}: ${it.title.take(220)}"
        }.ifBlank { "- других заголовков пока нет" }
        val prompt = """
            Ты проверяешь возможное краткосрочное влияние публичного события на Bitcoin, Solana и волатильный PUMP.
            Сопоставь событие с текущими рыночными измерениями. Если доступен Google Search, используй его только
            для проверки свежего контекста и не заменяй факт предположением. Не давай торговый совет.
            Верни только JSON без markdown:
            {"direction": число от -100 до 100, "importance": 0..100, "confidence": 0..100,
             "category": "краткая категория", "summary_ru": "одно короткое предложение",
             "detailed_analysis_ru": "структурированный подробный анализ",
             "evidence": ["конкретный факт или наблюдение"],
             "risks": ["что может опровергнуть вывод"], "horizon_hours": 0..168}
            -100 означает сильное давление вниз, +100 — вверх, 0 — направление неясно.
            Не называй direction вероятностью прибыли. Если данных мало, снижай confidence.
            ${if (detailed) "Дай 12–20 содержательных нумерованных разделов: факты и первоисточники, механизм влияния, BTC, SOL, PUMP, срочность, противоречия, альтернативные объяснения, горизонт и условия отмены. Чётко разделяй факт, вывод и неизвестность. Не повторяй заголовки списком." else "Дай 3–5 коротких аналитических пунктов."}
            Источник: ${event.source}
            Заголовок: ${event.title}
            Текст: ${event.summary.take(1200)}
            Рыночный снимок PUMP/EUR: цена ${market.lastPrice}; RSI ${market.lastRsi};
            поток ${market.directionScore}/100; активность ${market.energyScore}/100;
            сжатие ${market.compressionScore}/100; согласованность ${market.breathingConfidence}/100;
            риск позднего входа ${market.lateEntryRisk}/100; состояние ${market.breathingState}.
            Другие свежие официальные заголовки:
            $recentContext
        """.trimIndent()
        val requestJson = JSONObject()
            .put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            ))
            .put("generationConfig", JSONObject()
                .put("responseMimeType", "application/json")
                .put("maxOutputTokens", if (detailed) 6144 else 1536)
                .put("thinkingConfig", JSONObject().put("thinkingLevel", "MINIMAL"))
            )
        if (useGoogleSearch) {
            requestJson.put("tools", JSONArray().put(JSONObject().put("google_search", JSONObject())))
        }
        val body = requestJson
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent")
            .header("x-goog-api-key", apiKey)
            .post(body)
            .build()
        return client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = runCatching {
                    JSONObject(responseBody).optJSONObject("error")?.optString("message")
                }.getOrNull().orEmpty().ifBlank { "Gemini HTTP ${response.code}" }
                throw GeminiApiException(response.code, message.take(500))
            }
            GeminiResponseParser.parse(responseBody, event, response.code)
        }
    }
}
