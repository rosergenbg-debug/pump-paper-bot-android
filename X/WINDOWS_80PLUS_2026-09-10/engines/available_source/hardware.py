from __future__ import annotations

import ctypes
import json
import os
import platform
import subprocess
from dataclasses import asdict, dataclass
from pathlib import Path

try:
    import winreg
except ImportError:  # pragma: no cover - non-Windows development host
    winreg = None


@dataclass(frozen=True, slots=True)
class HardwareProfile:
    cpu: str
    physical_cores: int
    logical_processors: int
    ram_gb: float
    gpu: str
    gpu_vram_mb: int
    cuda_available: bool
    recommended_workers: int

    def to_dict(self) -> dict:
        return asdict(self)


class _MemoryStatusEx(ctypes.Structure):
    _fields_ = [
        ("dwLength", ctypes.c_ulong),
        ("dwMemoryLoad", ctypes.c_ulong),
        ("ullTotalPhys", ctypes.c_ulonglong),
        ("ullAvailPhys", ctypes.c_ulonglong),
        ("ullTotalPageFile", ctypes.c_ulonglong),
        ("ullAvailPageFile", ctypes.c_ulonglong),
        ("ullTotalVirtual", ctypes.c_ulonglong),
        ("ullAvailVirtual", ctypes.c_ulonglong),
        ("ullAvailExtendedVirtual", ctypes.c_ulonglong),
    ]


def _ram_gb() -> float:
    if os.name != "nt":
        return 0.0
    status = _MemoryStatusEx()
    status.dwLength = ctypes.sizeof(status)
    if ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(status)):
        return round(status.ullTotalPhys / 1024**3, 1)
    return 0.0


def _physical_cores(logical: int) -> int:
    if os.name == "nt":
        try:
            relation_processor_core = 0
            needed = ctypes.c_ulong(0)
            ctypes.windll.kernel32.GetLogicalProcessorInformationEx(relation_processor_core, None, ctypes.byref(needed))
            buffer = ctypes.create_string_buffer(needed.value)
            if ctypes.windll.kernel32.GetLogicalProcessorInformationEx(relation_processor_core, buffer, ctypes.byref(needed)):
                offset = cores = 0
                while offset < needed.value:
                    size = ctypes.c_ulong.from_buffer(buffer, offset + 4).value
                    if not size:
                        break
                    cores += 1
                    offset += size
                if cores:
                    return cores
        except Exception:
            pass
    return max(1, logical // 2)


def _gpu() -> tuple[str, int, bool]:
    creation_flags = getattr(subprocess, "CREATE_NO_WINDOW", 0)
    try:
        result = subprocess.run(
            ["nvidia-smi", "--query-gpu=name,memory.total", "--format=csv,noheader,nounits"],
            capture_output=True,
            text=True,
            timeout=5,
            creationflags=creation_flags,
            check=True,
        )
        first = result.stdout.strip().splitlines()[0]
        name, memory = first.rsplit(",", 1)
        return name.strip(), int(memory.strip()), True
    except Exception:
        return "Не обнаружена", 0, False


def detect_hardware() -> HardwareProfile:
    logical = max(1, os.cpu_count() or 1)
    physical = _physical_cores(logical)
    cpu = ""
    if winreg is not None:
        try:
            with winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE, r"HARDWARE\DESCRIPTION\System\CentralProcessor\0") as key:
                cpu = str(winreg.QueryValueEx(key, "ProcessorNameString")[0]).strip()
        except OSError:
            pass
    cpu = cpu or os.environ.get("PROCESSOR_IDENTIFIER") or platform.processor() or "Неизвестный процессор"
    gpu, vram, cuda = _gpu()
    # Physical cores are the reliable starting point for branch-heavy replay.
    # Leave one core free on small systems and let the user choose a stronger profile.
    recommended = max(1, min(physical, logical - 2 if logical >= 6 else logical - 1))
    return HardwareProfile(cpu, physical, logical, _ram_gb(), gpu, vram, cuda, recommended)


def workers_for_mode(profile: HardwareProfile, mode: str) -> int:
    mode = mode.upper()
    if mode == "BACKGROUND":
        return max(1, profile.physical_cores // 2)
    if mode == "FAST":
        return profile.recommended_workers
    if mode == "MAXIMUM":
        return max(profile.recommended_workers, min(profile.logical_processors - 2, profile.physical_cores + 4))
    raise ValueError("Неизвестный режим ресурсов")


def save_hardware_profile(path: Path, profile: HardwareProfile, resource_mode: str, workers: int) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = profile.to_dict() | {"resource_mode": resource_mode, "selected_workers": workers}
    partial = path.with_suffix(path.suffix + ".partial")
    partial.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    partial.replace(path)
