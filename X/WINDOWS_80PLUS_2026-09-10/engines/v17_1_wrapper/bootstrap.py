"""Load the unmodified, hash-verified V16 application, including its complete UI."""
import hashlib
import importlib
import json
from pathlib import Path
import sys


def load_original():
    if sys.version_info[:2] != (3, 13):
        raise RuntimeError('Для восстановленной V16 требуется Python 3.13.')
    base = Path(sys._MEIPASS) if getattr(sys, 'frozen', False) else Path(__file__).resolve().parent.parent
    recovery = base / 'recovery_v16'
    compiled = recovery / 'compiled'
    manifest = json.loads((recovery / 'manifest.json').read_text(encoding='utf-8'))
    for item in manifest['modules']:
        relative = Path(*item['module'].split('.'))
        relative = relative / '__init__.pyc' if item['package'] else relative.with_suffix('.pyc')
        if hashlib.sha256((compiled / relative).read_bytes()).hexdigest() != item['compiled_sha256']:
            raise RuntimeError('Повреждён модуль V16: ' + item['module'])
    for name, module in tuple(sys.modules.items()):
        if name == 'pump_research_lab' or name.startswith('pump_research_lab.'):
            if not Path(module.__file__).resolve().is_relative_to(compiled.resolve()):
                raise RuntimeError('Смешаны разные версии движка.')
    if str(compiled) not in sys.path:
        sys.path.insert(0, str(compiled))
    return importlib.import_module('pump_research_lab.gui_v12')
