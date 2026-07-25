package com.example.pumppaperbot

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

data class GeminiMarketFrame(
    val hourId: Long,
    val candleTime: Long,
    val analysisPrice: Double,
    val executionPrice: Double,
    val pump1hPercent: Double?,
    val pump3hPercent: Double?,
    val pump6hPercent: Double?,
    val btc1hPercent: Double?,
    val btc3hPercent: Double?,
    val sol1hPercent: Double?,
    val sol3hPercent: Double?,
    val spotTakerBuyPercent: Double?,
    val futuresTakerBuyPercent: Double?,
    val premiumPercent: Double?,
    val fundingPercent: Double,
    val snapshot: LiveSnapshot,
    val news: List<MarketEvent>,
    val completedOutcomes: List<GeminiHourOutcome>
) {
    companion object {
        private const val HOUR_MILLIS = 60L * 60L * 1000L

        fun from(context: Context): GeminiMarketFrame? {
            val saved = PumpBotEngine.savedMarketPayloads(context)
            val pumpUsdt = PumpBotEngine.parseCandles(saved.pumpJson)
            val eurUsdt = PumpBotEngine.parseCandles(saved.eurJson)
            val pumpEur = StrategyV2.synthesizeEur(pumpUsdt, eurUsdt)
            val index = pumpEur.indices.lastOrNull { isFullHourClose(pumpEur[it].closeTime) }
                ?: return null
            val candle = pumpEur[index]
            if (candle.close <= 0.0) return null

            val btc = PumpBotEngine.parseCandles(saved.btcJson)
            val sol = PumpBotEngine.parseCandles(saved.solJson)
            val futures = PumpBotEngine.parseCandles(saved.futuresJson)
            val premium = PumpBotEngine.parseCandles(saved.premiumJson)
            val snapshot = PumpBotEngine.snapshot(context)
            val events = EventRadarStore.state(context).recent
                .filter { System.currentTimeMillis() - it.publishedAt <= 72L * HOUR_MILLIS }
                .sortedByDescending { it.publishedAt }
                .take(8)
            val executionPrice = snapshot.lastPrice.takeIf { it > 0.0 } ?: candle.close
            val outcomes = pumpEur.indices.mapNotNull { outcomeIndex ->
                val source = pumpEur[outcomeIndex]
                if (!isFullHourClose(source.closeTime)) return@mapNotNull null
                val first = pumpEur.getOrNull(outcomeIndex + 1) ?: return@mapNotNull null
                val second = pumpEur.getOrNull(outcomeIndex + 2) ?: return@mapNotNull null
                val halfHour = 30L * 60L * 1000L
                val firstGap = first.closeTime - source.closeTime
                val secondGap = second.closeTime - source.closeTime
                if (firstGap !in (halfHour - 2_000L)..(halfHour + 2_000L)) {
                    return@mapNotNull null
                }
                if (secondGap !in (HOUR_MILLIS - 2_000L)..(HOUR_MILLIS + 2_000L) ||
                    !isFullHourClose(second.closeTime)
                ) {
                    return@mapNotNull null
                }
                GeminiHourOutcome(
                    decisionId = (source.closeTime + 1L) / HOUR_MILLIS,
                    closePrice = second.close,
                    highPrice = maxOf(first.high, second.high)
                )
            }.takeLast(400)
            return GeminiMarketFrame(
                hourId = (candle.closeTime + 1L) / HOUR_MILLIS,
                candleTime = candle.closeTime,
                analysisPrice = candle.close,
                executionPrice = executionPrice,
                pump1hPercent = returnAt(pumpEur, index, 2),
                pump3hPercent = returnAt(pumpEur, index, 6),
                pump6hPercent = returnAt(pumpEur, index, 12),
                btc1hPercent = alignedReturn(btc, candle.closeTime, 2),
                btc3hPercent = alignedReturn(btc, candle.closeTime, 6),
                sol1hPercent = alignedReturn(sol, candle.closeTime, 2),
                sol3hPercent = alignedReturn(sol, candle.closeTime, 6),
                spotTakerBuyPercent = alignedTakerPercent(pumpUsdt, candle.closeTime, 2),
                futuresTakerBuyPercent = alignedTakerPercent(futures, candle.closeTime, 2),
                premiumPercent = premium.lastOrNull { it.closeTime <= candle.closeTime }
                    ?.close?.times(100.0),
                fundingPercent = snapshot.fundingRate * 100.0,
                snapshot = snapshot,
                news = events,
                completedOutcomes = outcomes
            )
        }

        internal fun isFullHourClose(closeTime: Long): Boolean {
            if (closeTime <= 0L) return false
            val remainder = (closeTime + 1L) % HOUR_MILLIS
            return remainder <= 1_000L
        }

        private fun returnAt(candles: List<PumpCandle>, index: Int, bars: Int): Double? {
            val current = candles.getOrNull(index)?.close ?: return null
            val old = candles.getOrNull(index - bars)?.close ?: return null
            return if (current > 0.0 && old > 0.0) (current / old - 1.0) * 100.0 else null
        }

        private fun alignedReturn(candles: List<PumpCandle>, time: Long, bars: Int): Double? {
            val index = candles.indexOfLast { it.closeTime <= time }
            return returnAt(candles, index, bars)
        }

        private fun alignedTakerPercent(candles: List<PumpCandle>, time: Long, bars: Int): Double? {
            val index = candles.indexOfLast { it.closeTime <= time }
            if (index < bars - 1) return null
            val selected = candles.subList(index - bars + 1, index + 1)
            val volume = selected.sumOf { it.volume }
            val taker = selected.sumOf { it.takerBuyVolume }
            return if (volume > 0.0) taker / volume * 100.0 else null
        }
    }
}

