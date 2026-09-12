# PUMP Research Lab V15

Сохранено — остановлено

Проверено: 29; прибыльных на всех выбранных окнах: 15.

Это историческое исследование, не прогноз и не допуск к торговле.

## Фактические периоды

{
  "BEAR": {
    "start_ms": 1757980800000,
    "end_ms": 1773532799999,
    "minutes": 259200,
    "requested_days": 180,
    "excluded_minutes": 0,
    "return": -0.7692753623188406,
    "actual_direction": "BEAR",
    "regime_changed_by_holdout_exclusion": false,
    "old_test_reused_as_research": true,
    "independent_test": false
  },
  "SIDEWAYS": {
    "start_ms": 1770249600000,
    "end_ms": 1785801599999,
    "minutes": 259200,
    "requested_days": 180,
    "excluded_minutes": 0,
    "return": -0.0586419753086419,
    "actual_direction": "SIDEWAYS",
    "regime_changed_by_holdout_exclusion": false,
    "old_test_reused_as_research": true,
    "independent_test": false
  },
  "BULL": {
    "start_ms": 1771977600000,
    "end_ms": 1787529599999,
    "minutes": 259200,
    "requested_days": 180,
    "excluded_minutes": 0,
    "return": 2.0855962219598583,
    "actual_direction": "BULL",
    "regime_changed_by_holdout_exclusion": false,
    "old_test_reused_as_research": true,
    "independent_test": false
  }
}

## Успешные кандидаты: >=5% на каждом из трёх окон

