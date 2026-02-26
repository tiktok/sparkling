# Sparkling CLI

Sparkling CLI（`sparkling-app-cli`）是内置的命令行工具，驱动整个开发工作流。它负责构建 Lynx bundle、自动链接原生方法模块、在 Android/iOS 上运行应用以及诊断开发环境。

## 安装

使用 `create-sparkling-app` 创建项目时会自动包含 CLI。你也可以手动安装：

```bash
npm install sparkling-app-cli
```

安装后，通过 `npx sparkling` 或项目 `package.json` 中定义的 npm scripts 来运行命令。

## 命令

### `sparkling build`

使用项目的 `app.config.ts` 构建 Lynx bundle。

```bash
npx sparkling build
```

| 选项 | 说明 |
| --- | --- |
| `--config <path>` | `app.config.ts` 路径（默认：`app.config.ts`） |
| `--copy` | 将构建产物复制到 Android 和 iOS 原生 Shell |
| `--skip-copy` | 跳过复制（默认行为） |

默认跳过资源复制以加快开发迭代速度。需要将 bundle 放入原生项目时（如发布构建），请使用 `--copy`。

### `sparkling dev`

启动 Rspeedy 开发服务器，支持热重载开发。无需手动重新构建和复制 bundle，开发服务器通过 HTTP 提供 bundle，修改即时生效。

```bash
npx sparkling dev
```

| 选项 | 说明 |
| --- | --- |
| `--config <path>` | `app.config.ts` 路径（默认：`app.config.ts`） |
| `--port <number>` | 开发服务器端口（默认：`5969`） |

默认端口 **5969** 对应手机九宫格键盘上的 **LYNX**（L=5, Y=9, N=6, X=9）。

服务器启动后，将应用指向 `http://<你的IP>:5969/main.lynx.bundle`（或你需要的入口）。项目模板中，**DEBUG** 构建会自动连接开发服务器。

### `sparkling copy-assets`

将编译好的 bundle 复制到 Android 和 iOS 资源目录。

```bash
npx sparkling copy-assets
```

| 选项 | 说明 |
| --- | --- |
| `--source <path>` | 编译产物路径（默认：`dist`） |
| `--android-dest <path>` | Android 资源目标路径（默认：`android/app/src/main/assets`） |
| `--ios-dest <path>` | iOS 资源目标路径（默认：`ios/SparklingGo/SparklingGo/Resources/Assets`） |

### `sparkling autolink`

自动发现并链接 Sparkling 方法模块。CLI 会扫描工作区和 `node_modules` 中的 `module.config.json` 文件，然后更新 Gradle/Podfile 配置并生成注册文件。

```bash
npx sparkling autolink
```

| 选项 | 说明 |
| --- | --- |
| `--platform <platform>` | 目标平台：`android`、`ios` 或 `all`（默认：`all`） |

**执行内容：**

- **Android** — 更新 `settings.gradle(.kts)` 和 `app/build.gradle(.kts)` 的模块引入/依赖，并生成 `SparklingAutolink.kt`。
- **iOS** — 更新 `Podfile` 的 pod 条目，并生成 `SparklingAutolink.swift`。

### `sparkling run:android`

一键完成构建、自动链接和启动 Android 调试版本。

```bash
npx sparkling run:android
```

| 选项 | 说明 |
| --- | --- |
| `--copy` | 将资源复制到原生 Shell |
| `--skip-copy` | 跳过资源复制（默认行为） |

该命令会依次执行：

1. 为 Android 自动链接方法模块
2. 构建 Lynx bundle
3. 运行 `gradlew assembleDebug`
4. 将 APK 安装到已连接的设备/模拟器
5. 启动主 Activity

### `sparkling run:ios`

一键完成构建、自动链接和启动 iOS 模拟器版本。

```bash
npx sparkling run:ios
```

| 选项 | 说明 |
| --- | --- |
| `--copy` | 将资源复制到原生 Shell |
| `--skip-copy` | 跳过资源复制（默认行为） |
| `--device <nameOrId>` | 模拟器名称或 UDID |
| `--skip-pod-install` | 跳过 `pod install` |

该命令会依次执行：

1. 选择模拟器（优先选择已启动的设备，否则回退到 iPhone 17 Pro 等常用型号）
2. 为 iOS 自动链接方法模块
3. 运行 `pod install`（除非指定 `--skip-pod-install`）
4. 构建 Lynx bundle
5. 构建、安装并在模拟器上启动应用

你也可以通过设置 `SPARKLING_IOS_SIMULATOR` 环境变量来指定默认模拟器。

### `sparkling doctor`

检查开发环境是否正确配置。

```bash
npx sparkling doctor
```

| 选项 | 说明 |
| --- | --- |
| `--platform <platform>` | 检查平台：`android`、`ios` 或 `all`（默认：`all`） |

doctor 命令检查的内容：

| 检查项 | 要求 |
| --- | --- |
| Node.js | 版本 ^22 或 ^24 |
| JDK | 版本 >= 11（Android） |
| Android SDK | 已设置 `ANDROID_HOME`，已安装 `android-34` |
| adb | 在 PATH 中可用 |
| Ruby | 版本 >= 2.7, < 3.4 |
| Xcode | 版本 >= 16（仅 macOS） |
| CocoaPods | 已安装（仅 macOS） |
| iOS 模拟器 | 至少有一个可用（仅 macOS） |

如果检查未通过，输出会包含修复提示。

## 全局选项

所有命令都支持以下标志：

| 选项 | 说明 |
| --- | --- |
| `-v, --verbose` | 启用详细日志输出，用于调试 |

你也可以设置 `SPARKLING_VERBOSE` 环境变量达到同样的效果。

## 典型工作流

```bash
# 1. 创建项目
npm create sparkling-app@latest my-app
cd my-app

# 2. 检查环境
npx sparkling doctor

# 3. 启动开发服务器，支持热重载
npx sparkling dev

# 4. 在 Android 上运行
npx sparkling run:android

# 5. 在 iOS 上运行
npx sparkling run:ios

# 6. 构建发布用 bundle
npx sparkling build --copy
```