internal data class GeminiHourlyApiResult(
    val recommendation: GeminiHourlyRecommendation,
    val promptTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int
)

internal object GeminiHourlyResponseParser {
    fun parse(responseText: String, requestedModel: String, httpCode: Int = 200): GeminiHourlyApiResult {
        val root = JSONObject(responseText)
        val candidate = root.optJSONArray("candidates")?.optJSONObject(0)
            ?: throw GeminiApiException(httpCode, "Gemini не вернул вариант ответа")
        val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
        val text = buildString {
            if (parts != null) for (index in 0 until parts.length()) {
                append(parts.optJSONObject(index)?.optString("text").orEmpty())
            }
        }.trim()
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace < 0 || lastBrace <= firstBrace) {
            throw GeminiApiException(httpCode, "Ответ Gemini не содержит JSON")
        }
        val json = JSONObject(text.substring(firstBrace, lastBrace + 1))
        val action = json.optString("action", "HOLD").uppercase().let {
            if (it in setOf("BUY", "HOLD", "SELL")) it else "HOLD"
        }
        val risksJson = json.optJSONArray("risks") ?: JSONArray()
        val usage = root.optJSONObject("usageMetadata")
        return GeminiHourlyApiResult(
            recommendation = GeminiHourlyRecommendation(
                action = action,
                directionScore = json.optInt("direction").coerceIn(-100, 100),
                confidence = json.optInt("confidence").coerceIn(0, 100),
                horizonHours = json.optInt("horizon_hours", 1).coerceIn(1, 6),
                reason = json.optString("reason_ru", "Направление не объяснено").take(1000),
                risks = (0 until risksJson.length()).mapNotNull {
                    risksJson.optString(it).trim().takeIf(String::isNotBlank)
                }.take(5),
                model = root.optString("modelVersion", requestedModel)
            ),
            promptTokens = usage?.optInt("promptTokenCount") ?: 0,
            outputTokens = usage?.optInt("candidatesTokenCount") ?: 0,
            totalTokens = usage?.optInt("totalTokenCount") ?: 0
        )
    }
}

/**
 * One independent Gemini request for each fully closed UTC market hour.
 * Automatic runs do not retry the same hour after a failure, which protects quota.
 */
class GeminiExperimentClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(70, TimeUnit.SECONDS)
        .callTimeout(85, TimeUnit.SECONDS)
        .build()

    fun sync(context: Context, force: Boolean = false): GeminiExperimentState =
        synchronized(RUN_LOCK) {
            syncLocked(context, force)
        }

    private fun syncLocked(context: Context, force: Boolean): GeminiExperimentState {
        val existing = GeminiPaperStore.state(context)
        if (!existing.enabled && !force) return existing
        val frame = GeminiMarketFrame.from(context)
        if (frame == null) {
            GeminiPaperStore.markWaiting(context, "ЖДЁМ ПОЛНЫЕ РЫНОЧНЫЕ ДАННЫЕ")
            return GeminiPaperStore.state(context)
        }
        val gradedPortfolio = GeminiPaperTrader.gradeCompletedHours(
            existing.portfolio,
            frame.completedOutcomes
        )
        if (gradedPortfolio != existing.portfolio) {
            GeminiPaperStore.savePortfolio(context, gradedPortfolio)
        }
        val current = existing.copy(portfolio = gradedPortfolio)
        val key = EventRadarStore.apiKey(context)
        if (key.isBlank()) {
            GeminiPaperStore.markWaiting(context, "НЕТ КЛЮЧА GEMINI")
            return GeminiPaperStore.state(context)
        }
        if (current.portfolio.lastDecisionId >= frame.hourId) {
            GeminiPaperStore.markWaiting(context, "ТЕКУЩИЙ ЧАС УЖЕ ОБРАБОТАН")
            return GeminiPaperStore.state(context)
        }
        if (!force && current.lastAttemptHour >= frame.hourId) return GeminiPaperStore.state(context)

        GeminiPaperStore.markAttempt(context, frame.hourId)
        return runCatching {
            analyzeWithFallback(key, frame, current.portfolio)
        }.fold(
            onSuccess = { result ->
                val updated = GeminiPaperTrader.applyDecision(
                    current = current.portfolio,
                    price = frame.executionPrice,
                    decisionId = frame.hourId,
                    candleTime = frame.candleTime,
                    recommendation = result.recommendation
                )
                GeminiPaperStore.saveSuccess(
                    context = context,
                    portfolio = updated,
                    model = result.recommendation.model,
                    promptTokens = result.promptTokens,
                    outputTokens = result.outputTokens,
                    totalTokens = result.totalTokens
                )
                GeminiPaperStore.state(context)
            },
            onFailure = { error ->
                val code = (error as? GeminiApiException)?.httpCode ?: 0
                val prefix = if (code > 0) "HTTP $code: " else ""
                GeminiPaperStore.saveFailure(context, prefix + (error.message ?: "Gemini не ответил"))
                GeminiPaperStore.state(context)
            }
        )
    }

    private fun analyzeWithFallback(
        apiKey: String,
        frame: GeminiMarketFrame,
        portfolio: GeminiPaperPortfolio
    ): GeminiHourlyApiResult {
        var lastError: GeminiApiException? = null
        for ((index, model) in MODELS.withIndex()) {
            try {
                return analyze(apiKey, model, frame, portfolio)
            } catch (error: GeminiApiException) {
                lastError = error
                val canFallback = error.httpCode in setOf(404, 429, 500, 503)
                if (!canFallback || index == MODELS.lastIndex) throw error
            }
        }
        throw lastError ?: GeminiApiException(0, "Gemini не ответил")
    }

    private fun analyze(
        apiKey: String,
        model: String,
        frame: GeminiMarketFrame,
        portfolio: GeminiPaperPortfolio
    ): GeminiHourlyApiResult {
        val prompt = buildPrompt(frame, portfolio)
        val schema = JSONObject()
            .put("type", "OBJECT")
            .put("properties", JSONObject()
                .put("action", JSONObject().put("type", "STRING").put(
                    "enum", JSONArray(listOf("BUY", "HOLD", "SELL"))
                ))
                .put("direction", JSONObject().put("type", "INTEGER").put("minimum", -100).put("maximum", 100))
                .put("confidence", JSONObject().put("type", "INTEGER").put("minimum", 0).put("maximum", 100))
                .put("horizon_hours", JSONObject().put("type", "INTEGER").put("minimum", 1).put("maximum", 6))
                .put("reason_ru", JSONObject().put("type", "STRING"))
                .put("risks", JSONObject()
                    .put("type", "ARRAY")
                    .put("items", JSONObject().put("type", "STRING"))
                    .put("maxItems", 5))
            )
            .put("required", JSONArray(listOf(
                "action", "direction", "confidence", "horizon_hours", "reason_ru", "risks"
            )))
        val requestJson = JSONObject()
            .put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            ))
            .put("generationConfig", JSONObject()
                .put("responseMimeType", "application/json")
                .put("responseSchema", schema)
                .put("maxOutputTokens", 1400)
                .put("thinkingConfig", JSONObject().put("thinkingLevel", "LOW"))
            )
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
            .header("x-goog-api-key", apiKey)
            .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = runCatching {
                    JSONObject(body).optJSONObject("error")?.optString("message")
                }.getOrNull().orEmpty().ifBlank { "Gemini HTTP ${response.code}" }
                throw GeminiApiException(response.code, message.take(500))
            }
            GeminiHourlyResponseParser.parse(body, model, response.code)
        }
    }

    private fun buildPrompt(frame: GeminiMarketFrame, portfolio: GeminiPaperPortfolio): String {
        fun number(value: Double?, suffix: String = ""): String =
            value?.takeIf(Double::isFinite)?.let {
                String.format(Locale.US, "%+.3f%s", it, suffix)
            } ?: "нет данных"
        val news = frame.news.joinToString("\n") {
            "- ${it.source}: ${it.title.take(180)}; оценка правил ${it.directionScore}/100, важность ${it.importance}/100"
        }.ifBlank { "- новых существенных заголовков за 72 часа нет" }
        val lastDecisions = portfolio.decisions.takeLast(4).joinToString("\n") {
            "- ${it.requestedAction}, направление ${it.directionScore}/100, цена ${formatPrice(it.price)}, результат следующего часа ${number(it.evaluatedReturnPercent, "%")}"
        }.ifBlank { "- решений пока нет" }
        val position = if (portfolio.inPosition) {
            "в PUMP: ${String.format(Locale.US, "%.2f", portfolio.pumpAmount)} монет, вход €${formatPrice(portfolio.entryPrice)}, текущая стоимость €${String.format(Locale.US, "%.2f", portfolio.value(frame.executionPrice))}"
        } else {
            "в наличных: €${String.format(Locale.US, "%.2f", portfolio.cashEur)}"
        }
        return """
            Ты управляешь только исследовательским виртуальным счётом PUMP/EUR. Реальных ордеров нет.
            Это отдельный эксперимент: не копируй решение основной стратегии приложения.
            Горизонт главного прогноза — примерно один следующий час. После каждого закрытого часа выбери:
            BUY — купить PUMP на все свободные виртуальные евро;
            HOLD — сохранить текущую позицию или продолжить ждать в наличных;
            SELL — полностью продать виртуальный PUMP.
            Комиссия симуляции 0,15% на покупку и 0,15% на продажу. Не обещай прибыль.
            Считай цифры ниже наблюдениями, а не вероятностями. Если данные противоречат друг другу,
            выбирай HOLD и снижай confidence. Не покупай только потому, что цена уже резко выросла.

            ЗАКРЫТЫЙ ЧАС
            Время свечи UTC: ${frame.candleTime}
            PUMP/EUR закрытие: €${formatPrice(frame.analysisPrice)}
            Ориентир исполнения сейчас: €${formatPrice(frame.executionPrice)}
            PUMP: 1ч ${number(frame.pump1hPercent, "%")}; 3ч ${number(frame.pump3hPercent, "%")}; 6ч ${number(frame.pump6hPercent, "%")}
            BTC: 1ч ${number(frame.btc1hPercent, "%")}; 3ч ${number(frame.btc3hPercent, "%")}
            SOL: 1ч ${number(frame.sol1hPercent, "%")}; 3ч ${number(frame.sol3hPercent, "%")}
            Покупки taker: spot ${number(frame.spotTakerBuyPercent, "%")}; futures ${number(frame.futuresTakerBuyPercent, "%")}
            Funding ${number(frame.fundingPercent, "%")}; premium ${number(frame.premiumPercent, "%")}
            Стакан imbalance ${number(frame.snapshot.bookImbalance)}; spread ${number(frame.snapshot.spreadPercent, "%")}
            Open interest ${number(frame.snapshot.openInterest)}; изменение OI ${number(frame.snapshot.openInterestChangePercent, "%")}
            Рыночные признаки приложения: активность ${frame.snapshot.energyScore}/100,
            сжатие ${frame.snapshot.compressionScore}/100, поток ${frame.snapshot.directionScore}/100,
            полнота ${frame.snapshot.breathingConfidence}/100, поздний вход ${frame.snapshot.lateEntryRisk}/100.
            Связь рынка: ${frame.snapshot.marketRelation}

            СВЕЖИЕ НОВОСТИ
            $news

            ВИРТУАЛЬНЫЙ СЧЁТ
            $position
            Результат от старта: ${number(portfolio.profitPercent(frame.executionPrice), "%")}
            Предыдущие решения:
            $lastDecisions

            Верни только JSON по заданной схеме. reason_ru: 2–5 конкретных предложений по-русски,
            где отдельно видны главный аргумент и причина неуверенности. direction от -100 до +100
            означает ожидаемое направление PUMP на ближайший час, а не вероятность прибыли.
        """.trimIndent()
    }

    private fun formatPrice(value: Double): String = String.format(Locale.US, "%.8f", value)

    private companion object {
        val RUN_LOCK = Any()
        val MODELS = listOf("gemini-3.6-flash", "gemini-3.5-flash")
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
