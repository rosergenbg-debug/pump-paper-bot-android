# PUMP / PumpBot — CURRENT STATE

Обновлено: **2026-08-28**

Это короткий снимок текущего состояния. Старую историю сюда не накапливать.

## VERSION / BRANCH

- Canonical branch after merge: `main`.
- Current source version in this change: **V6.5**, `versionCode 123`.
- `applicationId`: `com.example.pumppaperbot.v8` — сохранён для совместимого update/data continuity.
- V6.5 implementation PR: **#87**, branch `v6.5-t32-profit-variants`.
- Реальные ордера не добавлены: все новые T32 варианты остаются paper/research-only.
- Production commit, от которого велись последние research replay: `cbc10b4948bd22cbf5684b36596121ee562c8614`.

## ОБЯЗАТЕЛЬНАЯ ПАМЯТЬ X

В корне репозитория создана защищённая папка `X/`.

Каждый новый чат обязан читать в порядке:

`ИНСТРУКЦИЯ_1.md` → `AGENTS.md` → `X/README.md` → project-memory.

`X/` хранит текущие лучшие измеренные T32/VWAP checkpoints и защищает проект от потери уже найденной линии:

- исторический T32/VWAP canary: **32,65% WR** (`1772 signals / 974 fills / 318 positive`);
- тот же T32 + `-4% / rolling peak 12h`: **34,88% WR**;
- тот же -4%/12h + VWAP exit при STOP `-1,2%`: **52,03% WR**;
- тот же -4%/12h + VWAP exit при STOP `-1,5%`: **57,36% WR**;
- высокий WR сам по себе не равен прибыльности: эти 52–57% варианты всё ещё имели отрицательный Avg NET/PF < 1;
- текущий лучший экономический компромисс: `-4%/12h + TP +2,5% NET + STOP -1,2% NET + max 2 entries/day`, с WR **40,21% / 43,14% / 37,50%** на 60d/30d/7d и PF до **0,982** на recent 7d.

Точные цифры и provenance лежат в `X/RESULTS_2026-08-28.md`.

## V6.5 — FOUR T32 EXPERIMENTS

Один и тот же исходный T32/VWAP entry evaluator теперь сравнивается на четырёх независимых paper-счетах:

1. `T32 ORIGINAL` — полностью автоматический вход T32; исходный VWAP/STOP/90m выход сохранён как контроль.
2. `T32 +1,5% NET` — полностью автоматический вход T32; автоматический TP только после достижения +1,5% NET.
3. `T32 +2,0% NET` — полностью автоматический вход T32; автоматический TP только после достижения +2,0% NET.
4. `HUMAN +2,0% NET` — T32 создаёт предложение входа; BUY выполняется только после кнопки владельца `ВОЙТИ`; после этого выход автоматический при +2,0% NET либо по safety STOP/90m.

- Все четыре ветви используют T32 commission model **0,21% BUY + 0,21% SELL**.
- Fixed-profit target вычисляется математически так, чтобы после обеих комиссий осталось ровно +1,5% или +2,0% NET; это не простое прибавление 0,42 п.п. к цене.
- Для production V6.5 fixed-profit/Human ветвей safety exit пока сохранён: `NET <= -0,80%` или максимум 90 минут. Последний research показывает, что `-0,80%` вероятно слишком тесный для исследовательской линии, но production пока не изменён.
- Новые +1,5%/+2,0% portfolios имеют отдельные `SharedPreferences`; существующие T32 ORIGINAL и Human Factor сохраняют прежние prefs/history для continuity.

## HUMAN FACTOR ALERT

- Human Factor entry больше не зависит от обычного preparatory-alert schedule/master sound gate.
- Используется отдельный high-importance alarm channel `pump_human_factor_v650`.
- При pending Human Factor выполняются notification alarm + сильная vibration + direct alarm sound attempt.
- Пока setup остаётся pending, alarm самостоятельно повторяется примерно раз в 60 секунд между полными торговыми циклами.
- `ВОЙТИ`, `ОТКЛОНИТЬ`, распад setup или уже открытая Human позиция останавливают повтор.
- Android DND/ручное отключение системного канала/жёсткие OEM-ограничения всё ещё могут переопределить звук на уровне ОС; приложение максимально дублирует delivery, но не может обойти системный запрет.

