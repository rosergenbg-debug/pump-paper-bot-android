# PumpSignal working memory

This file is mandatory context for every agent working in this repository.

## Required startup sequence

1. Read this file completely.
2. Read `DEVELOPMENT_LOG.md` completely.
3. Inspect the actual branch, `git status`, version name/code, package id, signing configuration and recent commits.
4. Preserve unrelated worktree changes. Never assume `main` is current; the project has historically advanced through version branches.
5. After every material code, build, release or repository change, append an accurate entry to `DEVELOPMENT_LOG.md`.
6. Do not ask Serge to repeat old chat history when these files and the repository answer the question.

## Product and user intent

PumpSignal is an Android paper-trading and signal research application for PUMP/EUR. It never places real orders. Its purpose is to compare four independent participants:

- APP: built-in StrategyV2 virtual account.
- Gemini: independent hourly AI research circuit and virtual account.
- Gemini experiment: a separate virtual account that can mirror Gemini's executed BUY or enter earlier from a signed APP/Gemini signal confirmed by buyer flow/CVD, then tests a market-evidence exit without changing the control Gemini.
- SERGE: user-controlled virtual account.

All four must keep separate balances, trades and performance. The comparison view must show current money, signed percent return, entry/exit markers and trade profit/loss. Stored data must survive compatible APK updates.

Serge values timely, unmistakable phone alerts. APP and Gemini must each generate their own loud notification for an executed entry and exit. Do not let one participant's notification replace the other's. Quiet hours apply to ordinary preparatory signals, but actual requested trade alerts are intended to be delivered immediately.

## Current strategy direction

- Gemini's entry timing is considered strong and should not be weakened.
- Gemini historically held too long, but remains the unchanged control for the exit experiment.
- Gemini experiment may copy Gemini's executed BUY at the same price. From V3.20 it may also enter earlier when APP reaches 99/100 or a fresh positive Gemini direction is confirmed by PUMP momentum and buyer flow/CVD. Late-entry, overheat, unconfirmed rapid-drop and simultaneous BTC/SOL weakness vetoes remain mandatory. Its exit evaluates buyer flow, spot/futures CVD, BTC/SOL, open interest, order book, direction and a pullback scaled to current PUMP volatility. A moderate reversal needs two monitor cycles; a strong multi-group reversal may exit immediately. A 5% loss is only an emergency backstop.
- APP's exit timing is considered strong.
- APP historically entered too rarely. V3.19 modestly widens entry confirmation, especially in Active mode, while preserving late-entry, rapid-drop and market-overheat blocks.
- Fees are 0.15% on entry and 0.15% on exit.
- Gemini, Gemini experiment, APP and Serge remain separate. Experimental exit rules may manage only the Gemini experiment portfolio.

## Release invariants

- Application id must remain `com.example.pumppaperbot.v8`.
- The installed compatible line uses certificate SHA-256 beginning `1f778c4291c9` and ending `27dc7823`.
- Never give Serge a GitHub intermediate APK as an installable update unless its signing certificate has been compared with the compatible line.
- The user-facing download must be one direct `.apk` link, not a ZIP or artifact directory.
- Never delete the installed app during an update; doing so loses local state.
- Increase both `versionName` and `versionCode` exactly once for a new release and keep UI title, workflow checks, artifact name and README consistent.

## Current version chain

- V3.17: installed predecessor, code 49.
- V3.18: three-way APP/Gemini/Serge competition, code 50, compatible update over V3.17.
- V3.19: trade alerts and entry/exit tuning, code 51, current work.
- V3.20: attributed signals and confirmed early entry for Gemini experiment, code 52, current work.
- V3.21: APP confirmed-trend continuation entry, code 53, current work.
- V3.22: full audit fixes for durable trade-alert delivery, independent APP readiness, storage recovery and current UI text, code 54, current work.
- V4.0: protected Gemini/DeepSeek key UX and DeepSeek Flash/Pro supervision of Serge's open position, code 55, current work.
- V4.1: DeepSeek Flash primary market/news circuit, DeepSeek Pro position reserve and 50/50 Gemini daily quota split, code 56, current work.
- V4.2: dedicated DeepSeek/Gemini API centers, DeepSeek signal gauge, five-minute rich-market analysis and two-hour routine Gemini cadence, code 57, current work.
- V4.3: freshness-labelled live price, 15-second spot trade/top-book stream and closed 5-minute spot/futures flow supplied to DeepSeek without changing StrategyV2 or paid-call cadence, code 58, current work.
- V4.4: finish-reason-aware DeepSeek JSON recovery, economical reasoning, stale-signal suppression and shareable redacted diagnostics, code 59, current work.
- V4.5: copyable/selectable diagnostics with per-version API telemetry separation, code 60, current work.

## Accumulated next-release backlog

- Record Serge's small, non-urgent corrections here as soon as they are agreed, instead of relying on chat memory or producing a separate APK for every minor issue.
- Before scoping the next release, review every open item in this section, implement the compatible items together, add focused verification, and then remove or mark each completed item while recording the result in `DEVELOPMENT_LOG.md`.
- Open diagnostic-timestamp correction: `lastAttempt` may continue to represent request start, but `lastSuccess` must be persisted and displayed as the completion/acceptance time of the successful response. In V4.5 it currently repeats the start time, even though the API event correctly records the later completion time. This is display/telemetry accuracy only and must not change DeepSeek scheduling or trading logic.
- Do not create a standalone release solely for the timestamp correction; include it with the next suitable accumulated release unless it is found to affect runtime correctness.

## Verification before delivery

- Run unit tests and assemble the APK.
- Inspect package id, version name/code and launchable activity with Android build tools.
- Verify APK signature schemes and compare the final certificate fingerprint with the compatible line.
- Confirm notification permission remains requested on Android 13+ and that notification channels use alarm sound/high importance.
- Check the final APK directly, not merely a successful CI badge.
