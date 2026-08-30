# PUMP / PumpBot — REGRESSION MATRIX

Статусы:

- `AUTO` — есть прямой автоматический test/check.
- `PARTIAL` — часть поведения покрыта тестом/build, но не весь lifecycle.
- `MANUAL` — требуется runtime/device/release проверка.
- `NEEDS_TEST` — критично, но достаточного автоматического теста пока не установлено.

| Область | Ожидаемое поведение | Как проверяется | Статус |
|---|---|---|---|
| Build baseline | `testDebugUnitTest`, `lintDebug`, `assembleDebug` проходят | `.github/workflows/android.yml` | AUTO |
| Package/version | compatible line остаётся `com.example.pumppaperbot.v8`; V6.9/code129 | CI `aapt dump badging` | AUTO |
| Launch APK | launch activity `MainActivity`, APK ZIP валиден, v2+v3 и exact compatible certificate проверяются | CI final APK checks | AUTO |
| V6.5 runtime restoration | V6.7 использует полный MainActivity/service runtime V6.5; эксперимент не заменяет центральный app | manifest/source diff + service stage review | PARTIAL |
| V6.7 owner mapping | Сеть содержит AUTO X ECONOMY → AUTO X52 SELECT → HUMAN +2.0 → SERGE → APP | `CompetitionAccountSpecTest` + UI device check | AUTO/PARTIAL |
| X causal entry data | T32/12h контекст вычисляется по raw closed Binance PUMPUSDT 1m candles, не по 30m/latest proxy | `T32NetworkV670Test` + source review | PARTIAL |
| X execution currency | Binance USD intent не сравнивается с EUR ask; virtual fill uses fresh executable Bitpanda PUMP/EUR ask | source review + runtime paper journal | PARTIAL |
| AUTO X ECONOMY exits | TP +2.5% NET, STOP -1.2% NET, TIME 120m, max 2 entries/UTC day | `T32NetworkV670Test` + runtime lifecycle | PARTIAL |
| AUTO X52 SELECT | требует SOL-BTC REL6 >= +0.40; dynamic VWAP exit, STOP -1.2%, TIME 90m | `T32NetworkV670Test` + runtime lifecycle | PARTIAL |
| 57% claim guard | 57.36% WR не называется прибыльностью: replay также фиксирует Avg NET -0.297% и PF 0.533 | X report + project-memory review | MANUAL |
| Real trading | Ни одна текущая автономная стратегия не отправляет реальный order/cancel/transfer | source review, `ResearchModePolicy`, `V49SafetyPolicyTest` и related safety tests | PARTIAL |
| Bitpanda access | Fusion client использует read-only GET order book; key не пишется plaintext в prefs/log | `BitpandaFusionTest` + source review | PARTIAL |
| Persistent history | App version changes не очищают append-only SQLite ledger | `ResearchPerformanceLedger`, `ResearchHistoryArchiveTest`, retention tests | PARTIAL |
| V4→V5 history | доступная V4 history захватывается/остаётся экспортируемой | `ResearchHistoryArchiveTest`, ledger capture | PARTIAL |
| Compatible update data | portfolios/settings/keys/history переживают update без uninstall | version-specific retention tests + final APK upgrade on device | MANUAL |
| Engine migration | известные `PumpBotEngine` algorithm versions мигрируют без неожиданного reset | source migration + targeted tests | NEEDS_TEST |
| Independent accounts | AUTO X ECONOMY/AUTO X52/HUMAN/APP/SERGE используют раздельные portfolios; hidden legacy stores не очищаются | store separation + strategy tests + Competition inspection | PARTIAL |
| V6.7 X fee model | Обе AUTO X линии считают 0,21% BUY + 0,21% SELL; fixed TP ECONOMY даёт +2.5% NET после обеих комиссий | `T32NetworkV670Test` | AUTO |
| Human control | HUMAN сохраняет owner-confirmed V6.5 entry authority и отдельный portfolio | `HumanFactorVwapTest` + source review | PARTIAL |
| Retired T32 variants | Старые auto +1.5/+2.0 stores не очищаются, но их sync не запускается в V6.7 owner runtime | service/source review | PARTIAL |
| X failure isolation | Ошибка одной AUTO X линии не блокирует вторую или остальные независимые service stages | isolated `runCatching` + runtime fault injection | PARTIAL |
| Fast PM independence | responsive PUMP_2 может сам активировать 15s fast-path, не ожидая PUMP_3 candidate; каждый PM fast-sync идёт только по своей позиции/кандидату | `PumpFastCandidatePolicyV537Test` + service wiring | AUTO/PARTIAL |
| DeepSeek verdict scope | cached/PENDING entry verdict одного PM-профиля не является решением другого профиля | `DeepSeekEntryCoachTest` profile-scope tests + source wiring | AUTO/PARTIAL |
| DeepSeek shared budget | общий provider request budget/running lock может экономить API, но при отсутствии профильного verdict другой PM может использовать только свой strict local fallback, а не чужой verdict | `DeepSeekEntryCoachTest` + source review; runtime concurrency scenario | PARTIAL |
| Shared tuning layer | pooled outcomes/shared soft regulators не должны скрыто ухудшать profile independence; полезность должна подтверждаться trial/rollback outcomes | `AdaptiveTuningGuardV536Test` + representative ledger analysis | PARTIAL |
| Legacy UI exclusion | Старые T32/PM/Fusion/DeepSig accounts не возвращаются в focused owner network, но их stores не очищаются | `CompetitionAccountSpecTest` + UI device check | AUTO/PARTIAL |
| Compact DeepSeek status | Верхний блок показывает наличие ключа/работоспособность, ошибку, дневные успехи/ошибки и оценку расхода | source review + UI device check | PARTIAL |
| Human Factor authority | Human BUY невозможен без `ВОЙТИ`; `ОТКЛОНИТЬ` блокирует текущий setup до распада; после BUY выход автоматический +2,0% NET/STOP/90m | `HumanFactorVwapTest`, `T32CostPolicyV650Test` + source/UI review | PARTIAL |
| Human Factor repeated alarm | Пока pending, отдельный alarm/vibration повторяется примерно каждые 60s; ВОЙТИ/ОТКЛОНИТЬ/распад setup/позиция прекращают повтор | `T32CostPolicyV650Test` repeat-policy + source/device check | PARTIAL |
| Human Factor OS delivery | Приложение использует dedicated high-importance alarm channel, direct alarm ringtone и vibration; OS DND/manual mute всё равно могут переопределить звук | Android device test | MANUAL |
| 24h T32 text log | TXT содержит current state всех 4 T32, BUY/SELL и raw T32 journal события включая Human ALERT/PENDING/REJECT/ERROR | `V6ScalpReport.kt` source + runtime export inspection | PARTIAL |
| 24h JSON/TXT reliability | Создание идёт вне UI thread; части ≤900 KB, комплектны, читаемы и безопасны до открытия Android share | `SupportExportReliabilityTest` (3 passes) + CI | AUTO/PARTIAL |
| Main PUMP flow | Показаны executed aggressive BUY/SELL/NET за 1/5/15/30/60m; это не выдаётся за assets held | `MoneyFlowPresentationTest`, `MoneyFlowCoveragePolicyTest` + UI check | AUTO/PARTIAL |
| BTC mini context | BTC 24h path и causal 2h/6h/24h percentages не используют будущие candles | `BtcMiniPresentationTest` + UI check | AUTO/PARTIAL |
| Chart range guides | Main и все comparison charts показывают ±1.0% и ±1.5% от reference/current price | `RangeGuidePolicyTest` + UI check | AUTO/PARTIAL |
| PM UI 1 / PUMP_2 | +2.00% NET target, hard stop -1.10%, отдельный state | `PumpMachine2PolicyTest`, `PumpProfitEngineV526Test` | AUTO |
| PM UI 2 / PUMP_3 | +3.00% NET target, hard stop -1.30%, отдельный state | `PumpMachinePolicyTest`, `PumpProfitEngineV526Test` | AUTO |
| PM RETEST | Вход только после allowed candidate + bounded pullback/rebound; target +2/-1.10 | `PumpProfitEngineV526Test` + variant logic tests if present | PARTIAL |
| PM SAFE | Строгий local flow + APP evidence; target +1.15/-0.75 | `PumpProfitEngineV526Test` + source review | PARTIAL |
| Paper execution price | PM/Fusion BUY uses fresh ask, SELL uses bid | `BitpandaFusionTest`, PumpMachine/Fusion tests | PARTIAL |
| Fusion/PM fees | Fusion/PM simulation uses 0.25% each side and bid/ask spread | `FusionTradingCosts`, PumpMachine tests | AUTO |
| APP/legacy fees | APP/legacy base fee remains 0.15% per side unless explicitly changed | `PumpBotEngine.feeRate`, App/strategy tests | PARTIAL |
| Entry hard veto | stale/missing tape, no executable ask, dangerous spread, seller takeover/extreme absorption/late chase cannot be overridden by AI | `AdaptiveBreathEntryPolicy`/`PumpProfitEngineV526Test`, coach policy tests | PARTIAL |
| PM profile thresholds | responsive PUMP_2 confirmation и strict PUMP_3 confirmation остаются различными; V5.37 independence fix не меняет их TP/SL/thresholds | `PumpProfitEngineV526Test` + diff review | AUTO/PARTIAL |
| Shadow liquidity release | V5.32 observer не открывает/закрывает paper trades | `LiquidityReleaseShadowPolicyTest` + source wiring review | PARTIAL |
| DeepSeek coach authority | Coach вызывается после local candidate и не может override hard veto | DeepSeek entry/tuning tests + source review | PARTIAL |
| DeepSeek request economy | coach ≤6/UTC day, min 15 min; verdict reuse ≤10 min; backoff работает | DeepSeek coach policy/unit tests | PARTIAL |
| V5.36 tuning preconditions | >=8 prior closed trades, confidence >=85, max one bounded step/24h | `DeepSeekEntryTuningPolicy` tests / V5.36 tests | PARTIAL |
| V5.36 rollback | Одновременно один trial; post-change NET loss/weakness возвращает exact previous tuning | `AdaptiveTuningGuardV536Test` | AUTO |
| Tuning scope | AI не меняет hard veto, exits, real orders или portfolios | source contract + tests | PARTIAL |
| Service persistence | Смахивание UI не останавливает foreground monitor | `PumpSignalService` source; Android runtime check | MANUAL |
| Failure isolation | exception optional stage не отменяет независимые последующие participants | `CycleStageGuard`, T32 isolated sync + integration tests | PARTIAL |
| Alerts master | OFF глушит обычные пользовательские alerts/sound, но не останавливает research/paper/logging; Human manual-decision alarm V6.5 намеренно отдельный | `ResearchModePolicy`, Human alarm source/runtime | PARTIAL |
| Logs | BUY/SELL/ERROR/history остаются доступны; support export не очищает full archive | V5.35 log/export tests + runtime export | PARTIAL |
| Ledger after restart | SQLite ledger и individual prefs восстанавливаются после process/app restart | device/instrumentation restart scenario | NEEDS_TEST |
| Replay/live equivalence APP | research decision/replay использует causal closed data и next execution logic | `ResearchDecisionEngineTest`, `ResearchReplayEngine` tests | AUTO/PARTIAL |
| Replay/live equivalence current PM stack | Replay должен воспроизводить actual 15s flow/book/coach/hard-veto decision path до сравнения profitability | полноценный current-stack replay harness | NEEDS_TEST |
| Profitability claim | Нельзя объявлять стратегию улучшенной по одной сделке/одному короткому окну | review против `MASTER_SPEC.md`, representative ledger analysis | MANUAL |
| Stop text consistency | UI/status/comment должен соответствовать фактическим -1.10/-1.30 constants | targeted string/unit test отсутствует | NEEDS_TEST |
| Final compatible APK | Перед выдачей проверить package/version/activity/ZIP/signature и compatible cert | apksigner/aapt + certificate fingerprint + upgrade verification | MANUAL |

## Правило расширения

Каждый найденный реальный regression должен либо:

1. получить автоматический test, либо
2. остаться здесь как явно описанный manual/NEEDS_TEST check с причиной, почему автоматизация пока отсутствует.

Не удалять строку только потому, что текущий bug исправлен.
