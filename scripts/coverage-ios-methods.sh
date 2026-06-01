#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
HARNESS_DIR="$ROOT_DIR/packages/methods/ios-unit-tests"
OUT_ROOT="$ROOT_DIR/coverage-reports/ios-methods"
BUNDLE_GEMFILE="${BUNDLE_GEMFILE:-$ROOT_DIR/Gemfile}"
SKIP_POD_INSTALL="${SKIP_POD_INSTALL:-0}"
XCODEBUILD_TIMEOUT_SECONDS="${XCODEBUILD_TIMEOUT_SECONDS:-900}"

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
        | sed -E 's/^[[:space:]]+//' \
        | sed -E 's/[[:space:]]+\([0-9A-Fa-f-]{36}\)[[:space:]]+\((Booted|Shutdown)\).*$//' \
        | sed -E 's/[[:space:]]+$//')"
      destination="platform=iOS Simulator,OS=$runtime,name=$sim_name"
    fi
  done < <(xcrun simctl list devices available 2>/dev/null)

  if [[ -n "$destination" ]]; then
    echo "$destination"
  else
    echo "platform=iOS Simulator"
  fi
}

IOS_DESTINATION="${IOS_DESTINATION:-$(pick_ios_destination)}"
FALLBACK_DESTINATION="platform=iOS Simulator"

echo "[coverage:ios-methods] Using primary destination: $IOS_DESTINATION"

rm -rf "$OUT_ROOT"
mkdir -p "$OUT_ROOT"

run_with_timeout() {
  local timeout_seconds="$1"
  shift
  local log_path="$1"
  shift

  : > "$log_path"
  echo "[coverage:ios-methods] xcodebuild log: $log_path"
  "$@" > "$log_path" 2>&1 &
  local command_pid=$!
  (
    local elapsed=0
    while sleep 60; do
      if ! kill -0 "$command_pid" 2>/dev/null; then
        exit 0
      fi
      elapsed=$((elapsed + 60))
      echo "[coverage:ios-methods] Command still running after ${elapsed}s: $*" >&2
      if (( elapsed >= timeout_seconds )); then
        echo "[coverage:ios-methods] Command timed out after ${timeout_seconds}s: $*" | tee -a "$log_path" >&2
        kill "$command_pid" 2>/dev/null || true
        sleep 10
        kill -9 "$command_pid" 2>/dev/null || true
        exit 0
      fi
    done
  ) &
  local monitor_pid=$!

  local status=0
  wait "$command_pid" || status=$?
  kill "$monitor_pid" 2>/dev/null || true
  wait "$monitor_pid" 2>/dev/null || true
  cat "$log_path"
  return "$status"
}

should_retry_destination() {
  local log_path="$1"

  grep -Eq \
    'Unable to find a device matching the provided destination specifier|Found no destinations|Ineligible destinations' \
    "$log_path"
}

