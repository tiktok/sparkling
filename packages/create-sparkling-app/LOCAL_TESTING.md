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

## Environment variable: `SPK_TEMPLATE_SKIP_INSTALL`

`SPK_TEMPLATE_SKIP_INSTALL=1` only affects resolution when the template is fetched **via npm** (it skips installing the npm package). It does **not** apply when you pass a **local directory** to `--template` as above.

## Generated app dependencies

Scaffolding copies the template and generates `package.json` from it. **Installing dependencies** in the new project (`npm install`, `pnpm install`, etc.) still resolves packages such as `sparkling-app-cli` from the registry according to semver in that template.

If you also need those packages to point at **local workspace packages** in this monorepo, you must configure that separately (for example `file:` dependencies, `pnpm.overrides`, or `pnpm link`) after the project is created.
