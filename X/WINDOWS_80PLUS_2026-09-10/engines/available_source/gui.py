from __future__ import annotations

import hashlib
import json
import os
import sys
import threading
import traceback
import multiprocessing
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Callable

from PySide6.QtCore import QObject, QRunnable, QThreadPool, QTimer, Qt, Signal, Slot
from PySide6.QtCore import QPointF
from PySide6.QtGui import QColor, QFont, QPainter, QPen, QPolygonF
from PySide6.QtWidgets import (
    QApplication, QComboBox, QDateTimeEdit, QFileDialog, QFormLayout, QFrame, QGridLayout,
    QHBoxLayout, QHeaderView, QLabel, QListWidget, QListWidgetItem, QMainWindow, QMessageBox,
    QDoubleSpinBox, QPlainTextEdit, QProgressBar, QPushButton, QSplitter, QStackedWidget, QTableWidget,
    QTableWidgetItem, QVBoxLayout, QWidget, QScrollArea, QSizePolicy,
)

from .automation import AutomationConfig, AutomatedResearchRunner, PERIOD_DAYS, ready_reports_directory
from .adaptive_optimizer import optimize_adaptive
from .baseline_x import replay_economy, replay_vwap_canary
from .catalog import DuckDbCatalog
from .data_manager import DataManager
from .demo import demo_candles
from .domain import DatasetManifest
from .io import read_csv, read_jsonl, write_jsonl
from .holdout import seal_immutable_holdout
from .hardware import detect_hardware, save_hardware_profile, workers_for_mode
from .optimizer import ResearchCancelled, optimize_variants, write_optimizer_report
from .reporting import write_replay_report
from .scenarios import run_synthetic_school
from .validation import validate_candles
from . import __version__


NAV = [
    "Главная панель", "Данные", "Качество данных", "Сценарии", "Конструктор эксперимента",
    "Фабрика вариантов", "Сравнение моделей", "Champion / Challenger", "Журналы и отчёты",
    "Экспорт в Android", "Настройки производительности",
]
SIMPLE_NAV = [(0, "ИССЛЕДОВАНИЕ"), (1, "БАЗЫ ДАННЫХ")]


def app_root() -> Path:
    override = os.environ.get("PUMP_LAB_HOME")
    if override:
        root = Path(override)
    elif getattr(sys, "frozen", False):
        legacy = Path.home() / "Documents" / "PUMP Research Lab"
        # Preserve already downloaded market history. New portable installs
        # without an existing workspace keep everything beside the EXE.
        root = legacy if (legacy / "data").exists() else Path(sys.executable).resolve().parent / "PUMP Research Lab Workspace"
    else:
        root = Path.home() / "Documents" / "PUMP Research Lab"
    for name in ("data", "models", "checkpoints", "reports", "logs", "exports", "experiments"):
        (root / name).mkdir(parents=True, exist_ok=True)
    return root


class WorkerSignals(QObject):
    result = Signal(object)
    progress = Signal(object)
    error = Signal(str)
    finished = Signal()


class Worker(QRunnable):
    def __init__(self, fn: Callable, with_progress: bool = False) -> None:
        super().__init__()
        self.fn = fn
        self.with_progress = with_progress
        self.signals = WorkerSignals()

    @Slot()
    def run(self) -> None:
        try:
            result = self.fn(self.signals.progress.emit) if self.with_progress else self.fn()
            self.signals.result.emit(result)
        except Exception:
            self.signals.error.emit(traceback.format_exc())
        finally:
            self.signals.finished.emit()


class Card(QFrame):
    def __init__(self, title: str, body: QWidget | None = None) -> None:
        super().__init__()
        self.setObjectName("card")
        layout = QVBoxLayout(self)
        heading = QLabel(title)
        heading.setObjectName("cardTitle")
        heading.setWordWrap(True)
        heading.setSizePolicy(QSizePolicy.Policy.Ignored, QSizePolicy.Policy.Preferred)
        layout.addWidget(heading)
        if body is not None:
            layout.addWidget(body)


