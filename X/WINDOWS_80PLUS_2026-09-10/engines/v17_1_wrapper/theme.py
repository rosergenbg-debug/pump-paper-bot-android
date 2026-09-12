"""Presentation only: no market or research parameters."""
from datetime import datetime, timezone
import math
import os
from pathlib import Path
from PySide6.QtCore import Qt, QPointF
from PySide6.QtGui import QColor, QFont, QFontDatabase, QPainter, QPen, QPolygonF
from PySide6.QtWidgets import QWidget

STYLE = '''
QMainWindow, QWidget {background:#101113;color:#eceef1;font-family:'Segoe UI';font-size:13px;}
QLabel {background:transparent;}
QLabel#title {font-size:23px;font-weight:600;}
QLabel#subtitle {color:#a2a7af;font-size:12px;}
QLabel#stage {color:#e7ba56;font-size:14px;font-weight:600;padding:6px 0;}
QFrame#metric {background:#1a1c20;border:1px solid #2d3036;border-radius:9px;}
QLabel#metricValue {font-size:19px;font-weight:600;color:#f2f3f5;}
QLabel#metricTitle {color:#a2a7af;font-size:11px;}
QPushButton {background:#25282d;border:1px solid #383c43;border-radius:6px;padding:7px 13px;}
QPushButton:hover {background:#33373d;border-color:#a68d56;}
QPushButton:pressed {background:#454035;}
QPushButton:disabled {background:#1c1e22;color:#737983;border-color:#292c31;}
QPushButton#primary {background:#e7ba56;color:#111315;font-weight:600;border:1px solid #e7ba56;}
QPushButton#primary:hover {background:#f4cc75;}
QPushButton#primary:disabled {background:#645534;color:#b5aa93;border-color:#645534;}
QComboBox,QDateEdit,QDoubleSpinBox,QLineEdit {background:#1a1c20;border:1px solid #3a3e45;border-radius:5px;padding:6px;selection-background-color:#6b582e;}
QComboBox QAbstractItemView {background:#202328;selection-background-color:#51442b;color:#f0f0f0;}
QCheckBox {spacing:7px;padding:4px;}
QCheckBox::indicator {width:17px;height:17px;border:1px solid #707681;border-radius:4px;background:#1a1c20;}
QCheckBox::indicator:checked {background:#e7ba56;border:3px solid #51452b;}
QCheckBox:disabled {color:#737983;}
QTabWidget::pane {border:1px solid #2e3137;border-radius:5px;}
QTabBar::tab {padding:9px 16px;color:#a7acb5;border-bottom:2px solid transparent;}
QTabBar::tab:selected {color:#e7ba56;border-bottom:2px solid #e7ba56;background:#191b1f;}
QTabBar::tab:hover {color:#ffffff;background:#202226;}
QPlainTextEdit {background:#17191d;border:1px solid #2e3137;border-radius:5px;padding:5px;selection-background-color:#51442b;}
QTableWidget {background:#16181c;alternate-background-color:#1b1e22;border:1px solid #2e3137;gridline-color:#272a30;selection-background-color:#51442b;selection-color:#ffffff;}
QHeaderView::section {background:#25282d;color:#b8bec8;border:0;border-bottom:1px solid #3a3e45;padding:8px;}
QProgressBar {background:#22252a;border:0;border-radius:4px;min-height:14px;text-align:center;color:#fff;}
QProgressBar::chunk {background:#9e813e;border-radius:4px;}
QScrollBar:vertical {background:#17191d;width:10px;}
QScrollBar::handle:vertical {background:#505660;min-height:25px;border-radius:4px;}
QScrollBar:horizontal {background:#17191d;height:10px;}
QScrollBar::handle:horizontal {background:#505660;min-width:25px;border-radius:4px;}
QToolTip {background:#292c32;color:#f2f2f2;border:1px solid #62666c;}
'''


def configure(app):
    font = Path(os.environ.get('WINDIR', r'C:\Windows')) / 'Fonts' / 'segoeui.ttf'
    if font.exists():
        QFontDatabase.addApplicationFont(str(font))
    app.setFont(QFont('Segoe UI', 10))
    app.setStyle('Fusion')
    app.setStyleSheet(STYLE)


class Timeline(QWidget):
    """Same points contract as V16; only painting is replaced."""
    def __init__(self):
        super().__init__()
        self.points = []
        self.setMinimumHeight(210)

    def paintEvent(self, event):
        p = QPainter(self)
        p.fillRect(self.rect(), QColor('#17191d'))
        p.setRenderHint(QPainter.RenderHint.Antialiasing)
        p.setPen(QColor('#a2a7af'))
        points = [(t, v) for t, v in self.points if math.isfinite(t) and math.isfinite(v)]
        if len(points) < 2:
            p.drawText(self.rect(), Qt.AlignmentFlag.AlignCenter,
                       'Здесь появится график капитала текущего исследования\nСтарые находки — во вкладке сохранённых стратегий')
            return
        lo, hi = min(v for _, v in points), max(v for _, v in points)
        pad = max((hi-lo)*.08, abs(hi)*.001, 1)
        lo, hi = lo-pad, hi+pad
        start, end = points[0][0], points[-1][0]
        left, top, right, bottom = 80, 40, self.width()-22, self.height()-33
        for i in range(5):
            y = top + i*(bottom-top)/4
            v = hi-i*(hi-lo)/4
            p.setPen(QPen(QColor('#2c2f35'), 1))
            p.drawLine(QPointF(left, y), QPointF(right, y))
            p.setPen(QColor('#a2a7af'))
            p.drawText(7, int(y)+4, f'{v:,.0f}')
        line = QPolygonF([QPointF(left+(t-start)/max(end-start,1)*(right-left),
                                   top+(hi-v)/(hi-lo)*(bottom-top)) for t,v in points])
        color = '#42ce98' if points[-1][1] >= points[0][1] else '#ed7888'
        p.setPen(QPen(QColor(color), 2.2))
        p.drawPolyline(line)
        p.drawText(14, 23, f'{points[0][1]:,.2f} → {points[-1][1]:,.2f} условных EUR · после расходов')
        p.setPen(QColor('#a2a7af'))
        for t, x in [(start, left), (end, max(left+110, right-95))]:
            p.drawText(int(x), self.height()-9, datetime.fromtimestamp(t/1000, timezone.utc).strftime('%d.%m.%Y'))
