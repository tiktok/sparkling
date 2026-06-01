#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_ROOT="$ROOT_DIR/coverage-reports/ios"
BUNDLE_GEMFILE="${BUNDLE_GEMFILE:-$ROOT_DIR/Gemfile}"
SKIP_POD_INSTALL="${SKIP_POD_INSTALL:-0}"

# Auto-pick an available iPhone simulator if IOS_DESTINATION isn't set,
# so we don't fail on hosts where a specific device (e.g. iPhone 16) isn't installed.
pick_ios_destination() {
  local runtime=""
  local destination=""
  local sim_name

  while IFS= read -r line; do
    if [[ "$line" =~ ^--[[:space:]]iOS[[:space:]]([0-9.]+)[[:space:]]--$ ]]; then
      runtime="${BASH_REMATCH[1]}"
      continue
    fi

    if [[ -n "$runtime" && "$line" =~ ^[[:space:]]+iPhone[[:space:]] ]]; then
      sim_name="$(echo "$line" \
        | sed -E 's/^[[:space:]]+([^(]+)\(.*$/\1/' \
        | sed -E 's/[[:space:]]+$//')"
      destination="platform=iOS Simulator,OS=$runtime,name=$sim_name"
    fi
  done < <(xcrun simctl list devices available 2>/dev/null)

  if [[ -n "$destination" ]]; then
    echo "$destination"
  else
    # Last resort placeholder; xcodebuild may still reject it, but we'll try.
    echo "platform=iOS Simulator"
  fi
}

IOS_DESTINATION="${IOS_DESTINATION:-$(pick_ios_destination)}"
FALLBACK_DESTINATION="platform=iOS Simulator"

echo "[coverage:ios] Using primary destination: $IOS_DESTINATION"

rm -rf "$OUT_ROOT"
mkdir -p "$OUT_ROOT"

run_pod_install() {
  local ios_dir="$1"

  if [[ "$SKIP_POD_INSTALL" == "1" || "$SKIP_POD_INSTALL" == "true" ]]; then
    echo "[coverage:ios] Skipping pod install for $ios_dir"
    return 0
  fi

  echo "[coverage:ios] Running pod install in $ios_dir"

  if [[ -f "$BUNDLE_GEMFILE" && -x "$(command -v bundle)" ]]; then
    (
      cd "$ios_dir"
      BUNDLE_GEMFILE="$BUNDLE_GEMFILE" bundle exec pod install
    )
  else
    (
      cd "$ios_dir"
      pod install
    )
  fi
}

run_ios_coverage() {
  local name="$1"
  local project_path="$2"
  local scheme="$3"
  local out_dir="$OUT_ROOT/$name"
  local xcresult_path="$out_dir/TestResults.xcresult"
  local ios_dir
  local test_selection_args=(
    -only-testing:SparklingGoTests
    -only-testing:SparklingMethodTests
    -skip-testing:SparklingGoUITests
    -skip-testing:SparklingMethodTests/SPKChooseMediaMethodTest
    -skip-testing:SparklingMethodTests/SPKDownloadFileMethodTests
    -skip-testing:SparklingMethodTests/SPKRouterTest
    -skip-testing:SparklingMethodTests/SPKStorageTest
    -skip-testing:SparklingMethodTests/SPKUploadFileMethodTests
  )
  local xcodebuild_test_args=(
    -parallel-testing-enabled NO
    -maximum-concurrent-test-simulator-destinations 1
    -maximum-concurrent-test-device-destinations 1
  )

  ios_dir="$(dirname "$project_path")"

  rm -rf "$xcresult_path"
  mkdir -p "$out_dir"

  run_pod_install "$ios_dir"

  # Prefer the .xcworkspace next to the .xcodeproj so CocoaPods-provided
  # modules (Sparkling, SparklingMethod, Lynx, SDWebImage, ...) resolve.
  local workspace_path="${project_path%.xcodeproj}.xcworkspace"
  local target_args
  local target_label
  if [[ -d "$workspace_path" ]]; then
    target_args=(-workspace "$workspace_path")
    target_label="workspace=$workspace_path"
  else
    target_args=(-project "$project_path")
    target_label="project=$project_path"
  fi

  echo "[coverage:ios] Running xcodebuild for $name ($target_label)"

  local run_ok=0
  local used_destination="$IOS_DESTINATION"
  if xcodebuild "${target_args[@]}" \
    -scheme "$scheme" \
    -destination "$IOS_DESTINATION" \
    -destination-timeout 30 \
    "${xcodebuild_test_args[@]}" \
    -enableCodeCoverage YES \
    -resultBundlePath "$xcresult_path" \
    "${test_selection_args[@]}" \
    test; then
    run_ok=1
  else
    echo "[coverage:ios] Primary destination failed for $name, retrying with fallback: $FALLBACK_DESTINATION"
    rm -rf "$xcresult_path"
    if xcodebuild "${target_args[@]}" \
      -scheme "$scheme" \
      -destination "$FALLBACK_DESTINATION" \
      -destination-timeout 30 \
      "${xcodebuild_test_args[@]}" \
      -enableCodeCoverage YES \
      -resultBundlePath "$xcresult_path" \
      "${test_selection_args[@]}" \
      test; then
      run_ok=1
      used_destination="$FALLBACK_DESTINATION"
    fi
  fi

  if [[ "$run_ok" -ne 1 ]]; then
    echo "[coverage:ios] Failed to produce coverage for $name"
    return 1
  fi

  xcrun xccov view --report "$xcresult_path" > "$out_dir/summary.txt"
  xcrun xccov view --report --json "$xcresult_path" > "$out_dir/summary.json"
  echo "$used_destination" > "$out_dir/destination.txt"
}

failures=()

run_ios_coverage \
  "playground" \
  "$ROOT_DIR/packages/playground/ios/SparklingGo.xcodeproj" \
  "SparklingGo" || failures+=("playground")

run_ios_coverage \
  "sparkling-app-template" \
  "$ROOT_DIR/template/sparkling-app-template/ios/SparklingGo.xcodeproj" \
  "SparklingGo" || failures+=("sparkling-app-template")

if [[ ${#failures[@]} -gt 0 ]]; then
  echo "[coverage:ios] Failed modules: ${failures[*]}"
  exit 1
fi

echo "[coverage:ios] Reports generated at: $OUT_ROOT"
