from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}: {old[:120]!r}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


def insert_before_once(path: str, anchor: str, insertion: str) -> None:
    replace_once(path, anchor, insertion + anchor)


# ---------------------------------------------------------------------------
# 1) Money-flow history must survive WebSocket reconnects and short process
#    restarts. Readiness is based on real contiguous minute coverage, not on
#    time since the most recent socket onOpen(). Trading ratios stay unchanged.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/example/pumppaperbot/MicroImpulseStream.kt"
replace_once(
    path,
    "import org.json.JSONObject\n",
    "import org.json.JSONArray\nimport org.json.JSONObject\n"
)
replace_once(
    path,
    '''    // V5.18 keeps only 16 tiny minute buckets for the new 15m money-mass display.\n    private val moneyMinutes = ArrayDeque<MoneyMinuteBucket>()\n''',
    '''    // V5.20 restores the tiny minute buckets after reconnect/process restart.\n    // This is display/order-flow aggregate state only; no raw trade tape is persisted.\n    private val moneyMinutes = ArrayDeque<MoneyMinuteBucket>().apply {\n        addAll(MoneyFlowHistoryStore.load(appContext))\n    }\n'''
)
replace_once(
    path,
    '''        val fifteenMinuteKey = now / 60_000L - 14L\n        val fifteenMinuteBuckets = moneyMinutes.filter { it.minuteKey >= fifteenMinuteKey }\n''',
    '''        val currentMinuteKey = now / 60_000L\n        val fiveMinuteKey = currentMinuteKey - 4L\n        val fifteenMinuteKey = currentMinuteKey - 14L\n        val fiveMinuteBuckets = moneyMinutes.filter { it.minuteKey >= fiveMinuteKey }\n        val fifteenMinuteBuckets = moneyMinutes.filter { it.minuteKey >= fifteenMinuteKey }\n'''
)
replace_once(
    path,
    '''        val buy15m = fifteenMinuteBuckets.sumOf { it.buyUsdt }\n        val sell15m = fifteenMinuteBuckets.sumOf { it.sellUsdt }\n        val turnover60 = buy60 + sell60\n        val turnover5m = buy5m + sell5m\n        val moneyActivityRatio = if (connectedAt > 0L && now - connectedAt >= 4L * 60L * 1000L && turnover5m > 0.0) {\n            turnover60 / (turnover5m / 5.0)\n        } else null\n''',
    '''        val moneyBuy5m = fiveMinuteBuckets.sumOf { it.buyUsdt }\n        val moneySell5m = fiveMinuteBuckets.sumOf { it.sellUsdt }\n        val buy15m = fifteenMinuteBuckets.sumOf { it.buyUsdt }\n        val sell15m = fifteenMinuteBuckets.sumOf { it.sellUsdt }\n        val moneyCoverageSeconds = MoneyFlowCoveragePolicy.continuousSeconds(\n            moneyMinutes.map { it.minuteKey },\n            now\n        )\n        val turnover60 = buy60 + sell60\n        val turnover5m = moneyBuy5m + moneySell5m\n        val moneyActivityRatio = if (moneyCoverageSeconds >= 4L * 60L && turnover5m > 0.0) {\n            turnover60 / (turnover5m / 5.0)\n        } else null\n'''
)
replace_once(
    path,
    '''        val flowHistorySeconds = if (connectedAt > 0L) {\n            ((now - connectedAt).coerceAtLeast(0L) / 1_000L).coerceAtMost(15L * 60L)\n        } else 0L\n''',
    '''        val flowHistorySeconds = moneyCoverageSeconds\n'''
)
replace_once(
    path,
    '''            buyNotional5m = buy5m,\n            sellNotional5m = sell5m,\n''',
    '''            buyNotional5m = moneyBuy5m,\n            sellNotional5m = moneySell5m,\n'''
)
replace_once(
    path,
    '''        MicroImpulseStore.save(appContext, snapshot)\n        LiveMarketBreathingStore.append(appContext, snapshot)\n''',
    '''        MoneyFlowHistoryStore.save(appContext, moneyMinutes, now)\n        MicroImpulseStore.save(appContext, snapshot)\n        LiveMarketBreathingStore.append(appContext, snapshot)\n'''
)
insert_before_once(
    path,
    '''data class MicroTrade(\n''',
    '''object MoneyFlowCoveragePolicy {\n    fun continuousSeconds(minuteKeys: Collection<Long>, now: Long): Long {\n        if (minuteKeys.isEmpty()) return 0L\n        val keys = minuteKeys.toHashSet()\n        val current = now / 60_000L\n        val newest = when {\n            keys.contains(current) -> current\n            keys.contains(current - 1L) -> current - 1L\n            else -> return 0L\n        }\n        var oldest = newest\n        while (newest - oldest < 15L && keys.contains(oldest - 1L)) oldest--\n        val tailSeconds = if (newest == current) {\n            ((now % 60_000L) / 1_000L).coerceIn(0L, 59L)\n        } else 60L\n        return (((newest - oldest) * 60L) + tailSeconds).coerceIn(0L, 15L * 60L)\n    }\n}\n\nprivate object MoneyFlowHistoryStore {\n    private const val PREFS = "money_flow_history_v520"\n    private const val KEY = "minute_buckets"\n\n    fun load(context: Context, now: Long = System.currentTimeMillis()): List<MoneyMinuteBucket> {\n        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()\n        return runCatching {\n            val array = JSONArray(raw)\n            val cutoff = now / 60_000L - 15L\n            buildList {\n                for (index in 0 until array.length()) {\n                    val item = array.optJSONObject(index) ?: continue\n                    val key = item.optLong("m", Long.MIN_VALUE)\n                    if (key < cutoff) continue\n                    add(\n                        MoneyMinuteBucket(\n                            minuteKey = key,\n                            buyUsdt = item.optDouble("b", 0.0).coerceAtLeast(0.0),\n                            sellUsdt = item.optDouble("s", 0.0).coerceAtLeast(0.0)\n                        )\n                    )\n                }\n            }.sortedBy { it.minuteKey }.takeLast(16)\n        }.getOrDefault(emptyList())\n    }\n\n    fun save(context: Context, buckets: Collection<MoneyMinuteBucket>, now: Long) {\n        val cutoff = now / 60_000L - 15L\n        val array = JSONArray()\n        buckets.asSequence()\n            .filter { it.minuteKey >= cutoff }\n            .sortedBy { it.minuteKey }\n            .takeLast(16)\n            .forEach { bucket ->\n                array.put(\n                    JSONObject()\n                        .put("m", bucket.minuteKey)\n                        .put("b", bucket.buyUsdt)\n                        .put("s", bucket.sellUsdt)\n                )\n            }\n        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()\n            .putString(KEY, array.toString())\n            .apply()\n    }\n}\n\n'''
)


