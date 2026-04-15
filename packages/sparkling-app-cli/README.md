# sparkling-app-cli

[![npm version](https://img.shields.io/npm/v/sparkling-app-cli.svg)](https://npmjs.com/package/sparkling-app-cli)
[![license](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](../../LICENSE)

Workspace helper CLI for building, running, and managing Sparkling applications on Android and iOS.

## Installation

> **Note:** Projects created with `create-sparkling-app` already include `sparkling-app-cli` as a dependency. Manual installation is only needed for existing projects or global usage.

```bash
# Global installation
npm install -g sparkling-app-cli@latest

# Or add to an existing project
npm install sparkling-app-cli@latest --save-dev
```

## Usage

```bash
# Build Lynx bundle (uses app.config.ts)
sparkling build

# Copy dist/ assets into Android/iOS resource folders
sparkling copy-assets

# Auto-link sparkling-method modules to native projects
sparkling autolink

# Build and run on Android emulator/device
sparkling run:android

# Build and run on iOS simulator
sparkling run:ios
```

Run `sparkling --help` to see all available commands and options.

## Dev Server Port And Host Behavior

- `sparkling dev` resolves the port from (highest to lowest): `--port` -> `app.config.ts` `dev.server.port` -> `app.config.ts` `lynxConfig.server.port` -> `5969`.
- `sparkling dev --port <x>` persists the selected port back to `app.config.ts` as `dev.server.port`.
- `sparkling run:ios` and `sparkling run:android` reuse the same resolved port and auto-start a dev server when needed.
- For Android, `run:android` auto-detects connected targets:
  - emulator: app uses `127.0.0.1` and CLI applies `adb reverse tcp:<port> tcp:<port>`
  - physical device: app uses your local LAN IPv4 and CLI starts the server on `0.0.0.0`

## Template Debug/Release Loading

In the app template:

- **Debug** builds can load either:
  - remote URL source (for example `http://<host>:<port>/main.lynx.bundle`), or
  - local asset bundle source (for example `main.lynx.bundle`).
- **Release** builds do not rely on debug-tool and load from assets only (`bundle=...`).

## Local testing

When developing inside the Sparkling monorepo, see [LOCAL_TESTING.md](./LOCAL_TESTING.md) for building this CLI, running `sparkling` / `sparkling-app-cli` from the workspace, and trying commands from an app directory.

## Requirements

- Node.js >= 18

## See Also

- [create-sparkling-app](../create-sparkling-app) - Create new Sparkling projects
- [Sparkling Documentation](../../docs/en/guide/get-started/create-new-app.md)
