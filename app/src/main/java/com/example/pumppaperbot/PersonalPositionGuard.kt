package com.example.pumppaperbot

import android.content.Context
import org.json.JSONObject
import kotlin.math.max

data class PersonalPositionGuardState(
    val entryTime: Long = 0L,
    val entryPrice: Double = 0.0,
    val peakPrice: Double = 0.0,
    val lastAlertKey: String = "",
    val lastAlertAt: Long = 0L,
    val criticalActive: Boolean = false,
    val status: String = "Ожидает ручную позицию"
) {
    fun toJson(): JSONObject = JSONObject()
        .put("entryTime", entryTime)
        .put("entryPrice", entryPrice)
        .put("peakPrice", peakPrice)
        .put("lastAlertKey", lastAlertKey)
        .put("lastAlertAt", lastAlertAt)
        .put("criticalActive", criticalActive)
        .put("status", status)

    companion object {
        fun fromJson(json: JSONObject) = PersonalPositionGuardState(
            entryTime = json.optLong("entryTime"),
            entryPrice = json.optDouble("entryPrice"),
            peakPrice = json.optDouble("peakPrice"),
            lastAlertKey = json.optString("lastAlertKey"),
            lastAlertAt = json.optLong("lastAlertAt"),
            criticalActive = json.optBoolean("criticalActive", false),
            status = RussianOutputPolicy.visible(json.optString("status", "Ожидает ручную позицию"))
        )
    }
}

data class PersonalPositionGuardOutcome(
    val state: PersonalPositionGuardState,
    val forceCriticalAi: Boolean = false,
    val alertReason: String = ""
)

internal object PersonalPositionGuardPolicy {
    private const val HARD_STOP_PERCENT = -4.4
    private const val ALERT_COOLDOWN_MILLIS = 10L * 60L * 1000L

    fun evaluate(
        state: PersonalPositionGuardState,
        snapshot: LiveSnapshot,
        micro: MicroImpulseSnapshot,
        price: Double,
        now: Long
    ): PersonalPositionGuardOutcome {
        if (snapshot.waitMode != "SELL" || snapshot.entryPrice <= 0.0 || price <= 0.0) {
            return PersonalPositionGuardOutcome(PersonalPositionGuardState())
        }
        val base = if (state.entryTime != snapshot.entryTime || state.entryPrice != snapshot.entryPrice) {
            PersonalPositionGuardState(snapshot.entryTime, snapshot.entryPrice, snapshot.entryPrice)
        } else state
        val peak = max(max(base.peakPrice, snapshot.entryPrice), price)
        val pnl = (price / snapshot.entryPrice - 1.0) * 100.0
        val pullback = (1.0 - price / peak) * 100.0
        val microFresh = micro.connected && DeepSeekFreshMarketContext.isFresh(
            micro.updatedAt,
            now,
            DeepSeekFreshMarketContext.MICRO_MAX_AGE
        )
        val moneyPressure = FastMoneyPressurePolicy.evaluate(micro)
        val microWeak = microFresh && !moneyPressure.absorptionPossible && (
            moneyPressure.heavySelling ||
                (micro.aggressiveBuyPercent15s <= 45.0 && micro.aggressiveBuyPercent60s <= 48.0 &&
                    micro.priceChange60sPercent <= -0.12) ||
                ((micro.topBookImbalance ?: 0.0) <= -0.15 && micro.priceChange60sPercent <= -0.20)
            )
        val broadWeak = snapshot.directionScore <= -35
        val strongLiveRecovery = microFresh &&
            micro.aggressiveBuyPercent15s >= 60.0 &&
            micro.aggressiveBuyPercent60s >= 58.0 &&
            micro.aggressiveBuyPercent5m >= 54.0 &&
            micro.priceChange60sPercent >= 0.08
        val reason = when {
            pnl <= HARD_STOP_PERCENT -> "ЖИВОЙ СТОП: результат ${format(pnl)}%, достигнут защитный предел −4,4%"
            snapshot.rapidDrop.active && !snapshot.rapidDrop.recoveryConfirmed && !strongLiveRecovery ->
                "РЕЗКОЕ ПАДЕНИЕ: рынок потерял ${format(-snapshot.rapidDrop.dropPercent)}% от локального пика"
            snapshot.sellSignal -> "ЛОКАЛЬНЫЙ ВЫХОД APP подтверждён закрытой свечой: ${snapshot.signalReason}"
            pnl >= 4.0 && pullback >= 1.2 && microWeak && broadWeak ->
                "ЗАЩИТА ПРИБЫЛИ: прибыль ${format(pnl)}%, откат ${format(pullback)}% подтверждён потоком и рынком"
            pnl >= 2.0 && pullback >= 1.6 && microWeak && snapshot.directionScore <= -20 ->
                "ВОЗМОЖНЫЙ ВЕРХ: откат ${format(pullback)}% после прибыли, микропоток развернулся"
            else -> ""
        }
        val key = reason.substringBefore(':')
        val rearmedKey = if (reason.isBlank() && base.lastAlertAt > 0L &&
            now - base.lastAlertAt >= ALERT_COOLDOWN_MILLIS
        ) "" else base.lastAlertKey
        // A persistent unchanged condition updates the card but never rings periodically.
        // It can alert again only after a real recovery interval re-arms the guard.
        val canAlert = reason.isNotBlank() && key != rearmedKey
        val status = if (reason.isBlank()) {
            "Живой контроль: ${format(pnl)}%, пик €${String.format(java.util.Locale.GERMANY, "%.8f", peak)}, откат ${format(pullback)}%"
        } else reason
        val updated = base.copy(
            peakPrice = peak,
            lastAlertKey = if (canAlert) key else rearmedKey,
            lastAlertAt = if (canAlert) now else base.lastAlertAt,
            criticalActive = reason.startsWith("ЖИВОЙ СТОП") ||
                reason.startsWith("РЕЗКОЕ ПАДЕНИЕ") ||
                reason.startsWith("ЗАЩИТА ПРИБЫЛИ") ||
                reason.startsWith("ВОЗМОЖНЫЙ ВЕРХ"),
            status = status
        )
        return PersonalPositionGuardOutcome(
            state = updated,
            forceCriticalAi = canAlert,
            alertReason = reason.takeIf { canAlert }.orEmpty()
        )
    }

