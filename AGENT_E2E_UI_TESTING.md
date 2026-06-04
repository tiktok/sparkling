# Agent E2E UI 自动化测试

本指南用于引导 Agent 在提交代码前执行 Sparkling 的端到端 UI 回归。只要本次改动可能影响 CLI、模板、Android/iOS shell、Sparkling SDK、Sparkling Method、playground 或构建链路，提交前都必须跑完整流程，并在提交说明或 PR 测试记录里写明结果。

## 目标

E2E 流程必须覆盖以下能力：

- 当前仓库的 Android/iOS playground 可以编译、安装、启动。
- playground 的主要页面与内置 method 功能可以正常交互。
- 使用当前仓库的 `create-sparkling-app` CLI 和本地模板创建的新项目可以编译、安装、启动。
- 新项目安装额外 Sparkling Method 包，例如 `sparkling-storage`，可以正常 autolink、重新编译运行，并通过 UI 验证 method 功能。

## 触发时机

每次完成代码修改并准备提交前执行。不要等到 commit 后再补测。

Agent 在以下情况下不得提交：

- 任一命令失败且未定位为环境问题。
- App 启动后白屏、崩溃、卡在 splash、无法加载 Lynx bundle。
- UI 自动化无法完成关键交互。
- `sparkling-storage` 等新增 method 的 autolink 文件未更新，或功能调用失败。
- Android/iOS 只测了一端，且没有在报告中说明另一端的明确阻塞原因。

## 前置条件

从仓库根目录开始：

```bash
ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

node -v
pnpm -v
pnpm install
```

移动端环境至少满足：

- Android：JDK、Android SDK、`adb`、可用 emulator 或已连接设备；`ANDROID_HOME` 和 `ANDROID_SDK_ROOT` 必须指向同一个有效 SDK 目录。
- iOS：macOS、Xcode、CocoaPods、可用 iOS Simulator。
- Node.js：22 或 24。
- pnpm：10.x，建议与 lockfile 中记录的版本一致。
- Ruby：3.2.6 或更高，供 CocoaPods 使用。
- 格式工具：`swift-format`、`clang-format`、`ktlint`。

提交前先确认必要工具可解析：

```bash
node -v
pnpm -v
ruby -v
java -version
adb version
test -d "${ANDROID_HOME:-}" && test -d "${ANDROID_SDK_ROOT:-}"
xcrun simctl list devices available

command -v clang-format
command -v ktlint
command -v swift-format || xcrun --find swift-format
```

macOS 上缺少格式工具时优先安装：

```bash
brew install clang-format ktlint
```

`swift-format` 可以来自 Homebrew，也可以来自 Xcode toolchain。`scripts/lint.sh` 会在 `PATH` 中找不到 `swift-format` 时自动使用 `xcrun --find swift-format`。

在系统临时目录生成的项目不会继承仓库内的 `android/local.properties`。运行生成项目 Android E2E 前必须导出：

```bash
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
```

如果当前 shell 没有设置这些变量，Agent 也可以在临时生成项目内写入 `android/local.properties`，内容为 `sdk.dir=$ANDROID_HOME`。

先运行环境诊断：

```bash
cd "$ROOT/packages/playground"
node "$ROOT/packages/sparkling-app-cli/bin.js" doctor --platform all
cd "$ROOT"
```

如果 `doctor` 失败，Agent 必须先修复环境或在最终报告中标记为阻塞，不能把后续失败归为业务回归。

## 记录目录

为每次执行创建独立产物目录，保存截图、日志、失败现场和最终报告。该目录只用于本地排查，不要提交。

```bash
ARTIFACTS="$ROOT/.e2e-artifacts/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$ARTIFACTS"
```

最终报告至少包含：

- 执行日期、commit hash、当前分支。
- Android/iOS 设备或模拟器型号。
- 每个阶段的命令和结果。
- 关键 UI 截图路径。
- 失败项、日志路径、是否判断为环境阻塞。

## 1. 基础构建与单测

先验证工作区基础能力，避免把 TypeScript、Jest 或代码生成问题带到移动端阶段。

```bash
pnpm --filter sparkling-app-cli build
pnpm --filter create-sparkling-app build
pnpm -r test
pnpm -r build
```

