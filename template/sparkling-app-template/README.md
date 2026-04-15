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

## Release Behavior

Release builds do not rely on debug-tool configuration.

- iOS Release: loads from `bundle=...` only
- Android Release: loads from `bundle=...` only

That means release packages read Lynx bundles from app assets/resources, not remote dev URLs.
