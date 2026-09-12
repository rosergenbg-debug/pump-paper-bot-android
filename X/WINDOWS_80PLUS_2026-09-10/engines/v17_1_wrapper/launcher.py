"""Startup diagnostics also cover errors while importing the recovered runtime."""
import json
import multiprocessing
from pathlib import Path
import sys
import traceback


def launch():
    try:
        from bootstrap import load_original
        load_original()
        multiprocessing.freeze_support()
        if '--engine-self-test' in sys.argv:
            from engine_smoke import run
            run(Path(sys.argv[sys.argv.index('--engine-self-test')+1]))
            return 0
        if '--self-test' in sys.argv:
            from smoke import run
            return run(Path(sys.argv[sys.argv.index('--self-test')+1]))
        from restored_app import main
        return main()
    except Exception:
        error=traceback.format_exc()
        test_flag = next((flag for flag in ('--self-test','--engine-self-test') if flag in sys.argv), None)
        if test_flag:
            output=Path(sys.argv[sys.argv.index(test_flag)+1])
            output.mkdir(parents=True,exist_ok=True)
            (output/'verification.json').write_text(json.dumps(dict(status='FAIL',error=error),ensure_ascii=False,indent=2),encoding='utf-8')
        else:
            from PySide6.QtWidgets import QApplication,QMessageBox
            app=QApplication.instance() or QApplication(sys.argv)
            QMessageBox.critical(None,'PUMP — ошибка запуска',error)
        return 1


if __name__=='__main__':
    raise SystemExit(launch())
