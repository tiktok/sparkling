// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Sparkling
import SparklingMethod
import UIKit

@objc(CloseMethod)
public final class CloseMethod: SPKMethod {
    public override class func methodName() -> String? { "router.close" }
    public override var methodName: String? { Self.methodName() }
    public override var paramModelClass: (any SPKMethodModelProtocol.Type)? { CloseMethodParamModel.self }

    public override func invoke(withParamModel paramModel: SPKMethodModel?, completionHandler: SPKMethodCompletionHandler?) {
        if SPKRouter.close(container: paramModel?.spk_callingContainer) {
            completionHandler?(nil, nil)
        } else {
            let status = SPKMethodStatus(statusCode: .failed)
            status.message = "Unable to close the container"
            completionHandler?(nil, status)
        }
    }
}

@SPKGlobalMethod
extension CloseMethod {}

@objc(CloseMethodParamModel)
public final class CloseMethodParamModel: SPKMethodModel {
    // The original Playground router closes the calling page and ignores these options.
    @objc public var containerID: String?
    @objc public var animated = false

    public override class func requiredKeyPaths() -> Set<String>? { [] }
    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        ["containerID": "containerID", "animated": "animated"]
    }
}
