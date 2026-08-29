# PUMP / PumpBot — CURRENT STATE

Обновлено: **2026-08-29**

Это короткий снимок текущего состояния. Исследовательские checkpoints хранятся в `X/`.

## VERSION / PRODUCTION

- Canonical branch: `main`.
- Current production source: **V6.6.1**, `versionCode 125`.
- `applicationId`: `com.example.pumppaperbot.v8` — неизменён для update/data continuity.
- V6.6.1 hotfix: PR **#89**, merge commit `68a807730cc09123fc092760f5cf3e58742b1981`.
- Validated hotfix source head: `85263690e3620eb5055852578eb1dde7222cb51c`.
- Green Android Build run: **33239864413** (`testDebugUnitTest + lintDebug + assembleDebug + APK checks`).
- Real orders are not implemented. Production remains paper-only.

## MANDATORY STARTUP MEMORY

Every new chat must read:

`ИНСТРУКЦИЯ_1.md` → `AGENTS.md` → `X/README.md` → `docs/project-memory/*`

For strategy work also read the newest files under `X/`.

## V6.6.1 — OWNER NETWORK

The permanent owner-facing network contains **six accounts**:

1. `AUTO CORE`
2. `AUTO BTC GUARD`
3. `AUTO SOL/BTC SELECT`
4. `HUMAN SELECT`
5. `СЕРЖ`
6. `APP`

Only the first four are the fresh V6.6 experiment and start from **€1000 / zero trade history** in new V6.6 preferences.

### PERMANENT ACCOUNTS

`СЕРЖ` and `APP` are permanent and MUST NOT be removed, hidden, reset, or migrated to fresh preferences when experimental algorithms are replaced.

- `СЕРЖ` continues to read the existing `UserPaperStore` state/history/P&L.
- `APP` continues to read the existing `AppPaperStore` (`app_paper_v5_research`) state/history/P&L.
- `APP` StrategyV2 background sync is active again in `PumpSignalService` via `AppPaperStore.syncWithAlerts()`.
- Restoring APP does **not** reactivate the other retired V6.5 automatic research engines.
- Dashboard and comparison screen both show all six accounts.

## THREE NEW AUTO PROFILES

All three share the same economic core:

- T32/VWAP-style entry context from protected X lineage;
- `BELOW4_PEAK12H` context;
- short discounted-limit entry intent;
- **TP +2.5% NET**;
- **STOP -1.2% NET**;
- **TIME 120 minutes**;
- **max 2 entries per UTC day**;
- fee model **0.21% BUY + 0.21% SELL**.

They differ only by entry context:

- `AUTO CORE`: frozen core conditions.
- `AUTO BTC GUARD`: blocks strong recent BTC rise in the recent 1–3h context.
- `AUTO SOL/BTC SELECT`: requires delayed relative strength around lag 6h (`SOL-BTC L6 >= +0.40 percentage points`).

## HUMAN SELECT

- Same economic exit core as AUTO: `+2.5% NET / -1.2% NET / 120m`.
- Entry is owner-confirmed only through `ВОЙТИ HUMAN`.
- `ОТКЛОНИТЬ` suppresses the current setup until it decays/reset conditions are met.
- Opportunity monitoring continues while a HUMAN position is already open.
- A second HUMAN BUY cannot be opened while the current HUMAN position is open.

## LIVE READINESS 0–100

The owner-facing readiness score is continuous rather than a 0→95/100 last-second jump. It uses live one-minute context from:

- distance below VWAP;
- 12h drawdown depth;
- current taker BUY share;
- candle recovery/body;
- acceleration of BUY share.

Fast market path refreshes roughly every **30 seconds**. The gauge is informative only; AUTO BUY still requires the exact closed-candle setup and profile gate.

## ALERT / CALL LOGIC

One master policy applies:

- `OFF` = silence;
- `WORK` = allowed work schedule only;
- `DAILY` = every day in configured window;
- `ALWAYS` = 24/7.

Important behavior:

- HUMAN opportunity alerts continue even if a HUMAN position is already open;
- USER/HUMAN safety sounds respect master switch/schedule;
- APP paper trading continues independently of whether sound is allowed; only APP notification delivery is gated by the alert policy;
- dashboard includes immediate HUMAN sound/vibration test;
- Android DND, manually muted notification channels, and OEM restrictions may still suppress delivery at OS level.

## BACKGROUND SERVICE

`PumpSignalService` V6.6.1 runs:

- three V6.6 AUTO profiles;
- HUMAN SELECT;
- permanent APP StrategyV2 paper account;
- existing personal-position safety warning for SERGE.

Other legacy automatic research engines remain dormant.

## SIGNING / INSTALL CONTINUITY

- Signing alias: `pump-signal-update`.
- Expected certificate SHA-256:
  `1F:77:8C:42:91:C9:D1:1C:5F:89:F4:DE:87:73:BD:A3:5A:01:25:03:1A:DC:05:78:5D:AE:E2:3F:27:DC:78:23`
- V6.6.1 signed APK certificate matches V6.5/V6.6 exactly and verifies with APK Signature Scheme **v2 + v3**.
- V6.6.1 signed APK SHA-256:
  `5b102b3976e74f54de1695cd352794be560238b2c88ae57689e19dfb386aed21`
- Never create a replacement signing key and never commit JKS/password/recovery bundle.
- Install updates **over** the existing app; never uninstall first.

## RESEARCH BASIS / WHAT IS STILL UNPROVEN

Protected `X/` remains the strategy source of truth:

- original canary: `1772 signals / 974 fills / 318 positive = 32.65% WR`;
- `BELOW4_PEAK12H` improved garbage filtering;
- wider stop showed production `-0.8%` was too tight for the research line;
- fixed `TP +2.5 / STOP -1.2` plus a 120–135m TIME plateau improved economics versus 90m;
- 120m was selected conservatively;
- BTC delayed context and SOL/BTC relative-strength context remain experimental forward-paper features, not proven profit guarantees.

## INVARIANTS

1. Keep package/signing identity unchanged.
2. Never uninstall the old app just to update.
3. **SERGE and APP are permanent owner accounts: never remove, hide, reset, or replace their persisted stores.**
4. New strategy experiments may reset only their own new portfolios unless the owner explicitly says otherwise.
5. APP must keep its paper engine active unless the owner explicitly asks to stop it.
6. No real-order authority without a separate explicit decision.
7. Judge strategies by NET expectancy/PF and trade frequency, not win rate alone.
8. Do not exceed three automatic experimental profiles without a new explicit decision.
9. Keep HUMAN entry owner-confirmed.
10. Before strategy research, read X and reproduce the relevant canary.
11. Never erase older X checkpoints when a new result appears.
