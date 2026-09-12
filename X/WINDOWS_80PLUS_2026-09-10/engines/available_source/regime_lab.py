from __future__ import annotations

import gc
import hashlib
import json
import math
import time
import urllib.parse
import urllib.request
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Callable

from .adaptive_features import build_feature_store
from .adaptive_optimizer import _frequency_ok, optimize_adaptive, replay_adaptive
from .automation import ready_reports_directory
from .baseline_x import EconomyConfig
from .data_manager import DataManager
from .domain import Candle, ONE_MINUTE_MS
from .optimizer import ResearchCancelled, compact_metrics


REGIME_LAB_VERSION = "1.0.0"
SYMBOLS = ("PUMPUSDT", "BTCUSDT", "SOLUSDT")
REGIME_DAYS = 180
DISCOVERY_DAYS = 365


@dataclass(frozen=True, slots=True)
class DailyBar:
    open_time_ms: int
    open: float
    high: float
    low: float
    close: float

    @classmethod
    def from_binance(cls, row: list) -> "DailyBar":
        return cls(int(row[0]), float(row[1]), float(row[2]), float(row[3]), float(row[4]))


@dataclass(frozen=True, slots=True)
class RegimeWindow:
    name: str
    label_ru: str
    start_ms: int
    end_day_ms: int
    return_fraction: float
    trend_fraction: float
    efficiency: float
    max_drawdown: float

    @property
    def end_ms(self) -> int:
        return self.end_day_ms + (1440 - 1) * ONE_MINUTE_MS

    def to_dict(self) -> dict:
        value = asdict(self)
        value["end_ms"] = self.end_ms
        value["start_utc"] = datetime.fromtimestamp(self.start_ms / 1000, timezone.utc).date().isoformat()
        value["end_utc"] = datetime.fromtimestamp(self.end_day_ms / 1000, timezone.utc).date().isoformat()
        return value


