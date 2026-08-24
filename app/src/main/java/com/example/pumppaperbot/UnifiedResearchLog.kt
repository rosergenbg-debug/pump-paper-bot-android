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

/** One sanitized journal for APP, Pump Machine, DeepSigX and FusionSim. */
object UnifiedResearchLog {
    private const val DIRECTORY = "research_logs"
    private const val RETENTION_DAYS = 30
    private const val HEARTBEAT_MILLIS = 15L * 60L * 1000L
    private const val CONTROL_PREFS = "unified_log_control_v530"
    private val lock = Any()
    private val utc = TimeZone.getTimeZone("UTC")

    fun record(context: Context, agent: String, result: String, detail: String, now: Long = System.currentTimeMillis()) {
        synchronized(lock) {
            val cleanAgent = agent.take(40)
            val cleanResult = result.take(20)
            val cleanDetail = sanitize(detail).take(800)
            val semantic = ResearchLogCompactionPolicy.semantic(cleanDetail)
            val control = context.getSharedPreferences(CONTROL_PREFS, Context.MODE_PRIVATE)
            val key = Integer.toHexString(cleanAgent.hashCode())
            val important = cleanResult in setOf("BUY", "SELL", "ERROR", "START", "STOP")
            val lastAt = control.getLong("at_$key", 0L)
            val lastSemantic = control.getString("semantic_$key", "").orEmpty()
            if (!important && semantic == lastSemantic && now - lastAt in 0 until HEARTBEAT_MILLIS) return
            val dir = File(context.filesDir, DIRECTORY).apply { mkdirs() }
            val line = JSONObject()
                .put("time", now).put("agent", cleanAgent).put("result", cleanResult)
                .put("detail", cleanDetail).toString() + "\n"
            File(dir, "unified-${day(now)}.ndjson").appendText(line, Charsets.UTF_8)
            control.edit().putLong("at_$key", now).putString("semantic_$key", semantic).apply()
            val cutoff = now - RETENTION_DAYS * 24L * 60L * 60L * 1000L
            dir.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
        }
    }

    fun captureCycle(context: Context, source: String, now: Long = System.currentTimeMillis()) {
        val market = PumpBotEngine.snapshot(context)
        val price = PaperExecutionPolicy.displayPrice(market, now)
        val app = AppPaperStore.state(context)
        val deepSeek = DeepSeekPrimaryStore.state(context, now)
        val pumpMachine = PumpMachineStore.state(context)
        val pumpMachine2 = PumpMachine2Store.state(context)
        val pumpRetest = PumpMachineRetestStore.state(context)
        val pumpSafe = PumpMachineSafeStore.state(context)
        val deepSigX = GeminiExitExperimentStore.state(context)?.portfolio ?: GeminiPaperPortfolio()
        val fusionMarket = BitpandaFusionStore.state(context)
        val fusionPrice = fusionMarket.bid.takeIf { fusionMarket.fresh(now) } ?: price
        val fusion = FusionSimStore.state(context)
        val fusionPriority = FusionPriorityPolicy.plan(fusion)
        val fusionMetrics = FusionPriorityPolicy.metrics(
            fusion, fusionPrice, fusionMarket.feeRate, fusionMarket.fresh(now)
        )
        record(context, "APP", "CYCLE", "$source; value=${app.value(price)}; trades=${app.trades.size}", now)
        val pumpMachineValue = PumpMachinePolicy.netLiquidationValue(
            pumpMachine,
            fusionPrice,
            fusionMarket.feeRate
        )
        record(
            context,
            "PUMP_MACHINE",
            if (pumpMachine.inPosition) "IN_POSITION" else "CYCLE",
            "$source; value=$pumpMachineValue; tradeNet=${PumpMachinePolicy.tradeNetPercent(pumpMachine, fusionPrice, fusionMarket.feeRate)}; " +
                "trades=${pumpMachine.trades.size}; ${PumpMachineStore.lastStatus(context)}",
            now
        )
        record(context, "PUMP_MACHINE_RETEST", if (pumpRetest.inPosition) "IN_POSITION" else "CYCLE",
            "$source; value=${PumpMachineRetestStore.netValue(context, now)}; tradeNet=${PumpMachineRetestStore.tradeNetPercent(context, now)}; " +
                "trades=${pumpRetest.trades.size}; ${PumpMachineRetestStore.lastStatus(context)}", now)
        record(context, "PUMP_MACHINE_SAFE", if (pumpSafe.inPosition) "IN_POSITION" else "CYCLE",
            "$source; value=${PumpMachineSafeStore.netValue(context, now)}; tradeNet=${PumpMachineSafeStore.tradeNetPercent(context, now)}; " +
                "trades=${pumpSafe.trades.size}; ${PumpMachineSafeStore.lastStatus(context)}", now)
        val pumpMachine2Value = PumpMachine2Policy.netLiquidationValue(
            pumpMachine2,
            fusionPrice,
            fusionMarket.feeRate
        )
        record(
            context,
            "PUMP_MACHINE_2",
            if (pumpMachine2.inPosition) "IN_POSITION" else "CYCLE",
            "$source; value=$pumpMachine2Value; tradeNet=${PumpMachine2Policy.tradeNetPercent(pumpMachine2, fusionPrice, fusionMarket.feeRate)}; " +
                "trades=${pumpMachine2.trades.size}; ${PumpMachine2Store.lastStatus(context)}",
            now
        )
        record(context, "DEEPSIGX", "CYCLE", "$source; value=${deepSigX.value(price)}; trades=${deepSigX.trades.size}", now)
        record(
            context,
            "FUSION_SIM",
            if (fusionPriority.active) "MAX_CONTROL" else "CYCLE",
            "$source; netValue=${fusionMetrics.netLiquidationValueEur}; netPnl=${fusionMetrics.netPnlEur}; " +
                "pullback=${fusionMetrics.pullbackFromPeakPercent}; trades=${fusion.trades.size}; " +
                "venueFresh=${fusionMarket.fresh(now)}; ${fusionPriority.label}",
            now
        )
    }

