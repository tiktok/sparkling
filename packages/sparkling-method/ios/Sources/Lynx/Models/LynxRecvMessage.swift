// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

struct LynxRecvMessage {
    private(set) var methodName: String
    private(set) var rawData: [String: Any]
    private(set) var protocolVersion: String
    private(set) var containerID: String?
    var namescope: String?
    private(set) var data: Any?
    private(set) var useUIThread: Bool = true

    init(methodName: String, rawData: [String: Any]) {
        self.methodName = methodName
        self.rawData = rawData

        self.data = rawData[LynxKeys.data]
        self.namescope = rawData[LynxKeys.namespace] as? String
        self.containerID = rawData[LynxKeys.containerID] as? String
        self.protocolVersion = rawData[LynxKeys.protocolVersion] as? String ?? "1.0.0"

        if let data = self.data as? [String: Any], let useUIThread = data[LynxKeys.useUIThread] as? Bool {
            self.useUIThread = useUIThread
        } else if let useUIThread = rawData[LynxKeys.useUIThread] as? Bool {
            self.useUIThread = useUIThread
        }
    }
}
