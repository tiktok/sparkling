#!/bin/bash

# Sparkling Version Alignment Script
# Usage: ./align-version.sh [version] [--dry-run]
# Example: ./align-version.sh 2.0.0-rc.6
# Example: ./align-version.sh 2.0.0 --dry-run

set -e

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print colored messages
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

# Cross-platform sed in-place: macOS requires `sed -i ''`, Linux requires `sed -i`
sedi() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "$@"
    else
        sed -i "$@"
    fi
}

# Show usage
show_usage() {
    echo "Usage: ./align-version.sh [version] [options]"
    echo ""
    echo "Align version numbers across all platforms (Android, iOS, TypeScript)."
    echo "Updates package versions, template dependency references, and podspec files."
    echo ""
    echo "Options:"
    echo "  version     The version number to set (e.g., 2.0.0 or 2.0.0-rc.6)"
    echo "  --dry-run   Show what would be changed without making changes"
    echo "  --help, -h  Show this help message"
    echo ""
    echo "Examples:"
    echo "  ./align-version.sh 2.0.0-rc.6        # Update all versions to 2.0.0-rc.6"
    echo "  ./align-version.sh 2.0.0 --dry-run   # Preview changes for 2.0.0"
}

# Parse arguments
VERSION=""
DRY_RUN=false

for arg in "$@"; do
    case $arg in
        --dry-run)
            DRY_RUN=true
            shift
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

# Validate version number
if [ -z "$VERSION" ]; then
    print_error "Please provide a version number"
    show_usage
    exit 1
fi

# Validate version format (supports: 2.0.0, 2.0.0-rc.6, 2.0.0-beta.1, etc.)
if [[ ! $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.]+)?$ ]]; then
    print_error "Invalid version format: $VERSION"
    echo "Valid formats: 2.0.0, 2.0.0-rc.1, 2.0.0-beta.1"
    exit 1
fi

print_info "Aligning version to: $VERSION"

# Project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

print_info "Project root: $PROJECT_ROOT"

# Files to update
declare -a TYPESCRIPT_FILES=(
    "package.json"
    "packages/sparkling-debug-tool/package.json"
    "packages/sparkling-sdk/package.json"
    "packages/sparkling-method/package.json"
    "packages/sparkling-types/package.json"
    "template/sparkling-app-template/package.json"
)

declare -a IOS_PODSPEC_FILES=(
    "packages/sparkling-method/ios/SparklingMethod.podspec"
    "packages/sparkling-sdk/ios/Sparkling.podspec"
)

TEMPLATE_PACKAGE_FILE="template/sparkling-app-template/package.json"
TEMPLATE_ANDROID_FILE="template/sparkling-app-template/android/app/build.gradle.kts"
TEMPLATE_IOS_FILE="template/sparkling-app-template/ios/Podfile"

# Function to update TypeScript package.json files
update_typescript_versions() {
    print_info "========================================"
    print_info "Updating TypeScript package.json files..."
    print_info "========================================"

    for file in "${TYPESCRIPT_FILES[@]}"; do
        local filepath="$PROJECT_ROOT/$file"
        if [ -f "$filepath" ]; then
            local current_version
            current_version=$(grep -o '"version": *"[^"]*"' "$filepath" | head -1 | sed 's/.*"\([^"]*\)".*/\1/')

            if [ "$DRY_RUN" = true ]; then
                print_info "[DRY RUN] Would update $file: $current_version -> $VERSION"
            else
                # Use sed to update version in package.json
                # Handle both "version": "x.x.x" and "version":"x.x.x" formats
                sedi "s/\"version\": *\"[^\"]*\"/\"version\": \"$VERSION\"/" "$filepath"
                print_success "Updated $file: $current_version -> $VERSION"
            fi
        else
            print_warning "File not found: $file"
        fi
    done
}

# Function to update iOS podspec files
update_ios_versions() {
    print_info ""
    print_info "========================================"
    print_info "Updating iOS podspec files..."
    print_info "========================================"

    for file in "${IOS_PODSPEC_FILES[@]}"; do
        local filepath="$PROJECT_ROOT/$file"
        if [ -f "$filepath" ]; then
            local current_version
            current_version=$(grep -o 's\.version.*=.*"[^"]*"' "$filepath" | head -1 | sed 's/.*"\([^"]*\)".*/\1/')

            if [ "$DRY_RUN" = true ]; then
                print_info "[DRY RUN] Would update $file: $current_version -> $VERSION"
            else
                # Update s.version in podspec file
                sedi "s/s\.version.*=.*\"[^\"]*\"/s.version        = \"$VERSION\"/" "$filepath"
                print_success "Updated $file: $current_version -> $VERSION"
            fi
        else
            print_warning "File not found: $file"
        fi
    done
}

