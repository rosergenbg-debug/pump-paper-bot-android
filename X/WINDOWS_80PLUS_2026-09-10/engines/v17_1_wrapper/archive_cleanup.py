"""Explicit audited SQLite archive compaction with an online backup per file."""
from datetime import datetime, timezone
import json
from pathlib import Path
import sqlite3
import sys
from bootstrap import load_original
load_original()
from journal import compact


def run(output, apply=False):
    root=Path(r'F:\PUMP Research Lab').resolve()
    output=Path(output).resolve(); output.mkdir(parents=True,exist_ok=False)
    from PySide6.QtCore import QLockFile
    lock=QLockFile(str(root/'v16.lock'))
    if not lock.tryLock(0):
        raise RuntimeError('V16 currently owns the workspace. Pause and close it before cleanup.')
    records=[]
    try:
        for path in sorted((root/'experiments').glob('v*/research.sqlite3')):
            if not path.resolve().is_relative_to(root/'experiments'):
                raise ValueError('Archive outside the approved directory')
            con=sqlite3.connect(path if apply else path.as_uri()+'?mode=ro',uri=not apply,timeout=30)
            try:
                before=path.stat().st_size
                if apply:
                    backup=output/(path.parent.name+'.sqlite3')
                    dest=sqlite3.connect(backup)
                    try:
                        con.backup(dest)
                        assert dest.execute('PRAGMA quick_check').fetchone()[0]=='ok'
                    finally:
                        dest.close()
                    # Refuse uncoordinated writers while deciding and applying retention.
                    con.execute('BEGIN IMMEDIATE')
                stats=compact(con,apply)
                if apply:
                    con.commit()
                    con.execute('PRAGMA wal_checkpoint(TRUNCATE)')
                    con.execute('VACUUM')
                    con.execute('PRAGMA wal_checkpoint(TRUNCATE)')
                    assert con.execute('PRAGMA quick_check').fetchone()[0]=='ok'
                row=dict(path=str(path),before_bytes=before,after_bytes=path.stat().st_size,**stats)
                records.append(row); print(json.dumps(row),flush=True)
            finally:
                con.close()
        # V11's separate positive library is not an execution checkpoint.
        path=root/'models'/'positive-strategies-v11.sqlite3'
        if path.is_file():
            con=sqlite3.connect(path if apply else path.as_uri()+'?mode=ro',uri=not apply,timeout=30)
            try:
                before=path.stat().st_size
                candidates={row[0] for row in con.execute('SELECT candidate_id FROM strategies')}
                good={}
                for cid,regime,net in con.execute('SELECT candidate_id,regime,MAX(development_compound) FROM discoveries GROUP BY candidate_id,regime'):
                    if net is not None and net>=.5:
                        good.setdefault(cid,set()).add(regime.upper())
                keep={cid for cid,names in good.items() if {'BEAR','SIDEWAYS','BULL'}<=names}
                remove=candidates-keep
                if apply:
                    dest=sqlite3.connect(output/'positive-strategies-v11.sqlite3')
                    try:
                        con.backup(dest)
                        assert dest.execute('PRAGMA quick_check').fetchone()[0]=='ok'
                    finally:
                        dest.close()
                    with con:
                        for table in ('retests','discoveries','strategies'):
                            con.executemany(f'DELETE FROM {table} WHERE candidate_id=?',[(cid,) for cid in remove])
                    con.execute('PRAGMA wal_checkpoint(TRUNCATE)'); con.execute('VACUUM')
                    con.execute('PRAGMA wal_checkpoint(TRUNCATE)')
                    assert con.execute('PRAGMA quick_check').fetchone()[0]=='ok'
                row=dict(path=str(path),before_bytes=before,after_bytes=path.stat().st_size,
                         completed=len(candidates),journal_50_each=len(keep),details_removed=len(remove),working_frontier=0)
                records.append(row); print(json.dumps(row),flush=True)
            finally:
                con.close()
        (output/'cleanup-report.json').write_text(json.dumps(dict(applied=apply,at=datetime.now(timezone.utc).isoformat(),archives=records),indent=2),encoding='utf-8')
    finally:
        lock.unlock()


if __name__=='__main__':
    run(sys.argv[1], '--apply' in sys.argv)
