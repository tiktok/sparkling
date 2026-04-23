// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

public func isEmptyString(_ string: Any?) -> Bool {
    guard let value = optionalCast(string) else {
        return true
    }
    guard let string = value as? String else {
        return true
    }
    return string.isEmpty
}

public func isEmptyArray(_ array: Any?) -> Bool {
    guard let value = optionalCast(array) else {
        return true
    }
    guard let array = value as? [Any] else {
        return true
    }
    return array.isEmpty
}

public func isEmptyDictionary(_ dictionary: Any?) -> Bool {
    guard let value = optionalCast(dictionary) else {
        return true
    }
    guard let dictionary = value as? [AnyHashable: Any] else {
        return true
    }
    return dictionary.isEmpty
}

public func optionalCast<T>(_ value: T) -> T? {
    let object = value as AnyObject?
    switch object {
    case .none:
        return nil
    case .some(let castObject):
        return (castObject as? T)
    }
}
