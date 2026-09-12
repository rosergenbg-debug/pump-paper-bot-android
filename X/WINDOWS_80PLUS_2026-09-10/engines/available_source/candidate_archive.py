from __future__ import annotations

import csv
import json
import sqlite3
from contextlib import contextmanager
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable


ARCHIVE_SCHEMA_VERSION = 1


def recovered_seven_trade_candidate() -> tuple[dict, dict]:
    """Candidate captured from the V7 live report before archival existed."""
    empty = {
        "signals": 0, "fills": 0, "wins": 0, "win_rate": 0.0, "average_net": 0.0,
        "profit_factor": None, "compound_net": 0.0, "trades_per_day": 0.0,
        "exits": {}, "max_drawdown": 0.0, "cvar_5": 0.0,
    }
    a = dict(empty) | {
        "signals": 5, "fills": 3, "wins": 3, "win_rate": 1.0,
        "average_net": 0.01, "compound_net": 0.030301,
        "trades_per_day": 1 / 12, "exits": {"TP": 3}, "cvar_5": 0.01,
    }
    b = dict(empty) | {
        "signals": 5, "fills": 4, "wins": 4, "win_rate": 1.0,
        "average_net": 0.01, "compound_net": 0.04060401,
        "trades_per_day": 1 / 9, "exits": {"TP": 4}, "cvar_5": 0.01,
    }
    config = {
        "fee_rate": 0.0021, "limit_discount": 0.002, "limit_ttl_minutes": 30,
        "target_net": 0.01, "stop_net": -0.04, "max_hold_minutes": 2880,
        "adverse_slippage": 0.0008, "max_entries_per_utc_day": 3,
        "min_hours_between_entries": 12, "drawdown_gate": -0.002,
        "spread_rate": 0.0, "entry_latency_minutes": 0,
        "btc_context_rule": "ANY", "sol_context_rule": "ANY",
        "vwap_deviation_gate": -0.015, "min_buy_share": 0.35,
        "min_buy_share_delta": -0.25, "min_green_candle_return": -0.004,
        "min_volume_ratio_20m": 3.0, "vwap_window_minutes": 10080,
        "drawdown_window_minutes": 180, "volume_window_minutes": 5,
        "pump_return_window_minutes": 30, "min_pump_return": -0.004,
        "max_pump_return": 0.04, "rsi_period_minutes": 5, "min_rsi": 50.0,
        "max_rsi": 50.0, "atr_period_minutes": 720, "min_atr_ratio": 0.001,
        "max_atr_ratio": 0.02, "btc_return_window_minutes": 720,
        "min_btc_return": -0.1, "max_btc_return": 0.06,
        "sol_return_window_minutes": 120, "min_sol_return": 0.004,
        "max_sol_return": 0.1, "utc_start_hour": 22, "utc_end_hour": 7,
        "trailing_stop_fraction": 0.06, "breakeven_trigger_net": 0.0,
    }
    candidate = {
        "candidate_id": "A10E681DF96", "config": config, "train": dict(empty),
        "validation_a": a, "validation_b": b, "eligible": False,
        "rejection_reasons": [
            "validation_a:trades_per_day_outside_0.5_to_5",
            "validation_b:trades_per_day_outside_0.5_to_5",
        ],
        "objective": 0.070301, "score": [0, 0.070301, 5.0, 0.0, 7],
    }
    preview = {
        "initial": 1000.0, "final": 1072.1353521070098,
        "metrics": {
            "signals": 11, "fills": 7, "wins": 7, "win_rate": 1.0,
            "average_net": 0.01, "profit_factor": None,
            "compound_net": 0.07213535210700983, "trades_per_day": 0.06511585842468717,
            "exits": {"TP": 7}, "max_drawdown": 0.0, "cvar_5": 0.01,
        },
        "timeline": [], "trades": [],
        "recovery_note": "Metrics and parameters were captured from the V7 live report; detailed trades were not retained by the old build.",
    }
    return candidate, preview


def _combined_compound(candidate: dict) -> float:
    value = 1.0
    for name in ("train", "validation_a", "validation_b"):
        value *= 1.0 + float(candidate.get(name, {}).get("compound_net", 0.0))
    return value - 1.0


