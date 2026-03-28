# Sparkling SDK - Android API Reference

Native Android SDK for hosting Sparkling content.
For usage guide and concepts, see [Containers](../guide/containers.md).
For the `hybrid://...` URL format, see [Scheme](./scheme.md).

## Dependency

```kotlin
dependencies {
  implementation("com.tiktok.sparkling:sparkling:2.0.0")
}
```

## Initialization (Application.onCreate)

```kotlin
HybridKit.init(this)

val baseInfoConfig = BaseInfoConfig(isDebug = BuildConfig.DEBUG)
val lynxConfig = SparklingLynxConfig.build(this) {
  // optional: add global Lynx behaviors/modules, template provider, etc.
}
val hybridConfig = SparklingHybridConfig.build(baseInfoConfig) {
  setLynxConfig(lynxConfig)
}

HybridKit.setHybridConfig(hybridConfig, this)
HybridKit.initLynxKit()
```

## Sparkling

Entry point for creating containers. See [Containers](../guide/containers.md) for usage guide.

| Method | Description |
|--------|-------------|
| `Sparkling.build(context, sparklingContext)` | Creates a `Sparkling` instance from an Android `Context` and a `SparklingContext`. |
| `navigate()` | Starts `SparklingActivity` (full-page container). Returns `true` on success. |
| `createView(withoutPrepare)` | Creates a `SparklingView` (embedded container). Returns `null` on failure. |

## SparklingView

Embedded container — a `FrameLayout` hosting Lynx content. See [Containers — Embedded](../guide/containers.md#embedded-containers) for usage guide.

| Method / Property | Description |
|-------------------|-------------|
| `prepare(sparklingContext)` | Initializes the view with scheme params, creates the kit view, and sets up loading/error UI. Called automatically by `createView()` unless `withoutPrepare = true`. |
| `loadUrl()` | Triggers content loading. Call after `prepare()` and after adding to the layout. |
| `onShowEvent()` | Forwards show event to Lynx runtime. **Must be called by hosting Activity/Fragment.** |
| `onHideEvent()` | Forwards hide event to Lynx runtime. **Must be called by hosting Activity/Fragment.** |
| `release()` | Destroys the Lynx runtime and cleans up resources. Call when the view is permanently removed. |
| `sendEventByJSON(eventName, params)` | Sends a custom event to the Lynx page. |
| `updateGlobalPropsByIncrement(data)` | Updates global props at runtime. |
| `refreshData(context, hybridContext)` | Refreshes content with updated scheme params without recreating the view. |
| `loadStatus()` | Returns current load status (`INIT`, `LOADING`, `SUCCESS`). |
| `isLoadSuccess()` | Returns `true` if content loaded successfully. |

## SparklingContext

Configuration object passed to both container types.

| Property | Description |
|----------|-------------|
| `scheme` | The `hybrid://...` URL to load. |
| `sparklingUIProvider` | Implements `SparklingUIProvider` for custom loading/error/toolbar views. |
| `hybridSchemeParam` | Parsed scheme parameters (auto-populated from `scheme`). |
| `containerId` | Unique container identifier (auto-generated). |

## SparklingUIProvider

Interface for customizing container UI. Applies to both full-page and embedded containers.

| Method | Description |
|--------|-------------|
| `getLoadingView(context)` | Returns a custom loading view, or `null` for the default. |
| `getErrorView(context)` | Returns a custom error view, or `null` for the default. |
| `getToolBar(context)` | Returns a custom `Toolbar` for `SparklingActivity` (full-page only). |