# Function to update app template dependency references
update_template_dependencies() {
    print_info ""
    print_info "========================================"
    print_info "Updating template dependency references..."
    print_info "========================================"

    local template_pkg="$PROJECT_ROOT/$TEMPLATE_PACKAGE_FILE"
    local template_android="$PROJECT_ROOT/$TEMPLATE_ANDROID_FILE"
    local template_ios="$PROJECT_ROOT/$TEMPLATE_IOS_FILE"

    if [ -f "$template_pkg" ]; then
        if [ "$DRY_RUN" = true ]; then
            print_info "[DRY RUN] Would update $TEMPLATE_PACKAGE_FILE dependencies:"
            print_info "          sparkling-navigation -> ^$VERSION"
            print_info "          sparkling-debug-tool -> ~$VERSION"
            print_info "          sparkling-app-cli -> ~$VERSION"
            print_info "          sparkling-types -> ~$VERSION"
        else
            sedi "s|\"sparkling-navigation\": *\"[^\"]*\"|\"sparkling-navigation\": \"^${VERSION}\"|" "$template_pkg"
            sedi "s|\"sparkling-debug-tool\": *\"[^\"]*\"|\"sparkling-debug-tool\": \"~${VERSION}\"|" "$template_pkg"
            sedi "s|\"sparkling-app-cli\": *\"[^\"]*\"|\"sparkling-app-cli\": \"~${VERSION}\"|" "$template_pkg"
            sedi "s|\"sparkling-types\": *\"[^\"]*\"|\"sparkling-types\": \"~${VERSION}\"|" "$template_pkg"
            print_success "Updated template npm dependencies in $TEMPLATE_PACKAGE_FILE"
        fi
    else
        print_warning "File not found: $TEMPLATE_PACKAGE_FILE"
    fi

    if [ -f "$template_android" ]; then
        if [ "$DRY_RUN" = true ]; then
            print_info "[DRY RUN] Would update $TEMPLATE_ANDROID_FILE Sparkling deps to $VERSION"
        else
            sedi "s|com.tiktok.sparkling:sparkling:[^\"]*|com.tiktok.sparkling:sparkling:${VERSION}|" "$template_android"
            sedi "s|com.tiktok.sparkling:sparkling-method:[^\"]*|com.tiktok.sparkling:sparkling-method:${VERSION}|" "$template_android"
            print_success "Updated template Android dependencies in $TEMPLATE_ANDROID_FILE"
        fi
    else
        print_warning "File not found: $TEMPLATE_ANDROID_FILE"
    fi

    if [ -f "$template_ios" ]; then
        if [ "$DRY_RUN" = true ]; then
            print_info "[DRY RUN] Would update $TEMPLATE_IOS_FILE SPARKLING_VERSION -> '$VERSION'"
        else
            sedi "s|SPARKLING_VERSION = '[^']*'|SPARKLING_VERSION = '${VERSION}'|" "$template_ios"
            print_success "Updated template iOS dependency in $TEMPLATE_IOS_FILE"
        fi
    else
        print_warning "File not found: $TEMPLATE_IOS_FILE"
    fi
}

# Function to show Android info
show_android_info() {
    print_info ""
    print_info "========================================"
    print_info "Android Version Info"
    print_info "========================================"
    print_info "Android uses Gradle properties for versioning."
    print_info "Version is passed at publish time via SPARKLING_PUBLISHING_VERSION."
    print_info "Template Android dependency references are updated in build.gradle.kts."
    print_info ""
    print_info "To publish Android with version $VERSION, run:"
    print_info "  ./scripts/publish-android-maven.sh $VERSION"
}

# Function to verify all current versions
verify_current_versions() {
    print_info ""
    print_info "========================================"
    print_info "Current Versions (Before Update)"
    print_info "========================================"

    # Check TypeScript files
    print_info "TypeScript package.json versions:"
    for file in "${TYPESCRIPT_FILES[@]}"; do
        local filepath="$PROJECT_ROOT/$file"
        if [ -f "$filepath" ]; then
            local current_version
            current_version=$(grep -o '"version": *"[^"]*"' "$filepath" | head -1 | sed 's/.*"\([^"]*\)".*/\1/')
            echo "  $file: $current_version"
        fi
    done

    # Check iOS podspec files
    print_info ""
    print_info "iOS podspec versions:"
    for file in "${IOS_PODSPEC_FILES[@]}"; do
        local filepath="$PROJECT_ROOT/$file"
        if [ -f "$filepath" ]; then
            local current_version
            current_version=$(grep -o 's\.version.*=.*"[^"]*"' "$filepath" | head -1 | sed 's/.*"\([^"]*\)".*/\1/')
            echo "  $file: $current_version"
        fi
    done
}

# Main execution
main() {
    if [ "$DRY_RUN" = true ]; then
        print_warning "DRY RUN MODE - No actual changes will be made"
        print_info ""
    fi

    # Show current versions
    verify_current_versions

    # Update versions
    update_typescript_versions
    update_ios_versions
    update_template_dependencies
    show_android_info

    print_info ""
    print_info "========================================"
    if [ "$DRY_RUN" = true ]; then
        print_success "Dry run completed! No changes were made."
    else
        print_success "Version alignment completed!"
    fi
    print_info "========================================"
    print_info ""
    print_info "All platforms are now aligned to version: $VERSION"
    print_info ""
    print_info "Next steps:"
    print_info "  1. Review the changes: git diff"
    print_info "  2. Commit the changes: git add -A && git commit -m \"chore: bump version to $VERSION\""
    print_info "  3. Create and push tag to trigger GitHub Actions publish:"
    print_info "     git tag $VERSION"
    print_info "     git push origin $VERSION"
    print_info ""
    print_info "Or run local publish scripts:"
    print_info "     - Android: ./scripts/publish-android-maven.sh $VERSION"
    print_info "     - iOS:     ./scripts/publish-ios-sparkling-method.sh $VERSION"
    print_info ""
}

# Run main function
main
