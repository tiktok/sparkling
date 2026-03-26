#!/bin/bash

# Sparkling iOS CocoaPods Release Script - Publish Sparkling-DebugTool
# Usage: ./publish-ios-sparkling-debug-tool.sh [version] [--dry-run]

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

show_usage() {
    echo "Usage: ./publish-ios-sparkling-debug-tool.sh [version] [--dry-run]"
    echo ""
    echo "Publishes Sparkling-DebugTool pod to CocoaPods trunk."
    echo ""
    echo "Options:"
    echo "  --dry-run   Validate podspec (if CocoaPods installed) without pushing"
    echo ""
    echo "Examples:"
    echo "  ./publish-ios-sparkling-debug-tool.sh 2.0.0"
    echo "  ./publish-ios-sparkling-debug-tool.sh 2.0.0-rc.6"
    echo "  ./publish-ios-sparkling-debug-tool.sh 2.0.0-rc.6 --dry-run"
}

VERSION=""
DRY_RUN=false
for arg in "$@"; do
    case $arg in
        --dry-run)
            DRY_RUN=true
            ;;
        --help|-h)
            show_usage
            exit 0
            ;;
        *)
            if [ -z "$VERSION" ]; then
                VERSION=$arg
            fi
            ;;
    esac
done

if [ -z "$VERSION" ]; then
    print_error "Please provide a version number"
    show_usage
    exit 1
fi

if [[ ! $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.]+)?$ ]]; then
    print_error "Invalid version format: $VERSION"
    echo "Valid formats: 2.0.0 or 2.0.0-rc.1"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
PODSPEC_DIR="$PROJECT_ROOT/packages/sparkling-debug-tool/ios"

print_info "Publishing Sparkling-DebugTool for version: $VERSION"
cd "$PODSPEC_DIR"

if [ "$DRY_RUN" = true ]; then
    print_warning "DRY RUN MODE - No publish will occur"
    if command -v pod >/dev/null 2>&1; then
        print_info "Running podspec validation in dry-run..."
        if pod spec lint Sparkling-DebugTool.podspec --allow-warnings 2>&1; then
            print_success "Dry-run validation passed"
        else
            print_warning "Dry-run validation reported issues"
        fi
    else
        print_warning "CocoaPods not found, skipping validation"
    fi
    print_info "Would run: pod trunk push Sparkling-DebugTool.podspec --allow-warnings"
    exit 0
fi

print_info "Validating Sparkling-DebugTool.podspec..."
if pod spec lint Sparkling-DebugTool.podspec --allow-warnings 2>&1; then
    print_success "Sparkling-DebugTool validation passed"
else
    print_warning "Sparkling-DebugTool validation has warnings or errors, trying to publish anyway..."
fi

print_info "Pushing Sparkling-DebugTool to CocoaPods..."
pod trunk push Sparkling-DebugTool.podspec --allow-warnings

print_success "Sparkling-DebugTool published successfully!"
