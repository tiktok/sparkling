# RFC 0001 — SparklingModule: a native module system on Node-API

Status: draft · Supersedes nothing · Depends on nothing

## Summary

Replace Sparkling Method's JSON-dictionary pipe with a module system whose
bridge is implemented in C++ on top of the Node-API environment Lynx already
creates, so that a native capability is written once in C++ and reached from the
background JS thread without crossing a JSON boundary — with Kotlin and Swift
kept for the cases where a platform API genuinely requires them.

## Why

Sparkling Method works, and its shape is the reason it is hard to extend.

**The wire is JSON-shaped.** A call goes `pipe.call(name, [String: Any], callback)`
and comes back as `{ code, msg, data }`. On Android the params travel
`ReadableMap.toHashMap()` → `HashMap<String, Any>` → a `java.lang.reflect.Proxy`
per model → `org.json.JSONObject`
(`packages/sparkling-method/android/.../LynxPlatformDataProcessor.kt:33`). There
is nowhere in that chain for bytes to live. A file, an image, a buffer of audio
has to become base64 or a path.

**The IDL cannot express what the runtime can already do.** The codegen's type
lattice is `string | number | boolean | void | object | any`, arrays,
references, and literal unions
(`packages/sparkling-method-cli/src/codegen/types.ts:13`). It has no
`ArrayBuffer`, no `Promise`, no event or progress stream — while the pipe itself
already implements `callAsync`, `callWithTimeout`, `on` and `off`. The
declaration language is behind its own runtime.

**Every capability is written twice.** `packages/methods` carries three
abilities, each implemented once in Kotlin and once in Swift, with the semantics
kept in step by hand. That is the cost that decides how many abilities Sparkling
can afford to ship, and it is why there are three.

Meanwhile Lynx already hosts a Node-API environment on the background JS thread,
and PrimJS implements `napi_create_external_arraybuffer` on both its QuickJS and
its JSC backend — a genuine zero-copy handoff in both directions. Nothing in
Sparkling reaches for it.

## What this is not

It is not React Native's NitroModules, and the difference matters. Nitro builds
on JSI: a `HostObject` is a live C++ object the JS engine calls into
synchronously, on the JS thread, with the engine's own value types. Node-API is
a **C ABI** — stable across engines, which is why Lynx can offer it over both
QuickJS and JSC — and it buys that stability by going through `napi_value`
handles rather than engine internals. In practice:

- Synchronous property access on a host object is cheaper in JSI than in NAPI.
  Model modules as **functions and handles**, not as objects with many hot
  properties.
- NAPI gives a documented threading contract (`napi_threadsafe_function`) that
  JSI does not. Long work belongs on a worker thread that resolves a promise
  back on the JS thread; this is the shape the design is built around.
- A NAPI addon is only reachable from the **background JS thread**. There is no
  main-thread (MTS) NAPI on mobile: `NapiLoaderUI` does not implement
  `RegisterModule`. A module that must touch the main thread does so by posting
  to it from C++, never by being called there.

## Design

### The spec file

One declaration, in TypeScript, per module. It is a real type-checked file, not
a syntax-scanned one — the current parser uses `ts.createSourceFile` with no
`ts.Program` and therefore no type checker, which is why an imported type, a
`Record<K, V>` or a generic silently degrades to a dangling type name.

```ts
// storage.module.ts
import type { SparklingModule } from '@sparklingjs/module';

export interface Storage extends SparklingModule<'storage'> {
  get(key: string): Promise<ArrayBuffer | null>;
  set(key: string, value: ArrayBuffer): Promise<void>;
  remove(key: string): Promise<void>;
  keys(prefix?: string): Promise<string[]>;
  /** Fires for every write, including writes from another process. */
  onChange: SparklingEvent<{ key: string }>;
}
```

Types the bridge can carry: the primitives, `ArrayBuffer` and the typed arrays,
arrays and records of those, declared interfaces, `Promise<T>` for anything, and
`SparklingEvent<T>` for a stream. Anything the checker cannot resolve is an
error at codegen time, not a dangling identifier in the generated Kotlin.

### What is generated

