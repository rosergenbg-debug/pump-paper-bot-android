from __future__ import annotations

import ctypes
import os
import subprocess
import time
from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class TelemetrySnapshot:
    cpu_percent: float | None
    cpu_temperature_c: float | None
    ram_percent: float | None
    gpu_percent: float | None
    gpu_temperature_c: float | None
    gpu_memory_used_mb: int | None
    gpu_memory_total_mb: int | None
    gpu_name: str
    exact_replay_device: str = "CPU"


class _FileTime(ctypes.Structure):
    _fields_ = [("low", ctypes.c_ulong), ("high", ctypes.c_ulong)]

    @property
    def value(self) -> int:
        return (int(self.high) << 32) | int(self.low)


class _MemoryStatusEx(ctypes.Structure):
    _fields_ = [
        ("dwLength", ctypes.c_ulong), ("dwMemoryLoad", ctypes.c_ulong),
        ("ullTotalPhys", ctypes.c_ulonglong), ("ullAvailPhys", ctypes.c_ulonglong),
        ("ullTotalPageFile", ctypes.c_ulonglong), ("ullAvailPageFile", ctypes.c_ulonglong),
        ("ullTotalVirtual", ctypes.c_ulonglong), ("ullAvailVirtual", ctypes.c_ulonglong),
        ("ullAvailExtendedVirtual", ctypes.c_ulonglong),
    ]


class SystemTelemetry:
    def __init__(self) -> None:
        self._previous_cpu: tuple[int, int] | None = None
        self._cached_cpu_temp: float | None = None
        self._cpu_temp_checked_at = 0.0

    def _cpu(self) -> float | None:
        if os.name != "nt":
            return None
        idle = _FileTime(); kernel = _FileTime(); user = _FileTime()
        if not ctypes.windll.kernel32.GetSystemTimes(ctypes.byref(idle), ctypes.byref(kernel), ctypes.byref(user)):
            return None
        total = kernel.value + user.value
        current = (idle.value, total)
        if self._previous_cpu is None:
            self._previous_cpu = current
            return None
        idle_delta = current[0] - self._previous_cpu[0]
        total_delta = current[1] - self._previous_cpu[1]
        self._previous_cpu = current
        return round(max(0.0, min(100.0, (1.0 - idle_delta / total_delta) * 100.0)), 1) if total_delta else None

    @staticmethod
    def _ram() -> float | None:
        if os.name != "nt":
            return None
        status = _MemoryStatusEx(); status.dwLength = ctypes.sizeof(status)
        return float(status.dwMemoryLoad) if ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(status)) else None

    def _cpu_temperature(self) -> float | None:
        now = time.monotonic()
        if now - self._cpu_temp_checked_at < 30:
            return self._cached_cpu_temp
        self._cpu_temp_checked_at = now
        creation_flags = getattr(subprocess, "CREATE_NO_WINDOW", 0)
        try:
            command = (
                "(Get-CimInstance -Namespace root/wmi -ClassName MSAcpi_ThermalZoneTemperature "
                "-ErrorAction Stop | Select-Object -First 1 -ExpandProperty CurrentTemperature)"
            )
            result = subprocess.run(
                ["powershell", "-NoProfile", "-Command", command], capture_output=True,
                text=True, timeout=4, creationflags=creation_flags, check=True,
            )
            raw = float(result.stdout.strip())
            value = raw / 10.0 - 273.15
            self._cached_cpu_temp = round(value, 1) if 0 < value < 130 else None
        except Exception:
            self._cached_cpu_temp = None
        return self._cached_cpu_temp

    @staticmethod
    def _gpu() -> tuple[str, float | None, float | None, int | None, int | None]:
        creation_flags = getattr(subprocess, "CREATE_NO_WINDOW", 0)
        try:
            result = subprocess.run(
                [
                    "nvidia-smi", "--query-gpu=name,utilization.gpu,temperature.gpu,memory.used,memory.total",
                    "--format=csv,noheader,nounits",
                ],
                capture_output=True, text=True, timeout=4, creationflags=creation_flags, check=True,
            )
            name, load, temperature, used, total = [item.strip() for item in result.stdout.splitlines()[0].split(",")]
            return name, float(load), float(temperature), int(used), int(total)
        except Exception:
            return "Не обнаружена", None, None, None, None

    def snapshot(self) -> TelemetrySnapshot:
        gpu_name, gpu_load, gpu_temp, gpu_used, gpu_total = self._gpu()
        return TelemetrySnapshot(
            self._cpu(), self._cpu_temperature(), self._ram(), gpu_load, gpu_temp,
            gpu_used, gpu_total, gpu_name,
        )
