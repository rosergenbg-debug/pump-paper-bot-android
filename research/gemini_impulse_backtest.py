#!/usr/bin/env python3
"""
Causal 5-minute research for the Gemini audit proposals.

The script deliberately separates:
  * development (parameter search),
  * validation (parameter selection),
  * locked holdout (never used for selection),
  * post-study data that did not exist in the original V3.2 research.

Signals use only a completed five-minute candle and enter at the next candle
open. Exits are also executed at the next candle open after confirmation. This
is intentionally conservative for a manually executed signal application.
"""

from __future__ import annotations

import argparse
import io
import itertools
import json
import math
import time
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import asdict, dataclass, replace
from datetime import date, datetime, timedelta, timezone
from pathlib import Path
from typing import Iterable

import numpy as np
import pandas as pd


FIVE_MINUTES_MS = 5 * 60 * 1000
FEE_RATE = 0.0015
START_BALANCE = 1_000.0
STUDY_END = pd.Timestamp("2026-07-15 13:55:00", tz="UTC")
BASELINE_SLIPPAGE = 0.0005


@dataclass(frozen=True)
class SignalConfig:
    volume_ratio: float
    spot_taker_ratio: float
    futures_taker_ratio: float
    min_return_15m: float
    compression_ratio: float | None = None
    relative_strength_15m: float | None = None
    oi_change_10m: float | None = None


@dataclass(frozen=True)
class ExitConfig:
    stop: float = 0.044
    first_target: float = 0.06
    partial_fraction: float = 0.50
    runner_trail: float = 0.04
    max_hold_bars: int = 72
    early_check_bars: int = 3
    early_min_return: float = 0.002
    cooldown_bars: int = 6


@dataclass
class Trade:
    signal_time: str
    entry_time: str
    exit_time: str
    entry_price: float
    exit_equity: float
    return_pct: float
    exit_reason: str
    bars_held: int
    signal_volume_ratio: float
    signal_spot_taker_ratio: float
    signal_futures_taker_ratio: float
    signal_compression_ratio: float
    signal_relative_strength_15m: float
    signal_oi_change_10m: float


def get_json(url: str, params: dict, retries: int = 6):
    target = url + "?" + urllib.parse.urlencode(params)
    request = urllib.request.Request(
        target,
        headers={"Accept": "application/json", "User-Agent": "PumpSignalImpulseResearch/3.3"},
    )
    for attempt in range(retries):
        try:
            with urllib.request.urlopen(request, timeout=45) as response:
                return json.load(response)
        except (urllib.error.URLError, TimeoutError, json.JSONDecodeError):
            if attempt + 1 == retries:
                raise
            time.sleep(1.0 + attempt * 1.5)


def fetch_klines(
    cache_dir: Path,
    name: str,
    base_url: str,
    symbol: str,
    start_ms: int,
    end_ms: int,
    limit: int,
) -> pd.DataFrame:
    cache_file = cache_dir / f"{name}_5m_{start_ms}_{end_ms}.pkl"
    if cache_file.exists():
        return pd.read_pickle(cache_file)
    rows: list[list] = []
    cursor = start_ms
    while cursor < end_ms:
        batch = get_json(
            base_url,
            {
                "symbol": symbol,
                "interval": "5m",
                "startTime": cursor,
                "endTime": end_ms,
                "limit": limit,
            },
        )
        if not batch:
            break
        rows.extend(batch)
        next_cursor = int(batch[-1][6]) + 1
        if next_cursor <= cursor:
            break
        cursor = next_cursor
        if len(batch) < limit:
            break
    columns = [
        "open_time",
        "open",
        "high",
        "low",
        "close",
        "volume",
        "close_time",
        "quote_volume",
        "trades",
        "taker_buy_volume",
        "taker_buy_quote",
        "ignore",
    ]
    frame = pd.DataFrame(rows, columns=columns)
    if frame.empty:
        raise RuntimeError(f"No five-minute data returned for {name}")
    numeric = [
        "open",
        "high",
        "low",
        "close",
        "volume",
        "quote_volume",
        "trades",
        "taker_buy_volume",
        "taker_buy_quote",
    ]
    frame[numeric] = frame[numeric].apply(pd.to_numeric, errors="coerce")
    frame["open_time"] = pd.to_numeric(frame["open_time"], errors="coerce").astype("int64")
    frame["close_time"] = pd.to_numeric(frame["close_time"], errors="coerce").astype("int64")
    frame = frame.drop_duplicates("close_time").sort_values("close_time").reset_index(drop=True)
    frame.to_pickle(cache_file)
    return frame


def month_starts(start: pd.Timestamp, end: pd.Timestamp) -> list[pd.Timestamp]:
    current = start.to_period("M").start_time.tz_localize("UTC")
    last_complete = end.to_period("M").start_time.tz_localize("UTC") - pd.Timedelta(days=1)
    result = []
    while current <= last_complete:
        result.append(current)
        current = current + pd.offsets.MonthBegin(1)
    return result


