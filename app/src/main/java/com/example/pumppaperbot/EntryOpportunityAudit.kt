package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class EntryGateStatus(
    val participant: String,
    val state: String,
    val reason: String,
    val confirmations: Int = 0,
    val confirmationsRequired: Int = 0
) {
    fun toJson() = JSONObject()
        .put("participant", participant)
        .put("state", state)
        .put("reason", reason)
        .put("confirmations", confirmations)
        .put("confirmations_required", confirmationsRequired)

    companion object {
        fun fromJson(value: JSONObject) = EntryGateStatus(
            participant = value.optString("participant"),
            state = value.optString("state"),
            reason = value.optString("reason"),
            confirmations = value.optInt("confirmations"),
            confirmationsRequired = value.optInt("confirmations_required")
        )
    }
}

data class EntryOpportunityAuditSnapshot(
    val at: Long = 0L,
    val priceEur: Double = 0.0,
    val flowScore: Int? = null,
    val capitalFlow: CapitalFlowProxy = CapitalFlowProxy(),
    val participants: List<EntryGateStatus> = emptyList()
) {
    fun toJson() = JSONObject()
        .put("at", at)
        .put("price_eur", priceEur)
        .put("flow_score", flowScore ?: JSONObject.NULL)
        .put("capital_flow", capitalFlow.toJson())
        .put("participants", JSONArray(participants.map { it.toJson() }))

    companion object {
        fun fromJson(value: JSONObject): EntryOpportunityAuditSnapshot {
            val items = value.optJSONArray("participants") ?: JSONArray()
            return EntryOpportunityAuditSnapshot(
                at = value.optLong("at"),
                priceEur = value.optDouble("price_eur"),
                flowScore = if (value.isNull("flow_score")) null else value.optInt("flow_score"),
                capitalFlow = CapitalFlowProxy.fromJson(value.optJSONObject("capital_flow")),
                participants = (0 until items.length()).mapNotNull {
                    items.optJSONObject(it)?.let(EntryGateStatus::fromJson)
                }
            )
        }
    }
}

object EntryGateStatusPolicy {
    fun app(portfolio: AppPaperPortfolio): EntryGateStatus {
        if (portfolio.inPosition) return EntryGateStatus("APP", "В ПОЗИЦИИ", "Виртуальный вход уже исполнен.")
        val decision = portfolio.decisions.lastOrNull()
            ?: return EntryGateStatus("APP", "ЖДЁТ", "Ещё нет закрытой 30-минутной свечи для решения.")
        return EntryGateStatus(
            "APP",
            if (decision.action == "BUY") "КАНДИДАТ" else "НЕ ВОШЁЛ",
            "${decision.action}: ${decision.reason}".take(600)
        )
    }

    fun deepSig(state: DeepSeekPrimaryState, portfolio: GeminiPaperPortfolio): EntryGateStatus {
        if (portfolio.inPosition) return EntryGateStatus("DeepSig", "В ПОЗИЦИИ", "Виртуальный BUY уже исполнен.")
        val confirming = state.proposedAction.uppercase() == "BUY" || state.independentEntryConfirmStreak > 0
        return EntryGateStatus(
            "DeepSig",
            if (confirming) "ПОДТВЕРЖДАЕТ ВХОД" else "НАБЛЮДАЕТ",
            if (confirming) "${state.executionStatus}. ${state.verificationSummary}".take(600)
            else state.summary.take(600),
            state.independentEntryConfirmStreak,
            2
        )
    }

    fun deepSigX(state: GeminiExitExperimentState?): EntryGateStatus {
        if (state == null) return EntryGateStatus("DeepSigX", "ЖДЁТ", "Ещё нет первой автономной проверки.")
        if (state.portfolio.inPosition) return EntryGateStatus("DeepSigX", "В ПОЗИЦИИ", "Автономный виртуальный BUY уже исполнен.")
        return EntryGateStatus(
            "DeepSigX",
            when (state.lastSignal) {
                "ENTRY_ARMED" -> "ПОДТВЕРЖДАЕТ ВХОД"
                "ENTRY_BLOCKED" -> "ВХОД ЗАБЛОКИРОВАН"
                "BUY" -> "ВХОД ИСПОЛНЕН"
                else -> "НЕ ВОШЁЛ"
            },
            state.lastReason.take(600),
            state.entryConfirmStreak,
            3
        )
    }

