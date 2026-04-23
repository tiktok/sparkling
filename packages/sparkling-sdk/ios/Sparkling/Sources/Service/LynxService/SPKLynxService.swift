// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Lynx

/// Service class responsible for creating and managing Lynx-based views.
/// 
/// This service implements the SPKViewRegisterService protocol to provide
/// Lynx rendering engine integration within the SPK framework. It handles
/// the creation of SPKWrapperLynxView instances with proper parameter conversion
/// and configuration.
@objcMembers
open class SPKLynxService: NSObject, SPKViewRegisterService {
    
    static func executePrepareServiceTask() {
        LynxEnv.sharedInstance()
        // Enable Lynx Debug switch
        LynxEnv.sharedInstance().lynxDebugEnabled = true
        // Enable Lynx DevTool switch
        LynxEnv.sharedInstance().devtoolEnabled = true
        // Enable Lynx LogBox switch
        LynxEnv.sharedInstance().logBoxEnabled = true
        SPKKit.DIContainer.register(SPKViewRegisterService.self, name: String(SPKHybridEngineType.SPKHybridEngineTypeLynx.rawValue), scope: .transient) {
            SPKLynxService()
        }
    }
    
    /// Initializes a new instance of the Lynx service.
    /// 
    /// This initializer sets up the service for creating Lynx-based views
    /// within the SPK framework.
    public override init() {
        super.init()
    }
    
    /// Creates a new Lynx kit view with the specified parameters and context.
    /// 
    /// This method converts the hybrid context into Lynx-specific parameters
    /// and creates a configured SPKWrapperLynxView instance. The view is ready
    /// for rendering Lynx templates and handling user interactions.
    /// 
    /// - Parameters:
    ///   - params: The hybrid scheme parameters containing URL and configuration data.
    ///   - context: The hybrid context with global properties and providers.
    ///   - frame: The initial frame rectangle for the view.
    /// - Returns: A configured SPKWrapperLynxView instance conforming to SPKWrapperViewProtocol,
    ///           or nil if creation fails.
    public func createKitView(withParams params: SPKHybridSchemeParam?, context: SPKHybridContext?, frame: CGRect) -> (any UIView & SPKWrapperViewProtocol)? {
        let lynxParams = SPKLynxKitUtils.lynxKitParams(withContext: context)
        let kitView = SPKWrapperLynxView(withFrame: frame, params: lynxParams)
        context?.customUIElements?.forEach({ elements in
            guard let element = elements as? SPKLynxElement else {
                return
            }
            kitView.register(withUI: element.lynxElementClassName, withName: element.lynxElementName)
        })
        return kitView
    }
}
