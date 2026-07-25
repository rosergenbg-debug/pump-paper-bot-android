#!/usr/bin/env python3
"""
Causal six-month audit for the separate hourly Gemini shadow experiment.

The historical test does not pretend to replay Gemini or past news.  It tests
whether the numeric market context that will be shown to Gemini has enough
out-of-sample information to flag a useful share of >3% rises during the next
hour.  Every feature is trailing, the trade starts after the sampled candle,
and model/threshold selection never sees the final holdout.
"""

from __future__ import annotations

import argparse
import json
from dataclasses import asdict, dataclass
from pathlib import Path

import numpy as np
import pandas as pd
from sklearn.ensemble import HistGradientBoostingClassifier, RandomForestClassifier
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import average_precision_score, roc_auc_score
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler


FEE_RATE = 0.0015
START_BALANCE = 1_000.0
TARGET_RISE = 0.03
FORWARD_BARS = 12
MAX_SIGNALS_PER_DAY = 1.5
SIX_MONTH_DAYS = 182
RANDOM_SEED = 34


@dataclass(frozen=True)
class SplitMetrics:
    start: str
    end: str
    samples: int
    rise_events: int
    signals: int
    signals_per_day: float
    captured_events: int
    capture_rate: float
    false_signals: int
    signal_precision: float
    positive_close_rate: float
    roc_auc: float
    average_precision: float
    return_after_fees_pct: float
    max_drawdown_pct: float
    stress_returns_pct: dict[str, float]


def find_source(cache_dir: Path, prefix: str) -> Path:
    matches = sorted(cache_dir.glob(f"{prefix}*.pkl"))
    if not matches:
        raise FileNotFoundError(f"No cached source matching {prefix}*.pkl in {cache_dir}")
    return matches[-1]


def load_source(cache_dir: Path, prefix: str) -> pd.DataFrame:
    frame = pd.read_pickle(find_source(cache_dir, prefix))
    required = {
        "close_time",
        "open",
        "high",
        "low",
        "close",
        "volume",
        "quote_volume",
        "trades",
        "taker_buy_volume",
    }
    missing = required.difference(frame.columns)
    if missing:
        raise ValueError(f"{prefix} is missing columns: {sorted(missing)}")
    return frame.drop_duplicates("close_time").set_index("close_time").sort_index()


def load_aligned(cache_dir: Path) -> pd.DataFrame:
    pump = load_source(cache_dir, "pump_spot_5m_hybrid")
    eur = load_source(cache_dir, "eur_spot_5m_hybrid")
    btc = load_source(cache_dir, "btc_spot_5m_hybrid")
    sol = load_source(cache_dir, "sol_spot_5m_hybrid")
    futures = load_source(cache_dir, "pump_futures_5m_hybrid")
    common = pump.index.intersection(eur.index)
    common = common.intersection(btc.index).intersection(sol.index).intersection(futures.index)
    pump, eur, btc, sol, futures = (
        frame.loc[common].copy() for frame in (pump, eur, btc, sol, futures)
    )
    time = pd.to_datetime(common, unit="ms", utc=True)
    base = pd.DataFrame(index=time)
    base["price"] = (pump["close"] / eur["close"]).to_numpy()
    base["open"] = (pump["open"] / eur["open"]).to_numpy()
    base["high"] = (pump["high"] / eur["low"]).to_numpy()
    base["low"] = (pump["low"] / eur["high"]).to_numpy()
    for name, frame in (
        ("pump", pump),
        ("btc", btc),
        ("sol", sol),
        ("futures", futures),
    ):
        base[f"{name}_close"] = frame["close"].to_numpy()
        base[f"{name}_volume"] = frame["volume"].to_numpy()
        base[f"{name}_quote_volume"] = frame["quote_volume"].to_numpy()
        base[f"{name}_trades"] = frame["trades"].to_numpy()
        base[f"{name}_taker"] = (
            frame["taker_buy_volume"] / frame["volume"].replace(0.0, np.nan)
        ).to_numpy()
    return base


def rolling_taker(base: pd.DataFrame, prefix: str, bars: int) -> pd.Series:
    volume = base[f"{prefix}_volume"]
    bought = volume * base[f"{prefix}_taker"]
    return bought.rolling(bars).sum() / volume.rolling(bars).sum()


