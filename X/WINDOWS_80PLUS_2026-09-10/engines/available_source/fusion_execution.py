from __future__ import annotations

from dataclasses import asdict

from .baseline_x import EconomyConfig


FUSION_ENTRY_ORDER_TYPES = ("MARKET", "LIMIT", "STOP_MARKET", "STOP_LIMIT")
FUSION_STOP_ORDER_TYPES = ("STOP_MARKET", "STOP_LIMIT")
FUSION_TIME_IN_FORCE_EXAMPLES = ("GTC",)


def validate_fusion_compatible(config: EconomyConfig) -> list[str]:
    """Return incompatibilities with the public Fusion order contract.

    Pair-specific amount, size and precision constraints are deliberately not
    guessed. A future paper/live connector must fetch them from ``/v1/pairs``.
    """
    reasons: list[str] = []
    if config.entry_order_type not in FUSION_ENTRY_ORDER_TYPES:
        reasons.append("fusion:unsupported_entry_order_type")
    if config.stop_exit_order_type not in FUSION_STOP_ORDER_TYPES:
        reasons.append("fusion:unsupported_stop_order_type")
    if config.entry_order_type in {"STOP_MARKET", "STOP_LIMIT"} and config.entry_trigger_offset <= 0:
        reasons.append("fusion:buy_stop_trigger_must_be_above_market")
    if config.stop_exit_order_type == "STOP_LIMIT" and config.stop_limit_buffer <= 0:
        reasons.append("fusion:stop_limit_requires_execution_buffer")
    if config.client_cancel_latency_minutes < 0:
        reasons.append("fusion:negative_cancel_latency")
    return reasons


def fusion_execution_contract(config: EconomyConfig | None = None) -> dict:
    payload = {
        "platform": "Bitpanda Fusion API",
        "supported_order_types": list(FUSION_ENTRY_ORDER_TYPES),
        "documented_time_in_force_examples": list(FUSION_TIME_IN_FORCE_EXAMPLES),
        "native_oco_documented": False,
        "protective_pair_model": "CLIENT_LINKED_CANCEL_SIBLING",
        "pair_constraints": "FETCH_AT_RUNTIME_FROM_/v1/pairs",
        "order_submission": "ASYNC_HTTP_202_MONITOR_STATUS",
        "sizing_rule": "USE_EXACTLY_ONE_OF_BASE_QUANTITY_OR_QUOTE_AMOUNT",
        "api_rate_limits_per_minute": {"global": 1000, "market_data": 240, "create_order": 300},
        "required_live_scope": "Trade",
        "real_orders": False,
    }
    if config is not None:
        payload["candidate_execution"] = {
            key: asdict(config)[key]
            for key in (
                "entry_order_type", "stop_exit_order_type", "entry_trigger_offset",
                "stop_limit_buffer", "client_cancel_latency_minutes",
            )
        }
    return payload
