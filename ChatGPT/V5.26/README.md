# PumpSignal V5.26 — PM2/PM3 Profit Engine

V5.26 is a bounded paper-trading experiment focused on Pump Machine 2 and Pump Machine 3.
It is designed to improve expectancy but does not guarantee profit.

## Evidence behind the change

Recent V5.21 logs showed PM3 repeatedly reaching `ENTRY_ARMED_STRONG` while remaining at the €1000 baseline with zero trades. The next 2–3 minute cycle often returned to WAIT. The old design required all current/5m/15m/30m Fusion horizons plus a 60-second confirmation, so short opportunities could disappear before execution.

## What changed

- PM2/PM3 no longer use the old 60-second shared Fusion confirmation for ordinary entry.
- The existing MicroImpulse websocket (~15-second causal observer) can trigger local PM checks without an LLM request.
- Entry is restricted to BuyerBreath `IGNITION` or `EXPANSION`.
- Anti-FOMO uses move since phase start, 5m move, absorption risk, price-response efficiency, buyer share, activity and current flow.
- PM2 and PM3 use different entry strength requirements.
- All exit thresholds are measured in executable NET PnL after simulated buy fee, sell fee and Bitpanda bid/ask.

### PM2
- TP: +2.00% NET
- hard stop: -1.10% NET
- breakeven arm: +0.85% NET
- protected floor after BE: +0.10% NET
- early adverse-flow exit: from -0.45% NET when flow deteriorates
- soft timeout: 20 min
- absolute timeout: 30 min

### PM3
- TP: +3.00% NET
- hard stop: -1.30% NET
- breakeven arm: +1.25% NET
- protected floor after BE: +0.15% NET
- early adverse-flow exit: from -0.55% NET when flow deteriorates
- soft timeout: 35 min
- absolute timeout: 50 min

## What was deliberately NOT copied from the Gemini proposal

- Gross +2.6/+3.6 targets: the app already knows true NET PnL, so approximation is unnecessary.
- 1m EMA20 + 5m RSI as the hot-path gate: those are not available at reliable 15-second cadence in the PM execution layer; BuyerBreath supplies more direct causal flow/absorption information.
- LLM execution control: PM execution remains local; DeepSeek is not called in the fast path.
- PM3 70/30 partial liquidation: the existing PM portfolio is full-position based. Partial quantities are postponed until the first V5.26 experiment is measured, so account history and fee accounting stay simple and auditable.

## Compatibility

- Package: `com.example.pumppaperbot.v8`
- Version: 5.26
- Version code: 106
- PM3 prefs remain `pump_machine_paper_v521`
- PM2 prefs remain `pump_machine_2_paper_v524`
- No account reset or migration is introduced.
