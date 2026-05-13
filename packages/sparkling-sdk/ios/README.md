# Sparkling iOS SDK

iOS framework for hosting Lynx-driven Sparkling content.

## Install (CocoaPods)
Add the local pod from the monorepo, then install:
```ruby
pod 'Sparkling', :path => '../packages/sparkling-sdk/ios/Sparkling'
pod 'SparklingMacro', :path => '../packages/sparkling-sdk/ios/SparklingMacro'
```
```bash
pod install
```

## Minimal setup
```swift
import Sparkling

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        SPKKit.shared.initialize()
        return true
    }
}
```

```swift
// Register and open a Lynx bundle
SPKScheme.resolver(
    withScheme: "hybrid://lynxview?bundle=.%2Fmain.lynx.bundle",
    context: nil,
    paramClass: SPKSchemeParam.self
)

let result = SPKRouter.open(
    withURL: "hybrid://lynxview?bundle=.%2Fmain.lynx.bundle",
    context: nil
)
```

## Bridging Sparkling methods
- Add any generated method podspecs (for example, `Sparkling-Router`, `Sparkling-Storage`,
  or your custom module) beside the Sparkling pod entry before running `pod install`.
- Ensure the bundled Lynx assets built by your web app are copied into the app target so routes above
  can resolve.

## Reference implementation
The iOS project under `template/sparkling-app-template/ios` shows the expected Podfile entries and
application bootstrap for running Sparkling inside a SwiftUI host.
        NSLayoutConstraint.activate([
            sparklingView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            sparklingView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            sparklingView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            sparklingView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
        
        // Load content
        sparklingView.load(withURL: "hybrid://lynxview?bundle=.%2Fmain.lynx.bundle", nil, nil)
    }
    
    // MARK: - SPKWrapperViewLifecycleProtocol
    
    func viewDidStartLoading(_ view: SPKWrapperViewProtocol?) {
        print("Started loading")
    }
    
    func view(_ view: SPKWrapperViewProtocol?, didFinishLoadWithURL url: URL?) {
        print("Finished loading: \(url?.absoluteString ?? "unknown")")
    }
    
    func view(_ view: SPKWrapperViewProtocol?, didLoadFailedWithURL url: URL?, error: Error?) {
        print("Failed to load: \(error?.localizedDescription ?? "unknown error")")
    }
}
```

#### Device Information

```swift
// Get device information
let deviceInfo = UIDevice.spk
let isIPhoneX = deviceInfo.isIPhoneXSeries
let hwModel = deviceInfo.hwModel

// Get screen size from UIScreen
let screenSize = UIScreen.main.bounds.size

// Get safe area insets from main window
let safeAreaInsets = UIApplication.shared.windows.first?.safeAreaInsets ?? UIEdgeInsets.zero

// Global properties
let globalProps = SPKGlobalPropsUtils.defaultGlobalProps()
print("Screen width: \(globalProps["screenWidth"] ?? 0)")
print("Device model: \(globalProps["deviceModel"] ?? "Unknown")")
```

## Requirements

- iOS 12.0+
- Xcode 12.0+
- Swift 5.0+
