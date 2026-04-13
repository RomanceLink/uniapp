#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
LABELS_FILE="${PROJECT_ROOT}/training/meter_ocr/annotations/labels.txt"
OUT_ROOT="${PROJECT_ROOT}/training/meter_ocr/outputs"

python3 "${PROJECT_ROOT}/training/meter_ocr/scripts/split_by_scene.py" \
  --labels "${LABELS_FILE}" \
  --output-dir "${OUT_ROOT}"

mkdir -p "${OUT_ROOT}/led" "${OUT_ROOT}/water_meter"

python3 "${PROJECT_ROOT}/training/meter_ocr/scripts/prepare_dataset.py" \
  --labels "${OUT_ROOT}/led_labels.txt" \
  --output-dir "${OUT_ROOT}/led"

python3 "${PROJECT_ROOT}/training/meter_ocr/scripts/prepare_dataset.py" \
  --labels "${OUT_ROOT}/water_meter_labels.txt" \
  --output-dir "${OUT_ROOT}/water_meter"

echo "[OK] dataset prepared under ${OUT_ROOT}"
