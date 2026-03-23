# Local testing

Use this guide when you work inside the Sparkling monorepo and want to run **this package’s CLI** and the **in-repo app template** instead of the versions published to npm.

## Prerequisites

- Repository root: the monorepo root (the directory that contains `pnpm-workspace.yaml`).
- Build the CLI once so `packages/create-sparkling-app/dist/` exists (the `bin` entry loads `dist/`).

```bash
pnpm --filter create-sparkling-app build
```

## Run the CLI from this repository

From the monorepo root:

```bash
pnpm --filter create-sparkling-app exec create-sparkling-app my-app <other flags>
```

Equivalent:

```bash
node packages/create-sparkling-app/bin/index.js my-app <other flags>
```

When the CLI runs from a workspace build, the banner may show a `-local` suffix on the version label, which indicates you are not using a globally installed npm package.

## Use the in-repo template (skip npm `sparkling-app-template`)

Built-in template names such as `default` / `sparkling-default` resolve the template from npm. To use the **template sources in this repo**, pass a **filesystem path** to `--template` / `-t` so the CLI treats it as a custom template directory.

The template lives at:

`template/sparkling-app-template`

From the monorepo root (relative path):

```bash
pnpm --filter create-sparkling-app exec create-sparkling-app my-app \
  --template ./template/sparkling-app-template \
  -y
```

Using an absolute path (recommended if your shell’s current working directory is not the repo root):

```bash
pnpm --filter create-sparkling-app exec create-sparkling-app my-app \
  --template /absolute/path/to/sparkling/template/sparkling-app-template \
  -y
```

The path must exist and be a directory. The CLI will copy from that folder and will not download the template package from npm.

## Creating a project from another directory

Your shell can be **anywhere** (for example `~/projects` or `/tmp`). The new app is created relative to **current working directory** (the `project-name` / target path is resolved from `cwd`), not relative to the Sparkling repo.

1. Build the CLI once inside the clone (see [Prerequisites](#prerequisites)).
2. Call the CLI with an **absolute path** to `bin/index.js` (workspace-only `pnpm --filter … exec` only works from the monorepo root).
3. Pass an **absolute path** to `--template` pointing at `…/template/sparkling-app-template` in your clone. Relative `./template/...` only works if your `cwd` is the monorepo root.

Example (replace `/path/to/sparkling` with your clone):

```bash
cd ~/projects
node /path/to/sparkling/packages/create-sparkling-app/bin/index.js my-app \
  --template /path/to/sparkling/template/sparkling-app-template \
  -y
```

This creates `~/projects/my-app` using local CLI output and local template sources.

Optional: after `pnpm build` in `packages/create-sparkling-app`, run `pnpm link` or `npm link` from that package directory, then run `create-sparkling-app` from your `PATH`—you still need the **absolute** `--template` path when not working from the repo.

## Environment variable: `SPK_TEMPLATE_SKIP_INSTALL`

`SPK_TEMPLATE_SKIP_INSTALL=1` only affects resolution when the template is fetched **via npm** (it skips installing the npm package). It does **not** apply when you pass a **local directory** to `--template` as above.

## Generated app dependencies

Scaffolding copies the template and generates `package.json` from it. **Installing dependencies** in the new project (`npm install`, `pnpm install`, etc.) still resolves packages such as `sparkling-app-cli` from the registry according to semver in that template.

If you also need those packages to point at **local workspace packages** in this monorepo, you must configure that separately (for example `file:` dependencies, `pnpm.overrides`, or `pnpm link`) after the project is created.

## iOS `Pods` and `pod install`

If your template directory contains `ios/Pods` (for example after running `pod install` while developing the template), **do not commit that tree** to git if you can avoid it—regenerate it in each new app with:

```bash
cd my-app/ios && pod install
```

The scaffold step **does not copy** `Pods` or `.gradle` vendor/cache folders into the new project; install native dependencies in the generated app instead. That avoids permission errors from trying to rewrite third-party headers under `Pods/`.
