from __future__ import annotations

import hashlib
import json
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from collections.abc import Iterator
from pathlib import Path
from typing import Callable

from .domain import Candle, DatasetManifest, ONE_MINUTE_MS
from .io import read_jsonl, write_jsonl
from .validation import validate_candles


class BinanceSpotAdapter:
    base_url = "https://data-api.binance.vision"

    def __init__(self, timeout_seconds: int = 20, max_retries: int = 5) -> None:
        self.timeout_seconds = timeout_seconds
        self.max_retries = max_retries

    def download_candles(self, symbol: str, start_ms: int, end_ms: int, interval: str = "1m") -> Iterator[Candle]:
        if interval != "1m":
            raise ValueError("V1 adapter currently supports only 1m candles")
        cursor = start_ms
        while cursor <= end_ms:
            params = urllib.parse.urlencode(
                {"symbol": symbol.upper(), "interval": interval, "startTime": cursor, "endTime": end_ms, "limit": 1000}
            )
            url = f"{self.base_url}/api/v3/klines?{params}"
            payload = self._get_json(url)
            if not isinstance(payload, list) or not payload:
                break
            candles = [Candle.from_binance(symbol, interval, row) for row in payload]
            for candle in candles:
                if start_ms <= candle.open_time_ms <= end_ms:
                    yield candle
            next_cursor = candles[-1].open_time_ms + ONE_MINUTE_MS
            if next_cursor <= cursor:
                raise RuntimeError("Binance pagination did not advance")
            cursor = next_cursor
            if len(candles) < 1000:
                break

    def _get_json(self, url: str):
        request = urllib.request.Request(url, headers={"User-Agent": "PUMP-Research-Lab/0.1"})
        for attempt in range(self.max_retries):
            try:
                with urllib.request.urlopen(request, timeout=self.timeout_seconds) as response:
                    return json.loads(response.read().decode("utf-8"))
            except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as exc:
                if attempt + 1 == self.max_retries:
                    raise RuntimeError(f"Binance download failed after {self.max_retries} attempts: {exc}") from exc
                time.sleep(min(2**attempt, 16))
        raise AssertionError("unreachable")


