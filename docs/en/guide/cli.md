# Sparkling CLI

Sparkling CLI (`sparkling-app-cli`) is the built-in command-line tool that powers the development workflow. It handles building Lynx bundles, autolinking native method modules, running apps on Android, iOS, and HarmonyOS, and diagnosing your environment.

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
| `--copy` | Copy built assets to Android, iOS, and HarmonyOS native shells |
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
| `--port <number>` | Dev server port (default: `app.config.ts -> dev.server.port`, fallback `5969`) |
| `--host <host>` | Dev server host (default: `app.config.ts -> dev.server.host`, then Rspeedy default) |

The default port **5969** spells **LYNX** on a phone keypad (L=5, Y=9, N=6, X=9).

Port resolution priority is:

1. `--port`
2. `app.config.ts` `dev.server.port`
3. `app.config.ts` `lynxConfig.server.port`
4. `5969`

If you run `sparkling dev --port <number>`, the CLI writes it back to `app.config.ts` as `dev.server.port`.

Once the server is running, point your app to `http://<your-ip>:5969/main.lynx.bundle` (or whatever entry point you need). In the project template, **DEBUG** builds connect to the dev server automatically.

### `sparkling copy-assets`

Copy compiled bundles into Android, iOS, and HarmonyOS resource directories.

```bash
npx sparkling copy-assets
```

| Option | Description |
| --- | --- |
| `--source <path>` | Path to compiled assets (default: `dist`) |
| `--android-dest <path>` | Android asset destination (default: `android/app/src/main/assets`) |
| `--ios-dest <path>` | iOS asset destination (default: `ios/LynxResources/Assets`) |
| `--harmony-dest <path>` | HarmonyOS asset destination (defaults to `harmony/entry/src/main/resources/rawfile` when a `harmony/` project exists) |

For compatibility with existing Android/iOS-only apps, the default command does not create a new
`harmony/` directory. Pass `--harmony-dest` explicitly or generate the HarmonyOS shell first.

### `sparkling autolink`

Discover Sparkling method modules for Android, iOS, and HarmonyOS. The CLI scans for `module.config.json` files in the workspace and `node_modules`, then updates the native Sparkling method links and generates Sparkling registry files. Host apps also enable the Lynx library Autolink plugins, so non-method Lynx libraries that ship `lynx.lib.json` can be linked by the Lynx toolchain.

```bash
npx sparkling autolink
```

| Option | Description |
| --- | --- |
| `--platform <platform>` | Platform to autolink: `android`, `ios`, `harmony`, or `all` (default: `all`) |

**What it does:**

- **Android** — Links Sparkling method Gradle projects and generates `SparklingAutolink.kt`. Debug-tool packages are linked as `debugImplementation`.
- **iOS** — Links Sparkling method pods and generates `SparklingAutolink.swift`. Debug-tool packages are linked in the debug target.
- **HarmonyOS** — Generates `SparklingAutolink.ets`; declared ArkTS handlers are copied, statically imported, and dispatched through `spkPipe`.

### `sparkling run:android`

Build, autolink, and launch the Android debug build in one step.

```bash
npx sparkling run:android
```

| Option | Description |
| --- | --- |
| `--copy` | Copy assets to native shells |
| `--skip-copy` | Skip copying assets (default) |
| `--host <host>` | Dev server host for Android devices, useful when auto-detected LAN IPv4 is not the desired network |

This command will:

1. Autolink method modules for Android
2. Resolve the dev port from `app.config.ts` and auto-start a dev server if needed
3. Auto-detect device type and inject host for template debug URL:
   - emulator: `127.0.0.1` (and auto `adb reverse`)
   - physical device: host LAN IPv4
4. Build the Lynx bundle
5. Run `gradlew assembleDebug`
6. Install the APK on a connected device/emulator
7. Launch the main activity

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
2. Resolve the dev port from `app.config.ts` and auto-start a dev server if needed
3. Autolink method modules for iOS
4. Run `pod install` (unless `--skip-pod-install`)
5. Build the Lynx bundle
6. Build, install, and launch the app on the simulator

You can also set the `SPARKLING_IOS_SIMULATOR` environment variable to specify a default simulator.

### `sparkling run:harmony`

Build, autolink, and launch the HarmonyOS debug HAP in one step.

```bash
npx sparkling run:harmony --copy
```

This command will:

1. Autolink method modules for HarmonyOS
2. Build the Lynx bundle
3. Copy bundles into `harmony/entry/src/main/resources/rawfile` when `--copy` is specified
4. Install OHPM dependencies
5. Run `hvigorw assembleHap`
6. Install and launch the HAP with `hdc` when a connected device or emulator is available

### `sparkling doctor`

Verify that your development environment is properly set up.

```bash
npx sparkling doctor
```

| Option | Description |
| --- | --- |
| `--platform <platform>` | Platform to check: `android`, `ios`, `harmony`, or `all` (default: `all`) |

The doctor command checks:

| Check | Requirement |
| --- | --- |
| Node.js | Version ^22 or ^24 |
| JDK | Version >= 11 (Android) |
| Android SDK | `ANDROID_HOME` set, `android-34` installed |
| adb | Available in PATH |
| Ruby | Version >= 3.2.6, < 3.4 |
| Xcode | Version >= 16 (macOS only) |
| CocoaPods | Installed (macOS only) |
| iOS Simulator | At least one available (macOS only) |
| OHPM | Available from PATH, HarmonyOS Command Line Tools, or DevEco Studio |
| Hvigor | Available from PATH, HarmonyOS Command Line Tools, or DevEco Studio |
| hdc | Available from PATH, HarmonyOS Command Line Tools, or DevEco Studio |

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

# 6. Run on HarmonyOS (local bundled assets)
npx sparkling run:harmony --copy

# 7. Build bundles for release
npx sparkling build --copy
```
