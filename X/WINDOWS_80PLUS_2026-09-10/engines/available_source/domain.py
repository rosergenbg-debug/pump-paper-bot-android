from __future__ import annotations

from dataclasses import asdict, dataclass, field
from datetime import datetime, timezone
from typing import Any


ONE_MINUTE_MS = 60_000


@dataclass(frozen=True, slots=True)
class Candle:
    source: str
    symbol: str
    interval: str
    open_time_ms: int
    close_time_ms: int
    open: float
    high: float
    low: float
    close: float
    volume: float
    quote_volume: float
    trade_count: int
    taker_buy_volume: float
    taker_buy_quote_volume: float

    @classmethod
    def from_binance(cls, symbol: str, interval: str, row: list[Any]) -> "Candle":
        if len(row) < 11:
            raise ValueError("Binance kline row has fewer than 11 fields")
        return cls(
            source="binance_spot",
            symbol=symbol.upper(),
            interval=interval,
            open_time_ms=int(row[0]),
            open=float(row[1]),
            high=float(row[2]),
            low=float(row[3]),
            close=float(row[4]),
            volume=float(row[5]),
            close_time_ms=int(row[6]),
            quote_volume=float(row[7]),
            trade_count=int(row[8]),
            taker_buy_volume=float(row[9]),
            taker_buy_quote_volume=float(row[10]),
        )

    @classmethod
    def from_dict(cls, value: dict[str, Any]) -> "Candle":
        return cls(
            source=str(value["source"]),
            symbol=str(value["symbol"]).upper(),
            interval=str(value["interval"]),
            open_time_ms=int(value["open_time_ms"]),
            close_time_ms=int(value["close_time_ms"]),
            open=float(value["open"]),
            high=float(value["high"]),
            low=float(value["low"]),
            close=float(value["close"]),
            volume=float(value["volume"]),
            quote_volume=float(value["quote_volume"]),
            trade_count=int(value["trade_count"]),
            taker_buy_volume=float(value["taker_buy_volume"]),
            taker_buy_quote_volume=float(value["taker_buy_quote_volume"]),
        )

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)

    @property
    def buy_share(self) -> float:
        if self.quote_volume > 0:
            return self.taker_buy_quote_volume / self.quote_volume
        if self.volume > 0:
            return self.taker_buy_volume / self.volume
        return 0.0


@dataclass(slots=True)
class ValidationReport:
    row_count: int
    start_ms: int | None
    end_ms: int | None
    gaps: list[dict[str, int]] = field(default_factory=list)
    duplicates: list[int] = field(default_factory=list)
    invalid_rows: list[dict[str, Any]] = field(default_factory=list)

    @property
    def valid(self) -> bool:
        return not self.gaps and not self.duplicates and not self.invalid_rows

    def to_dict(self) -> dict[str, Any]:
        return asdict(self) | {"valid": self.valid}


@dataclass(slots=True)
class DatasetManifest:
    dataset_id: str
    source: str
    symbol: str
    data_type: str
    interval: str
    start_utc: str
    end_utc: str
    schema_version: str
    row_count: int
    gaps: list[dict[str, int]]
    duplicates: list[int]
    checksum_sha256: str
    downloaded_at_utc: str
    origin_url: str

    @classmethod
    def create(
        cls,
        dataset_id: str,
        symbol: str,
        interval: str,
        report: ValidationReport,
        checksum: str,
        origin_url: str,
    ) -> "DatasetManifest":
        def iso(ms: int | None) -> str:
            if ms is None:
                return ""
            return datetime.fromtimestamp(ms / 1000, tz=timezone.utc).isoformat()

        return cls(
            dataset_id=dataset_id,
            source="binance_spot",
            symbol=symbol.upper(),
            data_type="candles",
            interval=interval,
            start_utc=iso(report.start_ms),
            end_utc=iso(report.end_ms),
            schema_version="candle-v1",
            row_count=report.row_count,
            gaps=report.gaps,
            duplicates=report.duplicates,
            checksum_sha256=checksum,
            downloaded_at_utc=datetime.now(timezone.utc).isoformat(),
            origin_url=origin_url,
        )

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)
