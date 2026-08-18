package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ArchivedResearchAccount(
    val name: String,
    val trades: Int,
    val decisions: Int,
    val firstEventAt: Long,
    val lastEventAt: Long,
    val readable: Boolean
)

data class ResearchArchiveSummary(
    val accounts: List<ArchivedResearchAccount>
) {
    val tradeCount: Int get() = accounts.sumOf { it.trades }
    val decisionCount: Int get() = accounts.sumOf { it.decisions }
    val hasHistory: Boolean get() = tradeCount > 0 || decisionCount > 0

    fun compactText(): String = if (hasHistory) {
        "АРХИВ ДО V5 СОХРАНЁН • " + accounts.joinToString(" • ") {
            "${it.name}: ${it.trades} сделок, ${it.decisions} решений"
        }
    } else {
        "АРХИВ ДО V5 • сохранённых сделок не найдено"
    }
}

/**
 * V5 deliberately started separate comparable portfolios. This archive keeps the
 * earlier V4 research visible without mixing incompatible balances or PnL.
 */
object ResearchHistoryArchive {
    private const val DIRECTORY = "research_archive"
    private const val FILE_NAME = "legacy-v4-snapshot.json"
    private const val APP_PREFS = "app_paper_v317"
    private const val DEEPSIG_PREFS = "gemini_paper_v34"
    private const val DEEPSIGX_PREFS = "gemini_exit_experiment_v319"

    @Synchronized
    fun ensureCaptured(context: Context, now: Long = System.currentTimeMillis()) {
        val target = archiveFile(context)
        if (target.exists()) return
        val root = JSONObject()
            .put("schema", "pump-signal-legacy-v4-archive-v1")
            .put("capturedAt", now)
            .put("note", "V4 archive only; not merged into V5 balances or PnL")
            .put("accounts", JSONObject()
                .put("APP", legacyValue(context, APP_PREFS, "portfolio"))
                .put("DeepSig", legacyValue(context, DEEPSIG_PREFS, "portfolio"))
                .put("DeepSigX", legacyValue(context, DEEPSIGX_PREFS, "state")))
        val dir = target.parentFile?.apply { mkdirs() } ?: return
        val temporary = File(dir, "$FILE_NAME.tmp")
        temporary.writeText(root.toString(), Charsets.UTF_8)
        if (!temporary.renameTo(target)) {
            target.writeText(root.toString(), Charsets.UTF_8)
            temporary.delete()
        }
    }

    fun summary(context: Context): ResearchArchiveSummary {
        return runCatching {
            ensureCaptured(context)
            summarize(read(context))
        }.getOrDefault(ResearchArchiveSummary(emptyList()))
    }

    fun exportJson(context: Context): JSONObject {
        return runCatching {
            ensureCaptured(context)
            read(context)
        }.getOrElse {
            JSONObject().put("schema", "pump-signal-legacy-v4-archive-v1")
                .put("error", "archive unavailable")
        }
    }

    internal fun summarize(root: JSONObject): ResearchArchiveSummary {
        val accounts = root.optJSONObject("accounts") ?: JSONObject()
        return ResearchArchiveSummary(listOf("APP", "DeepSig", "DeepSigX").map { name ->
            summarizeAccount(name, accounts.opt(name))
        })
    }

    private fun summarizeAccount(name: String, raw: Any?): ArchivedResearchAccount {
        val value = when (raw) {
            is JSONObject -> raw
            is String -> runCatching { JSONObject(raw) }.getOrNull()
            else -> null
        }
        val portfolio = when {
            value == null -> null
            value.has("trades") || value.has("decisions") -> value
            value.has("portfolio") -> when (val nested = value.opt("portfolio")) {
                is JSONObject -> nested
                is String -> runCatching { JSONObject(nested) }.getOrNull()
                else -> null
            }
            else -> null
        }
        val trades = portfolio?.optJSONArray("trades") ?: JSONArray()
        val decisions = portfolio?.optJSONArray("decisions") ?: JSONArray()
        val times = buildList {
            for (index in 0 until trades.length()) {
                trades.optJSONObject(index)?.let { event ->
                    event.optLong("time", event.optLong("candleTime", 0L))
                        .takeIf { it > 0L }?.let(::add)
                }
            }
            for (index in 0 until decisions.length()) {
                decisions.optJSONObject(index)?.let { event ->
                    event.optLong("decidedAt", event.optLong("time", event.optLong("candleTime", 0L)))
                        .takeIf { it > 0L }?.let(::add)
                }
            }
        }
        return ArchivedResearchAccount(
            name = name,
            trades = trades.length(),
            decisions = decisions.length(),
            firstEventAt = times.minOrNull() ?: 0L,
            lastEventAt = times.maxOrNull() ?: 0L,
            readable = raw == null || portfolio != null
        )
    }

    private fun legacyValue(context: Context, prefsName: String, key: String): Any {
        val raw = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getString(key, null)
        if (raw.isNullOrBlank()) return JSONObject.NULL
        return runCatching { JSONObject(raw) }.getOrElse {
            JSONObject().put("unreadable", true).put("rawLength", raw.length)
        }
    }

    private fun read(context: Context): JSONObject = runCatching {
        JSONObject(archiveFile(context).readText(Charsets.UTF_8))
    }.getOrElse {
        JSONObject()
            .put("schema", "pump-signal-legacy-v4-archive-v1")
            .put("error", "archive unreadable")
            .put("accounts", JSONObject())
    }

    private fun archiveFile(context: Context): File =
        File(File(context.filesDir, DIRECTORY), FILE_NAME)
}
