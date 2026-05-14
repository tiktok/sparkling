// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

extension MethodPipe {
    public convenience init(withNothing nothing: String? = nil) {
        self.init()
    }

    /// Async/await version of `debugExecuteMethod`.
    /// Wraps the completion-based API with a checked continuation.
    /// - Note: Available on iOS 13+.
    /// - Parameters:
    ///   - methodName: The method to execute.
    ///   - params: Optional parameters.
    ///   - thread: Target thread to run the method.
    /// - Returns: A tuple `(status, result)`.
    @available(iOS 13.0, *)
    public func debugExecuteMethod(
        methodName: String,
        params: [String: Any]?,
        thread: MethodThread = .mainThread
    ) async -> (MethodStatus, [String: Any]?) {
        await withCheckedContinuation { continuation in
            var resumed = false
            self.executeMethod(methodName: methodName, params: params, thread: thread) { status, result in
                if !resumed {
                    resumed = true
                    continuation.resume(returning: (status, result))
                } else {
                    fatalError("method completed twice!")
                }
            }
        }
    }
}
