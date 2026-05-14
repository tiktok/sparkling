// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

@objc(RemoveStorageItemMethod)
public final class RemoveStorageItemMethod: PipeMethod {

    public override var methodName: String {
        return "storage.removeItem"
    }

    public override class func methodName() -> String {
        return "storage.removeItem"
    }

    @objc public override var paramsModelClass: AnyClass {
        return RemoveStorageItemMethodParamModel.self
    }

    @objc public override var resultModelClass: AnyClass {
        return EmptyMethodModelClass.self
    }

}

@objc(RemoveStorageItemMethodParamModel)
public class RemoveStorageItemMethodParamModel: SPKMethodModel {
    public override class func requiredKeyPaths() -> Set<String>? {
        return ["key"]
    }

    @objc public var key: String?

    @objc public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        var keyPaths = super.jsonKeyPathsByPropertyKey()
        keyPaths["key"] = "key"
        return keyPaths
    }
}
