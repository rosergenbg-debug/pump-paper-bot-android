from __future__ import annotations

import hashlib
import heapq
import json
import math
import os
import time
from concurrent.futures import ProcessPoolExecutor
from dataclasses import asdict, replace
from datetime import datetime, timezone
from pathlib import Path
from typing import Callable

from .baseline_x import EconomyConfig, ReplayResult, Signal, Trade, core_signal_at, net_return, price_for_net, replay_economy
from .domain import Candle
from .validation import require_replay_quality


ProgressCallback = Callable[[dict], None]
MODE_COUNTS = {"FAST": 10_000, "STANDARD": 250_000, "DEEP": 2_000_000, "MAXIMUM": 10_000_000}
MODE_MIN_DAYS = {"FAST": 15, "STANDARD": 30, "DEEP": 90, "MAXIMUM": 180}
MIN_CONFIRMATORY_HISTORY_DAYS = 60


class ResearchCancelled(Exception):
    pass


def effective_mode(requested_mode: str, history_days: float) -> str:
    requested_mode = requested_mode.upper()
    if requested_mode not in MODE_COUNTS:
        raise ValueError("Неизвестный режим фабрики")
    # The user controls compute depth. Short history is reported as an evidence
    # limitation; it must not silently reduce the requested computation.
    return requested_mode


CONTEXT_RULES = ("ANY", "UP_15", "UP_60", "DOWN_15", "DOWN_60")
HALTON_BASES = (2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47)
DIMENSION_NAMES = (
    "drawdown_gate", "limit_discount", "limit_ttl_minutes", "target_net", "stop_net",
    "max_hold_minutes", "max_entries_per_utc_day", "min_hours_between_entries",
    "btc_context_rule", "sol_context_rule", "vwap_deviation_gate", "min_buy_share",
    "min_buy_share_delta", "min_green_candle_return", "min_volume_ratio_20m",
)


def _variant_dimensions(context_ready: bool) -> tuple[tuple[object, ...], ...]:
    rules = CONTEXT_RULES if context_ready else ("ANY",)
    return (
        (-0.02, -0.03, -0.04, -0.05, -0.06, -0.08, -0.10, -0.15),
        (0.0, 0.0005, 0.001, 0.0015, 0.0025, 0.004), (1, 2, 3, 5, 10),
        (0.005, 0.01, 0.015, 0.02, 0.025, 0.03, 0.04, 0.05, 0.075, 0.10, 0.15, 0.25, 0.50, 1.0),
        (-0.005, -0.008, -0.01, -0.012, -0.015, -0.02, -0.03, -0.04, -0.06, -0.10, -0.20),
        (120, 180, 360, 720, 1440, 2880, 4320, 7200, 10080, 14400),
        (1, 2, 3, 4, 5, 6, 8, 10), (0, 6, 12, 24, 48, 72, 168), rules, rules,
        (-0.002, -0.004, -0.006, -0.008, -0.010, -0.015, -0.020, -0.030),
        (0.48, 0.50, 0.52, 0.55, 0.58, 0.60, 0.65),
        (-0.02, 0.0, 0.01, 0.02, 0.05, 0.10),
        (0.0, 0.001, 0.002, 0.004, 0.008, 0.015),
        (0.0, 0.50, 0.75, 1.0, 1.5, 2.0, 3.0, 5.0),
    )


def _config_at(index: int, dimensions: tuple[tuple[object, ...], ...]) -> EconomyConfig:
    """Decode one deterministic point from the full parameter space without
    allocating the whole million-plus point grid in memory."""
    choices: list[object] = []
    for values in reversed(dimensions):
        index, offset = divmod(index, len(values))
        choices.append(values[offset])
    gate, discount, ttl, target, stop, hold, daily, entry_gap, btc_rule, sol_rule, vwap_gate, buy_share, buy_delta, candle_return, volume_ratio = reversed(choices)
    return EconomyConfig(
        drawdown_gate=float(gate), limit_discount=float(discount), limit_ttl_minutes=int(ttl),
        target_net=float(target), stop_net=float(stop), max_hold_minutes=int(hold),
        max_entries_per_utc_day=int(daily), min_hours_between_entries=int(entry_gap), btc_context_rule=str(btc_rule), sol_context_rule=str(sol_rule),
        vwap_deviation_gate=float(vwap_gate), min_buy_share=float(buy_share),
        min_buy_share_delta=float(buy_delta), min_green_candle_return=float(candle_return),
        min_volume_ratio_20m=float(volume_ratio),
    )


def _radical_inverse(index: int, base: int) -> float:
    value = 0.0
    denominator = 1.0
    while index:
        index, digit = divmod(index, base)
        denominator *= base
        value += digit / denominator
    return value


