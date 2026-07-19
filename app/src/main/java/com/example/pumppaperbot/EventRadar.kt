package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.abs

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
    val aiConfigured: Boolean
) {
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
    const val dailyTrafficLimitBytes = 8L * 1024L * 1024L

    private fun prefs(context: Context) = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(keyEnabled, true)
    fun setEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(keyEnabled, value).apply()
    fun useAi(context: Context): Boolean = prefs(context).getBoolean(keyUseAi, false)
    fun setUseAi(context: Context, value: Boolean) = prefs(context).edit().putBoolean(keyUseAi, value).apply()
    fun apiKey(context: Context): String = prefs(context).getString(keyApiKey, "").orEmpty().trim()
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

    fun saveSync(context: Context, events: List<MarketEvent>, sourceCount: Int, error: String) {
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
            .putInt(keySourceCount, sourceCount)
            .putString(keyEvents, array.toString())
            .putString(keyError, error.take(300))
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
            aiEnabled = p.getBoolean(keyUseAi, false),
            aiConfigured = p.getString(keyApiKey, "").orEmpty().isNotBlank()
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

    private fun resetTrafficDayIfNeeded(context: Context, now: Long) {
        val day = now / (24L * 60L * 60L * 1000L)
        val p = prefs(context)
        if (p.getLong(keyTrafficDay, -1L) != day) {
            p.edit().putLong(keyTrafficDay, day).putLong(keyTrafficBytes, 0L).apply()
        }
    }
}
