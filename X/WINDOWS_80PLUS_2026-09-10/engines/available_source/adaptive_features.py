from __future__ import annotations

import math
from array import array
from collections import deque
from dataclasses import dataclass

from .domain import Candle


VWAP_WINDOWS = (5, 15, 30, 60, 120, 240, 720, 1440, 4320, 10080)
DRAWDOWN_WINDOWS = (30, 60, 180, 360, 720, 1440, 2880, 7200, 14400, 43200)
VOLUME_WINDOWS = (5, 10, 20, 30, 60, 120, 240, 720, 1440)
RETURN_WINDOWS = (1, 3, 5, 15, 30, 60, 120, 240, 720, 1440, 4320, 10080)
RSI_PERIODS = (2, 5, 7, 14, 21, 30, 60, 120, 240, 720)
ATR_PERIODS = (2, 5, 7, 14, 21, 30, 60, 120, 240, 720)
FLOW_WINDOWS = (3, 5, 10, 15, 30, 60, 120, 240)


def _prefix(values: list[float]) -> array:
    result = array("d", [0.0])
    total = 0.0
    for value in values:
        total += value
        result.append(total)
    return result


def _rolling_ratio(values: list[float], windows: tuple[int, ...]) -> dict[int, array]:
    prefix = _prefix(values)
    result: dict[int, array] = {}
    for window in windows:
        output = array("d", [math.nan]) * len(values)
        for index in range(window, len(values)):
            average = (prefix[index] - prefix[index - window]) / window
            output[index] = values[index] / average if average > 0 else math.nan
        result[window] = output
    return result


def _returns(values: list[float], windows: tuple[int, ...]) -> dict[int, array]:
    result: dict[int, array] = {}
    for window in windows:
        output = array("d", [math.nan]) * len(values)
        for index in range(window, len(values)):
            previous = values[index - window]
            output[index] = values[index] / previous - 1.0 if previous > 0 else math.nan
        result[window] = output
    return result


def _vwap_deviation(rows: list[Candle]) -> dict[int, array]:
    weighted = [((item.high + item.low + item.close) / 3.0) * item.quote_volume for item in rows]
    quote = [item.quote_volume for item in rows]
    weighted_prefix, quote_prefix = _prefix(weighted), _prefix(quote)
    result: dict[int, array] = {}
    for window in VWAP_WINDOWS:
        output = array("d", [math.nan]) * len(rows)
        for index in range(window - 1, len(rows)):
            start = index + 1 - window
            denominator = quote_prefix[index + 1] - quote_prefix[start]
            if denominator > 0:
                vwap = (weighted_prefix[index + 1] - weighted_prefix[start]) / denominator
                output[index] = rows[index].close / vwap - 1.0
        result[window] = output
    return result


def _rolling_drawdown(rows: list[Candle]) -> dict[int, array]:
    result: dict[int, array] = {}
    for window in DRAWDOWN_WINDOWS:
        output = array("d", [math.nan]) * len(rows)
        peaks: deque[int] = deque()
        for index, item in enumerate(rows):
            while peaks and peaks[0] <= index - window:
                peaks.popleft()
            while peaks and rows[peaks[-1]].high <= item.high:
                peaks.pop()
            peaks.append(index)
            if index >= window - 1:
                peak = rows[peaks[0]].high
                output[index] = item.close / peak - 1.0 if peak > 0 else math.nan
        result[window] = output
    return result


def _rolling_rebound(rows: list[Candle]) -> dict[int, array]:
    result: dict[int, array] = {}
    for window in DRAWDOWN_WINDOWS:
        output = array("d", [math.nan]) * len(rows)
        lows: deque[int] = deque()
        for index, item in enumerate(rows):
            while lows and lows[0] <= index - window:
                lows.popleft()
            while lows and rows[lows[-1]].low >= item.low:
                lows.pop()
            lows.append(index)
            if index >= window - 1:
                low = rows[lows[0]].low
                output[index] = item.close / low - 1.0 if low > 0 else math.nan
        result[window] = output
    return result


