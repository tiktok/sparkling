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
  // setSharedProcessDensityOverride(2.0f)
  setDefaultThreadStrategy(SparklingThreadStrategy.PART_ON_LAYOUT)
}
val hybridConfig = SparklingHybridConfig.build(baseInfoConfig) {
  setLynxConfig(lynxConfig)
}

HybridKit.setHybridConfig(hybridConfig, this)
HybridKit.initLynxKit()
```

### Lynx 全进程 density

`setSharedProcessDensityOverride(density)` 为宿主进程内 Sparkling 创建的所有 `LynxView`
设置同一个逻辑 density。该值必须是有限数且大于零。未设置时，Sparkling 不覆盖 Lynx
density，由 Lynx 沿用默认的屏幕 density 行为。

Lynx 当前将 density override 视为进程级状态。如果宿主启用该选项，**同一进程内所有非
Sparkling 创建的 `LynxView` 也必须使用完全相同的 density 构造**。不要按容器或 URL
计算或修改该值。

该 API 只提供宿主级统一 density 策略，不代表可以兼容任意 Legacy URL 中的 density
语义。

## Sparkling

创建容器的入口。使用指南请参阅[容器](../guide/containers.md)。

| 方法 | 说明 |
|------|------|
| `Sparkling.build(context, sparklingContext)` | 通过 Android `Context` 和 `SparklingContext` 创建 `Sparkling` 实例。 |
| `navigate()` | 启动 `SparklingActivity`（全页容器）。成功返回 `true`；Lynx 配置不兼容时抛出类型安全异常。 |
| `createView(withoutPrepare)` | 创建 `SparklingView`（嵌入式容器）。其他创建失败返回 `null`；启用 prepare 时遇到不兼容 Lynx 配置会抛出类型安全异常。 |

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
| `screenOrientationPolicy` | 全页 `SparklingActivity` 可选的类型安全方向策略。 |
| `threadStrategy` | 可选的容器级 `SparklingThreadStrategy`，优先于全局默认值。 |
| `hybridSchemeParam` | 解析后的 scheme 参数（从 `scheme` 自动填充）。 |
| `lynxViewport` | 可选的 `SparklingLynxViewport(widthPx, heightPx)`，以物理像素指定固定 viewport。程序化配置会覆盖 scheme 中解析的尺寸。 |
| `containerId` | 唯一的容器标识符（自动生成）。 |

## 屏幕方向

`SparklingScreenOrientationPolicy` 为全页容器提供 Java 友好的 `SYSTEM`、
`PORTRAIT` 和 `LANDSCAPE`：

```java
SparklingContext sparklingContext = new SparklingContext();
sparklingContext.setScreenOrientationPolicy(
    SparklingScreenOrientationPolicy.LANDSCAPE);
