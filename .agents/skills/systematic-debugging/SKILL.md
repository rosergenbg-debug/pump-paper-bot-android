---
name: systematic-debugging
description: Use whenever PUMP crashes, behaves incorrectly, regresses, becomes flaky, loses UI/runtime behavior, or produces suspicious data. Requires reproduction, evidence, root-cause isolation, a minimal fix, a regression test when feasible, and verification of nearby invariants.
---

# Systematic Debugging

Use this skill for bugs, crashes, wrong calculations, broken UI behavior, missing runtime stages, flaky tests, data corruption risks, and regressions.

`AGENTS.md`, the owner's latest explicit instruction, `X/`, project-memory, and existing tests remain authoritative.

## Debugging sequence

### 1. Reproduce first

- Establish the exact failing behavior before editing code.
- Capture the smallest reliable reproduction: input, state, sequence, log/stack trace, failing test, or deterministic replay.
- If the bug cannot be reproduced locally, identify the strongest available evidence and explicitly mark what remains uncertain.

### 2. Find the boundary of failure

Trace the path from input to observed failure and determine where reality first diverges from expected behavior.

Check, as relevant:
- lifecycle/state ownership;
- threading/background work;
- parsing and bounded memory use;
- persistence/migration;
- network/API assumptions;
- unit conversion, market pair, currency, fees, or timestamps;
- UI visibility/state;
- service startup/continuity;
- stale or duplicated state;
- replay/live semantic drift.

Do not modify code until there is at least one evidence-backed root-cause hypothesis.

### 3. Test the hypothesis

Prefer a narrow experiment, assertion, log point, unit test, integration test, or replay that can distinguish the hypothesis from alternatives.

A plausible story is not enough. Seek evidence that would be different if the hypothesis were wrong.

### 4. Fix the cause, not the symptom

- Make the smallest architecturally correct change that removes the cause.
- Do not hide failures, swallow exceptions, widen timeouts, reset state, or bypass guards merely to make the symptom disappear.
- Do not replace established runtime/UI/components to repair one local bug.

### 5. Add a regression guard

When feasible, add or strengthen a test that fails before the fix and passes after it.

For trading/research bugs, use a deterministic causal replay or invariant check when a normal unit test cannot prove the behavior.

### 6. Check neighboring invariants

Verify that the fix did not break:
- existing screens/buttons/graphs;
- `СЕРЖ` / `APP` histories and stores;
- package/signing/update continuity;
- paper-only behavior;
- independent runtime stages;
- X-protected strategy checkpoints;
- fees, units, currencies, executable prices, timestamps, or causal data semantics.

### 7. Escalate repeated repairs

If this area is receiving its third consecutive repair, or this fix repairs consequences of the previous fix, stop patching and apply the `AGENTS.md` rule of three repairs: inspect the original assumption, state ownership, architectural placement, duplicated logic, and need for a small refactor.

## Debug report format

Before completion, be able to state:
- reproduction;
- root cause;
- evidence;
- exact fix;
- regression guard;
- verification performed;
- anything still uncertain.

Then use `verification-before-completion`.