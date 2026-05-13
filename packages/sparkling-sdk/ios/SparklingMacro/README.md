# SparklingMacro

A Swift compiler macro plugin for the [Sparkling](https://github.com/tiktok/sparkling) framework.

## Overview

`SparklingMacro` provides the `#spk_register` freestanding declaration macro, and also exports `SPKExecuteAllPrepareBootTask()` for executing registered boot tasks during app startup. The macro registers a Sparkling service class into the `__DATA, SPK_PRE_SVC` Mach-O section at compile time, and the Sparkling runtime reads this section to discover and instantiate registered services.

```swift
import SparklingMacro

#spk_register(class: "MySparklingService")
```

## Requirements

- iOS 13.0+
- Swift 5.10+
- Xcode 15.0+

## Installation

### CocoaPods

> **Note:** CocoaPods is currently the only supported integration method because the Sparkling SDK has not yet adopted SPM. This pod serves as a short-term compatibility solution. The long-term plan is to migrate the entire Sparkling SDK to Swift Package Manager and replace this approach.

Add `SparklingMacro` to your `Podfile`:

```ruby
pod 'SparklingMacro'
```

Then run:

```bash
pod install
```

The pod ships with a pre-built `SparklingMacrosImpl` compiler plugin binary. CocoaPods automatically configures `OTHER_SWIFT_FLAGS` via `pod_target_xcconfig` to load the plugin during compilation. If you need the macro available in your **app target** as well, add the following to your target's build settings:

```
OTHER_SWIFT_FLAGS = $(inherited) -Xfrontend -load-plugin-executable -Xfrontend $(PODS_ROOT)/SparklingMacro/Release/SparklingMacrosImpl#SparklingMacrosImpl
```

## Usage

Import `SparklingMacro` and call `#spk_register` at file scope to register a service class:

```swift
import SparklingMacro

#spk_register(class: "MySparklingService")
```

The same module also exposes `SPKExecuteAllPrepareBootTask()` for app startup:

```swift
import SparklingMacro

SPKServiceRegister.registerAll()
SPKExecuteAllPrepareBootTask()
```

This expands at compile time to a static constant placed in the `__DATA, SPK_PRE_SVC` Mach-O section, making the class name discoverable by the Sparkling runtime without any runtime registration overhead.

## License

Apache 2.0. See [LICENSE](../../../LICENSE) for details.
