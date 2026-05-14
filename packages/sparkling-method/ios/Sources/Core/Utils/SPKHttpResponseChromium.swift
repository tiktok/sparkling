// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// HTTP response Swift implementation
public class SPKHttpResponse: NSObject {
    public var allHeaderFields: [AnyHashable: Any]? = [:]

    init(allHeaderFields: [AnyHashable: Any]? = nil) {
        self.allHeaderFields = allHeaderFields
    }
}

/// Chromium-style HTTP response
public class SPKHttpResponseChromium: SPKHttpResponse {
    public var statusCode: Int = 200

    init(statusCode: Int = 200, allHeaderFields: [AnyHashable: Any]? = nil) {
        self.statusCode = statusCode
        super.init(allHeaderFields: allHeaderFields)
    }
}
