# PumpSignal V5.15

Performance repair for the unified V5.14 flow engine.

- keeps the V5.14 completed-minute flow math and smoothing;
- removes repeated quadratic history scans from the hot UI path;
- computes the expensive multi-minute snapshot once per completed wall-clock minute;
- keeps the instant score live between minute boundaries without rebuilding 5/15/20/30/60/360 history;
- reuses one minute-bucket set across horizons, BuyerBreath and flow history;
- preserves Fusion paper state and existing app data;
- no real exchange orders are introduced.

This version is intentionally a performance-only continuation of V5.14 rather than another algorithm rewrite.
