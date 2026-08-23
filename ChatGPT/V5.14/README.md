# PumpSignal V5.14 — Unified Flow

V5.14 replaces the previous mixed multi-horizon flow math with one completed-minute engine shared by the upper market-breathing bars, flow-wave history, BuyerBreath time base, CapitalFlow inputs and Fusion decisions.

## What changed

- `Instant` remains intentionally fast and can react inside the current minute.
- 5/15/20/30/60/360-minute horizons use one observation per **completed wall-clock minute**. Repeated 15-second snapshots of the same rolling 60-second trade window are no longer counted as separate minutes.
- The 20-minute price normalization is explicit (`1.50`) instead of falling through to the old generic `6.0` scale.
- A horizon is not published until it has real coverage: 4/12/16/25/50/300 completed minutes for 5/15/20/30/60/360 respectively.
- Flow is based primarily on aggregate executed BUY/SELL notional across the window. Price response, book imbalance and capped BTC context remain secondary inputs.
- A neutral band around zero and cross-minute confirmation prevent small `+/-` noise from changing direction immediately. Strong moves still pass without unnecessary delay.
- The historical flow arcs no longer use the old EWMA half-life algorithm. They use the same fixed-window scoring engine as the upper bars.
- During a feed pause the last real fixed-window value is held and marked stale instead of being artificially decayed toward zero.
- BuyerBreath receives the same non-overlapping completed-minute time base.
- Fusion continues to consume the upper 5/15/20/30 values, so it automatically inherits the slower, coverage-checked flow and keeps its existing anti-chatter/armed-exit/virtual-stop protections.

## Compatibility

- `applicationId` remains `com.example.pumppaperbot.v8`.
- `versionCode` is `94`.
- `versionName` is `5.14`.
- Existing app data and Fusion paper history are not intentionally reset.
- Exchange access remains read-only; Fusion is still virtual/paper execution only.

## Verification focus

Unit tests cover incomplete 30-minute history, exact 20-minute behavior, current-minute isolation, neutral-zone behavior, one-minute reversal resistance, Fusion warm-up, stale-feed handling and BTC-context capping.