| Artifact | For |
| --- | --- |
| `<Module>.napi.cc/.h` | The NAPI binding: argument marshalling, promise plumbing, the threadsafe function for events. |
| `<Module>Spec.h` | The pure-virtual C++ interface an implementation fills in. |
| `<module>.d.ts` + loader shim | What the page imports. Lazily resolves through `getNapiLoader().load('<Module>')`. |
| `<Module>Bridge.kt` / `.swift` | Only when the spec marks a member `@platform`, for the cases that need a JNI or ObjC++ hop. |

The generator emits **no** empty files. Today
`kotlin-impl-template`, `swift-impl-template`, `oc-idl-template`,
`oc-impl-template` and `ts/call-template` are zero bytes and referenced by no
code, which reads as "unfinished" to every newcomer.

### Loading — the part that is currently missing entirely

An addon's shared library is built and packaged, and **nothing loads it**.
`lynx.lib.json`'s `nodeApiAddons` entry is consumed by neither
`sparkling-app-cli`'s autolink nor `lynx-library-plugin`. The sequence has to
happen in the host application, in this order, before any `LynxView` exists:

```kotlin
System.loadLibrary("napi")          // PrimJS's Node-API implementation
System.loadLibrary("napi_adapter")  // the adapter Lynx reaches it through
System.loadLibrary("<Module>")      // the addon; its constructor calls napi_module_register
```

Lynx decides whether to attach a Node-API environment while it initialises, and
only does so if the first two are already in the process. On iOS the equivalent
is the `LynxWeakNodeAPI` pod plus `NAPI_USE(<Module>)` at startup.

`sparkling autolink` gains a `napi` module kind and generates exactly this, the
same way it generates the method registries today. This is the single highest
-value piece of the whole RFC: without it every adopter rediscovers the
ordering by trial and error.

### Versions

The Node-API runtime is not present in every published artifact:

| | minimum | why |
| --- | --- | --- |
| Android | `lynx` 4.1.0, `primjs` 4.1.1 | `liblynx.so` before 4.1.0 carries no Node-API runtime; `libnapi_adapter.so` exists only in primjs 4.0.0 and 4.1.1 |
| iOS | `Lynx` 4.0.2+, `PrimJS` 4.0.0+, `LynxWeakNodeAPI` | the pods are published on a different cadence from Maven |

`sparkling doctor` should assert these rather than letting a build fail at
`getNapiLoader() is undefined`.

### Storage as the first module

Storage is the right first module: its semantics are small, its current
implementation is two platform files, and it is the one every app uses.

Backed by [MMKV](https://github.com/Tencent/MMKV) (BSD 3-Clause, actively
maintained, C++ core; OpenSSL is pulled in only for the optional encryption,
which this does not enable). One implementation, mmap-backed, multi-process
safe, and — unlike `SharedPreferences` and `UserDefaults` — able to store bytes
without base64.

Migration is one-way and automatic on first read: if a key is absent in MMKV and
present in the old store, copy it across. The old store is not written to again.
The existing API keeps working; `get`/`set` over `ArrayBuffer` is additive.

### Coexistence

SparklingMethod stays. The two systems share nothing at runtime — one goes
through the pipe, the other through NAPI — so a module can be ported one at a
time and an app can use both. The deprecation, if it ever happens, is a separate
decision made after the new system has carried real modules for a release or
two.

## Staging

1. `napi` module kind in autolink: generate the load sequence, both platforms,
   plus the doctor checks. Nothing else depends on this and everything else does.
2. `@sparklingjs/module` spec types and a type-checked parser (a `ts.Program`,
   with diagnostics), emitting the C++ binding and the TS shim for a
   primitives-only module.
3. `ArrayBuffer` and promises end to end, with the zero-copy path tested for
   actual aliasing rather than equality of contents.
4. Events, via `napi_threadsafe_function`.
5. `storage` on MMKV, behind the existing method API, with the migration.
6. `@platform` members, for the capabilities that cannot be done in C++.

Each stage is independently useful. Stage 1 is worth landing even if the rest is
never built, because the loading problem is real today for anyone shipping a
Lynx addon.

## Open questions

- Binary size of MMKV on both platforms, measured rather than estimated.
- Whether the spec parser should live in `sparkling-method-cli` (renamed) or a
  new package, given the existing CLI is 2,400 lines built on a syntax-only
  parse that this replaces.
- Whether `SparklingEvent` should be delivered on the JS thread only, or offer
  an explicit "coalesce on the native side" mode for high-frequency sources.
