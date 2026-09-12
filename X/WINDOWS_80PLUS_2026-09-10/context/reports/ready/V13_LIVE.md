# PUMP Research Lab V13

Сохранено — остановлено

Проверено: 48171; прибыльных на всех выбранных окнах: 16786.

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

## Лидер

{
  "id": "V13-90b41cc55ee1c848",
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
        "signals": 1353,
        "fills": 41,
        "wins": 25,
        "win_rate": 0.6097560975609756,
        "average_net": 0.01818378036542966,
        "profit_factor": 2.4778725730577147,
        "compound_net": 0.9668307024807485,
        "trades_per_day": 0.22777777777777777,
        "exits": {
          "TP": 25,
          "STOP_MARKET": 3,
          "TRAIL": 12,
          "TIME": 1
        },
        "liquidity_rejections": 58,
        "max_drawdown": 0.18398488745382713,
        "closed_trade_drawdown": 0.15135945600000011,
        "cvar_5": -0.15068
      },
      "folds": {
        "train": {
          "signals": 21,
          "fills": 21,
          "wins": 15,
          "win_rate": 0.7142857142857143,
          "average_net": 0.021211428571428607,
          "profit_factor": 2.4625689519306566,
          "compound_net": 0.4948302907728517,
          "trades_per_day": 0.35,
          "exits": {
            "TP": 15,
            "STOP_MARKET": 2,
            "TRAIL": 4
          },
          "liquidity_rejections": 0,
          "max_drawdown": 0.1793013770158085,
          "closed_trade_drawdown": 0.15135945600000011,
          "cvar_5": -0.15067999999999998,
          "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
        },
        "validation_a": {
          "signals": 13,
          "fills": 13,
          "wins": 7,
          "win_rate": 0.5384615384615384,
          "average_net": 0.011791922690970411,
          "profit_factor": 1.7793141560839643,
          "compound_net": 0.140243199398421,
          "trades_per_day": 0.21666666666666667,
          "exits": {
            "STOP_MARKET": 1,
            "TP": 7,
            "TRAIL": 4,
            "TIME": 1
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
          "compound_net": 0.15392504290965792,
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
      "objective": 0.9668307024807485,
      "distance": 0
    },
    "SIDEWAYS": {
      "metrics": {
        "signals": 1256,
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
        "liquidity_rejections": 38,
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
        "signals": 1104,
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
        "liquidity_rejections": 33,
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
  "origin": "MUTATION"
}

## ИИ

huihui-gemma-4-12b-it-abliterated-i1: Гипотеза: отскок после падения; цель 5.0%, стоп -15.0%, до 168 ч. Описание построено из параметров; результат ещё не доказан.

## Архив

F:\PUMP Research Lab\experiments\v13-6a0382929b3c2184\research.sqlite3