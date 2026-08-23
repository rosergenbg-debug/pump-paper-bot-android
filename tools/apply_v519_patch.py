from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}: {old[:120]!r}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


def replace_all_checked(path: str, old: str, new: str, expected: int) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != expected:
        raise SystemExit(f"{path}: expected {expected} matches, found {count}: {old!r}")
    p.write_text(text.replace(old, new), encoding="utf-8")


# ---------------------------------------------------------------------------
# 1) DeepSig must own only its own paper position. Fusion stays context only.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/example/pumppaperbot/DeepSeekPrimaryAnalyst.kt"
replace_once(
    path,
    '''        val aiPaperPortfolio = GeminiPaperStore.state(context).portfolio
        val aiPaperPositionOpen = aiPaperPortfolio.inPosition
        val managedVirtualPositionOpen = aiPaperPositionOpen || fusionSim.inPosition
        val fusionVenueFresh = fusion.fresh(now)
''',
    '''        val aiPaperPortfolio = GeminiPaperStore.state(context).portfolio
        val aiPaperPositionOpen = aiPaperPortfolio.inPosition
        val positionScope = DeepSigPositionScope(
            deepSigPositionOpen = aiPaperPositionOpen,
            fusionPositionOpen = fusionSim.inPosition
        )
        val deepSigPositionOpen = positionScope.actionPositionOpen
        val fusionVenueFresh = fusion.fresh(now)
'''
)
replace_all_checked(path, "managedVirtualPositionOpen", "deepSigPositionOpen", 8)
replace_once(
    path,
    '''            .put("managed_virtual_position_open", deepSigPositionOpen)
            .put("fusion_priority_position", JSONObject()
''',
    '''            .put("managed_virtual_position_open", deepSigPositionOpen)
            .put("fusion_position_open_context_only", positionScope.fusionContextOnly)
            .put("fusion_priority_position", JSONObject()
'''
)
replace_once(
    path,
    '''        val allowedActions = if (deepSigPositionOpen) {
            setOf("HOLD", "WATCH", "EXIT")
        } else {
            setOf("BUY", "HOLD", "WATCH")
        }
''',
    '''        val allowedActions = positionScope.allowedActions
'''
)
replace_once(
    path,
    '''            fusion_priority_position — отдельная виртуальная позиция исполнения на Bitpanda Fusion. Когда
            maximum_control_active=true, считай её высшим приоритетом среди виртуальных исследований: контролируй
            исходную гипотезу, свежие продажи, flow/CVD, 5/15/30/60 минут, BTC/SOL, rapid drop, фактический bid/ask,
            спред, комиссию, чистый PnL, достигнутый пик и откат от пика. Снижение на 2% само по себе не EXIT.
            Если открыт только FusionSim, решение EXIT относится только к нему и будет виртуально исполнено по
            свежему bid. Эта позиция не является позицией Сержа, не меняет его PnL и не нажимает его кнопки.
            При просроченном Fusion-стакане продолжай оценивать рыночный риск, но не выдумывай цену исполнения:
            приложение само откажется виртуально закрывать позицию без свежего bid.
''',
    '''            fusion_priority_position — отдельная локальная paper-позиция FusionSim и только дополнительный
            рыночный контекст для DeepSig. Даже когда maximum_control_active=true, её bid/ask, спред, PnL, пик и
            flow можно использовать как независимое наблюдение за исполнимостью и риском, но открытая FusionSim
            позиция не является позицией DeepSig, не запрещает DeepSig BUY и не переводит DeepSig в EXIT.
            Локальная FusionSim сама исполняет собственные BUY/EXIT по своим правилам; action этого ответа управляет
            только paper-счётом DeepSig. При просроченном Fusion-стакане просто не используй его как свежий факт.
'''
)
replace_once(
    path,
    '''            Ты управляешь виртуальным счётом DeepSig и приоритетно сопровождаешь связанную FusionSim-позицию.
            managed_virtual_position_open означает, что хотя бы одна из этих позиций открыта. BUY разрешён лишь
            когда обе закрыты; EXIT закрывает открытый DeepSig и/или FusionSim по правилам соответствующего
            виртуального исполнения. Не меняй счёт APP или Сержа.
''',
    '''            Ты управляешь только виртуальным счётом DeepSig. managed_virtual_position_open относится только
            к собственной позиции DeepSig. FusionSim — отдельный локальный paper-участник и остаётся контекстом:
            открытая Fusion-позиция не блокирует DeepSig BUY, не создаёт DeepSig EXIT и не является целью его сделки.
            BUY/EXIT этого ответа меняют только счёт DeepSig. Не меняй счёт APP, FusionSim или Сержа.
'''
)
replace_once(
    path,
    '''        val activeBuyAt = if (fusionSim.inPosition) {
            fusionEntryTime.takeIf { it > 0L } ?: now
        } else aiPaperPortfolio.trades.lastOrNull { it.action == "BUY" }?.time ?: now
        val managedReturn = if (fusionSim.inPosition) {
            fusionMetrics.netPnlPercent
        } else if (aiPaperPortfolio.inPosition && aiPaperPortfolio.entryPrice > 0.0 && currentPrice > 0.0) {
            (currentPrice / aiPaperPortfolio.entryPrice - 1.0) * 100.0
        } else 0.0
''',
    '''        val activeBuyAt = aiPaperPortfolio.trades.lastOrNull { it.action == "BUY" }?.time ?: now
        val managedReturn = if (aiPaperPortfolio.inPosition && aiPaperPortfolio.entryPrice > 0.0 && currentPrice > 0.0) {
            (currentPrice / aiPaperPortfolio.entryPrice - 1.0) * 100.0
        } else 0.0
'''
)
replace_once(
    path,
    '''                forcePro = fusionPriority.active && exitFusion.emergency &&
                    snapshot.rapidDrop.active && !snapshot.rapidDrop.recoveryConfirmed
''',
    '''                forcePro = exitFusion.emergency &&
                    snapshot.rapidDrop.active && !snapshot.rapidDrop.recoveryConfirmed
'''
)
replace_once(
    path,
    '''        } else if (fusionPriority.active) {
            "Fusion под локальным контролем; DeepSig проверяет ключевые изменения: ${json.optString("summary", "позиция удерживается")}"
        } else {
            json.optString("summary", "DeepSig не дал пояснение")
        }
''',
    '''        } else {
            json.optString("summary", "DeepSig не дал пояснение")
        }
'''
)
replace_once(
    path,
    '''            Если fusion_priority_position.maximum_control_active=true, перепроверь именно цену входа Fusion,
            свежий bid/ask, спред, комиссию, чистый PnL и откат от пика. Два процента снижения сами по себе не
            являются EXIT. При одобренном EXIT приложение закроет только открытые виртуальные позиции; позиция
            Сержа и его кнопки полностью отделены.
''',
    '''            fusion_priority_position используй только как дополнительный независимый контекст рынка.
            Открытая FusionSim-позиция не является позицией DeepSig и сама по себе не может одобрить, запретить
            или перенаправить BUY/EXIT DeepSig. Перепроверяй именно гипотезу и собственную позицию DeepSig.
            Позиция Сержа, APP и локальная FusionSim полностью отделены.
'''
)

