# PUMP / PumpBot — CURRENT STATE

Обновлено: **2026-08-26**

Это короткий снимок текущего состояния. Старую историю сюда не накапливать.

## VERSION / BRANCH

- Canonical branch: `main`.
- Current source version: **V5.36**, `versionCode 116`.
- `applicationId`: `com.example.pumppaperbot.v8`.
- Source baseline до Guardian setup: commit `3975dd5fb9c965a9a8b9306c4c942df940842acf` (`Record canonical V5.36 integration`).
- Последний CI для этого baseline: GitHub Actions run #386 — **success**; workflow выполняет `testDebugUnitTest`, `lintDebug`, `assembleDebug`, package/activity/APK checks.

## BUILD / RELEASE STATUS

- **Verified V5.36 source/build:** да.
- `DEVELOPMENT_LOG.md` фиксирует совместимо подписанный `PumpSignal-V5.36-Compatible-FINAL.apk`, SHA-256 `819385353189bfec4b04e48e8a33ba65f63f94a302ffaa2df4a8e8a96789c96f`, certificate SHA-256 `1f778c4291c9d11c5f89f4de8773bda35a0125031adc05785daee23f27dc7823`.
- Этот V5.36 APK был сохранён вне GitHub Release как `/Биткоин/PumpSignal-V5.36-Compatible-FINAL.apk`.
- **Latest published GitHub Release:** V4.9. Git tag/Release `v5.36` в GitHub сейчас отсутствует.
- Фактически установленная сейчас на телефоне владельца версия: `UNKNOWN`.

Поэтому не использовать выражение «последняя стабильная версия» без уточнения: source/build, compatible APK или published Release.

## WHAT WORKS

- Android foreground monitoring и автоматическое продолжение после закрытия UI.
- Binance market sync + short-horizon local evidence.
- Bitpanda Fusion read-only bid/ask/order-book с Keystore key storage.
- Четыре независимых Pump Machine paper accounts.
- Отдельный Fusion paper account.
- APP, DeepSigX и ручной SERGE account/history.
- 8-account Competition UI с отдельными markers/history.
- SQLite append-only `ResearchPerformanceLedger` и V4→V5 archive capture.
- V5.33 adaptive breath entry scoring + hard veto.
- V5.34/5.35 bounded DeepSeek pre-entry coach.
- V5.36 guarded one-change tuning trial with rollback.
- V5.32 liquidity-release observer остаётся shadow-only.
- Master alerts могут быть выключены без остановки research/paper journals.

## WORKS PARTIALLY / NEEDS SYSTEM VALIDATION

- Глобальная прибыльность/expectancy текущего V5.36 набора стратегий: **не доказана**.
- Полное соответствие live Pump Machine decision path и replay/walk-forward проверки: `NEEDS_VERIFICATION`.
- Полная migration coverage всех старых compatible installs до V5.36: `NEEDS_VERIFICATION`.
- AI self-tuning имеет safety guard, но его полезность должна оцениваться на достаточном количестве новых closed NET trades; наличие механизма само по себе не доказывает улучшение.
- Release publication process отстаёт от source/build state.

## KNOWN PROBLEMS

1. **Naming drift:** UI Pump Machine 1/2 не совпадает с именами `PumpMachine2Store`/`PumpMachineStore`.
2. **Stop text drift:** код PM2/PUMP_2 использует -1.10% NET и PM3/PUMP_3 -1.30%, но часть старых комментариев/status strings всё ещё говорит -1.5%.
3. **Architecture layering:** старый V5 research/replay контур и новый Pump Machine/Fusion fast-flow контур сосуществуют без одного общего явно проверенного evaluation interface.
4. **Release drift:** V5.36 source/build есть, published GitHub Release только V4.9.
5. **Persistence complexity:** много versioned SharedPreferences/stores; неизвестный `PumpBotEngine` algorithm version приводит к reset его engine prefs.
6. Полные фактические текущие device-ledger/log outcomes отсутствуют в GitHub: без экспорта с устройства нельзя достоверно утверждать, почему система сейчас в плюсе/минусе.

## CURRENT MAIN PRIORITY

**Остановить архитектурный дрейф и получить честный baseline эффективности V5.36 прежде, чем снова менять пороги/торговые алгоритмы.**

Guardian project-memory создан именно для этого.

## LAST MATERIAL CHANGES

- V5.33: fixed-capital lock заменён относительным adaptive breath score; PM responsive/strict profiles снова разведены.
- V5.34: DeepSeek Flash стал post-local-candidate coach и получил bounded soft controls.
- V5.35: жёстко ограничены paid AI cadence/cost и облегчён support export.
- V5.36: каждое soft-tuning предложение превращено в guarded paper trial с сохранением исходной настройки и rollback.
- 2026-08-26: создан Project Guardian и постоянная project-memory; код торговых алгоритмов не менялся.

## UNFINISHED WORK

- Нет единого зафиксированного performance baseline текущего V5.36 по всем доступным closed trades/regimes.
- Не установлена replay/live equivalence полного Pump Machine/Fusion/coach пути.
- Не устранены naming/text inconsistencies; это пока зафиксированный долг, а не текущая задача.
- GitHub Release V5.36 отсутствует; это отдельная release-management задача, не повод менять торговую логику.

## RISKS

- Следующая локальная подкрутка может оптимизировать последний убыточный эпизод и ухудшить другой режим.
- Shared flow/AI layers могут создать скрытую корреляцию между «независимыми» стратегиями даже при раздельных portfolios; это надо измерять, а не предполагать.
- Плохое различение class/UI naming повышает риск правки не того агента.
- Без сохранённого before/after baseline self-tuning может быть ошибочно признан полезным по слишком маленькой выборке.

## NEXT REASONABLE STEP

**Одна следующая задача:** провести baseline-аудит V5.36 **без изменения алгоритмов**: взять доступный 30-дневный/ledger export с устройства, разложить closed trades по каждому current account и рыночным режимам, проверить NET PnL/expectancy/drawdown/серии убытков/причины входа и сопоставить их с текущими hard/soft gates. Результат должен определить root cause убыточности до любой новой юстировки.