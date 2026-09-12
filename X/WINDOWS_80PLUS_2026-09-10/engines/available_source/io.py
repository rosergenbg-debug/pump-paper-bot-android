from __future__ import annotations

import csv
import json
from collections.abc import Iterable
from pathlib import Path

from .domain import Candle


def read_jsonl(path: Path) -> list[Candle]:
    rows: list[Candle] = []
    with path.open("r", encoding="utf-8") as handle:
        for line_number, line in enumerate(handle, 1):
            if line.strip():
                try:
                    rows.append(Candle.from_dict(json.loads(line)))
                except Exception as exc:
                    raise ValueError(f"Invalid JSONL at line {line_number}: {exc}") from exc
    return rows


def write_jsonl(path: Path, candles: Iterable[Candle]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    partial = path.with_suffix(path.suffix + ".partial")
    with partial.open("w", encoding="utf-8", newline="\n") as handle:
        for candle in candles:
            handle.write(json.dumps(candle.to_dict(), ensure_ascii=False, separators=(",", ":")) + "\n")
        handle.flush()
    partial.replace(path)


def read_csv(path: Path, source: str = "import", symbol: str = "PUMPUSDT", interval: str = "1m") -> list[Candle]:
    aliases = {
        "open_time": "open_time_ms",
        "close_time": "close_time_ms",
        "quoteVolume": "quote_volume",
        "trades": "trade_count",
        "takerBuyVolume": "taker_buy_volume",
        "takerBuyQuoteVolume": "taker_buy_quote_volume",
    }
    rows: list[Candle] = []
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        for raw in csv.DictReader(handle):
            normalized = {aliases.get(key, key): value for key, value in raw.items()}
            rows.append(
                Candle(
                    source=normalized.get("source") or source,
                    symbol=(normalized.get("symbol") or symbol).upper(),
                    interval=normalized.get("interval") or interval,
                    open_time_ms=int(normalized["open_time_ms"]),
                    close_time_ms=int(normalized["close_time_ms"]),
                    open=float(normalized["open"]),
                    high=float(normalized["high"]),
                    low=float(normalized["low"]),
                    close=float(normalized["close"]),
                    volume=float(normalized.get("volume") or 0),
                    quote_volume=float(normalized.get("quote_volume") or 0),
                    trade_count=int(normalized.get("trade_count") or 0),
                    taker_buy_volume=float(normalized.get("taker_buy_volume") or 0),
                    taker_buy_quote_volume=float(normalized.get("taker_buy_quote_volume") or 0),
                )
            )
    return rows