# ---------------------------------------------------------------------------
# 2) Give a still-conservative DeepSig lane one strong local continuation path.
#    It still goes through the independent trade verifier.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/example/pumppaperbot/AppLedHybridPolicy.kt"
replace_once(
    path,
    '''        val moderateWatchPromotion = evidence.aiFresh &&
            evidence.aiAction in setOf("WATCH", "HOLD") &&
            evidence.aiReadiness >= 8 && evidence.aiDirection >= 50 && evidence.aiConfidence >= 65 &&
            localEntryConfirmed && mediumSupport
        val effectiveAiAction = if (moderateWatchPromotion) {
            "BUY"
''',
    '''        val strongLocalContinuation = evidence.aiFresh &&
            evidence.aiAction in setOf("WATCH", "HOLD") &&
            evidence.aiReadiness >= 6 && evidence.aiDirection >= 25 && evidence.aiConfidence >= 60 &&
            microBuyers && five >= 30 && fifteen >= 18 && thirty >= 5
        val moderateWatchPromotion = evidence.aiFresh &&
            evidence.aiAction in setOf("WATCH", "HOLD") &&
            evidence.aiReadiness >= 8 && evidence.aiDirection >= 50 && evidence.aiConfidence >= 65 &&
            localEntryConfirmed && mediumSupport
        val effectiveAiAction = if (moderateWatchPromotion || strongLocalContinuation) {
            "BUY"
'''
)
replace_once(
    path,
    '''        val promotedDeepSeekEntry = evidence.aiAction in setOf("WATCH", "HOLD") &&
            evidence.aiReadiness >= 8 && evidence.aiDirection >= 50 && evidence.aiConfidence >= 65
        val independentDeepSeekSetup = evidence.aiFresh && effectiveAiAction == "BUY" &&
            (explicitDeepSeekEntry || promotedDeepSeekEntry) &&
''',
    '''        val promotedDeepSeekEntry = evidence.aiAction in setOf("WATCH", "HOLD") &&
            evidence.aiReadiness >= 8 && evidence.aiDirection >= 50 && evidence.aiConfidence >= 65
        val independentDeepSeekSetup = evidence.aiFresh && effectiveAiAction == "BUY" &&
            (explicitDeepSeekEntry || promotedDeepSeekEntry || strongLocalContinuation) &&
'''
)
replace_once(
    path,
    '''            independentDeepSeekSetup && moderateWatchPromotion ->
                "DeepSig дал сильный 8/10 WATCH/HOLD, 5–15 минут подтвердили покупателей, а 30 минут не разваливается; запускается проверка BUY."
            independentDeepSeekSetup ->
''',
    '''            independentDeepSeekSetup && strongLocalContinuation ->
                "DeepSig осторожен, но сильный 5/15/30-минутный поток и свежие покупатели подтвердили продолжение; запускается независимая проверка BUY."
            independentDeepSeekSetup && moderateWatchPromotion ->
                "DeepSig дал сильный 8/10 WATCH/HOLD, 5–15 минут подтвердили покупателей, а 30 минут не разваливается; запускается проверка BUY."
            independentDeepSeekSetup ->
'''
)

