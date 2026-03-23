#!/bin/bash

# Sparkling Android Maven Central Release Script
# Usage: ./publish-maven-central.sh [version] [--skip-tests] [--dry-run]
# Example: ./publish-maven-central.sh 2.0.0
# Example: ./publish-maven-central.sh 2.0.0-rc.6
# Example: ./publish-maven-central.sh 2.0.0 --dry-run

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

# Show usage
show_usage() {
    echo "Usage: ./publish-maven-central.sh [version] [options]"
    echo ""
    echo "Publish Sparkling Android SDK (sparkling-method, sparkling-sdk, sparkling-debug-tool) to Maven Central."
    echo ""
    echo "Options:"
    echo "  version       The version number to publish (e.g., 2.0.0 or 2.0.0-rc.6)"
    echo "  --skip-tests  Skip running tests before publish"
    echo "  --dry-run     Perform a dry run without actually publishing"
    echo "  --help, -h    Show this help message"
    echo ""
    echo "Environment Variables Required:"
    echo "  MAVEN_CENTRAL_USERNAME    Maven Central token username"
    echo "  MAVEN_CENTRAL_PASSWORD    Maven Central token password"
    echo "  SIGNING_KEY_ID            GPG key ID (last 8 chars)"
    echo "  SIGNING_PASSWORD          GPG key password"
    echo ""
    echo "Optional Environment Variables:"
    echo "  SIGNING_SECRET_KEY_RING_FILE  Path to GPG secring.gpg (optional, uses in-memory key if not set)"
    echo ""
    echo "Examples:"
    echo "  ./publish-maven-central.sh 2.0.0                    # Release version 2.0.0"
    echo "  ./publish-maven-central.sh 2.0.0-rc.6               # Release RC version"
    echo "  ./publish-maven-central.sh 2.0.0 --dry-run          # Dry run"
    echo "  ./publish-maven-central.sh 2.0.0 --skip-tests       # Skip tests"
}

# Parse arguments
VERSION=""
SKIP_TESTS=false
DRY_RUN=false

for arg in "$@"; do
    case $arg in
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
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

# Check if it's an RC version
IS_RC=false
if [[ $VERSION =~ -rc\.[0-9]+$ ]]; then
    IS_RC=true
    print_info "Detected RC version: $VERSION"
fi

# Validate environment variables
print_info "Checking environment variables..."

if [ -z "$MAVEN_CENTRAL_USERNAME" ]; then
    print_error "MAVEN_CENTRAL_USERNAME environment variable is not set"
    echo "Please set it to your Maven Central token username"
    exit 1
fi

if [ -z "$MAVEN_CENTRAL_PASSWORD" ]; then
    print_error "MAVEN_CENTRAL_PASSWORD environment variable is not set"
    echo "Please set it to your Maven Central token password"
    exit 1
fi

if [ -z "$SIGNING_KEY_ID" ]; then
    print_error "SIGNING_KEY_ID environment variable is not set"
    echo "Please set it to your GPG key ID (last 8 characters)"
    exit 1
fi

if [ -z "$SIGNING_PASSWORD" ]; then
    print_error "SIGNING_PASSWORD environment variable is not set"
    echo "Please set it to your GPG key password"
    exit 1
fi

print_success "Environment variables check passed"

# Determine which Maven Central repository to use
# Central Portal staging API (replaces legacy s01.oss.sonatype.org shut down June 2025)
MAVEN_CENTRAL_REPO_URL=${MAVEN_CENTRAL_REPO_URL:-"https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"}
print_info "Using Maven Central repository: $MAVEN_CENTRAL_REPO_URL"

