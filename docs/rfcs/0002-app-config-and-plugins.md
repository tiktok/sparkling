# RFC 0002 — App config and config plugins

Status: draft · Depends on nothing · Related: RFC 0001

## Summary

Make `app.config.ts` describe the whole app and give it a plugin system that
applies ordered, idempotent modifications to the native projects. Unlike Expo,
Sparkling's native projects are **checked-in source**, so plugins patch them in
place rather than regenerating them, and every patch has to survive being run
twice and has to be reversible by hand.

## Why

`AppConfig` already declares two fields that no code reads:

- `appIcon?: string` — `packages/sparkling-app-cli/src/types.ts:59`
- `plugin?: PluginConfig[]`, including a typed `SplashScreenPluginConfig` —
  `types.ts:38-46`

Both are set by the template. Neither has a consumer anywhere in the repository.
The template's `res/values/styles.xml` even says

```xml
<!-- SplashScreen style removed; app.config.ts now manages shared assets -->
```

which describes a migration that did not land: the native splash style was
removed and the configuration that was supposed to replace it does nothing. A
new app therefore has a declared icon that is never applied and a splash screen
that is configured and absent.

Everything else an app needs from its native projects is hand-edited. From one
real app, the edits required before it could ship at all:

| Android | iOS |
| --- | --- |
| permissions | `CFBundleDocumentTypes` + `UTImportedTypeDeclarations` |
| `ACTION_VIEW` / `ACTION_SEND` intent filters with `data` patterns | `CFBundleURLTypes` |
| extra activities | `NSLocalNetworkUsageDescription`, `NSBonjourServices` |
| `application` attributes (name, `networkSecurityConfig`, `largeHeap`) | `LSSupportsOpeningDocumentsInPlace` |
| gradle deps, JDK toolchain, packaging options, ABI splits | entitlements, Podfile entries, build settings |

Each of those is one line of configuration in an Expo app and a manual,
easily-forgotten edit in a Sparkling one.

## The decision Expo does not have to make

Expo's `prebuild` **generates** `android/` and `ios/` from configuration; the
directories are build output and may be deleted at any time. Sparkling's are
source: they are created once by the template, committed, and then edited by
hand — for anything from a custom `Application` subclass to a native screen.

Regenerating them is therefore not on the table. Plugins **patch**, which brings
three requirements Expo can mostly ignore:

1. **Idempotence.** Running a plugin twice must produce the same file as running
   it once. Every edit is anchored by a marker comment and a plugin first
   removes its own previous output.
2. **Coexistence with hand edits.** A plugin owns the region between its
   markers. Everything outside is the developer's, and a plugin that cannot find
   its anchor reports it rather than guessing.
3. **Reversibility.** `sparkling prebuild --revert` strips every marked region,
   returning the project to what the developer wrote.

This is the same discipline `sparkling autolink` already follows when it rewrites
`settings.gradle`, `app/build.gradle` and the `Podfile` — the mechanism exists
and is proven; this generalises it rather than inventing it.

## Design

### Config

```ts
const config: AppConfig = {
  // ...what exists today
  android: {
    permissions: ['android.permission.INTERNET'],
    intentFilters: [
      { action: 'VIEW', categories: ['DEFAULT', 'BROWSABLE'], data: [{ scheme: 'myapp' }] },
    ],
    manifest: { application: { networkSecurityConfig: '@xml/network_security_config' } },
  },
  ios: {
    urlSchemes: ['myapp'],
    infoPlist: { NSLocalNetworkUsageDescription: 'Find printers on your network.' },
    entitlements: { 'com.apple.developer.networking.multicast': true },
  },
  plugins: [
    'sparkling-plugin-splash-screen',
    ['sparkling-plugin-app-icon', { image: './resource/app_icon.png' }],
    './plugins/with-our-custom-thing',
  ],
};
```

The common cases are declarative fields, because ninety percent of what apps
need is a permission, a scheme, or an Info.plist key, and forcing a plugin for
those is ceremony. `plugins` is the escape hatch and the extension point.

### The mod API

A plugin is a function that receives the config and returns it, having
registered modifications:

```ts
import { withAndroidManifest, withInfoPlist, withDangerousMod } from '@sparklingjs/config-plugins';

export default function withLocalNetwork(config: AppConfig): AppConfig {
  config = withInfoPlist(config, (plist) => {
    plist.NSBonjourServices = ['_printer._tcp'];
    return plist;
  });
  return withAndroidManifest(config, (manifest) => {
    manifest.addPermission('android.permission.CHANGE_WIFI_MULTICAST_STATE');
    return manifest;
  });
}
```

Mods run in a fixed order per platform file, each receiving the result of the
one before it, and the file is written once at the end. The typed mods —
`withAndroidManifest`, `withGradleProperties`, `withAppBuildGradle`,
`withInfoPlist`, `withEntitlements`, `withPodfile`, `withXcodeProject` — parse
the file into a structure, so two plugins touching the same file compose instead
of racing over a regex. `withDangerousMod` hands over raw text for everything
else, and is named to be uncomfortable.

### `sparkling prebuild`

A new command, which `build`, `run:android` and `run:ios` call first:

```
sparkling prebuild            # apply config and plugins to android/ and ios/
sparkling prebuild --check    # fail if applying would change anything (for CI)
sparkling prebuild --revert   # strip every marked region
```

`--check` is what keeps a repository honest: it turns "someone edited the
manifest instead of the config" into a failed build rather than a divergence
nobody notices.

### The first two plugins

`app-icon` and `splash-screen`, because they are already declared, already
documented, already in the template, and do nothing. Between them they exercise
the whole mechanism: reading an asset, writing generated files into both native
trees, patching a manifest and an Info.plist, and being idempotent about it.

They need an image resizer, which the CLI does not currently have any dependency
capable of. That choice — a pure-JS encoder plus a box filter, versus taking on
`sharp` — is the one open question that should be settled before implementation
starts, and it is settled by measuring install size and cold-start cost of the
CLI, not by preference.

## Staging

1. `sparkling prebuild` with the declarative Android and iOS fields, marker
   discipline, `--check` and `--revert`. No plugin API yet.
2. The mod API and plugin resolution (`node_modules` name, relative path,
   inline function), with ordering rules and a composition test.
3. `app-icon` and `splash-screen` as the first two real plugins, replacing the
   dead config fields.
4. `autolink` becomes a plugin itself, so there is one mechanism rather than two
   that both rewrite `settings.gradle`.

## Open questions

- Whether `prebuild --check` should run in the app template's CI by default.
- Whether declarative fields and plugins can both write the same key, and who
  wins. Proposed: declarative fields are applied first, plugins may override,
  and a conflict between two plugins is an error naming both.
- Image resizing dependency, as above.
