from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}: {old[:120]!r}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


def insert_before_once(path: str, anchor: str, insertion: str) -> None:
    replace_once(path, anchor, insertion + anchor)


# ---------------------------------------------------------------------------
# A) MicroImpulse is already a 15-second observer. Add a 3-minute local
#    peak-to-low context and feed the fast shock/rebound state machine.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/example/pumppaperbot/MicroImpulseStream.kt"
replace_once(
    path,
    "class MicroImpulseStream(context: Context) : WebSocketListener() {",
    "class MicroImpulseStream(\n    context: Context,\n    private val onUrgentMarketEvent: (() -> Unit)? = null\n) : WebSocketListener() {"
)
replace_once(
    path,
    '''        val fiveMinutes = trades.toList()
        val fifteenMinuteKey = now / 60_000L - 14L
''',
    '''        val fiveMinutes = trades.toList()
        val threeMinutes = fiveMinutes.filter { it.at >= now - 3L * 60L * 1000L }
        val old15Price = fifteen.firstOrNull()?.price ?: currentPrice
        val change15 = if (old15Price > 0.0) (currentPrice / old15Price - 1.0) * 100.0 else 0.0
        var runningPeak3m = threeMinutes.firstOrNull()?.price ?: currentPrice
        var maxDrawdown3m = 0.0
        var lowAtMaxDrawdown3m = currentPrice
        threeMinutes.forEach { trade ->
            if (trade.price > runningPeak3m) runningPeak3m = trade.price
            if (runningPeak3m > 0.0) {
                val drawdown = (1.0 - trade.price / runningPeak3m) * 100.0
                if (drawdown > maxDrawdown3m) {
                    maxDrawdown3m = drawdown
                    lowAtMaxDrawdown3m = trade.price
                }
            }
        }
        val rebound3m = if (lowAtMaxDrawdown3m > 0.0 && currentPrice >= lowAtMaxDrawdown3m) {
            (currentPrice / lowAtMaxDrawdown3m - 1.0) * 100.0
        } else 0.0
        val fifteenMinuteKey = now / 60_000L - 14L
'''
)
replace_once(
    path,
    '''        val buy15m = fifteenMinuteBuckets.sumOf { it.buyUsdt }
        val sell15m = fifteenMinuteBuckets.sumOf { it.sellUsdt }
        val buyRatio5 = ratio(buy5, sell5)
''',
    '''        val buy15m = fifteenMinuteBuckets.sumOf { it.buyUsdt }
        val sell15m = fifteenMinuteBuckets.sumOf { it.sellUsdt }
        val turnover60 = buy60 + sell60
        val turnover5m = buy5m + sell5m
        val moneyActivityRatio = if (connectedAt > 0L && now - connectedAt >= 4L * 60L * 1000L && turnover5m > 0.0) {
            turnover60 / (turnover5m / 5.0)
        } else null
        val buyRatio5 = ratio(buy5, sell5)
'''
)
replace_once(
    path,
    '''        MicroImpulseStore.save(appContext, snapshot)
        LiveMarketBreathingStore.append(appContext, snapshot)
''',
    '''        MicroImpulseStore.save(appContext, snapshot)
        LiveMarketBreathingStore.append(appContext, snapshot)
        ShockReboundStore.observe(
            appContext,
            ShockReboundObservation(
                at = now,
                price = currentPrice,
                drawdown3mPercent = maxDrawdown3m,
                rebound3mPercent = rebound3m,
                change15sPercent = change15,
                change60sPercent = change60,
                buyer5sPercent = buyRatio5 * 100.0,
                buyer15sPercent = buyRatio15 * 100.0,
                buyer60sPercent = buyRatio60 * 100.0,
                tradeAcceleration = tradeAcceleration,
                moneyActivityRatio = moneyActivityRatio,
                bookImbalance = bookImbalance
            )
        )
        // Lightweight callback only. No DeepSeek request is made here; the service may refresh
        // the read-only Bitpanda book only after a real shock/rebound needs execution checking.
        onUrgentMarketEvent?.invoke()
'''
)


