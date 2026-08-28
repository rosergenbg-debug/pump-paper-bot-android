# PUMP / PumpBot — REGRESSION MATRIX

Статусы:

- `AUTO` — есть прямой автоматический test/check.
- `PARTIAL` — часть поведения покрыта тестом/build, но не весь lifecycle.
- `MANUAL` — требуется runtime/device/release проверка.
- `NEEDS_TEST` — критично, но достаточного автоматического теста пока не установлено.

| Область | Ожидаемое поведение | Как проверяется | Статус |
|---|---|---|---|
| Build baseline | `testDebugUnitTest`, `lintDebug`, `assembleDebug` проходят | `.github/workflows/android.yml` | AUTO |
| Package/version | compatible line остаётся `com.example.pumppaperbot.v8`; V5.37/code117 после merge | CI `aapt dump badging` | AUTO |
| Launch APK | launch activity `MainActivity`, APK ZIP валиден, signature scheme проверяется | CI APK checks; final certificate отдельно | PARTIAL |
| Real trading | Ни одна текущая автономная стратегия не отправляет реальный order/cancel/transfer | source review, `ResearchModePolicy`, `V49SafetyPolicyTest` и related safety tests | PARTIAL |
| Bitpanda access | Fusion client использует read-only GET order book; key не пишется plaintext в prefs/log | `BitpandaFusionTest` + source review | PARTIAL |
| Persistent history | App version changes не очищают append-only SQLite ledger | `ResearchPerformanceLedger`, `ResearchHistoryArchiveTest`, retention tests | PARTIAL |
| V4→V5 history | доступная V4 history захватывается/остаётся экспортируемой | `ResearchHistoryArchiveTest`, ledger capture | PARTIAL |
| Compatible update data | portfolios/settings/keys/history переживают update без uninstall | version-specific retention tests + final APK upgrade on device | MANUAL |
| Engine migration | известные `PumpBotEngine` algorithm versions мигрируют без неожиданного reset | source migration + targeted tests | NEEDS_TEST |
| Independent accounts | APP/PM profiles/Fusion/DeepSigX/SERGE не используют общий portfolio | store separation + strategy tests + Competition inspection | PARTIAL |
| Fast PM independence | responsive PUMP_2 может сам активировать 15s fast-path, не ожидая PUMP_3 candidate; каждый PM fast-sync идёт только по своей позиции/кандидату | `PumpFastCandidatePolicyV537Test` + service wiring | AUTO/PARTIAL |
| DeepSeek verdict scope | cached/PENDING entry verdict одного PM-профиля не является решением другого профиля | `DeepSeekEntryCoachTest` profile-scope tests + source wiring | AUTO/PARTIAL |
| DeepSeek shared budget | общий provider request budget/running lock может экономить API, но при отсутствии профильного verdict другой PM может использовать только свой strict local fallback, а не чужой verdict | `DeepSeekEntryCoachTest` + source review; runtime concurrency scenario | PARTIAL |
| Shared tuning layer | pooled outcomes/shared soft regulators не должны скрыто ухудшать profile independence; полезность должна подтверждаться trial/rollback outcomes | `AdaptiveTuningGuardV536Test` + representative ledger analysis | PARTIAL |
| UI account mapping | Competition содержит 10 счетов: 4 PM → Fusion → DeepSigX → APP → SERGE → VWAP AUTO → HUMAN FACTOR | `CompetitionAccountSpecTest` + UI device check | AUTO/PARTIAL |
| Human Factor authority | VWAP AUTO торгует только виртуально и без звука; Human Factor BUY невозможен без явного подтверждения, pending исчезает при распаде сигнала | `HumanFactorVwapTest` + source/UI review | PARTIAL |
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
| Failure isolation | exception optional stage не отменяет независимые последующие participants | `CycleStageGuard`, integration tests | PARTIAL |
| Alerts master | OFF глушит пользовательские alerts/sound, но не останавливает research/paper/logging | `ResearchModePolicy` + alert policy tests/runtime | PARTIAL |
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