guard_no_sparkling_sdk_dependencies() {
  local import_pattern='(^|[[:space:]])(@testable[[:space:]]+)?import[[:space:]]+Sparkling([[:space:]]|$)|@import[[:space:]]+Sparkling;'
  local podspec_dependency_pattern="dependency[[:space:]]+['\"]Sparkling(['\"]|/)|dependency[[:space:]]+['\"]Sparkling-DebugTool"

  if rg -n --glob '*.{swift,h,m,mm}' "$import_pattern" "$ROOT_DIR/packages/methods"/*/ios 2>/dev/null; then
    echo "[coverage:ios-methods] Method sources/tests must not import Sparkling SDK directly."
    return 1
  fi

  if rg -n "$podspec_dependency_pattern" "$ROOT_DIR/packages/methods"/*/ios/*.podspec 2>/dev/null; then
    echo "[coverage:ios-methods] Method podspecs may depend on SparklingMethod, but not Sparkling SDK or Sparkling-DebugTool."
    return 1
  fi
}

run_pod_install() {
  if [[ "$SKIP_POD_INSTALL" == "1" || "$SKIP_POD_INSTALL" == "true" ]]; then
    echo "[coverage:ios-methods] Skipping pod install"
    return 0
  fi

  echo "[coverage:ios-methods] Running pod install in $HARNESS_DIR"

  if [[ -f "$BUNDLE_GEMFILE" && -x "$(command -v bundle)" ]]; then
    (
      cd "$HARNESS_DIR"
      BUNDLE_GEMFILE="$BUNDLE_GEMFILE" bundle exec pod install
    )
  else
    (
      cd "$HARNESS_DIR"
      pod install
    )
  fi
}

guard_lockfile() {
  local lockfile="$HARNESS_DIR/Podfile.lock"

  if [[ ! -f "$lockfile" ]]; then
    return 0
  fi

  if grep -E '^[[:space:]]+- Sparkling \(' "$lockfile"; then
    echo "[coverage:ios-methods] Independent method tests must not install the Sparkling SDK pod."
    return 1
  fi

  if grep -E '^[[:space:]]+- Sparkling-DebugTool([ /(:]|$)' "$lockfile"; then
    echo "[coverage:ios-methods] Independent method tests must not install Sparkling-DebugTool."
    return 1
  fi

  if grep -F 'packages/sparkling-sdk' "$lockfile"; then
    echo "[coverage:ios-methods] Independent method tests must not resolve packages/sparkling-sdk."
    return 1
  fi
}

discover_test_schemes() {
  find "$HARNESS_DIR/Pods" -maxdepth 2 -name '*.xcodeproj' -type d | sort | while IFS= read -r project; do
    if [[ "$(basename "$project")" == "Pods.xcodeproj" ]]; then
      continue
    fi

    xcodebuild -list -json -project "$project" 2>/dev/null \
      | ruby -rjson -e 'data = JSON.parse(STDIN.read); (data.dig("project", "schemes") || []).select { |scheme| scheme.end_with?("-Unit-Tests") }.sort.each { |scheme| puts "#{ARGV[0]}\t#{scheme}" }' "$project"
  done
}

run_scheme_coverage() {
  local project="$1"
  local scheme="$2"
  local safe_name
  local out_dir
  local xcresult_path
  local used_destination="$IOS_DESTINATION"
  local xcodebuild_test_args=(
    -parallel-testing-enabled NO
    -maximum-concurrent-test-simulator-destinations 1
    -maximum-concurrent-test-device-destinations 1
  )

  safe_name="$(printf '%s' "$scheme" | tr -c '[:alnum:]._-' '_')"
  out_dir="$OUT_ROOT/$safe_name"
  xcresult_path="$out_dir/TestResults.xcresult"

  rm -rf "$xcresult_path"
  mkdir -p "$out_dir"

  echo "[coverage:ios-methods] Running xcodebuild for $scheme"

  local run_ok=0
  local primary_log="$out_dir/xcodebuild-primary.log"
  if run_with_timeout "$XCODEBUILD_TIMEOUT_SECONDS" "$primary_log" xcodebuild -project "$project" \
    -scheme "$scheme" \
    -destination "$IOS_DESTINATION" \
    -destination-timeout 30 \
    "${xcodebuild_test_args[@]}" \
    -enableCodeCoverage YES \
    -resultBundlePath "$xcresult_path" \
    test; then
    run_ok=1
  else
    if should_retry_destination "$primary_log"; then
      echo "[coverage:ios-methods] Primary destination failed for $scheme, retrying with fallback: $FALLBACK_DESTINATION"
      rm -rf "$xcresult_path"
      local fallback_log="$out_dir/xcodebuild-fallback.log"
      if run_with_timeout "$XCODEBUILD_TIMEOUT_SECONDS" "$fallback_log" xcodebuild -project "$project" \
        -scheme "$scheme" \
        -destination "$FALLBACK_DESTINATION" \
        -destination-timeout 30 \
        "${xcodebuild_test_args[@]}" \
        -enableCodeCoverage YES \
        -resultBundlePath "$xcresult_path" \
        test; then
        run_ok=1
        used_destination="$FALLBACK_DESTINATION"
      fi
    else
      echo "[coverage:ios-methods] xcodebuild failed for $scheme; not retrying because this was not a destination resolution failure"
    fi
  fi

  if [[ "$run_ok" -ne 1 ]]; then
    echo "[coverage:ios-methods] Failed to produce coverage for $scheme"
    return 1
  fi

  xcrun xccov view --report "$xcresult_path" > "$out_dir/summary.txt"
  xcrun xccov view --report --json "$xcresult_path" > "$out_dir/summary.json"
  echo "$used_destination" > "$out_dir/destination.txt"
}

guard_no_sparkling_sdk_dependencies
run_pod_install
guard_lockfile

scheme_entries=()
while IFS= read -r entry; do
  scheme_entries+=("$entry")
done < <(discover_test_schemes)

if [[ ${#scheme_entries[@]} -eq 0 ]]; then
  echo "[coverage:ios-methods] No CocoaPods test schemes were discovered."
  exit 1
fi

failures=()
for entry in "${scheme_entries[@]}"; do
  project="${entry%%$'\t'*}"
  scheme="${entry#*$'\t'}"
  run_scheme_coverage "$project" "$scheme" || failures+=("$scheme")
done

if [[ ${#failures[@]} -gt 0 ]]; then
  echo "[coverage:ios-methods] Failed schemes: ${failures[*]}"
  exit 1
fi

echo "[coverage:ios-methods] Reports generated at: $OUT_ROOT"