```

宿主也可以通过
`SparklingHybridConfig.Builder.setDefaultScreenOrientationPolicy(...)`
设置可选的应用级默认值。解析优先级为：

1. `SparklingContext.screenOrientationPolicy`，包括显式设置的 `SYSTEM`；
2. canonical scheme 的 `screen_orientation`；
3. 全局默认值；
4. 全部未设置时沿用 Android 当前的系统/默认行为。

canonical scheme 中的 `portrait` 和 `landscape` 会映射到对应的类型安全策略。
未知 canonical 值继续保持现有的 `SYSTEM` 行为，不会回退到全局默认值。
`SparklingActivity` 在创建内容前通过 Android 公开的 `requestedOrientation`
API 应用最终策略。

该策略不会旋转承载嵌入式 `SparklingView` 的 Activity。嵌入式 View
不拥有宿主 Activity；需要固定方向时，应由宿主自行应用方向策略。

高级宿主如果已经使用 `LynxKitInitParams`，也可以设置其 `lynxViewport` 属性。优先级依次为：
init params、`SparklingContext.lynxViewport`、canonical scheme 的 `width` 和 `height`。
三种入口都只接受完整的正数宽高组合。

## 线程策略

`SparklingThreadStrategy` 为 Lynx 的 `ALL_ON_UI`、`MOST_ON_TASM`、
`PART_ON_LAYOUT` 和 `MULTI_THREADS` 渲染线程策略提供类型安全的映射。
可以通过 `SparklingLynxConfig.Builder.setDefaultThreadStrategy(...)`
设置可选的全局默认值，也可以通过 `SparklingContext.threadStrategy`
为单个容器覆盖。容器级配置优先；两者都未设置时，Sparkling 不改变
Lynx SDK 的默认策略。

### 固定 viewport 兼容性

不要同时使用最终生效的固定 viewport 和
`SparklingThreadStrategy.MULTI_THREADS`。这个组合可能在 Lynx SDK
native 内部触发崩溃。Sparkling 会在构造 `LynxView` 前拒绝该组合，并抛出
`SparklingLynxConfigurationException`；其 `error` 为
`SparklingLynxConfigurationError.FIXED_VIEWPORT_WITH_MULTI_THREADS`。
`navigate()` 和会执行 prepare 的 `createView(false)` 都会在启动或构造容器前同步失败。

Sparkling 会先解析所有类型安全配置，再执行校验：

1. viewport：`LynxKitInitParams.lynxViewport`、其次
   `SparklingContext.lynxViewport`、最后 canonical scheme 的
   `width`/`height`；
2. 线程策略：`SparklingContext.threadStrategy`、其次
   `SparklingLynxConfig.defaultThreadStrategy`、最后保持 Lynx 默认值不变。

因此，无论先设置 viewport 还是线程策略，校验结果都相同。安全的页面级策略
可以覆盖全局 `MULTI_THREADS` 默认值。未使用固定 viewport 的
`MULTI_THREADS`，以及搭配 `ALL_ON_UI`、`MOST_ON_TASM`、
`PART_ON_LAYOUT` 或未显式设置线程策略的固定 viewport，均保持原有行为。

Java 调用方可以捕获并检查类型安全异常：

```java
try {
  SparklingView view = Sparkling.build(context, sparklingContext).createView(false);
} catch (SparklingLynxConfigurationException exception) {
  if (exception.getError()
      == SparklingLynxConfigurationError.FIXED_VIEWPORT_WITH_MULTI_THREADS) {
    // 改用安全的线程策略，或移除固定 viewport。
  }
}
```

## SparklingUIProvider

自定义容器 UI 的接口。适用于全页和嵌入式容器。

| 方法 | 说明 |
|------|------|
| `getLoadingView(context)` | 返回自定义加载视图，返回 `null` 使用默认。 |
| `getErrorView(context)` | 返回自定义错误视图，返回 `null` 使用默认。 |
| `getToolBar(context)` | 返回 `SparklingActivity` 使用的自定义 `Toolbar`（仅全页容器）。 |

### 失败页重试

如果自定义错误页需要通过 Sparkling SDK 自己的加载链路重试，让
`getErrorView(context)` 返回的 View 实现 `SparklingRetryableErrorView`。
这是可选能力；已有 `SparklingUIProvider` 和普通错误 View 无需修改。

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

每次当前加载失败时，Sparkling 都会注册一个新的、只能成功使用一次的
`SparklingFailedViewRetry`。`retry()` 必须在 Android 主线程调用；只有
该请求仍对应当前失败且被原子接受时才返回 `true`。双击、过期请求、
非主线程调用以及容器释放后的调用都会返回 `false`。接受后 Sparkling
会清除错误 UI，并通过 SDK 自己的 reload 链路重试，不重新打开路由。

如果重试仍失败，容器会重新进入 `FAIL` 并注册新的 retry；如果成功，
容器会进入 `SUCCESS`。在新加载、成功、接受重试或释放等请求失效时，
Sparkling 也会调用 `setSparklingRetry(null)`。实现方必须替换之前的
监听器或请求，并且不能持有传入的 `Context`。全页
`SparklingActivity` 和嵌入式 `SparklingView` 使用相同契约。
