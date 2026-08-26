# PUMP / PumpBot — CURRENT ARCHITECTURE

Снимок фактической архитектуры целевой V5.37 на 2026-08-26. Это описание существующего кода ветки V5.37; после merge `main` должен соответствовать этому состоянию.

## 1. RUNTIME / ORCHESTRATION

Центральный runtime — Android foreground `PumpSignalService`.

- `START_STICKY`, `stopWithTask=false`: закрытие UI не означает остановку мониторинга.
- Нормальный цикл около 2 минут; интенсивность может меняться для важного состояния/позиции.
- `MicroImpulseStream` запускает быстрые локальные shock/entry checks без обязательного AI-запроса.
- Главный цикл и fast/shock path защищены от параллельного повторного запуска.
- Большинство необязательных стадий обёрнуты в `CycleStageGuard`, чтобы падение одного модуля не отменяло независимые последующие стадии.

Фактический упрощённый полный цикл:

`MarketSync → evidence/context → Bitpanda read-only → personal guard → DeepSeek/position advisers → APP → PM PUMP_3 → PM PUMP_2 → RETEST → SAFE → Fusion → audit/alerts → performance ledger → unified log`

Обязательный `MarketSyncClient.sync()` находится раньше failure-isolated optional stages: его полная ошибка может оборвать текущий полный цикл.

### V5.37 fast-path independence

Один общий `SharedFusionEntryObservation` используется как market evidence, но `PumpFastCandidatePolicyV537` отдельно вычисляет fast-candidate для `PUMP_3`, `PUMP_2`, `PUMP_RETEST` и `PUMP_SAFE`.

Каждый Pump Machine получает быстрый sync только если **его собственная** позиция открыта или **его собственный** fast-candidate активен. В V5.36 общий `commonFastCandidate` вычислялся только через строгий `PUMP_3` и затем использовался для всех четырёх PM; из-за этого responsive PUMP_2 мог не получить быстрый ~15s цикл вовремя.

## 2. MARKET DATA

### Binance/public data

`MarketSyncClient` + `PumpBotEngine` получают:

- PUMP spot 30m;
- EUR, BTC, ETH, SOL контекст;
- PUMP futures 30m;
- premium index;
- funding;
- top-20 depth;
- open interest;
- live PUMP/EUR-related ticker data.

Spot public endpoints идут через `data-api.binance.vision`; futures/OI/funding — через `fapi.binance.com`.

История свечей инкрементально объединяется только по закрытым данным (`closeTime < now`).

### Bitpanda Fusion

`BitpandaFusionClient` — **GET-only read-only** клиент для `PUMP-EUR` order book.

Он даёт:

- executable bid/ask;
- mid/spread;
- top-20 depth в EUR;
- timestamp freshness.

API key шифруется Android Keystore (`BitpandaFusionSecureKeyStore`). Команды order/cancel/transfer в клиенте отсутствуют.

### High-frequency/local evidence

В проекте существуют `MicroImpulseStream`, `LiveMarketBreathing`, `BuyerBreathCycle`, `UnifiedFlowEngine`, `CapitalFlowProxy`, `LargeFlowFingerprint`, `ImpulseRadar`, order-book и flow stores. Они формируют более короткие горизонты, чем базовые 30m candles.

Общие market observations — допустимый shared evidence. Они не должны автоматически означать общий candidate/cooldown/AI verdict или общий portfolio.

## 3. ANALYSIS LAYERS

Основные текущие уровни:

- `ResearchDecisionEngine` — причинный детерминированный APP baseline по закрытым candles.
- `LiveMarketBreathing` / `BuyerBreathCycle` — buyer-pressure lifecycle и multi-horizon состояние.
- `UnifiedFlowEngine` / `FusionFlowPolicy` — instant/5m/15m/20m/30m flow и признаки deterioration/exit.
- `AdaptiveBreathEntryPolicy` — V5.33+ относительная оценка входа: imbalance, acceleration, book, activity, price efficiency, phase, absorption и др.
- `CapitalFlowProxy` / `LargeFlowFingerprint` — признаки участия капитала; после V5.33 абсолютный USDT-порог не является обязательным главным замком для четырёх PM-профилей.
- `LiquidityReleaseShadow` — V5.32 причинный seller-exhaustion/liquidity-release observer. Он сохраняет evidence, но не имеет trading authority.
- `RapidDropDetector`, `ShockReboundStore`, market/context guards — аварийные/режимные признаки.
- `PumpFastCandidatePolicyV537` — только orchestration helper для profile-specific fast eligibility; он не вводит новый trading threshold.

