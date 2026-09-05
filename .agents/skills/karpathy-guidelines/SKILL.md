---
name: karpathy-guidelines
description: Use for code, architecture, refactoring, configuration, or implementation changes in PUMP. Prevents unsupported assumptions, overengineering, scope creep, and unverified completion by requiring explicit assumptions, the smallest sufficient change, focused diffs, and a verifiable goal.
---

# Disciplined Engineering Guidelines

This is a PUMP-adapted engineering discipline skill. It does not override `AGENTS.md`, the owner's latest explicit instruction, `X/`, project-memory, or repository tests.

## Core rules

1. **Do not guess when the repository can answer.**
   - Read the relevant code, tests, project-memory, recent history, and protected X checkpoints first.
   - If something still cannot be established, state the assumption explicitly instead of silently treating it as fact.

2. **Turn the request into a verifiable goal.**
   - Define what must be true when the task is complete.
   - Prefer observable acceptance criteria: a reproduced bug no longer occurs, a test passes, a build succeeds, a metric changes as intended, or a UI element remains present.

3. **Choose the simplest sufficient solution.**
   - Avoid new abstractions, frameworks, state stores, services, or rewrites unless the existing structure cannot safely support the requirement.
   - "Simplest" means the smallest architecturally correct solution, not a shortcut that creates another patch layer.

4. **Change only what belongs to the task.**
   - Do not clean up neighboring code merely because it looks improvable.
   - Do not replace working screens, stores, services, strategies, or runtime paths to add one feature.
   - Preserve unrelated behavior unless the owner explicitly requests a broader redesign.

5. **Make the diff explainable.**
   - Every changed file should have a direct reason tied to the task, a required test, or necessary project-memory maintenance.
   - If a file cannot be justified, leave it unchanged.

6. **Prove the result.**
   - Run the relevant tests/checks/build instead of inferring success from code inspection alone.
   - Use `verification-before-completion` before saying the task is finished.

## PUMP-specific guardrails

- Keep owner data, histories, settings, account identities, package identity, and update continuity intact.
- Never alter trading strategy authority as a side effect of UI, support, networking, logging, or build work.
- For research changes, compare against the protected X baseline with the metrics required by `AGENTS.md`; a higher win rate alone is not proof of improvement.
- If this would be the third repair in the same area, apply the `AGENTS.md` rule of three repairs before adding another patch.

## Completion question

Before finishing, answer internally: **What concrete evidence proves that I solved exactly the requested problem without silently changing anything else?**