如果改动包含原生代码，再运行格式检查：

```bash
scripts/lint.sh
```

失败时先修复，再进入 playground 和脚手架项目测试。

## 2. Playground Android/iOS E2E

playground 是当前仓库功能的主回归入口，必须使用本仓库 workspace 版本，不要使用全局安装的 CLI。

```bash
pnpm --filter sparkling-playground build
```

### Android

确认设备：

```bash
adb devices
```

如没有已启动设备，选择一个本机 AVD 启动：

```bash
emulator -list-avds
emulator -avd <avd-name> -netdelay none -netspeed full &
adb wait-for-device
```

编译、安装并启动 playground：

```bash
cd "$ROOT/packages/playground"
node "$ROOT/packages/sparkling-app-cli/bin.js" run:android
cd "$ROOT"
```

采集现场：

```bash
adb shell pidof com.tiktok.sparkling.playground
adb exec-out screencap -p > "$ARTIFACTS/playground-android-home.png"
adb logcat -d > "$ARTIFACTS/playground-android-logcat.txt"
adb shell uiautomator dump /sdcard/window.xml
adb pull /sdcard/window.xml "$ARTIFACTS/playground-android-window.xml"
```

### iOS

确认可用模拟器：

```bash
xcrun simctl list devices available
```

如需指定模拟器：

```bash
export SPARKLING_IOS_SIMULATOR="<simulator name or UDID>"
```

编译、安装并启动 playground：

```bash
cd "$ROOT/packages/playground"
node "$ROOT/packages/sparkling-app-cli/bin.js" run:ios --copy
cd "$ROOT"
```

采集现场：

```bash
xcrun simctl get_app_container booted com.sparkling.playground
xcrun simctl io booted screenshot "$ARTIFACTS/playground-ios-home.png"
xcrun simctl spawn booted log collect --last 5m --output "$ARTIFACTS/playground-ios.logarchive"
```

### UI 自动化检查点

Agent 必须优先使用可用的移动端自动化能力完成点击、输入和断言，例如 Appium、Maestro、XCUITest、uiautomator、`adb shell input`、`xcrun simctl` 或当前运行环境提供的视觉点击工具。仅截图观察不能替代关键交互；如果某个平台只能截图，必须在报告中标记为“未完成自动化交互”。

playground 至少覆盖：

| 区域 | 操作 | 通过标准 |
| --- | --- | --- |
| 首页 | 启动后等待首页稳定 | 可见 `Sparkling Go`、`Open Page`、`Scheme & Navigation`、`Storage`、`Media` |
| Open Page | 输入或保留 `gp-screen.lynx.bundle`，点击 `Go` | 成功打开 Screen / Safe Area 页面，无白屏或崩溃 |
| 导航 | 打开 `Passing Data` 或 `Navigation Stack`，再返回 | 页面 push/pop 正常，返回后首页仍可交互 |
| GlobalProps | 打开 `Device & OS Info` 和 `Screen & Safe Area` | 平台、屏幕、安全区信息有实际值，不是全量 `N/A` |
| Storage | 打开 `setItem / getItem`，点击 `Save` 后点击 `Read` | `setItem Response` 和 `getItem Response` 出现成功 code，`getItem` 返回 `sparkling` |
| Media | 打开 choose/upload/download 页面 | 页面渲染和基础按钮可见；涉及相册、相机、网络权限时记录权限弹窗处理结果 |
| Settings | 切换 `Settings`，切换 Light/Dark/Auto | 主题切换生效，Dev Server 状态和 System Info 可见 |

Android 失败排查优先看：

```bash
adb logcat -d | rg -i "fatal|crash|exception|Sparkling|Lynx|autolink|storage"
```

iOS 失败排查优先看：

```bash
xcrun simctl spawn booted log show --last 10m --style compact | rg -i "fatal|crash|exception|Sparkling|Lynx|autolink|storage"
```

## 3. `create-sparkling-app` 生成项目 E2E

该阶段验证脚手架输出的真实项目是否可编译、安装、启动。必须使用当前仓库的 CLI 和本地模板：

