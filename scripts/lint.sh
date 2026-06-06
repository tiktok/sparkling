#!/usr/bin/env bash
# Lint or format iOS Swift / ObjC++ and Android Kotlin sources.
#
# Usage:
#   scripts/lint.sh [--fix] [swift|clang|kotlin|all]
#
# Default target is "all". Without --fix, the script only checks formatting
# and exits non-zero on violations. With --fix, it applies formatting in place.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

FIX=0
TARGET=all

for arg in "$@"; do
  case "$arg" in
    --fix) FIX=1 ;;
    swift|clang|kotlin|all) TARGET="$arg" ;;
    -h|--help)
      sed -n '2,9p' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *)
      echo "Unknown argument: $arg" >&2
      exit 2
      ;;
  esac
done

SWIFT_DIRS=(
  packages/sparkling-sdk/ios/Sparkling/Sources
  packages/sparkling-sdk/ios/SparklingMacro
  packages/sparkling-method/ios/Sources
  packages/sparkling-debug-tool/ios/Sources
  packages/methods/sparkling-media/ios/Sources
  packages/methods/sparkling-media/ios/SparklingMethodTests
  packages/methods/sparkling-navigation/ios/Sources
  packages/methods/sparkling-storage/ios/Sources
  packages/playground/ios/SparklingGo
  packages/playground/ios/SparklingGoTests
  packages/playground/ios/SparklingGoUITests
  packages/playground/ios/SparklingMethodTests
  template/sparkling-app-template/ios/SparklingGo
  template/sparkling-app-template/ios/SparklingGoTests
  template/sparkling-app-template/ios/SparklingGoUITests
  template/sparkling-app-template/ios/SparklingMethodTests
)

require() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "error: '$1' not found in PATH. $2" >&2
    exit 1
  fi
}

resolve_swift_format() {
  if command -v swift-format >/dev/null 2>&1; then
    command -v swift-format
    return 0
  fi

  if command -v xcrun >/dev/null 2>&1; then
    xcrun --find swift-format 2>/dev/null && return 0
  fi

  return 1
}

run_swift() {
  local swift_format
  if ! swift_format="$(resolve_swift_format)"; then
    echo "error: 'swift-format' not found. Install Xcode with swift-format, or run 'brew install swift-format'." >&2
    exit 1
  fi

  if [[ $FIX -eq 1 ]]; then
    echo "==> swift-format format"
    "$swift_format" format --in-place --recursive "${SWIFT_DIRS[@]}"
  else
    echo "==> swift-format lint"
    "$swift_format" lint --recursive "${SWIFT_DIRS[@]}"
  fi
}

run_clang() {
  require clang-format "Install with 'brew install clang-format' (macOS) or 'apt-get install clang-format' (Linux)."
  local files
  files=$(find packages template \( -name '*.m' -o -name '*.mm' -o -name '*.cpp' \) \
    | grep -v Pods | grep -v '/build/' || true)
  if [[ -z "$files" ]]; then
    echo "==> clang-format: no files found"
    return 0
  fi
  if [[ $FIX -eq 1 ]]; then
    echo "==> clang-format -i"
    echo "$files" | xargs clang-format -i
  else
    echo "==> clang-format --dry-run --Werror"
    echo "$files" | xargs clang-format --dry-run --Werror
  fi
}

run_kotlin() {
  require ktlint "Install with 'brew install ktlint' (macOS) or download from https://github.com/pinterest/ktlint/releases (Linux)."
  local files
  files=$(find packages template \( -name '*.kt' -o -name '*.kts' \) \
    | grep -v '/build/' | grep -v '/.gradle/' | grep -v '/node_modules/' || true)
  if [[ -z "$files" ]]; then
    echo "==> ktlint: no files found"
    return 0
  fi
  if [[ $FIX -eq 1 ]]; then
    echo "==> ktlint --format"
    echo "$files" | xargs ktlint --format
  else
    echo "==> ktlint"
    echo "$files" | xargs ktlint
  fi
}

case "$TARGET" in
  swift)  run_swift ;;
  clang)  run_clang ;;
  kotlin) run_kotlin ;;
  all)    run_swift; run_clang; run_kotlin ;;
esac

echo "Done."
