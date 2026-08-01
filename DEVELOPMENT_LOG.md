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

## V4.0 — protected API keys and Serge position supervision

- Serge requested that a saved API key never remain in an editable field. Replaced the always-visible Gemini input with explicit provider panels: initially only «Ввести API-ключ» is shown; after save the input disappears and only «Изменить API» and «Удалить» remain. Deletion requires a confirmation dialog and a blank edit can no longer overwrite a valid key.
- Added a separate Android-Keystore-backed DeepSeek credential store and excluded it from cloud backup and device transfer. Gemini migration/storage remains compatible and neither key is embedded in the APK or repository.
- Added DeepSeek supervision for Serge's actual manual position. `deepseek-v4-pro` is forced immediately after «Я купил» and used for critical follow-ups; `deepseek-v4-flash` handles ordinary 15-minute position checks. Existing free Gemini hourly/news circuits remain independent to conserve their limited quota.
- The supervisor persists the first exit recommendation and continues checking when Serge does not sell. It shows change relative to that exit baseline on a −10…+10 scale, a separate 0…10 danger level, worsening/improvement messages and an explicit `CANCEL_EXIT` state.
- Added a dedicated high-importance Android alarm channel for initial exit, later deterioration/improvement and exit cancellation. Position supervision state is reset only for a new/closed/reset Serge position and is compatible with a V3.22 update.
- Release metadata advanced once to V4.0/code 55; package id remains `com.example.pumppaperbot.v8`. README and CI artifact/badging checks were updated. Focused state/scale/status unit tests were added; full CI build and compatible final signature verification are pending.
- Work is isolated on local branch `agent/v4-deepseek-position-supervisor`. Static diff and secret scans pass. This environment has no Gradle executable, XML validator or required `gh` CLI, so compilation, GitHub publication and CI APK verification remain explicit blockers rather than assumed successes.
- Created local implementation commit `6c1dac3` on `agent/v4-deepseek-position-supervisor`; the worktree was clean immediately after the checkpoint. It has not been pushed because the mandatory GitHub CLI prerequisite is unavailable.
- Serge explicitly authorized direct GitHub publication and the V4.0 build. Published the exact local V4 tree `82306340b1d9892fb06b62f162098bd0ed95464a` to `agent/v4-deepseek-position-supervisor` through the connected GitHub app as remote commit `55d40cb7dce87b4aac643d6dcee2ba8c27098c46`, then opened draft PR #31 as the Android Build trigger.
- GitHub Actions run #117 (`30700745815`) completed successfully. Unit tests, Kotlin/Android compilation, `assembleDebug`, package `com.example.pumppaperbot.v8`, V4.0/code 55, launch activity, APK Signature Scheme v2, archive integrity and the minimum-size check all passed.
- Downloaded artifact `PumpSignal-V4.0-Intermediate-APK`; its ZIP is 6,030,790 bytes with SHA-256 `72804bef21583ca7d7c5a6710b6169564501c9c382e80da786e97dcb159515a7`.
- Re-signed the CI APK with the installed-compatible personal update key. Final certificate SHA-256 is `1f778c4291c9d11c5f89f4de8773bda35a0125031adc05785daee23f27dc7823`; APK Signature Schemes v2 and v3 verify successfully.
- Final APK: `PumpSignal-V4.0-Compatible-FINAL.apk`, 7,088,431 bytes, SHA-256 `138902fcacf8a80edb191674a9ed2f3a477ea89383a1e3c95bb6fdde7c177397`. Its `AndroidManifest.xml` and `classes.dex` hashes exactly match the CI-verified intermediate APK.

## V4.1 — DeepSeek primary circuit and Gemini position reserve

