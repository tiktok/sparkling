// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Lynx

/// Delegate protocol for monitoring resource loading operations.
/// 
/// This protocol provides callbacks for tracking the lifecycle of resource
/// loading operations, allowing observers to monitor start and completion events.
@objc public protocol SPKLynxResourceProviderDelegate {
    /// Called when a resource loading operation begins.
    /// - Parameter url: The URL of the resource being loaded.
    func resourceProviderDidStartLoad(withURL url: String)
    
    /// Called when a resource loading operation completes.
    /// - Parameters:
    ///   - url: The URL of the resource that was loaded.
    ///   - resource: The loaded resource, or nil if loading failed.
    ///   - error: Any error that occurred during loading, or nil if successful.
    func resourceProviderDidFinishLoad(withURL url: String, resource:SPKResourceProtocol?, error: Error?)
}

/// Comprehensive resource provider for Lynx rendering engine.
/// 
/// This class implements multiple Lynx resource provider protocols to handle
/// various types of resource loading including templates, dynamic components,
/// SSR data, and general resources. It manages concurrent requests with
/// thread-safe operations and provides delegate callbacks for monitoring.
@objcMembers
open class SPKLynxResourceProvider: NSObject, LynxResourceProvider {
    
    /// Thread-safe map of active resource loading tasks keyed by URL.
    private var requestMap: [String: SPKResourceLoaderTaskProtocol] = [:]
    /// Recursive lock for thread-safe access to the request map.
    private var requestMapLock: NSRecursiveLock = NSRecursiveLock()
    
    
    /// Delegate for receiving resource loading lifecycle callbacks.
    public weak var delegate: SPKLynxResourceProviderDelegate?
    /// Source URL for template resources.
    public var templateSourceURL: String?
    
    /// Associated Lynx view for resource loading context.
    public weak var lynxView: LynxView?
    /// Hybrid context containing configuration and parameters.
    public weak var context: SPKHybridContext?
    
    /// Custom template provider for specialized template loading logic.
    public weak var customTemplateProvider: LynxTemplateProvider?
    
    /// Resource loader instance resolved from dependency injection container.
    public var resourceLoader: SPKResourceLoaderProtocol? = SPKKit.DIContainer.resolve(SPKResourceLoaderProtocol.self)
    
    /// Handles resource loading requests from the Lynx engine.
    /// 
    /// This method processes resource requests by delegating to the configured
    /// resource loader, managing the request lifecycle, and converting responses
    /// to the expected Lynx format. It maintains thread-safe tracking of active
    /// requests for proper cancellation support.
    /// 
    /// - Parameters:
    ///   - request: The Lynx resource request containing URL and metadata.
    ///   - callback: Completion block called with the resource response.
    public func request(_ request: LynxResourceRequest, onComplete callback: @escaping LynxResourceLoadBlock) {
        var currentTask = self.resourceLoader?.loadResource(withURL: URL.spk.url(string: request.url), completion: { [weak self] resource, error in
            var response: LynxResourceResponse = LynxResourceResponse()
            if let nsError = error as? NSError {
                response = LynxResourceResponse(error: nsError, code: nsError.code)
            } else if resource != nil {
                response = LynxResourceResponse(data: resource?.resourceData)
            } else {
                var lError = NSError(domain: "SPKLynxResourceProvider", code: -1, userInfo: ["reason": "no data"])
                response = LynxResourceResponse(error: lError, code: lError.code)
            }
            self?.requestMapLock.lock()
            self?.requestMap.removeValue(forKey: request.url)
            self?.requestMapLock.unlock()
            callback(response)
        })
        
        guard let task = currentTask else {
            return
        }
        self.requestMapLock.lock()
        self.requestMap.updateValue(task, forKey: request.url)
        self.requestMapLock.unlock()
    }
    
    /// Cancels an active resource loading request.
    /// 
    /// This method safely cancels a resource loading task and removes it
    /// from the active request tracking map. The operation is thread-safe
    /// and handles cases where the request may have already completed.
    /// 
    /// - Parameter request: The Lynx resource request to cancel.
    public func cancel(_ request: LynxResourceRequest) {
        self.requestMapLock.lock()
        var task: SPKResourceLoaderTaskProtocol? = self.requestMap.spk.object(forKey: request.url)
        task?.cancel()
        self.requestMap.removeValue(forKey: request.url)
        self.requestMapLock.unlock()
    }
}