# ---------------------------------------------------------------------------
# B) Foreground service: consume 15-second shock observations on a dedicated
#    lightweight executor. Full AI/market cycles remain at their existing cadence.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/example/pumppaperbot/PumpSignalService.kt"
replace_once(
    path,
    '''    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val market = MarketSyncClient()
''',
    '''    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val shockExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val shockCheckQueuedOrRunning = AtomicBoolean(false)
    private val market = MarketSyncClient()
'''
)
replace_once(
    path,
    '''        microImpulse = MicroImpulseStream(this)
''',
    '''        microImpulse = MicroImpulseStream(this) { requestFastShockCheck() }
'''
)
replace_once(
    path,
    '''        microImpulse.stop()
        executor.shutdownNow()
        super.onDestroy()
''',
    '''        microImpulse.stop()
        executor.shutdownNow()
        shockExecutor.shutdownNow()
        super.onDestroy()
'''
)
insert_before_once(
    path,
    '''    private fun checkNow() {
''',
    '''    private fun requestFastShockCheck() {
        if (!shockCheckQueuedOrRunning.compareAndSet(false, true)) return
        shockExecutor.execute {
            try {
                val now = System.currentTimeMillis()
                // When Serge owns a position, this produces only evidence-based exit warnings;
                // entry chatter and virtual-agent trade sounds are handled separately below.
                FastPositionWarningStore.sync(this, now)

                val shock = ShockReboundStore.state(this)
                if (!shock.active || !shock.fresh(now)) return@execute
                val fusion = FusionSimStore.state(this)
                if (!shock.ready && !fusion.inPosition) return@execute

                // A fast rebound cannot wait for the 1-3 minute full cycle. Refresh only the
                // read-only execution book and run the local paper engine; no AI call is made.
                BitpandaFusionClient().sync(this, force = true)
                FusionSimStore.sync(this, DeepSeekPrimaryStore.state(this), System.currentTimeMillis())
            } finally {
                shockCheckQueuedOrRunning.set(false)
            }
        }
    }

'''
)
replace_once(
    path,
    '''                val snapshot = PumpBotEngine.snapshot(this)
                val appTrade = CycleStageGuard.run(
''',
    '''                val snapshot = PumpBotEngine.snapshot(this)
                val userPositionOpen = snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0
                val appTrade = CycleStageGuard.run(
'''
)
replace_once(
    path,
    '''                    if (!rapidDropAlerted && !appTrade.tradeAlerted && PumpBotEngine.shouldAlert(this, snapshot)) {
''',
    '''                    if (!rapidDropAlerted && !appTrade.tradeAlerted &&
                        (!userPositionOpen || snapshot.sellSignal) && PumpBotEngine.shouldAlert(this, snapshot)
                    ) {
'''
)
replace_once(
    path,
    '''                if (!rapidDropAlerted && !appTrade.tradeAlerted && !signalAlerted &&
                    EventRadarStore.shouldAlert(this, eventState)
''',
    '''                if (!userPositionOpen && !rapidDropAlerted && !appTrade.tradeAlerted && !signalAlerted &&
                    EventRadarStore.shouldAlert(this, eventState)
'''
)


# Same notification routing for WorkManager fallback cycles.
path = "app/src/main/java/com/example/pumppaperbot/PumpBotWorker.kt"
replace_once(
    path,
    '''            val snapshot = PumpBotEngine.snapshot(applicationContext)
            val appTrade = CycleStageGuard.run(applicationContext, "APP_PAPER", {
''',
    '''            val snapshot = PumpBotEngine.snapshot(applicationContext)
            val userPositionOpen = snapshot.waitMode == "SELL" && snapshot.entryPrice > 0.0
            val appTrade = CycleStageGuard.run(applicationContext, "APP_PAPER", {
'''
)
replace_once(
    path,
    '''                if (!rapidDropAlerted && !appTrade.tradeAlerted && PumpBotEngine.shouldAlert(applicationContext, snapshot)) {
''',
    '''                if (!rapidDropAlerted && !appTrade.tradeAlerted &&
                    (!userPositionOpen || snapshot.sellSignal) && PumpBotEngine.shouldAlert(applicationContext, snapshot)
                ) {
'''
)
replace_once(
    path,
    '''            if (!rapidDropAlerted && !appTrade.tradeAlerted && !signalAlerted &&
                EventRadarStore.shouldAlert(applicationContext, eventState)
''',
    '''            if (!userPositionOpen && !rapidDropAlerted && !appTrade.tradeAlerted && !signalAlerted &&
                EventRadarStore.shouldAlert(applicationContext, eventState)
'''
)