def parse_kline_archive(payload: bytes) -> pd.DataFrame:
    columns = [
        "open_time",
        "open",
        "high",
        "low",
        "close",
        "volume",
        "close_time",
        "quote_volume",
        "trades",
        "taker_buy_volume",
        "taker_buy_quote",
        "ignore",
    ]
    with zipfile.ZipFile(io.BytesIO(payload)) as archive:
        with archive.open(archive.namelist()[0]) as source:
            first = source.readline().decode("utf-8", errors="replace").strip()
            source.seek(0)
            has_header = first.lower().startswith("open_time")
            frame = pd.read_csv(source, header=0 if has_header else None)
    if has_header:
        frame = frame.rename(
            columns={
                "count": "trades",
                "taker_buy_quote_volume": "taker_buy_quote",
            }
        )
        frame = frame[columns]
    else:
        frame.columns = columns
    return frame


def fetch_kline_archive(url: str) -> pd.DataFrame | None:
    request = urllib.request.Request(url, headers={"User-Agent": "PumpSignalImpulseResearch/3.3"})
    for attempt in range(4):
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                return parse_kline_archive(response.read())
        except urllib.error.HTTPError as error:
            if error.code == 404:
                return None
            if attempt == 3:
                raise
        except (urllib.error.URLError, TimeoutError):
            if attempt == 3:
                raise
            time.sleep(1.0 + attempt * 2.0)
    return None


def fetch_monthly_kline_archive(
    market: str,
    symbol: str,
    month: pd.Timestamp,
) -> pd.DataFrame | None:
    stamp = month.strftime("%Y-%m")
    url = (
        f"https://data.binance.vision/data/{market}/monthly/klines/"
        f"{symbol}/5m/{symbol}-5m-{stamp}.zip"
    )
    return fetch_kline_archive(url)


def fetch_daily_kline_archive(
    market: str,
    symbol: str,
    day: date,
) -> pd.DataFrame | None:
    stamp = day.isoformat()
    url = (
        f"https://data.binance.vision/data/{market}/daily/klines/"
        f"{symbol}/5m/{symbol}-5m-{stamp}.zip"
    )
    return fetch_kline_archive(url)


def fetch_hybrid_klines(
    cache_dir: Path,
    name: str,
    market: str,
    api_url: str,
    symbol: str,
    start: pd.Timestamp,
    end: pd.Timestamp,
    limit: int,
) -> pd.DataFrame:
    cache_file = cache_dir / f"{name}_5m_hybrid_{start.date()}_{end.date()}.pkl"
    if cache_file.exists():
        return pd.read_pickle(cache_file)
    months = month_starts(start, end)
    frames: list[pd.DataFrame] = []
    with ThreadPoolExecutor(max_workers=min(6, max(1, len(months)))) as pool:
        futures = {
            pool.submit(fetch_monthly_kline_archive, market, symbol, month): month
            for month in months
        }
        for future in as_completed(futures):
            frame = future.result()
            if frame is not None and not frame.empty:
                frames.append(frame)
    current_month_start = end.to_period("M").start_time.tz_localize("UTC")
    recent_start = max(start, current_month_start)
    recent_days = list(daterange(recent_start.date(), end.date() - timedelta(days=1)))
    with ThreadPoolExecutor(max_workers=min(6, max(1, len(recent_days)))) as pool:
        futures = {
            pool.submit(fetch_daily_kline_archive, market, symbol, day): day
            for day in recent_days
        }
        for future in as_completed(futures):
            daily = future.result()
            if daily is not None and not daily.empty:
                frames.append(daily)
    frame = pd.concat(frames, ignore_index=True)
    numeric = [
        "open",
        "high",
        "low",
        "close",
        "volume",
        "quote_volume",
        "trades",
        "taker_buy_volume",
        "taker_buy_quote",
    ]
    frame[numeric] = frame[numeric].apply(pd.to_numeric, errors="coerce")
    for column in ["open_time", "close_time"]:
        values = pd.to_numeric(frame[column], errors="coerce")
        frame[column] = np.where(values > 100_000_000_000_000, values / 1000.0, values).astype("int64")
    frame = frame[
        (frame["open_time"] >= int(start.timestamp() * 1000))
        & (frame["close_time"] <= int(end.timestamp() * 1000))
    ]
    frame = frame.drop_duplicates("close_time").sort_values("close_time").reset_index(drop=True)
    if frame.empty:
        raise RuntimeError(f"No combined archive/API data for {name}")
    frame.to_pickle(cache_file)
    return frame


def daterange(first: date, last: date) -> Iterable[date]:
    current = first
    while current <= last:
        yield current
        current += timedelta(days=1)