class EquityChart(QWidget):
    def __init__(self) -> None:
        super().__init__()
        self.values = [1.0]
        self.currency = "USDT"
        self.caption = "Капитал"
        self.setMinimumHeight(170)

    def set_trades(self, trades: list[dict], initial_capital: float = 1.0, currency: str = "USDT") -> None:
        value = initial_capital
        self.currency = currency
        self.values = [value]
        for trade in trades:
            value *= 1.0 + float(trade.get("net_return", 0.0))
            self.values.append(value)
        self.update()

    def paintEvent(self, _event) -> None:
        painter = QPainter(self)
        painter.setRenderHint(QPainter.RenderHint.Antialiasing)
        painter.fillRect(self.rect(), QColor("#08131c"))
        painter.setPen(QPen(QColor("#20384b"), 1))
        painter.drawLine(12, self.height() // 2, self.width() - 12, self.height() // 2)
        if len(self.values) < 2:
            painter.setPen(QColor("#89a3b6")); painter.drawText(self.rect(), Qt.AlignmentFlag.AlignCenter, "Кривая капитала появится после исследования")
            return
        low, high = min(self.values), max(self.values)
        spread = max(high - low, 1e-9)
        points = QPolygonF()
        for index, value in enumerate(self.values):
            x = 12 + index * (self.width() - 24) / max(len(self.values) - 1, 1)
            y = 12 + (high - value) * (self.height() - 24) / spread
            points.append(QPointF(x, y))
        color = QColor("#39d98a" if self.values[-1] >= self.values[0] else "#ff667a")
        painter.setPen(QPen(color, 3)); painter.drawPolyline(points)
        painter.setPen(QColor("#dfeef7"))
        painter.drawText(18, 25, f"Старт: {self.values[0]:,.2f} {self.currency}")
        painter.drawText(max(18, self.width() - 220), 25, f"Итог: {self.values[-1]:,.2f} {self.currency}")
        painter.setPen(QColor("#89a3b6"))
        painter.drawText(18, self.height() - 12, f"Мин.: {low:,.2f}   Макс.: {high:,.2f}")


def text_label(text: str, css: str = "") -> QLabel:
    value = QLabel(text)
    value.setWordWrap(True)
    value.setSizePolicy(QSizePolicy.Policy.Ignored, QSizePolicy.Policy.Preferred)
    if css:
        value.setStyleSheet(css)
    return value


def run_full_self_test(root: Path) -> dict:
    work = root / "data" / "self_test"
    raw = work / "raw" / "demo.jsonl"
    rows = demo_candles()
    write_jsonl(raw, rows)
    report = validate_candles(rows)
    manifest = DatasetManifest.create("self-test", "PUMPUSDT", "1m", report, "synthetic", "synthetic")
    parquet = work / "normalized" / "demo.parquet"
    DuckDbCatalog(work / "catalog" / "pump.duckdb").register_candles(rows, manifest, parquet)
    result = replay_economy(rows)
    reports = write_replay_report(result, root / "reports" / "self_test", "synthetic")
    return {"status": "PASS", "metrics": result.to_dict()["metrics"], "report": str(reports[1])}


class ResearchLab(QMainWindow):
    def __init__(self) -> None:
        super().__init__()
        self.root = app_root()
        self.pool = QThreadPool.globalInstance()
        self.workers: set[Worker] = set()
        self.factory_cancel_event = threading.Event()
        self.factory_pause_event = threading.Event()
        self.auto_cancel_event = threading.Event()
        self.auto_pause_event = threading.Event()
        self.auto_active_phase: str | None = None
        self.hardware = detect_hardware()
        self.setWindowTitle(f"PUMP Research Lab V{__version__} — Simulation")
        self.resize(1380, 860)
        self.setMinimumSize(1050, 680)
        self.pages = QStackedWidget()
        self.nav = QListWidget()
        self.nav.setFixedWidth(255)
        self.nav.setObjectName("nav")
        self.nav.setHorizontalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOff)
        self._build_pages()
        self.simple_navigation = True
        self.nav_page_indices: list[int] = []
        self.nav.currentRowChanged.connect(self._navigation_changed)
        self._populate_navigation()
        sidebar = QWidget()
        sidebar_layout = QVBoxLayout(sidebar)
        brand = QLabel(f"PUMP Research Lab\nV{__version__}")
        brand.setObjectName("brand")
        sidebar_layout.addWidget(brand)
        sidebar_layout.addWidget(text_label("● SIMULATION / PAPER\nREAL ORDERS OFF", "color:#39d98a;font-size:11px"))
        sidebar_layout.addSpacing(8)
        sidebar_layout.addWidget(self.nav, 1)
        close = QPushButton("Закрыть программу")
        close.setObjectName("secondary")
        close.clicked.connect(self.close)
        sidebar_layout.addWidget(close)
        splitter = QSplitter()
        splitter.addWidget(sidebar)
        splitter.addWidget(self.pages)
        splitter.setStretchFactor(1, 1)
        self.setCentralWidget(splitter)
        self.statusBar().showMessage(f"Хранилище: {self.root}")
        self._apply_style()
        QTimer.singleShot(100, self.refresh_catalog)

    def _populate_navigation(self, target_page: int = 0) -> None:
        self.nav.blockSignals(True); self.nav.clear()
        if self.simple_navigation:
            items = SIMPLE_NAV
        else:
            items = list(enumerate(NAV))
        self.nav_page_indices = [page_index for page_index, _name in items]
        for number, (_page_index, name) in enumerate(items, 1):
            self.nav.addItem(QListWidgetItem(f"{number:02d}   {name}"))
        row = self.nav_page_indices.index(target_page) if target_page in self.nav_page_indices else 0
        self.nav.setCurrentRow(row); self.pages.setCurrentIndex(self.nav_page_indices[row])
        self.nav.blockSignals(False)

    def _navigation_changed(self, row: int) -> None:
        if 0 <= row < len(self.nav_page_indices):
            self.pages.setCurrentIndex(self.nav_page_indices[row])

    def go_to_page(self, page_index: int) -> None:
        if page_index not in self.nav_page_indices:
            self.nav.setCurrentRow(0)
            return
        self.nav.setCurrentRow(self.nav_page_indices.index(page_index))

    def toggle_navigation_mode(self) -> None:
        self._populate_navigation(self.pages.currentIndex() if self.pages.currentIndex() in (0, 1) else 0)

    def _build_pages(self) -> None:
        self.pages.addWidget(self._dashboard())
        self.pages.addWidget(self._data_page())
        self.pages.addWidget(self._quality_page())
        self.pages.addWidget(self._scenarios_page())
        self.pages.addWidget(self._experiment_page())
        self.pages.addWidget(self._training_page())
        self.pages.addWidget(self._comparison_page())
        self.pages.addWidget(self._champion_page())
        self.pages.addWidget(self._reports_page())
        self.pages.addWidget(self._export_page())
        self.pages.addWidget(self._settings_page())

    def _page(self, title: str, subtitle: str = "") -> tuple[QWidget, QVBoxLayout]:
        content = QWidget()
        content.setSizePolicy(QSizePolicy.Policy.Ignored, QSizePolicy.Policy.Preferred)
        outer = QVBoxLayout(content)
        outer.setContentsMargins(26, 22, 26, 22)
        heading = QLabel(title)
        heading.setObjectName("pageTitle")
        heading.setWordWrap(True)
        outer.addWidget(heading)
        if subtitle:
            outer.addWidget(text_label(subtitle, "color:#89a3b6"))
        outer.addSpacing(10)
        page = QScrollArea()
        page.setWidgetResizable(True)
        page.setFrameShape(QFrame.Shape.NoFrame)
        page.setHorizontalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOff)
        page.setWidget(content)
        return page, outer

    def _dashboard(self) -> QWidget:
        page, outer = self._page("Автоматическое исследование", "Два понятных этапа: сначала фиксируем базу, затем отдельно запускаем вычисление")
        auto = QWidget(); auto_grid = QGridLayout(auto)
        self.auto_period = QComboBox()
        self.auto_period.addItem("1 месяц", 1); self.auto_period.addItem("3 месяца", 3); self.auto_period.addItem("6 месяцев", 6)
        self.auto_period.currentIndexChanged.connect(self.update_auto_estimate)
        self.auto_capital = QDoubleSpinBox(); self.auto_capital.setRange(1, 100_000_000); self.auto_capital.setDecimals(2); self.auto_capital.setValue(1000)
        self.auto_currency = QComboBox(); self.auto_currency.addItems(["EUR", "USDT"])
        capital_box = QWidget(); capital_layout = QHBoxLayout(capital_box); capital_layout.setContentsMargins(0, 0, 0, 0); capital_layout.addWidget(self.auto_capital, 1); capital_layout.addWidget(self.auto_currency)
        self.auto_search_mode = QComboBox()
        self.auto_search_mode.addItem("Адаптивно — до сходимости или остановки", "ADAPTIVE")
        self.auto_search_mode.currentIndexChanged.connect(self.update_auto_estimate)
        self.auto_resource_mode = QComboBox()
        self.auto_resource_mode.addItem("Быстро", "FAST")
        self.auto_resource_mode.addItem("Максимум", "MAXIMUM")
        self.auto_resource_mode.addItem("Фоновый", "BACKGROUND")
        self.auto_resource_mode.setCurrentIndex(1)
        self.auto_resource_mode.currentIndexChanged.connect(self.update_auto_estimate)
        self.auto_estimate = text_label("")
        self.auto_prepare_button = QPushButton("1. ПОДГОТОВИТЬ / ВЫБРАТЬ БАЗУ")
        self.auto_prepare_button.setObjectName("primaryLarge"); self.auto_prepare_button.clicked.connect(self.prepare_automatic_data)
        self.auto_data_progress = QProgressBar(); self.auto_data_progress.setRange(0, 100); self.auto_data_progress.setValue(0)
        self.auto_data_stage = text_label("База ещё не выбрана", "color:#ffbf69;font-size:15px")
        self.auto_run_button = QPushButton("2. ЗАПУСТИТЬ РАСЧЁТ ПО ЭТОЙ БАЗЕ")
        self.auto_run_button.setObjectName("primaryLarge"); self.auto_run_button.clicked.connect(self.run_automatic_research)
        self.auto_run_button.setEnabled(False)
        self.auto_stop_button = QPushButton("ОСТАНОВИТЬ"); self.auto_stop_button.setObjectName("warning"); self.auto_stop_button.setEnabled(False); self.auto_stop_button.clicked.connect(self.stop_automatic_research)
        self.auto_pause_button = QPushButton("ПАУЗА"); self.auto_pause_button.setObjectName("secondary"); self.auto_pause_button.setEnabled(False); self.auto_pause_button.clicked.connect(self.pause_automatic_research)
        self.auto_resume_button = QPushButton("ПРОДОЛЖИТЬ"); self.auto_resume_button.setObjectName("secondary"); self.auto_resume_button.setEnabled(False); self.auto_resume_button.clicked.connect(self.resume_automatic_research)
        self.auto_progress = QProgressBar(); self.auto_progress.setRange(0, 100); self.auto_progress.setValue(0)
        self.auto_stage = text_label("Готово к запуску", "color:#89a3b6;font-size:15px")
        self.auto_result = text_label("После запуска здесь появятся итоговый процент, результат для выбранного капитала и вывод.", "font-size:17px")
        open_reports = QPushButton("ОТКРЫТЬ ОТЧЁТЫ"); open_reports.setObjectName("secondary"); open_reports.clicked.connect(self.open_ready_reports)
        auto_grid.addWidget(QLabel("Период данных"), 0, 0); auto_grid.addWidget(QLabel("Стартовый капитал"), 0, 1)
        auto_grid.addWidget(self.auto_period, 1, 0); auto_grid.addWidget(capital_box, 1, 1)
        auto_grid.addWidget(QLabel("Как искать"), 2, 0); auto_grid.addWidget(QLabel("Нагрузка на компьютер"), 2, 1)
        auto_grid.addWidget(self.auto_search_mode, 3, 0); auto_grid.addWidget(self.auto_resource_mode, 3, 1)
        auto_grid.addWidget(self.auto_prepare_button, 4, 0, 1, 2)
        auto_grid.addWidget(self.auto_data_progress, 5, 0, 1, 2); auto_grid.addWidget(self.auto_data_stage, 6, 0, 1, 2)
        auto_grid.addWidget(self.auto_run_button, 7, 0, 1, 2)
        auto_actions = QHBoxLayout(); auto_actions.addWidget(self.auto_pause_button); auto_actions.addWidget(self.auto_resume_button); auto_actions.addWidget(self.auto_stop_button); auto_actions.addWidget(open_reports)
        auto_grid.addLayout(auto_actions, 8, 0, 1, 2)
        auto_grid.addWidget(self.auto_estimate, 9, 0, 1, 2)
        auto_grid.addWidget(self.auto_progress, 10, 0, 1, 2); auto_grid.addWidget(self.auto_stage, 11, 0, 1, 2)
        auto_grid.addWidget(self.auto_result, 12, 0, 1, 2)
        self.auto_best_parameters = text_label("Параметры текущего лидера появятся во время расчёта.", "font-size:14px;color:#77c7ff")
        auto_grid.addWidget(self.auto_best_parameters, 13, 0, 1, 2)
        auto_grid.setColumnStretch(0, 1); auto_grid.setColumnStretch(1, 1)
        outer.addWidget(Card("БАЗА И РАСЧЁТ РАЗДЕЛЕНЫ — скрытого скачивания во время перебора нет", auto))
        self.checkpoint_notice = text_label("Незавершённых исследований нет.", "font-size:15px;color:#89a3b6")
        outer.addWidget(Card("ВОССТАНОВЛЕНИЕ ПОСЛЕ ПЕРЕЗАГРУЗКИ", self.checkpoint_notice))
        self.auto_capital_chart = EquityChart()
        outer.addWidget(Card("ЧТО ПРОИСХОДИТ С ВАШИМИ 1 000 — историческая кривая после комиссий", self.auto_capital_chart))
        self.update_auto_estimate()
        grid = QGridLayout()
        self.dataset_metric = text_label("0", "font-size:34px;font-weight:700")
        self.report_metric = text_label("0", "font-size:34px;font-weight:700")
        grid.addWidget(Card("Локальные datasets", self.dataset_metric), 0, 0)
        grid.addWidget(Card("Отчёты исследований", self.report_metric), 0, 1)
        grid.addWidget(Card("Защищённый X-canary", text_label("1772 → 974 → 318\n32,6489%", "font-size:22px;color:#39d98a")), 0, 2)
        self.hardware_metric = text_label(
            f"{self.hardware.physical_cores} ядер / {self.hardware.logical_processors} потоков\n"
            f"{self.hardware.ram_gb:.0f} GB RAM\n{self.hardware.gpu} ({self.hardware.gpu_vram_mb} MB)\n"
            "Точный replay: CPU; GPU не используется",
            "font-size:15px;color:#77c7ff",
        )
        grid.addWidget(Card("Компьютер подключён", self.hardware_metric), 0, 3)
        state = text_label(
            "✓ Data Manager и gap repair\n✓ Причинный AUTO X replay\n✓ Parquet / DuckDB / manifests\n"
            "✓ 50 синтетических сценариев\n✓ TRAIN / VALIDATION / TEST и закрытый holdout\n◷ Adaptive Router — только после доказанного кандидата\n"
            "✕ Реальные ордера — запрещены",
            "font-size:16px;line-height:1.5",
        )
        grid.addWidget(Card("Состояние системы", state), 1, 0, 1, 3)
        quick = QWidget(); q = QVBoxLayout(quick)
        test = QPushButton("Полный внутренний тест"); test.clicked.connect(self.run_self_test)
        data = QPushButton("Перейти к данным"); data.setObjectName("secondary"); data.clicked.connect(lambda: self.go_to_page(1))
        archive = QPushButton("ОТКРЫТЬ НАКОПЛЕННЫЕ ВАРИАНТЫ"); archive.clicked.connect(self.open_interesting_candidates)
        q.addWidget(archive); q.addWidget(data); q.addWidget(test); q.addStretch()
        grid.addWidget(Card("Быстрые действия", quick), 1, 3)
        outer.addLayout(grid); outer.addStretch()
        return page

    def _data_page(self) -> QWidget:
        page, outer = self._page("Данные", "Скачивание, актуализация и регистрация публичных рыночных данных")
        location = QWidget(); location_row = QHBoxLayout(location)
        self.data_location = text_label(str(self.root / "data"), "font-size:16px;color:#39d98a")
        open_data = QPushButton("ОТКРЫТЬ ПАПКУ С ДАННЫМИ"); open_data.setObjectName("secondary"); open_data.clicked.connect(self.open_data_folder)
        open_all = QPushButton("ОТКРЫТЬ ВСЮ РАБОЧУЮ ПАПКУ"); open_all.setObjectName("secondary"); open_all.clicked.connect(self.open_workspace_folder)
        location_row.addWidget(self.data_location, 1); location_row.addWidget(open_data); location_row.addWidget(open_all)
        outer.addWidget(Card("ВСЕ СКАЧАННЫЕ ДАННЫЕ НАХОДЯТСЯ ЗДЕСЬ", location))
        form_box = QWidget(); form = QGridLayout(form_box)
        self.symbol = QComboBox(); self.symbol.addItems(["PUMPUSDT", "BTCUSDT", "SOLUSDT"])
        self.data_start = QDateTimeEdit(); self.data_start.setCalendarPopup(True); self.data_start.setDisplayFormat("yyyy-MM-dd HH:mm 'UTC'")
        self.data_end = QDateTimeEdit(); self.data_end.setCalendarPopup(True); self.data_end.setDisplayFormat("yyyy-MM-dd HH:mm 'UTC'")
        self.data_end.setDateTime(datetime.now()); self.data_start.setDateTime(datetime.now() - timedelta(days=7))
        form.addWidget(QLabel("Источник"), 0, 0); form.addWidget(QLabel("Символ"), 0, 1); form.addWidget(QLabel("Начало"), 0, 2); form.addWidget(QLabel("Конец"), 0, 3)
        source = QComboBox(); source.addItem("Binance Spot public API")
        form.addWidget(source, 1, 0); form.addWidget(self.symbol, 1, 1); form.addWidget(self.data_start, 1, 2); form.addWidget(self.data_end, 1, 3)
        buttons = QHBoxLayout()
        download = QPushButton("СКАЧАТЬ ВЫБРАННЫЙ ПЕРИОД"); download.clicked.connect(self.download_data)
        refresh = QPushButton("ОБНОВИТЬ КАТАЛОГ"); refresh.setObjectName("secondary"); refresh.clicked.connect(self.refresh_catalog)
        buttons.addWidget(download); buttons.addWidget(refresh); buttons.addStretch()
        form.addLayout(buttons, 2, 0, 1, 4)
        presets = QHBoxLayout(); presets.addWidget(QLabel("Быстрый период:"))
        for months in (1, 3, 6):
            preset = QPushButton(f"{months} мес."); preset.setObjectName("secondary"); preset.clicked.connect(lambda _checked=False, value=months: self.set_data_period(value)); presets.addWidget(preset)
        presets.addStretch(); form.addLayout(presets, 3, 0, 1, 4)
        outer.addWidget(Card("Data Manager", form_box))
        self.datasets_table = QTableWidget(0, 8); self.datasets_table.setHorizontalHeaderLabels(["Символ", "Период UTC", "Строк", "Размер", "Актуальность", "Файл", "Источник", "SHA-256"])
        self.datasets_table.horizontalHeader().setSectionResizeMode(QHeaderView.ResizeMode.Stretch)
        self.datasets_table.cellDoubleClicked.connect(self.open_dataset_location)
        outer.addWidget(Card("СКАЧАННЫЕ БАЗЫ — программа выбирает последнюю автоматически", self.datasets_table), 1)
        self.data_progress = QProgressBar(); self.data_progress.setRange(0, 100)
        self.data_log = QPlainTextEdit(); self.data_log.setReadOnly(True); self.data_log.setMaximumHeight(150)
        status = QWidget(); sl = QVBoxLayout(status); sl.addWidget(self.data_progress); sl.addWidget(self.data_log)
        outer.addWidget(Card("Задание", status))
        return page

    def _quality_page(self) -> QWidget:
        page, outer = self._page("Качество данных", "Пропуски, дубликаты, OHLC, временные границы и идентичность набора")
        self.quality_path = QLabel("Последняя база PUMP будет выбрана автоматически"); self.quality_path.setTextInteractionFlags(Qt.TextInteractionFlag.TextSelectableByMouse)
        choose = QPushButton("Открыть папку данных"); choose.clicked.connect(self.open_data_folder)
        validate = QPushButton("ПРОВЕРИТЬ КАЧЕСТВО"); validate.clicked.connect(self.validate_selected)
        repair = QPushButton("ИСПРАВИТЬ ПРОПУСКИ"); repair.setObjectName("warning"); repair.clicked.connect(self.repair_selected)
        row = QHBoxLayout(); row.addWidget(choose); row.addWidget(validate); row.addWidget(repair); row.addWidget(self.quality_path, 1)
        outer.addLayout(row)
        metrics = QHBoxLayout()
        self.gaps_metric = text_label("—", "font-size:30px"); self.dupes_metric = text_label("—", "font-size:30px"); self.invalid_metric = text_label("—", "font-size:30px")
        metrics.addWidget(Card("Пропуски", self.gaps_metric)); metrics.addWidget(Card("Дубликаты", self.dupes_metric)); metrics.addWidget(Card("Повреждения", self.invalid_metric))
        outer.addLayout(metrics)
        self.quality_log = QPlainTextEdit(); self.quality_log.setReadOnly(True)
        outer.addWidget(Card("Подробный отчёт", self.quality_log), 1)
        return page

    def _scenarios_page(self) -> QWidget:
        page, outer = self._page("Сценарии", "Детерминированные и стрессовые проверки механики исполнения")
        cards = QHBoxLayout()
        synthetic = QWidget(); s = QVBoxLayout(synthetic); s.addWidget(text_label("Закрытые свечи, fee-aware TP/STOP, TIME exit, отчёт.")); b = QPushButton("Запустить синтетику"); b.clicked.connect(self.run_self_test); s.addWidget(b)
        cards.addWidget(Card("Базовый сценарий", synthetic))
        cards.addWidget(Card("Консервативный intrabar", text_label("STOP проверяется раньше TP.\nAdverse slippage: 0,08%.\nСтатус: включено в ядре.")))
        cards.addWidget(Card("Исторический L2", text_label("Бесплатный архив не найден.\nВыдуманные данные запрещены.\nСледующий модуль: live collector.")))
        outer.addLayout(cards)
        self.scenario_log = QPlainTextEdit(); self.scenario_log.setReadOnly(True)
        outer.addWidget(Card("Результаты", self.scenario_log), 1)
        return page

    def _experiment_page(self) -> QWidget:
        page, outer = self._page("Конструктор эксперимента", "Каждый результат сохраняет dataset, параметры, комиссии и правила исполнения")
        self.replay_path = QLabel("Последняя база PUMP будет выбрана автоматически"); self.replay_path.setTextInteractionFlags(Qt.TextInteractionFlag.TextSelectableByMouse)
        choose = QPushButton("Открыть папку данных"); choose.clicked.connect(self.open_data_folder)
        self.strategy = QComboBox(); self.strategy.addItems(["AUTO X ECONOMY", "VWAP/T32 CANARY", "NO_TRADE"])
        run = QPushButton("ЗАПУСТИТЬ ИССЛЕДОВАНИЕ"); run.clicked.connect(self.run_replay)
        top = QHBoxLayout(); top.addWidget(choose); top.addWidget(self.replay_path, 1); top.addWidget(self.strategy); top.addWidget(run)
        outer.addLayout(top)
        params = QHBoxLayout()
        for title, value in (("Fee", "0,21% / сторона"), ("TP / Stop", "+2,5% / −1,2% NET"), ("Hold", "120 минут"), ("Лимит", "2 входа / UTC день")):
            params.addWidget(Card(title, text_label(value, "font-size:18px")))
        outer.addLayout(params)
        results = QHBoxLayout()
        self.result_trades = text_label("—", "font-size:26px;font-weight:700")
        self.result_winrate = text_label("—", "font-size:26px;font-weight:700")
        self.result_net = text_label("—", "font-size:26px;font-weight:700")
        self.result_pf = text_label("—", "font-size:26px;font-weight:700")
        for title, widget in (("Сделки", self.result_trades), ("Win rate", self.result_winrate), ("Compound NET", self.result_net), ("Profit Factor", self.result_pf)):
            results.addWidget(Card(title, widget))
        outer.addLayout(results)
        self.equity_chart = EquityChart()
        self.trades_table = QTableWidget(0, 6)
        self.trades_table.setHorizontalHeaderLabels(["Signal UTC", "Entry", "Exit", "Причина", "NET", "Цена входа"])
        self.trades_table.horizontalHeader().setSectionResizeMode(QHeaderView.ResizeMode.Stretch)
        split = QSplitter(Qt.Orientation.Horizontal); split.addWidget(self.equity_chart); split.addWidget(self.trades_table); split.setStretchFactor(1, 1)
        outer.addWidget(Card("Кривая капитала и сделки", split), 2)
        self.replay_log = QPlainTextEdit(); self.replay_log.setReadOnly(True)
        self.replay_log.setMaximumHeight(130)
        outer.addWidget(Card("Вывод, путь к отчёту и ошибки", self.replay_log))
        return page

    def _training_page(self) -> QWidget:
        page, outer = self._page("Фабрика вариантов", "Программа сама перебирает параметры, проверяет устойчивость и не использует TEST для подбора")
        controls = QWidget(); grid = QGridLayout(controls)
        self.factory_path = QLabel("Будет выбран последний локальный PUMP dataset")
        self.factory_path.setTextInteractionFlags(Qt.TextInteractionFlag.TextSelectableByMouse)
        self.factory_path.setWordWrap(True)
        self.factory_path.setSizePolicy(QSizePolicy.Policy.Ignored, QSizePolicy.Policy.Preferred)
        choose = QPushButton("ОТКРЫТЬ ПАПКУ ДАННЫХ"); choose.setObjectName("secondary"); choose.clicked.connect(self.open_data_folder)
        self.factory_mode = QComboBox()
        self.factory_mode.addItem("Адаптивный поиск до сходимости", "ADAPTIVE")
        self.factory_mode.currentIndexChanged.connect(self.update_factory_estimate)
        self.factory_run = QPushButton("НАЧАТЬ АВТОМАТИЧЕСКИЙ ПОИСК"); self.factory_run.setObjectName("primaryLarge"); self.factory_run.clicked.connect(self.run_variant_factory)
        self.factory_stop = QPushButton("ОСТАНОВИТЬ"); self.factory_stop.setObjectName("warning"); self.factory_stop.setEnabled(False); self.factory_stop.clicked.connect(self.stop_variant_factory)
        self.factory_pause = QPushButton("ПАУЗА"); self.factory_pause.setObjectName("secondary"); self.factory_pause.setEnabled(False); self.factory_pause.clicked.connect(self.pause_variant_factory)
        self.factory_resume = QPushButton("ПРОДОЛЖИТЬ"); self.factory_resume.setObjectName("secondary"); self.factory_resume.setEnabled(False); self.factory_resume.clicked.connect(self.resume_variant_factory)
        self.factory_estimate = text_label("", "color:#ffbf69")
        grid.addWidget(QLabel("База данных"), 0, 0); grid.addWidget(self.factory_path, 0, 1)
        grid.addWidget(choose, 1, 1, 1, 1, Qt.AlignmentFlag.AlignLeft)
        grid.addWidget(QLabel("Способ поиска"), 2, 0); grid.addWidget(self.factory_mode, 2, 1)
        grid.addWidget(self.factory_run, 3, 0, 1, 2)
        actions = QHBoxLayout(); actions.addWidget(self.factory_pause); actions.addWidget(self.factory_resume); actions.addWidget(self.factory_stop)
        grid.addWidget(self.factory_estimate, 4, 0, 1, 2); grid.addLayout(actions, 5, 0, 1, 2)
        grid.setColumnStretch(1, 1)
        self.update_factory_estimate()
        outer.addWidget(Card("БАЗА ВЫБИРАЕТСЯ АВТОМАТИЧЕСКИ: выберите мощность → нажмите START", controls))
        self.factory_progress = QProgressBar(); self.factory_progress.setRange(0, 100)
        self.factory_stage = text_label("Готово к запуску. Защищённый baseline X и NO_TRADE сравниваются автоматически.", "font-size:16px")
        status = QWidget(); status_layout = QVBoxLayout(status); status_layout.addWidget(self.factory_progress); status_layout.addWidget(self.factory_stage)
        outer.addWidget(Card("Ход исследования", status))
        self.factory_summary = text_label("Результат ещё не получен.", "font-size:18px")
        summary_box = QWidget(); summary_layout = QVBoxLayout(summary_box); summary_layout.addWidget(self.factory_summary)
        self.factory_report_button = QPushButton("ОТКРЫТЬ ГОТОВЫЙ ОТЧЁТ"); self.factory_report_button.setObjectName("secondary"); self.factory_report_button.setEnabled(False); self.factory_report_button.clicked.connect(self.open_factory_report)
        summary_layout.addWidget(self.factory_report_button, 0, Qt.AlignmentFlag.AlignLeft)
        outer.addWidget(Card("Понятный вывод", summary_box))
        self.factory_table = QTableWidget(0, 10)
        self.factory_table.setHorizontalHeaderLabels(["#", "ID", "TP", "STOP", "Hold", "Частота входов", "BTC / SOL", "Худший Avg NET", "PF A / B", "Сделки"])
        self.factory_table.horizontalHeader().setSectionResizeMode(QHeaderView.ResizeMode.Stretch)
        outer.addWidget(Card("10 лучших по двум validation-периодам", self.factory_table), 1)
        return page

    def _comparison_page(self) -> QWidget:
        page, outer = self._page("Сравнение моделей", "Никаких выводов о прибыльности только по train-участку")
        table = QTableWidget(3, 6); table.setHorizontalHeaderLabels(["Кандидат", "Статус", "Win rate", "Avg NET", "PF", "Контроль"])
        values = [["AUTO X ECONOMY", "ПРОВЕРЕН", "42,27%", "−0,0062%", "0,9911", "60 дней"], ["VWAP/T32", "CANARY OK", "32,65%", "—", "—", "1772/974/318"], ["NO_TRADE", "BASELINE", "—", "0", "—", "всегда"]]
        for r, row in enumerate(values):
            for c, value in enumerate(row): table.setItem(r, c, QTableWidgetItem(value))
        table.horizontalHeader().setSectionResizeMode(QHeaderView.ResizeMode.Stretch)
        outer.addWidget(table, 1)
        outer.addWidget(text_label("AUTO X ECONOMY на контрольном окне слегка убыточен. Отрицательный результат сохранён честно.", "color:#ffbf69;font-size:16px"))
        return page

    def _champion_page(self) -> QWidget:
        page, outer = self._page("Champion / Challenger", "Продвижение возможно только после независимой проверки")
        row = QHBoxLayout(); row.addWidget(Card("Champion", text_label("NO_TRADE\nБезопасный замороженный baseline", "font-size:22px;color:#39d98a"))); row.addWidget(Card("Challenger", text_label("Нет допущенного кандидата\nAUTO X: PF < 1", "font-size:22px;color:#ffbf69")))
        outer.addLayout(row); outer.addWidget(Card("Promotion gate", text_label("walk-forward → immutable holdout → stress → сравнение с X/NO_TRADE → simulation → shadow"))); outer.addStretch(); return page

    def _reports_page(self) -> QWidget:
        page, outer = self._page("Журналы и отчёты", "Воспроизводимые результаты, ошибки и provenance")
        refresh = QPushButton("Обновить список"); refresh.clicked.connect(self.refresh_catalog); outer.addWidget(refresh, 0, Qt.AlignmentFlag.AlignLeft)
        self.reports_table = QTableWidget(0, 4); self.reports_table.setHorizontalHeaderLabels(["Отчёт", "Изменён", "Размер", "Путь"]); self.reports_table.horizontalHeader().setSectionResizeMode(QHeaderView.ResizeMode.Stretch)
        self.reports_table.cellDoubleClicked.connect(self.open_report)
        outer.addWidget(self.reports_table, 1)
        return page

    def _export_page(self) -> QWidget:
        page, outer = self._page("Экспорт в Android", "Формат .pumpmodel и parity fixtures")
        outer.addWidget(Card("Экспорт заблокирован", text_label("Нет Challenger, прошедшего holdout. Android PUMP не изменялся.\nБудущий пакет: model + feature schema + normalization + risk limits + validation + checksum.", "font-size:17px;color:#ffbf69")))
        button = QPushButton("ЭКСПОРТИРОВАТЬ .pumpmodel"); button.setEnabled(False); outer.addWidget(button, 0, Qt.AlignmentFlag.AlignLeft); outer.addStretch(); return page

    def _settings_page(self) -> QWidget:
        page, outer = self._page("Настройки производительности", "Хранилище, ресурсы и диагностика")
        form_widget = QWidget(); form = QFormLayout(form_widget)
        form.addRow("Папка данных", text_label(str(self.root)))
        form.addRow("Процессор", text_label(self.hardware.cpu))
        form.addRow("Ядра / потоки", text_label(f"{self.hardware.physical_cores} / {self.hardware.logical_processors}"))
        form.addRow("Оперативная память", text_label(f"{self.hardware.ram_gb:.1f} GB"))
        form.addRow("Видеокарта", text_label(f"{self.hardware.gpu}, {self.hardware.gpu_vram_mb} MB; CUDA: {'да' if self.hardware.cuda_available else 'нет'}"))
        form.addRow("Автоматически рекомендовано", text_label(f"{self.hardware.recommended_workers} процессов для точного replay"))
        form.addRow("GPU-задачи", text_label("Подготовка признаков и будущие модели; точное ветвистое исполнение — CPU"))
        form.addRow("Режим", text_label("Детерминированный / simulation / реальные ордера отключены"))
        outer.addWidget(Card("Система", form_widget)); button = QPushButton("Полная диагностика"); button.clicked.connect(self.run_self_test); outer.addWidget(button, 0, Qt.AlignmentFlag.AlignLeft); outer.addStretch(); return page

    def _run(
        self,
        label: str,
        fn: Callable,
        success: Callable[[object], None],
        progress: Callable[[object], None] | None = None,
        with_progress: bool = False,
    ) -> None:
        self.statusBar().showMessage(label)
        worker = Worker(fn, with_progress)
        self.workers.add(worker)
        worker.signals.result.connect(success)
        if progress is not None:
            worker.signals.progress.connect(progress)
        worker.signals.error.connect(lambda error: self._show_error(label, error))
        worker.signals.finished.connect(lambda: (self.statusBar().showMessage("Готово", 5000), self.workers.discard(worker)))
        self.pool.start(worker)

    def _show_error(self, title: str, error: str) -> None:
        log_path = self.root / "logs" / "errors.log"
        with log_path.open("a", encoding="utf-8") as handle:
            handle.write(f"\n[{datetime.now(timezone.utc).isoformat()}] {title}\n{error}\n")
        QMessageBox.critical(self, title, error.splitlines()[-1] if error else "Неизвестная ошибка")
        for log in (getattr(self, "data_log", None), getattr(self, "quality_log", None), getattr(self, "replay_log", None)):
            if log is not None: log.setPlainText(error)
        if hasattr(self, "auto_stage"):
            self.auto_stage.setText(f"Ошибка: {error.splitlines()[-1] if error else 'неизвестная ошибка'}")

    def run_self_test(self) -> None:
        def done(result: object) -> None:
            text = json.dumps(result, ensure_ascii=False, indent=2)
            self.scenario_log.setPlainText(text)
            QMessageBox.information(self, "Диагностика", f"Синтетическая школа: {result['passed']} / {result['total']} сценариев пройдено.")
            self.refresh_catalog()
        self._run("Выполняются 50 синтетических сценариев...", run_synthetic_school, done)

    def download_data(self) -> None:
        start = int(self.data_start.dateTime().toPython().replace(tzinfo=timezone.utc).timestamp() * 1000)
        end = int(self.data_end.dateTime().toPython().replace(tzinfo=timezone.utc).timestamp() * 1000)
        symbol = self.symbol.currentText(); self.data_progress.setRange(0, 0); self.data_log.setPlainText("Загрузка и строгая проверка...")
        def action(progress) -> dict:
            raw, manifest = DataManager(self.root / "data").download_candles(
                symbol, start, end, progress_callback=lambda done, total: progress({"done": done, "total": total})
            )
            return {"raw": str(raw), "manifest": str(manifest)}
        def show_progress(value: object) -> None:
            done, total = value["done"], max(value["total"], 1)
            percent = min(100, int(done / total * 100)); self.data_progress.setRange(0, 100); self.data_progress.setValue(percent)
            self.data_log.setPlainText(f"{symbol}: {percent}% — {done:,} / {total:,} минутных свечей")
        def done(result: object) -> None:
            self.data_progress.setRange(0, 100); self.data_progress.setValue(100)
            self.data_log.setPlainText(f"ГОТОВО. Файл сохранён здесь:\n{result['raw']}\n\nManifest:\n{result['manifest']}")
            self.refresh_catalog()
        self._run(f"Загрузка {symbol}...", action, done, show_progress, True)

    def set_data_period(self, months: int) -> None:
        self.data_end.setDateTime(datetime.now())
        self.data_start.setDateTime(datetime.now() - timedelta(days=PERIOD_DAYS[months]))

    def choose_factory_file(self) -> None:
        path, _ = QFileDialog.getOpenFileName(self, "Выберите PUMP dataset", str(self.root / "data"), "JSONL (*.jsonl)")
        if path:
            self.factory_path.setText(path)

    def latest_pump_dataset(self) -> Path | None:
        return self.latest_symbol_dataset("PUMPUSDT")

    def latest_symbol_dataset(self, symbol: str) -> Path | None:
        candidates = []
        for manifest in self.root.rglob("*.manifest.json"):
            try:
                value = json.loads(manifest.read_text(encoding="utf-8"))
                raw = manifest.with_suffix("").with_suffix(".jsonl")
                if value.get("symbol") == symbol and raw.exists():
                    candidates.append((raw.stat().st_mtime, raw))
            except Exception:
                continue
        return max(candidates, default=(0, None), key=lambda item: item[0])[1]

    def run_variant_factory(self) -> None:
        visible_path = Path(self.factory_path.text())
        path = visible_path if visible_path.exists() else self.latest_pump_dataset()
        if path is None:
            QMessageBox.warning(self, "Нет базы данных", "Сначала нажмите «ЗАПУСТИТЬ ВСЁ ОДНОЙ КНОПКОЙ» на главном экране.")
            self.go_to_page(0)
            return
        self.factory_path.setText(str(path))
        mode = str(self.factory_mode.currentData())
        self.factory_cancel_event.clear(); self.factory_pause_event.clear(); self.factory_run.setEnabled(False); self.factory_stop.setEnabled(True); self.factory_pause.setEnabled(True); self.factory_resume.setEnabled(False); self.factory_report_button.setEnabled(False); self.factory_progress.setValue(0)
        self.factory_summary.setText("Идёт автоматическое исследование. TEST закрыт до выбора одного победителя.")

        def action(progress) -> dict:
            holdout = seal_immutable_holdout(path, self.root / "data")
            rows = read_jsonl(Path(holdout.development_path))
            btc_path = self.latest_symbol_dataset("BTCUSDT")
            sol_path = self.latest_symbol_dataset("SOLUSDT")
            try:
                workers = workers_for_mode(self.hardware, "MAXIMUM")
                checkpoint = self.root / "checkpoints" / f"factory-{holdout.source_sha256[:16]}-adaptive-v8.json"
                result = optimize_adaptive(
                    rows, progress,
                    read_jsonl(btc_path) if btc_path else None,
                    read_jsonl(sol_path) if sol_path else None,
                    self.factory_cancel_event.is_set,
                    self.factory_pause_event.is_set,
                    workers,
                    checkpoint,
                    ready_reports_directory(self.root),
                )
            except ResearchCancelled:
                return {"cancelled": True}
            result["immutable_holdout"] = holdout.to_dict()
            json_report, md_report = write_optimizer_report(
                result, ready_reports_directory(self.root), Path(holdout.development_path)
            )
            result["json_report"] = str(json_report); result["md_report"] = str(md_report)
            return result

        def show_progress(value: object) -> None:
            self.factory_progress.setValue(int(value.get("percent", 0)))
            provisional = value.get("best_provisional", {})
            provisional_text = (
                f" Предварительно: худший Avg NET {provisional.get('worst_average_net', 0) * 100:+.3f}%, "
                f"сделок {provisional.get('validation_fills', 0)}."
                if provisional else ""
            )
            archive = value.get("interesting_archive", {})
            archive_text = f" Навсегда сохранено: {archive.get('candidate_count', 0)}." if archive else ""
            self.factory_stage.setText(
                f"{value.get('stage', '')}: поколение {value.get('generation', 0):,}, проверено {value.get('tested', 0):,}. "
                f"Процессов: {value.get('workers', 1)}. Скорость: {value.get('variants_per_second', 0):,.0f}/с. "
                f"Текущий лучший: {value.get('best', '—')}.{provisional_text} "
                f"Без существенного улучшения: {value.get('stagnant_generations', 0)}/{value.get('convergence_generations', '—')}."
                f"{archive_text}"
            )

        def done(result: object) -> None:
            self.factory_run.setEnabled(True); self.factory_stop.setEnabled(False); self.factory_pause.setEnabled(False); self.factory_resume.setEnabled(False)
            if result.get("cancelled"):
                self.factory_stage.setText("Остановлено пользователем. Данные не потеряны; можно запустить снова.")
                self.factory_summary.setText("Исследование остановлено. Незавершённый результат не выдаётся за готовый.")
                return
            self.factory_progress.setValue(100)
            self.factory_report_path = result["md_report"]
            self.factory_report_button.setEnabled(True)
            test = result["test"]
            verdict_text = {
                "CHALLENGER_CANDIDATE": "Найден кандидат, подтвердившийся на отдельном TEST.",
                "REJECTED_ON_TEST": "Лучший вариант не подтвердился на TEST. Он отклонён.",
                "REJECTED_ON_STRESS": "Кандидат прошёл TEST, но развалился при повышенных расходах или задержке.",
                "BASELINE_X_WINS": "Вариант положителен, но защищённый baseline X оказался лучше.",
                "NO_TRADE_WINS": "Устойчивого прибыльного варианта нет. Лучшее решение сейчас — не торговать.",
            }.get(result["verdict"], result["verdict"])
            mode_text = "Режим: адаптивный поиск без заранее заданного количества вариантов."
            if test is None:
                test_text = (
                    "TEST не открывался: для подтверждающего этапа недостаточно development-истории."
                    if not result.get("history_sufficient_for_test", True)
                    else "TEST не открывался: validation не допустил ни одного кандидата."
                )
                stage_text = "Готово. TRAIN → VALIDATION A → VALIDATION B. TEST сохранён закрытым."
            else:
                pf = test["profit_factor"]
                test_text = (
                    f"Отдельный TEST: {test['fills']} сделок, WR {test['win_rate'] * 100:.2f}%, "
                    f"Avg NET {test['average_net'] * 100:+.4f}%, PF {'n/a' if pf is None else f'{pf:.3f}'}, "
                    f"итог {test['compound_net'] * 100:+.3f}%, Max DD {test['max_drawdown'] * 100:.3f}%."
                )
                stage_text = "Готово. TRAIN → VALIDATION A → VALIDATION B → отдельный TEST завершены."
            space = result.get("search_space", {})
            context_text = "PUMP + BTC + SOL" if space.get("btc_sol_context_ready") else "только PUMP (BTC/SOL-базы не найдены)"
            self.factory_summary.setText(
                f"{verdict_text}\n{mode_text} Проверено: {result['variants_tested']:,} вариантов. "
                f"Рынки: {context_text}; пространство динамическое, фиксированного общего числа нет. "
                f"Формальный лидер: {result['selected']['candidate_id']}.\n{test_text}\n"
                f"Финальный holdout: {result['immutable_holdout']['holdout_rows']:,} строк, SEALED_UNOPENED."
            )
            self.factory_stage.setText(stage_text)
            self.factory_table.setRowCount(min(10, len(result["leaderboard"])))
            for row, candidate in enumerate(result["leaderboard"][:10]):
                config = candidate["config"]; a = candidate["validation_a"]; b = candidate["validation_b"]
                a_pf = "∞" if a["profit_factor"] is None and a["fills"] else (f"{a['profit_factor']:.2f}" if a["profit_factor"] is not None else "—")
                b_pf = "∞" if b["profit_factor"] is None and b["fills"] else (f"{b['profit_factor']:.2f}" if b["profit_factor"] is not None else "—")
                values = [
                    str(row + 1), candidate["candidate_id"], f"{config['target_net'] * 100:.1f}%",
                    f"{config['stop_net'] * 100:.1f}%", f"{config['max_hold_minutes']}m",
                    f"≤{config['max_entries_per_utc_day']}/д; пауза {config.get('min_hours_between_entries', 0)}ч",
                    f"{config.get('btc_context_rule', 'ANY')} / {config.get('sol_context_rule', 'ANY')}",
                    f"{min(a['average_net'], b['average_net']) * 100:+.3f}%",
                    f"{a_pf} / {b_pf}",
                    str(a["fills"] + b["fills"]),
                ]
                for column, value in enumerate(values):
                    self.factory_table.setItem(row, column, QTableWidgetItem(value))
            self.refresh_catalog()
            QMessageBox.information(self, "Фабрика вариантов", verdict_text)

        self._run("Автоматический поиск вариантов...", action, done, show_progress, True)

    def stop_variant_factory(self) -> None:
        self.factory_cancel_event.set()
        self.factory_stop.setEnabled(False)
        self.factory_stage.setText("Безопасно останавливаю и сохраняю checkpoint…")

    def pause_variant_factory(self) -> None:
        self.factory_pause_event.set(); self.factory_pause.setEnabled(False); self.factory_resume.setEnabled(True)
        self.factory_stage.setText("Пауза: завершается текущий блок и сохраняется checkpoint…")

    def resume_variant_factory(self) -> None:
        self.factory_pause_event.clear(); self.factory_pause.setEnabled(True); self.factory_resume.setEnabled(False)
        self.factory_stage.setText("Продолжаю с сохранённой точки…")

    def update_factory_estimate(self) -> None:
        if not hasattr(self, "factory_mode"):
            return
        self.factory_estimate.setText(
            "Нет потолка 10 млн: широкое исследование, скрещивание лидеров и локальное уточнение. "
            "Завершение — по сходимости; Pause, Resume и безопасная остановка сохраняют результат."
        )

    def open_factory_report(self) -> None:
        path = getattr(self, "factory_report_path", "")
        if path:
            try:
                os.startfile(path)
            except Exception as exc:
                QMessageBox.warning(self, "Отчёт", str(exc))

    def update_auto_estimate(self) -> None:
        if not hasattr(self, "auto_period"):
            return
        months = int(self.auto_period.currentData())
        rows = PERIOD_DAYS[months] * 1440 * 3
        mode = str(self.auto_search_mode.currentData()) if hasattr(self, "auto_search_mode") else "ADAPTIVE"
        resource = str(self.auto_resource_mode.currentData()) if hasattr(self, "auto_resource_mode") else "FAST"
        workers = workers_for_mode(self.hardware, resource)
        self.auto_estimate.setText(
            f"Данные: ≈ {rows:,} минутных свечей по PUMP/BTC/SOL. "
            f"Поиск: адаптивный, без заданного потолка вариантов; остановка по сходимости или вашей кнопке. "
            f"Точный replay: до {workers} CPU-процессов. ACTIVE: 0,5–5/сутки; SWING: 0,05–0,5/сутки."
        )
        self.refresh_prepared_data_status()

    def _automatic_config(self) -> AutomationConfig:
        return AutomationConfig(
            months=int(self.auto_period.currentData()), initial_capital=float(self.auto_capital.value()),
            currency=self.auto_currency.currentText(), search_mode=str(self.auto_search_mode.currentData()),
            resource_mode=str(self.auto_resource_mode.currentData()),
        )

    def refresh_prepared_data_status(self) -> None:
        if not hasattr(self, "auto_data_stage"):
            return
        prepared = AutomatedResearchRunner(self.root).load_prepared_data(self._automatic_config())
        if not prepared:
            self.auto_data_stage.setText("БАЗЫ НЕТ — сначала нажмите кнопку 1. Расчёт не начнётся и ничего не будет скачивать скрыто.")
            self.auto_data_stage.setStyleSheet("color:#ffbf69;font-size:15px")
            self.auto_run_button.setEnabled(False)
            return
        start = datetime.fromtimestamp(prepared["period"]["start_ms"] / 1000, tz=timezone.utc).strftime("%d.%m.%Y")
        end = datetime.fromtimestamp(prepared["period"]["end_ms"] / 1000, tz=timezone.utc).strftime("%d.%m.%Y")
        self.auto_data_stage.setText(
            f"БАЗА ГОТОВА: {start} — {end} UTC, PUMP + BTC + SOL, возраст {prepared.get('age_days', 0):.1f} дня. "
            "Повторно используется до 30 дней."
        )
        self.auto_data_stage.setStyleSheet("color:#39d98a;font-size:15px")
        self.auto_data_progress.setValue(100)
        self.auto_run_button.setEnabled(True)

    def prepare_automatic_data(self) -> None:
        config = self._automatic_config()
        self.auto_active_phase = "DATA"
        self.auto_cancel_event.clear(); self.auto_data_progress.setValue(0)
        self.auto_prepare_button.setEnabled(False); self.auto_run_button.setEnabled(False)
        self.auto_stop_button.setEnabled(True)
        self.auto_data_stage.setText("ЭТАП 1 — проверяю локальные базы. Расчёт вариантов ещё не запущен.")

        def action(progress) -> dict:
            try:
                return AutomatedResearchRunner(
                    self.root, progress_callback=progress, cancel_check=self.auto_cancel_event.is_set,
                ).prepare_data(config)
            except ResearchCancelled:
                return {"cancelled": True}

        def show_progress(value: object) -> None:
            self.auto_data_progress.setValue(int(value.get("percent", 0)))
            done, total = int(value.get("downloaded_rows", 0)), int(value.get("total_rows", 0))
            suffix = f" — {done:,} / {total:,} свечей" if total else ""
            self.auto_data_stage.setText(f"{value.get('stage', '')}{suffix}")

        def done(result: object) -> None:
            if result.get("cancelled"):
                self.auto_data_stage.setText("Подготовка базы остановлена. Уже скачанная часть сохранена.")
            else:
                self.refresh_prepared_data_status(); self.refresh_catalog()

        def finished() -> None:
            self.auto_active_phase = None
            self.auto_prepare_button.setEnabled(True); self.auto_stop_button.setEnabled(False)
            self.refresh_prepared_data_status(); self.statusBar().showMessage("Подготовка базы завершена", 5000)

        worker = Worker(action, True); self.workers.add(worker)
        worker.signals.progress.connect(show_progress); worker.signals.result.connect(done)
        worker.signals.error.connect(lambda error: self._show_error("Подготовка базы", error))
        worker.signals.finished.connect(lambda: (finished(), self.workers.discard(worker)))
        self.pool.start(worker)

    def run_automatic_research(self) -> None:
        config = self._automatic_config()
        prepared = AutomatedResearchRunner(self.root).load_prepared_data(config)
        if not prepared:
            QMessageBox.warning(self, "Сначала база", "Подходящая база не подготовлена. Нажмите кнопку 1 — расчёт сам ничего скачивать не будет.")
            self.refresh_prepared_data_status()
            return
        self.auto_active_phase = "COMPUTE"
        self.auto_cancel_event.clear(); self.auto_pause_event.clear(); self.auto_progress.setValue(0); self.auto_run_button.setEnabled(False); self.auto_stop_button.setEnabled(True); self.auto_pause_button.setEnabled(True); self.auto_resume_button.setEnabled(False)
        self.auto_stage.setText("Запуск..."); self.auto_result.setText("Исследование выполняется. Программу можно оставить работать.")

        def action(progress) -> dict:
            try:
                return AutomatedResearchRunner(
                    self.root, progress_callback=progress, cancel_check=self.auto_cancel_event.is_set,
                    pause_check=self.auto_pause_event.is_set,
                ).run(config, prepared=prepared)
            except ResearchCancelled:
                return {"cancelled": True}

        def show_progress(value: object) -> None:
            self.auto_progress.setValue(int(value.get("percent", 0)))
            tested = int(value.get("tested", 0)); total_variants = int(value.get("variants_total", 0))
            generation = int(value.get("generation", 0)); stagnant = int(value.get("stagnant_generations", 0))
            suffix = f" — поколение {generation:,}, проверено {tested:,}, без улучшения {stagnant}" if tested else ""
            workers = value.get("workers")
            rate = value.get("variants_per_second")
            compute = f" — {workers} процессов, {rate:,.0f} вариантов/с" if workers and rate else ""
            provisional = value.get("best_provisional", {})
            current = (
                f" — предварительный лидер: Avg NET {provisional.get('worst_average_net', 0) * 100:+.3f}%, "
                f"{provisional.get('validation_fills', 0)} сделок, "
                f"{provisional.get('trades_per_day_a', 0):.2f}/{provisional.get('trades_per_day_b', 0):.2f} в сутки"
                if provisional else ""
            )
            archive = value.get("interesting_archive", {})
            archived = f" — навсегда сохранено: {archive.get('candidate_count', 0)}" if archive else ""
            self.auto_stage.setText(f"{value.get('stage', '')}{suffix}{compute}{current}{archived}")
            if provisional.get("config"):
                self.auto_best_parameters.setText(
                    "ТЕКУЩИЙ ЛИДЕР — параметры:\n" + json.dumps(provisional["config"], ensure_ascii=False, indent=2)
                )
            preview = provisional.get("equity_preview", {})
            if preview.get("trades"):
                self.auto_capital_chart.set_trades(preview["trades"], float(self.auto_capital.value()), self.auto_currency.currentText())

        def done(result: object) -> None:
            if result.get("cancelled"):
                self.auto_stage.setText("Расчёт остановлен. База не изменена, checkpoint сохранён.")
                self.auto_result.setText("Незавершённый расчёт не выдан за готовый результат.")
                return
            self.auto_run_button.setEnabled(True); self.auto_progress.setValue(100)
            best = result.get("best_candidate", {})
            capital_result = best.get("capital") or result["capital"]
            metrics = best.get("metrics") or result["metrics"]
            downloaded_mb = result["downloaded_bytes"] / 1_048_576
            factory = result.get("variant_factory", {})
            self.auto_result.setText(
                f"Историческая симуляция: {capital_result['initial']:.2f} → {capital_result['historical_final']:.2f} {capital_result['currency']}\n"
                f"Результат: {capital_result['historical_profit']:+.2f} {capital_result['currency']} ({capital_result['historical_return'] * 100:+.3f}%)\n"
                f"Сделок: {metrics['fills']}  |  Win rate: {metrics['win_rate'] * 100:.2f}%\n"
                f"Обработано: {result['downloaded_rows']:,} свечей / {downloaded_mb:.1f} MB\n"
                f"Фабрика: {factory.get('variants_tested', 0):,} вариантов → {factory.get('verdict', '—')}\n{result['conclusion']}"
            )
            best_trades = best.get("trades") or result.get("trades", [])
            self.auto_capital_chart.set_trades(best_trades, capital_result["initial"], capital_result["currency"])
            if best.get("parameters"):
                self.auto_best_parameters.setText("ЛУЧШИЙ НАЙДЕННЫЙ ВАРИАНТ:\n" + json.dumps(best["parameters"], ensure_ascii=False, indent=2))
            self.auto_stage.setText(f"Готово. Постоянный отчёт: {result['latest_report']}")
            self.refresh_catalog()
            QMessageBox.information(self, "Автоматическое исследование", "Данные и тесты обработаны. Готовый отчёт создан рядом с программой.")

        def restore_button() -> None:
            self.auto_active_phase = None
            self.refresh_prepared_data_status(); self.auto_stop_button.setEnabled(False); self.auto_pause_button.setEnabled(False); self.auto_resume_button.setEnabled(False)

        self.statusBar().showMessage("Автоматическое исследование...")
        worker = Worker(action, True); self.workers.add(worker)
        worker.signals.progress.connect(show_progress); worker.signals.result.connect(done)
        worker.signals.error.connect(lambda error: self._show_error("Автоматическое исследование", error))
        worker.signals.finished.connect(lambda: (restore_button(), self.statusBar().showMessage("Готово", 5000), self.workers.discard(worker)))
        self.pool.start(worker)

    def stop_automatic_research(self) -> None:
        self.auto_cancel_event.set()
        self.auto_stop_button.setEnabled(False)
        if self.auto_active_phase == "DATA":
            self.auto_data_stage.setText("Останавливаю скачивание. Уже полученные данные не удаляются…")
        else:
            self.auto_stage.setText("Безопасно останавливаю расчёт и сохраняю checkpoint…")

    def pause_automatic_research(self) -> None:
        self.auto_pause_event.set(); self.auto_pause_button.setEnabled(False); self.auto_resume_button.setEnabled(True)
        self.auto_stage.setText("Пауза: завершается текущий блок, результаты сохраняются…")

    def resume_automatic_research(self) -> None:
        self.auto_pause_event.clear(); self.auto_pause_button.setEnabled(True); self.auto_resume_button.setEnabled(False)
        self.auto_stage.setText("Продолжаю с сохранённой точки…")

    def open_ready_reports(self) -> None:
        directory = ready_reports_directory(self.root)
        try:
            os.startfile(directory)
        except Exception as exc:
            QMessageBox.warning(self, "Папка отчётов", str(exc))

    def open_interesting_candidates(self) -> None:
        directory = ready_reports_directory(self.root)
        report = directory / "INTERESTING_CANDIDATES.md"
        try:
            os.startfile(report if report.exists() else directory)
        except Exception as exc:
            QMessageBox.warning(self, "Накопленные варианты", str(exc))

    def open_data_folder(self) -> None:
        directory = self.root / "data"
        directory.mkdir(parents=True, exist_ok=True)
        try:
            os.startfile(directory)
        except Exception as exc:
            QMessageBox.warning(self, "Папка данных", str(exc))

    def open_workspace_folder(self) -> None:
        try:
            os.startfile(self.root)
        except Exception as exc:
            QMessageBox.warning(self, "Рабочая папка", str(exc))

    def refresh_catalog(self) -> None:
        manifests = []
        for path in self.root.rglob("*.manifest.json"):
            try:
                item = json.loads(path.read_text(encoding="utf-8"))
                if not item.get("symbol") or not item.get("row_count"):
                    continue
                item["manifest_path"] = str(path); manifests.append(item)
            except Exception: pass
        manifests.sort(key=lambda item: item.get("end_utc", ""), reverse=True)
        self.datasets_table.setRowCount(len(manifests))
        for row, item in enumerate(manifests):
            raw_guess = str(Path(item["manifest_path"]).with_suffix("").with_suffix(".jsonl"))
            raw_path = Path(raw_guess)
            size = f"{raw_path.stat().st_size / 1_048_576:.1f} MB" if raw_path.exists() else "—"
            try:
                end_time = datetime.fromisoformat(item.get("end_utc", "").replace("Z", "+00:00"))
                age_days = max(0.0, (datetime.now(timezone.utc) - end_time).total_seconds() / 86_400)
                freshness = f"актуальна, {age_days:.1f} дн." if age_days <= 30 else f"старше 30 дней ({age_days:.0f})"
            except ValueError:
                freshness = "дата неизвестна"
            values = [item.get("symbol", ""), f"{item.get('start_utc','')[:16]} → {item.get('end_utc','')[:16]}", str(item.get("row_count", 0)), size, freshness, raw_path.name, item.get("source", ""), item.get("checksum_sha256", "")[:12]]
            for col, value in enumerate(values):
                cell = QTableWidgetItem(value); cell.setData(Qt.ItemDataRole.UserRole, raw_guess); self.datasets_table.setItem(row, col, cell)
        reports = list(self.root.rglob("*.md")) + list(self.root.rglob("*_report.json"))
        ready = ready_reports_directory(self.root)
        reports += list(ready.glob("*.md")) + list(ready.glob("*.json")) + list(ready.glob("*.zip"))
        reports = list(dict.fromkeys(reports))
        self.reports_table.setRowCount(len(reports))
        for row, path in enumerate(sorted(reports, key=lambda p: p.stat().st_mtime, reverse=True)):
            values = [path.name, datetime.fromtimestamp(path.stat().st_mtime).strftime("%Y-%m-%d %H:%M"), f"{path.stat().st_size/1024:.1f} KB", str(path)]
            for col, value in enumerate(values):
                cell = QTableWidgetItem(value); cell.setData(Qt.ItemDataRole.UserRole, str(path)); self.reports_table.setItem(row, col, cell)
        self.dataset_metric.setText(str(len(manifests))); self.report_metric.setText(str(len(reports)))
        incomplete = []
        for checkpoint in (self.root / "checkpoints").glob("*.json"):
            try:
                value = json.loads(checkpoint.read_text(encoding="utf-8"))
                if value.get("status") != "COMPLETED":
                    incomplete.append(value | {"path": str(checkpoint)})
            except Exception:
                continue
        if incomplete:
            latest = max(incomplete, key=lambda item: item.get("updated_at_utc", ""))
            tested, total = int(latest.get("tested", 0)), int(latest.get("variants_total", 0))
            self.checkpoint_notice.setText(
                f"Найдено незавершённое исследование: {tested:,} / {total:,} вариантов. "
                "Выберите тот же режим и нажмите запуск — программа продолжит автоматически."
            )
            self.checkpoint_notice.setStyleSheet("font-size:15px;color:#ffbf69")
        else:
            self.checkpoint_notice.setText("Незавершённых исследований нет. Все результаты сохранены.")
            self.checkpoint_notice.setStyleSheet("font-size:15px;color:#39d98a")
        if hasattr(self, "factory_path") and not Path(self.factory_path.text()).exists():
            latest = self.latest_pump_dataset()
            if latest is not None:
                self.factory_path.setText(str(latest))
                self.quality_path.setText(str(latest)); self.replay_path.setText(str(latest))

    def use_dataset(self, row: int, _column: int) -> None:
        path = self.datasets_table.item(row, 0).data(Qt.ItemDataRole.UserRole)
        self.quality_path.setText(path); self.replay_path.setText(path); self.go_to_page(4)

    def open_dataset_location(self, row: int, _column: int) -> None:
        path = Path(str(self.datasets_table.item(row, 0).data(Qt.ItemDataRole.UserRole)))
        if path.exists():
            self.quality_path.setText(str(path)); self.replay_path.setText(str(path))
            try:
                os.startfile(path.parent)
            except Exception as exc:
                QMessageBox.warning(self, "Папка базы", str(exc))

    def choose_quality_file(self) -> None:
        path, _ = QFileDialog.getOpenFileName(self, "Выберите данные", str(self.root / "data"), "Market data (*.jsonl *.csv);;Все файлы (*)")
        if path: self.quality_path.setText(path)

    def choose_replay_file(self) -> None:
        path, _ = QFileDialog.getOpenFileName(self, "Выберите dataset", str(self.root / "data"), "JSONL (*.jsonl)")
        if path: self.replay_path.setText(path)

    def validate_selected(self) -> None:
        path = Path(self.quality_path.text())
        if not path.exists():
            path = self.latest_pump_dataset()
        if path is None:
            QMessageBox.warning(self, "Нет базы", "Сначала скачайте данные на экране «Данные».")
            return
        def action():
            rows = read_csv(path) if path.suffix.lower() == ".csv" else read_jsonl(path)
            return validate_candles(rows).to_dict()
        def done(result):
            self.gaps_metric.setText(str(len(result["gaps"]))); self.dupes_metric.setText(str(len(result["duplicates"]))); self.invalid_metric.setText(str(len(result["invalid_rows"]))); self.quality_log.setPlainText(json.dumps(result, ensure_ascii=False, indent=2))
        self._run("Проверка качества...", action, done)

    def repair_selected(self) -> None:
        path = Path(self.quality_path.text())
        if not path.exists():
            path = self.latest_pump_dataset()
        if path is None:
            QMessageBox.warning(self, "Нет базы", "Сначала скачайте данные на экране «Данные».")
            return
        if path.suffix.lower() != ".jsonl":
            QMessageBox.warning(self, "Исправление пропусков", "Автоматический gap repair поддерживает JSONL. CSV сначала импортируется и нормализуется.")
            return
        def action():
            raw, manifest, state = DataManager(self.root / "data").repair_gaps(path); return {"raw": str(raw), "manifest": str(manifest), "job": str(state)}
        def done(result): self.quality_path.setText(result["raw"]); self.quality_log.setPlainText(json.dumps(result, ensure_ascii=False, indent=2)); self.refresh_catalog()
        self._run("Восстановление пропусков...", action, done)

    def run_replay(self) -> None:
        path = Path(self.replay_path.text()); strategy = self.strategy.currentText()
        if not path.exists():
            path = self.latest_pump_dataset()
        if path is None:
            QMessageBox.warning(self, "Нет базы", "Сначала скачайте данные на экране «Данные».")
            return
        if strategy == "NO_TRADE":
            self.replay_log.setPlainText(json.dumps({"strategy": "NO_TRADE", "trades": 0, "net": 0, "status": "baseline"}, ensure_ascii=False, indent=2)); return
        def action():
            rows = read_jsonl(path)
            if strategy.startswith("VWAP"):
                result = replay_vwap_canary(rows).to_dict()
                stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
                directory = self.root / "experiments" / stamp
                directory.mkdir(parents=True, exist_ok=True)
                payload = {"experiment_id": stamp, "strategy": strategy, "dataset": str(path), "result": result, "simulation": True}
                (directory / "experiment.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
                return payload
            checksum = hashlib.sha256(path.read_bytes()).hexdigest(); result = replay_economy(rows)
            stamp = datetime.now().strftime("%Y%m%d-%H%M%S"); directory = self.root / "experiments" / stamp
            reports = write_replay_report(result, self.root / "reports" / stamp, checksum)
            payload = result.to_dict()
            record = {"experiment_id": stamp, "status": "completed", "strategy": strategy, "dataset": str(path), "dataset_sha256": checksum, "created_at_utc": datetime.now(timezone.utc).isoformat(), "simulation": True, "reports": [str(p) for p in reports], "result": payload}
            directory.mkdir(parents=True, exist_ok=True)
            partial = directory / "experiment.json.partial"
            partial.write_text(json.dumps(record, ensure_ascii=False, indent=2), encoding="utf-8")
            partial.replace(directory / "experiment.json")
            return record
        def done(record):
            payload = record.get("result", {})
            metrics = payload.get("metrics", payload)
            trades = payload.get("trades", [])
            self.result_trades.setText(str(metrics.get("fills", metrics.get("fills", "—"))))
            self.result_winrate.setText(f"{float(metrics.get('win_rate', 0))*100:.2f}%")
            self.result_net.setText(f"{float(metrics.get('compound_net', 0))*100:+.3f}%" if "compound_net" in metrics else "—")
            pf = metrics.get("profit_factor"); self.result_pf.setText("n/a" if pf is None else f"{float(pf):.3f}")
            self.equity_chart.set_trades(trades)
            self.trades_table.setRowCount(len(trades))
            for row, trade in enumerate(trades):
                signal = datetime.fromtimestamp(trade["signal_time_ms"] / 1000, tz=timezone.utc).strftime("%Y-%m-%d %H:%M")
                fill = datetime.fromtimestamp(trade["fill_time_ms"] / 1000, tz=timezone.utc).strftime("%m-%d %H:%M")
                exit_time = datetime.fromtimestamp(trade["exit_time_ms"] / 1000, tz=timezone.utc).strftime("%m-%d %H:%M")
                values = [signal, fill, exit_time, trade["reason"], f"{trade['net_return']*100:+.3f}%", f"{trade['entry']:.8f}"]
                for col, value in enumerate(values): self.trades_table.setItem(row, col, QTableWidgetItem(value))
            summary = {key: value for key, value in record.items() if key != "result"}
            summary["metrics"] = metrics
            self.replay_log.setPlainText(json.dumps(summary, ensure_ascii=False, indent=2)); self.refresh_catalog()
        self._run(f"Replay {strategy}...", action, done)

    def open_report(self, row: int, _column: int) -> None:
        path = Path(self.reports_table.item(row, 0).data(Qt.ItemDataRole.UserRole))
        try: os.startfile(path)
        except Exception as exc: QMessageBox.warning(self, "Открытие отчёта", str(exc))

    def _apply_style(self) -> None:
        self.setStyleSheet("""
        QMainWindow,QWidget{background:#071018;color:#eaf4fb;font-family:'Segoe UI';font-size:14px}
        #nav{background:#09141e;border:0;outline:0}#nav::item{padding:11px;border-radius:7px;color:#8da8ba}#nav::item:selected{background:#173148;color:white}
        #brand{font-size:23px;font-weight:700;padding:8px 0}#pageTitle{font-size:27px;font-weight:700}#card{background:#0d1924;border:1px solid #20384b;border-radius:10px}#cardTitle{font-size:15px;font-weight:600;color:#dfeef7}
        QPushButton{background:#2588db;border:0;border-radius:7px;padding:10px 15px;font-weight:600}QPushButton:hover{background:#3699eb}QPushButton:disabled{background:#273743;color:#71818d}QPushButton#secondary{background:#293f50}QPushButton#warning{background:#9b6725}QPushButton#primaryLarge{font-size:16px;padding:15px;background:#19a86b}
        QComboBox,QDateTimeEdit,QDoubleSpinBox,QPlainTextEdit,QTableWidget{background:#08131c;border:1px solid #20384b;border-radius:6px;padding:6px}QHeaderView::section{background:#122231;color:#89a3b6;border:0;padding:8px}QTableWidget{gridline-color:#20384b}QProgressBar{border:1px solid #20384b;border-radius:5px;text-align:center;min-height:22px}QProgressBar::chunk{background:#39d98a}
        """)

    def closeEvent(self, event) -> None:
        self.auto_cancel_event.set()
        self.factory_cancel_event.set()
        event.accept()


def main() -> int:
    multiprocessing.freeze_support()
    if os.environ.get("PUMP_LAB_AUTOTEST") == "core":
        root = app_root()
        result = run_full_self_test(root)
        (root / "logs" / "autotest.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
        return 0
    app = QApplication.instance() or QApplication(sys.argv)
    app.setApplicationName("PUMP Research Lab")
    app.setOrganizationName("PUMP")
    window = ResearchLab(); window.show()
    if os.environ.get("PUMP_LAB_AUTOTEST") == "ui":
        QTimer.singleShot(1200, app.quit)
    return app.exec()


if __name__ == "__main__":
    raise SystemExit(main())
