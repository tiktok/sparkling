// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

private enum SPKStorage {
    // Match the original open-source Playground's StorageServiceImpl.
    static let defaults = UserDefaults(suiteName: "com.SPK.custom.userdefault") ?? .standard

    static func set(_ value: Any, key: String) {
        defaults.set(value, forKey: key)
    }

    // Same supported values and filtering as the original AnyCodableValue initializer.
    static func resultValue(_ value: Any) -> Any? {
        switch value {
        case let v as String: return v
        case let v as Int: return v
        case let v as Double: return v
        case let v as Bool: return v
        case let v as [String: Any]: return v.compactMapValues { resultValue($0) }
        case let v as [Any]: return v.compactMap { resultValue($0) }
        default: return nil
        }
    }

    static func get(_ key: String) -> Any? { defaults.object(forKey: key) }
    static func remove(_ key: String) { defaults.removeObject(forKey: key) }
}

@objc(SPKStorageKeyModel)
public class SPKStorageKeyModel: SPKMethodModel {
    @objc public var key: String?

    public override class func requiredKeyPaths() -> Set<String>? { ["key"] }
    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        ["key": "key"]
    }
}

@objc(SPKStorageSetModel)
public final class SPKStorageSetModel: SPKStorageKeyModel {
    @objc public var data: Any?

    public override class func requiredKeyPaths() -> Set<String>? { ["key", "data"] }
    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        var paths = super.jsonKeyPathsByPropertyKey()
        paths["data"] = "data"
        return paths
    }
}

@objc(SPKStorageGetResult)
public final class SPKStorageGetResult: SPKMethodModel {
    @objc public var data: Any?

    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        ["data": "data"]
    }
}

private func storageError(_ code: SPKMethodStatusCode, _ message: String) -> SPKMethodStatus {
    let status = SPKMethodStatus(statusCode: code)
    status.message = message
    return status
}

private func validKey(_ key: String?) -> String? {
    guard let key, !key.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return nil }
    return key
}

@objc(SetStorageItemMethod)
public final class SetStorageItemMethod: SPKMethod {
    public override class func methodName() -> String? { "storage.setItem" }
    public override var methodName: String? { Self.methodName() }
    public override var paramModelClass: (any SPKMethodModelProtocol.Type)? { SPKStorageSetModel.self }

    public override func invoke(withParamModel paramModel: SPKMethodModel?, completionHandler: SPKMethodCompletionHandler?) {
        guard let params = paramModel as? SPKStorageSetModel,
              let key = validKey(params.key), let data = params.data else {
            completionHandler?(nil, storageError(.invalidParameter, "The key and data are required."))
            return
        }
        SPKStorage.set(data, key: key)
        completionHandler?(nil, nil)
    }
}

@SPKGlobalMethod
extension SetStorageItemMethod {}

@objc(GetStorageItemMethod)
public final class GetStorageItemMethod: SPKMethod {
    public override class func methodName() -> String? { "storage.getItem" }
    public override var methodName: String? { Self.methodName() }
    public override var paramModelClass: (any SPKMethodModelProtocol.Type)? { SPKStorageKeyModel.self }
    public override var resultModelClass: AnyClass? { SPKStorageGetResult.self }

    public override func invoke(withParamModel paramModel: SPKMethodModel?, completionHandler: SPKMethodCompletionHandler?) {
        guard let params = paramModel as? SPKStorageKeyModel, let key = validKey(params.key) else {
            completionHandler?(nil, storageError(.invalidParameter, "The key must be a non-empty string."))
            return
        }
        guard let result = SPKStorageGetResult() else {
            completionHandler?(nil, storageError(.failed, "Unable to create storage result."))
            return
        }
        result.data = SPKStorage.get(key).flatMap { SPKStorage.resultValue($0) }
        completionHandler?(result, nil)
    }
}

@SPKGlobalMethod
extension GetStorageItemMethod {}

@objc(RemoveStorageItemMethod)
public final class RemoveStorageItemMethod: SPKMethod {
    public override class func methodName() -> String? { "storage.removeItem" }
    public override var methodName: String? { Self.methodName() }
    public override var paramModelClass: (any SPKMethodModelProtocol.Type)? { SPKStorageKeyModel.self }

    public override func invoke(withParamModel paramModel: SPKMethodModel?, completionHandler: SPKMethodCompletionHandler?) {
        guard let params = paramModel as? SPKStorageKeyModel, let key = params.key, !key.isEmpty else {
            completionHandler?(nil, storageError(.invalidParameter, "The key should not be empty."))
            return
        }
        SPKStorage.remove(key)
        completionHandler?(nil, nil)
    }
}

@SPKGlobalMethod
extension RemoveStorageItemMethod {}
