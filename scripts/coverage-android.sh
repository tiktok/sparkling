#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ANDROID_DIR="$ROOT_DIR/packages/playground/android"
OUT_ROOT="$ROOT_DIR/coverage-reports/android"

rm -rf "$OUT_ROOT"
mkdir -p "$OUT_ROOT"

echo "[coverage:android] Running JaCoCo tasks from $ANDROID_DIR"

cd "$ANDROID_DIR"

./gradlew \
  :sparkling:jacocoTestReport \
  :sparkling-method:jacocoTestReport \
  :sparkling-media:jacocoTestReport \
  :sparkling-navigation:jacocoTestReport \
  :sparkling-storage:jacocoTestReport \
  :app:jacocoTestReport \
  --no-daemon

copy_report() {
  local module_name="$1"
  local module_dir="$2"
  local xml_path="$module_dir/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
  local html_path="$module_dir/build/reports/jacoco/jacocoTestReport/html"
  local module_out="$OUT_ROOT/$module_name"

  mkdir -p "$module_out"

  if [[ -f "$xml_path" ]]; then
    cp "$xml_path" "$module_out/jacoco.xml"
  fi

  if [[ -d "$html_path" ]]; then
    rm -rf "$module_out/html"
    cp -R "$html_path" "$module_out/html"
  fi
}

copy_report "sparkling-sdk" "$ROOT_DIR/packages/sparkling-sdk/android/sparkling"
copy_report "sparkling-method" "$ROOT_DIR/packages/sparkling-method/android"
copy_report "sparkling-media" "$ROOT_DIR/packages/methods/sparkling-media/android"
copy_report "sparkling-navigation" "$ROOT_DIR/packages/methods/sparkling-navigation/android"
copy_report "sparkling-storage" "$ROOT_DIR/packages/methods/sparkling-storage/android"
copy_report "playground-app" "$ROOT_DIR/packages/playground/android/app"

echo "[coverage:android] Reports copied to: $OUT_ROOT"
