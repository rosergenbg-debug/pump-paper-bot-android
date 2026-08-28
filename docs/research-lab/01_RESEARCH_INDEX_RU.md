# PUMP RESEARCH LAB — постоянный реестр исследований

Назначение папки: не терять проверенные гипотезы, отрицательные результаты, перспективные наблюдения, методику и воспроизводимые инструменты. Каждая следующая существенная симуляция получает новый нумерованный отчёт и строку в этом индексе.

## Правила

1. Синтетический PASS доказывает только правильность логики сценария, не прибыльность.
2. Исторический сигнал формируется только из уже доступных данных.
3. Результат обязательно делится по времени на research/train и control/test.
4. Комиссии, spread/slippage, незаполненные ордера и одна открытая позиция учитываются явно.
5. В production не продвигается гипотеза, которая положительна только в одной части или на малой выборке.
6. Отрицательный результат не удаляется: он защищает проект от повторения той же подгонки.

## Файлы

1. `01_RESEARCH_INDEX_RU.md` — этот индекс и краткий реестр.
2. `02_SYNTHETIC_REPLAY_REPORT_20260828_RU.txt` — полный объединённый отчёт по top-view, BTC/SOL, order types, целям, flow/absorption, режимам, микроотскоку, volume node и динамическим выходам.
3. `03_CROSS_MARKET_BREATH_20260828_RU.md` — отдельный компактный отчёт первого BTC/SOL replay.
4. `04_CONTEXTUAL_FIVE_20260828_RU.md` — regime-adaptive, liquidity sweep, VWAP, sell exhaustion и 4H node + relative strength на 120 днях.
5. `05_CONTEXTUAL_FIVE_SHARE_REPORT_RU.txt` — переносимый полный TXT-отчёт пяти контекстных гипотез.
6. `06_VWAP_GAP_FORWARD_REPORT_RU.txt` — отдельный out-of-sample replay cost-aware VWAP и session gap-fill.
7. `07_FOUR_FIXED_OHLCV_REPORT_RU.txt` — Z-Score VWAP, volatility squeeze, lead-lag и session-open breakout на третьем независимом блоке.

Воспроизводимые инструменты находятся в `tools/`:

- `research_cross_market_breath.js`;
- `research_order_execution.js`;
- `research_flow_absorption.js`;
- `research_five_hypotheses.js`.
- `research_contextual_five.js`.
- `research_vwap_gap_forward.js`.
- `research_four_fixed_ohlcv.js`.

## Реестр результатов на 2026-08-28

