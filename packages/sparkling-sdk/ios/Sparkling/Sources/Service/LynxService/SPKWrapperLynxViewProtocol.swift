// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Lynx

@objc
public protocol SPKLynxElement {
    var lynxElementName: String {set get}
    var lynxElementClassName: AnyObject.Type {set get}
}

/// Configuration parameters for SPKWrapperLynxView instances.
///
/// This class encapsulates all the necessary configuration data required to
/// initialize and configure a Lynx-based hybrid view, including resource providers,
/// layout modes, and metadata for loading and updating operations.
@objc public class SPKLynxKitParams: NSObject, SPKHybridParams {
    /// Global properties to be shared across the Lynx runtime environment.
    public var globalPropos: Any?
    
    /// The hybrid context containing configuration and state information.
    public var context: SPKHybridContext?
    
    /// The source URL from which to load the Lynx template or content.
    public var sourceUrl: String?
    
    /// Initial properties to pass to the Lynx view upon creation.
    public var initialProperties: Any?
    
    /// The width sizing mode for the Lynx view layout.
    public var widthMode: LynxViewSizeMode?
    
    /// The height sizing mode for the Lynx view layout.
    public var heightMode: LynxViewSizeMode?
    
    /// Query parameters to be passed along with requests.
    public var queryItems: [String: Any]?
    
    /// Optional block for custom view builder configuration.
    public var rawViewBuilderBlock: ((Any, String) -> Void)?
    
    /// Provider for loading Lynx templates.
    public weak var templateProvider: LynxTemplateProvider?
    
    /// Provider for fetching images used within the Lynx view.
    public weak var imageFetcher: LynxImageFetcher?
    
    /// Provider for fetching general resources.
    public weak var resourceFetcher: LynxResourceFetcher?
    
    /// Provider for loading dynamic components at runtime.
    public weak var dynamicComponentFetcher: LynxDynamicComponentFetcher?
    
    /// Metadata associated with the initial load operation.
    public var loadMeta: Any?
}

/// Type alias for bridge handler functions that facilitate communication
/// between native code and the Lynx JavaScript runtime.
///
/// - Parameters:
///   - container: The container object (typically the view)
///   - name: The method name being called
///   - params: Optional parameters for the method call
///   - callback: Completion callback with result code and data
public typealias HybridLynxBridgeHandler = (_ container: Any, _ name: String, _ params: [AnyHashable: Any]?, _ callback: (_ code: Int, _ data: [AnyHashable: Any]?) -> Void) -> Void

/// Protocol defining the interface for SPK views that integrate with the Lynx rendering engine.
///
/// This protocol extends SPKWrapperViewProtocol to provide Lynx-specific functionality,
/// including access to the underlying Lynx view, configuration management, and
/// registration of modules, shadow nodes, and UI components.
@objc public protocol SPKWrapperLynxViewProtocol: SPKWrapperViewProtocol {
    /// The underlying Lynx view instance that handles the actual rendering.
    var lynxView: LynxView? {set get}
    
    /// The Lynx configuration object that defines the rendering environment.
    var lynxConfig: LynxConfig? {set get}
    
    /// Flag indicating whether the Lynx view has been successfully created.
    var isLynxCreated: Bool {set get}
    
    /// Initializes a new Lynx view with the specified frame and parameters.
    ///
    /// - Parameters:
    ///   - frame: The frame rectangle for the view
    ///   - params: Configuration parameters for the Lynx view
    init(withFrame frame: CGRect, params: SPKLynxKitParams?)
    
//    func register(withHandler handler: @escaping HybridLynxBridgeHandler, forMethod method: String)
    
    /// Registers a Lynx module with optional parameters.
    ///
    /// - Parameters:
    ///   - module: The module type to register
    ///   - param: Optional parameters for the module
    func register(withModule module: LynxModule.Type, param: Any?)
    
    /// Registers a shadow node class with a specific name.
    ///
    /// - Parameters:
    ///   - node: The shadow node class to register
    ///   - name: The name to associate with the shadow node
    func register(withShadowNode node: AnyClass, withName name: String)
    
    /// Registers a UI component class with a specific name.
    ///
    /// - Parameters:
    ///   - ui: The UI component class to register
    ///   - name: The name to associate with the UI component
    func register(withUI ui: AnyClass, withName name: String)
    
//    func remove(LynxBridgeWithMethodNames methods: [String])
}