## 4. STRATEGY / PAPER EXECUTION MODULES

### APP

`AppPaperStore` / `AppPaperTrader` используют V5 research baseline. Историческая архитектура V5 была построена вокруг `ResearchDecisionEngine`, `ResearchPositionEngine` и `ResearchReplayEngine`.

### User-facing Pump Machine mapping

Здесь есть важное историческое несоответствие имён:

| UI | Реальный профиль/хранилище | Текущая цель |
|---|---|---|
| Pump Machine 1 | `PumpMachine2Store` / `PUMP_2` | +2.00% NET, hard stop -1.10% |
| Pump Machine 2 | `PumpMachineStore` / `PUMP_3` | +3.00% NET, hard stop -1.30% |
| Pump Machine 3 RETEST | `PumpMachineRetestStore` / `PUMP_RETEST` | +2.00% NET, stop -1.10% + retest |
| Pump Machine 4 SAFE + APP | `PumpMachineSafeStore` / `PUMP_SAFE` | +1.15% NET, stop -0.75% + APP evidence |

Все четыре используют `PumpProfitEngineV526` для текущего PM entry/position logic, но имеют отдельные portfolios/state/cooldowns. RETEST намеренно зависит от retest/rebound сценария, SAFE намеренно требует APP evidence; это design dependencies, а не случайное разделение state.

BUY выполняется виртуально по свежему Bitpanda `ask`, SELL — по `bid`, с `FusionTradingCosts.FEE_RATE = 0.25%` за сторону.

### Fusion

`FusionSimStore` — отдельный paper portfolio. `FusionFlowPolicy` и `FusionStabilityPolicy` используют flow/breathing, anti-churn, price confirmation, trailing/profit-defense/cooldown. Это отдельная стратегия, не общий счёт Pump Machine.

### DeepSigX

Текущий экран показывает `DEEPSIGX`, но portfolio по причинам совместимости хранится через `GeminiExitExperimentStore`/Gemini-named legacy classes. Это техническое имя не означает, что Gemini имеет общий trading authority над Pump Machine.

### SERGE

`UserPaperStore` / manual-position функции представляют ручное действие пользователя и reference account. Они не создают реальный exchange order.

## 5. DEEPSEEK / AI COMPONENTS

- `DeepSeekPrimaryAnalyst` — основной AI market-analysis circuit.
- `DeepSeekEntryCoach` — V5.34+ вызывается **после** того, как локальный PM candidate уже сформирован. Он классифицирует фазу/качество и не может отменять hard veto.
- `DeepSeekEntryTuning` содержит восемь ограниченных soft-регуляторов.
- `DeepSeekAdaptiveTuningGuard` (V5.36) хранит предыдущую настройку, разрешает одно активное испытание и reconcile по закрытым NET outcomes.
- Новая автоматическая юстировка требует не менее 8 закрытых paper-сделок до применения, confidence >=85 и не чаще одного малого шага за 24 часа.
- Trial обычно оценивается после 6 новых закрытых сделок, максимум 10 или 7 дней; существенное ухудшение вызывает rollback.
- Hard veto, exits, реальные ордера и stored portfolios не являются областью автоматической юстировки.
- V5.35 ограничивает предвходный coach 6 запросами/UTC day с интервалом 15 минут; compatible verdict может переиспользоваться до 10 минут.

### V5.37 profile-scoped coach state

V5.37 разделяет **торговое решение** и **общий внешний ресурс**:

- `DeepSeekEntryCoachState` хранит `candidateProfile`;
- cached `APPROVE/WAIT/REJECT` compatible только с тем же `PumpProfitModeV526`;
- `PENDING` одного профиля не является `PENDING` другого профиля;
- ordinary retry/backoff относится к запросившему профилю;
- provider-level `PAUSED_BALANCE` остаётся глобальным, потому что это состояние самого API provider/account;
- `running` lock, 6 requests/UTC day и 15-minute minimum interval остаются shared provider constraints;
- если отдельного профильного verdict нет и paid resource занят/ограничен, другой PM может пройти только через **свой** строгий local fallback; чужой verdict не наследуется;
- persisted V5.36 coach state без поля profile загружается как `UNKNOWN` и не считается compatible ни с одним PM.