```bash
APP_WORKDIR="$(mktemp -d "${TMPDIR:-/tmp}/sparkling-app-e2e.XXXXXX")"

node "$ROOT/packages/create-sparkling-app/bin/index.js" "$APP_WORKDIR/agent-app" \
  --template "$ROOT/template/sparkling-app-template" \
  -y

cd "$APP_WORKDIR/agent-app"
```

`template/sparkling-app-template` 会被 `create-sparkling-app` 原样复制到用户项目中，因此模板目录本身必须保持发布态，不允许包含任何只在本仓库成立的本地依赖。提交前必须确认模板内没有 `SPARKLING_REPO_ROOT`、`workspace:*`、`file:`、`../../../packages`、机器绝对路径，或指向本仓库 package 的 Android/iOS/TS 依赖。

为了让生成项目验证当前仓库的 JS/TS package 改动，而不是 npm registry 中的旧版本，Agent 只允许在临时生成项目中把相关 npm 依赖替换为当前仓库打出的 tarball。不要在 `template/sparkling-app-template` 中加入本地 override；也不要在 standalone 生成项目里直接写 `file:$ROOT/packages/...`，因为部分 package 含有 `workspace:*` 依赖，离开本仓库 workspace 后会导致 `pnpm install` 失败。

生成项目的 Android/iOS method 依赖应由临时项目自己的 `node_modules` 通过 `sparkling-app-cli autolink` 写入。若某次改动需要验证尚未发布的 native SDK/API，应只 patch 临时生成项目或使用本地 Maven/CocoaPods repo 完成验证，不能把这些本地依赖写回模板。

先在本仓库打包需要覆盖的 package：

```bash
PACK_DIR="$ARTIFACTS/local-packs"
mkdir -p "$PACK_DIR"

pack_pkg() {
  (cd "$ROOT/$1" && pnpm pack --pack-destination "$PACK_DIR")
}

pack_pkg packages/sparkling-method
pack_pkg packages/sparkling-debug-tool
pack_pkg packages/methods/sparkling-navigation
pack_pkg packages/sparkling-app-cli
pack_pkg packages/sparkling-types
```

再更新生成项目的 `package.json`：

```bash
cd "$APP_WORKDIR/agent-app"

PACK_DIR="$PACK_DIR" node <<'NODE'
const fs = require('node:fs');
const path = require('node:path');
const packDir = process.env.PACK_DIR;
const pkg = JSON.parse(fs.readFileSync('package.json', 'utf8'));

function packed(name) {
  const files = fs.readdirSync(packDir).filter((file) => file.startsWith(`${name}-`) && file.endsWith('.tgz'));
  if (files.length !== 1) {
    throw new Error(`Expected one tarball for ${name}, found ${files.length}`);
  }
  return `file:${path.join(packDir, files[0])}`;
}

pkg.dependencies = pkg.dependencies || {};
pkg.devDependencies = pkg.devDependencies || {};

pkg.dependencies['sparkling-debug-tool'] = packed('sparkling-debug-tool');
pkg.dependencies['sparkling-method'] = packed('sparkling-method');
pkg.dependencies['sparkling-navigation'] = packed('sparkling-navigation');
pkg.devDependencies['sparkling-app-cli'] = packed('sparkling-app-cli');
pkg.devDependencies['sparkling-types'] = packed('sparkling-types');

fs.writeFileSync('package.json', `${JSON.stringify(pkg, null, 2)}\n`);
NODE

pnpm install
```

构建 Lynx bundle：

```bash
pnpm run build
```

Android：

```bash
pnpm run run:android
adb exec-out screencap -p > "$ARTIFACTS/generated-android-home.png"
```

iOS：

```bash
pnpm run run:ios
xcrun simctl io booted screenshot "$ARTIFACTS/generated-ios-home.png"
```

生成项目至少覆盖：

| 操作 | 通过标准 |
| --- | --- |
| 首页加载 | 可见 `Sparkling Starter`、`Multi page demo`、`Open second page` |
| 路由跳转 | 点击 `Open second page` | 可见 `This is the second page` |
| 路由关闭 | 点击 `Close` | 回到首页，无崩溃 |

