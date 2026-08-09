#!/usr/bin/env bash
#
# Build and run the Sparkling Android compatibility acceptance matrix on a
# freshly leased Lynx Sandbox device.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_ROOT="$REPO_ROOT/packages/playground/android"
APP_APK="$ANDROID_ROOT/app/build/outputs/apk/debug/app-debug.apk"
TEST_APK="$ANDROID_ROOT/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
PACKAGE="com.tiktok.sparkling.playground"
RUNNER="$PACKAGE.test/androidx.test.runner.AndroidJUnitRunner"
SANDBOX_BASE_URL="${SANDBOX_BASE_URL:-https://lynx-sandbox.byted.org}"
ANDROID_HOME="${ANDROID_HOME:-/data00/home/xuan.huang/android-sdk}"
EVIDENCE_ROOT="${EVIDENCE_ROOT:-/tmp/sparkling-android-device-acceptance-$(date +%s)}"
ISSUER="${ISSUER:-$(git -C "$REPO_ROOT" config --get user.email || true)}"
ISSUE_ID="${ISSUE_ID:-sparkling-android-device-acceptance-$(date +%s)}"
EXPECTED_HEAD="${EXPECTED_HEAD:-}"
SERIAL=""
CONNECTED=0
RELEASED=0

if [[ -z "$ISSUER" ]]; then
  ISSUER="sparkling-android-device-acceptance"
fi

mkdir -p "$EVIDENCE_ROOT/logs" "$EVIDENCE_ROOT/screenshots"

release_device() {
  local strict=${1:-0}
  local release_failed=0
  if [[ -n "$SERIAL" && $RELEASED -eq 0 ]]; then
    local encoded_serial
    encoded_serial="$(
      python3 - "$SERIAL" <<'PY'
import sys
import urllib.parse

print(urllib.parse.quote(sys.argv[1], safe=""))
PY
    )"
    if ! curl -fsS -X DELETE \
      "$SANDBOX_BASE_URL/pool/lease?serial=$encoded_serial" \
      >"$EVIDENCE_ROOT/release.json"; then
      echo "error: failed to release Sandbox lease $SERIAL" >&2
      release_failed=1
    elif ! python3 - "$EVIDENCE_ROOT/release.json" "$SERIAL" <<'PY'
import json
import pathlib
import sys

response = json.loads(pathlib.Path(sys.argv[1]).read_text())
if response.get("acquired") != sys.argv[2]:
    raise SystemExit(f"unexpected release response: {response!r}")
PY
    then
      echo "error: Sandbox release response did not confirm $SERIAL" >&2
      release_failed=1
    fi
    if [[ $CONNECTED -eq 1 ]]; then
      adb disconnect "$SERIAL" \
        >"$EVIDENCE_ROOT/logs/adb-disconnect.txt" 2>&1 || true
    fi
    RELEASED=1
  fi
  if [[ $strict -eq 1 && $release_failed -ne 0 ]]; then
    return 1
  fi
  return "$release_failed"
}

cleanup() {
  local exit_code=$?
  set +e
  release_device 0
  local release_code=$?
  if [[ $exit_code -eq 0 && $release_code -ne 0 ]]; then
    exit "$release_code"
  fi
  exit "$exit_code"
}
trap cleanup EXIT

run_instrumentation() {
  local name=$1
  local class_filter=$2
  local expected_tests=$3
  local marker=$4
  local instrumentation_log="$EVIDENCE_ROOT/logs/$name.instrumentation.txt"
  local logcat_log="$EVIDENCE_ROOT/logs/$name.logcat.txt"

  adb -s "$SERIAL" shell pm clear "$PACKAGE" >/dev/null
  adb -s "$SERIAL" logcat -c
  adb -s "$SERIAL" shell am instrument -w -r \
    -e class "$class_filter" \
    "$RUNNER" | tee "$instrumentation_log"
  adb -s "$SERIAL" logcat -d -v threadtime >"$logcat_log"

  grep -Fq "OK ($expected_tests test" "$instrumentation_log"
  grep -Fq "INSTRUMENTATION_CODE: -1" "$instrumentation_log"
  if grep -Fq "FAILURES!!!" "$instrumentation_log"; then
    echo "error: instrumentation failed for $name" >&2
    return 1
  fi
  grep -Fq "$marker" "$logcat_log"
}

echo "==> Build Playground APKs on $(hostname)"
ACTUAL_HEAD="$(git -C "$REPO_ROOT" rev-parse HEAD)"
if [[ -n "$EXPECTED_HEAD" && "$ACTUAL_HEAD" != "$EXPECTED_HEAD" ]]; then
  echo "error: expected HEAD $EXPECTED_HEAD, got $ACTUAL_HEAD" >&2
  exit 1
fi
if [[ -n "$(git -C "$REPO_ROOT" status --short --untracked-files=all)" ]]; then
  echo "error: acceptance must run from a clean checkout" >&2
  git -C "$REPO_ROOT" status --short --untracked-files=all >&2
  exit 1
fi
(
  cd "$ANDROID_ROOT"
  unset ANDROID_SDK_ROOT
  export ANDROID_HOME
  ./gradlew --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest
) 2>&1 | tee "$EVIDENCE_ROOT/logs/gradle-build.txt"