def fetch_daily_bars(symbol: str = "PUMPUSDT", limit: int = DISCOVERY_DAYS) -> list[DailyBar]:
    params = urllib.parse.urlencode({"symbol": symbol, "interval": "1d", "limit": limit})
    request = urllib.request.Request(
        f"https://data-api.binance.vision/api/v3/klines?{params}",
        headers={"User-Agent": f"PUMP-Regime-Stress-Lab/{REGIME_LAB_VERSION}"},
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        payload = json.loads(response.read().decode("utf-8"))
    if not isinstance(payload, list) or not payload:
        raise ValueError("Binance не вернул дневную историю PUMPUSDT")
    return [DailyBar.from_binance(row) for row in payload]


def _window_metrics(rows: list[DailyBar], start: int, days: int) -> tuple[float, float, float, float]:
    window = rows[start : start + days]
    total_return = window[-1].close / window[0].open - 1.0
    logs = [math.log(item.close) for item in window]
    midpoint = (days - 1) / 2.0
    denominator = sum((index - midpoint) ** 2 for index in range(days))
    mean = sum(logs) / days
    slope = sum((index - midpoint) * (value - mean) for index, value in enumerate(logs)) / denominator
    trend = math.exp(slope * (days - 1)) - 1.0
    path = sum(abs(logs[index] - logs[index - 1]) for index in range(1, days))
    efficiency = abs(logs[-1] - logs[0]) / path if path else 0.0
    peak = window[0].high
    max_drawdown = 0.0
    for item in window:
        peak = max(peak, item.high)
        max_drawdown = min(max_drawdown, item.low / peak - 1.0)
    return total_return, trend, efficiency, max_drawdown


def detect_regime_windows(rows: list[DailyBar], days: int = REGIME_DAYS) -> list[RegimeWindow]:
    if len(rows) < days:
        raise ValueError(f"Для выбора режима требуется минимум {days} дневных свечей")
    candidates = []
    for start in range(len(rows) - days + 1):
        metrics = _window_metrics(rows, start, days)
        candidates.append((start, *metrics))
    selectors = (
        ("BEAR", "ПАДЕНИЕ", sorted(candidates, key=lambda item: item[1] + item[2])),
        ("SIDEWAYS", "БОКОВИК", sorted(candidates, key=lambda item: abs(item[1]))),
        ("BULL", "РОСТ", sorted(candidates, key=lambda item: item[1] + item[2], reverse=True)),
    )
    used: set[int] = set()
    result: list[RegimeWindow] = []
    for name, label, ordered in selectors:
        selected = next((item for item in ordered if item[0] not in used), ordered[0])
        used.add(selected[0])
        start, total_return, trend, efficiency, max_drawdown = selected
        result.append(RegimeWindow(
            name, label, rows[start].open_time_ms, rows[start + days - 1].open_time_ms,
            total_return, trend, efficiency, max_drawdown,
        ))
    return result


def overlap_days(first: RegimeWindow, second: RegimeWindow) -> int:
    start = max(first.start_ms, second.start_ms)
    end = min(first.end_ms, second.end_ms)
    return max(0, int((end - start) // 86_400_000) + 1) if end >= start else 0


def data_requirements(windows: list[RegimeWindow]) -> list[dict]:
    union_start = min(item.start_ms for item in windows)
    union_end = max(item.end_ms for item in windows)
    rows_per_symbol = (union_end - union_start) // ONE_MINUTE_MS + 1
    result = [{
        "kind": "HISTORICAL_DISCOVERY", "symbol": "PUMPUSDT", "interval": "1d",
        "period": f"последние доступные {DISCOVERY_DAYS} дней", "rows": DISCOVERY_DAYS,
        "purpose": "автоматически выбрать 3 режима", "source": "Binance public API",
    }]
    for symbol in SYMBOLS:
        result.append({
            "kind": "HISTORICAL_REPLAY", "symbol": symbol, "interval": "1m",
            "period": f"{_date(union_start)} — {_date(union_end)}", "rows": int(rows_per_symbol),
            "purpose": "три шестимесячных stress-replay", "source": "Binance public API",
        })
    result.append({
        "kind": "SYNTHETIC_LOCAL", "symbol": "8 execution-сценариев", "interval": "локально",
        "period": "fee / spread / slippage / latency", "rows": 8,
        "purpose": "проверка устойчивости исполнения", "source": "генерируются, не скачиваются",
    })
    return result


def _date(value_ms: int) -> str:
    return datetime.fromtimestamp(value_ms / 1000, timezone.utc).date().isoformat()


def _atomic_json(path: Path, payload: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    partial = path.with_suffix(path.suffix + ".partial")
    partial.write_text(json.dumps(payload, ensure_ascii=False, indent=2, allow_nan=False), encoding="utf-8")
    partial.replace(path)


def _sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def _read_range(path: Path, start_ms: int, end_ms: int) -> list[Candle]:
    rows: list[Candle] = []
    with path.open("r", encoding="utf-8") as handle:
        for line in handle:
            if not line.strip():
                continue
            raw = json.loads(line)
            value = int(raw["open_time_ms"])
            if value < start_ms:
                continue
            if value > end_ms:
                break
            rows.append(Candle.from_dict(raw))
    return rows


class RegimeStressRunner:
    def __init__(
        self, root: Path, progress: Callable[[dict], None] | None = None,
        cancel_check: Callable[[], bool] | None = None,
        pause_check: Callable[[], bool] | None = None,
    ) -> None:
        self.root = Path(root)
        self.progress = progress
        self.cancel_check = cancel_check
        self.pause_check = pause_check
        self.state_dir = self.root / "regime_stress"
        self.state_dir.mkdir(parents=True, exist_ok=True)

    def _emit(self, **payload) -> None:
        if self.progress:
            self.progress(payload)

    def discover(self, bars: list[DailyBar] | None = None) -> dict:
        bars = bars or fetch_daily_bars()
        windows = detect_regime_windows(bars)
        requirements = data_requirements(windows)
        requirements[0]["rows"] = len(bars)
        requirements[0]["period"] = f"{_date(bars[0].open_time_ms)} — {_date(bars[-1].open_time_ms)}"
        payload = {
            "version": REGIME_LAB_VERSION,
            "created_at_utc": datetime.now(timezone.utc).isoformat(),
            "available_daily_rows": len(bars),
            "available_start_utc": _date(bars[0].open_time_ms),
            "available_end_utc": _date(bars[-1].open_time_ms),
            "requested_discovery_days": DISCOVERY_DAYS,
            "history_is_shorter_than_requested": len(bars) < DISCOVERY_DAYS,
            "windows": [item.to_dict() for item in windows],
            "overlap_days": {
                f"{a.name}_{b.name}": overlap_days(a, b)
                for index, a in enumerate(windows) for b in windows[index + 1 :]
            },
            "requirements": requirements,
            "daily_price": [{"time_ms": item.open_time_ms, "close": item.close} for item in bars],
            "selection_warning": "Окна выбраны по самой PUMP-цене и частично пересекаются; это stress-набор, не независимый holdout.",
        }
        _atomic_json(self.state_dir / "regime-selection.json", payload)
        return payload

    def _find_covering_dataset(self, symbol: str, start_ms: int, end_ms: int) -> tuple[Path, Path] | None:
        directory = self.root / "data" / "raw" / "binance_spot" / symbol / "1m"
        candidates = []
        for raw in directory.glob("*.jsonl"):
            manifest = raw.with_suffix(".manifest.json")
            try:
                parts = raw.stem.split("-")
                file_start, file_end = int(parts[0]), int(parts[1])
                if manifest.exists() and file_start <= start_ms and file_end >= end_ms:
                    candidates.append((file_end - file_start, raw, manifest))
            except (ValueError, IndexError):
                continue
        if not candidates:
            return None
        _span, raw, manifest = min(candidates, key=lambda item: item[0])
        return raw, manifest

    def prepare_data(self, selection: dict) -> dict:
        windows = [RegimeWindow(**{key: value[key] for key in RegimeWindow.__dataclass_fields__}) for value in selection["windows"]]
        start_ms = min(item.start_ms for item in windows)
        end_ms = max(item.end_ms for item in windows)
        total_per_symbol = (end_ms - start_ms) // ONE_MINUTE_MS + 1
        manager = DataManager(self.root / "data")
        datasets = {}
        for index, symbol in enumerate(SYMBOLS):
            if self.cancel_check and self.cancel_check():
                raise ResearchCancelled("Скачивание остановлено; partial-файл сохранён")
            local = self._find_covering_dataset(symbol, start_ms, end_ms)
            if local:
                raw, manifest = local
                reused = True
                self._emit(phase="DOWNLOAD", percent=int((index + 1) / 3 * 100), symbol=symbol, done=total_per_symbol, total=total_per_symbol, reused=True)
            else:
                base = index * total_per_symbol

                def callback(done: int, _total: int, active_symbol: str = symbol) -> None:
                    if self.cancel_check and self.cancel_check():
                        raise ResearchCancelled("Скачивание остановлено; partial-файл сохранён")
                    while self.pause_check and self.pause_check():
                        if self.cancel_check and self.cancel_check():
                            raise ResearchCancelled("Скачивание остановлено; partial-файл сохранён")
                        time.sleep(0.2)
                    self._emit(
                        phase="DOWNLOAD", percent=int((base + done) / (total_per_symbol * 3) * 100),
                        symbol=active_symbol, done=done, total=total_per_symbol, reused=False,
                    )

                raw, manifest = manager.download_candles(symbol, start_ms, end_ms, progress_callback=callback)
                reused = False
            datasets[symbol] = {
                "raw_path": str(raw), "manifest_path": str(manifest), "reused": reused,
                "size_bytes": raw.stat().st_size, "sha256": _sha256_file(raw),
                "rows": int(total_per_symbol),
            }
        prepared = {
            "version": REGIME_LAB_VERSION, "period": {"start_ms": start_ms, "end_ms": end_ms},
            "windows": selection["windows"], "datasets": datasets,
            "prepared_at_utc": datetime.now(timezone.utc).isoformat(),
        }
        _atomic_json(self.state_dir / "prepared-regime-data.json", prepared)
        return prepared

    def run_stress(self, prepared: dict, workers: int) -> dict:
        outputs: dict[str, dict] = {}
        windows = prepared["windows"]
        report_dir = ready_reports_directory(self.root) / "REGIME-STRESS"
        report_dir.mkdir(parents=True, exist_ok=True)
        for regime_index, window in enumerate(windows):
            name = window["name"]
            start_ms, end_ms = int(window["start_ms"]), int(window["end_ms"])
            self._emit(phase="LOAD", regime=name, percent=int(regime_index / len(windows) * 100), stage=f"Загрузка {name}")
            pump = _read_range(Path(prepared["datasets"]["PUMPUSDT"]["raw_path"]), start_ms, end_ms)
            btc = _read_range(Path(prepared["datasets"]["BTCUSDT"]["raw_path"]), start_ms, end_ms)
            sol = _read_range(Path(prepared["datasets"]["SOLUSDT"]["raw_path"]), start_ms, end_ms)
            checkpoint = self.root / "checkpoints" / f"regime-{name.lower()}-{prepared['datasets']['PUMPUSDT']['sha256'][:12]}-adaptive-v8.json"

            def optimizer_progress(value: dict, active_name: str = name, base: int = regime_index) -> None:
                self._emit(
                    phase="COMPUTE", regime=active_name,
                    percent=min(99, int((base + value.get("percent", 0) / 100) / len(windows) * 100)),
                    stage=value.get("stage", "Поиск"), tested=value.get("tested", 0),
                    workers=value.get("workers", workers), variants_per_second=value.get("variants_per_second", 0),
                    best=value.get("best", "—"), best_provisional=value.get("best_provisional", {}),
                    equity_preview=value.get("best_provisional", {}).get("equity_preview", {}),
                    interesting_archive=value.get("interesting_archive", {}),
                )

            outputs[name] = optimize_adaptive(
                pump, optimizer_progress, btc, sol, self.cancel_check, self.pause_check,
                workers, checkpoint, report_dir / name,
            )
            self._write_live_summary(report_dir, outputs, windows)
            del pump, btc, sol
            gc.collect()

        cross = self._cross_regime(outputs, prepared)
        result = {
            "version": REGIME_LAB_VERSION, "created_at_utc": datetime.now(timezone.utc).isoformat(),
            "simulation": True, "real_orders": False, "windows": windows,
            "independent_searches": outputs, "cross_regime": cross,
            "warning": "Режимы выбраны по PUMP и пересекаются. Результат является stress-research, а не независимым доказательством будущей прибыли.",
        }
        stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
        json_path = report_dir / f"PUMP_REGIME_STRESS_{stamp}.json"
        md_path = report_dir / f"PUMP_REGIME_STRESS_{stamp}.md"
        _atomic_json(json_path, result)
        self._write_markdown(md_path, result)
        _atomic_json(report_dir / "LATEST_REGIME_STRESS.json", result)
        latest = report_dir / "LATEST_REGIME_STRESS.md"
        partial = latest.with_suffix(".md.partial"); partial.write_text(md_path.read_text(encoding="utf-8"), encoding="utf-8"); partial.replace(latest)
        self._emit(phase="DONE", percent=100, stage="Три режима и перекрёстная проверка завершены", report=str(md_path), cross=cross)
        return result | {"json_report": str(json_path), "md_report": str(md_path)}

    def _cross_regime(self, outputs: dict[str, dict], prepared: dict) -> dict:
        configs = {name: EconomyConfig(**value["selected"]["config"]) for name, value in outputs.items()}
        matrix = {candidate: {} for candidate in configs}
        for window in prepared["windows"]:
            name = window["name"]; start_ms, end_ms = int(window["start_ms"]), int(window["end_ms"])
            pump = _read_range(Path(prepared["datasets"]["PUMPUSDT"]["raw_path"]), start_ms, end_ms)
            btc = _read_range(Path(prepared["datasets"]["BTCUSDT"]["raw_path"]), start_ms, end_ms)
            sol = _read_range(Path(prepared["datasets"]["SOLUSDT"]["raw_path"]), start_ms, end_ms)
            features = build_feature_store(pump, btc, sol)
            for candidate_name, config in configs.items():
                matrix[candidate_name][name] = compact_metrics(replay_adaptive(pump, features, config, 719, len(pump)))
            del pump, btc, sol, features
            gc.collect()
        ranked = []
        for candidate_name, by_regime in matrix.items():
            config = configs[candidate_name]
            positive_all = all(
                metrics["average_net"] > 0 and metrics["compound_net"] > 0
                and ((metrics["profit_factor"] is None and metrics["fills"] > 0) or (metrics["profit_factor"] or 0) > 1)
                and _frequency_ok(metrics, config)
                for metrics in by_regime.values()
            )
            ranked.append({
                "source_regime": candidate_name, "candidate_id": outputs[candidate_name]["selected"]["candidate_id"],
                "config": asdict(config), "metrics_by_regime": by_regime, "positive_in_all_regimes": positive_all,
                "worst_compound_net": min(item["compound_net"] for item in by_regime.values()),
                "worst_average_net": min(item["average_net"] for item in by_regime.values()),
                "maximum_drawdown": max(item["max_drawdown"] for item in by_regime.values()),
            })
        ranked.sort(key=lambda item: (item["positive_in_all_regimes"], item["worst_compound_net"], item["worst_average_net"], -item["maximum_drawdown"]), reverse=True)
        return {
            "winner": ranked[0], "ranking": ranked,
            "verdict": "ROBUST_STRESS_CANDIDATE" if ranked[0]["positive_in_all_regimes"] else "NO_ROBUST_CANDIDATE",
            "not_independent_holdout": True,
        }

    @staticmethod
    def _write_live_summary(directory: Path, outputs: dict[str, dict], windows: list[dict]) -> None:
        payload = {"status": "RUNNING", "completed_regimes": list(outputs), "results": outputs, "windows": windows}
        _atomic_json(directory / "LIVE_REGIME_SUMMARY.json", payload)

    @staticmethod
    def _write_markdown(path: Path, result: dict) -> None:
        window_lines = []
        for window in result["windows"]:
            output = result["independent_searches"][window["name"]]
            chosen = output["selected"]
            window_lines.append(
                f"| {window['label_ru']} | {window['start_utc']} — {window['end_utc']} | "
                f"{window['return_fraction'] * 100:+.2f}% | {chosen['candidate_id']} | "
                f"{chosen.get('strategy_family', '—')} | {output['verdict']} |"
            )
        cross = result["cross_regime"]
        text = f"""# PUMP Regime Stress Lab — отчёт

## Итог

**{cross['verdict']}**

| Режим | Период | PUMP | Независимый лидер | Семейство | Решение |
|---|---|---:|---|---|---|
{chr(10).join(window_lines)}

Перекрёстный кандидат: **{cross['winner']['candidate_id']}**.
Худший Compound NET по трём режимам: **{cross['winner']['worst_compound_net'] * 100:+.3f}%**.
Худший Avg NET: **{cross['winner']['worst_average_net'] * 100:+.4f}%**.
Максимальная просадка: **{cross['winner']['maximum_drawdown'] * 100:.3f}%**.

## Ограничения

- {result['warning']}
- TEST каждого внутреннего поиска не использовался при выборе его параметров.
- Перекрёстная таблица сравнивает только трёх найденных лидеров и не является новым immutable holdout.
- Реальные ордера отключены.
"""
        partial = path.with_suffix(".md.partial"); partial.write_text(text, encoding="utf-8"); partial.replace(path)
