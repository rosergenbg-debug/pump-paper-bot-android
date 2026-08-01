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

## V3.20 — attributed signals and earlier Gemini experiment entry

- Serge reported a correctly timed preparatory signal followed by a strong rise, but could not tell which participant generated it or why no virtual account bought. He requested a persistent top-screen source/reason label and a moderately earlier entry for Gemini experiment.
- Root cause confirmed in V3.19 code: Gemini experiment had no independent entry path. While in EUR it could only wait for and mirror an already executed control-Gemini BUY; visible positive gauges/evidence could never open its portfolio.
- Added a persistent top signal attribution panel. It records source, signal/trade type, full reason, time and whether a virtual trade was actually executed. APP 99/100 is explicitly marked as no trade; APP, Gemini and Gemini-experiment trades are distinct.
- Added an independent early-entry path for Gemini experiment. Its anchor is APP readiness 99/100 or a fresh positive Gemini direction (at least +20 direction and 55 confidence). Entry additionally requires confirmed PUMP momentum and either aligned spot/futures buyer flow or positive spot/futures CVD.
- Preserved hard entry vetoes for late price, broad overheat, unconfirmed rapid drop, simultaneous BTC/SOL weakness and missing independent buyer confirmation. A blocked signal remains visible with its exact reason.
- Kept control-Gemini entry mirroring as a fallback and kept all portfolios independent. Existing V3.19 experiment state remains readable; new `lastPhase` state defaults safely for old JSON.
- Added unit cases for confirmed independent entry, visible non-executed preparation and late-entry veto. Release metadata advanced once to V3.20/code 52; build and final compatible-signature verification remain pending.
- Changes were committed locally on `agent/v3-20-signal-source-and-early-entry` as `d9f8f1f`. The worktree was clean after the commit. Full Android compilation remains pending because this workspace has neither Gradle nor the required authenticated `gh` CLI.

## V3.21 — APP confirmed trend continuation

- Serge reported that APP remained in EUR throughout a long, orderly rise even though the visible market setup looked ideal. Code review confirmed that every APP entry path required a preceding RSI dip, shock or multi-drop exhaustion; there was no continuation entry for a clean rising trend.
- Added a separate continuation path that requires two completed rising 30-minute candles above a rising EMA20, RSI 47–60, safe distance from EMA200, no three-hour chase, positive BTC trend and a non-weak broad market.
- Buyer confirmation is mandatory: aligned non-negative spot/futures taker imbalance, or stronger spot imbalance with non-negative relative-strength slope. The old late-entry, broad overheat and rapid-drop vetoes remain in force.
- Funding up to and including `+0.01%` is treated as neutral (`0.0001` raw), fixing the prior rejection of a harmless `+0.005%` rate. The same neutral threshold now applies to the original trend-recovery path.
- The new entry opens the existing `TREND` position mode so saved-state compatibility and the established APP exit remain unchanged. Continuation readiness contributes a visible 99/100 preparatory signal when exactly one independent confirmation is still missing.
- Added focused unit coverage for the funding boundary, two-candle confirmation, RSI band and buyer-flow alternatives. Release metadata advanced once to V3.21/code 53; README, UI title and GitHub workflow checks/artifact names were updated consistently.
- Static diff/XML checks pass. Full Gradle unit tests, APK assembly and compatible-certificate verification remain pending because `gh` is not installed in the workspace; the mandatory GitHub publish procedure requires authenticated GitHub CLI and forbids bypassing this prerequisite.

## V3.21 — build and compatible APK