def _rolling_mean_and_slope(values: list[float]) -> tuple[dict[int, array], dict[int, array]]:
    prefix = _prefix(values)
    means: dict[int, array] = {}
    slopes: dict[int, array] = {}
    for window in FLOW_WINDOWS:
        mean_output = array("d", [math.nan]) * len(values)
        slope_output = array("d", [math.nan]) * len(values)
        for index in range(window - 1, len(values)):
            start = index + 1 - window
            current = (prefix[index + 1] - prefix[start]) / window
            mean_output[index] = current
            if start >= window:
                previous = (prefix[start] - prefix[start - window]) / window
                slope_output[index] = current - previous
        means[window] = mean_output
        slopes[window] = slope_output
    return means, slopes


def _rsi(rows: list[Candle]) -> dict[int, array]:
    gains = [0.0] * len(rows); losses = [0.0] * len(rows)
    for index in range(1, len(rows)):
        change = rows[index].close - rows[index - 1].close
        gains[index] = max(change, 0.0); losses[index] = max(-change, 0.0)
    gain_prefix, loss_prefix = _prefix(gains), _prefix(losses)
    result: dict[int, array] = {}
    for period in RSI_PERIODS:
        output = array("d", [math.nan]) * len(rows)
        for index in range(period, len(rows)):
            start = index + 1 - period
            gain = gain_prefix[index + 1] - gain_prefix[start]
            loss = loss_prefix[index + 1] - loss_prefix[start]
            output[index] = 100.0 if loss == 0 and gain > 0 else (0.0 if gain == 0 else 100.0 - 100.0 / (1.0 + gain / loss))
        result[period] = output
    return result


def _atr_ratio(rows: list[Candle]) -> dict[int, array]:
    true_ranges = [0.0] * len(rows)
    for index, item in enumerate(rows):
        previous_close = rows[index - 1].close if index else item.close
        true_ranges[index] = max(item.high - item.low, abs(item.high - previous_close), abs(item.low - previous_close))
    prefix = _prefix(true_ranges)
    result: dict[int, array] = {}
    for period in ATR_PERIODS:
        output = array("d", [math.nan]) * len(rows)
        for index in range(period - 1, len(rows)):
            start = index + 1 - period
            atr = (prefix[index + 1] - prefix[start]) / period
            output[index] = atr / rows[index].close if rows[index].close > 0 else math.nan
        result[period] = output
    return result


def _aligned_close(pump_rows: list[Candle], context_rows: list[Candle] | None) -> list[float]:
    if not context_rows:
        return [math.nan] * len(pump_rows)
    by_time = {item.open_time_ms: item.close for item in context_rows}
    return [float(by_time.get(item.open_time_ms, math.nan)) for item in pump_rows]


@dataclass(slots=True)
class AdaptiveFeatureStore:
    vwap_deviation: dict[int, array]
    drawdown: dict[int, array]
    volume_ratio: dict[int, array]
    pump_return: dict[int, array]
    rsi: dict[int, array]
    atr_ratio: dict[int, array]
    rebound_from_low: dict[int, array]
    buy_flow_mean: dict[int, array]
    buy_flow_slope: dict[int, array]
    btc_return: dict[int, array]
    sol_return: dict[int, array]
    btc_available: bool
    sol_available: bool


def build_feature_store(
    rows: list[Candle], btc_rows: list[Candle] | None = None, sol_rows: list[Candle] | None = None,
) -> AdaptiveFeatureStore:
    closes = [item.close for item in rows]
    flow_mean, flow_slope = _rolling_mean_and_slope([item.buy_share for item in rows])
    return AdaptiveFeatureStore(
        vwap_deviation=_vwap_deviation(rows),
        drawdown=_rolling_drawdown(rows),
        volume_ratio=_rolling_ratio([item.quote_volume for item in rows], VOLUME_WINDOWS),
        pump_return=_returns(closes, RETURN_WINDOWS),
        rsi=_rsi(rows),
        atr_ratio=_atr_ratio(rows),
        rebound_from_low=_rolling_rebound(rows),
        buy_flow_mean=flow_mean,
        buy_flow_slope=flow_slope,
        btc_return=_returns(_aligned_close(rows, btc_rows), RETURN_WINDOWS),
        sol_return=_returns(_aligned_close(rows, sol_rows), RETURN_WINDOWS),
        btc_available=bool(btc_rows),
        sol_available=bool(sol_rows),
    )