    private fun format(value: Double): String =
        String.format(java.util.Locale.GERMANY, "%+.2f", value)
}

object PersonalPositionGuardStore {
    private const val PREFS = "personal_position_guard_v49"
    private const val KEY_STATE = "state"
    private const val KEY_BACKUP = "state_backup"

    fun open(context: Context, price: Double, at: Long) {
        save(context, PersonalPositionGuardState(at, price, price, status = "Позиция открыта, живой контроль запущен"))
    }

    fun state(context: Context): PersonalPositionGuardState {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        for (key in listOf(KEY_STATE, KEY_BACKUP)) {
            val raw = p.getString(key, null)
            if (raw.isNullOrBlank()) continue
            runCatching {
                PersonalPositionGuardState.fromJson(JSONObject(raw))
            }.getOrNull()?.let { return it }
        }
        return PersonalPositionGuardState()
    }

    @Synchronized
    fun sync(
        context: Context,
        snapshot: LiveSnapshot = PumpBotEngine.snapshot(context),
        now: Long = System.currentTimeMillis()
    ): PersonalPositionGuardOutcome {
        if (snapshot.waitMode != "SELL" || snapshot.entryPrice <= 0.0) {
            clear(context)
            return PersonalPositionGuardOutcome(PersonalPositionGuardState())
        }
        val price = PaperExecutionPolicy.freshLivePrice(snapshot, now)
            ?: return PersonalPositionGuardOutcome(
                state(context).copy(status = "Живой контроль ослаблен: свежая цена недоступна")
                    .also { save(context, it) }
            )
        val previous = state(context)
        val outcome = PersonalPositionGuardPolicy.evaluate(
            previous, snapshot, MicroImpulseStore.state(context), price, now
        )
        if (outcome.alertReason.isBlank()) {
            save(context, outcome.state)
            return outcome
        }
        val delivered = runCatching {
            PumpAlert.showPersonalPositionGuard(context, outcome.alertReason)
        }.isSuccess
        if (delivered) {
            save(context, outcome.state)
            return outcome
        }
        val retryState = outcome.state.copy(
            lastAlertKey = previous.lastAlertKey,
            lastAlertAt = previous.lastAlertAt
        )
        save(context, retryState)
        return outcome.copy(state = retryState, forceCriticalAi = true)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun save(context: Context, state: PersonalPositionGuardState) {
        val raw = state.toJson().toString()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_STATE, raw)
            .putString(KEY_BACKUP, raw)
            .commit()
    }
}
