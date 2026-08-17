package com.example.pumppaperbot

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** One sanitized journal for APP, DeepSig, DeepSigX and FusionSim. */
object UnifiedResearchLog {
    private const val DIRECTORY = "research_logs"
    private const val RETENTION_DAYS = 30
    private val lock = Any()
    private val utc = TimeZone.getTimeZone("UTC")

    fun record(context: Context, agent: String, result: String, detail: String, now: Long = System.currentTimeMillis()) {
        synchronized(lock) {
            val dir = File(context.filesDir, DIRECTORY).apply { mkdirs() }
            val line = JSONObject()
                .put("time", now).put("agent", agent.take(40)).put("result", result.take(20))
                .put("detail", sanitize(detail).take(800)).toString() + "\n"
            File(dir, "unified-${day(now)}.ndjson").appendText(line, Charsets.UTF_8)
            val cutoff = now - RETENTION_DAYS * 24L * 60L * 60L * 1000L
            dir.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
        }
    }

    fun captureCycle(context: Context, source: String, now: Long = System.currentTimeMillis()) {
        val market = PumpBotEngine.snapshot(context)
        val price = PaperExecutionPolicy.displayPrice(market, now)
        val app = AppPaperStore.state(context)
        val deepSeek = DeepSeekPrimaryStore.state(context, now)
        val deepSig = GeminiPaperStore.state(context).portfolio
        val deepSigX = GeminiExitExperimentStore.state(context)?.portfolio ?: GeminiPaperPortfolio()
        val fusionMarket = BitpandaFusionStore.state(context)
        val fusionPrice = fusionMarket.bid.takeIf { fusionMarket.fresh(now) } ?: price
        val fusion = FusionSimStore.state(context)
        record(context, "APP", "CYCLE", "$source; value=${app.value(price)}; trades=${app.trades.size}", now)
        record(context, "DEEPSIG", deepSeek.action, "$source; value=${deepSig.value(price)}; ${deepSeek.summary}", now)
        record(context, "DEEPSIGX", "CYCLE", "$source; value=${deepSigX.value(price)}; trades=${deepSigX.trades.size}", now)
        record(context, "FUSION_SIM", "CYCLE", "$source; value=${fusion.value(fusionPrice)}; trades=${fusion.trades.size}; venueFresh=${fusionMarket.fresh(now)}", now)
    }

    fun export(context: Context, now: Long = System.currentTimeMillis()): File {
        val market = PumpBotEngine.snapshot(context)
        val displayPrice = PaperExecutionPolicy.displayPrice(market)
        val app = AppPaperStore.state(context)
        val deepSig = GeminiPaperStore.state(context, includeActivity = true)
        val deepSigX = GeminiExitExperimentStore.state(context)
        val fusion = BitpandaFusionStore.state(context)
        val fusionSim = FusionSimStore.state(context)
        val deepSeek = DeepSeekPrimaryStore.state(context)
        val journal = JSONArray()
        File(context.filesDir, DIRECTORY).listFiles()
            ?.filter { it.name.endsWith(".ndjson") }
            ?.sortedBy { it.name }
            ?.takeLast(2)
            ?.forEach { file ->
                file.useLines { lines -> lines.takeLastSafe(2500).forEach { raw ->
                    runCatching { journal.put(JSONObject(raw)) }
                } }
            }
        val report = JSONObject()
            .put("schema", "pump-signal-unified-log-v51")
            .put("appVersion", "5.1")
            .put("generatedAt", now)
            .put("safety", JSONObject()
                .put("realOrdersImplemented", false)
                .put("bitpandaMode", "READ_ONLY_MARKET_DATA_AND_PAPER_SIMULATION")
                .put("containsApiKeys", false))
            .put("market", JSONObject().put("pumpEur", displayPrice).put("lastSync", market.lastSync))
            .put("bitpandaFusion", fusion.toJson().apply { put("error", sanitize(fusion.error)) })
            .put("deepSeekAnalysis", deepSeek.toJson())
            .put("accounts", JSONObject()
                .put("APP", appJson(app))
                .put("DeepSig", geminiJson(deepSig.portfolio))
                .put("DeepSigX", deepSigX?.let { geminiJson(it.portfolio)
                    .put("lastSignal", it.lastSignal).put("lastReason", it.lastReason) } ?: JSONObject.NULL)
                .put("FusionSim", FusionSimStore.toJson(fusionSim)))
            .put("deepSigActivity", JSONArray(deepSig.activity.map { it.toJson() }))
            .put("journal", journal)
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        return File(dir, "PumpSignal-V5.1-Log-${stamp(now)}.json").apply {
            writeText(report.toString(2), Charsets.UTF_8)
        }
    }

    fun share(context: Context) {
        val file = export(context)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "PumpSignal V5.1 — единый диагностический лог")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Отправить единый лог"))
    }

    private fun appJson(p: AppPaperPortfolio) = JSONObject()
        .put("cashEur", p.cashEur).put("pumpAmount", p.pumpAmount).put("entryPrice", p.entryPrice)
        .put("lastCandleTime", p.lastCandleTime).put("totalFeesEur", p.totalFeesEur)
        .put("peakValueEur", p.peakValueEur).put("maxDrawdownPercent", p.maxDrawdownPercent)
        .put("trades", JSONArray(p.trades.map { it.toJson() }))
        .put("decisions", JSONArray(p.decisions.map { it.toJson() }))

    private fun geminiJson(p: GeminiPaperPortfolio): JSONObject = GeminiPaperStore.portfolioToJson(p)

    private fun sanitize(value: String): String = value
        .replace(Regex("(?i)(x-api-key|authorization|api[_ -]?key)\\s*[:=]?\\s*[^,;\\s]+"), "$1=[СКРЫТО]")

    private fun day(now: Long) = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utc }.format(Date(now))
    private fun stamp(now: Long) = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).apply { timeZone = utc }.format(Date(now))

    private fun Sequence<String>.takeLastSafe(limit: Int): List<String> {
        val queue = ArrayDeque<String>(limit)
        forEach { line -> if (queue.size == limit) queue.removeFirst(); queue.addLast(line) }
        return queue.toList()
    }
}
