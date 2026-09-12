from __future__ import annotations

from .domain import Candle, ONE_MINUTE_MS


def demo_candles(count: int = 900, start_ms: int = 1_699_999_980_000) -> list[Candle]:
    rows: list[Candle] = []
    for index in range(count):
        base = 100.0 if index < 660 else 95.0
        open_price = base
        close = base
        high = base * 1.001
        low = base * 0.999
        buy_share = 0.48
        if index == 719:
            open_price = 94.40
            close = 94.50
            high = 94.55
            low = 94.30
            buy_share = 0.55
        elif index == 718:
            buy_share = 0.45
        elif index == 720:
            open_price = 94.40
            close = 94.60
            high = 94.70
            low = 94.20
            buy_share = 0.56
        elif 721 <= index <= 730:
            open_price = 94.60 + (index - 721) * 0.30
            close = open_price + 0.25
            high = close + 0.20
            low = open_price - 0.10
            buy_share = 0.57
        quote_volume = 100_000.0
        open_time = start_ms + index * ONE_MINUTE_MS
        rows.append(
            Candle(
                source="synthetic",
                symbol="PUMPUSDT",
                interval="1m",
                open_time_ms=open_time,
                close_time_ms=open_time + ONE_MINUTE_MS - 1,
                open=open_price,
                high=high,
                low=low,
                close=close,
                volume=1_000.0,
                quote_volume=quote_volume,
                trade_count=100,
                taker_buy_volume=1_000.0 * buy_share,
                taker_buy_quote_volume=quote_volume * buy_share,
            )
        )
    return rows
