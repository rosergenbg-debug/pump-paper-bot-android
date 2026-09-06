# SOLUSDT cross-market OOS replay — February 2026

Status: **negative transfer result; research-only; main/X unchanged**.

## Question

Does the protected PUMP T32/X fixed-target line transfer unchanged to SOL itself?

## Method

The exact historical replay semantics were taken from commit `11b94eb7e73c81a6759a4c4e23665d451b6c2280`, file `tools/research_drop4_fixed_target_stop_frequency.js`.

Only the target market and evaluation window were changed:

- target: `SOLUSDT` spot 1m;
- period: `2026-02-01T00:00:00Z` through `2026-03-01T00:00:00Z`;
- 40,320 one-minute candles;
- no SOL-specific parameter fitting.

Frozen strategy semantics:

- original T32/VWAP entry;
- current price >=4% below causal rolling 12h high;
- LIMIT = signal close * 0.999;
- TTL = 2 minutes;
- fee = 0.21% each side;
- fixed TP = +2.5% NET;
- STOP = -1.2% NET;
- max hold = 90 minutes;
- max 2 filled entries per UTC day;
- adverse 0.08% slippage on STOP/TIME, same as historical X replay.

## Exact result

- signals: **62**;
- expired limits: **27**;
- fills/trades: **35**;
- trades/day: **1.25**;
- wins: **6**;
- win rate: **17.14%**;
- Avg NET: **-0.758545%**;
- Profit Factor: **0.134200**;
- compounded result: **-23.4782%**;
- max drawdown: **-23.4782%**;
- TP / STOP / TIME: **0 / 22 / 13**.

GitHub Actions run: `34059849002`.
Artifact: `9997112929` / `sol-x-exact-2026-02`.
Artifact digest: `sha256:5d62ac31d335a79766cbb178b43d0d491a8e6585f7f50272ac2e970872601af0`.

## Interpretation

This is a strong negative cross-market control. The PUMP T32/X fixed-target line is not a universal dip/rebound rule that can be copied unchanged to SOL. In this month it produced no +2.5% TP exits at all, while most fills hit the -1.2% NET stop.

The result does **not** prove that T32-style information has no value on SOL. It proves that the complete frozen PUMP parameterization/exit package does not transfer directly. Any SOL version would need separate research and out-of-sample validation rather than parameter tuning on this one month.

## Important scope note

The repository's current protected X files do not contain a checkpoint corresponding to a six-month result of approximately +100%. If such a result exists only in a Work/Codex local branch or an uncommitted report, it must be committed or otherwise supplied before that exact algorithm can be independently replayed here.
