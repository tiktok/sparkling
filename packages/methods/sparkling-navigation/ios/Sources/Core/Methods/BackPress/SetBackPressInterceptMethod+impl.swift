// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

extension SetBackPressInterceptMethod {
    @objc public override func call(withParamModel paramModel: Any, completionHandler: CompletionHandlerProtocol) {
        guard paramModel is SetBackPressInterceptMethodParamModel else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "Invalid parameter model type"), result: nil)
            return
        }

        // Succeeds and says no: there is nothing on iOS to intercept. A page
        // that checks `supported` keeps its own back affordance; one that does
        // not is no worse off than before it asked.
        let result = SetBackPressInterceptMethodResultModel()
        result.supported = false
        completionHandler.handleCompletion(status: .succeeded, result: result)
    }
}
