package com.example.pumppaperbot

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

data class ResearchLedgerSummary(
    val events: Int,
    val trades: Int,
    val decisions: Int
)

/** Stable, append-only performance history. App version changes never clear this database. */
object ResearchPerformanceLedger {
    private const val DATABASE = "research_performance_ledger.db"
    private const val VERSION = 1
    private val lock = Any()
    @Volatile private var helper: Helper? = null

    fun capture(context: Context) = synchronized(lock) {
        ResearchHistoryArchive.ensureCaptured(context)
        val db = db(context)
        db.beginTransaction()
        try {
            captureLegacy(db, ResearchHistoryArchive.exportJson(context))
            captureApp(db, "V5+", AppPaperStore.state(context), !hasEpoch(db, "APP", "V5+"))
            capturePumpMachine(
                db,
                PumpMachineStore.state(context),
                !hasEpoch(db, "PumpMachine", "V5.21+")
            )
            capturePumpMachine2(
                db,
                PumpMachine2Store.state(context),
                !hasEpoch(db, "PumpMachine2", "V5.24+")
            )
            capturePumpVariant(db, "PumpMachineRetest", "V5.29+", PumpMachineRetestStore.state(context),
                !hasEpoch(db, "PumpMachineRetest", "V5.29+"))
            capturePumpVariant(db, "PumpMachineSafe", "V5.29+", PumpMachineSafeStore.state(context),
                !hasEpoch(db, "PumpMachineSafe", "V5.29+"))
            GeminiExitExperimentStore.state(context)?.portfolio?.let {
                captureGemini(db, "DeepSigX", "V5+", it, !hasEpoch(db, "DeepSigX", "V5+"))
            }
            captureFusion(db, FusionSimStore.state(context), !hasEpoch(db, "FusionSim", "V5+"))
            captureUser(db, UserPaperStore.state(context), !hasEpoch(db, "SERGE", "V5+"))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun summary(context: Context): ResearchLedgerSummary = synchronized(lock) {
        db(context).rawQuery(
            "SELECT COUNT(*), SUM(CASE WHEN kind='TRADE' THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN kind='DECISION' THEN 1 ELSE 0 END) FROM event",
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) ResearchLedgerSummary(0, 0, 0) else ResearchLedgerSummary(
                cursor.getInt(0), cursor.getInt(1), cursor.getInt(2)
            )
        }
    }

    fun exportJson(context: Context, limit: Int = 25_000): JSONObject = synchronized(lock) {
        capture(context)
        val db = db(context)
        val total = db.rawQuery("SELECT COUNT(*) FROM event", null).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
        val rows = JSONArray()
        db.query(
            "event",
            arrayOf("account", "epoch", "event_key", "kind", "action", "at", "price", "pnl_eur", "reason"),
            null, null, null, null, "at DESC, id DESC", limit.coerceIn(1, 50_000).toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                rows.put(JSONObject()
                    .put("account", cursor.getString(0))
                    .put("epoch", cursor.getString(1))
                    .put("eventKey", cursor.getString(2))
                    .put("kind", cursor.getString(3))
                    .put("action", cursor.getString(4))
                    .put("time", cursor.getLong(5))
                    .put("price", cursor.getDouble(6))
                    .put("pnlEur", cursor.getDouble(7))
                    .put("reason", cursor.getString(8)))
            }
        }
        JSONObject()
            .put("schema", "pump-signal-performance-ledger-v1")
            .put("appendOnly", true)
            .put("totalEvents", total)
            .put("exportedEvents", rows.length())
            .put("eventsNewestFirst", rows)
    }

    private fun captureLegacy(db: SQLiteDatabase, archive: JSONObject) {
        val accounts = archive.optJSONObject("accounts") ?: return
        captureLegacyPortfolio(db, "APP", accounts.opt("APP"))
        captureLegacyPortfolio(db, "DeepSig", accounts.opt("DeepSig"))
        val deepSigX = accounts.optJSONObject("DeepSigX")
        captureLegacyPortfolio(db, "DeepSigX", deepSigX?.opt("portfolio"))
    }

