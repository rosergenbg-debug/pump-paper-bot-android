# PUMP / PumpBot — CURRENT STATE

Обновлено: **2026-08-30**

## VERSION / PRODUCTION

- Canonical branch: `main`.
- Current release candidate: **V6.9.1**, `versionCode 130`.
- Verification PR: создаётся после локальной проверки.
- Stable restoration base: **V6.5**, commit `cbc10b4948bd22cbf5684b36596121ee562c8614`.
- `applicationId`: `com.example.pumppaperbot.v8` — неизменён.
- Launcher: `com.example.pumppaperbot.MainActivity`.
- Real orders are not implemented. V6.9.1 remains paper-only.

## V6.9.1 UI REGRESSION REPAIR

- V6.9 incorrectly added a details toggle that set established V6.8.1 controls and information blocks to `GONE` on launch. V6.9.1 removes that mechanism completely: existing graphs, windows, buttons and functions remain visible in their original order.
- Top status cards use smaller padding/text and lower minimum heights, but no status field or action is removed.
- BTC 2h/6h/24h values are vertical overlays at the left of the BTC chart; the price curve starts to their right.
- The chart grid uses integer ±1/±2/±3/±4 percent levels. ±1 is included in the viewport; farther levels render only when the graph's actual range reaches them. ±1.5 is removed from presentation guides.
- The BOT network button is visually prominent; account mapping and click behavior are unchanged.

## V6.9 SUPPORT / DASHBOARD UPDATE

- Both support buttons create bounded 24-hour reports on a background executor, validate every part before Android sharing, and preserve UTF-8 rows across splitting.
- The main screen shows a compact BTC 24-hour chart with causal 2h/6h/24h change, the established PUMP chart, and executed PUMP/USDT aggressive BUY/SELL/NET flow for now, 5m, 15m, 30m and 1h.
- Money flow is explicitly executed taker notional, not market cap or capital currently owned by all PUMP holders.
- Main and comparison charts in V6.9.1 show presentation-only integer ±1/±2/±3/±4 guides from the current/reference price.
- Existing stores, package, paper strategy authority, fees and thresholds are unchanged.

## MANDATORY STARTUP MEMORY

Every new chat must read:

`ИНСТРУКЦИЯ_1.md` → `AGENTS.md` → `X/README.md` → newest relevant `X/*` → `docs/project-memory/*`.

## WHY V6.7 EXISTS

V6.6/V6.6.1 did not physically erase most Kotlin sources, but replaced the V6.5 launcher and central service with a narrow V6.6 dashboard/runtime. This hid and stopped much of the established app and made it appear deleted. V6.7 restores the complete V6.5 launcher, UI and service runtime, then adds the selected X lines as isolated stages.

The following V6.6 transfer errors are forbidden:

- comparing a Binance PUMP/USDT limit directly with a Bitpanda PUMP/EUR ask;
- deriving the 12h entry context from a 30m/latest close instead of the causal closed Binance 1m series used by replay;
- presenting a high win rate as profitability when Avg NET and PF are negative;
- replacing the whole owner app/runtime to add one experiment.

## OWNER NETWORK

The V6.7 owner-facing network contains five accounts:

1. `AUTO X ECONOMY`
2. `AUTO X52 SELECT`
3. `HUMAN +2,0% NET`
4. `СЕРЖ`
5. `APP`

`СЕРЖ` and `APP` keep their existing stores/history/P&L and may never be removed, hidden, reset or migrated to fresh preferences. Old V6.5 experimental stores are not erased, but their retired T32 automatic variants are not run by the V6.7 owner network.

## TWO AUTONOMOUS X PAPER LINES

Shared causal entry core:

- raw Binance `PUMPUSDT` closed 1m candles;
- exact T32/VWAP setup plus `BELOW4_PEAK12H`;
- signal limit = signal close × 0.999, TTL 2 minutes;
- paper execution only against a fresh executable Bitpanda `PUMP/EUR` ask;
- fee model 0.21% BUY + 0.21% SELL;
- STOP -1.2% NET;
- maximum 2 entries per UTC day.

`AUTO X ECONOMY`:

- fixed TP +2.5% NET;
- TIME 120 minutes.

`AUTO X52 SELECT`:

- additionally requires delayed relative strength `SOL-BTC REL6 >= +0.40 percentage points`;
- dynamic signal/VWAP exit;
- TIME 90 minutes.

X52 is a selective, regime-dependent forward-paper hypothesis, not a profit guarantee.

## HUMAN / APP / LEGACY RUNTIME

- HUMAN entry still requires owner confirmation; its established V6.5 +2.0% NET exit behavior remains.
- APP StrategyV2 paper sync remains active.
- Full V6.5 PumpMachine/Fusion/DeepSeek background runtime is restored for continuity; its older accounts remain hidden from the focused owner network.
- One optional stage failure must not stop the independent service stages.

## WHAT “57%” REALLY MEANS

The original replay was reproduced twice. Stop -1.5% produced **57.36% win rate** (333 fills / 191 wins), but **Avg NET -0.297%** and **PF 0.533**. It is therefore not a profitable strategy and is not promoted as an autonomous V6.7 rule. V6.7 uses the more defensible economic/time and selective X candidates only as isolated forward-paper experiments.

## SIGNING / INSTALL CONTINUITY

- Signing alias: `pump-signal-update`.
- Required certificate SHA-256:
  `1F:77:8C:42:91:C9:D1:1C:5F:89:F4:DE:87:73:BD:A3:5A:01:25:03:1A:DC:05:78:5D:AE:E2:3F:27:DC:78:23`
- Final CI must verify APK Signature Scheme v2 + v3, package, versionCode 130, versionName 6.9.1, launcher and ZIP integrity.
- Never create a replacement signing key and never commit JKS/password/recovery material.
- Install V6.9.1 over the existing app; never uninstall first.

## INVARIANTS

1. Keep package/signing identity unchanged.
2. Never uninstall the old app just to update.
3. SERGE and APP are permanent and retain their stores/history.
4. Paper-only until a separate explicit real-order decision.
5. Judge strategies by NET expectancy/PF and frequency, not win rate alone.
6. Keep HUMAN entry owner-confirmed.
7. Read and reproduce the relevant X canary before changing strategy authority.
8. Never erase older X checkpoints.
9. Cross-market filters remain experimental until forward evidence is representative.
10. Add experiments as isolated stages; do not replace the central app/runtime.
