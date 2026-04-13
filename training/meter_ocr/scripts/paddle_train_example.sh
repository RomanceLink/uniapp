#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 3 ]]; then
  echo "Usage: $0 <PADDLEOCR_ROOT> <scene: led|water_meter> <config_path>"
  exit 1
fi

PADDLEOCR_ROOT="$1"
SCENE="$2"
CONFIG_PATH="$3"

cd "${PADDLEOCR_ROOT}"

python3 tools/train.py -c "${CONFIG_PATH}"
python3 tools/export_model.py -c "${CONFIG_PATH}" -o Global.pretrained_model= Global.save_inference_dir="./inference/${SCENE}"

echo "[OK] exported scene=${SCENE}"
