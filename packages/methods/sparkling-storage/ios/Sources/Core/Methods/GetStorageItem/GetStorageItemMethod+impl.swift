// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

extension GetStorageItemMethod {
    @objc public override func call(withParamModel paramModel: Any, completionHandler: CompletionHandlerProtocol) {
        // Validate parameter model type
        guard let typedParamModel = paramModel as? GetStorageItemMethodParamModel else {
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

        // Retrieve data from storage
        let data = storageService.object(forKey: key)

        // Handle case where no data exists for the key (this is valid, return nil data)
        let resultModel = GetStorageItemMethodResultModel()

        if let data = data {
            // Try to wrap the data in AnyCodableValue
            if let anyCodableValue = AnyCodableValue(data) {
                resultModel.data = anyCodableValue
            } else {
                // Data exists but cannot be converted - still return success with nil data
                // This handles edge cases where stored data format is incompatible
                resultModel.data = nil
            }
        } else {
            // No data found for key - return success with nil data
            resultModel.data = nil
        }

        completionHandler.handleCompletion(status: .succeeded(), result: resultModel)
    }
}
