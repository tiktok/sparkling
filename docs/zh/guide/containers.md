# 容器

Sparkling 内容始终运行在**原生容器**中 — 一个承载 Lynx 运行时的平台视图。SDK 提供两种容器模式以适配不同场景。

## 两种容器模式

| | 全页容器 | 嵌入式容器 |
|---|---|---|
| **是什么** | 独立的页面（`UIViewController` / `Activity`），自带导航栏、状态栏和生命周期管理 | 普通视图（`UIView` / `FrameLayout`），由你放入现有原生布局中 |
| **典型场景** | 标准页面导航 | 卡片、Banner、局部面板，或任何混合原生与 Lynx UI 的页面 |
| **iOS 类名** | `SPKViewController`（通过 `SPKRouter`） | `SPKContainerView` |
| **Android 类名** | `SparklingActivity`（通过 `Sparkling.navigate()`） | `SparklingView`（通过 `Sparkling.createView()`） |

## 全页容器

全页容器是展示 Sparkling 内容的默认方式。原生层管理导航栏、状态栏、屏幕方向和加载/错误视图 — 通过 [Scheme 参数](./scheme.md)配置。

### iOS

```swift
// 方式 A：push 到已有的导航栈
SPKRouter.open(withURL: "hybrid://lynxview_page?bundle=detail.lynx.bundle&title=Detail", context: nil)

// 方式 B：自行创建和管理 VC
let vc = SPKRouter.create(withURL: url, context: SPKContext())
navigationController.pushViewController(vc, animated: true)
```

### Android

```kotlin
val ctx = SparklingContext().apply {
  scheme = "hybrid://lynxview_page?bundle=detail.lynx.bundle&title=Detail"
}
Sparkling.build(this, ctx).navigate()   // 启动 SparklingActivity
```

### 通过 Scheme 配置

全页容器通过 URL 查询参数配置。常用参数：

| 参数 | 效果 |
|------|------|
| `title` | 导航栏标题 |
| `hide_nav_bar=1` | 隐藏导航栏 |
| `hide_status_bar=1` | 隐藏状态栏 |
| `trans_status_bar=1` | 透明状态栏（内容延伸到状态栏下方） |
| `show_nav_bar_in_trans_status_bar=1` | 透明状态栏模式下仍显示导航栏 |
| `nav_bar_color` | 导航栏背景色 |
| `container_bg_color` | 容器背景色 |

完整参数列表请参阅 [Scheme](./scheme.md)。

## 嵌入式容器

嵌入式容器让你将 Sparkling 内容放置在原生布局的任意位置。你控制 frame，容器在其中渲染 Lynx 内容。

### iOS — SPKContainerView

```swift
// 1. 创建并添加到视图层级
let spkView = SPKContainerView(frame: containerView.bounds)
spkView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
containerView.addSubview(spkView)

// 2. 加载 bundle
let context = SPKContext()
spkView.load(withURL: "hybrid://lynxview?bundle=card.lynx.bundle", context)
```

#### 内容尺寸（iOS）

默认情况下，容器保持其指定的 frame。设置 `sparkContentMode` 可让容器根据 Lynx 内容自动调整大小：

| 模式 | 行为 |
|------|------|
| `FixedSize`（默认） | 容器保持 frame；Lynx 内容填充其中。 |
| `FixedWidth` | 宽度固定；高度随内容自适应。 |
| `FixedHeight` | 高度固定；宽度随内容自适应。 |
| `FitSize` | 宽高均随内容自适应。 |

加载完成后，可读取 `preferredLayoutSize` 获取内容的固有尺寸。

### Android — SparklingView

```kotlin
// 1. 通过 Sparkling builder 创建
val ctx = SparklingContext().apply {
  scheme = "hybrid://lynxview?bundle=card.lynx.bundle"
}
val spkView = Sparkling.build(this, ctx).createView()

// 2. 添加到布局并加载
container.addView(spkView)
spkView?.loadUrl()
```

## 生命周期

### 全页容器

生命周期自动管理。`SPKViewController` / `SparklingActivity` 会将 `viewDidAppear` / `onResume` 和 `viewDidDisappear` / `onPause` 转发给 Lynx 运行时。无需额外操作。

### 嵌入式容器

由于 SDK 无法控制你的宿主 ViewController 或 Fragment，**你必须手动转发可见性事件**。如果忽略这一步，Lynx 运行时将无法正确暂停定时器、动画或网络请求。

**iOS：**

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

**Android：**

```kotlin
// Fragment.onResume 或视图变为可见时
spkView?.onShowEvent()

// Fragment.onPause 或视图变为不可见时
spkView?.onHideEvent()

// 永久销毁视图时
spkView?.release()
```

### 生命周期回调（iOS）

在 `SPKContext` 上设置 `containerLifecycleDelegate` 以监听加载状态。两种容器模式均适用：

```swift
context.containerLifecycleDelegate = self

// 主要回调：
func container(_ container: SPKContainerProtocol,
               didFinishLoadWithURL url: URL?) { /* 加载成功 */ }
func container(_ container: SPKContainerProtocol,
               didLoadFailedWithURL url: URL?, error: Error?) { /* 加载失败 */ }
func containerDidFirstScreen(_ container: SPKContainerProtocol) { /* 首屏渲染 */ }
```

## 与 Lynx 通信

两种容器模式都支持在运行时发送事件和更新 globalProps。

### 发送事件

**iOS（两种模式）：**
```swift
spkView.send(event: "refresh", params: ["key": "value"], callback: nil)
```

**Android（嵌入式）：**
```kotlin
spkView?.sendEventByJSON("refresh", JSONObject().put("key", "value"))
```

### 更新 globalProps

**iOS（两种模式）：**
```swift
spkView.update(withGlobalProps: ["theme": "dark"])
```

**Android（嵌入式）：**
```kotlin
spkView?.updateGlobalPropsByIncrement(mapOf("theme" to "dark"))
```

## 下一步

- [Scheme](./scheme.md) — 配置容器的完整 URL 参数列表
- [多页面导航](./multi-page-navigation.md) — 页面间跳转、传递数据、关闭页面
- [Sparkling SDK — iOS](../apis/sparkling-sdk-ios.md) — iOS API 参考
- [Sparkling SDK — Android](../apis/sparkling-sdk-android.md) — Android API 参考
