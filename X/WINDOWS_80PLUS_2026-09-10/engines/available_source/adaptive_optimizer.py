from __future__ import annotations

import hashlib
import json
import math
import os
import random
import re
import shutil
import time
from concurrent.futures import ProcessPoolExecutor
from dataclasses import asdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Callable, Iterable

from .adaptive_features import (
    ATR_PERIODS, DRAWDOWN_WINDOWS, FLOW_WINDOWS, RETURN_WINDOWS, RSI_PERIODS, VOLUME_WINDOWS,
    VWAP_WINDOWS, AdaptiveFeatureStore, build_feature_store,
)
from .baseline_x import EconomyConfig, ReplayResult, Signal, Trade, net_return, price_for_net, replay_economy
from .candidate_archive import CandidateArchive, recovered_seven_trade_candidate
from .domain import Candle
from .fusion_execution import fusion_execution_contract, validate_fusion_compatible
from .optimizer import (
    ResearchCancelled,
    _pf_value,
    compact_metrics,
)
from .validation import require_replay_quality


ProgressCallback = Callable[[dict], None]
MIN_TRADES_PER_DAY = 0.5
MAX_TRADES_PER_DAY = 5.0
MIN_CONFIRMATORY_HISTORY_DAYS = 60
ACTIVE_MIN_TRADES_PER_DAY = 0.5
ACTIVE_MAX_TRADES_PER_DAY = 5.0
SWING_MIN_TRADES_PER_DAY = 0.05
SWING_MAX_TRADES_PER_DAY = 0.5
ACTIVE_MAX_HOLD_MINUTES = 2_880

# This is a hypothesis grammar, not a Cartesian-grid target. The optimizer can
# revisit and refine regions indefinitely; no "number of all variants" exists.
PARAMETER_SPACE: dict[str, tuple[object, ...]] = {
    "drawdown_gate": (-0.002, -0.004, -0.008, -0.012, -0.02, -0.03, -0.04, -0.06, -0.10, -0.15, -0.25, -0.40),
    "limit_discount": (0.0, 0.0005, 0.001, 0.002, 0.004, 0.008, 0.015, 0.025, 0.05),
    "limit_ttl_minutes": (1, 2, 3, 5, 10, 20, 30, 60),
    "target_net": (0.005, 0.01, 0.015, 0.02, 0.025, 0.03, 0.04, 0.05, 0.075, 0.10, 0.15, 0.25, 0.50, 0.75, 1.0),
    "stop_net": (-0.005, -0.008, -0.01, -0.012, -0.015, -0.02, -0.03, -0.04, -0.06, -0.10, -0.15, -0.20, -0.30, -0.50),
    "max_hold_minutes": (60, 120, 180, 360, 720, 1440, 2880, 4320, 7200, 10080, 14400, 28800, 43200),
    "max_entries_per_utc_day": (1, 2, 3, 4, 5),
    "min_hours_between_entries": (0, 3, 6, 12, 18, 24, 36, 48),
    "btc_context_rule": ("ANY", "UP_15", "UP_60", "DOWN_15", "DOWN_60"),
    "sol_context_rule": ("ANY", "UP_15", "UP_60", "DOWN_15", "DOWN_60"),
    "vwap_deviation_gate": (-0.001, -0.002, -0.004, -0.006, -0.01, -0.015, -0.025, -0.04, -0.075, -0.12, -0.20),
    "min_buy_share": (0.0, 0.35, 0.42, 0.48, 0.50, 0.52, 0.55, 0.60, 0.70, 0.80, 0.90),
    "min_buy_share_delta": (-0.25, -0.10, -0.05, -0.02, 0.0, 0.01, 0.02, 0.05, 0.10, 0.25),
    "min_green_candle_return": (-0.10, -0.04, -0.02, -0.01, -0.004, 0.0, 0.001, 0.002, 0.004, 0.008, 0.015, 0.03, 0.06, 0.10),
    "min_volume_ratio_20m": (0.0, 0.25, 0.50, 0.75, 1.0, 1.25, 1.5, 2.0, 3.0, 5.0, 10.0),
    "vwap_window_minutes": VWAP_WINDOWS,
    "drawdown_window_minutes": DRAWDOWN_WINDOWS,
    "volume_window_minutes": VOLUME_WINDOWS,
    "pump_return_window_minutes": RETURN_WINDOWS,
    "min_pump_return": (-1.0, -0.25, -0.15, -0.10, -0.06, -0.04, -0.02, -0.01, -0.004, 0.0, 0.004, 0.01, 0.02),
    "max_pump_return": (-0.02, -0.01, -0.004, 0.0, 0.004, 0.01, 0.02, 0.04, 0.06, 0.10, 0.15, 0.25, 0.50, 1.0),
    "rsi_period_minutes": RSI_PERIODS,
    "min_rsi": (0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0),
    "max_rsi": (30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0),
    "atr_period_minutes": ATR_PERIODS,
    "min_atr_ratio": (0.0, 0.001, 0.002, 0.004, 0.008, 0.015, 0.03, 0.06, 0.10),
    "max_atr_ratio": (0.01, 0.02, 0.04, 0.08, 0.15, 0.30, 0.50, 1.0),
    "btc_return_window_minutes": RETURN_WINDOWS,
    "min_btc_return": (-1.0, -0.15, -0.10, -0.06, -0.04, -0.02, -0.01, -0.004, 0.0, 0.004, 0.01, 0.02),
    "max_btc_return": (-0.02, -0.01, -0.004, 0.0, 0.004, 0.01, 0.02, 0.04, 0.06, 0.10, 0.15, 1.0),
    "sol_return_window_minutes": RETURN_WINDOWS,
    "min_sol_return": (-1.0, -0.25, -0.15, -0.10, -0.06, -0.04, -0.02, -0.01, -0.004, 0.0, 0.004, 0.01, 0.02),
    "max_sol_return": (-0.02, -0.01, -0.004, 0.0, 0.004, 0.01, 0.02, 0.04, 0.06, 0.10, 0.15, 0.25, 1.0),
    "utc_start_hour": tuple(range(24)),
    "utc_end_hour": tuple(range(1, 25)),
    "trailing_stop_fraction": (0.0, 0.005, 0.008, 0.01, 0.015, 0.02, 0.03, 0.04, 0.06, 0.10, 0.15, 0.25),
    "breakeven_trigger_net": (0.0, 0.005, 0.01, 0.015, 0.02, 0.03, 0.04, 0.06, 0.10, 0.15, 0.25),
    "entry_order_type": ("MARKET", "LIMIT", "STOP_MARKET", "STOP_LIMIT"),
    "stop_exit_order_type": ("STOP_MARKET", "STOP_LIMIT"),
    "entry_trigger_offset": (0.0005, 0.001, 0.002, 0.004, 0.008, 0.015, 0.03),
    "stop_limit_buffer": (0.001, 0.002, 0.003, 0.005, 0.008, 0.015),
    "client_cancel_latency_minutes": (0, 1, 2, 5),
    "exit_distance_mode": ("PERCENT", "ATR"),
    "target_atr_multiple": (0.5, 1.0, 1.5, 2.0, 3.0, 4.0, 6.0, 8.0, 12.0),
    "stop_atr_multiple": (0.5, 1.0, 1.5, 2.0, 3.0, 4.0, 6.0, 8.0),
    "structure_window_minutes": DRAWDOWN_WINDOWS,
    "min_rebound_from_low": (0.0, 0.002, 0.005, 0.01, 0.02, 0.04, 0.08, 0.15),
    "max_rebound_from_low": (0.01, 0.02, 0.04, 0.08, 0.15, 0.30, 0.60, 1.0),
    "buy_flow_window_minutes": FLOW_WINDOWS,
    "min_buy_flow_mean": (0.0, 0.40, 0.45, 0.48, 0.50, 0.52, 0.55, 0.60, 0.70),
    "min_buy_flow_slope": (-1.0, -0.10, -0.05, -0.02, 0.0, 0.01, 0.02, 0.05, 0.10),
    "min_relative_btc_return": (-1.0, -0.10, -0.06, -0.03, -0.01, 0.0, 0.01, 0.03, 0.06),
    "max_relative_btc_return": (-0.03, -0.01, 0.0, 0.01, 0.03, 0.06, 0.10, 0.25, 1.0),
    "min_relative_sol_return": (-1.0, -0.15, -0.10, -0.06, -0.03, -0.01, 0.0, 0.01, 0.03, 0.06),
    "max_relative_sol_return": (-0.03, -0.01, 0.0, 0.01, 0.03, 0.06, 0.10, 0.15, 0.30, 1.0),
}
PARAMETER_NAMES = tuple(PARAMETER_SPACE)


