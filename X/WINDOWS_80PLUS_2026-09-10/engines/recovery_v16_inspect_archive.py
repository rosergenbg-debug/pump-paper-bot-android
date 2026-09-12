"""Recover compiled project modules without importing or executing them."""
import dis
import hashlib
import importlib.util
import io
import json
import marshal
from pathlib import Path
from PyInstaller.archive.readers import ZlibArchiveReader

ROOT = Path(__file__).resolve().parent
SOURCE = Path(r"F:\PUMP Research Lab\build-v16\PumpResearchLab-V16.0.0\PYZ-00.pyz")
archive = ZlibArchiveReader(str(SOURCE))
records = []
for name in sorted(archive.toc):
    if not name.startswith("pump_research_lab"):
        continue
    code = archive.extract(name)
    is_package = archive.toc[name][0] == 1
    relative = Path(*name.split("."))
    target = ROOT / "compiled" / (relative / "__init__.pyc" if is_package else relative.with_suffix(".pyc"))
    target.parent.mkdir(parents=True, exist_ok=True)
    payload = importlib.util.MAGIC_NUMBER + bytes(12) + marshal.dumps(code)
    if target.exists() and target.read_bytes() != payload:
        raise RuntimeError(f"Refusing to overwrite differing recovered module: {target}")
    target.write_bytes(payload)
    listing = io.StringIO()
    dis.dis(code, file=listing)
    listing_path = ROOT / "disassembly" / (name + ".txt")
    listing_path.parent.mkdir(parents=True, exist_ok=True)
    listing_path.write_text(listing.getvalue(), encoding="utf-8")
    records.append({"module": name, "original_source": code.co_filename,
                    "compiled_sha256": hashlib.sha256(payload).hexdigest(),
                    "compiled_bytes": len(payload), "package": is_package})
manifest = {"status": "COMPILED_RECOVERY_ONLY_NOT_SOURCE_NOT_V17",
            "archive": str(SOURCE), "archive_sha256": hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
            "modules": records}
(ROOT / "manifest.json").write_text(json.dumps(manifest, indent=2, ensure_ascii=False), encoding="utf-8")
print(json.dumps({"recovered_modules": len(records), "executed_modules": 0,
                  "manifest": str(ROOT / "manifest.json")}, ensure_ascii=False))
