package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RussianOutputPolicy {
    private val han = Regex("[\\u3400-\\u4DBF\\u4E00-\\u9FFF\\uF900-\\uFAFF]")

    fun containsHan(text: String): Boolean = han.containsMatchIn(text)

    fun visible(text: String, fallback: String = "Текст скрыт: модель вернула ответ не на русском языке"): String =
        if (containsHan(text)) fallback else text

    fun validate(vararg values: String): String? =
        if (values.any(::containsHan)) "ответ содержит китайские иероглифы вместо русского текста" else null
}

data class ProviderDiagnosticCheck(
    val name: String,
    val status: String,
    val detail: String
) {
    fun toJson() = JSONObject().put("name", name).put("status", status).put("detail", detail)

    companion object {
        fun fromJson(json: JSONObject) = ProviderDiagnosticCheck(
            name = json.optString("name"),
            status = json.optString("status", "WARN"),
            detail = RussianOutputPolicy.visible(json.optString("detail")).take(500)
        )
    }
}

data class ProviderDiagnosticRun(
    val provider: String,
    val at: Long,
    val overall: String,
    val checks: List<ProviderDiagnosticCheck>
) {
    fun toJson() = JSONObject()
        .put("provider", provider)
        .put("at", at)
        .put("overall", overall)
        .put("checks", JSONArray().apply { checks.forEach { put(it.toJson()) } })

    companion object {
        fun fromJson(json: JSONObject): ProviderDiagnosticRun {
            val array = json.optJSONArray("checks") ?: JSONArray()
            return ProviderDiagnosticRun(
                provider = json.optString("provider"),
                at = json.optLong("at"),
                overall = json.optString("overall", "WARN"),
                checks = List(array.length()) { ProviderDiagnosticCheck.fromJson(array.getJSONObject(it)) }
            )
        }
    }
}

object ProviderDiagnosticsStore {
    private const val PREFS = "provider_diagnostics_v46"

    fun save(context: Context, run: ProviderDiagnosticRun) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(run.provider.uppercase(Locale.ROOT), run.toJson().toString()).apply()
    }

    fun load(context: Context, provider: String): ProviderDiagnosticRun? = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(provider.uppercase(Locale.ROOT), null) ?: return@runCatching null
        ProviderDiagnosticRun.fromJson(JSONObject(raw))
    }.getOrNull()
}

