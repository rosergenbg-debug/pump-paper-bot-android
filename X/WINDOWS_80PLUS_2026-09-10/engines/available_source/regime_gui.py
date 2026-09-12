from __future__ import annotations

import json
import os
import subprocess
import sys
import threading
import traceback
from pathlib import Path
from typing import Callable

from PySide6.QtCore import QObject, QPointF, QRunnable, QThreadPool, QTimer, Qt, Signal, Slot
from PySide6.QtGui import QColor, QPainter, QPen, QPolygonF
from PySide6.QtWidgets import (
    QApplication, QFrame, QGridLayout, QHBoxLayout, QHeaderView, QLabel, QMainWindow,
    QMessageBox, QProgressBar, QPushButton, QScrollArea, QTableWidget, QTableWidgetItem,
    QVBoxLayout, QWidget,
)

from .automation import ready_reports_directory
from .hardware import detect_hardware, workers_for_mode
from .optimizer import ResearchCancelled
from .regime_lab import REGIME_LAB_VERSION, RegimeStressRunner
from .telemetry import SystemTelemetry


def regime_root() -> Path:
    override = os.environ.get("PUMP_LAB_HOME")
    if override:
        root = Path(override)
    elif getattr(sys, "frozen", False):
        existing = Path.home() / "Documents" / "PUMP Research Lab"
        root = existing if existing.exists() else Path(sys.executable).resolve().parent / "PUMP Research Lab Workspace"
    else:
        root = Path.home() / "Documents" / "PUMP Research Lab"
    for name in ("data", "checkpoints", "reports", "regime_stress"):
        (root / name).mkdir(parents=True, exist_ok=True)
    return root


class WorkerSignals(QObject):
    result = Signal(object)
    progress = Signal(object)
    error = Signal(str)
    finished = Signal()


class Worker(QRunnable):
    def __init__(self, fn: Callable) -> None:
        super().__init__(); self.fn = fn; self.signals = WorkerSignals()

    @Slot()
    def run(self) -> None:
        try:
            self.signals.result.emit(self.fn(self.signals.progress.emit))
        except ResearchCancelled as exc:
            self.signals.error.emit(str(exc))
        except Exception:
            self.signals.error.emit(traceback.format_exc())
        finally:
            self.signals.finished.emit()


class Card(QFrame):
    def __init__(self, title: str) -> None:
        super().__init__(); self.setObjectName("card")
        self.box = QVBoxLayout(self)
        heading = QLabel(title); heading.setObjectName("cardTitle")
        self.box.addWidget(heading)


class PriceRegimeChart(QWidget):
    COLORS = {"BEAR": QColor("#ff667a"), "SIDEWAYS": QColor("#ffc857"), "BULL": QColor("#39d98a")}

    def __init__(self) -> None:
        super().__init__(); self.rows: list[dict] = []; self.windows: list[dict] = []
        self.setMinimumHeight(250)

    def set_data(self, rows: list[dict], windows: list[dict]) -> None:
        self.rows, self.windows = rows, windows; self.update()

    def paintEvent(self, _event) -> None:
        p = QPainter(self); p.setRenderHint(QPainter.RenderHint.Antialiasing); p.fillRect(self.rect(), QColor("#09141e"))
        if len(self.rows) < 2:
            p.setPen(QColor("#8ea6b8")); p.drawText(self.rect(), Qt.AlignmentFlag.AlignCenter, "Сначала нажмите «Найти 3 режима»")
            return
        left, top, right, bottom = 55, 35, self.width() - 18, self.height() - 35
        times = [int(r["time_ms"]) for r in self.rows]; values = [float(r["close"]) for r in self.rows]
        lo, hi = min(values), max(values); span = max(hi - lo, 1e-9); tspan = max(times[-1] - times[0], 1)
        def xy(t: int, v: float) -> QPointF:
            return QPointF(left + (t - times[0]) / tspan * (right - left), top + (hi - v) / span * (bottom - top))
        for w in self.windows:
            x1 = xy(int(w["start_ms"]), lo).x(); x2 = xy(int(w["end_ms"]), lo).x(); color = self.COLORS[w["name"]]
            shade = QColor(color); shade.setAlpha(28); p.fillRect(int(x1), top, max(2, int(x2 - x1)), bottom - top, shade)
            p.setPen(QPen(color, 1)); p.drawText(int(x1) + 4, 22, w["label_ru"])
        points = QPolygonF([xy(t, v) for t, v in zip(times, values)])
        p.setPen(QPen(QColor("#5aa9ff"), 2)); p.drawPolyline(points)
        p.setPen(QColor("#dcebf4")); p.drawText(8, top + 8, f"${hi:.4f}"); p.drawText(8, bottom, f"${lo:.4f}")
        p.setPen(QColor("#8ea6b8")); p.drawText(left, self.height() - 10, "PUMPUSDT — дневная цена и выбранные 180-дневные участки")