def build_adaptive_signal_cache(rows: list[Candle]) -> list[Signal]:
    """Causal feature cache without the protected AUTO-X entry filters.

    The old grid pre-filtered green candles, buy share >= 50%, and rising buy
    share before optimization. That silently prevented the optimizer from
    exploring the agreed wider thresholds. V7 caches every usable minute.
    """
    signals: list[Signal] = []
    for index in range(719, len(rows)):
        current = rows[index]
        previous = rows[index - 1]
        vwap_rows = rows[index - 59 : index + 1]
        quote = sum(row.quote_volume for row in vwap_rows)
        if quote <= 0 or current.open <= 0:
            continue
        vwap = sum(((row.high + row.low + row.close) / 3.0) * row.quote_volume for row in vwap_rows) / quote
        peak = max(row.high for row in rows[index - 719 : index + 1])
        volume_rows = rows[index - 19 : index]
        average_volume = sum(row.quote_volume for row in volume_rows) / len(volume_rows)
        signals.append(
            Signal(
                index=index,
                time_ms=current.open_time_ms,
                close=current.close,
                vwap=vwap,
                deviation=current.close / vwap - 1.0,
                drawdown_12h=current.close / peak - 1.0,
                buy_share=current.buy_share,
                buy_share_delta=current.buy_share - previous.buy_share,
                green_candle_return=current.close / current.open - 1.0,
                volume_ratio_20m=current.quote_volume / average_volume if average_volume > 0 else 0.0,
            )
        )
    return signals


def _candidate_id(config: EconomyConfig) -> str:
    raw = json.dumps(asdict(config), sort_keys=True, separators=(",", ":"))
    return "A" + hashlib.sha256(raw.encode("utf-8")).hexdigest()[:10].upper()


def _strategy_family(config: EconomyConfig) -> str:
    return "ACTIVE" if config.max_hold_minutes <= ACTIVE_MAX_HOLD_MINUTES else "SWING"


def _frequency_bounds(config: EconomyConfig) -> tuple[float, float]:
    return (
        (ACTIVE_MIN_TRADES_PER_DAY, ACTIVE_MAX_TRADES_PER_DAY)
        if _strategy_family(config) == "ACTIVE"
        else (SWING_MIN_TRADES_PER_DAY, SWING_MAX_TRADES_PER_DAY)
    )


def _frequency_ok(metrics: dict, config: EconomyConfig | None = None) -> bool:
    value = float(metrics.get("trades_per_day", 0.0))
    minimum, maximum = _frequency_bounds(config or EconomyConfig())
    return minimum <= value <= maximum


def _frequency_distance(metrics: dict, config: EconomyConfig) -> float:
    value = float(metrics.get("trades_per_day", 0.0))
    minimum, maximum = _frequency_bounds(config)
    if value < minimum:
        return (minimum - value) / minimum
    if value > maximum:
        return (value - maximum) / maximum
    return 0.0


def _hour_allowed(hour: int, start: int, end: int) -> bool:
    if start == 0 and end == 24:
        return True
    if start < end:
        return start <= hour < end
    return hour >= start or hour < end


def _legacy_context_allows(rule: str, return_15: float, return_60: float) -> bool:
    if rule == "ANY":
        return True
    if math.isnan(return_15) or math.isnan(return_60):
        return False
    return {
        "UP_15": return_15 >= 0.001, "UP_60": return_60 >= 0.002,
        "DOWN_15": return_15 <= -0.001, "DOWN_60": return_60 <= -0.002,
    }[rule]


