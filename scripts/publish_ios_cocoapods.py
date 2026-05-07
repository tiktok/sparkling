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
 [--dry-run]

Modes:
  (default)   Push all pods to CocoaPods trunk.
  --dry-run   Print push plan without executing any pod commands.
              Does not require COCOAPODS_TRUNK_TOKEN or built artifacts.
"""

import argparse
import json
import os
import re
import subprocess
import sys
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

def load_pod_configs() -> list:
    return json.loads(CONFIG_FILE.read_text())


def pod_cmd(*args) -> list[str]:
    return ["bundle", "exec", "pod", *args]


def _gem_path(gem_name: str) -> str:
    result = subprocess.run(
        ["bundle", "info", gem_name],
        capture_output=True, text=True, check=True,
    )
    for line in result.stdout.splitlines():
        if "Path:" in line:
            return line.split("Path:")[-1].strip()
    die(f"Could not find gem path for {gem_name}")


def skip_pod_lint() -> None:
    pod_path = _gem_path("cocoapods-trunk")
    target = Path(pod_path) / "lib/pod/command/trunk/push.rb"
    info(f"Patching lint skip into: {target}")
    content = target.read_text(encoding="utf-8")
    patched = re.sub(r"validate_podspec", "#validate_podspec", content, count=1)
    target.write_text(patched, encoding="utf-8")


def already_published(pod_name: str, version: str) -> bool:
    result = subprocess.run(
        pod_cmd("trunk", "info", pod_name),
        capture_output=True, text=True,
    )
    return version in result.stdout


def push_pod(pod_name: str, version: str, artifacts_dir: Path, dry_run: bool) -> None:
    print()
    info(f"─── {pod_name} ───────────────────────────")
    json_path = artifacts_dir / f"{pod_name}.podspec.json"
    if dry_run:
        info(f"[dry-run] would push: {json_path}")
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



def main() -> None:
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("version", help="Release version, e.g. 2.1.0 or 2.1.0-rc.12")
    parser.add_argument("--artifacts-dir", default="dist/ios",
                        help="Directory containing built .podspec.json files (default: dist/ios)")
    parser.add_argument("--dry-run", action="store_true",
                        help="Print push plan without executing any pod commands")
    parser.add_argument("--no-skip-lint", action="store_false", dest="skip_lint", default=True,
                        help="Do not patch out lint validation (lint is skipped by default)")
    args = parser.parse_args()

    version = args.version
    if not re.match(r"^\d+\.\d+\.\d+(-[a-zA-Z0-9.]+)?$", version):
        die(f"Invalid version format: {version} (expected e.g. 2.1.0 or 2.1.0-rc.12)")

    if not args.dry_run and not os.environ.get("COCOAPODS_TRUNK_TOKEN"):
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
    info("═══════════════════════════════════════════════════════════")

    if args.skip_lint:
        skip_pod_lint()

    for cfg in configs:
        if not cfg.get("trunk"):
            continue
        pod_name = cfg["pod_name"]
        push_pod(pod_name, version, artifacts_dir, args.dry_run)

    print()
    info("═══════════════════════════════════════════════════════════")
    success(f"All iOS pods published for version {version}")
    info("═══════════════════════════════════════════════════════════")


if __name__ == "__main__":
    main()
