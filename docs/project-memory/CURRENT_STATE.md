# PUMP / PumpBot — CURRENT STATE

Обновлено: **2026-08-28**

Это короткий снимок текущего состояния. Старую историю сюда не накапливать.

## VERSION / BRANCH

- Canonical branch: `main`.
- Current source version: **V6.3**, `versionCode 121`.
- `applicationId`: `com.example.pumppaperbot.v8` — сохранён для совместимого update/data continuity.
- V6.2 is present on canonical `main` at merge commit `949fa1b`.
- Full `main` build run **#423** (`33020059070`): **success** — unit tests, lint, assemble, APK package/version/activity/v2-signature/ZIP checks and artifact upload passed.

## BUILD / RELEASE STATUS

- V6.0 source/runtime code прошёл полный CI на `main`.
- CI source artifact: `PumpSignal-V6.0-Execution-Intelligence-Intermediate.apk`.
- Final compatible handoff APK создан: `PumpSignal-V6.0-Compatible-FINAL.apk`.
- Final APK size: `7,669,709` bytes.
- Final APK SHA-256: `c07089854a9326e7abc742d5fc87d4922244e0e66b1a8e0320d5ba38394d5701`.
- Final APK uses APK Signature Scheme **v2** and was independently verified after signing.
- Final signing certificate SHA-256: `1f778c4291c9d11c5f89f4de8773bda35a0125031adc05785daee23f27dc7823` — совпадает с исторической совместимой `.v8` линией.
- Keystore/пароль получены из приватного Google Drive recovery bundle; они **не записаны в GitHub**.
- Новый signing key не создавался. Uninstall/clean install для обновления не использовать.
- Фактически установленная сейчас на телефоне владельца версия: `UNKNOWN` до подтверждения установки V6.0.

## V6.0 MATERIAL CHANGE

## V6.3 HUMAN FACTOR EXPERIMENT

- Добавлены два независимых VWAP 32,65 paper-счёта: полностью автономный без звуков и Human Factor с обязательным подтверждением владельца.
- Human Factor показывает подготовку 90–97/100 и постоянную карточку подтверждения при 98–100/100; отменяет карточку при распаде условий.
- Общий экран теперь содержит 10 счетов. Остальные виртуальные стратегии продолжают расчёт, но не создают пользовательские trade-звонки; safety/exit предупреждения по ручной позиции сохраняются.
- Реальные ордера не добавлены.

V6.0 добавляет **shadow-only execution intelligence**, не меняя торговую власть V5.37 и не меняя PM entry/TP/SL thresholds.

## UNRELEASED CROSS-MARKET RESEARCH

- Третий независимый replay Z-Score VWAP, volatility squeeze, strict lead-lag и session-open breakout также не нашёл edge. Squeeze дал 50% на early/24 fills, но только 14,29% и отрицательный NET на control/14 fills; лучший control WR серии 14,29%. Strict lead-lag не создал ни одного сигнала. Production authority не выдана.
- Отдельный ранее не использованный 120-дневный replay cost-aware VWAP и session gap-fill не превзошёл прежние 32,65%: обе формулы дали 8,33% win rate на control, отрицательный NET и PF значительно ниже 1. Production authority не выдана; следующий новый уровень требует tape/L2.
- Новый 120-дневный причинный replay пяти контекстных идей (regime-adaptive, liquidity sweep, VWAP reversion, sell exhaustion, 4H node + relative strength) не нашёл edge: лучший control win rate 32,65%, лучший средний NET −0,419%, лучший PF 0,399. Synthetic predicates прошли 10/10, но production authority не выдана.

