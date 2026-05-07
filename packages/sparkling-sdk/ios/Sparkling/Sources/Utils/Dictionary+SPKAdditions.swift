// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

public protocol DictionaryProtocol: Collection {
    associatedtype Key: Hashable
    associatedtype Value
}

extension Dictionary: DictionaryProtocol {
    public typealias Key = Key
    public typealias Value = Value
}

extension Dictionary: SPKKitCompatibleValue {}

public extension SPKKitWrapper where Base: DictionaryProtocol {
    var urlQueryString: String? {
        guard let dictionary = base as? [Base.Key: Base.Value] else {
            return nil
        }
        var items: [String] = []
        items.reserveCapacity(dictionary.count)
        
        dictionary.forEach { key, value in
            guard let unwrappedValue = value as? Any,
                  String(describing: unwrappedValue) != "nil" else {
                return
            }
            
            let encodedKey = "\(key)".addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "\(key)"
            let encodedValue = "\(unwrappedValue)".addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "\(unwrappedValue)"
            let item = "\(encodedKey)=\(encodedValue)"
            items.append(item)
        }
        return items.isEmpty ? "" : items.joined(separator: "&")
    }
    func bool(forKey key: Base.Key, default defaultValue: Bool = false) -> Bool {
        guard let dictionary = base as? [Base.Key: Base.Value] else {
            return defaultValue
        }
        guard let value = dictionary[key] else {
            return defaultValue
        }
        if let number = value as? NSNumber {
            return number.boolValue
        } else if let string = value as? NSString {
            return string.boolValue
        } else {
            return defaultValue
        }
    }

    func int(forKey key: Base.Key, default defaultValue: Int = 0) -> Int {
        guard let dictionary = base as? [Base.Key: Base.Value] else {
            return defaultValue
        }
        guard let value = dictionary[key] else {
            return defaultValue
        }
        if let number = value as? NSNumber {
            return number.intValue
        } else if let string = value as? NSString {
            return string.integerValue
        } else {
            return defaultValue
        }
    }

    func float(forKey key: Base.Key, default defaultValue: Float = 0.0) -> Float {
        guard let dictionary = base as? [Base.Key: Base.Value] else {
            return defaultValue
        }
        guard let value = dictionary[key] else {
            return defaultValue
        }
        if let number = value as? NSNumber {
            return number.floatValue
        } else if let string = value as? NSString {
            return string.floatValue
        } else {
            return defaultValue
        }
    }

    func double(forKey key: Base.Key, default defaultValue: Double = 0.0) -> Double {
        guard let dictionary = base as? [Base.Key: Base.Value] else {
            return defaultValue
        }
        guard let value = dictionary[key] else {
            return defaultValue
        }
        if let number = value as? NSNumber {
            return number.doubleValue
        } else if let string = value as? NSString {
            return string.doubleValue
        } else {
            return defaultValue
        }
    }
    func string(forKey key: Base.Key) -> String? {
        guard let dictionary = base as? [Base.Key: Base.Value] else {
            return nil
        }
        guard let value = dictionary[key] else {
            return nil
        }
        if let string = value as? String {
            return string
        } else if let number = value as? NSNumber {
            return number.stringValue
        } else {
            return nil
        }
    }

    func string(forKey key: Base.Key, default defaultValue: String) -> String {
        guard let dictionary = base as? [Base.Key: Base.Value] else {
            return defaultValue
        }
        guard let value = dictionary[key] else {
            return defaultValue
        }
        if let string = value as? String {
            return string
        } else if let number = value as? NSNumber {
            return number.stringValue
        } else {
            return defaultValue
        }
    }

    func array<T>(forKey key: Base.Key) -> [T]? {
        guard let dictionary = base as? [Base.Key: Base.Value] else {
            return nil
        }
        guard let value = dictionary[key] else {
            return nil
        }
        if let array = value as? [T] {
            return array
        } else {
            return nil
        }
    }

    func array<T>(forKey key: Base.Key, default defaultValue: [T]) -> [T] {
        guard let dictionary = base as? [Base.Key: Base.Value] else {
            return defaultValue
        }
        guard let value = dictionary[key] else {
            return defaultValue
        }
        if let array = value as? [T] {
            return array
        } else {
            return defaultValue
        }
    }

    func dictionary<K, V>(forKey key: Base.Key) -> [K: V]? {
        guard let dictionary = base as? [Base.Key: Base.Value] else {
            return nil
        }
        guard let value = dictionary[key] else {
            return nil
        }
        if let dictionary = value as? [K: V] {
            return dictionary
        } else {
            return nil
        }
    }

    func dictionary<K, V>(forKey key: Base.Key, default defaultValue: [K: V]) -> [K: V] {
        guard let dictionary = base as? [Base.Key: Base.Value] else {
            return defaultValue
        }
        guard let value = dictionary[key] else {
            return defaultValue
        }
        if let dictionary = value as? [K: V] {
            return dictionary
        } else {
            return defaultValue
        }
    }
    func object(forKey key: Base.Key) -> Any? {
        guard let dictionary = base as? [Base.Key: Base.Value] else {
            return nil
        }
        guard let value = dictionary[key] else {
            return nil
        }
        return value
    }

    func object<T>(forKey key: Base.Key) -> T? {
        return object(forKey: key) as? T
    }
    func object<T>(forKey key: Base.Key, default defaultValue: T) -> T {
        guard let dictionary = base as? [Base.Key: Base.Value] else {
            return defaultValue
        }
        guard let value = dictionary[key] else {
            return defaultValue
        }
        if let object = value as? T {
            return object
        } else {
            return defaultValue
        }
    }
}
