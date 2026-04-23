// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

@objc
public protocol SPKHybridSchemeParsing: AnyObject {
    func parseScheme(_ originURL: URL?, context: SPKHybridContext?) -> SPKHybridSchemeParam?
}

/// Enumeration defining the types of hybrid engines available for rendering content.
/// 
/// This enum specifies which rendering engine should be used for hybrid views,
/// allowing the system to choose between different rendering technologies.
@objc
public enum SPKHybridEngineType : Int {
    /// Unknown or unspecified engine type.
    /// 
    /// Used as a default value when the engine type cannot be determined
    /// or has not been explicitly set.
    case SPKHybridEngineTypeUnknown = 0
    
    /// Web-based rendering engine.
    /// 
    /// Uses WebKit or similar web technologies to render HTML/CSS/JavaScript content.
    case SPKHybridEngineTypeWeb
    
    /// Lynx rendering engine.
    /// 
    /// Uses the Lynx template engine for high-performance native rendering
    /// with dynamic content capabilities.
    case SPKHybridEngineTypeLynx
}

/// Parameter container for hybrid scheme URL resolution and configuration.
/// 
/// This class handles parsing and resolving hybrid scheme URLs, extracting
/// configuration parameters, and determining the appropriate rendering engine.
/// It supports both URL-style and bundle-style scheme formats.
@objcMembers
open class SPKHybridSchemeParam: NSObject {
    
    /// Scheme identifier for Lynx view rendering.
    /// 
    /// Used to identify URLs that should be rendered using the Lynx engine.
    static let lynxViewScheme = "lynxview"
    
    /// Scheme identifier for web view rendering.
    /// 
    /// Used to identify URLs that should be rendered using the web engine.
    static let webViewScheme = "webview"
    
    private static var customSchemeParser: SPKHybridSchemeParsing?
    
    /// Additional parameters extracted from the scheme URL.
    /// 
    /// Contains key-value pairs of configuration parameters that don't
    /// fit into the standard properties. Used for custom configurations
    /// and engine-specific settings.
    open var extra: [String: Any] = [:]
    
    /// The type of rendering engine determined for this scheme.
    /// 
    /// Automatically set based on the scheme URL analysis to indicate
    /// which rendering engine should be used.
    open var engineType: SPKHybridEngineType = .SPKHybridEngineTypeUnknown
    
    /// The original URL that was passed for scheme resolution.
    /// 
    /// Stores the unmodified source URL for reference and debugging purposes.
    open var originURL: URL?
    
    /// The resolved hybrid scheme URL after processing.
    /// 
    /// Contains the final URL that will be used for loading the hybrid content,
    /// with all parameters properly formatted and encoded.
    open var resolvedURL: URL?
    
    /// The target URL string extracted from the scheme parameters.
    /// 
    /// Contains the actual content URL that should be loaded by the
    /// selected rendering engine.
    open var url: String?
    
    /// Initializes a new scheme parameter instance with optional dictionary data.
    /// 
    /// - Parameter dictionary: Optional dictionary containing initial parameter values.
    ///                        If provided, the instance will be updated with these values.
    required public init(withDictionary dictionary: [String: Any]? = nil){
        super.init()
        self.update(withDictionary: dictionary)
    }

    @objc
    public class func setCustomSchemeParser(_ parser: SPKHybridSchemeParsing?) {
        self.customSchemeParser = parser
    }
    
    /// Updates the scheme parameters with values from a dictionary.
    /// 
    /// This method processes the input dictionary to extract URL and other parameters,
    /// updating the instance's properties accordingly.
    /// 
    /// - Parameters:
    ///   - dict: Dictionary containing parameter values to update.
    ///   - context: Optional hybrid context for additional configuration.
    open func update(withDictionary dict: [String: Any]?, context: SPKHybridContext? = nil) {
        guard dict != nil else {
            return
        }
        let newDict = self.dictionary(withInnerUrlQueryItems: dict)
        self.extra = newDict ?? [:]
        self.url = newDict?.spk.string(forKey: "url")
    }
    
    /// Updates this instance with values from another scheme parameter.
    /// 
    /// This method merges the extra parameters and copies the URL and engine type
    /// from the provided parameter instance.
    /// 
    /// - Parameter newParam: The source scheme parameter to merge from.
    open func update(withParam newParam: SPKHybridSchemeParam) {
        var newExtraDict = self.extra

        newExtraDict.merge(newParam.extra) { _, new in new }
        
        self.update(withDictionary: newExtraDict)
        self.originURL = newParam.originURL
        self.resolvedURL = newParam.resolvedURL
        self.engineType = newParam.engineType
    }
    
    /// Resolves a scheme URL into a hybrid scheme parameter configuration.
    /// 
    /// This method analyzes the provided URL to determine the appropriate rendering engine
    /// and extracts configuration parameters. It supports both URL-style and bundle-style
    /// scheme formats and automatically detects the engine type based on the URL structure.
    /// 
    /// - Parameters:
    ///   - orignalURL: The original scheme URL to resolve.
    ///   - context: Optional hybrid context to update with resolved information.
    /// - Returns: A configured SPKHybridSchemeParam instance with resolved parameters.
    public static func resolver(withScheme orignalURL: URL?, context: SPKHybridContext? = nil) -> SPKHybridSchemeParam {
        if let customParam = Self.parseWithCustomSchemeParser(withScheme: orignalURL, context: context) {
            return customParam
        }
        return Self.defaultResolver(withScheme: orignalURL, context: context)
    }

