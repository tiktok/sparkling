// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// Protocol defining the interface for scheme resolution in the SPK framework.
/// 
/// This protocol establishes the contract for resolving URLs into SPKSchemeParam objects,
/// which contain the configuration and parameters needed to load SPK content.
/// Classes conforming to this protocol must implement the resolver method.
@objc protocol SPKSchemeProtocol {
    /// Resolves a URL scheme into a SPKSchemeParam object.
    /// 
    /// This method processes the provided URL and context to create appropriate
    /// scheme parameters for loading SPK content.
    /// 
    /// - Parameters:
    ///   - originURL: The original URL to resolve
    ///   - context: The SPK context containing configuration
    ///   - paramClass: The class type to use for creating the scheme parameters
    /// - Returns: A configured SPKSchemeParam instance, or nil if resolution fails
    static func resolver(
        withScheme originURL: URL?,
        context: SPKContext,
        paramClass: AnyClass) -> SPKSchemeParam?
}

/// Default implementation of the SPKSchemeProtocol for URL scheme resolution.
/// 
/// SPKScheme provides the core logic for resolving URLs into SPKSchemeParam objects.
/// It handles engine type detection, parameter extraction from URL query items, context merging,
/// and parameter validation. This class serves as the primary entry point for scheme resolution
/// in the SPK framework.
/// 
/// The @objcMembers attribute ensures Objective-C compatibility for all properties and methods.
@objcMembers
open class SPKScheme: NSObject, SPKSchemeProtocol {
    /// Resolves a URL scheme into a configured SPKSchemeParam object.
    /// 
    /// This method performs comprehensive URL resolution including:
    /// - Engine type detection based on the URL
    /// - Parameter extraction from URL query items
    /// - Context merging and parameter validation
    /// - Integration with existing context scheme parameters
    /// 
    /// The resolution process validates the engine type and ensures all necessary
    /// parameters are properly configured before returning the result.
    /// 
    /// - Parameters:
    ///   - originURL: The original URL to resolve and process
    ///   - context: The SPK context for configuration and state
    ///   - paramClass: The parameter class type (defaults to SPKSchemeParam)
    /// - Returns: A fully configured SPKSchemeParam instance, or nil if resolution fails
    static func resolver(withScheme originURL: URL?, context: SPKContext?, paramClass: AnyClass = SPKSchemeParam.self) -> SPKSchemeParam? {
        guard let paramClass = paramClass as? SPKSchemeParam.Type else {
            return nil
        }
        
        var extra = originURL?.spk.decodedQueryItems
        var params = paramClass.parseWithCustomSchemeParser(withScheme: originURL, context: context) as? SPKSchemeParam

        if params == nil {
            let engineType = SPKSchemeParam.engineType(withURL: originURL)
            guard engineType != .SPKHybridEngineTypeUnknown else {
                return nil
            }
            params = paramClass.defaultResolver(withScheme: originURL, context: context) as? SPKSchemeParam
        }

        guard let parsedParams = params,
              parsedParams.engineType != .SPKHybridEngineTypeUnknown else {
            return nil
        }

        parsedParams.update(withDictionary: extra)
        
        extra?.updateValue(parsedParams.resolvedURL?.absoluteString ?? "", forKey: "resolvedURL")
        extra?.updateValue(parsedParams.originURL?.absoluteString ?? "", forKey: "originURL")
        
        var newParams = parsedParams
        
        if let contextParams = context?.schemeParams as? SPKSchemeParam {
            newParams = contextParams
            newParams.update(withParam: parsedParams)
        }
        
        context?.schemeParams = newParams
        return newParams
    }
    
}
