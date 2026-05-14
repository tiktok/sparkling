// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

extension SetStorageItemMethod {
    @objc public override func call(withParamModel paramModel: Any, completionHandler: CompletionHandlerProtocol) {
        // Validate parameter model type
        guard let typedParamModel = paramModel as? SetStorageItemMethodParamModel else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "Invalid parameter model type"), result: nil)
            return
        }

        // Check if storage service is available
        guard let storageService = DIProviderRegistry.provider.pipeShared().resolve(StorageService.self) else {
            handleNotImplemented { status, result in
                completionHandler.handleCompletion(status: status, result: result)
            }
            return
        }

        // Validate key is not nil or empty
        guard let key = typedParamModel.key, !key.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "The key must be a non-empty string"), result: nil)
            return
        }

        // Validate data is provided
        guard let data = typedParamModel.data else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "The data must not be nil"), result: nil)
            return
        }

        // Attempt to store the data
        do {
            storageService.setObject(key: key, value: data)
            completionHandler.handleCompletion(status: MethodStatus.succeeded(), result: nil)
        } catch {
            completionHandler.handleCompletion(status: .failed(message: "Failed to store data: \(error.localizedDescription)"), result: nil)
        }
    }
}
