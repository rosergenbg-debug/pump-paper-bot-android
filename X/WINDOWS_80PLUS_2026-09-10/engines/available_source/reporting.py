from __future__ import annotations

import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path

from .baseline_x import ReplayResult


def write_replay_report(result: ReplayResult, output_directory: Path, dataset_checksum: str = "") -> tuple[Path, Path]:
    output_directory.mkdir(parents=True, exist_ok=True)
    payload = result.to_dict()
    payload["provenance"] = {
        "dataset_checksum": dataset_checksum,
        "created_at_utc": datetime.now(timezone.utc).isoformat(),
        "causal": True,
        "real_orders": False,
    }
    canonical = json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False)
    payload["report_hash_sha256"] = hashlib.sha256(canonical.encode("utf-8")).hexdigest()
    json_path = output_directory / "auto_x_economy_report.json"
    json_partial = json_path.with_suffix(json_path.suffix + ".partial")
    json_partial.write_text(json.dumps(payload, ensure_ascii=False, indent=2, allow_nan=False), encoding="utf-8")
    json_partial.replace(json_path)

    metrics = payload["metrics"]
    exits = metrics["exits"]
    profit_factor = f"{metrics['profit_factor']:.4f}" if metrics["profit_factor"] is not None else "n/a"
    markdown = f"""# AUTO X ECONOMY — replay report

- Signals: {metrics['signals']}
- Fills/trades: {metrics['fills']}
- Win rate: {metrics['win_rate'] * 100:.2f}%
- Avg NET: {metrics['average_net'] * 100:+.4f}%
- Profit Factor: {profit_factor}
- Compound NET: {metrics['compound_net'] * 100:+.4f}%
- Trades/day: {metrics['trades_per_day']:.4f}
- TP / STOP / TIME: {exits.get('TP', 0)} / {exits.get('STOP', 0)} / {exits.get('TIME', 0)}

Комиссии, slippage и правила исполнения включены. Результат является simulation,
не доказательством будущей прибыльности и не разрешением на реальные ордера.
"""
    markdown_path = output_directory / "auto_x_economy_report.md"
    markdown_partial = markdown_path.with_suffix(markdown_path.suffix + ".partial")
    markdown_partial.write_text(markdown, encoding="utf-8")
    markdown_partial.replace(markdown_path)
    return json_path, markdown_path
