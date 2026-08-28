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

Воспроизводимые инструменты находятся в `tools/`:

- `research_cross_market_breath.js`;
- `research_order_execution.js`;
- `research_flow_absorption.js`.

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

## Текущая лучшая следующая гипотеза

Замороженная идея для новых, ещё не просмотренных forward-данных:

`UTC 08–16 + negative 4h context + deep fall + near 4h volume node + relative strength + SELL decay + stop-limit entry with TTL + OCO +2%/-1.10% NET`.

Она сформулирована после анализа текущих 30 дней, поэтому на этих же данных не может получить статус доказанной. Любой следующий тест должен сохранить формулу без подбора порогов на control.

## Текущий честный итог

- Логические synthetic-сценарии работают.
- Несколько признаков дают lift и приближают отдельные выборки к breakeven.
- Ни одна фиксированная гипотеза пока не дала положительный средний NET одновременно на train и control.
- Целевые 60–70% прибыльных сделок не достигнуты.
- Production-стратегии и версия приложения этими исследованиями не изменялись.