    private fun captureLegacyPortfolio(db: SQLiteDatabase, account: String, raw: Any?) {
        if (hasEpoch(db, account, "V4_ARCHIVE")) return
        val portfolio = jsonObject(raw) ?: return
        val trades = portfolio.optJSONArray("trades") ?: JSONArray()
        for (index in 0 until trades.length()) {
            val item = trades.optJSONObject(index) ?: continue
            insert(
                db, account, "V4_ARCHIVE", "TRADE",
                "${item.optLong("time")}:${item.optLong("decisionId", item.optLong("candleTime"))}:${item.optString("action")}",
                item.optString("action"), item.optLong("time", item.optLong("candleTime")),
                item.optDouble("price"), item.optDouble("pnlEur"), item.optString("reason")
            )
        }
        val decisions = portfolio.optJSONArray("decisions") ?: JSONArray()
        for (index in 0 until decisions.length()) {
            val item = decisions.optJSONObject(index) ?: continue
            insert(
                db, account, "V4_ARCHIVE", "DECISION",
                "${item.optLong("id", item.optLong("candleTime"))}:${item.optLong("decidedAt", item.optLong("time"))}",
                item.optString("requestedAction", item.optString("action")),
                item.optLong("decidedAt", item.optLong("time", item.optLong("candleTime"))),
                item.optDouble("price"), 0.0, item.optString("reason")
            )
        }
    }

    private fun captureApp(
        db: SQLiteDatabase,
        epoch: String,
        value: AppPaperPortfolio,
        fullImport: Boolean
    ) {
        (if (fullImport) value.trades else value.trades.takeLast(20)).forEach {
            insert(db, "APP", epoch, "TRADE", "${it.time}:${it.candleTime}:${it.action}",
                it.action, it.time, it.price, it.pnlEur, it.reason)
        }
        (if (fullImport) value.decisions else value.decisions.takeLast(50)).forEach {
            insert(db, "APP", epoch, "DECISION", "${it.time}:${it.candleTime}:${it.action}",
                it.action, it.time, it.price, 0.0, it.reason)
        }
    }

    private fun captureGemini(
        db: SQLiteDatabase,
        account: String,
        epoch: String,
        value: GeminiPaperPortfolio,
        fullImport: Boolean
    ) {
        (if (fullImport) value.trades else value.trades.takeLast(20)).forEach {
            insert(db, account, epoch, "TRADE", "${it.time}:${it.decisionId}:${it.action}",
                it.action, it.time, it.price, it.pnlEur, it.reason)
        }
        (if (fullImport) value.decisions else value.decisions.takeLast(50)).forEach {
            insert(db, account, epoch, "DECISION", it.id.toString(), it.requestedAction,
                it.decidedAt, it.price, 0.0, it.reason)
        }
    }

    private fun captureFusion(db: SQLiteDatabase, value: FusionSimPortfolio, fullImport: Boolean) {
        (if (fullImport) value.trades else value.trades.takeLast(20)).forEach {
            insert(db, "FusionSim", "V5+", "TRADE", "${it.time}:${it.decisionId}:${it.action}",
                it.action, it.time, it.price, it.pnlEur, it.reason)
        }
        (if (fullImport) value.decisions else value.decisions.takeLast(50)).forEach {
            insert(db, "FusionSim", "V5+", "DECISION", "${it.time}:${it.decisionId}",
                it.requestedAction, it.time, it.venuePrice, 0.0, "${it.result}; ${it.reason}")
        }
    }

    private fun capturePumpMachine(
        db: SQLiteDatabase,
        value: FusionSimPortfolio,
        fullImport: Boolean
    ) {
        (if (fullImport) value.trades else value.trades.takeLast(20)).forEach {
            insert(db, "PumpMachine", "V5.21+", "TRADE", "${it.time}:${it.decisionId}:${it.action}",
                it.action, it.time, it.price, it.pnlEur, it.reason)
        }
        (if (fullImport) value.decisions else value.decisions.takeLast(50)).forEach {
            insert(db, "PumpMachine", "V5.21+", "DECISION", "${it.time}:${it.decisionId}",
                it.requestedAction, it.time, it.venuePrice, 0.0, "${it.result}; ${it.reason}")
        }
    }

