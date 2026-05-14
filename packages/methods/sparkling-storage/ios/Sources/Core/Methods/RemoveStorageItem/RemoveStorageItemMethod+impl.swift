// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

extension RemoveStorageItemMethod {
    @objc public override func call(withParamModel paramModel: Any, completionHandler: CompletionHandlerProtocol) {
        guard let typedParamModel = paramModel as? RemoveStorageItemMethodParamModel else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "Invalid parameter model type"), result: nil)
            return
        }
        guard let storageService = DIProviderRegistry.provider.pipeShared().resolve(StorageService.self) else {
            handleNotImplemented { status, result in
                completionHandler.handleCompletion(status: status, result: result)
            }
            return
        }

        guard let key = typedParamModel.key, !key.isEmpty else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "The key should not be empty"), result: nil)
            return
        }

        storageService.removeObject(forKey: key)
        completionHandler.handleCompletion(status: MethodStatus.succeeded(), result: nil)
    }

}
