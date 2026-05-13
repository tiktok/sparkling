# sparkling-sdk

[![license](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](../../LICENSE)

Native Android and iOS SDK for the Sparkling Framework, providing the core runtime for Lynx-based hybrid applications.

## Platform Support

| Platform | Minimum Version |
|----------|----------------|
| Android  | API 21+ |
| iOS      | iOS 12+ |

## Installation

### Android

Add the SDK to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.tiktok.sparkling:sparkling:+")  // Use latest version
}
```

### iOS

Add to your `Podfile`:

```ruby
pod 'Sparkling'  # Uses latest version
pod 'SparklingMacro'  # Required for SPKExecuteAllPrepareBootTask and macro registration
```

Then run `pod install`.
