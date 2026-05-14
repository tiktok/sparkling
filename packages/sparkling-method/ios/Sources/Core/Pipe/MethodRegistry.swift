// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import ObjectiveC.runtime

public final class MethodRegistry {
    public static let global = MethodRegistry()

    private var methodTypeMap: [String: PipeMethod.Type] = [:]
    private var methodMap: [String: PipeMethod] = [:]
    private var lock = ReadWriteLock()

    init() {}

    public func register(methodType: PipeMethod.Type) {
        lock.write {
            methodTypeMap[methodType.methodName()] = methodType
        }
    }

    public func register(method: PipeMethod) {
        let methodName = type(of: method).methodName()
        lock.write {
            methodMap[methodName] = method
        }
    }

    public func register(methods: [PipeMethod]) {
        if methods.isEmpty { return }
        lock.write {
            for method in methods {
                methodMap[type(of: method).methodName()] = method
            }
        }
    }

    public func register(methodTypes: [PipeMethod.Type]) {
        if methodTypes.isEmpty { return }
        let validMap: [String: PipeMethod.Type] = methodTypes.reduce(into: [String: PipeMethod.Type]()) { result, type in
            result[type.methodName()] = type
        }
        lock.write {
            methodTypeMap.merge(validMap) { (_, new) in new }
        }
    }

    public func unregister(methodName name: String) {
        lock.write {
            methodMap[name] = nil
            methodTypeMap[name] = nil
        }
    }

    public func respondTo(methodName name: String) -> Bool {
        return lock.read {
            methodMap[name] != nil || methodTypeMap[name] != nil
        }
    }

    public func method(forName name: String) -> PipeMethod? {
        return lock.read { methodMap[name] }
            ?? lock.write {
                if let method = methodMap[name] {
                    return method
                }
                guard let methodType = methodTypeMap[name] else {
                    return nil
                }
                let newMethod = methodType.init()
                methodMap[name] = newMethod
                return newMethod
            }
    }

    public func method<T: PipeMethod>(forName name: String) -> T? {
        return method(forName: name) as? T
    }

    public static func autoRegisterGlobalMethods() {
        var count: UInt32 = 0
        let classList = objc_copyClassList(&count)!
        defer {
            free(UnsafeMutableRawPointer(classList))
        }

        let classes = UnsafeBufferPointer(start: classList, count: Int(count))
        var methodTypes: [PipeMethod.Type] = []
        for i in 0..<Int(count) {
            let cls: AnyClass = classes[i]
            if class_getSuperclass(cls) == PipeMethod.self, let type = cls as? PipeMethod.Type {
                methodTypes.append(type)
            }
        }
        MethodRegistry.global.register(methodTypes: methodTypes)
    }
}