object ProviderSelfDiagnostics {
    fun run(context: Context, provider: String, requestStartedAt: Long): ProviderDiagnosticRun {
        val normalized = provider.uppercase(Locale.ROOT)
        val now = System.currentTimeMillis()
        val snapshot = PumpBotEngine.snapshot(context)
        val events = ApiUsageLogStore.list(context, normalized)
        val recent = events.filter { it.at >= requestStartedAt }
        val successful = recent.lastOrNull { it.status == "OK" }
        val checks = buildList {
            val configured = if (normalized == "DEEPSEEK") {
                DeepSeekSecureKeyStore.read(context).isNotBlank()
            } else {
                EventRadarStore.apiKey(context).isNotBlank()
            }
            add(check("API-ключ", configured, "ключ защищён и доступен приложению", "ключ отсутствует"))
            add(check(
                "Контрольный запрос",
                successful != null,
                successful?.let { "${it.model}; ответ принят за ${it.durationMillis} мс" }.orEmpty(),
                recent.lastOrNull { it.status == "ERROR" }?.detail ?: "успешный ответ не зафиксирован"
            ))
            add(check(
                "Модель и разбор ответа",
                successful?.model?.isNotBlank() == true,
                successful?.let { "модель ${it.model}; структурированный результат принят" }.orEmpty(),
                "нет принятого структурированного результата"
            ))
            val marketAge = if (snapshot.lastSync > 0L && now >= snapshot.lastSync) now - snapshot.lastSync else Long.MAX_VALUE
            add(ProviderDiagnosticCheck(
                "Свежесть рынка",
                if (marketAge <= 5L * 60L * 1000L) "PASS" else if (snapshot.lastPrice > 0.0) "WARN" else "FAIL",
                if (snapshot.lastPrice <= 0.0) "рыночный кадр отсутствует" else "возраст синхронизации ${marketAge / 1000L} сек"
            ))
            if (normalized == "DEEPSEEK") {
                val primary = DeepSeekPrimaryStore.state(context, now)
                val micro = MicroImpulseStore.state(context)
                add(ProviderDiagnosticCheck(
                    "Основной контур и расписание",
                    if (primary.lastAttempt > 0L) "PASS" else "WARN",
                    "последняя попытка ${stamp(primary.lastAttempt)}; интервал 5 минут"
                ))
                add(ProviderDiagnosticCheck(
                    "Микропоток",
                    if (micro.connected && now - micro.updatedAt <= DeepSeekFreshMarketContext.MICRO_MAX_AGE) "PASS" else "WARN",
                    "подключение=${micro.connected}; возраст=${age(micro.updatedAt, now)} сек"
                ))
            } else {
                val budget = GeminiRequestBudget.state(context)
                add(ProviderDiagnosticCheck(
                    "Контуры и квота Gemini",
                    if (EventRadarStore.useAi(context) && budget.remainingToday > 0) "PASS" else "WARN",
                    "ИИ включён=${EventRadarStore.useAi(context)}; доступно сегодня=${budget.remainingToday}; обычный интервал 2 часа"
                ))
            }
            add(check(
                "Журнал телеметрии",
                recent.isNotEmpty(),
                "сохранено событий проверки: ${recent.size}",
                "события проверки не сохранились"
            ))
            val languageOk = recent.none { RussianOutputPolicy.containsHan(it.detail) }
            add(check(
                "Русский язык",
                languageOk,
                "иероглифы в отображаемом результате отсутствуют",
                "обнаружен текст не на русском; он скрыт приложением"
            ))
        }
        val overall = when {
            checks.any { it.status == "FAIL" } -> "FAIL"
            checks.any { it.status == "WARN" } -> "WARN"
            else -> "PASS"
        }
        return ProviderDiagnosticRun(normalized, now, overall, checks).also {
            ProviderDiagnosticsStore.save(context, it)
            ApiUsageLogStore.record(context, ApiUsageEvent(
                provider = normalized,
                circuit = "САМОДИАГНОСТИКА",
                model = successful?.model.orEmpty(),
                status = overall,
                at = now,
                detail = checks.joinToString("; ") { check -> "${check.status} ${check.name}: ${check.detail}" }.take(500)
            ))
        }
    }

    private fun check(name: String, passed: Boolean, ok: String, failed: String) =
        ProviderDiagnosticCheck(name, if (passed) "PASS" else "FAIL", if (passed) ok else failed)

    private fun age(at: Long, now: Long): String =
        if (at <= 0L || now < at) "неизвестно" else ((now - at) / 1000L).toString()

    private fun stamp(at: Long): String = if (at <= 0L) "никогда" else
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date(at))
}

object ProviderDiagnostics {
    fun report(context: Context, provider: String, now: Long = System.currentTimeMillis()): String =
        if (provider.equals("DEEPSEEK", true)) deepSeek(context, now) else gemini(context, now)

    private fun deepSeek(context: Context, now: Long): String = buildString {
        append(DeepSeekDiagnostics.report(context, now).substringBeforeLast("API-ключи, данные авторизации и полные запросы намеренно исключены."))
        appendSelfDiagnostic(context, "DEEPSEEK")
        append("\nAPI-ключи, данные авторизации и полные запросы намеренно исключены.")
    }

