// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// Sample method implementation extension
@objc extension SampleMethod {
    /// Implement the main call method
    @objc override func call(withParamModel paramModel: Any, completionHandler: @escaping PipeMethod.CompletionBlock) {
        // Parameter type checking
        guard let params = paramModel as? SampleMethodParamModel else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "Invalid parameter model type"), result: nil)
            return
        }

        // Simulate processing logic
        let result = SampleMethodResultModel()
        result.sampleResult = "Processed: \(params.sampleParam ?? "")"

        // Return success result
        completionHandler.handleCompletion(status: MethodStatus.succeeded(), result: result)
    }
}
