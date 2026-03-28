// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

@objc(SetStorageItemMethod)
public class SetStorageItemMethod: PipeMethod {
    
    public override var methodName: String {
        return "storage.setItem"
    }
    
    public override class func methodName() -> String {
        return "storage.setItem"
    }
    
    @objc public override var paramsModelClass: AnyClass {
        return SetStorageItemMethodParamModel.self
    }
    
    @objc public override var resultModelClass: AnyClass {
        return EmptyMethodModelClass.self
    }


}

@objc(SetStorageItemMethodParamModel)
public class SetStorageItemMethodParamModel: SPKMethodModel {
    
    public override class func requiredKeyPaths() -> Set<String>? {
        [
            "key",
            "data"
        ]
    }
    
    @objc public var key: String?
    @objc public var data: AnyObject?
    
    @objc public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        return [
            "key": "key",
            "data": "data"
        ]
    }
}
