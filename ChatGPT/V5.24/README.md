# PumpSignal V5.24 — Pump Machine 2% NET

## Purpose

V5.24 adds a third independent paper account to test Serge's lower-profit, higher-turnover hypothesis without changing the existing Fusion or Pump Machine 3% experiment.

## Account behavior

- **Fusion:** unchanged; existing state and history preserved.
- **Pump Machine 3%:** unchanged; existing state and history preserved.
- **Pump Machine 2% (PM2):** new independent account, fresh €1000 paper baseline, isolated preferences `pump_machine_2_paper_v524`.
- PM2 consumes the same cached `SharedFusionEntryObservation` and the same `SharedFusionEntryPolicy` as Fusion/PM3.
- PM2 keeps its own confirmation/cooldown state after its own exits.
- PM2 hard profit target: **+2.00% NET**, computed from executable bid after simulated exit fee and including the buy-side fee already paid through `entryCostEur`.
- PM2 hard stop: **−1.50% NET**.
- PM2 retains the same confirmed Fusion `SYSTEM_EXIT` and failed shock-rebound safety exits as PM3.
- Paper-only. No real-order path is added.

## No-reset guarantee

V5.24 deliberately disables invocation of the old V5.23 one-shot reset. It does not rely on the old marker being present. Installing V5.24 must not clear or migrate existing Fusion/PM3 paper preferences. Only the brand-new PM2 store starts fresh.

## UI

- Existing full-width Fusion account button is split into two half-width buttons: Fusion and `PUMP 2% NET`.
- Existing PM3 button remains and is labeled `PUMP 3% NET`.
- The Pump Machine lab screen compares Fusion / PM3 / PM2 and merges their recent trade events.

## Diagnostics

PM2 is included in cycle logs, unified research export, and the append-only performance ledger under its own identity (`PUMP_MACHINE_2` / `PumpMachine2`, epoch `V5.24+`).

## Regression check

`PumpMachine2PolicyTest` verifies that PM2 waits below +2% net and exits after executable net return reaches +2%.

## Version

- package: `com.example.pumppaperbot.v8`
- versionName: `5.24`
- versionCode: `104`
