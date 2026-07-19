package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class RawMarketEvent(
    val source: String,
    val sourceUrl: String,
    val title: String,
    val summary: String,
    val link: String,
    val publishedAt: Long
)

data class MarketEvent(
    val id: String,
    val source: String,
    val sourceUrl: String,
    val title: String,
    val summary: String,
    val link: String,
    val publishedAt: Long,
    val receivedAt: Long,
    val directionScore: Int,
    val importance: Int,
    val confidence: Int,
    val category: String,
    val explanation: String,
    val aiAnalyzed: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("source", source)
        .put("sourceUrl", sourceUrl)
        .put("title", title)
        .put("summary", summary)
        .put("link", link)
        .put("publishedAt", publishedAt)
        .put("receivedAt", receivedAt)
        .put("directionScore", directionScore)
        .put("importance", importance)
        .put("confidence", confidence)
        .put("category", category)
        .put("explanation", explanation)
        .put("aiAnalyzed", aiAnalyzed)

    companion object {
        fun fromJson(json: JSONObject): MarketEvent = MarketEvent(
            id = json.optString("id"),
            source = json.optString("source"),
            sourceUrl = json.optString("sourceUrl"),
            title = json.optString("title"),
            summary = json.optString("summary"),
            link = json.optString("link"),
            publishedAt = json.optLong("publishedAt"),
            receivedAt = json.optLong("receivedAt"),
            directionScore = json.optInt("directionScore"),
            importance = json.optInt("importance"),
            confidence = json.optInt("confidence"),
            category = json.optString("category"),
            explanation = json.optString("explanation"),
            aiAnalyzed = json.optBoolean("aiAnalyzed")
        )
    }
}

data class EventSourceCheck(
    val source: String,
    val httpCode: Int,
    val cacheHit: Boolean,
    val downloadedBytes: Int,
    val parsedEntries: Int,
    val checkedAt: Long,
    val error: String = ""
) {
    val successful: Boolean get() = error.isBlank() && httpCode in listOf(200, 304)

    fun toJson(): JSONObject = JSONObject()
        .put("source", source)
        .put("httpCode", httpCode)
        .put("cacheHit", cacheHit)
        .put("downloadedBytes", downloadedBytes)
        .put("parsedEntries", parsedEntries)
        .put("checkedAt", checkedAt)
        .put("error", error)

    companion object {
        fun fromJson(json: JSONObject) = EventSourceCheck(
            source = json.optString("source"),
            httpCode = json.optInt("httpCode"),
            cacheHit = json.optBoolean("cacheHit"),
            downloadedBytes = json.optInt("downloadedBytes"),
            parsedEntries = json.optInt("parsedEntries"),
            checkedAt = json.optLong("checkedAt"),
            error = json.optString("error")
        )
    }
}

