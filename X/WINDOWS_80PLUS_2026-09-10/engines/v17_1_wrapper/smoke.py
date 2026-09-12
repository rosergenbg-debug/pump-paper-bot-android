"""Offline UI acceptance check against real existing descriptors, no market writes."""
import json
import os
from pathlib import Path
import shutil
import sys
import time


def run(output):
    output = Path(output).resolve()
    if output.exists():
        raise ValueError('Self-test requires a NEW directory, never the user workspace')
    output.mkdir(parents=True)
    root = output / 'isolated-workspace'
    (root / 'data' / 'prepared').mkdir(parents=True)
    source = Path(r'F:\PUMP Research Lab\data\prepared')
    for name in ('bear', 'sideways', 'bull'):
        shutil.copy2(source / f'research-base-6m-{name}.json', root / 'data' / 'prepared')
    os.environ['PUMP_LAB_HOME'] = str(root)
    os.environ['QT_QPA_PLATFORM'] = 'offscreen'
    from PySide6.QtWidgets import QApplication
    from restored_app import RestoredWindow, original
    from theme import configure
    from unittest.mock import patch
    app = QApplication.instance() or QApplication([])
    configure(app)
    # Alias can occur when this module is entered from a frozen __main__.
    window = RestoredWindow()
    window.show()
    window.timer.stop()
    window.update_telemetry()
    errors = []
    old_hook = sys.excepthook
    sys.excepthook = lambda kind, exc, tb:errors.append(str(exc))
    deadline = time.monotonic()+8
    while time.monotonic()<deadline:
        app.processEvents()
        if window.bases_ready() and not window.tasks and 'Определяю' not in window.hardware.text():
            break
        time.sleep(.02)
    try:
        assert set(window.selected_names()) == {'BEAR','SIDEWAYS','BULL'}
        assert window.bases_ready(), list(window.prepared)
        assert window.data_table.rowCount()==9, window.data_table.rowCount()
        assert window.catalog.rowCount()==3
        assert window.start_button.isEnabled()
        assert window.prepare_button.isEnabled()
        assert window.resource.count()==3
        assert not window.explicit.isChecked()
        assert all(not d.isEnabled() for d in window.period_dates)
        assert RestoredWindow.start is original.ResearchWindow.start
        assert RestoredWindow.toggle_pause is original.ResearchWindow.toggle_pause
        assert RestoredWindow.export_selected is original.ResearchWindow.export_selected
        # A single click with cached bases dispatches to original start, no preparation.
        with patch.object(window, 'start') as start, patch.object(window, 'prepare') as prepare:
            window.auto_button.click()
            start.assert_called_once(); prepare.assert_not_called()
        # Two simultaneous regime choices, then all three, are accepted.
        window.regimes['BULL'].setChecked(False)
        assert len(window.prepared)==2 and window.bases_ready()
        window.regimes['BULL'].setChecked(True)
        assert len(window.prepared)==3 and window.bases_ready()
        # The restored UI validates all local files and reaches the original search callback.
        from offline_data import verify_existing
        with patch('urllib.request.urlopen', side_effect=AssertionError('Unexpected network')):
            prepared = verify_existing(root, 6, ['BEAR','SIDEWAYS','BULL'])
            with patch.object(original.ResearchWindow, 'run_search') as dispatch:
                window.auto_button.click()
                deadline = time.monotonic()+15
                while not dispatch.called and time.monotonic()<deadline:
                    app.processEvents(); time.sleep(.02)
                dispatch.assert_called_once_with(False)
                assert not window.active
                assert len(window.prepared)==3
        assert set(prepared)=={'BEAR','SIDEWAYS','BULL'}
        assert not errors, errors
        window.grab().save(str(output/'research.png'))
        window.tabs.setCurrentWidget(window.data_page)
        app.processEvents()
        window.grab().save(str(output/'databases.png'))
        result = dict(status='PASS',selected_regimes=list(window.prepared),files_visible=window.data_table.rowCount(),
                      cached_preparation_without_network=True,original_controls_preserved=True,
                      automatic_verification_dispatches_to_original_search=True,
                      cpu_profiles=window.resource.count(),hardware=window.hardware.text(),
                      warning='UI and cached preparation test; no profitability claim; user data not modified')
        (output/'verification.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
        return 0
    finally:
        sys.excepthook = old_hook
        window.timer.stop()
        window.pool.waitForDone(10000)
        app.processEvents()
        window.close()
        app.processEvents()


if __name__=='__main__':
    raise SystemExit(run(Path(sys.argv[1])))
