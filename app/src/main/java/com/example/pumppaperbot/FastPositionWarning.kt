package com.example.pumppaperbot

import android.content.Context
import org.json.JSONObject

/**
 * Fast, local warning layer for Serge's manually opened position.
 * It never sells anything. It only converts a real short-horizon shock plus money pressure
 * into one preparation alert and, if conditions worsen, one critical escalation.
 */
data class FastPositionWarningState(
    val lastBand: Int = 0,
    val lastAlertAt: Long = 0L,
    val recoveryStartedAt: Long = 0L
) {
    fun toJson(): JSONObject = JSONObject()
        .put("lastBand", lastBand)
        .put("lastAlertAt", lastAlertAt)
        .put("recoveryStartedAt", recoveryStartedAt)

    companion object {
        fun fromJson(j: JSONObject) = FastPositionWarningState(
            lastBand = j.optInt("lastBand").coerceIn(0, 2),
            lastAlertAt = j.optLong("lastAlertAt"),
            recoveryStartedAt = j.optLong("recoveryStartedAt")
        )
    }
}

data class FastPositionWarningDecision(
    val band: Int,
    val shouldAlert: Boolean,
    val next: FastPositionWarningState,
    val pressure: FastMoneyPressure,
    val reason: String
)

object FastPositionWarningPolicy {
    const val PREPARE = 1
    const val CRITICAL = 2
    const val REARM_AFTER_RECOVERY_MILLIS = 2L * 60L * 1000L

    fun evaluate(
        previous: FastPositionWarningState,
        userPositionOpen: Boolean,
        shock: ShockReboundState,
        micro: MicroImpulseSnapshot,
        now: Long
    ): FastPositionWarningDecision {
        val pressure = FastMoneyPressurePolicy.evaluate(micro)
        val fresh = shock.fresh(now) && micro.connected &&
            micro.updatedAt > 0L && now - micro.updatedAt in 0..45_000L
        val critical = userPositionOpen && fresh && shock.active && (
            shock.failed ||
                (shock.drawdown3mPercent >= 4.0 && pressure.heavySelling && micro.aggressiveBuyPercent15s <= 44.0)
            )
        val prepare = userPositionOpen && fresh && shock.active && !critical && (
            pressure.heavySelling ||
                (shock.drawdown3mPercent >= 2.5 && micro.priceChange60sPercent <= -0.80 &&
                    micro.aggressiveBuyPercent15s <= 47.0)
            )
        val band = when {
            critical -> CRITICAL
            prepare -> PREPARE
            else -> 0
        }

        if (band == 0) {
            if (!userPositionOpen) {
                return FastPositionWarningDecision(
                    0, false, FastPositionWarningState(), pressure,
                    "Ручной позиции нет — входные/выходные предупреждения не требуются"
                )
            }
            val recoveryAt = if (previous.lastBand > 0) {
                previous.recoveryStartedAt.takeIf { it > 0L } ?: now
            } else 0L
            val rearmed = previous.lastBand > 0 && recoveryAt > 0L &&
                now - recoveryAt >= REARM_AFTER_RECOVERY_MILLIS
            val next = if (rearmed) FastPositionWarningState() else previous.copy(recoveryStartedAt = recoveryAt)
            return FastPositionWarningDecision(
                0, false, next, pressure,
                "Быстрый поток не подтверждает опасный продавливание; звонок не нужен"
            )
        }

        val escalation = band > previous.lastBand
        val newSeries = previous.lastBand == 0
        val shouldAlert = escalation || newSeries
        val next = previous.copy(
            lastBand = maxOf(previous.lastBand, band),
            lastAlertAt = if (shouldAlert) now else previous.lastAlertAt,
            recoveryStartedAt = 0L
        )
        val reason = if (band == CRITICAL) {
            "Критический быстрый провал: продавцы сохраняют давление или подтверждённый отскок сорвался"
        } else {
            "Ситуация меняется: локальный провал подтверждён ускоренным денежным потоком продавцов"
        }
        return FastPositionWarningDecision(band, shouldAlert, next, pressure, reason)
    }
}

object FastPositionWarningStore {
    private const val PREFS = "fast_position_warning_v519"
    private const val KEY = "state"

    @Synchronized
    fun sync(context: Context, now: Long = System.currentTimeMillis()): FastPositionWarningDecision {
        val snapshot = PumpBotEngine.snapshot(context)
        val decision = FastPositionWarningPolicy.evaluate(
            previous = state(context),
            userPositionOpen = snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0,
            shock = ShockReboundStore.state(context),
            micro = MicroImpulseStore.state(context),
            now = now
        )
        save(context, decision.next)
        if (decision.shouldAlert) {
            runCatching { PumpAlert.showFastPositionWarning(context, decision) }
        }
        return decision
    }

    fun state(context: Context): FastPositionWarningState {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        return if (raw.isNullOrBlank()) FastPositionWarningState() else runCatching {
            FastPositionWarningState.fromJson(JSONObject(raw))
        }.getOrDefault(FastPositionWarningState())
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun save(context: Context, state: FastPositionWarningState) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, state.toJson().toString()).apply()
    }
}
