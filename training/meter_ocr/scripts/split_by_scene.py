#!/usr/bin/env python3
from __future__ import annotations

import argparse
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser(description="Split mixed labels into led and water_meter label files")
    parser.add_argument("--labels", required=True, help="Path to labels txt")
    parser.add_argument("--output-dir", required=True, help="Output directory")
    args = parser.parse_args()

    labels_path = Path(args.labels)
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    led_records: list[str] = []
    water_records: list[str] = []

    for raw in labels_path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line:
            continue
        image_path, label = line.split("\t", 1)
        lower = image_path.lower()
        if "/led/" in lower or "\\led\\" in lower or "scale_" in lower:
            led_records.append(f"{image_path}\t{label}")
        elif "/water_meter/" in lower or "\\water_meter\\" in lower or "meter_" in lower:
            water_records.append(f"{image_path}\t{label}")

    (output_dir / "led_labels.txt").write_text("\n".join(led_records), encoding="utf-8")
    (output_dir / "water_meter_labels.txt").write_text("\n".join(water_records), encoding="utf-8")

    print(f"[OK] led={len(led_records)} water_meter={len(water_records)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
