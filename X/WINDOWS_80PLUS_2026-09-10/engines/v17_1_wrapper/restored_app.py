"""Complete V16 application with a presentation-only V17.1 restoration layer.

No replacements of replay, search, persistence, export, preparation or safety code.
Research version remains V16 so its checkpoints and findings keep their identity.
"""
import json
import multiprocessing
import os
from pathlib import Path
import sys
import traceback
from bootstrap import load_original

original = load_original()
from journal import install, qualifies
install()
from PySide6.QtCore import Qt, QTimer
from PySide6.QtWidgets import (QApplication, QFrame, QHBoxLayout, QLabel, QMessageBox,
    QPushButton, QTableWidget, QTableWidgetItem, QHeaderView, QVBoxLayout, QTabWidget)
from theme import STYLE, Timeline, configure
from offline_data import select_existing, verify_existing, signature

VERSION = 'V17.1 · локальные базы · алгоритм V16'


def existing_bases(root):
    """Read manifests, never rewrite, delete or download the user's data."""
    rows = []
    for path in sorted((Path(root) / 'data' / 'prepared').glob('*.json')):
        try:
            value = json.loads(path.read_text(encoding='utf-8'))
            from pump_research_lab.storage_v13 import relocated
            value = relocated(value, Path(root))
            datasets = value.get('datasets', {})
            ready = all(Path(datasets.get(s, {}).get('raw_path', '')).is_file()
                        and Path(datasets.get(s, {}).get('manifest_path', '')).is_file()
                        for s in ('PUMPUSDT', 'BTCUSDT', 'SOLUSDT'))
            period = value.get('period', {})
            rows.append(dict(name=value.get('market_regime', 'RECENT'), months=value.get('months', ''),
                             start=period.get('start_ms'), end=period.get('end_ms'), ready=ready,
                             size=sum(r.get('size_bytes', 0) for r in datasets.values()), path=str(path)))
        except (OSError, ValueError, KeyError, TypeError):
            rows.append(dict(name=path.stem, months='', start=None, end=None, ready=False, size=0, path=str(path)))
    return rows