def feature_frame(base: pd.DataFrame) -> pd.DataFrame:
    feature = pd.DataFrame(index=base.index)
    for bars in (1, 3, 6, 12, 36, 72, 144, 288):
        feature[f"pump_return_{bars}"] = base["price"].pct_change(bars)
        feature[f"btc_return_{bars}"] = base["btc_close"].pct_change(bars)
        feature[f"sol_return_{bars}"] = base["sol_close"].pct_change(bars)
        if bars in (3, 6, 12, 36, 72):
            feature[f"range_{bars}"] = (
                base["high"].rolling(bars).max() / base["low"].rolling(bars).min() - 1.0
            )
            current_volume = base["pump_volume"].rolling(bars).sum()
            historical_volume = base["pump_volume"].rolling(
                bars * 12, min_periods=bars * 4
            ).sum() * (bars / (bars * 12))
            feature[f"volume_ratio_{bars}"] = current_volume / historical_volume
            feature[f"spot_taker_{bars}"] = rolling_taker(base, "pump", bars)
            feature[f"futures_taker_{bars}"] = rolling_taker(base, "futures", bars)

    feature["futures_premium"] = (
        base["futures_close"] / base["pump_close"] - 1.0
    )
    feature["futures_premium_change_1h"] = (
        feature["futures_premium"] - feature["futures_premium"].shift(12)
    )
    feature["relative_strength_1h"] = feature["pump_return_12"] - (
        feature["btc_return_12"] + feature["sol_return_12"]
    ) / 2.0
    feature["relative_strength_3h"] = feature["pump_return_36"] - (
        feature["btc_return_36"] + feature["sol_return_36"]
    ) / 2.0
    feature["ema_20h_gap"] = (
        base["price"] / base["price"].ewm(span=20 * 12, adjust=False).mean() - 1.0
    )
    feature["ema_200h_gap"] = (
        base["price"] / base["price"].ewm(span=200 * 12, adjust=False).mean() - 1.0
    )
    feature["compression_1h"] = feature["range_12"] / feature["range_12"].rolling(
        24 * 12, min_periods=12 * 12
    ).median()

    change = base["price"].diff()
    up = change.clip(lower=0.0).ewm(alpha=1.0 / (14 * 12), adjust=False).mean()
    down = (-change.clip(upper=0.0)).ewm(alpha=1.0 / (14 * 12), adjust=False).mean()
    feature["rsi_14h"] = 100.0 - 100.0 / (1.0 + up / down.replace(0.0, np.nan))

    hourly = feature.loc[feature.index.minute == 59].copy()
    hourly["hour_sin"] = np.sin(2.0 * np.pi * hourly.index.hour / 24.0)
    hourly["hour_cos"] = np.cos(2.0 * np.pi * hourly.index.hour / 24.0)
    hourly["weekday_sin"] = np.sin(2.0 * np.pi * hourly.index.dayofweek / 7.0)
    hourly["weekday_cos"] = np.cos(2.0 * np.pi * hourly.index.dayofweek / 7.0)
    return hourly


def add_outcomes(
    features: pd.DataFrame,
    base: pd.DataFrame,
) -> tuple[pd.DataFrame, pd.Series, pd.Series, pd.Series]:
    position = {timestamp: index for index, timestamp in enumerate(base.index)}
    event, peak_return, close_return = [], [], []
    for timestamp in features.index:
        index = position[timestamp]
        if index + FORWARD_BARS >= len(base):
            event.append(np.nan)
            peak_return.append(np.nan)
            close_return.append(np.nan)
            continue
        entry = base["open"].iloc[index + 1]
        peak = base["high"].iloc[index + 1:index + FORWARD_BARS + 1].max() / entry - 1.0
        closing = base["price"].iloc[index + FORWARD_BARS] / entry - 1.0
        event.append(float(peak >= TARGET_RISE))
        peak_return.append(peak)
        close_return.append(closing)

    event_series = pd.Series(event, index=features.index, name="rise_event")
    peak_series = pd.Series(peak_return, index=features.index, name="peak_return")
    close_series = pd.Series(close_return, index=features.index, name="close_return")
    start = features.index.max() - pd.Timedelta(days=SIX_MONTH_DAYS)
    usable = event_series.notna() & (features.notna().mean(axis=1) >= 0.80)
    usable &= features.index >= start
    clean = features.loc[usable].replace([np.inf, -np.inf], np.nan)
    return (
        clean,
        event_series.loc[usable].astype(int),
        peak_series.loc[usable],
        close_series.loc[usable],
    )


