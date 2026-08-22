# PumpSignal V5.21 — Pump Machine

Sequential experimental release built on V5.20.

Replaces the old DEEPSIG paper-trading participant with **Pump Machine**:
- entry timing and normal system exits follow the same unified Fusion flow / stability logic;
- hard per-trade take-profit at +3.00% **net** after buy/sell fees and executable bid/ask;
- hard per-trade stop-loss at -1.50% **net**;
- earlier Fusion system exit and failed shock-rebound exit remain allowed;
- the old DEEPSIG paper coordinator no longer executes the top competition account;
- a fresh Pump Machine storage namespace starts the 24-hour experiment cleanly;
- APP, DeepSigX, Fusion and Serge accounts are unchanged.

All orders remain virtual/paper-only. Exchange integrations remain read-only.
