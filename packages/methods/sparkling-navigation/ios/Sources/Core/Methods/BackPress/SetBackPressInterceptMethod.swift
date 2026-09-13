// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

/// Let the page handle the hardware back button.
///
/// iOS has no hardware back button, so this answers `supported: false` rather
/// than failing: a page that asks is written for both platforms, and a failure
/// would make it look like something went wrong when nothing did. The back
/// *gesture* is the navigation controller's, and a page that wants to intervene
/// there does it by pushing its own container, not through this.
@objc(SetBackPressInterceptMethod)
public class SetBackPressInterceptMethod: PipeMethod {

    public override var methodName: String {
        return "router.setBackPressIntercept"
    }

    public override class func methodName() -> String {
        return "router.setBackPressIntercept"
    }

    @objc public override var paramsModelClass: AnyClass {
        return SetBackPressInterceptMethodParamModel.self
    }

    @objc public override var resultModelClass: AnyClass {
        return SetBackPressInterceptMethodResultModel.self
    }

}

@objc(SetBackPressInterceptMethodParamModel)
public class SetBackPressInterceptMethodParamModel: SPKMethodModel {
    @objc public var intercept: Bool = false
    @objc public var containerID: String?

    public override class func requiredKeyPaths() -> Set<String>? {
        return ["intercept"]
    }

    @objc public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        return [
            "intercept": "intercept",
            "containerID": "containerID",
        ]
    }
}

@objc(SetBackPressInterceptMethodResultModel)
public class SetBackPressInterceptMethodResultModel: SPKMethodModel {
    @objc public var supported: Bool = false

    @objc public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        return ["supported": "supported"]
    }
}
