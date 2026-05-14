// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

public protocol RouterService {
    func openScheme(withParams params: OpenMethodParamModel, completion: @escaping PipeMethod.CompletionBlock)
    func closeContainer(withParams params: CloseMethodParamModel, completion: @escaping PipeMethod.CompletionBlock)

}
