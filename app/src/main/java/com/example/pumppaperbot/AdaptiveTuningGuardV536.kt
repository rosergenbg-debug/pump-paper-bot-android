package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max

data class DeepSeekTuningTrial(
    val active: Boolean = false,
    val key: String = "none",
    val startedAt: Long = 0L,
    val baselineMeanPnlEur: Double = 0.0,
    val baselineWinRate: Double = 0.0,
    val previousTuning: DeepSeekEntryTuning = DeepSeekEntryTuning(),
    val observedTrades: Int = 0,
    val candidateMeanPnlEur: Double = 0.0,
    val candidateWinRate: Double = 0.0,
    val status: String = "IDLE",
    val reason: String = "Автоюстировка ещё не запускалась."
) {
    fun toJson() = JSONObject()
        .put("active", active).put("key", key).put("startedAt", startedAt)
        .put("baselineMeanPnlEur", baselineMeanPnlEur).put("baselineWinRate", baselineWinRate)
        .put("previousTuning", previousTuning.toJson()).put("observedTrades", observedTrades)
        .put("candidateMeanPnlEur", candidateMeanPnlEur).put("candidateWinRate", candidateWinRate)
        .put("status", status).put("reason", reason)

    companion object {
        fun fromJson(j: JSONObject) = DeepSeekTuningTrial(
            active = j.optBoolean("active"), key = j.optString("key", "none"),
            startedAt = j.optLong("startedAt"), baselineMeanPnlEur = j.optDouble("baselineMeanPnlEur"),
            baselineWinRate = j.optDouble("baselineWinRate").coerceIn(0.0, 1.0),
            previousTuning = j.optJSONObject("previousTuning")?.let(DeepSeekEntryTuning::fromJson)
                ?: DeepSeekEntryTuning(),
            observedTrades = j.optInt("observedTrades").coerceAtLeast(0),
            candidateMeanPnlEur = j.optDouble("candidateMeanPnlEur"),
            candidateWinRate = j.optDouble("candidateWinRate").coerceIn(0.0, 1.0),
            status = j.optString("status", "IDLE"),
            reason = RussianOutputPolicy.visible(j.optString("reason")).take(500)
        )
    }
}

object DeepSeekTuningTrialStore {
    private const val PREFS = "deepseek_entry_tuning_trial_v536"
    private const val STATE = "state"
    fun state(c: Context): DeepSeekTuningTrial = runCatching {
        DeepSeekTuningTrial.fromJson(JSONObject(c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(STATE, null).orEmpty()))
    }.getOrDefault(DeepSeekTuningTrial())
    fun save(c: Context, v: DeepSeekTuningTrial) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(STATE, v.toJson().toString()).commit()
    }
}

data class AdaptiveTuningClosedOutcome(val agent: String, val time: Long, val pnlEur: Double)
data class AdaptiveTuningEvaluation(
    val tuning: DeepSeekEntryTuning,
    val trial: DeepSeekTuningTrial,
    val action: String,
    val reason: String
)

object DeepSeekAdaptiveTuningPolicy {
    const val STANDARD_TRIAL_TRADES = 6
    const val MAX_TRIAL_TRADES = 10
    private const val MAX_TRIAL_AGE = 7L * 24L * 60L * 60L * 1000L

    fun parse(rows: JSONArray): List<AdaptiveTuningClosedOutcome> =
        (0 until rows.length()).mapNotNull { i -> rows.optJSONObject(i)?.let {
            AdaptiveTuningClosedOutcome(it.optString("agent"), it.optLong("time"), it.optDouble("pnl_eur"))
        }}.distinctBy { "${it.agent}:${it.time}" }.sortedBy { it.time }

    fun begin(
        previous: DeepSeekEntryTuning,
        key: String,
        rows: List<AdaptiveTuningClosedOutcome>,
        now: Long
    ): DeepSeekTuningTrial {
        val baseline = rows.filter { it.time <= now }.takeLast(12)
        return DeepSeekTuningTrial(
            active = true, key = key, startedAt = now,
            baselineMeanPnlEur = baseline.meanPnl(), baselineWinRate = baseline.winRate(),
            previousTuning = previous, status = "TRIAL",
            reason = "Проверяем одно изменение $key; исходная настройка сохранена."
        )
    }

