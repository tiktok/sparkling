// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

@objc public enum SPKLoadState: Int {
    case SPKLoadStateNotLoad
    case SPKLoadStateLoading
    case SPKLoadStateSucceed
    case SPKLoadStateFailed
}

@objc public protocol SPKBaseProtocol {
    /// Unique identifier for the view container.
    var containerID: String {get}
    
    var context: SPKHybridContext? {set get}
    
    /// Current loading state of the view.
    var loadState: SPKLoadState {set get}
    
    /// Loads content into the view based on the current configuration.
    func load()
    
    /// Reloads the view with a new context.
    ///
    /// - Parameter context: The new hybrid context to use for reloading
    func reload(_ context: SPKHybridContext?)
    
    
    /// Sends an event to the runtime.
    ///
    /// - Parameters:
    ///   - event: The event name to send
    ///   - params: Optional parameters to include with the event
    ///   - callback: Optional callback to handle the response
    func send(event event:String, params: [String: Any]?, callback:((Any?)->Void)?)
}