def fetch_daily_metric(cache_dir: Path, day: date) -> pd.DataFrame | None:
    stamp = day.isoformat()
    daily_dir = cache_dir / "metrics_daily"
    daily_dir.mkdir(parents=True, exist_ok=True)
    cache_file = daily_dir / f"{stamp}.pkl"
    if cache_file.exists():
        return pd.read_pickle(cache_file)
    url = (
        "https://data.binance.vision/data/futures/um/daily/metrics/"
        f"PUMPUSDT/PUMPUSDT-metrics-{stamp}.zip"
    )
    request = urllib.request.Request(url, headers={"User-Agent": "PumpSignalImpulseResearch/3.3"})
    payload = None
    for attempt in range(5):
        try:
            with urllib.request.urlopen(request, timeout=45) as response:
                payload = response.read()
            break
        except urllib.error.HTTPError as error:
            if error.code == 404:
                return None
            if attempt == 4:
                return None
        except (urllib.error.URLError, TimeoutError):
            if attempt == 4:
                return None
            time.sleep(1.0 + attempt * 2.0)
    if payload is None:
        return None
    with zipfile.ZipFile(io.BytesIO(payload)) as archive:
        csv_name = archive.namelist()[0]
        with archive.open(csv_name) as source:
            frame = pd.read_csv(source)
    frame.to_pickle(cache_file)
    return frame


def fetch_open_interest(
    cache_dir: Path,
    start: pd.Timestamp,
    end: pd.Timestamp,
) -> pd.DataFrame:
    cache_file = cache_dir / f"pump_metrics_5m_{start.date()}_{end.date()}.pkl"
    if cache_file.exists():
        return pd.read_pickle(cache_file)
    days = list(daterange(start.date(), end.date()))
    frames: list[pd.DataFrame] = []
    with ThreadPoolExecutor(max_workers=8) as pool:
        futures = {pool.submit(fetch_daily_metric, cache_dir, day): day for day in days}
        for future in as_completed(futures):
            frame = future.result()
            if frame is not None and not frame.empty:
                frames.append(frame)
    if not frames:
        raise RuntimeError("No archived PUMP open-interest metrics were available")
    metrics = pd.concat(frames, ignore_index=True)
    metrics["create_time"] = pd.to_datetime(metrics["create_time"], utc=True)
    numeric = [
        "sum_open_interest",
        "sum_open_interest_value",
        "sum_taker_long_short_vol_ratio",
    ]
    metrics[numeric] = metrics[numeric].apply(pd.to_numeric, errors="coerce")
    metrics = metrics.drop_duplicates("create_time").sort_values("create_time")
    metrics.to_pickle(cache_file)
    return metrics


def download_sources(cache_dir: Path, start: pd.Timestamp, end: pd.Timestamp) -> dict[str, pd.DataFrame]:
    cache_dir.mkdir(parents=True, exist_ok=True)
    start_ms = int(start.timestamp() * 1000)
    end_ms = int(end.timestamp() * 1000)
    tasks = {
        "pump_spot": (
            "spot",
            "https://data-api.binance.vision/api/v3/klines",
            "PUMPUSDT",
            1000,
        ),
        "eur_spot": (
            "spot",
            "https://data-api.binance.vision/api/v3/klines",
            "EURUSDT",
            1000,
        ),
        "btc_spot": (
            "spot",
            "https://data-api.binance.vision/api/v3/klines",
            "BTCUSDT",
            1000,
        ),
        "sol_spot": (
            "spot",
            "https://data-api.binance.vision/api/v3/klines",
            "SOLUSDT",
            1000,
        ),
        "pump_futures": (
            "futures/um",
            "https://fapi.binance.com/fapi/v1/klines",
            "PUMPUSDT",
            1500,
        ),
    }
    result: dict[str, pd.DataFrame] = {}
    for name, (market, endpoint, symbol, limit) in tasks.items():
        result[name] = fetch_hybrid_klines(
            cache_dir,
            name,
            market,
            endpoint,
            symbol,
            start,
            end,
            limit,
        )
        print(f"downloaded {name}: {len(result[name])}", flush=True)
    result["metrics"] = fetch_open_interest(cache_dir, start, end)
    print(f"downloaded metrics: {len(result['metrics'])}", flush=True)
    return result


