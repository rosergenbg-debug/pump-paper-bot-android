# PUMP / PumpBot — AI CHANGELOG

Только существенные законченные задачи. Не дублировать Git history.

## 2026-08-28 / V6.3 — VWAP AUTO + HUMAN FACTOR

**Task**
Создать параллельное сравнение исходного VWAP 32,65 алгоритма с человеческим подтверждением входа.

**Change**
Добавлены независимые AUTO и HUMAN portfolios, 1m VWAP evaluator, 90/100 warning, постоянная approve/reject карточка, два новых Competition slots и маршрутизация пользовательских входных звонков только через Human Factor.

**Verification**
Kotlin compilation passed. Добавлены policy tests и обновлён account contract; полный Gradle test task блокируется локальным Android SDK jlink transform до выполнения Java/unit стадии.

**Project impact**
Два paper-эксперимента позволяют измерить ценность человеческого решения. Live trading не добавлен; applicationId и история сохранены.

---

## 2026-08-28 — Четыре фиксированные OHLCV-гипотезы

**Task**
Проверить Z-Score VWAP, volatility squeeze, strict BTC/SOL lead-lag и session-open breakout на отдельном блоке.

**Change**
Добавлены воспроизводимый research-only replay и полный TXT-отчёт. Runtime/version не менялись.

**Verification**
159 565 общих минут, временной split пополам, synthetic 8/8 PASS. Лучший control WR 14,29%, все Avg NET отрицательны, PF ниже 1.

**Project impact**
Ни одна формула не получает trading authority. Следующий новый уровень — trade tape/L2.

---

## 2026-08-28 — Cost-aware VWAP и session gap-fill replay

**Task**
Проверить на отдельном окне, можно ли превзойти прежний VWAP win rate 32,65% после исправления cost-floor/adverse-selection и новой session gap-fill формулой.

**Change**
Добавлен воспроизводимый research-only replay и полный TXT-отчёт. Runtime/version не менялись.

**Verification**
172 800 минут, split 60/60 дней, synthetic 4/4 PASS. Обе формулы дали 8,33% control WR, отрицательный NET и PF ниже 1.

**Project impact**
Прежние 32,65% не улучшены. Формулы отклонены; tape/L2 обозначен следующим новым уровнем исследования.

---

## 2026-08-28 — Пять контекстных гипотез

**Task**
Причинно проверить regime-adaptive entry, liquidity sweep, VWAP reversion, sell exhaustion и 4H node + relative strength.

**Change**
Добавлен research-only воспроизводимый 120-дневный replay и отчёт. Production/runtime/version не менялись.

**Verification**
Synthetic 10/10 PASS; 172 800 выровненных минут; временной split 60/60 дней; комиссии, slippage, TTL и незаполненные заявки учтены. Лучший control WR 32,65%, Avg NET −0,419%, PF 0,399.

**Project impact**
Гипотезы не получают торговую власть; отрицательное evidence сохранено против повторной подгонки.

---

## 2026-08-26 / V5.36 — Project Guardian initial setup

**Task**  
Восстановить цели и фактическую архитектуру PUMP, создать постоянную внешнюю память проекта и простую точку входа для любого нового AI-чата.

**Root cause**  
После многих version-specific fixes знания о цели, архитектуре, текущем состоянии и исторических причинах решений были смешаны между огромным `AGENTS.md`, `DEVELOPMENT_LOG.md`, README, кодом и чатами. Это создавало архитектурный дрейф и риск «fix поверх fix».

**Change**  
Созданы `ИНСТРУКЦИЯ_1.md`, новый короткий `AGENTS.md` и шесть файлов `docs/project-memory/*`. Торговые алгоритмы, thresholds, stores и UI не изменялись.

**Affected components**  
Repository governance/documentation only.

**Regression risk**  
Основной риск — неверно зафиксировать устаревшее историческое правило как текущую архитектуру. Неизвестное помечено `UNKNOWN`/`NEEDS_VERIFICATION`.

**Verification**  
Сверены repo/current main, V5.36 build/CI, research audit, service/data flow, четыре Pump Machine, Fusion, UI, ResearchMode, Bitpanda read-only, DeepSeek tuning guard, ledger и tests. Guardian CI run #387 завершился success.

**Project impact**  
Системное улучшение: следующие изменения оцениваются относительно `MASTER_SPEC`, решений и regression matrix, а не только последнего симптома.

---

## 2026-08-26 / V5.37 — Scalp timing and Pump profile independence

**Task**  
Проверить V5.36 после Project Guardian относительно реальной цели скальпинга и найти скрытые перекрёстные блокировки между автономными paper-ботами.

