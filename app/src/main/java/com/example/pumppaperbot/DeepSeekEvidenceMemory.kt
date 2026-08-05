package com.example.pumppaperbot

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max

data class EvidencePatternStats(
    val featureKey: String,
    val horizonMinutes: Int,
    val independentCases: Int,
    val precisionPercent: Double,
    val validationPrecisionPercent: Double,
    val netExpectancyPercent: Double,
    val baselinePrecisionPercent: Double,
    val liftPercent: Double,
    val promoted: Boolean
) {
    val usefulnessPercent: Double get() = precisionPercent
}

data class EvidenceMemoryStatus(
    val bytesUsed: Long,
    val byteLimit: Long,
    val predictions: Int,
    val evaluatedPredictions: Int,
    val suppliedToDecisions: Int,
    val promotedPatterns: Int,
    val bestUsefulnessPercent: Double,
    val promptRequired: Boolean
) {
    val percentFull: Int = if (byteLimit > 0L) (bytesUsed * 100L / byteLimit).toInt().coerceIn(0, 100) else 0

    fun russianSummary(): String = buildString {
        append("Память: $predictions прогнозов, $evaluatedPredictions уже проверены")
        append("; доказанных закономерностей: $promotedPatterns")
        if (bestUsefulnessPercent > 0.0) append(String.format(java.util.Locale.GERMANY, "; лучшая полезность %.1f%%", bestUsefulnessPercent))
        append("; использовано $percentFull% из ${byteLimit / MIB} МБ")
    }

    companion object { const val MIB = 1024L * 1024L }
}

internal data class EvidenceOutcomePoint(
    val observedAt: Long,
    val predictedDirection: Int,
    val baselineDirection: Int,
    val returnPercent: Double
)

internal object EvidencePatternEvaluator {
    const val MIN_CASES = 30
    const val MIN_PRECISION = 60.0
    const val ROUND_TRIP_FEE_PERCENT = 0.30
    const val MAX_PATTERN_AGE_MILLIS = 180L * 24L * 60L * 60L * 1000L

    fun evaluate(
        featureKey: String,
        horizonMinutes: Int,
        raw: List<EvidenceOutcomePoint>,
        now: Long
    ): EvidencePatternStats {
        val spacing = TimeUnit.MINUTES.toMillis(horizonMinutes.toLong())
        val independent = mutableListOf<EvidenceOutcomePoint>()
        var lastAcceptedAt = Long.MIN_VALUE
        raw.asSequence().filter { it.observedAt >= now - MAX_PATTERN_AGE_MILLIS }
            .sortedBy { it.observedAt }.forEach { point ->
                if (lastAcceptedAt == Long.MIN_VALUE || point.observedAt - lastAcceptedAt >= spacing) {
                    independent += point
                    lastAcceptedAt = point.observedAt
                }
            }
        val precision = precision(independent)
        val baseline = baselinePrecision(independent)
        val expectancy = independent.map { point ->
            point.predictedDirection * point.returnPercent - ROUND_TRIP_FEE_PERCENT
        }.averageOrZero()
        val validationSize = max(6, (independent.size * 0.20).toInt()).coerceAtMost(independent.size)
        val validation = independent.takeLast(validationSize)
        val validationPrecision = precision(validation)
        val lift = precision - baseline
        val promoted = independent.size >= MIN_CASES && precision >= MIN_PRECISION &&
            validationPrecision >= MIN_PRECISION && expectancy > 0.0 && lift > 0.0
        return EvidencePatternStats(
            featureKey = featureKey,
            horizonMinutes = horizonMinutes,
            independentCases = independent.size,
            precisionPercent = precision,
            validationPrecisionPercent = validationPrecision,
            netExpectancyPercent = expectancy,
            baselinePrecisionPercent = baseline,
            liftPercent = lift,
            promoted = promoted
        )
    }

    private fun precision(points: List<EvidenceOutcomePoint>): Double {
        if (points.isEmpty()) return 0.0
        return points.count { it.predictedDirection * it.returnPercent > 0.0 } * 100.0 / points.size
    }