class EquityLineChart(QWidget):
    def __init__(self) -> None:
        super().__init__(); self.values = [1000.0]; self.caption = "Текущий лучший: ещё не найден"
        self.setMinimumHeight(220)

    def set_preview(self, preview: dict, caption: str) -> None:
        raw = preview.get("equity", preview.get("values", [])) if isinstance(preview, dict) else []
        if raw:
            first = float(raw[0]) or 1.0; self.values = [1000.0 * float(v) / first for v in raw]
        self.caption = caption; self.update()

    def paintEvent(self, _event) -> None:
        p = QPainter(self); p.setRenderHint(QPainter.RenderHint.Antialiasing); p.fillRect(self.rect(), QColor("#09141e"))
        p.setPen(QColor("#dcebf4")); p.drawText(18, 24, self.caption)
        if len(self.values) < 2:
            p.setPen(QColor("#8ea6b8")); p.drawText(self.rect(), Qt.AlignmentFlag.AlignCenter, "График €1 000 появится во время stress-теста")
            return
        lo, hi = min(self.values), max(self.values); span = max(hi - lo, 1e-9)
        points = QPolygonF()
        for i, value in enumerate(self.values):
            points.append(QPointF(18 + i * (self.width() - 36) / (len(self.values) - 1), 38 + (hi - value) * (self.height() - 70) / span))
        p.setPen(QPen(QColor("#39d98a" if self.values[-1] >= 1000 else "#ff667a"), 3)); p.drawPolyline(points)
        p.setPen(QColor("#dcebf4")); p.drawText(18, self.height() - 10, f"€1 000 → €{self.values[-1]:,.2f}   минимум €{lo:,.2f}   максимум €{hi:,.2f}")