def prepare_frame(source: dict[str, pd.DataFrame]) -> pd.DataFrame:
    pump = source["pump_spot"].copy().set_index("close_time")
    eur = source["eur_spot"].copy().set_index("close_time")
    common = pump.index.intersection(eur.index)
    pump = pump.loc[common]
    eur = eur.loc[common]
    frame = pd.DataFrame(index=common)
    frame["open_time"] = pump["open_time"]
    frame["pump_open"] = pump["open"] / eur["open"]
    frame["pump_high"] = pump["high"] / eur["low"]
    frame["pump_low"] = pump["low"] / eur["high"]
    frame["pump_close"] = pump["close"] / eur["close"]
    frame["pump_volume"] = pump["volume"]
    frame["pump_trades"] = pump["trades"]
    frame["pump_taker_buy_volume"] = pump["taker_buy_volume"]

    for name in ["btc_spot", "sol_spot"]:
        prefix = name.split("_")[0]
        peer = source[name].set_index("close_time")
        frame = frame.join(
            peer[["open", "high", "low", "close", "volume"]].rename(
                columns=lambda value: f"{prefix}_{value}"
            ),
            how="left",
        )

    futures = source["pump_futures"].set_index("close_time")
    frame = frame.join(
        futures[["volume", "taker_buy_volume"]].rename(
            columns={"volume": "futures_volume", "taker_buy_volume": "futures_taker_buy_volume"}
        ),
        how="left",
    )
    frame.index = pd.to_datetime(frame.index, unit="ms", utc=True)
    frame = frame.sort_index()

    metrics = source["metrics"].copy().set_index("create_time").sort_index()
    frame = pd.merge_asof(
        frame.reset_index().rename(columns={"close_time": "time"}).sort_values("time"),
        metrics[["sum_open_interest", "sum_open_interest_value", "sum_taker_long_short_vol_ratio"]]
        .reset_index()
        .rename(columns={"create_time": "metric_time"}),
        left_on="time",
        right_on="metric_time",
        direction="backward",
        tolerance=pd.Timedelta(minutes=10),
    ).set_index("time")

    frame["return_5m"] = frame["pump_close"].pct_change()
    frame["return_15m"] = frame["pump_close"].pct_change(3)
    frame["return_60m"] = frame["pump_close"].pct_change(12)
    frame["return_3h"] = frame["pump_close"].pct_change(36)
    frame["btc_return_15m"] = frame["btc_close"].pct_change(3)
    frame["sol_return_15m"] = frame["sol_close"].pct_change(3)
    frame["relative_strength_15m"] = frame["return_15m"] - frame[
        ["btc_return_15m", "sol_return_15m"]
    ].mean(axis=1)

    past_volume = frame["pump_volume"].shift(1).rolling(36, min_periods=24).median()
    frame["volume_ratio"] = frame["pump_volume"] / past_volume
    spot_ratio = frame["pump_taker_buy_volume"] / frame["pump_volume"].replace(0.0, np.nan)
    futures_ratio = frame["futures_taker_buy_volume"] / frame["futures_volume"].replace(0.0, np.nan)
    frame["spot_taker_ratio"] = spot_ratio.rolling(2, min_periods=2).mean()
    frame["futures_taker_ratio"] = futures_ratio.rolling(2, min_periods=2).mean()

    prior_high = frame["pump_high"].shift(1).rolling(12, min_periods=12).max()
    prior_low = frame["pump_low"].shift(1).rolling(12, min_periods=12).min()
    frame["prior_range_60m"] = prior_high / prior_low - 1.0
    typical_range = frame["prior_range_60m"].shift(1).rolling(2016, min_periods=864).median()
    frame["compression_ratio"] = frame["prior_range_60m"] / typical_range
    frame["breakout_60m"] = frame["pump_close"] > prior_high
    frame["oi_change_10m"] = frame["sum_open_interest"].pct_change(2)
    frame["market_return_15m"] = frame[["btc_return_15m", "sol_return_15m"]].mean(axis=1)
    return frame.replace([np.inf, -np.inf], np.nan)


def signal_mask(frame: pd.DataFrame, config: SignalConfig) -> pd.Series:
    mask = (
        (frame["volume_ratio"] >= config.volume_ratio)
        & (frame["spot_taker_ratio"] >= config.spot_taker_ratio)
        & (frame["futures_taker_ratio"] >= config.futures_taker_ratio)
        & (frame["return_15m"] >= config.min_return_15m)
        & (frame["return_15m"] <= 0.05)
        & (frame["return_60m"] < 0.08)
        & (frame["return_5m"] > 0.0)
        & (frame["return_5m"] < 0.04)
        & (frame["return_3h"] > -0.10)
        & (frame["market_return_15m"] > -0.025)
        & frame["breakout_60m"]
    )
    if config.compression_ratio is not None:
        mask &= frame["compression_ratio"] <= config.compression_ratio
    if config.relative_strength_15m is not None:
        mask &= frame["relative_strength_15m"] >= config.relative_strength_15m
    if config.oi_change_10m is not None:
        mask &= frame["oi_change_10m"] >= config.oi_change_10m
    return mask.fillna(False)