def candidate_models() -> dict[str, object]:
    return {
        "logistic_balanced": make_pipeline(
            SimpleImputer(strategy="median"),
            StandardScaler(),
            LogisticRegression(
                class_weight="balanced",
                C=0.1,
                max_iter=2_000,
                random_state=RANDOM_SEED,
            ),
        ),
        "hist_gradient_boosting": make_pipeline(
            SimpleImputer(strategy="median"),
            HistGradientBoostingClassifier(
                max_iter=200,
                max_leaf_nodes=10,
                learning_rate=0.04,
                l2_regularization=2.0,
                class_weight="balanced",
                random_state=RANDOM_SEED,
            ),
        ),
        "random_forest": make_pipeline(
            SimpleImputer(strategy="median"),
            RandomForestClassifier(
                n_estimators=500,
                max_depth=6,
                min_samples_leaf=20,
                max_features=0.5,
                class_weight="balanced_subsample",
                n_jobs=-1,
                random_state=RANDOM_SEED,
            ),
        ),
    }


def signal_budget(sample_count: int) -> int:
    days = sample_count / 24.0
    return max(1, int(np.floor(days * MAX_SIGNALS_PER_DAY)))


def select_threshold(probabilities: np.ndarray, actual: np.ndarray) -> dict:
    budget = signal_budget(len(probabilities))
    thresholds = np.unique(np.quantile(probabilities, np.linspace(0.0, 1.0, 1_001)))
    candidates = []
    for threshold in thresholds:
        predicted = probabilities >= threshold
        signals = int(predicted.sum())
        if signals > budget:
            continue
        captured = int((predicted & (actual == 1)).sum())
        precision = captured / max(1, signals)
        recall = captured / max(1, int(actual.sum()))
        candidates.append((recall, precision, -signals, float(threshold)))
    if not candidates:
        raise RuntimeError("No threshold satisfies the signal budget")
    recall, precision, negative_signals, threshold = max(candidates)
    return {
        "threshold": threshold,
        "capture_rate": recall,
        "precision": precision,
        "signals": -negative_signals,
        "budget": budget,
    }


def threshold_for_half_capture(probabilities: np.ndarray, actual: np.ndarray) -> dict:
    positives = int(actual.sum())
    candidates = []
    for threshold in np.unique(np.quantile(probabilities, np.linspace(0.0, 1.0, 1_001))):
        predicted = probabilities >= threshold
        captured = int((predicted & (actual == 1)).sum())
        if captured / max(1, positives) < 0.50:
            continue
        signals = int(predicted.sum())
        precision = captured / max(1, signals)
        candidates.append((precision, -signals, float(threshold), captured))
    if not candidates:
        return {}
    precision, negative_signals, threshold, captured = max(candidates)
    signals = -negative_signals
    return {
        "threshold": threshold,
        "captured": captured,
        "events": positives,
        "capture_rate": captured / max(1, positives),
        "signals": signals,
        "signals_per_day": signals / max(1.0, len(actual) / 24.0),
        "false_signals": signals - captured,
        "precision": precision,
    }


def portfolio_metrics(
    signals: np.ndarray,
    close_returns: np.ndarray,
    slippage: float,
) -> tuple[float, float]:
    equity = START_BALANCE
    peak = equity
    drawdown = 0.0
    for signal, gross_return in zip(signals, close_returns):
        if not signal:
            continue
        execution_factor = (
            (1.0 - FEE_RATE)
            * (1.0 - slippage)
            * (1.0 + gross_return)
            * (1.0 - slippage)
            * (1.0 - FEE_RATE)
        )
        equity *= max(0.0, execution_factor)
        peak = max(peak, equity)
        drawdown = max(drawdown, (peak - equity) / peak)
    return (equity / START_BALANCE - 1.0) * 100.0, drawdown * 100.0


