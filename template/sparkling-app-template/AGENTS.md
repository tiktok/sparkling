# Working on this app

This is a [Sparkling](https://github.com/tiktok/sparkling) app: [Lynx](https://lynxjs.org)
pages rendered inside native containers, with native capabilities reached through
Sparkling Method packages. This file is for coding agents. It covers the things
that are true of every Sparkling app and are not guessable from the source.

## Layout

| Path | What it is |
| --- | --- |
| `src/pages/<name>/` | One page. `index.tsx` mounts it, `App.tsx` is the page. |
| `app.config.ts` | Entries, router, app identifiers, icon, splash, plugins. |
| `android/`, `ios/` | Real native projects. Yours to edit; nothing regenerates them. |
| `dist/` | Built `.lynx.bundle` files. Generated - never edit, never commit. |
| `android/app/src/main/assets/`, `ios/LynxResources/` | Where `build --copy` puts the bundles. Generated. |

## Commands

```bash
pnpm dev            # dev server on :5969 with HMR
pnpm build          # build bundles into dist/
pnpm build --copy   # build and copy them into the native projects
pnpm autolink       # re-link Sparkling Method packages after adding one
pnpm run:android    # build + install + launch
pnpm run:ios --copy
```

A JS-only change needs `build --copy` before the native app sees it. Adding or
removing a Method package needs `autolink` as well.

## The rules that bite

**One page is one bundle, with its own module state.** Pages do not share
memory. A module-level variable, a store, a cache - each pushed page gets its
own. State that has to survive a push travels either as a scheme parameter or
through `sparkling-storage`, and a page that must notice another page's change
re-reads it when it becomes visible again (see *Page visibility* below).

**Adding a page means editing `app.config.ts` twice.** Once under
`source.entry` so the bundle is built, once under `router` so the scheme can
address it. A name in one and not the other fails at runtime, not at build time.

**Scheme flags are the string `"1"`.** The Android parser compares against
`SchemeConstants.Value.ENABLED`, which is `"1"`. `hide_nav_bar=true` is
silently false, and the container's own navigation bar then sits above your
page's header. Same for `hide_status_bar`, `trans_status_bar`, `hide_loading`,
`hide_error`.

**The two platforms lay a page out differently.** Android puts the Lynx view
below the status bar unless `trans_status_bar=1`; iOS puts it below the status
bar and paints that strip in the container's own colour. If your page already
pads itself by the window's top inset, an iOS push needs
`trans_status_bar=1&status_font_mode=light` or you get a white band and content
pushed down twice. Android needs neither.

**Everything in `queryItems` is a string.** Scheme parameters arrive as
`lynx.__globalProps.queryItems`, flattened to strings; a missing key and an
empty one are indistinguishable. Parse and validate at the page boundary.

**Method calls are callbacks with an envelope.** A Method package function takes
`(params, callback)` and answers `{ code, msg, data }`. `code` is 1 for success.
Anything else is a failure, and the code matters: `-2` and `-4` mean the bridge
or namespace is absent, `-3` means either "module not registered" or "bad
parameter" depending on who answered, `-7` is a user cancellation, `-9` is not
found. Wrap the calls in a promise helper once, decide the failure taxonomy
there, and give every call a timeout - nothing in the pipe guarantees the
callback runs.

**Page visibility has two sets of event names.** The container tells the page it
appeared or disappeared through `GlobalEventEmitter`, and the names differ:
Android sends `viewAppeared` / `viewDisappeared` / `viewDisappearedWithType`
(`ViewEventUtils.kt`), iOS sends `kHybridKitEventViewDidAppear` /
`kHybridKitEventViewDidDisappear` (`SPKKitEvent.swift`). Subscribe to both sets.
Start in the visible state, so a runtime that never sends them does not leave
the page permanently "hidden" with its polling stopped.

## What the Lynx runtime does not have

Check before reaching for a web API. Absent, as of Lynx 4.x:

- `Intl` - no `toLocaleString`, no `Intl.NumberFormat`/`DateTimeFormat`.
  Format by hand or ship a polyfill.
- `TextEncoder` / `TextDecoder`.
- `WebSocket`, `XMLHttpRequest`. `fetch` exists but without `FormData`, `Blob`
  or CORS/redirect control.
- `localStorage` (only `sessionStorage`), the file system, `crypto`, `Worker`.
- `<canvas>`, WebGL. There is no 2D or 3D drawing surface.
- `:active` in CSS. Press feedback is `bindtouchstart` plus `bindtouchend` **and**
  `bindtouchcancel` - a press that becomes a scroll fires cancel only, and
  without it every element the finger crossed stays visually pressed.
- `Children` from React: ReactLynx does not re-export the helpers, so flatten
  arrays and fragments yourself before counting or interleaving children.
- `var()` inside an inline `style` object is not resolved. Use class names, or
  literal values.

WebAssembly exists on Android only, on the background thread only, and only in
its constructor form (`new WebAssembly.Module` / `Instance`).

## Native code

Adding a native capability means a Sparkling Method package, not a patch to the
container. `sparkling-method-cli init` scaffolds one; its `module.config.json`
is read by two different tools and needs both dialects (`packageName` and
`moduleName` for codegen; `name`, `methods`, `android.*` and `ios.*` for
autolink). `codegen` generates the abstract IDL class; you write the concrete
`<Module><Method>Method` class next to it, which is what autolink registers.

Native project files are source, not build output: `AndroidManifest.xml`,
`Info.plist`, `Podfile`, `build.gradle.kts` are yours to edit and your edits
survive. Permissions, intent filters, URL schemes, document types and
entitlements all go there by hand.

## Before you say it works

- `pnpm build` succeeds and the bundle sizes look sane.
- The change was run on **both** platforms, or the report says which one and why.
- No `dist/` or copied bundle is staged.
- A page you added is in both `source.entry` and `router`.
