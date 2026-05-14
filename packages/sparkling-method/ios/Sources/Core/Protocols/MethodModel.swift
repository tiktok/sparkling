// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Mantle

public protocol MethodModel: Codable, DictionaryCodable {
    static var requiredKeyPaths: Set<String>? { get }

    var context: MethodContext? { get set }
}

extension MethodModel {
    public func toDict() throws -> [String: Any]? {
        let data = try JSONEncoder().encode(self)
        let dict = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        return dict
    }

    public static func from(dict: [String: Any]) throws -> Self? {
        if JSONSerialization.isValidJSONObject(dict) {
            let data = try JSONSerialization.data(withJSONObject: dict)
            let obj = try JSONDecoder().decode(Self.self, from: data)
            return obj
        }
        throw NSError(
            domain: MethodStatus.Constants.ErrorDomain, code: -1,
            userInfo: [
                NSLocalizedDescriptionKey: "\(dict) is not a valid JSONObject"
            ])
        return nil
    }
}

extension MethodModel {
    public static var requiredKeyPaths: Set<String>? {
        return nil
    }
}

public struct EmptyMethodModel: MethodModel {
    public static var requiredKeyPaths: Set<String>? { return nil }

    public var extraInfo: Any?
    public var context: MethodContext?

    private enum CodingKeys: CodingKey {}

}

// Added Mantle-based MethodModel base class for both Objective-C and Swift
@objc(SPKMethodModel)
open class SPKMethodModel: MTLModel, MTLJSONSerializing, MethodModel {
    open var context: MethodContext?

    @objc open class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        return [:]
    }

    @objc open class func defaultValues() -> [AnyHashable: Any]? {
        return nil
    }

    open class func requiredKeyPaths() -> Set<String>? {
        []
    }

    // Implement DictionaryCodable protocol
    open func toDict() throws -> [String: Any]? {
        return self.dictionaryValue as? [String: Any]
    }

    public static func from(dict: [String: Any]) throws -> Self? {
        // throw a error when requiredKeyPaths is not nil and any of the keys are missing
        if let requiredKeyPaths = Self.requiredKeyPaths() {
            for key in requiredKeyPaths {
                if dict[key] == nil {
                    throw NSError(
                        domain: MethodStatus.Constants.ErrorDomain, code: -1,
                        userInfo: [
                            NSLocalizedDescriptionKey: "\(key) is required"
                        ])
                }
            }
        }
        // Using Swift's try-catch and Mantle's standard approach

        let adapter = MTLJSONAdapter(modelClass: self)
        do {
            let model = try adapter?.model(fromJSONDictionary: dict) as? Self
            return model
        } catch {
            throw error
        }
    }

    // Ensure compliance with Codable protocol
    public override required init() {
        super.init()
    }

    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }

    required public init(dictionary dictionaryValue: [AnyHashable: Any]!) throws {
        // throw a error when requiredKeyPaths is not nil and any of the keys are missing
        if let requiredKeyPaths = Self.requiredKeyPaths() {
            for key in requiredKeyPaths {
                if dictionaryValue[key] == nil {
                    throw NSError(
                        domain: MethodStatus.Constants.ErrorDomain, code: -1,
                        userInfo: [
                            NSLocalizedDescriptionKey: "\(key) is required"
                        ])
                }
            }
        }
        if let dict = dictionaryValue {
            try super.init(dictionary: dict)
        } else {
            super.init()
        }
    }

    // Implement Codable protocol methods
    public func encode(to encoder: Encoder) throws {
        // Basic implementation, subclasses can override
    }

    required public init(from decoder: Decoder) throws {
        super.init()
        // Basic implementation, subclasses can override
    }
}

// For compatibility, also provide a class version of EmptyMethodModel
@objc(EmptyMethodModelClass)
public class EmptyMethodModelClass: SPKMethodModel {
    @objc public var extraInfo: Any?

    @objc public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        var keyPaths = super.jsonKeyPathsByPropertyKey()
        keyPaths["extraInfo"] = "extraInfo"
        return keyPaths
    }
}
