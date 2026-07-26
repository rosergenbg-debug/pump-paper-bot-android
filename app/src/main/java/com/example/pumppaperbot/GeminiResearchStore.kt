package com.example.pumppaperbot

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class GeminiPriceObservation(
    val at: Long,
    val priceEur: Double
)

/**
 * Append-only research storage for activity and post-response market prices.
 * Portfolio state stays in the existing preferences because it is small and
 * updated only when a decision or evaluation changes.
 */
object GeminiResearchStore {
    private const val DATABASE = "gemini_research_v37.db"
    private const val VERSION = 1
    private const val MAX_ACTIVITY_ROWS = 25_000
    internal const val ACTIVITY_RETENTION_MILLIS = 24L * 60L * 60L * 1000L
    private const val PRICE_RETENTION_MILLIS = 14L * 24L * 60L * 60L * 1000L
    private const val MAX_ACTIVITY_READ = 2_000
    private val lock = Any()
    private var activityWritesSincePrune = 0
    private var activityRevision = 0L
    private var cachedActivityRevision = -1L
    private var cachedActivity: List<GeminiActivityEvent> = emptyList()
    @Volatile private var helper: Helper? = null

    fun recordActivity(context: Context, event: GeminiActivityEvent) = synchronized(lock) {
        val db = db(context)
        insertActivity(db, event)
        activityRevision++
        activityWritesSincePrune++
        if (activityWritesSincePrune >= 20) {
            pruneActivity(db, event.at)
            activityWritesSincePrune = 0
        }
    }