# ---------------------------------------------------------------------------
# C) Fusion: let the confirmed 15-second rebound lane enter while normal
#    5/15/30 horizons are still red, but never bypass anti-churn cooldown.
#    A failed bounce exits quickly; ordinary Fusion remains unchanged.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/example/pumppaperbot/FusionSim.kt"
replace_once(
    path,
    '''    const val MIN_HOLD_MILLIS = 10L * 60L * 1000L
    const val EXIT_ARM_TTL_MILLIS = 8L * 60L * 1000L
''',
    '''    const val MIN_HOLD_MILLIS = 10L * 60L * 1000L
    const val SHOCK_MIN_HOLD_MILLIS = 2L * 60L * 1000L
    const val SHOCK_FAILURE_MIN_AGE_MILLIS = 15_000L
    const val EXIT_ARM_TTL_MILLIS = 8L * 60L * 1000L
'''
)
replace_once(
    path,
    '''        feeRate: Double,
        now: Long,
        positionAgeMillis: Long = Long.MAX_VALUE
    ): FusionStabilityDecision {
''',
    '''        feeRate: Double,
        now: Long,
        positionAgeMillis: Long = Long.MAX_VALUE,
        shockReady: Boolean = false,
        shockFailed: Boolean = false,
        shockEntry: Boolean = false
    ): FusionStabilityDecision {
'''
)
replace_once(
    path,
    '''            val buy = frame?.buySignal == true
            if (!buy) {
''',
    '''            if (shockReady) {
                return FusionStabilityDecision(
                    "BUY",
                    previous.copy(
                        entryStreak = 0,
                        entryCandidateAt = 0L,
                        exitStreak = 0,
                        exitArmedAt = 0L,
                        exitArmedBid = 0.0,
                        peakBid = 0.0,
                        profitDefenseArmed = false,
                        cooldownUntil = 0L
                    ),
                    0.0,
                    "SHOCK_REBOUND_ENTRY: локальный 3-минутный провал остановлен; два 15-секундных кадра подтвердили возврат покупателей и цены"
                )
            }

            val buy = frame?.buySignal == true
            if (!buy) {
'''
)
replace_once(
    path,
    '''        val basePositionState = previous.copy(
            entryStreak = 0,
            entryCandidateAt = 0L,
            peakBid = peak,
            profitDefenseArmed = defenseArmed,
            cooldownUntil = 0L
        )

        if (activeStop > 0.0 && bid <= activeStop) {
''',
    '''        val basePositionState = previous.copy(
            entryStreak = 0,
            entryCandidateAt = 0L,
            peakBid = peak,
            profitDefenseArmed = defenseArmed,
            cooldownUntil = 0L
        )

        if (shockEntry && shockFailed && positionAgeMillis >= SHOCK_FAILURE_MIN_AGE_MILLIS) {
            return FusionStabilityDecision(
                "EXIT", basePositionState, activeStop,
                "SHOCK_REBOUND_FAILED: быстрый отскок после провала сорвался; продавцы вернули контроль, paper-позицию закрываем без ожидания медленных горизонтов"
            )
        }

        if (activeStop > 0.0 && bid <= activeStop) {
'''
)
replace_once(
    path,
    '''        val exitConfirmed = severeExit || streak >= 2
        val holdLockActive = positionAgeMillis < MIN_HOLD_MILLIS

        if (armed && actualDecline && exitConfirmed && (!holdLockActive || severeExit)) {
''',
    '''        val exitConfirmed = severeExit || streak >= 2
        val holdLimit = if (shockEntry) SHOCK_MIN_HOLD_MILLIS else MIN_HOLD_MILLIS
        val holdLockActive = positionAgeMillis < holdLimit

        if (armed && actualDecline && exitConfirmed && (!holdLockActive || severeExit)) {
'''
)
replace_once(
    path,
    '''            val left = ((MIN_HOLD_MILLIS - positionAgeMillis).coerceAtLeast(0L) / 1000L)
''',
    '''            val left = ((holdLimit - positionAgeMillis).coerceAtLeast(0L) / 1000L)
'''
)
replace_once(
    path,
    '''        val plan = FusionStabilityPolicy.evaluate(
            inPosition = tracked.inPosition,
            entryPrice = tracked.entryPrice,
            previous = previousStability,
            frame = frame,
            bid = market.bid,
            feeRate = market.feeRate,
            now = now,
            positionAgeMillis = positionAgeMillis
        )
''',
    '''        val shock = ShockReboundStore.state(context)
        val shockFresh = shock.fresh(now)
        val lastBuyReason = tracked.trades.asReversed().firstOrNull { it.action == "BUY" }?.reason.orEmpty()
        val shockEntry = tracked.inPosition && lastBuyReason.contains("SHOCK_REBOUND_ENTRY")
        val plan = FusionStabilityPolicy.evaluate(
            inPosition = tracked.inPosition,
            entryPrice = tracked.entryPrice,
            previous = previousStability,
            frame = frame,
            bid = market.bid,
            feeRate = market.feeRate,
            now = now,
            positionAgeMillis = positionAgeMillis,
            shockReady = !tracked.inPosition && shockFresh && shock.ready,
            shockFailed = shockFresh && shock.failed,
            shockEntry = shockEntry
        )
'''
)
replace_once(
    path,
    '''                    "Виртуальный BUY исполнен по ask; комиссия 0,25% учтена; V5.16 подтверждает вход во времени и не ловит один зелёный тик"
''',
    '''                    if (reason.contains("SHOCK_REBOUND_ENTRY")) {
                        "Виртуальный BUY исполнен по ask после подтверждённого быстрого отскока; комиссия 0,25% учтена; реальных ордеров нет"
                    } else {
                        "Виртуальный BUY исполнен по ask; комиссия 0,25% учтена; обычный вход подтверждён во времени и не ловит один зелёный тик"
                    }
'''
)