def _sample_config(index: int, dimensions: tuple[tuple[object, ...], ...]) -> EconomyConfig:
    """Low-discrepancy deterministic sampling covers every dimension even in FAST.

    A linear mixed-radix stride can alias the last dimensions when only a tiny
    fraction of a billion-point grid is inspected. Independent Halton bases
    avoid that blind spot while remaining reproducible and checkpoint-safe.
    """
    choices = []
    for dimension, (values, base) in enumerate(zip(dimensions, HALTON_BASES, strict=True)):
        fraction = _radical_inverse(index + 1 + dimension * 17, base)
        choices.append(values[min(len(values) - 1, int(fraction * len(values)))])
    gate, discount, ttl, target, stop, hold, daily, entry_gap, btc_rule, sol_rule, vwap_gate, buy_share, buy_delta, candle_return, volume_ratio = choices
    return EconomyConfig(
        drawdown_gate=float(gate), limit_discount=float(discount), limit_ttl_minutes=int(ttl),
        target_net=float(target), stop_net=float(stop), max_hold_minutes=int(hold),
        max_entries_per_utc_day=int(daily), min_hours_between_entries=int(entry_gap), btc_context_rule=str(btc_rule), sol_context_rule=str(sol_rule),
        vwap_deviation_gate=float(vwap_gate), min_buy_share=float(buy_share),
        min_buy_share_delta=float(buy_delta), min_green_candle_return=float(candle_return),
        min_volume_ratio_20m=float(volume_ratio),
    )


def generate_variants(mode: str, context_ready: bool = True) -> list[EconomyConfig]:
    mode = mode.upper()
    if mode not in MODE_COUNTS:
        raise ValueError("Неизвестный режим фабрики")
    return list(iter_variants(mode, context_ready))


def iter_variants(mode: str, context_ready: bool = True):
    mode = mode.upper()
    if mode not in MODE_COUNTS:
        raise ValueError("Неизвестный режим фабрики")
    dimensions = _variant_dimensions(context_ready)
    total = math.prod(len(values) for values in dimensions)
    requested = min(MODE_COUNTS[mode], total)
    protected = EconomyConfig()
    for index in range(requested):
        config = _sample_config(index, dimensions)
        yield protected if index == 0 else config


def variant_at(mode: str, zero_index: int, context_ready: bool = True) -> EconomyConfig:
    mode = mode.upper()
    if mode not in MODE_COUNTS:
        raise ValueError("Неизвестный режим фабрики")
    dimensions = _variant_dimensions(context_ready)
    total = math.prod(len(values) for values in dimensions)
    requested = min(MODE_COUNTS[mode], total)
    if not 0 <= zero_index < requested:
        raise IndexError("Индекс варианта вне выбранного режима")
    return EconomyConfig() if zero_index == 0 else _sample_config(zero_index, dimensions)


def full_space_count(context_ready: bool) -> int:
    return math.prod(len(values) for values in _variant_dimensions(context_ready))


def build_context_cache(rows: list[Candle], btc_rows: list[Candle] | None, sol_rows: list[Candle] | None) -> dict[int, tuple[float | None, float | None, float | None, float | None]]:
    """Return only information closed before each PUMP signal minute."""
    def returns(source: list[Candle] | None) -> dict[int, tuple[float | None, float | None]]:
        if not source:
            return {}
        by_time = {item.open_time_ms: item for item in source}
        result = {}
        for item in source:
            r15 = by_time.get(item.open_time_ms - 15 * 60_000)
            r60 = by_time.get(item.open_time_ms - 60 * 60_000)
            result[item.open_time_ms] = (
                None if r15 is None else item.close / r15.close - 1.0,
                None if r60 is None else item.close / r60.close - 1.0,
            )
        return result
    btc, sol = returns(btc_rows), returns(sol_rows)
    return {index: (*(btc.get(row.open_time_ms, (None, None))), *(sol.get(row.open_time_ms, (None, None)))) for index, row in enumerate(rows)}


def _context_allows(rule: str, return_15: float | None, return_60: float | None) -> bool:
    if rule == "ANY":
        return True
    if return_15 is None or return_60 is None:
        return False
    return {
        "UP_15": return_15 >= 0.001,
        "UP_60": return_60 >= 0.002,
        "DOWN_15": return_15 <= -0.001,
        "DOWN_60": return_60 <= -0.002,
    }[rule]


def build_signal_cache(rows: list[Candle]) -> list[Signal]:
    """Compute the protected core signal once; every variant reuses exactly these causal signals."""
    signals: list[Signal] = []
    for index in range(719, len(rows)):
        current = rows[index]
        previous = rows[index - 1]
        if current.close <= current.open or current.buy_share < 0.50 or current.buy_share <= previous.buy_share:
            continue
        signal = core_signal_at(rows, index)
        if signal is not None:
            signals.append(signal)
    return signals


