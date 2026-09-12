from __future__ import annotations

from collections.abc import Sequence

from .domain import Candle, ONE_MINUTE_MS, ValidationReport


INTERVAL_MS = {"1m": ONE_MINUTE_MS}


def validate_candles(
    candles: Sequence[Candle],
    interval: str = "1m",
    expected_start_ms: int | None = None,
    expected_end_open_ms: int | None = None,
) -> ValidationReport:
    step = INTERVAL_MS.get(interval)
    if step is None:
        raise ValueError(f"Unsupported interval: {interval}")
    rows = sorted(candles, key=lambda item: item.open_time_ms)
    report = ValidationReport(
        row_count=len(rows),
        start_ms=rows[0].open_time_ms if rows else None,
        end_ms=rows[-1].close_time_ms if rows else None,
    )
    seen: set[int] = set()
    previous: Candle | None = None
    expected_identity: tuple[str, str, str] | None = None
    for index, row in enumerate(rows):
        if row.open_time_ms in seen:
            report.duplicates.append(row.open_time_ms)
        seen.add(row.open_time_ms)
        errors: list[str] = []
        identity = (row.source, row.symbol, row.interval)
        if expected_identity is None:
            expected_identity = identity
        elif identity != expected_identity:
            errors.append("mixed source/symbol/interval")
        if row.interval != interval:
            errors.append("row interval differs from requested interval")
        if row.open_time_ms % step != 0:
            errors.append("open time is not interval-aligned")
        if row.close_time_ms != row.open_time_ms + step - 1:
            errors.append("close time does not match interval")
        if min(row.open, row.high, row.low, row.close) <= 0:
            errors.append("non-positive OHLC")
        if row.high < max(row.open, row.close, row.low):
            errors.append("high below OHLC")
        if row.low > min(row.open, row.close, row.high):
            errors.append("low above OHLC")
        if row.close_time_ms <= row.open_time_ms:
            errors.append("invalid time range")
        if min(row.volume, row.quote_volume, row.taker_buy_volume, row.taker_buy_quote_volume) < 0:
            errors.append("negative volume")
        if row.taker_buy_quote_volume > row.quote_volume + 1e-9:
            errors.append("taker buy quote exceeds quote volume")
        if errors:
            report.invalid_rows.append({"index": index, "open_time_ms": row.open_time_ms, "errors": errors})
        if previous is not None:
            delta = row.open_time_ms - previous.open_time_ms
            if delta > step:
                report.gaps.append(
                    {
                        "after_ms": previous.open_time_ms,
                        "before_ms": row.open_time_ms,
                        "missing_intervals": delta // step - 1,
                    }
                )
        previous = row
    if rows and expected_start_ms is not None and rows[0].open_time_ms > expected_start_ms:
        report.gaps.insert(
            0,
            {
                "after_ms": expected_start_ms - step,
                "before_ms": rows[0].open_time_ms,
                "missing_intervals": (rows[0].open_time_ms - expected_start_ms) // step,
            },
        )
    if rows and expected_end_open_ms is not None and rows[-1].open_time_ms < expected_end_open_ms:
        report.gaps.append(
            {
                "after_ms": rows[-1].open_time_ms,
                "before_ms": expected_end_open_ms + step,
                "missing_intervals": (expected_end_open_ms - rows[-1].open_time_ms) // step,
            }
        )
    return report


def require_replay_quality(candles: Sequence[Candle], interval: str = "1m") -> ValidationReport:
    report = validate_candles(candles, interval)
    if not report.valid:
        raise ValueError(
            "Dataset is unsafe for replay: "
            f"gaps={len(report.gaps)}, duplicates={len(report.duplicates)}, invalid={len(report.invalid_rows)}"
        )
    return report