def _total_fills(candidate: dict) -> int:
    return sum(int(candidate.get(name, {}).get("fills", 0)) for name in ("train", "validation_a", "validation_b"))


def _trade_bucket(fills: int) -> str:
    if fills <= 30:
        return f"exact:{fills}"
    for upper in (50, 75, 100, 150, 200, 300, 450, 650, 900, 1_500, 2_500):
        if fills <= upper:
            return f"up_to:{upper}"
    return "above:2500"


class CandidateArchive:
    """Durable, disk-conscious archive of candidates worth revisiting.

    The optimizer checkpoint keeps only the current elite. This archive never
    deletes a candidate and therefore preserves leaders after they leave that
    elite. SQLite keeps the archive queryable without rewriting an ever-growing
    JSON document on every generation.
    """

    def __init__(self, database_path: Path, export_directory: Path | None = None) -> None:
        self.database_path = Path(database_path)
        self.export_directory = Path(export_directory) if export_directory else None
        self.database_path.parent.mkdir(parents=True, exist_ok=True)
        self._initialize()

    @contextmanager
    def _connect(self):
        connection = sqlite3.connect(self.database_path, timeout=30)
        connection.execute("PRAGMA journal_mode=WAL")
        connection.execute("PRAGMA synchronous=NORMAL")
        try:
            with connection:
                yield connection
        finally:
            connection.close()

    def _initialize(self) -> None:
        with self._connect() as connection:
            connection.executescript(
                """
                CREATE TABLE IF NOT EXISTS candidates (
                    candidate_id TEXT PRIMARY KEY,
                    first_seen_utc TEXT NOT NULL,
                    last_seen_utc TEXT NOT NULL,
                    first_generation INTEGER NOT NULL,
                    last_generation INTEGER NOT NULL,
                    eligible INTEGER NOT NULL,
                    total_fills INTEGER NOT NULL,
                    validation_fills INTEGER NOT NULL,
                    development_compound REAL NOT NULL,
                    worst_average_net REAL NOT NULL,
                    max_drawdown REAL NOT NULL,
                    tags_json TEXT NOT NULL,
                    rejection_reasons_json TEXT NOT NULL,
                    config_json TEXT NOT NULL,
                    train_json TEXT NOT NULL,
                    validation_a_json TEXT NOT NULL,
                    validation_b_json TEXT NOT NULL,
                    preview_json TEXT,
                    source TEXT NOT NULL
                );
                CREATE INDEX IF NOT EXISTS idx_candidates_profit
                    ON candidates(development_compound DESC);
                CREATE INDEX IF NOT EXISTS idx_candidates_fills
                    ON candidates(total_fills, development_compound DESC);
                CREATE TABLE IF NOT EXISTS archive_meta (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                );
                """
            )
            connection.execute(
                "INSERT OR REPLACE INTO archive_meta(key, value) VALUES('schema_version', ?)",
                (str(ARCHIVE_SCHEMA_VERSION),),
            )

    def contains(self, candidate_id: str) -> bool:
        with self._connect() as connection:
            return connection.execute(
                "SELECT 1 FROM candidates WHERE candidate_id = ?", (candidate_id,)
            ).fetchone() is not None

    def has_preview(self, candidate_id: str) -> bool:
        with self._connect() as connection:
            row = connection.execute(
                "SELECT preview_json FROM candidates WHERE candidate_id = ?", (candidate_id,)
            ).fetchone()
        return bool(row and row[0])

    def select_interesting(self, candidates: Iterable[dict], forced_ids: set[str] | None = None) -> list[tuple[dict, set[str]]]:
        forced_ids = forced_ids or set()
        unique = {item["candidate_id"]: item for item in candidates if item.get("candidate_id")}
        selected: dict[str, tuple[dict, set[str]]] = {}
        bucket_best: dict[str, dict] = {}
        eligible_candidates: list[dict] = []

        for candidate_id, candidate in unique.items():
            tags: set[str] = set()
            if candidate_id in forced_ids:
                tags.add("DISPLAYED_LEADER")
            if bool(candidate.get("eligible")):
                eligible_candidates.append(candidate)
            fills = _total_fills(candidate)
            compound = _combined_compound(candidate)
            a = candidate.get("validation_a", {})
            b = candidate.get("validation_b", {})
            if fills >= 2 and compound >= 0.01 and float(a.get("average_net", 0.0)) > 0 and float(b.get("average_net", 0.0)) > 0:
                bucket = _trade_bucket(fills)
                previous = bucket_best.get(bucket)
                if previous is None or _combined_compound(candidate) > _combined_compound(previous):
                    bucket_best[bucket] = candidate
            if tags:
                selected[candidate_id] = (candidate, tags)

        # Keep the strongest eligible discoveries from every generation while
        # avoiding thousands of nearly identical rows and replays.
        for candidate in sorted(
            eligible_candidates, key=lambda item: tuple(item.get("score", ())), reverse=True
        )[:5]:
            candidate_id = candidate["candidate_id"]
            if candidate_id in selected:
                selected[candidate_id][1].add("ELIGIBLE_TOP5_GENERATION")
            else:
                selected[candidate_id] = (candidate, {"ELIGIBLE_TOP5_GENERATION"})

        with self._connect() as connection:
            for bucket, candidate in bucket_best.items():
                archived = connection.execute(
                    "SELECT MAX(development_compound) FROM candidates WHERE tags_json LIKE ?",
                    (f'%"PROFIT_RECORD:{bucket}"%',),
                ).fetchone()[0]
                if archived is None or _combined_compound(candidate) > float(archived) + 1e-12:
                    candidate_id = candidate["candidate_id"]
                    if candidate_id in selected:
                        selected[candidate_id][1].add(f"PROFIT_RECORD:{bucket}")
                    else:
                        selected[candidate_id] = (candidate, {f"PROFIT_RECORD:{bucket}"})
        return list(selected.values())

    def store(self, candidate: dict, generation: int, tags: set[str], preview: dict | None = None, source: str = "adaptive-v8") -> None:
        now = datetime.now(timezone.utc).isoformat()
        candidate_id = str(candidate["candidate_id"])
        fills = _total_fills(candidate)
        validation_fills = int(candidate.get("validation_a", {}).get("fills", 0)) + int(candidate.get("validation_b", {}).get("fills", 0))
        worst_average = min(
            float(candidate.get("validation_a", {}).get("average_net", 0.0)),
            float(candidate.get("validation_b", {}).get("average_net", 0.0)),
        )
        max_drawdown = max(
            float(candidate.get("validation_a", {}).get("max_drawdown", 0.0)),
            float(candidate.get("validation_b", {}).get("max_drawdown", 0.0)),
        )
        with self._connect() as connection:
            existing = connection.execute(
                "SELECT tags_json, preview_json FROM candidates WHERE candidate_id = ?", (candidate_id,)
            ).fetchone()
            merged_tags = set(json.loads(existing[0])) if existing else set()
            merged_tags.update(tags)
            preview_json = json.dumps(preview, ensure_ascii=False, separators=(",", ":")) if preview else (existing[1] if existing else None)
            values = (
                candidate_id, now, now, int(generation), int(generation), int(bool(candidate.get("eligible"))),
                fills, validation_fills, _combined_compound(candidate), worst_average, max_drawdown,
                json.dumps(sorted(merged_tags), ensure_ascii=False),
                json.dumps(candidate.get("rejection_reasons", []), ensure_ascii=False),
                json.dumps(candidate.get("config", {}), ensure_ascii=False, separators=(",", ":")),
                json.dumps(candidate.get("train", {}), ensure_ascii=False, separators=(",", ":")),
                json.dumps(candidate.get("validation_a", {}), ensure_ascii=False, separators=(",", ":")),
                json.dumps(candidate.get("validation_b", {}), ensure_ascii=False, separators=(",", ":")),
                preview_json, source,
            )
            connection.execute(
                """
                INSERT INTO candidates VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(candidate_id) DO UPDATE SET
                    last_seen_utc=excluded.last_seen_utc,
                    last_generation=MAX(candidates.last_generation, excluded.last_generation),
                    eligible=MAX(candidates.eligible, excluded.eligible),
                    tags_json=excluded.tags_json,
                    preview_json=COALESCE(excluded.preview_json, candidates.preview_json)
                """,
                values,
            )

    def count(self) -> int:
        with self._connect() as connection:
            return int(connection.execute("SELECT COUNT(*) FROM candidates").fetchone()[0])

    def summary(self) -> dict:
        with self._connect() as connection:
            row = connection.execute(
                "SELECT COUNT(*), COALESCE(SUM(eligible),0), COALESCE(MAX(last_generation),0) FROM candidates"
            ).fetchone()
        return {
            "database": str(self.database_path),
            "candidate_count": int(row[0]),
            "eligible_count": int(row[1]),
            "last_generation": int(row[2]),
            "policy": "displayed leaders, top eligible discoveries, and positive profit records by trade-count bucket",
        }

    def export(self) -> tuple[Path, Path] | None:
        if not self.export_directory:
            return None
        self.export_directory.mkdir(parents=True, exist_ok=True)
        csv_path = self.export_directory / "INTERESTING_CANDIDATES.csv"
        md_path = self.export_directory / "INTERESTING_CANDIDATES.md"
        with self._connect() as connection:
            connection.row_factory = sqlite3.Row
            rows = connection.execute(
                "SELECT * FROM candidates ORDER BY eligible DESC, development_compound DESC, total_fills DESC"
            ).fetchall()
        partial = csv_path.with_suffix(".csv.partial")
        columns = [
            "candidate_id", "strategy_family", "eligible", "total_fills", "validation_fills", "development_compound_pct",
            "worst_average_net_pct", "max_drawdown_pct", "first_generation", "last_generation",
            "first_seen_utc", "last_seen_utc", "tags", "rejection_reasons", "config_json", "preview_json",
        ]
        with partial.open("w", encoding="utf-8-sig", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=columns)
            writer.writeheader()
            for row in rows:
                config = json.loads(row["config_json"])
                family = "ACTIVE" if int(config.get("max_hold_minutes", 120)) <= 2_880 else "SWING"
                writer.writerow({
                    "candidate_id": row["candidate_id"], "strategy_family": family,
                    "eligible": bool(row["eligible"]),
                    "total_fills": row["total_fills"], "validation_fills": row["validation_fills"],
                    "development_compound_pct": round(row["development_compound"] * 100, 6),
                    "worst_average_net_pct": round(row["worst_average_net"] * 100, 6),
                    "max_drawdown_pct": round(row["max_drawdown"] * 100, 6),
                    "first_generation": row["first_generation"], "last_generation": row["last_generation"],
                    "first_seen_utc": row["first_seen_utc"], "last_seen_utc": row["last_seen_utc"],
                    "tags": ";".join(json.loads(row["tags_json"])),
                    "rejection_reasons": ";".join(json.loads(row["rejection_reasons_json"])),
                    "config_json": row["config_json"], "preview_json": row["preview_json"] or "",
                })
        partial.replace(csv_path)
        lines = [
            "# PUMP Research Lab — постоянный архив интересных кандидатов", "",
            f"Сохранено кандидатов: **{len(rows):,}**. Старые записи не удаляются.", "",
            "> Это исследовательские кандидаты. Прибыль на TRAIN/VALIDATION не разрешает реальные ордера.", "",
            "| ID | Семейство | Допущен | Сделки | Validation | Development NET | Худший Avg NET | Поколение |", "|---|---|---:|---:|---:|---:|---:|---:|",
        ]
        for row in rows[:100]:
            config = json.loads(row["config_json"])
            family = "ACTIVE" if int(config.get("max_hold_minutes", 120)) <= 2_880 else "SWING"
            lines.append(
                f"| {row['candidate_id']} | {family} | {'да' if row['eligible'] else 'нет'} | {row['total_fills']} | "
                f"{row['validation_fills']} | {row['development_compound'] * 100:+.3f}% | "
                f"{row['worst_average_net'] * 100:+.3f}% | {row['first_generation']} |"
            )
        md_partial = md_path.with_suffix(".md.partial")
        md_partial.write_text("\n".join(lines) + "\n", encoding="utf-8")
        md_partial.replace(md_path)
        return csv_path, md_path
