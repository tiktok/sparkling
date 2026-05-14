// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

public enum AnyCodableValue: Codable {
    case string(String)
    case int(Int)
    case double(Double)
    case bool(Bool)
    case dictionary([String: AnyCodableValue])
    case array([AnyCodableValue])

    public init?(_ any: Any) {
        switch any {
        case let v as String:
            self = .string(v)
        case let v as Int:
            self = .int(v)
        case let v as Double:
            self = .double(v)
        case let v as Bool:
            self = .bool(v)
        case let v as [String: Any]:
            let dict = v.compactMapValues { AnyCodableValue($0) }
            self = .dictionary(dict)
        case let v as [Any]:
            let arr = v.compactMap { AnyCodableValue($0) }
            self = .array(arr)
        default:
            return nil
        }
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let int = try? container.decode(Int.self) {
            self = .int(int)
        } else if let double = try? container.decode(Double.self) {
            self = .double(double)
        } else if let bool = try? container.decode(Bool.self) {
            self = .bool(bool)
        } else if let str = try? container.decode(String.self) {
            self = .string(str)
        } else if let dict = try? container.decode([String: AnyCodableValue].self) {
            self = .dictionary(dict)
        } else if let arr = try? container.decode([AnyCodableValue].self) {
            self = .array(arr)
        } else {
            throw DecodingError.dataCorruptedError(
                in: container,
                debugDescription: "Unsupported value type"
            )
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch self {
        case .int(let v):
            try container.encode(v)
        case .double(let v):
            try container.encode(v)
        case .bool(let v):
            try container.encode(v)
        case .string(let v):
            try container.encode(v)
        case .dictionary(let v):
            try container.encode(v)
        case .array(let v):
            try container.encode(v)
        }
    }
}

extension AnyCodableValue {
    public var anyValue: Any {
        switch self {
        case .string(let v):
            return v
        case .int(let v):
            return v
        case .double(let v):
            return v
        case .bool(let v):
            return v
        case .dictionary(let dict):
            return dict.mapValues { $0.anyValue }
        case .array(let arr):
            return arr.map { $0.anyValue }
        }
    }

    public func get<T>() -> T? {
        switch self {
        case .string(let v as T):
            return v
        case .int(let v as T):
            return v
        case .double(let v as T):
            return v
        case .bool(let v as T):
            return v
        case .dictionary(let dict as T):
            return dict
        case .array(let arr as T):
            return arr
        default: return nil
        }
    }

    public func dictionaryValue<ValueType>() -> [String: ValueType]? {
        if case .dictionary(let dict) = self {
            return dict.reduce(into: [String: ValueType]()) { result, pair in
                if let value = pair.value.anyValue as? ValueType {
                    result[pair.key] = value
                }
            }
        }
        return nil
    }

    public func arrayValue<ValueType>() -> [ValueType]? {
        if case .array(let arr) = self {
            return arr.compactMap { $0.anyValue as? ValueType }
        }
        return nil
    }
}
