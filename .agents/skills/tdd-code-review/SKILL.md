---
name: tdd-code-review
description: Use for behavior changes, refactors, risky logic, persistence, trading calculations, parsers, services, and bug fixes where tests can prove the intended behavior. Combines pragmatic red-green-refactor with a focused review for regressions, scope creep, and violations of PUMP invariants.
---

# Pragmatic TDD + Focused Code Review

Use tests as proof, not as ceremony. Do not force TDD onto documentation-only or purely cosmetic work where a test would add no value.

The owner instruction, `AGENTS.md`, `X/`, project-memory, and established repository behavior take precedence.

## Before implementation

1. State the behavior that must change or remain invariant.
2. Find the closest existing test/replay/check and reuse its style where possible.
3. Decide the cheapest proof that would fail for the old wrong behavior and pass for the intended behavior.

## Red → Green → Refactor

### RED

Create or identify a focused failing test/check when feasible.

Good targets include:
- parsing and large/bounded input handling;
- state persistence and migration;
- fee/NET calculations;
- unit/currency/market-pair conversion;
- service stage independence;
- UI state that previously disappeared or reset;
- deterministic strategy/replay invariants;
- regressions previously seen in released versions.

Confirm the failure is for the expected reason. A broken test setup is not a useful red state.

### GREEN

Make the smallest correct implementation change that satisfies the behavior.

Do not:
- rewrite unrelated modules;
- loosen assertions merely to make tests pass;
- change production semantics to fit an incorrect test;
- silently update strategy thresholds or protected X logic as part of unrelated work.

### REFACTOR

Only after the proof is green:
- remove duplication directly exposed by the change;
- improve names/structure only where it reduces risk or clarifies the changed path;
- keep the diff narrow.

## PUMP research tests

For trading/research changes, a deterministic causal replay can be the test.

The comparison must preserve:
- identical data window unless the experiment explicitly changes it;
- fee model and execution semantics;
- causality/no look-ahead;
- protected baseline/canary reproduction;
- metrics required by `AGENTS.md`: `fills`, `trades/day`, `WR`, `Avg NET`, `PF`, `TP/STOP/TIME`.

A higher WR with worse expectancy is not a passing test for an economic improvement.

## Focused code review before completion

Review the final diff as if it came from another engineer.

Check:
1. Does every changed file belong to the request?
2. Is there an unsupported assumption or hidden fallback?
3. Is state duplicated or owned in two places?
4. Could existing user data/history/settings be reset or orphaned?
5. Could lifecycle/threading/background execution change unintentionally?
6. Are price, pair, currency, units, fees, timestamps, or causality mixed?
7. Did a local feature replace/hide established UI or runtime behavior?
8. Did any safety/hard veto become bypassable?
9. Are failures observable rather than silently swallowed?
10. Do tests prove the important behavior rather than implementation trivia?

Fix material findings before declaring completion, then invoke `verification-before-completion`.