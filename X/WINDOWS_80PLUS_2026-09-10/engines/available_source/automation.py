from __future__ import annotations

import hashlib
import csv
import json
import math
import sys
import time
import zipfile
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Callable

from .baseline_x import replay_economy
from .adaptive_optimizer import optimize_adaptive
from .data_manager import DataManager
from .demo import demo_candles
from .domain import DatasetManifest, ONE_MINUTE_MS
from .io import read_jsonl, write_jsonl
from .holdout import seal_immutable_holdout
from .hardware import detect_hardware, save_hardware_profile, workers_for_mode
from .optimizer import ResearchCancelled, optimize_variants
from .scenarios import run_synthetic_school
from .validation import validate_candles


ProgressCallback = Callable[[dict], None]
PERIOD_DAYS = {1: 30, 3: 90, 6: 180}
SYMBOLS = ("PUMPUSDT", "BTCUSDT", "SOLUSDT")
CACHE_MAX_AGE_DAYS = 30


@dataclass(frozen=True, slots=True)
class AutomationConfig:
    months: int = 1
    initial_capital: float = 1000.0
    currency: str = "USDT"
    end_ms: int | None = None
    search_mode: str = "ADAPTIVE"
    resource_mode: str = "FAST"

    def validate(self) -> None:
        if self.months not in PERIOD_DAYS:
            raise ValueError("Период должен быть 1, 3 или 6 месяцев")
        if self.initial_capital <= 0:
            raise ValueError("Стартовый капитал должен быть больше нуля")
        if self.search_mode.upper() not in {"FAST", "STANDARD", "DEEP", "MAXIMUM", "ADAPTIVE"}:
            raise ValueError("Неизвестная глубина автоматического поиска")
        if self.resource_mode.upper() not in {"BACKGROUND", "FAST", "MAXIMUM"}:
            raise ValueError("Неизвестный режим использования компьютера")


def ready_reports_directory(root: Path) -> Path:
    if getattr(sys, "frozen", False):
        destination = Path(sys.executable).resolve().parent / "ОТЧЁТЫ-ДЛЯ-АНАЛИЗА"
    else:
        destination = root / "reports" / "ready"
    destination.mkdir(parents=True, exist_ok=True)
    return destination


