from __future__ import annotations

from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from math import prod

from .domain import Candle
from .validation import require_replay_quality


@dataclass(frozen=True, slots=True)
class EconomyConfig:
    fee_rate: float = 0.0021
    limit_discount: float = 0.001
    limit_ttl_minutes: int = 2
    target_net: float = 0.025
    stop_net: float = -0.012
    max_hold_minutes: int = 120
    adverse_slippage: float = 0.0008
    max_entries_per_utc_day: int = 2
    min_hours_between_entries: int = 0
    drawdown_gate: float = -0.04
    spread_rate: float = 0.0
    entry_latency_minutes: int = 0
    # Context filters are intentionally inert in the protected X replay.  The
    # variant factory applies them from separately downloaded, time-aligned
    # BTC/SOL candles before a PUMP entry is considered.
    btc_context_rule: str = "ANY"
    sol_context_rule: str = "ANY"
    vwap_deviation_gate: float = -0.004
    min_buy_share: float = 0.50
    min_buy_share_delta: float = 0.0
    min_green_candle_return: float = 0.0
    min_volume_ratio_20m: float = 0.0
    # V7 adaptive-only dimensions. Defaults preserve the protected X mechanics;
    # replay_economy deliberately ignores them.
    vwap_window_minutes: int = 60
    drawdown_window_minutes: int = 720
    volume_window_minutes: int = 20
    pump_return_window_minutes: int = 1
    min_pump_return: float = -1.0
    max_pump_return: float = 1.0
    rsi_period_minutes: int = 14
    min_rsi: float = 0.0
    max_rsi: float = 100.0
    atr_period_minutes: int = 14
    min_atr_ratio: float = 0.0
    max_atr_ratio: float = 1.0
    btc_return_window_minutes: int = 60
    min_btc_return: float = -1.0
    max_btc_return: float = 1.0
    sol_return_window_minutes: int = 60
    min_sol_return: float = -1.0
    max_sol_return: float = 1.0
    utc_start_hour: int = 0
    utc_end_hour: int = 24
    trailing_stop_fraction: float = 0.0
    breakeven_trigger_net: float = 0.0
    # V8 execution grammar. These values model only order mechanics available
    # through Bitpanda Fusion's published API; they never place real orders.
    entry_order_type: str = "LIMIT"
    stop_exit_order_type: str = "STOP_MARKET"
    entry_trigger_offset: float = 0.002
    stop_limit_buffer: float = 0.003
    client_cancel_latency_minutes: int = 1
    exit_distance_mode: str = "PERCENT"
    target_atr_multiple: float = 3.0
    stop_atr_multiple: float = 2.0
    structure_window_minutes: int = 720
    min_rebound_from_low: float = 0.0
    max_rebound_from_low: float = 1.0
    buy_flow_window_minutes: int = 15
    min_buy_flow_mean: float = 0.0
    min_buy_flow_slope: float = -1.0
    min_relative_btc_return: float = -1.0
    max_relative_btc_return: float = 1.0
    min_relative_sol_return: float = -1.0
    max_relative_sol_return: float = 1.0


@dataclass(frozen=True, slots=True)
class Signal:
    index: int
    time_ms: int
    close: float
    vwap: float
    deviation: float
    drawdown_12h: float
    buy_share: float
    buy_share_delta: float
    green_candle_return: float
    volume_ratio_20m: float


@dataclass(frozen=True, slots=True)
class Trade:
    signal_time_ms: int
    fill_time_ms: int
    exit_time_ms: int
    entry: float
    exit: float
    reason: str
    gross_return: float
    net_return: float
    buy_fee_rate: float
    sell_fee_rate: float
    slippage_rate: float

    def to_dict(self) -> dict:
        return asdict(self)


