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
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

data class EventFeedSource(val name: String, val url: String)

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
        EventFeedSource("BLS", "https://www.bls.gov/feed/bls_latest.rss")
    )

    fun sync(context: Context, force: Boolean = false): EventRadarState {
        if (!EventRadarStore.isEnabled(context)) return EventRadarStore.state(context)
        if (!force && !EventRadarStore.shouldSync(context)) return EventRadarStore.state(context)
        EventRadarStore.markAttempt(context)
        val collected = ArrayList<MarketEvent>()
        val errors = ArrayList<String>()
        var successes = 0
        sources.forEach { source ->
            runCatching {
                fetchSource(context, source).map { EventRadarClassifier.classify(it) }
            }.onSuccess {
                successes += 1
                collected += it
            }.onFailure {
                errors += "${source.name}: ${it.message ?: "ошибка"}"
            }
        }

        if (successes == 0) {
            EventRadarStore.saveFailure(context, errors.joinToString("; "))
            return EventRadarStore.state(context)
        }

        val enriched = maybeUseAi(context, collected)
        EventRadarStore.saveSync(context, enriched, successes, errors.joinToString("; "))
        return EventRadarStore.state(context)
    }

    private fun fetchSource(context: Context, source: EventFeedSource): List<RawMarketEvent> {
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
            if (response.code == 304) return emptyList()
            if (!response.isSuccessful) error("HTTP ${response.code}")
            EventRadarStore.saveHttpValidators(
                context,
                source.name,
                response.header("ETag"),
                response.header("Last-Modified")
            )
            val bytes = response.body?.bytes() ?: ByteArray(0)
            if (bytes.isEmpty()) return emptyList()
            if (bytes.size > maxFeedBytes) error("лента больше допустимого размера")
            return EventFeedParser.parse(bytes, source, System.currentTimeMillis())
        }
    }

    private fun maybeUseAi(context: Context, events: List<MarketEvent>): List<MarketEvent> {
        val key = EventRadarStore.apiKey(context)
        if (!EventRadarStore.useAi(context) || key.isBlank()) return events
        val candidate = events
            .filter { it.importance >= 45 }
            .maxByOrNull { it.publishedAt }
            ?: return events
        val enriched = runCatching { GeminiEventInterpreter(client).analyze(key, candidate) }.getOrNull()
            ?: return events
        return events.map { if (it.id == candidate.id) enriched else it }
    }

    private companion object {
        const val maxFeedBytes = 2 * 1024 * 1024
    }
}

internal object EventFeedParser {
    fun parse(bytes: ByteArray, source: EventFeedSource, now: Long): List<RawMarketEvent> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            isXIncludeAware = false
            isExpandEntityReferences = false
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

internal class GeminiEventInterpreter(private val client: OkHttpClient) {
    fun analyze(apiKey: String, event: MarketEvent): MarketEvent {
        val prompt = """
            Ты оцениваешь только возможное краткосрочное влияние публичного события на крипторынок.
            Не давай торговый совет и не выдумывай факты. Верни только JSON без markdown:
            {"direction": число от -100 до 100, "importance": 0..100, "confidence": 0..100,
             "category": "краткая категория", "summary_ru": "одно короткое предложение"}
            -100 означает сильное давление вниз, +100 — вверх, 0 — направление неясно.
            Источник: ${event.source}
            Заголовок: ${event.title}
            Текст: ${event.summary.take(1200)}
        """.trimIndent()
        val body = JSONObject()
            .put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            ))
            .put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent")
            .header("x-goog-api-key", apiKey)
            .post(body)
            .build()
        val text = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Gemini HTTP ${response.code}")
            val root = JSONObject(response.body?.string().orEmpty())
            root.getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        }
        val json = JSONObject(text.substringAfter('{', "").substringBeforeLast('}', "").let { "{$it}" })
        return event.copy(
            directionScore = json.optInt("direction", event.directionScore).coerceIn(-100, 100),
            importance = json.optInt("importance", event.importance).coerceIn(0, 100),
            confidence = json.optInt("confidence", event.confidence).coerceIn(0, 100),
            category = json.optString("category", event.category).take(60),
            explanation = json.optString("summary_ru", event.explanation).take(300),
            aiAnalyzed = true
        )
    }
}
