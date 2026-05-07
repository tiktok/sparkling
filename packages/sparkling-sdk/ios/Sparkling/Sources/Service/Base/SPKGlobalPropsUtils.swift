// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// Utility class for generating global properties and device information.
/// 
/// `SPKGlobalPropsUtils` provides static methods to collect and format
/// device-specific information, screen metrics, and system properties that are
/// commonly used throughout the SPKKit framework. This includes screen
/// dimensions, safe area insets, device capabilities, and accessibility settings.
/// 
/// The class is designed to provide consistent device information across
/// different iOS versions and device types, handling compatibility issues
/// and providing fallback values when necessary.
@objcMembers
open class SPKGlobalPropsUtils: NSObject {
    /// Generates a dictionary of default global properties for the current device and environment.
    /// 
    /// This method collects comprehensive device information including screen dimensions,
    /// safe area insets, system version, device capabilities, and accessibility settings.
    /// The returned dictionary is commonly used to provide context information to
    /// hybrid views and JavaScript environments.
    /// 
    /// - Returns: A dictionary containing device and environment properties with the following keys:
    ///   - screenWidth: Screen width in points
    ///   - screenHeight: Screen height in points
    ///   - statusBarHeight: Status bar height in points
    ///   - os: Operating system identifier ("ios")
    ///   - osVersion: iOS version string
    ///   - language: Preferred language code
    ///   - isIPhoneX: 1 if iPhone X series device, 0 otherwise
    ///   - isIPhoneXMax: 1 if iPhone X series device, 0 otherwise
    ///   - safeAreaHeight: Bottom safe area height
    ///   - contentHeight: Available content height excluding safe areas
    ///   - isPad: 1 if iPad device, 0 otherwise
    ///   - topHeight: Top safe area height
    ///   - bottomHeight: Bottom safe area height
    ///   - accessibleMode: Accessibility mode flags
    ///   - isLowPowerMode: 1 if low power mode enabled, 0 otherwise
    ///   - isAppBackground: true if app is in background
    ///   - screenOrientation: Current screen orientation string
    ///   - deviceModel: Device model identifier in lowercase
    static func defaultGlobalProps() -> [String: Any] {
        var statusBarHeight: CGFloat = 0
        if #available (iOS 13.0, *) {
            statusBarHeight = UIApplication.shared.windows.first?.windowScene?.statusBarManager?.statusBarFrame.size.height ?? statusBarHeight
        } else {
            statusBarHeight = UIApplication.shared.statusBarFrame.size.height
        }
        if (statusBarHeight ?? 0) < 0.1 {
            statusBarHeight = UIDevice.spk.isIPhoneXSeries ? 44 : 20
        }
        var bottomHeight: CGFloat = 0
        var topHeight: CGFloat = 0
        
        if #available(iOS 11.0, *) {
            bottomHeight = UIApplication.spk.mainWindow?.spk.safeAreaInsets.bottom ?? bottomHeight
            topHeight = UIApplication.spk.mainWindow?.spk.safeAreaInsets.top ?? topHeight
        }
        
        if topHeight < 0.1 {
            topHeight = statusBarHeight
        }
        
        return [
            "screenWidth": UIScreen.main.bounds.size.width,
            "screenHeight": UIScreen.main.bounds.size.height,
            "statusBarHeight": statusBarHeight,
            "os": "ios",
            "osVersion": UIDevice.current.systemVersion,
            "language": NSLocale.preferredLanguages.first ?? "",
            "isIPhoneX": UIDevice.spk.isIPhoneXSeries ? 1 : 0,
            "isIPhoneXMax": UIDevice.spk.isIPhoneXSeries ? 1 : 0,
            "safeAreaHeight": UIApplication.spk.mainWindow?.spk.safeAreaInsets.top ?? 0,
            "contentHeight": UIScreen.main.bounds.size.height - topHeight - bottomHeight,
            "isPad": (UIDevice.current.userInterfaceIdiom == .pad ? 1 : 0),
            "topHeight": topHeight,
            "bottomHeight": bottomHeight,
            "accessibleMode": accessModeNumber(),
            "isLowPowerMode": ProcessInfo.processInfo.isLowPowerModeEnabled ? 1 : 0,
            "isAppBackground": UIApplication.shared.applicationState == .background,
            "screenOrientation": self.screenOrientationString(),
            "deviceModel": UIDevice.spk.hwModel?.lowercased() ?? ""
        ]
    }
    
    /// Returns the current screen orientation as a string representation.
    /// 
    /// This method converts the current status bar orientation to a human-readable
    /// string format. It handles all possible orientation states and provides
    /// consistent string representations for use in JavaScript environments.
    /// 
    /// - Returns: A string representing the current orientation:
    ///   - "Portrait" for normal portrait orientation
    ///   - "PortraitUpsideDown" for upside-down portrait
    ///   - "LandscapeLeft" for left landscape orientation
    ///   - "LandscapeRight" for right landscape orientation
    ///   - "Unknown" for unknown or unrecognized orientations
    static func screenOrientationString() -> String {
        switch UIApplication.shared.statusBarOrientation {
        case .unknown:
            return "Unknown"
        case .portrait:
            return "Portrait"
        case .portraitUpsideDown:
            return "PortraitUpsideDown"
        case .landscapeLeft:
            return "LandscapeLeft"
        case .landscapeRight:
            return "LandscapeRight"
        }
        return "Unknwon"
    }
    
    /// Returns a bitmask representing the current accessibility mode settings.
    /// 
    /// This method checks various accessibility features and returns a numeric
    /// representation using bit flags. Currently, it checks for VoiceOver status
    /// and can be extended to include other accessibility features.
    /// 
    /// - Returns: An integer bitmask where:
    ///   - Bit 0 (value 1): VoiceOver is running
    ///   - Additional bits can be added for other accessibility features
    static func accessModeNumber() -> Int {
        var result = 0
        if UIAccessibility.isVoiceOverRunning {
            result = result | (1 << 0)
        }
        return result
    }
}