@dataclass(slots=True)
class ReplayResult:
    strategy: str
    config: EconomyConfig
    signals: int
    fills: int
    trades: list[Trade]
    start_ms: int
    end_ms: int

    @property
    def wins(self) -> int:
        return sum(trade.net_return > 0 for trade in self.trades)

    @property
    def win_rate(self) -> float:
        return self.wins / len(self.trades) if self.trades else 0.0

    @property
    def average_net(self) -> float:
        return sum(item.net_return for item in self.trades) / len(self.trades) if self.trades else 0.0

    @property
    def profit_factor(self) -> float | None:
        gains = sum(max(item.net_return, 0.0) for item in self.trades)
        losses = -sum(min(item.net_return, 0.0) for item in self.trades)
        return gains / losses if losses else None

    @property
    def compound_net(self) -> float:
        return prod(1.0 + item.net_return for item in self.trades) - 1.0 if self.trades else 0.0

    @property
    def trades_per_day(self) -> float:
        days = max((self.end_ms - self.start_ms + 1) / 86_400_000, 1 / 1440)
        return len(self.trades) / days

    @property
    def exits(self) -> dict[str, int]:
        values: dict[str, int] = {}
        for trade in self.trades:
            values[trade.reason] = values.get(trade.reason, 0) + 1
        return values

    def to_dict(self) -> dict:
        return {
            "strategy": self.strategy,
            "config": asdict(self.config),
            "period": {"start_ms": self.start_ms, "end_ms": self.end_ms},
            "metrics": {
                "signals": self.signals,
                "fills": self.fills,
                "wins": self.wins,
                "win_rate": self.win_rate,
                "average_net": self.average_net,
                "profit_factor": self.profit_factor,
                "compound_net": self.compound_net,
                "trades_per_day": self.trades_per_day,
                "exits": self.exits,
            },
            "trades": [item.to_dict() for item in self.trades],
        }


@dataclass(frozen=True, slots=True)
class CanaryResult:
    signals: int
    fills: int
    wins: int

    @property
    def win_rate(self) -> float:
        return self.wins / self.fills if self.fills else 0.0

    def to_dict(self) -> dict:
        return asdict(self) | {"win_rate": self.win_rate}


def net_return(entry: float, exit_price: float, fee_rate: float) -> float:
    return exit_price * (1.0 - fee_rate) / (entry * (1.0 + fee_rate)) - 1.0


def price_for_net(entry: float, desired_net: float, fee_rate: float) -> float:
    return entry * (1.0 + fee_rate) * (1.0 + desired_net) / (1.0 - fee_rate)


def _signal_values(candles: list[Candle], index: int) -> Signal | None:
    if index < 719:
        return None
    signal = candles[index]
    previous = candles[index - 1]
    vwap_rows = candles[index - 59 : index + 1]
    quote = sum(row.quote_volume for row in vwap_rows)
    if quote <= 0:
        return None
    vwap = sum(((row.high + row.low + row.close) / 3.0) * row.quote_volume for row in vwap_rows) / quote
    deviation = signal.close / vwap - 1.0
    peak = max(row.high for row in candles[index - 719 : index + 1])
    drawdown = signal.close / peak - 1.0
    if not (
        deviation <= -0.004
        and signal.close > signal.open
        and signal.buy_share >= 0.50
        and signal.buy_share > previous.buy_share
    ):
        return None
    volume_rows = candles[index - 19 : index]
    average_volume = sum(row.quote_volume for row in volume_rows) / len(volume_rows)
    volume_ratio = signal.quote_volume / average_volume if average_volume > 0 else 0.0
    return Signal(
        index, signal.open_time_ms, signal.close, vwap, deviation, drawdown, signal.buy_share,
        signal.buy_share - previous.buy_share, signal.close / signal.open - 1.0, volume_ratio,
    )


def core_signal_at(candles: list[Candle], index: int) -> Signal | None:
    return _signal_values(candles, index)


def signal_at(candles: list[Candle], index: int, drawdown_gate: float = -0.04) -> Signal | None:
    signal = _signal_values(candles, index)
    return signal if signal is not None and signal.drawdown_12h <= drawdown_gate else None


def _bounds(rows: list[Candle], evaluation_start_ms: int | None, evaluation_end_ms: int | None) -> tuple[int, int]:
    start_index = 0
    if evaluation_start_ms is not None:
        while start_index < len(rows) and rows[start_index].open_time_ms < evaluation_start_ms:
            start_index += 1
    end_index = len(rows)
    if evaluation_end_ms is not None:
        end_index = start_index
        while end_index < len(rows) and rows[end_index].open_time_ms < evaluation_end_ms:
            end_index += 1
    return start_index, end_index


