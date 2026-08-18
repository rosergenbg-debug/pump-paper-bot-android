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

PumpSignal is an Android paper-trading and signal research application for PUMP/EUR. It never places real exchange orders. V5 compares three autonomous participants:

- APP: causal `ResearchDecisionEngine` baseline and its own V5 virtual account.
- DeepSig: independent primary AI market circuit and its own V5 virtual account. Legacy Gemini-named storage/classes remain only where needed for installed-data compatibility.
- DeepSigX: an independent quantitative flow/CVD/breathing experiment and its own V5 virtual account; it must not mirror APP or DeepSig.

SERGE is a separate user-controlled reference account, not a fourth autonomous competitor. The comparison view must show separate balances, signed percent return, entry/exit markers and trade profit/loss. Stored data must survive compatible APK updates. `Я купил` and `Я продал` record Serge's manual action and enable/disable personal-position supervision; they do not submit an exchange order.

V5 has one persistent master switch for user signal notifications, sound and vibration. It defaults OFF after the update. Switching it off must immediately silence/cancel user alerts and reminders without stopping market analysis, AI cycles, journals or any of the three virtual portfolios. Switching it on restores the existing V4.22 workday/daily/24-hour schedule and participant-specific sound tests. No missed-alert backlog may ring after re-enabling.

## Current strategy direction

