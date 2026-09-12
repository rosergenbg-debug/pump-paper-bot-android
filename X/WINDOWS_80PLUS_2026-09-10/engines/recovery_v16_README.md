# V16 recovery checkpoint — 2026-09-09

Status: compiled modules recovered, editable source not recovered, V17 not built.

## Preserved implementation

34 project modules were extracted from the V16 PyInstaller PYZ archive without
importing or executing their code. These Python 3.13 compiled files preserve the
implementation, but are NOT a maintainable source release. `manifest.json`
records the archive SHA-256 and individual compiled-module hashes.

`inspect_archive.py` reproduces extraction using Python 3.13 and PyInstaller.
It also generates recursive disassembly for source reconstruction. Disassembly is
an aid, not evidence that reconstructed code behaves identically.

Original working application: `F:\PUMP Research Lab\PumpResearchLab-V16.0.0.exe`.
Original build: `F:\PUMP Research Lab\build-v16\PumpResearchLab-V16.0.0`.
The original application and research data have not been modified by recovery.

## Data locations

- Raw and prepared market data: `F:\PUMP Research Lab\data`.
- V16 experiment: `F:\PUMP Research Lab\experiments\v16-59b126eb7bd78c8c`.
- Database: `research.sqlite3`, with SQLite WAL files while active.
- Reports: `F:\PUMP Research Lab\reports\ready`.
- Strategies/models: `F:\PUMP Research Lab\models`.
- Checkpoints: `F:\PUMP Research Lab\checkpoints`.

Some old prepared manifests retain absolute C: paths. Do not blindly rewrite
them or redownload data; resolve existing files and verify checksums first.
Do not copy a live SQLite main file alone: use SQLite online backup or a safely
closed database. Market datasets, research databases and secrets are not uploaded.

## Recovery and V17 acceptance

1. Recover editable source; retain the extracted V16 as the comparison reference.
2. Compare deterministic results with V16 on identical data/configurations.
3. Keep legacy strategies and checkpoints untouched, with explicit version IDs.
4. Implement V17 in a separate versioned path and use isolated experiment storage.
5. Remove owner-revoked trade-frequency quotas from V17, not legacy fixtures.
6. Keep physical execution and data-integrity constraints; model fees and gaps.
7. Use all verified available continuous history and causal walk-forward checks.
8. Treat NET >=20% as a display filter only; retain negative and partial results.
9. Add version filtering, numeric header sorting and a dark non-blue interface.
10. Test pause/restart, archive retention, causality and execution before packaging.

No claim of profitable strategy, recovered editable source, or working V17 is
made by this checkpoint. The initial protected X baseline remains unchanged.
