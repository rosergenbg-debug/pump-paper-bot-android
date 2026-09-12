# PUMP Research Lab V12

ОШИБКА — завершённые результаты сохранены; незавершённые будут повторены

Проверено: 5644; прибыльных на всех выбранных окнах: 1922.

Это историческое исследование, не прогноз и не допуск к торговле.

## Фактические периоды

{
  "BEAR": {
    "start_ms": 1758023940000,
    "end_ms": 1770422399999,
    "minutes": 206641,
    "requested_days": 180,
    "excluded_minutes": 0,
    "return": -0.7390445623997037,
    "actual_direction": "BEAR",
    "regime_changed_by_holdout_exclusion": false
  },
  "SIDEWAYS": {
    "start_ms": 1776772740000,
    "end_ms": 1782691199999,
    "minutes": 98641,
    "requested_days": 180,
    "excluded_minutes": 108000,
    "return": -0.22563274098007535,
    "actual_direction": "BEAR",
    "regime_changed_by_holdout_exclusion": true
  },
  "BULL": {
    "start_ms": 1776772740000,
    "end_ms": 1782691199999,
    "minutes": 98641,
    "requested_days": 180,
    "excluded_minutes": 108000,
    "return": -0.22563274098007535,
    "actual_direction": "BEAR",
    "regime_changed_by_holdout_exclusion": true
  }
}

## Лидер

