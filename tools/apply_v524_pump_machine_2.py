from pathlib import Path


def read(path: str) -> str:
    return Path(path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    Path(path).write_text(text, encoding="utf-8")


def replace_once(path: str, old: str, new: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one anchor, found {count}: {old[:120]!r}")
    write(path, text.replace(old, new, 1))


def replace_all_required(path: str, old: str, new: str, minimum: int = 1) -> None:
    text = read(path)
    count = text.count(old)
    if count < minimum:
        raise SystemExit(f"{path}: expected at least {minimum} anchors, found {count}: {old[:120]!r}")
    write(path, text.replace(old, new))


# ---------------------------------------------------------------------------
# V5.24: add a third independent paper account, Pump Machine 2.
# It consumes the exact same Shared Fusion Entry observation/policy as PM3/Fusion,
# but owns its own portfolio, confirmation/cooldown state and exits at +2.00% NET.
# Existing V5.23 Fusion and PM3 prefs are never cleared or migrated here.
# ---------------------------------------------------------------------------

pm3_path = "app/src/main/java/com/example/pumppaperbot/PumpMachine.kt"
pm2_path = "app/src/main/java/com/example/pumppaperbot/PumpMachine2.kt"
pm2 = read(pm3_path)
if "SharedFusionEntryObservation" not in pm2 or "SharedFusionEntryPolicy.evaluate" not in pm2:
    raise SystemExit("PumpMachine.kt is not the V5.22 shared-entry implementation")

for old, new in [
    ("PumpMachineSyncResult", "PumpMachine2SyncResult"),
    ("PumpMachineDecision", "PumpMachine2Decision"),
    ("PumpMachinePolicy", "PumpMachine2Policy"),
    ("PumpMachineStore", "PumpMachine2Store"),
    ("pump_machine_paper_v521", "pump_machine_2_paper_v524"),
    ("PUMP_MACHINE", "PUMP_MACHINE_2"),
    ("PUMP MACHINE", "PUMP MACHINE 2"),
    ("Pump Machine", "Pump Machine 2"),
    ("V5.21 Pump Machine 2.", "V5.24 Pump Machine 2."),
    ("const val TAKE_PROFIT_NET_PERCENT = 3.00", "const val TAKE_PROFIT_NET_PERCENT = 2.00"),
    ("TAKE_PROFIT_3_NET", "TAKE_PROFIT_2_NET"),
    ("+3.00%", "+2.00%"),
    ("+3,00%", "+2,00%"),
    ("+3 / -1.5", "+2 / -1.5"),
    ("+3% TP", "+2% TP"),
]:
    pm2 = pm2.replace(old, new)

if "TAKE_PROFIT_NET_PERCENT = 2.00" not in pm2:
    raise SystemExit("PM2 TP was not changed to +2.00% NET")
if 'private const val PREFS = "pump_machine_2_paper_v524"' not in pm2:
    raise SystemExit("PM2 does not have an isolated V5.24 preference store")
if 'UnifiedResearchLog.record(context, "PUMP_MACHINE_2"' not in pm2:
    raise SystemExit("PM2 research identity was not isolated")
write(pm2_path, pm2)

# ---------------------------------------------------------------------------
# Foreground and WorkManager cycles: PM2 runs beside PM3 and Fusion.
# ---------------------------------------------------------------------------
service = "app/src/main/java/com/example/pumppaperbot/PumpSignalService.kt"
replace_once(
    service,
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
                CycleStageGuard.run(
                    this, "PUMP_MACHINE_2", {
                        PumpMachine2SyncResult(
                            PumpMachine2Store.state(this),
                            "ошибка Pump Machine 2 изолирована",
                            0.0
                        )
                    }
                ) { PumpMachine2Store.sync(this) }
                CycleStageGuard.run(this, "FUSION_SIM", { FusionSimStore.state(this) }) {
'''
)

# Fast observer: an already-open PM2 gets the same fast book refresh as PM3;
# a shock-rebound entry also fans the same fresh observation to all paper engines.
replace_once(
    service,
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
''',
    '''                val pumpMachineFast = PumpMachineStore.state(this)
                val pumpMachine2Fast = PumpMachine2Store.state(this)
                if (pumpMachineFast.inPosition || pumpMachine2Fast.inPosition) {
                    val venue = BitpandaFusionStore.state(this)
                    if (!venue.fresh(now) || now - venue.lastSuccess >= 30_000L) {
                        BitpandaFusionClient().sync(this, force = true)
                    }
                    val fastNow = System.currentTimeMillis()
                    if (pumpMachineFast.inPosition) PumpMachineStore.sync(this, fastNow)
                    if (pumpMachine2Fast.inPosition) PumpMachine2Store.sync(this, fastNow)
                }

                val shock = ShockReboundStore.state(this)
                if (!shock.active || !shock.fresh(now)) return@execute
                val fusion = FusionSimStore.state(this)
                if (!shock.ready && !fusion.inPosition && !pumpMachineFast.inPosition && !pumpMachine2Fast.inPosition) return@execute

                // A fast rebound cannot wait for the 1-3 minute full cycle. Refresh only the
                // read-only execution book and run the local paper engines; no AI call is made.
                BitpandaFusionClient().sync(this, force = true)
                val shockNow = System.currentTimeMillis()
                FusionSimStore.sync(this, DeepSeekPrimaryStore.state(this), shockNow)
                PumpMachineStore.sync(this, shockNow)
                PumpMachine2Store.sync(this, shockNow)
'''
)

worker = "app/src/main/java/com/example/pumppaperbot/PumpBotWorker.kt"
replace_once(
    worker,
    '''            val pumpMachine = CycleStageGuard.run(applicationContext, "PUMP_MACHINE", {
                PumpMachineSyncResult(
                    PumpMachineStore.state(applicationContext),
                    "ошибка Pump Machine изолирована",
                    0.0
                )
            }) { PumpMachineStore.sync(applicationContext) }
            CycleStageGuard.run(applicationContext, "FUSION_SIM", {
''',
    '''            val pumpMachine = CycleStageGuard.run(applicationContext, "PUMP_MACHINE", {
                PumpMachineSyncResult(
                    PumpMachineStore.state(applicationContext),
                    "ошибка Pump Machine изолирована",
                    0.0
                )
            }) { PumpMachineStore.sync(applicationContext) }
            CycleStageGuard.run(applicationContext, "PUMP_MACHINE_2", {
                PumpMachine2SyncResult(
                    PumpMachine2Store.state(applicationContext),
                    "ошибка Pump Machine 2 изолирована",
                    0.0
                )
            }) { PumpMachine2Store.sync(applicationContext) }
            CycleStageGuard.run(applicationContext, "FUSION_SIM", {
'''
)

# ---------------------------------------------------------------------------
# Main screen: keep PM3 in its existing slot; split the full-width Fusion row
# into FUSION + PM2, exactly as requested.
# ---------------------------------------------------------------------------
layout = "app/src/main/res/layout/activity_main.xml"
replace_once(
    layout,
    '''            <Button
                android:id="@+id/btnFusionSim"
                android:layout_width="match_parent"
                android:layout_height="68dp"
                android:layout_marginTop="5dp"
                android:backgroundTint="#0F6B78"
                android:includeFontPadding="false"
                android:insetTop="0dp"
                android:insetBottom="0dp"
                android:maxLines="3"
                android:padding="2dp"
                android:text="DEEPSIG FUSION • READ-ONLY SIM&#10;€1 000,00&#10;+0,00%"
                android:textColor="#FFFFFF"
                android:textSize="11sp"
                android:textStyle="bold" />
''',
    '''            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="68dp"
                android:layout_marginTop="5dp"
                android:orientation="horizontal">

                <Button
                    android:id="@+id/btnFusionSim"
                    android:layout_width="0dp"
                    android:layout_height="68dp"
                    android:layout_weight="1"
                    android:backgroundTint="#0F6B78"
                    android:includeFontPadding="false"
                    android:insetTop="0dp"
                    android:insetBottom="0dp"
                    android:maxLines="3"
                    android:padding="2dp"
                    android:text="DEEPSIG FUSION&#10;€1 000,00&#10;+0,00%"
                    android:textColor="#FFFFFF"
                    android:textSize="11sp"
                    android:textStyle="bold" />

                <Button
                    android:id="@+id/btnPumpMachine2"
                    android:layout_width="0dp"
                    android:layout_height="68dp"
                    android:layout_marginStart="5dp"
                    android:layout_weight="1"
                    android:backgroundTint="#B85C00"
                    android:includeFontPadding="false"
                    android:insetTop="0dp"
                    android:insetBottom="0dp"
                    android:maxLines="3"
                    android:padding="2dp"
                    android:text="PUMP 2% NET&#10;€1 000,00&#10;+0,00%"
                    android:textColor="#FFFFFF"
                    android:textSize="11sp"
                    android:textStyle="bold" />
            </LinearLayout>
'''
)

main = "app/src/main/java/com/example/pumppaperbot/MainActivity.kt"
replace_once(main, "    private var btnFusionSim: Button? = null\n", "    private var btnFusionSim: Button? = null\n    private var btnPumpMachine2: Button? = null\n")
replace_once(main, "        btnFusionSim = findViewById(R.id.btnFusionSim)\n", "        btnFusionSim = findViewById(R.id.btnFusionSim)\n        btnPumpMachine2 = findViewById(R.id.btnPumpMachine2)\n")
replace_once(
    main,
    '''        btnFusionSim?.setOnClickListener {
            startActivity(Intent(this, BitpandaFusionActivity::class.java))
        }
''',
    '''        btnFusionSim?.setOnClickListener {
            startActivity(Intent(this, BitpandaFusionActivity::class.java))
        }
        btnPumpMachine2?.setOnClickListener {
            startActivity(Intent(this, PumpMachineActivity::class.java))
        }
'''
)
replace_once(main, "        val pumpMachineAccount = PumpMachineStore.state(this)\n", "        val pumpMachineAccount = PumpMachineStore.state(this)\n        val pumpMachine2Account = PumpMachine2Store.state(this)\n")
replace_once(
    main,
    '''        btnGeminiExperiment?.text = accountButtonText(
            "PUMP MACHINE",
            pumpMachineValue,
            (pumpMachineValue / FusionSimPortfolio.START_BALANCE - 1.0) * 100.0
        )
''',
    '''        btnGeminiExperiment?.text = accountButtonText(
            "PUMP 3% NET",
            pumpMachineValue,
            (pumpMachineValue / FusionSimPortfolio.START_BALANCE - 1.0) * 100.0
        )
        val pumpMachine2Value = PumpMachine2Policy.netLiquidationValue(
            pumpMachine2Account,
            fusionMarket.bid.takeIf { fusionMarket.fresh(now) } ?: accountPrice,
            fusionMarket.feeRate
        )
        btnPumpMachine2?.text = accountButtonText(
            "PUMP 2% NET",
            pumpMachine2Value,
            (pumpMachine2Value / FusionSimPortfolio.START_BALANCE - 1.0) * 100.0
        )
'''
)

# ---------------------------------------------------------------------------
# PM lab screen: retain the existing PM3/Fusion history and add PM2 side-by-side.
# ---------------------------------------------------------------------------
activity = "app/src/main/java/com/example/pumppaperbot/PumpMachineActivity.kt"
replace_once(activity, '            text = "FUSION ↔ PUMP MACHINE • 24H LAB"\n', '            text = "FUSION ↔ PUMP 3% ↔ PUMP 2% • LAB"\n')
replace_once(activity, "        root.addView(pairSummary, LinearLayout.LayoutParams(-1, dp(138)).apply { topMargin = dp(6) })\n", "        root.addView(pairSummary, LinearLayout.LayoutParams(-1, dp(166)).apply { topMargin = dp(6) })\n")
replace_once(activity, "        val pumpMachine = PumpMachineStore.state(this)\n", "        val pumpMachine = PumpMachineStore.state(this)\n        val pumpMachine2 = PumpMachine2Store.state(this)\n")
replace_once(
    activity,
    '''        val bid = market.bid.takeIf { market.fresh(now) }
            ?: pumpMachine.entryPrice.takeIf { it > 0.0 }
            ?: fusion.entryPrice

        val pumpValue = PumpMachinePolicy.netLiquidationValue(pumpMachine, bid, market.feeRate)
        val pumpTotal = (pumpValue / FusionSimPortfolio.START_BALANCE - 1.0) * 100.0
        val pumpTradeNet = PumpMachinePolicy.tradeNetPercent(pumpMachine, bid, market.feeRate)
''',
    '''        val bid = market.bid.takeIf { market.fresh(now) }
            ?: pumpMachine2.entryPrice.takeIf { it > 0.0 }
            ?: pumpMachine.entryPrice.takeIf { it > 0.0 }
            ?: fusion.entryPrice

        val pumpValue = PumpMachinePolicy.netLiquidationValue(pumpMachine, bid, market.feeRate)
        val pumpTotal = (pumpValue / FusionSimPortfolio.START_BALANCE - 1.0) * 100.0
        val pumpTradeNet = PumpMachinePolicy.tradeNetPercent(pumpMachine, bid, market.feeRate)
        val pump2Value = PumpMachine2Policy.netLiquidationValue(pumpMachine2, bid, market.feeRate)
        val pump2Total = (pump2Value / FusionSimPortfolio.START_BALANCE - 1.0) * 100.0
        val pump2TradeNet = PumpMachine2Policy.tradeNetPercent(pumpMachine2, bid, market.feeRate)
'''
)
replace_once(activity, "        val pmLast = pumpMachine.trades.lastOrNull()\n", "        val pmLast = pumpMachine.trades.lastOrNull()\n        val pm2Last = pumpMachine2.trades.lastOrNull()\n")
replace_once(
    activity,
    '''            append(String.format(Locale.GERMANY, "MACHINE  €%.2f  %+.2f%%  • %s", pumpValue, pumpTotal, if (pumpMachine.inPosition) "В PUMP" else "В EUR"))
            append("\\n")
            append("Последнее: FUSION ${lastTradeLabel(fusionLast)} • MACHINE ${lastTradeLabel(pmLast)}")
''',
    '''            append(String.format(Locale.GERMANY, "PUMP 3%%  €%.2f  %+.2f%%  • %s", pumpValue, pumpTotal, if (pumpMachine.inPosition) "В PUMP" else "В EUR"))
            append("\\n")
            append(String.format(Locale.GERMANY, "PUMP 2%%  €%.2f  %+.2f%%  • %s", pump2Value, pump2Total, if (pumpMachine2.inPosition) "В PUMP" else "В EUR"))
            append("\\n")
            append("Последнее: FUSION ${lastTradeLabel(fusionLast)} • PM3 ${lastTradeLabel(pmLast)} • PM2 ${lastTradeLabel(pm2Last)}")
'''
)
replace_once(
    activity,
    '''        summary.text = buildString {
            append(String.format(Locale.GERMANY, "PUMP MACHINE • СЧЁТ €%.2f • ВСЕГО %+.2f%%", pumpValue, pumpTotal))
            append("\\n")
            if (pumpMachine.inPosition) {
                append(String.format(Locale.GERMANY, "ТЕКУЩАЯ СДЕЛКА %+.2f%% NET", pumpTradeNet))
                append(" • TP +3,00% • SL −1,50%")
            } else {
                append("В EUR • ждём следующий независимый общий Fusion-вход")
            }
        }
        summary.setTextColor(Color.parseColor(if (pumpTotal >= 0.0) "#7EE787" else "#FF7B72"))

        status.text = "ВХОД У ОБОИХ ФИЗИЧЕСКИ ОДИНАКОВЫЙ; состояния подтверждения и cooldown независимы.\\n" +
            "MACHINE: ${PumpMachineStore.lastStatus(this)}"
''',
    '''        summary.text = buildString {
            append(String.format(Locale.GERMANY, "PM3  €%.2f  %+.2f%%", pumpValue, pumpTotal))
            if (pumpMachine.inPosition) append(String.format(Locale.GERMANY, " • сделка %+.2f%% NET", pumpTradeNet))
            append("\\n")
            append(String.format(Locale.GERMANY, "PM2  €%.2f  %+.2f%%", pump2Value, pump2Total))
            if (pumpMachine2.inPosition) append(String.format(Locale.GERMANY, " • сделка %+.2f%% NET", pump2TradeNet))
            append("\\nPM3: TP +3,00% NET • PM2: TP +2,00% NET • обе SL −1,50% NET")
        }
        summary.setTextColor(Color.parseColor(if (pump2Total >= 0.0) "#7EE787" else "#FF7B72"))

        status.text = "ВХОДНОЙ МОЗГ ОДИН: Shared Fusion Entry. После собственного выхода cooldown независимый.\\n" +
            "PM3: ${PumpMachineStore.lastStatus(this)}\\nPM2: ${PumpMachine2Store.lastStatus(this)}"
'''
)
replace_once(activity, '            pumpMachine.trades.takeLast(60).forEach { add(PairEvent("MACHINE", it)) }\n', '            pumpMachine.trades.takeLast(60).forEach { add(PairEvent("PM3", it)) }\n            pumpMachine2.trades.takeLast(60).forEach { add(PairEvent("PM2", it)) }\n')
replace_once(activity, '            "Сделок пока нет. После V5.22 оба участника получают один и тот же входной снимок рынка, но ведут позиции независимо."\n', '            "Сделок пока нет. Fusion, PM3 и PM2 получают один общий входной снимок рынка, но ведут позиции и выходы независимо."\n')

# ---------------------------------------------------------------------------
# Durable diagnostics: PM2 is visible in cycle logs, export and the append-only ledger.
# ---------------------------------------------------------------------------
unified = "app/src/main/java/com/example/pumppaperbot/UnifiedResearchLog.kt"
replace_all_required(unified, "        val pumpMachine = PumpMachineStore.state(context)\n", "        val pumpMachine = PumpMachineStore.state(context)\n        val pumpMachine2 = PumpMachine2Store.state(context)\n")
replace_once(
    unified,
    '''        record(
            context,
            "PUMP_MACHINE",
            if (pumpMachine.inPosition) "IN_POSITION" else "CYCLE",
            "$source; value=$pumpMachineValue; tradeNet=${PumpMachinePolicy.tradeNetPercent(pumpMachine, fusionPrice, fusionMarket.feeRate)}; " +
                "trades=${pumpMachine.trades.size}; ${PumpMachineStore.lastStatus(context)}",
            now
        )
''',
    '''        record(
            context,
            "PUMP_MACHINE",
            if (pumpMachine.inPosition) "IN_POSITION" else "CYCLE",
            "$source; value=$pumpMachineValue; tradeNet=${PumpMachinePolicy.tradeNetPercent(pumpMachine, fusionPrice, fusionMarket.feeRate)}; " +
                "trades=${pumpMachine.trades.size}; ${PumpMachineStore.lastStatus(context)}",
            now
        )
        val pumpMachine2Value = PumpMachine2Policy.netLiquidationValue(
            pumpMachine2,
            fusionPrice,
            fusionMarket.feeRate
        )
        record(
            context,
            "PUMP_MACHINE_2",
            if (pumpMachine2.inPosition) "IN_POSITION" else "CYCLE",
            "$source; value=$pumpMachine2Value; tradeNet=${PumpMachine2Policy.tradeNetPercent(pumpMachine2, fusionPrice, fusionMarket.feeRate)}; " +
                "trades=${pumpMachine2.trades.size}; ${PumpMachine2Store.lastStatus(context)}",
            now
        )
'''
)
replace_once(unified, '                .put("PumpMachine", PumpMachineStore.toJson(pumpMachine))\n', '                .put("PumpMachine", PumpMachineStore.toJson(pumpMachine))\n                .put("PumpMachine2", PumpMachine2Store.toJson(pumpMachine2))\n')

ledger = "app/src/main/java/com/example/pumppaperbot/ResearchPerformanceLedger.kt"
replace_once(
    ledger,
    '''            capturePumpMachine(
                db,
                PumpMachineStore.state(context),
                !hasEpoch(db, "PumpMachine", "V5.21+")
            )
''',
    '''            capturePumpMachine(
                db,
                PumpMachineStore.state(context),
                !hasEpoch(db, "PumpMachine", "V5.21+")
            )
            capturePumpMachine2(
                db,
                PumpMachine2Store.state(context),
                !hasEpoch(db, "PumpMachine2", "V5.24+")
            )
'''
)
replace_once(
    ledger,
    '''    private fun captureUser(db: SQLiteDatabase, value: UserPaperPortfolio, fullImport: Boolean) {
''',
    '''    private fun capturePumpMachine2(
        db: SQLiteDatabase,
        value: FusionSimPortfolio,
        fullImport: Boolean
    ) {
        (if (fullImport) value.trades else value.trades.takeLast(20)).forEach {
            insert(db, "PumpMachine2", "V5.24+", "TRADE", "${it.time}:${it.decisionId}:${it.action}",
                it.action, it.time, it.price, it.pnlEur, it.reason)
        }
        (if (fullImport) value.decisions else value.decisions.takeLast(50)).forEach {
            insert(db, "PumpMachine2", "V5.24+", "DECISION", "${it.time}:${it.decisionId}",
                it.requestedAction, it.time, it.venuePrice, 0.0, "${it.result}; ${it.reason}")
        }
    }

    private fun captureUser(db: SQLiteDatabase, value: UserPaperPortfolio, fullImport: Boolean) {
'''
)

# ---------------------------------------------------------------------------
# Version only. No reset/migration is introduced in V5.24.
# ---------------------------------------------------------------------------
gradle = "app/build.gradle"
replace_once(gradle, "        versionCode 103\n", "        versionCode 104\n")
replace_once(gradle, '        versionName "5.23"\n', '        versionName "5.24"\n')

print("V5.24 Pump Machine 2 (+2.00% NET) patch applied; no existing account reset added")
