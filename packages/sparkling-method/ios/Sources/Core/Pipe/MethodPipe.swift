// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

public class MethodPipe: MethodPipeFacade {
    var engine: PipeEngine?
    public private(set) lazy var registry = MethodRegistry()

    public func register(localMethod method: PipeMethod) {
        registry.register(method: method)
    }

    public func register(localMethods methods: [PipeMethod]) {
        registry.register(methods: methods)
    }

    public func register(globalMethod method: PipeMethod) {
        MethodRegistry.global.register(method: method)
    }
    public func unregister(localMethodName name: String) {
        registry.unregister(methodName: name)
    }

    public func unregister(globalMethodName name: String) {
        MethodRegistry.global.unregister(methodName: name)
    }

    public func respondTo(methodName name: String) -> Bool {
        return registry.respondTo(methodName: name) || MethodRegistry.global.respondTo(methodName: name)
    }

    public func method(forName name: String) -> PipeMethod? {
        return registry.method(forName: name) ?? MethodRegistry.global.method(forName: name)
    }

    public func method<T: PipeMethod>(forName name: String) -> T? {
        return method(forName: name) as? T
    }

    public func fireEvent(name: String, params: [String: Any]?) {
        engine?.fireEvent(name: name, params: params)
    }
}