## 4. 生成项目新增 Sparkling Method 并验证 autolink

继续使用步骤 3 的生成项目，安装 `sparkling-storage`。优先使用本仓库打出的 tarball，确保测试覆盖当前仓库改动：

```bash
pack_pkg packages/methods/sparkling-storage

cd "$APP_WORKDIR/agent-app"

PACK_DIR="$PACK_DIR" node <<'NODE'
const fs = require('node:fs');
const path = require('node:path');
const packDir = process.env.PACK_DIR;
const pkg = JSON.parse(fs.readFileSync('package.json', 'utf8'));
const files = fs.readdirSync(packDir).filter((file) => file.startsWith('sparkling-storage-') && file.endsWith('.tgz'));
if (files.length !== 1) {
  throw new Error(`Expected one tarball for sparkling-storage, found ${files.length}`);
}
pkg.dependencies = pkg.dependencies || {};
pkg.dependencies['sparkling-storage'] = `file:${path.join(packDir, files[0])}`;
fs.writeFileSync('package.json', `${JSON.stringify(pkg, null, 2)}\n`);
NODE

pnpm install
pnpm run autolink
```

检查 autolink 结果：

```bash
rg -n "sparkling-storage|Sparkling-Storage|Storage" \
  android/settings.gradle.kts \
  android/app/build.gradle.kts \
  android/app/src/main/java \
  ios/Podfile \
  ios/SparklingGo/SparklingGo
```

通过标准：

- Android `settings.gradle.kts` 包含 `sparkling-storage` include。
- Android `app/build.gradle.kts` 包含 `project(":sparkling-storage")` 依赖。
- Android `SparklingAutolink.kt` 注册 storage method。
- iOS `Podfile` 包含 `Sparkling-Storage` 或 `sparkling-storage` 对应 pod。
- iOS `SparklingAutolink.swift` 注册 storage method。

然后在生成项目中加入临时 UI smoke，用于真实调用 storage method。Agent 可以修改临时项目的 `src/pages/main/App.tsx`，增加：

```ts
import { setItem, getItem } from 'sparkling-storage'
```

并在首页增加一个按钮，点击后执行：

```ts
setItem({ key: 'agent-e2e', data: 'sparkling-storage' }, () => {
  getItem({ key: 'agent-e2e' }, (res) => {
    setLastResult(JSON.stringify(res))
  })
})
```

Sparkling Method JS SDK 的成功码是 `1`。UI 上必须展示 `agent-e2e` 的读取结果，例如 `Storage OK: sparkling-storage`。这是临时生成项目内的改动，不提交到 Sparkling 仓库。

重新构建并运行：

```bash
pnpm run build
pnpm run run:android
pnpm run run:ios
```

新增 method 的 UI 通过标准：

| 平台 | 操作 | 通过标准 |
| --- | --- | --- |
| Android | 点击 storage smoke 按钮 | 页面展示 `Storage OK: sparkling-storage`，logcat 无 method not found / autolink 异常 |
| iOS | 点击 storage smoke 按钮 | 页面展示 `Storage OK: sparkling-storage`，系统日志无 method not found / pod 注册异常 |

## 5. 提交前报告模板

提交前把下面模板填入 PR、commit `TEST:` 区域或本地验证记录：

```text
Agent E2E UI:
- Workspace: pnpm -r test / pnpm -r build / scripts/lint.sh: PASS|FAIL|SKIPPED
- Playground Android: PASS|FAIL|BLOCKED, device=<name>, screenshot=<path>
- Playground iOS: PASS|FAIL|BLOCKED, simulator=<name>, screenshot=<path>
- create-sparkling-app Android: PASS|FAIL|BLOCKED, screenshot=<path>
- create-sparkling-app iOS: PASS|FAIL|BLOCKED, screenshot=<path>
- Generated app + sparkling-storage autolink: PASS|FAIL|BLOCKED
- Storage method UI smoke: PASS|FAIL|BLOCKED
- Logs/artifacts: <ARTIFACTS path>
- Known gaps: <none or explicit reason>
```

只有当所有非阻塞项都为 PASS，并且阻塞项有清晰环境原因时，Agent 才能继续提交。
