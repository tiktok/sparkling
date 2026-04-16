# sparkling-app-template

This template supports both local-bundle and remote-bundle workflows in development.

## Dev Server Port

Configure the preferred port in `app.config.ts`:

```ts
export default {
  dev: {
    server: {
      port: 5969,
    },
  },
}
```

`sparkling dev --port <number>` will update `dev.server.port` automatically.

## Debug Bundle Source (Local or Remote)

In **Debug** builds, the dev URL input supports two forms:

- Remote URL: `http://127.0.0.1:5969/main.lynx.bundle`
- Local bundle: `main.lynx.bundle`

Behavior:

- `http(s)://...` -> app loads with `url=...`
- `*.lynx.bundle` (or other non-http value) -> app loads with `bundle=...`

## Android Host Selection During `run:android`

`sparkling run:android` automatically picks host behavior:

- Emulator: uses `127.0.0.1` and applies `adb reverse tcp:<port> tcp:<port>`
- Physical device: injects your local LAN IPv4

## Optional sparkling method packages

The template ships with **`sparkling-navigation` only** (router) in npm, native autolink, Gradle, and CocoaPods.

Other method packages like `sparkling-media` (image picker, camera) and `sparkling-storage` (key-value persistence) are **not included by default**. Add them when your app needs those capabilities:

### 1. Install the npm package

```bash
pnpm add sparkling-media
# or
pnpm add sparkling-storage
```

### 2. Run autolink

```bash
pnpm autolink
# or: sparkling autolink
```

This single command updates all native integration points:

| Platform | Files updated |
|----------|---------------|
| Android  | `settings.gradle.kts` (project include), `build.gradle.kts` (dependency), `SparklingAutolink.kt` (module registry) |
| iOS      | `Podfile` (pod entry), `SparklingAutolink.swift` (module registry) |

### 3. Reinstall pods (iOS only)

```bash
cd ios && pod install && cd ..
```

### 4. Rebuild native apps

```bash
pnpm run:android   # or run:ios
```

Now you can import and use the newly added methods in your Lynx code:

```ts
import { pickImage } from 'sparkling-media'
import { setItem, getItem } from 'sparkling-storage'
```

## Release Behavior

Release builds do not rely on debug-tool configuration.

- iOS Release: loads from `bundle=...` only
- Android Release: loads from `bundle=...` only

That means release packages read Lynx bundles from app assets/resources, not remote dev URLs.
