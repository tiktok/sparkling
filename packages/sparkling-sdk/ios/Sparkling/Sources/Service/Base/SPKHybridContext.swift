// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Lynx

/// Protocol defining the interface for hybrid parameter objects.
/// 
/// `SPKHybridParams` provides a common interface for objects that
/// carry global properties and context information for hybrid views.
/// This protocol ensures consistent access to context data across
/// different parameter types.
@objc
public protocol SPKHybridParams {
    /// Global properties dictionary containing device and environment information.
    var globalPropos: Any? {set get}
    
    /// The hybrid context containing configuration and state information.
    var context: SPKHybridContext? {set get}
}

/// Core context class for hybrid view configuration and state management.
/// 
/// `SPKHybridContext` serves as the central configuration object for
/// hybrid views, containing engine settings, global properties, URL parameters,
/// and provider instances. It supports context merging, copying, and provides
/// specialized configuration for different hybrid engines (Web, Lynx).
/// 
/// The context includes:
/// - Engine type and configuration
/// - Global properties and query parameters
/// - Pipe method instances for JavaScript bridge
/// - Lynx-specific providers and configuration
/// - Layout and rendering parameters
@objcMembers
open class SPKHybridContext: NSObject, NSCopying {
    
    /// The type of hybrid engine to use for rendering content.
    /// 
    /// Determines which rendering engine (Web, Lynx, etc.) should be used
    /// for the hybrid view. Defaults to unknown type.
    public var engineType: SPKHybridEngineType = .SPKHybridEngineTypeUnknown
    
    /// Global properties containing device and environment information.
    /// 
    /// This can be either a dictionary or other data structure containing
    /// system properties, device capabilities, and environment settings.
    public var globalProps: Any?
    
    /// extra query parameters
    public var extra: [String: AnyHashable]?
    
    /// Legacy field kept for API compatibility
    public var queryItems: [String: AnyHashable]?
    
    /// Namespace for pipe method communication.
    /// 
    /// Defines the namespace used for JavaScript bridge communication.
    /// Defaults to "host" for standard host-to-guest communication.
    public var pipeNameSpace: String = "host"
    
    /// Scheme parameters containing URL-based configuration.
    /// 
    /// Holds parsed parameters from URL schemes that configure
    /// navigation, appearance, and behavior settings.
    public var schemeParams: SPKHybridSchemeParam?
    
    /// The original URL that initiated the hybrid view creation.
    /// 
    /// Stores the source URL for reference and debugging purposes.
    public var originURL: String?
    
    /// Outer scheme string with missing merged query keys appended (Spark `getFullUrl` semantics). Filled when building Lynx params.
    public var fullURL: String?
    
    /// Array of pipe method instances for JavaScript bridge communication.
    /// 
    /// Contains method handlers that can be called from JavaScript
    /// to interact with native functionality.
    public var pipeMethodInstances: [Any]?
    
    // MARK: - Lynx Engine Configuration
    
    /// Width mode configuration for Lynx rendering.
    /// 
    /// Specifies how the Lynx view should handle width calculations
    /// and layout constraints.
    public var widthMode: NSNumber?
    
    /// Height mode configuration for Lynx rendering.
    /// 
    /// Specifies how the Lynx view should handle height calculations
    /// and layout constraints.
    public var heightMode: NSNumber?
    
    /// Data to be loaded into the Lynx view.
    /// 
    /// Contains the initial data or content that should be rendered
    /// by the Lynx engine.
    public var loadData: Any?
    
    public var customUIElements: [Any]? = []
    /// Lynx module configuration dictionary.
    /// 
    /// Contains module-specific settings and configurations
    /// for the Lynx rendering engine.
    public var lynxModule: [String: Any]?
    
    /// Initial data passed to the Lynx view on creation.
    /// 
    /// Contains the data structure that will be available
    /// to the Lynx template during initial rendering.
    public var initialData: [AnyHashable: Any]?
    
    /// Provider for Lynx templates and template resolution.
    /// 
    /// Handles loading and caching of Lynx templates used
    /// for rendering hybrid content.
    public var templateProvider: LynxTemplateProvider?
    
    /// Image fetcher for loading images in Lynx views.
    /// 
    /// Handles image loading, caching, and optimization
    /// for images displayed in Lynx content.
    public var imageFetcher: LynxImageFetcher?
    
    /// Fetcher for dynamic components in Lynx views.
    /// 
    /// Handles loading of dynamic components that can be
    /// injected into Lynx templates at runtime.
    public var dynamicComponentFetcher: LynxDynamicComponentFetcher?
    
    /// Block for building raw views when needed.
    /// 
    /// Called to create custom native views that can be
    /// integrated into the hybrid view hierarchy.
    public var rawViewBuilderBlock: ((Any?) -> Void)?
    
    
    /// Merges two dictionaries with optional override behavior.
    /// 
    /// - Parameters:
    ///   - sourDict: The source dictionary to merge from. If nil, returns nil.
    ///   - targetDict: The target dictionary to merge into. If nil, returns nil.
    ///   - isOverride: If true, existing keys in target will be overwritten.
    ///                 If false, existing keys in target will be preserved.
    /// - Returns: A new merged dictionary, or nil if either input is nil.
    public static func merge(withDict sourDict: [String: Any]?,
                             to targetDict: [String: Any]?,
                             isOverride: Bool = false) -> Dictionary<String, Any>? {
        guard let sourDict = sourDict,
              let targetDict = targetDict else {
            return nil
        }
        
        if (isEmptyDictionary(targetDict)) {
            return sourDict
        }
        
        if (isEmptyDictionary(sourDict)) {
            return targetDict
        }
        
        return targetDict.merging(sourDict) { old, new in
            return isOverride ? new : old
        }
    }
    