{
  "id": "V12-eac260974b98aa91",
  "config": {
    "fee_rate": 0.0021,
    "limit_discount": 0.025,
    "limit_ttl_minutes": 60,
    "target_net": 0.025,
    "stop_net": -0.15,
    "max_hold_minutes": 43200,
    "adverse_slippage": 0.0008,
    "max_entries_per_utc_day": 3,
    "min_hours_between_entries": 6,
    "drawdown_gate": -0.03,
    "spread_rate": 0.0,
    "entry_latency_minutes": 0,
    "btc_context_rule": "ANY",
    "sol_context_rule": "ANY",
    "vwap_deviation_gate": -0.004,
    "min_buy_share": 0.0,
    "min_buy_share_delta": -0.05,
    "min_green_candle_return": -0.1,
    "min_volume_ratio_20m": 0.25,
    "vwap_window_minutes": 10080,
    "drawdown_window_minutes": 7200,
    "volume_window_minutes": 720,
    "pump_return_window_minutes": 240,
    "min_pump_return": -1.0,
    "max_pump_return": 1.0,
    "rsi_period_minutes": 14,
    "min_rsi": 0.0,
    "max_rsi": 80.0,
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
    "breakeven_trigger_net": 0.06,
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
    "buy_flow_window_minutes": 30,
    "min_buy_flow_mean": 0.0,
    "min_buy_flow_slope": -1.0,
    "min_relative_btc_return": -1.0,
    "max_relative_btc_return": 1.0,
    "min_relative_sol_return": -0.15,
    "max_relative_sol_return": 0.15,
    "micro_window_minutes": 720,
    "min_trade_intensity": 0.0,
    "max_trade_intensity": 1000000.0,
    "min_average_trade_size_ratio": 0.5,
    "max_average_trade_size_ratio": 1000000.0,
    "min_cvd_ratio": -0.5,
    "max_cvd_ratio": 0.1,
    "min_realized_volatility": 0.0,
    "max_realized_volatility": 1.0,
    "min_path_efficiency": 0.0,
    "max_path_efficiency": 0.85,
    "min_illiquidity_ratio": 0.0,
    "max_illiquidity_ratio": 10.0,
    "macro_shock_rule": "BTC",
    "macro_shock_window_minutes": 5,
    "btc_shock_threshold": -1.0,
    "sol_shock_threshold": -1.0,
    "entry_family": "DIP",
    "execution_fee_rate": null
  },
  "regimes": {
    "BEAR": {
      "metrics": {
        "signals": 836,
        "fills": 41,
        "wins": 39,
        "win_rate": 0.9512195121951219,
        "average_net": 0.016430243902438937,
        "profit_factor": 3.2353331563578323,
        "compound_net": 0.8896155424517942,
        "trades_per_day": 0.285712903054089,
        "exits": {
          "TP": 39,
          "STOP_MARKET": 2
        },
        "liquidity_rejections": 21,
        "max_drawdown": 0.19349641552216967,
        "closed_trade_drawdown": 0.15068000000000015,
        "cvar_5": -0.09212000000000005
      },
      "folds": {
        "train": {
          "signals": 16,
          "fills": 16,
          "wins": 15,
          "win_rate": 0.9375,
          "average_net": 0.014019999999999908,
          "profit_factor": 2.488717812582946,
          "compound_net": 0.23006859877017427,
          "trades_per_day": 0.33683718074297164,
          "exits": {
            "TP": 15,
            "STOP_MARKET": 1
          },
          "liquidity_rejections": 0,
          "max_drawdown": 0.17246691534894198,
          "closed_trade_drawdown": 0.15068000000000015,
          "cvar_5": -0.15068000000000015,
          "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
        },
        "validation_a": {
          "signals": 16,
          "fills": 16,
          "wins": 15,
          "win_rate": 0.9375,
          "average_net": 0.014019999999999921,
          "profit_factor": 2.4887178125829497,
          "compound_net": 0.23006859877017471,
          "trades_per_day": 0.3333333333333333,
          "exits": {
            "TP": 15,
            "STOP_MARKET": 1
          },
          "liquidity_rejections": 0,
          "max_drawdown": 0.19349641552216978,
          "closed_trade_drawdown": 0.15067999999999993,
          "cvar_5": -0.15067999999999993,
          "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
        },
        "validation_b": {
          "signals": 9,
          "fills": 9,
          "wins": 9,
          "win_rate": 1.0,
          "average_net": 0.02499999999999991,
          "profit_factor": null,
          "compound_net": 0.2488629699476652,
          "trades_per_day": 0.1875,
          "exits": {
            "TP": 9
          },
          "liquidity_rejections": 0,
          "max_drawdown": 0.11641321447299424,
          "closed_trade_drawdown": 0.0,
          "cvar_5": 0.02499999999999991,
          "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
        }
      },
      "eligible": true,
      "reasons": [],
      "objective": -0.033046024513080266,
      "distance": 0.0
    },
    "SIDEWAYS": {
      "metrics": {
        "signals": 391,
        "fills": 8,
        "wins": 8,
        "win_rate": 1.0,
        "average_net": 0.02499999999999991,
        "profit_factor": null,
        "compound_net": 0.21840289750991748,
        "trades_per_day": 0.11678713719447288,
        "exits": {
          "TP": 8
        },
        "liquidity_rejections": 14,
        "max_drawdown": 0.06316976875352498,
        "closed_trade_drawdown": 0.0,
        "cvar_5": 0.02499999999999991
      },
      "folds": {
        "train": {
          "signals": 1,
          "fills": 1,
          "wins": 1,
          "win_rate": 1.0,
          "average_net": 0.02499999999999991,
          "profit_factor": null,
          "compound_net": 0.02499999999999991,
          "trades_per_day": 0.044443072744668376,
          "exits": {
            "TP": 1
          },
          "liquidity_rejections": 0,
          "max_drawdown": 0.019279553526128868,
          "closed_trade_drawdown": 0.0,
          "cvar_5": 0.02499999999999991,
          "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
        },
        "validation_a": {
          "signals": 5,
          "fills": 5,
          "wins": 5,
          "win_rate": 1.0,
          "average_net": 0.02499999999999991,
          "profit_factor": null,
          "compound_net": 0.13140821289062465,
          "trades_per_day": 0.21739130434782608,
          "exits": {
            "TP": 5
          },
          "liquidity_rejections": 0,
          "max_drawdown": 0.06316976875352498,
          "closed_trade_drawdown": 0.0,
          "cvar_5": 0.02499999999999991,
          "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
        },
        "validation_b": {
          "signals": 2,
          "fills": 2,
          "wins": 2,
          "win_rate": 1.0,
          "average_net": 0.02499999999999991,
          "profit_factor": null,
          "compound_net": 0.0506249999999997,
          "trades_per_day": 0.08695652173913043,
          "exits": {
            "TP": 2
          },
          "liquidity_rejections": 0,
          "max_drawdown": 0.05560098119378565,
          "closed_trade_drawdown": 0.0,
          "cvar_5": 0.02499999999999991,
          "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
        }
      },
      "eligible": false,
      "reasons": [
        "train:trades_per_day_outside_0.05_to_0.5"
      ],
      "objective": 0.05437034686971187,
      "distance": 0.11113854510663254
    },
    "BULL": {
      "metrics": {
        "signals": 391,
        "fills": 8,
        "wins": 8,
        "win_rate": 1.0,
        "average_net": 0.02499999999999991,
        "profit_factor": null,
        "compound_net": 0.21840289750991748,
        "trades_per_day": 0.11678713719447288,
        "exits": {
          "TP": 8
        },
        "liquidity_rejections": 14,
        "max_drawdown": 0.06316976875352498,
        "closed_trade_drawdown": 0.0,
        "cvar_5": 0.02499999999999991
      },
      "folds": {
        "train": {
          "signals": 1,
          "fills": 1,
          "wins": 1,
          "win_rate": 1.0,
          "average_net": 0.02499999999999991,
          "profit_factor": null,
          "compound_net": 0.02499999999999991,
          "trades_per_day": 0.044443072744668376,
          "exits": {
            "TP": 1
          },
          "liquidity_rejections": 0,
          "max_drawdown": 0.019279553526128868,
          "closed_trade_drawdown": 0.0,
          "cvar_5": 0.02499999999999991,
          "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
        },
        "validation_a": {
          "signals": 5,
          "fills": 5,
          "wins": 5,
          "win_rate": 1.0,
          "average_net": 0.02499999999999991,
          "profit_factor": null,
          "compound_net": 0.13140821289062465,
          "trades_per_day": 0.21739130434782608,
          "exits": {
            "TP": 5
          },
          "liquidity_rejections": 0,
          "max_drawdown": 0.06316976875352498,
          "closed_trade_drawdown": 0.0,
          "cvar_5": 0.02499999999999991,
          "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
        },
        "validation_b": {
          "signals": 2,
          "fills": 2,
          "wins": 2,
          "win_rate": 1.0,
          "average_net": 0.02499999999999991,
          "profit_factor": null,
          "compound_net": 0.0506249999999997,
          "trades_per_day": 0.08695652173913043,
          "exits": {
            "TP": 2
          },
          "liquidity_rejections": 0,
          "max_drawdown": 0.05560098119378565,
          "closed_trade_drawdown": 0.0,
          "cvar_5": 0.02499999999999991,
          "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
        }
      },
      "eligible": false,
      "reasons": [
        "train:trades_per_day_outside_0.05_to_0.5"
      ],
      "objective": 0.05437034686971187,
      "distance": 0.11113854510663254
    }
  },
  "positive": true,
  "eligible": false,
  "score": [
    0,
    -0.11113854510663254,
    -0.033046024513080266
  ],
  "worst_net": 0.21840289750991748,
  "origin": "MUTATION"
}

## ИИ

huihui-gemma-4-12b-it-abliterated-i1: Изменение климата в позднем бронзовом веке напрямую повлияло на структуру социальных иерархий в Средиземноморье.

## Архив

C:\Users\sera-\Documents\PUMP Research Lab\experiments\v12-658fdbc4e5d85b1b\research.sqlite3