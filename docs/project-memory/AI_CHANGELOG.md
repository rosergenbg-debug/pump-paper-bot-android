# PUMP / PumpBot — AI CHANGELOG

Только существенные законченные задачи. Не дублировать Git history.

## 2026-08-26 / V5.36 — Project Guardian initial setup

**Task**  
Восстановить цели и фактическую архитектуру PUMP, создать постоянную внешнюю память проекта и простую точку входа для любого нового AI-чата.

**Root cause**  
После многих version-specific fixes знания о цели, архитектуре, текущем состоянии и исторических причинах решений были смешаны между огромным `AGENTS.md`, `DEVELOPMENT_LOG.md`, README, кодом и чатами. Это создавало архитектурный дрейф и риск «fix поверх fix».

**Change**  
Созданы:

- `ИНСТРУКЦИЯ_1.md`;
- новый короткий `AGENTS.md`;
- `docs/project-memory/MASTER_SPEC.md`;
- `docs/project-memory/ARCHITECTURE.md`;
- `docs/project-memory/CURRENT_STATE.md`;
- `docs/project-memory/DECISIONS.md`;
- `docs/project-memory/REGRESSION_MATRIX.md`;
- `docs/project-memory/AI_CHANGELOG.md`.

Торговые алгоритмы, thresholds, stores и UI не изменялись.

**Affected components**  
Repository governance/documentation only.

**Regression risk**  
Основной риск — неверно зафиксировать устаревшее историческое правило как текущую архитектуру. Поэтому version-specific старые материалы использованы как evidence, а current code/CI/main получили приоритет при описании фактической реализации. Неизвестное помечено `UNKNOWN`/`NEEDS_VERIFICATION`.

**Verification**  
Перед созданием памяти сверены структура repo, current `main`, V5.36 build config, recent commits/CI, V5 research audit, service/data flow, Pump Machine 2/3/Retest/Safe, Fusion, Competition UI, ResearchMode, Bitpanda read-only client, DeepSeek coach/tuning guard, performance ledger и существующий набор unit tests. Документы сверены между собой на названия счетов, fees, paper-only contract, current version и known uncertainties.

**Project impact**  
Системное улучшение: следующие изменения должны оцениваться относительно `MASTER_SPEC`, существующих решений и regression matrix, а не только последнего наблюдаемого симптома.