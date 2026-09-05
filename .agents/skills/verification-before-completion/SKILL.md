---
name: verification-before-completion
description: Use immediately before claiming a PUMP task is done, fixed, tested, safe, release-ready, or improved. Requires fresh evidence from relevant tests/checks/builds, diff review, invariant checks, and release/research verification appropriate to the task.
---

# Verification Before Completion

Never convert "looks correct" into "done" without evidence.

Use this skill before any completion claim such as fixed, working, tested, safe, release-ready, compatible, profitable, improved, or ready to install.

## 1. Match verification to the claim

Examples:
- "bug fixed" -> reproduce old failure and show it no longer occurs;
- "tests pass" -> run the relevant tests now;
- "build works" -> run the actual build now;
- "no regression" -> run the relevant regression checks and inspect the diff;
- "APK ready" -> verify package/version/signing/launcher/artifact integrity as required by project rules;
- "strategy improved" -> reproduce the protected canary and compare the required metrics on a fair causal replay.

Do not cite an old successful run as proof for a new change unless nothing relevant changed and that limitation is explicit.

## 2. Verify the final tree, not an intermediate state

After the last code change:
- inspect the final diff/status;
- ensure no temporary debug code, secrets, generated junk, disabled assertions, or accidental file replacements remain;
- rerun checks affected by the final edit.

## 3. PUMP invariant check

As relevant to the task, confirm:
- owner data/history/settings remain preserved;
- `СЕРЖ` and `APP` continuity remains intact;
- `applicationId` and update identity are unchanged unless explicitly authorized;
- no uninstall/reset/migration shortcut was introduced;
- paper-only boundaries remain intact;
- UI/runtime elements unrelated to the task were not hidden or replaced;
- independent service stages remain independent;
- fee, price, pair, currency, timestamp, and causal semantics remain correct;
- hard veto/safety rules were not weakened.

## 4. Research verification

For strategy/replay work:
- reproduce the relevant X canary first;
- compare identical windows and execution assumptions unless the experiment explicitly changes them;
- report at minimum `fills`, `trades/day`, `WR`, `Avg NET`, `PF`, `TP/STOP/TIME`;
- distinguish exploration from a promoted checkpoint;
- never call a higher WR alone an improvement;
- do not overwrite older X checkpoints.

## 5. Release verification

For an APK/release task, verify all checks required by current project-memory, including the current package/version/launcher/signing requirements. Read `CURRENT_STATE.md` and `SIGNING-RECOVERY.md` instead of relying on hard-coded version numbers in this skill.

Never create a replacement signing key or expose signing secrets.

## 6. Honest completion state

End in exactly one of these states conceptually:
- **Verified:** required checks were actually run and passed.
- **Partially verified:** some checks passed, but named checks could not be run.
- **Not verified:** implementation exists but evidence is insufficient.

Do not hide skipped or unavailable verification behind confident wording.