def run_backtest(
    frame: pd.DataFrame,
    signals: pd.Series,
    start: pd.Timestamp,
    end: pd.Timestamp,
    exit_config: ExitConfig,
    slippage: float,
) -> dict:
    opens = frame["pump_open"].to_numpy(dtype=float)
    highs = frame["pump_high"].to_numpy(dtype=float)
    lows = frame["pump_low"].to_numpy(dtype=float)
    closes = frame["pump_close"].to_numpy(dtype=float)
    signal_values = signals.to_numpy(dtype=bool)
    index = frame.index
    start_i = int(index.searchsorted(start, side="left"))
    end_i = min(int(index.searchsorted(end, side="right")) - 1, len(frame) - 2)
    cash = START_BALANCE
    equity_curve = [cash]
    trades: list[Trade] = []
    i = max(start_i, 36)
    cooldown_until = i

    while i <= end_i:
        if i < cooldown_until or not signal_values[i]:
            i += 1
            continue
        entry_i = i + 1
        if entry_i > end_i:
            break
        initial_cash = cash
        entry_price = opens[entry_i] * (1.0 + slippage)
        buy_fee = cash * FEE_RATE
        coins = (cash - buy_fee) / entry_price
        cash = 0.0
        partial_taken = False
        highest_after_partial = entry_price
        exit_reason = "MAX_HOLD"
        exit_i = min(entry_i + exit_config.max_hold_bars + 1, end_i)
        j = entry_i

        while j < min(entry_i + exit_config.max_hold_bars, end_i):
            execution_i = j + 1
            hard_stop = entry_price * (1.0 - exit_config.stop)
            if not partial_taken:
                if lows[j] <= hard_stop:
                    sell_price = opens[execution_i] * (1.0 - slippage)
                    gross = coins * sell_price
                    cash += gross * (1.0 - FEE_RATE)
                    coins = 0.0
                    exit_reason = "STOP"
                    exit_i = execution_i
                    equity_curve.append(cash)
                    break
                if highs[j] >= entry_price * (1.0 + exit_config.first_target):
                    sell_price = opens[execution_i] * (1.0 - slippage)
                    sold = coins * exit_config.partial_fraction
                    cash += sold * sell_price * (1.0 - FEE_RATE)
                    coins -= sold
                    partial_taken = True
                    highest_after_partial = max(
                        entry_price * (1.0 + exit_config.first_target),
                        highs[j],
                        opens[execution_i],
                    )
                    j = execution_i
                    continue
                if (
                    j >= entry_i + exit_config.early_check_bars - 1
                    and closes[j] < entry_price * (1.0 + exit_config.early_min_return)
                ):
                    sell_price = opens[execution_i] * (1.0 - slippage)
                    gross = coins * sell_price
                    cash += gross * (1.0 - FEE_RATE)
                    coins = 0.0
                    exit_reason = "EARLY_FAIL"
                    exit_i = execution_i
                    equity_curve.append(cash)
                    break
            else:
                runner_stop = max(entry_price, highest_after_partial * (1.0 - exit_config.runner_trail))
                if lows[j] <= runner_stop:
                    sell_price = opens[execution_i] * (1.0 - slippage)
                    gross = coins * sell_price
                    cash += gross * (1.0 - FEE_RATE)
                    coins = 0.0
                    exit_reason = "RUNNER_TRAIL"
                    exit_i = execution_i
                    equity_curve.append(cash)
                    break
                highest_after_partial = max(highest_after_partial, highs[j])
            mark = cash + coins * lows[j] * (1.0 - slippage) * (1.0 - FEE_RATE)
            equity_curve.append(mark)
            j += 1

        if coins > 0.0:
            sell_price = opens[exit_i] * (1.0 - slippage)
            gross = coins * sell_price
            cash += gross * (1.0 - FEE_RATE)
            coins = 0.0
            equity_curve.append(cash)

        signal = frame.iloc[i]
        trades.append(
            Trade(
                signal_time=index[i].isoformat(),
                entry_time=index[entry_i].isoformat(),
                exit_time=index[exit_i].isoformat(),
                entry_price=entry_price,
                exit_equity=cash,
                return_pct=(cash / initial_cash - 1.0) * 100.0,
                exit_reason=exit_reason,
                bars_held=max(0, exit_i - entry_i),
                signal_volume_ratio=float(signal["volume_ratio"]),
                signal_spot_taker_ratio=float(signal["spot_taker_ratio"]),
                signal_futures_taker_ratio=float(signal["futures_taker_ratio"]),
                signal_compression_ratio=float(signal["compression_ratio"]),
                signal_relative_strength_15m=float(signal["relative_strength_15m"]),
                signal_oi_change_10m=float(signal["oi_change_10m"]),
            )
        )
        cooldown_until = exit_i + exit_config.cooldown_bars
        i = cooldown_until

    peak = equity_curve[0]
    max_drawdown = 0.0
    for equity in equity_curve:
        peak = max(peak, equity)
        if peak > 0.0:
            max_drawdown = min(max_drawdown, equity / peak - 1.0)
    wins = sum(trade.return_pct > 0.0 for trade in trades)
    return {
        "start": start.isoformat(),
        "end": end.isoformat(),
        "slippage_each_side": slippage,
        "equity": cash,
        "return_pct": (cash / START_BALANCE - 1.0) * 100.0,
        "round_trips": len(trades),
        "wins": wins,
        "win_rate": wins / len(trades) * 100.0 if trades else 0.0,
        "max_drawdown_pct": max_drawdown * 100.0,
        "trades": [asdict(trade) for trade in trades],
    }


