"""Portable archive verification, exact record export and isolated SQLite/data restoration.

Standard library only. Never modifies original PUMP stores. All destinations must
be new. Reading this archive is not a new backtest or promotion to trading.
"""
import argparse, base64, csv, gzip, hashlib, json, sqlite3, zlib
from pathlib import Path

ROOT=Path(__file__).resolve().parent
def canonical(value):return json.dumps(value,ensure_ascii=False,sort_keys=True,separators=(',',':'))
def digest(path):
    h=hashlib.sha256()
    with path.open('rb') as f:
        for b in iter(lambda:f.read(1024*1024),b''):h.update(b)
    return h.hexdigest()
def records():
    for part in sorted((ROOT/'records').glob('*.jsonl.gz')):
        with gzip.open(part,'rt',encoding='utf-8') as f:
            for line in f:yield json.loads(line)
def new_file(path):
    path=Path(path).resolve()
    if path.exists():raise FileExistsError('Refusing to overwrite '+str(path))
    path.parent.mkdir(parents=True,exist_ok=True)
    return path
def export_record(args):
    found=[r for r in records() if r['candidate_id']==args.id and (not args.record_hash or r['record_sha256']==args.record_hash)]
    if not found:raise ValueError('ID / record hash not found')
    p=new_file(args.output);p.write_text(json.dumps(found,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps({'records':len(found),'output':str(p)}))
def report_record(args):
    found=[r for r in records() if r['candidate_id']==args.id]
    if not found:raise ValueError('ID not found')
    lines=['# '+args.id+' — полный сохранённый отчёт','',
           'Исторические записи. Это не новый независимый тест и не гарантия прибыли.',
           'Все версии записи показаны отдельно; меньшие результаты и расхождения сохранены.','']
    for r in found:
        lines+=['## Запись '+r['record_sha256'],'',
                'Полные доступные параметры: '+str(r['full_detail'])+'. Причина сохранения: '+r['selection_reason']+'.','',
                '### Записанная доходность','', '| Поле | Проценты |','|---|---:|']
        lines += ['| '+k+' | '+format(v*100,'.8f')+' |' for k,v in r['recorded_returns_fraction'].items()]
        lines += ['', '### Параметры алгоритма','', '| Параметр | Точное значение |','|---|---|']
        lines += ['| '+k+' | `'+json.dumps(v,ensure_ascii=False)+'` |' for k,v in r['decoded'].get('config',{}).items()]
        lines += ['', '### Происхождение','', '```json',json.dumps(r['provenance'],ensure_ascii=False,indent=2),'```','',
                  '### Все сохранённые подробности, метрики и траектория','', '```json',json.dumps(r['decoded'],ensure_ascii=False,indent=2),'```','']
    p=new_file(args.output);p.write_text('\n'.join(lines),encoding='utf-8');print(json.dumps({'records':len(found),'report':str(p)}))
def restore_db(args):
    sources=json.loads((ROOT/'SOURCE_DATABASES.json').read_text(encoding='utf-8'))
    source=next((s for s in sources if s['source']==args.source),None)
    if not source:raise ValueError('Unknown source; see SOURCE_DATABASES.json')
    p=new_file(args.output);db=sqlite3.connect(p)
    try:
        for schema in source['schema']:
            if schema['sql'] and not schema['name'].startswith('sqlite_'):db.execute(schema['sql'])
        count=0
        for r in records():
            provenance=[x for x in r['provenance'] if x['source']==args.source]
            if not provenance:continue
            row={k:base64.b64decode(v['$base64']) if isinstance(v,dict) and '$base64' in v else v for k,v in r['original_row'].items()}
            for pr in provenance:
                table=pr['table'];cols=list(row)
                quote=lambda s:'"'+s.replace('"','""')+'"'
                db.execute('INSERT INTO '+quote(table)+' ('+','.join(map(quote,cols))+') VALUES ('+','.join('?' for _ in cols)+')',[row[k] for k in cols]);count+=1
        for table,values in source.get('metadata',{}).items():
            for row in values:
                cols=list(row);quote=lambda s:'"'+s.replace('"','""')+'"'
                db.execute('INSERT INTO '+quote(table)+' ('+','.join(map(quote,cols))+') VALUES ('+','.join('?' for _ in cols)+')',[row[k] for k in cols])
        db.commit()
        if db.execute('pragma integrity_check').fetchone()[0]!='ok':raise RuntimeError('SQLite integrity failed')
    finally:db.close()
    print(json.dumps({'restored_selected_rows':count,'output':str(p),'note':'Selected archive subset; not a complete campaign database.'}))
def restore_data(args):
    dest=Path(args.output).resolve()
    if dest.exists():raise FileExistsError('Destination must be a new directory')
    mapping=json.loads((ROOT/'data/DATA_FILES.json').read_text(encoding='utf-8'))
    selected=[x for x in mapping if not args.prefix or x['original_relative_path'].startswith(args.prefix)]
    if not selected:raise ValueError('No matching paths')
    dest.mkdir(parents=True)
    for item in selected:
        p=(dest/item['original_relative_path']).resolve()
        if not p.is_relative_to(dest):raise ValueError('Unsafe data path')
        p.parent.mkdir(parents=True,exist_ok=True);h=hashlib.sha256();n=0
        with gzip.open(ROOT/item['blob'],'rb') as src,p.open('xb') as out:
            for b in iter(lambda:src.read(1024*1024),b''):out.write(b);h.update(b);n+=len(b)
        if h.hexdigest()!=item['sha256'] or n!=item['bytes']:raise ValueError('Corrupt restored file '+str(p))
    print(json.dumps({'restored_files':len(selected),'output':str(dest),'note':'Original manifests remain byte-exact, including old absolute paths. Make separate remapped copies for replay.'}))
def verify(args):
    manifest=json.loads((ROOT/'MANIFEST.json').read_text(encoding='utf-8'))
    for item in manifest['files']:
        p=ROOT/item['path']
        if p.stat().st_size!=item['bytes'] or digest(p)!=item['sha256']:raise ValueError('File mismatch '+str(p))
    n=q=full=0;ids=set();hashes=set()
    for r in records():
        h=r['record_sha256'];body={k:v for k,v in r.items() if k not in ('record_sha256','provenance','selection_reason')}
        if hashlib.sha256(canonical(body).encode()).hexdigest()!=h:raise ValueError('Record mismatch '+h)
        if h in hashes:raise ValueError('Duplicate record hash')
        hashes.add(h);ids.add(r['candidate_id']);n+=1;q+=r['qualifies_80_any_recorded_return'];full+=r['full_detail']
        if 'result' in r['original_row']:
            raw=r['original_row']['result']
            decoded=json.loads(zlib.decompress(base64.b64decode(raw['$base64'])) if isinstance(raw,dict) else raw)
            # The exporter can add only a default config to legacy records.
            for k,v in decoded.items():
                if r['decoded'][k]!=v:raise ValueError('Original SQLite result differs '+h)
    summary=json.loads((ROOT/'SUMMARY.json').read_text(encoding='utf-8'))
    if [n,q,full,len(ids)]!=[summary['preserved_record_versions'],summary['qualifying_record_versions'],summary['full_detail_record_versions'],summary['distinct_candidate_ids']]:raise ValueError('Summary counts differ')
    with (ROOT/'INDEX.csv').open(encoding='utf-8',newline='') as f:
        index=list(csv.DictReader(f))
    if len(index)!=n or {x['record_sha256'] for x in index}!=hashes:raise ValueError('Index coverage differs')
    data_count=0
    if args.deep:
        for item in json.loads((ROOT/'data/DATA_FILES.json').read_text(encoding='utf-8')):
            h=hashlib.sha256();size=0
            with gzip.open(ROOT/item['blob'],'rb') as f:
                for b in iter(lambda:f.read(1024*1024),b''):h.update(b);size+=len(b)
            if h.hexdigest()!=item['sha256'] or size!=item['bytes']:raise ValueError('Data payload mismatch '+item['blob'])
            data_count+=1
    print(json.dumps({'status':'PASS','files':len(manifest['files']),'record_versions':n,'qualifying_records':q,'full_detail_records':full,'candidate_ids':len(ids),'data_files_deep_verified':data_count}))
def main():
    p=argparse.ArgumentParser(description=__doc__);sub=p.add_subparsers(dest='command',required=True)
    a=sub.add_parser('verify');a.add_argument('--deep',action='store_true');a.set_defaults(func=verify)
    a=sub.add_parser('extract');a.add_argument('--id',required=True);a.add_argument('--record-hash');a.add_argument('--output',required=True);a.set_defaults(func=export_record)
    a=sub.add_parser('report');a.add_argument('--id',required=True);a.add_argument('--output',required=True);a.set_defaults(func=report_record)
    a=sub.add_parser('restore-db');a.add_argument('--source',required=True);a.add_argument('--output',required=True);a.set_defaults(func=restore_db)
    a=sub.add_parser('restore-data');a.add_argument('--output',required=True);a.add_argument('--prefix');a.set_defaults(func=restore_data)
    args=p.parse_args();args.func(args)
if __name__=='__main__':main()
