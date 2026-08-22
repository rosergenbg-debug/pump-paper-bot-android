from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}: {old[:160]!r}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


def replace_between(path: str, object_marker: str, start: str, end: str, replacement: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    obj = text.find(object_marker)
    if obj < 0:
        raise SystemExit(f"{path}: object marker missing: {object_marker!r}")
    a = text.find(start, obj)
    if a < 0:
        raise SystemExit(f"{path}: start marker missing after {object_marker!r}: {start[:120]!r}")
    b = text.find(end, a)
    if b < 0:
        raise SystemExit(f"{path}: end marker missing: {end[:120]!r}")
    p.write_text(text[:a] + replacement + text[b:], encoding="utf-8")


# ---------------------------------------------------------------------------
# 1) Fusion and Pump Machine use one physical entry policy and one 15-second
#    observation. Their FusionStabilityState objects remain independent.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/example/pumppaperbot/FusionSim.kt"
replace_once(
    path,
    '''        shockReady: Boolean = false,
        shockFailed: Boolean = false,
        shockEntry: Boolean = false
    ): FusionStabilityDecision {
''',
    '''        shockReady: Boolean = false,
        shockFailed: Boolean = false,
        shockEntry: Boolean = false,
        entryObservation: SharedFusionEntryObservation? = null
    ): FusionStabilityDecision {
'''
)
replace_between(
    path,
    "object FusionStabilityPolicy {",
    '''        if (!inPosition) {
''',
    '''        val peak = max(max(previous.peakBid, bid), entryPrice)
''',
    '''        if (!inPosition) {
            val observation = entryObservation ?: SharedFusionEntryObservation(
                frame = frame,
                shockReady = shockReady,
                sampledAt = now,
                sampleBucket = now / 15_000L
            )
            val shared = SharedFusionEntryPolicy.evaluate(previous, observation, now)
            return FusionStabilityDecision(
                shared.action,
                shared.nextState,
                0.0,
                shared.reason
            )
        }

'''
)
replace_once(
    path,
    '''        val breathing = LiveMarketBreathingStore.snapshot(context, now)
        val frame = FusionFlowPolicy.frame(breathing)
        val market = BitpandaFusionStore.state(context)
''',
    '''        val entryObservation = SharedFusionEntryObservationStore.snapshot(context, now)
        val frame = entryObservation.frame
        val market = BitpandaFusionStore.state(context)
'''
)
replace_once(
    path,
    '''            shockReady = !tracked.inPosition && shockFresh && shock.ready,
            shockFailed = shockFresh && shock.failed,
            shockEntry = shockEntry
        )
''',
    '''            shockReady = !tracked.inPosition && shockFresh && shock.ready,
            shockFailed = shockFresh && shock.failed,
            shockEntry = shockEntry,
            entryObservation = entryObservation
        )
'''
)

path = "app/src/main/java/com/example/pumppaperbot/PumpMachine.kt"
replace_once(
    path,
    '''        shockReady: Boolean,
        shockFailed: Boolean,
        shockEntry: Boolean,
        positionAgeMillis: Long
    ): PumpMachineDecision {
''',
    '''        shockReady: Boolean,
        shockFailed: Boolean,
        shockEntry: Boolean,
        positionAgeMillis: Long,
        entryObservation: SharedFusionEntryObservation
    ): PumpMachineDecision {
'''
)
replace_between(
    path,
    "object PumpMachinePolicy {",
    '''        if (!portfolio.inPosition) {
''',
    '''        val tradeNet = tradeNetPercent(portfolio, bid, feeRate)
''',
    '''        if (!portfolio.inPosition) {
            val shared = SharedFusionEntryPolicy.evaluate(previous, entryObservation, now)
            return PumpMachineDecision(
                action = shared.action,
                nextState = shared.nextState,
                reason = shared.reason,
                tradeNetPercent = 0.0
            )
        }

'''
)
replace_once(
    path,
    '''        val breathing = LiveMarketBreathingStore.snapshot(context, now)
        val frame = FusionFlowPolicy.frame(breathing)
        val shock = ShockReboundStore.state(context)
''',
    '''        val entryObservation = SharedFusionEntryObservationStore.snapshot(context, now)
        val frame = entryObservation.frame
        val shock = ShockReboundStore.state(context)
'''
)
replace_once(
    path,
    '''            shockFailed = shockFresh && shock.failed,
            shockEntry = shockEntry,
            positionAgeMillis = positionAge
        )
''',
    '''            shockFailed = shockFresh && shock.failed,
            shockEntry = shockEntry,
            positionAgeMillis = positionAge,
            entryObservation = entryObservation
        )
'''
)
replace_once(
    path,
    '''            "PUMP MACHINE • ждём первый Fusion-сигнал")
''',
    '''            "PUMP MACHINE • ждём общий Fusion-entry engine")
'''
)


# ---------------------------------------------------------------------------
# 2) Main chart vertical price-pan is armed only from the far-left edge.
#    Everywhere else vertical motion is left to the parent ScrollView.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/example/pumppaperbot/StrategyChartView.kt"
replace_once(
    path,
    '''    private var draggingVertically = false
    private var movedGesture = false
''',
    '''    private var draggingVertically = false
    private var verticalGestureArmed = false
    private var movedGesture = false
'''
)
replace_once(
    path,
    '''        contentDescription = "График PUMP/EUR. На главном экране тяните вверх или вниз для вертикального просмотра; нажмите для большого графика."
''',
    '''        contentDescription = "График PUMP/EUR. Вертикальный сдвиг цены доступен только жестом у самого левого края; остальная область листает экран."
'''
)
replace_once(
    path,
    '''                draggingHorizontally = false
                draggingVertically = false
                movedGesture = false
''',
    '''                draggingHorizontally = false
                draggingVertically = false
                verticalGestureArmed = mainViewportMode && event.x <= dp(32f)
                movedGesture = false
'''
)
replace_once(
    path,
    '''                    if (!draggingVertically && abs(dy) > dp(5f) && abs(dy) > abs(dx)) {
                        draggingVertically = true
''',
    '''                    if (verticalGestureArmed && !draggingVertically && abs(dy) > dp(5f) && abs(dy) > abs(dx)) {
                        draggingVertically = true
'''
)
replace_once(
    path,
    '''                draggingHorizontally = false
                draggingVertically = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
''',
    '''                draggingHorizontally = false
                draggingVertically = false
                verticalGestureArmed = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
'''
)
replace_once(
    path,
    '''                draggingHorizontally = false
                draggingVertically = false
                return true
            }
        }
''',
    '''                draggingHorizontally = false
                draggingVertically = false
                verticalGestureArmed = false
                return true
            }
        }
'''
)


# ---------------------------------------------------------------------------
# 3) Sequential version metadata after the V5.21 patch has been applied.
# ---------------------------------------------------------------------------
path = "app/build.gradle"
replace_once(
    path,
    '''        versionCode 101
        versionName "5.21"
''',
    '''        versionCode 102
        versionName "5.22"
'''
)

print("V5.22 shared Fusion entry + left-edge chart gesture patch applied")
