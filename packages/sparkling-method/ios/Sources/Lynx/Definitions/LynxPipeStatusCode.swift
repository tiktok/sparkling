// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

public enum LynxPipeStatusCode: Int {
    case unknownError = -1000  // Unknown error
    case manualCallback = -999  // Callback from the business side
    case undefined = -998  // Frontend method not defined
    case code404 = -997  // Frontend returns 404
    case parameterError = -3  // Parameter error
    case noHandler = -2  // Method not registered
    case failed = 0  // Failure
    case succeeded = 1  // Success
}

extension LynxPipeStatusCode {
    init(_ methodStatus: MethodStatus) {
        if methodStatus.code == .notFound {
            self = .noHandler
        } else {
            self = LynxPipeStatusCode(rawValue: methodStatus.code.rawValue) ?? .unknownError
        }
    }
}
