from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}: {old[:160]!r}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


# V5.23 originally introduced a clean A/B lab start. On the V5.24 continuation branch
# the reset implementation is retained for source compatibility/audit history, but it is
# deliberately NOT invoked. V5.24 must preserve every accumulated Fusion/PM3 state even
# if an old marker is missing or damaged; only the brand-new PM2 store starts fresh.
path = "app/src/main/java/com/example/pumppaperbot/V513Application.kt"

with Path(path).open("a", encoding="utf-8") as f:
    f.write('''\n\n/** V5.23 one-shot reset implementation retained but intentionally dormant in V5.24. */\ninternal object CleanFusionLabResetV523 {\n    private const val MARKER_PREFS = "v523_clean_fusion_lab_reset"\n    private const val DONE = "done"\n    private const val PUMP_MACHINE_PREFS = "pump_machine_paper_v521"\n\n    @Synchronized\n    fun ensure(context: android.content.Context) {\n        val marker = context.getSharedPreferences(MARKER_PREFS, android.content.Context.MODE_PRIVATE)\n        if (marker.getBoolean(DONE, false)) return\n\n        FusionSimStore.reset(context)\n        context.getSharedPreferences(PUMP_MACHINE_PREFS, android.content.Context.MODE_PRIVATE)\n            .edit().clear().commit()\n\n        marker.edit().putBoolean(DONE, true).commit()\n        UnifiedResearchLog.record(\n            context,\n            "V523_LAB",\n            "START",\n            "Fusion и Pump Machine сброшены один раз: €1000 / 0 сделок / чистые entry-cooldown состояния"\n        )\n    }\n}\n''')

path = "app/build.gradle"
replace_once(
    path,
    '''        versionCode 102
        versionName "5.22"
''',
    '''        versionCode 103
        versionName "5.23"
'''
)

print("V5.23 compatibility layer applied with reset invocation disabled for V5.24")
