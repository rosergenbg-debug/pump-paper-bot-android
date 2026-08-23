# ChatGPT V5.12 — candle-first main chart

Base: V5.11. This revision changes presentation only; Fusion/DeepSig/APP decision algorithms are unchanged.

- Main-screen vertical scale is based on visible candle highs/lows only. EMA50/EMA200 remain drawn but no longer compress the candle price action.
- Main chart supports vertical drag to inspect indicator lines that sit outside the candle-centered auto window. Horizontal history dragging remains available in the separate detailed chart, not on the main screen.
- The old DeepSig/APP right-side gauges are replaced on the main chart by four narrow live bars: instant, 5m, 15m and 30m.
- The four bars read the same `LiveMarketBreathingStore` snapshot/horizon values used by the Critical Overview and Fusion flow display; no new signal calculation is introduced.
- Stale breathing data renders neutral bars instead of pretending a current signal.
- Package remains `com.example.pumppaperbot.v8`; versionCode 91; versionName 5.12.