- DeepSeek is the primary AI and owns the former Gemini paper-trading role: accepted BUY/EXIT decisions are executed once on its independent virtual account using a fresh quote.
- DeepSeek experiment may copy DeepSeek's executed BUY at the same price. It may also enter earlier when APP reaches 99/100 or a fresh positive DeepSeek direction is confirmed by PUMP momentum and buyer flow/CVD. Late-entry, overheat, unconfirmed rapid-drop and simultaneous BTC/SOL weakness vetoes remain mandatory. Its exit evaluates buyer flow, spot/futures CVD, BTC/SOL, open interest, order book, direction and a pullback scaled to current PUMP volatility. A moderate reversal needs two monitor cycles; a strong multi-group reversal may exit immediately. A 5% loss is only an emergency backstop.
- APP's exit timing is considered strong.
- APP historically entered too rarely. V3.19 modestly widens entry confirmation, especially in Active mode, while preserving late-entry, rapid-drop and market-overheat blocks.
- Fees are 0.15% on entry and 0.15% on exit.
- DeepSeek, DeepSeek experiment, APP and Serge remain separate. Gemini is a manual second-opinion provider only: it has no automatic cadence and no trading authority. Experimental exit rules may manage only the DeepSeek experiment portfolio.
- Pressing `Я купил` activates the highest-priority personal-position protection mode for the entire time Serge's position remains open. Protecting Serge's real position outranks routine market analysis, virtual-portfolio research and conserving AI quota or cost for a later trade/day. V4.14 removes the internal DeepSeek daily cost stop entirely for both research and position supervision; an estimated daily cost near €5 produces one informational warning only and never blocks or slows a request. Gemini position supervision may use the entire remaining provider quota for that day. Lower-priority automatic calls should yield first when a provider limit is shared. Use the available provider resources intelligently across the life of the position, with immediate checks after entry and escalation on fresh danger. Hard provider rate/quota limits, credential/network availability and local safety fallbacks still apply; exhausting a provider must never disable the free local guard or urgent alarms.
- V4.11 adds profit-adaptive supervision: below +2% DeepSeek Flash checks every three minutes; from +2% the foreground market cycle and DeepSeek Pro position analysis run every minute; from +4%, on a protected-peak pullback or fresh seller takeover, Pro uses maximum reasoning. Position output must separately explain the decision, 20-level book, executed PUMP flow, live BTC flow and the concrete invalidation/watch condition. A single book wall is never sufficient evidence because it may be spoofed.
- V4.13 adds one user-action scale. Before Serge buys, DeepSeek readiness 1–10 is red/yellow/green and can intensify the primary circuit to one minute or Pro; after Serge buys, exit danger 1–10 reverses to green/yellow/red. This is a conservative display/scheduling fusion layer only, never an automatic Serge trade. Virtual exits always notify even during Serge's open position; virtual BUY alerts remain muted then.
- V4.14 removes the internal DeepSeek cost stop, retaining only one non-blocking warning near €5/day. Its critical overview graphically separates action level, direction, 20-level book, live PUMP executions, spot/futures flow, Bitcoin and open interest; it is display-only and cannot override hard entry vetoes or execute Serge trades. The main chart starts at 60 visible candles, and four-account trade connectors route wins above and losses below the price line.
- V4.15 persists public live PUMP/BTC flow and book observations for 24 hours and supplies robust instant/5m/15m/30m/1h/6h breathing scores. Closed 30-minute APP late-entry/overheat flags are context only for the independent DeepSeek circuit and cannot freeze an intrabar DeepSeek BUY. The experiment uses a faster breathing score capped within 15 points of normal DeepSeek. A rejected proposal remains visible but never executes or anchors the experiment. APP trend mode accompanies the winner after first reaching +8% and waits for a later confirmed pullback instead of selling on the same rising candle.
- V4.16 adds a no-new-screen Pump.fun fundamentals layer and a local 50-MiB verified DeepSeek evidence memory. Predictions are frozen before outcomes and evaluated at 15m/1h/3h/6h/24h. Promotion requires 30 independent cases, >=60% directional precision, positive expectancy after 0.30% round-trip fees, lift over baseline and walk-forward confirmation. Memory/fundamentals are context only and cannot execute a trade or bypass safety rules.
- V4.17 removes distant preparation calls. DeepSeek entry notifications start at 7/10 (yellow) and repeat only on increases to 8/9/10, with 9–10 green. Ordinary sound is limited to Monday, Tuesday, Thursday and Friday, 06:15–23:00 local time; outside it messages are silent. Only an urgent exit for Serge's already-open position may ring around the clock. The selected Android alarm melody is user-configurable.
- V4.19 carries the unreleased V4.18 stabilization forward. Position warnings notify only on the first confirmed exit or material new deterioration, with oscillation protection and a ten-minute ordinary repeat floor. An isolated APP sell is warning-only; red 9–10/10 requires DeepSeek plus fresh multi-group confirmation or a real local emergency. A strong live 15s/60s/5m buyer-confirmed rebound clears a contradictory EXIT immediately; moderate recovery must persist for two control cycles before clearing it. A past alarm cannot keep the scale at 10/10 after its condition disappears. BTC is a regime filter rather than a minute-by-minute synchronization gate. No participant may auto-exit or cap profit at +8%; trend winners use confirmed pullback/trailing evidence and may continue beyond +20%.
- V4.21 keeps APP as the stable pilot while restoring independent DeepSeek authority on its own account. APP-confirmed entries remain immediate; DeepSeek may enter with APP below 55/100 only after two separate strong 30–90-minute AI evaluations confirmed by 5/15-minute flow and no APP sell. A DeepSeek-only normal exit likewise needs two AI evaluations plus fresh selling and 15/30/60-minute weakness. The experiment uses a stable 15-minute signal anchor, three entry confirmations, a 30-minute ordinary hold, three exit confirmations and a 30-minute re-entry pause. Virtual BUY and SELL trades all notify, including while Serge is in a position, but ordinary sound still obeys the agreed work schedule. A position-adviser response must re-check the current position id before saving; responses that finish after sale or replacement are discarded and their old notifications cleared.
- V4.22 separates sound policy by event importance. Executed APP, DeepSeek and DeepSeek-experiment BUY/SELL trades ring every day from 06:15 to 23:00 even when preparatory alerts remain in workday mode. Preparatory alerts may be workdays, daily daytime or 24 hours according to the saved setting; the setting must not be reset when its screen opens. Urgent confirmed Serge exits ring around the clock. V4.22 uses fresh selectable channel ids and provides an explicit sound test for each participant channel.
- V5.0 replaces the old APP threshold path with the causal research baseline and starts three new isolated €1 000 paper portfolios for APP, DeepSig and DeepSigX; V4.22 data remains archived. Candidate output is analytical evidence for manual review, never a profitability claim or real-money order. Fresh V5.0 notification channels and the master switch above apply before every user alert, including urgent Serge alerts.
- V5.1 adds Bitpanda Fusion strictly as a read-only execution-venue evidence layer and a fourth isolated €1 000 `DEEPSIG FUSION` paper account. Store its key only through Android Keystore, require users to create a Read-only key, never implement Fusion order/cancel/transfer calls, and fail closed when venue data is stale. The unified export must omit secrets and combine the four autonomous agents' decisions/trades in one sanitized log.
- V5.1.1 fixes the comparison interaction: all five displayed accounts (four autonomous agents plus Serge reference) share the available screen height with equal weights. Do not reintroduce a vertical ScrollView around gesture-handling charts; their horizontal offsets remain synchronized.
- V5.2 treats an open `DEEPSIG FUSION` paper position as the highest-priority virtual research position. It forces the primary DeepSig and its trade verification to Pro, targets a one-minute foreground cycle, supplies Fusion entry/bid/ask/spread/fees/net PnL/peak/pullback to the model, and continues supervision even if the main DeepSig paper position has already closed. It remains financially and logically separate from Serge's manual position; no real Fusion order is implemented.
- V5.3 makes Serge's manual-position card action-first and explanatory: it must show a deterministic hold/watch/confirmed-exit command, urgency, trend, risk, 30–90 minute scenario and explicit invalidation while preserving the multi-group exit confirmation and hard emergency precedence. The BTC/PUMP relation is a probabilistic regime only; never encode a permanent inverse rule or an automatic BTC-sideways catch-up assumption.

