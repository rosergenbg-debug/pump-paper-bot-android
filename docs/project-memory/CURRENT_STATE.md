# PUMP / PumpBot — CURRENT STATE

Обновлено: **2026-08-28**

Это короткий снимок текущего состояния. Старую историю сюда не накапливать; исследовательские checkpoints хранятся в `X/`.

## VERSION / PRODUCTION

- Canonical branch: `main`.
- Current production source: **V6.6**, `versionCode 124`.
- `applicationId`: `com.example.pumppaperbot.v8` — сохранён для update/data continuity.
- Production merge: PR **#88**, merge commit `9eb48bdab784f60fc9e7ab5d10ae62c8efda3103`.
- Validated source head before merge: `080f3c3ae7732098314e28ffcb47a2683f390666`.
- Green Android Build run: **33214504275** (`testDebugUnitTest + lintDebug + assembleDebug + APK checks`).
- Real orders are not implemented. V6.6 remains paper-only.

## MANDATORY STARTUP MEMORY

Every new chat must read:

`ИНСТРУКЦИЯ_1.md` → `AGENTS.md` → `X/README.md` → `docs/project-memory/*`

For strategy work also read the newest files under `X/`.

## V6.6 — FOCUSED NETWORK

V6.5's active T32 variants are removed from the production service loop. V6.6 actively runs exactly four fresh portfolios, each starting from **€1000 / zero trade history** in new V6.6 preferences:

1. `AUTO CORE`
2. `AUTO BTC GUARD`
3. `AUTO SOL/BTC SELECT`
4. `HUMAN SELECT`

The old V6.5 persisted stores remain dormant for evidence/compatibility only; they are not driven by the V6.6 foreground service.

All three AUTO profiles share the same economic core:

- T32/VWAP-style entry context from protected X lineage;
- `BELOW4_PEAK12H` context;
- execution intent equivalent to `LIMIT close * 0.999`, short TTL;
- **TP +2.5% NET**;
- **STOP -1.2% NET**;
- **TIME 120 minutes**;
- **max 2 entries per UTC day**;
- fee model **0.21% BUY + 0.21% SELL**.

Profiles differ only by entry context:

- `AUTO CORE`: frozen core conditions without extra market gate.
- `AUTO BTC GUARD`: blocks when BTC has a strong recent 1h rise in the recent 1–3h window.
- `AUTO SOL/BTC SELECT`: requires delayed SOL-vs-BTC relative strength around lag 6h (`SOL-BTC L6 >= +0.40 percentage points`).

This separation is intentional: compare entry context, not multiple TP/STOP changes at once.

## HUMAN SELECT

- Uses the same V6.6 economic exit core as AUTO: `+2.5% NET / -1.2% NET / 120m`.
- Entry is **owner-confirmed only** through `ВОЙТИ HUMAN`.
- `ОТКЛОНИТЬ` suppresses the current setup until it decays/reset conditions are met.
- Opportunity monitoring continues even while a HUMAN position is already open.
- A second HUMAN BUY cannot be opened while the current HUMAN position is open.

## LIVE READINESS 0–100

The old near-binary behavior was replaced with a continuous owner-facing readiness score.

The score develops from live one-minute context using:

- distance below VWAP;
- 12h drawdown depth;
- current taker BUY share;
- current candle recovery/body;
- acceleration of BUY share.

The fast path refreshes roughly every **30 seconds**. The gauge is informative only; AUTO BUY still requires the exact closed-candle setup and the profile gate.

## ALERT / CALL LOGIC V6.6

All alert paths now use one master policy:

- `OFF` = absolute silence;
- `WORK` = only allowed work-schedule windows;
- `DAILY` = every day in configured daily window;
- `ALWAYS` = 24/7.

Important fixes:

- HUMAN alerts no longer stop merely because a HUMAN position is already open;
- old safety/legacy sound paths cannot bypass the common master switch/schedule;
- while HUMAN remains pending, alarm can repeat approximately once per minute;
- main V6.6 screen includes `ТЕСТ HUMAN-ЗВОНКА СЕЙЧАС` for immediate sound/vibration verification;
- Android DND, manually muted notification channels, and OEM battery/audio restrictions can still suppress delivery at OS level.

## BACKGROUND SERVICE

`PumpSignalService` V6.6 foreground loop runs only:

- three V6.6 AUTO profiles;
- HUMAN SELECT;
- existing personal-position safety warning (does not open real orders).

Fast market path refreshes ~30s; broader market/ecosystem context refreshes ~2m.

## SIGNING / INSTALL CONTINUITY

- Signing alias: `pump-signal-update`.
- Expected certificate SHA-256:
  `1F:77:8C:42:91:C9:D1:1C:5F:89:F4:DE:87:73:BD:A3:5A:01:25:03:1A:DC:05:78:5D:AE:E2:3F:27:DC:78:23`
- V6.6 signed APK certificate matches V6.5 exactly.
- V6.6 APK verifies with APK Signature Scheme **v2 + v3**.
- Delivered V6.6 signed APK SHA-256:
  `62463dfc460d6e0706415583954f89b3a33fbe74ee8fe77c08ca7a95d5193dbe`
- Never create a replacement signing key and never commit JKS/password/recovery bundle.
- Install V6.6 **over** the existing app; do not uninstall first.

## RESEARCH BASIS / WHAT IS STILL UNPROVEN

Protected X results remain the source of truth:

- original canary: `1772 signals / 974 fills / 318 positive = 32.65% WR`;
- `BELOW4_PEAK12H` improved garbage filtering;
- wider stop showed that production `-0.8%` had been too tight;
- fixed `TP +2.5 / STOP -1.2` plus a **120–135m TIME plateau** materially improved economics versus 90m;
- 120m was selected for production V6.6 as the conservative point in that plateau, not because 135m looked best on one recent slice;
- SOL/BTC delayed relative-strength context is an experimental profile, not proven alpha.

V6.6 is a forward paper test. Do not call it profitable until forward NET/PF evidence supports that conclusion.

## INVARIANTS

1. Keep package/signing identity unchanged.
2. Never uninstall the old app just to update.
3. No real-order authority without an explicit separate decision.
4. Judge strategies by NET expectancy/PF and trade frequency, not win rate alone.
5. Do not exceed three automatic owner-facing strategy profiles without a new explicit decision.
6. Keep HUMAN entry owner-confirmed.
7. Before strategy research, read X and reproduce the relevant canary.
8. Never erase older X checkpoints when a new result appears.