class DataManager:
    def __init__(self, root: Path, adapter: BinanceSpotAdapter | None = None) -> None:
        self.root = root
        self.adapter = adapter or BinanceSpotAdapter()

    def download_candles(
        self,
        symbol: str,
        start_ms: int,
        end_ms: int,
        interval: str = "1m",
        progress_callback: Callable[[int, int], None] | None = None,
    ) -> tuple[Path, Path]:
        if end_ms < start_ms:
            raise ValueError("end_ms must be >= start_ms")
        start_ms -= start_ms % ONE_MINUTE_MS
        end_ms -= end_ms % ONE_MINUTE_MS
        last_closed_open_ms = (int(time.time() * 1000) // ONE_MINUTE_MS) * ONE_MINUTE_MS - ONE_MINUTE_MS
        end_ms = min(end_ms, last_closed_open_ms)
        if end_ms < start_ms:
            raise ValueError("Requested range contains no closed 1m candles")
        symbol = symbol.upper()
        dataset_id = f"binance-{symbol}-{interval}-{start_ms}-{end_ms}-{uuid.uuid4().hex[:8]}"
        directory = self.root / "raw" / "binance_spot" / symbol / interval
        directory.mkdir(parents=True, exist_ok=True)
        raw_path = directory / f"{start_ms}-{end_ms}.jsonl"
        partial = raw_path.with_suffix(raw_path.suffix + ".partial")
        total_rows = ((end_ms - start_ms) // ONE_MINUTE_MS) + 1

        resume_ms = start_ms
        completed_rows = 0
        if partial.exists() and partial.stat().st_size:
            existing = read_jsonl(partial)
            if existing:
                resume_ms = existing[-1].open_time_ms + ONE_MINUTE_MS
                completed_rows = len(existing)
        if progress_callback:
            progress_callback(completed_rows, total_rows)
        mode = "a" if partial.exists() else "w"
        with partial.open(mode, encoding="utf-8", newline="\n") as handle:
            for candle in self.adapter.download_candles(symbol, resume_ms, end_ms, interval):
                handle.write(json.dumps(candle.to_dict(), ensure_ascii=False, separators=(",", ":")) + "\n")
                completed_rows += 1
                if progress_callback and (completed_rows % 250 == 0 or completed_rows >= total_rows):
                    handle.flush()
                    progress_callback(completed_rows, total_rows)

        candles = read_jsonl(partial)
        report = validate_candles(candles, interval, expected_start_ms=start_ms, expected_end_open_ms=end_ms)
        if report.row_count == 0:
            raise ValueError("Binance returned no candles; partial file preserved")
        if not report.valid:
            raise ValueError(
                "Downloaded data failed validation; partial file preserved: "
                f"gaps={len(report.gaps)}, duplicates={len(report.duplicates)}, invalid={len(report.invalid_rows)}"
            )
        partial.replace(raw_path)
        checksum = hashlib.sha256(raw_path.read_bytes()).hexdigest()
        origin = f"{self.adapter.base_url}/api/v3/klines?symbol={symbol}&interval={interval}"
        manifest = DatasetManifest.create(dataset_id, symbol, interval, report, checksum, origin)
        manifest_path = raw_path.with_suffix(".manifest.json")
        manifest_partial = manifest_path.with_suffix(manifest_path.suffix + ".partial")
        manifest_partial.write_text(json.dumps(manifest.to_dict(), ensure_ascii=False, indent=2), encoding="utf-8")
        manifest_partial.replace(manifest_path)
        if progress_callback:
            progress_callback(report.row_count, total_rows)
        return raw_path, manifest_path

    def update_candles(
        self, existing_path: Path, end_ms: int, overlap_minutes: int = 120
    ) -> tuple[Path, Path, Path]:
        """Create a new immutable snapshot, refreshing an overlap at the tail."""
        if overlap_minutes < 1:
            raise ValueError("overlap_minutes must be positive")
        rows = sorted(read_jsonl(existing_path), key=lambda row: row.open_time_ms)
        report = validate_candles(rows)
        if not rows or not report.valid:
            raise ValueError("Existing dataset must be non-empty and valid")
        end_ms -= end_ms % ONE_MINUTE_MS
        if end_ms < rows[-1].open_time_ms:
            raise ValueError("Update end cannot precede the existing dataset")
        job_id = uuid.uuid4().hex
        state_path = self.root / "jobs" / f"update-{job_id}.json"
        self._write_job(state_path, "running", existing_path=str(existing_path), requested_end_ms=end_ms)
        try:
            overlap_start = max(rows[0].open_time_ms, rows[-1].open_time_ms - (overlap_minutes - 1) * ONE_MINUTE_MS)
            refreshed = list(self.adapter.download_candles(rows[0].symbol, overlap_start, end_ms, rows[0].interval))
            merged = {row.open_time_ms: row for row in rows if row.open_time_ms < overlap_start}
            merged.update((row.open_time_ms, row) for row in refreshed)
            output_rows = [merged[key] for key in sorted(merged)]
            output, manifest = self._publish_snapshot(output_rows, end_ms, f"update-{job_id}")
            self._write_job(state_path, "completed", output=str(output), manifest=str(manifest))
            return output, manifest, state_path
        except Exception as exc:
            self._write_job(state_path, "failed", error=str(exc))
            raise

    def repair_gaps(self, existing_path: Path) -> tuple[Path, Path, Path]:
        """Fetch only missing intervals and publish a separate repaired snapshot."""
        rows = sorted(read_jsonl(existing_path), key=lambda row: row.open_time_ms)
        report = validate_candles(rows)
        if not rows:
            raise ValueError("Cannot repair an empty dataset")
        if report.duplicates or report.invalid_rows:
            raise ValueError("Repair accepts gaps only; duplicates or invalid rows require quarantine")
        job_id = uuid.uuid4().hex
        state_path = self.root / "jobs" / f"repair-{job_id}.json"
        self._write_job(state_path, "running", existing_path=str(existing_path), gaps=len(report.gaps))
        try:
            merged = {row.open_time_ms: row for row in rows}
            for gap in report.gaps:
                gap_start = gap["after_ms"] + ONE_MINUTE_MS
                gap_end = gap["before_ms"] - ONE_MINUTE_MS
                repaired = self.adapter.download_candles(rows[0].symbol, gap_start, gap_end, rows[0].interval)
                merged.update((row.open_time_ms, row) for row in repaired)
            output_rows = [merged[key] for key in sorted(merged)]
            output, manifest = self._publish_snapshot(output_rows, rows[-1].open_time_ms, f"repair-{job_id}")
            self._write_job(state_path, "completed", output=str(output), manifest=str(manifest))
            return output, manifest, state_path
        except Exception as exc:
            self._write_job(state_path, "failed", error=str(exc))
            raise

    def _publish_snapshot(self, rows: list[Candle], expected_end_ms: int, suffix: str) -> tuple[Path, Path]:
        report = validate_candles(
            rows, rows[0].interval, expected_start_ms=rows[0].open_time_ms, expected_end_open_ms=expected_end_ms
        )
        if not report.valid:
            raise ValueError(f"Snapshot validation failed: gaps={len(report.gaps)}")
        directory = self.root / "raw" / "binance_spot" / rows[0].symbol / rows[0].interval
        raw_path = directory / f"{rows[0].open_time_ms}-{expected_end_ms}-{suffix}.jsonl"
        write_jsonl(raw_path, rows)
        checksum = hashlib.sha256(raw_path.read_bytes()).hexdigest()
        dataset_id = f"binance-{rows[0].symbol}-{rows[0].interval}-{suffix}"
        origin = f"{self.adapter.base_url}/api/v3/klines?symbol={rows[0].symbol}&interval={rows[0].interval}"
        manifest = DatasetManifest.create(dataset_id, rows[0].symbol, rows[0].interval, report, checksum, origin)
        manifest_path = raw_path.with_suffix(".manifest.json")
        partial = manifest_path.with_suffix(manifest_path.suffix + ".partial")
        partial.write_text(json.dumps(manifest.to_dict(), ensure_ascii=False, indent=2), encoding="utf-8")
        partial.replace(manifest_path)
        return raw_path, manifest_path

    @staticmethod
    def _write_job(path: Path, status: str, **details) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        current = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
        current.update({"status": status, **details, "updated_at_ms": int(time.time() * 1000)})
        partial = path.with_suffix(path.suffix + ".partial")
        partial.write_text(json.dumps(current, ensure_ascii=False, indent=2), encoding="utf-8")
        partial.replace(path)