    fun fusion(
        portfolio: FusionSimPortfolio,
        market: FusionMarketSnapshot,
        breathing: LiveMarketBreathingSnapshot,
        now: Long
    ): EntryGateStatus {
        if (portfolio.inPosition) return EntryGateStatus("Fusion", "В ПОЗИЦИИ", "Flow BUY исполнен виртуально по Bitpanda ask; выход ждёт минус мгн/5/15/20.")
        val wave = breathing.flowWave.latest
        val reason = when {
            !market.configured -> "Read-only ключ Bitpanda не настроен; Fusion не может проверить реальную цену исполнения."
            !market.fresh(now) -> "Стакан Bitpanda устарел или недоступен; виртуальное исполнение безопасно заблокировано."
            !breathing.fresh || wave == null || breathing.instantScore == null -> "Fusion ждёт свежие мгновенный/5/15/30-минутные потоки."
            else -> "Для BUY нужны все плюсы: сейчас мгн ${breathing.instantScore}, 5м ${wave.score5m}, 15м ${wave.score15m}, 30м ${wave.score30m}."
        }
        return EntryGateStatus("Fusion", "НЕ ВОШЁЛ", reason)
    }
}

/** Durable 30-day explanation journal. It survives an APK update with the same package/signature. */
object EntryOpportunityAuditStore {
    private const val DIRECTORY = "entry_opportunity_audit_v57"
    private const val LATEST = "latest.json"
    private const val PREFS = "entry_opportunity_audit_v57"
    private const val LAST_CAPTURE = "last_capture"
    private const val MIN_GAP = 60_000L
    private const val RETENTION = 30L * 24L * 60L * 60L * 1_000L

    @Synchronized
    fun capture(context: Context, now: Long = System.currentTimeMillis()): EntryOpportunityAuditSnapshot {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (now - prefs.getLong(LAST_CAPTURE, 0L) < MIN_GAP) return latest(context)
        val market = PumpBotEngine.snapshot(context)
        val breathing = LiveMarketBreathingStore.snapshot(context, now)
        val impulse = ImpulseRadarStore.state(context)
        val deepSig = DeepSeekPrimaryStore.state(context, now)
        val deepSigPaper = GeminiPaperStore.state(context).portfolio
        val fusionMarket = BitpandaFusionStore.state(context)
        val result = EntryOpportunityAuditSnapshot(
            at = now,
            priceEur = PaperExecutionPolicy.displayPrice(market, now),
            flowScore = breathing.flowWave.latest?.composite(),
            capitalFlow = CapitalFlowProxyPolicy.evaluate(impulse, breathing, now),
            participants = listOf(
                EntryGateStatusPolicy.app(AppPaperStore.state(context)),
                EntryGateStatusPolicy.deepSig(deepSig, deepSigPaper),
                EntryGateStatusPolicy.deepSigX(GeminiExitExperimentStore.state(context)),
                EntryGateStatusPolicy.fusion(FusionSimStore.state(context), fusionMarket, breathing, now)
            )
        )
        val dir = File(context.filesDir, DIRECTORY).apply { mkdirs() }
        val raw = result.toJson().toString()
        File(dir, LATEST).writeText(raw, Charsets.UTF_8)
        File(dir, "audit-${now / (24L * 60L * 60L * 1_000L)}.ndjson")
            .appendText(raw + "\n", Charsets.UTF_8)
        val cutoff = now - RETENTION
        dir.listFiles()?.filter { it.name.endsWith(".ndjson") && it.lastModified() < cutoff }
            ?.forEach { it.delete() }
        prefs.edit().putLong(LAST_CAPTURE, now).commit()
        return result
    }

    fun latest(context: Context): EntryOpportunityAuditSnapshot = runCatching {
        EntryOpportunityAuditSnapshot.fromJson(
            JSONObject(File(File(context.filesDir, DIRECTORY), LATEST).readText(Charsets.UTF_8))
        )
    }.getOrDefault(EntryOpportunityAuditSnapshot())

    fun exportJson(context: Context): JSONObject {
        val dir = File(context.filesDir, DIRECTORY)
        val history = JSONArray()
        dir.listFiles()?.filter { it.name.endsWith(".ndjson") }?.sortedBy { it.name }?.forEach { file ->
            file.useLines(Charsets.UTF_8) { lines -> lines.forEach { line ->
                runCatching { history.put(JSONObject(line)) }
            } }
        }
        return JSONObject()
            .put("retention_days", 30)
            .put("latest", latest(context).toJson())
            .put("history", history)
    }
}
