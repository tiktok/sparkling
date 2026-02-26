# Sparkling CLI

Sparkling CLI (`sparkling-app-cli`) is the built-in command-line tool that powers the development workflow. It handles building Lynx bundles, autolinking native method modules, running apps on Android/iOS, and diagnosing your environment.

## Installation

The CLI is included automatically when you scaffold a project with `create-sparkling-app`. You can also install it manually:

```bash
npm install sparkling-app-cli
```

Once installed, run commands via `npx sparkling` or through the npm scripts defined in your project's `package.json`.

## Commands

### `sparkling build`

Build Lynx bundles using your project's `app.config.ts`.

```bash
npx sparkling build
```

| Option | Description |
| --- | --- |
| `--config <path>` | Path to `app.config.ts` (default: `app.config.ts`) |
| `--copy` | Copy built assets to Android and iOS native shells |
| `--skip-copy` | Skip copying assets (default) |

By default, asset copying is skipped for faster iteration during development. Use `--copy` when you need the bundles inside the native projects (e.g. for a release build).

### `sparkling dev`

Start the Rspeedy dev server for hot-reload development. Instead of rebuilding and copying bundles manually, the dev server serves bundles over HTTP so changes are reflected instantly.

```bash
npx sparkling dev
```

| Option | Description |
| --- | --- |
| `--config <path>` | Path to `app.config.ts` (default: `app.config.ts`) |
| `--port <number>` | Dev server port (default: `5969`) |

The default port **5969** spells **LYNX** on a phone keypad (L=5, Y=9, N=6, X=9).

Once the server is running, point your app to `http://<your-ip>:5969/main.lynx.bundle` (or whatever entry point you need). In the project template, **DEBUG** builds connect to the dev server automatically.

### `sparkling copy-assets`

Copy compiled bundles into Android and iOS resource directories.

```bash
npx sparkling copy-assets
```

| Option | Description |
| --- | --- |
| `--source <path>` | Path to compiled assets (default: `dist`) |
| `--android-dest <path>` | Android asset destination (default: `android/app/src/main/assets`) |
| `--ios-dest <path>` | iOS asset destination (default: `ios/SparklingGo/SparklingGo/Resources/Assets`) |

### `sparkling autolink`

Discover and link Sparkling method modules for Android and iOS. The CLI scans for `module.config.json` files in the workspace and `node_modules`, then updates Gradle/Podfile config and generates registry files.

```bash
npx sparkling autolink
```

| Option | Description |
| --- | --- |
| `--platform <platform>` | Platform to autolink: `android`, `ios`, or `all` (default: `all`) |

**What it does:**

- **Android** — Updates `settings.gradle(.kts)` and `app/build.gradle(.kts)` with module includes/dependencies, and generates `SparklingAutolink.kt`.
- **iOS** — Updates the `Podfile` with pod entries and generates `SparklingAutolink.swift`.

### `sparkling run:android`

Build, autolink, and launch the Android debug build in one step.

```bash
npx sparkling run:android
```

| Option | Description |
| --- | --- |
| `--copy` | Copy assets to native shells |
| `--skip-copy` | Skip copying assets (default) |

This command will:

1. Autolink method modules for Android
2. Build the Lynx bundle
3. Run `gradlew assembleDebug`
4. Install the APK on a connected device/emulator
5. Launch the main activity

### `sparkling run:ios`

Build, autolink, and launch the iOS simulator build in one step.

```bash
npx sparkling run:ios
```

| Option | Description |
| --- | --- |
| `--copy` | Copy assets to native shells |
| `--skip-copy` | Skip copying assets (default) |
| `--device <nameOrId>` | Simulator name or UDID |
| `--skip-pod-install` | Skip running `pod install` |

This command will:

1. Pick a simulator (prefers a booted device; falls back to common names like iPhone 17 Pro)
2. Autolink method modules for iOS
3. Run `pod install` (unless `--skip-pod-install`)
4. Build the Lynx bundle
5. Build, install, and launch the app on the simulator

You can also set the `SPARKLING_IOS_SIMULATOR` environment variable to specify a default simulator.

### `sparkling doctor`

Verify that your development environment is properly set up.

```bash
npx sparkling doctor
```

| Option | Description |
| --- | --- |
| `--platform <platform>` | Platform to check: `android`, `ios`, or `all` (default: `all`) |

The doctor command checks:

| Check | Requirement |
| --- | --- |
| Node.js | Version ^22 or ^24 |
| JDK | Version >= 11 (Android) |
| Android SDK | `ANDROID_HOME` set, `android-34` installed |
| adb | Available in PATH |
| Ruby | Version >= 2.7, < 3.4 |
| Xcode | Version >= 16 (macOS only) |
| CocoaPods | Installed (macOS only) |
| iOS Simulator | At least one available (macOS only) |

If any check fails, the output includes a hint on how to fix it.

## Global Options

All commands support the following flag:

| Option | Description |
| --- | --- |
| `-v, --verbose` | Enable verbose logging for debugging |

You can also set the `SPARKLING_VERBOSE` environment variable for the same effect.

## Typical Workflow

```bash
# 1. Create a project
npm create sparkling-app@latest my-app
cd my-app

# 2. Check your environment
npx sparkling doctor

# 3. Start the dev server for hot-reload development
npx sparkling dev

# 4. Run on Android
npx sparkling run:android

# 5. Run on iOS
npx sparkling run:ios

# 6. Build bundles for release
npx sparkling build --copy
```
