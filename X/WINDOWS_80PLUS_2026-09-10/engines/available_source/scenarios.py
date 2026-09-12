from __future__ import annotations

from dataclasses import replace

from .baseline_x import EconomyConfig, net_return, price_for_net, replay_economy
from .demo import demo_candles
from .validation import validate_candles


FAMILIES = (
    "непрерывное падение",
    "слабые отскоки",
    "капитуляция",
    "ложный разворот",
    "настоящий разворот",
    "BTC shock context",
    "spread shock",
    "slippage shock",
    "тонкая ликвидность",
    "одновременный TP/STOP",
)


def _scenario(family_index: int, severity: int):
    rows = demo_candles(count=900, start_ms=1_699_999_980_000 + (family_index * 5 + severity) * 120_000_000)
    changed = []
    for index, row in enumerate(rows):
        wave = ((index % (19 + family_index)) - (9 + family_index / 2)) * 0.00004 * severity
        trend = (family_index - 4.5) * index * 0.0000008 * severity
        factor = max(0.25, 1.0 + wave + trend)
        open_price = row.open * factor
        close_price = row.close * factor
        high = max(open_price, close_price, row.high * factor)
        low = min(open_price, close_price, row.low * factor)
        if family_index in {2, 5, 7} and index == 760:
            low *= 1.0 - 0.01 * severity
        if family_index == 9 and index == 800:
            high *= 1.0 + 0.02 * severity
            low *= 1.0 - 0.02 * severity
        changed.append(replace(row, open=open_price, high=high, low=low, close=close_price))
    return changed


def run_synthetic_school() -> dict:
    results = []
    for family_index, family in enumerate(FAMILIES):
        for severity in range(1, 6):
            rows = _scenario(family_index, severity)
            quality = validate_candles(rows)
            first = replay_economy(rows).to_dict()
            second = replay_economy(rows).to_dict()
            deterministic = first == second
            results.append(
                {
                    "id": f"S{family_index * 5 + severity:02d}",
                    "family": family,
                    "severity": severity,
                    "status": "PASS" if quality.valid and deterministic else "FAIL",
                    "deterministic": deterministic,
                    "rows": quality.row_count,
                    "fills": first["metrics"]["fills"],
                }
            )
    fee_exact = abs(net_return(1.0, price_for_net(1.0, 0.025, 0.0021), 0.0021) - 0.025) < 1e-12
    no_free_round_trip = net_return(1.0, 1.0, 0.0021) < 0
    passed = sum(item["status"] == "PASS" for item in results)
    return {
        "status": "PASS" if passed == 50 and fee_exact and no_free_round_trip else "FAIL",
        "passed": passed,
        "total": 50,
        "properties": {
            "deterministic_replay": all(item["deterministic"] for item in results),
            "fee_aware_target_exact": fee_exact,
            "round_trip_cost_is_negative": no_free_round_trip,
            "real_orders": False,
        },
        "scenarios": results,
        "meaning": "Проверяет механику и воспроизводимость, но не доказывает прибыльность.",
    }
