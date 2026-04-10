#!/usr/bin/env python3
from __future__ import annotations

import argparse
import random
from pathlib import Path


ALLOWED_CHARS = set("0123456789.-|")


def validate_label(label: str) -> list[str]:
    errors: list[str] = []
    if not label:
        errors.append("empty label")
    bad = sorted(set(ch for ch in label if ch not in ALLOWED_CHARS))
    if bad:
        errors.append(f"invalid chars: {''.join(bad)}")
    if "||" in label:
        errors.append("contains empty segment")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate and split meter OCR dataset")
    parser.add_argument("--labels", required=True, help="Path to labels txt")
    parser.add_argument("--train-ratio", type=float, default=0.9)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--output-dir", required=True)
    args = parser.parse_args()

    labels_path = Path(args.labels)
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    lines = [line.strip() for line in labels_path.read_text(encoding="utf-8").splitlines() if line.strip()]
    records: list[tuple[str, str]] = []
    failed = False

    for idx, line in enumerate(lines, start=1):
        if "\t" not in line:
            print(f"[ERROR] line {idx}: missing TAB separator")
            failed = True
            continue
        image_path, label = line.split("\t", 1)
        errors = validate_label(label)
        if errors:
            print(f"[ERROR] line {idx}: {image_path}: {'; '.join(errors)}")
            failed = True
            continue
        records.append((image_path, label))

    if failed:
        return 1

    random.seed(args.seed)
    random.shuffle(records)

    split_index = int(len(records) * args.train_ratio)
    train_records = records[:split_index]
    val_records = records[split_index:]

    (output_dir / "train.txt").write_text(
        "\n".join(f"{path}\t{label}" for path, label in train_records),
        encoding="utf-8",
    )
    (output_dir / "val.txt").write_text(
        "\n".join(f"{path}\t{label}" for path, label in val_records),
        encoding="utf-8",
    )

    print(f"[OK] total={len(records)} train={len(train_records)} val={len(val_records)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