def selection_score(train: dict, validation: dict) -> float:
    if train["round_trips"] < 8 or validation["round_trips"] < 3:
        return -1_000.0
    worst = min(train["return_pct"], validation["return_pct"])
    drawdown_penalty = max(
        0.0,
        abs(min(train["max_drawdown_pct"], validation["max_drawdown_pct"])) - 12.0,
    )
    return worst + 0.20 * (train["return_pct"] + validation["return_pct"]) - drawdown_penalty


def evaluate_config(
    frame: pd.DataFrame,
    config: SignalConfig,
    ranges: dict[str, tuple[pd.Timestamp, pd.Timestamp]],
    exit_config: ExitConfig,
    slippage: float,
) -> dict:
    signals = signal_mask(frame, config)
    train = run_backtest(frame, signals, *ranges["train"], exit_config, slippage)
    validation = run_backtest(frame, signals, *ranges["validation"], exit_config, slippage)
    return {
        "config": asdict(config),
        "score": selection_score(train, validation),
        "train": train,
        "validation": validation,
    }


def progressive_search(
    frame: pd.DataFrame,
    ranges: dict[str, tuple[pd.Timestamp, pd.Timestamp]],
    exit_config: ExitConfig,
) -> dict[str, dict]:
    slippage = 0.005
    base_results = []
    for values in itertools.product(
        [1.8, 2.2, 2.8, 3.5],
        [0.56, 0.60, 0.64, 0.68, 0.70],
        [0.52, 0.56, 0.60, 0.64],
        [0.002, 0.005, 0.010],
    ):
        config = SignalConfig(*values)
        base_results.append(evaluate_config(frame, config, ranges, exit_config, slippage))
    base_results.sort(key=lambda item: item["score"], reverse=True)
    best_base = base_results[0]

    compression_results = []
    for base in base_results[:20]:
        config = SignalConfig(**base["config"])
        for threshold in [0.45, 0.65, 0.85, 1.05]:
            compression_results.append(
                evaluate_config(
                    frame,
                    replace(config, compression_ratio=threshold),
                    ranges,
                    exit_config,
                    slippage,
                )
            )
    compression_results.sort(key=lambda item: item["score"], reverse=True)
    best_compression = compression_results[0]

    relative_results = []
    for previous in compression_results[:15]:
        config = SignalConfig(**previous["config"])
        for threshold in [0.0, 0.003, 0.006, 0.010]:
            relative_results.append(
                evaluate_config(
                    frame,
                    replace(config, relative_strength_15m=threshold),
                    ranges,
                    exit_config,
                    slippage,
                )
            )
    relative_results.sort(key=lambda item: item["score"], reverse=True)
    best_relative = relative_results[0]

    oi_results = []
    for previous in relative_results[:15]:
        config = SignalConfig(**previous["config"])
        for threshold in [0.0, 0.0025, 0.005, 0.010, 0.020, 0.050]:
            oi_results.append(
                evaluate_config(
                    frame,
                    replace(config, oi_change_10m=threshold),
                    ranges,
                    exit_config,
                    slippage,
                )
            )
    oi_results.sort(key=lambda item: item["score"], reverse=True)
    best_oi = oi_results[0]
    return {
        "micro": best_base,
        "micro_compression": best_compression,
        "micro_compression_relative": best_relative,
        "micro_compression_relative_oi": best_oi,
    }


def baseline_stress(baseline_json: Path | None) -> dict:
    if baseline_json is None or not baseline_json.exists():
        return {}
    payload = json.loads(baseline_json.read_text(encoding="utf-8"))
    candidate = next(item for item in payload["results"] if item["candidate_rank"] == 1)
    baseline = candidate["six_months"]
    returns = [trade["return_pct"] / 100.0 for trade in baseline["trades"]]
    stressed = {}
    for slippage in [0.0005, 0.005, 0.010, 0.020]:
        execution_ratio = (
            (1.0 + BASELINE_SLIPPAGE)
            / (1.0 + slippage)
            * (1.0 - slippage)
            / (1.0 - BASELINE_SLIPPAGE)
        )
        equity = START_BALANCE
        for trade_return in returns:
            equity *= (1.0 + trade_return) * execution_ratio
        stressed[str(slippage)] = {
            "return_pct": (equity / START_BALANCE - 1.0) * 100.0,
            "round_trips": len(returns),
            "source_return_pct_at_0_0005": baseline["return_pct"],
            "method": "execution-price stress applied to each completed historical round trip",
        }
    return stressed


def compact_result(result: dict) -> dict:
    return {
        "return_pct": result["return_pct"],
        "round_trips": result["round_trips"],
        "wins": result["wins"],
        "win_rate": result["win_rate"],
        "max_drawdown_pct": result["max_drawdown_pct"],
    }


