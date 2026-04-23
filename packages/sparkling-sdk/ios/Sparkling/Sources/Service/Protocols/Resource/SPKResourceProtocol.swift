// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// Protocol defining the interface for SPK resource objects.
///
/// This protocol represents a resource that has been loaded and contains
/// data that can be accessed by the SPK framework.
@objc
public protocol SPKResourceProtocol {
    /// The raw data content of the resource.
    ///
    /// This property provides access to the actual data bytes of the resource,
    /// which could be HTML, JavaScript, CSS, images, or other content types.
    var resourceData: Data? {get}
}

/// Protocol defining the interface for resource loading task control.
///
/// This protocol provides methods to control the lifecycle of a resource
/// loading operation, allowing for cancellation, suspension, and resumption
/// of ongoing network or file system operations.
@objc
public protocol SPKResourceLoaderTaskProtocol {
    /// Cancels the resource loading task.
    ///
    /// Once cancelled, the task cannot be resumed and will not invoke
    /// its completion handler.
    func cancel()
    
    /// Suspends the resource loading task.
    ///
    /// A suspended task can be resumed later using the resume() method.
    func suspend()
    
    /// Resumes a previously suspended resource loading task.
    ///
    /// This method has no effect if the task is not currently suspended.
    func resume()
}

/// Completion handler type for resource loading operations.
///
/// This handler is called when a resource loading operation completes,
/// either successfully with a resource object or with an error.
///
/// - Parameters:
///   - resource: The loaded resource object, or nil if loading failed
///   - error: An error object if loading failed, or nil if successful
public typealias SPKResourceCompletionHandler = (SPKResourceProtocol?, Error?) -> Void

public typealias SPKResourceImageCompletionHandler = (UIImage?, Error?) -> Void

/// Protocol defining the interface for resource loading services.
///
/// This protocol abstracts the resource loading mechanism, allowing
/// different implementations for loading resources from various sources
/// such as network, local files, or embedded bundles.
@objc
public protocol SPKResourceLoaderProtocol {
    /// Loads a resource from the specified URL.
    ///
    /// This method initiates an asynchronous resource loading operation
    /// and returns a task object that can be used to control the operation.
    ///
    /// - Parameters:
    ///   - url: The URL from which to load the resource
    ///   - completion: Completion handler called when loading finishes
    /// - Returns: A task object for controlling the loading operation, or nil if the operation couldn't be started
    func loadResource(withURL url: URL?, completion: @escaping SPKResourceCompletionHandler) -> SPKResourceLoaderTaskProtocol?
    
    func loadImage(withURL url: URL?, completion: @escaping SPKResourceImageCompletionHandler) -> SPKResourceLoaderTaskProtocol?
}


