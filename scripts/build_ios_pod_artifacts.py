#!/usr/bin/env python3
"""
Build iOS pod artifacts for GitHub Release and CocoaPods publishing.

For each iOS component (defined in scripts/ios_pods.json):
  1. Locates the .podspec by searching under the configured search_dir
  2. Generates a <Pod>.podspec.json with source.http pointing to the
     GitHub Release zip asset, replacing the git-based source field
     and stripping monorepo-prefixed source path patterns.
  3. Packages the ios/ source directory into a <Pod>-<version>.zip
     (removes stale podspec files, adds LICENSE and the generated podspec.json)

Usage:
  python3 scripts/build_ios_pod_artifacts.py <version> [--pod <POD_NAME>] [--output-dir <dir>]

Requirements: CocoaPods (via Bundler), Python 3.8+
"""

import argparse
import json
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile
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


GITHUB_RELEASES_BASE = "https://github.com/tiktok/sparkling/releases/download"
CONFIG_FILE = Path(__file__).parent / "ios_pods.json"

_PODSPEC_VERSION_RE = re.compile(r's\.version\s*=\s*"[^"]*"')

ZIP_EXCLUDE_FILES = {".DS_Store", ".gitignore"}
ZIP_EXCLUDE_DIRS  = {"__MACOSX"}
ZIP_EXCLUDE_EXTS  = {".podspec", ".log"}


def _human_size(size_bytes: int) -> str:
    for unit in ("B", "K", "M", "G"):
        if size_bytes < 1024:
            return f"{size_bytes}{unit}"
        size_bytes //= 1024
    return f"{size_bytes}T"


def load_pod_configs() -> list:
    return json.loads(CONFIG_FILE.read_text())


def find_podspec(repo_root: Path, pod_name: str, search_dir: str) -> Path:
    matches = list((repo_root / search_dir).rglob(f"{pod_name}.podspec"))
    if not matches:
        die(f"No {pod_name}.podspec found under {search_dir}")
    if len(matches) > 1:
        die(f"Multiple {pod_name}.podspec found under {search_dir}: {[str(m) for m in matches]}")
    return matches[0]


def update_hardcoded_versions(repo_root: Path, version: str, pod_configs: list) -> None:
    # Some podspecs (Sparkling, SparklingMethod) have hardcoded version strings
    # instead of reading from package.json; update them before pod ipc spec runs.
    replacement = f's.version        = "{version}"'
    for cfg in pod_configs:
        if not cfg.get("hardcoded_version"):
            continue
        podspec = find_podspec(repo_root, cfg["pod_name"], cfg["search_dir"])
        podspec.write_text(_PODSPEC_VERSION_RE.sub(replacement, podspec.read_text()))
    success("Podspec versions updated")


def _should_exclude(rel: Path) -> bool:
    if rel.name in ZIP_EXCLUDE_FILES:
        return True
    if any(part in ZIP_EXCLUDE_DIRS for part in rel.parts):
        return True
    name = rel.name
    return any(name.endswith(ext) or name.endswith(ext + ".json") for ext in ZIP_EXCLUDE_EXTS)


def build_zip(ios_dir: Path, repo_root: Path, output_path: Path, podspec_json: Path) -> None:
    with tempfile.TemporaryDirectory() as tmp:
        src = Path(tmp) / "src"
        shutil.copytree(ios_dir, src)
        shutil.copy2(repo_root / "LICENSE", src / "LICENSE")

        with zipfile.ZipFile(output_path, "w", zipfile.ZIP_DEFLATED) as zf:
            for file_path in sorted(src.rglob("*")):
                if not file_path.is_file():
                    continue
                rel = file_path.relative_to(src)
                if _should_exclude(rel):
                    continue
                zf.write(file_path, rel)
            zf.write(podspec_json, podspec_json.name)


def _filter_paths(v):
    if isinstance(v, str):
        v = [v]
    if not isinstance(v, list):
        return v
    kept = [p for p in v if isinstance(p, str)
            and not p.startswith("packages/")
            and not p.startswith("ios/")]
    result = kept if kept else v
    return result[0] if len(result) == 1 else result


