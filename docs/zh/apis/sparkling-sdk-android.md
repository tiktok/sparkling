# Sparkling SDK - Android API 参考

用于承载 Sparkling 内容的 Android 原生 SDK。
使用指南和概念说明请参阅[容器](../guide/containers.md)。
关于 `hybrid://...` URL 格式，请参阅 [Scheme](./scheme.md)。

## 依赖

```kotlin
dependencies {
  implementation("com.tiktok.sparkling:sparkling:2.0.0")
}
```

## 初始化（Application.onCreate）

```kotlin
HybridKit.init(this)

val baseInfoConfig = BaseInfoConfig(isDebug = BuildConfig.DEBUG)
val lynxConfig = SparklingLynxConfig.build(this) {
  // 可选：添加全局 Lynx 行为/模块、模板提供者等
}
val hybridConfig = SparklingHybridConfig.build(baseInfoConfig) {
  setLynxConfig(lynxConfig)
}

HybridKit.setHybridConfig(hybridConfig, this)
HybridKit.initLynxKit()
```

## Sparkling

创建容器的入口。使用指南请参阅[容器](../guide/containers.md)。

| 方法 | 说明 |
|------|------|
| `Sparkling.build(context, sparklingContext)` | 通过 Android `Context` 和 `SparklingContext` 创建 `Sparkling` 实例。 |
| `navigate()` | 启动 `SparklingActivity`（全页容器）。成功返回 `true`。 |
| `createView(withoutPrepare)` | 创建 `SparklingView`（嵌入式容器）。失败返回 `null`。 |

## SparklingView

嵌入式容器 — 承载 Lynx 内容的 `FrameLayout`。使用指南请参阅[容器 — 嵌入式容器](../guide/containers.md#嵌入式容器)。

| 方法 / 属性 | 说明 |
|-------------|------|
| `prepare(sparklingContext)` | 使用 scheme 参数初始化视图，创建 kit view 并设置加载/错误 UI。`createView()` 会自动调用，除非 `withoutPrepare = true`。 |
| `loadUrl()` | 触发内容加载。在 `prepare()` 之后、添加到布局之后调用。 |
| `onShowEvent()` | 转发显示事件到 Lynx 运行时。**宿主 Activity/Fragment 必须调用。** |
| `onHideEvent()` | 转发隐藏事件到 Lynx 运行时。**宿主 Activity/Fragment 必须调用。** |
| `release()` | 销毁 Lynx 运行时并清理资源。在视图永久移除时调用。 |
| `sendEventByJSON(eventName, params)` | 向 Lynx 页面发送自定义事件。 |
| `updateGlobalPropsByIncrement(data)` | 运行时更新 globalProps。 |
| `refreshData(context, hybridContext)` | 使用更新的 scheme 参数刷新内容，无需重建视图。 |
| `loadStatus()` | 返回当前加载状态（`INIT`、`LOADING`、`SUCCESS`）。 |
| `isLoadSuccess()` | 内容加载成功时返回 `true`。 |

## SparklingContext

传递给两种容器类型的配置对象。

| 属性 | 说明 |
|------|------|
| `scheme` | 要加载的 `hybrid://...` URL。 |
| `sparklingUIProvider` | 实现 `SparklingUIProvider` 以自定义加载/错误/工具栏视图。 |
| `hybridSchemeParam` | 解析后的 scheme 参数（从 `scheme` 自动填充）。 |
| `containerId` | 唯一的容器标识符（自动生成）。 |

## SparklingUIProvider

自定义容器 UI 的接口。适用于全页和嵌入式容器。

| 方法 | 说明 |
|------|------|
| `getLoadingView(context)` | 返回自定义加载视图，返回 `null` 使用默认。 |
| `getErrorView(context)` | 返回自定义错误视图，返回 `null` 使用默认。 |
| `getToolBar(context)` | 返回 `SparklingActivity` 使用的自定义 `Toolbar`（仅全页容器）。 |