def replay_adaptive(
    rows: list[Candle], features: AdaptiveFeatureStore, config: EconomyConfig,
    start_index: int, end_index: int,
) -> ReplayResult:
    start_index = max(start_index, 1)
    last_signal_exclusive = end_index - config.max_hold_minutes - config.limit_ttl_minutes - config.entry_latency_minutes - 2
    if last_signal_exclusive <= start_index:
        raise ValueError("Evaluation window is too short")
    pump_feature_arrays = (
        features.vwap_deviation[config.vwap_window_minutes],
        features.drawdown[config.drawdown_window_minutes],
        features.volume_ratio[config.volume_window_minutes],
        features.pump_return[config.pump_return_window_minutes],
        features.rsi[config.rsi_period_minutes],
        features.atr_ratio[config.atr_period_minutes],
    )
    vwap_values, drawdown_values, volume_values, pump_returns, rsi_values, atr_values = pump_feature_arrays
    btc_returns = features.btc_return[config.btc_return_window_minutes]
    sol_returns = features.sol_return[config.sol_return_window_minutes]
    btc15 = features.btc_return[15]; btc60 = features.btc_return[60]
    sol15 = features.sol_return[15]; sol60 = features.sol_return[60]
    rebound_values = features.rebound_from_low[config.structure_window_minutes]
    flow_mean_values = features.buy_flow_mean[config.buy_flow_window_minutes]
    flow_slope_values = features.buy_flow_slope[config.buy_flow_window_minutes]
    trades: list[Trade] = []
    signal_count = fill_count = 0
    entries_by_day: dict[str, int] = {}
    last_entry_time_ms: int | None = None
    cursor = start_index
    for index in range(start_index, last_signal_exclusive):
        if index < cursor:
            continue
        values = tuple(array_values[index] for array_values in pump_feature_arrays)
        if any(math.isnan(value) for value in values):
            continue
        current = rows[index]
        hour = datetime.fromtimestamp(current.open_time_ms / 1000, tz=timezone.utc).hour
        if not _hour_allowed(hour, config.utc_start_hour, config.utc_end_hour):
            continue
        deviation, drawdown, volume_ratio, pump_return, rsi, atr_ratio = values
        rebound = rebound_values[index]
        flow_mean = flow_mean_values[index]
        flow_slope = flow_slope_values[index]
        if any(math.isnan(value) for value in (rebound, flow_mean, flow_slope)):
            continue
        btc_return = btc_returns[index]; sol_return = sol_returns[index]
        if features.btc_available:
            if math.isnan(btc_return) or not config.min_btc_return <= btc_return <= config.max_btc_return:
                continue
        elif not (config.btc_context_rule == "ANY" and config.min_btc_return <= -1.0 and config.max_btc_return >= 1.0):
            continue
        if features.sol_available:
            if math.isnan(sol_return) or not config.min_sol_return <= sol_return <= config.max_sol_return:
                continue
        elif not (config.sol_context_rule == "ANY" and config.min_sol_return <= -1.0 and config.max_sol_return >= 1.0):
            continue
        relative_btc = pump_return - btc_return if features.btc_available else 0.0
        relative_sol = pump_return - sol_return if features.sol_available else 0.0
        buy_delta = current.buy_share - rows[index - 1].buy_share
        candle_return = current.close / current.open - 1.0 if current.open > 0 else -math.inf
        if not (
            drawdown <= config.drawdown_gate
            and deviation <= config.vwap_deviation_gate
            and current.buy_share >= config.min_buy_share
            and buy_delta >= config.min_buy_share_delta
            and candle_return >= config.min_green_candle_return
            and volume_ratio >= config.min_volume_ratio_20m
            and config.min_pump_return <= pump_return <= config.max_pump_return
            and config.min_rsi <= rsi <= config.max_rsi
            and config.min_atr_ratio <= atr_ratio <= config.max_atr_ratio
            and config.min_rebound_from_low <= rebound <= config.max_rebound_from_low
            and flow_mean >= config.min_buy_flow_mean
            and flow_slope >= config.min_buy_flow_slope
            and (not features.btc_available or config.min_relative_btc_return <= relative_btc <= config.max_relative_btc_return)
            and (not features.sol_available or config.min_relative_sol_return <= relative_sol <= config.max_relative_sol_return)
            and (not features.btc_available or _legacy_context_allows(config.btc_context_rule, btc15[index], btc60[index]))
            and (not features.sol_available or _legacy_context_allows(config.sol_context_rule, sol15[index], sol60[index]))
        ):
            continue
        signal_count += 1
        fill_index = None; entry = 0.0
        first_fill_index = index + 1 + config.entry_latency_minutes
        entry_type = config.entry_order_type
        if entry_type == "MARKET" and first_fill_index < end_index:
            fill_index = first_fill_index
            entry = rows[fill_index].open * (1.0 + config.adverse_slippage) * (1.0 + config.spread_rate / 2.0)
        elif entry_type == "LIMIT":
            limit = current.close * (1.0 - config.limit_discount)
            for candidate_index in range(first_fill_index, min(first_fill_index + config.limit_ttl_minutes, end_index)):
                candle = rows[candidate_index]
                if candle.low <= limit:
                    fill_index = candidate_index
                    entry = min(limit, candle.open) * (1.0 + config.spread_rate / 2.0)
                    break
        else:
            trigger = current.close * (1.0 + config.entry_trigger_offset)
            trigger_index = None
            for candidate_index in range(first_fill_index, min(first_fill_index + config.limit_ttl_minutes, end_index)):
                candle = rows[candidate_index]
                if trigger_index is None and candle.high >= trigger:
                    trigger_index = candidate_index
                    if entry_type == "STOP_MARKET":
                        fill_index = candidate_index
                        entry = max(trigger, candle.open) * (1.0 + config.adverse_slippage) * (1.0 + config.spread_rate / 2.0)
                        break
                    # The high/low order inside a 1m candle is unknown. A buy
                    # stop-limit may only fill from the next candle onward.
                    continue
                if trigger_index is not None and entry_type == "STOP_LIMIT":
                    stop_limit = trigger * (1.0 + config.stop_limit_buffer)
                    if candle.low <= stop_limit:
                        fill_index = candidate_index
                        entry = min(stop_limit, max(trigger, candle.open)) * (1.0 + config.spread_rate / 2.0)
                        break
        if fill_index is None:
            cursor = first_fill_index + config.limit_ttl_minutes
            continue
        fill_time = rows[fill_index].open_time_ms
        day = datetime.fromtimestamp(fill_time / 1000, tz=timezone.utc).strftime("%Y%m%d")
        if last_entry_time_ms is not None and fill_time - last_entry_time_ms < config.min_hours_between_entries * 3_600_000:
            cursor = index + 1
            continue
        if entries_by_day.get(day, 0) >= config.max_entries_per_utc_day:
            cursor = index + 1
            continue
        entries_by_day[day] = entries_by_day.get(day, 0) + 1
        last_entry_time_ms = fill_time; fill_count += 1
        if config.exit_distance_mode == "ATR":
            target_net = max(0.001, config.target_atr_multiple * atr_ratio)
            stop_net = -max(0.001, config.stop_atr_multiple * atr_ratio)
        else:
            target_net = config.target_net
            stop_net = config.stop_net
        target = price_for_net(entry, target_net, config.fee_rate)
        hard_stop = price_for_net(entry, stop_net, config.fee_rate)
        active_stop = hard_stop; highest = entry
        exit_index = min(fill_index + config.max_hold_minutes, end_index - 1)
        exit_price = rows[exit_index].close * (1.0 - config.adverse_slippage) * (1.0 - config.spread_rate / 2.0)
        reason = "TIME"; slippage = config.adverse_slippage
        stop_limit_triggered = False
        stop_limit_price = 0.0
        for candidate_index in range(fill_index + 1, min(fill_index + config.max_hold_minutes + 1, end_index)):
            candle = rows[candidate_index]
            highest = max(highest, candle.high)
            if config.breakeven_trigger_net > 0 and highest >= price_for_net(entry, config.breakeven_trigger_net, config.fee_rate):
                active_stop = max(active_stop, price_for_net(entry, 0.0, config.fee_rate))
            if config.trailing_stop_fraction > 0:
                active_stop = max(active_stop, highest * (1.0 - config.trailing_stop_fraction))
            # Conservative ordering: if both are reachable in one OHLC candle,
            # the adverse stop is applied first.
            if stop_limit_triggered and candle.high >= stop_limit_price:
                exit_index = candidate_index
                exit_price = max(stop_limit_price, candle.open) * (1.0 - config.spread_rate / 2.0)
                reason = "STOP_LIMIT_FILLED"
                slippage = 0.0
                break
            if candle.low <= active_stop:
                if config.stop_exit_order_type == "STOP_LIMIT":
                    stop_limit_triggered = True
                    stop_limit_price = active_stop * (1.0 - config.stop_limit_buffer)
                    # With 1m OHLC the order of high/low is unknown. Do not
                    # award a same-candle TP after the protective stop fired.
                    continue
                else:
                    exit_index = candidate_index
                    exit_price = min(active_stop, candle.open) * (1.0 - config.adverse_slippage) * (1.0 - config.spread_rate / 2.0)
                    reason = "TRAIL" if active_stop > hard_stop else "STOP_MARKET"
                    break
            if candle.high >= target:
                exit_index = candidate_index
                exit_price = target * (1.0 - config.spread_rate / 2.0)
                reason = "TP"; slippage = 0.0
                break
        trades.append(Trade(
            current.open_time_ms, fill_time, rows[exit_index].open_time_ms,
            entry, exit_price, reason, exit_price / entry - 1.0,
            net_return(entry, exit_price, config.fee_rate), config.fee_rate,
            config.fee_rate, slippage,
        ))
        cursor = exit_index + 1
    return ReplayResult(
        "ADAPTIVE_V8_CANDIDATE", config, signal_count, fill_count, trades,
        rows[start_index].open_time_ms, rows[end_index - 1].close_time_ms,
    )


