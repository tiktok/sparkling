// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// Defines event constants and keys used throughout the SPKKit framework.
/// 
/// `SPKEvent` provides a centralized collection of event names, action types,
/// and parameter keys used for inter-component communication within the framework.
/// This includes container events, navigation events, theme changes, and common
/// parameter keys used across different event types.
/// 
/// The enum is organized into logical groups using nested enums to provide
/// clear categorization and avoid naming conflicts.
public enum SPKEvent {
    static let containerTaskDidTap = "containerMaskTapped"
    enum Back {
        static let pageBack = "sparkPageBackEvent"
        static let finishBack = "pageFinishBackEvent"
        static let actionFromKey = "actionFrom"
        static let actionTypeNavBarBackPress = "navBarBackPress"
        static let actionTypeSwipe = "swipe"
    }

    // MARK: - Common Keys
    
    /// Contains commonly used parameter keys across different event types.
    /// 
    /// This enum provides standardized key names for parameters that are
    /// frequently used across multiple event types within the framework.
    enum Common {
        static let containerIdKey = "containerId"
    }

    // MARK: - Theme Event
    
    /// Contains event names and parameter keys related to theme changes.
    enum Theme {
        static let changed = "onThemeChanged"
        static let valueKey = "theme"
        static let valueLight = "light"
        static let valueDark = "dark"
    }
}
