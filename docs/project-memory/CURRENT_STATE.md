# PUMP / PumpBot — CURRENT STATE

Обновлено: **2026-08-26**

Это короткий снимок текущего состояния. Старую историю сюда не накапливать.

## VERSION / BRANCH

- Canonical branch: `main`.
- Current source version: **V5.37**, `versionCode 117`.
- `applicationId`: `com.example.pumppaperbot.v8` — сохранён для совместимого update/data continuity.
- V5.37 merged via PR **#84**, merge commit `48a3c3001e01fbc8f7722fb25c077a3ce25e0ea3`.
- Guardian V5.36 CI run #387: **success**.
- V5.37 code/build validation run #388: **success**.
- Latest full `main` build run #402: **success** — tests, lint, assemble, APK package/version/activity/v2-signature/ZIP checks and artifact upload passed.

## BUILD / RELEASE STATUS

- V5.37 source/runtime code прошёл полный CI.
- Fresh CI source artifact from run #402: `PumpSignal-V5.37-Scalp-Independence-Intermediate.apk`.
- Final compatible handoff APK создан: `PumpSignal-V5.37-Compatible-FINAL.apk`.
- Final APK size: `7,625,105` bytes.
- Final APK SHA-256: `4acfcc3030d517660c7a7bb45b12116aa88ef18287eed0ffc5366662443e3837`.
- Final APK uses APK Signature Scheme **v2** and was independently verified after signing.
- Final signing certificate SHA-256: `1f778c4291c9d11c5f89f4de8773bda35a0125031adc05785daee23f27dc7823` — совпадает с исторической совместимой `.v8` линией.
- Keystore/пароль получены из приватного Google Drive recovery bundle; они **не записаны в GitHub**.
- Новый signing key не создавался. Uninstall/clean install для обновления не использовать.
- Фактически установленная сейчас на телефоне владельца версия: `UNKNOWN`.

## WHAT WORKS

- Android foreground monitoring и автоматическое продолжение после закрытия UI.
- Binance/public market sync + short-horizon local evidence.
- Bitpanda Fusion read-only bid/ask/order-book с Keystore key storage.
- Четыре раздельных Pump Machine paper portfolios/state/cooldowns.
- Fusion, APP, DeepSigX и SERGE отдельными paper/reference accounts.
- 8-account Competition UI.
- SQLite append-only `ResearchPerformanceLedger` и V4→V5 history capture.
- V5.33 adaptive breath entry + hard veto.
- V5.34/5.35 bounded DeepSeek pre-entry coach/cost limits.
- V5.36 guarded one-change tuning trial with rollback.
- V5.32 liquidity-release observer shadow-only.

## V5.37 MATERIAL CHANGE

Архитектурный аудит после Guardian выявил скрытую взаимную зависимость стратегий:

1. В V5.36 fast ~15s path всех четырёх PM был привязан к `PUMP_3` candidate.
2. Единственный DeepSeek cached/PENDING entry state не содержал profile identity и мог влиять на другой PM-профиль.

V5.37 исправляет именно эти причины:

- `PumpFastCandidatePolicyV537` считает fast eligibility отдельно для PUMP_2/PUMP_3/RETEST/SAFE;
- каждый PM fast-sync зависит от собственного candidate/position;
- DeepSeek cached verdict/PENDING/ordinary retry state теперь profile-scoped;
- общий DeepSeek paid request budget/running lock остаётся общим provider resource;
- старый persisted coach state без profile читается как `UNKNOWN` и не переиспользуется как чужой verdict;
- добавлены targeted regression tests;
- TP/SL и основные entry thresholds **не менялись**.

## WORKS PARTIALLY / NEEDS SYSTEM VALIDATION

- Глобальная прибыльность/expectancy текущего набора стратегий: **не доказана**.
- Full live↔replay equivalence текущего 15s PM/coach path: `NEEDS_VERIFICATION`.
- Shared DeepSeek tuning layer использует pooled outcomes и несколько общих soft regulators: допустимо как guarded learning layer, но влияние на независимость/NET требует forward evidence.
- Полная migration coverage всех старых compatible installs: `NEEDS_VERIFICATION`.
- Published GitHub Release всё ещё может отставать от source/signed-handoff state.

## KNOWN PROBLEMS / DEBT

1. Naming drift: UI PM1 = `PumpMachine2Store`/PUMP_2; UI PM2 = `PumpMachineStore`/PUMP_3.
2. Часть старых stop strings/comments может говорить -1.5%, хотя фактические PUMP_2/PUMP_3 stops -1.10/-1.30.
3. Research/replay baseline и более поздний fast PM/Fusion слой сосуществуют без одного полного replay interface.
4. Persistence фрагментирован между versioned stores; все upgrade paths не доказаны.
5. Фактические device ledger/log outcomes сейчас отсутствуют в GitHub, поэтому нельзя достоверно объяснить общий плюс/минус без экспорта.

## CURRENT MAIN PRIORITY

**Не менять thresholds после V5.37, пока не получен честный performance baseline на forward/device ledger.**

## RISKS

- Fast local processing может активироваться чаще, потому что responsive PM2 больше не ждёт PM3 — это ожидаемое восстановление скальпинговой своевременности, но runtime следует наблюдать.
- Общий DeepSeek API budget может означать, что не каждый профиль получит отдельный paid verdict; это ресурсное ограничение, а не право другого профиля блокировать сделку.
- Shared tuning может создавать корреляцию между стратегиями; не разделять/переписывать его без evidence.
- Любая новая подкрутка thresholds до baseline снова создаст архитектурный drift.

## NEXT REASONABLE STEP

**Установить совместимо подписанный V5.37 поверх существующей `.v8` установки без удаления приложения, затем собрать device/ledger baseline и сравнить по каждому account своевременность входа, NET PnL/expectancy/drawdown/loss streak и причины BUY/EXIT.**