def _evaluate_config(
    config: EconomyConfig,
    rows: list[Candle],
    features: AdaptiveFeatureStore,
    split_1: int,
    split_2: int,
    test_start: int,
) -> dict:
    fusion_reasons = validate_fusion_compatible(config)
    invalid_ranges = (
        config.min_pump_return > config.max_pump_return
        or config.min_rsi > config.max_rsi
        or config.min_atr_ratio > config.max_atr_ratio
        or config.min_btc_return > config.max_btc_return
        or config.min_sol_return > config.max_sol_return
        or config.min_rebound_from_low > config.max_rebound_from_low
        or config.min_relative_btc_return > config.max_relative_btc_return
        or config.min_relative_sol_return > config.max_relative_sol_return
    )
    minimum_window = min(split_1 - 719, split_2 - split_1, test_start - split_2)
    replay_results: list[ReplayResult] = []
    if invalid_ranges or fusion_reasons or config.max_hold_minutes + config.limit_ttl_minutes + 2 >= minimum_window:
        empty = {
            "signals": 0, "fills": 0, "wins": 0, "win_rate": 0.0, "average_net": 0.0,
            "profit_factor": None, "compound_net": 0.0, "trades_per_day": 0.0,
            "exits": {}, "max_drawdown": 0.0, "cvar_5": 0.0,
        }
        train = dict(empty); validation_a = dict(empty); validation_b = dict(empty)
        reasons = list(fusion_reasons)
        if invalid_ranges:
            reasons.append("invalid_min_max_range")
        if config.max_hold_minutes + config.limit_ttl_minutes + 2 >= minimum_window:
            reasons.append("hold_exceeds_validation_window")
    else:
        replay_results = [
            replay_adaptive(rows, features, config, 719, split_1),
            replay_adaptive(rows, features, config, split_1, split_2),
            replay_adaptive(rows, features, config, split_2, test_start),
        ]
        train, validation_a, validation_b = (compact_metrics(item) for item in replay_results)
        reasons = []
    family = _strategy_family(config)
    minimum_frequency, maximum_frequency = _frequency_bounds(config)
    for name, fold in (("train", train), ("validation_a", validation_a), ("validation_b", validation_b)):
        if not _frequency_ok(fold, config):
            reasons.append(f"{name}:trades_per_day_outside_{minimum_frequency:g}_to_{maximum_frequency:g}")
        if fold["average_net"] <= 0:
            reasons.append(f"{name}:non_positive_avg_net")
        if _pf_value(fold) <= 1:
            reasons.append(f"{name}:profit_factor_not_above_1")
        if fold["compound_net"] <= 0:
            reasons.append(f"{name}:non_positive_compound_net")
    eligible = not reasons
    worst_avg = min(validation_a["average_net"], validation_b["average_net"])
    worst_compound = min(validation_a["compound_net"], validation_b["compound_net"])
    worst_pf = min(_pf_value(validation_a), _pf_value(validation_b))
    max_dd = max(validation_a["max_drawdown"], validation_b["max_drawdown"])
    instability = abs(validation_a["average_net"] - validation_b["average_net"])
    train_instability = abs(train["average_net"] - (validation_a["average_net"] + validation_b["average_net"]) / 2.0)
    objective = worst_compound + 4.0 * worst_avg - 1.5 * max_dd - 2.0 * instability - train_instability
    constraint_distance = sum(_frequency_distance(fold, config) for fold in (train, validation_a, validation_b))
    for fold in (train, validation_a, validation_b):
        constraint_distance += max(0.0, -float(fold["average_net"])) * 20.0
        constraint_distance += max(0.0, 1.0 - min(_pf_value(fold), 1.0))
        constraint_distance += max(0.0, -float(fold["compound_net"])) * 5.0
    score = (
        1 if eligible else 0, -constraint_distance, objective,
        min(worst_pf, 5.0), -max_dd, validation_a["fills"] + validation_b["fills"],
    )
    behavior_raw = "|".join(
        f"{trade.fill_time_ms}:{trade.exit_time_ms}:{trade.reason}"
        for result in replay_results for trade in result.trades
    ) or "NO_TRADES"
    behavior_signature = hashlib.sha256(behavior_raw.encode("utf-8")).hexdigest()[:16]
    return {
        "candidate_id": _candidate_id(config), "config": asdict(config), "train": train,
        "validation_a": validation_a, "validation_b": validation_b,
        "eligible": eligible, "rejection_reasons": reasons,
        "objective": objective, "constraint_distance": constraint_distance,
        "strategy_family": family, "behavior_signature": behavior_signature,
        "score": list(score),
    }


_WORKER_STATE: dict = {}


def _init_worker(rows, features, split_1, split_2, test_start) -> None:
    global _WORKER_STATE
    _WORKER_STATE = {
        "rows": rows, "features": features,
        "split_1": split_1, "split_2": split_2, "test_start": test_start,
    }


def _evaluate_batch(config_values: list[dict]) -> list[dict]:
    return [_evaluate_config(EconomyConfig(**value), **_WORKER_STATE) for value in config_values]