    /// Creates a deep copy of the hybrid context.
    /// 
    /// - Parameter zone: The memory zone for allocation (unused in ARC).
    /// - Returns: A new SPKHybridContext instance with all properties copied.
    public func copy(with zone: NSZone? = nil) -> Any {
        let copy = SPKHybridContext()
        if let context = copy as? SPKHybridContext {
            context.merge(withContext: self, isOverride: true)
        }
        return copy
    }
    
    /// Merges a property value with optional override behavior.
    /// 
    /// - Parameters:
    ///   - sourceProp: The source property to merge from.
    ///   - targetProp: The target property to merge into.
    ///   - isOverride: If true, source takes precedence over target.
    ///                 If false, target takes precedence over source.
    /// - Returns: The merged property value, preferring non-nil values.
    public static func merge(withProp sourceProp: Any?,
                             to targetProp: Any?,
                             isOverride: Bool) -> Any? {
        return isOverride ? (sourceProp ?? targetProp) : (targetProp ?? sourceProp)
    }
    
    /// Merges two arrays by concatenating them.
    /// 
    /// - Parameters:
    ///   - sourceArray: The source array to merge from. If nil, returns nil.
    ///   - targetArray: The target array to merge into. If nil, returns nil.
    /// - Returns: A new array containing elements from both arrays, or nil if either input is nil.
    ///            Source array elements are placed before target array elements.
    public static func merge(withArray sourceArray: [Any]?, to targetArray: [Any]?) -> [Any]? {
        guard let sourceArray = sourceArray,
              let targetArray = targetArray else {
            return nil;
        }
        if isEmptyArray(sourceArray) {
            return targetArray
        }
        if isEmptyArray(targetArray) {
            return sourceArray
        }

        return sourceArray + targetArray
    }
    
    /// Merges another context into this context with optional override behavior.
    /// 
    /// This method merges all properties from the provided context into the current context.
    /// Different merge strategies are applied based on property types:
    /// - Dictionary properties use dictionary merging
    /// - Other properties use property merging with override behavior
    /// 
    /// - Parameters:
    ///   - context: The source context to merge from. If nil, no operation is performed.
    ///   - isOverride: If true, source context properties will override existing properties.
    ///                 If false, existing properties will be preserved when conflicts occur.
    open func merge(withContext context:SPKHybridContext?, isOverride: Bool = false) {
        guard let context = context else {
            return
        }
        
        if self.globalProps is [String: Any] {
            self.globalProps = Self.merge(withDict: context.globalProps as? [String: Any],
                                          to: self.globalProps as? [String: Any],isOverride: isOverride)
        } else {
            self.globalProps = Self.merge(withProp: context.globalProps,
                                          to: self.globalProps,
                                          isOverride: isOverride)
        }
        
        self.engineType = Self.merge(withProp: context.engineType,
                                     to: self.engineType,
                                     isOverride: isOverride) as? SPKHybridEngineType ?? .SPKHybridEngineTypeUnknown
        
        self.schemeParams = Self.merge(withProp: context.schemeParams,
                                       to: self.schemeParams,
                                       isOverride: isOverride) as? SPKHybridSchemeParam
        
        self.originURL = Self.merge(withProp: context.originURL,
                                    to: self.originURL,
                                    isOverride: isOverride) as? String
        
        self.fullURL = Self.merge(withProp: context.fullURL,
                                  to: self.fullURL,
                                  isOverride: isOverride) as? String
        
        self.extra = Self.merge(withProp: context.extra,
                                to: self.extra,
                                isOverride: isOverride) as? [String: AnyHashable]
        
        self.queryItems = Self.merge(withProp: context.queryItems,
                                     to: self.queryItems,
                                     isOverride: isOverride) as? [String: AnyHashable]
        
        self.pipeNameSpace = Self.merge(withProp: context.pipeNameSpace,
                                        to: self.pipeNameSpace,isOverride: isOverride)as? String ?? self.pipeNameSpace

        self.pipeMethodInstances = Self.merge(withProp: context.pipeMethodInstances,
                                              to: self.pipeMethodInstances,
                                              isOverride: isOverride) as? [Any]

        self.widthMode = Self.merge(withProp: context.widthMode,
                                    to: self.widthMode,
                                    isOverride: isOverride) as? NSNumber

        self.heightMode = Self.merge(withProp: context.heightMode,
                                     to: self.heightMode,
                                     isOverride: isOverride) as? NSNumber

        self.initialData = Self.merge(withProp: context.initialData,
                                      to: self.initialData,
                                      isOverride: isOverride) as? [String: Any]
        
        self.lynxModule = Self.merge(withDict: context.lynxModule,
                                     to: self.lynxModule)
        
        self.customUIElements = Self.merge(withArray: context.customUIElements,
                                           to: self.customUIElements)
        
        self.loadData = Self.merge(withProp: context.loadData,
                                   to: self.loadData,
                                   isOverride: isOverride)

        self.templateProvider = Self.merge(withProp: context.templateProvider,
                                           to: self.templateProvider,
                                           isOverride: isOverride) as? LynxTemplateProvider

        self.imageFetcher = Self.merge(withProp: context.imageFetcher,
                                       to: self.imageFetcher,
                                       isOverride: isOverride) as? LynxImageFetcher

        self.dynamicComponentFetcher = Self.merge(withProp: context.dynamicComponentFetcher,
                                                  to: self.dynamicComponentFetcher,
                                                  isOverride: isOverride) as? LynxDynamicComponentFetcher
        
        self.rawViewBuilderBlock = Self.merge(withProp: context.rawViewBuilderBlock,
                                              to: self.rawViewBuilderBlock,
                                              isOverride: isOverride) as? ((Any?) -> Void)
    }
}
