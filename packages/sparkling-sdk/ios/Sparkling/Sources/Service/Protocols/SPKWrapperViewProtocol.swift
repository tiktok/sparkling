// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import UIKit

import SparklingMethod

/// Protocol for services that can create and register SPK views.
///
/// This protocol defines the interface for factory services that are responsible
/// for creating view instances that conform to SPKWrapperViewProtocol. It's typically
/// implemented by service classes that manage specific types of hybrid views.
@objc public protocol SPKViewRegisterService {
    /// Creates a new kit view with the specified parameters and context.
    ///
    /// - Parameters:
    ///   - params: The scheme parameters for view configuration
    ///   - context: The hybrid context containing state and configuration
    ///   - frame: The frame rectangle for the view
    /// - Returns: A UIView that conforms to SPKWrapperViewProtocol, or nil if creation fails
    func createKitView(withParams params: SPKHybridSchemeParam?, context: SPKHybridContext?, frame: CGRect) -> (UIView & SPKWrapperViewProtocol)?
}

/// Core protocol defining the interface for all SPK hybrid views.
///
/// This protocol extends SPKBaseProtocol to provide a comprehensive interface
/// for hybrid views that can load content, manage lifecycle events, handle communication
/// with runtimes, and provide progress tracking capabilities.
///
/// This protocol is for raw view. (Lynx/ webview)
@objc public protocol SPKWrapperViewProtocol: SPKBaseProtocol {
    
    /// The underlying raw UIView instance, if available.
    var rawView: UIView? {get}
    
    /// Configuration parameters for the hybrid view.
    var params: SPKHybridParams? {get}
    
    /// Delegate for handling view lifecycle events.
    weak var lifeCycleDelegate: SPKWrapperViewLifecycleProtocol? { set get }
    
    /// Method pipe instance for communication with JavaScript runtime.
    var anyMethodPipe: Any? { get }
    
    /// Progress indicator for loading operations (0.0 to 1.0).
    var estimatedProgress: Float {get}
    
    /// Configures the view with new parameters.
    ///
    /// - Parameter params: The hybrid parameters to apply to the view
    func config(withParams params: SPKHybridParams?)
    
    /// Called when the view becomes visible with optional parameters.
    ///
    /// - Parameter params: Parameters passed when the view is shown
    func onshow(params: [AnyHashable: Any])
    
    /// Called when the view becomes hidden with optional parameters.
    ///
    /// - Parameter params: Parameters passed when the view is hidden
    func onHide(params: [AnyHashable: Any])
    
    /// Updates the view with new global properties.
    ///
    /// - Parameter globalProps: The global properties to update
    func update(withGlobalProps globalProps: Any?)
    
    /// Configures the view with global properties.
    ///
    /// - Parameter globalProps: The global properties to configure
    func config(withGlobalProps globalProps: Any?)
    
    /// Registers a UI component class with a processor name.
    ///
    /// - Parameters:
    ///   - withUI: The UI component class to register
    ///   - name: The processor name to associate with the UI component
    @objc optional func register(withUI: AnyClass, processorName name: String)
    
    /// Updates the view with new data using an optional processor.
    ///
    /// - Parameters:
    ///   - data: The data to update with
    ///   - processor: Optional processor name for handling the data
    @objc optional func update(withData data: Any?, processorName processor: String?)
    
    /// Triggers a layout update for the view.
    @objc optional func triggerLayout()
    
    /// Returns the URL string currently being loaded.
    ///
    /// - Returns: The URL string, or nil if not available
    @objc optional func loadURLString() -> String?
    
    /// Called when the view controller is about to be destroyed.
    @objc optional func onVCWillDestory()
}

/// Extension providing convenient access to the method pipe.
extension SPKWrapperViewProtocol {
    /// Provides typed access to the method pipe facade.
    ///
    /// - Returns: The method pipe as a MethodPipeFacade, or nil if not available
    public var methodPipe: MethodPipeFacade? {
        return anyMethodPipe as? MethodPipeFacade
    }
}