class RestoredWindow(original.ResearchWindow):
    def __init__(self):
        self._auto_prepare = False
        self._auto_ready = False
        self._stage = 'Ожидание'
        self._verified_signature = None
        self._followup_retest = False
        super().__init__()
        self.setWindowTitle('PUMP Research Lab — ' + VERSION)
        self.setStyleSheet(STYLE)
        self.resize(1280, 950)
        outer = self.centralWidget().layout()
        outer.setContentsMargins(20, 14, 20, 14)
        outer.setSpacing(8)
        title = outer.itemAt(0).widget()
        title.setText('PUMP / RESEARCH LAB     V17.1')
        title.setObjectName('title')
        title.setStyleSheet('')
        self.hardware.setObjectName('subtitle')
        self.hardware.setWordWrap(True)
        self.metric_values = {}
        metrics = QHBoxLayout()
        for key, label in [('cpu','CPU · загрузка'), ('cpu_temp','CPU · температура'),
                           ('ram','Память RAM'), ('gpu','GPU · загрузка'), ('gpu_temp','GPU · температура')]:
            box = QFrame(); box.setObjectName('metric')
            layout = QVBoxLayout(box); layout.setContentsMargins(12,7,12,7)
            caption = QLabel(label); caption.setObjectName('metricTitle')
            value = QLabel('…'); value.setObjectName('metricValue')
            layout.addWidget(caption); layout.addWidget(value); metrics.addWidget(box)
            self.metric_values[key] = value
        outer.insertLayout(2, metrics)
        self.stage_label = QLabel('Ожидание · выберите режимы рынка ниже'); self.stage_label.setObjectName('stage')
        outer.insertWidget(3, self.stage_label)
        subtitle = QLabel('Алгоритм и архив V16 сохранены · только ваши локальные базы · скачивание отключено')
        subtitle.setObjectName('subtitle'); subtitle.setWordWrap(True)
        outer.insertWidget(1, subtitle)
        old = self.chart
        self.chart = Timeline()
        old.parentWidget().layout().replaceWidget(old, self.chart)
        old.setParent(None); old.deleteLater()
        self.tabs = next(t for t in self.findChildren(QTabWidget)
                         if any(t.tabText(i) == 'Базы данных' for i in range(t.count())))
        research = self.tabs.widget(0).layout()
        self.auto_button = QPushButton('Проверить локальные базы → начать / продолжить поиск')
        self.auto_button.setObjectName('primary')
        self.auto_button.clicked.connect(self.automatic)
        research.insertWidget(0, self.auto_button)
        self.prepare_button.setText('1. Проверить / подключить местные базы')
        self.start_button.setText('2. Поиск / продолжить на выбранных рынках')
        self.reselect_button.setText('Перечитать имеющиеся комплекты')
        self.explicit.setText('Задать даты трёх периодов вручную')
        self.explicit.setChecked(False)
        # The owner explicitly removed new downloads/date preparation from this build.
        self.explicit.hide()
        for control in self.period_dates:
            control.hide()
        self.explicit.toggled.connect(self.date_state)
        self.date_state()
        for name, control in self.regimes.items():
            control.setText({'BEAR':'Медвежий · падение','SIDEWAYS':'Боковой · боковик','BULL':'Бычий · рост'}[name])
            control.setChecked(True)
        self.prepare_button.setToolTip('Читает только существующие файлы, проверяет даты PUMP/BTC/SOL и контрольные суммы. Не скачивает.')
        self.resource.setToolTip('Исходные профили V16: число CPU-процессов выбирается с учётом компьютера. GPU не используется для replay.')
        self.months.setToolTip('Длительность КАЖДОГО выбранного рыночного периода: 1, 3 или 6 месяцев.')
        self.data_page = self.tabs.widget(next(i for i in range(self.tabs.count()) if self.tabs.tabText(i)=='Базы данных'))
        dl = self.data_page.layout()
        location = QLabel('Хранилище: ' + str(self.root)); location.setWordWrap(True)
        dl.insertWidget(0, location)
        bar = QHBoxLayout()
        self.data_prepare_button = QPushButton('Проверить выбранные локальные комплекты')
        self.data_prepare_button.clicked.connect(self.prepare)
        bar.addWidget(self.data_prepare_button)
        refresh = QPushButton('Обновить список баз'); refresh.clicked.connect(self.refresh_catalog); bar.addWidget(refresh)
        choose = QPushButton('Выбрать рынки и период'); choose.clicked.connect(lambda:self.tabs.setCurrentIndex(0)); bar.addWidget(choose)
        dl.insertLayout(1, bar)
        self.catalog = QTableWidget(0, 6)
        self.catalog.setHorizontalHeaderLabels(['Рынок','Месяцев','Период UTC','PUMP / BTC / SOL','Объём','Комплект базы'])
        self.catalog.horizontalHeader().setSectionResizeMode(QHeaderView.ResizeMode.ResizeToContents)
        self.catalog.horizontalHeader().setStretchLastSection(True)
        self.catalog.setEditTriggers(QTableWidget.EditTrigger.NoEditTriggers)
        self.catalog.setMinimumHeight(160)
        self.catalog.cellDoubleClicked.connect(self.use_catalog)
        dl.insertWidget(2, QLabel('Все сохранённые комплекты · двойной щелчок выбирает период и рынок, остальные рынки можно отметить на главной'))
        dl.insertWidget(3, self.catalog)
        dl.insertWidget(4, QLabel('Ниже — файлы выбранных режимов. Двойной щелчок открывает расположение файла.'))
        for label in self.data_page.findChildren(QLabel):
            if 'после 30 дней' in label.text():
                label.setText('Локальные PUMP + BTC + SOL · срок давности не мешает исследованию · интернет-загрузка отключена')
        for table in self.findChildren(QTableWidget):
            table.setAlternatingRowColors(True)
            table.setShowGrid(False)
        for label in self.findChildren(QLabel):
            if 'Свечи и сигналы:' in label.text():
                label.setText('Свечи и сигналы: 1 минута · Журнал: от +50% NET на каждом из трёх рынков · Реальные ордера отключены')
        self.archive_note = QLabel('Журнал: минимум +50% NET на КАЖДОМ рынке · BEAR + SIDEWAYS + BULL. Это не торговый допуск.')
        self.archive_note.setWordWrap(True)
        self.library_table.parentWidget().layout().insertWidget(0, self.archive_note)
        self.refresh_catalog()
        self.selection_changed()
        QTimer.singleShot(200, self.update_telemetry)

    def date_state(self, *_):
        for control in self.period_dates:
            control.setEnabled(self.explicit.isChecked() and not self.active)

    def render_library(self, *_):
        self.library = [row for row in self.library if qualifies(row)]
        if hasattr(self, 'archive_note'):
            self.archive_note.setText(f'Журнал: {len(self.library):,} записей · от +50% NET на КАЖДОМ рынке · старые версии не равны независимой проверке')
        super().render_library()

    def set_active(self, active):
        super().set_active(active)
        if hasattr(self, 'data_prepare_button'):
            self.auto_button.setEnabled(not active)
            self.data_prepare_button.setEnabled(not active)
            self.catalog.setEnabled(not active)
            self.date_state()
        if not active and self._auto_ready:
            self._auto_ready = False
            if not self.cancel.is_set() and not self.closing and self.bases_ready():
                retest = self._followup_retest
                self._followup_retest = False
                QTimer.singleShot(0, lambda:self.run_search(retest))

    def selection_changed(self, *_):
        if not hasattr(self, 'status') or self.active:
            return
        self._verified_signature = None
        try:
            self.prepared = select_existing(self.root, self.months.currentData(), self.selected_names())
            complete = self.bases_ready()
            self.status.setText('Местные базы найдены: '+', '.join(self.prepared) if complete else
                                'Не все выбранные комплекты есть на диске. Все три рынка доступны за 6 месяцев. Ничего не скачивается.')
        except (OSError, ValueError, KeyError, TypeError) as error:
            self.prepared = {}
            self.status.setText('Ошибка каталога: '+str(error))
        self.set_active(False)
        self.show_data()

    def automatic(self):
        if self.active:
            return
        if not self.selected_names():
            self.status.setText('Отметьте хотя бы один рынок. Можно выбрать все три одновременно.')
            return
        if self.bases_ready():
            self.start()
        else:
            self._auto_prepare = True
            self.prepare()

    def prepare(self, checked=False, reselect=False):
        if self.active:
            return
        if not self.selected_names() and not self.explicit.isChecked():
            self._auto_prepare = False
            self.status.setText('Отметьте рынки для подготовки баз.')
            return
        self._stage = 'Проверка существующих баз · без скачивания'
        self.stage_label.setText(self._stage)
        names, months = self.selected_names(), self.months.currentData()
        def done(prepared):
            self.prepared = prepared
            self._verified_signature = signature(prepared)
            self.show_data()
            self.status.setText('Локальные базы проверены: '+', '.join(prepared)+'. Скачано 0 байт.')
        self.launch(lambda emit:verify_existing(self.root, months, names, emit, self.cancel.is_set, self.paused.is_set), done)

    def launch(self, function, done=None):
        automatic = self._auto_prepare
        self._auto_prepare = False
        def completed(value):
            if done:
                done(value)
            if automatic and self.bases_ready():
                self._auto_ready = True
            if hasattr(self, 'catalog'):
                self.refresh_catalog()
            if hasattr(self, 'stage_label'):
                self.stage_label.setText('Этап завершён · ' + self.status.text().split('\n')[0])
        super().launch(function, completed)

    def run_search(self, retest=False):
        if self.active:
            return
        if not self.bases_ready():
            self.status.setText('Выберите готовые местные комплекты. Для всех трёх рынков выберите 6 месяцев.')
            return
        try:
            valid = self._verified_signature == signature(self.prepared)
        except OSError:
            valid = False
        if not valid:
            self._auto_prepare = True
            self._followup_retest = retest
            self.prepare()
            return
        self._stage = 'Вычисление · CPU · ' + ', '.join(self.selected_names())
        self.stage_label.setText(self._stage)
        # Keep original safety dialog, checkpoint ID and search engine.
        super().run_search(retest)

    def on_progress(self, value):
        super().on_progress(value)
        if hasattr(self, 'stage_label'):
            self.stage_label.setText(self._stage + ' · ' + str(value.get('stage', value.get('status', ''))))

    def update_telemetry(self):
        if any(getattr(w, 'telemetry_task', False) for w in self.tasks):
            return
        worker = original.Worker(self.telemetry.snapshot)
        worker.telemetry_task = True
        self.tasks.add(worker)
        def done(s):
            if not hasattr(self, 'metric_values'):
                return
            for key, value, suffix in [('cpu',s.cpu_percent,'%'), ('ram',s.ram_percent,'%'),
                                      ('cpu_temp',s.cpu_temperature_c,' °C'), ('gpu',s.gpu_percent,'%'),
                                      ('gpu_temp',s.gpu_temperature_c,' °C')]:
                self.metric_values[key].setText('Нет датчика' if value is None else f'{value:.0f}{suffix}')
            self.hardware.setText(f'{s.gpu_name} · датчики: общая нагрузка компьютера · replay: CPU, не GPU · CPU °C: {getattr(s,"cpu_temperature_source","нет источника")}')
        worker.signals.result.connect(done)
        worker.signals.error.connect(lambda error:self.hardware.setText('Не удалось прочитать датчики: '+error[-180:]))
        worker.signals.finished.connect(lambda:self.tasks.discard(worker))
        self.pool.start(worker)

    def refresh_catalog(self):
        self.catalog_rows = existing_bases(self.root)
        self.catalog.setRowCount(len(self.catalog_rows))
        names = {'BEAR':'Падение','SIDEWAYS':'Боковик','BULL':'Рост','RECENT':'Последний период'}
        for i, row in enumerate(self.catalog_rows):
            dates = f'{original.date(row["start"])} — {original.date(row["end"])}' if row['start'] and row['end'] else 'Нет периода'
            cells = [names.get(row['name'],row['name']),row['months'], dates,
                     'Файлы на диске' if row['ready'] else 'Нужна подготовка',f'{row["size"]/1024**2:,.1f} МБ',row['path']]
            for j, value in enumerate(cells):
                self.catalog.setItem(i,j,QTableWidgetItem(str(value)))

    def use_catalog(self, row, column):
        item = self.catalog_rows[row]
        if self.active or item['name'] not in self.regimes:
            return
        index = self.months.findData(item['months'])
        if index < 0:
            return
        self.explicit.setChecked(False)
        self.months.setCurrentIndex(index)
        for name, control in self.regimes.items():
            control.setChecked(name == item['name'])
        self.selection_changed()
        self.tabs.setCurrentIndex(0)


def main():
    app = QApplication(sys.argv)
    configure(app)
    try:
        window = RestoredWindow()
        window.show()
        return app.exec()
    except Exception:
        QMessageBox.critical(None, 'PUMP Research Lab — запуск', traceback.format_exc())
        return 1


if __name__ == '__main__':
    multiprocessing.freeze_support()
    if '--self-test' in sys.argv:
        from smoke import run
        raise SystemExit(run(Path(sys.argv[sys.argv.index('--self-test')+1])))
    raise SystemExit(main())
