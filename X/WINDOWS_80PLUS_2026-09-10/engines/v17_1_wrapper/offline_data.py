"""Use existing synchronized contexts only. No network client, no data writes."""
from datetime import datetime
import hashlib
import json
from pathlib import Path
import time

SYMBOLS = ('PUMPUSDT', 'BTCUSDT', 'SOLUSDT')


def select_existing(root, months, names):
    from pump_research_lab.storage_v13 import relocated
    result = {}
    root = Path(root)
    for path in sorted((root/'data'/'prepared').glob('*.json')):
        try:
            value = json.loads(path.read_text(encoding='utf-8'))
        except (OSError, ValueError):
            continue
        if not isinstance(value, dict):
            continue
        name = value.get('market_regime', 'RECENT')
        if name not in names or value.get('months') != months:
            continue
        value = relocated(value, root)
        if all(Path(value.get('datasets', {}).get(s, {}).get('raw_path','')).is_file()
               and Path(value.get('datasets', {}).get(s, {}).get('manifest_path','')).is_file() for s in SYMBOLS):
            # Keep the original period/engine identity. Age is not a reason to redownload history.
            value.update(selection_path=str(path), reused=True, offline=True)
            if name in result:
                raise ValueError(f'Несколько комплектов {name} за {months} мес.: требуется явный выбор, без скрытой подмены.')
            result[name] = value
    return {name:result[name] for name in names if name in result}


def signature(prepared):
    values = []
    for name, bundle in sorted(prepared.items()):
        values.append((name, bundle['period']))
        for symbol in SYMBOLS:
            record = bundle['datasets'][symbol]
            for field in ('raw_path','manifest_path'):
                path = Path(record[field]); stat = path.stat()
                values.append((symbol, field, str(path), stat.st_size, stat.st_mtime_ns))
            values.append(record.get('checksum_sha256'))
    return hashlib.sha256(json.dumps(values,sort_keys=True).encode()).hexdigest()


def verify_existing(root, months, names, emit=lambda v:None, cancel=lambda:False, pause=lambda:False):
    from pump_research_lab.optimizer import ResearchCancelled
    selected = select_existing(root, months, names)
    if set(selected)!=set(names) or not names:
        raise ValueError('Нет готовых локальных комплектов: '+', '.join(n for n in names if n not in selected)+
                         '. Загрузка отключена. Выберите доступный период/рынки (все три есть за 6 месяцев).')
    before = signature(selected)
    total = sum(Path(b['datasets'][s]['raw_path']).stat().st_size for b in selected.values() for s in SYMBOLS)
    done = 0
    for name, bundle in selected.items():
        period = bundle['period']
        start, end = int(period['start_ms']), int(period['end_ms'])
        expected = (end-start)//60000+1
        if start%60000 or end%60000 or end < start:
            raise ValueError(f'{name}: неверные минутные границы периода')
        for symbol in SYMBOLS:
            record = bundle['datasets'][symbol]
            manifest = json.loads(Path(record['manifest_path']).read_text(encoding='utf-8'))
            if manifest.get('symbol') != symbol or manifest.get('interval') != '1m':
                raise ValueError(f'{name}/{symbol}: неверный символ/интервал')
            first = round(datetime.fromisoformat(manifest['start_utc'].replace('Z','+00:00')).timestamp()*1000)
            last = round(datetime.fromisoformat(manifest['end_utc'].replace('Z','+00:00')).timestamp()*1000)
            if first != start or not end <= last <= end+59999:
                raise ValueError(f'{name}/{symbol}: даты контекста не совпадают с PUMP')
            if manifest.get('row_count')!=expected or record.get('row_count')!=expected or manifest.get('gaps') or manifest.get('duplicates'):
                raise ValueError(f'{name}/{symbol}: пропуски, дубликаты или неверное число свечей')
            h = hashlib.sha256()
            with Path(record['raw_path']).open('rb') as file:
                while block := file.read(4*1024*1024):
                    if cancel():
                        raise ResearchCancelled('Проверка локальных баз остановлена; файлы сохранены')
                    while pause():
                        if cancel():
                            raise ResearchCancelled('Проверка локальных баз остановлена; файлы сохранены')
                        time.sleep(.1)
                    h.update(block); done += len(block)
                    emit(dict(stage=f'Проверка на диске · {name} / {symbol} · скачивание отключено', percent=100*done/max(total,1)))
            if h.hexdigest()!=manifest.get('checksum_sha256') or h.hexdigest()!=record.get('checksum_sha256'):
                raise ValueError(f'{name}/{symbol}: контрольная сумма не совпадает; исходный файл не изменён')
    if signature(selected)!=before:
        raise ValueError('База изменилась во время проверки. Остановите другую запись и повторите.')
    return selected
