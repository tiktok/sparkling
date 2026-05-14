// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

@objc(CloseMethod)
public class CloseMethod: PipeMethod {

    public override var methodName: String {
        return "router.close"
    }

    public override class func methodName() -> String {
        return "router.close"
    }

    @objc public override var paramsModelClass: AnyClass {
        return CloseMethodParamModel.self
    }

    @objc public override var resultModelClass: AnyClass {
        return EmptyMethodModelClass.self
    }

}

@objc(CloseMethodParamModel)
public class CloseMethodParamModel: SPKMethodModel {
    @objc public var containerID: String?
    @objc public var animated: Bool = false

    public override class func requiredKeyPaths() -> Set<String>? {
        return []
    }

    @objc public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        return [
            "containerID": "containerID",
            "animated": "animated",
        ]
    }
}
