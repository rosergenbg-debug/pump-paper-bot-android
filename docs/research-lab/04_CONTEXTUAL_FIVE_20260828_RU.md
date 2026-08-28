# Пять контекстных гипотез — причинный replay

Дата: 2026-08-28. Статус: research-only, production-стратегии не изменены.

## Методика

- PUMPUSDT/BTCUSDT/SOLUSDT, публичные Binance spot klines, 1 минута.
- 120 дней / 172 800 общих выровненных минут.
- Early: 2026-04-30 07:55 — 2026-06-29 07:54 UTC.
- Control: 2026-06-29 07:55 — 2026-08-28 07:54 UTC.
- Параметры зафиксированы до просмотра результата.
- Сигнал использует только закрытые доступные свечи; одна позиция/pending-заявка.
- Комиссия 0,21% за сторону; adverse slippage market-exit 0,08%; limit/stop-limit имеют TTL.
- Неизвестный intrabar-порядок разрешается консервативно: stop раньше target.

Воспроизведение:

`node tools/research_contextual_five.js 120 2026-08-28T07:55:00Z`

## Зафиксированные формулы

1. Regime-adaptive: Efficiency Ratio(20); range rebound у нижней границы либо pullback в положительном тренде; limit; +1,2/−0,8% NET.
2. Liquidity sweep: прокол 30m low минимум на 0,1%, возврат закрытия выше уровня, зелёная свеча и нижняя тень; limit на уровне; +1,0/−0,7% NET.
3. VWAP mean-reversion: цена минимум на 0,4% ниже причинного 60m VWAP, зелёная свеча и восстановление taker-buy share; limit; выход к VWAP, stop −0,8%, максимум 90 минут.
4. Sell exhaustion: taker-sell минимум 1,5× среднего 20m, low удержан, зелёная свеча и восстановление buy share; stop-limit; +1,3/−0,8% NET.
5. 4H node + relative strength: возле причинного 4H volume node, PUMP сильнее среднего BTC/SOL минимум на 0,2 п.п., SELL-flow затухает, UTC 08–16; stop-limit; +1,5/−0,9% NET.

## Synthetic

Для каждой гипотезы проверены разрешённый и запрещённый сценарии: **10/10 PASS**. Это подтверждает ветвление, но не прибыльность.

## Результаты

| Гипотеза | Период | Filled | Win rate | Avg NET | PF |
|---|---:|---:|---:|---:|---:|
| Regime-adaptive | early | 295 | 18,31% | −0,515% | 0,287 |
| Regime-adaptive | control | 470 | 20,43% | −0,465% | 0,345 |
| Liquidity sweep | early | 89 | 14,61% | −0,520% | 0,219 |
| Liquidity sweep | control | 104 | 18,27% | −0,462% | 0,283 |
| VWAP reversion | early | 708 | 28,67% | −0,517% | 0,170 |
| VWAP reversion | control | 974 | **32,65%** | −0,446% | 0,251 |
| Sell exhaustion | early | 471 | 17,41% | −0,513% | 0,300 |
| Sell exhaustion | control | 685 | 21,46% | **−0,419%** | **0,399** |
| Node + relative strength | early | 95 | 18,95% | −0,533% | 0,329 |
| Node + relative strength | control | 118 | 20,34% | −0,486% | 0,373 |

## Вывод

- Цель win rate >50% не достигнута. Максимум на control: 32,65%.
- Положительной средней NET-прибыли нет. Лучший control Avg NET: −0,419%.
- Ни у одной гипотезы PF не приблизился к 1; лучший control PF 0,399.
- VWAP повысил долю положительных выходов, но движение к VWAP часто недостаточно для покрытия round-trip costs.
- Liquidity sweep на минутной агрегации часто остаётся продолжением снижения, а не разворотом.
- Sell exhaustion — лучший по control expectancy/PF, но результат всё равно явно отрицательный.
- Комбинация node/relative strength/SELL decay/session не создала синергии.

Ни одна формула не получает production authority и не должна добавляться в существующие автономные счета. Повторная оптимизация этих порогов на тех же 120 днях методологически запрещена. Возможный следующий шаг — trade-level tape/order-book replay для absorption/liquidity sweep, где минутные свечи не теряют порядок сделок и реальное поглощение.