[
  {
    "id": "V15-fff93f2b60c3f843",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.15,
      "max_hold_minutes": 10080,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 5,
      "min_hours_between_entries": 18,
      "drawdown_gate": -0.002,
      "spread_rate": 0.0,
      "entry_latency_minutes": 0,
      "btc_context_rule": "ANY",
      "sol_context_rule": "ANY",
      "vwap_deviation_gate": -0.004,
      "min_buy_share": 0.0,
      "min_buy_share_delta": 0.0,
      "min_green_candle_return": -0.04,
      "min_volume_ratio_20m": 0.5,
      "vwap_window_minutes": 10080,
      "drawdown_window_minutes": 1440,
      "volume_window_minutes": 240,
      "pump_return_window_minutes": 720,
      "min_pump_return": -0.25,
      "max_pump_return": 0.06,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 15,
      "min_btc_return": -0.1,
      "max_btc_return": 0.15,
      "sol_return_window_minutes": 1,
      "min_sol_return": -1.0,
      "max_sol_return": 0.15,
      "utc_start_hour": 0,
      "utc_end_hour": 18,
      "trailing_stop_fraction": 0.25,
      "breakeven_trigger_net": 0.03,
      "entry_order_type": "LIMIT",
      "stop_exit_order_type": "STOP_MARKET",
      "entry_trigger_offset": 0.002,
      "stop_limit_buffer": 0.003,
      "client_cancel_latency_minutes": 1,
      "exit_distance_mode": "PERCENT",
      "target_atr_multiple": 3.0,
      "stop_atr_multiple": 2.0,
      "structure_window_minutes": 720,
      "min_rebound_from_low": 0.0,
      "max_rebound_from_low": 0.3,
      "buy_flow_window_minutes": 120,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 0.25,
      "min_relative_sol_return": -0.1,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 1440,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 10.0,
      "min_average_trade_size_ratio": 0.0,
      "max_average_trade_size_ratio": 5.0,
      "min_cvd_ratio": -0.5,
      "max_cvd_ratio": 0.75,
      "min_realized_volatility": 0.015,
      "max_realized_volatility": 0.2,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 0.85,
      "min_illiquidity_ratio": 0.0,
      "max_illiquidity_ratio": 10.0,
      "macro_shock_rule": "OFF",
      "macro_shock_window_minutes": 5,
      "btc_shock_threshold": -1.0,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1079,
          "fills": 42,
          "wins": 25,
          "win_rate": 0.5952380952380952,
          "average_net": 0.007026071309109895,
          "profit_factor": 1.3090307343998506,
          "compound_net": 0.2069136012368602,
          "trades_per_day": 0.23333333333333334,
          "exits": {
            "TP": 25,
            "STOP_MARKET": 6,
            "TRAIL": 10,
            "TIME": 1
          },
          "liquidity_rejections": 43,
          "max_drawdown": 0.32389765874943865,
          "closed_trade_drawdown": 0.25650834171652226,
          "cvar_5": -0.15068000000000015
        },
        "folds": {
          "train": {
            "signals": 20,
            "fills": 20,
            "wins": 14,
            "win_rate": 0.7,
            "average_net": 0.019772000000000033,
            "profit_factor": 2.298397688468613,
            "compound_net": 0.4236478959741443,
            "trades_per_day": 0.3333333333333333,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1793013770158084,
            "closed_trade_drawdown": 0.151359456,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 13,
            "fills": 13,
            "wins": 7,
            "win_rate": 0.5384615384615384,
            "average_net": -0.011266538847491146,
            "profit_factor": 0.7049842314419402,
            "compound_net": -0.1761742884346411,
            "trades_per_day": 0.21666666666666667,
            "exits": {
              "STOP_MARKET": 3,
              "TP": 7,
              "TRAIL": 2,
              "TIME": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.32389765874943854,
            "closed_trade_drawdown": 0.25650834171652237,
            "cvar_5": -0.15068000000000015,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 9,
            "fills": 9,
            "wins": 4,
            "win_rate": 0.4444444444444444,
            "average_net": 0.005124444444444425,
            "profit_factor": 1.2997140629061588,
            "compound_net": 0.029054198316232238,
            "trades_per_day": 0.15,
            "exits": {
              "TP": 4,
              "TRAIL": 4,
              "STOP_MARKET": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1805770284191276,
            "closed_trade_drawdown": 0.15068000000000015,
            "cvar_5": -0.15068000000000015,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.2069136012368602,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1228,
          "fills": 14,
          "wins": 13,
          "win_rate": 0.9285714285714286,
          "average_net": 0.046371428571428605,
          "profit_factor": 812.4999999998647,
          "compound_net": 0.884140623009378,
          "trades_per_day": 0.07777777777777778,
          "exits": {
            "TP": 13,
            "TRAIL": 1
          },
          "liquidity_rejections": 35,
          "max_drawdown": 0.15595757953836553,
          "closed_trade_drawdown": 0.0008000000000001339,
          "cvar_5": -0.0008000000000001339
        },
        "folds": {
          "train": {
            "signals": 3,
            "fills": 3,
            "wins": 3,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.15762500000000013,
            "trades_per_day": 0.05,
            "exits": {
              "TP": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.04671457905544163,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 7,
            "fills": 7,
            "wins": 6,
            "win_rate": 0.8571428571428571,
            "average_net": 0.04274285714285716,
            "profit_factor": 374.99999999993753,
            "compound_net": 0.3390235641125001,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 6,
              "TRAIL": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.07807118254879475,
            "closed_trade_drawdown": 0.0008000000000001339,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 4,
            "fills": 4,
            "wins": 4,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.21550625000000023,
            "trades_per_day": 0.06666666666666667,
            "exits": {
              "TP": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.15595757953836553,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.884140623009378,
        "distance": 0
      },
      "BULL": {
        "metrics": {
          "signals": 1072,
          "fills": 14,
          "wins": 13,
          "win_rate": 0.9285714285714286,
          "average_net": 0.046371428571428605,
          "profit_factor": 812.4999999998647,
          "compound_net": 0.884140623009378,
          "trades_per_day": 0.07777777777777778,
          "exits": {
            "TP": 13,
            "TRAIL": 1
          },
          "liquidity_rejections": 30,
          "max_drawdown": 0.15595757953836553,
          "closed_trade_drawdown": 0.0008000000000001339,
          "cvar_5": -0.0008000000000001339
        },
        "folds": {
          "train": {
            "signals": 3,
            "fills": 3,
            "wins": 3,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.15762500000000013,
            "trades_per_day": 0.05,
            "exits": {
              "TP": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.04671457905544152,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 9,
            "fills": 9,
            "wins": 8,
            "win_rate": 0.8888888888888888,
            "average_net": 0.04435555555555558,
            "profit_factor": 499.9999999999167,
            "compound_net": 0.47627347943403175,
            "trades_per_day": 0.15,
            "exits": {
              "TP": 8,
              "TRAIL": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.15595757953836553,
            "closed_trade_drawdown": 0.0008000000000001339,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 2,
            "fills": 2,
            "wins": 2,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.10250000000000004,
            "trades_per_day": 0.03333333333333333,
            "exits": {
              "TP": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.06241519674355489,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.884140623009378,
        "distance": 0
      }
    },
    "positive": true,
    "eligible": true,
    "score": [
      1,
      0,
      0.2069136012368602
    ],
    "worst_net": 0.2069136012368602,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788796227.6636584
  },
  {
    "id": "V15-ffe2238a6760799c",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.15,
      "max_hold_minutes": 10080,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 3,
      "min_hours_between_entries": 18,
      "drawdown_gate": -0.002,
      "spread_rate": 0.0,
      "entry_latency_minutes": 0,
      "btc_context_rule": "ANY",
      "sol_context_rule": "ANY",
      "vwap_deviation_gate": -0.004,
      "min_buy_share": 0.0,
      "min_buy_share_delta": 0.0,
      "min_green_candle_return": -0.1,
      "min_volume_ratio_20m": 0.5,
      "vwap_window_minutes": 10080,
      "drawdown_window_minutes": 1440,
      "volume_window_minutes": 240,
      "pump_return_window_minutes": 720,
      "min_pump_return": -0.15,
      "max_pump_return": 0.06,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 15,
      "min_btc_return": -0.15,
      "max_btc_return": 0.15,
      "sol_return_window_minutes": 1,
      "min_sol_return": -1.0,
      "max_sol_return": 1.0,
      "utc_start_hour": 0,
      "utc_end_hour": 18,
      "trailing_stop_fraction": 0.25,
      "breakeven_trigger_net": 0.03,
      "entry_order_type": "LIMIT",
      "stop_exit_order_type": "STOP_MARKET",
      "entry_trigger_offset": 0.002,
      "stop_limit_buffer": 0.003,
      "client_cancel_latency_minutes": 1,
      "exit_distance_mode": "PERCENT",
      "target_atr_multiple": 3.0,
      "stop_atr_multiple": 2.0,
      "structure_window_minutes": 720,
      "min_rebound_from_low": 0.0,
      "max_rebound_from_low": 0.3,
      "buy_flow_window_minutes": 120,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 0.25,
      "min_relative_sol_return": -1.0,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 1440,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 10.0,
      "min_average_trade_size_ratio": 0.0,
      "max_average_trade_size_ratio": 5.0,
      "min_cvd_ratio": -0.5,
      "max_cvd_ratio": 1.0,
      "min_realized_volatility": 0.015,
      "max_realized_volatility": 0.5,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 0.85,
      "min_illiquidity_ratio": 0.0,
      "max_illiquidity_ratio": 10.0,
      "macro_shock_rule": "BTC",
      "macro_shock_window_minutes": 5,
      "btc_shock_threshold": -0.03,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1079,
          "fills": 42,
          "wins": 25,
          "win_rate": 0.5952380952380952,
          "average_net": 0.007026071309109895,
          "profit_factor": 1.3090307343998506,
          "compound_net": 0.2069136012368602,
          "trades_per_day": 0.23333333333333334,
          "exits": {
            "TP": 25,
            "STOP_MARKET": 6,
            "TRAIL": 10,
            "TIME": 1
          },
          "liquidity_rejections": 43,
          "max_drawdown": 0.32389765874943865,
          "closed_trade_drawdown": 0.25650834171652226,
          "cvar_5": -0.15068000000000015
        },
        "folds": {
          "train": {
            "signals": 20,
            "fills": 20,
            "wins": 14,
            "win_rate": 0.7,
            "average_net": 0.019772000000000033,
            "profit_factor": 2.298397688468613,
            "compound_net": 0.4236478959741443,
            "trades_per_day": 0.3333333333333333,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1793013770158084,
            "closed_trade_drawdown": 0.151359456,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 13,
            "fills": 13,
            "wins": 7,
            "win_rate": 0.5384615384615384,
            "average_net": -0.011266538847491146,
            "profit_factor": 0.7049842314419402,
            "compound_net": -0.1761742884346411,
            "trades_per_day": 0.21666666666666667,
            "exits": {
              "STOP_MARKET": 3,
              "TP": 7,
              "TRAIL": 2,
              "TIME": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.32389765874943854,
            "closed_trade_drawdown": 0.25650834171652237,
            "cvar_5": -0.15068000000000015,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 9,
            "fills": 9,
            "wins": 4,
            "win_rate": 0.4444444444444444,
            "average_net": 0.005124444444444425,
            "profit_factor": 1.2997140629061588,
            "compound_net": 0.029054198316232238,
            "trades_per_day": 0.15,
            "exits": {
              "TP": 4,
              "TRAIL": 4,
              "STOP_MARKET": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1805770284191276,
            "closed_trade_drawdown": 0.15068000000000015,
            "cvar_5": -0.15068000000000015,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.2069136012368602,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1229,
          "fills": 14,
          "wins": 13,
          "win_rate": 0.9285714285714286,
          "average_net": 0.046371428571428605,
          "profit_factor": 812.4999999998647,
          "compound_net": 0.884140623009378,
          "trades_per_day": 0.07777777777777778,
          "exits": {
            "TP": 13,
            "TRAIL": 1
          },
          "liquidity_rejections": 36,
          "max_drawdown": 0.15595757953836553,
          "closed_trade_drawdown": 0.0008000000000001339,
          "cvar_5": -0.0008000000000001339
        },
        "folds": {
          "train": {
            "signals": 3,
            "fills": 3,
            "wins": 3,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.15762500000000013,
            "trades_per_day": 0.05,
            "exits": {
              "TP": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.04671457905544163,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 7,
            "fills": 7,
            "wins": 6,
            "win_rate": 0.8571428571428571,
            "average_net": 0.04274285714285716,
            "profit_factor": 374.99999999993753,
            "compound_net": 0.3390235641125001,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 6,
              "TRAIL": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.07807118254879475,
            "closed_trade_drawdown": 0.0008000000000001339,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 4,
            "fills": 4,
            "wins": 4,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.21550625000000023,
            "trades_per_day": 0.06666666666666667,
            "exits": {
              "TP": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.15595757953836553,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.884140623009378,
        "distance": 0
      },
      "BULL": {
        "metrics": {
          "signals": 1073,
          "fills": 14,
          "wins": 13,
          "win_rate": 0.9285714285714286,
          "average_net": 0.046371428571428605,
          "profit_factor": 812.4999999998647,
          "compound_net": 0.884140623009378,
          "trades_per_day": 0.07777777777777778,
          "exits": {
            "TP": 13,
            "TRAIL": 1
          },
          "liquidity_rejections": 31,
          "max_drawdown": 0.15595757953836553,
          "closed_trade_drawdown": 0.0008000000000001339,
          "cvar_5": -0.0008000000000001339
        },
        "folds": {
          "train": {
            "signals": 3,
            "fills": 3,
            "wins": 3,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.15762500000000013,
            "trades_per_day": 0.05,
            "exits": {
              "TP": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.04671457905544152,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 9,
            "fills": 9,
            "wins": 8,
            "win_rate": 0.8888888888888888,
            "average_net": 0.04435555555555558,
            "profit_factor": 499.9999999999167,
            "compound_net": 0.47627347943403175,
            "trades_per_day": 0.15,
            "exits": {
              "TP": 8,
              "TRAIL": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.15595757953836553,
            "closed_trade_drawdown": 0.0008000000000001339,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 2,
            "fills": 2,
            "wins": 2,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.10250000000000004,
            "trades_per_day": 0.03333333333333333,
            "exits": {
              "TP": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.06241519674355489,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.884140623009378,
        "distance": 0
      }
    },
    "positive": true,
    "eligible": true,
    "score": [
      1,
      0,
      0.2069136012368602
    ],
    "worst_net": 0.2069136012368602,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788796231.51945
  },
  {
    "id": "V15-ffbc264439b92c0d",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.15,
      "max_hold_minutes": 10080,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 5,
      "min_hours_between_entries": 18,
      "drawdown_gate": -0.002,
      "spread_rate": 0.0,
      "entry_latency_minutes": 0,
      "btc_context_rule": "ANY",
      "sol_context_rule": "ANY",
      "vwap_deviation_gate": -0.004,
      "min_buy_share": 0.0,
      "min_buy_share_delta": 0.0,
      "min_green_candle_return": -0.04,
      "min_volume_ratio_20m": 0.5,
      "vwap_window_minutes": 10080,
      "drawdown_window_minutes": 1440,
      "volume_window_minutes": 240,
      "pump_return_window_minutes": 720,
      "min_pump_return": -0.1,
      "max_pump_return": 0.06,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 15,
      "min_btc_return": -0.1,
      "max_btc_return": 0.15,
      "sol_return_window_minutes": 1,
      "min_sol_return": -1.0,
      "max_sol_return": 0.15,
      "utc_start_hour": 0,
      "utc_end_hour": 18,
      "trailing_stop_fraction": 0.25,
      "breakeven_trigger_net": 0.03,
      "entry_order_type": "LIMIT",
      "stop_exit_order_type": "STOP_MARKET",
      "entry_trigger_offset": 0.002,
      "stop_limit_buffer": 0.003,
      "client_cancel_latency_minutes": 1,
      "exit_distance_mode": "PERCENT",
      "target_atr_multiple": 3.0,
      "stop_atr_multiple": 2.0,
      "structure_window_minutes": 1440,
      "min_rebound_from_low": 0.0,
      "max_rebound_from_low": 0.3,
      "buy_flow_window_minutes": 120,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 0.25,
      "min_relative_sol_return": -0.1,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 1440,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 10.0,
      "min_average_trade_size_ratio": 0.0,
      "max_average_trade_size_ratio": 5.0,
      "min_cvd_ratio": -0.5,
      "max_cvd_ratio": 0.75,
      "min_realized_volatility": 0.015,
      "max_realized_volatility": 0.2,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 0.85,
      "min_illiquidity_ratio": 0.0,
      "max_illiquidity_ratio": 10.0,
      "macro_shock_rule": "BTC",
      "macro_shock_window_minutes": 3,
      "btc_shock_threshold": -0.03,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1078,
          "fills": 42,
          "wins": 25,
          "win_rate": 0.5952380952380952,
          "average_net": 0.007026071309109895,
          "profit_factor": 1.3090307343998506,
          "compound_net": 0.2069136012368602,
          "trades_per_day": 0.23333333333333334,
          "exits": {
            "TP": 25,
            "STOP_MARKET": 6,
            "TRAIL": 10,
            "TIME": 1
          },
          "liquidity_rejections": 43,
          "max_drawdown": 0.32389765874943865,
          "closed_trade_drawdown": 0.25650834171652226,
          "cvar_5": -0.15068000000000015
        },
        "folds": {
          "train": {
            "signals": 20,
            "fills": 20,
            "wins": 14,
            "win_rate": 0.7,
            "average_net": 0.019772000000000033,
            "profit_factor": 2.298397688468613,
            "compound_net": 0.4236478959741443,
            "trades_per_day": 0.3333333333333333,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1793013770158084,
            "closed_trade_drawdown": 0.151359456,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 13,
            "fills": 13,
            "wins": 7,
            "win_rate": 0.5384615384615384,
            "average_net": -0.011266538847491146,
            "profit_factor": 0.7049842314419402,
            "compound_net": -0.1761742884346411,
            "trades_per_day": 0.21666666666666667,
            "exits": {
              "STOP_MARKET": 3,
              "TP": 7,
              "TRAIL": 2,
              "TIME": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.32389765874943854,
            "closed_trade_drawdown": 0.25650834171652237,
            "cvar_5": -0.15068000000000015,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 9,
            "fills": 9,
            "wins": 4,
            "win_rate": 0.4444444444444444,
            "average_net": 0.005124444444444425,
            "profit_factor": 1.2997140629061588,
            "compound_net": 0.029054198316232238,
            "trades_per_day": 0.15,
            "exits": {
              "TP": 4,
              "TRAIL": 4,
              "STOP_MARKET": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1805770284191276,
            "closed_trade_drawdown": 0.15068000000000015,
            "cvar_5": -0.15068000000000015,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.2069136012368602,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1228,
          "fills": 14,
          "wins": 13,
          "win_rate": 0.9285714285714286,
          "average_net": 0.046371428571428605,
          "profit_factor": 812.4999999998647,
          "compound_net": 0.884140623009378,
          "trades_per_day": 0.07777777777777778,
          "exits": {
            "TP": 13,
            "TRAIL": 1
          },
          "liquidity_rejections": 35,
          "max_drawdown": 0.15595757953836553,
          "closed_trade_drawdown": 0.0008000000000001339,
          "cvar_5": -0.0008000000000001339
        },
        "folds": {
          "train": {
            "signals": 3,
            "fills": 3,
            "wins": 3,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.15762500000000013,
            "trades_per_day": 0.05,
            "exits": {
              "TP": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.04671457905544163,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 7,
            "fills": 7,
            "wins": 6,
            "win_rate": 0.8571428571428571,
            "average_net": 0.04274285714285716,
            "profit_factor": 374.99999999993753,
            "compound_net": 0.3390235641125001,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 6,
              "TRAIL": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.07807118254879475,
            "closed_trade_drawdown": 0.0008000000000001339,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 4,
            "fills": 4,
            "wins": 4,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.21550625000000023,
            "trades_per_day": 0.06666666666666667,
            "exits": {
              "TP": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.15595757953836553,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.884140623009378,
        "distance": 0
      },
      "BULL": {
        "metrics": {
          "signals": 1072,
          "fills": 14,
          "wins": 13,
          "win_rate": 0.9285714285714286,
          "average_net": 0.046371428571428605,
          "profit_factor": 812.4999999998647,
          "compound_net": 0.884140623009378,
          "trades_per_day": 0.07777777777777778,
          "exits": {
            "TP": 13,
            "TRAIL": 1
          },
          "liquidity_rejections": 30,
          "max_drawdown": 0.15595757953836553,
          "closed_trade_drawdown": 0.0008000000000001339,
          "cvar_5": -0.0008000000000001339
        },
        "folds": {
          "train": {
            "signals": 3,
            "fills": 3,
            "wins": 3,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.15762500000000013,
            "trades_per_day": 0.05,
            "exits": {
              "TP": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.04671457905544152,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 9,
            "fills": 9,
            "wins": 8,
            "win_rate": 0.8888888888888888,
            "average_net": 0.04435555555555558,
            "profit_factor": 499.9999999999167,
            "compound_net": 0.47627347943403175,
            "trades_per_day": 0.15,
            "exits": {
              "TP": 8,
              "TRAIL": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.15595757953836553,
            "closed_trade_drawdown": 0.0008000000000001339,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 2,
            "fills": 2,
            "wins": 2,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.10250000000000004,
            "trades_per_day": 0.03333333333333333,
            "exits": {
              "TP": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.06241519674355489,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.884140623009378,
        "distance": 0
      }
    },
    "positive": true,
    "eligible": true,
    "score": [
      1,
      0,
      0.2069136012368602
    ],
    "worst_net": 0.2069136012368602,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788796232.6296983
  },
  {
    "id": "V15-ffae6c2a955af81b",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.15,
      "max_hold_minutes": 10080,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 2,
      "min_hours_between_entries": 18,
      "drawdown_gate": -0.004,
      "spread_rate": 0.0,
      "entry_latency_minutes": 0,
      "btc_context_rule": "ANY",
      "sol_context_rule": "ANY",
      "vwap_deviation_gate": -0.004,
      "min_buy_share": 0.0,
      "min_buy_share_delta": 0.0,
      "min_green_candle_return": -0.04,
      "min_volume_ratio_20m": 0.5,
      "vwap_window_minutes": 10080,
      "drawdown_window_minutes": 1440,
      "volume_window_minutes": 240,
      "pump_return_window_minutes": 720,
      "min_pump_return": -0.15,
      "max_pump_return": 0.06,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 30,
      "min_btc_return": -0.1,
      "max_btc_return": 1.0,
      "sol_return_window_minutes": 1,
      "min_sol_return": -0.25,
      "max_sol_return": 0.15,
      "utc_start_hour": 0,
      "utc_end_hour": 18,
      "trailing_stop_fraction": 0.25,
      "breakeven_trigger_net": 0.03,
      "entry_order_type": "LIMIT",
      "stop_exit_order_type": "STOP_MARKET",
      "entry_trigger_offset": 0.002,
      "stop_limit_buffer": 0.003,
      "client_cancel_latency_minutes": 1,
      "exit_distance_mode": "PERCENT",
      "target_atr_multiple": 3.0,
      "stop_atr_multiple": 2.0,
      "structure_window_minutes": 720,
      "min_rebound_from_low": 0.0,
      "max_rebound_from_low": 0.3,
      "buy_flow_window_minutes": 120,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 0.25,
      "min_relative_sol_return": -1.0,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 1440,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 10.0,
      "min_average_trade_size_ratio": 0.0,
      "max_average_trade_size_ratio": 5.0,
      "min_cvd_ratio": -0.5,
      "max_cvd_ratio": 0.75,
      "min_realized_volatility": 0.015,
      "max_realized_volatility": 0.2,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 1.0,
      "min_illiquidity_ratio": 0.0,
      "max_illiquidity_ratio": 10.0,
      "macro_shock_rule": "BTC",
      "macro_shock_window_minutes": 3,
      "btc_shock_threshold": -0.03,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1078,
          "fills": 42,
          "wins": 25,
          "win_rate": 0.5952380952380952,
          "average_net": 0.007026071309109895,
          "profit_factor": 1.3090307343998506,
          "compound_net": 0.2069136012368602,
          "trades_per_day": 0.23333333333333334,
          "exits": {
            "TP": 25,
            "STOP_MARKET": 6,
            "TRAIL": 10,
            "TIME": 1
          },
          "liquidity_rejections": 42,
          "max_drawdown": 0.32389765874943865,
          "closed_trade_drawdown": 0.25650834171652226,
          "cvar_5": -0.15068000000000015
        },
        "folds": {
          "train": {
            "signals": 20,
            "fills": 20,
            "wins": 14,
            "win_rate": 0.7,
            "average_net": 0.019772000000000033,
            "profit_factor": 2.298397688468613,
            "compound_net": 0.4236478959741443,
            "trades_per_day": 0.3333333333333333,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1793013770158084,
            "closed_trade_drawdown": 0.151359456,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 13,
            "fills": 13,
            "wins": 7,
            "win_rate": 0.5384615384615384,
            "average_net": -0.011266538847491146,
            "profit_factor": 0.7049842314419402,
            "compound_net": -0.1761742884346411,
            "trades_per_day": 0.21666666666666667,
            "exits": {
              "STOP_MARKET": 3,
              "TP": 7,
              "TRAIL": 2,
              "TIME": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.32389765874943854,
            "closed_trade_drawdown": 0.25650834171652237,
            "cvar_5": -0.15068000000000015,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 9,
            "fills": 9,
            "wins": 4,
            "win_rate": 0.4444444444444444,
            "average_net": 0.005124444444444425,
            "profit_factor": 1.2997140629061588,
            "compound_net": 0.029054198316232238,
            "trades_per_day": 0.15,
            "exits": {
              "TP": 4,
              "TRAIL": 4,
              "STOP_MARKET": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1805770284191276,
            "closed_trade_drawdown": 0.15068000000000015,
            "cvar_5": -0.15068000000000015,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.2069136012368602,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1222,
          "fills": 14,
          "wins": 13,
          "win_rate": 0.9285714285714286,
          "average_net": 0.046371428571428605,
          "profit_factor": 812.4999999998647,
          "compound_net": 0.884140623009378,
          "trades_per_day": 0.07777777777777778,
          "exits": {
            "TP": 13,
            "TRAIL": 1
          },
          "liquidity_rejections": 34,
          "max_drawdown": 0.15595757953836553,
          "closed_trade_drawdown": 0.0008000000000001339,
          "cvar_5": -0.0008000000000001339
        },
        "folds": {
          "train": {
            "signals": 3,
            "fills": 3,
            "wins": 3,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.15762500000000013,
            "trades_per_day": 0.05,
            "exits": {
              "TP": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.04671457905544163,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 7,
            "fills": 7,
            "wins": 6,
            "win_rate": 0.8571428571428571,
            "average_net": 0.04274285714285716,
            "profit_factor": 374.99999999993753,
            "compound_net": 0.3390235641125001,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 6,
              "TRAIL": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.07807118254879475,
            "closed_trade_drawdown": 0.0008000000000001339,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 4,
            "fills": 4,
            "wins": 4,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.21550625000000023,
            "trades_per_day": 0.06666666666666667,
            "exits": {
              "TP": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.15595757953836553,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.884140623009378,
        "distance": 0
      },
      "BULL": {
        "metrics": {
          "signals": 1069,
          "fills": 14,
          "wins": 13,
          "win_rate": 0.9285714285714286,
          "average_net": 0.046371428571428605,
          "profit_factor": 812.4999999998647,
          "compound_net": 0.884140623009378,
          "trades_per_day": 0.07777777777777778,
          "exits": {
            "TP": 13,
            "TRAIL": 1
          },
          "liquidity_rejections": 31,
          "max_drawdown": 0.15595757953836553,
          "closed_trade_drawdown": 0.0008000000000001339,
          "cvar_5": -0.0008000000000001339
        },
        "folds": {
          "train": {
            "signals": 3,
            "fills": 3,
            "wins": 3,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.15762500000000013,
            "trades_per_day": 0.05,
            "exits": {
              "TP": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.04671457905544152,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 9,
            "fills": 9,
            "wins": 8,
            "win_rate": 0.8888888888888888,
            "average_net": 0.04435555555555558,
            "profit_factor": 499.9999999999167,
            "compound_net": 0.47627347943403175,
            "trades_per_day": 0.15,
            "exits": {
              "TP": 8,
              "TRAIL": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.15595757953836553,
            "closed_trade_drawdown": 0.0008000000000001339,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 2,
            "fills": 2,
            "wins": 2,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.10250000000000004,
            "trades_per_day": 0.03333333333333333,
            "exits": {
              "TP": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.06241519674355489,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.884140623009378,
        "distance": 0
      }
    },
    "positive": true,
    "eligible": true,
    "score": [
      1,
      0,
      0.2069136012368602
    ],
    "worst_net": 0.2069136012368602,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788796232.7876768
  },
  {
    "id": "V15-ff74f28350970adc",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.15,
      "max_hold_minutes": 10080,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 3,
      "min_hours_between_entries": 18,
      "drawdown_gate": -0.002,
      "spread_rate": 0.0,
      "entry_latency_minutes": 0,
      "btc_context_rule": "ANY",
      "sol_context_rule": "ANY",
      "vwap_deviation_gate": -0.004,
      "min_buy_share": 0.0,
      "min_buy_share_delta": 0.0,
      "min_green_candle_return": -0.1,
      "min_volume_ratio_20m": 0.5,
      "vwap_window_minutes": 10080,
      "drawdown_window_minutes": 1440,
      "volume_window_minutes": 240,
      "pump_return_window_minutes": 720,
      "min_pump_return": -0.15,
      "max_pump_return": 0.06,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 30,
      "min_btc_return": -0.1,
      "max_btc_return": 0.15,
      "sol_return_window_minutes": 1,
      "min_sol_return": -1.0,
      "max_sol_return": 1.0,
      "utc_start_hour": 0,
      "utc_end_hour": 18,
      "trailing_stop_fraction": 0.25,
      "breakeven_trigger_net": 0.03,
      "entry_order_type": "LIMIT",
      "stop_exit_order_type": "STOP_MARKET",
      "entry_trigger_offset": 0.002,
      "stop_limit_buffer": 0.003,
      "client_cancel_latency_minutes": 1,
      "exit_distance_mode": "PERCENT",
      "target_atr_multiple": 3.0,
      "stop_atr_multiple": 2.0,
      "structure_window_minutes": 720,
      "min_rebound_from_low": 0.0,
      "max_rebound_from_low": 0.3,
      "buy_flow_window_minutes": 120,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 0.25,
      "min_relative_sol_return": -1.0,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 1440,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 10.0,
      "min_average_trade_size_ratio": 0.0,
      "max_average_trade_size_ratio": 5.0,
      "min_cvd_ratio": -0.5,
      "max_cvd_ratio": 1.0,
      "min_realized_volatility": 0.015,
      "max_realized_volatility": 0.2,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 1.0,
      "min_illiquidity_ratio": 0.0,
      "max_illiquidity_ratio": 10.0,
      "macro_shock_rule": "BTC",
      "macro_shock_window_minutes": 3,
      "btc_shock_threshold": -0.03,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1081,
          "fills": 42,
          "wins": 25,
          "win_rate": 0.5952380952380952,
          "average_net": 0.007026071309109895,
          "profit_factor": 1.3090307343998506,
          "compound_net": 0.2069136012368602,
          "trades_per_day": 0.23333333333333334,
          "exits": {
            "TP": 25,
            "STOP_MARKET": 6,
            "TRAIL": 10,
            "TIME": 1
          },
          "liquidity_rejections": 44,
          "max_drawdown": 0.32389765874943865,
          "closed_trade_drawdown": 0.25650834171652226,
          "cvar_5": -0.15068000000000015
        },
        "folds": {
          "train": {
            "signals": 20,
            "fills": 20,
            "wins": 14,
            "win_rate": 0.7,
            "average_net": 0.019772000000000033,
            "profit_factor": 2.298397688468613,
            "compound_net": 0.4236478959741443,
            "trades_per_day": 0.3333333333333333,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1793013770158084,
            "closed_trade_drawdown": 0.151359456,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 13,
            "fills": 13,
            "wins": 7,
            "win_rate": 0.5384615384615384,
            "average_net": -0.011266538847491146,
            "profit_factor": 0.7049842314419402,
            "compound_net": -0.1761742884346411,
            "trades_per_day": 0.21666666666666667,
            "exits": {
              "STOP_MARKET": 3,
              "TP": 7,
              "TRAIL": 2,
              "TIME": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.32389765874943854,
            "closed_trade_drawdown": 0.25650834171652237,
            "cvar_5": -0.15068000000000015,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 9,
            "fills": 9,
            "wins": 4,
            "win_rate": 0.4444444444444444,
            "average_net": 0.005124444444444425,
            "profit_factor": 1.2997140629061588,
            "compound_net": 0.029054198316232238,
            "trades_per_day": 0.15,
            "exits": {
              "TP": 4,
              "TRAIL": 4,
              "STOP_MARKET": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1805770284191276,
            "closed_trade_drawdown": 0.15068000000000015,
            "cvar_5": -0.15068000000000015,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.2069136012368602,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1229,
          "fills": 14,
          "wins": 13,
          "win_rate": 0.9285714285714286,
          "average_net": 0.046371428571428605,
          "profit_factor": 812.4999999998647,
          "compound_net": 0.884140623009378,
          "trades_per_day": 0.07777777777777778,
          "exits": {
            "TP": 13,
            "TRAIL": 1
          },
          "liquidity_rejections": 36,
          "max_drawdown": 0.15595757953836553,
          "closed_trade_drawdown": 0.0008000000000001339,
          "cvar_5": -0.0008000000000001339
        },
        "folds": {
          "train": {
            "signals": 3,
            "fills": 3,
            "wins": 3,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.15762500000000013,
            "trades_per_day": 0.05,
            "exits": {
              "TP": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.04671457905544163,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 7,
            "fills": 7,
            "wins": 6,
            "win_rate": 0.8571428571428571,
            "average_net": 0.04274285714285716,
            "profit_factor": 374.99999999993753,
            "compound_net": 0.3390235641125001,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 6,
              "TRAIL": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.07807118254879475,
            "closed_trade_drawdown": 0.0008000000000001339,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 4,
            "fills": 4,
            "wins": 4,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.21550625000000023,
            "trades_per_day": 0.06666666666666667,
            "exits": {
              "TP": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.15595757953836553,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.884140623009378,
        "distance": 0
      },
      "BULL": {
        "metrics": {
          "signals": 1073,
          "fills": 14,
          "wins": 13,
          "win_rate": 0.9285714285714286,
          "average_net": 0.046371428571428605,
          "profit_factor": 812.4999999998647,
          "compound_net": 0.884140623009378,
          "trades_per_day": 0.07777777777777778,
          "exits": {
            "TP": 13,
            "TRAIL": 1
          },
          "liquidity_rejections": 31,
          "max_drawdown": 0.15595757953836553,
          "closed_trade_drawdown": 0.0008000000000001339,
          "cvar_5": -0.0008000000000001339
        },
        "folds": {
          "train": {
            "signals": 3,
            "fills": 3,
            "wins": 3,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.15762500000000013,
            "trades_per_day": 0.05,
            "exits": {
              "TP": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.04671457905544152,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 9,
            "fills": 9,
            "wins": 8,
            "win_rate": 0.8888888888888888,
            "average_net": 0.04435555555555558,
            "profit_factor": 499.9999999999167,
            "compound_net": 0.47627347943403175,
            "trades_per_day": 0.15,
            "exits": {
              "TP": 8,
              "TRAIL": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.15595757953836553,
            "closed_trade_drawdown": 0.0008000000000001339,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 2,
            "fills": 2,
            "wins": 2,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.10250000000000004,
            "trades_per_day": 0.03333333333333333,
            "exits": {
              "TP": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.06241519674355489,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.884140623009378,
        "distance": 0
      }
    },
    "positive": true,
    "eligible": true,
    "score": [
      1,
      0,
      0.2069136012368602
    ],
    "worst_net": 0.2069136012368602,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788796232.8569932
  },
  {
    "id": "V15-ff85f6a1c534f39f",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.15,
      "max_hold_minutes": 10080,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 5,
      "min_hours_between_entries": 18,
      "drawdown_gate": -0.002,
      "spread_rate": 0.0,
      "entry_latency_minutes": 0,
      "btc_context_rule": "ANY",
      "sol_context_rule": "ANY",
      "vwap_deviation_gate": -0.004,
      "min_buy_share": 0.0,
      "min_buy_share_delta": 0.0,
      "min_green_candle_return": -0.04,
      "min_volume_ratio_20m": 0.5,
      "vwap_window_minutes": 10080,
      "drawdown_window_minutes": 1440,
      "volume_window_minutes": 240,
      "pump_return_window_minutes": 720,
      "min_pump_return": -0.1,
      "max_pump_return": 0.06,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 0.3,
      "btc_return_window_minutes": 15,
      "min_btc_return": -0.1,
      "max_btc_return": 0.15,
      "sol_return_window_minutes": 3,
      "min_sol_return": -0.25,
      "max_sol_return": 0.06,
      "utc_start_hour": 0,
      "utc_end_hour": 18,
      "trailing_stop_fraction": 0.25,
      "breakeven_trigger_net": 0.03,
      "entry_order_type": "LIMIT",
      "stop_exit_order_type": "STOP_MARKET",
      "entry_trigger_offset": 0.002,
      "stop_limit_buffer": 0.003,
      "client_cancel_latency_minutes": 1,
      "exit_distance_mode": "PERCENT",
      "target_atr_multiple": 3.0,
      "stop_atr_multiple": 2.0,
      "structure_window_minutes": 720,
      "min_rebound_from_low": 0.0,
      "max_rebound_from_low": 0.3,
      "buy_flow_window_minutes": 120,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -1.0,
      "max_relative_btc_return": 0.25,
      "min_relative_sol_return": -0.1,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 1440,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 10.0,
      "min_average_trade_size_ratio": 0.0,
      "max_average_trade_size_ratio": 5.0,
      "min_cvd_ratio": -0.5,
      "max_cvd_ratio": 0.75,
      "min_realized_volatility": 0.015,
      "max_realized_volatility": 1.0,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 1.0,
      "min_illiquidity_ratio": 0.0,
      "max_illiquidity_ratio": 10.0,
      "macro_shock_rule": "BTC",
      "macro_shock_window_minutes": 1,
      "btc_shock_threshold": -0.03,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1079,
          "fills": 42,
          "wins": 25,
          "win_rate": 0.5952380952380952,
          "average_net": 0.007026071309109895,
          "profit_factor": 1.3090307343998506,
          "compound_net": 0.2069136012368602,
          "trades_per_day": 0.23333333333333334,
          "exits": {
            "TP": 25,
            "STOP_MARKET": 6,
            "TRAIL": 10,
            "TIME": 1
          },
          "liquidity_rejections": 44,
          "max_drawdown": 0.32389765874943865,
          "closed_trade_drawdown": 0.25650834171652226,
          "cvar_5": -0.15068000000000015
        },
        "folds": {
          "train": {
            "signals": 20,
            "fills": 20,
            "wins": 14,
            "win_rate": 0.7,
            "average_net": 0.019772000000000033,
            "profit_factor": 2.298397688468613,
            "compound_net": 0.4236478959741443,
            "trades_per_day": 0.3333333333333333,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1793013770158084,
            "closed_trade_drawdown": 0.151359456,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 13,
            "fills": 13,
            "wins": 7,
            "win_rate": 0.5384615384615384,
            "average_net": -0.011266538847491146,
            "profit_factor": 0.7049842314419402,
            "compound_net": -0.1761742884346411,
            "trades_per_day": 0.21666666666666667,
            "exits": {
              "STOP_MARKET": 3,
              "TP": 7,
              "TRAIL": 2,
              "TIME": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.32389765874943854,
            "closed_trade_drawdown": 0.25650834171652237,
            "cvar_5": -0.15068000000000015,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 9,
            "fills": 9,
            "wins": 4,
            "win_rate": 0.4444444444444444,
            "average_net": 0.005124444444444425,
            "profit_factor": 1.2997140629061588,
            "compound_net": 0.029054198316232238,
            "trades_per_day": 0.15,
            "exits": {
              "TP": 4,
              "TRAIL": 4,
              "STOP_MARKET": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1805770284191276,
            "closed_trade_drawdown": 0.15068000000000015,
            "cvar_5": -0.15068000000000015,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.2069136012368602,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1228,
          "fills": 14,
          "wins": 13,
          "win_rate": 0.9285714285714286,
          "average_net": 0.046371428571428605,
          "profit_factor": 812.4999999998647,
          "compound_net": 0.884140623009378,
          "trades_per_day": 0.07777777777777778,
          "exits": {
            "TP": 13,
            "TRAIL": 1
          },
          "liquidity_rejections": 35,
          "max_drawdown": 0.15595757953836553,
          "closed_trade_drawdown": 0.0008000000000001339,
          "cvar_5": -0.0008000000000001339
        },
        "folds": {
          "train": {
            "signals": 3,
            "fills": 3,
            "wins": 3,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.15762500000000013,
            "trades_per_day": 0.05,
            "exits": {
              "TP": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.04671457905544163,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 7,
            "fills": 7,
            "wins": 6,
            "win_rate": 0.8571428571428571,
            "average_net": 0.04274285714285716,
            "profit_factor": 374.99999999993753,
            "compound_net": 0.3390235641125001,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 6,
              "TRAIL": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.07807118254879475,
            "closed_trade_drawdown": 0.0008000000000001339,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 4,
            "fills": 4,
            "wins": 4,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.21550625000000023,
            "trades_per_day": 0.06666666666666667,
            "exits": {
              "TP": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.15595757953836553,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.884140623009378,
        "distance": 0
      },
      "BULL": {
        "metrics": {
          "signals": 1072,
          "fills": 14,
          "wins": 13,
          "win_rate": 0.9285714285714286,
          "average_net": 0.046371428571428605,
          "profit_factor": 812.4999999998647,
          "compound_net": 0.884140623009378,
          "trades_per_day": 0.07777777777777778,
          "exits": {
            "TP": 13,
            "TRAIL": 1
          },
          "liquidity_rejections": 30,
          "max_drawdown": 0.15595757953836553,
          "closed_trade_drawdown": 0.0008000000000001339,
          "cvar_5": -0.0008000000000001339
        },
        "folds": {
          "train": {
            "signals": 3,
            "fills": 3,
            "wins": 3,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.15762500000000013,
            "trades_per_day": 0.05,
            "exits": {
              "TP": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.04671457905544152,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 9,
            "fills": 9,
            "wins": 8,
            "win_rate": 0.8888888888888888,
            "average_net": 0.04435555555555558,
            "profit_factor": 499.9999999999167,
            "compound_net": 0.47627347943403175,
            "trades_per_day": 0.15,
            "exits": {
              "TP": 8,
              "TRAIL": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.15595757953836553,
            "closed_trade_drawdown": 0.0008000000000001339,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 2,
            "fills": 2,
            "wins": 2,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.10250000000000004,
            "trades_per_day": 0.03333333333333333,
            "exits": {
              "TP": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.06241519674355489,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.884140623009378,
        "distance": 0
      }
    },
    "positive": true,
    "eligible": true,
    "score": [
      1,
      0,
      0.2069136012368602
    ],
    "worst_net": 0.2069136012368602,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788796232.8666904
  },
  {
    "id": "V15-ffbe301e31439bce",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.15,
      "max_hold_minutes": 10080,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 3,
      "min_hours_between_entries": 18,
      "drawdown_gate": -0.002,
      "spread_rate": 0.0,
      "entry_latency_minutes": 0,
      "btc_context_rule": "ANY",
      "sol_context_rule": "ANY",
      "vwap_deviation_gate": -0.004,
      "min_buy_share": 0.0,
      "min_buy_share_delta": 0.0,
      "min_green_candle_return": -0.04,
      "min_volume_ratio_20m": 0.5,
      "vwap_window_minutes": 10080,
      "drawdown_window_minutes": 7200,
      "volume_window_minutes": 240,
      "pump_return_window_minutes": 720,
      "min_pump_return": -0.15,
      "max_pump_return": 0.06,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 30,
      "min_btc_return": -0.1,
      "max_btc_return": 0.1,
      "sol_return_window_minutes": 1,
      "min_sol_return": -0.25,
      "max_sol_return": 0.15,
      "utc_start_hour": 0,
      "utc_end_hour": 18,
      "trailing_stop_fraction": 0.25,
      "breakeven_trigger_net": 0.03,
      "entry_order_type": "LIMIT",
      "stop_exit_order_type": "STOP_MARKET",
      "entry_trigger_offset": 0.002,
      "stop_limit_buffer": 0.003,
      "client_cancel_latency_minutes": 1,
      "exit_distance_mode": "PERCENT",
      "target_atr_multiple": 3.0,
      "stop_atr_multiple": 2.0,
      "structure_window_minutes": 720,
      "min_rebound_from_low": 0.0,
      "max_rebound_from_low": 0.3,
      "buy_flow_window_minutes": 120,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 0.25,
      "min_relative_sol_return": -1.0,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 1440,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 10.0,
      "min_average_trade_size_ratio": 0.0,
      "max_average_trade_size_ratio": 1000000.0,
      "min_cvd_ratio": -0.5,
      "max_cvd_ratio": 0.75,
      "min_realized_volatility": 0.015,
      "max_realized_volatility": 1.0,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 1.0,
      "min_illiquidity_ratio": 0.0,
      "max_illiquidity_ratio": 3.0,
      "macro_shock_rule": "BTC",
      "macro_shock_window_minutes": 3,
      "btc_shock_threshold": -0.03,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1085,
          "fills": 43,
          "wins": 25,
          "win_rate": 0.5813953488372093,
          "average_net": 0.006844069650758502,
          "profit_factor": 1.3079349730697098,
          "compound_net": 0.20594807035587048,
          "trades_per_day": 0.2388888888888889,
          "exits": {
            "TP": 25,
            "STOP_MARKET": 6,
            "TRAIL": 11,
            "TIME": 1
          },
          "liquidity_rejections": 47,
          "max_drawdown": 0.32389765874943854,
          "closed_trade_drawdown": 0.25650834171652226,
          "cvar_5": -0.15068000000000015
        },
        "folds": {
          "train": {
            "signals": 21,
            "fills": 21,
            "wins": 14,
            "win_rate": 0.6666666666666666,
            "average_net": 0.018792380952380984,
            "profit_factor": 2.292376211684571,
            "compound_net": 0.4225089776573647,
            "trades_per_day": 0.35,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 5
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1793013770158084,
            "closed_trade_drawdown": 0.151359456,
            "cvar_5": -0.15067999999999998,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 13,
            "fills": 13,
            "wins": 7,
            "win_rate": 0.5384615384615384,
            "average_net": -0.011266538847491146,
            "profit_factor": 0.7049842314419402,
            "compound_net": -0.176174288434641,
            "trades_per_day": 0.21666666666666667,
            "exits": {
              "STOP_MARKET": 3,
              "TP": 7,
              "TRAIL": 2,
              "TIME": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.32389765874943854,
            "closed_trade_drawdown": 0.25650834171652237,
            "cvar_5": -0.15068000000000015,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 9,
            "fills": 9,
            "wins": 4,
            "win_rate": 0.4444444444444444,
            "average_net": 0.005124444444444425,
            "profit_factor": 1.2997140629061588,
            "compound_net": 0.029054198316232016,
            "trades_per_day": 0.15,
            "exits": {
              "TP": 4,
              "TRAIL": 4,
              "STOP_MARKET": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.18057702841912748,
            "closed_trade_drawdown": 0.15068000000000015,
            "cvar_5": -0.15068000000000015,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.20594807035587048,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1232,
          "fills": 14,
          "wins": 13,
          "win_rate": 0.9285714285714286,
          "average_net": 0.046371428571428605,
          "profit_factor": 812.4999999998647,
          "compound_net": 0.884140623009378,
          "trades_per_day": 0.07777777777777778,
          "exits": {
            "TP": 13,
            "TRAIL": 1
          },
          "liquidity_rejections": 37,
          "max_drawdown": 0.15595757953836553,
          "closed_trade_drawdown": 0.0008000000000001339,
          "cvar_5": -0.0008000000000001339
        },
        "folds": {
          "train": {
            "signals": 3,
            "fills": 3,
            "wins": 3,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.15762500000000013,
            "trades_per_day": 0.05,
            "exits": {
              "TP": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.04671457905544163,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 7,
            "fills": 7,
            "wins": 6,
            "win_rate": 0.8571428571428571,
            "average_net": 0.04274285714285716,
            "profit_factor": 374.99999999993753,
            "compound_net": 0.3390235641125001,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 6,
              "TRAIL": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.07807118254879475,
            "closed_trade_drawdown": 0.0008000000000001339,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 4,
            "fills": 4,
            "wins": 4,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.21550625000000023,
            "trades_per_day": 0.06666666666666667,
            "exits": {
              "TP": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.15595757953836553,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.884140623009378,
        "distance": 0
      },
      "BULL": {
        "metrics": {
          "signals": 1075,
          "fills": 14,
          "wins": 13,
          "win_rate": 0.9285714285714286,
          "average_net": 0.046371428571428605,
          "profit_factor": 812.4999999998647,
          "compound_net": 0.884140623009378,
          "trades_per_day": 0.07777777777777778,
          "exits": {
            "TP": 13,
            "TRAIL": 1
          },
          "liquidity_rejections": 31,
          "max_drawdown": 0.15595757953836553,
          "closed_trade_drawdown": 0.0008000000000001339,
          "cvar_5": -0.0008000000000001339
        },
        "folds": {
          "train": {
            "signals": 3,
            "fills": 3,
            "wins": 3,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.15762500000000013,
            "trades_per_day": 0.05,
            "exits": {
              "TP": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.04671457905544152,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 9,
            "fills": 9,
            "wins": 8,
            "win_rate": 0.8888888888888888,
            "average_net": 0.04435555555555558,
            "profit_factor": 499.9999999999167,
            "compound_net": 0.47627347943403175,
            "trades_per_day": 0.15,
            "exits": {
              "TP": 8,
              "TRAIL": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.15595757953836553,
            "closed_trade_drawdown": 0.0008000000000001339,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 2,
            "fills": 2,
            "wins": 2,
            "win_rate": 1.0,
            "average_net": 0.050000000000000044,
            "profit_factor": null,
            "compound_net": 0.10250000000000004,
            "trades_per_day": 0.03333333333333333,
            "exits": {
              "TP": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.06241519674355489,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.050000000000000044,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.884140623009378,
        "distance": 0
      }
    },
    "positive": true,
    "eligible": true,
    "score": [
      1,
      0,
      0.20594807035587048
    ],
    "worst_net": 0.20594807035587048,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788796231.8272498
  },
  {
    "id": "V15-57e3552db688aafb",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.04,
      "stop_net": -0.15,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 5,
      "min_hours_between_entries": 18,
      "drawdown_gate": -0.03,
      "spread_rate": 0.0,
      "entry_latency_minutes": 0,
      "btc_context_rule": "ANY",
      "sol_context_rule": "ANY",
      "vwap_deviation_gate": -0.004,
      "min_buy_share": 0.0,
      "min_buy_share_delta": -0.25,
      "min_green_candle_return": -0.1,
      "min_volume_ratio_20m": 0.25,
      "vwap_window_minutes": 10080,
      "drawdown_window_minutes": 2880,
      "volume_window_minutes": 240,
      "pump_return_window_minutes": 120,
      "min_pump_return": -1.0,
      "max_pump_return": 1.0,
      "rsi_period_minutes": 14,
      "min_rsi": 0.0,
      "max_rsi": 100.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 1,
      "min_btc_return": -1.0,
      "max_btc_return": 1.0,
      "sol_return_window_minutes": 1,
      "min_sol_return": -1.0,
      "max_sol_return": 1.0,
      "utc_start_hour": 0,
      "utc_end_hour": 14,
      "trailing_stop_fraction": 0.25,
      "breakeven_trigger_net": 0.25,
      "entry_order_type": "LIMIT",
      "stop_exit_order_type": "STOP_MARKET",
      "entry_trigger_offset": 0.002,
      "stop_limit_buffer": 0.003,
      "client_cancel_latency_minutes": 1,
      "exit_distance_mode": "PERCENT",
      "target_atr_multiple": 3.0,
      "stop_atr_multiple": 2.0,
      "structure_window_minutes": 720,
      "min_rebound_from_low": 0.0,
      "max_rebound_from_low": 1.0,
      "buy_flow_window_minutes": 240,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -1.0,
      "max_relative_btc_return": 1.0,
      "min_relative_sol_return": -1.0,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 720,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 1000000.0,
      "min_average_trade_size_ratio": 0.5,
      "max_average_trade_size_ratio": 2.0,
      "min_cvd_ratio": -0.1,
      "max_cvd_ratio": 0.1,
      "min_realized_volatility": 0.0,
      "max_realized_volatility": 1.0,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 1.0,
      "min_illiquidity_ratio": 0.0,
      "max_illiquidity_ratio": 1000000.0,
      "macro_shock_rule": "OFF",
      "macro_shock_window_minutes": 5,
      "btc_shock_threshold": -1.0,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 796,
          "fills": 30,
          "wins": 27,
          "win_rate": 0.9,
          "average_net": 0.02093200000000003,
          "profit_factor": 2.389169100079641,
          "compound_net": 0.7665023288065735,
          "trades_per_day": 0.16666666666666666,
          "exits": {
            "TP": 27,
            "STOP_MARKET": 3
          },
          "liquidity_rejections": 22,
          "max_drawdown": 0.2767448280672816,
          "closed_trade_drawdown": 0.21979382946816017,
          "cvar_5": -0.15068000000000004
        },
        "folds": {
          "train": {
            "signals": 17,
            "fills": 17,
            "wins": 15,
            "win_rate": 0.8823529411764706,
            "average_net": 0.01756705882352944,
            "profit_factor": 1.9909742500663672,
            "compound_net": 0.299100624792658,
            "trades_per_day": 0.2833333333333333,
            "exits": {
              "TP": 15,
              "STOP_MARKET": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2767448280672816,
            "closed_trade_drawdown": 0.21979382946816017,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 7,
            "fills": 7,
            "wins": 6,
            "win_rate": 0.8571428571428571,
            "average_net": 0.01276000000000004,
            "profit_factor": 1.5927794000530948,
            "compound_net": 0.07466074878902296,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "STOP_MARKET": 1,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2528099225386935,
            "closed_trade_drawdown": 0.15067999999999993,
            "cvar_5": -0.15067999999999993,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 6,
            "fills": 6,
            "wins": 6,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.26531901849600015,
            "trades_per_day": 0.1,
            "exits": {
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.11641321447299413,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.7665023288065735,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 616,
          "fills": 6,
          "wins": 5,
          "win_rate": 0.8333333333333334,
          "average_net": 0.03229290171178569,
          "profit_factor": 32.03798562345599,
          "compound_net": 0.20905783748737217,
          "trades_per_day": 0.03333333333333333,
          "exits": {
            "TP": 5,
            "TIME": 1
          },
          "liquidity_rejections": 12,
          "max_drawdown": 0.15595757953836553,
          "closed_trade_drawdown": 0.00624258972928593,
          "cvar_5": -0.006242589729286041
        },
        "folds": {
          "train": {
            "signals": 2,
            "fills": 2,
            "wins": 2,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.08160000000000012,
            "trades_per_day": 0.03333333333333333,
            "exits": {
              "TP": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.044060234244283514,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 2,
            "fills": 2,
            "wins": 1,
            "win_rate": 0.5,
            "average_net": 0.016878705135356997,
            "profit_factor": 6.407597124691197,
            "compound_net": 0.03350770668154257,
            "trades_per_day": 0.03333333333333333,
            "exits": {
              "TIME": 1,
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.13433637829124123,
            "closed_trade_drawdown": 0.006242589729286041,
            "cvar_5": -0.006242589729286041,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 2,
            "fills": 2,
            "wins": 2,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.08160000000000012,
            "trades_per_day": 0.03333333333333333,
            "exits": {
              "TP": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.15595757953836553,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.20905783748737217,
        "distance": 0
      },
      "BULL": {
        "metrics": {
          "signals": 518,
          "fills": 5,
          "wins": 4,
          "win_rate": 0.8,
          "average_net": 0.03075148205414282,
          "profit_factor": 25.630388498764788,
          "compound_net": 0.16255561296862675,
          "trades_per_day": 0.027777777777777776,
          "exits": {
            "TP": 4,
            "TIME": 1
          },
          "liquidity_rejections": 11,
          "max_drawdown": 0.15595757953836553,
          "closed_trade_drawdown": 0.006242589729286041,
          "cvar_5": -0.006242589729286041
        },
        "folds": {
          "train": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.04671457905544163,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 3,
            "fills": 3,
            "wins": 2,
            "win_rate": 0.6666666666666666,
            "average_net": 0.024585803423571344,
            "profit_factor": 12.815194249382394,
            "compound_net": 0.07484801494880422,
            "trades_per_day": 0.05,
            "exits": {
              "TIME": 1,
              "TP": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.15595757953836542,
            "closed_trade_drawdown": 0.006242589729286041,
            "cvar_5": -0.006242589729286041,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.05560098119378576,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.16255561296862675,
        "distance": 0
      }
    },
    "positive": true,
    "eligible": true,
    "score": [
      1,
      0,
      0.16255561296862675
    ],
    "worst_net": 0.16255561296862675,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788796207.9284325
  },
  {
    "id": "V15-6121cac293a7c917",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.04,
      "stop_net": -0.15,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 5,
      "min_hours_between_entries": 18,
      "drawdown_gate": -0.03,
      "spread_rate": 0.0,
      "entry_latency_minutes": 0,
      "btc_context_rule": "ANY",
      "sol_context_rule": "ANY",
      "vwap_deviation_gate": -0.01,
      "min_buy_share": 0.0,
      "min_buy_share_delta": -0.25,
      "min_green_candle_return": -0.1,
      "min_volume_ratio_20m": 0.25,
      "vwap_window_minutes": 10080,
      "drawdown_window_minutes": 2880,
      "volume_window_minutes": 1440,
      "pump_return_window_minutes": 15,
      "min_pump_return": -1.0,
      "max_pump_return": 1.0,
      "rsi_period_minutes": 14,
      "min_rsi": 0.0,
      "max_rsi": 100.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 15,
      "min_btc_return": -1.0,
      "max_btc_return": 1.0,
      "sol_return_window_minutes": 1,
      "min_sol_return": -1.0,
      "max_sol_return": 1.0,
      "utc_start_hour": 0,
      "utc_end_hour": 14,
      "trailing_stop_fraction": 0.15,
      "breakeven_trigger_net": 0.25,
      "entry_order_type": "LIMIT",
      "stop_exit_order_type": "STOP_LIMIT",
      "entry_trigger_offset": 0.002,
      "stop_limit_buffer": 0.005,
      "client_cancel_latency_minutes": 1,
      "exit_distance_mode": "PERCENT",
      "target_atr_multiple": 3.0,
      "stop_atr_multiple": 2.0,
      "structure_window_minutes": 720,
      "min_rebound_from_low": 0.0,
      "max_rebound_from_low": 1.0,
      "buy_flow_window_minutes": 240,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 1.0,
      "min_relative_sol_return": -1.0,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 720,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 1000000.0,
      "min_average_trade_size_ratio": 0.5,
      "max_average_trade_size_ratio": 5.0,
      "min_cvd_ratio": -0.1,
      "max_cvd_ratio": 0.1,
      "min_realized_volatility": 0.0,
      "max_realized_volatility": 1.0,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 1.0,
      "min_illiquidity_ratio": 0.0,
      "max_illiquidity_ratio": 1000000.0,
      "macro_shock_rule": "OFF",
      "macro_shock_window_minutes": 5,
      "btc_shock_threshold": -1.0,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 762,
          "fills": 34,
          "wins": 29,
          "win_rate": 0.8529411764705882,
          "average_net": 0.01513573177048986,
          "profit_factor": 1.7973764259601503,
          "compound_net": 0.5617104167016351,
          "trades_per_day": 0.18888888888888888,
          "exits": {
            "TP": 29,
            "STOP_LIMIT_FILLED": 5
          },
          "liquidity_rejections": 18,
          "max_drawdown": 0.2627637836666683,
          "closed_trade_drawdown": 0.24937570723616398,
          "cvar_5": -0.14454416531457442
        },
        "folds": {
          "train": {
            "signals": 18,
            "fills": 18,
            "wins": 16,
            "win_rate": 0.8888888888888888,
            "average_net": 0.02232379883482836,
            "profit_factor": 2.687137944416607,
            "compound_net": 0.4533689236050491,
            "trades_per_day": 0.3,
            "exits": {
              "TP": 16,
              "STOP_LIMIT_FILLED": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.20398068817645432,
            "closed_trade_drawdown": 0.16071566047053532,
            "cvar_5": -0.1257385306580665,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 9,
            "fills": 9,
            "wins": 7,
            "win_rate": 0.7777777777777778,
            "average_net": 0.0014492861767395862,
            "profit_factor": 1.0488603172578292,
            "compound_net": -0.012229638885641103,
            "trades_per_day": 0.15,
            "exits": {
              "STOP_LIMIT_FILLED": 2,
              "TP": 7
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2627637836666683,
            "closed_trade_drawdown": 0.24937570723616398,
            "cvar_5": -0.14883125620823712,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 6,
            "win_rate": 0.8571428571428571,
            "average_net": 0.014248989368441212,
            "profit_factor": 1.7111436338659096,
            "compound_net": 0.08784907475261172,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 6,
              "STOP_LIMIT_FILLED": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.24518578933674873,
            "closed_trade_drawdown": 0.14025707442091173,
            "cvar_5": -0.14025707442091173,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.5617104167016351,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 624,
          "fills": 5,
          "wins": 5,
          "win_rate": 1.0,
          "average_net": 0.040000000000000036,
          "profit_factor": null,
          "compound_net": 0.21665290240000035,
          "trades_per_day": 0.027777777777777776,
          "exits": {
            "TP": 5
          },
          "liquidity_rejections": 13,
          "max_drawdown": 0.05560098119378565,
          "closed_trade_drawdown": 0.0,
          "cvar_5": 0.040000000000000036
        },
        "folds": {
          "train": {
            "signals": 3,
            "fills": 3,
            "wins": 3,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.12486400000000009,
            "trades_per_day": 0.05,
            "exits": {
              "TP": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.0440602342442834,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.03004941782909365,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.05560098119378554,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.21665290240000035,
        "distance": 0
      },
      "BULL": {
        "metrics": {
          "signals": 531,
          "fills": 3,
          "wins": 3,
          "win_rate": 1.0,
          "average_net": 0.040000000000000036,
          "profit_factor": null,
          "compound_net": 0.12486400000000009,
          "trades_per_day": 0.016666666666666666,
          "exits": {
            "TP": 3
          },
          "liquidity_rejections": 12,
          "max_drawdown": 0.05560098119378576,
          "closed_trade_drawdown": 0.0,
          "cvar_5": 0.040000000000000036
        },
        "folds": {
          "train": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.022447888829502993,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.03004941782909365,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.05560098119378554,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.12486400000000009,
        "distance": 0
      }
    },
    "positive": true,
    "eligible": true,
    "score": [
      1,
      0,
      0.12486400000000009
    ],
    "worst_net": 0.12486400000000009,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788796207.9273205
  },
  {
    "id": "V15-51eb8bcfb4575d17",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.04,
      "stop_net": -0.15,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 5,
      "min_hours_between_entries": 18,
      "drawdown_gate": -0.03,
      "spread_rate": 0.0,
      "entry_latency_minutes": 0,
      "btc_context_rule": "ANY",
      "sol_context_rule": "ANY",
      "vwap_deviation_gate": -0.004,
      "min_buy_share": 0.0,
      "min_buy_share_delta": -0.25,
      "min_green_candle_return": -0.1,
      "min_volume_ratio_20m": 0.25,
      "vwap_window_minutes": 10080,
      "drawdown_window_minutes": 2880,
      "volume_window_minutes": 1440,
      "pump_return_window_minutes": 120,
      "min_pump_return": -1.0,
      "max_pump_return": 1.0,
      "rsi_period_minutes": 14,
      "min_rsi": 0.0,
      "max_rsi": 100.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 1,
      "min_btc_return": -1.0,
      "max_btc_return": 1.0,
      "sol_return_window_minutes": 1,
      "min_sol_return": -1.0,
      "max_sol_return": 1.0,
      "utc_start_hour": 0,
      "utc_end_hour": 14,
      "trailing_stop_fraction": 0.25,
      "breakeven_trigger_net": 0.25,
      "entry_order_type": "LIMIT",
      "stop_exit_order_type": "STOP_MARKET",
      "entry_trigger_offset": 0.002,
      "stop_limit_buffer": 0.003,
      "client_cancel_latency_minutes": 1,
      "exit_distance_mode": "PERCENT",
      "target_atr_multiple": 3.0,
      "stop_atr_multiple": 2.0,
      "structure_window_minutes": 720,
      "min_rebound_from_low": 0.0,
      "max_rebound_from_low": 1.0,
      "buy_flow_window_minutes": 240,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -1.0,
      "max_relative_btc_return": 1.0,
      "min_relative_sol_return": -1.0,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 720,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 1000000.0,
      "min_average_trade_size_ratio": 0.5,
      "max_average_trade_size_ratio": 5.0,
      "min_cvd_ratio": -0.1,
      "max_cvd_ratio": 0.1,
      "min_realized_volatility": 0.0,
      "max_realized_volatility": 1.0,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 1.0,
      "min_illiquidity_ratio": 0.0,
      "max_illiquidity_ratio": 1000000.0,
      "macro_shock_rule": "OFF",
      "macro_shock_window_minutes": 5,
      "btc_shock_threshold": -1.0,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 708,
          "fills": 32,
          "wins": 27,
          "win_rate": 0.84375,
          "average_net": 0.010206250000000031,
          "profit_factor": 1.4335014600477847,
          "compound_net": 0.2742566727013258,
          "trades_per_day": 0.17777777777777778,
          "exits": {
            "TP": 27,
            "STOP_MARKET": 5
          },
          "liquidity_rejections": 21,
          "max_drawdown": 0.28791903039417865,
          "closed_trade_drawdown": 0.24980175910399993,
          "cvar_5": -0.15068000000000004
        },
        "folds": {
          "train": {
            "signals": 17,
            "fills": 17,
            "wins": 15,
            "win_rate": 0.8823529411764706,
            "average_net": 0.01756705882352944,
            "profit_factor": 1.9909742500663672,
            "compound_net": 0.299100624792658,
            "trades_per_day": 0.2833333333333333,
            "exits": {
              "TP": 15,
              "STOP_MARKET": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2767448280672816,
            "closed_trade_drawdown": 0.21979382946816017,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 7,
            "fills": 7,
            "wins": 6,
            "win_rate": 0.8571428571428571,
            "average_net": 0.01276000000000004,
            "profit_factor": 1.5927794000530948,
            "compound_net": 0.07466074878902296,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "STOP_MARKET": 1,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2528099225386935,
            "closed_trade_drawdown": 0.15067999999999993,
            "cvar_5": -0.15067999999999993,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 8,
            "fills": 8,
            "wins": 6,
            "win_rate": 0.75,
            "average_net": -0.0076699999999999685,
            "profit_factor": 0.7963897000265472,
            "compound_net": -0.08726913283850712,
            "trades_per_day": 0.13333333333333333,
            "exits": {
              "TP": 6,
              "STOP_MARKET": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.28791903039417865,
            "closed_trade_drawdown": 0.24980175910399993,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.2742566727013258,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 589,
          "fills": 8,
          "wins": 7,
          "win_rate": 0.875,
          "average_net": 0.016165000000000027,
          "profit_factor": 1.858242633395276,
          "compound_net": 0.11764717874058395,
          "trades_per_day": 0.044444444444444446,
          "exits": {
            "STOP_MARKET": 1,
            "TP": 7
          },
          "liquidity_rejections": 12,
          "max_drawdown": 0.21066599169813682,
          "closed_trade_drawdown": 0.15068000000000004,
          "cvar_5": -0.15068000000000004
        },
        "folds": {
          "train": {
            "signals": 5,
            "fills": 5,
            "wins": 4,
            "win_rate": 0.8,
            "average_net": 0.0018640000000000212,
            "profit_factor": 1.0618529333687292,
            "compound_net": -0.006415727820799799,
            "trades_per_day": 0.08333333333333333,
            "exits": {
              "STOP_MARKET": 1,
              "TP": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.21066599169813682,
            "closed_trade_drawdown": 0.15068000000000004,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.027755034429517433,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 2,
            "fills": 2,
            "wins": 2,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.08160000000000012,
            "trades_per_day": 0.03333333333333333,
            "exits": {
              "TP": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.15595757953836553,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.11764717874058395,
        "distance": 0
      },
      "BULL": {
        "metrics": {
          "signals": 550,
          "fills": 5,
          "wins": 5,
          "win_rate": 1.0,
          "average_net": 0.040000000000000036,
          "profit_factor": null,
          "compound_net": 0.21665290240000035,
          "trades_per_day": 0.027777777777777776,
          "exits": {
            "TP": 5
          },
          "liquidity_rejections": 12,
          "max_drawdown": 0.15595757953836553,
          "closed_trade_drawdown": 0.0,
          "cvar_5": 0.040000000000000036
        },
        "folds": {
          "train": {
            "signals": 2,
            "fills": 2,
            "wins": 2,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.08160000000000012,
            "trades_per_day": 0.03333333333333333,
            "exits": {
              "TP": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.04671457905544163,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 2,
            "fills": 2,
            "wins": 2,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.08160000000000012,
            "trades_per_day": 0.03333333333333333,
            "exits": {
              "TP": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.15595757953836553,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.05560098119378554,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.21665290240000035,
        "distance": 0
      }
    },
    "positive": true,
    "eligible": true,
    "score": [
      1,
      0,
      0.11764717874058395
    ],
    "worst_net": 0.11764717874058395,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788796207.9295616
  },
  {
    "id": "V15-d3b376d3df5b088d",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.04,
      "stop_net": -0.15,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 5,
      "min_hours_between_entries": 18,
      "drawdown_gate": -0.03,
      "spread_rate": 0.0,
      "entry_latency_minutes": 0,
      "btc_context_rule": "ANY",
      "sol_context_rule": "ANY",
      "vwap_deviation_gate": -0.001,
      "min_buy_share": 0.0,
      "min_buy_share_delta": -0.25,
      "min_green_candle_return": -0.1,
      "min_volume_ratio_20m": 0.25,
      "vwap_window_minutes": 10080,
      "drawdown_window_minutes": 2880,
      "volume_window_minutes": 1440,
      "pump_return_window_minutes": 120,
      "min_pump_return": -1.0,
      "max_pump_return": 1.0,
      "rsi_period_minutes": 14,
      "min_rsi": 0.0,
      "max_rsi": 100.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 1,
      "min_btc_return": -1.0,
      "max_btc_return": 1.0,
      "sol_return_window_minutes": 1,
      "min_sol_return": -1.0,
      "max_sol_return": 1.0,
      "utc_start_hour": 0,
      "utc_end_hour": 14,
      "trailing_stop_fraction": 0.25,
      "breakeven_trigger_net": 0.25,
      "entry_order_type": "LIMIT",
      "stop_exit_order_type": "STOP_MARKET",
      "entry_trigger_offset": 0.002,
      "stop_limit_buffer": 0.003,
      "client_cancel_latency_minutes": 1,
      "exit_distance_mode": "PERCENT",
      "target_atr_multiple": 3.0,
      "stop_atr_multiple": 2.0,
      "structure_window_minutes": 720,
      "min_rebound_from_low": 0.0,
      "max_rebound_from_low": 1.0,
      "buy_flow_window_minutes": 240,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -1.0,
      "max_relative_btc_return": 1.0,
      "min_relative_sol_return": -1.0,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 720,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 1000000.0,
      "min_average_trade_size_ratio": 0.5,
      "max_average_trade_size_ratio": 5.0,
      "min_cvd_ratio": -0.1,
      "max_cvd_ratio": 0.1,
      "min_realized_volatility": 0.0,
      "max_realized_volatility": 1.0,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 1.0,
      "min_illiquidity_ratio": 0.0,
      "max_illiquidity_ratio": 1000000.0,
      "macro_shock_rule": "OFF",
      "macro_shock_window_minutes": 5,
      "btc_shock_threshold": -1.0,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 735,
          "fills": 32,
          "wins": 28,
          "win_rate": 0.875,
          "average_net": 0.016165000000000034,
          "profit_factor": 1.8582426333952766,
          "compound_net": 0.5603387882180797,
          "trades_per_day": 0.17777777777777778,
          "exits": {
            "TP": 28,
            "STOP_MARKET": 4
          },
          "liquidity_rejections": 22,
          "max_drawdown": 0.28791903039417854,
          "closed_trade_drawdown": 0.24980175910400004,
          "cvar_5": -0.15068000000000004
        },
        "folds": {
          "train": {
            "signals": 16,
            "fills": 16,
            "wins": 15,
            "win_rate": 0.9375,
            "average_net": 0.02808250000000003,
            "profit_factor": 3.9819485001327344,
            "compound_net": 0.5295773380971345,
            "trades_per_day": 0.26666666666666666,
            "exits": {
              "TP": 15,
              "STOP_MARKET": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.22562417481627295,
            "closed_trade_drawdown": 0.15068000000000004,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 8,
            "fills": 8,
            "wins": 7,
            "win_rate": 0.875,
            "average_net": 0.01616500000000004,
            "profit_factor": 1.8582426333952773,
            "compound_net": 0.11764717874058395,
            "trades_per_day": 0.13333333333333333,
            "exits": {
              "STOP_MARKET": 1,
              "TP": 7
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2528099225386934,
            "closed_trade_drawdown": 0.15067999999999993,
            "cvar_5": -0.15067999999999993,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 8,
            "fills": 8,
            "wins": 6,
            "win_rate": 0.75,
            "average_net": -0.0076699999999999685,
            "profit_factor": 0.7963897000265472,
            "compound_net": -0.08726913283850712,
            "trades_per_day": 0.13333333333333333,
            "exits": {
              "TP": 6,
              "STOP_MARKET": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.28791903039417843,
            "closed_trade_drawdown": 0.24980175910399993,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.5603387882180797,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 607,
          "fills": 8,
          "wins": 7,
          "win_rate": 0.875,
          "average_net": 0.016165000000000027,
          "profit_factor": 1.858242633395276,
          "compound_net": 0.11764717874058395,
          "trades_per_day": 0.044444444444444446,
          "exits": {
            "STOP_MARKET": 1,
            "TP": 7
          },
          "liquidity_rejections": 13,
          "max_drawdown": 0.21066599169813682,
          "closed_trade_drawdown": 0.15068000000000004,
          "cvar_5": -0.15068000000000004
        },
        "folds": {
          "train": {
            "signals": 5,
            "fills": 5,
            "wins": 4,
            "win_rate": 0.8,
            "average_net": 0.0018640000000000212,
            "profit_factor": 1.0618529333687292,
            "compound_net": -0.006415727820799799,
            "trades_per_day": 0.08333333333333333,
            "exits": {
              "STOP_MARKET": 1,
              "TP": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.21066599169813682,
            "closed_trade_drawdown": 0.15068000000000004,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.027755034429517433,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 2,
            "fills": 2,
            "wins": 2,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.08160000000000012,
            "trades_per_day": 0.03333333333333333,
            "exits": {
              "TP": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.15595757953836553,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.11764717874058395,
        "distance": 0
      },
      "BULL": {
        "metrics": {
          "signals": 567,
          "fills": 5,
          "wins": 5,
          "win_rate": 1.0,
          "average_net": 0.040000000000000036,
          "profit_factor": null,
          "compound_net": 0.21665290240000035,
          "trades_per_day": 0.027777777777777776,
          "exits": {
            "TP": 5
          },
          "liquidity_rejections": 12,
          "max_drawdown": 0.15595757953836553,
          "closed_trade_drawdown": 0.0,
          "cvar_5": 0.040000000000000036
        },
        "folds": {
          "train": {
            "signals": 2,
            "fills": 2,
            "wins": 2,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.08160000000000012,
            "trades_per_day": 0.03333333333333333,
            "exits": {
              "TP": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.04671457905544163,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 2,
            "fills": 2,
            "wins": 2,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.08160000000000012,
            "trades_per_day": 0.03333333333333333,
            "exits": {
              "TP": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.15595757953836564,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.05560098119378554,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.21665290240000035,
        "distance": 0
      }
    },
    "positive": true,
    "eligible": true,
    "score": [
      1,
      0,
      0.11764717874058395
    ],
    "worst_net": 0.11764717874058395,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788796219.4824922
  },
  {
    "id": "V15-7714cd027fe7b955",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.04,
      "stop_net": -0.15,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 5,
      "min_hours_between_entries": 18,
      "drawdown_gate": -0.03,
      "spread_rate": 0.0,
      "entry_latency_minutes": 0,
      "btc_context_rule": "ANY",
      "sol_context_rule": "ANY",
      "vwap_deviation_gate": -0.01,
      "min_buy_share": 0.0,
      "min_buy_share_delta": -0.25,
      "min_green_candle_return": -0.1,
      "min_volume_ratio_20m": 0.25,
      "vwap_window_minutes": 10080,
      "drawdown_window_minutes": 2880,
      "volume_window_minutes": 720,
      "pump_return_window_minutes": 15,
      "min_pump_return": -1.0,
      "max_pump_return": 1.0,
      "rsi_period_minutes": 14,
      "min_rsi": 0.0,
      "max_rsi": 100.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 15,
      "min_btc_return": -1.0,
      "max_btc_return": 1.0,
      "sol_return_window_minutes": 1,
      "min_sol_return": -1.0,
      "max_sol_return": 1.0,
      "utc_start_hour": 0,
      "utc_end_hour": 14,
      "trailing_stop_fraction": 0.15,
      "breakeven_trigger_net": 0.25,
      "entry_order_type": "LIMIT",
      "stop_exit_order_type": "STOP_LIMIT",
      "entry_trigger_offset": 0.002,
      "stop_limit_buffer": 0.005,
      "client_cancel_latency_minutes": 1,
      "exit_distance_mode": "PERCENT",
      "target_atr_multiple": 3.0,
      "stop_atr_multiple": 2.0,
      "structure_window_minutes": 720,
      "min_rebound_from_low": 0.0,
      "max_rebound_from_low": 1.0,
      "buy_flow_window_minutes": 240,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -1.0,
      "max_relative_btc_return": 1.0,
      "min_relative_sol_return": -1.0,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 720,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 1000000.0,
      "min_average_trade_size_ratio": 0.5,
      "max_average_trade_size_ratio": 1000000.0,
      "min_cvd_ratio": -0.1,
      "max_cvd_ratio": 0.1,
      "min_realized_volatility": 0.0,
      "max_realized_volatility": 1.0,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 1.0,
      "min_illiquidity_ratio": 0.0,
      "max_illiquidity_ratio": 1000000.0,
      "macro_shock_rule": "OFF",
      "macro_shock_window_minutes": 5,
      "btc_shock_threshold": -1.0,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 762,
          "fills": 34,
          "wins": 29,
          "win_rate": 0.8529411764705882,
          "average_net": 0.01513573177048986,
          "profit_factor": 1.7973764259601503,
          "compound_net": 0.5617104167016351,
          "trades_per_day": 0.18888888888888888,
          "exits": {
            "TP": 29,
            "STOP_LIMIT_FILLED": 5
          },
          "liquidity_rejections": 18,
          "max_drawdown": 0.2627637836666683,
          "closed_trade_drawdown": 0.24937570723616398,
          "cvar_5": -0.14454416531457442
        },
        "folds": {
          "train": {
            "signals": 18,
            "fills": 18,
            "wins": 16,
            "win_rate": 0.8888888888888888,
            "average_net": 0.02232379883482836,
            "profit_factor": 2.687137944416607,
            "compound_net": 0.4533689236050491,
            "trades_per_day": 0.3,
            "exits": {
              "TP": 16,
              "STOP_LIMIT_FILLED": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.20398068817645432,
            "closed_trade_drawdown": 0.16071566047053532,
            "cvar_5": -0.1257385306580665,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 9,
            "fills": 9,
            "wins": 7,
            "win_rate": 0.7777777777777778,
            "average_net": 0.0014492861767395862,
            "profit_factor": 1.0488603172578292,
            "compound_net": -0.012229638885641103,
            "trades_per_day": 0.15,
            "exits": {
              "STOP_LIMIT_FILLED": 2,
              "TP": 7
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2627637836666683,
            "closed_trade_drawdown": 0.24937570723616398,
            "cvar_5": -0.14883125620823712,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 6,
            "win_rate": 0.8571428571428571,
            "average_net": 0.014248989368441212,
            "profit_factor": 1.7111436338659096,
            "compound_net": 0.08784907475261172,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 6,
              "STOP_LIMIT_FILLED": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.24518578933674873,
            "closed_trade_drawdown": 0.14025707442091173,
            "cvar_5": -0.14025707442091173,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.5617104167016351,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 632,
          "fills": 4,
          "wins": 4,
          "win_rate": 1.0,
          "average_net": 0.040000000000000036,
          "profit_factor": null,
          "compound_net": 0.1698585600000002,
          "trades_per_day": 0.022222222222222223,
          "exits": {
            "TP": 4
          },
          "liquidity_rejections": 13,
          "max_drawdown": 0.05560098119378576,
          "closed_trade_drawdown": 0.0,
          "cvar_5": 0.040000000000000036
        },
        "folds": {
          "train": {
            "signals": 2,
            "fills": 2,
            "wins": 2,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.08160000000000012,
            "trades_per_day": 0.03333333333333333,
            "exits": {
              "TP": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.0440602342442834,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.03004941782909376,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.05560098119378576,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.1698585600000002,
        "distance": 0
      },
      "BULL": {
        "metrics": {
          "signals": 540,
          "fills": 2,
          "wins": 2,
          "win_rate": 1.0,
          "average_net": 0.040000000000000036,
          "profit_factor": null,
          "compound_net": 0.08160000000000012,
          "trades_per_day": 0.011111111111111112,
          "exits": {
            "TP": 2
          },
          "liquidity_rejections": 12,
          "max_drawdown": 0.05560098119378565,
          "closed_trade_drawdown": 0.0,
          "cvar_5": 0.040000000000000036
        },
        "folds": {
          "train": {
            "signals": 0,
            "fills": 0,
            "wins": 0,
            "win_rate": 0.0,
            "average_net": 0.0,
            "profit_factor": null,
            "compound_net": 0.0,
            "trades_per_day": 0.0,
            "exits": {},
            "liquidity_rejections": 0,
            "max_drawdown": 0.0,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.0,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.03004941782909365,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.05560098119378565,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.08160000000000012,
        "distance": 0
      }
    },
    "positive": true,
    "eligible": true,
    "score": [
      1,
      0,
      0.08160000000000012
    ],
    "worst_net": 0.08160000000000012,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788796207.9267917
  },
  {
    "id": "V15-b26e1bf2a88feb3a",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.04,
      "stop_net": -0.15,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 5,
      "min_hours_between_entries": 18,
      "drawdown_gate": -0.03,
      "spread_rate": 0.0,
      "entry_latency_minutes": 0,
      "btc_context_rule": "ANY",
      "sol_context_rule": "ANY",
      "vwap_deviation_gate": -0.01,
      "min_buy_share": 0.0,
      "min_buy_share_delta": -0.25,
      "min_green_candle_return": -0.1,
      "min_volume_ratio_20m": 0.25,
      "vwap_window_minutes": 10080,
      "drawdown_window_minutes": 2880,
      "volume_window_minutes": 240,
      "pump_return_window_minutes": 15,
      "min_pump_return": -1.0,
      "max_pump_return": 1.0,
      "rsi_period_minutes": 14,
      "min_rsi": 0.0,
      "max_rsi": 100.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 15,
      "min_btc_return": -1.0,
      "max_btc_return": 1.0,
      "sol_return_window_minutes": 1,
      "min_sol_return": -1.0,
      "max_sol_return": 1.0,
      "utc_start_hour": 0,
      "utc_end_hour": 14,
      "trailing_stop_fraction": 0.15,
      "breakeven_trigger_net": 0.25,
      "entry_order_type": "LIMIT",
      "stop_exit_order_type": "STOP_LIMIT",
      "entry_trigger_offset": 0.002,
      "stop_limit_buffer": 0.005,
      "client_cancel_latency_minutes": 1,
      "exit_distance_mode": "PERCENT",
      "target_atr_multiple": 3.0,
      "stop_atr_multiple": 2.0,
      "structure_window_minutes": 720,
      "min_rebound_from_low": 0.0,
      "max_rebound_from_low": 1.0,
      "buy_flow_window_minutes": 240,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 1.0,
      "min_relative_sol_return": -1.0,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 720,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 1000000.0,
      "min_average_trade_size_ratio": 0.5,
      "max_average_trade_size_ratio": 5.0,
      "min_cvd_ratio": -0.1,
      "max_cvd_ratio": 0.1,
      "min_realized_volatility": 0.0,
      "max_realized_volatility": 1.0,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 1.0,
      "min_illiquidity_ratio": 0.0,
      "max_illiquidity_ratio": 1000000.0,
      "macro_shock_rule": "OFF",
      "macro_shock_window_minutes": 5,
      "btc_shock_threshold": -1.0,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 786,
          "fills": 34,
          "wins": 29,
          "win_rate": 0.8529411764705882,
          "average_net": 0.01513573177048986,
          "profit_factor": 1.7973764259601503,
          "compound_net": 0.5617104167016356,
          "trades_per_day": 0.18888888888888888,
          "exits": {
            "TP": 29,
            "STOP_LIMIT_FILLED": 5
          },
          "liquidity_rejections": 20,
          "max_drawdown": 0.2627637836666683,
          "closed_trade_drawdown": 0.24937570723616398,
          "cvar_5": -0.14454416531457442
        },
        "folds": {
          "train": {
            "signals": 18,
            "fills": 18,
            "wins": 16,
            "win_rate": 0.8888888888888888,
            "average_net": 0.02232379883482836,
            "profit_factor": 2.687137944416607,
            "compound_net": 0.4533689236050491,
            "trades_per_day": 0.3,
            "exits": {
              "TP": 16,
              "STOP_LIMIT_FILLED": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.20398068817645432,
            "closed_trade_drawdown": 0.16071566047053532,
            "cvar_5": -0.1257385306580665,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 9,
            "fills": 9,
            "wins": 7,
            "win_rate": 0.7777777777777778,
            "average_net": 0.0014492861767395862,
            "profit_factor": 1.0488603172578292,
            "compound_net": -0.012229638885641103,
            "trades_per_day": 0.15,
            "exits": {
              "STOP_LIMIT_FILLED": 2,
              "TP": 7
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2627637836666683,
            "closed_trade_drawdown": 0.24937570723616398,
            "cvar_5": -0.14883125620823712,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 6,
            "win_rate": 0.8571428571428571,
            "average_net": 0.014248989368441212,
            "profit_factor": 1.7111436338659096,
            "compound_net": 0.08784907475261217,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 6,
              "STOP_LIMIT_FILLED": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.24518578933674873,
            "closed_trade_drawdown": 0.14025707442091173,
            "cvar_5": -0.14025707442091173,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.5617104167016356,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 600,
          "fills": 5,
          "wins": 4,
          "win_rate": 0.8,
          "average_net": 0.030534504453647048,
          "profit_factor": 21.835617364812176,
          "compound_net": 0.1612864474522857,
          "trades_per_day": 0.027777777777777776,
          "exits": {
            "TP": 4,
            "TIME": 1
          },
          "liquidity_rejections": 16,
          "max_drawdown": 0.13433637829124112,
          "closed_trade_drawdown": 0.007327477731764898,
          "cvar_5": -0.007327477731764898
        },
        "folds": {
          "train": {
            "signals": 2,
            "fills": 2,
            "wins": 2,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.08160000000000012,
            "trades_per_day": 0.03333333333333333,
            "exits": {
              "TP": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.0440602342442834,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 2,
            "fills": 2,
            "wins": 1,
            "win_rate": 0.5,
            "average_net": 0.01633626113411757,
            "profit_factor": 5.458904341203044,
            "compound_net": 0.03237942315896447,
            "trades_per_day": 0.03333333333333333,
            "exits": {
              "TIME": 1,
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.13433637829124112,
            "closed_trade_drawdown": 0.007327477731764898,
            "cvar_5": -0.007327477731764898,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.05560098119378565,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.1612864474522857,
        "distance": 0
      },
      "BULL": {
        "metrics": {
          "signals": 507,
          "fills": 3,
          "wins": 2,
          "win_rate": 0.6666666666666666,
          "average_net": 0.024224174089411726,
          "profit_factor": 10.917808682406088,
          "compound_net": 0.073674600085323,
          "trades_per_day": 0.016666666666666666,
          "exits": {
            "TIME": 1,
            "TP": 2
          },
          "liquidity_rejections": 14,
          "max_drawdown": 0.13433637829124112,
          "closed_trade_drawdown": 0.007327477731764898,
          "cvar_5": -0.007327477731764898
        },
        "folds": {
          "train": {
            "signals": 0,
            "fills": 0,
            "wins": 0,
            "win_rate": 0.0,
            "average_net": 0.0,
            "profit_factor": null,
            "compound_net": 0.0,
            "trades_per_day": 0.0,
            "exits": {},
            "liquidity_rejections": 0,
            "max_drawdown": 0.0,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.0,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 2,
            "fills": 2,
            "wins": 1,
            "win_rate": 0.5,
            "average_net": 0.01633626113411757,
            "profit_factor": 5.458904341203044,
            "compound_net": 0.03237942315896447,
            "trades_per_day": 0.03333333333333333,
            "exits": {
              "TIME": 1,
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.13433637829124112,
            "closed_trade_drawdown": 0.007327477731764898,
            "cvar_5": -0.007327477731764898,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.05560098119378565,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.073674600085323,
        "distance": 0
      }
    },
    "positive": true,
    "eligible": true,
    "score": [
      1,
      0,
      0.073674600085323
    ],
    "worst_net": 0.073674600085323,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788796207.9278283
  },
  {
    "id": "V15-4708c3a4c239719a",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.04,
      "stop_net": -0.15,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 5,
      "min_hours_between_entries": 18,
      "drawdown_gate": -0.03,
      "spread_rate": 0.0,
      "entry_latency_minutes": 0,
      "btc_context_rule": "ANY",
      "sol_context_rule": "ANY",
      "vwap_deviation_gate": -0.006,
      "min_buy_share": 0.0,
      "min_buy_share_delta": -0.25,
      "min_green_candle_return": -0.1,
      "min_volume_ratio_20m": 0.25,
      "vwap_window_minutes": 10080,
      "drawdown_window_minutes": 2880,
      "volume_window_minutes": 1440,
      "pump_return_window_minutes": 15,
      "min_pump_return": -1.0,
      "max_pump_return": 1.0,
      "rsi_period_minutes": 14,
      "min_rsi": 0.0,
      "max_rsi": 100.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 1,
      "min_btc_return": -1.0,
      "max_btc_return": 1.0,
      "sol_return_window_minutes": 1,
      "min_sol_return": -1.0,
      "max_sol_return": 1.0,
      "utc_start_hour": 0,
      "utc_end_hour": 14,
      "trailing_stop_fraction": 0.15,
      "breakeven_trigger_net": 0.25,
      "entry_order_type": "LIMIT",
      "stop_exit_order_type": "STOP_LIMIT",
      "entry_trigger_offset": 0.002,
      "stop_limit_buffer": 0.005,
      "client_cancel_latency_minutes": 1,
      "exit_distance_mode": "PERCENT",
      "target_atr_multiple": 3.0,
      "stop_atr_multiple": 2.0,
      "structure_window_minutes": 720,
      "min_rebound_from_low": 0.0,
      "max_rebound_from_low": 1.0,
      "buy_flow_window_minutes": 240,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 1.0,
      "min_relative_sol_return": -1.0,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 720,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 1000000.0,
      "min_average_trade_size_ratio": 0.5,
      "max_average_trade_size_ratio": 5.0,
      "min_cvd_ratio": -0.1,
      "max_cvd_ratio": 0.1,
      "min_realized_volatility": 0.0,
      "max_realized_volatility": 1.0,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 1.0,
      "min_illiquidity_ratio": 0.0,
      "max_illiquidity_ratio": 1000000.0,
      "macro_shock_rule": "OFF",
      "macro_shock_window_minutes": 5,
      "btc_shock_threshold": -1.0,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 752,
          "fills": 33,
          "wins": 28,
          "win_rate": 0.8484848484848485,
          "average_net": 0.014517647912952012,
          "profit_factor": 1.7474944782609554,
          "compound_net": 0.5093180781459272,
          "trades_per_day": 0.18333333333333332,
          "exits": {
            "TP": 28,
            "STOP_LIMIT_FILLED": 5
          },
          "liquidity_rejections": 20,
          "max_drawdown": 0.2627637836666682,
          "closed_trade_drawdown": 0.24937570723616398,
          "cvar_5": -0.14454416531457442
        },
        "folds": {
          "train": {
            "signals": 18,
            "fills": 18,
            "wins": 17,
            "win_rate": 0.9444444444444444,
            "average_net": 0.03153149498249872,
            "profit_factor": 6.048041533811131,
            "compound_net": 0.7288920232147229,
            "trades_per_day": 0.3,
            "exits": {
              "TP": 17,
              "STOP_LIMIT_FILLED": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.15838226767673302,
            "closed_trade_drawdown": 0.11243309031502358,
            "cvar_5": -0.11243309031502358,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 8,
            "fills": 8,
            "wins": 6,
            "win_rate": 0.75,
            "average_net": -0.00336955305116797,
            "profit_factor": 0.8990231290781394,
            "compound_net": -0.05022080662080852,
            "trades_per_day": 0.13333333333333333,
            "exits": {
              "STOP_LIMIT_FILLED": 2,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2627637836666682,
            "closed_trade_drawdown": 0.24937570723616398,
            "cvar_5": -0.14883125620823712,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 5,
            "win_rate": 0.7142857142857143,
            "average_net": -0.00878972916403098,
            "profit_factor": 0.7647361672711597,
            "compound_net": -0.08084182954859032,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 5,
              "STOP_LIMIT_FILLED": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.24960804340212417,
            "closed_trade_drawdown": 0.2142997436789199,
            "cvar_5": -0.14025707442091173,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.5093180781459272,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 625,
          "fills": 6,
          "wins": 5,
          "win_rate": 0.8333333333333334,
          "average_net": 0.013121495045449147,
          "profit_factor": 1.6491984973635325,
          "compound_net": 0.06910815210523769,
          "trades_per_day": 0.03333333333333333,
          "exits": {
            "STOP_LIMIT_FILLED": 1,
            "TP": 5
          },
          "liquidity_rejections": 13,
          "max_drawdown": 0.17828286037474372,
          "closed_trade_drawdown": 0.1212710297273053,
          "cvar_5": -0.1212710297273053
        },
        "folds": {
          "train": {
            "signals": 4,
            "fills": 4,
            "wins": 3,
            "win_rate": 0.75,
            "average_net": -0.0003177574318262988,
            "profit_factor": 0.9895190984181195,
            "compound_net": -0.01154941558317546,
            "trades_per_day": 0.06666666666666667,
            "exits": {
              "STOP_LIMIT_FILLED": 1,
              "TP": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.17828286037474372,
            "closed_trade_drawdown": 0.1212710297273053,
            "cvar_5": -0.1212710297273053,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.03004941782909365,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.05560098119378565,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.06910815210523769,
        "distance": 0
      },
      "BULL": {
        "metrics": {
          "signals": 564,
          "fills": 4,
          "wins": 4,
          "win_rate": 1.0,
          "average_net": 0.040000000000000036,
          "profit_factor": null,
          "compound_net": 0.1698585600000002,
          "trades_per_day": 0.022222222222222223,
          "exits": {
            "TP": 4
          },
          "liquidity_rejections": 13,
          "max_drawdown": 0.05560098119378576,
          "closed_trade_drawdown": 0.0,
          "cvar_5": 0.040000000000000036
        },
        "folds": {
          "train": {
            "signals": 2,
            "fills": 2,
            "wins": 2,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.08160000000000012,
            "trades_per_day": 0.03333333333333333,
            "exits": {
              "TP": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.04671457905544152,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.03004941782909376,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.05560098119378576,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.1698585600000002,
        "distance": 0
      }
    },
    "positive": true,
    "eligible": true,
    "score": [
      1,
      0,
      0.06910815210523769
    ],
    "worst_net": 0.06910815210523769,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788796219.4813817
  },
  {
    "id": "V15-cf16c3d422084089",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.04,
      "stop_net": -0.15,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 5,
      "min_hours_between_entries": 24,
      "drawdown_gate": -0.03,
      "spread_rate": 0.0,
      "entry_latency_minutes": 0,
      "btc_context_rule": "ANY",
      "sol_context_rule": "ANY",
      "vwap_deviation_gate": -0.006,
      "min_buy_share": 0.0,
      "min_buy_share_delta": -0.25,
      "min_green_candle_return": -0.1,
      "min_volume_ratio_20m": 0.25,
      "vwap_window_minutes": 10080,
      "drawdown_window_minutes": 2880,
      "volume_window_minutes": 1440,
      "pump_return_window_minutes": 1,
      "min_pump_return": -1.0,
      "max_pump_return": 1.0,
      "rsi_period_minutes": 14,
      "min_rsi": 0.0,
      "max_rsi": 100.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 15,
      "min_btc_return": -1.0,
      "max_btc_return": 1.0,
      "sol_return_window_minutes": 1,
      "min_sol_return": -1.0,
      "max_sol_return": 1.0,
      "utc_start_hour": 0,
      "utc_end_hour": 14,
      "trailing_stop_fraction": 0.15,
      "breakeven_trigger_net": 0.15,
      "entry_order_type": "LIMIT",
      "stop_exit_order_type": "STOP_MARKET",
      "entry_trigger_offset": 0.002,
      "stop_limit_buffer": 0.003,
      "client_cancel_latency_minutes": 1,
      "exit_distance_mode": "PERCENT",
      "target_atr_multiple": 3.0,
      "stop_atr_multiple": 2.0,
      "structure_window_minutes": 720,
      "min_rebound_from_low": 0.0,
      "max_rebound_from_low": 1.0,
      "buy_flow_window_minutes": 240,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -1.0,
      "max_relative_btc_return": 1.0,
      "min_relative_sol_return": -1.0,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 720,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 1000000.0,
      "min_average_trade_size_ratio": 0.75,
      "max_average_trade_size_ratio": 5.0,
      "min_cvd_ratio": -0.1,
      "max_cvd_ratio": 0.25,
      "min_realized_volatility": 0.0,
      "max_realized_volatility": 1.0,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 1.0,
      "min_illiquidity_ratio": 0.0,
      "max_illiquidity_ratio": 1000000.0,
      "macro_shock_rule": "OFF",
      "macro_shock_window_minutes": 5,
      "btc_shock_threshold": -1.0,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 762,
          "fills": 31,
          "wins": 27,
          "win_rate": 0.8709677419354839,
          "average_net": 0.01771880250431495,
          "profit_factor": 2.034982393604995,
          "compound_net": 0.6308078663637748,
          "trades_per_day": 0.17222222222222222,
          "exits": {
            "TP": 27,
            "TRAIL": 3,
            "STOP_MARKET": 1
          },
          "liquidity_rejections": 19,
          "max_drawdown": 0.2498033502034901,
          "closed_trade_drawdown": 0.21580258037776945,
          "cvar_5": -0.1457594586079176
        },
        "folds": {
          "train": {
            "signals": 17,
            "fills": 17,
            "wins": 16,
            "win_rate": 0.9411764705882353,
            "average_net": 0.030774096720600778,
            "profit_factor": 5.47755949468825,
            "compound_net": 0.6541414506637488,
            "trades_per_day": 0.2833333333333333,
            "exits": {
              "TP": 16,
              "TRAIL": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1488350845134765,
            "closed_trade_drawdown": 0.11684035574978735,
            "cvar_5": -0.11684035574978735,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 7,
            "fills": 7,
            "wins": 6,
            "win_rate": 0.8571428571428571,
            "average_net": 0.01276000000000001,
            "profit_factor": 1.5927794000530926,
            "compound_net": 0.07466074878902274,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "STOP_MARKET": 1,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.20876229265244872,
            "closed_trade_drawdown": 0.15068000000000015,
            "cvar_5": -0.15068000000000015,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 5,
            "win_rate": 0.7142857142857143,
            "average_net": -0.009028109516635685,
            "profit_factor": 0.7598877545918151,
            "compound_net": -0.08259993592502157,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 5,
              "TRAIL": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2498033502034902,
            "closed_trade_drawdown": 0.21580258037776967,
            "cvar_5": -0.14083891721583508,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.6308078663637748,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 661,
          "fills": 6,
          "wins": 5,
          "win_rate": 0.8333333333333334,
          "average_net": 0.01294035843323088,
          "profit_factor": 1.6345498141698713,
          "compound_net": 0.06778586979531998,
          "trades_per_day": 0.03333333333333333,
          "exits": {
            "TRAIL": 1,
            "TP": 5
          },
          "liquidity_rejections": 11,
          "max_drawdown": 0.17794039110439397,
          "closed_trade_drawdown": 0.1223578494006149,
          "cvar_5": -0.1223578494006149
        },
        "folds": {
          "train": {
            "signals": 4,
            "fills": 4,
            "wins": 3,
            "win_rate": 0.75,
            "average_net": -0.0005894623501536966,
            "profit_factor": 0.9807298885019228,
            "compound_net": -0.012771939908173158,
            "trades_per_day": 0.06666666666666667,
            "exits": {
              "TRAIL": 1,
              "TP": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.17794039110439397,
            "closed_trade_drawdown": 0.1223578494006149,
            "cvar_5": -0.1223578494006149,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.03004941782909365,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.05560098119378565,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.06778586979531998,
        "distance": 0
      },
      "BULL": {
        "metrics": {
          "signals": 599,
          "fills": 4,
          "wins": 4,
          "win_rate": 1.0,
          "average_net": 0.040000000000000036,
          "profit_factor": null,
          "compound_net": 0.1698585600000002,
          "trades_per_day": 0.022222222222222223,
          "exits": {
            "TP": 4
          },
          "liquidity_rejections": 11,
          "max_drawdown": 0.05560098119378576,
          "closed_trade_drawdown": 0.0,
          "cvar_5": 0.040000000000000036
        },
        "folds": {
          "train": {
            "signals": 2,
            "fills": 2,
            "wins": 2,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.08160000000000012,
            "trades_per_day": 0.03333333333333333,
            "exits": {
              "TP": 2
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.04671457905544152,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.03004941782909376,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 1,
            "fills": 1,
            "wins": 1,
            "win_rate": 1.0,
            "average_net": 0.040000000000000036,
            "profit_factor": null,
            "compound_net": 0.040000000000000036,
            "trades_per_day": 0.016666666666666666,
            "exits": {
              "TP": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.05560098119378576,
            "closed_trade_drawdown": 0.0,
            "cvar_5": 0.040000000000000036,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.1698585600000002,
        "distance": 0
      }
    },
    "positive": true,
    "eligible": true,
    "score": [
      1,
      0,
      0.06778586979531998
    ],
    "worst_net": 0.06778586979531998,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788796219.4819572
  }
]

## ИИ

ИИ недоступен; CPU продолжает: <urlopen error [WinError 10061] Es konnte keine Verbindung hergestellt werden, da der Zielcomputer die Verbindung verweigerte>

## Архив

F:\PUMP Research Lab\experiments\v15-59b126eb7bd78c8c\research.sqlite3