data class GeminiDiagnostics(
    val lastAttempt: Long = 0L,
    val lastSuccess: Long = 0L,
    val status: String = "НЕ ПРОВЕРЕН",
    val httpCode: Int = 0,
    val model: String = "",
    val inputTitle: String = "",
    val outputSummary: String = "",
    val detailedAnalysis: String = "",
    val evidence: List<String> = emptyList(),
    val risks: List<String> = emptyList(),
    val horizonHours: Int = 0,
    val directionScore: Int = 0,
    val importance: Int = 0,
    val confidence: Int = 0,
    val requestsToday: Int = 0,
    val promptTokensToday: Int = 0,
    val outputTokensToday: Int = 0,
    val totalTokensToday: Int = 0,
    val webReferences: Int = 0,
    val webReferenceTitles: List<String> = emptyList(),
    val error: String = "",
    val lastAutoNote: String = ""
) {
    fun toJson(): JSONObject = JSONObject()
        .put("lastAttempt", lastAttempt)
        .put("lastSuccess", lastSuccess)
        .put("status", status)
        .put("httpCode", httpCode)
        .put("model", model)
        .put("inputTitle", inputTitle)
        .put("outputSummary", outputSummary)
        .put("detailedAnalysis", detailedAnalysis)
        .put("evidence", JSONArray(evidence))
        .put("risks", JSONArray(risks))
        .put("horizonHours", horizonHours)
        .put("directionScore", directionScore)
        .put("importance", importance)
        .put("confidence", confidence)
        .put("requestsToday", requestsToday)
        .put("promptTokensToday", promptTokensToday)
        .put("outputTokensToday", outputTokensToday)
        .put("totalTokensToday", totalTokensToday)
        .put("webReferences", webReferences)
        .put("webReferenceTitles", JSONArray(webReferenceTitles))
        .put("error", error)
        .put("lastAutoNote", lastAutoNote)

    companion object {
        fun fromJson(json: JSONObject) = GeminiDiagnostics(
            lastAttempt = json.optLong("lastAttempt"),
            lastSuccess = json.optLong("lastSuccess"),
            status = json.optString("status", "НЕ ПРОВЕРЕН"),
            httpCode = json.optInt("httpCode"),
            model = json.optString("model"),
            inputTitle = json.optString("inputTitle"),
            outputSummary = json.optString("outputSummary"),
            detailedAnalysis = json.optString("detailedAnalysis"),
            evidence = json.optJSONArray("evidence")?.toStringList().orEmpty(),
            risks = json.optJSONArray("risks")?.toStringList().orEmpty(),
            horizonHours = json.optInt("horizonHours"),
            directionScore = json.optInt("directionScore"),
            importance = json.optInt("importance"),
            confidence = json.optInt("confidence"),
            requestsToday = json.optInt("requestsToday"),
            promptTokensToday = json.optInt("promptTokensToday"),
            outputTokensToday = json.optInt("outputTokensToday"),
            totalTokensToday = json.optInt("totalTokensToday"),
            webReferences = json.optInt("webReferences"),
            webReferenceTitles = json.optJSONArray("webReferenceTitles")?.let { array ->
                (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
            }.orEmpty(),
            error = json.optString("error"),
            lastAutoNote = json.optString("lastAutoNote")
        )

        private fun JSONArray.toStringList(): List<String> =
            (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }
    }
}

data class EventRadarState(
    val enabled: Boolean,
    val lastAttempt: Long,
    val lastSuccess: Long,
    val sourceCount: Int,
    val latest: MarketEvent?,
    val alertCandidate: MarketEvent?,
    val recent: List<MarketEvent>,
    val error: String,
    val aiEnabled: Boolean,
    val aiConfigured: Boolean,
    val fetchBytes: Int,
    val parsedEntries: Int,
    val newEvents: Int,
    val sourceChecks: List<EventSourceCheck>,
    val gemini: GeminiDiagnostics
) {
    /**
     * Provisional shadow adjustment. It is deliberately capped and is not used by StrategyV2.
     * Direction, importance and confidence must agree; old information rapidly decays.
     */
    fun informationAdjustment(now: Long = System.currentTimeMillis()): Int {
        if (!aiEnabled || gemini.status != "РАБОТАЕТ" || gemini.lastSuccess <= 0L) return 0
        val related = recent.firstOrNull { it.aiAnalyzed && it.title == gemini.inputTitle } ?: return 0
        val eventTime = related.publishedAt
        val ageHours = ((now - eventTime).coerceAtLeast(0L) / 3_600_000.0)
        val decay = when {
            ageHours <= 2.0 -> 1.0
            ageHours <= 6.0 -> 0.75
            ageHours <= 12.0 -> 0.50
            ageHours <= 24.0 -> 0.25
            else -> 0.0
        }
        val raw = 12.0 * (gemini.directionScore / 100.0) *
            (gemini.importance / 100.0) * (gemini.confidence / 100.0) * decay
        return raw.roundToInt().coerceIn(-12, 12)
    }

    fun combinedDirection(internalDirection: Int, now: Long = System.currentTimeMillis()): Int =
        (internalDirection + informationAdjustment(now)).coerceIn(-100, 100)

    fun confirmation(
        marketDirection: Int,
        marketConfidence: Int,
        event: MarketEvent? = latest
    ): String {
        event ?: return "Ждём первое новое событие"
        if (abs(event.directionScore) < 20) return "Направление события не определено"
        if (marketConfidence < 45) return "Рынок пока читается неуверенно"
        return when {
            event.directionScore > 0 && marketDirection >= 20 -> "Рынок подтверждает движение вверх"
            event.directionScore < 0 && marketDirection <= -20 -> "Рынок подтверждает давление вниз"
            event.directionScore > 0 && marketDirection <= -20 -> "Рынок идёт против положительного события"
            event.directionScore < 0 && marketDirection >= 20 -> "Рынок пока не подтверждает риск"
            else -> "Рынок ещё не выбрал направление"
        }
    }
}

object EventRadarClassifier {
    private data class WeightedTerm(val text: String, val weight: Int, val category: String)

    private val negative = listOf(
        WeightedTerm("emergency rate hike", 60, "СТАВКИ"),
        WeightedTerm("rate hike", 34, "СТАВКИ"),
        WeightedTerm("higher for longer", 28, "СТАВКИ"),
        WeightedTerm("hawkish", 24, "СТАВКИ"),
        WeightedTerm("inflation accelerat", 30, "ИНФЛЯЦИЯ"),
        WeightedTerm("hotter than expected", 28, "ИНФЛЯЦИЯ"),
        WeightedTerm("sanction", 25, "ГЕОПОЛИТИКА"),
        WeightedTerm("tariff", 22, "ГЕОПОЛИТИКА"),
        WeightedTerm("military attack", 55, "ГЕОПОЛИТИКА"),
        WeightedTerm("war", 35, "ГЕОПОЛИТИКА"),
        WeightedTerm("default", 48, "КРЕДИТНЫЙ РИСК"),
        WeightedTerm("insolven", 48, "КРЕДИТНЫЙ РИСК"),
        WeightedTerm("bank run", 55, "КРЕДИТНЫЙ РИСК"),
        WeightedTerm("exchange hack", 62, "КРИПТО-РИСК"),
        WeightedTerm("security breach", 38, "КРИПТО-РИСК"),
        WeightedTerm("exploit", 35, "КРИПТО-РИСК"),
        WeightedTerm("trading halt", 40, "РЫНОК"),
        WeightedTerm("enforcement action", 28, "РЕГУЛИРОВАНИЕ"),
        WeightedTerm("charges against", 26, "РЕГУЛИРОВАНИЕ"),
        WeightedTerm("crypto ban", 55, "РЕГУЛИРОВАНИЕ")
    )

    private val positive = listOf(
        WeightedTerm("emergency rate cut", 58, "СТАВКИ"),
        WeightedTerm("rate cut", 34, "СТАВКИ"),
        WeightedTerm("monetary easing", 32, "СТАВКИ"),
        WeightedTerm("liquidity facility", 32, "ЛИКВИДНОСТЬ"),
        WeightedTerm("quantitative easing", 42, "ЛИКВИДНОСТЬ"),
        WeightedTerm("stimulus", 30, "ЛИКВИДНОСТЬ"),
        WeightedTerm("ceasefire", 36, "ГЕОПОЛИТИКА"),
        WeightedTerm("etf approval", 48, "КРИПТО"),
        WeightedTerm("approves spot", 48, "КРИПТО"),
        WeightedTerm("crypto reserve", 45, "КРИПТО"),
        WeightedTerm("digital asset framework", 24, "РЕГУЛИРОВАНИЕ")
    )

    private val relevant = listOf(
        "interest rate", "monetary policy", "inflation", "consumer price", "employment",
        "payroll", "unemployment", "liquidity", "financial stability", "crypto", "bitcoin",
        "digital asset", "exchange-traded fund", "etf", "sanction", "tariff", "bank", "market"
    )

    fun classify(raw: RawMarketEvent, now: Long = System.currentTimeMillis()): MarketEvent {
        val text = "${raw.title} ${raw.summary}".lowercase(Locale.US)
        val negativeHits = negative.filter { text.contains(it.text) }
        val positiveHits = positive.filter { text.contains(it.text) }
        val negativeScore = negativeHits.sumOf { it.weight }
        val positiveScore = positiveHits.sumOf { it.weight }
        val signed = (positiveScore - negativeScore).coerceIn(-100, 100)
        val strongest = (negativeHits + positiveHits).maxByOrNull { it.weight }
        val relevanceCount = relevant.count { text.contains(it) }
        val sourceBase = when (raw.source) {
            "ФРС", "ЕЦБ" -> 22
            "BLS" -> 20
            "SEC" -> 18
            else -> 10
        }
        val impact = maxOf(negativeScore, positiveScore)
        val importance = (sourceBase + impact + relevanceCount * 5).coerceIn(0, 100)
        val confidence = when {
            impact >= 50 -> 82
            impact >= 28 -> 70
            relevanceCount >= 2 -> 52
            relevanceCount == 1 -> 40
            else -> 25
        }
        val direction = when {
            signed >= 20 -> "вероятное давление вверх"
            signed <= -20 -> "вероятное давление вниз"
            else -> "направление неясно"
        }
        return MarketEvent(
            id = stableId(raw),
            source = raw.source,
            sourceUrl = raw.sourceUrl,
            title = raw.title.trim().take(280),
            summary = raw.summary.trim().take(700),
            link = raw.link,
            publishedAt = raw.publishedAt.takeIf { it > 0L } ?: now,
            receivedAt = now,
            directionScore = signed,
            importance = importance,
            confidence = confidence,
            category = strongest?.category ?: if (relevanceCount > 0) "МАКРО/РЫНОК" else "ОБЩЕЕ",
            explanation = "Правила: $direction; совпадений по важным словам $relevanceCount.",
            aiAnalyzed = false
        )
    }

    private fun stableId(raw: RawMarketEvent): String {
        val input = "${raw.source}|${raw.link}|${raw.title}"
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(24)
    }
}

object EventRadarStore {
    private const val prefsName = "PumpEventRadarV3"
    private const val keyEnabled = "enabled"
    private const val keyUseAi = "use_ai"
    private const val keyApiKey = "gemini_api_key"
    private const val keyLastAttempt = "last_attempt"
    private const val keyLastSuccess = "last_success"
    private const val keySourceCount = "source_count"
    private const val keyEvents = "events"
    private const val keyError = "error"
    private const val keyInitialized = "initialized"
    private const val keyLastAlert = "last_alert"
    private const val keyPendingAlert = "pending_alert"
    private const val keyTrafficDay = "traffic_day"
    private const val keyTrafficBytes = "traffic_bytes"
    private const val keySourceChecks = "source_checks"
    private const val keyFetchBytes = "fetch_bytes"
    private const val keyParsedEntries = "parsed_entries"
    private const val keyNewEvents = "new_events"
    private const val keyGeminiDiagnostics = "gemini_diagnostics"
    private const val keyGeminiUsageDay = "gemini_usage_day"
    private const val keyV31AiInitialized = "v31_ai_initialized"
    const val dailyTrafficLimitBytes = 8L * 1024L * 1024L

    private fun prefs(context: Context) = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(keyEnabled, true)
    fun setEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(keyEnabled, value).apply()
    fun useAi(context: Context): Boolean {
        val p = prefs(context)
        if (!p.getBoolean(keyV31AiInitialized, false) && EmbeddedGeminiKey.value.isNotBlank()) {
            p.edit().putBoolean(keyV31AiInitialized, true).putBoolean(keyUseAi, true).apply()
            return true
        }
        return p.getBoolean(keyUseAi, EmbeddedGeminiKey.value.isNotBlank())
    }
    fun setUseAi(context: Context, value: Boolean) = prefs(context).edit().putBoolean(keyUseAi, value).apply()
    fun apiKey(context: Context): String = prefs(context).getString(keyApiKey, "").orEmpty().trim()
        .ifBlank { EmbeddedGeminiKey.value }
    fun hasCustomApiKey(context: Context): Boolean = prefs(context).getString(keyApiKey, "").orEmpty().isNotBlank()
    fun saveApiKey(context: Context, value: String) = prefs(context).edit().putString(keyApiKey, value.trim()).apply()
    fun etag(context: Context, source: String): String = prefs(context).getString("etag_$source", "").orEmpty()
    fun lastModified(context: Context, source: String): String = prefs(context).getString("modified_$source", "").orEmpty()
    fun saveHttpValidators(context: Context, source: String, etag: String?, lastModified: String?) {
        val editor = prefs(context).edit()
        if (!etag.isNullOrBlank()) editor.putString("etag_$source", etag)
        if (!lastModified.isNullOrBlank()) editor.putString("modified_$source", lastModified)
        editor.apply()
    }

    fun remainingTrafficBytes(context: Context, now: Long = System.currentTimeMillis()): Long {
        resetTrafficDayIfNeeded(context, now)
        return (dailyTrafficLimitBytes - prefs(context).getLong(keyTrafficBytes, 0L)).coerceAtLeast(0L)
    }

    fun recordTrafficBytes(context: Context, bytes: Long, now: Long = System.currentTimeMillis()) {
        if (bytes <= 0L) return
        resetTrafficDayIfNeeded(context, now)
        val p = prefs(context)
        val total = (p.getLong(keyTrafficBytes, 0L) + bytes).coerceAtMost(dailyTrafficLimitBytes)
        p.edit().putLong(keyTrafficBytes, total).apply()
    }

    fun trafficText(context: Context): String {
        val used = dailyTrafficLimitBytes - remainingTrafficBytes(context)
        return String.format(Locale.GERMANY, "%.2f из 8 МБ", used / 1024.0 / 1024.0)
    }

    fun shouldSync(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        if (!isEnabled(context)) return false
        return now - prefs(context).getLong(keyLastAttempt, 0L) >= 10L * 60L * 1000L
    }

    fun markAttempt(context: Context, now: Long = System.currentTimeMillis()) {
        prefs(context).edit().putLong(keyLastAttempt, now).apply()
    }

    fun saveSync(context: Context, events: List<MarketEvent>, checks: List<EventSourceCheck>, error: String) {
        val p = prefs(context)
        val firstSync = !p.getBoolean(keyInitialized, false)
        val existing = readEvents(p.getString(keyEvents, "[]").orEmpty())
        val existingIds = existing.mapTo(HashSet<String>()) { it.id }
        val newEvents = events.filterNot { existingIds.contains(it.id) }
        val merged = (existing + events)
            .distinctBy { it.id }
            .sortedByDescending { it.publishedAt }
            .take(40)
        val newAlert = if (firstSync) null else newEvents
            .filter { it.importance >= 75 && it.confidence >= 60 }
            .maxWithOrNull(compareBy<MarketEvent> { it.importance }.thenBy { it.publishedAt })
        val array = JSONArray().apply { merged.forEach { put(it.toJson()) } }
        val editor = p.edit()
            .putLong(keyLastSuccess, System.currentTimeMillis())
            .putInt(keySourceCount, checks.count { it.successful })
            .putString(keyEvents, array.toString())
            .putString(keyError, error.take(300))
            .putString(keySourceChecks, JSONArray().apply { checks.forEach { put(it.toJson()) } }.toString())
            .putInt(keyFetchBytes, checks.sumOf { it.downloadedBytes })
            .putInt(keyParsedEntries, checks.sumOf { it.parsedEntries })
            .putInt(keyNewEvents, newEvents.size)
            .putBoolean(keyInitialized, true)
        if (firstSync) {
            editor.putString(keyLastAlert, merged.firstOrNull()?.id.orEmpty())
            editor.putString(keyPendingAlert, "")
        } else if (newAlert != null) {
            editor.putString(keyPendingAlert, newAlert.id)
        }
        editor.apply()
    }

    fun saveFailure(context: Context, error: String) {
        prefs(context).edit().putString(keyError, error.take(300)).apply()
    }

    fun saveFetchFailure(context: Context, checks: List<EventSourceCheck>, error: String) {
        prefs(context).edit()
            .putString(keyError, error.take(300))
            .putInt(keySourceCount, checks.count { it.successful })
            .putString(keySourceChecks, JSONArray().apply { checks.forEach { put(it.toJson()) } }.toString())
            .putInt(keyFetchBytes, checks.sumOf { it.downloadedBytes })
            .putInt(keyParsedEntries, checks.sumOf { it.parsedEntries })
            .putInt(keyNewEvents, 0)
            .apply()
    }

    fun markGeminiSkipped(context: Context, note: String) {
        val current = geminiDiagnostics(context)
        saveGemini(context, current.copy(lastAutoNote = note.take(240)))
    }

    fun markGeminiAttempt(context: Context, inputTitle: String, note: String) {
        resetGeminiUsageIfNeeded(context)
        val current = geminiDiagnostics(context)
        saveGemini(context, current.copy(
            lastAttempt = System.currentTimeMillis(),
            status = "ПРОВЕРЯЕТСЯ",
            inputTitle = inputTitle.take(280),
            requestsToday = current.requestsToday + 1,
            error = "",
            lastAutoNote = note.take(240)
        ))
    }

    fun saveGeminiSuccess(
        context: Context,
        event: MarketEvent,
        httpCode: Int,
        model: String,
        promptTokens: Int,
        outputTokens: Int,
        totalTokens: Int,
        webTitles: List<String>,
        detailedAnalysis: String,
        evidence: List<String>,
        risks: List<String>,
        horizonHours: Int,
        saveEvent: Boolean = true
    ) {
        val current = geminiDiagnostics(context)
        saveGemini(context, current.copy(
            lastSuccess = System.currentTimeMillis(),
            status = "РАБОТАЕТ",
            httpCode = httpCode,
            model = model.take(80),
            outputSummary = event.explanation.take(500),
            detailedAnalysis = detailedAnalysis.take(24_000),
            evidence = evidence.map { it.take(500) }.take(30),
            risks = risks.map { it.take(500) }.take(20),
            horizonHours = horizonHours.coerceIn(0, 168),
            directionScore = event.directionScore,
            importance = event.importance,
            confidence = event.confidence,
            promptTokensToday = current.promptTokensToday + promptTokens,
            outputTokensToday = current.outputTokensToday + outputTokens,
            totalTokensToday = current.totalTokensToday + totalTokens,
            webReferences = webTitles.size,
            webReferenceTitles = webTitles.take(8),
            error = ""
        ))
        if (saveEvent) replaceEvent(context, event)
    }

    fun saveGeminiFailure(context: Context, httpCode: Int, error: String) {
        val current = geminiDiagnostics(context)
        saveGemini(context, current.copy(
            status = "ОШИБКА",
            httpCode = httpCode,
            error = error.take(500)
        ))
    }

    fun state(context: Context): EventRadarState {
        val p = prefs(context)
        val events = readEvents(p.getString(keyEvents, "[]").orEmpty())
        val pendingId = p.getString(keyPendingAlert, "").orEmpty()
        return EventRadarState(
            enabled = p.getBoolean(keyEnabled, true),
            lastAttempt = p.getLong(keyLastAttempt, 0L),
            lastSuccess = p.getLong(keyLastSuccess, 0L),
            sourceCount = p.getInt(keySourceCount, 0),
            latest = events.firstOrNull(),
            alertCandidate = events.firstOrNull { it.id == pendingId },
            recent = events,
            error = p.getString(keyError, "").orEmpty(),
            aiEnabled = useAi(context),
            aiConfigured = apiKey(context).isNotBlank(),
            fetchBytes = p.getInt(keyFetchBytes, 0),
            parsedEntries = p.getInt(keyParsedEntries, 0),
            newEvents = p.getInt(keyNewEvents, 0),
            sourceChecks = readSourceChecks(p.getString(keySourceChecks, "[]").orEmpty()),
            gemini = geminiDiagnostics(context)
        )
    }

    fun shouldAlert(context: Context, state: EventRadarState): Boolean {
        val candidate = state.alertCandidate ?: return false
        if (!state.enabled || candidate.importance < 75 || candidate.confidence < 60) return false
        if (System.currentTimeMillis() - candidate.publishedAt > 12L * 60L * 60L * 1000L) return false
        if (!AlertSchedule.isAllowedNow(context)) return false
        return candidate.id != prefs(context).getString(keyLastAlert, "")
    }

    fun markAlerted(context: Context, state: EventRadarState) {
        val id = state.alertCandidate?.id ?: return
        prefs(context).edit()
            .putString(keyLastAlert, id)
            .putString(keyPendingAlert, "")
            .apply()
    }

    private fun readEvents(value: String): List<MarketEvent> = runCatching {
        val array = JSONArray(value)
        (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let { MarketEvent.fromJson(it) }
        }
    }.getOrDefault(emptyList())

    private fun readSourceChecks(value: String): List<EventSourceCheck> = runCatching {
        val array = JSONArray(value)
        (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let { EventSourceCheck.fromJson(it) }
        }
    }.getOrDefault(emptyList())

    private fun geminiDiagnostics(context: Context): GeminiDiagnostics {
        resetGeminiUsageIfNeeded(context)
        return runCatching {
            GeminiDiagnostics.fromJson(JSONObject(prefs(context).getString(keyGeminiDiagnostics, "{}").orEmpty()))
        }.getOrDefault(GeminiDiagnostics())
    }

    private fun saveGemini(context: Context, value: GeminiDiagnostics) {
        prefs(context).edit().putString(keyGeminiDiagnostics, value.toJson().toString()).apply()
    }

    private fun replaceEvent(context: Context, event: MarketEvent) {
        val p = prefs(context)
        val events = readEvents(p.getString(keyEvents, "[]").orEmpty())
        val replaced = if (events.any { it.id == event.id }) {
            events.map { if (it.id == event.id) event else it }
        } else {
            listOf(event) + events
        }.sortedByDescending { it.publishedAt }.take(40)
        p.edit().putString(keyEvents, JSONArray().apply { replaced.forEach { put(it.toJson()) } }.toString()).apply()
    }

    private fun resetGeminiUsageIfNeeded(context: Context, now: Long = System.currentTimeMillis()) {
        val day = now / (24L * 60L * 60L * 1000L)
        val p = prefs(context)
        if (p.getLong(keyGeminiUsageDay, -1L) == day) return
        val old = runCatching {
            GeminiDiagnostics.fromJson(JSONObject(p.getString(keyGeminiDiagnostics, "{}").orEmpty()))
        }.getOrDefault(GeminiDiagnostics())
        p.edit()
            .putLong(keyGeminiUsageDay, day)
            .putString(keyGeminiDiagnostics, old.copy(
                requestsToday = 0,
                promptTokensToday = 0,
                outputTokensToday = 0,
                totalTokensToday = 0
            ).toJson().toString())
            .apply()
    }

    private fun resetTrafficDayIfNeeded(context: Context, now: Long) {
        val day = now / (24L * 60L * 60L * 1000L)
        val p = prefs(context)
        if (p.getLong(keyTrafficDay, -1L) != day) {
            p.edit().putLong(keyTrafficDay, day).putLong(keyTrafficBytes, 0L).apply()
        }
    }
}