# Export GPG private key for signing
print_info "Exporting GPG private key..."
if [ -z "$SIGNING_KEY" ]; then
    # Try to export from GPG keyring
    if command -v gpg &> /dev/null; then
        SIGNING_KEY=$(gpg --armor --export-secret-keys "$SIGNING_KEY_ID" 2>/dev/null || echo "")
        if [ -n "$SIGNING_KEY" ]; then
            export SIGNING_KEY
            print_success "GPG key exported successfully"
        else
            print_warning "Could not export GPG key automatically"
            print_info "Please set SIGNING_KEY environment variable with your GPG private key:"
            print_info "  export SIGNING_KEY=\$(gpg --armor --export-secret-keys $SIGNING_KEY_ID)"
            exit 1
        fi
    else
        print_error "GPG command not found and SIGNING_KEY not set"
        print_info "Please either:"
        print_info "  1. Install GPG and ensure SIGNING_KEY_ID is correct"
        print_info "  2. Set SIGNING_KEY environment variable directly"
        exit 1
    fi
else
    print_success "Using SIGNING_KEY from environment variable"
fi

# Project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
PLAYGROUND_DIR="$PROJECT_ROOT/packages/playground/android"

print_info "Project root: $PROJECT_ROOT"
print_info "Playground directory: $PLAYGROUND_DIR"

# Pass version to Gradle via -P flag so findProperty("SPARKLING_PUBLISHING_VERSION") picks it up.
# Note: gradle.properties in subproject dirs are NOT read by Gradle when projectDir is remapped
# in settings.gradle.kts, so we must pass the version explicitly on the command line.
GRADLE_VERSION_PROP="-PSPARKLING_PUBLISHING_VERSION=$VERSION"
print_info "Publishing version: $VERSION (via Gradle property)"

# Change to playground directory
cd "$PLAYGROUND_DIR"

# Dry run mode
if [ "$DRY_RUN" = true ]; then
    print_warning "DRY RUN MODE - No actual publishing will occur"
    print_info "Would publish version: $VERSION"
    print_info "sparkling-method artifact: com.tiktok.sparkling:sparkling-method:$VERSION"
    print_info "sparkling artifact: com.tiktok.sparkling:sparkling:$VERSION"
    print_info "sparkling-debug-tool artifact: com.tiktok.sparkling:sparkling-debug-tool:$VERSION"
    exit 0
fi

# Build and publish sparkling-method first (dependency of sparkling-sdk)
print_info "========================================"
print_info "Step 1: Building sparkling-method"
print_info "========================================"

if [ "$SKIP_TESTS" = false ]; then
    print_info "Running tests for sparkling-method..."
    ./gradlew $GRADLE_VERSION_PROP :sparkling-method:testDebugUnitTest
    print_success "Tests passed for sparkling-method"
fi

print_info "Building sparkling-method..."
./gradlew $GRADLE_VERSION_PROP :sparkling-method:clean :sparkling-method:assembleRelease

print_info "Publishing sparkling-method to Maven Central..."
./gradlew $GRADLE_VERSION_PROP :sparkling-method:publishReleasePublicationToMavenCentralRepository

print_success "sparkling-method published successfully!"

# Build and publish sparkling-sdk
print_info "========================================"
print_info "Step 2: Building sparkling-sdk"
print_info "========================================"

if [ "$SKIP_TESTS" = false ]; then
    print_info "Running tests for sparkling-sdk..."
    ./gradlew $GRADLE_VERSION_PROP :sparkling:testDebugUnitTest
    print_success "Tests passed for sparkling-sdk"
fi

print_info "Building sparkling-sdk..."
./gradlew $GRADLE_VERSION_PROP :sparkling:clean :sparkling:assembleRelease

print_info "Publishing sparkling-sdk to Maven Central..."
./gradlew $GRADLE_VERSION_PROP :sparkling:publishReleasePublicationToMavenCentralRepository

print_success "sparkling-sdk published successfully!"

# Build and publish sparkling-debug-tool
print_info "========================================"
print_info "Step 3: Building sparkling-debug-tool"
print_info "========================================"

print_info "Building sparkling-debug-tool..."
./gradlew $GRADLE_VERSION_PROP :sparkling-debug-tool:clean :sparkling-debug-tool:assembleRelease