    fun insertLegacyActivity(
        context: Context,
        events: List<GeminiActivityEvent>
    ) = synchronized(lock) {
        if (events.isEmpty()) return@synchronized
        val db = db(context)
        db.beginTransaction()
        try {
            events.forEach { insertActivity(db, it) }
            activityRevision++
            pruneActivity(db, System.currentTimeMillis())
            activityWritesSincePrune = 0
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun activity(context: Context): List<GeminiActivityEvent> = synchronized(lock) {
        val cutoff = System.currentTimeMillis() - ACTIVITY_RETENTION_MILLIS
        if (cachedActivityRevision == activityRevision) {
            val current = cachedActivity.filter { it.at >= cutoff }
            if (current.size != cachedActivity.size) cachedActivity = current
            return@synchronized current
        }
        val result = ArrayList<GeminiActivityEvent>()
        db(context).query(
            "activity",
            arrayOf("at", "stage", "result", "detail", "duration_ms", "model", "hour_id", "attempt"),
            "at >= ?",
            arrayOf(cutoff.toString()),
            null,
            null,
            "id DESC",
            MAX_ACTIVITY_READ.toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += GeminiActivityEvent(
                    at = cursor.getLong(0),
                    stage = cursor.getString(1).orEmpty(),
                    result = cursor.getString(2).orEmpty(),
                    detail = cursor.getString(3).orEmpty(),
                    durationMillis = cursor.getLong(4),
                    model = cursor.getString(5).orEmpty(),
                    hourId = cursor.getLong(6),
                    attempt = cursor.getInt(7)
                )
            }
        }
        result.asReversed().also {
            cachedActivity = it
            cachedActivityRevision = activityRevision
        }
    }

    fun recordPrice(
        context: Context,
        observation: GeminiPriceObservation
    ) = synchronized(lock) {
        if (observation.at <= 0L || observation.priceEur <= 0.0 ||
            !observation.priceEur.isFinite()
        ) return@synchronized
        db(context).insertWithOnConflict(
            "price_observation",
            null,
            ContentValues().apply {
                put("at", observation.at)
                put("price_eur", observation.priceEur)
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
        db(context).delete(
            "price_observation",
            "at < ?",
            arrayOf((observation.at - PRICE_RETENTION_MILLIS).toString())
        )
    }

    fun completedOutcomes(
        context: Context,
        decisions: List<GeminiHourlyDecision>
    ): List<GeminiHourOutcome> = synchronized(lock) {
        val pending = decisions.filter {
            it.evaluationVersion >= GeminiHourlyDecision.CAUSAL_EVALUATION_VERSION &&
                it.evaluatedReturnPercent == null &&
                it.executionQuoteAt > 0L &&
                it.price > 0.0
        }
        if (pending.isEmpty()) return@synchronized emptyList()
        val earliest = pending.minOf { it.executionQuoteAt }
        val observations = loadPricesSince(db(context), earliest)
        pending.mapNotNull { decision ->
            val targetAt = GeminiEvaluationWindow.targetAt(decision)
            val close = observations.firstOrNull { it.at >= targetAt } ?: return@mapNotNull null
            val high = observations.asSequence()
                .filter { it.at >= decision.executionQuoteAt && it.at <= close.at }
                .maxOfOrNull { it.priceEur }
                ?: close.priceEur
            GeminiHourOutcome(
                decisionId = decision.id,
                evaluatedAt = close.at,
                closePrice = close.priceEur,
                highPrice = high
            )
        }
    }

    fun savePortfolioMetrics(
        context: Context,
        portfolio: GeminiPaperPortfolio
    ) = synchronized(lock) {
        db(context).insertWithOnConflict(
            "portfolio_metrics",
            null,
            ContentValues().apply {
                put("id", 1)
                put("peak_value_eur", portfolio.peakValueEur)
                put("max_drawdown_pct", portfolio.maxDrawdownPercent)
                put("causal_peak_value_eur", portfolio.causalPeakValueEur)
                put("causal_max_drawdown_pct", portfolio.causalMaxDrawdownPercent)
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun applyPortfolioMetrics(
        context: Context,
        portfolio: GeminiPaperPortfolio
    ): GeminiPaperPortfolio = synchronized(lock) {
        db(context).query(
            "portfolio_metrics",
            arrayOf(
                "peak_value_eur",
                "max_drawdown_pct",
                "causal_peak_value_eur",
                "causal_max_drawdown_pct"
            ),
            "id = 1",
            null,
            null,
            null,
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@synchronized portfolio
            portfolio.copy(
                peakValueEur = maxOf(portfolio.peakValueEur, cursor.getDouble(0)),
                maxDrawdownPercent = maxOf(portfolio.maxDrawdownPercent, cursor.getDouble(1)),
                causalPeakValueEur = maxOf(portfolio.causalPeakValueEur, cursor.getDouble(2)),
                causalMaxDrawdownPercent = maxOf(
                    portfolio.causalMaxDrawdownPercent,
                    cursor.getDouble(3)
                )
            )
        }
    }

    fun clear(context: Context) = synchronized(lock) {
        val db = db(context)
        db.delete("activity", null, null)
        db.delete("price_observation", null, null)
        db.delete("portfolio_metrics", null, null)
        activityRevision++
        cachedActivityRevision = activityRevision
        cachedActivity = emptyList()
    }

    private fun loadPricesSince(
        db: SQLiteDatabase,
        since: Long
    ): List<GeminiPriceObservation> {
        val result = ArrayList<GeminiPriceObservation>()
        db.query(
            "price_observation",
            arrayOf("at", "price_eur"),
            "at >= ?",
            arrayOf(since.toString()),
            null,
            null,
            "at ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += GeminiPriceObservation(cursor.getLong(0), cursor.getDouble(1))
            }
        }
        return result
    }

    private fun insertActivity(db: SQLiteDatabase, event: GeminiActivityEvent) {
        db.insert(
            "activity",
            null,
            ContentValues().apply {
                put("at", event.at)
                put("stage", event.stage)
                put("result", event.result)
                put("detail", event.detail)
                put("duration_ms", event.durationMillis)
                put("model", event.model)
                put("hour_id", event.hourId)
                put("attempt", event.attempt)
            }
        )
    }

    private fun pruneActivity(db: SQLiteDatabase, now: Long) {
        db.delete(
            "activity",
            "at < ?",
            arrayOf((now - ACTIVITY_RETENTION_MILLIS).toString())
        )
        db.execSQL(
            "DELETE FROM activity WHERE id NOT IN " +
                "(SELECT id FROM activity ORDER BY id DESC LIMIT $MAX_ACTIVITY_ROWS)"
        )
    }

    private fun db(context: Context): SQLiteDatabase {
        val existing = helper
        if (existing != null) return existing.writableDatabase
        return synchronized(lock) {
            val again = helper
            if (again != null) {
                again.writableDatabase
            } else {
                Helper(context.applicationContext).also { helper = it }.writableDatabase
            }
        }
    }

    private class Helper(context: Context) :
        SQLiteOpenHelper(context, DATABASE, null, VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE activity (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    at INTEGER NOT NULL,
                    stage TEXT NOT NULL,
                    result TEXT NOT NULL,
                    detail TEXT NOT NULL,
                    duration_ms INTEGER NOT NULL,
                    model TEXT NOT NULL,
                    hour_id INTEGER NOT NULL,
                    attempt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX activity_at_idx ON activity(at)")
            db.execSQL(
                """
                CREATE TABLE price_observation (
                    at INTEGER PRIMARY KEY,
                    price_eur REAL NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE portfolio_metrics (
                    id INTEGER PRIMARY KEY,
                    peak_value_eur REAL NOT NULL,
                    max_drawdown_pct REAL NOT NULL,
                    causal_peak_value_eur REAL NOT NULL,
                    causal_max_drawdown_pct REAL NOT NULL
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }
}
