# PUMP / PumpBot — CURRENT ARCHITECTURE

Снимок фактической архитектуры целевой V5.37 на 2026-08-26. Это описание существующего кода ветки V5.37; после merge `main` должен соответствовать этому состоянию.

## 1. RUNTIME / ORCHESTRATION

Центральный runtime — Android foreground `PumpSignalService`.

- `START_STICKY`, `stopWithTask=false`: закрытие UI не означает остановку мониторинга.
- Нормальный полный цикл около 2 минут; интенсивность может повышаться для важных состояний/позиций.
- `MicroImpulseStream` запускает быстрый локальный ~15-second путь.
- Главный и fast/shock циклы защищены от параллельного повторного запуска.
- Необязательные стадии полного цикла обёрнуты в `CycleStageGuard`, чтобы ошибка одного модуля не отменяла независимые последующие стадии.

Упрощённый полный цикл:

`MarketSync → evidence/context → Bitpanda read-only → guards/AI evidence → APP → PUMP_3 → PUMP_2 → RETEST → SAFE → Fusion → audit/alerts → performance ledger → unified log`

### V5.37 fast-path

Один общий `SharedFusionEntryObservation` используется как market evidence, но `PumpFastCandidatePolicyV537` отдельно рассчитывает fast-candidate для:

- `PUMP_3`;
- `PUMP_2`;
- `PUMP_RETEST`;
- `PUMP_SAFE`.

Каждый Pump Machine получает быстрый sync только если **его собственная** позиция открыта или **его собственный** fast-candidate активен. В V5.36 все четыре зависели от `PUMP_3` candidate; это устранено.

## 2. MARKET DATA

### Binance/public

`MarketSyncClient` + `PumpBotEngine` получают PUMP spot/futures, BTC/ETH/SOL/EUR context, premium/funding, depth, open interest и ticker/history data. Closed candle history используется причинно (`closeTime < now`).

### Bitpanda Fusion

`BitpandaFusionClient` — **GET-only/read-only** источник `PUMP-EUR` executable bid/ask, spread и depth. API key хранится через Android Keystore. Order/cancel/transfer path в текущем проекте отсутствует.

### Short-horizon evidence

`MicroImpulseStream`, `LiveMarketBreathing`, `BuyerBreathCycle`, `UnifiedFlowEngine`, `CapitalFlowProxy`, `LargeFlowFingerprint`, `ImpulseRadar` и order-book stores формируют short-horizon evidence для скальпинговых решений.

Общее evidence между стратегиями допустимо; общее право на сделку — нет.

## 3. ANALYSIS / ENTRY LAYERS

- `ResearchDecisionEngine` — причинный APP baseline.
- `LiveMarketBreathing` / `BuyerBreathCycle` — lifecycle buyer pressure.
- `UnifiedFlowEngine` / `FusionFlowPolicy` — instant/5m/15m/20m/30m flow.
- `AdaptiveBreathEntryPolicy` — V5.33+ relative entry score и hard veto: freshness, executable ask, spread, seller dominance, excessive absorption, late phase/chase, deceleration и др.
- `PumpProfitEngineV526` — профильный PM candidate confirmation, paper execution и position exits.
- `LiquidityReleaseShadow` — causal observer без trading authority.

## 4. STRATEGIES / PAPER ACCOUNTS

| UI | Реальный профиль/store | Цель |
|---|---|---|
| Pump Machine 1 | `PumpMachine2Store` / `PUMP_2` | responsive +2.00% NET, hard stop -1.10% |
| Pump Machine 2 | `PumpMachineStore` / `PUMP_3` | strict +3.00% NET, hard stop -1.30% |
| Pump Machine 3 RETEST | `PumpMachineRetestStore` / `PUMP_RETEST` | +2.00% NET, retest/rebound |
| Pump Machine 4 SAFE + APP | `PumpMachineSafeStore` / `PUMP_SAFE` | +1.15% NET, stop -0.75%, APP evidence |
| Fusion | `FusionSimStore` | отдельная flow/breath state machine |
| DeepSigX | legacy Gemini-named store | отдельный experimental account |
| APP | `AppPaperStore` | deterministic research baseline |
| SERGE | `UserPaperStore` | ручной reference/paper account |

Все paper accounts сохраняют раздельные portfolios/state/cooldowns/history. PM/Fusion BUY симулируется по Bitpanda ask, SELL по bid, fee `0.25%` за сторону. APP/legacy линия использует отдельную модель costs (`0.15%`/side), поэтому сравнение требует явной нормализации.

## 5. DEEPSEEK / AI

### Entry coach

`DeepSeekEntryCoach` вызывается только после local PM candidate. Он может влиять на разрешённый soft contract, но не отменяет hard veto.

V5.37 разделяет **решение** и **ресурс**:

- persisted `DeepSeekEntryCoachState` содержит `candidateProfile`;
- cached APPROVE/WAIT/REJECT compatible только с тем же `PumpProfitModeV526`;
- `PENDING` одного профиля не является `PENDING` другого профиля;
- обычный error backoff относится к запросившему профилю;
- provider-level `PAUSED_BALANCE` остаётся глобальным;
- `running` lock и максимум `6 requests/UTC day`, минимум 15 минут между paid requests остаются общими как ограничение внешнего API;
- если свежего профильного AI verdict нет и общий ресурс занят/ограничен, стратегия может пройти только через свой строгий local fallback — она не наследует verdict другого профиля.

### Adaptive tuning

`DeepSeekEntryTuning` содержит profile-specific score offsets и несколько shared soft regulators. `DeepSeekAdaptiveTuningGuard` разрешает один bounded trial с exact rollback.

**NEEDS_VERIFICATION:** pooled PM outcomes и shared soft regulators могут создавать корреляцию между стратегиями. Это не признано текущим bug, потому что tuning является общим market-learning layer и защищён trial/rollback, но его полезность должна проверяться по representative forward outcomes.

## 6. STATE / PERSISTENCE

- `SharedPreferences`: отдельные paper portfolios/state/cooldowns, AI/tuning state, settings/snapshots.
- Android Keystore: credentials.
- `ResearchPerformanceLedger`: append-only SQLite `research_performance_ledger.db`.
- `ResearchHistoryArchive`: V4→V5 continuity.
- `UnifiedResearchLog` и support/audit logs: evidence.

`applicationId` совместимой линии остаётся `com.example.pumppaperbot.v8`; V5.37 не создаёт второй Android app/package и не требует очистки данных.

## 7. SIGNAL LIFECYCLE

`shared market data → profile-specific local candidate → hard veto → profile confirmation → profile-scoped DeepSeek verdict/strict fallback → paper BUY ask → independent state → profile exit → paper SELL bid → NET PnL → ledger/log/UI`

Для fast-path shared market snapshot сначала один раз оценивается каждым profile gate; ни один PM не должен ждать, пока другой PM станет кандидатом.

## 8. VIRTUAL TRADE LIFECYCLE

1. Стратегия держит собственный EUR/PUMP paper portfolio.
2. BUY — по executable ask с соответствующей fee model.
3. Position state хранит entry/peak/risk/cooldown отдельно.
4. EXIT — TP/SL/breakeven/giveback/adverse-flow/timeout/shock согласно стратегии.
5. SELL — по executable bid и fee.
6. Trade/decision попадает в individual history, затем в append-only ledger.
7. UI показывает balance, position и markers.

Реального exchange order в lifecycle нет.

## 9. UI

`CompetitionActivity` отображает 8 вертикальных графиков: четыре PM → Fusion → DeepSigX → APP → SERGE. Историческое несовпадение UI PM1/PM2 и имён stores остаётся и требует осторожности при будущих изменениях.

## 10. EXTERNAL API BOUNDARIES

- Binance: public market data.
- Bitpanda Fusion: read-only order book.
- DeepSeek: analysis/entry coach/bounded tuning.
- Gemini: отдельные adviser/legacy/DeepSigX функции.

# ARCHITECTURAL DEBT

## A. Два исторических слоя

Research/replay слой V5 и более поздний fast PM/Fusion слой сосуществуют. `NEEDS_VERIFICATION`: полный V5.37 PM path (15s flow/book/profile-scoped coach) пока не имеет подтверждённого replay/walk-forward harness, воспроизводящего тот же runtime contract.

## B. Naming drift

- UI PM1 = `PumpMachine2Store` / PUMP_2.
- UI PM2 = `PumpMachineStore` / PUMP_3.
- DeepSigX хранится в Gemini-named legacy classes.
- Некоторые version-named classes/comments старше текущей V5.37.

Это повышает риск правки не того агента, но массовое переименование без migration plan не требуется.

## C. Stop text drift

Фактические PM2/PUMP_2 и PM3/PUMP_3 hard stops — `-1.10%` и `-1.30%`; отдельные старые status/comment strings всё ещё могут говорить `-1.5%`. Это text debt, не основание менять trading constants.

## D. Persistence fragmentation

Много versioned SharedPreferences/stores повышают migration risk. `NEEDS_VERIFICATION`: все исторические compatible upgrade paths до V5.37 покрыты не полностью.

## E. Shared tuning correlation

В V5.37 entry verdict/PENDING исправлены как profile-scoped, однако часть DeepSeek soft tuning намеренно shared. Перед дальнейшим разделением/объединением нужно сначала измерить outcomes, а не создавать новый patch.

## F. Patch-oriented history

Последовательность V5.33→V5.36 уже потребовала нескольких ремонтов одной области. V5.37 применяет правило трёх ремонтов: исправлена причина скрытой связности вместо очередной настройки thresholds. Следующий fix в этой же области требует нового architecture review.

## G. Release/replay drift

Source/build state и опубликованный GitHub Release исторически расходились; release artifact не равен автоматически install-compatible APK. Current device runtime outcomes и фактически установленная версия остаются `UNKNOWN`, пока это не подтверждено с устройства.
