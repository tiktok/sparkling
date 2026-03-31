# Containers

Sparkling content always runs inside a **native container** — a platform view that hosts the Lynx runtime. The SDK provides two container modes to fit different use cases.

## Two container modes

| | Full-page | Embedded |
|---|---|---|
| **What it is** | A dedicated screen (`UIViewController` / `Activity`) that manages its own nav bar, status bar, and lifecycle | A plain view (`UIView` / `FrameLayout`) you place inside an existing native layout |
| **Typical use** | Standard page navigation | Cards, banners, partial-screen panels, or any native screen that mixes native and Lynx UI |
| **iOS class** | `SPKViewController` (via `SPKRouter`) | `SPKContainerView` |
| **Android class** | `SparklingActivity` (via `Sparkling.navigate()`) | `SparklingView` (via `Sparkling.createView()`) |

## Full-page containers

Full-page containers are the default way to display Sparkling content. The native layer manages the navigation bar, status bar, screen orientation, and loading/error views — you configure them via [scheme parameters](./scheme.md).

### iOS

```swift
// Option A: push onto an existing navigation stack
SPKRouter.open(withURL: "hybrid://lynxview_page?bundle=detail.lynx.bundle&title=Detail", context: nil)

// Option B: create and manage the VC yourself
let vc = SPKRouter.create(withURL: url, context: SPKContext())
navigationController.pushViewController(vc, animated: true)
```

### Android

```kotlin
val ctx = SparklingContext().apply {
  scheme = "hybrid://lynxview_page?bundle=detail.lynx.bundle&title=Detail"
}
Sparkling.build(this, ctx).navigate()   // starts SparklingActivity
```

### Configuration via scheme

Full-page containers are configured through URL query parameters. Some common ones:

| Parameter | Effect |
|-----------|--------|
| `title` | Navigation bar title |
| `hide_nav_bar=1` | Hide the navigation bar |
| `hide_status_bar=1` | Hide the status bar |
| `trans_status_bar=1` | Transparent status bar (content extends behind it) |
| `show_nav_bar_in_trans_status_bar=1` | Keep nav bar visible even with transparent status bar |
| `nav_bar_color` | Navigation bar background color |
| `container_bg_color` | Container background color |

See [Scheme](./scheme.md) for the full parameter list.

## Embedded containers

Embedded containers let you place Sparkling content anywhere inside a native layout. You control the frame, and the container renders Lynx content within it.

### iOS — SPKContainerView

```swift
// 1. Create and add to your view hierarchy
let spkView = SPKContainerView(frame: containerView.bounds)
spkView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
containerView.addSubview(spkView)

// 2. Load a bundle
let context = SPKContext()
spkView.load(withURL: "hybrid://lynxview?bundle=card.lynx.bundle", context)
```

#### Content sizing (iOS)

By default, the container keeps its assigned frame. Set `sparkContentMode` to let the container resize based on Lynx content:

| Mode | Behavior |
|------|----------|
| `FixedSize` (default) | Container keeps its frame; Lynx fills it. |
| `FixedWidth` | Width stays fixed; height adjusts to content. |
| `FixedHeight` | Height stays fixed; width adjusts to content. |
| `FitSize` | Both dimensions adjust to content. |

After loading completes, read `preferredLayoutSize` to get the content's intrinsic size.

### Android — SparklingView

```kotlin
// 1. Create via Sparkling builder
val ctx = SparklingContext().apply {
  scheme = "hybrid://lynxview?bundle=card.lynx.bundle"
}
val spkView = Sparkling.build(this, ctx).createView()

// 2. Add to your layout and load
container.addView(spkView)
spkView?.loadUrl()
```

## Lifecycle

### Full-page containers

Lifecycle is managed automatically. The `SPKViewController` / `SparklingActivity` forwards `viewDidAppear` / `onResume` and `viewDidDisappear` / `onPause` to the Lynx runtime. No extra work needed.

### Embedded containers

Since the SDK has no control over your hosting view controller or fragment, **you must forward visibility events manually**. If you skip this, the Lynx runtime won't know when to pause timers, animations, or network requests.

**iOS:**

```swift
override func viewDidAppear(_ animated: Bool) {
    super.viewDidAppear(animated)
    spkView.handleViewDidAppear()
}

override func viewDidDisappear(_ animated: Bool) {
    super.viewDidDisappear(animated)
    spkView.handleViewDidDisappear()
}
```

**Android:**

```kotlin
// Fragment.onResume or when the view becomes visible
spkView?.onShowEvent()

// Fragment.onPause or when the view becomes hidden
spkView?.onHideEvent()

// When permanently destroying the view
spkView?.release()
```

### Lifecycle delegate (iOS)

Set `containerLifecycleDelegate` on the `SPKContext` to observe loading status. This works for both container modes:

```swift
context.containerLifecycleDelegate = self

// Key callbacks:
func container(_ container: SPKContainerProtocol,
               didFinishLoadWithURL url: URL?) { /* loaded */ }
func container(_ container: SPKContainerProtocol,
               didLoadFailedWithURL url: URL?, error: Error?) { /* failed */ }
func containerDidFirstScreen(_ container: SPKContainerProtocol) { /* first paint */ }
```

## Communicating with Lynx

Both container modes support sending events and updating global props at runtime.

### Send events

**iOS (both modes):**
```swift
spkView.send(event: "refresh", params: ["key": "value"], callback: nil)
```

**Android (embedded):**
```kotlin
spkView?.sendEventByJSON("refresh", JSONObject().put("key", "value"))
```

### Update globalProps

**iOS (both modes):**
```swift
spkView.update(withGlobalProps: ["theme": "dark"])
```

**Android (embedded):**
```kotlin
spkView?.updateGlobalPropsByIncrement(mapOf("theme" to "dark"))
```

## Next steps

- [Scheme](./scheme.md) — full list of URL parameters for configuring containers
- [Multi-page Navigation](./multi-page-navigation.md) — navigate between pages, pass data, close pages
- [Sparkling SDK — iOS](../apis/sparkling-sdk-ios.md) — iOS API reference
- [Sparkling SDK — Android](../apis/sparkling-sdk-android.md) — Android API reference
