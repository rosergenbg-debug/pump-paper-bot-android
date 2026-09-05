---
name: pump-development
description: Use for substantial work on PUMP/PumpBot. Restores project context, classifies the task, selects the relevant project skills, protects X checkpoints and owner state, preserves paper-only/update continuity, and ensures project-memory is updated after meaningful changes.
---

# PUMP Development Orchestrator

This is the project-specific coordinating skill. It does not replace `ИНСТРУКЦИЯ_1.md` or `AGENTS.md`; it operationalizes them.

## 1. Restore project context before substantial work

Follow the mandatory startup chain:

1. `ИНСТРУКЦИЯ_1.md`
2. `AGENTS.md`
3. `X/README.md`
4. newest relevant files in `X/` for trading/research work
5. relevant `docs/project-memory/*`
6. affected code/tests and recent Git history

Do not use chat history as the primary project memory.

## 2. Classify the task

Choose the dominant class before editing:

- **Bug/regression** -> use `systematic-debugging`.
- **Behavior/refactor/risky logic** -> use `karpathy-guidelines` and usually `tdd-code-review`.
- **Trading/replay/research** -> restore X canary first, then use `tdd-code-review` with deterministic causal replay.
- **Release/build/signing/install continuity** -> use `verification-before-completion` and current signing/project-memory rules.
- **Documentation-only** -> keep the change narrow; do not invoke heavy implementation workflows unnecessarily.

Multiple skills may apply, but load only what materially helps the task.

## 3. Protect the architecture and owner state

Before implementation, identify what must not change.

Always preserve unless the owner explicitly authorizes otherwise:
- owner histories, settings, account identities, and persistent stores;
- `СЕРЖ` and `APP` continuity;
- application/update identity;
- established launcher/runtime/UI not directly targeted by the request;
- paper-only/research-only boundary;
- independent background/service stages;
- hard safety/data-freshness/executable-price/spread/late-chase/seller-takeover protections;
- protected X research ladder.

Never replace the central app/runtime merely to add one experiment or dashboard feature.

## 4. Trading/research protocol

Before changing strategy authority or evaluating a new idea:

1. read the relevant X checkpoint;
2. reproduce the protected canary on the appropriate data/mechanics;
3. define exactly one experiment or clearly enumerate independent variables;
4. preserve causal/no-look-ahead semantics;
5. preserve fees and execution assumptions unless the experiment explicitly changes them;
6. compare `fills`, `trades/day`, `WR`, `Avg NET`, `PF`, `TP/STOP/TIME`;
7. distinguish statistical curiosity from economic improvement;
8. never promote a result solely because WR increased;
9. keep old X checkpoints intact.

A new best checkpoint belongs in `X/` only after it is reproducible and materially better under the project criteria.

## 5. Implementation protocol

- Establish a verifiable goal.
- Prefer the smallest architecturally correct change.
- Keep experiments isolated from established behavior.
- Avoid unrelated cleanup or rewrites.
- Add regression protection where feasible.
- If the same area reaches the third consecutive repair, apply the `AGENTS.md` rule of three repairs before patching again.

## 6. Completion protocol

Before saying the task is finished:

1. invoke `verification-before-completion`;
2. run relevant tests/checks/build after the final edit;
3. inspect the final diff for accidental scope expansion;
4. verify affected project invariants;
5. update `CURRENT_STATE.md` for meaningful state changes;
6. update `DECISIONS.md` for meaningful architectural/product decisions;
7. update `REGRESSION_MATRIX.md` when a new guarantee or regression guard exists;
8. add only a concise substantial entry to `AI_CHANGELOG.md`;
9. update `X/` only for a genuinely established research checkpoint, without deleting prior checkpoints.

## 7. Skill evolution

If a recurring workflow, repeated failure pattern, or new capability would be better handled by a reusable skill, follow the skill-governance section in `AGENTS.md`: tell the owner what skill should be created or changed and why. Do not silently accumulate unnecessary skills.