- Published the exact local V3.21 tree `38d20a82dbd1e19f0271dc08843888179585621f` to `agent/v3-21-app-trend-continuation`; the remote tree matches local commit `8cdec01` byte-for-byte.
- Opened draft PR #29 as the Android Build trigger. GitHub Actions run #111 completed successfully, including unit tests, `assembleDebug`, package/version/activity checks and intermediate APK verification.
- Re-signed the CI APK with the installed-compatible personal update key. Final certificate SHA-256: `1f778c4291c9d11c5f89f4de8773bda35a0125031adc05785daee23f27dc7823`; APK Signature Schemes v2 and v3 verify successfully.
- Final APK: `PumpSignal-V3.21-Compatible-FINAL.apk`, 7,059,759 bytes, SHA-256 `a8d22bd12834c2eab7e44250a1d47097d702a15723589df27b8048e112fcc4be`. Its `AndroidManifest.xml` and `classes.dex` hashes exactly match the CI-verified intermediate APK.

## V3.22 — full audit fixes

- Serge requested one combined release from the full V3.21 control-flow audit and the separate visible-text audit, without changing trading thresholds.
- Added persistent per-participant trade-alert outboxes. APP, Gemini and Gemini-experiment now commit the new portfolio and pending trade alert together before attempting the Android notification. A failed/disabled notification remains queued and is retried in later cycles; the virtual trade itself is not repeated.
- Android notification permission and app-level notification availability are checked before a trade alert is considered accepted by the notification manager.
- Gemini-experiment entry now reads readiness from `PumpBotEngine.evaluateAppPaper()` with the independent APP portfolio. It no longer uses the manual Serge position's `waitMode/readinessScore`. The experiment also reconciles the latest control-Gemini trade each cycle so an interruption between control BUY persistence and mirroring cannot permanently drop that command.
- APP, Gemini and Gemini-experiment stores now keep a last-good JSON backup. If both primary and backup data are unreadable, automated trading stops with an explicit storage error instead of silently replacing history with a fresh €1,000 account.
- Ordinary quiet hours now apply every day and migrate the old default start from 06:00 to 06:15. Executed trade alerts and urgent exits remain immediate. Settings and status text were updated accordingly.
- User-visible release headers now use `BuildConfig.VERSION_NAME`; stale V3.15/V3.4 headers, radar explanations, reset wording and the old three-account details screen were updated for the four-participant architecture.
- Internal network identification now uses the build version instead of hard-coded `3.18`.
- Removed the personal key fetch and plaintext signing password from the active GitHub workflow. CI produces and verifies only an intermediate debug APK; the installed-compatible personal signature is applied after CI verification.
- Release metadata advanced once to V3.22/code 54. Added focused unit coverage for the 06:15 schedule boundary and independent APP readiness. Full CI build, APK inspection and compatible signature verification are pending.
- GitHub Actions run #113 reached Kotlin compilation and exposed two build-only type issues: BuildConfig generation was disabled and storage-error message lambdas returned nullable strings. Enabled `buildConfig` and made all four messages non-null; no strategy logic changed. A clean CI rerun is required.
- Published the corrected tree `c73e4a260617caa011209b9661d0bce95e586ee5` to `agent/v3-22-audit-fixes`. GitHub Actions run #114 completed successfully: all unit tests, Kotlin/Android compilation, `assembleDebug`, package `com.example.pumppaperbot.v8`, V3.22/code 54, launch activity and APK integrity checks passed.
- Downloaded artifact `PumpSignal-V3.22-Intermediate-APK` from run #114; its ZIP digest is SHA-256 `c5d21e06836f8254338ed6de260805d705003a74e4b7a6256fc329152916e555`.
- Re-signed the verified intermediate APK with the installed-compatible personal update key. Final certificate SHA-256 is `1f778c4291c9d11c5f89f4de8773bda35a0125031adc05785daee23f27dc7823`, exactly matching V3.21; APK Signature Schemes v2 and v3 verify successfully.
- Final APK: `PumpSignal-V3.22-Compatible-FINAL.apk`, 6,989,928 bytes, SHA-256 `07dfbaa7b160c427205d89dd881684b88fa8db9228f332adbb956521b8d99e3d`. Its `AndroidManifest.xml` and `classes.dex` hashes exactly match the CI intermediate APK.
