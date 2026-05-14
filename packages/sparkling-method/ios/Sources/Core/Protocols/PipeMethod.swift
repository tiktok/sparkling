// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// Base class for pipe methods, all method implementations must inherit from this class
@objc open class PipeMethod: NSObject {
    // Re-added CompletionBlock type for compatibility
    public typealias CompletionBlock = (_ status: MethodStatus, _ result: SPKMethodModel?) -> Void
    // Modified CompletionBlock to be compatible with Objective-C
    @objc public protocol CompletionHandlerProtocol {
        func handleCompletion(status: MethodStatus, result: SPKMethodModel?)
    }

    // Method name, subclasses must override
    @objc open var methodName: String {
        fatalError("Subclasses must override 'methodName'")
    }
    open class func methodName() -> String {
        fatalError("Subclasses must override 'methodName'")
    }

    // Whether it's a global method, default is true
    @objc open class var isGlobal: Bool {
        return true
    }

    // Parameter model type
    @objc open var paramsModelClass: AnyClass {
        fatalError("Subclasses must override 'paramsModelType'")
    }

    // Result model type
    @objc open var resultModelClass: AnyClass {
        fatalError("Subclasses must override 'resultModelType'")
    }

    // Empty constructor, must use required modifier to support instance creation via metatype
    @objc required public override init() {
        super.init()
    }

    // Main call method, subclasses must implement - Using Objective-C compatible way
    @objc dynamic open func call(withParamModel paramModel: Any, completionHandler: CompletionHandlerProtocol) {
        fatalError("Subclasses must override 'call(withParamModel:completionHandler:)")
    }

    // Convenience method for Swift calls
    public func call(withParamModel paramModel: Any, completionHandler: @escaping (_ status: MethodStatus, _ result: SPKMethodModel?) -> Void) {
        let handler = ClosureCompletionHandler(closure: completionHandler)
        call(withParamModel: paramModel, completionHandler: handler)
    }

    // CompletionHandlerProtocol implementation that wraps a closure
    private class ClosureCompletionHandler: NSObject, CompletionHandlerProtocol {
        private let closure: (_ status: MethodStatus, _ result: SPKMethodModel?) -> Void

        init(closure: @escaping (_ status: MethodStatus, _ result: SPKMethodModel?) -> Void) {
            self.closure = closure
        }

        func handleCompletion(status: MethodStatus, result: SPKMethodModel?) {
            closure(status, result)
        }
    }

    // Compatibility method
    open func invoke(withParams params: Any, completion: @escaping (_ status: MethodStatus, _ result: SPKMethodModel?) -> Void) {
        call(withParamModel: params, completionHandler: completion)
    }

    internal func invokeErased(withParams params: Any, completion: @escaping CompletionBlock) {
        // Default implementation, subclasses can override as needed
        call(withParamModel: params, completionHandler: completion)
    }

    // Handle unimplemented methods
    public func handleNotImplemented(message: String? = nil, completion: CompletionBlock) {
        let status = MethodStatus.notImplemented(message: message ?? "The method '\(Self.methodName())' is not implemented")
        completion(status, nil)
    }
}
