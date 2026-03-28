# Sparkling SDK - iOS API 参考

用于承载 Sparkling 内容的 iOS 原生 SDK。
使用指南和概念说明请参阅[容器](../guide/containers.md)。
关于 `hybrid://...` URL 格式，请参阅 [Scheme](./scheme.md)。

## 依赖（CocoaPods）

```ruby
pod 'Sparkling', '2.0.0'
```

## 初始化

```swift
SPKServiceRegister.registerAll()
SPKExecuteAllPrepareBootTask()
```

`SPKServiceRegister` 通常在你的 App Target 中定义（参见模板
`template/sparkling-app-template/ios/.../MethodServices/SPKServiceRegistrar.swift`）。

## SPKRouter

打开全页 Sparkling 容器。使用指南请参阅[容器 — 全页容器](../guide/containers.md#全页容器)。

| 方法 | 说明 |
|------|------|
| `SPKRouter.open(withURL:context:)` | 创建容器并 push 到当前导航栈。返回 `(container, success)`。 |
| `SPKRouter.create(withURL:context:frame:)` | 创建容器但不展示。返回 `UIViewController & SPKContainerProtocol`。 |
| `SPKRouter.close(container:)` | Pop 或 dismiss 指定容器。 |
| `SPKRouter.openInSystemBrowser(withURL:)` | 在 Safari 中打开 HTTP/HTTPS URL。 |

## SPKContainerView

以子视图形式嵌入 Sparkling 内容。使用指南请参阅[容器 — 嵌入式容器](../guide/containers.md#嵌入式容器)。

| 方法 / 属性 | 说明 |
|-------------|------|
| `SPKContainerView(frame:)` | 创建指定 frame 的容器视图。 |
| `load(withURL:context:forceInitKitView:)` | 通过 scheme URL 字符串加载内容。 |
| `load(withParams:context:forceInitKitView:)` | 通过 `SPKHybridSchemeParam` 加载内容。 |
| `reload(context:)` | 重新加载当前内容。 |
| `handleViewDidAppear()` | 转发 appear 事件到 Lynx 运行时。**宿主 VC 必须调用。** |
| `handleViewDidDisappear()` | 转发 disappear 事件到 Lynx 运行时。**宿主 VC 必须调用。** |
| `send(event:params:callback:)` | 向 Lynx 页面发送自定义事件。 |
| `config(withGlobalProps:)` | 设置初始 globalProps。 |
| `update(withGlobalProps:)` | 运行时更新 globalProps。 |
| `sparkContentMode` | 尺寸模式：`FixedSize`（默认）、`FixedWidth`、`FixedHeight`、`FitSize`。 |
| `preferredLayoutSize` | 加载完成后的内容固有尺寸（非固定模式下使用）。 |
| `containerID` | 容器实例的唯一标识符。 |
| `loadState` | 当前加载状态（`NotLoad`、`Loading`、`Loaded`、`Failed`）。 |

## SPKContext

传递给两种容器类型的配置对象。

| 属性 | 说明 |
|------|------|
| `containerLifecycleDelegate` | 遵循 `SPKContainerLifecycleProtocol` 的回调代理。 |
| `loadingViewBuilder` | 返回自定义加载视图的闭包。 |
| `failedViewBuilder` | 返回自定义错误视图的闭包。 |
| `naviBar` | 自定义导航栏（仅全页容器）。 |
| `appTheme` | 主题配置。 |
| `customUIElements` | 需要注册的自定义 Lynx UI 元素。 |
| `extra` | 传递给容器的额外数据字典。 |

## SPKContainerLifecycleProtocol

观察容器生命周期事件的回调协议。适用于两种容器类型。

| 回调 | 说明 |
|------|------|
| `container(_:didFinishLoadWithURL:)` | 内容加载成功。 |
| `container(_:didLoadFailedWithURL:error:)` | 内容加载失败。 |
| `containerDidFirstScreen(_:)` | 首帧渲染完成。 |
| `container(_:didStartFetchResourceWithURL:)` | 资源拉取开始。 |
| `containerDidUpdate(_:)` | 内容更新。 |
| `container(_:didChangeIntrinsicContentSize:)` | 内容固有尺寸变化（嵌入式模式）。 |
