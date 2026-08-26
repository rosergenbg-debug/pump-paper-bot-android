# PUMP / PumpBot — AI CHANGELOG

Только существенные законченные задачи. Не дублировать Git history.

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
Targeted regression tests добавлены. GitHub Actions run #388 полностью прошёл `testDebugUnitTest`, `lintDebug`, `assembleDebug`, APK package/version/activity/v2-signature/ZIP checks и upload artifact. После этого runtime/trading code больше не менялся — до final PR head менялись только шесть `docs/project-memory/*` файлов. PR #84 успешно слит в `main` как V5.37. CI artifact проверен как debug-signed intermediate, поэтому он не выдаётся как совместимое пользовательское обновление без отдельной подписи правильным ключом.

**Project impact**  
Системное исправление в сторону `MASTER_SPEC`: уменьшает скрытую связность стратегий и возвращает responsive профилю собственную скорость реакции без подгонки торговых порогов.
