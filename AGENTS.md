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
- DeepSeek: primary five-minute AI market circuit and virtual account. The legacy Gemini store is intentionally retained underneath so installed V4.6 balances, positions and history survive the ownership migration.
- DeepSeek experiment: a separate virtual account that can mirror DeepSeek's executed BUY or enter earlier from a signed APP/DeepSeek signal confirmed by buyer flow/CVD, then tests a market-evidence exit without changing the main DeepSeek account.
- SERGE: user-controlled virtual account.

All four must keep separate balances, trades and performance. The comparison view must show current money, signed percent return, entry/exit markers and trade profit/loss. Stored data must survive compatible APK updates.

Serge values timely, unmistakable phone alerts. APP, DeepSeek and DeepSeek experiment must each generate their own loud notification for an executed entry and exit. Do not let one participant's notification replace another's. Quiet hours apply to ordinary preparatory signals, but actual requested trade alerts are intended to be delivered immediately.

## Current strategy direction

- DeepSeek is the primary AI and owns the former Gemini paper-trading role: accepted BUY/EXIT decisions are executed once on its independent virtual account using a fresh quote.
- DeepSeek experiment may copy DeepSeek's executed BUY at the same price. It may also enter earlier when APP reaches 99/100 or a fresh positive DeepSeek direction is confirmed by PUMP momentum and buyer flow/CVD. Late-entry, overheat, unconfirmed rapid-drop and simultaneous BTC/SOL weakness vetoes remain mandatory. Its exit evaluates buyer flow, spot/futures CVD, BTC/SOL, open interest, order book, direction and a pullback scaled to current PUMP volatility. A moderate reversal needs two monitor cycles; a strong multi-group reversal may exit immediately. A 5% loss is only an emergency backstop.
- APP's exit timing is considered strong.
- APP historically entered too rarely. V3.19 modestly widens entry confirmation, especially in Active mode, while preserving late-entry, rapid-drop and market-overheat blocks.
- Fees are 0.15% on entry and 0.15% on exit.
- DeepSeek, DeepSeek experiment, APP and Serge remain separate. Gemini is a manual second-opinion provider only: it has no automatic cadence and no trading authority. Experimental exit rules may manage only the DeepSeek experiment portfolio.

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
- V4.6: provider-parity diagnostics, manual self-diagnostics, accurate completion timestamps and Russian-only visible AI output, code 61, current work.
- V4.7: DeepSeek owns both former Gemini AI trading roles; Gemini becomes manual-only second opinion, code 62, current work.

## Accumulated next-release backlog

- Record Serge's small, non-urgent corrections here as soon as they are agreed, instead of relying on chat memory or producing a separate APK for every minor issue.
- Before scoping the next release, review every open item in this section, implement the compatible items together, add focused verification, and then remove or mark each completed item while recording the result in `DEVELOPMENT_LOG.md`.
- Completed in V4.6 — diagnostic timestamp: `lastAttempt` continues to represent request start, while `lastSuccess` is persisted and displayed as the completion/acceptance time of the successful response. This remains display/telemetry accuracy only and does not change DeepSeek scheduling or trading logic.
- Do not create a standalone release solely for the timestamp correction; include it with the next suitable accumulated release unless it is found to affect runtime correctness.
- Completed in V4.6 — provider-diagnostics parity: Gemini has the same practical diagnostics UX that DeepSeek has, including equivalent log/status panels and copy/share actions. DeepSeek and Gemini remain separate providers in the report so their requests, responses, timing, models, errors, retries/repairs, quota state and current-version totals can be audited independently.
- The manual provider check for both DeepSeek and Gemini must run an expanded self-diagnostic, not merely a connectivity ping. It must verify the provider/API path, selected model and response parsing, current market-data freshness/availability, relevant scheduler/circuit state, telemetry persistence and the provider's other implemented runtime prerequisites. Record a clear PASS/WARN/FAIL result for every check and append the run to the provider log.
- The copied redacted report must be sufficiently complete for another AI or developer to audit observable behaviour: include sanitized input/context summaries, executed stages, accepted action/output and reason/evidence fields, timestamps/durations, token usage, finish state, errors and recovery attempts, plus the self-diagnostic results. Do not claim or attempt to expose a model's private chain-of-thought; use observable decision traces and concise model-supplied reasons instead. Continue excluding API keys, authorization data and full sensitive request payloads.
- Implement the provider-diagnostics parity and expanded self-diagnostic together in a future accumulated release. Do not change either provider's trading authority, strategy thresholds or paid automatic cadence merely to add diagnostics, and do not create a standalone APK until the accumulated release is deliberately scoped.
- V4.6 completed the accumulated items above after unit tests, Android compilation, direct APK inspection and compatible final signing passed.
- Permanent language invariant: all user-visible AI conclusions, reasons, evidence, risks, errors and copied diagnostics must be in Russian. DeepSeek/Gemini prompts must demand Russian, runtime validation must reject Han-script output, and persisted legacy text containing Han characters must be hidden behind a Russian explanatory placeholder rather than displayed.

## Verification before delivery

- Run unit tests and assemble the APK.
- Inspect package id, version name/code and launchable activity with Android build tools.
- Verify APK signature schemes and compare the final certificate fingerprint with the compatible line.
- Confirm notification permission remains requested on Android 13+ and that notification channels use alarm sound/high importance.
- Check the final APK directly, not merely a successful CI badge.