sha256sum "$APP_APK" "$TEST_APK" \
  >"$EVIDENCE_ROOT/apk-sha256.txt"
unzip -p "$APP_APK" assets/device-acceptance.lynx.bundle |
  sha256sum >"$EVIDENCE_ROOT/bundle-sha256.txt"
printf '%s\n' "$ACTUAL_HEAD" >"$EVIDENCE_ROOT/git-head.txt"
git -C "$REPO_ROOT" status --short --untracked-files=all \
  >"$EVIDENCE_ROOT/git-status.txt"

echo "==> Lease a fresh Lynx Sandbox device"
curl -fsS -X POST "$SANDBOX_BASE_URL/pool/lease" \
  -H "Content-Type: application/json" \
  -H "X-Issuer: $ISSUER" \
  -H "X-Issue-Id: $ISSUE_ID" \
  -d '{}' |
  tee "$EVIDENCE_ROOT/lease.json"
SERIAL="$(
  python3 - "$EVIDENCE_ROOT/lease.json" <<'PY'
import json
import pathlib
import sys

print(json.loads(pathlib.Path(sys.argv[1]).read_text())["acquired"])
PY
)"
printf '%s\n' "$SERIAL" >"$EVIDENCE_ROOT/serial.txt"

adb connect "$SERIAL" | tee "$EVIDENCE_ROOT/logs/adb-connect.txt"
CONNECTED=1
adb -s "$SERIAL" wait-for-device
adb devices -l | grep -F "$SERIAL" \
  >"$EVIDENCE_ROOT/adb-device.txt"
{
  echo "product=$(adb -s "$SERIAL" shell getprop ro.product.name | tr -d '\r')"
  echo "model=$(adb -s "$SERIAL" shell getprop ro.product.model | tr -d '\r')"
  echo "release=$(adb -s "$SERIAL" shell getprop ro.build.version.release | tr -d '\r')"
  echo "sdk=$(adb -s "$SERIAL" shell getprop ro.build.version.sdk | tr -d '\r')"
} >"$EVIDENCE_ROOT/device.txt"

echo "==> Install APKs"
adb -s "$SERIAL" install -r -t "$APP_APK" |
  tee "$EVIDENCE_ROOT/logs/install-app.txt"
adb -s "$SERIAL" install -r -t "$TEST_APK" |
  tee "$EVIDENCE_ROOT/logs/install-test.txt"

echo "==> Run viewport, density, thread, and fetcher gates"
run_instrumentation \
  "compatibility" \
  "$PACKAGE.AndroidCompatibilityDeviceGateTest" \
  3 \
  "COMPAT_GATE event=pass"
grep -Fq \
  "COMPAT_GATE event=unsafe_rejected" \
  "$EVIDENCE_ROOT/logs/compatibility.logcat.txt"
grep -Fq \
  "COMPAT_GATE event=thread_matrix" \
  "$EVIDENCE_ROOT/logs/compatibility.logcat.txt"

echo "==> Run failed-view retry gate"
run_instrumentation \
  "retry" \
  "$PACKAGE.SparklingRetryDeviceGateTest" \
  1 \
  "RETRY_GATE event=pass"
adb -s "$SERIAL" pull \
  "/sdcard/Android/data/$PACKAGE/files/retry-validation" \
  "$EVIDENCE_ROOT/screenshots/retry" \
  >"$EVIDENCE_ROOT/logs/pull-retry-screenshots.txt"

echo "==> Run orientation matrix"
ORIENTATION_CLASS="$PACKAGE.PlaygroundOrientationDeviceTest"
for test_case in \
  typedLandscapeFullPage \
  typedPortraitFullPage \
  explicitSystemOverridesGlobalLandscape \
  canonicalScreenOrientationLandscape \
  unsetUsesAndroidSystemBehavior \
  embeddedLandscapePolicyDoesNotChangeHostActivity; do
  run_instrumentation \
    "orientation-$test_case" \
    "$ORIENTATION_CLASS#$test_case" \
    1 \
    "ORIENTATION_GATE event="
  if [[ "$test_case" == "typedLandscapeFullPage" || "$test_case" == "typedPortraitFullPage" ]]; then
    adb -s "$SERIAL" pull \
      "/sdcard/Android/data/$PACKAGE/files/orientation-validation" \
      "$EVIDENCE_ROOT/screenshots/orientation-$test_case" \
      >"$EVIDENCE_ROOT/logs/pull-$test_case-screenshot.txt"
  fi
done

find "$EVIDENCE_ROOT/screenshots" -type f -print0 |
  sort -z |
  xargs -0 sha256sum >"$EVIDENCE_ROOT/screenshot-sha256.txt"

cat >"$EVIDENCE_ROOT/verdict.txt" <<EOF
PASS
serial=$SERIAL
compatibility_tests=3
retry_tests=1
orientation_tests=6
total_tests=10
EOF

release_device 1
trap - EXIT

archive="$EVIDENCE_ROOT.tar.gz"
tar -C "$(dirname "$EVIDENCE_ROOT")" \
  -czf "$archive" "$(basename "$EVIDENCE_ROOT")"
sha256sum "$archive" >"$archive.sha256"

echo "PASS: 10 Android compatibility acceptance tests"
echo "Evidence: $EVIDENCE_ROOT"
echo "Archive: $archive"