def replay_economy(
    candles: list[Candle],
    config: EconomyConfig = EconomyConfig(),
    evaluation_start_ms: int | None = None,
    evaluation_end_ms: int | None = None,
) -> ReplayResult:
    require_replay_quality(candles)
    rows = sorted(candles, key=lambda item: item.open_time_ms)
    if len(rows) < 722:
        raise ValueError("AUTO X ECONOMY replay requires at least 722 contiguous 1m candles")
    start_index, end_index = _bounds(rows, evaluation_start_ms, evaluation_end_ms)
    if start_index < 719:
        start_index = 719
    if start_index >= end_index:
        raise ValueError("Evaluation window contains no candles")
    last_signal_exclusive = end_index - config.max_hold_minutes - 5
    if last_signal_exclusive <= start_index:
        raise ValueError("Evaluation window is too short after warm-up and exit buffer")
    trades: list[Trade] = []
    signal_count = 0
    fill_count = 0
    entries_by_day: dict[str, int] = {}
    last_entry_time_ms: int | None = None
    index = start_index
    while index < last_signal_exclusive:
        signal = signal_at(rows, index, config.drawdown_gate)
        if signal is None:
            index += 1
            continue
        signal_count += 1
        limit = signal.close * (1.0 - config.limit_discount)
        fill_index: int | None = None
        entry = 0.0
        first_fill_index = index + 1 + config.entry_latency_minutes
        for candidate_index in range(first_fill_index, min(first_fill_index + config.limit_ttl_minutes, len(rows))):
            candidate = rows[candidate_index]
            if candidate.low <= limit:
                fill_index = candidate_index
                entry = min(limit, candidate.open) * (1.0 + config.spread_rate / 2.0)
                break
        if fill_index is None:
            index += config.entry_latency_minutes + config.limit_ttl_minutes + 1
            continue
        day = datetime.fromtimestamp(rows[fill_index].open_time_ms / 1000, tz=timezone.utc).strftime("%Y%m%d")
        if last_entry_time_ms is not None and rows[fill_index].open_time_ms - last_entry_time_ms < config.min_hours_between_entries * 3_600_000:
            index += 1
            continue
        if entries_by_day.get(day, 0) >= config.max_entries_per_utc_day:
            index += 1
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
            candidate = rows[candidate_index]
            if candidate.low <= stop:
                exit_index = candidate_index
                exit_price = min(stop, candidate.open) * (1.0 - config.adverse_slippage) * (1.0 - config.spread_rate / 2.0)
                reason = "STOP"
                break
            if candidate.high >= target:
                exit_index = candidate_index
                exit_price = target * (1.0 - config.spread_rate / 2.0)
                reason = "TP"
                slippage = 0.0
                break
        gross = exit_price / entry - 1.0
        trades.append(
            Trade(
                signal_time_ms=signal.time_ms,
                fill_time_ms=rows[fill_index].open_time_ms,
                exit_time_ms=rows[exit_index].open_time_ms,
                entry=entry,
                exit=exit_price,
                reason=reason,
                gross_return=gross,
                net_return=net_return(entry, exit_price, config.fee_rate),
                buy_fee_rate=config.fee_rate,
                sell_fee_rate=config.fee_rate,
                slippage_rate=slippage,
            )
        )
        index = exit_index + 1
    return ReplayResult(
        "AUTO_X_ECONOMY",
        config,
        signal_count,
        fill_count,
        trades,
        rows[start_index].open_time_ms,
        rows[end_index - 1].close_time_ms,
    )


def replay_vwap_canary(
    candles: list[Candle], evaluation_start_ms: int | None = None, evaluation_end_ms: int | None = None
) -> CanaryResult:
    require_replay_quality(candles)
    rows = sorted(candles, key=lambda item: item.open_time_ms)
    start_index, end_index = _bounds(rows, evaluation_start_ms, evaluation_end_ms)
    start_index = max(start_index, 1500)
    config = EconomyConfig(stop_net=-0.008, max_hold_minutes=90, max_entries_per_utc_day=10**9)
    busy = -1
    signals = fills = wins = 0
    for index in range(start_index, end_index - 365):
        if index <= busy:
            continue
        signal = core_signal_at(rows, index)
        if signal is None:
            continue
        signals += 1
        limit = signal.close * (1.0 - config.limit_discount)
        fill_index = None
        entry = 0.0
        for candidate_index in range(index + 1, index + 1 + config.limit_ttl_minutes):
            candidate = rows[candidate_index]
            if candidate.low <= limit:
                fill_index = candidate_index
                entry = min(limit, candidate.open)
                break
        if fill_index is None:
            busy = index + config.limit_ttl_minutes
            continue
        fills += 1
        target = max(entry * 1.001, signal.vwap)
        stop = price_for_net(entry, config.stop_net, config.fee_rate)
        exit_index = fill_index + config.max_hold_minutes
        pnl = net_return(entry, rows[exit_index].close * (1.0 - config.adverse_slippage), config.fee_rate)
        for candidate_index in range(fill_index + 1, exit_index + 1):
            candidate = rows[candidate_index]
            if candidate.low <= stop:
                exit_index = candidate_index
                pnl = net_return(
                    entry, min(stop, candidate.open) * (1.0 - config.adverse_slippage), config.fee_rate
                )
                break
            if candidate.high >= target:
                exit_index = candidate_index
                pnl = net_return(entry, target, config.fee_rate)
                break
        if pnl > 0:
            wins += 1
        busy = exit_index
    return CanaryResult(signals, fills, wins)
