// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// Event constants for SPK Kit lifecycle and system notifications.
/// 
/// This class defines string constants for various events that can occur
/// within the SPK framework, enabling consistent event handling
/// and notification across different components.
@objc public class SPKKitEvent: NSObject {
    /// Event fired when a hybrid view appears on screen.
    /// 
    /// This event is triggered when a SPK hybrid view becomes visible
    /// to the user, typically during view controller lifecycle transitions.
    @objc public static let viewDidAppear = "kHybridKitEventViewDidAppear"
    
    /// Event fired when a hybrid view disappears from screen.
    /// 
    /// This event is triggered when a SPK hybrid view becomes hidden
    /// or is removed from the view hierarchy.
    @objc public static let viewDidDisappear = "kHybridKitEventViewDidDisappear"
    
    /// Event fired when global properties are updated.
    /// 
    /// This event is triggered when system-wide properties (such as device
    /// orientation, accessibility settings, or theme changes) are modified
    /// and need to be propagated to hybrid views.
    @objc public static let globalPropsChanged = "kHybridKitEventGlobalPropsChanged"
    
    /// Event fired when the device orientation changes.
    /// 
    /// This event is triggered when the user interface rotates, allowing
    /// hybrid views to respond to orientation changes and update their layout.
    @objc public static let userInterfaceDidRotate = "kHybridKitEventUserInterfaceDidRotate"
}
