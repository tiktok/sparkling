// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

@objc(OpenMethod)
public class OpenMethod: PipeMethod {
    public override var methodName: String {
        return "router.open"
    }
    
    public override class func methodName() -> String {
        return "router.open"
    }
    
    @objc public override var paramsModelClass: AnyClass {
        return OpenMethodParamModel.self
    }
    
    @objc public override var resultModelClass: AnyClass {
        return EmptyMethodModelClass.self
    }


}

@objc(OpenMethodParamModel)
public class OpenMethodParamModel: SPKMethodModel {
    public override class func requiredKeyPaths() -> Set<String>? {
        return ["scheme"]
    }
    
    @objc public var scheme: String?
    @objc public var replace: Bool = false
    @objc public var replaceType: String?
    @objc public var useSysBrowser: Bool = false
    @objc public var animated: Bool = false
    @objc public var interceptor: String?
    @objc public var extra: NSDictionary?

    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        return [
            "scheme": "scheme",
            "replace": "replace",
            "replaceType": "replaceType",
            "useSysBrowser": "useSysBrowser",
            "animated": "animated",
            "interceptor": "interceptor",
            "extra": "extra",
        ]
    }
}
