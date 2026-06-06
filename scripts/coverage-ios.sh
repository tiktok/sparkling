#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_ROOT="$ROOT_DIR/coverage-reports/ios"
BUNDLE_GEMFILE="${BUNDLE_GEMFILE:-$ROOT_DIR/Gemfile}"
SKIP_POD_INSTALL="${SKIP_POD_INSTALL:-0}"
XCODEBUILD_TIMEOUT_SECONDS="${XCODEBUILD_TIMEOUT_SECONDS:-900}"
IOS_COVERAGE_APP_HOSTS="${IOS_COVERAGE_APP_HOSTS:-playground sparkling-app-template}"

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
        | sed -E 's/^[[:space:]]+//' \
        | sed -E 's/[[:space:]]+\([0-9A-Fa-f-]{36}\)[[:space:]]+\((Booted|Shutdown)\).*$//' \
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

run_with_timeout() {
  local timeout_seconds="$1"
  shift
  local log_path="$1"
  shift

  : > "$log_path"
  echo "[coverage:ios] xcodebuild log: $log_path"
  "$@" > "$log_path" 2>&1 &
  local command_pid=$!
  (
    local elapsed=0
    while sleep 60; do
      if ! kill -0 "$command_pid" 2>/dev/null; then
        exit 0
      fi
      elapsed=$((elapsed + 60))
      echo "[coverage:ios] Command still running after ${elapsed}s: $*" >&2
      if (( elapsed >= timeout_seconds )); then
        echo "[coverage:ios] Command timed out after ${timeout_seconds}s: $*" | tee -a "$log_path" >&2
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

prepare_coverage_scheme() {
  local project_path="$1"
  local scheme="$2"
  local test_target="$3"
  COVERAGE_SCHEME=""
  COVERAGE_SCHEME_PATH=""

  local scheme_dir="$project_path/xcshareddata/xcschemes"
  local source_scheme="$scheme_dir/$scheme.xcscheme"
  local coverage_scheme="${scheme}Coverage"
  local coverage_scheme_path="$scheme_dir/$coverage_scheme.xcscheme"

  if [[ ! -f "$source_scheme" ]]; then
    echo "[coverage:ios] Missing source scheme: $source_scheme"
    return 1
  fi

  if ! ruby - "$source_scheme" "$coverage_scheme_path" "$test_target" <<'RUBY'
require 'rexml/document'
require 'rexml/formatters/pretty'

source_scheme, coverage_scheme, test_target = ARGV
doc = REXML::Document.new(File.read(source_scheme))
testables = REXML::XPath.first(doc, '//TestAction/Testables')
removed = 0
kept = 0

if testables
  testables.elements.to_a('TestableReference').each do |testable|
    ref = REXML::XPath.first(testable, 'BuildableReference')
    blueprint_name = ref&.attributes&.[]('BlueprintName').to_s
    buildable_name = ref&.attributes&.[]('BuildableName').to_s
    if blueprint_name == test_target || buildable_name == "#{test_target}.xctest"
      kept += 1
      next
    end

    testables.delete_element(testable)
    removed += 1
  end
end

raise "#{test_target} was not found in #{source_scheme}" if kept.zero?

File.open(coverage_scheme, 'w') do |file|
  file.write(%(<?xml version="1.0" encoding="UTF-8"?>\n))
  formatter = REXML::Formatters::Pretty.new(3)
  formatter.compact = true
  formatter.write(doc.root, file)
  file.write("\n")
end
RUBY
  then
    echo "[coverage:ios] Failed to prepare coverage scheme from $source_scheme"
    return 1
  fi

  COVERAGE_SCHEME="$coverage_scheme"
  COVERAGE_SCHEME_PATH="$coverage_scheme_path"
  echo "[coverage:ios] Using temporary coverage scheme: $coverage_scheme ($test_target only)"
}

cleanup_coverage_scheme() {
  local coverage_scheme_path="$1"

  if [[ -n "$coverage_scheme_path" ]]; then
    rm -f "$coverage_scheme_path"
  fi
}

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

prepare_coverage_podfile() {
  local ios_dir="$1"
  COVERAGE_PODFILE_BACKUP_DIR=""

  if [[ "$ios_dir" != "$ROOT_DIR/template/sparkling-app-template/ios" ]]; then
    return 0
  fi

  local backup_dir
  backup_dir="$(mktemp -d "${TMPDIR:-/tmp}/sparkling-ios-podfile.XXXXXX")"
  cp "$ios_dir/Podfile" "$backup_dir/Podfile"
  if [[ -f "$ios_dir/Podfile.lock" ]]; then
    cp "$ios_dir/Podfile.lock" "$backup_dir/Podfile.lock"
  fi

  if ! ruby - "$ios_dir/Podfile" <<'RUBY'
podfile = ARGV.fetch(0)
content = File.read(podfile)
replacements = {
  /pod 'SparklingMethod', '[^']+', :subspecs =>/ =>
    "pod 'SparklingMethod', :path => '../../../packages/sparkling-method/ios', :subspecs =>",
  /pod 'Sparkling-Router', '[^']+'/ =>
    "pod 'Sparkling-Router', :path => '../../../packages/methods/sparkling-navigation/ios'",
  /pod 'Sparkling-DebugTool', '[^']+'/ =>
    "pod 'Sparkling-DebugTool', :path => '../../../packages/sparkling-debug-tool/ios'",
  /pod 'Sparkling', '[^']+'/ =>
    "pod 'Sparkling', :path => '../../../packages/sparkling-sdk/ios/Sparkling'",
  /pod 'SparklingMacro', '[^']+'/ =>
    "pod 'SparklingMacro', :path => '../../../packages/sparkling-sdk/ios/SparklingMacro'",
}
replacements.each do |pattern, replacement|
  content = content.gsub(pattern, replacement)