def replay_cached(
    rows: list[Candle],
    signals: list[Signal],
    config: EconomyConfig,
    start_index: int,
    end_index: int,
    context_cache: dict[int, tuple[float | None, float | None, float | None, float | None]] | None = None,
) -> ReplayResult:
    start_index = max(start_index, 719)
    last_signal_exclusive = end_index - config.max_hold_minutes - 5
    if last_signal_exclusive <= start_index:
        raise ValueError("Evaluation window is too short")
    trades: list[Trade] = []
    signal_count = fill_count = 0
    entries_by_day: dict[str, int] = {}
    last_entry_time_ms: int | None = None
    cursor = start_index
    for signal in signals:
        index = signal.index
        if index < cursor or index < start_index:
            continue
        if index >= last_signal_exclusive:
            break
        if signal.drawdown_12h > config.drawdown_gate:
            continue
        if signal.deviation > config.vwap_deviation_gate:
            continue
        if signal.buy_share < config.min_buy_share or signal.buy_share_delta < config.min_buy_share_delta:
            continue
        if signal.green_candle_return < config.min_green_candle_return or signal.volume_ratio_20m < config.min_volume_ratio_20m:
            continue
        if config.btc_context_rule != "ANY" or config.sol_context_rule != "ANY":
            if context_cache is None:
                raise ValueError("Для BTC/SOL-фильтра требуется причинно выровненный контекст")
            btc15, btc60, sol15, sol60 = context_cache.get(index, (None, None, None, None))
            if not _context_allows(config.btc_context_rule, btc15, btc60) or not _context_allows(config.sol_context_rule, sol15, sol60):
                continue
        signal_count += 1
        limit = signal.close * (1.0 - config.limit_discount)
        fill_index = None
        entry = 0.0
        first_fill_index = index + 1 + config.entry_latency_minutes
        for candidate_index in range(first_fill_index, min(first_fill_index + config.limit_ttl_minutes, len(rows))):
            candle = rows[candidate_index]
            if candle.low <= limit:
                fill_index = candidate_index
                entry = min(limit, candle.open) * (1.0 + config.spread_rate / 2.0)
                break
        if fill_index is None:
            cursor = index + config.entry_latency_minutes + config.limit_ttl_minutes + 1
            continue
        day = datetime.fromtimestamp(rows[fill_index].open_time_ms / 1000, tz=timezone.utc).strftime("%Y%m%d")
        if last_entry_time_ms is not None and rows[fill_index].open_time_ms - last_entry_time_ms < config.min_hours_between_entries * 3_600_000:
            cursor = index + 1
            continue
        if entries_by_day.get(day, 0) >= config.max_entries_per_utc_day:
            cursor = index + 1
            continue
        entries_by_day[day] = entries_by_day.get(day, 0) + 1
        last_entry_time_ms = rows[fill_index].open_time_ms
        fill_count += 1
        target = price_for_net(entry, config.target_net, config.fee_rate)
        stop = price_for_net(entry, config.stop_net, config.fee_rate)
        exit_index = min(fill_index + config.max_hold_minutes, end_index - 1)
        exit_price = rows[exit_index].close * (1.0 - config.adverse_slippage) * (1.0 - config.spread_rate / 2.0)
        reason = "TIME"
        slippage = config.adverse_slippage
        for candidate_index in range(fill_index + 1, min(fill_index + config.max_hold_minutes + 1, end_index)):
            candle = rows[candidate_index]
            if candle.low <= stop:
                exit_index = candidate_index
                exit_price = min(stop, candle.open) * (1.0 - config.adverse_slippage) * (1.0 - config.spread_rate / 2.0)
                reason = "STOP"
                break
            if candle.high >= target:
                exit_index = candidate_index
                exit_price = target * (1.0 - config.spread_rate / 2.0)
                reason = "TP"
                slippage = 0.0
                break
        trades.append(
            Trade(
                signal.time_ms,
                rows[fill_index].open_time_ms,
                rows[exit_index].open_time_ms,
                entry,
                exit_price,
                reason,
                exit_price / entry - 1.0,
                net_return(entry, exit_price, config.fee_rate),
                config.fee_rate,
                config.fee_rate,
                slippage,
            )
        )
        cursor = exit_index + 1
    return ReplayResult(
        "AUTO_X_GRID_CANDIDATE",
        config,
        signal_count,
        fill_count,
        trades,
        rows[start_index].open_time_ms,
        rows[end_index - 1].close_time_ms,
    )


def risk_metrics(result: ReplayResult) -> dict:
    equity = peak = 1.0
    max_drawdown = 0.0
    returns = []
    for trade in result.trades:
        returns.append(trade.net_return)
        equity *= 1.0 + trade.net_return
        peak = max(peak, equity)
        max_drawdown = max(max_drawdown, 1.0 - equity / peak)
    tail_count = max(1, math.ceil(len(returns) * 0.05)) if returns else 0
    cvar_5 = sum(sorted(returns)[:tail_count]) / tail_count if tail_count else 0.0
    return {"max_drawdown": max_drawdown, "cvar_5": cvar_5}


def compact_metrics(result: ReplayResult) -> dict:
    value = result.to_dict()["metrics"] | risk_metrics(result)
    return value


def _pf_value(metrics: dict) -> float:
    value = metrics.get("profit_factor")
    if value is not None:
        return float(value)
    return math.inf if metrics.get("fills", 0) > 0 and metrics.get("average_net", 0.0) > 0 else 0.0


