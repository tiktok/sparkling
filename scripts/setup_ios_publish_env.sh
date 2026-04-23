#!/usr/bin/env bash
# Prepare the iOS publish environment:
#   1. Install Ruby gems (CocoaPods via Bundler)
#   2. Install JS dependencies
set -euo pipefail

echo "📦 Installing CocoaPods..."
bundle install
echo "✅ CocoaPods version: $(bundle exec pod --version)"