# ---------------------------------------------------------------------------
# 3) Fusion: do not arm the tight 1% profit-defense from a one-minute/5m dip
#    while 15/20-minute flow remains strongly positive. Structural 1.75% trail
#    and the full system exit stay unchanged.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/example/pumppaperbot/FusionSim.kt"
replace_once(
    path,
    '''    val deteriorationSignal: Boolean get() {
        val mediumNegative = listOf(score5m, score15m, score20m).count { it <= -2 } >= 2
        val fastBreak = instant <= -6 && score5m <= -2
        return mediumNegative || fastBreak
    }
''',
    '''    val deteriorationSignal: Boolean get() {
        val mediumNegative = listOf(score5m, score15m, score20m).count { it <= -8 } >= 2
        val mediumCoreWeak = score15m <= -8 || score20m <= -8
        return mediumNegative && mediumCoreWeak
    }
'''
)

# ---------------------------------------------------------------------------
# 4) DeepSigX: profit-defense needs actual retained profit and 20m/slow support
#    for the reversal. Emergency and strong reversal logic are untouched.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/example/pumppaperbot/GeminiExitExperiment.kt"
replace_once(
    path,
    '''        val profitProtection = peakReturnPercent >= 1.60 &&
            evidence.pullbackPercent >= maxOf(0.90, evidence.adaptivePullbackPercent) &&
            mediumWeakCount >= 2 && sellerConfirmation
''',
    '''        val profitProtection = peakReturnPercent >= 1.60 &&
            evidence.currentReturnPercent >= 0.75 &&
            evidence.pullbackPercent >= maxOf(0.90, evidence.adaptivePullbackPercent) &&
            mediumTurningWeak && (twenty <= -8 || slowContextWeak) && sellerConfirmation
'''
)

# ---------------------------------------------------------------------------
# 5) Main screen: move the V5.18 money-flow strip directly above the chart,
#    below the speed / x2 overview row, exactly where requested.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/example/pumppaperbot/V513Application.kt"
replace_once(
    path,
    '''    private fun installMoneyFlowStrip(activity: MainActivity) {
        val root = activity.findViewById<View>(R.id.tvLatestSignal)?.parent as? LinearLayout ?: return
        if (root.findViewWithTag<MoneyFlowStripView>(MoneyFlowStripView.VIEW_TAG) != null) return
        val anchor = activity.findViewById<View>(R.id.tvLatestSignal) ?: return
        val index = root.indexOfChild(anchor)
        if (index < 0) return
        val strip = MoneyFlowStripView(activity).apply {
            minimumHeight = dp(activity, 104)
            contentDescription = "Денежный поток за одну, пять и пятнадцать минут"
        }
        root.addView(
            strip,
            index,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(activity, 104)
            ).apply { topMargin = dp(activity, 7) }
        )
    }
''',
    '''    private fun installMoneyFlowStrip(activity: MainActivity) {
        val chart = activity.findViewById<StrategyChartView>(R.id.chart) ?: return
        val root = chart.parent as? LinearLayout ?: return
        if (root.findViewWithTag<MoneyFlowStripView>(MoneyFlowStripView.VIEW_TAG) != null) return
        val index = root.indexOfChild(chart)
        if (index < 0) return
        val strip = MoneyFlowStripView(activity).apply {
            minimumHeight = dp(activity, 104)
            contentDescription = "Денежный поток за одну, пять и пятнадцать минут"
        }
        root.addView(
            strip,
            index,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(activity, 104)
            ).apply {
                topMargin = dp(activity, 8)
                bottomMargin = dp(activity, 2)
            }
        )
    }
'''
)