# ---------------------------------------------------------------------------
# D) Once Serge has bought, virtual-agent trade noise is not useful. Keep logs,
#    but suppress their sounds and focus user notifications on exit risk.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/example/pumppaperbot/DeepSeekActionLevel.kt"
replace_once(
    path,
    '''internal object VirtualTradeAlertPolicy {
    @Suppress("UNUSED_PARAMETER")
    fun shouldNotify(action: String, userPositionOpen: Boolean): Boolean = true
}
''',
    '''internal object VirtualTradeAlertPolicy {
    @Suppress("UNUSED_PARAMETER")
    fun shouldNotify(action: String, userPositionOpen: Boolean): Boolean = !userPositionOpen
}
'''
)


# ---------------------------------------------------------------------------
# E) Exit alerts: first evidence-based yellow warning at danger 7, immediate
#    critical escalation at 9, while unchanged conditions still do not spam.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/example/pumppaperbot/PositionSupervisor.kt"
replace_once(
    path,
    '''internal object PositionAlertPolicy {
    const val MIN_REPEAT_INTERVAL_MILLIS = 10L * 60L * 1000L

    fun shouldAlert(
        previous: PositionSupervisionState,
        firstExit: Boolean,
        stillExit: Boolean,
        dangerLevel: Int,
        conditionDelta: Int,
        now: Long
    ): Boolean {
        if (firstExit) return true
        if (!stillExit) return false
        val notifiedDanger = previous.lastAlertDanger.takeIf { previous.lastAlertAt > 0L }
            ?: previous.dangerLevel
        val notifiedDelta = previous.lastAlertConditionDelta.takeIf { previous.lastAlertAt > 0L }
            ?: previous.conditionDelta
        val materiallyWorse = dangerLevel > notifiedDanger || conditionDelta <= notifiedDelta - 2
        if (!materiallyWorse) return false
        if (dangerLevel >= 10 && notifiedDanger < 10) return true
        return previous.lastAlertAt <= 0L || now < previous.lastAlertAt ||
            now - previous.lastAlertAt >= MIN_REPEAT_INTERVAL_MILLIS
    }
}
''',
    '''internal object PositionAlertPolicy {
    const val MIN_REPEAT_INTERVAL_MILLIS = 10L * 60L * 1000L
    const val PREPARE_LEVEL = 7
    const val CRITICAL_LEVEL = 9

    fun shouldAlert(
        previous: PositionSupervisionState,
        firstExit: Boolean,
        stillExit: Boolean,
        dangerLevel: Int,
        conditionDelta: Int,
        now: Long
    ): Boolean {
        val notifiedDanger = previous.lastAlertDanger.takeIf { previous.lastAlertAt > 0L }
            ?: previous.dangerLevel
        val notifiedDelta = previous.lastAlertConditionDelta.takeIf { previous.lastAlertAt > 0L }
            ?: previous.conditionDelta
        if (dangerLevel >= CRITICAL_LEVEL && notifiedDanger < CRITICAL_LEVEL) return true
        if (firstExit) return true
        if (!stillExit) {
            if (dangerLevel < PREPARE_LEVEL) return false
            val firstPrepare = notifiedDanger < PREPARE_LEVEL
            val rearmedAfterRecovery = previous.dangerLevel <= 4 &&
                (previous.lastAlertAt <= 0L || now - previous.lastAlertAt >= MIN_REPEAT_INTERVAL_MILLIS)
            return firstPrepare || rearmedAfterRecovery
        }
        val materiallyWorse = dangerLevel > notifiedDanger || conditionDelta <= notifiedDelta - 2
        if (!materiallyWorse) return false
        return previous.lastAlertAt <= 0L || now < previous.lastAlertAt ||
            now - previous.lastAlertAt >= MIN_REPEAT_INTERVAL_MILLIS
    }
}
'''
)


