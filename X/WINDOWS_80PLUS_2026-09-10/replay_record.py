"""Explicit, isolated one-candidate replay using a preserved V13-V16 reference.

This is a replay utility, not a promise that archived headline returns reproduce.
Requires Python 3.13 and engines/requirements.txt. Never uses the original F: store.
"""
import argparse, hashlib, json, multiprocessing, sys
from pathlib import Path
ROOT=Path(__file__).resolve().parent

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--engine',choices=['v13','v14','v15','v16'],required=True)
    p.add_argument('--record',required=True,help='JSON object or array exported by archive.py extract')
    p.add_argument('--record-hash',required=True,help='Explicitly choose a historical record version')
    p.add_argument('--data',required=True,help='Isolated root created by archive.py restore-data')
    p.add_argument('--output',required=True,help='New output directory')
    p.add_argument('--regimes',nargs='+',choices=['BEAR','SIDEWAYS','BULL'],default=['BEAR','SIDEWAYS','BULL'])
    p.add_argument('--capital',type=float,default=1000.0)
    args=p.parse_args()
    if sys.version_info[:2]!=(3,13):raise RuntimeError('Compiled references require Python 3.13')
    output=Path(args.output).resolve();data=Path(args.data).resolve()
    if output.exists():raise FileExistsError('Use a new output directory')
    if output==data or output.is_relative_to(data):raise ValueError('Keep replay outputs separate from restored source data')
    manifest=json.loads((ROOT/'engines'/args.engine/'manifest.json').read_text(encoding='utf-8'))
    compiled=ROOT/'engines'/args.engine/'compiled'
    for item in manifest['modules']:
        if hashlib.sha256((compiled/item['path']).read_bytes()).hexdigest()!=item['sha256']:raise ValueError('Engine checksum mismatch')
    entries=json.loads(Path(args.record).read_text(encoding='utf-8'))
    if isinstance(entries,dict):entries=[entries]
    record=next((r for r in entries if r['record_sha256']==args.record_hash),None)
    if not record or not record['full_detail']:raise ValueError('Selected full record not found')
    cid=record['candidate_id'];expected_prefix=args.engine.upper()+'-'
    if not cid.startswith(expected_prefix):raise ValueError('Engine/ID mismatch. Legacy A* and V12 require their historical implementation, not silent substitution.')
    config=record['decoded']['config']
    sys.path.insert(0,str(compiled))
    from pump_research_lab.research_v12 import ResearchV12
    from pump_research_lab.baseline_x import EconomyConfig
    from dataclasses import fields
    expected={x.name for x in fields(EconomyConfig)}
    if set(config)!=expected:raise ValueError('Config fields differ; do not silently default or discard historical parameters')
    prepared={}
    for name in args.regimes:
        descriptor=json.loads((data/f'data/prepared/research-base-6m-{name.lower()}.json').read_text(encoding='utf-8'))
        for symbol,ds in descriptor['datasets'].items():
            for field in ('raw_path','manifest_path'):
                parts=ds[field].replace('\\','/').split('/data/',1)
                if len(parts)!=2:raise ValueError('Unknown original data path')
                ds[field]=str(data/'data'/parts[1])
        prepared[name]=descriptor
    output.mkdir(parents=True)
    for sub in ('reports/ready','logs','models','checkpoints','experiments'):(output/sub).mkdir(parents=True,exist_ok=True)
    # Reproduce the documented historical research policy. This is not a fresh TEST.
    policy=json.loads((ROOT/'context/research-policy-v13.json').read_text(encoding='utf-8'))
    (output/'research-policy-v13.json').write_text(json.dumps(policy),encoding='utf-8')
    (output/'request.json').write_text(json.dumps({'engine':args.engine,'record_sha256':args.record_hash,'candidate_id':cid,'capital':args.capital,'prepared':prepared,'authority':'HISTORICAL_REPLAY_NOT_NEW_INDEPENDENT_TEST'},ensure_ascii=False,indent=2),encoding='utf-8')
    def progress(s):print(s.get('stage',s.get('status','')),s.get('completed',''),flush=True)
    result=ResearchV12(output,progress).run(prepared,resource='BACKGROUND',ai_enabled=False,seeds=[config],retest_only=True,max_trials=1,workers_override=1,initial_capital=args.capital)
    (output/'replay-result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2,default=str),encoding='utf-8')
    print('Replay output:',output)
if __name__=='__main__':
    multiprocessing.freeze_support();main()