| Гипотеза | Данные | Лучший результат | Решение |
|---|---|---|---|
| Геометрия дна PUMP | 14 дней | control 31,3% wins при старой event-модели | Shadow; edge не найден |
| BTC/SOL simultaneous regime | 14 дней | 31,3% → 32,3% | Только режим/warning |
| SOL lead | 14 дней | 27,3% | Отвергнуто |
| Market/limit/stop-market/stop-limit | 30 дней | stop-limit лучше остальных, но общий NET отрицательный | Execution tool, не signal edge |
| Цели +1/+1,5/+2/+3 | 30 дней | общий лучший stop-limit +3: −0,296% mean NET control | Не продвигать |
| Deep fall >1,5% + stop-limit +2/−1,10 | 30 дней | train −0,023%, control −0,086% | Наиболее близко к breakeven; forward hypothesis |
| Absorption | 30 дней | train −0,374%, control −0,175% | Soft evidence |
| SELL-flow decay | 30 дней | train −0,380%, control −0,141% | Soft evidence |
| Relative strength PUMP vs BTC/SOL | 30 дней | train −0,505%, control −0,072% | Режимно, не gate |
| Relative strength + SELL decay | 30 дней | train −0,357%, control +0,013% на 16 fills | Новый forward-кандидат; выборка мала |
| UTC 08–16 | 30 дней | train −0,244%, control −0,185% | Стабильный lift, но не edge |
| Negative 4h context | 30 дней | train −0,247%, control −0,287% | Стабильный lift, но не edge |
| Volume spike | 30 дней | отрицательно в обеих частях | Отвергнуто как standalone |
| Soft score 3/4/5 | 30 дней | улучшает baseline, остаётся отрицательным | Не продвигать |
| ATR target + 25m time-stop | 30 дней | control около −0,53% | Конкретная формула отвергнута |
| Partial 50% at +1% + trailing | 30 дней | baseline train −0,490%, control −0,598% | Отвергнуто |
| Bounce candle quality + 2m hold | 30 дней | train +0,013%, control −0,914% | Не воспроизводится |
| Hard price-impact absorption | 30 дней | 3 train fills, 0 control fills | Слишком редко/зажато |
| Approximate 4h volume node + flow | 30 дней | train +0,410% (8), control −0,023% (11) | Почти breakeven, выборка мала |
| Three-regime policy | 30 дней | train +0,638% (7), control −0,543% (5) | Не воспроизводится |
| Quiet entry: deep fall + ATR(5) compression + limit | 60 дней, 30/30 | early −0,683% (20), recent −0,303% (11) | Отвергнуто в текущем виде |
| Fake breakout 60m resistance + pullback limit | 60 дней, 30/30 | early −0,503% (263), recent −0,370% (326) | Устойчивый минус; отвергнуто |
| 60m volume cluster + SELL decay + BTC/SOL | 60 дней, 30/30 | early −0,362% (9), recent −0,632% (8) | Минус и малая выборка; отвергнуто |
| BTC/SOL cross-impulse with PUMP lag | 60 дней, 30/30 | early −0,486% (19), recent −0,425% (42) | Lead edge не найден; отвергнуто |
| UTC 09–15 + negative 4h + high ATR + red-run rebound | 60 дней, 30/30 | early −0,403% (45), recent +0,113% (62), PF 1,255 | Research/shadow; recent plus не воспроизведён early |
| Пять контекстных гипотез: regime/sweep/VWAP/exhaustion/node+RS | 120 дней, 60/60 | лучший control WR 32,65%; лучший Avg NET −0,419%; лучший PF 0,399 | Ни одну не продвигать |
| Cost-aware VWAP + session gap-fill | отдельные 120 дней, 60/60 | оба control WR 8,33%; Avg NET −0,566% / −0,917% | Отвергнуть; прежние 32,65% не улучшены |
| Z-VWAP / squeeze / lead-lag / session breakout | отдельные 159 565 минут | лучший control WR 14,29%; все Avg NET <0; lead-lag 0 сигналов | Не продвигать |

## Текущая лучшая следующая гипотеза

Замороженная идея для новых, ещё не просмотренных forward-данных:

`UTC 08–16 + negative 4h context + deep fall + near 4h volume node + relative strength + SELL decay + stop-limit entry with TTL + OCO +2%/-1.10% NET`.

Она сформулирована после анализа текущих 30 дней, поэтому на этих же данных не может получить статус доказанной. Любой следующий тест должен сохранить формулу без подбора порогов на control.

Дополнительный замороженный кандидат после пяти новых тестов:

`UTC 09–15 + negative 4h + ATR(30) above causal 24h average + prior 3-red run + strong green close + stop-limit +0,05% TTL 1 + OCO +1,5%/-0,9% NET + 20m time-exit`.

На recent/control он дал +0,113% среднего NET и PF 1,255 на 62 исполнениях, но на равном early-периоде дал −0,403% и PF 0,373. Поэтому это только отдельная forward-гипотеза, не кандидат на немедленное внедрение.

## Текущий честный итог

- Логические synthetic-сценарии работают.
- Несколько признаков дают lift и приближают отдельные выборки к breakeven.
- Ни одна фиксированная гипотеза пока не дала положительный средний NET одновременно на train и control.
- Целевые 60–70% прибыльных сделок не достигнуты.
- Production-стратегии и версия приложения этими исследованиями не изменялись.
