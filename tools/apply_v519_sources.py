from pathlib import Path

source = Path("tools/apply_v519_patch.py").read_text(encoding="utf-8")
source = source.replace(
    'replace_all_checked(path, "managedVirtualPositionOpen", "deepSigPositionOpen", 8)',
    'replace_all_checked(path, "managedVirtualPositionOpen", "deepSigPositionOpen", 15)'
)
marker = "# ---------------------------------------------------------------------------\n# 7) Version and CI metadata."
if marker not in source:
    raise SystemExit("V5.19 source patch marker not found")
source_only = source.split(marker, 1)[0]
exec(compile(source_only, "tools/apply_v519_patch.py", "exec"), {})

fast_extension = Path("tools/apply_v519_fast_extension.py").read_text(encoding="utf-8")
exec(compile(fast_extension, "tools/apply_v519_fast_extension.py", "exec"), {})

contract_fixes = Path("tools/apply_v519_contract_fixes.py").read_text(encoding="utf-8")
exec(compile(contract_fixes, "tools/apply_v519_contract_fixes.py", "exec"), {})
print("V5.19 source + fast shock + alert contract patches applied")