class RegimeLabWindow(QMainWindow):
    def __init__(self) -> None:
        super().__init__(); self.root = regime_root(); self.pool = QThreadPool.globalInstance(); self.workers: set[Worker] = set()
        self.cancel_event = threading.Event(); self.pause_event = threading.Event(); self.selection: dict | None = None; self.prepared: dict | None = None
        self.telemetry = SystemTelemetry(); self.hardware = detect_hardware(); self.setWindowTitle(f"PUMP Regime Stress Lab V{REGIME_LAB_VERSION}")
        self.resize(1380, 900); self._build(); self._load_saved()
        self.timer = QTimer(self); self.timer.timeout.connect(self._update_telemetry); self.timer.start(2000); self._update_telemetry()

    def _build(self) -> None:
        scroll = QScrollArea(); scroll.setWidgetResizable(True); body = QWidget(); page = QVBoxLayout(body); scroll.setWidget(body); self.setCentralWidget(scroll)
        title = QLabel(f"PUMP REGIME STRESS LAB  ·  V{REGIME_LAB_VERSION}"); title.setObjectName("brand"); page.addWidget(title)
        page.addWidget(QLabel("Отдельная исследовательская программа · симуляция · реальные ордера отключены"))

        system = Card("КОМПЬЮТЕР И РЕАЛЬНАЯ НАГРУЗКА"); grid = QGridLayout(); system.box.addLayout(grid)
        self.cpu = QLabel("CPU: —"); self.ram = QLabel("RAM: —"); self.gpu = QLabel("GPU: —"); self.device = QLabel("Точный перебор: CPU")
        for i, widget in enumerate((self.cpu, self.ram, self.gpu, self.device)): widget.setObjectName("metric"); grid.addWidget(widget, i // 2, i % 2)
        note = QLabel(f"{self.hardware.cpu} · {self.hardware.physical_cores} ядер / {self.hardware.logical_processors} потоков · {self.hardware.gpu}. "
                      "Точный свечной replay ветвистый: он распараллелен по CPU. GPU контролируется, но расчёт на неё искусственно не переносится.")
        note.setWordWrap(True); system.box.addWidget(note); page.addWidget(system)

        actions = Card("ТРИ ПОНЯТНЫХ ШАГА"); row = QHBoxLayout(); actions.box.addLayout(row)
        self.discover_btn = QPushButton("1  НАЙТИ 3 РЕЖИМА"); self.download_btn = QPushButton("2  ПОДГОТОВИТЬ ДАННЫЕ"); self.run_btn = QPushButton("3  ЗАПУСТИТЬ STRESS-ТЕСТЫ")
        self.pause_btn = QPushButton("ПАУЗА"); self.stop_btn = QPushButton("БЕЗОПАСНО ОСТАНОВИТЬ"); self.open_btn = QPushButton("ОТКРЫТЬ ОТЧЁТЫ")
        for b in (self.discover_btn, self.download_btn, self.run_btn, self.pause_btn, self.stop_btn, self.open_btn): row.addWidget(b)
        self.discover_btn.clicked.connect(self.discover); self.download_btn.clicked.connect(self.prepare); self.run_btn.clicked.connect(self.run_stress)
        self.pause_btn.clicked.connect(self.toggle_pause); self.stop_btn.clicked.connect(self.stop); self.open_btn.clicked.connect(self.open_reports)
        self.progress = QProgressBar(); self.progress.setRange(0, 100); actions.box.addWidget(self.progress)
        self.stage = QLabel("Готово к выбору участков"); self.stage.setWordWrap(True); actions.box.addWidget(self.stage); page.addWidget(actions)

        regimes = Card("ВЫБРАННЫЕ 6-МЕСЯЧНЫЕ УЧАСТКИ"); self.regime_table = self._table(["Режим", "Начало", "Конец", "PUMP", "Тренд", "Макс. просадка", "Пересечения"], 3)
        regimes.box.addWidget(self.regime_table); self.price_chart = PriceRegimeChart(); regimes.box.addWidget(self.price_chart); page.addWidget(regimes)

        data = Card("ЧТО ИМЕННО СКАЧИВАЕТСЯ И СОЗДАЁТСЯ"); self.data_table = self._table(["Тип", "Данные", "Интервал", "Период", "Строк", "Зачем", "Источник", "Состояние"], 0)
        data.box.addWidget(self.data_table); page.addWidget(data)

        live = Card("ЧТО ОБРАБАТЫВАЕТСЯ ПРЯМО СЕЙЧАС"); self.live_labels = QLabel("Фаза: ожидание · режим: — · данные: — · проверено: 0 · скорость: 0 вариантов/с")
        self.live_labels.setWordWrap(True); live.box.addWidget(self.live_labels)
        self.winner_table = self._table(["Режим", "Состояние", "Лидер", "Худший Avg NET", "Сделки validation", "Архив"], 3)
        live.box.addWidget(self.winner_table); self.equity_chart = EquityLineChart(); live.box.addWidget(self.equity_chart); page.addWidget(live)
        self.setStyleSheet("""
            QMainWindow,QScrollArea,QWidget{background:#071019;color:#dcebf4;font-size:13px} #brand{font-size:25px;font-weight:800;color:#ffffff}
            #card{background:#101d29;border:1px solid #23394b;border-radius:10px;margin:5px} #cardTitle{font-size:16px;font-weight:700;color:#7bc6ff}
            #metric{background:#0a151f;border-radius:7px;padding:12px;font-size:15px;font-weight:600} QPushButton{background:#1769d2;padding:10px;border:0;border-radius:6px;font-weight:700}
            QPushButton:hover{background:#2384ff} QTableWidget{background:#09141e;gridline-color:#294255;alternate-background-color:#0d1a25} QHeaderView::section{background:#172a39;padding:7px}
            QProgressBar{height:22px;text-align:center;background:#09141e;border-radius:6px} QProgressBar::chunk{background:#39d98a;border-radius:6px}
        """)

    @staticmethod
    def _table(headers: list[str], rows: int) -> QTableWidget:
        table = QTableWidget(rows, len(headers)); table.setHorizontalHeaderLabels(headers); table.setAlternatingRowColors(True)
        table.horizontalHeader().setSectionResizeMode(QHeaderView.ResizeMode.Stretch); table.verticalHeader().setVisible(False); table.setMinimumHeight(150)
        return table

    def _load_saved(self) -> None:
        for name, attr, renderer in (("regime-selection.json", "selection", self._show_selection), ("prepared-regime-data.json", "prepared", self._show_prepared)):
            path = self.root / "regime_stress" / name
            if path.exists():
                try:
                    value = json.loads(path.read_text(encoding="utf-8")); setattr(self, attr, value); renderer(value)
                except Exception: pass

    def _start(self, fn: Callable, result: Callable[[dict], None]) -> None:
        self.cancel_event.clear(); worker = Worker(fn); self.workers.add(worker)
        worker.signals.progress.connect(self._progress); worker.signals.result.connect(result); worker.signals.error.connect(self._error)
        worker.signals.finished.connect(lambda w=worker: self.workers.discard(w)); self.pool.start(worker)

    def discover(self) -> None:
        self.stage.setText("Получаем только дневной PUMP и выбираем падение, боковик и рост…")
        self._start(lambda emit: RegimeStressRunner(self.root, emit).discover(), self._discovered)

    def _discovered(self, value: dict) -> None:
        self.selection = value; self._show_selection(value); self.stage.setText("Режимы выбраны. Следующий шаг — подготовить минутные данные.")

    def _show_selection(self, value: dict) -> None:
        overlaps = value.get("overlap_days", {})
        for row, window in enumerate(value["windows"]):
            related = [f"{k}: {v} дн." for k, v in overlaps.items() if window["name"] in k]
            cells = [window["label_ru"], window["start_utc"], window["end_utc"], f"{window['return_fraction']*100:+.2f}%", f"{window['trend_fraction']*100:+.2f}%", f"{window['max_drawdown']*100:.2f}%", ", ".join(related) or "нет"]
            for col, text in enumerate(cells): self.regime_table.setItem(row, col, QTableWidgetItem(str(text)))
        self.price_chart.set_data(value.get("daily_price", []), value["windows"]); self._show_requirements(value.get("requirements", []))

    def _show_requirements(self, rows: list[dict]) -> None:
        self.data_table.setRowCount(len(rows))
        for r, item in enumerate(rows):
            values = [item.get("kind"), item.get("symbol"), item.get("interval"), item.get("period"), f"{int(item.get('rows',0)):,}", item.get("purpose"), item.get("source"), "ожидает"]
            for c, text in enumerate(values): self.data_table.setItem(r, c, QTableWidgetItem(str(text)))

    def prepare(self) -> None:
        if not self.selection: QMessageBox.information(self, "Сначала шаг 1", "Сначала найдите три режима."); return
        self.stage.setText("Проверяем локальные базы; скачиваем только недостающие минуты…")
        self._start(lambda emit: RegimeStressRunner(self.root, emit, self.cancel_event.is_set, self.pause_event.is_set).prepare_data(self.selection), self._prepared)

    def _prepared(self, value: dict) -> None:
        self.prepared = value; self._show_prepared(value); self.stage.setText("Данные готовы. Можно запускать три stress-теста.")

    def _show_prepared(self, value: dict) -> None:
        for row in range(self.data_table.rowCount()):
            symbol_item = self.data_table.item(row, 1); status = self.data_table.item(row, 7)
            symbol = symbol_item.text() if symbol_item else ""
            if symbol in value.get("datasets", {}):
                d = value["datasets"][symbol]; status.setText(("использована локальная база" if d["reused"] else "скачано") + f" · {d['size_bytes']/1024**2:.1f} MB")
            elif status and "сценариев" in symbol: status.setText("создаются локально во время теста")

    def run_stress(self) -> None:
        if not self.prepared: QMessageBox.information(self, "Сначала шаг 2", "Сначала подготовьте данные."); return
        workers = workers_for_mode(self.hardware, "MAXIMUM"); self.stage.setText(f"Запуск трёх независимых поисков · {workers} CPU-процессов")
        self._start(lambda emit: RegimeStressRunner(self.root, emit, self.cancel_event.is_set, self.pause_event.is_set).run_stress(self.prepared, workers), self._stress_done)

    def _stress_done(self, value: dict) -> None:
        cross = value["cross_regime"]; self.progress.setValue(100); self.stage.setText(f"Готово: {cross['verdict']} · отчёт {value['md_report']}")

    def _progress(self, value: dict) -> None:
        self.progress.setValue(int(value.get("percent", 0))); phase = value.get("phase", "—"); regime = value.get("regime", "—")
        tested = int(value.get("tested", value.get("done", 0))); speed = float(value.get("variants_per_second", 0)); symbol = value.get("symbol", "—")
        self.live_labels.setText(f"Фаза: {phase} · режим: {regime} · данные: {symbol} · проверено: {tested:,} · скорость: {speed:,.0f} вариантов/с · процессов: {value.get('workers','—')}")
        self.stage.setText(str(value.get("stage", "Скачивание" if phase == "DOWNLOAD" else "Работа")))
        if regime in ("BEAR", "SIDEWAYS", "BULL"):
            row = {"BEAR": 0, "SIDEWAYS": 1, "BULL": 2}[regime]; best = value.get("best", "—"); provisional = value.get("best_provisional", {})
            archive = value.get("interesting_archive", {}); cells = [regime, "обрабатывается", best, f"{float(provisional.get('worst_average_net',0))*100:+.3f}%", str(provisional.get("validation_fills", 0)), str(archive.get("total_candidates", archive.get("count", 0)))]
            for col, text in enumerate(cells): self.winner_table.setItem(row, col, QTableWidgetItem(str(text)))
            preview = value.get("equity_preview", {}); self.equity_chart.set_preview(preview, f"{regime} · текущий лидер {best}")

    def toggle_pause(self) -> None:
        if self.pause_event.is_set(): self.pause_event.clear(); self.pause_btn.setText("ПАУЗА"); self.stage.setText("Продолжение…")
        else: self.pause_event.set(); self.pause_btn.setText("ПРОДОЛЖИТЬ"); self.stage.setText("Пауза: checkpoint сохраняется")

    def stop(self) -> None:
        self.cancel_event.set(); self.stage.setText("Безопасная остановка: сохраняем checkpoint…")

    def open_reports(self) -> None:
        path = ready_reports_directory(self.root) / "REGIME-STRESS"; path.mkdir(parents=True, exist_ok=True)
        subprocess.Popen(["explorer", str(path)])

    def _error(self, message: str) -> None:
        self.stage.setText("Остановлено или произошла ошибка. Сохранённые данные не удалены."); QMessageBox.warning(self, "PUMP Regime Stress Lab", message[-2500:])

    def _update_telemetry(self) -> None:
        snap = self.telemetry.snapshot(); fmt = lambda v, unit="%": "нет датчика" if v is None else f"{v:.1f}{unit}"
        self.cpu.setText(f"CPU: {fmt(snap.cpu_percent)} · температура: {fmt(snap.cpu_temperature_c, '°C')}")
        self.ram.setText(f"RAM: {fmt(snap.ram_percent)}")
        memory = "—" if snap.gpu_memory_used_mb is None else f"{snap.gpu_memory_used_mb}/{snap.gpu_memory_total_mb} MB"
        self.gpu.setText(f"GPU {snap.gpu_name}: {fmt(snap.gpu_percent)} · {fmt(snap.gpu_temperature_c, '°C')} · VRAM {memory}")
        self.device.setText(f"Точный перебор: {snap.exact_replay_device} · GPU алгоритмом не используется")


def main() -> int:
    app = QApplication(sys.argv); app.setApplicationName("PUMP Regime Stress Lab")
    window = RegimeLabWindow(); window.show()
    if os.environ.get("PUMP_REGIME_AUTOTEST") == "ui": QTimer.singleShot(1200, app.quit)
    return app.exec()


if __name__ == "__main__":
    raise SystemExit(main())
