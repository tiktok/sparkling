# sparkling-method-cli

[![npm version](https://img.shields.io/npm/v/sparkling-method-cli.svg)](https://npmjs.com/package/sparkling-method-cli)
[![license](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](../../LICENSE)

CLI utilities for creating and managing Sparkling method modules with cross-platform code generation.

## Installation

```bash
npm install -g sparkling-method-cli@latest
```

Or add to your project:

```bash
npm install sparkling-method-cli@latest --save-dev
```

## Usage

```bash
# Create a new method module
sparkling-method-cli init my-method

# Generate native code from TypeScript definitions
sparkling-method-cli codegen
```

Run `sparkling-method-cli --help` to see all available commands and options.

## Local testing

When developing inside the Sparkling monorepo, see [LOCAL_TESTING.md](./LOCAL_TESTING.md) for building this CLI, running it from the workspace, and pointing `init` at `src/create/template` or the bundled `dist/create/template`.

## Workflow

1. Create method module: `sparkling-method-cli init my-method`
2. Define TypeScript interfaces in `src/*.d.ts`
3. Generate native code: `sparkling-method-cli codegen`
4. Implement native handlers in `android/` and `ios/`