def metrics_for(
    index: pd.DatetimeIndex,
    actual: np.ndarray,
    probabilities: np.ndarray,
    close_returns: np.ndarray,
    threshold: float,
) -> SplitMetrics:
    signals = probabilities >= threshold
    captured = int((signals & (actual == 1)).sum())
    signal_count = int(signals.sum())
    events = int(actual.sum())
    days = max(1.0, (index[-1] - index[0]).total_seconds() / 86_400.0)
    baseline_return, drawdown = portfolio_metrics(signals, close_returns, 0.0)
    stress = {}
    for slippage in (0.005, 0.01, 0.02):
        value, _ = portfolio_metrics(signals, close_returns, slippage)
        stress[f"{slippage:.3f}"] = value
    return SplitMetrics(
        start=index[0].isoformat(),
        end=index[-1].isoformat(),
        samples=len(actual),
        rise_events=events,
        signals=signal_count,
        signals_per_day=signal_count / days,
        captured_events=captured,
        capture_rate=captured / max(1, events),
        false_signals=signal_count - captured,
        signal_precision=captured / max(1, signal_count),
        positive_close_rate=float((close_returns[signals] > 0.0).mean()) if signal_count else 0.0,
        roc_auc=float(roc_auc_score(actual, probabilities)),
        average_precision=float(average_precision_score(actual, probabilities)),
        return_after_fees_pct=baseline_return,
        max_drawdown_pct=drawdown,
        stress_returns_pct=stress,
    )


def make_report(payload: dict) -> str:
    validation = payload["validation"]
    holdout = payload["holdout"]
    forced = payload["validation_half_capture"]
    lines = [
        "# Часовой эксперимент Gemini V3.4 — шестимесячная проверка",
        "",
        "Это причинная проверка числового контекста, а не историческая симуляция Gemini. "
        "Архива одинаково доступных в тот момент новостей и ответов Gemini нет, поэтому они "
        "не подмешивались задним числом.",
        "",
        "## Метод",
        "",
        f"- История: {payload['study_start']} — {payload['study_end']}.",
        "- Решение принимается после закрытия часа; исполнение начинается со следующей 5‑минутной свечи.",
        "- Целевое событие: цена хотя бы раз поднимается на 3% от исполнимой цены входа в течение следующего часа.",
        "- Первые 60% истории — обучение, следующие 20% — выбор модели и порога, последние 20% — закрытый holdout.",
        f"- Порог выбирался на validation с лимитом не больше {MAX_SIGNALS_PER_DAY:.1f} сигнала в сутки. "
        "На holdout частота могла измениться из-за сдвига распределения и измеряется отдельно. "
        "Без лимита цель 50% можно имитировать почти постоянными сигналами.",
        "- Комиссия: 0,15% на вход и 0,15% на выход; отдельно показан стресс проскальзывания.",
        "",
        "## Выбранная модель",
        "",
        f"- Модель: `{payload['selected_model']}`.",
        f"- Порог, выбранный только на validation: {payload['threshold']:.6f}.",
        "",
        "| Отрезок | Подъёмов >3% | Поймано | Сигналов/сутки | Сигналов | Ложных | Точность сигнала | Результат после комиссий |",
        "|---|---:|---:|---:|---:|---:|---:|---:|",
        f"| Validation | {validation['rise_events']} | {validation['captured_events']} "
        f"({validation['capture_rate'] * 100:.1f}%) | {validation['signals_per_day']:.2f} | {validation['signals']} | "
        f"{validation['false_signals']} | {validation['signal_precision'] * 100:.1f}% | "
        f"{validation['return_after_fees_pct']:+.2f}% |",
        f"| Закрытый holdout | {holdout['rise_events']} | {holdout['captured_events']} "
        f"({holdout['capture_rate'] * 100:.1f}%) | {holdout['signals_per_day']:.2f} | {holdout['signals']} | "
        f"{holdout['false_signals']} | {holdout['signal_precision'] * 100:.1f}% | "
        f"{holdout['return_after_fees_pct']:+.2f}% |",
        "",
        "## Почему 50% на validation не считается успехом",
        "",
        f"Чтобы принудительно поймать не меньше половины подъёмов на validation, потребовалось "
        f"{forced.get('signals', 0)} сигналов ({forced.get('signals_per_day', 0.0):.1f} в сутки), "
        f"из них {forced.get('false_signals', 0)} ложных. Точность такого сигнала — "
        f"{forced.get('precision', 0.0) * 100:.1f}%. Это почти постоянное предупреждение, "
        "а не пригодное преимущество.",
        "",
        "## Вывод",
        "",
        f"Порог, выбранный при практическом лимите на validation, на закрытом holdout давал "
        f"{holdout['signals_per_day']:.2f} сигнала в сутки и поймал только "
        f"{holdout['captured_events']} из {holdout['rise_events']} подъёмов "
        f"({holdout['capture_rate'] * 100:.1f}%). Цель 50% не подтверждена. "
        "Поэтому часовой Gemini‑модуль допустим только как отдельный живой Shadow‑эксперимент "
        "с собственными виртуальными 1 000 €, журналом решений и нулевым влиянием на основную V3.2.",
        "",
        "## Ограничения",
        "",
        "- Binance не равен фактическому исполнению на Bitpanda Fusion.",
        "- Исторического bid/ask spread нет.",
        "- Результат относится к одной монете и одному шестимесячному режиму.",
        "- Живые ответы Gemini и новости должны оцениваться только с момента запуска журнала.",
    ]
    return "\n".join(lines) + "\n"


