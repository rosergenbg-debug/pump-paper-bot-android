package com.example.pumppaperbot

fun capitalReadyObservation(
    frame: FusionFlowFrame?,
    now: Long,
    ask: Double = 1.0,
    shock: Boolean = false
) = SharedFusionEntryObservation(
    frame = frame,
    shockReady = shock,
    sampledAt = now,
    sampleBucket = now / 15_000L,
    micro = MicroImpulseSnapshot(
        connected = true,
        updatedAt = now,
        trades60s = 100,
        buyNotional5m = 420_000.0,
        sellNotional5m = 180_000.0,
        buyNotional15m = 720_000.0,
        sellNotional15m = 680_000.0,
        flowHistorySeconds = 3_600L,
        largeFlow = LargeFlowFingerprint(
            mode = LargeFlowMode.BUY_SERIES,
            confidence = 75,
            largeBuyUsdt = 150_000.0,
            largeSellUsdt = 30_000.0
        )
    ),
    executionAsk = ask,
    bookBidNotional = 90_000.0,
    bookAskNotional = 60_000.0,
    bookSpreadPercent = 0.08,
    capitalFlow = CapitalFlowProxy(mode = CapitalFlowMode.MIXED, score = 20, confidence = 90)
)
