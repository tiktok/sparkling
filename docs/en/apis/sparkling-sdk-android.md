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
  // setSharedProcessDensityOverride(2.0f)
  setDefaultThreadStrategy(SparklingThreadStrategy.PART_ON_LAYOUT)
  setResourceFetcherFactory { sparklingContext ->
    SparklingResourceFetcherConfig.builder()
      .setGenericResourceFetcher(createGenericFetcher(sparklingContext))
      .setMediaResourceFetcher(createMediaFetcher(sparklingContext))
      .setTemplateResourceFetcher(createTemplateFetcher(sparklingContext))
      .build()
  }
}
val hybridConfig = SparklingHybridConfig.build(baseInfoConfig) {
  setLynxConfig(lynxConfig)
}

HybridKit.setHybridConfig(hybridConfig, this)
HybridKit.initLynxKit()
```

### Shared-process Lynx density

`setSharedProcessDensityOverride(density)` sets one logical density for every Sparkling
`LynxView` created in the host process. The value must be finite and greater than zero. When it is
unset, Sparkling leaves Lynx density unset and Lynx uses its default screen-density behavior.

Lynx currently treats a density override as process-wide state. If the host enables this option,
**every non-Sparkling `LynxView` in the same process must also be constructed with the same
density**. Do not derive or change this value per container or per URL.

This API provides a host-wide density policy; it does not claim arbitrary legacy URL density
compatibility.

## Sparkling

Entry point for creating containers. See [Containers](../guide/containers.md) for usage guide.

| Method | Description |
|--------|-------------|
| `Sparkling.build(context, sparklingContext)` | Creates a `Sparkling` instance from an Android `Context` and a `SparklingContext`. |
| `navigate()` | Starts `SparklingActivity` (full-page container). Returns `true` on success and throws a typed exception for incompatible Lynx configuration. |
| `createView(withoutPrepare)` | Creates a `SparklingView` (embedded container). Returns `null` for other creation failures and throws a typed exception for incompatible Lynx configuration when prepare is enabled. |

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
| `screenOrientationPolicy` | Optional typed orientation policy for a full-page `SparklingActivity`. |
| `threadStrategy` | Optional per-container `SparklingThreadStrategy`. Overrides the global default. |
| `hybridSchemeParam` | Parsed scheme parameters (auto-populated from `scheme`). |
| `lynxViewport` | Optional `SparklingLynxViewport(widthPx, heightPx)` fixed viewport in physical pixels. Programmatic configuration overrides parsed scheme dimensions. |
| `containerId` | Unique container identifier (auto-generated). |
| `resourceFetcherConfig` | Optional per-page typed resource fetchers. Overrides the global factory. |

## Screen orientation

`SparklingScreenOrientationPolicy` provides the Java-friendly `SYSTEM`,
`PORTRAIT`, and `LANDSCAPE` values for full-page containers:

```java
SparklingContext sparklingContext = new SparklingContext();
sparklingContext.setScreenOrientationPolicy(
    SparklingScreenOrientationPolicy.LANDSCAPE);
```

An optional application-wide default can be set with
`SparklingHybridConfig.Builder.setDefaultScreenOrientationPolicy(...)`.
Resolution order is:

1. `SparklingContext.screenOrientationPolicy`, including an explicit `SYSTEM`;
2. the canonical scheme `screen_orientation` value;
3. the global default;
4. Android's existing system/default behavior when all values are unset.

The canonical `portrait` and `landscape` values map to the corresponding typed
policies. Unknown canonical values preserve the existing `SYSTEM` behavior
instead of falling through to the global default. `SparklingActivity` applies
the resolved policy through Android's public `requestedOrientation` API before
creating its content.

The policy intentionally does not rotate an Activity that hosts an embedded
`SparklingView`. An embedded view does not own its host Activity; the host must
apply any desired orientation policy itself.

## Typed resource fetchers

Use `SparklingResourceFetcherConfig` to provide Lynx generic, media, and
template resource fetchers without accessing Sparkling's internal
`LynxViewBuilder`. Set it on `SparklingContext` for one page, or configure a
`SparklingResourceFetcherFactory` on `SparklingLynxConfig` to create fetchers
for every page.

```java
SparklingResourceFetcherConfig fetchers =
    SparklingResourceFetcherConfig.builder()
        .setGenericResourceFetcher(genericFetcher)
        .setMediaResourceFetcher(mediaFetcher)
        .setTemplateResourceFetcher(templateFetcher)
        .build();