def json_default(value):
    if isinstance(value, np.generic):
        return value.item()
    raise TypeError(type(value).__name__)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cache-dir", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, default=Path("research/output"))
    args = parser.parse_args()
    args.output_dir.mkdir(parents=True, exist_ok=True)

    base = load_aligned(args.cache_dir)
    features, actual, peak_returns, close_returns = add_outcomes(feature_frame(base), base)
    first_cut = int(len(features) * 0.60)
    second_cut = int(len(features) * 0.80)
    train_x, validation_x, holdout_x = (
        features.iloc[:first_cut],
        features.iloc[first_cut:second_cut],
        features.iloc[second_cut:],
    )
    train_y, validation_y, holdout_y = (
        actual.iloc[:first_cut],
        actual.iloc[first_cut:second_cut],
        actual.iloc[second_cut:],
    )

    candidates = {}
    trained = {}
    for name, model in candidate_models().items():
        model.fit(train_x, train_y)
        validation_probability = model.predict_proba(validation_x)[:, 1]
        selection = select_threshold(validation_probability, validation_y.to_numpy())
        candidates[name] = {
            **selection,
            "roc_auc": float(roc_auc_score(validation_y, validation_probability)),
            "average_precision": float(
                average_precision_score(validation_y, validation_probability)
            ),
        }
        trained[name] = (model, validation_probability)

    selected_name = max(
        candidates,
        key=lambda name: (
            candidates[name]["capture_rate"],
            candidates[name]["precision"],
            candidates[name]["average_precision"],
        ),
    )
    model, validation_probability = trained[selected_name]
    threshold = float(candidates[selected_name]["threshold"])
    holdout_probability = model.predict_proba(holdout_x)[:, 1]
    validation_metrics = metrics_for(
        validation_x.index,
        validation_y.to_numpy(),
        validation_probability,
        close_returns.loc[validation_x.index].to_numpy(),
        threshold,
    )
    holdout_metrics = metrics_for(
        holdout_x.index,
        holdout_y.to_numpy(),
        holdout_probability,
        close_returns.loc[holdout_x.index].to_numpy(),
        threshold,
    )
    forced_half = threshold_for_half_capture(
        validation_probability, validation_y.to_numpy()
    )
    payload = {
        "method": {
            "target_rise": TARGET_RISE,
            "forward_minutes": FORWARD_BARS * 5,
            "fee_rate_per_side": FEE_RATE,
            "max_signals_per_day": MAX_SIGNALS_PER_DAY,
            "split": [0.60, 0.20, 0.20],
            "news_replayed": False,
            "gemini_replayed": False,
        },
        "study_start": features.index[0].isoformat(),
        "study_end": features.index[-1].isoformat(),
        "samples": len(features),
        "train": {
            "start": train_x.index[0].isoformat(),
            "end": train_x.index[-1].isoformat(),
            "samples": len(train_x),
            "rise_events": int(train_y.sum()),
        },
        "candidate_validation": candidates,
        "selected_model": selected_name,
        "threshold": threshold,
        "validation_half_capture": forced_half,
        "validation": asdict(validation_metrics),
        "holdout": asdict(holdout_metrics),
        "peak_return_summary": {
            "validation_median": float(peak_returns.loc[validation_x.index].median()),
            "holdout_median": float(peak_returns.loc[holdout_x.index].median()),
        },
    }
    json_path = args.output_dir / "gemini_hourly_direction_results.json"
    report_path = args.output_dir / "GEMINI_HOURLY_DIRECTION_BACKTEST_RU.md"
    json_path.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2, default=json_default) + "\n",
        encoding="utf-8",
    )
    report_path.write_text(make_report(payload), encoding="utf-8")
    print(report_path)
    print(json_path)
    print(json.dumps(payload["validation"], ensure_ascii=False, indent=2))
    print(json.dumps(payload["holdout"], ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
