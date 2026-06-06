#!/usr/bin/env python3
# Copyright (c) 2025 TikTok Pte. Ltd.
# Licensed under the Apache License Version 2.0 that can be found in the
# LICENSE file in the root directory of this source tree.
"""Verify the Sparkling app template end-to-end.

Instead of building the in-repo template directly, this reproduces the real
developer flow: it builds the `create-sparkling-app` CLI from source, runs it
with `--template` pointed at the local template, and then runs `pnpm install`
plus `pnpm run:ios` / `pnpm run:android` on the generated project.

The project is scaffolded *outside* the monorepo so pnpm cannot resolve the
local workspace packages — it must consume the published npm/Maven/CocoaPods
artifacts, exactly as a real consumer would.

On iOS the scaffolded Podfile.lock is deleted before building so a fresh lock
is resolved against the just-published pods; the verified Podfile.lock is then
copied back into the repo so the publish job can ship and commit it.
"""

import argparse
import hashlib
import json
import os
import shutil
import subprocess
import time
import urllib.request
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
TEMPLATE_DIR = REPO_ROOT / "template" / "sparkling-app-template"
CLI_DIR = REPO_ROOT / "packages" / "create-sparkling-app"
CLI_ENTRY = CLI_DIR / "dist" / "index.js"
IOS_PODS_CONFIG = REPO_ROOT / "scripts" / "ios_pods.json"
COCOAPODS_USER_AGENT = "CocoaPods/1.16.2"
COCOAPODS_CDN_ROOT = "https://cdn.cocoapods.org"
COCOAPODS_CDN_BASE = f"{COCOAPODS_CDN_ROOT}/Specs"

# npm packages the scaffolded project installs at the release version.
NPM_PACKAGES = [
    "sparkling-navigation",
    "sparkling-app-cli",
    "sparkling-types",
    "sparkling-method",
]

# Android Sparkling artifacts published to Maven Central.
MAVEN_ARTIFACTS = [
    "com/tiktok/sparkling/sparkling",
    "com/tiktok/sparkling/sparkling-method",
    "com/tiktok/sparkling/sparkling-debug-tool",
]

REQUIRED_CDN_SUBSPECS = {
    "SparklingMethod": {"Core", "Lynx", "DIProvider", "Debug"},
}

IOS_VERIFY_RUN_COMMAND = ["pnpm", "exec", "sparkling-app-cli", "run:ios", "--copy", "--pod-repo-update"]

NAMESPACE = "com.sparkling.templateverify"

# Build retry: preserves the release pipeline's original timeout-retry logic.
BUILD_ATTEMPTS = {"ios": 6, "android": 3}
RETRY_BASE_DELAY = 60


def log(msg=""):
    print(msg, flush=True)


def section(title):
    log("")
    log("=" * 64)
    log(f"  {title}")
    log("=" * 64)


def run(cmd, cwd=None, check=True):
    """Run a command, streaming its output. Raises SystemExit on failure."""
    printable = " ".join(str(c) for c in cmd)
    log(f"$ {printable}  (cwd={cwd or os.getcwd()})")
    result = subprocess.run(cmd, cwd=str(cwd) if cwd else None)
    if check and result.returncode != 0:
        raise SystemExit(f"::error::Command failed (exit {result.returncode}): {printable}")
    return result.returncode


def capture(cmd, cwd=None):
    result = subprocess.run(
        cmd,
        cwd=str(cwd) if cwd else None,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
    )
    return result.returncode, result.stdout or ""


def http_ok(url):
    try:
        with urllib.request.urlopen(url, timeout=30) as resp:
            return 200 <= resp.status < 300
    except Exception:
        return False


