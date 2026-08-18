#!/usr/bin/env python3
"""Causal three-month study of PUMP aggressive-buy lifecycles.

The script uses only closed Binance five-minute bars.  A bar's taker-buy quote
volume is interpreted as aggressive buying, while the price response is kept as
a separate variable.  This distinction is essential: a high buy share with a
flat or falling price is potential sell-side absorption, not proof of growth.
"""

from __future__ import annotations

import argparse
import json
import time
import urllib.parse
import urllib.request
from dataclasses import dataclass
from pathlib import Path

import numpy as np
import pandas as pd


FIVE_MINUTES_MS = 300_000
BASE_URL = "https://data-api.binance.vision/api/v3/klines"


def fetch(symbol: str, start: pd.Timestamp, end: pd.Timestamp, cache: Path) -> pd.DataFrame:
    cache.mkdir(parents=True, exist_ok=True)
    target = cache / f"{symbol}_5m_{start.strftime('%Y%m%d')}_{end.strftime('%Y%m%d')}.pkl"
    if target.exists():
        return pd.read_pickle(target)
    rows: list[list] = []
    cursor = int(start.timestamp() * 1000)
    end_ms = int(end.timestamp() * 1000)
    while cursor < end_ms:
        params = urllib.parse.urlencode({
            "symbol": symbol,
            "interval": "5m",
            "startTime": cursor,
            "endTime": end_ms - 1,
            "limit": 1000,
        })
        request = urllib.request.Request(
            f"{BASE_URL}?{params}", headers={"User-Agent": "PumpSignalBuyerBreath/5.4"}
        )
        for attempt in range(5):
            try:
                with urllib.request.urlopen(request, timeout=45) as response:
                    batch = json.load(response)
                break
            except Exception:
                if attempt == 4:
                    raise
                time.sleep(1.0 + attempt)
        if not batch:
            break
        rows.extend(batch)
        next_cursor = int(batch[-1][6]) + 1
        if next_cursor <= cursor:
            break
        cursor = next_cursor
        if len(batch) < 1000:
            break
    columns = [
        "open_time", "open", "high", "low", "close", "volume", "close_time",
        "quote_volume", "trades", "taker_buy_volume", "taker_buy_quote", "ignore",
    ]
    frame = pd.DataFrame(rows, columns=columns)
    numeric = [
        "open", "high", "low", "close", "volume", "quote_volume", "trades",
        "taker_buy_volume", "taker_buy_quote",
    ]
    frame[numeric] = frame[numeric].apply(pd.to_numeric, errors="coerce")
    frame["open_time"] = pd.to_numeric(frame["open_time"], errors="coerce").astype("int64")
    frame["close_time"] = pd.to_numeric(frame["close_time"], errors="coerce").astype("int64")
    frame = frame.drop_duplicates("close_time").sort_values("close_time").reset_index(drop=True)
    frame.to_pickle(target)
    return frame


def quantile(values: pd.Series | list[float], q: float) -> float:
    clean = pd.Series(values, dtype=float).replace([np.inf, -np.inf], np.nan).dropna()
    return float(clean.quantile(q)) if len(clean) else float("nan")


def describe(values: list[float]) -> dict:
    clean = pd.Series(values, dtype=float).replace([np.inf, -np.inf], np.nan).dropna()
    if clean.empty:
        return {"n": 0}
    return {
        "n": int(len(clean)),
        "p25": round(float(clean.quantile(0.25)), 4),
        "median": round(float(clean.median()), 4),
        "p75": round(float(clean.quantile(0.75)), 4),
        "mean": round(float(clean.mean()), 4),
        "positive_pct": round(float((clean > 0).mean() * 100.0), 2),
    }


def build_features(pump: pd.DataFrame, btc: pd.DataFrame) -> pd.DataFrame:
    frame = pump.copy()
    btc_ret = btc.set_index("close_time")["close"].pct_change() * 100.0
    frame["btc_ret_5m_pct"] = frame["close_time"].map(btc_ret)
    frame["ret_5m_pct"] = frame["close"].pct_change() * 100.0
    frame["ret_15m_pct"] = frame["close"].pct_change(3) * 100.0
    frame["ret_30m_pct"] = frame["close"].pct_change(6) * 100.0
    frame["quote_15m"] = frame["quote_volume"].rolling(3).sum()
    frame["buy_quote_15m"] = frame["taker_buy_quote"].rolling(3).sum()
    frame["buy_pct_15m"] = frame["buy_quote_15m"] / frame["quote_15m"] * 100.0
    frame["quote_30m"] = frame["quote_volume"].rolling(6).sum()
    frame["buy_quote_30m"] = frame["taker_buy_quote"].rolling(6).sum()
    frame["buy_pct_30m"] = frame["buy_quote_30m"] / frame["quote_30m"] * 100.0
    frame["buy_pct_5m"] = frame["taker_buy_quote"] / frame["quote_volume"] * 100.0
    baseline_5m = frame["quote_volume"].rolling(2_016, min_periods=288).median()
    frame["volume_ratio_5m"] = frame["quote_volume"] / baseline_5m
    baseline = frame["quote_15m"].rolling(2_016, min_periods=288).median()
    frame["volume_ratio_15m"] = frame["quote_15m"] / baseline
    frame["flow_pressure"] = (
        (frame["buy_pct_15m"] - 50.0) / 10.0 * np.log1p(frame["volume_ratio_15m"].clip(lower=0.0))
    )
    for bars, label in ((3, "15m"), (6, "30m"), (12, "60m"), (24, "120m")):
        frame[f"forward_{label}_pct"] = frame["close"].shift(-bars) / frame["close"] * 100.0 - 100.0
    return frame


