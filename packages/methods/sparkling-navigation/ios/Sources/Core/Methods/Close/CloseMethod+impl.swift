// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

extension CloseMethod {
    @objc public override func call(withParamModel paramModel: Any, completionHandler: CompletionHandlerProtocol) {
        guard let typedParamModel = paramModel as? CloseMethodParamModel else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "Invalid parameter model type"), result: nil)
            return
        }
        guard let routerService = DIProviderRegistry.provider.pipeShared().resolve(RouterService.self) else {
            handleNotImplemented { status, result in
                completionHandler.handleCompletion(status: status, result: result)
            }
            return
        }

        routerService.closeContainer(withParams: typedParamModel) { status, result in
            completionHandler.handleCompletion(status: status, result: nil)
        }
    }

}