class AutomatedResearchRunner:
    def __init__(
        self,
        root: Path,
        progress_callback: ProgressCallback | None = None,
        manager_factory: Callable[[Path], DataManager] = DataManager,
        cancel_check: Callable[[], bool] | None = None,
        pause_check: Callable[[], bool] | None = None,
    ) -> None:
        self.root = root
        self.progress_callback = progress_callback
        self.manager_factory = manager_factory
        self.cancel_check = cancel_check
        self.pause_check = pause_check

    def _check_cancelled(self) -> None:
        if self.cancel_check and self.cancel_check():
            raise ResearchCancelled("Исследование остановлено пользователем")

    def _wait_if_paused(self) -> None:
        while self.pause_check and self.pause_check():
            self._check_cancelled()
            time.sleep(0.2)

    def _emit(self, percent: int, stage: str, **details) -> None:
        if self.progress_callback:
            self.progress_callback({"percent": max(0, min(100, percent)), "stage": stage, **details})

    def _find_local_window(self, symbol: str, start_ms: int, end_ms: int) -> tuple[Path, Path] | None:
        candidates = []
        for manifest_path in (self.root / "data").rglob("*.manifest.json"):
            try:
                manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
                if manifest.get("symbol") != symbol or manifest.get("interval") != "1m":
                    continue
                raw = manifest_path.with_suffix("").with_suffix(".jsonl")
                if not raw.exists():
                    continue
                rows = read_jsonl(raw)
                if rows and rows[0].open_time_ms <= start_ms and rows[-1].open_time_ms >= end_ms:
                    candidates.append((len(rows), raw, manifest_path, rows))
            except Exception:
                continue
        if not candidates:
            return None
        _count, raw, manifest_path, rows = min(candidates, key=lambda item: item[0])
        selected = [row for row in rows if start_ms <= row.open_time_ms <= end_ms]
        if len(selected) == len(rows):
            return raw, manifest_path
        cache = self.root / "data" / "cache" / "automatic_windows" / symbol
        window_path = cache / f"{start_ms}-{end_ms}.jsonl"
        window_manifest = window_path.with_suffix(".manifest.json")
        if not window_path.exists() or not window_manifest.exists():
            write_jsonl(window_path, selected)
            checksum = hashlib.sha256(window_path.read_bytes()).hexdigest()
            quality = validate_candles(selected, expected_start_ms=start_ms, expected_end_open_ms=end_ms)
            if not quality.valid:
                raise ValueError(f"Локальная база {symbol} не покрывает выбранный период без пропусков")
            source_manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
            value = DatasetManifest.create(
                f"cached-{symbol}-{start_ms}-{end_ms}", symbol, "1m", quality, checksum,
                source_manifest.get("origin_url", "local-cache"),
            )
            partial = window_manifest.with_suffix(window_manifest.suffix + ".partial")
            partial.write_text(json.dumps(value.to_dict(), ensure_ascii=False, indent=2), encoding="utf-8")
            partial.replace(window_manifest)
        return window_path, window_manifest

    @staticmethod
    def _requested_end_ms(config: AutomationConfig) -> int:
        value = config.end_ms
        if value is None:
            value = (int(time.time() * 1000) // ONE_MINUTE_MS) * ONE_MINUTE_MS - ONE_MINUTE_MS
        return value - value % ONE_MINUTE_MS

    def _selection_path(self, months: int) -> Path:
        return self.root / "data" / "prepared" / f"research-base-{months}m.json"

    def load_prepared_data(self, config: AutomationConfig) -> dict | None:
        config.validate()
        path = self._selection_path(config.months)
        if not path.exists():
            return None
        try:
            value = json.loads(path.read_text(encoding="utf-8"))
            age_ms = self._requested_end_ms(config) - int(value["period"]["end_ms"])
            if value.get("months") != config.months or age_ms < 0:
                return None
            if age_ms > CACHE_MAX_AGE_DAYS * 86_400_000:
                return None
            for symbol in SYMBOLS:
                item = value["datasets"][symbol]
                if not Path(item["raw_path"]).exists() or not Path(item["manifest_path"]).exists():
                    return None
            return value | {"selection_path": str(path), "age_days": age_ms / 86_400_000, "reused": True}
        except (KeyError, TypeError, ValueError, OSError, json.JSONDecodeError):
            return None

    def _find_recent_common_period(self, config: AutomationConfig) -> tuple[int, int] | None:
        requested_end = self._requested_end_ms(config)
        duration_ms = (PERIOD_DAYS[config.months] * 1440 - 1) * ONE_MINUTE_MS
        by_symbol: dict[str, list[tuple[int, int]]] = {symbol: [] for symbol in SYMBOLS}
        for symbol in SYMBOLS:
            directory = self.root / "data" / "raw" / "binance_spot" / symbol / "1m"
            for manifest in directory.glob("*.manifest.json"):
                raw = manifest.with_suffix("").with_suffix(".jsonl")
                try:
                    start_text, end_text = raw.stem.split("-", 1)
                    start_ms, end_ms = int(start_text), int(end_text)
                    if raw.exists() and end_ms <= requested_end:
                        by_symbol[symbol].append((start_ms, end_ms))
                except (ValueError, OSError):
                    continue
        if any(not values for values in by_symbol.values()):
            return None
        for end_ms in sorted({end for values in by_symbol.values() for _start, end in values}, reverse=True):
            if requested_end - end_ms > CACHE_MAX_AGE_DAYS * 86_400_000:
                break
            start_ms = end_ms - duration_ms
            if all(any(start <= start_ms and end >= end_ms for start, end in by_symbol[symbol]) for symbol in SYMBOLS):
                return start_ms, end_ms
        return None

    @staticmethod
    def _dataset_record(raw: Path, manifest: Path, reused: bool) -> dict:
        value = json.loads(manifest.read_text(encoding="utf-8"))
        return {
            "raw_path": str(raw), "manifest_path": str(manifest), "row_count": value["row_count"],
            "checksum_sha256": value["checksum_sha256"], "size_bytes": raw.stat().st_size,
            "quality": "PASS" if not value["gaps"] and not value["duplicates"] else "FAIL", "reused": reused,
        }

    def prepare_data(self, config: AutomationConfig) -> dict:
        """Stage 1: prepare one fixed aligned base. Optimization is never started here."""
        config.validate()
        existing = self.load_prepared_data(config)
        if existing:
            self._emit(100, "БАЗА ГОТОВА — повторное скачивание не требуется", phase="DATA", reused=True)
            return existing
        requested_end = self._requested_end_ms(config)
        days = PERIOD_DAYS[config.months]
        recent = self._find_recent_common_period(config)
        if recent:
            start_ms, end_ms = recent
            source = "LOCAL_CACHE"
        else:
            end_ms = requested_end
            start_ms = end_ms - (days * 1440 - 1) * ONE_MINUTE_MS
            source = "BINANCE_DOWNLOAD"
        rows_per_symbol = ((end_ms - start_ms) // ONE_MINUTE_MS) + 1
        total_rows = rows_per_symbol * len(SYMBOLS)
        datasets: dict[str, dict] = {}
        manager = self.manager_factory(self.root / "data")
        self._emit(0, "ЭТАП 1 — ПОДГОТОВКА БАЗЫ", phase="DATA", downloaded_rows=0, total_rows=total_rows)
        for symbol_index, symbol in enumerate(SYMBOLS):
            self._check_cancelled(); self._wait_if_paused()
            base_rows = symbol_index * rows_per_symbol

            def download_progress(done: int, _total: int, current_symbol: str = symbol) -> None:
                self._check_cancelled(); self._wait_if_paused()
                global_done = base_rows + min(done, rows_per_symbol)
                self._emit(
                    int(global_done / total_rows * 100),
                    f"СКАЧИВАНИЕ — {current_symbol}: {done:,} / {rows_per_symbol:,} свечей",
                    phase="DATA", downloaded_rows=global_done, total_rows=total_rows, reused=False,
                )

            local = self._find_local_window(symbol, start_ms, end_ms)
            if local:
                raw, manifest = local
                reused = True
                self._emit(
                    int((base_rows + rows_per_symbol) / total_rows * 100),
                    f"ЛОКАЛЬНАЯ БАЗА — {symbol} готов",
                    phase="DATA", downloaded_rows=base_rows + rows_per_symbol, total_rows=total_rows, reused=True,
                )
            else:
                raw, manifest = manager.download_candles(symbol, start_ms, end_ms, progress_callback=download_progress)
                reused = False
            datasets[symbol] = self._dataset_record(raw, manifest, reused)
        payload = {
            "version": "prepared-research-base-v1", "months": config.months,
            "prepared_at_utc": datetime.now(timezone.utc).isoformat(), "expires_after_days": CACHE_MAX_AGE_DAYS,
            "source": source, "period": {"months": config.months, "days": days, "start_ms": start_ms, "end_ms": end_ms},
            "datasets": datasets,
        }
        self._write_json(self._selection_path(config.months), payload)
        result = payload | {"selection_path": str(self._selection_path(config.months)), "age_days": (requested_end - end_ms) / 86_400_000, "reused": source == "LOCAL_CACHE"}
        self._emit(100, "БАЗА ГОТОВА — можно запускать расчёт", phase="DATA", reused=result["reused"])
        return result

    def run(self, config: AutomationConfig, prepared: dict | None = None) -> dict:
        config.validate()
        hardware = detect_hardware()
        workers = workers_for_mode(hardware, config.resource_mode)
        save_hardware_profile(self.root / "logs" / "hardware-profile.json", hardware, config.resource_mode, workers)
        if prepared is None:
            prepared = self.prepare_data(config)
        datasets = prepared["datasets"]
        days = int(prepared["period"]["days"])
        start_ms = int(prepared["period"]["start_ms"]); end_ms = int(prepared["period"]["end_ms"])
        total_rows = sum(int(item["row_count"]) for item in datasets.values())

        self._emit(1, "ЭТАП 2 — ПРОВЕРКА КАЧЕСТВА", phase="COMPUTE", total_rows=total_rows)
        holdout = seal_immutable_holdout(Path(datasets["PUMPUSDT"]["raw_path"]), self.root / "data")
        pump_rows = read_jsonl(Path(holdout.development_path))
        btc_rows = read_jsonl(Path(datasets["BTCUSDT"]["raw_path"]))
        sol_rows = read_jsonl(Path(datasets["SOLUSDT"]["raw_path"]))
        quality = validate_candles(pump_rows)
        if not quality.valid:
            raise ValueError("PUMP dataset не прошёл финальную проверку качества")

        self._emit(4, "РАСЧЁТ — контрольная AUTO X", phase="COMPUTE", total_rows=total_rows)
        replay = replay_economy(pump_rows, evaluation_start_ms=start_ms, evaluation_end_ms=end_ms + ONE_MINUTE_MS)
        replay_payload = replay.to_dict()
        metrics = replay_payload["metrics"]

        self._emit(8, "РАСЧЁТ — синтетические проверки", phase="COMPUTE", total_rows=total_rows)
        synthetic = run_synthetic_school()

        if len(pump_rows) >= 4_000:
            def optimizer_progress(value: dict) -> None:
                self._emit(
                    10 + int(value.get("percent", 0) * 0.85),
                    f"ПЕРЕБОР: {value.get('tested', 0):,} / {value.get('total', 0):,}",
                    phase="COMPUTE", tested=value.get("tested", 0), variants_total=value.get("total", 0),
                    total_rows=total_rows,
                    workers=value.get("workers", 1),
                    variants_per_second=value.get("variants_per_second", 0.0),
                    eta_seconds=value.get("eta_seconds"),
                    best=value.get("best", "—"),
                    best_provisional=value.get("best_provisional", {}),
                    checkpoint=value.get("checkpoint", ""),
                    interesting_archive=value.get("interesting_archive", {}),
                )

            pump_checksum = datasets["PUMPUSDT"]["checksum_sha256"][:16]
            checkpoint_path = self.root / "checkpoints" / f"automatic-{config.months}m-{pump_checksum}-{config.search_mode.lower()}.json"
            if config.search_mode.upper() == "ADAPTIVE":
                checkpoint_path = self.root / "checkpoints" / f"automatic-{config.months}m-{pump_checksum}-adaptive-v8.json"
                variant_factory = optimize_adaptive(
                    pump_rows, optimizer_progress, btc_rows, sol_rows,
                    self.cancel_check, self.pause_check, workers, checkpoint_path,
                    ready_reports_directory(self.root),
                )
            else:
                variant_factory = optimize_variants(
                    pump_rows, config.search_mode, optimizer_progress, btc_rows, sol_rows,
                    self.cancel_check, self.pause_check, workers, checkpoint_path,
                )
            variant_factory["immutable_holdout"] = holdout.to_dict()
        else:
            variant_factory = {
                "verdict": "INSUFFICIENT_DATA",
                "variants_tested": 0,
                "explanation": "Для фабрики вариантов требуется минимум 4000 минутных свечей.",
            }

        final_capital = config.initial_capital * (1.0 + metrics["compound_net"])
        historical_profit = final_capital - config.initial_capital
        running_capital = config.initial_capital
        evaluation_start_ms = pump_rows[0].open_time_ms
        evaluation_end_ms = pump_rows[-1].close_time_ms
        development_days = len(pump_rows) / 1440
        capital_timeline = [{"time_ms": evaluation_start_ms, "capital": running_capital, "event": "START"}]
        for trade in replay_payload["trades"]:
            running_capital *= 1.0 + float(trade["net_return"])
            capital_timeline.append(
                {"time_ms": trade["exit_time_ms"], "capital": running_capital, "event": trade["reason"]}
            )
        pf = metrics["profit_factor"]
        enough_trades = metrics["fills"] >= 30
        historically_positive = metrics["compound_net"] > 0 and pf is not None and pf > 1
        returns = [float(item["net_return"]) for item in replay_payload["trades"]]
        mean = sum(returns) / len(returns) if returns else 0.0
        variance = sum((item - mean) ** 2 for item in returns) / (len(returns) - 1) if len(returns) > 1 else 0.0
        approximate_ci = 2.026 * math.sqrt(variance) / math.sqrt(len(returns)) if returns else 0.0
        baseline_analysis = {
            "role": "FIXED_CONTROL_BASELINE_NOT_FACTORY_WINNER",
            "development_only": True,
            "fills": len(returns), "mean_net": mean,
            "approximate_mean_net_95ci": [mean - approximate_ci, mean + approximate_ci],
            "confidence_interval_contains_zero": mean - approximate_ci <= 0 <= mean + approximate_ci,
        }
        adaptive_preview = variant_factory.get("live_state", {}).get("equity_preview", {})
        adaptive_metrics = adaptive_preview.get("metrics", {})
        adaptive_capital = {
            "currency": config.currency,
            "initial": config.initial_capital,
            "historical_final": config.initial_capital * (1.0 + float(adaptive_metrics.get("compound_net", 0.0))),
            "historical_profit": config.initial_capital * float(adaptive_metrics.get("compound_net", 0.0)),
            "historical_return": float(adaptive_metrics.get("compound_net", 0.0)),
            "timeline": adaptive_preview.get("timeline", []),
        } if adaptive_preview else None
        if historically_positive and enough_trades:
            conclusion = "Контрольная AUTO X положительна на development-части; фабрика оценивается отдельно и этот результат не является прогнозом."
        elif historically_positive:
            conclusion = "Результат положительный, но сделок недостаточно для надёжного вывода."
        else:
            conclusion = "Контрольная AUTO X не подтвердила прибыльность на development-части."
        conclusion = f"Адаптивный поиск: {variant_factory.get('explanation', 'результат не получен')} Контроль: {conclusion}"

        recommendations = []
        if variant_factory.get("research_stage") == "EXPLORATORY":
            recommendations.append("Использовать 3 или 6 месяцев данных: короткий запуск предназначен только для разведки и не открывает TEST.")
        if not variant_factory.get("diagnostics", {}).get("eligible_retained_candidates", 0):
            recommendations.append("Проверить причины отсева по каждому validation-fold; не выбирать кандидата по одной-двум удачным сделкам.")
        if baseline_analysis["confidence_interval_contains_zero"]:
            recommendations.append("Накопить больше независимых сделок AUTO X: приближённый интервал среднего NET пока включает ноль.")
        recommendations.extend([
            "Следующий уровень данных для идей про sweep/absorption — trade tape и L2; минутные свечи этого не доказывают.",
            "Новые переменные добавлять отдельными версиями эксперимента и оценивать вне данных, на которых они придуманы.",
        ])

        stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
        output = ready_reports_directory(self.root)
        payload = {
            "report_version": "automatic-research-v3-v8-ai-handoff",
            "created_at_utc": datetime.now(timezone.utc).isoformat(),
            "simulation": True,
            "real_orders": False,
            "downloaded_period": {"months": config.months, "days": days, "start_ms": start_ms, "end_ms": end_ms},
            "period": {"months": config.months, "days": development_days, "start_ms": evaluation_start_ms, "end_ms": evaluation_end_ms},
            "capital": {
                "currency": config.currency,
                "initial": config.initial_capital,
                "historical_final": final_capital,
                "historical_profit": historical_profit,
                "historical_return": metrics["compound_net"],
                "minimum": min(item["capital"] for item in capital_timeline),
                "maximum": max(item["capital"] for item in capital_timeline),
                "timeline": capital_timeline,
            },
            "datasets": datasets,
            "strategy": replay_payload,
            "baseline_analysis": baseline_analysis,
            "synthetic_test": synthetic,
            "variant_factory": variant_factory,
            "best_candidate_analysis": {
                "role": "CURRENT_ADAPTIVE_TRAIN_VALIDATION_LEADER",
                "capital": adaptive_capital,
                "metrics": adaptive_metrics,
                "trades": adaptive_preview.get("trades", []),
                "parameters": variant_factory.get("selected", {}).get("config", {}),
                "not_a_forecast": True,
            },
            "hardware": hardware.to_dict() | {
                "resource_mode": config.resource_mode, "workers": workers,
                "exact_replay_device": "CPU", "gpu_used_for_exact_replay": False,
                "gpu_note": "GPU обнаружена, но ветвящийся точный candle replay этой версией выполняется на CPU.",
            },
            "immutable_holdout": holdout.to_dict(),
            "conclusion": conclusion,
            "next_research_recommendations": recommendations,
            "ai_handoff": {
                "question": "Есть ли устойчивый положительный кандидат и что исследовать следующим?",
                "decision_order": ["validation_sample", "average_net", "profit_factor", "compound_net", "max_drawdown", "unseen_test", "stress", "immutable_holdout"],
                "do_not_infer": ["Синтетический PASS не доказывает прибыльность", "Baseline AUTO X не является победителем фабрики", "EUR является масштабом капитала, а не валютной конвертацией"],
            },
            "limitations": [
                "Это историческая симуляция, а не обещание будущей прибыли.",
                "Комиссии и adverse slippage включены; latency и рыночное воздействие моделируются ограниченно.",
                "BTC и SOL используются фабрикой вариантов как причинные режимные фильтры; защищённый baseline AUTO X остаётся PUMP-only контролем.",
                "Последние 20% PUMP физически отделены в immutable_test и не используются фабрикой.",
                "Отображение капитала в EUR не моделирует курс EUR/USDT; это только выбранный масштаб.",
                "Реальные ордера отключены.",
            ],
        }
        canonical = json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False)
        payload["report_hash_sha256"] = hashlib.sha256(canonical.encode("utf-8")).hexdigest()
        json_path = output / f"PUMP_RESEARCH_REPORT_{stamp}.json"
        md_path = output / f"PUMP_RESEARCH_REPORT_{stamp}.md"
        zip_path = output / f"PUMP_RESEARCH_PACKAGE_{stamp}.zip"
        trades_csv = output / f"PUMP_TRADES_{stamp}.csv"
        leaders_csv = output / f"PUMP_CANDIDATES_{stamp}.csv"
        self._emit(97, "СОЗДАНИЕ ОТЧЁТА ДЛЯ СЛЕДУЮЩЕГО ЭТАПА", phase="COMPUTE", total_rows=total_rows)
        self._write_json(json_path, payload)
        self._write_markdown(md_path, payload)
        self._write_csv(trades_csv, replay_payload.get("trades", []))
        leader_rows = []
        for candidate in variant_factory.get("leaderboard", []):
            leader_rows.append({
                "candidate_id": candidate.get("candidate_id"), "eligible": candidate.get("eligible"),
                "train_fills": candidate.get("train", {}).get("fills", 0),
                "validation_a_fills": candidate.get("validation_a", {}).get("fills", 0),
                "validation_b_fills": candidate.get("validation_b", {}).get("fills", 0),
                "validation_a_avg_net": candidate.get("validation_a", {}).get("average_net", 0),
                "validation_b_avg_net": candidate.get("validation_b", {}).get("average_net", 0),
                "rejection_reasons": ";".join(candidate.get("rejection_reasons", [])),
                "config_json": json.dumps(candidate.get("config", {}), ensure_ascii=False, separators=(",", ":")),
            })
        self._write_csv(leaders_csv, leader_rows)
        with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
            archive.write(json_path, json_path.name)
            archive.write(md_path, md_path.name)
            archive.write(trades_csv, trades_csv.name)
            archive.write(leaders_csv, leaders_csv.name)
            for symbol, dataset in datasets.items():
                manifest_path = Path(dataset["manifest_path"])
                archive.write(manifest_path, f"manifests/{symbol}/{manifest_path.name}")
        latest_json = output / "LATEST_PUMP_RESEARCH.json"
        latest_md = output / "LATEST_PUMP_RESEARCH.md"
        self._write_json(latest_json, payload)
        latest_md_partial = latest_md.with_suffix(latest_md.suffix + ".partial")
        latest_md_partial.write_text(md_path.read_text(encoding="utf-8"), encoding="utf-8")
        latest_md_partial.replace(latest_md)
        self._emit(100, "ГОТОВО — отчёт сохранён и доступен для анализа", phase="COMPUTE", total_rows=total_rows)
        return {
            "status": "PASS",
            "report": str(md_path),
            "json": str(json_path),
            "package": str(zip_path),
            "report_directory": str(output),
            "latest_report": str(latest_md),
            "latest_json": str(latest_json),
            "trades_csv": str(trades_csv),
            "candidates_csv": str(leaders_csv),
            "capital": payload["capital"],
            "metrics": metrics,
            "trades": replay_payload["trades"],
            "best_candidate": payload["best_candidate_analysis"],
            "hardware": payload["hardware"],
            "variant_factory": variant_factory,
            "immutable_holdout": holdout.to_dict(),
            "conclusion": conclusion,
            "downloaded_rows": total_rows,
            "downloaded_bytes": sum(item["size_bytes"] for item in datasets.values()),
        }

    @staticmethod
    def _write_json(path: Path, payload: dict) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        partial = path.with_suffix(path.suffix + ".partial")
        partial.write_text(json.dumps(payload, ensure_ascii=False, indent=2, allow_nan=False), encoding="utf-8")
        partial.replace(path)

    @staticmethod
    def _write_csv(path: Path, rows: list[dict]) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        partial = path.with_suffix(path.suffix + ".partial")
        fields = list(rows[0]) if rows else ["empty"]
        with partial.open("w", encoding="utf-8-sig", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=fields, extrasaction="ignore")
            writer.writeheader()
            writer.writerows(rows)
        partial.replace(path)

    @staticmethod
    def _write_markdown(path: Path, payload: dict) -> None:
        metrics = payload["strategy"]["metrics"]
        capital = payload["capital"]
        pf = "n/a" if metrics["profit_factor"] is None else f"{metrics['profit_factor']:.4f}"
        datasets = "\n".join(
            f"- {symbol}: {item['row_count']:,} свечей, {item['size_bytes'] / 1_048_576:.1f} MB, качество {item['quality']}"
            for symbol, item in payload["datasets"].items()
        )
        factory = payload.get("variant_factory", {"variants_tested": 0, "verdict": "NOT_RUN", "explanation": "Фабрика не запускалась."})
        factory_text = (
            f"- Проверено вариантов: {factory.get('variants_tested', 0):,}\n"
            f"- Тип поиска: {factory.get('mode', factory.get('engine', '—'))}\n"
            f"- Поколений: {factory.get('generations', '—')}\n"
            f"- Стадия исследования: {factory.get('research_stage', '—')}\n"
            f"- Решение фабрики: {factory.get('verdict', '—')}\n"
            f"- Объяснение: {factory.get('explanation', '—')}"
        )
        hardware = payload.get("hardware", {})
        baseline = payload.get("baseline_analysis", {})
        ci = baseline.get("approximate_mean_net_95ci", [0.0, 0.0])
        recommendations = "\n".join(f"- {item}" for item in payload.get("next_research_recommendations", [])) or "- Нет"
        timeline_rows = []
        for item in capital.get("timeline", []):
            moment = datetime.fromtimestamp(item["time_ms"] / 1000, tz=timezone.utc).strftime("%Y-%m-%d %H:%M")
            timeline_rows.append(f"| {moment} | {item['event']} | {item['capital']:.2f} {capital['currency']} |")
        timeline_text = "\n".join(timeline_rows) if timeline_rows else "| — | — | — |"
        adaptive = payload.get("best_candidate_analysis", {})
        adaptive_capital = adaptive.get("capital") or {}
        adaptive_metrics = adaptive.get("metrics") or {}
        adaptive_parameters = json.dumps(adaptive.get("parameters", {}), ensure_ascii=False, indent=2)
        adaptive_summary = (
            f"- Development-капитал: {adaptive_capital.get('initial', 0):.2f} {capital['currency']} → "
            f"{adaptive_capital.get('historical_final', 0):.2f} {capital['currency']}\n"
            f"- Сделки: {adaptive_metrics.get('fills', 0)}; частота: {adaptive_metrics.get('trades_per_day', 0):.3f}/сутки\n"
            f"- Compound NET: {adaptive_metrics.get('compound_net', 0) * 100:+.3f}%; "
            f"Max DD: {adaptive_metrics.get('max_drawdown', 0) * 100:.3f}%"
            if adaptive_capital else "- Адаптивный лидер пока отсутствует"
        )
        text = f"""# PUMP Research Lab — автоматический отчёт

## Короткий итог — контрольная AUTO X

{payload['conclusion']}

- Скачано: {payload.get('downloaded_period', payload['period'])['days']} дней; фактически оценено: {payload['period']['days']:.1f} дня development
- Историческая симуляция капитала: {capital['initial']:.2f} {capital['currency']} → {capital['historical_final']:.2f} {capital['currency']}
- Исторический результат: {capital['historical_profit']:+.2f} {capital['currency']} ({capital['historical_return'] * 100:+.3f}%)
- Сделки: {metrics['fills']}; победы: {metrics['wins']}; win rate: {metrics['win_rate'] * 100:.2f}%
- Average NET: {metrics['average_net'] * 100:+.4f}%; Profit Factor: {pf}
- Приближённый 95% интервал среднего NET: {ci[0] * 100:+.4f}% … {ci[1] * 100:+.4f}%
- Роль результата: фиксированный контрольный baseline, не победитель фабрики
- Синтетический тест механики: {payload['synthetic_test']['status']}
- Синтетические сценарии: {payload['synthetic_test'].get('passed', 0)}/{payload['synthetic_test'].get('total', 0)}

## Использованный компьютер

- CPU: {hardware.get('cpu', '—')}
- Ядра / потоки: {hardware.get('physical_cores', '—')} / {hardware.get('logical_processors', '—')}
- RAM: {hardware.get('ram_gb', '—')} GB
- GPU: {hardware.get('gpu', '—')} ({hardware.get('gpu_vram_mb', 0)} MB)
- Точный replay: CPU; GPU в этом алгоритме не используется
- Процессов точного replay: {hardware.get('workers', '—')}

## Автоматическая фабрика вариантов

{factory_text}

- Допущенных сохранённых кандидатов: {factory.get('diagnostics', {}).get('eligible_retained_candidates', 0)}
- TEST открыт: {'да' if factory.get('split', {}).get('test_was_opened') else 'нет'}

## Лучший найденный вариант — TRAIN/VALIDATION

{adaptive_summary}

Параметры, приведшие к этому промежуточному результату:

```json
{adaptive_parameters}
```

Этот блок показывает текущего лидера исследования, а не прогноз будущей прибыли.

## Загруженные данные

{datasets}

## Движение выбранных {capital['initial']:.2f} {capital['currency']}

| Время UTC | Событие | Капитал после события |
|---|---|---:|
{timeline_text}

## Что исследовать следующим

{recommendations}

## Ограничения

""" + "\n".join(f"- {item}" for item in payload["limitations"]) + f"""

## Для повторной проверки

- SHA-256 отчёта: `{payload['report_hash_sha256']}`
- Полные параметры, сделки, manifests и метрики находятся в JSON/ZIP рядом с этим файлом.
"""
        partial = path.with_suffix(path.suffix + ".partial")
        partial.write_text(text, encoding="utf-8")
        partial.replace(path)