- Serge reported that the V4.0 screen still presented Gemini as the main worker and did not prove that DeepSeek was processing anything. Code inspection confirmed the root cause: DeepSeek was only called after Serge pressed «Я купил»; before an open position it performed no market or news analysis.
- Added an independent primary DeepSeek Flash circuit. It receives the latest PUMP/EUR market frame, order-book/OI fields, local APP evidence and up to five current radar headlines every ten minutes, whether or not Serge has an open position. Gemini failures are recorded separately and do not block this circuit.
- Kept the position supervisor separate and stronger: DeepSeek Pro is forced immediately after «Я купил» and on critical follow-ups; ordinary primary Flash analysis continues alongside it.
- Added a compact first-screen «DEEPSEEK • ОСНОВНОЙ» card showing the active model, action, short conclusion, today's successful requests/errors and last successful response time. The radar screen also exposes the same real counters.
- Replaced the former internal Gemini ceiling of 50 with a conservative observed budget of 20 daily attempts: 10 are available outside a Serge position and 10 remain reserved until a real position is open. Once the ordinary half is consumed, retry scheduling sees zero available ordinary calls instead of generating repeated blocked attempts.
- Official DeepSeek V4 documentation confirmed both model ids but exposed a V4.0 request-shape bug: `reasoning_effort` had been nested inside `thinking`. Both primary and position clients now send `thinking: {type: enabled}` plus top-level `reasoning_effort`, matching the documented Chat Completions request. Pro receives a larger output allowance for critical reasoning.
- Release metadata advanced once to V4.1/code 56. Package id and protected stores are unchanged, so V4.0 data remains update-compatible. README and CI badging/artifact checks were updated; focused unit tests cover position-independent DeepSeek scheduling, the ten-minute interval and the 50/50 Gemini split.
- Static diff/whitespace checks pass. Local Gradle compilation is unavailable because the repository intentionally has no wrapper and this workspace has no Gradle executable; GitHub Actions compilation, APK inspection and compatible signing remain pending.
- GitHub Actions run #119 reached Kotlin compilation and found one missing closing brace after the `DeepSeekPrimaryPolicy.compactStatus` `when` expression. Added only the missing delimiter; the resulting retry must rerun the full test/build/inspection workflow.
- Published the focused syntax fix as remote commit `9c0e86af3ff6dc4053782504badd0ecbb0be1a15`. GitHub Actions run #120 (`30703376353`) completed successfully: unit tests, Kotlin/Android compilation, `assembleDebug`, package `com.example.pumppaperbot.v8`, V4.1/code 56, launch activity, APK Signature Scheme v2, archive integrity and size checks all passed.
- Downloaded artifact `PumpSignal-V4.1-Intermediate-APK`; ZIP size is 6,041,924 bytes, SHA-256 `c76f3d6e23de1cfeff64e361464e2aa55b03c98d3e0f4ac85f62ec17cf16c1ae`. Intermediate APK SHA-256 is `dbfcdb38df5858dea92c9cb058c41943174d4f1283b73bbca4de50b1667a25b4`.
- Re-signed the verified intermediate with the installed-compatible update key. Final certificate SHA-256 is `1f778c4291c9d11c5f89f4de8773bda35a0125031adc05785daee23f27dc7823`, exactly matching V4.0; APK Signature Schemes v2 and v3 verify successfully.
- Final APK: `PumpSignal-V4.1-Compatible-FINAL.apk`, 7,096,623 bytes, SHA-256 `1d14832638044339efadf943eaf498ba0a4d87d22c91b6ea98a9021323ad5362`. Its `AndroidManifest.xml` and `classes.dex` hashes exactly match the CI-verified intermediate APK.

## V4.2 — API centers, DeepSeek signal and richer analysis

- Serge requested replacing the two lower «Покупка/Продажа» indicators with DeepSeek API and Gemini API buttons. Added dedicated provider centers with protected key create/change/delete UX, live connection state, real manual tests, actual request rates for the last minute/hour/day, token totals and a capped seven-day request journal. Key controls and manual Gemini testing were removed from the news-radar screen.
- Kept the four virtual portfolios unchanged. Replaced only the old second signal gauge: APP remains the first independent signal and DeepSeek is now the second. Gemini remains a separate virtual control participant and reserve expert; its portfolio, trades and research history are not migrated into DeepSeek.
- Primary DeepSeek Flash cadence changed from ten to five minutes. A material readiness change of at least 15 points or a BUY/SELL state transition triggers an earlier call, and the main «Проверить» button explicitly requests a fresh primary analysis. Position Pro remains immediate after «Я купил» and on critical follow-ups.
- Audit found the V4.1 DeepSeek primary prompt materially narrower than the Gemini hourly prompt. Added PUMP 1h/3h/6h, BTC/SOL 1h/3h, spot/futures taker flow and CVD, premium and the existing funding/order-book/OI/RSI/StrategyV2/news data. The position supervisor receives the same evidence groups plus realized volatility. Responses now persist short evidence and invalidation risks.
- Gemini's internal budget follows Serge's observed 25-request allowance: 12 requests are available normally and 13 more unlock for an open Serge position. Outside a position, the autonomous expert is limited to one successful routine review per two hours; position-open and explicit manual calls bypass this cadence while still respecting the total quota. A 429 response that explicitly names a daily/RPD quota now pauses Gemini until the next Pacific-time reset instead of retrying after a short generic backoff.
- Added unified local API telemetry for DeepSeek primary/position calls and Gemini hourly/news calls. No API key, prompt payload, account balance or private market history is written to this journal.
- Release metadata advanced once to V4.2/code 57; package id remains `com.example.pumppaperbot.v8`. README, manifest and CI artifact/version checks were updated. Local static checks are pending before GitHub publication and full Android CI.