    private fun baselinePrecision(points: List<EvidenceOutcomePoint>): Double {
        val eligible = points.filter { it.baselineDirection != 0 }
        if (eligible.isEmpty()) return 50.0
        return eligible.count { it.baselineDirection * it.returnPercent > 0.0 } * 100.0 / eligible.size
    }

    private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()
}

object EvidenceFeatureKey {
    fun from(
        snapshot: LiveSnapshot,
        breathingScore: Int?,
        ecosystem: PumpEcosystemSnapshot
    ): String = listOf(
        "дых=${bucket(breathingScore)}",
        "эко=${bucket(ecosystem.score)}",
        "кач=${when { ecosystem.dataQuality >= 75 -> "выс"; ecosystem.dataQuality >= 50 -> "ср"; else -> "низ" }}",
        "лок=${sign(snapshot.directionScore)}",
        "реж=${if (snapshot.waitMode == "SELL") "позиция" else "ожидание"}",
        "пад=${if (snapshot.rapidDrop.active) "да" else "нет"}"
    ).joinToString("|")

    internal fun bucket(value: Int?): String = when {
        value == null -> "нет"
        value >= 35 -> "++"
        value >= 10 -> "+"
        value <= -35 -> "--"
        value <= -10 -> "-"
        else -> "0"
    }

    private fun sign(value: Int): String = when {
        value >= 20 -> "+"
        value <= -20 -> "-"
        else -> "0"
    }

}

private class EvidenceDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE predictions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                observed_at INTEGER NOT NULL,
                price REAL NOT NULL,
                predicted_direction INTEGER NOT NULL,
                baseline_direction INTEGER NOT NULL,
                direction_score INTEGER NOT NULL,
                confidence INTEGER NOT NULL,
                action TEXT NOT NULL,
                feature_key TEXT NOT NULL,
                breathing_score INTEGER,
                ecosystem_score INTEGER,
                ecosystem_quality INTEGER NOT NULL,
                model_summary TEXT NOT NULL,
                memory_supplied INTEGER NOT NULL DEFAULT 0,
                return_15m REAL,
                return_60m REAL,
                return_180m REAL,
                return_360m REAL,
                return_1440m REAL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_predictions_feature_time ON predictions(feature_key, observed_at)")
        db.execSQL("CREATE INDEX idx_predictions_pending ON predictions(observed_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    companion object {
        const val DB_NAME = "deepseek_evidence_v416.db"
        const val DB_VERSION = 1
    }
}

object DeepSeekEvidenceMemory {
    private const val PREFS = "deepseek_evidence_memory_v416"
    private const val KEY_BLOCKS = "allocation_blocks"
    private const val KEY_PROMPT_DISMISSED_AT_BYTES = "prompt_dismissed_at_bytes"
    private const val MIB = 1024L * 1024L
    private const val DEFAULT_LIMIT = 50L * MIB
    private const val PROMPT_AT_PERCENT = 95
    private data class Horizon(val minutes: Int, val column: String, val toleranceMinutes: Int)
    private val horizons = listOf(
        Horizon(15, "return_15m", 5),
        Horizon(60, "return_60m", 15),
        Horizon(180, "return_180m", 30),
        Horizon(360, "return_360m", 60),
        Horizon(1440, "return_1440m", 120)
    )

