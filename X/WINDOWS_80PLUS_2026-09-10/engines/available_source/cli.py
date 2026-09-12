from __future__ import annotations

import argparse
import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path

from .baseline_x import replay_economy, replay_vwap_canary
from .catalog import DuckDbCatalog
from .data_manager import DataManager
from .demo import demo_candles
from .domain import DatasetManifest
from .io import read_csv, read_jsonl, write_jsonl
from .reporting import write_replay_report
from .validation import validate_candles


def _timestamp(value: str) -> int:
    parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    if parsed.tzinfo is None:
        parsed = parsed.replace(tzinfo=timezone.utc)
    return int(parsed.timestamp() * 1000)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(prog="pump-lab")
    sub = parser.add_subparsers(dest="command", required=True)
    download = sub.add_parser("download", help="Download Binance 1m candles")
    download.add_argument("--root", type=Path, required=True)
    download.add_argument("--symbol", default="PUMPUSDT")
    download.add_argument("--start", required=True, help="ISO-8601 UTC")
    download.add_argument("--end", required=True, help="ISO-8601 UTC")
    update = sub.add_parser("update", help="Incrementally refresh a candle snapshot")
    update.add_argument("path", type=Path)
    update.add_argument("--root", type=Path, required=True)
    update.add_argument("--end", required=True, help="ISO-8601 UTC")
    update.add_argument("--overlap", type=int, default=120, help="Tail overlap in minutes")
    repair = sub.add_parser("repair", help="Repair candle gaps into a new snapshot")
    repair.add_argument("path", type=Path)
    repair.add_argument("--root", type=Path, required=True)
    validate = sub.add_parser("validate", help="Validate JSONL or CSV candles")
    validate.add_argument("path", type=Path)
    replay = sub.add_parser("replay", help="Run causal AUTO X ECONOMY replay")
    replay.add_argument("path", type=Path)
    replay.add_argument("--reports", type=Path, required=True)
    replay.add_argument("--from", dest="evaluation_start")
    replay.add_argument("--to", dest="evaluation_end")
    canary = sub.add_parser("canary", help="Run protected 32.65%% VWAP/T32 canary")
    canary.add_argument("path", type=Path)
    canary.add_argument("--from", dest="evaluation_start")
    canary.add_argument("--to", dest="evaluation_end")
    normalize = sub.add_parser("normalize", help="Validate and write Parquet/DuckDB catalog")
    normalize.add_argument("path", type=Path)
    normalize.add_argument("--root", type=Path, required=True)
    demo = sub.add_parser("demo", help="Run deterministic synthetic vertical path")
    demo.add_argument("--work", type=Path, required=True)
    return parser


def _read(path: Path):
    return read_csv(path) if path.suffix.lower() == ".csv" else read_jsonl(path)


def _manifest(path: Path, candles, checksum: str) -> DatasetManifest:
    report = validate_candles(candles)
    if not report.valid:
        raise ValueError("Cannot publish invalid data to the catalog")
    first = candles[0]
    dataset_id = f"{first.source}-{first.symbol}-{first.interval}-{checksum[:12]}"
    return DatasetManifest.create(dataset_id, first.symbol, first.interval, report, checksum, str(path.resolve()))


def _normalize(path: Path, root: Path):
    candles = _read(path)
    if not candles:
        raise ValueError("Cannot normalize an empty dataset")
    checksum = hashlib.sha256(path.read_bytes()).hexdigest()
    manifest = _manifest(path, candles, checksum)
    parquet = root / "normalized" / candles[0].symbol / candles[0].interval / f"{manifest.dataset_id}.parquet"
    DuckDbCatalog(root / "catalog" / "pump.duckdb").register_candles(candles, manifest, parquet)
    return candles, manifest, parquet


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    if args.command == "download":
        raw, manifest = DataManager(args.root).download_candles(args.symbol, _timestamp(args.start), _timestamp(args.end))
        print(json.dumps({"raw": str(raw), "manifest": str(manifest)}, ensure_ascii=False))
        return 0
    if args.command == "update":
        raw, manifest, state = DataManager(args.root).update_candles(
            args.path, _timestamp(args.end), args.overlap
        )
        print(json.dumps({"raw": str(raw), "manifest": str(manifest), "job": str(state)}, ensure_ascii=False))
        return 0
    if args.command == "repair":
        raw, manifest, state = DataManager(args.root).repair_gaps(args.path)
        print(json.dumps({"raw": str(raw), "manifest": str(manifest), "job": str(state)}, ensure_ascii=False))
        return 0
    if args.command == "validate":
        report = validate_candles(_read(args.path))
        print(json.dumps(report.to_dict(), ensure_ascii=False, indent=2))
        return 0 if report.valid else 2
    if args.command == "replay":
        candles = _read(args.path)
        checksum = hashlib.sha256(args.path.read_bytes()).hexdigest()
        result = replay_economy(
            candles,
            evaluation_start_ms=_timestamp(args.evaluation_start) if args.evaluation_start else None,
            evaluation_end_ms=_timestamp(args.evaluation_end) if args.evaluation_end else None,
        )
        paths = write_replay_report(result, args.reports, checksum)
        print(json.dumps({"reports": [str(path) for path in paths], "metrics": result.to_dict()["metrics"]}, ensure_ascii=False))
        return 0
    if args.command == "canary":
        candles = _read(args.path)
        result = replay_vwap_canary(
            candles,
            evaluation_start_ms=_timestamp(args.evaluation_start) if args.evaluation_start else None,
            evaluation_end_ms=_timestamp(args.evaluation_end) if args.evaluation_end else None,
        )
        print(json.dumps(result.to_dict(), ensure_ascii=False))
        return 0
    if args.command == "normalize":
        candles, manifest, parquet = _normalize(args.path, args.root)
        print(json.dumps({"rows": len(candles), "dataset_id": manifest.dataset_id, "parquet": str(parquet)}, ensure_ascii=False))
        return 0
    if args.command == "demo":
        raw = args.work / "data" / "raw" / "demo_pump_1m.jsonl"
        write_jsonl(raw, demo_candles())
        candles, manifest, parquet = _normalize(raw, args.work / "data")
        result = replay_economy(candles)
        reports = write_replay_report(result, args.work / "reports", manifest.checksum_sha256)
        print(json.dumps({"raw": str(raw), "parquet": str(parquet), "catalog": str(args.work / 'data' / 'catalog' / 'pump.duckdb'), "reports": [str(path) for path in reports], "metrics": result.to_dict()["metrics"]}, ensure_ascii=False))
        return 0
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
