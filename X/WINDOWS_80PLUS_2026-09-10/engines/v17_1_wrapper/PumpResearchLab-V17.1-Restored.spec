# -*- mode: python ; coding: utf-8 -*-
import dis
import marshal
from pathlib import Path
import sys
import types

# PyInstaller cannot analyze the preserved sourceless modules. Include every
# standard-library import observed in their bytecode instead of guessing a list.
runtime_imports = {'duckdb'}
pending = [marshal.loads(p.read_bytes()[16:]) for p in Path('../recovery_v16/compiled').rglob('*.pyc')]
while pending:
    code = pending.pop()
    for instruction in dis.get_instructions(code):
        if instruction.opname == 'IMPORT_NAME' and instruction.argval.split('.')[0] in sys.stdlib_module_names:
            runtime_imports.add(instruction.argval)
    pending.extend(c for c in code.co_consts if isinstance(c, types.CodeType))


a = Analysis(
    ['launcher.py'],
    pathex=[],
    binaries=[],
    datas=[('..\\recovery_v16\\compiled', 'recovery_v16\\compiled'), ('..\\recovery_v16\\manifest.json', 'recovery_v16')],
    hiddenimports=sorted(runtime_imports),
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=['pump_research_lab'],
    noarchive=False,
    optimize=0,
)
pyz = PYZ(a.pure)

exe = EXE(
    pyz,
    a.scripts,
    [],
    exclude_binaries=True,
    name='PumpResearchLab-V17.1-Restored',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    console=False,
    disable_windowed_traceback=False,
    argv_emulation=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
)
coll = COLLECT(
    exe,
    a.binaries,
    a.datas,
    strip=False,
    upx=True,
    upx_exclude=[],
    name='PumpResearchLab-V17.1-Restored',
)
