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
    private const val RECENT_WINDOW_MILLIS = 24L * 60L * 60L * 1000L
    // V6 keeps diagnostic parts comfortably below the observed chat upload failure near 2 MB.
    private const val MAX_SUPPORT_FILE_BYTES = 900_000
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
        val coach = DeepSeekEntryCoachStore.state(context)
        val tuning = DeepSeekEntryCoachStore.tuning(context)
        record(
            context,
            "DEEPSEEK_ENTRY_COACH",
            coach.status,
            "$source; verdict=${coach.verdict}; stage=${coach.stage}; confidence=${coach.confidence}; " +
                "reason=${coach.reason}; tuningRevision=${tuning.revision}; ${tuning.compact()}",
            now
        )
        val v6 = ScalpExecutionIntelligenceStoreV600.current(context)
        record(
            context,
            "V6_EXECUTION_SHADOW",
            v6.agreement,
            "$source; score=${v6.executionScore}; costFloor=${v6.costFloorBps ?: -1.0}bp; ${v6.reason}",
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
            .put("schema", "pump-signal-unified-log-v600")
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
                .put("v6ExecutionMode", "SHADOW_ONLY")
                .put("containsApiKeys", false))
            .put("market", JSONObject().put("pumpEur", displayPrice).put("lastSync", market.lastSync))
            .put("bitpandaFusion", fusion.toJson().apply { put("error", sanitize(fusion.error)) })
            .put("v6ScalpExecutionShadow", ScalpExecutionIntelligenceStoreV600.current(context).toJson())
            .put("fusionPriority", JSONObject()
                .put("active", FusionPriorityPolicy.plan(fusionSim).active)
                .put("forceDeepSigPro", FusionPriorityPolicy.plan(fusionSim).forcePro)
                .put("intervalSeconds", FusionPriorityPolicy.plan(fusionSim).intervalMillis / 1000L)
                .put("separateFromSerge", true))
            .put("deepSeekAnalysis", deepSeek.toJson())
            .put("deepSeekEntryCoach", DeepSeekEntryCoachStore.exportJson(context))
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

    /** Lightweight legacy support export: 24 hours, minified and split below 900 KB. */
    fun exportRecent24h(context: Context, now: Long = System.currentTimeMillis()): List<File> {
        val cutoff = now - RECENT_WINDOW_MILLIS
        val market = PumpBotEngine.snapshot(context)
        val breathing = LiveMarketBreathingStore.snapshot(context, now)
        val fusionMarket = BitpandaFusionStore.state(context)
        val rawJournal = readJournal(context, cutoff)
        val compactJournal = ResearchLogCompactionPolicy.compact(rawJournal, HEARTBEAT_MILLIS)
        val accounts = JSONObject()
            .put("PumpMachine", recentJson(PumpMachineStore.toJson(PumpMachineStore.state(context)), cutoff))
            .put("PumpMachine2", recentJson(PumpMachine2Store.toJson(PumpMachine2Store.state(context)), cutoff))
            .put("PumpMachineRetest", recentJson(PumpMachineRetestStore.toJson(PumpMachineRetestStore.state(context)), cutoff))
            .put("PumpMachineSafe", recentJson(PumpMachineSafeStore.toJson(PumpMachineSafeStore.state(context)), cutoff))
            .put("FusionSim", recentJson(FusionSimStore.toJson(FusionSimStore.state(context)), cutoff))
        val report = JSONObject()
            .put("schema", "pump-signal-support-log-24h-v600")
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("generatedAt", now)
            .put("windowHours", 24)
            .put("cutoffAt", cutoff)
            .put("clearedAfterExport", false)
            .put("safety", JSONObject()
                .put("realOrdersImplemented", false)
                .put("v6ExecutionMode", "SHADOW_ONLY")
                .put("containsApiKeys", false))
            .put("market", JSONObject()
                .put("pumpEur", PaperExecutionPolicy.displayPrice(market, now))
                .put("lastSync", market.lastSync)
                .put("bookBidNotional", market.bookBidNotional ?: JSONObject.NULL)
                .put("bookAskNotional", market.bookAskNotional ?: JSONObject.NULL)
                .put("spreadPercent", market.spreadPercent ?: JSONObject.NULL))
            .put("flow", breathing.toJson())
            .put("bitpandaFusion", fusionMarket.toJson().apply { put("error", sanitize(fusionMarket.error)) })
            .put("v6ScalpExecutionShadow", ScalpExecutionIntelligenceStoreV600.current(context).toJson())
            .put("latestEntryAudit", EntryOpportunityAuditStore.latest(context).toJson())
            .put("latestLiquidityShadow", LiquidityReleaseShadowStore.latest(context).toJson())
            .put("deepSeekEntryCoach", DeepSeekEntryCoachStore.exportJson(context))
            .put("accounts", accounts)
            .put("journalPolicy", JSONObject()
                .put("rawRecords24h", rawJournal.size)
                .put("compactedRecords24h", compactJournal.size)
                .put("fullArchiveStillRetainedDays", RETENTION_DAYS)
                .put("maximumFileBytes", MAX_SUPPORT_FILE_BYTES)
                .put("format", "MINIFIED_JSON_SPLIT_IF_NEEDED"))
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        dir.listFiles()?.filter {
            "-Log-24h-" in it.name || "-Log-48h-" in it.name
        }?.forEach { it.delete() }
        val payloads = SupportLogSplitPolicy.split(
            base = report,
            journal = compactJournal,
            maxBytes = MAX_SUPPORT_FILE_BYTES
        )
        return payloads.mapIndexed { index, payload ->
            File(
                dir,
                "PumpSignal-V${BuildConfig.VERSION_NAME}-Log-24h-${stamp(now)}-part" +
                    "%02d-of-%02d.json".format(Locale.US, index + 1, payloads.size)
            ).apply { writeText(payload, Charsets.UTF_8) }
        }
    }

    /** V6 user-facing support button now exports the compact text report, not a near-2MB JSON. */
    fun shareRecent24h(context: Context) {
        V6ScalpReportStore.shareRecent24h(context)
    }

    private fun readJournal(context: Context, cutoff: Long): List<JSONObject> {
        val result = ArrayList<JSONObject>()
        File(context.filesDir, DIRECTORY).listFiles()
            ?.filter { it.name.endsWith(".ndjson") && it.lastModified() >= cutoff }
            ?.sortedBy { it.name }
            ?.forEach { file -> file.useLines { lines -> lines.forEach { raw ->
                runCatching { JSONObject(raw) }.getOrNull()?.takeIf { it.optLong("time") >= cutoff }
                    ?.let(result::add)
            } } }
        return result
    }

    /** Keeps balances/state but trims any nested time-based history array to the support window. */
    private fun recentJson(source: JSONObject, cutoff: Long): JSONObject {
        val result = JSONObject()
        source.keys().forEach { key ->
            when (val value = source.opt(key)) {
                is JSONObject -> result.put(key, recentJson(value, cutoff))
                is JSONArray -> {
                    val kept = JSONArray()
                    for (i in 0 until value.length()) {
                        val item = value.opt(i)
                        if (item is JSONObject) {
                            val time = item.optLong("time", item.optLong("at", Long.MAX_VALUE))
                            if (time >= cutoff) kept.put(recentJson(item, cutoff))
                        } else if (value.length() <= 100) kept.put(item)
                    }
                    result.put(key, kept)
                }
                else -> result.put(key, value)
            }
        }
        return result
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

internal object SupportLogSplitPolicy {
    fun split(base: JSONObject, journal: List<JSONObject>, maxBytes: Int): List<String> {
        require(maxBytes > 1_024) { "Слишком маленький предел файла" }
        val chunks = ArrayList<List<JSONObject>>()
        var current = ArrayList<JSONObject>()
        journal.forEach { event ->
            val candidate = ArrayList(current).apply { add(event) }
            if (payload(base, candidate, 9_999, 9_999, journal.size).utf8Size() <= maxBytes) {
                current = candidate
            } else {
                require(current.isNotEmpty()) { "Одна запись журнала превышает предел файла" }
                chunks += current
                current = arrayListOf(event)
            }
        }
        if (current.isNotEmpty() || chunks.isEmpty()) chunks += current
        val count = chunks.size
        return chunks.mapIndexed { index, events ->
            payload(base, events, index + 1, count, journal.size).also {
                require(it.utf8Size() <= maxBytes) { "Часть журнала превысила предел файла" }
            }
        }
    }

    private fun payload(
        base: JSONObject,
        events: List<JSONObject>,
        part: Int,
        partCount: Int,
        totalJournalRecords: Int
    ): String = JSONObject(base.toString())
        .put("parts", JSONObject()
            .put("part", part)
            .put("partCount", partCount)
            .put("journalRecordsInPart", events.size)
            .put("totalJournalRecords", totalJournalRecords)
            .put("uploadAllPartsTogether", partCount > 1))
        .put("journal", JSONArray().apply { events.forEach { put(it) } })
        .toString()

    private fun String.utf8Size(): Int = toByteArray(Charsets.UTF_8).size
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