    fun evaluate(
        current: DeepSeekEntryTuning,
        trial: DeepSeekTuningTrial,
        rows: List<AdaptiveTuningClosedOutcome>,
        now: Long
    ): AdaptiveTuningEvaluation {
        if (!trial.active) return AdaptiveTuningEvaluation(current, trial, "IDLE", trial.reason)
        val after = rows.filter { it.time > trial.startedAt }.takeLast(MAX_TRIAL_TRADES)
        val count = after.size
        val total = after.sumOf { it.pnlEur }
        val mean = after.meanPnl()
        val wins = after.winRate()
        val updated = trial.copy(
            observedTrades = count, candidateMeanPnlEur = mean, candidateWinRate = wins
        )
        fun rollback(reason: String): AdaptiveTuningEvaluation {
            val restored = trial.previousTuning.copy(
                revision = max(current.revision, trial.previousTuning.revision) + 1,
                lastAdjustedAt = now, lastAdjustment = "AUTO_ROLLBACK ${trial.key}: $reason"
            )
            return AdaptiveTuningEvaluation(
                restored, updated.copy(active = false, status = "ROLLED_BACK", reason = reason),
                "ROLLBACK", reason
            )
        }
        fun keep(reason: String) = AdaptiveTuningEvaluation(
            current, updated.copy(active = false, status = "KEPT", reason = reason), "KEEP", reason
        )
        if (count >= 3 && total <= -10.0) {
            return rollback("аварийный откат: $count сделок дали ${fmt(total)} EUR NET")
        }
        if (count < STANDARD_TRIAL_TRADES) {
            return AdaptiveTuningEvaluation(
                current, updated.copy(reason = "Испытание: $count/$STANDARD_TRIAL_TRADES NET-сделок."),
                "WAIT", "нужно больше сделок"
            )
        }
        if (mean < trial.baselineMeanPnlEur - 0.50 || total <= 0.0 || wins < 0.40) {
            return rollback(
                "результат ухудшился: среднее ${fmt(mean)} EUR против базы " +
                    "${fmt(trial.baselineMeanPnlEur)} EUR, прибыльных ${fmt(wins * 100)}%"
            )
        }
        if (mean >= 0.50 && wins >= 0.50 && mean >= trial.baselineMeanPnlEur - 0.25) {
            return keep("среднее ${fmt(mean)} EUR NET, прибыльных ${fmt(wins * 100)}%")
        }
        if (count >= MAX_TRIAL_TRADES || now - trial.startedAt >= MAX_TRIAL_AGE) {
            return if (total > 0 && mean >= trial.baselineMeanPnlEur) keep("испытание положительно")
            else rollback("испытание не доказало улучшение после комиссий")
        }
        return AdaptiveTuningEvaluation(current, updated, "WAIT", "испытание продолжается")
    }

    private fun List<AdaptiveTuningClosedOutcome>.meanPnl() =
        if (isEmpty()) 0.0 else sumOf { it.pnlEur } / size
    private fun List<AdaptiveTuningClosedOutcome>.winRate() =
        if (isEmpty()) 0.0 else count { it.pnlEur > 0 }.toDouble() / size
    private fun fmt(v: Double) = String.format(java.util.Locale.GERMANY, "%.2f", v)
}

object DeepSeekAdaptiveTuningGuard {
    fun startTrial(
        c: Context, previous: DeepSeekEntryTuning, key: String, outcomes: JSONArray, now: Long
    ) = DeepSeekTuningTrialStore.save(
        c, DeepSeekAdaptiveTuningPolicy.begin(
            previous, key, DeepSeekAdaptiveTuningPolicy.parse(outcomes), now
        )
    )

    fun reconcile(c: Context, outcomes: JSONArray, now: Long): String? {
        val trial = DeepSeekTuningTrialStore.state(c)
        if (!trial.active) return null
        val current = DeepSeekEntryCoachStore.tuning(c)
        val result = DeepSeekAdaptiveTuningPolicy.evaluate(
            current, trial, DeepSeekAdaptiveTuningPolicy.parse(outcomes), now
        )
        if (result.trial != trial) DeepSeekTuningTrialStore.save(c, result.trial)
        if (result.tuning != current) DeepSeekEntryCoachStore.saveTuning(c, result.tuning)
        return when (result.action) {
            "ROLLBACK" -> "АВТООТКАТ: ${result.reason}"
            "KEEP" -> "ЮСТИРОВКА СОХРАНЕНА: ${result.reason}"
            else -> null
        }
    }
}