    fun export(context: Context, now: Long = System.currentTimeMillis()): File {
        val market = PumpBotEngine.snapshot(context)
        val displayPrice = PaperExecutionPolicy.displayPrice(market)
        val app = AppPaperStore.state(context)
        val retiredDeepSig = GeminiPaperStore.state(context, includeActivity = true)
        val pumpMachine = PumpMachineStore.state(context)
        val pumpMachine2 = PumpMachine2Store.state(context)
        val pumpRetest = PumpMachineRetestStore.state(context)
        val pumpSafe = PumpMachineSafeStore.state(context)
        val deepSigX = GeminiExitExperimentStore.state(context)
        val fusion = BitpandaFusionStore.state(context)
        val fusionSim = FusionSimStore.state(context)
        val deepSeek = DeepSeekPrimaryStore.state(context)
        val legacyArchive = ResearchHistoryArchive.exportJson(context)
        val performanceLedger = runCatching { ResearchPerformanceLedger.exportJson(context) }
            .getOrElse { JSONObject().put("error", "performance ledger unavailable") }
        val rawJournal = ArrayList<JSONObject>()
        val cutoff = now - RETENTION_DAYS * 24L * 60L * 60L * 1000L
        File(context.filesDir, DIRECTORY).listFiles()
            ?.filter { it.name.endsWith(".ndjson") && it.lastModified() >= cutoff }
            ?.sortedBy { it.name }
            ?.forEach { file ->
                file.useLines { lines -> lines.forEach { raw ->
                    runCatching { JSONObject(raw) }.getOrNull()?.takeIf {
                        it.optLong("time") >= cutoff
                    }?.let(rawJournal::add)
                } }
            }
        val compactJournal = ResearchLogCompactionPolicy.compact(rawJournal, HEARTBEAT_MILLIS)
        val report = JSONObject()
            .put("schema", "pump-signal-unified-log-v530")
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("generatedAt", now)
            .put("logPolicy", JSONObject()
                .put("rawRetentionDays", RETENTION_DAYS)
                .put("clearedAfterExport", false)
                .put("exportCompacted", true)
                .put("rawJournalRecords", rawJournal.size)
                .put("exportedJournalRecords", compactJournal.size)
                .put("heartbeatMinutes", HEARTBEAT_MILLIS / 60_000L))
            .put("safety", JSONObject()
                .put("realOrdersImplemented", false)
                .put("bitpandaMode", "READ_ONLY_MARKET_DATA_AND_PAPER_SIMULATION")
                .put("containsApiKeys", false))
            .put("market", JSONObject().put("pumpEur", displayPrice).put("lastSync", market.lastSync))
            .put("bitpandaFusion", fusion.toJson().apply { put("error", sanitize(fusion.error)) })
            .put("fusionPriority", JSONObject()
                .put("active", FusionPriorityPolicy.plan(fusionSim).active)
                .put("forceDeepSigPro", FusionPriorityPolicy.plan(fusionSim).forcePro)
                .put("intervalSeconds", FusionPriorityPolicy.plan(fusionSim).intervalMillis / 1000L)
                .put("separateFromSerge", true))
            .put("deepSeekAnalysis", deepSeek.toJson())
            .put("legacyV4Archive", legacyArchive)
            .put("performanceLedger", performanceLedger)
            .put("entryOpportunityAudit", EntryOpportunityAuditStore.exportJson(context))
            .put("liquidityReleaseShadow", LiquidityReleaseShadowStore.exportJson(context, now))
            .put("accounts", JSONObject()
                .put("APP", appJson(app))
                .put("PumpMachine", PumpMachineStore.toJson(pumpMachine))
                .put("PumpMachine2", PumpMachine2Store.toJson(pumpMachine2))
                .put("PumpMachineRetest", PumpMachineRetestStore.toJson(pumpRetest))
                .put("PumpMachineSafe", PumpMachineSafeStore.toJson(pumpSafe))
                .put("DeepSigRetired", geminiJson(retiredDeepSig.portfolio).put("retiredInV521", true))
                .put("DeepSigX", deepSigX?.let { geminiJson(it.portfolio)
                    .put("lastSignal", it.lastSignal).put("lastReason", it.lastReason) } ?: JSONObject.NULL)
                .put("FusionSim", FusionSimStore.toJson(fusionSim)))
            .put("deepSigRetiredActivity", JSONArray(retiredDeepSig.activity.takeLast(1_200).map { it.toJson() }))
            .put("journal", JSONArray(compactJournal))
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        return File(dir, "PumpSignal-V${BuildConfig.VERSION_NAME}-Log-${stamp(now)}.json").apply {
            writeText(report.toString(2), Charsets.UTF_8)
        }
    }

