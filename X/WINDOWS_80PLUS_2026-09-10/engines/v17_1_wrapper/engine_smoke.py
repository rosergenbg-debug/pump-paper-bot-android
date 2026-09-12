"""One bounded original-engine replay on three existing windows, isolated outputs."""
import json
import multiprocessing
from pathlib import Path
import sys
from bootstrap import load_original
load_original()
from offline_data import verify_existing
from pump_research_lab.research_v12 import ResearchV12
from pump_research_lab.baseline_x import EconomyConfig
from dataclasses import asdict


def run(output):
    from journal import install
    install()
    output = Path(output).resolve()
    output.mkdir(parents=True,exist_ok=False)
    prepared = verify_existing(Path(r'F:\PUMP Research Lab'),6,['BEAR','SIDEWAYS','BULL'])
    for sub in ('reports/ready','logs','models','checkpoints','experiments'):
        (output/sub).mkdir(parents=True,exist_ok=True)
    def progress(value):
        print(value.get('stage',value.get('status','')),value.get('completed',''),flush=True)
    result = ResearchV12(output,progress).run(prepared,resource='BACKGROUND',ai_enabled=False,
                seeds=[asdict(EconomyConfig())],retest_only=True,max_trials=1,workers_override=2)
    (output/'engine-result.json').write_text(json.dumps(result,ensure_ascii=False,default=str,indent=2),encoding='utf-8')
    print('FINISHED', result.get('status'),result.get('completed'),flush=True)


if __name__=='__main__':
    multiprocessing.freeze_support()
    run(sys.argv[1])
