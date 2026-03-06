# Web Platform Guide

Sparkling supports rendering Lynx bundles in the browser via the [Lynx web platform](https://lynxjs.org). This lets you preview and test your app in a browser without a native simulator — useful for rapid iteration, CI testing, and potentially web deployment.

## Architecture

```
App TSX → rspeedy (multi-env) → *.lynx.bundle → Android/iOS native LynxView
                               → *.web.bundle  → <lynx-view> in browser
```

The same source code produces two outputs:
- **`*.lynx.bundle`** — consumed by native LynxView on Android/iOS
- **`*.web.bundle`** — consumed by `<lynx-view>` custom element in the browser

## New Projects

When you run `npm create sparkling-app@latest`, the scaffolder asks:

```
? Enable web platform support? (preview in browser) (Y/n)
```

Answering **Yes** (the default) sets up the project with:
- `environments` config in `app.config.ts` for dual-output builds
- `dev:web`, `build:web`, and `run:web` scripts
- `sparkling-web-shell` devDependency

You can also pass `--web` or `--no-web` as CLI flags:
```bash
npm create sparkling-app@latest my-app -- --web     # enable web
npm create sparkling-app@latest my-app -- --no-web  # disable web
```

## Existing Projects

To add web support to an existing Sparkling project:

### 1. Update `app.config.ts`

Add the `environments` block and move `assetPrefix` into the `lynx` environment:

```ts
const lynxConfig = defineConfig({
  source: {
    entry: {
      main: './src/pages/main/index.tsx',
    },
  },
  output: {
    filename: {
      bundle: '[name].lynx.bundle',
    },
  },
  environments: {
    web: {
      output: {
        assetPrefix: '/',
        distPath: {
          root: 'dist/web',
        },
      },
    },
    lynx: {
      output: {
        assetPrefix: 'asset:///',
      },
    },
  },
  plugins: [/* ... */],
})
```

### 2. Add dependencies

```bash
pnpm add -D sparkling-web-shell @lynx-js/web-core @lynx-js/web-elements
```

### 3. Add scripts to `package.json`

```json
{
  "scripts": {
    "dev:web": "sparkling-app-cli dev --platform web",
    "build:web": "sparkling-app-cli build --platform web",
    "run:web": "sparkling-app-cli run:web"
  }
}
```

### 4. Run autolink

```bash
pnpm autolink
```

This generates web method imports so that method SDKs (navigation, storage, etc.) work in the browser.

## Development Workflow

### Start the web dev server

```bash
pnpm run:web
```

This builds your web bundles and starts the web shell server at `http://localhost:3000`.

### Navigate between pages

For multi-page apps, use the `?page=` query parameter:

- `http://localhost:3000` — loads `main.web.bundle` (default)
- `http://localhost:3000?page=second` — loads `second.web.bundle`

When using `sparkling-navigation`'s `router.open()` in your app code, page transitions are handled automatically via the History API.

### Hot Module Replacement

The rspeedy dev server provides HMR for web bundles. Changes to your source files are reflected in the browser without a full reload.

### Debugging

Use your browser's built-in DevTools (Chrome DevTools, Firefox DevTools, etc.) to inspect the DOM, debug JavaScript, profile performance, and monitor network requests.

## Build for Web

To produce web bundles without starting a dev server:

```bash
pnpm build:web
```

Output goes to `dist/web/`. These bundles can be served by any static file server.

## Next Steps

- [Web Method Implementations](/guide/web-method-implementations) — how method SDKs work on web
- [Web Limitations](/guide/web-limitations) — what native features aren't available on web
