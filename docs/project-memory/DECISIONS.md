# PUMP / PumpBot — DECISIONS

Только существенные решения и причины. Не использовать как косметический changelog.

## 2026-08-16 — V5: research-first и причинная проверка

**Decision**  
Перестать трактовать старый составной V4 сигнал как доказанную торговую систему. V5 строится как paper-forward research architecture с причинным `ResearchDecisionEngine`/replay и раздельными экспериментальными счетами.

**Reason**  
Аудит показал, что исторический backtest проверял не тот полный механизм, который давал live-рекомендации; в логике было много вручную подобранных порогов, а LLM confidence не являлся измеренной вероятностью.

**Alternatives considered**  
Продолжать подкручивать V4 thresholds и считать локальный backtest достаточным.

**Rejected because**  
Такой путь не устранял несоответствие backtest/live и создавал риск дальнейшей подгонки.

**Consequences**  
`NO TRADE` считается нормальным; причинность, costs, forward evidence и независимые portfolios становятся базовыми принципами.

---

## 2026-08-23 — V5.29: четыре независимые Pump Machine + Fusion

**Decision**  
Сравнивать четыре Pump Machine entry-профиля как отдельные paper experiments; расположить их первыми в Competition, Fusion — пятым. Retest и Safe не должны автоматически копировать PM2/PM3.

**Reason**  
Нужно сравнивать разные гипотезы входа на одинаковом рынке вместо постоянного изменения одного алгоритма без контроля.

**Alternatives considered**  
Один объединённый «лучший» Pump Machine и общий portfolio.

**Rejected because**  
Это уничтожило бы информацию о том, какая гипотеза реально работает, и смешало бы PnL/риски.

**Consequences**  
Отдельные stores/state/cooldowns/history являются инвариантом.

---

## 2026-08-24 — V5.32: liquidity release только shadow

**Decision**  
Seller exhaustion / уход ask / liquidity release записывать как причинное evidence, но **не передавать** в entry/exit authority.

**Reason**  
Гипотеза полезна, но ещё не было достаточного outcome evidence, чтобы менять paper trades.

**Alternatives considered**  
Сразу разрешить observer открывать/закрывать сделки.

**Rejected because**  
Исчезновение ask или изменение потока легко даёт ложный сигнал без устойчивого подтверждения.

**Consequences**  
Любое будущее promotion требует отдельного review по сохранённым outcomes; нельзя тихо подключить shadow-модуль к торговле.

---

## 2026-08-25 — V5.33: отказаться от фиксированного capital lock

**Decision**  
Для четырёх Pump Machine заменить обязательный абсолютный USDT/large-BUY lock относительным adaptive breath score: executed-flow imbalance, cross-horizon acceleration, book, activity, price efficiency, phase, absorption. PM responsive и strict профили снова различаются.

**Reason**  
V5.31/5.32 абсолютный capital gate оказался слишком жёстким/хрупким и в одном месте PM2 фактически получила PM3-конфигурацию.

**Alternatives considered**  
Вернуть обязательный `$250k/$350k` turnover или mandatory repeated large-BUY fingerprint.

**Rejected because**  
Широкий turnover не гарантирует направление цены, а абсолютный порог плохо переносится между рыночными режимами.

**Consequences**  
Не возвращать fixed USDT minimum или mandatory large-BUY fingerprint без нового outcome evidence. Hard veto по freshness/executable ask/spread/extreme absorption/seller takeover/late chase остаются.

---

## 2026-08-25 — V5.34/5.35: DeepSeek только поверх готового локального candidate

**Decision**  
DeepSeek Flash pre-entry coach запускается после локального кандидата, а не вместо локального gate. Он может работать только с ограниченными soft controls. V5.35 ограничивает coach 6 запросами за UTC day, минимум 15 минут между запросами, с reuse compatible verdict до 10 минут и backoff после ошибок.

**Reason**  
Нужен AI second-opinion/адаптация без превращения дорогой недетерминированной модели в единственный execution authority и без бесконтрольного расхода API.

**Alternatives considered**  
Вызывать AI постоянно или позволить ему напрямую отменять локальные запреты.

**Rejected because**  
Стоимость, latency, nondeterminism и риск обхода проверяемых safety conditions.

**Consequences**  
Hard veto остаётся локальным. Отсутствие AI не должно автоматически превращаться в безусловный BUY.

---

## 2026-08-25 — V5.36: soft tuning только как guarded trial

**Decision**  
Каждое автоматическое изменение DeepSeek — одно ограниченное paper-испытание. Хранить точную предыдущую tuning, одновременно только один trial. Новая юстировка требует >=8 prior closed paper trades, confidence >=85 и не чаще одного малого шага в 24 часа. Trial оценивается по post-change NET outcomes; стандартная оценка после 6 сделок, максимум 10/7 дней; ухудшение вызывает rollback.

**Reason**  
Самонастройка без контрольной точки превращает ошибки в самоподдерживающийся drift.

**Alternatives considered**  
Применять каждое AI-предложение сразу и оставлять его до ручной проверки.

**Rejected because**  
Нельзя отличить улучшение от случайности или деградации; отсутствует безопасный возврат.

**Consequences**  
AI не может tuning hard veto, exits, real orders или stored portfolios. Оценка производится по NET PnL после расходов.

---

## 2026-08 — EXISTING RELEASE/DATA INVARIANT, подтверждено 2026-08-26

**Decision**  
Совместимая Android-линия сохраняет `applicationId = com.example.pumppaperbot.v8`, установленное приложение не удаляется ради обновления, persistent stores/history не очищаются. Совместимый certificate SHA-256: `1f778c4291c9d11c5f89f4de8773bda35a0125031adc05785daee23f27dc7823`.

**Reason**  
Удаление/смена package identity или подписи делает update несовместимым и может уничтожить локальную историю.

**Alternatives considered**  
Новый package id / clean install для упрощения миграции.

**Rejected because**  
Потеря continuity и накопленного research evidence.

**Consequences**  
Любое изменение schema должно использовать совместимость/миграцию. Перед выдачей APK проверять package/version/signature.

---

## 2026-08-26 — Project Guardian становится долговременной памятью

**Decision**  
Создать `ИНСТРУКЦИЯ_1.md`, короткий `AGENTS.md` и `docs/project-memory/*`. История чата больше не используется как основной источник истины. `DEVELOPMENT_LOG.md` остаётся подробной исторической хроникой, но актуальная цель/архитектура/состояние/решения/регрессии живут в project-memory.

**Reason**  
После множества версий локальные fixes начали заслонять первоначальную цель, а огромный `AGENTS.md` смешивал текущие правила с историей версий.

**Alternatives considered**  
Продолжить наращивать один `AGENTS.md` и полагаться на старые чаты/`DEVELOPMENT_LOG.md`.

**Rejected because**  
Новый агент тратит слишком много контекста на историю и легко принимает устаревшее version-specific правило за текущую концепцию.

**Consequences**  
Перед существенной задачей новый агент обязан восстановить контекст по project-memory; после задачи обновлять её. Фундаментальное новое решение владельца должно быть записано в `MASTER_SPEC.md`/`DECISIONS.md`.