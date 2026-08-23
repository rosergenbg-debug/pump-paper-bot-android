from pathlib import Path


def read(path: str) -> str:
    return Path(path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    Path(path).write_text(text, encoding="utf-8")


def replace_once(path: str, old: str, new: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one anchor, found {count}: {old[:180]!r}")
    write(path, text.replace(old, new, 1))


# ---------------------------------------------------------------------------
# V5.26: profit-first PM2/PM3 execution experiment.
#
# This deliberately does NOT copy the Gemini proposal literally. The live app already has
# a causal 15-second observer, executable Bitpanda bid/ask accounting and BuyerBreath phases.
# We therefore use those existing causal signals instead of adding a second slow RSI/EMA loop.
# Fusion/DeepSeek behavior is left untouched; only PM2 and PM3 get the fast entry/exit policy.
# Existing preference stores are preserved exactly, so account histories survive the update.
# ---------------------------------------------------------------------------

shared = "app/src/main/java/com/example/pumppaperbot/SharedFusionEntryPolicy.kt"
replace_once(
    shared,
    '''data class SharedFusionEntryObservation(
    val frame: FusionFlowFrame?,
    val shockReady: Boolean,
    val sampledAt: Long,
    val sampleBucket: Long
)
''',
    '''data class SharedFusionEntryObservation(
    val frame: FusionFlowFrame?,
    val shockReady: Boolean,
    val sampledAt: Long,
    val sampleBucket: Long,
    val breathing: LiveMarketBreathingSnapshot? = null
)
'''
)
replace_once(
    shared,
    '''        return SharedFusionEntryObservation(
            frame = FusionFlowPolicy.frame(breathing),
            shockReady = shock.fresh(now) && shock.ready,
            sampledAt = now,
            sampleBucket = bucket
        ).also {
''',
    '''        return SharedFusionEntryObservation(
            frame = FusionFlowPolicy.frame(breathing),
            shockReady = shock.fresh(now) && shock.ready,
            sampledAt = now,
            sampleBucket = bucket,
            breathing = breathing
        ).also {
'''
)

engine_path = "app/src/main/java/com/example/pumppaperbot/PumpProfitEngineV526.kt"
engine_source = r'''package com.example.pumppaperbot

import kotlin.math.max

enum class PumpProfitModeV526 { PUMP_2, PUMP_3 }

data class PumpProfitPositionDecisionV526(
    val action: String?,
    val nextState: FusionStabilityState,
    val reason: String?,
    val tradeNetPercent: Double,
    val peakNetPercent: Double
)

/**
 * V5.26 PM-only execution engine.
 *
 * Design goals:
 *  - enter while buyer breath is IGNITION/EXPANSION, not after every 15/30m bar turns green;
 *  - reject mature/exhausted/absorbed pumps instead of chasing taker-buy at the top;
 *  - use the existing 15-second causal observer, never an LLM response, in the execution path;
 *  - measure all exits in true simulated NET PnL after buy fee, sell fee and executable bid/ask;
 *  - prevent dead positions with breakeven, early adverse-flow exits and bounded hold time.
 *
 * This is an experiment intended to improve expectancy. It is not a profit guarantee.
 */
object PumpProfitEngineV526 {
    private data class Config(
        val name: String,
        val takeProfitNet: Double,
        val hardStopNet: Double,
        val breakevenTriggerNet: Double,
        val breakevenLockNet: Double,
        val earlyAdverseNet: Double,
        val givebackArmNet: Double,
        val maxGivebackNet: Double,
        val softHoldMillis: Long,
        val hardHoldMillis: Long,
        val timeoutKeepNet: Double,
        val minInstant: Int,
        val min5m: Int,
        val min15m: Int,
        val min30m: Int,
        val minBuyer5m: Double,
        val minActivityRatio: Double,
        val maxAbsorptionRisk: Int,
        val minEfficiency: Int,
        val maxEarlyMovePercent: Double,
        val confirmationMillis: Long
    )

    private val PM2 = Config(
        name = "PM2",
        takeProfitNet = 2.00,
        hardStopNet = -1.10,
        breakevenTriggerNet = 0.85,
        breakevenLockNet = 0.10,
        earlyAdverseNet = -0.45,
        givebackArmNet = 1.25,
        maxGivebackNet = 0.55,
        softHoldMillis = 20L * 60L * 1000L,
        hardHoldMillis = 30L * 60L * 1000L,
        timeoutKeepNet = 0.30,
        minInstant = 8,
        min5m = 2,
        min15m = -4,
        min30m = -8,
        minBuyer5m = 56.0,
        minActivityRatio = 1.05,
        maxAbsorptionRisk = 62,
        minEfficiency = -25,
        maxEarlyMovePercent = 1.45,
        confirmationMillis = 12_000L
    )

    private val PM3 = Config(
        name = "PM3",
        takeProfitNet = 3.00,
        hardStopNet = -1.30,
        breakevenTriggerNet = 1.25,
        breakevenLockNet = 0.15,
        earlyAdverseNet = -0.55,
        givebackArmNet = 2.00,
        maxGivebackNet = 0.75,
        softHoldMillis = 35L * 60L * 1000L,
        hardHoldMillis = 50L * 60L * 1000L,
        timeoutKeepNet = 0.45,
        minInstant = 10,
        min5m = 4,
        min15m = -1,
        min30m = -5,
        minBuyer5m = 59.0,
        minActivityRatio = 1.10,
        maxAbsorptionRisk = 58,
        minEfficiency = -15,
        maxEarlyMovePercent = 1.60,
        confirmationMillis = 15_000L
    )

    private fun cfg(mode: PumpProfitModeV526): Config = if (mode == PumpProfitModeV526.PUMP_2) PM2 else PM3

    private fun resetEntry(previous: FusionStabilityState, keepCooldown: Boolean = true) = previous.copy(
        entryStreak = 0,
        entryCandidateAt = 0L,
        exitStreak = 0,
        exitArmedAt = 0L,
        exitArmedBid = 0.0,
        peakBid = 0.0,
        profitDefenseArmed = false,
        cooldownUntil = if (keepCooldown) previous.cooldownUntil else 0L
    )

    fun isFastCandidate(mode: PumpProfitModeV526, observation: SharedFusionEntryObservation): Boolean {
        if (observation.shockReady) return shockPermitted(observation)
        return entryGate(mode, observation).first
    }

    fun evaluateEntry(
        mode: PumpProfitModeV526,
        previous: FusionStabilityState,
        observation: SharedFusionEntryObservation,
        now: Long
    ): SharedFusionEntryDecision {
        val c = cfg(mode)
        if (previous.cooldownUntil > now) {
            val left = ((previous.cooldownUntil - now + 999L) / 1000L).coerceAtLeast(1L)
            return SharedFusionEntryDecision(null, resetEntry(previous), "V526 ${c.name} COOLDOWN: ещё ${left}с")
        }

        if (observation.shockReady) {
            return if (shockPermitted(observation)) {
                SharedFusionEntryDecision(
                    "BUY",
                    resetEntry(previous, keepCooldown = false),
                    "V526_${c.name}_SHOCK_ENTRY: быстрый rebound подтверждён, перегрев/поглощение не блокируют вход"
                )
            } else {
                SharedFusionEntryDecision(
                    null,
                    resetEntry(previous, keepCooldown = false),
                    "V526_${c.name}_NO_FOMO: shock rebound есть, но поток уже выглядит поглощённым/поздним"
                )
            }
        }

        val (candidate, reason) = entryGate(mode, observation)
        if (!candidate) {
            return SharedFusionEntryDecision(null, resetEntry(previous, keepCooldown = false), reason)
        }

        val candidateAt = if (previous.entryStreak > 0 && previous.entryCandidateAt > 0L) {
            previous.entryCandidateAt
        } else now
        val streak = (previous.entryStreak + 1).coerceAtMost(2)
        val next = previous.copy(
            entryStreak = streak,
            entryCandidateAt = candidateAt,
            exitStreak = 0,
            exitArmedAt = 0L,
            exitArmedBid = 0.0,
            peakBid = 0.0,
            profitDefenseArmed = false,
            cooldownUntil = 0L
        )
        val elapsed = (now - candidateAt).coerceAtLeast(0L)
        return if (streak >= 2 && elapsed >= c.confirmationMillis) {
            SharedFusionEntryDecision(
                "BUY",
                next,
                "V526_${c.name}_EARLY_ENTRY: ранний импульс подтверждён двумя 15с наблюдениями; $reason"
            )
        } else {
            val left = ((c.confirmationMillis - elapsed).coerceAtLeast(0L) + 999L) / 1000L
            SharedFusionEntryDecision(
                null,
                next,
                "V526_${c.name}_ARMED ${streak}/2: ранний импульс есть; защита от одиночного тика ещё ${left}с; $reason"
            )
        }
    }

    private fun entryGate(
        mode: PumpProfitModeV526,
        observation: SharedFusionEntryObservation
    ): Pair<Boolean, String> {
        val c = cfg(mode)
        val breathing = observation.breathing
            ?: return false to "V526_${c.name}_WAIT: нет live breathing snapshot"
        val frame = observation.frame
            ?: return false to "V526_${c.name}_WAIT: быстрый flow frame ещё не готов"
        if (!breathing.fresh) return false to "V526_${c.name}_WAIT: live flow устарел"

        val breath = breathing.buyerBreath
        val phase = breath.phase
        if (phase != BuyerBreathPhase.IGNITION && phase != BuyerBreathPhase.EXPANSION) {
            return false to "V526_${c.name}_NO_FOMO: фаза $phase, вход разрешён только IGNITION/EXPANSION"
        }

        val buyer5 = breath.buyerPercent5m
            ?: breathing.horizons.firstOrNull { it.minutes == 5 }?.buyerPercent
            ?: 50.0
        val activity = breath.activityRatio
        val efficiency = breath.efficiencyScore ?: 0
        val move = max(
            0.0,
            max(
                breath.moveSincePhaseStartPercent ?: 0.0,
                breath.priceChange5mPercent ?: 0.0
            )
        )

        if (move > c.maxEarlyMovePercent) {
            return false to "V526_${c.name}_NO_FOMO: движение уже ${fmt(move)}% > ${fmt(c.maxEarlyMovePercent)}%"
        }
        if (breath.absorptionRisk > c.maxAbsorptionRisk) {
            return false to "V526_${c.name}_ABSORPTION: риск ${breath.absorptionRisk}/100"
        }
        if (efficiency < c.minEfficiency) {
            return false to "V526_${c.name}_ABSORPTION: эффективность цены $efficiency слишком слабая"
        }
        if (buyer5 < c.minBuyer5m) {
            return false to "V526_${c.name}_WAIT: buyer5=${fmt(buyer5)}% < ${fmt(c.minBuyer5m)}%"
        }
        if (activity != null && activity < c.minActivityRatio) {
            return false to "V526_${c.name}_WAIT: активность ${fmt(activity)}x < ${fmt(c.minActivityRatio)}x"
        }
        if (frame.instant < c.minInstant || frame.score5m < c.min5m) {
            return false to "V526_${c.name}_WAIT: instant/5m ${frame.instant}/${frame.score5m} ещё недостаточны"
        }
        if (frame.score15m < c.min15m || frame.score30m < c.min30m) {
            return false to "V526_${c.name}_WAIT: старший поток ещё падает слишком быстро (${frame.score15m}/${frame.score30m})"
        }

        return true to "phase=$phase instant/5/15/30=${frame.instant}/${frame.score5m}/${frame.score15m}/${frame.score30m}, buyer5=${fmt(buyer5)}%, move=${fmt(move)}%, absorption=${breath.absorptionRisk}"
    }

    private fun shockPermitted(observation: SharedFusionEntryObservation): Boolean {
        val frame = observation.frame
        val breath = observation.breathing?.buyerBreath
        if (frame != null && frame.instant < 4) return false
        if (breath == null) return true
        if (!breath.fresh) return false
        if (breath.phase == BuyerBreathPhase.SELLER_TAKEOVER || breath.phase == BuyerBreathPhase.EXHAUSTION) return false
        if (breath.absorptionRisk >= 72) return false
        return true
    }

    fun evaluatePosition(
        mode: PumpProfitModeV526,
        portfolio: FusionSimPortfolio,
        previous: FusionStabilityState,
        observation: SharedFusionEntryObservation?,
        bid: Double,
        feeRate: Double,
        positionAgeMillis: Long
    ): PumpProfitPositionDecisionV526 {
        val c = cfg(mode)
        val tradeNet = tradeNetPercent(portfolio, bid, feeRate)
        val peakBid = max(max(previous.peakBid, bid), portfolio.entryPrice)
        val peakNet = tradeNetPercent(portfolio, peakBid, feeRate)
        val armed = previous.profitDefenseArmed || peakNet >= c.breakevenTriggerNet
        val next = previous.copy(
            entryStreak = 0,
            entryCandidateAt = 0L,
            peakBid = peakBid,
            profitDefenseArmed = armed,
            cooldownUntil = 0L
        )

        fun exit(reason: String) = PumpProfitPositionDecisionV526(
            "EXIT", next, reason, tradeNet, peakNet
        )

        if (tradeNet >= c.takeProfitNet) {
            return exit("V526_TAKE_PROFIT_${c.name}: ${fmtSigned(tradeNet)}% NET; цель ${fmt(c.takeProfitNet)}% NET выполнена")
        }
        if (tradeNet <= c.hardStopNet) {
            return exit("V526_HARD_STOP_${c.name}: ${fmtSigned(tradeNet)}% NET; лимит ${fmtSigned(c.hardStopNet)}%")
        }
        if (armed && tradeNet <= c.breakevenLockNet) {
            return exit("V526_BREAKEVEN_${c.name}: пик ${fmtSigned(peakNet)}% NET; защищаем не менее ${fmtSigned(c.breakevenLockNet)}% NET")
        }

        val frame = observation?.frame
        val breath = observation?.breathing?.buyerBreath
        val deterioration = frame?.deteriorationSignal == true ||
            breath?.phase == BuyerBreathPhase.SELLER_TAKEOVER ||
            ((breath?.absorptionRisk ?: 0) >= 85 && (frame?.instant ?: 0) < 0)
        if (positionAgeMillis >= 90_000L && tradeNet <= c.earlyAdverseNet && deterioration) {
            return exit("V526_EARLY_RISK_EXIT_${c.name}: ${fmtSigned(tradeNet)}% NET и быстрый поток ухудшился; не ждём полного стопа")
        }

        val giveback = peakNet - tradeNet
        if (peakNet >= c.givebackArmNet && giveback >= c.maxGivebackNet && tradeNet > c.breakevenLockNet) {
            return exit("V526_PROFIT_GIVEBACK_${c.name}: пик ${fmtSigned(peakNet)}%, откат ${fmt(giveback)} п.п.; фиксируем ${fmtSigned(tradeNet)}% NET")
        }

        val constructive = frame != null &&
            frame.instant >= 0 && frame.score5m >= 0 && frame.score15m >= -2 &&
            breath?.phase != BuyerBreathPhase.EXHAUSTION &&
            breath?.phase != BuyerBreathPhase.SELLER_TAKEOVER
        if (positionAgeMillis >= c.hardHoldMillis) {
            return exit("V526_HARD_TIMEOUT_${c.name}: позиция живёт ${(positionAgeMillis / 60_000L)} мин; освобождаем капитал")
        }
        if (positionAgeMillis >= c.softHoldMillis && (tradeNet < c.timeoutKeepNet || !constructive)) {
            return exit("V526_TIMEOUT_${c.name}: ${(positionAgeMillis / 60_000L)} мин без достаточного продолжения; NET ${fmtSigned(tradeNet)}%")
        }

        val stateReason = if (armed) {
            "V526_${c.name}_HOLD: BE armed; peak=${fmtSigned(peakNet)}% net, now=${fmtSigned(tradeNet)}% net"
        } else {
            "V526_${c.name}_HOLD: peak=${fmtSigned(peakNet)}% net, now=${fmtSigned(tradeNet)}% net"
        }
        return PumpProfitPositionDecisionV526(null, next, stateReason, tradeNet, peakNet)
    }

    private fun tradeNetPercent(portfolio: FusionSimPortfolio, bid: Double, feeRate: Double): Double {
        if (!portfolio.inPosition || portfolio.entryCostEur <= 0.0 || bid <= 0.0) return 0.0
        val fee = feeRate.coerceIn(0.0, 0.02)
        val netExit = portfolio.pumpAmount * bid * (1.0 - fee)
        return (netExit / portfolio.entryCostEur - 1.0) * 100.0
    }

    private fun fmt(value: Double): String = String.format(java.util.Locale.GERMANY, "%.2f", value)
    private fun fmtSigned(value: Double): String = String.format(java.util.Locale.GERMANY, "%+.2f", value)
}
'''
write(engine_path, engine_source)


def patch_machine(path: str, decision_type: str, mode: str, tp_text: str, sl_text: str) -> None:
    replace_once(
        path,
        f'''            val shared = SharedFusionEntryPolicy.evaluate(previous, observation, now)\n            return {decision_type}(shared.action, shared.nextState, shared.reason, 0.0)\n''',
        f'''            val shared = PumpProfitEngineV526.evaluateEntry(\n                PumpProfitModeV526.{mode}, previous, observation, now\n            )\n            return {decision_type}(shared.action, shared.nextState, shared.reason, 0.0)\n'''
    )
    old_position = '''        val tradeNet = tradeNetPercent(portfolio, bid, feeRate)
        val peak = max(max(previous.peakBid, bid), portfolio.entryPrice)
        val base = previous.copy(
            entryStreak = 0,
            entryCandidateAt = 0L,
            peakBid = peak,
            // Pump Machine deliberately has no Fusion trailing/profit-defense.
            // Its hard risk limits are the net +3.00 / -1.50 contract below.
            profitDefenseArmed = false,
            cooldownUntil = 0L
        )
'''
    if mode == "PUMP_2":
        old_position = old_position.replace("Pump Machine deliberately", "Pump Machine 2 deliberately")
        old_position = old_position.replace("+3.00 / -1.50", "+2.00 / -1.50")
    new_position = f'''        val tradeNet = tradeNetPercent(portfolio, bid, feeRate)
        val v526 = PumpProfitEngineV526.evaluatePosition(
            mode = PumpProfitModeV526.{mode},
            portfolio = portfolio,
            previous = previous,
            observation = entryObservation,
            bid = bid,
            feeRate = feeRate,
            positionAgeMillis = positionAgeMillis
        )
        val peak = v526.nextState.peakBid
        val base = v526.nextState
        if (v526.action == "EXIT") {{
            return {decision_type}(
                "EXIT", base, v526.reason ?: "V526 risk exit", tradeNet
            )
        }}
'''
    replace_once(path, old_position, new_position)

    # Treat V5.26 risk exits as protective for the existing anti-churn cooldown.
    replace_once(
        path,
        '                    val protectiveStop = plan.reason.startsWith("STOP_LOSS_1_5_NET")\n',
        '                    val protectiveStop = plan.reason.startsWith("STOP_LOSS_1_5_NET") ||\n                        plan.reason.startsWith("V526_HARD_STOP") ||\n                        plan.reason.startsWith("V526_EARLY_RISK_EXIT")\n'
    )

    # Visible execution contract. NET values already include both fees and executable spread.
    old_buy_status = f'''                    val status = "BUY: ${{plan.reason}} • TP {tp_text} net • SL −1,50% net"\n'''
    new_buy_status = f'''                    val status = "BUY V5.26: ${{plan.reason}} • TP {tp_text} net • SL {sl_text} net • BE/timeout active"\n'''
    replace_once(path, old_buy_status, new_buy_status)


patch_machine(
    "app/src/main/java/com/example/pumppaperbot/PumpMachine.kt",
    "PumpMachineDecision",
    "PUMP_3",
    "+3,00%",
    "−1,30%"
)
patch_machine(
    "app/src/main/java/com/example/pumppaperbot/PumpMachine2.kt",
    "PumpMachine2Decision",
    "PUMP_2",
    "+2,00%",
    "−1,10%"
)

# ---------------------------------------------------------------------------
# The MicroImpulse websocket already invokes this callback about every 15 seconds.
# V5.26 lets it evaluate PM candidates locally; DeepSeek/LLM is not called here.
# Bitpanda execution quotes are refreshed only when a PM candidate/position actually exists.
# ---------------------------------------------------------------------------
service = "app/src/main/java/com/example/pumppaperbot/PumpSignalService.kt"
replace_once(
    service,
    '''                val pumpMachineFast = PumpMachineStore.state(this)
                val pumpMachine2Fast = PumpMachine2Store.state(this)
                if (pumpMachineFast.inPosition || pumpMachine2Fast.inPosition) {
''',
    '''                val pumpMachineFast = PumpMachineStore.state(this)
                val pumpMachine2Fast = PumpMachine2Store.state(this)
                val entryObservationFast = SharedFusionEntryObservationStore.snapshot(this, now)
                val pm3FastCandidate = !pumpMachineFast.inPosition &&
                    PumpProfitEngineV526.isFastCandidate(PumpProfitModeV526.PUMP_3, entryObservationFast)
                val pm2FastCandidate = !pumpMachine2Fast.inPosition &&
                    PumpProfitEngineV526.isFastCandidate(PumpProfitModeV526.PUMP_2, entryObservationFast)
                if (pumpMachineFast.inPosition || pumpMachine2Fast.inPosition || pm3FastCandidate || pm2FastCandidate) {
'''
)
replace_once(
    service,
    '''                    if (!venue.fresh(now) || now - venue.lastSuccess >= 30_000L) {
''',
    '''                    if (!venue.fresh(now) || now - venue.lastSuccess >= 15_000L) {
'''
)
replace_once(
    service,
    '''                    if (pumpMachineFast.inPosition) PumpMachineStore.sync(this, fastNow)
                    if (pumpMachine2Fast.inPosition) PumpMachine2Store.sync(this, fastNow)
''',
    '''                    if (pumpMachineFast.inPosition || pm3FastCandidate) PumpMachineStore.sync(this, fastNow)
                    if (pumpMachine2Fast.inPosition || pm2FastCandidate) PumpMachine2Store.sync(this, fastNow)
'''
)

# UI labels: make the changed risk contracts explicit without resetting any account.
activity = "app/src/main/java/com/example/pumppaperbot/CompetitionActivity.kt"
replace_once(activity,
             '            "PUMP 3% NET • TP +3% / SL −1,5%",\n',
             '            "PUMP 3% NET • V5.26 FAST • TP +3% / SL −1,3%",\n')
replace_once(activity,
             '            "PUMP 2% NET • TP +2% / SL −1,5%",\n',
             '            "PUMP 2% NET • V5.26 FAST • TP +2% / SL −1,1%",\n')

# Sequential installable version. Package and all existing preference keys remain unchanged.
gradle = "app/build.gradle"
replace_once(gradle, "        versionCode 105\n", "        versionCode 106\n")
replace_once(gradle, '        versionName "5.25"\n', '        versionName "5.26"\n')

print("V5.26 profit engine applied: fast PM2/PM3 entry, anti-FOMO, BE, tighter risk and bounded hold; histories preserved")
