from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}: {old[:180]!r}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


# ---------------------------------------------------------------------------
# V5.25: the full comparison screen must show all six active paper accounts.
# V5.24 added PM2 but the comparison UI remained hard-coded to five slots.
# No trading policy, preference store, history or reset behavior changes here.
# ---------------------------------------------------------------------------
activity = "app/src/main/java/com/example/pumppaperbot/CompetitionActivity.kt"
replace_once(activity,
             "    private val datasets = arrayOfNulls<CompetitionDataset>(5)\n",
             "    private val datasets = arrayOfNulls<CompetitionDataset>(CompetitionAccountSpec.COUNT)\n")
replace_once(activity,
             '            text = "←  СРАВНЕНИЕ ПЯТИ СЧЕТОВ"\n',
             "            text = CompetitionAccountSpec.SCREEN_TITLE\n")
replace_once(activity,
             "        repeat(5) { index ->\n",
             "        repeat(CompetitionAccountSpec.COUNT) { index ->\n")
replace_once(activity,
             "        val pumpMachine = PumpMachineStore.state(this)\n",
             "        val pumpMachine = PumpMachineStore.state(this)\n        val pumpMachine2 = PumpMachine2Store.state(this)\n")

old_pm3 = '''        val pumpMachineValue = PumpMachinePolicy.netLiquidationValue(
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
new_pm3_pm2 = '''        val pumpMachineValue = PumpMachinePolicy.netLiquidationValue(
            pumpMachine,
            fusionPrice,
            fusionMarket.feeRate
        )
        setChart(0, CompetitionDataset(
            "PUMP 3% NET • TP +3% / SL −1,5%",
            summary(
                pumpMachineValue,
                (pumpMachineValue / FusionSimPortfolio.START_BALANCE - 1.0) * 100.0,
                pumpMachine.inPosition
            ),
            candles,
            pumpMachine.trades.map { CompetitionMarker(it.time, it.action, it.price, it.pnlEur) },
            FusionTradingCosts.FEE_RATE
        ))

        val pumpMachine2Value = PumpMachine2Policy.netLiquidationValue(
            pumpMachine2,
            fusionPrice,
            fusionMarket.feeRate
        )
        setChart(1, CompetitionDataset(
            "PUMP 2% NET • TP +2% / SL −1,5%",
            summary(
                pumpMachine2Value,
                (pumpMachine2Value / FusionSimPortfolio.START_BALANCE - 1.0) * 100.0,
                pumpMachine2.inPosition
            ),
            candles,
            pumpMachine2.trades.map { CompetitionMarker(it.time, it.action, it.price, it.pnlEur) },
            FusionTradingCosts.FEE_RATE
        ))
'''
replace_once(activity, old_pm3, new_pm3_pm2)

# Shift the four remaining active accounts down by one slot.
replace_once(activity,
             '        setChart(1, CompetitionDataset(\n            "DEEPSIGX",\n',
             '        setChart(2, CompetitionDataset(\n            "DEEPSIGX",\n')
replace_once(activity,
             '        setChart(2, CompetitionDataset(\n            "APP",\n',
             '        setChart(3, CompetitionDataset(\n            "APP",\n')
replace_once(activity,
             '        setChart(3, CompetitionDataset(\n            "DEEPSIG FUSION",\n',
             '        setChart(4, CompetitionDataset(\n            "DEEPSIG FUSION",\n')
replace_once(activity,
             '        setChart(4, CompetitionDataset(\n            "СЕРЖ",\n',
             '        setChart(5, CompetitionDataset(\n            "СЕРЖ",\n')

# Main-screen button must agree with the comparison screen.
layout = "app/src/main/res/layout/activity_main.xml"
replace_once(layout,
             'android:text="СРАВНИТЬ 4 • APP | PUMP MACHINE | DEEPSIGX | FUSION"',
             'android:text="СРАВНИТЬ 6 • PM3 | PM2 | APP | DEEPSIGX | FUSION | СЕРЖ"')

# Sequential installable version. No reset/migration is introduced.
gradle = "app/build.gradle"
replace_once(gradle, "        versionCode 104\n", "        versionCode 105\n")
replace_once(gradle, '        versionName "5.24"\n', '        versionName "5.25"\n')

print("V5.25 six-account comparison patch applied; PM2 added to comparison; no account reset added")
