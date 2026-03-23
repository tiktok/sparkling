# Local testing

Use this guide when you work inside the Sparkling monorepo and want to run **this package’s CLI** from source instead of a globally installed or registry-fetched `sparkling-app-cli`.

## Prerequisites

- Monorepo root: the directory that contains `pnpm-workspace.yaml`.
- The `sparkling` / `sparkling-app-cli` entry prefers compiled output at `packages/sparkling-app-cli/dist/index.js`. If that file is missing, `bin.js` falls back to loading `src/index.ts` via `ts-node` (slower, but fine for quick iteration).

## Build

```bash
pnpm --filter sparkling-app-cli build
```

Use `pnpm --filter sparkling-app-cli dev` if you want `tsc --watch` while editing the CLI.

## Run the CLI from this repository

From the monorepo root:

```bash
pnpm --filter sparkling-app-cli exec sparkling -- <command> [options]
```

Examples:

```bash
pnpm --filter sparkling-app-cli exec sparkling -- --help
pnpm --filter sparkling-app-cli exec sparkling -- build
pnpm --filter sparkling-app-cli exec sparkling -- run:android
```

Equivalent (after a build, or rely on the `ts-node` fallback if `dist/` is absent):

```bash
node packages/sparkling-app-cli/bin.js build
node packages/sparkling-app-cli/bin.js --help
```

You can also invoke the long binary name:

```bash
pnpm --filter sparkling-app-cli exec sparkling-app-cli -- build
```

## Try it inside a sample app

The CLI uses **process.cwd()** for project paths (for example `app.config.ts` and native shells). Run it from an app directory that contains those files (for example `packages/playground` or a project generated from `template/sparkling-app-template`).

```bash
cd packages/playground
pnpm --filter sparkling-app-cli exec sparkling -- build
```

The `--filter` resolves the `sparkling` binary from `sparkling-app-cli` in the workspace; your shell’s current directory stays the app root, which is what the CLI expects.

## Dependencies in generated or linked apps

Projects created from the published template resolve `sparkling-app-cli` from the npm registry. To exercise **this repo’s CLI** from another package in the same workspace, point that package at the workspace version (for example `workspace:*` in a pnpm workspace) or use `pnpm link` / `file:` paths as appropriate for your setup.