def _pf_text(metrics: dict) -> str:
    raw = metrics.get("profit_factor")
    if raw is None:
        return "не определён"
    return f"{float(raw):.3f}"


def _eligible(fold_a: dict, fold_b: dict, minimum_trades: int) -> bool:
    return all(
        fold["fills"] >= minimum_trades
        and fold["average_net"] > 0
        and _pf_value(fold) > 1
        for fold in (fold_a, fold_b)
    )


def _rejection_reasons(fold_a: dict, fold_b: dict, minimum_trades: int) -> list[str]:
    reasons: list[str] = []
    for name, fold in (("validation_a", fold_a), ("validation_b", fold_b)):
        if fold["fills"] < minimum_trades:
            reasons.append(f"{name}:sample_below_{minimum_trades}")
        if fold["average_net"] <= 0:
            reasons.append(f"{name}:non_positive_avg_net")
        if _pf_value(fold) <= 1:
            reasons.append(f"{name}:profit_factor_not_above_1")
    return reasons


_WORKER_STATE: dict = {}


def _candidate_for_index(
    zero_index: int,
    rows: list[Candle],
    signals: list[Signal],
    context_cache: dict[int, tuple[float | None, float | None, float | None, float | None]],
    split_1: int,
    split_2: int,
    test_start: int,
    minimum_trades: int,
    dimensions: tuple[tuple[object, ...], ...],
    full_count: int,
    variants_total: int,
) -> dict:
    config = EconomyConfig() if zero_index == 0 else _sample_config(zero_index, dimensions)
    minimum_window = min(split_1 - 719, split_2 - split_1, test_start - split_2)
    if config.max_hold_minutes + 6 >= minimum_window:
        empty = {
            "signals": 0, "fills": 0, "wins": 0, "win_rate": 0.0, "average_net": 0.0,
            "profit_factor": None, "compound_net": 0.0, "trades_per_day": 0.0, "exits": {},
            "max_drawdown": 0.0, "cvar_5": 0.0, "ineligible_reason": "hold_exceeds_validation_window",
        }
        train = dict(empty); validation_a = dict(empty); validation_b = dict(empty)
    else:
        train = compact_metrics(replay_cached(rows, signals, config, 719, split_1, context_cache))
        validation_a = compact_metrics(replay_cached(rows, signals, config, split_1, split_2, context_cache))
        validation_b = compact_metrics(replay_cached(rows, signals, config, split_2, test_start, context_cache))
    eligible = _eligible(validation_a, validation_b, minimum_trades)
    rejection_reasons = _rejection_reasons(validation_a, validation_b, minimum_trades)
    score = (
        1 if eligible else 0,
        min(validation_a["average_net"], validation_b["average_net"]),
        min(min(_pf_value(validation_a), _pf_value(validation_b)), 3.0),
        min(validation_a["compound_net"], validation_b["compound_net"]),
        -max(validation_a["max_drawdown"], validation_b["max_drawdown"]),
        min(validation_a["fills"], validation_b["fills"]),
        train["average_net"],
    )
    return {
        "candidate_id": f"C{zero_index + 1:08d}",
        "config": asdict(config),
        "train": train,
        "validation_a": validation_a,
        "validation_b": validation_b,
        "eligible": eligible,
        "rejection_reasons": rejection_reasons,
        "score": list(score),
    }


def _init_optimizer_worker(
    rows: list[Candle],
    signals: list[Signal],
    context_cache: dict[int, tuple[float | None, float | None, float | None, float | None]],
    split_1: int,
    split_2: int,
    test_start: int,
    minimum_trades: int,
    dimensions: tuple[tuple[object, ...], ...],
    full_count: int,
    variants_total: int,
) -> None:
    global _WORKER_STATE
    _WORKER_STATE = {
        "rows": rows,
        "signals": signals,
        "context_cache": context_cache,
        "split_1": split_1,
        "split_2": split_2,
        "test_start": test_start,
        "minimum_trades": minimum_trades,
        "dimensions": dimensions,
        "full_count": full_count,
        "variants_total": variants_total,
    }


def _evaluate_chunk(bounds: tuple[int, int]) -> dict:
    start, end = bounds
    state = _WORKER_STATE
    heap: list[tuple[tuple, int, dict]] = []
    for zero_index in range(start, end):
        candidate = _candidate_for_index(zero_index, **state)
        score = tuple(candidate["score"])
        if len(heap) < 200:
            heapq.heappush(heap, (score, zero_index, candidate))
        elif score > heap[0][0]:
            heapq.heapreplace(heap, (score, zero_index, candidate))
    return {"start": start, "end": end, "leaders": [item[2] for item in heap]}


def _merge_leaders(heap: list[tuple[tuple, int, dict]], candidates: list[dict]) -> None:
    for candidate in candidates:
        number = int(candidate["candidate_id"][1:])
        score = tuple(candidate["score"])
        if len(heap) < 200:
            heapq.heappush(heap, (score, number, candidate))
        elif score > heap[0][0]:
            heapq.heapreplace(heap, (score, number, candidate))