    fun updateOutcomes(context: Context, currentPrice: Double, now: Long = System.currentTimeMillis()) {
        if (currentPrice <= 0.0 || !currentPrice.isFinite()) return
        val db = EvidenceDatabase(context).writableDatabase
        horizons.forEach { horizon ->
            val due = now - TimeUnit.MINUTES.toMillis(horizon.minutes.toLong())
            val earliest = due - TimeUnit.MINUTES.toMillis(horizon.toleranceMinutes.toLong())
            db.query(
                "predictions", arrayOf("id", "price"),
                "${horizon.column} IS NULL AND observed_at BETWEEN ? AND ?",
                arrayOf(earliest.toString(), due.toString()),
                null, null, "observed_at ASC", "1000"
            ).use { cursor ->
                val updates = mutableListOf<Pair<Long, Double>>()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val startPrice = cursor.getDouble(1)
                    if (startPrice > 0.0) updates += id to ((currentPrice / startPrice - 1.0) * 100.0)
                }
                updates.forEach { (id, result) ->
                    db.update("predictions", ContentValues().apply { put(horizon.column, result) }, "id=?", arrayOf(id.toString()))
                }
            }
        }
    }

    fun recordPrediction(
        context: Context,
        observedAt: Long,
        price: Double,
        directionScore: Int,
        baselineDirectionScore: Int,
        confidence: Int,
        action: String,
        featureKey: String,
        breathingScore: Int?,
        ecosystem: PumpEcosystemSnapshot,
        summary: String,
        memorySupplied: Boolean
    ): Boolean {
        if (price <= 0.0 || !price.isFinite() || abs(directionScore) < 10) return false
        if (bytesUsed(context) >= byteLimit(context)) return false
        val values = ContentValues().apply {
            put("observed_at", observedAt)
            put("price", price)
            put("predicted_direction", if (directionScore > 0) 1 else -1)
            put("baseline_direction", when { baselineDirectionScore >= 20 -> 1; baselineDirectionScore <= -20 -> -1; else -> 0 })
            put("direction_score", directionScore.coerceIn(-100, 100))
            put("confidence", confidence.coerceIn(0, 100))
            put("action", action.take(12))
            put("feature_key", featureKey.take(240))
            breathingScore?.let { put("breathing_score", it.coerceIn(-100, 100)) }
            ecosystem.score?.let { put("ecosystem_score", it.coerceIn(-100, 100)) }
            put("ecosystem_quality", ecosystem.dataQuality.coerceIn(0, 100))
            put("model_summary", summary.take(400))
            put("memory_supplied", if (memorySupplied) 1 else 0)
        }
        return EvidenceDatabase(context).writableDatabase.insert("predictions", null, values) > 0L
    }

    fun promptSummary(
        context: Context,
        featureKey: String,
        now: Long = System.currentTimeMillis()
    ): JSONObject {
        val patterns = patternStats(context, featureKey, now)
        val promoted = patterns.filter { it.promoted }.sortedByDescending { it.usefulnessPercent }.take(3)
        val background = patterns.filterNot { it.promoted }.sortedWith(
            compareByDescending<EvidencePatternStats> { it.independentCases }.thenByDescending { it.precisionPercent }
        ).take(2)
        return JSONObject()
            .put("feature_key", featureKey)
            .put("promoted_patterns", JSONArray(promoted.map(::patternJson)))
            .put("background_patterns", JSONArray(background.map(::patternJson)))
            .put("promotion_rule", "минимум 30 независимых случаев, точность >=60%, положительное ожидание после 0,30% комиссий, лучше baseline и >=60% на свежей walk-forward части")
            .put("authority", "только контекст; память не может самостоятельно исполнить сделку или отменить защитные запреты")
    }

    fun hasPromotedPattern(context: Context, featureKey: String, now: Long = System.currentTimeMillis()): Boolean =
        patternStats(context, featureKey, now).any { it.promoted }

    fun patternStats(context: Context, featureKey: String, now: Long = System.currentTimeMillis()): List<EvidencePatternStats> {
        val db = EvidenceDatabase(context).readableDatabase
        return horizons.map { horizon ->
            val rows = mutableListOf<EvidenceOutcomePoint>()
            db.query(
                "predictions", arrayOf("observed_at", "predicted_direction", "baseline_direction", horizon.column),
                "feature_key=? AND ${horizon.column} IS NOT NULL AND observed_at>=?",
                arrayOf(featureKey, (now - EvidencePatternEvaluator.MAX_PATTERN_AGE_MILLIS).toString()),
                null, null, "observed_at ASC"
            ).use { cursor ->
                while (cursor.moveToNext()) rows += EvidenceOutcomePoint(
                    observedAt = cursor.getLong(0),
                    predictedDirection = cursor.getInt(1),
                    baselineDirection = cursor.getInt(2),
                    returnPercent = cursor.getDouble(3)
                )
            }
            EvidencePatternEvaluator.evaluate(featureKey, horizon.minutes, rows, now)
        }
    }

    fun status(context: Context, now: Long = System.currentTimeMillis()): EvidenceMemoryStatus {
        val db = EvidenceDatabase(context).readableDatabase
        fun count(where: String? = null): Int = db.rawQuery(
            "SELECT COUNT(*) FROM predictions${where?.let { " WHERE $it" }.orEmpty()}", null
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        val predictions = count()
        val evaluated = count(horizons.joinToString(" OR ") { it.column + " IS NOT NULL" })
        val supplied = count("memory_supplied=1")
        val keys = mutableListOf<String>()
        db.rawQuery("SELECT DISTINCT feature_key FROM predictions WHERE observed_at>=? LIMIT 200",
            arrayOf((now - EvidencePatternEvaluator.MAX_PATTERN_AGE_MILLIS).toString())).use { cursor ->
            while (cursor.moveToNext()) keys += cursor.getString(0)
        }
        val promoted = keys.flatMap { patternStats(context, it, now) }.filter { it.promoted }
        val used = bytesUsed(context)
        val limit = byteLimit(context)
        val dismissed = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_PROMPT_DISMISSED_AT_BYTES, 0L)
        return EvidenceMemoryStatus(
            bytesUsed = used,
            byteLimit = limit,
            predictions = predictions,
            evaluatedPredictions = evaluated,
            suppliedToDecisions = supplied,
            promotedPatterns = promoted.size,
            bestUsefulnessPercent = promoted.maxOfOrNull { it.usefulnessPercent } ?: 0.0,
            promptRequired = used * 100L >= limit * PROMPT_AT_PERCENT && used > dismissed + MIB
        )
    }

    fun shouldPrompt(context: Context): Boolean {
        val used = bytesUsed(context)
        val limit = byteLimit(context)
        val dismissed = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_PROMPT_DISMISSED_AT_BYTES, 0L)
        return used * 100L >= limit * PROMPT_AT_PERCENT && used > dismissed + MIB
    }

    fun allocateAnotherBlock(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_BLOCKS, prefs.getInt(KEY_BLOCKS, 1).coerceAtLeast(1) + 1)
            .remove(KEY_PROMPT_DISMISSED_AT_BYTES).apply()
    }

    fun dismissPrompt(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_PROMPT_DISMISSED_AT_BYTES, bytesUsed(context)).apply()
    }

    fun pruneLowValue(context: Context, now: Long = System.currentTimeMillis()): Int {
        val db = EvidenceDatabase(context).writableDatabase
        val promotedKeys = mutableSetOf<String>()
        db.rawQuery("SELECT DISTINCT feature_key FROM predictions", null).use { cursor ->
            while (cursor.moveToNext()) {
                val key = cursor.getString(0)
                if (patternStats(context, key, now).any { it.promoted }) promotedKeys += key
            }
        }
        val cutoff = now - 30L * 24L * 60L * 60L * 1000L
        val deleted = if (promotedKeys.isEmpty()) {
            db.delete("predictions", "id IN (SELECT id FROM predictions ORDER BY observed_at ASC LIMIT (SELECT COUNT(*)/2 FROM predictions))", null)
        } else {
            val placeholders = promotedKeys.joinToString(",") { "?" }
            db.delete("predictions", "observed_at<? AND feature_key NOT IN ($placeholders)",
                arrayOf(cutoff.toString(), *promotedKeys.toTypedArray()))
        }
        db.execSQL("VACUUM")
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_PROMPT_DISMISSED_AT_BYTES).apply()
        return deleted
    }

    private fun patternJson(value: EvidencePatternStats): JSONObject = JSONObject()
        .put("horizon_minutes", value.horizonMinutes)
        .put("independent_cases", value.independentCases)
        .put("directional_precision_pct", value.precisionPercent)
        .put("walk_forward_precision_pct", value.validationPrecisionPercent)
        .put("net_expectancy_after_fees_pct", value.netExpectancyPercent)
        .put("baseline_precision_pct", value.baselinePrecisionPercent)
        .put("lift_vs_baseline_pct", value.liftPercent)
        .put("promoted", value.promoted)

    private fun bytesUsed(context: Context): Long {
        val database = context.getDatabasePath(EvidenceDatabase.DB_NAME)
        return listOf(database, java.io.File(database.path + "-wal"), java.io.File(database.path + "-shm"))
            .sumOf { it.length() }
    }

    private fun byteLimit(context: Context): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getInt(KEY_BLOCKS, 1).coerceAtLeast(1) * DEFAULT_LIMIT
}
