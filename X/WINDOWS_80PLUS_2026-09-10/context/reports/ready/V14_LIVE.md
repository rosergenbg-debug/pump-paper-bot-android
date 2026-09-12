# PUMP Research Lab V14

Сохранено — остановлено

Проверено: 66; прибыльных на всех выбранных окнах: 1.

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
  "id": "V14-b26e1bf2a88feb3a",
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
        "signals": 935,
        "fills": 32,
        "wins": 28,
        "win_rate": 0.875,
        "average_net": 0.018761044089210054,
        "profit_factor": 2.1553109813386633,
        "compound_net": 0.7176150203373206,
        "trades_per_day": 0.17777777777777778,
        "exits": {
          "TP": 28,
          "STOP_LIMIT_FILLED": 4
        },
        "liquidity_rejections": 31,
        "max_drawdown": 0.2627637836666681,
        "closed_trade_drawdown": 0.24937570723616387,
        "cvar_5": -0.14454416531457442
      },
      "folds": {
        "train": {
          "signals": 17,
          "fills": 17,
          "wins": 16,
          "win_rate": 0.9411764705882353,
          "average_net": 0.03103334762852806,
          "profit_factor": 5.692274384763417,
          "compound_net": 0.6623961761680028,
          "trades_per_day": 0.2833333333333333,
          "exits": {
            "TP": 16,
            "STOP_LIMIT_FILLED": 1
          },
          "liquidity_rejections": 0,
          "max_drawdown": 0.15838226767673302,
          "closed_trade_drawdown": 0.11243309031502358,
          "cvar_5": -0.11243309031502358,
          "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
        },
        "validation_a": {
          "signals": 9,
          "fills": 9,
          "wins": 7,
          "win_rate": 0.7777777777777778,
          "average_net": 0.0014492861767395862,
          "profit_factor": 1.0488603172578292,
          "compound_net": -0.01222963888564077,
          "trades_per_day": 0.15,
          "exits": {
            "STOP_LIMIT_FILLED": 2,
            "TP": 7
          },
          "liquidity_rejections": 0,
          "max_drawdown": 0.2627637836666681,
          "closed_trade_drawdown": 0.24937570723616398,
          "cvar_5": -0.14883125620823712,
          "accounting": "CONTINUOUS_EQUITY; trades attributed on exit; positions carried across folds"
        },
        "validation_b": {
          "signals": 6,
          "fills": 6,
          "wins": 5,
          "win_rate": 0.8333333333333334,
          "average_net": 0.009957154263181408,
          "profit_factor": 1.4259530282215913,
          "compound_net": 0.04600872572366499,
          "trades_per_day": 0.1,
          "exits": {
            "TP": 5,
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
      "objective": 0.7176150203373206,
      "distance": 0
    },
    "SIDEWAYS": {
      "metrics": {
        "signals": 608,
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
        "signals": 508,
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
  "completed_at": 1788795990.2264555
}

## ИИ

ИИ недоступен; CPU продолжает: <urlopen error [WinError 10061] Es konnte keine Verbindung hergestellt werden, da der Zielcomputer die Verbindung verweigerte>

## Архив

F:\PUMP Research Lab\experiments\v14-6a0382929b3c2184\research.sqlite3