/// Extension implementing LynxResourceFetcher protocol for general resource loading.
extension SPKLynxResourceProvider: LynxResourceFetcher {
    /// Loads a resource with the specified URL and type.
    /// 
    /// This method provides a cancellable resource loading operation that
    /// integrates with the Lynx resource fetching system. It returns a
    /// cancellation closure that can be used to abort the loading operation.
    /// 
    /// - Parameters:
    ///   - url: The URL of the resource to load.
    ///   - type: The type of resource being fetched.
    ///   - completionBlock: Completion block called with loading results.
    /// - Returns: A closure that can be called to cancel the loading operation.
    public func loadResource(with url: URL, type: LynxFetchResType, completion completionBlock: @escaping LynxResourceLoadCompletionBlock) -> () -> Void {
        var task = self.resourceLoader?.loadResource(withURL: url, completion: { resource, error in
            completionBlock(false, resource?.resourceData, error, url)
        })
        
        return { [weak task] in
            task?.cancel()
        }
    }
}

/// Extension implementing LynxTemplateProvider protocol for template loading.
extension SPKLynxResourceProvider: LynxTemplateProvider {
    /// Loads a Lynx template from the specified URL.
    /// 
    /// This method handles template loading with support for custom template
    /// providers. If a custom provider is configured and responds to the
    /// template loading selector, it delegates to that provider. Otherwise,
    /// it uses the standard resource loader with delegate notifications.
    /// 
    /// - Parameters:
    ///   - url: The URL string of the template to load.
    ///   - callback: Completion block called with template data and any error.
    public func loadTemplate(withUrl url: String!, onComplete callback: LynxTemplateLoadBlock!) {
        if self.customTemplateProvider != nil &&
            self.customTemplateProvider?.responds(to: #selector(LynxTemplateProvider.loadTemplate(withUrl:onComplete:))) == true {
            self.customTemplateProvider?.loadTemplate(withUrl: url, onComplete: callback)
            return
        }
        
        self.delegate?.resourceProviderDidStartLoad(withURL: url)
        
        if let resourceLoader = self.resourceLoader {
            resourceLoader.loadResource(withURL: URL.spk.url(string: url)) { [weak self] resource, error in
                DispatchQueue.spk.asyncMain { [weak self] in
                    self?.delegate?.resourceProviderDidFinishLoad(withURL: url, resource: resource, error: error)
                    callback(resource?.resourceData, error)
                }
            }
        }
    }
}

/// Extension implementing LynxDynamicComponentFetcher protocol for dynamic component loading.
extension SPKLynxResourceProvider: LynxDynamicComponentFetcher {
    /// Loads a dynamic component from the specified URL.
    /// 
    /// This method fetches dynamic component data that can be used to
    /// dynamically create and configure Lynx components at runtime.
    /// 
    /// - Parameters:
    ///   - url: The URL string of the dynamic component to load.
    ///   - block: Completion block called with component data and any error.
    public func loadDynamicComponent(_ url: String, withLoadedBlock block: @escaping onComponentLoaded) {
        self.resourceLoader?.loadResource(withURL: URL.spk.url(string: url), completion: { resource, error in
            block(resource?.resourceData, error)
        })
    }
}

/// Extension implementing LynxTemplateResourceFetcher protocol for template and SSR data fetching.
extension SPKLynxResourceProvider: LynxTemplateResourceFetcher {
    /// Fetches template resource data for Lynx rendering.
    /// 
    /// This method loads template resources and converts them to the
    /// LynxTemplateResource format required by the Lynx rendering engine.
    /// 
    /// - Parameters:
    ///   - request: The resource request containing URL and metadata.
    ///   - callback: Completion block called with template resource and any error.
    public func fetchTemplate(_ request: LynxResourceRequest, onComplete callback: @escaping LynxTemplateResourceCompletionBlock) {
        self.resourceLoader?.loadResource(withURL: URL.spk.url(string: request.url), completion: { resource, error in
            guard let data = resource?.resourceData else {
                callback(nil, error)
                return
            }
            var lynxData = LynxTemplateResource(nsData: data)
            callback(lynxData, error)
        })
    }
    
    /// Fetches server-side rendering (SSR) data for Lynx templates.
    /// 
    /// This method loads SSR data that provides pre-rendered content
    /// or initial state for Lynx templates, improving rendering performance
    /// and user experience.
    /// 
    /// - Parameters:
    ///   - request: The resource request containing URL and metadata.
    ///   - callback: Completion block called with SSR data and any error.
    public func fetchSSRData(_ request: LynxResourceRequest, onComplete callback: @escaping LynxSSRResourceCompletionBlock) {
        self.resourceLoader?.loadResource(withURL: URL.spk.url(string: request.url), completion: { resource, error in
            callback(resource?.resourceData, error)
        })
    }
}


extension SPKLynxResourceProvider: LynxImageFetcher {
    
    public func loadImage(with url: URL, size targetSize: CGSize, contextInfo: [AnyHashable : Any]?, completion completionBlock: @escaping LynxImageLoadCompletionBlock) -> () -> Void {
        let task = self.resourceLoader?.loadImage(withURL: url, completion: { image, error in
            completionBlock(image, error, url)
        })
        return {
            task?.cancel()
        }
    }
}