    private fun capturePumpMachine2(
        db: SQLiteDatabase,
        value: FusionSimPortfolio,
        fullImport: Boolean
    ) {
        (if (fullImport) value.trades else value.trades.takeLast(20)).forEach {
            insert(db, "PumpMachine2", "V5.24+", "TRADE", "${it.time}:${it.decisionId}:${it.action}",
                it.action, it.time, it.price, it.pnlEur, it.reason)
        }
        (if (fullImport) value.decisions else value.decisions.takeLast(50)).forEach {
            insert(db, "PumpMachine2", "V5.24+", "DECISION", "${it.time}:${it.decisionId}",
                it.requestedAction, it.time, it.venuePrice, 0.0, "${it.result}; ${it.reason}")
        }
    }

    private fun capturePumpVariant(
        db: SQLiteDatabase,
        account: String,
        epoch: String,
        value: FusionSimPortfolio,
        fullImport: Boolean
    ) {
        (if (fullImport) value.trades else value.trades.takeLast(20)).forEach {
            insert(db, account, epoch, "TRADE", "${it.time}:${it.decisionId}:${it.action}",
                it.action, it.time, it.price, it.pnlEur, it.reason)
        }
        (if (fullImport) value.decisions else value.decisions.takeLast(50)).forEach {
            insert(db, account, epoch, "DECISION", "${it.time}:${it.decisionId}",
                it.requestedAction, it.time, it.venuePrice, 0.0, "${it.result}; ${it.reason}")
        }
    }

    private fun captureUser(db: SQLiteDatabase, value: UserPaperPortfolio, fullImport: Boolean) {
        (if (fullImport) value.trades else value.trades.takeLast(20)).forEach {
            insert(db, "SERGE", "V5+", "TRADE", "${it.time}:${it.action}",
                it.action, it.time, it.price, it.pnlEur, "Ручная виртуальная сделка Сержа")
        }
    }

    private fun insert(
        db: SQLiteDatabase,
        account: String,
        epoch: String,
        kind: String,
        key: String,
        action: String,
        at: Long,
        price: Double,
        pnl: Double,
        reason: String
    ) {
        if (key.isBlank() || at <= 0L) return
        db.insertWithOnConflict("event", null, ContentValues().apply {
            put("account", account.take(30)); put("epoch", epoch.take(30)); put("event_key", key.take(120))
            put("kind", kind); put("action", action.take(30)); put("at", at)
            put("price", price.takeIf { it.isFinite() } ?: 0.0)
            put("pnl_eur", pnl.takeIf { it.isFinite() } ?: 0.0)
            put("reason", reason.take(800))
        }, SQLiteDatabase.CONFLICT_IGNORE)
    }

    private fun jsonObject(raw: Any?): JSONObject? = when (raw) {
        is JSONObject -> raw
        is String -> runCatching { JSONObject(raw) }.getOrNull()
        else -> null
    }

    private fun hasEpoch(db: SQLiteDatabase, account: String, epoch: String): Boolean =
        db.rawQuery(
            "SELECT 1 FROM event WHERE account=? AND epoch=? LIMIT 1",
            arrayOf(account, epoch)
        ).use { it.moveToFirst() }

    private fun db(context: Context): SQLiteDatabase {
        helper?.let { return it.writableDatabase }
        return synchronized(lock) {
            helper?.writableDatabase ?: Helper(context.applicationContext).also { helper = it }.writableDatabase
        }
    }

    private class Helper(context: Context) : SQLiteOpenHelper(context, DATABASE, null, VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE event (id INTEGER PRIMARY KEY AUTOINCREMENT, account TEXT NOT NULL, " +
                    "epoch TEXT NOT NULL, event_key TEXT NOT NULL, kind TEXT NOT NULL, action TEXT NOT NULL, " +
                    "at INTEGER NOT NULL, price REAL NOT NULL, pnl_eur REAL NOT NULL, reason TEXT NOT NULL, " +
                    "UNIQUE(account, epoch, kind, event_key))"
            )
            db.execSQL("CREATE INDEX event_at_idx ON event(at)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }
}
