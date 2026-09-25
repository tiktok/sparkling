# SparklingMethod for iOS

SparklingMethod is the native method runtime used by Sparkling on iOS. It
provides method registration, invocation, models, events, and optional Swift
declaration macros.

## Requirements

- iOS 14 or later
- Swift 6 toolchain for the optional macro product
- Mantle 2.2.0

## Swift Package Manager

The package exposes two products:

- `SparklingMethod`: the Objective-C runtime.
- `SparklingMethodMacros`: the optional `@SPKGlobalMethod` declaration macro.

Applications that do not use the Swift macro only need the runtime product.
Targets that apply `@SPKGlobalMethod` must enable Swift's
`SymbolLinkageMarkers` experimental feature:

```swift
swiftSettings: [
    .unsafeFlags([
        "-enable-experimental-feature",
        "SymbolLinkageMarkers",
        "-D_SYMBOL_LINKAGE_MARKERS_ENABLED",
    ]),
]
```

## CocoaPods

The CocoaPods package exposes `Core` by default. Add the optional `Lynx`
subspec to install the public Lynx module and transport. Swift macro
consumers can use the `Macros` subspec and bundled host compiler plugin.
The target that writes `@SPKGlobalMethod` must load that
plugin and enable `SymbolLinkageMarkers`; see `Sparkling-Router.podspec` for a
CocoaPods consumer example.

```ruby
pod 'SparklingMethod/Core'
pod 'SparklingMethod/Lynx'   # Only for Lynx containers.
pod 'SparklingMethod/Macros' # For @SPKGlobalMethod in Swift targets.
```

The bundled plugin is built from `Sources/SparklingMethodMacroPlugin` with
`Scripts/build_cocoapods_macro.sh`. Rebuild it with the Xcode toolchain used
for CocoaPods consumers when updating the macro implementation. The current
binary targets Apple Silicon macOS, matching the existing SparklingMacro Pod.

The Swift Package Manager product contains Core and the WebKit transport. Lynx
integration is delivered by CocoaPods because it depends on the Lynx framework.

`SPKMethodWebBridge` accepts any WebKit-based hybrid container, including a
`WKWebView` subclass owned by HybridKit. Create it with
`initWithRuntime:configuration:` and attach it with `setupWithContainer:`;
the bridge retains its CallRouter and Runtime. `SPKMethodLynxModule` is the direct
public Lynx entry. Lynx hosts connect its transport to `SPKMethodCallRouter`.
Hosts choose one entry transport per container.

Before publishing a release, compile the exported Objective-C package with:

```shell
pod lib lint SparklingMethod.podspec --allow-warnings --skip-tests
```

## License

SparklingMethod is available under the Apache License 2.0. See `LICENSE` and
`NOTICE` for attribution and source lineage.