## FOCUSED NETWORK UI

Экран сети V6.5 содержит шесть owner-facing счетов в порядке:

`T32 ORIGINAL → T32 +1,5% NET → T32 +2,0% NET → HUMAN +2,0% NET → СЕРЖ → APP`

Старые PM/Fusion/DeepSigX расчёты и persisted data не удалены; они остаются вне focused network.

## REPORTING / 24H TXT

- Основной support export остаётся UTF-8 `.txt` за 24 часа с разбиением примерно до 900 KB на часть.
- V6.5 TXT явно включает текущее состояние всех четырёх T32 ветвей: value, position, readiness/pending, target и status.
- BUY/SELL четырёх T32 ветвей за выбранное 24h окно добавляются в `[TRADES_LAST_24H]`.
- `UnifiedResearchLog` дополнительно получает отдельные агенты `T32_ORIGINAL`, `T32_NET_1P5`, `T32_NET_2P0`, `T32_HUMAN_2P0`; Human также пишет `ALERT`, `PENDING`, `REJECT`.
- Старый V6 execution sample/outcome journal сохранён и продолжает попадать в тот же support TXT.

## VERIFICATION TARGET

Перед merge V6.5 обязана пройти GitHub Actions:

- `testDebugUnitTest`
- `lintDebug`
- `assembleDebug`
- package/version/activity checks
- APK v2 signature check
- ZIP integrity / artifact upload

Добавлены regression tests для точной 0,21% fee model, +1,5/+2,0 NET target math, repeat-policy Human alarm и 6-account focused order.

## WHAT REMAINS UNPROVEN

- Win rate 32,65% — исторический research baseline, а не гарантия будущей прибыли.
- 52,03%/57,36% из stop sweep показывают чувствительность WR к слишком тесному STOP, но **не являются доказательством прибыльной стратегии**.
- Текущий fixed `+2,5% / -1,2% / max2/day` кандидат близок к breakeven, но Avg NET всё ещё отрицателен на 60d/30d/7d.
- Что production TP +1,5% или +2,0% улучшит expectancy по сравнению с original VWAP exit — **не доказано**; именно поэтому варианты разделены на независимые portfolios.
- Польза Human подтверждения должна оцениваться по будущим NET outcomes, а не по отдельным удачным сделкам.
- Реальные ордера всё ещё не реализованы.

## CURRENT PRIORITY

Не терять защищённую линию из `X/`.

Для следующего research шага заморозить текущий лучший экономический кандидат:

`T32 entry + BELOW4_PEAK12H + LIMIT -0,10% TTL2 + TP +2,5% NET + STOP -1,2% NET + max 2 entries/day`.

Не менять вход и риск одновременно. Следующий изолированный тест — TIME/exit: **60 / 90 / 120 / 180 минут**, возврат к VWAP и ранний защитный выход при угасании импульса. Причина: на control 60d у текущего кандидата **34 из 97** сделок закрылись по 90m TIME.

Параллельно продолжать накапливать forward outcomes production V6.5, не смешивая их с historical research.

## INVARIANTS

1. Не менять package/signing identity.
2. Не удалять установленное приложение ради update.
3. Не очищать накопленную history/state.
4. T32 ORIGINAL сохранять как контроль, пока fixed-profit варианты не накопят достаточное evidence.
5. Не смешивать portfolios четырёх T32 ветвей.
6. Оценивать результаты по NET после расходов.
7. Не добавлять live-order authority без отдельного явного решения владельца.
8. Перед новым T32 replay читать `X/README.md` и воспроизводить соответствующий canary.
9. Не стирать старые checkpoints из `X/` при появлении нового результата.