# ---------------------------------------------------------------------------
# 6) Regression tests for the exact overnight failure modes.
# ---------------------------------------------------------------------------
path = "app/src/test/java/com/example/pumppaperbot/FusionAntiChurnPolicyTest.kt"
replace_once(
    path,
    '''        val deterioration = FusionFlowFrame(
            instant = -7,
            score5m = -3,
            score15m = -2,
            score20m = 1,
            score30m = 4
        )
''',
    '''        val deterioration = FusionFlowFrame(
            instant = -18,
            score5m = -14,
            score15m = -12,
            score20m = -10,
            score30m = 4
        )
'''
)
insert_anchor = '''    @Test
    fun entryNeedsTwoObservationsAndAtLeastSixtySecondsEvenWhenStrong() {
'''
insert_test = '''    @Test
    fun shortDipDoesNotArmProfitDefenseWhileFifteenAndTwentyMinutesStayPositive() {
        val overnightLikeDip = FusionFlowFrame(
            instant = -37,
            score5m = -57,
            score15m = 22,
            score20m = 26,
            score30m = 37
        )
        val decision = FusionStabilityPolicy.evaluate(
            inPosition = true,
            entryPrice = 100.0,
            previous = FusionStabilityState(peakBid = 104.0),
            frame = overnightLikeDip,
            bid = 103.0,
            feeRate = 0.0025,
            now = 3_500_000L,
            positionAgeMillis = 30L * 60L * 1000L
        )

        assertNull(decision.action)
        assertFalse(decision.nextState.profitDefenseArmed)
        assertTrue(decision.activeStopPrice < 103.0)
    }

'''
replace_once(path, insert_anchor, insert_test + insert_anchor)

path = "app/src/test/java/com/example/pumppaperbot/GeminiExitExperimentTest.kt"
insert_anchor = '''    @Test fun `one isolated indicator cannot close the experiment`() {
'''
insert_test = '''    @Test fun `profit protection ignores shallow fast fade while 20 30 60 stay supportive`() {
        val result = GeminiExitExperimentEngine.evaluate(
            GeminiExitExperimentState(
                initializedAt = 1L,
                portfolio = bought().copy(positionPeakPrice = 1.0182)
            ),
            evidence(
                score = 3,
                groups = 2,
                spotWeak = false,
                futuresWeak = true,
                cvdWeak = false,
                currentReturn = 0.36,
                pullback = 1.43,
                microWeak = false,
                breathing5m = -13,
                breathing15m = -11,
                breathing20m = 0,
                breathing30m = 43,
                breathing60m = 29
            ),
            price = 1.0036,
            decisionId = 11L,
            now = 700_000L
        )

        assertTrue(result.state.portfolio.inPosition)
        assertEquals(null, result.executedTrade)
        assertFalse(result.state.lastReason.contains("ЗАЩИТА ПРИБЫЛИ"))
    }

'''
replace_once(path, insert_anchor, insert_test + insert_anchor)

path = "app/src/test/java/com/example/pumppaperbot/AppLedHybridPolicyTest.kt"
insert_anchor = '''    @Test fun `V517 medium background breakdown still blocks promoted DeepSig entry`() {
'''
insert_test = '''    @Test fun `V519 strong local continuation can wake cautious DeepSig before eight of ten`() {
        val result = AppLedHybridPolicy.entry(entryEvidence(
            aiAction = "HOLD",
            aiDirection = 30,
            aiConfidence = 62,
            aiReadiness = 6,
            appReadiness = 10,
            breathing5m = 34,
            breathing15m = 22,
            breathing30m = 8
        ))

        assertTrue(result.independentDeepSeekSetup)
        assertTrue(result.level >= 8)
        assertTrue(result.reason.contains("5/15/30"))
    }

'''
replace_once(path, insert_anchor, insert_test + insert_anchor)

# ---------------------------------------------------------------------------
# 7) Version and CI metadata.
# ---------------------------------------------------------------------------
replace_once(
    "app/build.gradle",
    '''        versionCode 98
        versionName "5.18"
''',
    '''        versionCode 99
        versionName "5.19"
'''
)

path = ".github/workflows/android.yml"
replace_once(
    path,
    '"chatgpt/v5-17-deepsig-entry-exit", "chatgpt/v5-18-ci-base" ]',
    '"chatgpt/v5-17-deepsig-entry-exit", "chatgpt/v5-18-ci-base", "chatgpt/v5-18-money-flow-ui" ]'
)
replace_once(path, "Validate and build V5.18 money-flow UI debug APK", "Validate and build V5.19 DeepSig independence debug APK")
replace_once(
    path,
    "package: name='com.example.pumppaperbot.v8' versionCode='98' versionName='5.18'",
    "package: name='com.example.pumppaperbot.v8' versionCode='99' versionName='5.19'"
)
replace_once(path, "PumpSignal-V5.18-Money-Flow-UI.apk", "PumpSignal-V5.19-DeepSig-Exit-Tuning.apk")
replace_once(path, "PumpSignal-V5.18-Money-Flow-UI-APK", "PumpSignal-V5.19-DeepSig-Exit-Tuning-APK")

print("V5.19 patch applied successfully")
