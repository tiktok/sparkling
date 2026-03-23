# Local testing

Use this guide when you work inside the Sparkling monorepo and want to run **this package’s CLI** and the **in-repo templates** used by `init` and `codegen` instead of the version published to npm.

## Prerequisites

- Monorepo root: the directory that contains `pnpm-workspace.yaml`.
- The published package entry is `dist/index.js`. You must build so `dist/` exists and bundled assets are copied (see below).

## Build

The build compiles TypeScript and copies template trees into `dist/`:

```bash
pnpm --filter sparkling-method-cli build
```

This runs `tsc` and `scripts/copy-assets.js`, which copies:

- `src/create/template` → `dist/create/template` (used by `sparkling-method-cli init`)
- `src/codegen/template` → `dist/codegen/template` (used by `sparkling-method-cli codegen`)

If you edit files under `src/.../template`, run `build` again so the copies under `dist/` stay in sync.

## Run the CLI from this repository

From the monorepo root:

```bash
pnpm --filter sparkling-method-cli exec sparkling-method-cli -- <command> [options]
```

Examples:

```bash
pnpm --filter sparkling-method-cli exec sparkling-method-cli -- --help
pnpm --filter sparkling-method-cli exec sparkling-method-cli -- codegen
```

Directly:

```bash
node packages/sparkling-method-cli/dist/index.js --help
```

## `init`: use the in-repo method template

By default, `init` uses the bundled template next to the compiled `create` code (`dist/create/template` after a build).

To point at the **source tree** in this repo (for example while changing `src/create/template`), pass `--template` / `-t` with a path relative to your **current working directory** (the CLI resolves it with `path.resolve(process.cwd(), ...)`).

From the monorepo root:

```bash
pnpm --filter sparkling-method-cli exec sparkling-method-cli -- init my-method \
  --template ./packages/sparkling-method-cli/src/create/template
```

Using an absolute path:

```bash
pnpm --filter sparkling-method-cli exec sparkling-method-cli -- init my-method \
  --template /absolute/path/to/sparkling/packages/sparkling-method-cli/src/create/template
```

## `codegen`

Run `codegen` with the working directory set to a method module workspace (where your `.d.ts` definitions and `module.config.json` live). After you change **codegen** Mustache templates under `src/codegen/template`, run `pnpm --filter sparkling-method-cli build` again so `dist/codegen/template` is updated.

## Generated module dependencies

New method packages scaffolded by `init` may list `sparkling-method-cli` in `package.json` for `codegen` scripts. Installing dependencies from the registry still resolves that package from npm unless you wire the project to this workspace (for example `workspace:*`, `pnpm link`, or `file:` dependencies).
