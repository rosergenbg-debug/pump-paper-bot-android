from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}: {old[:180]!r}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


def insert_before_once(path: str, anchor: str, insertion: str) -> None:
    replace_once(path, anchor, insertion + anchor)


# ---------------------------------------------------------------------------
# 1) Retire old DEEPSIG paper execution. DeepSeek remains an analyst, but the
#    top competition account is now the local Pump Machine paper engine.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/example/pumppaperbot/PumpSignalService.kt"
replace_once(
    path,
    '''                val deepSeekPaper = CycleStageGuard.run(
                    this, "DEEPSIG_PAPER", { DeepSeekPaperOutcome("ошибка модуля изолирована") }
                ) { DeepSeekPaperCoordinator().sync(this, deepSeek, source) }
                CycleStageGuard.run(this, "FUSION_SIM", { FusionSimStore.state(this) }) {
                    FusionSimStore.sync(this, deepSeek)
                }
''',
    '''                val pumpMachine = CycleStageGuard.run(
                    this, "PUMP_MACHINE", {
                        PumpMachineSyncResult(
                            PumpMachineStore.state(this),
                            "ошибка Pump Machine изолирована",
                            0.0
                        )
                    }
                ) { PumpMachineStore.sync(this) }
                CycleStageGuard.run(this, "FUSION_SIM", { FusionSimStore.state(this) }) {
                    FusionSimStore.sync(this, deepSeek)
                }
'''
)
replace_once(
    path,
    '''                    "проверка завершена; DeepSig: ${deepSeek.action}; виртуальный счёт: ${deepSeekPaper.status}; Gemini контролирует только открытую позицию Сержа",
''',
    '''                    "проверка завершена; DeepSeek аналитик: ${deepSeek.action}; Pump Machine: ${pumpMachine.status}; Gemini контролирует только открытую позицию Сержа",
'''
)

# Fast 15-second observer: while Pump Machine owns a position, refresh the
# read-only execution quote at most every ~30s and enforce TP/SL without waiting
# for the 2-3 minute full cycle. Shock entry also runs Pump Machine after quote refresh.
replace_once(
    path,
    '''                val shock = ShockReboundStore.state(this)
                if (!shock.active || !shock.fresh(now)) return@execute
                val fusion = FusionSimStore.state(this)
                if (!shock.ready && !fusion.inPosition) return@execute

                // A fast rebound cannot wait for the 1-3 minute full cycle. Refresh only the
                // read-only execution book and run the local paper engine; no AI call is made.
                BitpandaFusionClient().sync(this, force = true)
                FusionSimStore.sync(this, DeepSeekPrimaryStore.state(this), System.currentTimeMillis())
''',
    '''                val pumpMachineFast = PumpMachineStore.state(this)
                if (pumpMachineFast.inPosition) {
                    val venue = BitpandaFusionStore.state(this)
                    if (!venue.fresh(now) || now - venue.lastSuccess >= 30_000L) {
                        BitpandaFusionClient().sync(this, force = true)
                    }
                    PumpMachineStore.sync(this, System.currentTimeMillis())
                }

                val shock = ShockReboundStore.state(this)
                if (!shock.active || !shock.fresh(now)) return@execute
                val fusion = FusionSimStore.state(this)
                if (!shock.ready && !fusion.inPosition && !pumpMachineFast.inPosition) return@execute

                // A fast rebound cannot wait for the 1-3 minute full cycle. Refresh only the
                // read-only execution book and run the local paper engines; no AI call is made.
                BitpandaFusionClient().sync(this, force = true)
                FusionSimStore.sync(this, DeepSeekPrimaryStore.state(this), System.currentTimeMillis())
                PumpMachineStore.sync(this, System.currentTimeMillis())
'''
)


# WorkManager fallback gets the same replacement.
path = "app/src/main/java/com/example/pumppaperbot/PumpBotWorker.kt"
replace_once(
    path,
    '''            val deepSeekPaper = CycleStageGuard.run(applicationContext, "DEEPSIG_PAPER", {
                DeepSeekPaperOutcome("ошибка модуля изолирована")
            }) { DeepSeekPaperCoordinator().sync(applicationContext, deepSeek, source) }
            CycleStageGuard.run(applicationContext, "FUSION_SIM", {
                FusionSimStore.state(applicationContext)
            }) { FusionSimStore.sync(applicationContext, deepSeek) }
''',
    '''            val pumpMachine = CycleStageGuard.run(applicationContext, "PUMP_MACHINE", {
                PumpMachineSyncResult(
                    PumpMachineStore.state(applicationContext),
                    "ошибка Pump Machine изолирована",
                    0.0
                )
            }) { PumpMachineStore.sync(applicationContext) }
            CycleStageGuard.run(applicationContext, "FUSION_SIM", {
                FusionSimStore.state(applicationContext)
            }) { FusionSimStore.sync(applicationContext, deepSeek) }
'''
)
replace_once(
    path,
    '''                "проверка завершена; DeepSeek: ${deepSeek.action}; виртуальный счёт: ${deepSeekPaper.status}; Gemini контролирует только открытую позицию Сержа",
''',
    '''                "проверка завершена; DeepSeek аналитик: ${deepSeek.action}; Pump Machine: ${pumpMachine.status}; Gemini контролирует только открытую позицию Сержа",
'''
)


