from pathlib import Path

path = Path("app/src/main/java/com/example/pumppaperbot/MicroImpulseStream.kt")
text = path.read_text(encoding="utf-8")
old = '''        buckets.asSequence()\n            .filter { it.minuteKey >= cutoff }\n            .sortedBy { it.minuteKey }\n            .takeLast(16)\n            .forEach { bucket ->\n'''
new = '''        buckets.asSequence()\n            .filter { it.minuteKey >= cutoff }\n            .sortedBy { it.minuteKey }\n            .toList()\n            .takeLast(16)\n            .forEach { bucket ->\n'''
if text.count(old) != 1:
    raise SystemExit("V5.20 compile-fix anchor not found exactly once")
path.write_text(text.replace(old, new, 1), encoding="utf-8")
print("V5.20 compile fix applied")
