# create-sparkling-app

[![npm version](https://img.shields.io/npm/v/create-sparkling-app.svg)](https://npmjs.com/package/create-sparkling-app)
[![license](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](../../LICENSE)

CLI tool for creating new Sparkling app projects with pre-configured Android and iOS native shells.

## Quick Start

```bash
npx create-sparkling-app@latest my-app
cd my-app
npm run android  # or: npm run ios
```

> **Note:** Dependencies are installed automatically during project creation. Run `npm install` manually only if you skipped the auto-install step or need to add new dependencies.

## Usage

```bash
# Interactive mode (recommended)
npx create-sparkling-app@latest

# With project name
npx create-sparkling-app@latest my-app

# With verbose output
npx create-sparkling-app@latest my-app --verbose
```

Run `npx create-sparkling-app@latest --help` to see all available options.

## Local testing

When developing inside the Sparkling monorepo, see [LOCAL_TESTING.md](./LOCAL_TESTING.md) for building this CLI, running it from the workspace, and pointing `--template` at `template/sparkling-app-template`.

## What's Included

The generated project includes:

- ReactLynx frontend with TypeScript
- Pre-configured Android native shell
- Pre-configured iOS native shell

## Available Scripts

After creating your project, you can run:

| Script | Description |
|--------|-------------|
| `npm run build` | Build the Lynx bundle |
| `npm run run:android` | Build and run on Android |
| `npm run run:ios` | Build and run on iOS |
| `npm test` | Run tests |