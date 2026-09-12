from __future__ import annotations

import csv
import json
from pathlib import Path

from .domain import Candle, DatasetManifest
from .validation import require_replay_quality


class DuckDbCatalog:
    def __init__(self, database_path: Path) -> None:
        try:
            import duckdb
        except ImportError as exc:
            raise RuntimeError("DuckDB is required. Install the project dependencies first.") from exc
        database_path.parent.mkdir(parents=True, exist_ok=True)
        self._duckdb = duckdb
        self.database_path = database_path

    def register_candles(self, candles: list[Candle], manifest: DatasetManifest, parquet_path: Path) -> Path:
        require_replay_quality(candles, manifest.interval)
        parquet_path.parent.mkdir(parents=True, exist_ok=True)
        partial = parquet_path.with_suffix(parquet_path.suffix + ".partial")
        stage = parquet_path.with_suffix(parquet_path.suffix + ".stage.csv")
        if partial.exists():
            partial.unlink()
        if stage.exists():
            stage.unlink()
        fieldnames = list(Candle.__dataclass_fields__)
        with stage.open("w", encoding="utf-8", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(row.to_dict() for row in candles)
        connection = self._duckdb.connect(str(self.database_path))
        try:
            connection.execute(
                """
                CREATE TABLE IF NOT EXISTS datasets (
                    dataset_id VARCHAR PRIMARY KEY,
                    manifest_json VARCHAR NOT NULL,
                    parquet_path VARCHAR NOT NULL
                )
                """
            )
            escaped = str(partial).replace("'", "''")
            escaped_stage = str(stage).replace("'", "''")
            connection.execute(
                f"COPY (SELECT * FROM read_csv_auto('{escaped_stage}', header=true) ORDER BY open_time_ms) "
                f"TO '{escaped}' (FORMAT PARQUET)"
            )
        finally:
            connection.close()
            if stage.exists():
                stage.unlink()
        partial.replace(parquet_path)
        connection = self._duckdb.connect(str(self.database_path))
        try:
            connection.execute(
                "INSERT OR REPLACE INTO datasets VALUES (?, ?, ?)",
                [manifest.dataset_id, json.dumps(manifest.to_dict(), ensure_ascii=False), str(parquet_path)],
            )
        finally:
            connection.close()
        return parquet_path