def _write_checkpoint(path: Path, payload: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    partial = path.with_suffix(path.suffix + ".partial")
    partial.write_text(json.dumps(payload, ensure_ascii=False, indent=2, allow_nan=False), encoding="utf-8")
    partial.replace(path)


def _load_checkpoint(path: Path, mode: str, variants_total: int) -> dict | None:
    if not path.exists():
        return None
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
        if value.get("engine") != "causal-grid-v6-evidence" or value.get("mode") != mode:
            return None
        if int(value.get("variants_total", -1)) != variants_total or value.get("status") == "COMPLETED":
            return None
        return value
    except Exception:
        return None


def run_stress_suite(
    rows: list[Candle], signals: list[Signal], config: EconomyConfig, start_index: int, end_index: int,
    context_cache: dict[int, tuple[float | None, float | None, float | None, float | None]] | None = None,
) -> dict:
    cases = {
        "BASE": config,
        "FEE_25": replace(config, fee_rate=config.fee_rate * 1.25),
        "FEE_50": replace(config, fee_rate=config.fee_rate * 1.50),
        "SPREAD_10BP": replace(config, spread_rate=0.0010),
        "SLIPPAGE_X2": replace(config, adverse_slippage=config.adverse_slippage * 2.0),
        "LATENCY_1M": replace(config, entry_latency_minutes=1),
        "LATENCY_2M": replace(config, entry_latency_minutes=2),
        "COMBINED": replace(
            config,
            fee_rate=config.fee_rate * 1.50,
            spread_rate=0.0015,
            adverse_slippage=config.adverse_slippage * 2.0,
            entry_latency_minutes=2,
        ),
    }
    results = {}
    for name, stress_config in cases.items():
        metrics = compact_metrics(replay_cached(rows, signals, stress_config, start_index, end_index, context_cache))
        metrics["economically_positive"] = (
            metrics["fills"] > 0
            and metrics["average_net"] > 0
            and _pf_value(metrics) > 1
            and metrics["compound_net"] > 0
        )
        results[name] = metrics
    positive_cases = sum(value["economically_positive"] for value in results.values())
    passed = positive_cases >= 6 and results["COMBINED"]["economically_positive"]
    return {"passed": passed, "positive_cases": positive_cases, "total_cases": len(results), "cases": results}


def optimize_variants(
    rows: list[Candle], mode: str = "STANDARD", progress: ProgressCallback | None = None,
    btc_rows: list[Candle] | None = None, sol_rows: list[Candle] | None = None,
    cancel_check: Callable[[], bool] | None = None,
    pause_check: Callable[[], bool] | None = None,
    workers: int = 1,
    checkpoint_path: Path | None = None,
) -> dict:
    require_replay_quality(rows)
    rows = sorted(rows, key=lambda item: item.open_time_ms)
    if len(rows) < 4_000:
        raise ValueError("Для автоматического поиска требуется минимум 4000 непрерывных минутных свечей")
    requested_mode = mode.upper()
    history_days = len(rows) / 1440
    mode = effective_mode(requested_mode, history_days)
    context_ready = bool(btc_rows and sol_rows)
    variants_total = min(MODE_COUNTS[mode], full_space_count(context_ready))
    signals = build_signal_cache(rows)
    context_cache = build_context_cache(rows, btc_rows, sol_rows)
    n = len(rows)
    split_1 = max(720, int(n * 0.50))
    split_2 = max(split_1 + 1, int(n * 0.625))
    test_start = max(split_2 + 1, int(n * 0.75))
    validation_fold_days = (split_2 - split_1) / 1440
    # Multiple-testing guard: the more variants inspected, the larger the evidence
    # each temporal fold must contain before a candidate can be admitted.
    statistical_target = max(15, math.ceil(validation_fold_days), math.ceil(math.log2(variants_total) * 2))
    validation_a_calendar_days = max(1, math.ceil((rows[split_2 - 1].close_time_ms - rows[split_1].open_time_ms) / 86_400_000) + 1)
    validation_b_calendar_days = max(1, math.ceil((rows[test_start - 1].close_time_ms - rows[split_2].open_time_ms) / 86_400_000) + 1)
    maximum_entries_in_fold = min(validation_a_calendar_days, validation_b_calendar_days) * max(_variant_dimensions(context_ready)[6])
    minimum_trades = min(statistical_target, maximum_entries_in_fold)
    minimum_test_trades = max(25, math.ceil((n - test_start) / 1440), math.ceil(math.log2(variants_total) * 3))
    confirmatory_history = history_days >= MIN_CONFIRMATORY_HISTORY_DAYS
    dimensions = _variant_dimensions(context_ready)
    full_count = math.prod(len(values) for values in dimensions)
    workers = max(1, min(int(workers), max(1, os.cpu_count() or 1)))
    # Small datasets stay local: spawning Windows processes would cost more than the replay.
    parallel = workers > 1 and len(rows) >= 20_000 and variants_total >= 50_000
    chunk_size = 500 if parallel else 100
    ranked_heap: list[tuple[tuple, int, dict]] = []
    start_index = 0
    checkpoint = _load_checkpoint(checkpoint_path, mode, variants_total) if checkpoint_path else None
    if checkpoint:
        start_index = max(0, min(variants_total, int(checkpoint.get("next_index", 0))))
        _merge_leaders(ranked_heap, list(checkpoint.get("leaders", [])))
    started = time.monotonic()
    last_checkpoint = 0.0

    def save_state(next_index: int, status: str = "RUNNING") -> None:
        nonlocal last_checkpoint
        if checkpoint_path is None:
            return
        leaders = [item[2] for item in sorted(ranked_heap, key=lambda item: (item[0], item[1]), reverse=True)]
        _write_checkpoint(
            checkpoint_path,
            {
                "engine": "causal-grid-v6-evidence",
                "status": status,
                "mode": mode,
                "variants_total": variants_total,
                "next_index": next_index,
                "tested": next_index,
                "workers": workers,
                "leaders": leaders,
                "updated_at_utc": datetime.now(timezone.utc).isoformat(),
            },
        )
        last_checkpoint = time.monotonic()

    def update(next_index: int) -> None:
        nonlocal last_checkpoint
        if checkpoint_path and (time.monotonic() - last_checkpoint >= 5 or next_index >= variants_total):
            save_state(next_index)
        if progress and (next_index == variants_total or next_index % max(chunk_size, variants_total // 1000) < chunk_size):
            elapsed = max(time.monotonic() - started, 1e-6)
            completed_now = max(0, next_index - start_index)
            rate = completed_now / elapsed
            remaining = (variants_total - next_index) / rate if rate else None
            best = max(ranked_heap, default=((), 0, {"candidate_id": "—"}), key=lambda item: item[0])[2]
            best_a = best.get("validation_a", {})
            best_b = best.get("validation_b", {})
            progress(
                {
                    "tested": next_index,
                    "total": variants_total,
                    "percent": int(next_index / variants_total * 100),
                    "stage": "Многопроцессорный причинный перебор" if parallel else "Причинный перебор",
                    "best": best["candidate_id"],
                    "best_provisional": {
                        "eligible": bool(best.get("eligible", False)),
                        "worst_average_net": min(best_a.get("average_net", 0.0), best_b.get("average_net", 0.0)),
                        "worst_profit_factor": min(_pf_value(best_a), _pf_value(best_b)) if best_a and best_b else 0.0,
                        "validation_fills": best_a.get("fills", 0) + best_b.get("fills", 0),
                    },
                    "workers": workers if parallel else 1,
                    "variants_per_second": rate,
                    "eta_seconds": remaining,
                    "checkpoint": str(checkpoint_path) if checkpoint_path else "",
                    "resumed_from": start_index,
                }
            )

    def control(next_index: int) -> None:
        if cancel_check and cancel_check():
            save_state(next_index, "STOPPED_SAFE")
            raise ResearchCancelled("Исследование безопасно остановлено; checkpoint сохранён")
        while pause_check and pause_check():
            save_state(next_index, "PAUSED")
            if cancel_check and cancel_check():
                save_state(next_index, "STOPPED_SAFE")
                raise ResearchCancelled("Исследование безопасно остановлено; checkpoint сохранён")
            time.sleep(0.2)

    ranges = [(index, min(index + chunk_size, variants_total)) for index in range(start_index, variants_total, chunk_size)]
    state = (rows, signals, context_cache, split_1, split_2, test_start, minimum_trades, dimensions, full_count, variants_total)
    if parallel:
        with ProcessPoolExecutor(max_workers=workers, initializer=_init_optimizer_worker, initargs=state) as executor:
            for chunk in executor.map(_evaluate_chunk, ranges, chunksize=1):
                _merge_leaders(ranked_heap, chunk["leaders"])
                next_index = int(chunk["end"])
                update(next_index)
                control(next_index)
    else:
        _init_optimizer_worker(*state)
        for bounds in ranges:
            chunk = _evaluate_chunk(bounds)
            _merge_leaders(ranked_heap, chunk["leaders"])
            next_index = int(chunk["end"])
            update(next_index)
            control(next_index)
    save_state(variants_total, "COMPLETED")
    ranked = [item[2] for item in sorted(ranked_heap, key=lambda item: (item[0], item[1]), reverse=True)]
    selected = ranked[0]
    selected_config = EconomyConfig(**selected["config"])
    test_was_opened = bool(selected["eligible"] and confirmatory_history)
    if test_was_opened:
        test_result = replay_cached(rows, signals, selected_config, test_start, n, context_cache)
        test_metrics = compact_metrics(test_result)
        baseline_test = compact_metrics(replay_economy(rows, evaluation_start_ms=rows[test_start].open_time_ms))
        test_positive = (
            test_metrics["fills"] >= minimum_test_trades
            and test_metrics["average_net"] > 0
            and _pf_value(test_metrics) > 1
            and test_metrics["compound_net"] > 0
        )
        beats_baseline = (
            test_metrics["average_net"] >= baseline_test["average_net"]
            and _pf_value(test_metrics) >= _pf_value(baseline_test)
            and test_metrics["compound_net"] >= baseline_test["compound_net"]
            and test_metrics["max_drawdown"] <= max(baseline_test["max_drawdown"], 1e-12) * 1.10
        )
        stress = run_stress_suite(rows, signals, selected_config, test_start, n, context_cache)
    else:
        test_metrics = None
        baseline_test = None
        test_positive = beats_baseline = False
        stress = None
    if not confirmatory_history:
        verdict = "EXPLORATORY_ONLY"
        explanation = (
            f"История разработки содержит только {history_days:.1f} дня. Кандидаты показаны как диагностические, "
            f"но TEST закрыт до накопления минимум {MIN_CONFIRMATORY_HISTORY_DAYS} дней development-истории."
        )
    elif selected["eligible"] and test_positive and beats_baseline and stress and stress["passed"]:
        verdict = "CHALLENGER_CANDIDATE"
        explanation = "Кандидат прошёл validation, TEST и stress-suite; до Champion требуется отдельное открытие immutable holdout."
    elif selected["eligible"] and test_positive and beats_baseline:
        verdict = "REJECTED_ON_STRESS"
        explanation = "Кандидат прошёл TEST, но не выдержал обязательные стрессы комиссии/spread/slippage/latency."
    elif selected["eligible"]:
        verdict = "REJECTED_ON_TEST" if not test_positive else "BASELINE_X_WINS"
        explanation = (
            "Лучший validation-кандидат не подтвердился на невиданном TEST. Champion остаётся NO_TRADE."
            if not test_positive
            else "Кандидат положителен на TEST, но не превзошёл защищённый baseline X по экономике и риску."
        )
    else:
        verdict = "NO_TRADE_WINS"
        explanation = "Ни один вариант не набрал достаточную выборку и положительную экономику в обеих validation-секциях; TEST не открывался."
    payload = {
        "engine": "causal-grid-v6-evidence",
        "requested_mode": requested_mode,
        "mode": mode,
        "mode_was_reduced_for_data_length": requested_mode != mode,
        "history_days": history_days,
        "research_stage": "CONFIRMATORY" if confirmatory_history else "EXPLORATORY",
        "history_sufficient_for_test": confirmatory_history,
        "minimum_confirmatory_history_days": MIN_CONFIRMATORY_HISTORY_DAYS,
        "variants_tested": variants_total,
        "signals_cached": len(signals),
        "compute": {
            "requested_workers": workers,
            "active_workers": workers if parallel else 1,
            "multiprocessing": parallel,
            "checkpoint": str(checkpoint_path) if checkpoint_path else "",
            "resumed_from_variant": start_index,
        },
        "search_space": {
            "full_combinations": full_space_count(context_ready),
            "variants_tested": variants_total,
            "coverage_ratio": variants_total / full_count,
            "sampling": "deterministic_halton_low_discrepancy",
            "dimensions": {name: list(values) for name, values in zip(DIMENSION_NAMES, dimensions, strict=True)},
            "btc_sol_context_ready": context_ready, "btc_rows": len(btc_rows or []),
            "sol_rows": len(sol_rows or []), "retained_leaders": len(ranked),
        },
        "split": {
            "train": [rows[719].open_time_ms, rows[split_1 - 1].close_time_ms],
            "validation_a": [rows[split_1].open_time_ms, rows[split_2 - 1].close_time_ms],
            "validation_b": [rows[split_2].open_time_ms, rows[test_start - 1].close_time_ms],
            "test": [rows[test_start].open_time_ms, rows[-1].close_time_ms],
            "test_was_not_used_for_selection": True,
            "test_was_opened": test_was_opened,
            "minimum_trades_per_validation_fold": minimum_trades,
            "statistical_target_trades_per_validation_fold": statistical_target,
            "maximum_possible_entries_per_validation_fold": maximum_entries_in_fold,
            "eligibility_threshold_is_physically_reachable": minimum_trades <= maximum_entries_in_fold,
            "minimum_test_trades": minimum_test_trades,
        },
        "selected": selected,
        "test": test_metrics,
        "baseline_x_test": baseline_test,
        "beats_baseline_x_on_test": beats_baseline,
        "stress": stress,
        "no_trade": {"compound_net": 0.0, "max_drawdown": 0.0},
        "verdict": verdict,
        "explanation": explanation,
        "leaderboard": ranked[:20],
        "diagnostics": {
            "retained_candidates": len(ranked),
            "eligible_retained_candidates": sum(bool(item.get("eligible")) for item in ranked),
            "selected_rejection_reasons": selected.get("rejection_reasons", []),
            "short_history_blocks_test": not confirmatory_history,
        },
        "warnings": [
            "Массовый перебор повышает риск переобучения; TEST не использовался для выбора параметров.",
            "TEST не является immutable holdout и не даёт права на реальные ордера.",
            "Высокий win rate без положительных NET и Profit Factor не считается победой.",
            "BTC/SOL-фильтры применяются только при наличии обеих причинно выровненных баз; иначе контекстные варианты автоматически отсекаются.",
            f"Запрошенная вычислительная глубина не урезается. Для надёжных выводов режиму {requested_mode} желательно минимум {MODE_MIN_DAYS[requested_mode]} дней; короткая история всё равно не допускает слабую выборку к TEST.",
        ],
    }
    return payload


def write_optimizer_report(payload: dict, output_directory: Path, dataset_path: Path) -> tuple[Path, Path]:
    output_directory.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
    record = payload | {
        "created_at_utc": datetime.now(timezone.utc).isoformat(),
        "dataset": str(dataset_path),
        "dataset_sha256": hashlib.sha256(dataset_path.read_bytes()).hexdigest(),
        "simulation": True,
        "real_orders": False,
    }
    json_path = output_directory / f"VARIANT_FACTORY_{stamp}.json"
    md_path = output_directory / f"VARIANT_FACTORY_{stamp}.md"
    partial = json_path.with_suffix(".json.partial")
    partial.write_text(json.dumps(record, ensure_ascii=False, indent=2, allow_nan=False), encoding="utf-8")
    partial.replace(json_path)
    chosen = record["selected"]
    config = chosen["config"]
    test = record["test"]
    rows = []
    for index, candidate in enumerate(record["leaderboard"][:10], 1):
        va, vb = candidate["validation_a"], candidate["validation_b"]
        rows.append(
            f"| {index} | {candidate['candidate_id']} | {candidate.get('strategy_family', '—')} | {_pf_text(va)} | {_pf_text(vb)} | "
            f"{min(va['average_net'], vb['average_net']) * 100:+.3f}% | {va['fills'] + vb['fills']} |"
        )
    test_line = (
        f"- TEST: сделок {test['fills']}, WR {test['win_rate'] * 100:.2f}%, Avg NET {test['average_net'] * 100:+.4f}%, "
        f"PF {_pf_text(test)}, Compound NET {test['compound_net'] * 100:+.3f}%, Max DD {test['max_drawdown'] * 100:.3f}%"
        if test is not None else (
            "- TEST: **не открывался**, потому что короткая история допускает только разведочный анализ"
            if not record.get("history_sufficient_for_test", True)
            else "- TEST: **не открывался**, потому что validation никого не допустил"
        )
    )
    holdout = record.get("immutable_holdout")
    holdout_line = (
        f"- Immutable holdout: **{holdout['holdout_rows']:,} строк, {holdout['state']}**"
        if holdout else "- Immutable holdout: не создан в этом старом опыте"
    )
    stress = record.get("stress")
    stress_line = (
        f"- Stress-suite: **{'PASS' if stress['passed'] else 'FAIL'}**, положительных сценариев {stress['positive_cases']}/{stress['total_cases']}"
        if stress else "- Stress-suite: не запускался, потому что кандидат не дошёл до TEST"
    )
    space_text = (
        f"{record.get('search_space', {}).get('full_combinations', 0):,}"
        if record.get('search_space', {}).get('full_combinations')
        else "адаптивная грамматика без фиксированного числа"
    )
    fusion = record.get("fusion_execution", {})
    markdown = f"""# PUMP Research Lab — фабрика вариантов

## Решение

**{record['verdict']}** — {record['explanation']}

- Проверено вариантов: **{record['variants_tested']:,}**
- Пространство гипотез: **{space_text}**
- Причинный контекст BTC/SOL: **{'подключён' if record.get('search_space', {}).get('btc_sol_context_ready') else 'недоступен — контекстные гипотезы исключены'}**
- Запрошенный / фактический режим: **{record.get('requested_mode', record['mode'])} / {record['mode']}**
- Диагностический лидер TRAIN/VALIDATION: **{chosen['candidate_id']}**
- Допущен по размеру выборки: **{'да' if chosen['eligible'] else 'нет'}**
- TEST не использовался при выборе: **да**
{test_line}
{holdout_line}
{stress_line}
- NO_TRADE: NET 0%, Max DD 0%

## Параметры выбранного кандидата

```json
{json.dumps(config, ensure_ascii=False, indent=2)}
```

## Диагностические validation-варианты

| # | ID | Семейство | PF A | PF B | Худший Avg NET | Сделки |
|---:|---|---|---:|---:|---:|---:|
{chr(10).join(rows)}

## Исполнение Bitpanda Fusion

- Вход: **{config.get('entry_order_type', 'LIMIT')}**
- Защитный выход: **{config.get('stop_exit_order_type', 'STOP_MARKET')}**
- Нативный OCO подтверждён документацией API: **{'да' if fusion.get('native_oco_documented') else 'нет'}**
- Пара TP/STOP: **клиентская отмена второй заявки после исполнения первой**
- Ограничения размера и точности пары: **получать из `/v1/pairs` перед paper/live; в этой версии реальные ордера отключены**

## Ограничения

""" + "\n".join(f"- {warning}" for warning in record["warnings"])
    partial = md_path.with_suffix(".md.partial")
    partial.write_text(markdown, encoding="utf-8")
    partial.replace(md_path)
    return json_path, md_path