def read_url(url):
    req = urllib.request.Request(
        url,
        headers={"User-Agent": COCOAPODS_USER_AGENT},
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        return resp.read().decode("utf-8")


def read_json_url(url):
    return json.loads(read_url(url))


def cocoapods_cdn_shard(pod):
    digest = hashlib.md5(pod.encode("utf-8")).hexdigest()
    return digest[0], digest[1], digest[2]


def cocoapods_cdn_spec_url(pod, version):
    shard = cocoapods_cdn_shard(pod)
    return f"{COCOAPODS_CDN_BASE}/{shard[0]}/{shard[1]}/{shard[2]}/{pod}/{version}/{pod}.podspec.json"


def cocoapods_cdn_versions_url(pod):
    shard = cocoapods_cdn_shard(pod)
    return f"{COCOAPODS_CDN_ROOT}/all_pods_versions_{shard[0]}_{shard[1]}_{shard[2]}.txt"


def cocoapods_cdn_index_has_version(index_text, pod, version):
    prefix = f"{pod}/"
    for line in index_text.splitlines():
        line = line.strip()
        if not line.startswith(prefix):
            continue
        versions = line.split("/")[1:]
        return version in versions
    return False


def cocoapods_cdn_spec_ready(pod, version, fetch_json=read_json_url, fetch_text=read_url):
    url = cocoapods_cdn_spec_url(pod, version)
    try:
        spec = fetch_json(url)
    except Exception as err:
        return False, f"{pod} {version} CDN spec unavailable at {url}: {err}"

    actual_version = str(spec.get("version", ""))
    if actual_version != version:
        return False, f"{pod} CDN spec version mismatch: expected {version}, got {actual_version or '<missing>'}"

    required_subspecs = REQUIRED_CDN_SUBSPECS.get(pod, set())
    if required_subspecs:
        present_subspecs = {
            str(subspec.get("name", ""))
            for subspec in spec.get("subspecs", [])
            if isinstance(subspec, dict)
        }
        missing_subspecs = sorted(required_subspecs - present_subspecs)
        if missing_subspecs:
            return False, f"{pod} CDN spec missing subspec(s): {', '.join(missing_subspecs)}"

    versions_url = cocoapods_cdn_versions_url(pod)
    try:
        versions_index = fetch_text(versions_url)
    except Exception as err:
        return False, f"{pod} CDN versions index unavailable at {versions_url}: {err}"

    if not cocoapods_cdn_index_has_version(versions_index, pod, version):
        return False, f"{pod} CDN versions index missing {version} at {versions_url}"

    return True, f"{pod} {version} CDN spec and versions index ready"


# ──────────────────────────────────────────────────────────────
# Artifact availability waits (kept from the original verify jobs)
# ──────────────────────────────────────────────────────────────

def wait_for_npm(version, attempts=30, delay=10):
    section("Waiting for npm packages")
    missing = []
    for pkg in NPM_PACKAGES:
        found = False
        for i in range(1, attempts + 1):
            rc, out = capture(["npm", "view", f"{pkg}@{version}", "version"])
            if rc == 0 and version in out:
                log(f"  npm ok: {pkg}@{version} (after {i} check(s))")
                found = True
                break
            log(f"  waiting for {pkg}@{version}... ({i}/{attempts}, next check in {delay}s)")
            time.sleep(delay)
        if not found:
            log(f"::error::{pkg}@{version} not available on npm after {attempts * delay}s")
            missing.append(pkg)
    if missing:
        raise SystemExit(f"::error::npm packages unavailable: {', '.join(missing)}")


def wait_for_maven(version, attempts=90, delay=20):
    section("Waiting for Android Maven Central artifacts")
    missing = []
    for artifact in MAVEN_ARTIFACTS:
        base = artifact.rsplit("/", 1)[1]
        url = f"https://repo1.maven.org/maven2/{artifact}/{version}/{base}-{version}.pom"
        found = False
        for i in range(1, attempts + 1):
            if http_ok(url):
                log(f"  maven ok: {artifact.replace('/', '.')}:{version} (after {i} check(s))")
                found = True
                break
            log(f"  waiting for {artifact.replace('/', '.')}:{version}... ({i}/{attempts}, next check in {delay}s)")
            time.sleep(delay)
        if not found:
            log(f"::error::{artifact.replace('/', '.')}:{version} not on Maven Central after {attempts * delay}s")
            missing.append(artifact)
    if missing:
        raise SystemExit("::error::Maven artifacts unavailable")


def wait_for_cocoapods_cdn(version, attempts=120, delay=30):
    section("Waiting for CocoaPods CDN podspecs")
    pods = [entry["pod_name"] for entry in json.loads(IOS_PODS_CONFIG.read_text())]
    pending = list(pods)
    last_reasons = {}

    for i in range(1, attempts + 1):
        next_pending = []
        for pod in pending:
            ready, reason = cocoapods_cdn_spec_ready(pod, version)
            if ready:
                log(f"  cdn ok: {pod} {version} (after {i} check(s))")
                continue
            last_reasons[pod] = reason
            next_pending.append(pod)
            log(f"  waiting for CDN {pod} {version}... ({i}/{attempts}, next check in {delay}s): {reason}")

        pending = next_pending
        if not pending:
            return
        if i < attempts:
            time.sleep(delay)

    for pod in pending:
        log(f"::error::{pod} {version} not ready on CocoaPods CDN after {attempts * delay}s: {last_reasons.get(pod, '<unknown>')}")
    raise SystemExit("::error::CocoaPods CDN podspecs unavailable")


def wait_for_trunk(version, attempts=20, delay=30):
    section("Waiting for iOS pods on CocoaPods trunk")
    pods = [entry["pod_name"] for entry in json.loads(IOS_PODS_CONFIG.read_text())]
    missing = []
    for pod in pods:
        found = False
        for i in range(1, attempts + 1):
            rc, out = capture(["bundle", "exec", "pod", "trunk", "info", pod], cwd=REPO_ROOT)
            if rc == 0 and version in out:
                log(f"  trunk ok: {pod} {version} (after {i} check(s))")
                found = True
                break
            log(f"  waiting for {pod} {version}... ({i}/{attempts}, next check in {delay}s)")
            time.sleep(delay)
        if not found:
            log(f"::error::{pod} {version} not found on CocoaPods trunk after {attempts * delay}s")
            missing.append(pod)
    if missing:
        raise SystemExit("::error::CocoaPods trunk pods unavailable")
    wait_for_cocoapods_cdn(version)


# ──────────────────────────────────────────────────────────────
# Build the CLI and scaffold a fresh project from the local template
# ──────────────────────────────────────────────────────────────

def build_cli():
    section("Building create-sparkling-app CLI from source")
    run(["pnpm", "install", "--frozen-lockfile"], cwd=REPO_ROOT)
    run(["pnpm", "--filter", "create-sparkling-app", "build"], cwd=REPO_ROOT)
    if not CLI_ENTRY.exists():
        raise SystemExit(f"::error::CLI build did not produce {CLI_ENTRY}")
    log(f"CLI built: {CLI_ENTRY}")


def align_template_versions(version):
    section(f"Aligning local template to release version {version}")
    run(["bash", "scripts/update-all-version.sh", version], cwd=REPO_ROOT)


def scaffold(workspace_dir):
    section("Scaffolding a project from the local template via the CLI")
    workspace = Path(workspace_dir).resolve()
    if workspace.exists():
        shutil.rmtree(workspace, ignore_errors=True)
    workspace.parent.mkdir(parents=True, exist_ok=True)

    # The project is created outside the monorepo on purpose: if it lived next
    # to pnpm-workspace.yaml, pnpm would link the local workspace packages and
    # `pnpm run:ios/android` would not exercise the published artifacts.
    run(
        [
            "node",
            str(CLI_ENTRY),
            workspace.name,
            "--template",
            str(TEMPLATE_DIR),
            "--namespace",
            NAMESPACE,
            "--yes",
            "--no-install",
            "--no-git",
        ],
        cwd=workspace.parent,
    )
    if not (workspace / "package.json").exists():
        raise SystemExit(f"::error::Scaffold failed: {workspace}/package.json is missing")
    log(f"Project scaffolded at {workspace}")
    return workspace


def retry(label, attempts, attempt_fn):
    delay = RETRY_BASE_DELAY
    for attempt in range(1, attempts + 1):
        log("")
        log(f"--- {label}: attempt {attempt}/{attempts} ---")
        try:
            attempt_fn()
            log(f"{label}: succeeded on attempt {attempt}")
            return
        except SystemExit as err:
            if attempt == attempts:
                raise
            log(f"{label}: attempt {attempt} failed ({err}); retrying in {delay}s")
            time.sleep(delay)
            delay += RETRY_BASE_DELAY


# ──────────────────────────────────────────────────────────────
# Platform verification
# ──────────────────────────────────────────────────────────────

def verify_ios(version, workspace_dir, podfile_lock_out):
    wait_for_npm(version)
    wait_for_trunk(version)
    project = scaffold(workspace_dir)
    podfile_lock = project / "ios" / "Podfile.lock"

    def attempt():
        # Drop any Podfile.lock copied from the template so `pod install`
        # (run inside `pnpm run:ios`) resolves a fresh lock against the
        # just-published pods.
        if podfile_lock.exists():
            log(f"Removing scaffolded Podfile.lock: {podfile_lock}")
            podfile_lock.unlink()
        run(["pnpm", "install"], cwd=project)
        # Force CocoaPods to refresh specs during install. Call the CLI
        # directly so pnpm script forwarding cannot place the flag after `--`.
        run(IOS_VERIFY_RUN_COMMAND, cwd=project)

    section("Verifying template on iOS (pnpm run:ios)")
    retry("verify-template-ios", BUILD_ATTEMPTS["ios"], attempt)

    if not podfile_lock.exists():
        raise SystemExit("::error::Podfile.lock was not generated by pnpm run:ios")
    out_path = (REPO_ROOT / podfile_lock_out).resolve()
    out_path.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(podfile_lock, out_path)
    log(f"Verified Podfile.lock exported to {out_path}")


def verify_android(version, workspace_dir):
    wait_for_npm(version)
    wait_for_maven(version)
    project = scaffold(workspace_dir)

    def attempt():
        run(["pnpm", "install"], cwd=project)
        run(["pnpm", "run:android"], cwd=project)

    section("Verifying template on Android (pnpm run:android)")
    retry("verify-template-android", BUILD_ATTEMPTS["android"], attempt)


def main():
    parser = argparse.ArgumentParser(description="Verify the Sparkling app template.")
    parser.add_argument("--platform", required=True, choices=["ios", "android"])
    parser.add_argument("--version", required=True, help="Release version, e.g. 2.1.0 or 2.1.0-rc.12")
    parser.add_argument(
        "--workspace-dir",
        required=True,
        help="Directory (outside the monorepo) where the project is scaffolded",
    )
    parser.add_argument(
        "--podfile-lock-out",
        default="template/sparkling-app-template/ios/Podfile.lock",
        help="Repo-relative path the verified iOS Podfile.lock is copied to",
    )
    args = parser.parse_args()

    section(f"Verify App Template ({args.platform}) — version {args.version}")
    build_cli()
    align_template_versions(args.version)

    if args.platform == "ios":
        verify_ios(args.version, args.workspace_dir, args.podfile_lock_out)
    else:
        verify_android(args.version, args.workspace_dir)

    section(f"Template verification ({args.platform}) succeeded")


if __name__ == "__main__":
    main()
