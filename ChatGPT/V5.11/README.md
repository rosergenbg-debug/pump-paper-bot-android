# ChatGPT V5.11 — stable UI + smoother Fusion

Base: ChatGPT V5.10 branch. V5.9 and V5.10 remain preserved and untouched as prior revisions.

## V5.11 scope

- Keep vertically scrolled reading position stable while live TextViews and panels update in the background. The guard works on ScrollView-based screens and does not fight active touch/fling gestures.
- Preserve the existing Fusion hypotheses: BUY uses upper-bar current/5m/15m/30m > 0; EXIT is initiated by current/5m/15m/20m < 0 and does not wait for 30m.
- Add anti-chatter execution: strong BUY alignment may enter immediately; weak zero-crossing requires a second evaluation. EXIT is armed first and waits for a real bid decline instead of selling merely because the bars briefly crossed below zero.
- Add virtual protective stop: initial -1.50%. Once peak bid reaches +0.60% from entry, the stop rises near fee-covered break-even and then trails the peak by 0.60%.
- Fix Fusion simulation costs at 0.25% on BUY and 0.25% on EXIT. Existing saved Fusion state/history remains compatible.
- Comparison charts are tappable. A large 48-bar detail view plus a chronological trade-event list makes dense entry/exit sequences readable. Fusion chart P/L uses the 0.25% per-side fee.
- Package remains `com.example.pumppaperbot.v8`; versionCode 90; versionName 5.11.
- All Fusion/DeepSig trading remains paper/virtual only. Bitpanda integration stays read-only and contains no order endpoint.

Final delivery requires successful unit tests, lint and APK assembly, followed by signing with the existing personal update certificate outside GitHub and certificate/package verification.