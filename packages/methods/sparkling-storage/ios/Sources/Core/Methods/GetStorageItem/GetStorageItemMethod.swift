// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

@objc(GetStorageItemMethod)
public class GetStorageItemMethod: PipeMethod {

    public override var methodName: String {
        return "storage.getItem"
    }

    public override class func methodName() -> String {
        return "storage.getItem"
    }

    @objc public override var paramsModelClass: AnyClass {
        return GetStorageItemMethodParamModel.self
    }

    @objc public override var resultModelClass: AnyClass {
        return GetStorageItemMethodResultModel.self
    }

}

@objc(GetStorageItemMethodParamModel)
public class GetStorageItemMethodParamModel: SPKMethodModel {
    public override class func requiredKeyPaths() -> Set<String>? {
        return ["key"]
    }

    @objc public var key: String?

    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        return [
            "key": "key"
        ]
    }
}

@objc(GetStorageItemMethodResultModel)
public class GetStorageItemMethodResultModel: SPKMethodModel {
    public var data: AnyCodableValue?

    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        return [
            "data": "data"
        ]
    }

    public override func toDict() throws -> [String: Any]? {
        var dict = try super.toDict() ?? [:]
        if let data = self.data {
            dict["data"] = data.anyValue
        }
        return dict
    }
}