end
File.write(podfile, content)
RUBY
  then
    cp "$backup_dir/Podfile" "$ios_dir/Podfile"
    if [[ -f "$backup_dir/Podfile.lock" ]]; then
      cp "$backup_dir/Podfile.lock" "$ios_dir/Podfile.lock"
    fi
    rm -rf "$backup_dir"
    echo "[coverage:ios] Failed to prepare local template Podfile"
    return 1
  fi

  COVERAGE_PODFILE_BACKUP_DIR="$backup_dir"
  echo "[coverage:ios] Using local Sparkling pods for template coverage"
}

restore_coverage_podfile() {
  local ios_dir="$1"
  local backup_dir="$2"

  if [[ -z "$backup_dir" ]]; then
    return 0
  fi

  cp "$backup_dir/Podfile" "$ios_dir/Podfile"
  if [[ -f "$backup_dir/Podfile.lock" ]]; then
    cp "$backup_dir/Podfile.lock" "$ios_dir/Podfile.lock"
  else
    rm -f "$ios_dir/Podfile.lock"
  fi
  rm -rf "$backup_dir"
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
  )
  local xcodebuild_test_args=(
    -parallel-testing-enabled NO
    -maximum-concurrent-test-simulator-destinations 1
    -maximum-concurrent-test-device-destinations 1
  )

  ios_dir="$(dirname "$project_path")"

  rm -rf "$xcresult_path"
  mkdir -p "$out_dir"

  local podfile_backup_dir=""
  if ! prepare_coverage_podfile "$ios_dir"; then
    return 1
  fi
  podfile_backup_dir="$COVERAGE_PODFILE_BACKUP_DIR"

  if ! run_pod_install "$ios_dir"; then
    restore_coverage_podfile "$ios_dir" "$podfile_backup_dir"
    echo "[coverage:ios] pod install failed for $name"
    return 1
  fi

  # Prefer the .xcworkspace next to the .xcodeproj so CocoaPods-provided
  # modules (Sparkling, SparklingMethod, Lynx, SDWebImage, ...) resolve.
  local workspace_path="${project_path%.xcodeproj}.xcworkspace"
  local target_args
  local target_label
  local coverage_scheme="$scheme"
  local coverage_scheme_path=""
  if [[ -d "$workspace_path" ]]; then
    target_args=(-workspace "$workspace_path")
    target_label="workspace=$workspace_path"
  else
    target_args=(-project "$project_path")
    target_label="project=$project_path"
  fi

  if ! prepare_coverage_scheme "$project_path" "$scheme" "SparklingGoTests"; then
    restore_coverage_podfile "$ios_dir" "$podfile_backup_dir"
    return 1
  fi
  coverage_scheme="$COVERAGE_SCHEME"
  coverage_scheme_path="$COVERAGE_SCHEME_PATH"

  echo "[coverage:ios] Running xcodebuild for $name ($target_label)"

  local run_ok=0
  local used_destination="$IOS_DESTINATION"
  local primary_log="$out_dir/xcodebuild-primary.log"
  if run_with_timeout "$XCODEBUILD_TIMEOUT_SECONDS" "$primary_log" xcodebuild "${target_args[@]}" \
    -scheme "$coverage_scheme" \
    -destination "$IOS_DESTINATION" \
    -destination-timeout 30 \
    "${xcodebuild_test_args[@]}" \
    -enableCodeCoverage YES \
    -resultBundlePath "$xcresult_path" \
    "${test_selection_args[@]}" \
    test; then
    run_ok=1
  else
    if should_retry_destination "$primary_log"; then
      echo "[coverage:ios] Primary destination failed for $name, retrying with fallback: $FALLBACK_DESTINATION"
      rm -rf "$xcresult_path"
      local fallback_log="$out_dir/xcodebuild-fallback.log"
      if run_with_timeout "$XCODEBUILD_TIMEOUT_SECONDS" "$fallback_log" xcodebuild "${target_args[@]}" \
        -scheme "$coverage_scheme" \
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
    else
      echo "[coverage:ios] xcodebuild failed for $name; not retrying because this was not a destination resolution failure"
    fi
  fi

  cleanup_coverage_scheme "$coverage_scheme_path"

  if [[ "$run_ok" -ne 1 ]]; then
    restore_coverage_podfile "$ios_dir" "$podfile_backup_dir"
    echo "[coverage:ios] Failed to produce coverage for $name"
    return 1
  fi

  xcrun xccov view --report "$xcresult_path" > "$out_dir/summary.txt"
  xcrun xccov view --report --json "$xcresult_path" > "$out_dir/summary.json"
  echo "$used_destination" > "$out_dir/destination.txt"
  restore_coverage_podfile "$ios_dir" "$podfile_backup_dir"
}

failures=()

read -r -a app_hosts <<< "$IOS_COVERAGE_APP_HOSTS"

for app_host in "${app_hosts[@]}"; do
  case "$app_host" in
    playground)
      run_ios_coverage \
        "playground" \
        "$ROOT_DIR/packages/playground/ios/SparklingGo.xcodeproj" \
        "SparklingGo" || failures+=("playground")
      ;;
    sparkling-app-template | template)
      run_ios_coverage \
        "sparkling-app-template" \
        "$ROOT_DIR/template/sparkling-app-template/ios/SparklingGo.xcodeproj" \
        "SparklingGo" || failures+=("sparkling-app-template")
      ;;
    *)
      echo "[coverage:ios] Unknown app host: $app_host"
      failures+=("$app_host")
      ;;
  esac
done

if [[ ${#failures[@]} -gt 0 ]]; then
  echo "[coverage:ios] Failed modules: ${failures[*]}"
  exit 1
fi

echo "[coverage:ios] Reports generated at: $OUT_ROOT"