@dataclass
class Episode:
    start: str
    end: str
    duration_minutes: int
    price_peak_minutes: int
    flow_peak_minutes: int
    max_return_pct: float
    end_return_pct: float
    shock: bool


def episodes(frame: pd.DataFrame) -> list[Episode]:
    pump_shock = quantile(frame["ret_5m_pct"].abs(), 0.995)
    btc_shock = quantile(frame["btc_ret_5m_pct"].abs(), 0.995)
    prior_quiet = frame["buy_pct_5m"].shift(1).rolling(6).min() <= 50.0
    ignition = (
        (frame["buy_pct_5m"] >= 55.0)
        & (frame["volume_ratio_5m"] >= 1.20)
        & (frame["ret_5m_pct"] >= 0.04)
        & prior_quiet
    )
    results: list[Episode] = []
    blocked_until = -1
    for start in np.flatnonzero(ignition.fillna(False).to_numpy()):
        if start <= blocked_until:
            continue
        end = min(start + 72, len(frame) - 1)
        weak_count = 0
        for cursor in range(start + 1, end + 1):
            row = frame.iloc[cursor]
            weak = row.buy_pct_15m < 49.5 and row.ret_15m_pct <= 0.0
            weak_count = weak_count + 1 if weak else 0
            if weak_count >= 2:
                end = cursor
                break
        window = frame.iloc[start : end + 1]
        entry = float(frame.iloc[start].close)
        price_peak_idx = int(window["close"].to_numpy().argmax())
        flow_peak_idx = int(window["flow_pressure"].fillna(-999).to_numpy().argmax())
        shock = bool(
            (window["ret_5m_pct"].abs() >= pump_shock).any()
            or (window["btc_ret_5m_pct"].abs() >= btc_shock).any()
        )
        results.append(Episode(
            start=pd.to_datetime(int(frame.iloc[start].close_time), unit="ms", utc=True).isoformat(),
            end=pd.to_datetime(int(frame.iloc[end].close_time), unit="ms", utc=True).isoformat(),
            duration_minutes=int((end - start) * 5),
            price_peak_minutes=int(price_peak_idx * 5),
            flow_peak_minutes=int(flow_peak_idx * 5),
            max_return_pct=round(float(window["close"].max() / entry * 100.0 - 100.0), 4),
            end_return_pct=round(float(frame.iloc[end].close / entry * 100.0 - 100.0), 4),
            shock=shock,
        ))
        blocked_until = end + 12
    return results


def condition_stats(frame: pd.DataFrame, mask: pd.Series) -> dict:
    # One observation per 30 minutes prevents a long burst from dominating the sample.
    selected = frame.loc[mask.fillna(False)].iloc[::6]
    return {
        "observations": int(len(selected)),
        "forward_15m": describe(selected["forward_15m_pct"].tolist()),
        "forward_30m": describe(selected["forward_30m_pct"].tolist()),
        "forward_60m": describe(selected["forward_60m_pct"].tolist()),
    }