    fun share(context: Context) {
        val file = export(context)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "PumpSignal V${BuildConfig.VERSION_NAME} — единый диагностический лог")
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

}

internal object ResearchLogCompactionPolicy {
    private val dynamicNumber = Regex(
        "(?i)(value|tradeNet|netValue|netPnl|pullback|price)=(-?\\d+(?:\\.\\d+)?(?:[Ee][+-]?\\d+)?)"
    )
    private val cooldown = Regex("ещё \\d+с", RegexOption.IGNORE_CASE)

    fun semantic(detail: String): String = detail
        .substringAfter("; ", detail)
        .replace(dynamicNumber) { "${it.groupValues[1]}=#" }
        .replace(cooldown, "ещё #с")
        .trim()

    fun compact(events: List<JSONObject>, windowMillis: Long): List<JSONObject> {
        val result = ArrayList<JSONObject>()
        val lastByAgent = HashMap<String, Int>()
        events.sortedBy { it.optLong("time") }.forEach { event ->
            val agent = event.optString("agent")
            val resultName = event.optString("result")
            val semantic = semantic(event.optString("detail"))
            val time = event.optLong("time")
            val priorIndex = lastByAgent[agent]
            val prior = priorIndex?.let(result::get)
            val important = resultName in setOf("BUY", "SELL", "ERROR", "START", "STOP")
            val same = !important && prior != null && prior.optString("result") == resultName &&
                prior.optString("semantic") == semantic &&
                time - prior.optLong("lastTime", prior.optLong("time")) in 0 until windowMillis
            if (same) {
                prior!!.put("lastTime", time)
                    .put("repeatCount", prior.optInt("repeatCount", 1) + 1)
                    .put("detail", event.optString("detail"))
            } else {
                val next = JSONObject(event.toString())
                    .put("semantic", semantic)
                    .put("firstTime", time)
                    .put("lastTime", time)
                    .put("repeatCount", 1)
                result += next
                lastByAgent[agent] = result.lastIndex
            }
        }
        return result.sortedBy { it.optLong("lastTime", it.optLong("time")) }
    }
}