def _random_config(rng: random.Random) -> EconomyConfig:
    values = {name: rng.choice(options) for name, options in PARAMETER_SPACE.items()}
    # Each hypothesis activates only a bounded subset of optional filters.
    # Randomly constraining every dimension at once mostly creates zero-trade
    # garbage and teaches the evolutionary search nothing useful.
    neutral_blocks = {
        "pump_momentum": {"min_pump_return": -1.0, "max_pump_return": 1.0},
        "rsi": {"min_rsi": 0.0, "max_rsi": 100.0},
        "atr_regime": {"min_atr_ratio": 0.0, "max_atr_ratio": 1.0},
        "btc": {
            "btc_context_rule": "ANY", "min_btc_return": -1.0, "max_btc_return": 1.0,
            "min_relative_btc_return": -1.0, "max_relative_btc_return": 1.0,
        },
        "sol": {
            "sol_context_rule": "ANY", "min_sol_return": -1.0, "max_sol_return": 1.0,
            "min_relative_sol_return": -1.0, "max_relative_sol_return": 1.0,
        },
        "structure": {"min_rebound_from_low": 0.0, "max_rebound_from_low": 1.0},
        "persistent_flow": {"min_buy_flow_mean": 0.0, "min_buy_flow_slope": -1.0},
        "minute_flow": {
            "min_buy_share": 0.0, "min_buy_share_delta": -0.25,
            "min_green_candle_return": -0.10, "min_volume_ratio_20m": 0.0,
        },
        "session": {"utc_start_hour": 0, "utc_end_hour": 24},
    }
    active_count = rng.randint(2, min(6, len(neutral_blocks)))
    active = set(rng.sample(tuple(neutral_blocks), active_count))
    for name, neutral in neutral_blocks.items():
        if name not in active:
            values.update(neutral)
    return _normalize_config(EconomyConfig(**values))


def _normalize_config(config: EconomyConfig) -> EconomyConfig:
    values = asdict(config)
    for minimum, maximum in (
        ("min_pump_return", "max_pump_return"), ("min_rsi", "max_rsi"),
        ("min_atr_ratio", "max_atr_ratio"), ("min_btc_return", "max_btc_return"),
        ("min_sol_return", "max_sol_return"),
        ("min_rebound_from_low", "max_rebound_from_low"),
        ("min_relative_btc_return", "max_relative_btc_return"),
        ("min_relative_sol_return", "max_relative_sol_return"),
    ):
        low, high = sorted((values[minimum], values[maximum]))
        values[minimum], values[maximum] = low, high
    return EconomyConfig(**values)


def _disable_missing_context(config: EconomyConfig, btc_ready: bool, sol_ready: bool) -> EconomyConfig:
    values = asdict(config)
    if not btc_ready:
        values.update(btc_context_rule="ANY", min_btc_return=-1.0, max_btc_return=1.0)
    if not sol_ready:
        values.update(sol_context_rule="ANY", min_sol_return=-1.0, max_sol_return=1.0)
    return EconomyConfig(**values)


def _mutate(parent: dict, rng: random.Random, intensity: int = 2) -> EconomyConfig:
    values = dict(parent)
    for name in rng.sample(PARAMETER_NAMES, k=min(intensity, len(PARAMETER_NAMES))):
        options = PARAMETER_SPACE[name]
        current = values[name]
        try:
            index = options.index(current)
        except ValueError:
            index = rng.randrange(len(options))
        radius = max(1, int(math.ceil(len(options) * (0.08 if intensity <= 2 else 0.20))))
        values[name] = options[max(0, min(len(options) - 1, index + rng.randint(-radius, radius)))]
    return _normalize_config(EconomyConfig(**values))


def _crossover(first: dict, second: dict, rng: random.Random) -> EconomyConfig:
    return _normalize_config(EconomyConfig(**{name: (first[name] if rng.random() < 0.5 else second[name]) for name in PARAMETER_NAMES}))


def _unique(configs: Iterable[EconomyConfig]) -> list[EconomyConfig]:
    result: list[EconomyConfig] = []
    seen: set[str] = set()
    for config in configs:
        key = _candidate_id(config)
        if key not in seen:
            seen.add(key); result.append(config)
    return result


def _dedupe_behavior(candidates: Iterable[dict], limit: int = 100) -> list[dict]:
    """Keep one strongest parameterisation for each actual trade sequence."""
    best: dict[tuple[str, str], dict] = {}
    for candidate in candidates:
        key = (
            candidate.get("strategy_family", "ACTIVE"),
            candidate.get("behavior_signature", candidate["candidate_id"]),
        )
        current = best.get(key)
        if current is None or tuple(candidate["score"]) > tuple(current["score"]):
            best[key] = candidate
    return sorted(best.values(), key=lambda item: tuple(item["score"]), reverse=True)[:limit]


