from __future__ import annotations

import hashlib
import json
from dataclasses import asdict, dataclass
from pathlib import Path


@dataclass(frozen=True, slots=True)
class HoldoutBundle:
    source_path: str
    source_sha256: str
    development_path: str
    development_sha256: str
    development_rows: int
    holdout_path: str
    holdout_sha256: str
    holdout_rows: int
    split_open_time_ms: int
    state: str
    manifest_path: str

    def to_dict(self) -> dict:
        return asdict(self)


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def seal_immutable_holdout(dataset_path: Path, data_root: Path, holdout_fraction: float = 0.20) -> HoldoutBundle:
    if not 0.10 <= holdout_fraction <= 0.40:
        raise ValueError("Holdout fraction must be between 10% and 40%")
    dataset_path = dataset_path.resolve()
    source_sha = _sha256(dataset_path)
    key = source_sha[:20]
    development_directory = data_root / "development" / key
    holdout_directory = data_root / "immutable_test" / key
    development_path = development_directory / "development.jsonl"
    holdout_path = holdout_directory / "holdout.jsonl"
    manifest_path = holdout_directory / "holdout.manifest.json"
    if manifest_path.exists():
        value = json.loads(manifest_path.read_text(encoding="utf-8"))
        bundle = HoldoutBundle(**value)
        if bundle.source_sha256 != source_sha:
            raise ValueError("Existing holdout manifest does not match source dataset")
        if not Path(bundle.development_path).exists() or not Path(bundle.holdout_path).exists():
            raise ValueError("Sealed holdout files are missing")
        if _sha256(Path(bundle.holdout_path)) != bundle.holdout_sha256:
            raise ValueError("Immutable holdout checksum mismatch")
        return bundle

    total_rows = 0
    with dataset_path.open("r", encoding="utf-8") as handle:
        for line in handle:
            if line.strip():
                total_rows += 1
    holdout_rows = max(1, int(total_rows * holdout_fraction))
    development_rows = total_rows - holdout_rows
    if development_rows < 4_000 or holdout_rows < 1_000:
        raise ValueError("Недостаточно данных для физического development/immutable holdout разделения")
    development_directory.mkdir(parents=True, exist_ok=True)
    holdout_directory.mkdir(parents=True, exist_ok=True)
    development_partial = development_path.with_suffix(".jsonl.partial")
    holdout_partial = holdout_path.with_suffix(".jsonl.partial")
    split_open_time_ms = 0
    row_index = 0
    with (
        dataset_path.open("r", encoding="utf-8") as source,
        development_partial.open("w", encoding="utf-8", newline="\n") as development,
        holdout_partial.open("w", encoding="utf-8", newline="\n") as holdout,
    ):
        for line in source:
            if not line.strip():
                continue
            if row_index < development_rows:
                development.write(line if line.endswith("\n") else line + "\n")
            else:
                if row_index == development_rows:
                    split_open_time_ms = int(json.loads(line)["open_time_ms"])
                holdout.write(line if line.endswith("\n") else line + "\n")
            row_index += 1
    development_partial.replace(development_path)
    holdout_partial.replace(holdout_path)
    bundle = HoldoutBundle(
        source_path=str(dataset_path),
        source_sha256=source_sha,
        development_path=str(development_path),
        development_sha256=_sha256(development_path),
        development_rows=development_rows,
        holdout_path=str(holdout_path),
        holdout_sha256=_sha256(holdout_path),
        holdout_rows=holdout_rows,
        split_open_time_ms=split_open_time_ms,
        state="SEALED_UNOPENED",
        manifest_path=str(manifest_path),
    )
    partial = manifest_path.with_suffix(".json.partial")
    partial.write_text(json.dumps(bundle.to_dict(), ensure_ascii=False, indent=2), encoding="utf-8")
    partial.replace(manifest_path)
    return bundle
