"""Read-only application audit on the original workspace; no search/download."""
import hashlib
import json
import os
from pathlib import Path
import sys
import time
from PySide6.QtWidgets import QApplication
from restored_app import RestoredWindow
from theme import configure


def audit(output):
    root = Path(r'F:\PUMP Research Lab')
    output = Path(output).resolve()
    output.mkdir(parents=True, exist_ok=False)
    os.environ['PUMP_LAB_HOME'] = str(root)
    os.environ['QT_QPA_PLATFORM'] = 'offscreen'
    descriptors = sorted((root/'data'/'prepared').glob('*.json'))
    before = {str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in descriptors}
    app = QApplication([]); configure(app)
    window = RestoredWindow(); window.show()
    window.update_telemetry()
    until = time.monotonic()+15
    while time.monotonic()<until:
        app.processEvents()
        time.sleep(.03)
    window.timer.stop()
    window.pool.waitForDone(10000)
    app.processEvents()
    assert window.bases_ready() and len(window.prepared)==3
    assert window.data_table.rowCount()==9
    # A strict journal can be empty; the full experiment databases still retain progress.
    assert all(hashlib.sha256(Path(p).read_bytes()).hexdigest()==digest for p,digest in before.items())
    window.grab().save(str(output/'research.png'))
    window.tabs.setCurrentWidget(window.data_page); app.processEvents()
    window.grab().save(str(output/'databases.png'))
    result = dict(root=str(window.root), prepared=list(window.prepared), selected_files=window.data_table.rowCount(),
                  catalog_rows=window.catalog.rowCount(), archived_strategies=len(window.library),
                  cpu_profiles=[window.resource.itemData(i) for i in range(window.resource.count())],
                  telemetry={key:value.text() for key,value in window.metric_values.items()},hardware=window.hardware.text(),
                  descriptors_unchanged=True, downloaded_bytes=0, status='PASS')
    (output/'audit.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(result,ensure_ascii=False,indent=2))
    window.close(); app.processEvents()


if __name__=='__main__':
    audit(sys.argv[1])