    private fun gemini(context: Context, now: Long): String {
        val radar = EventRadarStore.state(context)
        val paper = GeminiPaperStore.state(context)
        val budget = GeminiRequestBudget.state(context)
        val usage = ApiUsageLogStore.summary(context, "GEMINI", now)
        val current = ApiUsageLogStore.summary(context, "GEMINI", now, BuildConfig.VERSION_NAME)
        val events = ApiUsageLogStore.list(context, "GEMINI").takeLast(60).asReversed()
        return buildString {
            appendLine("PumpSignal V${BuildConfig.VERSION_NAME} • Диагностика Gemini")
            appendLine("Создано: ${stamp(now)}")
            appendLine("Пакет: ${BuildConfig.APPLICATION_ID}")
            appendLine()
            appendLine("СОСТОЯНИЕ GEMINI")
            appendLine("включён=${EventRadarStore.useAi(context)} статус=${radar.gemini.status} модель=${paper.model}")
            appendLine("последняяПопытка=${stamp(radar.gemini.lastAttempt)} последнийУспех=${stamp(radar.gemini.lastSuccess)}")
            appendLine("часовойЭксперт=${GeminiHourlyRetryPolicy.visibleStatus(paper, now)}")
            appendLine("ошибка=${RussianOutputPolicy.visible(radar.gemini.error.ifBlank { "нет" }).take(500)}")
            appendLine("квотаОсталось=${budget.remainingToday} сброс=${stamp(budget.dayResetsAt)}")
            val lastDecision = paper.portfolio.decisions.lastOrNull()
            appendLine("\nПОСЛЕДНЕЕ РЕШЕНИЕ ЧАСОВОГО ЭКСПЕРТА")
            if (lastDecision == null) {
                appendLine("решений ещё нет")
            } else {
                appendLine("модель=${lastDecision.model} запрос=${lastDecision.requestedAction} исполнение=${lastDecision.execution} направление=${lastDecision.directionScore} уверенность=${lastDecision.confidence}")
                appendLine("отправлен=${stamp(lastDecision.requestSentAt)} получен=${stamp(lastDecision.responseReceivedAt)} котировка=${stamp(lastDecision.executionQuoteAt)}")
                appendLine("причина=${RussianOutputPolicy.visible(lastDecision.reason).take(1000)}")
                appendLine("риски=${lastDecision.risks.joinToString(" | ").ifBlank { "нет" }.take(1000)}")
            }
            appendLine("\nПОСЛЕДНИЙ НОВОСТНОЙ АНАЛИЗ")
            appendLine("модель=${radar.gemini.model} направление=${radar.gemini.directionScore} важность=${radar.gemini.importance} уверенность=${radar.gemini.confidence}")
            appendLine("вывод=${RussianOutputPolicy.visible(radar.gemini.outputSummary.ifBlank { "нет" }).take(1000)}")
            appendLine("факты=${radar.gemini.evidence.joinToString(" | ").ifBlank { "нет" }.take(1000)}")
            appendLine("риски=${radar.gemini.risks.joinToString(" | ").ifBlank { "нет" }.take(1000)}")
            appendLine()
            appendLine("СВЕЖЕСТЬ РЫНКА")
            val snapshot = PumpBotEngine.snapshot(context)
            appendLine("синхронизацияСек=${age(snapshot.lastSync, now)} живаяЦенаСек=${age(snapshot.livePriceAt, now)} свеча30мСек=${age(snapshot.lastCandle, now)}")
            appendLine()
            appendLine("ИСПОЛЬЗОВАНИЕ СЕГОДНЯ")
            appendLine("запросы=${usage.requestsToday} успешно=${usage.successesToday} ошибки=${usage.errorsToday} повторы=${usage.retriesToday}")
            appendLine("токены=${usage.promptTokensToday} вход + ${usage.outputTokensToday} выход")
            appendLine("текущаяВерсия=${BuildConfig.VERSION_NAME} запросы=${current.requestsToday} успешно=${current.successesToday} ошибки=${current.errorsToday}")
            appendSelfDiagnostic(context, "GEMINI")
            appendLine("\nПОСЛЕДНИЕ СОБЫТИЯ API (новые сверху)")
            events.forEach { event ->
                appendLine("${stamp(event.at)} | версия=${event.appVersion.ifBlank { "старая" }} | ${event.circuit} | ${event.model} | ${event.status} | ${event.durationMillis}мс | ${event.promptTokens}+${event.outputTokens} | ${RussianOutputPolicy.visible(event.detail).replace('\n', ' ').take(500)}")
            }
            append("\nAPI-ключи, данные авторизации и полные запросы намеренно исключены.")
        }
    }

    private fun StringBuilder.appendSelfDiagnostic(context: Context, provider: String) {
        appendLine("\nСАМОДИАГНОСТИКА")
        val run = ProviderDiagnosticsStore.load(context, provider)
        if (run == null) {
            appendLine("ещё не запускалась")
        } else {
            appendLine("итог=${run.overall} время=${stamp(run.at)}")
            run.checks.forEach { appendLine("${it.status} | ${it.name} | ${it.detail}") }
        }
    }

    private fun age(at: Long, now: Long): String =
        if (at <= 0L || now < at) "неизвестно" else ((now - at) / 1000L).toString()

    private fun stamp(at: Long): String = if (at <= 0L) "никогда" else
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date(at))
}
