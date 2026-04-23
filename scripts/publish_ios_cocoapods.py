#!/usr/bin/env python3
"""
Publish iOS pods to CocoaPods trunk in dependency order.

Reads pod config from scripts/ios_pods.json. Pods are pushed in list order;
a pod with "cdn_gate": true triggers a CDN propagation wait before the next
pod is pushed (used to ensure SparklingMethod is live before its dependents).

Usage:
  python3 scripts/publish_ios_cocoapods.py <version> [--artifacts-dir <dir>]

Requirements:
  - COCOAPODS_TRUNK_TOKEN env var
  - CocoaPods (via Bundler), Python 3.8+
  - Podspec JSON files already built under <artifacts-dir>
 [--dry-run] [--lint-only]

Modes:
  (default)   Push all pods to CocoaPods trunk.
  --dry-run   Print push plan without executing any pod commands.
              Does not require COCOAPODS_TRUNK_TOKEN or built artifacts.
  --lint-only Run `pod spec lint` instead of `pod trunk push`.
              Validates podspec + downloads zip from GitHub Release URL.
              Requires the GitHub Release zip assets to already be uploaded.
"""

import argparse
import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path

RED = '\033[0;31m'
GREEN = '\033[0;32m'
YELLOW = '\033[1;33m'
BLUE = '\033[0;34m'
NC = '\033[0m'

def info(msg):    print(f"{BLUE}[INFO]{NC}    {msg}")
def success(msg): print(f"{GREEN}[OK]{NC}      {msg}")
def warn(msg):    print(f"{YELLOW}[WARN]{NC}    {msg}")
def die(msg):     print(f"{RED}[ERROR]{NC}   {msg}", file=sys.stderr); sys.exit(1)


CONFIG_FILE = Path(__file__).parent / "ios_pods.json"
CDN_POLL_INTERVAL = 10
CDN_POLL_MAX = 30


def load_pod_configs() -> list:
    return json.loads(CONFIG_FILE.read_text())


def pod_cmd(*args) -> list[str]:
    return ["bundle", "exec", "pod", *args]


def already_published(pod_name: str, version: str) -> bool:
    result = subprocess.run(
        pod_cmd("trunk", "info", pod_name),
        capture_output=True, text=True,
    )
    return version in result.stdout


def push_pod(pod_name: str, version: str, artifacts_dir: Path, dry_run: bool, lint_only: bool) -> None:
    print()
    info(f"─── {pod_name} ───────────────────────────")
    json_path = artifacts_dir / f"{pod_name}.podspec.json"
    if dry_run:
        info(f"[dry-run] would push: {json_path}")
        return
    if lint_only:
        info(f"Linting {pod_name}...")
        subprocess.run(
            pod_cmd("spec", "lint", str(json_path), "--allow-warnings"),
            check=True,
        )
        success(f"{pod_name} lint passed")
        return
    if already_published(pod_name, version):
        warn(f"{pod_name} {version} already on CocoaPods — skipping")
        return
    info(f"Pushing {pod_name}...")
    subprocess.run(
        pod_cmd("trunk", "push", str(json_path),
                "--allow-warnings", "--verbose",
                "--skip-import-validation", "--skip-tests"),
        check=True,
    )
    success(f"{pod_name} pushed")


def wait_for_cdn(pod_name: str, version: str, dry_run: bool, lint_only: bool) -> None:
    print()
    if dry_run or lint_only:
        info(f"[{'dry-run' if dry_run else 'lint-only'}] skipping CDN wait for {pod_name} {version}")
        return
    info(f"Waiting for {pod_name} {version} to be available on CDN...")
    for i in range(1, CDN_POLL_MAX + 1):
        if already_published(pod_name, version):
            success(f"{pod_name} {version} is live on CDN")
            return
        info(f"  waiting... ({i}/{CDN_POLL_MAX}, next check in {CDN_POLL_INTERVAL}s)")
        time.sleep(CDN_POLL_INTERVAL)
    die(f"{pod_name} {version} not available on CocoaPods CDN after "
        f"{CDN_POLL_MAX * CDN_POLL_INTERVAL}s")


def main() -> None:
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("version", help="Release version, e.g. 2.1.0 or 2.1.0-rc.12")
    parser.add_argument("--artifacts-dir", default="dist/ios",
                        help="Directory containing built .podspec.json files (default: dist/ios)")
    parser.add_argument("--dry-run", action="store_true",
                        help="Print push plan without executing any pod commands")
    parser.add_argument("--lint-only", action="store_true",
                        help="Run pod spec lint instead of pod trunk push (requires zip on GitHub Release)")
    args = parser.parse_args()

    if args.dry_run and args.lint_only:
        die("--dry-run and --lint-only are mutually exclusive")

    version = args.version
    if not re.match(r"^\d+\.\d+\.\d+(-[a-zA-Z0-9.]+)?$", version):
        die(f"Invalid version format: {version} (expected e.g. 2.1.0 or 2.1.0-rc.12)")

    if not args.dry_run and not args.lint_only and not os.environ.get("COCOAPODS_TRUNK_TOKEN"):
        die("COCOAPODS_TRUNK_TOKEN is not set")

    artifacts_dir = Path(args.artifacts_dir)
    if not artifacts_dir.is_dir():
        die(f"Artifacts directory not found: {artifacts_dir}")

    configs = load_pod_configs()

    info("═══════════════════════════════════════════════════════════")
    info(f"Publishing iOS pods to CocoaPods for version: {version}")
    info(f"Artifacts: {artifacts_dir}")
    if args.dry_run:
        warn("DRY-RUN mode — no pod commands will be executed")
    elif args.lint_only:
        warn("LINT-ONLY mode — running pod spec lint, not trunk push")
    info("═══════════════════════════════════════════════════════════")

    for cfg in configs:
        pod_name = cfg["pod_name"]
        push_pod(pod_name, version, artifacts_dir, args.dry_run, args.lint_only)
        if cfg.get("cdn_gate"):
            wait_for_cdn(pod_name, version, args.dry_run, args.lint_only)

    print()
    info("═══════════════════════════════════════════════════════════")
    success(f"All iOS pods published for version {version}")
    info("═══════════════════════════════════════════════════════════")


if __name__ == "__main__":
    main()
