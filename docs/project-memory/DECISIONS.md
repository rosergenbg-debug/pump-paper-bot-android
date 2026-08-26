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

---

## 2026-08-26 — V5.37: scalping-first и независимость profile-specific решений

**Decision**  
Уточнить смысл PUMP как **скальпингового** проекта: название не ограничивает систему только классическими pump-событиями. Общие market observations могут использоваться всеми стратегиями, но fast-candidate eligibility, confirmation/cooldown, portfolio и cached/PENDING DeepSeek entry verdict относятся к конкретному Pump-профилю. Общий лимит DeepSeek остаётся общим как ограниченный внешний ресурс.

**Reason**  
Аудит V5.36 выявил две реальные скрытые связи: 15-секундный fast-path для всех четырёх PM запускался по кандидату `PUMP_3`, а единый cached/PENDING `DeepSeekEntryCoachState` не содержал profile identity, хотя запрос к модели содержал `candidate_profile`. Responsive `PUMP_2` мог терять своевременность или зависеть от AI-состояния другого профиля.

**Alternatives considered**  
1. Оставить V5.36 и просто ослабить thresholds.  
2. Полностью разнести market data и DeepSeek API на четыре независимых копии.  
3. Создать новый Android package/app с чистым состоянием.

**Rejected because**  
1. Порог не устраняет архитектурную причину.  
2. Четыре копии market/API создают лишний расход, гонки и не дают дополнительной независимости решений.  
3. Новый package нарушает data/update invariant и потеряет continuity.

**Consequences**  
V5.37 остаётся совместимым обновлением `com.example.pumppaperbot.v8`. TP/SL и основные entry thresholds в этой задаче не меняются. Shared tuning layer остаётся ограниченным общим soft-learning слоем и должен оцениваться по forward outcomes; его полезность пока `NEEDS_VERIFICATION`. Реальные ордера остаются отдельным будущим решением.

---

## 2026-08-26 — V6.0: Execution Intelligence сначала только SHADOW

**Decision**  
Не переписывать четыре Pump Machine и не подключать новый execution слой как gate. V6.0 добавляет `ScalpExecutionIntelligenceV600` как независимую shadow-надстройку: сохраняет отдельные уровни Bitpanda Fusion order book, сравнивает Binance executed-flow с Bitpanda execution-book, оценивает top-3/top-5 imbalance, microprice, изменение глубины, spread, slippage на диагностическом €1000 depth-probe и минимальный наблюдаемый round-trip cost floor. Для каждого V6 кадра причинно собираются future outcomes на 30/60/120/300 секунд.

**Reason**  
V5.37 уже неплохо описывает направление/дыхание рынка, но недостаточно измеряет качество исполнения именно на Bitpanda. Без shadow-периода невозможно доказать, что новый execution score отсекает плохие входы, а не просто зажимает систему. Без future outcomes нельзя отличить красивую классификацию `CONFIRMED/DIVERGENT` от реально полезного edge.

**Critical correction found during first review**  
Первая реализация пыталась записывать authenticated Fusion fee прямо в общий `FusionMarketSnapshot.feeRate`. Это незаметно изменило бы расчёты старых PM/Fusion и уничтожило V5.37 как контрольную группу. Исправлено: `feeRate`/`feeTier` остаются фиксированной V5 simulation 0.25%/side; authenticated account fee хранится отдельно как `observedAccountFeeRate/observedAccountFeeTier` и в V6.0 используется только shadow-аналитикой.

**Alternatives considered**  
1. Сразу дать V6 право блокировать BUY.  
2. Перенести фактический account fee во все старые paper engines.  
3. Сразу добавить ML/RL/Pump.fun BUY/market-making.  
4. Встроить GitHub write token в Android app для автоматической отправки отчётов.

**Rejected because**  
1. Нет forward evidence, поэтому это повторило бы старую ошибку чрезмерного зажатия.  
2. Нарушилась бы чистая контрольная группа и сравнимость V5.37.  
3. Сначала нужен причинный dataset; сложность без evidence повышает риск overfit/drift.  
4. Репозиторный write-secret в клиентском APK создаёт лишний security risk; V6.0 использует локальные безопасные exports.

**Consequences**  
- V6.0 не имеет entry/exit authority и не может разрешать/запрещать старые сделки.
- TP/SL/entry thresholds V5.37 не меняются.
- `costFloorBps` — наблюдаемая нижняя оценка расходов, а не прогноз прибыли.
- €1000 probe — диагностическая проверка depth, не position sizing.
- 24h V6 report экспортируется как UTF-8 TXT/TSV, автоматически режется на части <=900 KB и содержит SAMPLE + causal OUTCOME rows.
- Promotion V6 в отдельный paper account или gate допускается только после репрезентативного forward анализа.
- Автоматическая загрузка отчётов в GitHub и remote tuning не входят в V6.0; если понадобятся, проектировать отдельный authenticated relay/GitHub App без секретов в APK.