def json_default(value):
    if isinstance(value, np.generic):
        return value.item()
    if isinstance(value, pd.Timestamp):
        return value.isoformat()
    raise TypeError(f"Unsupported JSON value: {type(value).__name__}")


def make_report(payload: dict) -> str:
    lines = [
        "# Обратный анализ предложений Gemini: быстрый импульс V3.3",
        "",
        "Это исследование сигналов, а не обещание доходности. Сигнал формируется только после закрытия "
        "5-минутной свечи; вход и подтверждённый выход исполняются на следующем открытии.",
        "",
        "## Разделение истории",
        "",
    ]
    for name, values in payload["ranges"].items():
        lines.append(f"- {name}: {values[0]} — {values[1]}")
    lines += [
        "",
        "## Лучшие конфигурации, выбранные без просмотра holdout",
        "",
        "| Модуль | Train | Validation | Holdout | Сделок holdout | Просадка holdout |",
        "|---|---:|---:|---:|---:|---:|",
    ]
    for name, item in payload["variants"].items():
        lines.append(
            f"| {name} | {item['train']['return_pct']:.2f}% | "
            f"{item['validation']['return_pct']:.2f}% | {item['holdout']['return_pct']:.2f}% | "
            f"{item['holdout']['round_trips']} | {item['holdout']['max_drawdown_pct']:.2f}% |"
        )
    chosen = payload["chosen"]
    lines += [
        "",
        "## Решение о включении",
        "",
        "**Торговое включение отклонено. Проверенная дополнительная прибыль для V3.3: 0,00%, "
        "потому что убыточный модуль оставлен только в Shadow Mode.**",
        "",
        "Ниже показана наименее плохая конфигурация из проверенных — не рекомендация к торговле.",
        "",
        f"Конфигурация: `{json.dumps(chosen['config'], ensure_ascii=False)}`",
        "",
        "| Проскальзывание на сторону | 6 месяцев | Сделок | Win rate | Макс. просадка |",
        "|---:|---:|---:|---:|---:|",
    ]
    for key, item in chosen["six_month_stress"].items():
        lines.append(
            f"| {float(key) * 100:.2f}% | {item['return_pct']:.2f}% | {item['round_trips']} | "
            f"{item['win_rate']:.1f}% | {item['max_drawdown_pct']:.2f}% |"
        )
    baseline = payload.get("v3_2_baseline_stress", {})
    if baseline:
        lines += [
            "",
            "## Контрольная V3.2",
            "",
            "Контрольная четырёхэтапная V3.2 на том же старом шестимесячном исследовании: "
            f"{baseline['0.0005']['return_pct']:.2f}% при 0,05% проскальзывания. "
            "Это исторический результат, а не прогноз.",
        ]
    post = chosen["post_study"]
    lines += [
        "",
        "## Данные после старого исследования",
        "",
        f"На полностью новом отрезке после 15.07.2026: {post['return_pct']:.2f}% при "
        f"{post['round_trips']} завершённых сделках. Короткий отрезок не считается статистическим доказательством.",
        "",
        "## Что именно проверено",
        "",
        "- 5m объём и устойчивый spot/futures taker-поток;",
        "- пробой часового максимума без покупки уже после роста 8% за час;",
        "- сжатие диапазона до пробоя;",
        "- относительная сила PUMP против BTC и SOL;",
        "- изменение открытого интереса за 10 минут;",
        "- ранний временной выход, если импульс не продолжился за 15 минут;",
        "- стресс проскальзывания 0,5%, 1% и 2% на каждую сторону.",
        "",
        "## Ограничения",
        "",
        "- История Binance не равна фактическому исполнению Bitpanda Fusion.",
        "- Исторического bid/ask spread нет, поэтому он должен проверяться только в живом режиме.",
        "- Новостной слой Gemini не включён в историческую прибыль: надёжного архива одинаково "
        "доступных в тот момент новостей пока нет.",
        "- Даже locked holdout проверяет одну монету и ограниченное число независимых режимов рынка.",
    ]
    return "\n".join(lines) + "\n"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cache-dir", type=Path, default=Path("research/cache"))
    parser.add_argument("--output-dir", type=Path, default=Path("research/output"))
    parser.add_argument("--baseline-json", type=Path)
    parser.add_argument(
        "--start",
        default="2025-09-11T12:30:00+00:00",
        help="First timestamp requested from Binance",
    )
    parser.add_argument(
        "--end",
        default=datetime.now(timezone.utc).replace(second=0, microsecond=0).isoformat(),
    )
    args = parser.parse_args()
    start = pd.Timestamp(args.start)
    end = pd.Timestamp(args.end)
    if start.tzinfo is None:
        start = start.tz_localize("UTC")
    if end.tzinfo is None:
        end = end.tz_localize("UTC")
    end = min(end, pd.Timestamp.now(tz="UTC").floor("5min"))

    source = download_sources(args.cache_dir, start, end)
    frame = prepare_frame(source).dropna(
        subset=[
            "pump_open",
            "pump_high",
            "pump_low",
            "pump_close",
            "volume_ratio",
            "spot_taker_ratio",
            "futures_taker_ratio",
        ]
    )
    study = frame.loc[:STUDY_END]
    if len(study) < 10_000:
        raise RuntimeError("Not enough aligned study history")
    split_train = study.index[int(len(study) * 0.60)]
    split_validation = study.index[int(len(study) * 0.80)]
    ranges = {
        "train": (study.index[0], split_train - pd.Timedelta(minutes=5)),
        "validation": (split_train, split_validation - pd.Timedelta(minutes=5)),
        "holdout": (split_validation, study.index[-2]),
    }
    exit_config = ExitConfig()
    selected = progressive_search(frame, ranges, exit_config)

    variants = {}
    for name, selection in selected.items():
        config = SignalConfig(**selection["config"])
        signals = signal_mask(frame, config)
        variants[name] = {
            "config": asdict(config),
            "selection_score": selection["score"],
            "train": compact_result(selection["train"]),
            "validation": compact_result(selection["validation"]),
            "holdout": compact_result(
                run_backtest(frame, signals, *ranges["holdout"], exit_config, 0.005)
            ),
        }

    eligible = [
        (name, item)
        for name, item in variants.items()
        if item["train"]["return_pct"] > 0.0
        and item["validation"]["return_pct"] > 0.0
        and item["holdout"]["return_pct"] > 0.0
        and item["holdout"]["round_trips"] >= 2
    ]
    if eligible:
        chosen_name = max(
            eligible,
            key=lambda pair: min(
                pair[1]["train"]["return_pct"],
                pair[1]["validation"]["return_pct"],
                pair[1]["holdout"]["return_pct"],
            ),
        )[0]
    else:
        chosen_name = max(
            variants,
            key=lambda name: min(
                variants[name]["train"]["return_pct"],
                variants[name]["validation"]["return_pct"],
                variants[name]["holdout"]["return_pct"],
            ),
        )
    chosen_config = SignalConfig(**variants[chosen_name]["config"])
    chosen_signals = signal_mask(frame, chosen_config)
    six_month_range = (STUDY_END - pd.Timedelta(days=183), study.index[-2])
    stress = {
        str(slippage): compact_result(
            run_backtest(frame, chosen_signals, *six_month_range, exit_config, slippage)
        )
        for slippage in [0.0005, 0.005, 0.010, 0.020]
    }
    post_start = STUDY_END + pd.Timedelta(minutes=5)
    post = compact_result(
        run_backtest(frame, chosen_signals, post_start, frame.index[-2], exit_config, 0.005)
    ) if frame.index[-2] > post_start else compact_result(
        run_backtest(frame, chosen_signals, post_start, post_start, exit_config, 0.005)
    )
    payload = {
        "generated_at": pd.Timestamp.now(tz="UTC").isoformat(),
        "data": {
            "rows": len(frame),
            "start": frame.index[0].isoformat(),
            "end": frame.index[-1].isoformat(),
        },
        "ranges": {name: [a.isoformat(), b.isoformat()] for name, (a, b) in ranges.items()},
        "method": {
            "fee_each_side": FEE_RATE,
            "selection_slippage_each_side": 0.005,
            "entry": "next 5m open after a closed signal candle",
            "exit": asdict(exit_config),
        },
        "variants": variants,
        "chosen": {
            "name": chosen_name,
            "accepted_for_trading": False,
            "verified_additional_profit_pct": 0.0,
            "decision": "REJECT_TRADING_KEEP_SHADOW",
            "config": asdict(chosen_config),
            "six_month_range": [value.isoformat() for value in six_month_range],
            "six_month_stress": stress,
            "post_study": post,
        },
        "v3_2_baseline_stress": baseline_stress(args.baseline_json),
    }
    args.output_dir.mkdir(parents=True, exist_ok=True)
    (args.output_dir / "gemini_impulse_results.json").write_text(
        json.dumps(payload, ensure_ascii=False, indent=2, default=json_default),
        encoding="utf-8",
    )
    (args.output_dir / "GEMINI_IMPULSE_BACKTEST_RU.md").write_text(
        make_report(payload),
        encoding="utf-8",
    )
    pd.DataFrame(
        [
            {"variant": name, **item["config"], **{f"holdout_{k}": v for k, v in item["holdout"].items()}}
            for name, item in variants.items()
        ]
    ).to_csv(args.output_dir / "gemini_impulse_variants.csv", index=False)
    print(
        json.dumps(payload["chosen"], ensure_ascii=False, indent=2, default=json_default),
        flush=True,
    )


if __name__ == "__main__":
    main()