sparklingContext.setResourceFetcherConfig(fetchers);
```

The per-page config takes precedence over the global factory. Sparkling
automatically enables Lynx generic resource fetching when a generic fetcher is
configured. A typed template fetcher is wrapped so Sparkling's resource
lifecycle receives one start and at most one finish event for both template
and SSR requests. If no typed template fetcher is configured, Sparkling keeps
using the existing `SimpleLynxTemplateProvider` path.

For advanced hosts that already provide `LynxKitInitParams`, set its `lynxViewport` property. Init
params take precedence over `SparklingContext.lynxViewport`, which takes precedence over canonical
scheme `width` and `height`. All three paths require a complete positive width/height pair.

## Thread strategy

`SparklingThreadStrategy` provides typed mappings for the Lynx rendering
strategies `ALL_ON_UI`, `MOST_ON_TASM`, `PART_ON_LAYOUT`, and `MULTI_THREADS`.
Set an optional global default with
`SparklingLynxConfig.Builder.setDefaultThreadStrategy(...)`, or override it for
one container with `SparklingContext.threadStrategy`. The per-container value
takes precedence. If neither value is set, Sparkling leaves the Lynx SDK
default unchanged.

### Fixed viewport compatibility

Do not combine an effective fixed viewport with the effective
`SparklingThreadStrategy.MULTI_THREADS` strategy. This combination can crash
inside the native Lynx SDK. Sparkling rejects it before constructing the
`LynxView` and throws `SparklingLynxConfigurationException` with
`SparklingLynxConfigurationError.FIXED_VIEWPORT_WITH_MULTI_THREADS`.
Both `navigate()` and prepared `createView(false)` calls fail synchronously
before starting or constructing a container.

Sparkling resolves all typed configuration before validating it:

1. viewport: `LynxKitInitParams.lynxViewport`, then
   `SparklingContext.lynxViewport`, then canonical scheme `width`/`height`;
2. thread strategy: `SparklingContext.threadStrategy`, then
   `SparklingLynxConfig.defaultThreadStrategy`, then the unchanged Lynx
   default.

Validation therefore does not depend on whether the viewport or strategy
setter ran first. A safe page strategy can override a global
`MULTI_THREADS` default. `MULTI_THREADS` without a fixed viewport and fixed
viewports with `ALL_ON_UI`, `MOST_ON_TASM`, `PART_ON_LAYOUT`, or no explicit
strategy keep their existing behavior.

Java callers can catch and inspect the typed failure:

```java
try {
  SparklingView view = Sparkling.build(context, sparklingContext).createView(false);
} catch (SparklingLynxConfigurationException exception) {
  if (exception.getError()
      == SparklingLynxConfigurationError.FIXED_VIEWPORT_WITH_MULTI_THREADS) {
    // Select a safe strategy or remove the fixed viewport.
  }
}
```

## SparklingUIProvider

Interface for customizing container UI. Applies to both full-page and embedded containers.

| Method | Description |
|--------|-------------|
| `getLoadingView(context)` | Returns a custom loading view, or `null` for the default. |
| `getErrorView(context)` | Returns a custom error view, or `null` for the default. |
| `getToolBar(context)` | Returns a custom `Toolbar` for `SparklingActivity` (full-page only). |

### Failed-view retry

To let a custom error view retry through Sparkling's SDK-owned load path, make
the view returned by `getErrorView(context)` implement
`SparklingRetryableErrorView`. This is an optional capability; existing
`SparklingUIProvider` implementations and plain error views remain compatible.

```java
public final class AppErrorView extends FrameLayout
    implements SparklingRetryableErrorView {
  private SparklingFailedViewRetry retry;

  @Override
  public void setSparklingRetry(SparklingFailedViewRetry retry) {
    this.retry = retry;
    retryButton.setOnClickListener(
        ignored -> {
          SparklingFailedViewRetry current = this.retry;
          if (current != null && current.retry()) {
            this.retry = null;
          }
        });
  }
}
```

Sparkling registers a new single-use `SparklingFailedViewRetry` for each
current load failure. `retry()` must be called on the Android main thread and
returns `true` only when that exact current failure is atomically accepted.
Double taps, stale requests, off-main calls, and calls after container release
return `false`. An accepted retry clears the error UI and invokes Sparkling's
owned reload path without reopening the route.

If the retry fails, Sparkling returns the container to `FAIL` and registers a
new retry request. If it succeeds, the container reaches `SUCCESS`. Sparkling
also calls `setSparklingRetry(null)` when a request becomes invalid, including
on a new load, success, accepted retry, or release. Implementations must replace
their previous listener/request and must not retain the supplied `Context`.
The same contract is used by full-page `SparklingActivity` containers and
embedded `SparklingView` containers.