Gemini-компоненты в текущем коде в основном относятся к legacy compatibility/second-opinion/position-adviser и DeepSigX history; наличие Gemini-named класса не следует интерпретировать как право на автоматическую сделку.

### Shared tuning layer

`DeepSeekEntryTuning` имеет profile-specific score offsets, но часть soft regulators (`decelerationGap`, chase/confirmation/absorption tightening) общая, а recent closed PM outcomes подаются coach совместно.

Это **не прямой блокирующий state**, как старый cached/PENDING verdict, но создаёт возможную корреляцию экспериментов. `NEEDS_VERIFICATION`: доказать по forward outcomes, полезен ли общий learning layer. Не разрывать его следующей заплаткой без evidence.

## 6. STATE / PERSISTENCE

Состояние распределено между несколькими механизмами:

- `SharedPreferences` — engine state, individual paper portfolios, cooldowns, AI state/tuning, alert settings, snapshots.
- Android Keystore + encrypted preferences — API credentials.
- `ResearchPerformanceLedger` — SQLite append-only `research_performance_ledger.db`, объединяющий V4 archive и V5+ trades/decisions.
- `ResearchHistoryArchive` — историческая совместимость V4→V5.
- `UnifiedResearchLog`, market/audit logs и rolling retention — диагностическое evidence.

`ResearchPerformanceLedger` специально не очищается при смене app version.

Совместимая Android-линия сохраняет `applicationId = com.example.pumppaperbot.v8`. V5.37 — новая версия существующего приложения, а не второй package; clean install/uninstall не требуется для самой архитектурной правки.

## 7. SIGNAL LIFECYCLE

Для типичного Pump Machine входа:

1. Получить fresh market + executable Bitpanda ask/bid.
2. Сформировать общий multi-horizon flow/breath snapshot.
3. Для каждого PM отдельно `AdaptiveBreathEntryPolicy` считает score и hard veto.
4. `PumpProfitEngineV526` ведёт профильный candidate/hysteresis/confirmation и price acceptance.
5. Fast orchestration в V5.37 не требует, чтобы более строгий PUMP_3 стал кандидатом раньше PUMP_2/RETEST/SAFE.
6. Если локальный BUY готов, `DeepSeekEntryCoach.review()` использует только compatible verdict того же профиля либо строго ограниченный local fallback.
7. При разрешении paper BUY записывается по ask с fee; portfolio/state сохраняются отдельно.
8. Решение и причина идут в local store и unified log.

Упрощённо:

`shared data → profile local candidate → hard veto → profile confirmation → profile-scoped coach/fallback → paper BUY → independent state → EXIT → paper SELL → NET PnL → ledger/log/UI`

## 8. VIRTUAL TRADE LIFECYCLE

1. Independent portfolio находится в EUR или PUMP.
2. BUY: весь выделенный paper cash преобразуется в PUMP по ask после buy fee.
3. Во время позиции рассчитываются NET liquidation value, peak, drawdown и strategy-specific exit evidence.
4. EXIT может быть TP/SL, breakeven/profit giveback, early adverse-flow, timeout, shock/system exit — в зависимости от профиля.
5. SELL проходит по bid после sell fee.
6. Closed trade/decision сохраняется в portfolio и затем захватывается append-only performance ledger.
7. UI показывает balance, signed return, position state и markers.

Реальная биржевая заявка в этом lifecycle отсутствует.

## 9. UI

`AndroidManifest.xml` регистрирует `MainActivity` и отдельные экраны/activities для:

- Competition;
- Pump Machine;
- Critical/Big Overview;
- charts/backtest;
- APP paper;
- DeepSeek/Gemini experiments/API center;
- Bitpanda Fusion;
- alert settings/event radar.

`CompetitionActivity` отображает **8 вертикальных графиков** с синхронизированным horizontal offset: четыре Pump Machine, Fusion, DeepSigX, APP, SERGE. Нажатие графика открывает детальный диалог со сделками.

