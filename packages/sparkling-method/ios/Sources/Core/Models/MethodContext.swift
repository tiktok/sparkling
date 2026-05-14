// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

final class WeakWrap {
    weak var value: AnyObject?

    init(_ value: AnyObject) {
        self.value = value
    }
}

enum ValueWrap {
    case strong(Any)
    case weak(WeakWrap)
}

public class MethodContext {
    private var storage: [String: ValueWrap] = [:]

    public func set(_ value: Any, forKey key: String, weak: Bool = false) {
        if weak, let obj = value as? AnyObject {
            storage[key] = .weak(WeakWrap(obj))
        } else {
            storage[key] = .strong(value)
        }
    }

    public func get<T>(_ key: String, as type: T.Type = T.self) -> T? {
        guard let valueWrap = storage[key] else { return nil }
        switch valueWrap {
        case .strong(let value): return value as? T
        case .weak(let weakWrap): return weakWrap.value as? T
        }
    }

    public func removeValue(forKey key: String) {
        storage.removeValue(forKey: key)
    }

    public subscript<T>(key: String) -> T? {
        get { get(key, as: T.self) }
        set {
            if let newValue = newValue {
                set(newValue, forKey: key)
            } else {
                removeValue(forKey: key)
            }
        }
    }
}