- Причинный 14-дневный replay PUMP/BTC/SOL проверил BTC/SOL как внешнее подтверждение предполагаемого дна PUMP.
- Одноминутная contemporaneous correlation заметна (BTC около 0,34, SOL около 0,43), но устойчивого опережения PUMP на 1–10 минут не найдено.
- На контрольной части синхронный рост BTC/SOL изменил PM-like win rate только с 31,3% до 32,3%; другие turn-фильтры не улучшили результат.
- Недельное улучшение 33,3% → 41,2% не воспроизвелось на 14 днях и считается коротким нестабильным эффектом.
- Production entry authority не изменена; версия/сборка не создавались. Инструмент: `tools/research_cross_market_breath.js`; отчёты и постоянный реестр: `docs/research-lab/`.
- Отдельный order-type replay (`tools/research_order_execution.js`) сравнил market/limit/stop-market/stop-limit/OCO/trailing. Все варианты остались отрицательными; лучший контрольный вход stop-limit дал 24,6% wins и −0,397% среднего NET при 38/99 незаполненных сигналах. Реальные ордера не добавлены.
- 30-дневный flow/absorption replay (`tools/research_flow_absorption.js`) проверил taker SELL decay, buy-share/delta recovery, absorption, volume spike, relative strength, soft-score, ATR/time-stop, UTC sessions и 4h context. Европейская UTC-сессия и отрицательный 4h-контекст дали устойчивый lift, но ни одна заранее фиксированная гипотеза не получила положительный NET одновременно на train и control. Production не изменён.
- 60-дневный replay пяти дополнительных фиксированных гипотез (`tools/research_five_hypotheses.js`) разделён на два последовательных 30-дневных периода. Quiet-entry, fake-breakout, 60m volume-cluster и BTC/SOL cross-impulse отрицательны в обеих частях. Session-rebound дал recent +0,113% среднего NET, PF 1,255 на 62 fills, но early −0,403%, PF 0,373 на 45 fills; он сохранён только как замороженный forward/shadow-кандидат. Ни одна гипотеза не разрешена для V6.3.

- Bitpanda Fusion сохраняет отдельные уровни стакана.
- Считаются top-3/top-5 imbalance, microprice, spread, depth/slippage и execution cost floor.
- Binance-flow сравнивается с Bitpanda execution book: `CONFIRMED`, `LEADING`, `FUSION_LEADING`, `DIVERGENT`, `BAD_EXECUTION`, `NEUTRAL`, `INSUFFICIENT_DATA`.
- Authenticated Fusion account fee сохраняется отдельно как V6 evidence; старые paper engines остаются на фиксированной 0.25%/side control-group cost.
- V6 **не может разрешать или запрещать сделки** существующих стратегий.
- Для каждого V6 наблюдения собираются causal forward outcomes на 30/60/120/300 секунд по будущим Bitpanda bid snapshots; пропущенные горизонты записываются как `MISSED`, а не подменяются поздней ценой.
- Future outcome принимается только если `Fusion.lastSuccess > originAt`, чтобы прошлый стакан не мог быть записан как будущее.

## REPORTING

- Основной V6 support export: UTF-8 `.txt`, 24 часа.
- Файлы автоматически разбиваются примерно до **900 KB** на часть, чтобы не упираться в старую проблему загрузки >2 MB.
- Отчёт содержит account/trade state, V6 execution samples и causal outcome rows.
- Старый большой JSON остаётся диагностическим архивом, но не является основным каналом для V6 анализа.
- Автоматическая запись Android-приложения в GitHub пока **не реализована**, чтобы не хранить GitHub write-token в APK. Безопасный relay/GitHub-App канал — отдельный будущий этап.

## WHAT REMAINS UNPROVEN

- Глобальная прибыльность/expectancy текущего набора стратегий: **не доказана**.
- Полезность V6 execution score как будущего hard/soft gate: **не доказана**. V6 должна накопить forward evidence до promotion.
- Full live↔replay equivalence текущего fast PM/coach stack: `NEEDS_VERIFICATION`.
- Shared DeepSeek tuning remains a guarded soft-learning layer; не расширять его власть без NET forward evidence.
- Реальные ордера всё ещё не реализованы и требуют отдельного явного решения владельца.

## CURRENT MAIN PRIORITY

**Установить совместимо подписанный V6.0 поверх существующей `.v8` установки без удаления приложения, затем накопить V6 compact TXT + causal outcomes и только после этого решать, какую часть V6 execution evidence повышать из shadow в decision support.**

## INVARIANTS

1. Не менять package/signing identity.
2. Не удалять установленное приложение ради update.
3. Не очищать накопленную history/state.
4. Не подключать V6 shadow как hard gate без evidence review.
5. Не менять одновременно V5 control thresholds и V6 evaluation logic — иначе теряется контрольная группа.
6. Любую будущую автоюстировку оценивать по NET outcomes с guarded trial/rollback.