# ---------------------------------------------------------------------------
# 2) Make the monitor explicitly independent from the UI task and restore the
#    user-facing battery exemption prompt. Android can still stop a force-stopped
#    app, but switching to YouTube / another app should not stop this service.
# ---------------------------------------------------------------------------
path = "app/src/main/AndroidManifest.xml"
replace_once(
    path,
    '''    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />\n''',
    '''    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />\n    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />\n'''
)
replace_once(
    path,
    '''        <service\n            android:name=".PumpSignalService"\n            android:exported="false"\n            android:foregroundServiceType="dataSync" />\n''',
    '''        <service\n            android:name=".PumpSignalService"\n            android:exported="false"\n            android:stopWithTask="false"\n            android:foregroundServiceType="dataSync" />\n'''
)

path = "app/src/main/java/com/example/pumppaperbot/MainActivity.kt"
replace_once(path, "import android.graphics.Color\n", "import android.graphics.Color\nimport android.net.Uri\n")
replace_once(path, "import android.os.Looper\n", "import android.os.Looper\nimport android.os.PowerManager\n")
replace_once(path, "import android.view.View\n", "import android.provider.Settings\nimport android.view.View\n")
replace_once(
    path,
    '''    private var evidenceMemoryDialogVisible = false\n''',
    '''    private var evidenceMemoryDialogVisible = false\n    private var backgroundPersistencePromptVisible = false\n'''
)
replace_once(
    path,
    '''        if (PumpBotEngine.snapshot(this).running) {\n            ContextCompat.startForegroundService(this, Intent(this, PumpSignalService::class.java))\n            schedulePeriodicMonitor()\n        }\n''',
    '''        if (PumpBotEngine.snapshot(this).running) {\n            ContextCompat.startForegroundService(this, Intent(this, PumpSignalService::class.java))\n            schedulePeriodicMonitor()\n            handler.postDelayed({ maybeEnsureBackgroundPersistence() }, 1200L)\n        }\n'''
)
replace_once(
    path,
    '''        schedulePeriodicMonitor()\n        updateUi()\n    }\n''',
    '''        schedulePeriodicMonitor()\n        handler.postDelayed({ maybeEnsureBackgroundPersistence() }, 500L)\n        updateUi()\n    }\n'''
)
insert_before_once(
    path,
    '''    private fun requestNotificationPermission() {\n''',
    '''    private fun maybeEnsureBackgroundPersistence() {\n        if (backgroundPersistencePromptVisible || !PumpBotEngine.snapshot(this).running) return\n        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return\n        val power = getSystemService(PowerManager::class.java)\n        if (power.isIgnoringBatteryOptimizations(packageName)) return\n\n        backgroundPersistencePromptVisible = true\n        AlertDialog.Builder(this)\n            .setTitle("PUMP • РАБОТА В ФОНЕ")\n            .setMessage(\n                "Чтобы поток 1/5/15 минут, Fusion и предупреждения продолжали работать, когда открыт YouTube или другое приложение, " +\n                    "разрешите PUMP работать без оптимизации батареи. Постоянное уведомление монитора останется в шторке, пока монитор включён."\n            )\n            .setPositiveButton("РАЗРЕШИТЬ ВСЕГДА") { _, _ ->\n                backgroundPersistencePromptVisible = false\n                requestBatteryOptimizationExemption()\n            }\n            .setNegativeButton("ПОЗЖЕ") { _, _ -> backgroundPersistencePromptVisible = false }\n            .setOnCancelListener { backgroundPersistencePromptVisible = false }\n            .show()\n    }\n\n    private fun requestBatteryOptimizationExemption() {\n        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return\n        val direct = Intent(\n            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,\n            Uri.parse("package:$packageName")\n        )\n        runCatching { startActivity(direct) }\n            .onFailure {\n                runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }\n            }\n    }\n\n'''
)

path = "app/src/main/java/com/example/pumppaperbot/PumpSignalService.kt"
insert_before_once(
    path,
    '''    override fun onDestroy() {\n''',
    '''    override fun onTaskRemoved(rootIntent: Intent?) {\n        // android:stopWithTask=false + START_STICKY: removing the UI task must not be\n        // interpreted as turning the monitor off. A real user Stop still calls stopService().\n        GeminiPaperStore.recordActivity(\n            this,\n            "ФОН",\n            "HOLD",\n            "Окно приложения закрыто/смахнуто; foreground-монитор продолжает работу"\n        )\n        super.onTaskRemoved(rootIntent)\n    }\n\n'''
)


# ---------------------------------------------------------------------------
# 3) Real sequential version. Trading logic is intentionally untouched.
# ---------------------------------------------------------------------------
path = "app/build.gradle"
replace_once(
    path,
    '''        versionCode 99\n        versionName "5.19"\n''',
    '''        versionCode 100\n        versionName "5.20"\n'''
)

print("V5.20 background persistence patch applied")