# ---------------------------------------------------------------------------
# F) Personal guard: one weak 15-second tick or one book snapshot is no longer
#    enough for a loud warning. Use accelerated money pressure + price reaction;
#    explicit absorption blocks false seller-takeover warnings.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/example/pumppaperbot/PersonalPositionGuard.kt"
replace_once(
    path,
    '''        val microWeak = microFresh && (
            micro.aggressiveBuyPercent15s < 48.0 || micro.priceChange60sPercent <= -0.20 ||
                (micro.topBookImbalance ?: 0.0) <= -0.10
        )
''',
    '''        val moneyPressure = FastMoneyPressurePolicy.evaluate(micro)
        val microWeak = microFresh && !moneyPressure.absorptionPossible && (
            moneyPressure.heavySelling ||
                (micro.aggressiveBuyPercent15s <= 45.0 && micro.aggressiveBuyPercent60s <= 48.0 &&
                    micro.priceChange60sPercent <= -0.12) ||
                ((micro.topBookImbalance ?: 0.0) <= -0.15 && micro.priceChange60sPercent <= -0.20)
            )
'''
)


# ---------------------------------------------------------------------------
# G) Position notification presentation and fast shock warnings.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/example/pumppaperbot/PumpAlert.kt"
replace_once(
    path,
    '''    private const val sergeSoundTestNotificationId = 3523
''',
    '''    private const val sergeSoundTestNotificationId = 3523
    private const val fastPositionWarningNotificationId = 3524
'''
)
replace_once(
    path,
    '''        val title = when {
            state.action == "CANCEL_EXIT" -> "ОТМЕНА ВЫХОДА — ПРОДОЛЖАЕМ"
            state.exitAdvised && state.dangerLevel >= 9 -> "КРИТИЧЕСКАЯ СИТУАЦИЯ ${state.dangerLevel}/10"
            state.exitAdvised && state.conditionDelta < 0 ->
                "СИТУАЦИЯ УХУДШАЕТСЯ ${state.conditionDelta}/−10"
            state.exitAdvised && state.conditionDelta > 0 ->
                "СИТУАЦИЯ УЛУЧШАЕТСЯ +${state.conditionDelta}/+10"
            else -> "DEEPSEEK РЕКОМЕНДУЕТ ВЫХОД"
        }
''',
    '''        val title = when {
            state.action == "CANCEL_EXIT" -> "ОТМЕНА ВЫХОДА — ПРОДОЛЖАЕМ"
            state.dangerLevel >= PositionAlertPolicy.CRITICAL_LEVEL ->
                "КРИТИЧНО • ПРОВЕРЬ ВЫХОД ${state.dangerLevel}/10"
            !state.exitAdvised && state.dangerLevel >= PositionAlertPolicy.PREPARE_LEVEL ->
                "БУДЬ ГОТОВ К ВЫХОДУ ${state.dangerLevel}/10"
            state.exitAdvised && state.conditionDelta < 0 ->
                "СИТУАЦИЯ УХУДШАЕТСЯ ${state.conditionDelta}/−10"
            state.exitAdvised && state.conditionDelta > 0 ->
                "СИТУАЦИЯ УЛУЧШАЕТСЯ +${state.conditionDelta}/+10"
            else -> "DEEPSEEK РЕКОМЕНДУЕТ ВЫХОД"
        }
'''
)
replace_once(
    path,
    '''        val urgentExit = state.exitAdvised || state.dangerLevel >= 8
''',
    '''        val urgentExit = state.exitAdvised || state.dangerLevel >= PositionAlertPolicy.PREPARE_LEVEL
'''
)
insert_before_once(
    path,
    '''    fun showPositionSupervision(context: Context, state: PositionSupervisionState) {
''',
    '''    fun showFastPositionWarning(context: Context, decision: FastPositionWarningDecision) {
        ensureChannels(context)
        if (!ResearchModePolicy.userAlertsAllowed(context)) return
        val shock = ShockReboundStore.state(context)
        val title = if (decision.band >= FastPositionWarningPolicy.CRITICAL) {
            "СЕРЖ: КРИТИЧНО • ПРОВЕРЬ ВЫХОД"
        } else {
            "СЕРЖ: БУДЬ ГОТОВ К ВЫХОДУ"
        }
        val ratio = decision.pressure.activityRatio60sTo5m?.let {
            String.format(java.util.Locale.GERMANY, "×%.2f", it)
        } ?: "ещё без 5м фона"
        val text = String.format(
            java.util.Locale.GERMANY,
            "%s. Провал за локальные 3м %.2f%%, отскок %.2f%%, BUY за 60с %.0f%%, темп денег %s. Это предупреждение, не автоматическая продажа.",
            decision.reason,
            shock.drawdown3mPercent,
            shock.rebound3mPercent,
            decision.pressure.buyerShare60s,
            ratio
        )
        showTradeNotification(
            context,
            positionSupervisorChannelId,
            fastPositionWarningNotificationId,
            title,
            text,
            if (decision.band >= FastPositionWarningPolicy.CRITICAL) 0xFFDA3633.toInt() else 0xFFFFC107.toInt(),
            alwaysLoud = true
        )
    }

'''
)
replace_once(
    path,
    '''            sergeSoundTestNotificationId
        ).forEach(manager::cancel)
''',
    '''            sergeSoundTestNotificationId,
            fastPositionWarningNotificationId
        ).forEach(manager::cancel)
'''
)


# ---------------------------------------------------------------------------
# H) Final metadata. V5.19 is a real new installable version, not a same-version
#    replacement. Package and storage namespaces stay unchanged.
# ---------------------------------------------------------------------------
path = "app/build.gradle"
replace_once(
    path,
    '''        versionCode 98
        versionName "5.18"
''',
    '''        versionCode 99
        versionName "5.19"
'''
)

print("V5.19 fast shock + position alert extension applied")