# ---------------------------------------------------------------------------
# 2) Competition top chart is Pump Machine, with its own clean V5.21 history.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/example/pumppaperbot/CompetitionActivity.kt"
replace_once(
    path,
    '''        val gemini = GeminiPaperStore.state(this).portfolio
        val geminiExitExperiment = GeminiExitExperimentStore.state(this)?.portfolio
''',
    '''        val pumpMachine = PumpMachineStore.state(this)
        val geminiExitExperiment = GeminiExitExperimentStore.state(this)?.portfolio
'''
)
replace_once(
    path,
    '''        setChart(0, CompetitionDataset(
            "DEEPSIG",
            summary(gemini.value(price), gemini.profitPercent(price), gemini.inPosition),
            candles,
            gemini.trades.map { CompetitionMarker(it.time, it.action, it.price, it.pnlEur) },
            0.0015
        ))
''',
    '''        val pumpMachineValue = PumpMachinePolicy.netLiquidationValue(
            pumpMachine,
            fusionPrice,
            fusionMarket.feeRate
        )
        setChart(0, CompetitionDataset(
            "PUMP MACHINE • TP +3% / SL −1,5%",
            summary(
                pumpMachineValue,
                (pumpMachineValue / FusionSimPortfolio.START_BALANCE - 1.0) * 100.0,
                pumpMachine.inPosition
            ),
            candles,
            pumpMachine.trades.map { CompetitionMarker(it.time, it.action, it.price, it.pnlEur) },
            FusionTradingCosts.FEE_RATE
        ))
'''
)


# ---------------------------------------------------------------------------
# 3) Main screen button and latest virtual-event slot now point at Pump Machine.
#    DeepSeek cards remain clearly labelled as ANALYST, not as the trading account.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/example/pumppaperbot/MainActivity.kt"
replace_once(
    path,
    '''        btnGeminiExperiment?.setOnClickListener {
            startActivity(Intent(this, GeminiExperimentActivity::class.java))
        }
''',
    '''        btnGeminiExperiment?.setOnClickListener {
            startActivity(Intent(this, PumpMachineActivity::class.java))
        }
'''
)
replace_once(
    path,
    '''        val geminiAccount = GeminiPaperStore.state(this).portfolio
''',
    '''        val pumpMachineAccount = PumpMachineStore.state(this)
'''
)
replace_once(
    path,
    '''        btnGeminiExperiment?.text = accountButtonText(
            "DEEPSIG",
            geminiAccount.value(accountPrice),
            geminiAccount.profitPercent(accountPrice)
        )
''',
    '''        val pumpMachineValue = PumpMachinePolicy.netLiquidationValue(
            pumpMachineAccount,
            fusionMarket.bid.takeIf { fusionMarket.fresh(now) } ?: accountPrice,
            fusionMarket.feeRate
        )
        btnGeminiExperiment?.text = accountButtonText(
            "PUMP MACHINE",
            pumpMachineValue,
            (pumpMachineValue / FusionSimPortfolio.START_BALANCE - 1.0) * 100.0
        )
'''
)
replace_once(
    path,
    '''                GeminiPaperStore.state(this@MainActivity).portfolio.trades.lastOrNull()?.let {
                    add(PaperEvent("DEEPSIG", it.action, it.time, it.reason))
                }
''',
    '''                PumpMachineStore.state(this@MainActivity).trades.lastOrNull()?.let {
                    add(PaperEvent("PUMP MACHINE", it.action, it.time, it.reason))
                }
'''
)
replace_once(
    path,
    '''            val inPosition = GeminiPaperStore.state(this).portfolio.inPosition
''',
    '''            val inPosition = PumpMachineStore.state(this).inPosition
'''
)

path = "app/src/main/res/layout/activity_main.xml"
replace_once(path, 'android:text="DEEPSIG • ОСНОВНОЙ • ожидает первый анализ"',
             'android:text="DEEPSEEK АНАЛИТИК • ожидает первый анализ"')
replace_once(path, 'android:text="DEEPSIG • ГОТОВНОСТЬ ВХОДА\\n1/10 • НЕ ВХОДИТЬ\\nЖдём свежий подтверждённый анализ"',
             'android:text="DEEPSEEK АНАЛИТИК • РЫНОЧНЫЙ КОНТЕКСТ\\nне управляет счётом Pump Machine"')
replace_once(path, 'android:text="DEEPSIG&#10;€1 000,00&#10;+0,00%"',
             'android:text="PUMP MACHINE&#10;€1 000,00&#10;+0,00%"')
replace_once(path, 'android:text="СРАВНИТЬ 4 • APP | DEEPSIG | DEEPSIGX | FUSION"',
             'android:text="СРАВНИТЬ 4 • APP | PUMP MACHINE | DEEPSIGX | FUSION"')


