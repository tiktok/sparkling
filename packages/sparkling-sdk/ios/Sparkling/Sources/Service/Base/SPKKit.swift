// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Lynx
import SparklingMethod

/// Main factory class for creating and managing SPK hybrid views.
/// 
/// SPKKit serves as the central entry point for the SPK framework,
/// providing dependency injection container management and view creation services.
/// It uses a service-based architecture to support multiple rendering engines.
@objcMembers
open class SPKKit: NSObject {  
    public static var DIContainer = DIProviderRegistry.provider.container()
    
    public static var trackerService: SPKTrackerService? = DIContainer.resolve(SPKTrackerService.self)
    
    
    /// Creates a hybrid view based on the provided parameters and context.
    /// 
    /// This method serves as the main factory function for creating hybrid views.
    /// It resolves the appropriate view service based on the engine type specified
    /// in the parameters and delegates view creation to that service.
    /// 
    /// - Parameters:
    ///   - params: Scheme parameters containing engine type and configuration.
    ///   - context: Hybrid context with rendering configuration and data.
    ///   - frame: The initial frame for the created view.
    /// - Returns: A UIView conforming to SPKWrapperViewProtocol, or nil if creation fails.
    public static func createKitView(withParams params: SPKHybridSchemeParam?, context: SPKHybridContext?, frame: CGRect) -> (UIView & SPKWrapperViewProtocol)? {
        guard let params = params,
              let context = context else {
            return nil
        }
        let service = self.DIContainer.resolve(SPKViewRegisterService.self, name: String(params.engineType.rawValue))
        let view = service?.createKitView(withParams: params, context: context, frame: frame)
        return view
    }
}