def _patch_spec(spec: dict, pod_name: str, version: str, is_root: bool = True) -> dict:
    if is_root:
        spec["source"] = {
            "http": f"{GITHUB_RELEASES_BASE}/{version}/{pod_name}-{version}.zip"
        }
        spec.pop("readme", None)
    for key in ("source_files", "public_header_files", "private_header_files",
                "exclude_files", "preserve_paths"):
        if key in spec:
            spec[key] = _filter_paths(spec[key])
    if "resource_bundles" in spec:
        for name in list(spec["resource_bundles"]):
            spec["resource_bundles"][name] = _filter_paths(spec["resource_bundles"][name])
    for sub in spec.get("subspecs", []):
        _patch_spec(sub, pod_name, version, is_root=False)
    return spec


def generate_podspec_json(
    podspec_path: Path, pod_name: str, version: str, output_path: Path, repo_root: Path
) -> None:
    result = subprocess.run(
        ["bundle", "exec", "pod", "ipc", "spec", str(podspec_path)],
        capture_output=True, text=True, cwd=repo_root,
    )
    if result.stderr.strip():
        warn(f"pod ipc spec warnings for {pod_name}:\n{result.stderr.rstrip()}")
    if not result.stdout.strip():
        die(f"pod ipc spec produced no output for {pod_name} (exit {result.returncode})")

    raw = json.loads(result.stdout)
    patched = _patch_spec(raw, pod_name, version)
    output_path.write_text(json.dumps(patched, indent=2))


def main() -> None:
    all_configs = load_pod_configs()
    valid_pod_names = [c["pod_name"] for c in all_configs]

    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("version", help="Release version, e.g. 2.1.0 or 2.1.0-rc.12")
    parser.add_argument("--output-dir", default="dist/ios", help="Output directory (relative to repo root)")
    parser.add_argument(
        "--pod",
        metavar="POD_NAME",
        help=f"Build only the specified pod. Valid: {', '.join(valid_pod_names)}",
    )
    args = parser.parse_args()

    version = args.version
    if not re.match(r"^\d+\.\d+\.\d+(-[a-zA-Z0-9.]+)?$", version):
        die(f"Invalid version format: {version} (expected e.g. 2.1.0 or 2.1.0-rc.12)")

    if args.pod:
        configs = [c for c in all_configs if c["pod_name"] == args.pod]
        if not configs:
            die(f"Unknown pod '{args.pod}'. Valid pod names: {', '.join(valid_pod_names)}")
    else:
        configs = all_configs

    repo_root = Path(__file__).parent.parent.resolve()
    output_dir = repo_root / args.output_dir
    output_dir.mkdir(parents=True, exist_ok=True)

    info("═══════════════════════════════════════════════════════════")
    info(f"Building iOS pod artifacts for version: {version}")
    info(f"Output: {output_dir}")
    info("═══════════════════════════════════════════════════════════")

    if any(c.get("hardcoded_version") for c in configs):
        info(f"Updating hardcoded podspec versions to {version}...")
        update_hardcoded_versions(repo_root, version, configs)

    for cfg in configs:
        pod_name = cfg["pod_name"]
        podspec_path = find_podspec(repo_root, pod_name, cfg["search_dir"])
        ios_dir = podspec_path.parent
        zip_path = output_dir / f"{pod_name}-{version}.zip"
        json_path = output_dir / f"{pod_name}.podspec.json"

        print()
        info(f"─── {pod_name} ───────────────────────────────────────────")
        info(f"  podspec: {podspec_path.relative_to(repo_root)}")

        info(f"Generating {json_path.name}")
        generate_podspec_json(podspec_path, pod_name, version, json_path, repo_root)
        success(f"Generated {json_path.name}")

        info(f"Packaging source → {zip_path.name}")
        build_zip(ios_dir, repo_root, zip_path, json_path)
        success(f"Created {zip_path.name} ({_human_size(zip_path.stat().st_size)})")

    print()
    info("═══════════════════════════════════════════════════════════")
    success(f"All iOS pod artifacts built for version {version}")
    info("")
    info("Artifacts:")
    for f in sorted(output_dir.iterdir()):
        print(f"  {f.name:<52}  {_human_size(f.stat().st_size):>6}")
    info("═══════════════════════════════════════════════════════════")


if __name__ == "__main__":
    main()