## 10. EXTERNAL APIs

Подтверждено в коде:

- Binance public spot/futures data;
- Bitpanda Fusion read-only order book;
- DeepSeek API;
- Gemini API для отдельных adviser/legacy функций.

`UNKNOWN`: полный фактический расход/доступность внешних AI-провайдеров в данный момент — это runtime/account state, а не свойство репозитория.

# ARCHITECTURAL DEBT

## A. Два исторических архитектурных слоя

V5.0 начинался как research baseline с APP/DeepSig/DeepSigX и общим причинным replay-подходом. V5.21+ поверх него вырос отдельный fast Pump Machine/Fusion контур. Оба слоя живы одновременно. Это не обязательно ошибка, но граница между «research baseline» и «активным PM execution experiment» сейчас не выражена одним явным интерфейсом.

`NEEDS_VERIFICATION`: имеет ли полный V5.37 Pump Machine path полноценный replay/walk-forward harness, который воспроизводит тот же 15-second flow/book/profile-scoped DeepSeek-coach contract. Для исходного `ResearchDecisionEngine` такой подход документирован; для полного текущего PM-контура эквивалентность пока не установлена.

## B. Имена больше не отражают смысл

- UI `Pump Machine 1` фактически использует `PumpMachine2Store`/PUMP_2.
- UI `Pump Machine 2` использует `PumpMachineStore`/PUMP_3.
- DeepSigX живёт в Gemini-named storage/classes.
- Application class называется `V513Application` при текущей версии V5.37.

Это повышает риск, что будущий агент исправит не тот счёт.

## C. Проверенное противоречие stop-текста и кода

Фактические константы:

- PUMP_2: `-1.10% NET`;
- PUMP_3: `-1.30% NET`.

Но в некоторых комментариях/status strings `PumpMachine2.kt`/`PumpMachine.kt` всё ещё осталось старое `-1.5%`. Это документационно/UI-противоречие, не основание менять сами константы без отдельного решения.

## D. Сильная фрагментация persistence

Много version-suffixed SharedPreferences и legacy stores обеспечивают совместимость, но создают риск частичного сброса/расхождения состояния. `PumpBotEngine.ensureInitialized()` мигрирует engine algorithm versions 17–20→21, а неизвестную версию отправляет в `reset(context)` для своего prefs store.

`NEEDS_VERIFICATION`: покрыты ли тестами все реальные upgrade-paths старых совместимых установок до V5.37.

## E. Patch-oriented история разработки

Каталог `tools/` содержит последовательные `apply_v519_*` … `apply_v526_*` scripts. Они полезны как исторический след, но показывают, что значимая часть развития шла наслоением version patches.

V5.33→V5.36 уже дали серию ремонтов одной области. V5.37 поэтому применяет правило трёх ремонтов: не меняет очередной threshold, а устраняет подтверждённую cross-profile coupling. Следующий ремонт в той же области требует нового architecture review.

## F. Частично устаревшие комментарии/документация

`ResearchModePolicy` всё ещё может содержать историческое описание числа автономных систем; version-specific README/comments не являются текущей спецификацией.

## G. Release-state drift

Source/build, совместимо подписанный APK и опубликованный GitHub Release исторически являются разными состояниями. Intermediate CI APK нельзя автоматически называть install-compatible final APK без проверки certificate/update path.

Фактически установленная версия и current device outcomes: `UNKNOWN`, пока это не подтверждено с устройства.

## H. Дублирование strategy wrappers

`PumpMachine.kt`, `PumpMachine2.kt` и generic variant store повторяют близкие операции mark/buy/sell/persistence. Часть общей логики уже вынесена в `PumpProfitEngineV526`, но duplication остаётся источником расхождения текстов/правил.

Не начинать массовый рефакторинг только ради красоты. Этот долг должен устраняться при доказанной связи с ошибками или регрессиями.

## I. Shared tuning correlation

V5.37 исправляет cached/PENDING entry decision как profile-scoped, но часть bounded DeepSeek tuning остаётся shared. Это известная возможная корреляция, а не скрытый неизвестный факт. До representative performance baseline не менять её архитектуру без отдельного решения.
