# PumpSignal development log

This is the agent's chronological external memory. Append every material change; do not rewrite history to match an assumption.

## Recovered baseline

- Project repository: `rosergenbg-debug/pump-paper-bot-android`. Never modify the old `price-tracker-android` project.
- StrategyV2 and Gemini use separate virtual portfolios. User/Serge is the third independent virtual portfolio.
- Gemini runs as an hourly independent Shadow experiment. It receives market/news context, uses a fresh post-response execution quote, and does not control StrategyV2.
- Monitoring uses a foreground service near every two minutes plus WorkManager reserve cycles. Ordinary alerts include quiet-hour scheduling; urgent personal exits may bypass it.
- Signing history caused failed Android updates. Compatible release certificate SHA-256 is `1f778c4291c9…27dc7823`; GitHub intermediate builds previously used a different certificate and must not be handed to Serge as final updates.

## V3.18 — three-way competition

- Added APP, Gemini and SERGE virtual competition, balances, signed returns and graph trade markers.
- Correct release is V3.18/code 50 over V3.17/code 49. Earlier temporary V3.19 labels on the same feature were a numbering mistake and were reverted.
- Final V3.18 APK was signed with the compatible certificate and delivered as a direct APK.

## V3.19 — trade alerts and timing tuning

- Request: always ring for APP entry, APP exit, Gemini entry and Gemini exit. Gemini entry timing is good; Gemini exit is late. APP exit is good; APP entry is too rare.
- Added fresh high-importance alarm channels for APP trades and Gemini trades. BUY and SELL use distinct notification ids, so APP and Gemini events cannot overwrite one another.
- Alerts are triggered by newly appended executed trades, not only by a readiness gauge. When an APP trade alert fires, the generic APP readiness alert is suppressed for that cycle to avoid a duplicate ring.
- Replaced Gemini BUY-only alert detection with BUY/SELL trade detection.
- Initial implementation added fixed Gemini exits at −3% or after +2% and a 1.2% pullback. Serge rejected fixed percentages as the main decision logic before release, so this was removed from the control Gemini.
- Modestly widened APP entry confirmation: cautious RSI recovery accepts up to 57; Active mode keeps a valid RSI recovery for one extra closed candle, accepts a wider RSI band and slightly lower shock-volume threshold. Late-entry, rapid-drop and PUMP/BTC/SOL overheat vetoes remain unchanged.
- Added unit coverage for APP/Gemini executed-trade detection, Gemini protective exit and APP entry sensitivity.
- Release metadata set to V3.19/code 51. Final build/signature verification remains required before delivery.
- Local commit `83ea1ae` was created on `agent/v3-19-trade-alerts-and-tuning`. Push was blocked by the workspace publication guard pending explicit user approval for this exact repository/branch; no remote V3.19 branch or CI build exists yet.

## V3.19 — Gemini exit experiment

- Request: add a fourth visible participant, «Gemini‑эксперимент», alongside APP, Gemini and Serge. It must send its own entry and exit signals and be visible in a four-way comparison.
- Kept Gemini as the control. Removed the unreleased fixed-percentage protective exit and the prompt wording that would have changed Gemini's exit behaviour.
- Added a separate persistent experimental portfolio. On first run it starts from an exact checkpoint copy of Gemini; afterwards it mirrors only Gemini's newly executed BUY at the same quote. Gemini SELL does not close the experiment.
- The experimental exit checks every monitor cycle. Evidence groups are adaptive price pullback, spot/futures buyer flow, spot/futures CVD, BTC/SOL, open interest, order-book imbalance and the existing market-direction score. A moderate multi-group reversal must persist across two checks; a strong reversal can exit immediately. A −5% loss is only the emergency backstop.
- Added distinct high-importance alarm notifications «GEMINI‑ЭКСПЕРИМЕНТ: ВХОД/ВЫХОД», separate ids and a separate Android channel.
- Main screen now uses a 2×2 account grid. The comparison screen renders four synchronized graphs. A dedicated experiment screen shows balance, return, position, evidence score, confirmation streak, adaptive noise allowance and trades.
- Added pure unit coverage for checkpoint initialization, mirrored entry, ignored control SELL, two-cycle confirmation, isolated-indicator rejection and emergency backstop.

## V3.19 — build and compatible APK

- Serge explicitly authorized publishing `agent/v3-19-trade-alerts-and-tuning` to `rosergenbg-debug/pump-paper-bot-android` and running the V3.19 build on 2026-08-01.
- Published the exact local V3.19 tree `43140c1fb6e4967b1bdb3aaa31ff69cc197330e0` through GitHub. Remote branch head after reconciling the independent `main` history: `ce24b2b9bdec89f303e58d198a294a9cdf45225f`.
- Opened draft PR #28 only as the safe Android Build trigger. GitHub Actions run #109 completed successfully, including unit tests, `assembleDebug`, package/version/activity checks and intermediate APK verification.
- Re-signed the verified intermediate APK with the installed-compatible personal update key. Final certificate SHA-256: `1f778c4291c9d11c5f89f4de8773bda35a0125031adc05785daee23f27dc7823`; APK Signature Schemes v2 and v3 verify successfully.
- Final APK: `PumpSignal-V3.19-Compatible-FINAL.apk`, 7,047,471 bytes, SHA-256 `e6d48beb303a38e5e837771ffb6029bbced7c79ed625708e3f48ec0ad14e4bf9`. Its `AndroidManifest.xml` and `classes.dex` hashes exactly match the CI-verified intermediate APK.