print_info "Publishing sparkling-debug-tool to Maven Central..."
./gradlew $GRADLE_VERSION_PROP :sparkling-debug-tool:publishReleasePublicationToMavenCentralRepository

print_success "sparkling-debug-tool published successfully!"

# Finalize deployment on Central Portal
# When using Gradle's maven-publish plugin (a "Maven-API-like" plugin), the staging API
# does NOT automatically transfer artifacts to the Central Portal. We must explicitly
# call the manual upload endpoint to make the deployment visible and trigger release.
# See: https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/
print_info "========================================"
print_info "Step 4: Finalizing deployment on Central Portal"
print_info "========================================"

NAMESPACE=${SPARKLING_PUBLISHING_GROUP_ID:-"com.tiktok.sparkling"}
# Build the Bearer token: base64(username:password)
BEARER_TOKEN=$(printf '%s:%s' "$MAVEN_CENTRAL_USERNAME" "$MAVEN_CENTRAL_PASSWORD" | base64)

# publishing_type=automatic will auto-release to Maven Central if validation passes
PUBLISHING_TYPE="automatic"

print_info "Transferring deployment to Central Portal (namespace: $NAMESPACE, type: $PUBLISHING_TYPE)..."
UPLOAD_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -X POST \
    "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/${NAMESPACE}?publishing_type=${PUBLISHING_TYPE}" \
    -H "Authorization: Bearer ${BEARER_TOKEN}")

HTTP_CODE=$(echo "$UPLOAD_RESPONSE" | tail -1)
RESPONSE_BODY=$(echo "$UPLOAD_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
    print_success "Deployment transferred to Central Portal successfully!"
    if [ -n "$RESPONSE_BODY" ]; then
        print_info "Response: $RESPONSE_BODY"
    fi
else
    print_error "Failed to transfer deployment to Central Portal (HTTP $HTTP_CODE)"
    print_error "Response: $RESPONSE_BODY"
    print_warning "You may need to manually finalize at https://central.sonatype.com/publishing/deployments"
    print_info "Or search for open repositories:"
    print_info "  curl -H 'Authorization: Bearer <token>' \\"
    print_info "    'https://ossrh-staging-api.central.sonatype.com/manual/search/repositories?ip=any&profile_id=${NAMESPACE}'"
fi

# Summary
print_info ""
print_info "=========================================="
print_success "All artifacts published successfully!"
print_info "=========================================="
print_info ""
print_info "Published artifacts:"
print_info "  - com.tiktok.sparkling:sparkling-method:$VERSION"
print_info "  - com.tiktok.sparkling:sparkling:$VERSION"
print_info "  - com.tiktok.sparkling:sparkling-debug-tool:$VERSION"
print_info ""
print_info "Next steps:"
print_info "1. Login to https://central.sonatype.com/"
print_info "2. Navigate to 'Publishing > Deployments'"
print_info "3. If publishing_type was 'automatic', artifacts will auto-release after validation"
print_info "4. Otherwise, find your deployment and click 'Publish' to release to Maven Central"
print_info ""

if [ "$IS_RC" = true ]; then
    print_warning "RC Version Notice:"
    print_info "This is an RC version ($VERSION)."
    print_info "RC versions will be available on Maven Central but won't be resolved by default."
    print_info "Users need to specify the exact version to use it."
else
    print_info "Note: It may take 10-30 minutes for the artifacts to appear on Maven Central search."
fi

print_info ""
print_info "Verify at:"
print_info "  - https://repo1.maven.org/maven2/com/tiktok/sparkling/sparkling-method/$VERSION/"
print_info "  - https://repo1.maven.org/maven2/com/tiktok/sparkling/sparkling/$VERSION/"
print_info "  - https://repo1.maven.org/maven2/com/tiktok/sparkling/sparkling-debug-tool/$VERSION/"