# ---------------------------------------------------------------------------
# 4) Pump Machine gets its own activity declaration.
# ---------------------------------------------------------------------------
path = "app/src/main/AndroidManifest.xml"
replace_once(
    path,
    '''        <activity
            android:name=".CompetitionActivity"
            android:exported="false"
            android:screenOrientation="portrait" />
''',
    '''        <activity
            android:name=".CompetitionActivity"
            android:exported="false"
            android:screenOrientation="portrait" />
        <activity
            android:name=".PumpMachineActivity"
            android:exported="false"
            android:screenOrientation="portrait" />
'''
)


# ---------------------------------------------------------------------------
# 5) Unified JSON log and append-only ledger record Pump Machine explicitly.
#    Old DeepSig data remains only as historical/legacy data and is not executed.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/example/pumppaperbot/UnifiedResearchLog.kt"
replace_once(
    path,
    '''/** One sanitized journal for APP, DeepSig, DeepSigX and FusionSim. */
''',
    '''/** One sanitized journal for APP, Pump Machine, DeepSigX and FusionSim. */
'''
)
replace_once(
    path,
    '''        val deepSig = GeminiPaperStore.state(context).portfolio
        val deepSigX = GeminiExitExperimentStore.state(context)?.portfolio ?: GeminiPaperPortfolio()
''',
    '''        val pumpMachine = PumpMachineStore.state(context)
        val deepSigX = GeminiExitExperimentStore.state(context)?.portfolio ?: GeminiPaperPortfolio()
'''
)
replace_once(
    path,
    '''        record(context, "DEEPSIG", deepSeek.action, "$source; value=${deepSig.value(price)}; ${deepSeek.summary}", now)
''',
    '''        val pumpMachineValue = PumpMachinePolicy.netLiquidationValue(
            pumpMachine,
            fusionPrice,
            fusionMarket.feeRate
        )
        record(
            context,
            "PUMP_MACHINE",
            if (pumpMachine.inPosition) "IN_POSITION" else "CYCLE",
            "$source; value=$pumpMachineValue; tradeNet=${PumpMachinePolicy.tradeNetPercent(pumpMachine, fusionPrice, fusionMarket.feeRate)}; " +
                "trades=${pumpMachine.trades.size}; ${PumpMachineStore.lastStatus(context)}",
            now
        )
'''
)
replace_once(
    path,
    '''        val deepSig = GeminiPaperStore.state(context, includeActivity = true)
        val deepSigX = GeminiExitExperimentStore.state(context)
''',
    '''        val retiredDeepSig = GeminiPaperStore.state(context, includeActivity = true)
        val pumpMachine = PumpMachineStore.state(context)
        val deepSigX = GeminiExitExperimentStore.state(context)
'''
)
replace_once(
    path,
    '''                .put("DeepSig", geminiJson(deepSig.portfolio))
                .put("DeepSigX", deepSigX?.let { geminiJson(it.portfolio)
''',
    '''                .put("PumpMachine", PumpMachineStore.toJson(pumpMachine))
                .put("DeepSigRetired", geminiJson(retiredDeepSig.portfolio).put("retiredInV521", true))
                .put("DeepSigX", deepSigX?.let { geminiJson(it.portfolio)
'''
)
replace_once(
    path,
    '''            .put("deepSigActivity", JSONArray(deepSig.activity.map { it.toJson() }))
''',
    '''            .put("deepSigRetiredActivity", JSONArray(retiredDeepSig.activity.map { it.toJson() }))
'''
)

path = "app/src/main/java/com/example/pumppaperbot/ResearchPerformanceLedger.kt"
replace_once(
    path,
    '''            captureGemini(
                db, "DeepSig", "V5+", GeminiPaperStore.state(context).portfolio,
                !hasEpoch(db, "DeepSig", "V5+")
            )
''',
    '''            capturePumpMachine(
                db,
                PumpMachineStore.state(context),
                !hasEpoch(db, "PumpMachine", "V5.21+")
            )
'''
)
insert_before_once(
    path,
    '''    private fun captureUser(db: SQLiteDatabase, value: UserPaperPortfolio, fullImport: Boolean) {
''',
    '''    private fun capturePumpMachine(
        db: SQLiteDatabase,
        value: FusionSimPortfolio,
        fullImport: Boolean
    ) {
        (if (fullImport) value.trades else value.trades.takeLast(20)).forEach {
            insert(db, "PumpMachine", "V5.21+", "TRADE", "${it.time}:${it.decisionId}:${it.action}",
                it.action, it.time, it.price, it.pnlEur, it.reason)
        }
        (if (fullImport) value.decisions else value.decisions.takeLast(50)).forEach {
            insert(db, "PumpMachine", "V5.21+", "DECISION", "${it.time}:${it.decisionId}",
                it.requestedAction, it.time, it.venuePrice, 0.0, "${it.result}; ${it.reason}")
        }
    }

'''
)


# ---------------------------------------------------------------------------
# 6) Sequential installable version.
# ---------------------------------------------------------------------------
path = "app/build.gradle"
replace_once(
    path,
    '''        versionCode 100
        versionName "5.20"
''',
    '''        versionCode 101
        versionName "5.21"
'''
)

print("V5.21 Pump Machine patch applied")
