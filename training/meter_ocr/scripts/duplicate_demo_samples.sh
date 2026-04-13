#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
DATA_ROOT="${PROJECT_ROOT}/training/meter_ocr"
IMAGES_ROOT="${DATA_ROOT}/images"
LABELS_FILE="${DATA_ROOT}/annotations/labels.txt"

LED_SRC="${IMAGES_ROOT}/led/scale_0001.jpg"
WATER_SRC="${IMAGES_ROOT}/water_meter/meter_0001.jpg"

if [[ ! -f "${LED_SRC}" ]]; then
  echo "[ERROR] missing ${LED_SRC}"
  exit 1
fi

if [[ ! -f "${WATER_SRC}" ]]; then
  echo "[ERROR] missing ${WATER_SRC}"
  exit 1
fi

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

cp "${LED_SRC}" "${TMP_DIR}/scale_src.jpg"
cp "${WATER_SRC}" "${TMP_DIR}/meter_src.jpg"

rm -f "${IMAGES_ROOT}"/led/scale_0*.jpg
rm -f "${IMAGES_ROOT}"/water_meter/meter_0*.jpg

for i in $(seq 1 100); do
  led_name=$(printf "scale_%04d.jpg" "${i}")
  meter_name=$(printf "meter_%04d.jpg" "${i}")
  cp "${TMP_DIR}/scale_src.jpg" "${IMAGES_ROOT}/led/${led_name}"
  cp "${TMP_DIR}/meter_src.jpg" "${IMAGES_ROOT}/water_meter/${meter_name}"
done

{
  for i in $(seq 1 100); do
    led_name=$(printf "images/led/scale_%04d.jpg" "${i}")
    meter_name=$(printf "images/water_meter/meter_%04d.jpg" "${i}")
    printf "%s\t%s\n" "${led_name}" "0.23|989.4"
    printf "%s\t%s\n" "${meter_name}" "462796.7589"
  done
} > "${LABELS_FILE}"

echo "[OK] generated demo labels at ${LABELS_FILE}"
echo "[OK] led copies: $(find "${IMAGES_ROOT}/led" -maxdepth 1 -name 'scale_*.jpg' | wc -l | tr -d ' ')"
echo "[OK] water meter copies: $(find "${IMAGES_ROOT}/water_meter" -maxdepth 1 -name 'meter_*.jpg' | wc -l | tr -d ' ')"