**Root cause**  
Обнаружены две системные связи V5.36:

1. `PumpSignalService` использовал только `PUMP_3` как `commonFastCandidate`, поэтому responsive `PUMP_2` и другие профили могли не получить быстрый ~15s sync, пока строгий PUMP_3 не становился кандидатом.
2. `DeepSeekEntryCoachState` не хранил profile identity. Cached verdict и `PENDING` могли переиспользоваться/блокировать другой PM-профиль, хотя в DeepSeek request уже передавался конкретный `candidate_profile`.

Это проблема архитектуры независимых экспериментов и timing, а не недостаток очередного threshold.

**Change**  
- Добавлен `PumpFastCandidatePolicyV537`: один общий market observation, но отдельный fast-candidate для PUMP_2/PUMP_3/RETEST/SAFE.
- Fast sync каждого PM теперь зависит только от его собственной позиции/кандидата.
- `DeepSeekEntryCoachState` получил `candidateProfile` с безопасной миграцией старого persisted state в `UNKNOWN`.
- Cached/PENDING/ordinary retry AI state стал profile-scoped; provider balance pause и request budget остаются shared resource.
- Добавлены regression tests для PM2-fast-vs-PM3 и DeepSeek profile scope.
- Версия повышена до V5.37/code117, package id сохранён.
- TP/SL, основные entry thresholds, paper/live policy и portfolios не менялись.

**Affected components**  
`PumpSignalService`, `PumpFastCandidatePolicyV537`, `DeepSeekEntryCoach`, unit tests, build/CI metadata, Guardian project-memory.

**Regression risk**  
Fast-path может активироваться чаще, потому что теперь каждый профиль имеет право самостоятельно стать кандидатом; это ожидаемое восстановление timing, но требует наблюдения CPU/API-independent local behavior. DeepSeek paid API cadence остаётся общей и не увеличена. Старый cached coach state без profile намеренно не переиспользуется после upgrade.

**Verification**  
Targeted regression tests добавлены. GitHub Actions run #388 полностью прошёл `testDebugUnitTest`, `lintDebug`, `assembleDebug`, APK package/version/activity/v2-signature/ZIP checks и upload artifact. После этого runtime/trading code больше не менялся — до final PR head менялись только шесть `docs/project-memory/*` файлов. PR #84 успешно слит в `main` как V5.37.

**Project impact**  
Системное исправление в сторону `MASTER_SPEC`: уменьшает скрытую связность стратегий и возвращает responsive профилю собственную скорость реакции без подгонки торговых порогов.

---

## 2026-08-26 / V5.37 — Compatible signed final APK

**Task**  
Собрать пользовательский V5.37 APK и подписать его тем же update-ключом, что совместим с существующей `.v8` установкой.

**Change**  
Использован свежий artifact из успешного `main` build run #402. Debug signature заменена на исторический update certificate из приватного Google Drive recovery bundle. Новый signing key не создавался; package/version/runtime code не менялись.

**Verification**  
- build run #402: success;
- package/version исходного artifact проверены CI как `com.example.pumppaperbot.v8`, V5.37/code117;
- alias update keystore проверен перед подписью;
- certificate SHA-256: `1f778c4291c9d11c5f89f4de8773bda35a0125031adc05785daee23f27dc7823`;
- APK Signature Scheme v2 после подписи проверена криптографически;
- ZIP integrity: OK;
- final APK size: `7,625,105` bytes;
- final APK SHA-256: `4acfcc3030d517660c7a7bb45b12116aa88ef18287eed0ffc5366662443e3837`.

**Security**  
JKS, пароль и recovery bundle не коммитились и не должны коммититься в GitHub.

**Project impact**  
V5.37 готов как совместимый signed handoff APK для установки поверх существующей `.v8` версии без uninstall.

---

## 2026-08-28 / V6.4 — Focused four-account network UI

**Change**
- Сеть сокращена и переупорядочена: T32 → Human Factor → СЕРЖ → APP.
- PM1–PM4, Fusion/local node и DeepSigX скрыты из сети без очистки их persisted state.
- DeepSeek-блок наверху стал компактным и показывает состояние подключения, последнюю ошибку/успех, дневную статистику и расход.
- Удалена нижняя кнопка `ТЕСТ НАЗАД`.
- Версия повышена до V6.4/code122; package id сохранён.

**Verification**
Локально прошли `testDebugUnitTest`, `lintDebug` и `assembleDebug`.

**Project impact**
Уменьшен визуальный шум без изменения торговой логики, истории, счетов, API-ключей или paper-only ограничений.
