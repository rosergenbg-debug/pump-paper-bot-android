from pathlib import Path

source = Path("tools/apply_v519_patch.py").read_text(encoding="utf-8")
marker = "# ---------------------------------------------------------------------------\n# 7) Version and CI metadata."
if marker not in source:
    raise SystemExit("V5.19 source patch marker not found")
source_only = source.split(marker, 1)[0]
exec(compile(source_only, "tools/apply_v519_patch.py", "exec"), {})
print("V5.19 source-only patch applied")