## Release invariants

- Application id must remain `com.example.pumppaperbot.v8`.
- The installed compatible line uses certificate SHA-256 beginning `1f778c4291c9` and ending `27dc7823`.
- Never give Serge a GitHub intermediate APK as an installable update unless its signing certificate has been compared with the compatible line.
- The user-facing download must be one direct `.apk` link, not a ZIP or artifact directory.
- Never delete the installed app during an update; doing so loses local state.
- Increase both `versionName` and `versionCode` exactly once for a new release and keep UI title, workflow checks, artifact name and README consistent.

## Mandatory collaboration and repository order

- Serge has granted standing authorization to publish future PumpSignal version branches and source changes to the public `rosergenbg-debug/pump-paper-bot-android` repository, open/update pull requests, and run GitHub Actions without asking for a new per-version confirmation. This authorization is limited to this repository and the agreed PumpSignal work; release signing, destructive cleanup, credential handling and unrelated external actions remain subject to their existing safeguards.
- Every project must keep exactly these two coordination records at the repository root: `AGENTS.md` for durable rules/current truth and `DEVELOPMENT_LOG.md` for chronological work history. If either is missing, create it before material work. Every agent must read both files completely before planning or editing.
- `main` is the only canonical development line. Inspect existing branches and pull requests before creating anything. Reuse an already active branch for the same release/task; never create a parallel version branch merely because another agent or chat started the work.
- A temporary branch is allowed only for an active, bounded change. Merge it into `main` after verification, then delete the temporary branch. Do not leave finished draft PRs or abandoned agent branches behind. Never force-push or delete unmerged work unless Serge explicitly authorizes repository cleanup and the retained commit/tag has first been verified.
- Update `DEVELOPMENT_LOG.md` in the same branch and commit as each material code, configuration, build, release or repository operation. Each entry must say: date/time and agent, what changed, why, exact files/branch/commit/release affected, verification performed, and any remaining risk or next step. Do not record secrets, API keys, signing-key material, private payloads or chain-of-thought.
- Keep `AGENTS.md` concise and current: update it when a durable product rule, architecture invariant, workflow rule or active-version fact changes. Do not use it as a second chronological log. When the log becomes long, retain it as history rather than starting an unlinked replacement.
- A completed version must exist on `main`, have an immutable `vX.Y` tag, and be published as a GitHub Release. The Release must contain one compatible final APK named `PumpSignal-VX.Y-Compatible-FINAL.apk` plus checksum/signature facts in the notes. Never store an APK as Base64 chunks or commit build outputs to the source tree.
- Keep only the latest two user-facing releases/APKs readily available. Older temporary build artifacts, redundant release branches and finished agent branches may be removed only after confirming that their source history is retained by `main` or an immutable tag. Source history itself must not be rewritten merely to save space.
- Before giving Serge a download, verify the final APK itself: package id, version name/code, launch activity, ZIP integrity, signature schemes and compatible certificate fingerprint. Then provide both (1) a clickable local `sandbox:` link when the file exists in the current workspace and (2) the direct GitHub Release asset link for durable download. Never offer an expiring Actions artifact or an intermediate/debug-signed APK as the installable update.
- At the end of every task, leave one clear handoff: canonical branch/commit, tests/build result, direct artifact link if applicable, and the exact unfinished items. If a requested upload or deletion could not be performed, state that plainly instead of inventing a location or claiming success.

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
- V4.8: two-minute primary DeepSeek cadence, independent high-reasoning trade verification, confirmed intrabar entry and a $0.50 daily safety ceiling, code 63, current work.
- V4.9: fresh/slipped paper execution, bounded independent entry reminders, post-buy alert suppression with uninterrupted virtual competition, local peak protection, faster DeepSeek position supervision and quota-aware Gemini position/news supervision, code 64, current work.
- V4.10: selectable 1m/5m/15m/30m/1h display chart, 15-second live edge, one-time automatic 1m selection after `Я купил`, and maximum provider-resource priority for Serge's open position, code 65, current work. StrategyV2 remains on closed 30-minute candles.
- V4.11: profit-adaptive 3m Flash / 1m Pro position supervision, max reasoning from +4% or danger, richer PUMP/BTC micro-flow and 20-level book evidence, and a detailed position-support card, code 66, current work.
- V4.12: the four-account comparison adds a display-only live 30-minute edge and uses the fresh visible price so a Serge BUY/SELL marker and account state appear without waiting for the next closed strategy candle, code 67, current work.
- V4.13: large adaptive DeepSeek entry/exit scale, one-minute yellow-zone monitoring, Pro preference for confirmed green entry, separate DeepSeek preparation alerts and explicit always-on virtual exit alerts, code 68, current work.
- V4.14: no internal DeepSeek daily cost stop, one informational ≈€5 warning, critical multi-factor overview, closer 60-bar main chart and separated win/loss connector lanes, code 69, current work.
- V4.15: persistent robust live-market breathing, independent intrabar DeepSeek, visible proposal/verification/execution states, bounded experiment sensitivity and APP winner accompaniment, code 70, current work.
- V4.16: internal Pump.fun migrations/BOOST, volume, revenue, buyback/burn context; verified 50-MiB DeepSeek outcome memory; clearer short/long Russian scenarios and conflict-aware Pro escalation, code 71, current work.
- V4.17: 7/8/9/10 DeepSeek entry steps, attributed trade reasons, fixed Mon/Tue/Thu/Fri 06:15–23:00 ordinary ringing, silent off-hours messages, round-the-clock urgent Serge exits and selectable alarm sound, code 72, current work.
- V4.18: local-only intermediate checkpoint, never built or released, code 73.
- V4.19: V4.18 stabilization plus immediate strong-rebound cancellation and two-cycle moderate-recovery hysteresis for stale EXIT alarms, code 74, current work.
- V4.20: local-only APP-led checkpoint, never built or released, code 75.
- V4.21: dual APP/DeepSeek authority with two-cycle independent AI confirmation and a calmer 30-minute experiment regime, code 76, current work.
- V4.22: reliable ringing with separate executed-trade delivery, persistent workday/daily/24-hour modes and four participant-channel tests, code 77, current work.
- V5.0: causal three-system paper research, analytical candidates, master alert switch default OFF and fresh V5 notification channels, code 78, current work.
- V5.1: Bitpanda Fusion read-only market evidence, separate DeepSig Fusion paper account and sanitized unified four-agent log, code 79, current work.
- V5.1.1: five simultaneously visible comparison charts without nested vertical scrolling, code 80, current work.
- V5.2: one-minute DeepSig Pro priority supervision for an open FusionSim position, code 81, completed.
- V5.3: detailed personal-position adviser with BTC/PUMP regime analysis, code 82, current work.

## Accumulated next-release backlog

- Record Serge's small, non-urgent corrections here as soon as they are agreed, instead of relying on chat memory or producing a separate APK for every minor issue.
- Before scoping the next release, review every open item in this section, implement the compatible items together, add focused verification, and then remove or mark each completed item while recording the result in `DEVELOPMENT_LOG.md`.
- Completed in V4.16 — internal Pump.fun fundamentals, 50-MiB verified outcome memory, statistical promotion/demotion, existing-screen capacity choice, concise Russian short/long scenarios and conflict-aware Pro escalation. These layers remain non-authoritative context and preserve all existing safety/trade-verification boundaries.
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
