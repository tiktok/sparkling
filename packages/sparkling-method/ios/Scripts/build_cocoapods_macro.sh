#!/usr/bin/env bash
set -euo pipefail

package_dir="$(cd "$(dirname "$0")/.." && pwd)"
swift build --package-path "$package_dir" --configuration release --product SparklingMethodMacroPlugin
mkdir -p "$package_dir/Release"
cp "$package_dir/.build/release/SparklingMethodMacroPlugin-tool" "$package_dir/Release/SparklingMethodMacroPlugin"