    public static func parseWithCustomSchemeParser(withScheme orignalURL: URL?, context: SPKHybridContext? = nil) -> SPKHybridSchemeParam? {
        guard let parser = Self.customSchemeParser else {
            return nil
        }
        guard let customParam = parser.parseScheme(orignalURL, context: context) else {
            return nil
        }
        if customParam.originURL == nil {
            customParam.originURL = orignalURL
        }
        context?.originURL = orignalURL?.absoluteString
        return customParam
    }

    public static func defaultResolver(withScheme orignalURL: URL?, context: SPKHybridContext? = nil) -> SPKHybridSchemeParam {
        var queries = orignalURL?.spk.decodedQueryItems
        var param = self.init(withDictionary: queries)
        var innerScheme: URL? = nil
        
        if self.checkUrlStyle(inQueries: queries) {
            innerScheme = self.resolveURLStyle(toHybridScheme: orignalURL, queries: queries)
        } else if self.checkBundleStyle(inQueries: queries) {
            innerScheme = self.resolveBundleStyle(toHybridScheme: orignalURL, queries: queries)
        }
        
        if innerScheme?.host?.contains(Self.lynxViewScheme) == true {
            param.engineType = .SPKHybridEngineTypeLynx
        } else if innerScheme?.host?.contains(Self.webViewScheme) == true {
            param.engineType = .SPKHybridEngineTypeWeb
        }
        
        let scheme = String("hybrid://hybrid")
        var dict: [String: String] = [:]
        dict.updateValue(innerScheme?.absoluteString ?? "", forKey: "url")
        
        let resolvedScheme = URL.spk.url(string: scheme)?.spk.merging(queries: dict, encode: true)
        param.resolvedURL = resolvedScheme
        param.originURL = orignalURL
        context?.originURL = orignalURL?.absoluteString
        return param
    }
    
    /// Determines the engine type for a given URL.
    /// 
    /// This method analyzes the URL structure to identify which rendering engine
    /// should be used. It checks for specific scheme identifiers and validates
    /// that the URL can be properly resolved.
    /// 
    /// - Parameter url: The URL to analyze for engine type determination.
    /// - Returns: The appropriate SPKHybridEngineType, or Unknown if undetermined.
    public static func engineType(withURL url: URL?) -> SPKHybridEngineType {
        guard let url = url else {
            return .SPKHybridEngineTypeUnknown
        }
        if url.host?.contains(Self.lynxViewScheme) == true {
            return self.canResolve(url: url) ? .SPKHybridEngineTypeLynx : .SPKHybridEngineTypeUnknown
        } else if url.host?.contains(Self.webViewScheme) == true {
            return self.canResolve(url: url) ? .SPKHybridEngineTypeWeb : .SPKHybridEngineTypeUnknown
        }
        return .SPKHybridEngineTypeUnknown
    }
    
    /// Checks if a URL can be resolved by the hybrid scheme system.
    /// 
    /// This method validates whether the provided URL follows a supported
    /// scheme format (either URL-style or bundle-style) that can be processed
    /// by the hybrid system.
    /// 
    /// - Parameter url: The URL to validate for resolution capability.
    /// - Returns: true if the URL can be resolved, false otherwise.
    public static func canResolve(url: URL?) -> Bool {
        var queries = url?.spk.decodedQueryItems
        if self.checkUrlStyle(inQueries: queries) || self.checkBundleStyle(inQueries: queries) {
            return true
        }
        return false
    }
    
    open func dictionary(withInnerUrlQueryItems dict: [String: Any]?) -> Dictionary<String, Any>? {
        guard let dict = dict else {
            return nil
        }
        guard let innerUrl = dict.spk.string(forKey: "url") else {
            return dict
        }
        
        var innerParams: [String: Any] = URL.spk.url(string: innerUrl)?.spk.decodedQueryItems ?? [:]
        innerParams.merge(dict, uniquingKeysWith: { _, new in new})
        return innerParams
    }
    
    static public func checkUrlStyle(inQueries queries: [String: String]?) -> Bool {
        return !isEmptyString(queries?.spk.string(forKey: "url"))
    }
    
    static public func checkBundleStyle(inQueries queries: [String: String]?) -> Bool {
        return !isEmptyString(queries?.spk.string(forKey: "bundle"))
    }

    static public func resolveURLStyle(toHybridScheme originURL: URL?, queries: [String: String]?) -> URL? {
        if originURL?.host?.contains("lynx") == true {
            let url = queries?.spk.string(forKey: "url") ?? ""
            var lynxQuery = queries
            lynxQuery?.removeValue(forKey: "url")
            let resolved = URL.spk.url(string: "hybrid://lynxview", queryItems: lynxQuery)
            return resolved
        } else if originURL?.host?.contains("webview") == true {
            let baseUrl = String("hybrid://webview")
            let url = URL.spk.url(string: baseUrl, queryItems: queries)
            return url
        }
        return originURL
    }
    
    static public func resolveBundleStyle(toHybridScheme originURL: URL?, queries: [String: String]?) -> URL? {
        let bundle = queries?.spk.string(forKey: "bundle") ?? ""
        let baseURL = "hybrid://lynxview?bundle=\(bundle)"
        var lynxQuery = queries ?? [:]
        lynxQuery.removeValue(forKey: "bundle")
        let url = URL.spk.url(string: baseURL, queryItems: lynxQuery)
        return url
    }
}
  