def analyze(frame: pd.DataFrame, start: pd.Timestamp, end: pd.Timestamp) -> dict:
    all_episodes = episodes(frame)
    calm = [item for item in all_episodes if not item.shock]
    shocks = [item for item in all_episodes if item.shock]
    high_buy = frame["buy_pct_15m"] >= 60.0
    supported = high_buy & (frame["ret_15m_pct"] >= 0.10) & (frame["volume_ratio_15m"] >= 1.0)
    absorption = high_buy & (frame["ret_15m_pct"] <= 0.0) & (frame["volume_ratio_15m"] >= 1.0)
    prior_buy_peak = frame["buy_pct_15m"].shift(1).rolling(6).max()
    recent_price_peak = frame["close"].shift(1).rolling(12).max()
    exhaustion = (
        (prior_buy_peak - frame["buy_pct_15m"] >= 7.0)
        & (frame["buy_pct_15m"] <= 54.0)
        & (frame["ret_15m_pct"] <= 0.0)
        & (frame["close"] <= recent_price_peak * 0.997)
    )
    seller_takeover = (
        (frame["buy_pct_15m"] <= 47.0)
        & (frame["ret_15m_pct"] < 0.0)
        & (frame["close"] <= recent_price_peak * 0.997)
    )
    onset = (
        (frame["buy_pct_5m"] >= 55.0)
        & (frame["volume_ratio_5m"] >= 1.20)
        & (frame["ret_5m_pct"] >= 0.04)
        & (frame["buy_pct_5m"].shift(1).rolling(6).min() <= 50.0)
    )
    onset_indices: list[int] = []
    blocked_until = -1
    for index in np.flatnonzero(onset.fillna(False).to_numpy()):
        if index <= blocked_until:
            continue
        onset_indices.append(int(index))
        blocked_until = int(index) + 12
    onset_max: list[float] = []
    onset_peak: list[float] = []
    onset_final: list[float] = []
    onset_by_month: dict[str, list[tuple[float, float, float]]] = {}
    for index in onset_indices:
        window = frame.iloc[index : min(index + 13, len(frame))]
        entry = float(frame.iloc[index].close)
        maximum = float(window["close"].max() / entry * 100.0 - 100.0)
        peak_minutes = float(int(window["close"].to_numpy().argmax()) * 5)
        final = float(window.iloc[-1].close / entry * 100.0 - 100.0)
        onset_max.append(maximum)
        onset_peak.append(peak_minutes)
        onset_final.append(final)
        month = pd.to_datetime(int(frame.iloc[index].close_time), unit="ms", utc=True).strftime("%Y-%m")
        onset_by_month.setdefault(month, []).append((maximum, peak_minutes, final))
    return {
        "window_utc": {"start": start.isoformat(), "end": end.isoformat()},
        "bars": int(len(frame)),
        "method": {
            "interval": "5m closed bars",
            "aggressive_buy": "taker_buy_quote / quote_volume",
            "ignition": "5m buy>=55%, 5m volume>=1.20x rolling 7d median, 5m return>=0.04%, prior 30m included buy<=50%",
            "calm_cycle_end": "two consecutive bars with 15m buy<49.5% and non-positive 15m return; maximum 6h",
            "shock_exclusion": "absolute PUMP or BTC 5m return in top 0.5%",
        },
        "thresholds": {
            "pump_abs_5m_shock_pct": round(quantile(frame["ret_5m_pct"].abs(), 0.995), 4),
            "btc_abs_5m_shock_pct": round(quantile(frame["btc_ret_5m_pct"].abs(), 0.995), 4),
        },
        "episodes": {
            "all": len(all_episodes),
            "calm": len(calm),
            "shock_affected": len(shocks),
            "calm_duration_minutes": describe([x.duration_minutes for x in calm]),
            "calm_price_peak_minutes": describe([x.price_peak_minutes for x in calm]),
            "calm_flow_peak_minutes": describe([x.flow_peak_minutes for x in calm]),
            "calm_max_return_pct": describe([x.max_return_pct for x in calm]),
            "calm_end_return_pct": describe([x.end_return_pct for x in calm]),
        },
        "onset_follow_through_60m": {
            "events": len(onset_indices),
            "additional_max_return_pct": describe(onset_max),
            "minutes_to_price_peak": describe(onset_peak),
            "return_at_60m_pct": describe(onset_final),
            "by_month": {
                month: {
                    "events": len(rows),
                    "additional_max_return_pct": describe([row[0] for row in rows]),
                    "minutes_to_price_peak": describe([row[1] for row in rows]),
                    "return_at_60m_pct": describe([row[2] for row in rows]),
                }
                for month, rows in onset_by_month.items()
            },
        },
        "conditions": {
            "high_buy_with_price_response": condition_stats(frame, supported),
            "high_buy_absorbed": condition_stats(frame, absorption),
            "flow_exhaustion": condition_stats(frame, exhaustion),
            "confirmed_seller_takeover": condition_stats(frame, seller_takeover),
        },
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--start", default="2026-05-18T00:00:00Z")
    parser.add_argument("--end", default="2026-08-18T00:00:00Z")
    parser.add_argument("--cache", type=Path, default=Path("/tmp/pump-buyer-breath-cache"))
    parser.add_argument("--output", type=Path, default=Path("research/output/buyer_breath_cycle_results.json"))
    args = parser.parse_args()
    start = pd.Timestamp(args.start)
    end = pd.Timestamp(args.end)
    pump = fetch("PUMPUSDT", start, end, args.cache)
    btc = fetch("BTCUSDT", start, end, args.cache)
    result = analyze(build_features(pump, btc), start, end)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({k: result[k] for k in ("window_utc", "bars", "thresholds", "episodes", "conditions")}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