def _next_population(generation: int, size: int, leaders: list[dict], seed: int) -> tuple[list[EconomyConfig], dict[str, int]]:
    rng = random.Random(seed + generation * 1_000_003)
    population: list[EconomyConfig] = []
    arms = {"exploration": 0, "mutation": 0, "crossover": 0, "neighbourhood": 0}
    elite = leaders[: min(40, len(leaders))]
    # Exploration never disappears, preventing premature collapse.
    exploration = size if not elite else max(size // 5, 1)
    for _ in range(exploration):
        population.append(_random_config(rng)); arms["exploration"] += 1
    while len(population) < size and elite:
        roll = rng.random()
        if roll < 0.45:
            population.append(_mutate(rng.choice(elite)["config"], rng, 3)); arms["mutation"] += 1
        elif roll < 0.75 and len(elite) > 1:
            a, b = rng.sample(elite, 2)
            population.append(_crossover(a["config"], b["config"], rng)); arms["crossover"] += 1
        else:
            population.append(_mutate(rng.choice(elite[:10])["config"], rng, 1)); arms["neighbourhood"] += 1
    population = _unique(population)
    known = {_candidate_id(item) for item in population}
    while len(population) < size:
        candidate = _random_config(rng)
        key = _candidate_id(candidate)
        if key not in known:
            known.add(key); population.append(candidate); arms["exploration"] += 1
    return population, arms


def _equity_preview(result, initial_capital: float = 1000.0) -> dict:
    value = initial_capital
    points = [{"time_ms": result.start_ms, "capital": value, "event": "START"}]
    for trade in result.trades:
        value *= 1.0 + trade.net_return
        points.append({"time_ms": trade.exit_time_ms, "capital": value, "event": trade.reason})
    metrics = compact_metrics(result)
    return {"initial": initial_capital, "final": value, "timeline": points, "trades": [item.to_dict() for item in result.trades], "metrics": metrics}


def _atomic_json(path: Path, payload: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    partial = path.with_suffix(path.suffix + ".partial")
    partial.write_text(json.dumps(payload, ensure_ascii=False, indent=2, allow_nan=False), encoding="utf-8")
    partial.replace(path)


def _write_live_report(path: Path, state: dict) -> None:
    best = state.get("best") or {}
    config = best.get("config", {})
    preview = state.get("equity_preview", {})
    metrics = preview.get("metrics", {})
    archive = state.get("interesting_candidate_archive", {})
    text = f"""# PUMP Research Lab V8 — живой незавершённый отчёт

Статус: **{state.get('status', 'RUNNING')}**. Это промежуточный результат TRAIN/VALIDATION, не финальное доказательство.

- Поколение: **{state.get('generation', 0):,}**
- Проверено уникальных вариантов: **{state.get('evaluated', 0):,}**
- Стабильность лидера: **{state.get('stagnant_generations', 0)}** поколений без существенного улучшения
- Семейства: **ACTIVE 0,5–5/сутки; SWING 0,05–0,5/сутки**
- Текущий лидер: **{best.get('candidate_id', '—')}**
- Капитал на видимой development-части: **{preview.get('initial', 1000):.2f} → {preview.get('final', 1000):.2f}**
- Сделки: **{metrics.get('fills', 0)}**, Avg NET **{metrics.get('average_net', 0) * 100:+.4f}%**, Compound NET **{metrics.get('compound_net', 0) * 100:+.3f}%**, Max DD **{metrics.get('max_drawdown', 0) * 100:.3f}%**
- TEST использован при выборе: **нет**
- Навсегда сохранено интересных кандидатов: **{archive.get('candidate_count', 0):,}**
- Постоянный архив: `{archive.get('database', '—')}`

## Переменные текущего лидера

```json
{json.dumps(config, ensure_ascii=False, indent=2)}
```

## Важно

- График и цифры обновляются во время работы и могут измениться.
- Кандидаты из постоянного архива не исчезают при смене лидера.
- Без завершённого TEST, stress-suite и закрытого holdout кандидат не получает право называться Champion.
- Реальные ордера отключены.
"""
    partial = path.with_suffix(path.suffix + ".partial")
    partial.write_text(text, encoding="utf-8")
    partial.replace(path)


def optimize_adaptive(
    rows: list[Candle],
    progress: ProgressCallback | None = None,
    btc_rows: list[Candle] | None = None,
    sol_rows: list[Candle] | None = None,
    cancel_check: Callable[[], bool] | None = None,
    pause_check: Callable[[], bool] | None = None,
    workers: int = 1,
    checkpoint_path: Path | None = None,
    live_report_directory: Path | None = None,
    population_size: int | None = None,
    convergence_generations: int = 25,
    max_generations: int | None = None,
    seed: int = 27011987,
) -> dict:
    """Adaptive, resumable V8 search with feasibility-first selection.

    max_generations exists only for deterministic automated tests. The GUI does
    not set it. Production completion is governed by convergence or Safe Stop.
    """
    require_replay_quality(rows)
    rows = sorted(rows, key=lambda item: item.open_time_ms)
    if len(rows) < 4_000:
        raise ValueError("Для адаптивного поиска требуется минимум 4000 непрерывных минутных свечей")
    features = build_feature_store(rows, btc_rows, sol_rows)
    n = len(rows)
    # Four sequential quarters make a 30-day holding horizon physically
    # testable on the agreed six-month base after the 20% immutable holdout.
    split_1 = max(720, int(n * 0.25)); split_2 = max(split_1 + 1, int(n * 0.50)); test_start = max(split_2 + 1, int(n * 0.75))
    workers = max(1, min(int(workers), max(1, os.cpu_count() or 1)))
    population_size = population_size or max(400, workers * 250)
    parallel = workers > 1 and len(rows) >= 20_000
    generation = evaluated = stagnant = 0
    best_objective = -math.inf
    best_was_eligible = False
    leaders: list[dict] = []
    resumed_completed_checkpoint = False
    started = time.monotonic()
    if checkpoint_path and checkpoint_path.exists():
        try:
            saved = json.loads(checkpoint_path.read_text(encoding="utf-8"))
            if saved.get("engine") == "adaptive-evolution-v8":
                generation = int(saved.get("generation", 0)); evaluated = int(saved.get("evaluated", 0))
                stagnant = int(saved.get("stagnant_generations", 0))
                saved_objective = saved.get("best_objective")
                best_objective = float(saved_objective) if saved_objective is not None else -math.inf
                best_was_eligible = bool(saved.get("best_was_eligible", False))
                leaders = list(saved.get("leaders", []))
                if saved.get("status") == "COMPLETED":
                    # A newer build may add evidence capture without changing
                    # the hypothesis grammar. Preserve all completed work and
                    # open a fresh convergence window instead of restarting.
                    resumed_completed_checkpoint = True
                    stagnant = 0
        except (OSError, ValueError, TypeError, json.JSONDecodeError):
            pass

    archive: CandidateArchive | None = None
    if checkpoint_path:
        stable_stem = re.sub(r"-adaptive-v\d+(?:\.\d+)?$", "", checkpoint_path.stem)
        archive_path = checkpoint_path.with_name(f"{stable_stem}-interesting.sqlite3")
        legacy_archive = checkpoint_path.with_name(f"{stable_stem}-adaptive-v7-interesting.sqlite3")
        if not archive_path.exists() and legacy_archive.exists():
            shutil.copy2(legacy_archive, archive_path)
        archive = CandidateArchive(archive_path, live_report_directory)
        if "automatic-6m-e6afcb8f64205d76-adaptive-v7" in checkpoint_path.stem:
            recovered, recovered_preview = recovered_seven_trade_candidate()
            archive.store(
                recovered, 7, {"RECOVERED_FROM_OLD_LIVE_REPORT", "DISPLAYED_LEADER"},
                recovered_preview, source="recovered-v7-live-report",
            )

    def archive_candidates(items: Iterable[dict], forced_ids: set[str] | None = None, source: str = "adaptive-v8") -> None:
        if not archive:
            return
        selected_for_archive = archive.select_interesting(items, forced_ids)
        for candidate, tags in selected_for_archive:
            preview_for_archive = None
            if not archive.has_preview(candidate["candidate_id"]):
                try:
                    config = EconomyConfig(**candidate["config"])
                    preview_for_archive = _equity_preview(replay_adaptive(rows, features, config, 719, test_start))
                except (ValueError, KeyError, TypeError):
                    tags.add("PREVIEW_UNAVAILABLE")
            archive.store(candidate, generation, tags, preview_for_archive, source=source)
        if selected_for_archive:
            archive.export()

    if leaders:
        archive_candidates(leaders, {leaders[0]["candidate_id"]}, source="resumed-checkpoint")
    last_displayed_candidate_id = leaders[0]["candidate_id"] if leaders else None

    def save(status: str, preview: dict | None = None, arms: dict | None = None) -> dict:
        state = {
            "engine": "adaptive-evolution-v8", "status": status, "generation": generation,
            "evaluated": evaluated, "stagnant_generations": stagnant,
            "best_objective": best_objective if math.isfinite(best_objective) else None,
            "best_was_eligible": best_was_eligible,
            "leaders": leaders[:100], "best": leaders[0] if leaders else None,
            "equity_preview": preview or {}, "generator_allocation": arms or {},
            "frequency_gate": {
                "ACTIVE": {"minimum_trades_per_day": ACTIVE_MIN_TRADES_PER_DAY, "maximum_trades_per_day": ACTIVE_MAX_TRADES_PER_DAY},
                "SWING": {"minimum_trades_per_day": SWING_MIN_TRADES_PER_DAY, "maximum_trades_per_day": SWING_MAX_TRADES_PER_DAY},
            },
            "parameter_space": {key: list(value) for key, value in PARAMETER_SPACE.items()},
            "updated_at_utc": datetime.now(timezone.utc).isoformat(),
            "test_was_not_used_for_selection": True,
            "resumed_completed_checkpoint": resumed_completed_checkpoint,
        }
        if archive:
            state["interesting_candidate_archive"] = archive.summary()
        if checkpoint_path:
            _atomic_json(checkpoint_path, state)
        if live_report_directory:
            live_report_directory.mkdir(parents=True, exist_ok=True)
            _atomic_json(live_report_directory / "LIVE_PUMP_RESEARCH.json", state)
            _write_live_report(live_report_directory / "LIVE_PUMP_RESEARCH.md", state)
        return state

    initargs = (rows, features, split_1, split_2, test_start)
    executor = ProcessPoolExecutor(max_workers=workers, initializer=_init_worker, initargs=initargs) if parallel else None
    if not parallel:
        _init_worker(*initargs)
    try:
        while True:
            if cancel_check and cancel_check():
                save("STOPPED_SAFE")
                raise ResearchCancelled("Адаптивное исследование безопасно остановлено; checkpoint и живой отчёт сохранены")
            while pause_check and pause_check():
                save("PAUSED")
                if cancel_check and cancel_check():
                    save("STOPPED_SAFE")
                    raise ResearchCancelled("Адаптивное исследование безопасно остановлено; checkpoint и живой отчёт сохранены")
                time.sleep(0.2)
            generation += 1
            population, arms = _next_population(generation, population_size, leaders, seed)
            population = [
                _disable_missing_context(item, bool(btc_rows), bool(sol_rows)) for item in population
            ]
            population = _unique(population)
            chunks = [[asdict(item) for item in population[index:index + 25]] for index in range(0, len(population), 25)]
            evaluated_generation: list[dict] = []
            results = executor.map(_evaluate_batch, chunks, chunksize=1) if executor else map(_evaluate_batch, chunks)
            for batch_index, batch in enumerate(results, 1):
                evaluated_generation.extend(batch)
                evaluated += len(batch)
                if cancel_check and cancel_check():
                    by_id = {item["candidate_id"]: item for item in leaders + evaluated_generation}
                    leaders = _dedupe_behavior(by_id.values())
                    archive_candidates(evaluated_generation, {leaders[0]["candidate_id"]} if leaders else set(), source="safe-stop")
                    save("STOPPED_SAFE", arms=arms)
                    raise ResearchCancelled("Адаптивное исследование безопасно остановлено; checkpoint и живой отчёт сохранены")
                while pause_check and pause_check():
                    by_id = {item["candidate_id"]: item for item in leaders + evaluated_generation}
                    leaders = _dedupe_behavior(by_id.values())
                    archive_candidates(evaluated_generation, {leaders[0]["candidate_id"]} if leaders else set(), source="paused-partial-generation")
                    evaluated_generation = []
                    save("PAUSED", arms=arms)
                    if cancel_check and cancel_check():
                        save("STOPPED_SAFE", arms=arms)
                        raise ResearchCancelled("Адаптивное исследование безопасно остановлено; checkpoint и живой отчёт сохранены")
                    time.sleep(0.2)
                if progress and batch_index % 4 == 0:
                    temporary = max(leaders + evaluated_generation, key=lambda item: tuple(item["score"]), default=None)
                    if temporary and temporary["candidate_id"] != last_displayed_candidate_id:
                        archive_candidates([temporary], {temporary["candidate_id"]}, source="displayed-live-leader")
                        last_displayed_candidate_id = temporary["candidate_id"]
                    progress({
                        "stage": "Самоорганизующийся поиск — поколение выполняется", "tested": evaluated,
                        "total": 0, "percent": min(99, int(stagnant / convergence_generations * 100)),
                        "generation": generation, "stagnant_generations": stagnant,
                        "convergence_generations": convergence_generations,
                        "workers": workers if parallel else 1, "variants_per_second": evaluated / max(time.monotonic() - started, 1e-6),
                        "best": temporary["candidate_id"] if temporary else "—",
                        "best_provisional": ({
                            "eligible": temporary["eligible"],
                            "worst_average_net": min(temporary["validation_a"]["average_net"], temporary["validation_b"]["average_net"]),
                            "validation_fills": temporary["validation_a"]["fills"] + temporary["validation_b"]["fills"],
                            "trades_per_day_a": temporary["validation_a"]["trades_per_day"],
                            "trades_per_day_b": temporary["validation_b"]["trades_per_day"],
                            "config": temporary["config"],
                        } if temporary else {}),
                        "checkpoint": str(checkpoint_path or ""), "live_report": str(live_report_directory or ""),
                        "interesting_archive": archive.summary() if archive else {},
                    })
            by_id = {item["candidate_id"]: item for item in leaders + evaluated_generation}
            leaders = _dedupe_behavior(by_id.values())
            archive_candidates(evaluated_generation, {leaders[0]["candidate_id"]}, source="completed-generation")
            current_objective = float(leaders[0]["objective"])
            current_eligible = bool(leaders[0]["eligible"])
            meaningful = (current_eligible and not best_was_eligible) or (
                current_eligible == best_was_eligible and current_objective > best_objective + 0.0001
            )
            if meaningful:
                best_objective = current_objective; best_was_eligible = current_eligible; stagnant = 0
            else:
                stagnant += 1
            best_config = EconomyConfig(**leaders[0]["config"])
            preview_result = replay_adaptive(rows, features, best_config, 719, test_start)
            preview = _equity_preview(preview_result)
            state = save("RUNNING", preview, arms)
            elapsed = max(time.monotonic() - started, 1e-6)
            if progress:
                va, vb = leaders[0]["validation_a"], leaders[0]["validation_b"]
                progress({
                    "stage": "Самоорганизующийся поиск", "tested": evaluated, "total": 0, "percent": min(99, int(stagnant / convergence_generations * 100)),
                    "generation": generation, "stagnant_generations": stagnant, "convergence_generations": convergence_generations,
                    "workers": workers if parallel else 1, "variants_per_second": evaluated / elapsed,
                    "best": leaders[0]["candidate_id"],
                    "best_provisional": {
                        "eligible": leaders[0]["eligible"], "worst_average_net": min(va["average_net"], vb["average_net"]),
                        "validation_fills": va["fills"] + vb["fills"], "trades_per_day_a": va["trades_per_day"],
                        "trades_per_day_b": vb["trades_per_day"], "config": leaders[0]["config"],
                        "equity_preview": preview,
                    },
                    "checkpoint": str(checkpoint_path or ""), "live_report": str(live_report_directory or ""),
                    "interesting_archive": archive.summary() if archive else {},
                })
            if max_generations is not None and generation >= max_generations:
                break
            if generation >= 8 and stagnant >= convergence_generations:
                break
    finally:
        if executor:
            executor.shutdown(wait=True, cancel_futures=True)

    selected = leaders[0]
    selected_config = EconomyConfig(**selected["config"])
    history_days = len(rows) / 1440
    test_was_opened = bool(selected["eligible"] and history_days >= MIN_CONFIRMATORY_HISTORY_DAYS)
    test_metrics = baseline_test = stress = None
    beats_baseline = False
    if test_was_opened:
        test_result = replay_adaptive(rows, features, selected_config, test_start, n)
        test_metrics = compact_metrics(test_result)
        baseline_test = compact_metrics(replay_economy(rows, evaluation_start_ms=rows[test_start].open_time_ms))
        test_positive = _frequency_ok(test_metrics, selected_config) and test_metrics["average_net"] > 0 and _pf_value(test_metrics) > 1 and test_metrics["compound_net"] > 0
        beats_baseline = (
            test_metrics["compound_net"] >= baseline_test["compound_net"]
            and test_metrics["max_drawdown"] <= max(baseline_test["max_drawdown"], 1e-12) * 1.10
        )
        # Stress uses the same entry logic and perturbs execution costs below.
        stress_cases = {}
        stress_configs = {
            "BASE": selected_config,
            "FEE_25": EconomyConfig(**(asdict(selected_config) | {"fee_rate": selected_config.fee_rate * 1.25})),
            "FEE_50": EconomyConfig(**(asdict(selected_config) | {"fee_rate": selected_config.fee_rate * 1.50})),
            "SPREAD_10BP": EconomyConfig(**(asdict(selected_config) | {"spread_rate": selected_config.spread_rate + 0.001})),
            "SLIPPAGE_X2": EconomyConfig(**(asdict(selected_config) | {"adverse_slippage": selected_config.adverse_slippage * 2})),
            "LATENCY_1M": EconomyConfig(**(asdict(selected_config) | {"entry_latency_minutes": selected_config.entry_latency_minutes + 1})),
            "LATENCY_2M": EconomyConfig(**(asdict(selected_config) | {"entry_latency_minutes": selected_config.entry_latency_minutes + 2})),
            "COMBINED": EconomyConfig(**(asdict(selected_config) | {
                "fee_rate": selected_config.fee_rate * 1.50,
                "spread_rate": selected_config.spread_rate + 0.001,
                "adverse_slippage": selected_config.adverse_slippage * 2,
                "entry_latency_minutes": selected_config.entry_latency_minutes + 2,
            })),
        }
        for case_name, stress_config in stress_configs.items():
            case_metrics = compact_metrics(replay_adaptive(rows, features, stress_config, test_start, n))
            case_metrics["economically_positive"] = (
                case_metrics["fills"] > 0 and case_metrics["average_net"] > 0
                and _pf_value(case_metrics) > 1 and case_metrics["compound_net"] > 0
            )
            stress_cases[case_name] = case_metrics
        positive_cases = sum(bool(item["economically_positive"]) for item in stress_cases.values())
        stress = {
            "passed": positive_cases >= 6 and stress_cases["COMBINED"]["economically_positive"],
            "positive_cases": positive_cases, "total_cases": len(stress_cases), "cases": stress_cases,
        }
    else:
        test_positive = False
    if history_days < MIN_CONFIRMATORY_HISTORY_DAYS:
        verdict = "EXPLORATORY_ONLY"; explanation = "Истории недостаточно для открытия внутреннего TEST."
    elif not selected["eligible"]:
        verdict = "NO_TRADE_WINS"; explanation = "Ни один найденный кандидат не прошёл экономику и частоту своего семейства на TRAIN и обеих validation-секциях."
    elif not test_positive:
        verdict = "REJECTED_ON_TEST"; explanation = "Лучший TRAIN/VALIDATION-кандидат не подтвердился на невиданном TEST."
    elif not beats_baseline:
        verdict = "BASELINE_X_WINS"; explanation = "Кандидат положителен на TEST, но не превзошёл контрольный baseline по экономике и риску."
    elif not stress or not stress["passed"]:
        verdict = "REJECTED_ON_STRESS"; explanation = "Кандидат прошёл TEST, но не выдержал стрессы исполнения."
    else:
        verdict = "CHALLENGER_CANDIDATE"; explanation = "Кандидат прошёл validation, TEST и stress; immutable holdout остаётся закрытым."
    result = {
        "engine": "adaptive-evolution-v8", "mode": "ADAPTIVE_UNTIL_CONVERGENCE", "variants_tested": evaluated,
        "generations": generation, "stagnant_generations": stagnant, "stopping_rule": "robust_objective_improvement_below_0.01pct",
        "history_days": history_days, "research_stage": "CONFIRMATORY" if history_days >= MIN_CONFIRMATORY_HISTORY_DAYS else "EXPLORATORY",
        "history_sufficient_for_test": history_days >= MIN_CONFIRMATORY_HISTORY_DAYS,
        "selected": selected, "leaderboard": leaders[:20],
        "leaderboards_by_family": {
            "ACTIVE": [item for item in leaders if item.get("strategy_family") == "ACTIVE"][:10],
            "SWING": [item for item in leaders if item.get("strategy_family") == "SWING"][:10],
        },
        "test": test_metrics, "baseline_x_test": baseline_test,
        "stress": stress, "beats_baseline_x_on_test": beats_baseline, "verdict": verdict, "explanation": explanation,
        "split": {"test_was_not_used_for_selection": True, "test_was_opened": test_was_opened},
        "search_space": {
            "type": "adaptive_hypothesis_grammar", "fixed_variant_cap": None,
            "dimensions": {key: list(value) for key, value in PARAMETER_SPACE.items()},
            "btc_sol_context_ready": bool(btc_rows and sol_rows),
        },
        "frequency_gate": {
            "ACTIVE": {"minimum_trades_per_day": ACTIVE_MIN_TRADES_PER_DAY, "maximum_trades_per_day": ACTIVE_MAX_TRADES_PER_DAY, "maximum_hold_minutes": ACTIVE_MAX_HOLD_MINUTES},
            "SWING": {"minimum_trades_per_day": SWING_MIN_TRADES_PER_DAY, "maximum_trades_per_day": SWING_MAX_TRADES_PER_DAY, "minimum_hold_minutes_exclusive": ACTIVE_MAX_HOLD_MINUTES},
            "applied_to_train_and_each_validation_fold": True,
        },
        "fusion_execution": fusion_execution_contract(selected_config),
        "compute": {"active_workers": workers if parallel else 1, "multiprocessing": parallel, "checkpoint": str(checkpoint_path or "")},
        "diagnostics": {
            "retained_candidates": len(leaders),
            "eligible_retained_candidates": sum(bool(item.get("eligible")) for item in leaders),
            "selected_rejection_reasons": selected.get("rejection_reasons", []),
            "retained_behavior_families": len({item.get("behavior_signature") for item in leaders}),
        },
        "warnings": [
            "Адаптивный поиск уменьшает бессмысленный перебор, но не устраняет риск переобучения.",
            "TEST не использовался для выбора параметров; immutable holdout остаётся закрытым.",
            "Высокая историческая прибыль не является обещанием будущей доходности.",
            "Реальные ордера отключены.",
        ],
    }
    final_preview = _equity_preview(replay_adaptive(rows, features, selected_config, 719, test_start))
    final_state = save("COMPLETED", final_preview)
    result["live_state"] = final_state
    return result
