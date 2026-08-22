from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}: {old[:120]!r}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


# Preserve the old immediate 9 -> 10 critical escalation as well as the new 7 -> 9 path.
replace_once(
    "app/src/main/java/com/example/pumppaperbot/PositionSupervisor.kt",
    '''        if (dangerLevel >= CRITICAL_LEVEL && notifiedDanger < CRITICAL_LEVEL) return true
        if (firstExit) return true
''',
    '''        if (dangerLevel >= 10 && notifiedDanger < 10) return true
        if (dangerLevel >= CRITICAL_LEVEL && notifiedDanger < CRITICAL_LEVEL) return true
        if (firstExit) return true
'''
)

# V5.19 explicitly suppresses all virtual-agent trade sounds while Serge owns a position.
replace_once(
    "app/src/test/java/com/example/pumppaperbot/DeepSeekActionLevelPolicyTest.kt",
    '''    @Test fun `executed exits always notify while user is in position`() {
        assertTrue(VirtualTradeAlertPolicy.shouldNotify("SELL", userPositionOpen = true))
        assertTrue(VirtualTradeAlertPolicy.shouldNotify(StrategyV2.ACTION_SELL_HALF, userPositionOpen = true))
        assertTrue(VirtualTradeAlertPolicy.shouldNotify("BUY", userPositionOpen = true))
        assertTrue(VirtualTradeAlertPolicy.shouldNotify("BUY", userPositionOpen = false))
    }
''',
    '''    @Test fun `virtual agent trade sounds are muted while Serge is in position`() {
        assertFalse(VirtualTradeAlertPolicy.shouldNotify("SELL", userPositionOpen = true))
        assertFalse(VirtualTradeAlertPolicy.shouldNotify(StrategyV2.ACTION_SELL_HALF, userPositionOpen = true))
        assertFalse(VirtualTradeAlertPolicy.shouldNotify("BUY", userPositionOpen = true))
        assertTrue(VirtualTradeAlertPolicy.shouldNotify("BUY", userPositionOpen = false))
    }
'''
)

print("V5.19 alert contract fixes applied")
