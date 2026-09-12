# PUMP Research Lab V16

Сохранено — остановлено

Проверено: 27481; прибыльных на всех выбранных окнах: 16451.

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
    "id": "V16-fc6d22261c18944a",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.15,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 1,
      "min_hours_between_entries": 6,
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
      "min_pump_return": -0.15,
      "max_pump_return": 0.06,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 5,
      "min_btc_return": -0.1,
      "max_btc_return": 0.15,
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
      "max_rebound_from_low": 0.15,
      "buy_flow_window_minutes": 120,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 1.0,
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
      "btc_shock_threshold": -0.04,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1173,
          "fills": 42,
          "wins": 24,
          "win_rate": 0.5714285714285714,
          "average_net": 0.017522857142857163,
          "profit_factor": 2.585983966899406,
          "compound_net": 0.9522867708550757,
          "trades_per_day": 0.23333333333333334,
          "exits": {
            "TP": 24,
            "STOP_MARKET": 3,
            "TRAIL": 15
          },
          "liquidity_rejections": 45,
          "max_drawdown": 0.18398488745382702,
          "closed_trade_drawdown": 0.15135945600000011,
          "cvar_5": -0.15068000000000004
        },
        "folds": {
          "train": {
            "signals": 21,
            "fills": 21,
            "wins": 14,
            "win_rate": 0.6666666666666666,
            "average_net": 0.018792380952380977,
            "profit_factor": 2.2923762116845703,
            "compound_net": 0.42250897765736406,
            "trades_per_day": 0.35,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 5
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1793013770158084,
            "closed_trade_drawdown": 0.15135945600000011,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 14,
            "fills": 14,
            "wins": 7,
            "win_rate": 0.5,
            "average_net": 0.01389428571428574,
            "profit_factor": 2.2510933882171367,
            "compound_net": 0.18935361454538202,
            "trades_per_day": 0.23333333333333334,
            "exits": {
              "STOP_MARKET": 1,
              "TP": 7,
              "TRAIL": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.18398488745382702,
            "closed_trade_drawdown": 0.15068000000000004,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9522867708550757,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1240,
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
          "signals": 1087,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788902660.5951822
  },
  {
    "id": "V16-eb107b3f603e9779",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.3,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 1,
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
      "min_pump_return": -1.0,
      "max_pump_return": 0.06,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 5,
      "min_btc_return": -0.1,
      "max_btc_return": 0.15,
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
      "max_rebound_from_low": 0.15,
      "buy_flow_window_minutes": 120,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 1.0,
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
      "btc_shock_threshold": -0.04,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1080,
          "fills": 35,
          "wins": 21,
          "win_rate": 0.6,
          "average_net": 0.02261365503558397,
          "profit_factor": 4.06154872870492,
          "compound_net": 1.0730231173012124,
          "trades_per_day": 0.19444444444444445,
          "exits": {
            "TP": 21,
            "TRAIL": 14
          },
          "liquidity_rejections": 49,
          "max_drawdown": 0.2596144429498448,
          "closed_trade_drawdown": 0.24812207375456186,
          "cvar_5": -0.124461036877281
        },
        "folds": {
          "train": {
            "signals": 16,
            "fills": 16,
            "wins": 12,
            "win_rate": 0.75,
            "average_net": 0.02184237039033992,
            "profit_factor": 2.3949985364875457,
            "compound_net": 0.3470266867086309,
            "trades_per_day": 0.26666666666666666,
            "exits": {
              "TP": 12,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2596144429498448,
            "closed_trade_drawdown": 0.24812207375456186,
            "cvar_5": -0.24812207375456186,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 1.0730231173012124,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788902820.8165443
  },
  {
    "id": "V16-e557fb1898c03522",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.5,
      "max_hold_minutes": 43200,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 2,
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
      "min_sol_return": -0.25,
      "max_sol_return": 0.1,
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
          "signals": 1085,
          "fills": 35,
          "wins": 21,
          "win_rate": 0.6,
          "average_net": 0.02261365503558397,
          "profit_factor": 4.06154872870492,
          "compound_net": 1.0730231173012124,
          "trades_per_day": 0.19444444444444445,
          "exits": {
            "TP": 21,
            "TRAIL": 14
          },
          "liquidity_rejections": 50,
          "max_drawdown": 0.2596144429498448,
          "closed_trade_drawdown": 0.24812207375456186,
          "cvar_5": -0.124461036877281
        },
        "folds": {
          "train": {
            "signals": 16,
            "fills": 16,
            "wins": 12,
            "win_rate": 0.75,
            "average_net": 0.02184237039033992,
            "profit_factor": 2.3949985364875457,
            "compound_net": 0.3470266867086309,
            "trades_per_day": 0.26666666666666666,
            "exits": {
              "TP": 12,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2596144429498448,
            "closed_trade_drawdown": 0.24812207375456186,
            "cvar_5": -0.24812207375456186,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 1.0730231173012124,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788902870.681755
  },
  {
    "id": "V16-d35a650744b408ad",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.2,
      "max_hold_minutes": 28800,
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
          "signals": 1101,
          "fills": 38,
          "wins": 23,
          "win_rate": 0.6052631578947368,
          "average_net": 0.019429473684210552,
          "profit_factor": 2.793431791682863,
          "compound_net": 0.9423175445857954,
          "trades_per_day": 0.2111111111111111,
          "exits": {
            "TP": 23,
            "STOP_MARKET": 2,
            "TRAIL": 13
          },
          "liquidity_rejections": 49,
          "max_drawdown": 0.21285812733060927,
          "closed_trade_drawdown": 0.20063999999999993,
          "cvar_5": -0.20063999999999993
        },
        "folds": {
          "train": {
            "signals": 19,
            "fills": 19,
            "wins": 14,
            "win_rate": 0.7368421052631579,
            "average_net": 0.015595789473684252,
            "profit_factor": 1.7340467697185913,
            "compound_net": 0.2620957020612369,
            "trades_per_day": 0.31666666666666665,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.21285812733060927,
            "closed_trade_drawdown": 0.20063999999999993,
            "cvar_5": -0.20063999999999993,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9423175445857954,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788903047.5855434
  },
  {
    "id": "V16-cb8bc28baf3178b9",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.2,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 1,
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
      "min_pump_return": -0.15,
      "max_pump_return": 0.06,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 5,
      "min_btc_return": -0.1,
      "max_btc_return": 0.15,
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
      "max_rebound_from_low": 0.15,
      "buy_flow_window_minutes": 120,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 1.0,
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
      "btc_shock_threshold": -0.04,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1098,
          "fills": 38,
          "wins": 23,
          "win_rate": 0.6052631578947368,
          "average_net": 0.019429473684210552,
          "profit_factor": 2.793431791682863,
          "compound_net": 0.9423175445857954,
          "trades_per_day": 0.2111111111111111,
          "exits": {
            "TP": 23,
            "STOP_MARKET": 2,
            "TRAIL": 13
          },
          "liquidity_rejections": 49,
          "max_drawdown": 0.21285812733060927,
          "closed_trade_drawdown": 0.20063999999999993,
          "cvar_5": -0.20063999999999993
        },
        "folds": {
          "train": {
            "signals": 19,
            "fills": 19,
            "wins": 14,
            "win_rate": 0.7368421052631579,
            "average_net": 0.015595789473684252,
            "profit_factor": 1.7340467697185913,
            "compound_net": 0.2620957020612369,
            "trades_per_day": 0.31666666666666665,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.21285812733060927,
            "closed_trade_drawdown": 0.20063999999999993,
            "cvar_5": -0.20063999999999993,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9423175445857954,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788903133.3423755
  },
  {
    "id": "V16-649a5d4af82e515f",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.15,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 1,
      "min_hours_between_entries": 12,
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
      "min_pump_return": -0.15,
      "max_pump_return": 0.06,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 5,
      "min_btc_return": -0.1,
      "max_btc_return": 0.15,
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
      "max_rebound_from_low": 0.15,
      "buy_flow_window_minutes": 120,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 1.0,
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
      "btc_shock_threshold": -0.04,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1096,
          "fills": 41,
          "wins": 24,
          "win_rate": 0.5853658536585366,
          "average_net": 0.017969756097560987,
          "profit_factor": 2.590449874794922,
          "compound_net": 0.9538498507356641,
          "trades_per_day": 0.22777777777777777,
          "exits": {
            "TP": 24,
            "STOP_MARKET": 3,
            "TRAIL": 14
          },
          "liquidity_rejections": 45,
          "max_drawdown": 0.22170640169714295,
          "closed_trade_drawdown": 0.15135945600000011,
          "cvar_5": -0.15068000000000004
        },
        "folds": {
          "train": {
            "signals": 21,
            "fills": 21,
            "wins": 14,
            "win_rate": 0.6666666666666666,
            "average_net": 0.018792380952380977,
            "profit_factor": 2.2923762116845703,
            "compound_net": 0.42250897765736406,
            "trades_per_day": 0.35,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 5
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1793013770158084,
            "closed_trade_drawdown": 0.15135945600000011,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 13,
            "fills": 13,
            "wins": 7,
            "win_rate": 0.5384615384615384,
            "average_net": 0.015024615384615388,
            "profit_factor": 2.2627359710369777,
            "compound_net": 0.19030585923276822,
            "trades_per_day": 0.21666666666666667,
            "exits": {
              "STOP_MARKET": 1,
              "TP": 7,
              "TRAIL": 5
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.22170640169714306,
            "closed_trade_drawdown": 0.15068000000000004,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965792,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290447,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9538498507356641,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1236,
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
          "signals": 1083,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788939156.2718163
  },
  {
    "id": "V16-5b791a5cbb85ba93",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.2,
      "max_hold_minutes": 43200,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 2,
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
      "min_sol_return": -0.25,
      "max_sol_return": 0.1,
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
      "max_cvd_ratio": 0.25,
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
          "signals": 1103,
          "fills": 38,
          "wins": 23,
          "win_rate": 0.6052631578947368,
          "average_net": 0.019429473684210552,
          "profit_factor": 2.793431791682863,
          "compound_net": 0.9423175445857954,
          "trades_per_day": 0.2111111111111111,
          "exits": {
            "TP": 23,
            "STOP_MARKET": 2,
            "TRAIL": 13
          },
          "liquidity_rejections": 50,
          "max_drawdown": 0.21285812733060927,
          "closed_trade_drawdown": 0.20063999999999993,
          "cvar_5": -0.20063999999999993
        },
        "folds": {
          "train": {
            "signals": 19,
            "fills": 19,
            "wins": 14,
            "win_rate": 0.7368421052631579,
            "average_net": 0.015595789473684252,
            "profit_factor": 1.7340467697185913,
            "compound_net": 0.2620957020612369,
            "trades_per_day": 0.31666666666666665,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.21285812733060927,
            "closed_trade_drawdown": 0.20063999999999993,
            "cvar_5": -0.20063999999999993,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9423175445857954,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1227,
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
          "signals": 1071,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788939238.314801
  },
  {
    "id": "V16-39cb2fa5f0882421",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.2,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 1,
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
      "min_pump_return": -0.15,
      "max_pump_return": 0.06,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 1,
      "min_btc_return": -0.1,
      "max_btc_return": 0.15,
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
      "max_rebound_from_low": 0.15,
      "buy_flow_window_minutes": 120,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 1.0,
      "min_relative_sol_return": -1.0,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 1440,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 10.0,
      "min_average_trade_size_ratio": 0.0,
      "max_average_trade_size_ratio": 5.0,
      "min_cvd_ratio": -0.5,
      "max_cvd_ratio": 0.25,
      "min_realized_volatility": 0.015,
      "max_realized_volatility": 0.2,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 1.0,
      "min_illiquidity_ratio": 0.0,
      "max_illiquidity_ratio": 10.0,
      "macro_shock_rule": "BTC",
      "macro_shock_window_minutes": 3,
      "btc_shock_threshold": -0.04,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1100,
          "fills": 38,
          "wins": 23,
          "win_rate": 0.6052631578947368,
          "average_net": 0.019429473684210552,
          "profit_factor": 2.793431791682863,
          "compound_net": 0.9423175445857954,
          "trades_per_day": 0.2111111111111111,
          "exits": {
            "TP": 23,
            "STOP_MARKET": 2,
            "TRAIL": 13
          },
          "liquidity_rejections": 50,
          "max_drawdown": 0.21285812733060927,
          "closed_trade_drawdown": 0.20063999999999993,
          "cvar_5": -0.20063999999999993
        },
        "folds": {
          "train": {
            "signals": 19,
            "fills": 19,
            "wins": 14,
            "win_rate": 0.7368421052631579,
            "average_net": 0.015595789473684252,
            "profit_factor": 1.7340467697185913,
            "compound_net": 0.2620957020612369,
            "trades_per_day": 0.31666666666666665,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.21285812733060927,
            "closed_trade_drawdown": 0.20063999999999993,
            "cvar_5": -0.20063999999999993,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9423175445857954,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1226,
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
          "signals": 1070,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788939549.708056
  },
  {
    "id": "V16-311236134ba7ad57",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.3,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 1,
      "min_hours_between_entries": 6,
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
      "min_pump_return": -0.15,
      "max_pump_return": 0.06,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 5,
      "min_btc_return": -0.1,
      "max_btc_return": 0.15,
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
      "max_rebound_from_low": 0.15,
      "buy_flow_window_minutes": 120,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 1.0,
      "min_relative_sol_return": -1.0,
      "max_relative_sol_return": 0.3,
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
      "btc_shock_threshold": -0.04,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1119,
          "fills": 36,
          "wins": 22,
          "win_rate": 0.6111111111111112,
          "average_net": 0.023855182343726133,
          "profit_factor": 4.560276657665877,
          "compound_net": 1.226782496859478,
          "trades_per_day": 0.2,
          "exits": {
            "TP": 22,
            "TRAIL": 14
          },
          "liquidity_rejections": 49,
          "max_drawdown": 0.2518315693728599,
          "closed_trade_drawdown": 0.2308134356258602,
          "cvar_5": -0.11580671781293017
        },
        "folds": {
          "train": {
            "signals": 17,
            "fills": 17,
            "wins": 13,
            "win_rate": 0.7647058823529411,
            "average_net": 0.024516856727890612,
            "profit_factor": 2.787146453443545,
            "compound_net": 0.44693777108977617,
            "trades_per_day": 0.2833333333333333,
            "exits": {
              "TP": 13,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2518315693728599,
            "closed_trade_drawdown": 0.2308134356258602,
            "cvar_5": -0.2308134356258602,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000004,
            "profit_factor": 62.5000000000026,
            "compound_net": 0.333676032753802,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080511999856,
            "cvar_5": -0.0008000000000000229,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.1539250429096577,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1423168160229047,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 1.226782496859478,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1240,
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
          "signals": 1087,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788939632.6944013
  },
  {
    "id": "V16-1cc310fdb4db380c",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.2,
      "max_hold_minutes": 14400,
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
          "signals": 1101,
          "fills": 38,
          "wins": 23,
          "win_rate": 0.6052631578947368,
          "average_net": 0.019429473684210552,
          "profit_factor": 2.793431791682863,
          "compound_net": 0.9423175445857954,
          "trades_per_day": 0.2111111111111111,
          "exits": {
            "TP": 23,
            "STOP_MARKET": 2,
            "TRAIL": 13
          },
          "liquidity_rejections": 49,
          "max_drawdown": 0.21285812733060927,
          "closed_trade_drawdown": 0.20063999999999993,
          "cvar_5": -0.20063999999999993
        },
        "folds": {
          "train": {
            "signals": 19,
            "fills": 19,
            "wins": 14,
            "win_rate": 0.7368421052631579,
            "average_net": 0.015595789473684252,
            "profit_factor": 1.7340467697185913,
            "compound_net": 0.2620957020612369,
            "trades_per_day": 0.31666666666666665,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.21285812733060927,
            "closed_trade_drawdown": 0.20063999999999993,
            "cvar_5": -0.20063999999999993,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9423175445857954,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788939816.1228974
  },
  {
    "id": "V16-179543acf93b16ba",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.2,
      "max_hold_minutes": 43200,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 0,
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
      "min_sol_return": -0.25,
      "max_sol_return": 0.1,
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
      "btc_shock_threshold": -0.02,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1103,
          "fills": 38,
          "wins": 23,
          "win_rate": 0.6052631578947368,
          "average_net": 0.019429473684210552,
          "profit_factor": 2.793431791682863,
          "compound_net": 0.9423175445857954,
          "trades_per_day": 0.2111111111111111,
          "exits": {
            "TP": 23,
            "STOP_MARKET": 2,
            "TRAIL": 13
          },
          "liquidity_rejections": 50,
          "max_drawdown": 0.21285812733060927,
          "closed_trade_drawdown": 0.20063999999999993,
          "cvar_5": -0.20063999999999993
        },
        "folds": {
          "train": {
            "signals": 19,
            "fills": 19,
            "wins": 14,
            "win_rate": 0.7368421052631579,
            "average_net": 0.015595789473684252,
            "profit_factor": 1.7340467697185913,
            "compound_net": 0.2620957020612369,
            "trades_per_day": 0.31666666666666665,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.21285812733060927,
            "closed_trade_drawdown": 0.20063999999999993,
            "cvar_5": -0.20063999999999993,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9423175445857954,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788939859.4684684
  },
  {
    "id": "V16-14f874cd6e5007e7",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.5,
      "max_hold_minutes": 43200,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 2,
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
      "min_sol_return": -0.25,
      "max_sol_return": 0.1,
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
      "max_rebound_from_low": 0.15,
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
          "signals": 1082,
          "fills": 35,
          "wins": 21,
          "win_rate": 0.6,
          "average_net": 0.02261365503558397,
          "profit_factor": 4.06154872870492,
          "compound_net": 1.0730231173012124,
          "trades_per_day": 0.19444444444444445,
          "exits": {
            "TP": 21,
            "TRAIL": 14
          },
          "liquidity_rejections": 50,
          "max_drawdown": 0.2596144429498448,
          "closed_trade_drawdown": 0.24812207375456186,
          "cvar_5": -0.124461036877281
        },
        "folds": {
          "train": {
            "signals": 16,
            "fills": 16,
            "wins": 12,
            "win_rate": 0.75,
            "average_net": 0.02184237039033992,
            "profit_factor": 2.3949985364875457,
            "compound_net": 0.3470266867086309,
            "trades_per_day": 0.26666666666666666,
            "exits": {
              "TP": 12,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2596144429498448,
            "closed_trade_drawdown": 0.24812207375456186,
            "cvar_5": -0.24812207375456186,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 1.0730231173012124,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788939891.043515
  },
  {
    "id": "V16-077e8950c851f7b9",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.2,
      "max_hold_minutes": 43200,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 2,
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
      "min_sol_return": -0.25,
      "max_sol_return": 0.1,
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
          "signals": 1103,
          "fills": 38,
          "wins": 23,
          "win_rate": 0.6052631578947368,
          "average_net": 0.019429473684210552,
          "profit_factor": 2.793431791682863,
          "compound_net": 0.9423175445857954,
          "trades_per_day": 0.2111111111111111,
          "exits": {
            "TP": 23,
            "STOP_MARKET": 2,
            "TRAIL": 13
          },
          "liquidity_rejections": 50,
          "max_drawdown": 0.21285812733060927,
          "closed_trade_drawdown": 0.20063999999999993,
          "cvar_5": -0.20063999999999993
        },
        "folds": {
          "train": {
            "signals": 19,
            "fills": 19,
            "wins": 14,
            "win_rate": 0.7368421052631579,
            "average_net": 0.015595789473684252,
            "profit_factor": 1.7340467697185913,
            "compound_net": 0.2620957020612369,
            "trades_per_day": 0.31666666666666665,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.21285812733060927,
            "closed_trade_drawdown": 0.20063999999999993,
            "cvar_5": -0.20063999999999993,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9423175445857954,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788940030.7340035
  },
  {
    "id": "V16-453aedc114836bc9",
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
      "pump_return_window_minutes": 240,
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
      "min_average_trade_size_ratio": 0.5,
      "max_average_trade_size_ratio": 5.0,
      "min_cvd_ratio": -0.5,
      "max_cvd_ratio": 0.75,
      "min_realized_volatility": 0.015,
      "max_realized_volatility": 0.2,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 0.85,
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
          "signals": 1125,
          "fills": 41,
          "wins": 25,
          "win_rate": 0.6097560975609756,
          "average_net": 0.018183780365429655,
          "profit_factor": 2.4778725730577142,
          "compound_net": 0.9668307024807468,
          "trades_per_day": 0.22777777777777777,
          "exits": {
            "TP": 25,
            "STOP_MARKET": 3,
            "TRAIL": 12,
            "TIME": 1
          },
          "liquidity_rejections": 42,
          "max_drawdown": 0.2131689294141994,
          "closed_trade_drawdown": 0.15135945600000011,
          "cvar_5": -0.15068000000000004
        },
        "folds": {
          "train": {
            "signals": 20,
            "fills": 20,
            "wins": 14,
            "win_rate": 0.7,
            "average_net": 0.01977200000000003,
            "profit_factor": 2.298397688468612,
            "compound_net": 0.4236478959741432,
            "trades_per_day": 0.3333333333333333,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2131689294141994,
            "closed_trade_drawdown": 0.15135945600000011,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 14,
            "fills": 14,
            "wins": 8,
            "win_rate": 0.5714285714285714,
            "average_net": 0.014521071070186813,
            "profit_factor": 2.033501892667388,
            "compound_net": 0.19725535936834193,
            "trades_per_day": 0.23333333333333334,
            "exits": {
              "STOP_MARKET": 1,
              "TP": 8,
              "TRAIL": 4,
              "TIME": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1839848874538269,
            "closed_trade_drawdown": 0.15068000000000004,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1423168160229047,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9668307024807468,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1240,
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
          "signals": 1082,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "SAVED_STRATEGY_RECHECK",
    "completed_at": 1788947120.587728
  },
  {
    "id": "V16-2c1550bfc20e603f",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.15,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 1,
      "min_hours_between_entries": 12,
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
      "min_pump_return": -0.15,
      "max_pump_return": 0.06,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 5,
      "min_btc_return": -0.1,
      "max_btc_return": 0.15,
      "sol_return_window_minutes": 5,
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
      "max_rebound_from_low": 0.15,
      "buy_flow_window_minutes": 120,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 1.0,
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
      "btc_shock_threshold": -0.04,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1096,
          "fills": 41,
          "wins": 24,
          "win_rate": 0.5853658536585366,
          "average_net": 0.017969756097560987,
          "profit_factor": 2.590449874794922,
          "compound_net": 0.9538498507356641,
          "trades_per_day": 0.22777777777777777,
          "exits": {
            "TP": 24,
            "STOP_MARKET": 3,
            "TRAIL": 14
          },
          "liquidity_rejections": 45,
          "max_drawdown": 0.22170640169714295,
          "closed_trade_drawdown": 0.15135945600000011,
          "cvar_5": -0.15068000000000004
        },
        "folds": {
          "train": {
            "signals": 21,
            "fills": 21,
            "wins": 14,
            "win_rate": 0.6666666666666666,
            "average_net": 0.018792380952380977,
            "profit_factor": 2.2923762116845703,
            "compound_net": 0.42250897765736406,
            "trades_per_day": 0.35,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 5
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1793013770158084,
            "closed_trade_drawdown": 0.15135945600000011,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 13,
            "fills": 13,
            "wins": 7,
            "win_rate": 0.5384615384615384,
            "average_net": 0.015024615384615388,
            "profit_factor": 2.2627359710369777,
            "compound_net": 0.19030585923276822,
            "trades_per_day": 0.21666666666666667,
            "exits": {
              "STOP_MARKET": 1,
              "TP": 7,
              "TRAIL": 5
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.22170640169714306,
            "closed_trade_drawdown": 0.15068000000000004,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965792,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290447,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9538498507356641,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1236,
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
          "signals": 1083,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788953774.5469291
  },
  {
    "id": "V16-824bdf0c0863f161",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.3,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 1,
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
      "min_pump_return": -1.0,
      "max_pump_return": 0.06,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 5,
      "min_btc_return": -0.1,
      "max_btc_return": 0.15,
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
      "max_rebound_from_low": 0.15,
      "buy_flow_window_minutes": 120,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 1.0,
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
      "macro_shock_window_minutes": 1,
      "btc_shock_threshold": -0.04,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1080,
          "fills": 35,
          "wins": 21,
          "win_rate": 0.6,
          "average_net": 0.02261365503558397,
          "profit_factor": 4.06154872870492,
          "compound_net": 1.0730231173012124,
          "trades_per_day": 0.19444444444444445,
          "exits": {
            "TP": 21,
            "TRAIL": 14
          },
          "liquidity_rejections": 49,
          "max_drawdown": 0.2596144429498448,
          "closed_trade_drawdown": 0.24812207375456186,
          "cvar_5": -0.124461036877281
        },
        "folds": {
          "train": {
            "signals": 16,
            "fills": 16,
            "wins": 12,
            "win_rate": 0.75,
            "average_net": 0.02184237039033992,
            "profit_factor": 2.3949985364875457,
            "compound_net": 0.3470266867086309,
            "trades_per_day": 0.26666666666666666,
            "exits": {
              "TP": 12,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2596144429498448,
            "closed_trade_drawdown": 0.24812207375456186,
            "cvar_5": -0.24812207375456186,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 1.0730231173012124,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788953830.879244
  },
  {
    "id": "V16-b7940b31bc3023fe",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.15,
      "max_hold_minutes": 43200,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 1,
      "min_hours_between_entries": 12,
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
      "btc_return_window_minutes": 5,
      "min_btc_return": -0.1,
      "max_btc_return": 0.15,
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
      "structure_window_minutes": 1440,
      "min_rebound_from_low": 0.0,
      "max_rebound_from_low": 0.15,
      "buy_flow_window_minutes": 60,
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
          "signals": 1090,
          "fills": 41,
          "wins": 24,
          "win_rate": 0.5853658536585366,
          "average_net": 0.017969756097560987,
          "profit_factor": 2.590449874794922,
          "compound_net": 0.9538498507356641,
          "trades_per_day": 0.22777777777777777,
          "exits": {
            "TP": 24,
            "STOP_MARKET": 3,
            "TRAIL": 14
          },
          "liquidity_rejections": 43,
          "max_drawdown": 0.22170640169714295,
          "closed_trade_drawdown": 0.15135945600000011,
          "cvar_5": -0.15068000000000004
        },
        "folds": {
          "train": {
            "signals": 21,
            "fills": 21,
            "wins": 14,
            "win_rate": 0.6666666666666666,
            "average_net": 0.018792380952380977,
            "profit_factor": 2.2923762116845703,
            "compound_net": 0.42250897765736406,
            "trades_per_day": 0.35,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 5
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1793013770158084,
            "closed_trade_drawdown": 0.15135945600000011,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 13,
            "fills": 13,
            "wins": 7,
            "win_rate": 0.5384615384615384,
            "average_net": 0.015024615384615388,
            "profit_factor": 2.2627359710369777,
            "compound_net": 0.19030585923276822,
            "trades_per_day": 0.21666666666666667,
            "exits": {
              "STOP_MARKET": 1,
              "TP": 7,
              "TRAIL": 5
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.22170640169714306,
            "closed_trade_drawdown": 0.15068000000000004,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965792,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290447,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9538498507356641,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1236,
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
          "signals": 1083,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788954081.053174
  },
  {
    "id": "V16-d88ac516bfd1fff7",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.2,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 1,
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
      "min_pump_return": -0.15,
      "max_pump_return": 0.06,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 1,
      "min_btc_return": -0.1,
      "max_btc_return": 0.15,
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
      "max_rebound_from_low": 0.15,
      "buy_flow_window_minutes": 120,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 1.0,
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
      "btc_shock_threshold": -0.04,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1100,
          "fills": 38,
          "wins": 23,
          "win_rate": 0.6052631578947368,
          "average_net": 0.019429473684210552,
          "profit_factor": 2.793431791682863,
          "compound_net": 0.9423175445857954,
          "trades_per_day": 0.2111111111111111,
          "exits": {
            "TP": 23,
            "STOP_MARKET": 2,
            "TRAIL": 13
          },
          "liquidity_rejections": 50,
          "max_drawdown": 0.21285812733060927,
          "closed_trade_drawdown": 0.20063999999999993,
          "cvar_5": -0.20063999999999993
        },
        "folds": {
          "train": {
            "signals": 19,
            "fills": 19,
            "wins": 14,
            "win_rate": 0.7368421052631579,
            "average_net": 0.015595789473684252,
            "profit_factor": 1.7340467697185913,
            "compound_net": 0.2620957020612369,
            "trades_per_day": 0.31666666666666665,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.21285812733060927,
            "closed_trade_drawdown": 0.20063999999999993,
            "cvar_5": -0.20063999999999993,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9423175445857954,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788954382.5989122
  },
  {
    "id": "V16-90a394cb40782b90",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.2,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 1,
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
          "signals": 1101,
          "fills": 38,
          "wins": 23,
          "win_rate": 0.6052631578947368,
          "average_net": 0.019429473684210552,
          "profit_factor": 2.793431791682863,
          "compound_net": 0.9423175445857954,
          "trades_per_day": 0.2111111111111111,
          "exits": {
            "TP": 23,
            "STOP_MARKET": 2,
            "TRAIL": 13
          },
          "liquidity_rejections": 49,
          "max_drawdown": 0.21285812733060927,
          "closed_trade_drawdown": 0.20063999999999993,
          "cvar_5": -0.20063999999999993
        },
        "folds": {
          "train": {
            "signals": 19,
            "fills": 19,
            "wins": 14,
            "win_rate": 0.7368421052631579,
            "average_net": 0.015595789473684252,
            "profit_factor": 1.7340467697185913,
            "compound_net": 0.2620957020612369,
            "trades_per_day": 0.31666666666666665,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.21285812733060927,
            "closed_trade_drawdown": 0.20063999999999993,
            "cvar_5": -0.20063999999999993,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9423175445857954,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788954510.8091824
  },
  {
    "id": "V16-0666b13e7b05146b",
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
      "pump_return_window_minutes": 240,
      "min_pump_return": -0.1,
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
      "min_average_trade_size_ratio": 0.5,
      "max_average_trade_size_ratio": 5.0,
      "min_cvd_ratio": -0.5,
      "max_cvd_ratio": 0.75,
      "min_realized_volatility": 0.015,
      "max_realized_volatility": 0.2,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 0.85,
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
          "signals": 1125,
          "fills": 41,
          "wins": 25,
          "win_rate": 0.6097560975609756,
          "average_net": 0.018183780365429655,
          "profit_factor": 2.4778725730577142,
          "compound_net": 0.9668307024807468,
          "trades_per_day": 0.22777777777777777,
          "exits": {
            "TP": 25,
            "STOP_MARKET": 3,
            "TRAIL": 12,
            "TIME": 1
          },
          "liquidity_rejections": 42,
          "max_drawdown": 0.2131689294141994,
          "closed_trade_drawdown": 0.15135945600000011,
          "cvar_5": -0.15068000000000004
        },
        "folds": {
          "train": {
            "signals": 20,
            "fills": 20,
            "wins": 14,
            "win_rate": 0.7,
            "average_net": 0.01977200000000003,
            "profit_factor": 2.298397688468612,
            "compound_net": 0.4236478959741432,
            "trades_per_day": 0.3333333333333333,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2131689294141994,
            "closed_trade_drawdown": 0.15135945600000011,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 14,
            "fills": 14,
            "wins": 8,
            "win_rate": 0.5714285714285714,
            "average_net": 0.014521071070186813,
            "profit_factor": 2.033501892667388,
            "compound_net": 0.19725535936834193,
            "trades_per_day": 0.23333333333333334,
            "exits": {
              "STOP_MARKET": 1,
              "TP": 8,
              "TRAIL": 4,
              "TIME": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1839848874538269,
            "closed_trade_drawdown": 0.15068000000000004,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1423168160229047,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9668307024807468,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1240,
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
          "signals": 1082,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788954659.22504
  },
  {
    "id": "V16-fd7c86fe60f54b18",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.5,
      "max_hold_minutes": 43200,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 2,
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
      "min_sol_return": -0.25,
      "max_sol_return": 0.1,
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
      "max_rebound_from_low": 0.15,
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
      "max_cvd_ratio": 0.25,
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
          "signals": 1082,
          "fills": 35,
          "wins": 21,
          "win_rate": 0.6,
          "average_net": 0.02261365503558397,
          "profit_factor": 4.06154872870492,
          "compound_net": 1.0730231173012124,
          "trades_per_day": 0.19444444444444445,
          "exits": {
            "TP": 21,
            "TRAIL": 14
          },
          "liquidity_rejections": 50,
          "max_drawdown": 0.2596144429498448,
          "closed_trade_drawdown": 0.24812207375456186,
          "cvar_5": -0.124461036877281
        },
        "folds": {
          "train": {
            "signals": 16,
            "fills": 16,
            "wins": 12,
            "win_rate": 0.75,
            "average_net": 0.02184237039033992,
            "profit_factor": 2.3949985364875457,
            "compound_net": 0.3470266867086309,
            "trades_per_day": 0.26666666666666666,
            "exits": {
              "TP": 12,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2596144429498448,
            "closed_trade_drawdown": 0.24812207375456186,
            "cvar_5": -0.24812207375456186,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 1.0730231173012124,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1227,
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
          "signals": 1071,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788954722.0533195
  },
  {
    "id": "V16-a25ef363eec4a17d",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.2,
      "max_hold_minutes": 43200,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 0,
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
      "max_btc_return": 0.15,
      "sol_return_window_minutes": 1,
      "min_sol_return": -0.25,
      "max_sol_return": 0.1,
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
      "btc_shock_threshold": -0.02,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1100,
          "fills": 38,
          "wins": 23,
          "win_rate": 0.6052631578947368,
          "average_net": 0.019429473684210552,
          "profit_factor": 2.793431791682863,
          "compound_net": 0.9423175445857954,
          "trades_per_day": 0.2111111111111111,
          "exits": {
            "TP": 23,
            "STOP_MARKET": 2,
            "TRAIL": 13
          },
          "liquidity_rejections": 48,
          "max_drawdown": 0.21285812733060927,
          "closed_trade_drawdown": 0.20063999999999993,
          "cvar_5": -0.20063999999999993
        },
        "folds": {
          "train": {
            "signals": 19,
            "fills": 19,
            "wins": 14,
            "win_rate": 0.7368421052631579,
            "average_net": 0.015595789473684252,
            "profit_factor": 1.7340467697185913,
            "compound_net": 0.2620957020612369,
            "trades_per_day": 0.31666666666666665,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.21285812733060927,
            "closed_trade_drawdown": 0.20063999999999993,
            "cvar_5": -0.20063999999999993,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9423175445857954,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788954732.408686
  },
  {
    "id": "V16-0caf098e9d27e524",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.5,
      "max_hold_minutes": 43200,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 2,
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
      "drawdown_window_minutes": 720,
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
      "min_sol_return": -0.25,
      "max_sol_return": 0.1,
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
      "max_rebound_from_low": 0.15,
      "buy_flow_window_minutes": 240,
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
          "signals": 1082,
          "fills": 35,
          "wins": 21,
          "win_rate": 0.6,
          "average_net": 0.02261365503558397,
          "profit_factor": 4.06154872870492,
          "compound_net": 1.0730231173012124,
          "trades_per_day": 0.19444444444444445,
          "exits": {
            "TP": 21,
            "TRAIL": 14
          },
          "liquidity_rejections": 50,
          "max_drawdown": 0.2596144429498448,
          "closed_trade_drawdown": 0.24812207375456186,
          "cvar_5": -0.124461036877281
        },
        "folds": {
          "train": {
            "signals": 16,
            "fills": 16,
            "wins": 12,
            "win_rate": 0.75,
            "average_net": 0.02184237039033992,
            "profit_factor": 2.3949985364875457,
            "compound_net": 0.3470266867086309,
            "trades_per_day": 0.26666666666666666,
            "exits": {
              "TP": 12,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2596144429498448,
            "closed_trade_drawdown": 0.24812207375456186,
            "cvar_5": -0.24812207375456186,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 1.0730231173012124,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788954824.8538563
  },
  {
    "id": "V16-42823b062a5cb7b3",
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
      "pump_return_window_minutes": 240,
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
      "structure_window_minutes": 720,
      "min_rebound_from_low": 0.0,
      "max_rebound_from_low": 0.3,
      "buy_flow_window_minutes": 60,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 0.25,
      "min_relative_sol_return": -0.1,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 1440,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 10.0,
      "min_average_trade_size_ratio": 0.5,
      "max_average_trade_size_ratio": 5.0,
      "min_cvd_ratio": -0.5,
      "max_cvd_ratio": 0.75,
      "min_realized_volatility": 0.015,
      "max_realized_volatility": 0.2,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 0.85,
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
          "signals": 1125,
          "fills": 41,
          "wins": 25,
          "win_rate": 0.6097560975609756,
          "average_net": 0.018183780365429655,
          "profit_factor": 2.4778725730577142,
          "compound_net": 0.9668307024807468,
          "trades_per_day": 0.22777777777777777,
          "exits": {
            "TP": 25,
            "STOP_MARKET": 3,
            "TRAIL": 12,
            "TIME": 1
          },
          "liquidity_rejections": 42,
          "max_drawdown": 0.2131689294141994,
          "closed_trade_drawdown": 0.15135945600000011,
          "cvar_5": -0.15068000000000004
        },
        "folds": {
          "train": {
            "signals": 20,
            "fills": 20,
            "wins": 14,
            "win_rate": 0.7,
            "average_net": 0.01977200000000003,
            "profit_factor": 2.298397688468612,
            "compound_net": 0.4236478959741432,
            "trades_per_day": 0.3333333333333333,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2131689294141994,
            "closed_trade_drawdown": 0.15135945600000011,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 14,
            "fills": 14,
            "wins": 8,
            "win_rate": 0.5714285714285714,
            "average_net": 0.014521071070186813,
            "profit_factor": 2.033501892667388,
            "compound_net": 0.19725535936834193,
            "trades_per_day": 0.23333333333333334,
            "exits": {
              "STOP_MARKET": 1,
              "TP": 8,
              "TRAIL": 4,
              "TIME": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1839848874538269,
            "closed_trade_drawdown": 0.15068000000000004,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1423168160229047,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9668307024807468,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1240,
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
          "signals": 1082,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788954906.710949
  },
  {
    "id": "V16-36c4376b26632b38",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.15,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 1,
      "min_hours_between_entries": 6,
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
      "min_pump_return": -0.15,
      "max_pump_return": 0.06,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 5,
      "min_btc_return": -0.1,
      "max_btc_return": 0.15,
      "sol_return_window_minutes": 1,
      "min_sol_return": -0.06,
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
      "max_rebound_from_low": 0.15,
      "buy_flow_window_minutes": 120,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 1.0,
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
      "max_realized_volatility": 0.5,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 1.0,
      "min_illiquidity_ratio": 0.0,
      "max_illiquidity_ratio": 10.0,
      "macro_shock_rule": "BTC",
      "macro_shock_window_minutes": 5,
      "btc_shock_threshold": -0.04,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1173,
          "fills": 42,
          "wins": 24,
          "win_rate": 0.5714285714285714,
          "average_net": 0.017522857142857163,
          "profit_factor": 2.585983966899406,
          "compound_net": 0.9522867708550757,
          "trades_per_day": 0.23333333333333334,
          "exits": {
            "TP": 24,
            "STOP_MARKET": 3,
            "TRAIL": 15
          },
          "liquidity_rejections": 45,
          "max_drawdown": 0.18398488745382702,
          "closed_trade_drawdown": 0.15135945600000011,
          "cvar_5": -0.15068000000000004
        },
        "folds": {
          "train": {
            "signals": 21,
            "fills": 21,
            "wins": 14,
            "win_rate": 0.6666666666666666,
            "average_net": 0.018792380952380977,
            "profit_factor": 2.2923762116845703,
            "compound_net": 0.42250897765736406,
            "trades_per_day": 0.35,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 5
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1793013770158084,
            "closed_trade_drawdown": 0.15135945600000011,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 14,
            "fills": 14,
            "wins": 7,
            "win_rate": 0.5,
            "average_net": 0.01389428571428574,
            "profit_factor": 2.2510933882171367,
            "compound_net": 0.18935361454538202,
            "trades_per_day": 0.23333333333333334,
            "exits": {
              "STOP_MARKET": 1,
              "TP": 7,
              "TRAIL": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.18398488745382702,
            "closed_trade_drawdown": 0.15068000000000004,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9522867708550757,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1240,
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
          "signals": 1087,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788954926.0115392
  },
  {
    "id": "V16-bd37f948e8ffc48e",
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
      "pump_return_window_minutes": 240,
      "min_pump_return": -0.1,
      "max_pump_return": 0.04,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 15,
      "min_btc_return": -0.06,
      "max_btc_return": 0.15,
      "sol_return_window_minutes": 1,
      "min_sol_return": -1.0,
      "max_sol_return": 0.25,
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
      "buy_flow_window_minutes": 60,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 0.25,
      "min_relative_sol_return": -0.1,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 1440,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 10.0,
      "min_average_trade_size_ratio": 0.5,
      "max_average_trade_size_ratio": 5.0,
      "min_cvd_ratio": -0.5,
      "max_cvd_ratio": 0.75,
      "min_realized_volatility": 0.015,
      "max_realized_volatility": 0.2,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 0.85,
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
          "signals": 1105,
          "fills": 42,
          "wins": 25,
          "win_rate": 0.5952380952380952,
          "average_net": 0.017731785594824186,
          "profit_factor": 2.4739492891596377,
          "compound_net": 0.9652572379187625,
          "trades_per_day": 0.23333333333333334,
          "exits": {
            "TP": 25,
            "STOP_MARKET": 3,
            "TRAIL": 13,
            "TIME": 1
          },
          "liquidity_rejections": 43,
          "max_drawdown": 0.21379839427066816,
          "closed_trade_drawdown": 0.16628915649777187,
          "cvar_5": -0.15068000000000004
        },
        "folds": {
          "train": {
            "signals": 20,
            "fills": 20,
            "wins": 13,
            "win_rate": 0.65,
            "average_net": 0.017232000000000025,
            "profit_factor": 2.128635053707101,
            "compound_net": 0.35477045491177517,
            "trades_per_day": 0.3333333333333333,
            "exits": {
              "TP": 13,
              "STOP_MARKET": 2,
              "TRAIL": 5
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.21379839427066816,
            "closed_trade_drawdown": 0.15203836843520024,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 14,
            "fills": 14,
            "wins": 8,
            "win_rate": 0.5714285714285714,
            "average_net": 0.014521071070186813,
            "profit_factor": 2.033501892667388,
            "compound_net": 0.19725535936834215,
            "trades_per_day": 0.23333333333333334,
            "exits": {
              "STOP_MARKET": 1,
              "TP": 8,
              "TRAIL": 4,
              "TIME": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1839848874538269,
            "closed_trade_drawdown": 0.15068000000000004,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 8,
            "fills": 8,
            "wins": 4,
            "win_rate": 0.5,
            "average_net": 0.024599999999999997,
            "profit_factor": 62.4999999999961,
            "compound_net": 0.21162129505514105,
            "trades_per_day": 0.13333333333333333,
            "exits": {
              "TP": 4,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.002398080512000189,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9652572379187625,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1237,
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
          "signals": 1079,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788955301.085066
  },
  {
    "id": "V16-50bb3756aadd0c09",
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
      "pump_return_window_minutes": 240,
      "min_pump_return": -0.1,
      "max_pump_return": 0.1,
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
      "min_relative_btc_return": -1.0,
      "max_relative_btc_return": 0.25,
      "min_relative_sol_return": -0.1,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 1440,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 10.0,
      "min_average_trade_size_ratio": 0.5,
      "max_average_trade_size_ratio": 5.0,
      "min_cvd_ratio": -0.5,
      "max_cvd_ratio": 0.75,
      "min_realized_volatility": 0.015,
      "max_realized_volatility": 0.2,
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
          "signals": 1130,
          "fills": 41,
          "wins": 25,
          "win_rate": 0.6097560975609756,
          "average_net": 0.018183780365429655,
          "profit_factor": 2.4778725730577142,
          "compound_net": 0.9668307024807468,
          "trades_per_day": 0.22777777777777777,
          "exits": {
            "TP": 25,
            "STOP_MARKET": 3,
            "TRAIL": 12,
            "TIME": 1
          },
          "liquidity_rejections": 42,
          "max_drawdown": 0.2131689294141994,
          "closed_trade_drawdown": 0.15135945600000011,
          "cvar_5": -0.15068000000000004
        },
        "folds": {
          "train": {
            "signals": 20,
            "fills": 20,
            "wins": 14,
            "win_rate": 0.7,
            "average_net": 0.01977200000000003,
            "profit_factor": 2.298397688468612,
            "compound_net": 0.4236478959741432,
            "trades_per_day": 0.3333333333333333,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2131689294141994,
            "closed_trade_drawdown": 0.15135945600000011,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 14,
            "fills": 14,
            "wins": 8,
            "win_rate": 0.5714285714285714,
            "average_net": 0.014521071070186813,
            "profit_factor": 2.033501892667388,
            "compound_net": 0.19725535936834193,
            "trades_per_day": 0.23333333333333334,
            "exits": {
              "STOP_MARKET": 1,
              "TP": 8,
              "TRAIL": 4,
              "TIME": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1839848874538269,
            "closed_trade_drawdown": 0.15068000000000004,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1423168160229047,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9668307024807468,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1241,
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
          "signals": 1083,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788955342.586644
  },
  {
    "id": "V16-ba79936c71b15a14",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.15,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 1,
      "min_hours_between_entries": 12,
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
      "min_pump_return": -0.15,
      "max_pump_return": 0.06,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 5,
      "min_btc_return": -0.1,
      "max_btc_return": 0.15,
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
      "max_rebound_from_low": 0.15,
      "buy_flow_window_minutes": 120,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 1.0,
      "min_relative_sol_return": -1.0,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 1440,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 10.0,
      "min_average_trade_size_ratio": 0.0,
      "max_average_trade_size_ratio": 1000000.0,
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
      "btc_shock_threshold": -0.04,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1105,
          "fills": 42,
          "wins": 24,
          "win_rate": 0.5714285714285714,
          "average_net": 0.017522857142857156,
          "profit_factor": 2.5859839668994042,
          "compound_net": 0.9522867708550753,
          "trades_per_day": 0.23333333333333334,
          "exits": {
            "TP": 24,
            "STOP_MARKET": 3,
            "TRAIL": 15
          },
          "liquidity_rejections": 47,
          "max_drawdown": 0.22170640169714295,
          "closed_trade_drawdown": 0.15135945600000011,
          "cvar_5": -0.15068000000000004
        },
        "folds": {
          "train": {
            "signals": 22,
            "fills": 22,
            "wins": 14,
            "win_rate": 0.6363636363636364,
            "average_net": 0.017901818181818206,
            "profit_factor": 2.2863862032923974,
            "compound_net": 0.42137097047523797,
            "trades_per_day": 0.36666666666666664,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1793013770158084,
            "closed_trade_drawdown": 0.15135945600000011,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 13,
            "fills": 13,
            "wins": 7,
            "win_rate": 0.5384615384615384,
            "average_net": 0.015024615384615388,
            "profit_factor": 2.2627359710369777,
            "compound_net": 0.190305859232768,
            "trades_per_day": 0.21666666666666667,
            "exits": {
              "STOP_MARKET": 1,
              "TP": 7,
              "TRAIL": 5
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.22170640169714306,
            "closed_trade_drawdown": 0.15068000000000004,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9522867708550753,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1237,
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
          "signals": 1084,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788955389.6056511
  },
  {
    "id": "V16-7e8e211cc94bd5be",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.2,
      "max_hold_minutes": 28800,
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
      "max_path_efficiency": 0.7,
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
          "signals": 1101,
          "fills": 38,
          "wins": 23,
          "win_rate": 0.6052631578947368,
          "average_net": 0.019429473684210552,
          "profit_factor": 2.793431791682863,
          "compound_net": 0.9423175445857954,
          "trades_per_day": 0.2111111111111111,
          "exits": {
            "TP": 23,
            "STOP_MARKET": 2,
            "TRAIL": 13
          },
          "liquidity_rejections": 49,
          "max_drawdown": 0.21285812733060927,
          "closed_trade_drawdown": 0.20063999999999993,
          "cvar_5": -0.20063999999999993
        },
        "folds": {
          "train": {
            "signals": 19,
            "fills": 19,
            "wins": 14,
            "win_rate": 0.7368421052631579,
            "average_net": 0.015595789473684252,
            "profit_factor": 1.7340467697185913,
            "compound_net": 0.2620957020612369,
            "trades_per_day": 0.31666666666666665,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.21285812733060927,
            "closed_trade_drawdown": 0.20063999999999993,
            "cvar_5": -0.20063999999999993,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9423175445857954,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788955408.8927474
  },
  {
    "id": "V16-113d525a519b7f4f",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.2,
      "max_hold_minutes": 43200,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 2,
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
      "max_cvd_ratio": 0.25,
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
          "signals": 1103,
          "fills": 38,
          "wins": 23,
          "win_rate": 0.6052631578947368,
          "average_net": 0.019429473684210552,
          "profit_factor": 2.793431791682863,
          "compound_net": 0.9423175445857954,
          "trades_per_day": 0.2111111111111111,
          "exits": {
            "TP": 23,
            "STOP_MARKET": 2,
            "TRAIL": 13
          },
          "liquidity_rejections": 50,
          "max_drawdown": 0.21285812733060927,
          "closed_trade_drawdown": 0.20063999999999993,
          "cvar_5": -0.20063999999999993
        },
        "folds": {
          "train": {
            "signals": 19,
            "fills": 19,
            "wins": 14,
            "win_rate": 0.7368421052631579,
            "average_net": 0.015595789473684252,
            "profit_factor": 1.7340467697185913,
            "compound_net": 0.2620957020612369,
            "trades_per_day": 0.31666666666666665,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.21285812733060927,
            "closed_trade_drawdown": 0.20063999999999993,
            "cvar_5": -0.20063999999999993,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9423175445857954,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1227,
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
          "signals": 1071,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788955412.6624439
  },
  {
    "id": "V16-1e9125a3d2880997",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.5,
      "max_hold_minutes": 43200,
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
      "max_btc_return": 0.15,
      "sol_return_window_minutes": 1,
      "min_sol_return": -0.25,
      "max_sol_return": 0.1,
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
          "signals": 1082,
          "fills": 35,
          "wins": 21,
          "win_rate": 0.6,
          "average_net": 0.02261365503558397,
          "profit_factor": 4.06154872870492,
          "compound_net": 1.0730231173012124,
          "trades_per_day": 0.19444444444444445,
          "exits": {
            "TP": 21,
            "TRAIL": 14
          },
          "liquidity_rejections": 48,
          "max_drawdown": 0.2596144429498448,
          "closed_trade_drawdown": 0.24812207375456186,
          "cvar_5": -0.124461036877281
        },
        "folds": {
          "train": {
            "signals": 16,
            "fills": 16,
            "wins": 12,
            "win_rate": 0.75,
            "average_net": 0.02184237039033992,
            "profit_factor": 2.3949985364875457,
            "compound_net": 0.3470266867086309,
            "trades_per_day": 0.26666666666666666,
            "exits": {
              "TP": 12,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2596144429498448,
            "closed_trade_drawdown": 0.24812207375456186,
            "cvar_5": -0.24812207375456186,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 1.0730231173012124,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788955429.0871332
  },
  {
    "id": "V16-123e1de10fb6a02a",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.3,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 1,
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
      "min_pump_return": -1.0,
      "max_pump_return": 0.06,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 5,
      "min_btc_return": -0.1,
      "max_btc_return": 0.15,
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
      "max_rebound_from_low": 0.15,
      "buy_flow_window_minutes": 120,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 0.1,
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
      "macro_shock_window_minutes": 1,
      "btc_shock_threshold": -0.04,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1080,
          "fills": 35,
          "wins": 21,
          "win_rate": 0.6,
          "average_net": 0.02261365503558397,
          "profit_factor": 4.06154872870492,
          "compound_net": 1.0730231173012124,
          "trades_per_day": 0.19444444444444445,
          "exits": {
            "TP": 21,
            "TRAIL": 14
          },
          "liquidity_rejections": 49,
          "max_drawdown": 0.2596144429498448,
          "closed_trade_drawdown": 0.24812207375456186,
          "cvar_5": -0.124461036877281
        },
        "folds": {
          "train": {
            "signals": 16,
            "fills": 16,
            "wins": 12,
            "win_rate": 0.75,
            "average_net": 0.02184237039033992,
            "profit_factor": 2.3949985364875457,
            "compound_net": 0.3470266867086309,
            "trades_per_day": 0.26666666666666666,
            "exits": {
              "TP": 12,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2596144429498448,
            "closed_trade_drawdown": 0.24812207375456186,
            "cvar_5": -0.24812207375456186,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 1.0730231173012124,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788955431.894797
  },
  {
    "id": "V16-b381b5fb9020441f",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.15,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 1,
      "min_hours_between_entries": 12,
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
      "min_pump_return": -0.15,
      "max_pump_return": 0.06,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 3,
      "min_btc_return": -0.1,
      "max_btc_return": 0.15,
      "sol_return_window_minutes": 5,
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
      "max_rebound_from_low": 0.15,
      "buy_flow_window_minutes": 120,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 1.0,
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
      "btc_shock_threshold": -0.04,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1099,
          "fills": 41,
          "wins": 24,
          "win_rate": 0.5853658536585366,
          "average_net": 0.017969756097560987,
          "profit_factor": 2.590449874794922,
          "compound_net": 0.9538498507356641,
          "trades_per_day": 0.22777777777777777,
          "exits": {
            "TP": 24,
            "STOP_MARKET": 3,
            "TRAIL": 14
          },
          "liquidity_rejections": 46,
          "max_drawdown": 0.22170640169714295,
          "closed_trade_drawdown": 0.15135945600000011,
          "cvar_5": -0.15068000000000004
        },
        "folds": {
          "train": {
            "signals": 21,
            "fills": 21,
            "wins": 14,
            "win_rate": 0.6666666666666666,
            "average_net": 0.018792380952380977,
            "profit_factor": 2.2923762116845703,
            "compound_net": 0.42250897765736406,
            "trades_per_day": 0.35,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 5
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1793013770158084,
            "closed_trade_drawdown": 0.15135945600000011,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 13,
            "fills": 13,
            "wins": 7,
            "win_rate": 0.5384615384615384,
            "average_net": 0.015024615384615388,
            "profit_factor": 2.2627359710369777,
            "compound_net": 0.19030585923276822,
            "trades_per_day": 0.21666666666666667,
            "exits": {
              "STOP_MARKET": 1,
              "TP": 7,
              "TRAIL": 5
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.22170640169714306,
            "closed_trade_drawdown": 0.15068000000000004,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965792,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290447,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9538498507356641,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1235,
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
          "signals": 1082,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788955431.8323836
  },
  {
    "id": "V16-0b506befa37f40c9",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.3,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 1,
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
      "min_pump_return": -1.0,
      "max_pump_return": 0.06,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 5,
      "min_btc_return": -0.1,
      "max_btc_return": 0.15,
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
      "max_rebound_from_low": 0.15,
      "buy_flow_window_minutes": 120,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 1.0,
      "min_relative_sol_return": -1.0,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 1440,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 10.0,
      "min_average_trade_size_ratio": 0.0,
      "max_average_trade_size_ratio": 5.0,
      "min_cvd_ratio": -0.5,
      "max_cvd_ratio": 0.75,
      "min_realized_volatility": 0.008,
      "max_realized_volatility": 0.2,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 1.0,
      "min_illiquidity_ratio": 0.0,
      "max_illiquidity_ratio": 10.0,
      "macro_shock_rule": "BTC",
      "macro_shock_window_minutes": 3,
      "btc_shock_threshold": -0.04,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1080,
          "fills": 35,
          "wins": 21,
          "win_rate": 0.6,
          "average_net": 0.02261365503558397,
          "profit_factor": 4.06154872870492,
          "compound_net": 1.0730231173012124,
          "trades_per_day": 0.19444444444444445,
          "exits": {
            "TP": 21,
            "TRAIL": 14
          },
          "liquidity_rejections": 49,
          "max_drawdown": 0.2596144429498448,
          "closed_trade_drawdown": 0.24812207375456186,
          "cvar_5": -0.124461036877281
        },
        "folds": {
          "train": {
            "signals": 16,
            "fills": 16,
            "wins": 12,
            "win_rate": 0.75,
            "average_net": 0.02184237039033992,
            "profit_factor": 2.3949985364875457,
            "compound_net": 0.3470266867086309,
            "trades_per_day": 0.26666666666666666,
            "exits": {
              "TP": 12,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2596144429498448,
            "closed_trade_drawdown": 0.24812207375456186,
            "cvar_5": -0.24812207375456186,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 1.0730231173012124,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788955435.0181923
  },
  {
    "id": "V16-12767a1a60feb85a",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.5,
      "max_hold_minutes": 43200,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 2,
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
      "min_sol_return": -0.15,
      "max_sol_return": 0.1,
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
      "buy_flow_window_minutes": 240,
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
          "signals": 1085,
          "fills": 35,
          "wins": 21,
          "win_rate": 0.6,
          "average_net": 0.02261365503558397,
          "profit_factor": 4.06154872870492,
          "compound_net": 1.0730231173012124,
          "trades_per_day": 0.19444444444444445,
          "exits": {
            "TP": 21,
            "TRAIL": 14
          },
          "liquidity_rejections": 50,
          "max_drawdown": 0.2596144429498448,
          "closed_trade_drawdown": 0.24812207375456186,
          "cvar_5": -0.124461036877281
        },
        "folds": {
          "train": {
            "signals": 16,
            "fills": 16,
            "wins": 12,
            "win_rate": 0.75,
            "average_net": 0.02184237039033992,
            "profit_factor": 2.3949985364875457,
            "compound_net": 0.3470266867086309,
            "trades_per_day": 0.26666666666666666,
            "exits": {
              "TP": 12,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2596144429498448,
            "closed_trade_drawdown": 0.24812207375456186,
            "cvar_5": -0.24812207375456186,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 1.0730231173012124,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788955449.4946392
  },
  {
    "id": "V16-ea110ed36ab1b91d",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.15,
      "max_hold_minutes": 28800,
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
      "pump_return_window_minutes": 240,
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
      "structure_window_minutes": 720,
      "min_rebound_from_low": 0.0,
      "max_rebound_from_low": 0.3,
      "buy_flow_window_minutes": 60,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 0.25,
      "min_relative_sol_return": -0.1,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 1440,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 10.0,
      "min_average_trade_size_ratio": 0.5,
      "max_average_trade_size_ratio": 5.0,
      "min_cvd_ratio": -0.5,
      "max_cvd_ratio": 0.75,
      "min_realized_volatility": 0.015,
      "max_realized_volatility": 0.2,
      "min_path_efficiency": 0.0,
      "max_path_efficiency": 0.85,
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
          "signals": 1122,
          "fills": 41,
          "wins": 25,
          "win_rate": 0.6097560975609756,
          "average_net": 0.0192087804878049,
          "profit_factor": 2.70305336908572,
          "compound_net": 1.0531848911853956,
          "trades_per_day": 0.22777777777777777,
          "exits": {
            "TP": 25,
            "STOP_MARKET": 3,
            "TRAIL": 13
          },
          "liquidity_rejections": 42,
          "max_drawdown": 0.2131689294141994,
          "closed_trade_drawdown": 0.15135945600000011,
          "cvar_5": -0.15068000000000004
        },
        "folds": {
          "train": {
            "signals": 20,
            "fills": 20,
            "wins": 14,
            "win_rate": 0.7,
            "average_net": 0.01977200000000003,
            "profit_factor": 2.298397688468612,
            "compound_net": 0.4236478959741432,
            "trades_per_day": 0.3333333333333333,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2131689294141994,
            "closed_trade_drawdown": 0.15135945600000011,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 14,
            "fills": 14,
            "wins": 8,
            "win_rate": 0.5714285714285714,
            "average_net": 0.017522857142857166,
            "profit_factor": 2.585983966899407,
            "compound_net": 0.24982115219440648,
            "trades_per_day": 0.23333333333333334,
            "exits": {
              "STOP_MARKET": 1,
              "TP": 8,
              "TRAIL": 5
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1839848874538269,
            "closed_trade_drawdown": 0.15068000000000004,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 1.0531848911853956,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1240,
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
          "signals": 1082,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788955453.7871926
  },
  {
    "id": "V16-68e2504fb9d4c7f3",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.15,
      "max_hold_minutes": 43200,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 1,
      "min_hours_between_entries": 12,
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
      "min_btc_return": -0.1,
      "max_btc_return": 0.15,
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
      "structure_window_minutes": 1440,
      "min_rebound_from_low": 0.0,
      "max_rebound_from_low": 0.15,
      "buy_flow_window_minutes": 60,
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
          "signals": 1089,
          "fills": 41,
          "wins": 24,
          "win_rate": 0.5853658536585366,
          "average_net": 0.017969756097560987,
          "profit_factor": 2.590449874794922,
          "compound_net": 0.9538498507356641,
          "trades_per_day": 0.22777777777777777,
          "exits": {
            "TP": 24,
            "STOP_MARKET": 3,
            "TRAIL": 14
          },
          "liquidity_rejections": 43,
          "max_drawdown": 0.22170640169714295,
          "closed_trade_drawdown": 0.15135945600000011,
          "cvar_5": -0.15068000000000004
        },
        "folds": {
          "train": {
            "signals": 21,
            "fills": 21,
            "wins": 14,
            "win_rate": 0.6666666666666666,
            "average_net": 0.018792380952380977,
            "profit_factor": 2.2923762116845703,
            "compound_net": 0.42250897765736406,
            "trades_per_day": 0.35,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 5
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1793013770158084,
            "closed_trade_drawdown": 0.15135945600000011,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 13,
            "fills": 13,
            "wins": 7,
            "win_rate": 0.5384615384615384,
            "average_net": 0.015024615384615388,
            "profit_factor": 2.2627359710369777,
            "compound_net": 0.19030585923276822,
            "trades_per_day": 0.21666666666666667,
            "exits": {
              "STOP_MARKET": 1,
              "TP": 7,
              "TRAIL": 5
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.22170640169714306,
            "closed_trade_drawdown": 0.15068000000000004,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965792,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290447,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9538498507356641,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1236,
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
          "signals": 1083,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788955458.353638
  },
  {
    "id": "V16-6ad034dbe0b2f615",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.15,
      "max_hold_minutes": 14400,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 1,
      "min_hours_between_entries": 12,
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
      "min_pump_return": -0.15,
      "max_pump_return": 0.06,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 3,
      "min_btc_return": -0.1,
      "max_btc_return": 0.15,
      "sol_return_window_minutes": 5,
      "min_sol_return": -0.25,
      "max_sol_return": 0.25,
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
      "max_rebound_from_low": 0.15,
      "buy_flow_window_minutes": 120,
      "min_buy_flow_mean": 0.0,
      "min_buy_flow_slope": -1.0,
      "min_relative_btc_return": -0.1,
      "max_relative_btc_return": 1.0,
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
      "btc_shock_threshold": -0.04,
      "sol_shock_threshold": -1.0,
      "entry_family": "DIP",
      "execution_fee_rate": null
    },
    "regimes": {
      "BEAR": {
        "metrics": {
          "signals": 1099,
          "fills": 41,
          "wins": 24,
          "win_rate": 0.5853658536585366,
          "average_net": 0.017969756097560987,
          "profit_factor": 2.590449874794922,
          "compound_net": 0.9538498507356641,
          "trades_per_day": 0.22777777777777777,
          "exits": {
            "TP": 24,
            "STOP_MARKET": 3,
            "TRAIL": 14
          },
          "liquidity_rejections": 46,
          "max_drawdown": 0.22170640169714295,
          "closed_trade_drawdown": 0.15135945600000011,
          "cvar_5": -0.15068000000000004
        },
        "folds": {
          "train": {
            "signals": 21,
            "fills": 21,
            "wins": 14,
            "win_rate": 0.6666666666666666,
            "average_net": 0.018792380952380977,
            "profit_factor": 2.2923762116845703,
            "compound_net": 0.42250897765736406,
            "trades_per_day": 0.35,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 5
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1793013770158084,
            "closed_trade_drawdown": 0.15135945600000011,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 13,
            "fills": 13,
            "wins": 7,
            "win_rate": 0.5384615384615384,
            "average_net": 0.015024615384615388,
            "profit_factor": 2.2627359710369777,
            "compound_net": 0.19030585923276822,
            "trades_per_day": 0.21666666666666667,
            "exits": {
              "STOP_MARKET": 1,
              "TP": 7,
              "TRAIL": 5
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.22170640169714306,
            "closed_trade_drawdown": 0.15068000000000004,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965792,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290447,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9538498507356641,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1235,
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
          "signals": 1082,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788955461.2747028
  },
  {
    "id": "V16-f8e27e18b47c22da",
    "config": {
      "fee_rate": 0.0021,
      "limit_discount": 0.025,
      "limit_ttl_minutes": 60,
      "target_net": 0.05,
      "stop_net": -0.2,
      "max_hold_minutes": 43200,
      "adverse_slippage": 0.0008,
      "max_entries_per_utc_day": 2,
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
      "max_relative_btc_return": 0.1,
      "min_relative_sol_return": -1.0,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 1440,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 10.0,
      "min_average_trade_size_ratio": 0.0,
      "max_average_trade_size_ratio": 5.0,
      "min_cvd_ratio": -0.5,
      "max_cvd_ratio": 0.25,
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
          "signals": 1103,
          "fills": 38,
          "wins": 23,
          "win_rate": 0.6052631578947368,
          "average_net": 0.019429473684210552,
          "profit_factor": 2.793431791682863,
          "compound_net": 0.9423175445857954,
          "trades_per_day": 0.2111111111111111,
          "exits": {
            "TP": 23,
            "STOP_MARKET": 2,
            "TRAIL": 13
          },
          "liquidity_rejections": 50,
          "max_drawdown": 0.21285812733060927,
          "closed_trade_drawdown": 0.20063999999999993,
          "cvar_5": -0.20063999999999993
        },
        "folds": {
          "train": {
            "signals": 19,
            "fills": 19,
            "wins": 14,
            "win_rate": 0.7368421052631579,
            "average_net": 0.015595789473684252,
            "profit_factor": 1.7340467697185913,
            "compound_net": 0.2620957020612369,
            "trades_per_day": 0.31666666666666665,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 3
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.21285812733060927,
            "closed_trade_drawdown": 0.20063999999999993,
            "cvar_5": -0.20063999999999993,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 12,
            "fills": 12,
            "wins": 6,
            "win_rate": 0.5,
            "average_net": 0.02460000000000002,
            "profit_factor": 62.49999999999971,
            "compound_net": 0.33367603275380175,
            "trades_per_day": 0.2,
            "exits": {
              "TRAIL": 6,
              "TP": 6
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.16404833836858013,
            "closed_trade_drawdown": 0.002398080512000078,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.14231681602290458,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9423175445857954,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1227,
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
          "signals": 1071,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788955476.743289
  },
  {
    "id": "V16-2eb83fc5055ab7a2",
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
      "min_green_candle_return": -0.1,
      "min_volume_ratio_20m": 0.5,
      "vwap_window_minutes": 10080,
      "drawdown_window_minutes": 1440,
      "volume_window_minutes": 240,
      "pump_return_window_minutes": 240,
      "min_pump_return": -0.1,
      "max_pump_return": 0.1,
      "rsi_period_minutes": 7,
      "min_rsi": 0.0,
      "max_rsi": 90.0,
      "atr_period_minutes": 14,
      "min_atr_ratio": 0.0,
      "max_atr_ratio": 1.0,
      "btc_return_window_minutes": 15,
      "min_btc_return": -1.0,
      "max_btc_return": 0.04,
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
      "min_relative_btc_return": -1.0,
      "max_relative_btc_return": 0.25,
      "min_relative_sol_return": -0.1,
      "max_relative_sol_return": 1.0,
      "micro_window_minutes": 1440,
      "min_trade_intensity": 0.0,
      "max_trade_intensity": 10.0,
      "min_average_trade_size_ratio": 0.5,
      "max_average_trade_size_ratio": 5.0,
      "min_cvd_ratio": -0.5,
      "max_cvd_ratio": 0.75,
      "min_realized_volatility": 0.015,
      "max_realized_volatility": 0.2,
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
          "signals": 1130,
          "fills": 41,
          "wins": 25,
          "win_rate": 0.6097560975609756,
          "average_net": 0.018183780365429655,
          "profit_factor": 2.4778725730577142,
          "compound_net": 0.9668307024807468,
          "trades_per_day": 0.22777777777777777,
          "exits": {
            "TP": 25,
            "STOP_MARKET": 3,
            "TRAIL": 12,
            "TIME": 1
          },
          "liquidity_rejections": 42,
          "max_drawdown": 0.2131689294141994,
          "closed_trade_drawdown": 0.15135945600000011,
          "cvar_5": -0.15068000000000004
        },
        "folds": {
          "train": {
            "signals": 20,
            "fills": 20,
            "wins": 14,
            "win_rate": 0.7,
            "average_net": 0.01977200000000003,
            "profit_factor": 2.298397688468612,
            "compound_net": 0.4236478959741432,
            "trades_per_day": 0.3333333333333333,
            "exits": {
              "TP": 14,
              "STOP_MARKET": 2,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.2131689294141994,
            "closed_trade_drawdown": 0.15135945600000011,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_a": {
            "signals": 14,
            "fills": 14,
            "wins": 8,
            "win_rate": 0.5714285714285714,
            "average_net": 0.014521071070186813,
            "profit_factor": 2.033501892667388,
            "compound_net": 0.19725535936834193,
            "trades_per_day": 0.23333333333333334,
            "exits": {
              "STOP_MARKET": 1,
              "TP": 8,
              "TRAIL": 4,
              "TIME": 1
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1839848874538269,
            "closed_trade_drawdown": 0.15068000000000004,
            "cvar_5": -0.15068000000000004,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          },
          "validation_b": {
            "signals": 7,
            "fills": 7,
            "wins": 3,
            "win_rate": 0.42857142857142855,
            "average_net": 0.02097142857142856,
            "profit_factor": 46.87499999999707,
            "compound_net": 0.15392504290965814,
            "trades_per_day": 0.11666666666666667,
            "exits": {
              "TP": 3,
              "TRAIL": 4
            },
            "liquidity_rejections": 0,
            "max_drawdown": 0.1423168160229047,
            "closed_trade_drawdown": 0.003196162047590656,
            "cvar_5": -0.0008000000000001339,
            "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
          }
        },
        "eligible": true,
        "reasons": [],
        "objective": 0.9668307024807468,
        "distance": 0
      },
      "SIDEWAYS": {
        "metrics": {
          "signals": 1241,
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
          "signals": 1083,
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
      0.884140623009378
    ],
    "worst_net": 0.884140623009378,
    "origin": "MUTATION",
    "completed_at": 1788955484.9070282
  }
]

## ИИ

Выключен

## Архив

F:\PUMP Research Lab\experiments\v16-59b126eb7bd78c8c\research.sqlite3