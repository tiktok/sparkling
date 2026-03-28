# Sparkling SDK - iOS API Reference

Native iOS SDK for hosting Sparkling content.
For usage guide and concepts, see [Containers](../guide/containers.md).
For the `hybrid://...` URL format, see [Scheme](./scheme.md).

## Dependency (CocoaPods)

```ruby
pod 'Sparkling', '2.0.0'
```

## Initialization

```swift
SPKServiceRegister.registerAll()
SPKExecuteAllPrepareBootTask()
```

`SPKServiceRegister` is defined in your app target (see the template under
`template/sparkling-app-template/ios/.../MethodServices/SPKServiceRegistrar.swift`).

## SPKRouter

Opens full-page Sparkling containers. See [Containers — Full-page](../guide/containers.md#full-page-containers) for usage guide.

| Method | Description |
|--------|-------------|
| `SPKRouter.open(withURL:context:)` | Creates and pushes a container onto the current navigation stack. Returns `(container, success)`. |
| `SPKRouter.create(withURL:context:frame:)` | Creates a container without presenting it. Returns a `UIViewController & SPKContainerProtocol`. |
| `SPKRouter.close(container:)` | Pops or dismisses the given container. |
| `SPKRouter.openInSystemBrowser(withURL:)` | Opens an HTTP/HTTPS URL in Safari. |

## SPKContainerView

Embeds Sparkling content as a subview. See [Containers — Embedded](../guide/containers.md#embedded-containers) for usage guide.

| Method / Property | Description |
|-------------------|-------------|
| `SPKContainerView(frame:)` | Creates a container view with the given frame. |
| `load(withURL:context:forceInitKitView:)` | Loads content from a scheme URL string. |
| `load(withParams:context:forceInitKitView:)` | Loads content from an `SPKHybridSchemeParam`. |
| `reload(context:)` | Reloads the current content. |
| `handleViewDidAppear()` | Forwards appear event to Lynx runtime. **Must be called by hosting VC.** |
| `handleViewDidDisappear()` | Forwards disappear event to Lynx runtime. **Must be called by hosting VC.** |
| `send(event:params:callback:)` | Sends a custom event to the Lynx page. |
| `config(withGlobalProps:)` | Sets initial global props. |
| `update(withGlobalProps:)` | Updates global props at runtime. |
| `sparkContentMode` | Sizing mode: `FixedSize` (default), `FixedWidth`, `FixedHeight`, `FitSize`. |
| `preferredLayoutSize` | Intrinsic content size after loading (useful with non-fixed modes). |
| `containerID` | Unique identifier for this container instance. |
| `loadState` | Current loading state (`NotLoad`, `Loading`, `Loaded`, `Failed`). |

## SPKContext

Configuration object passed to both container types.

| Property | Description |
|----------|-------------|
| `containerLifecycleDelegate` | Delegate conforming to `SPKContainerLifecycleProtocol` for loading callbacks. |
| `loadingViewBuilder` | Closure that returns a custom loading view. |
| `failedViewBuilder` | Closure that returns a custom error view. |
| `naviBar` | Custom navigation bar (full-page containers only). |
| `appTheme` | Theme configuration. |
| `customUIElements` | Custom Lynx UI elements to register. |
| `extra` | Dictionary of additional data passed to the container. |

## SPKContainerLifecycleProtocol

Callback protocol for observing container lifecycle events. Works with both container types.

| Callback | Description |
|----------|-------------|
| `container(_:didFinishLoadWithURL:)` | Content loaded successfully. |
| `container(_:didLoadFailedWithURL:error:)` | Content failed to load. |
| `containerDidFirstScreen(_:)` | First frame rendered. |
| `container(_:didStartFetchResourceWithURL:)` | Resource fetch started. |
| `containerDidUpdate(_:)` | Content updated. |
| `container(_:didChangeIntrinsicContentSize:)` | Intrinsic content size changed (embedded mode). |
