// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

struct LynxSendMessage {
    private(set) var containerID: String?
    private(set) var protocolVersion: String?
    private(set) var statusDesc: String?
    var code: LynxPipeStatusCode
    var data: Any?
    var recvMessage: LynxRecvMessage?

    static func succeededMessage(with containerID: String) -> LynxSendMessage {
        let message = LynxSendMessage(containerID: containerID, code: .succeeded)
        return message
    }

    static func paramsErrorMessage(with containerID: String?, errorMsg: String?) -> LynxSendMessage {
        var message = LynxSendMessage(containerID: containerID, code: .parameterError)
        message.data = [LynxKeys.message: errorMsg ?? ""]
        return message
    }

    static func noHandlerErrorMessage(with containerID: String?) -> LynxSendMessage {
        return LynxSendMessage(containerID: containerID, code: .noHandler)
    }

    init(containerID: String?, data: Any? = nil, code: LynxPipeStatusCode = .unknownError) {
        self.containerID = containerID
        self.data = data
        self.code = code
    }

    func toDict() -> [String: Any] {
        var dict: [String: Any] = [:]
        dict[LynxKeys.code] = code.rawValue
        dict[LynxKeys.msg] = statusDesc
        dict[LynxKeys.data] = data
        dict[LynxKeys.containerID] = containerID
        dict[LynxKeys.protocolVersion] = "1.1.0"
        return dict
    }

    func paramsErrorDict(errorMsg: String?) -> [String: Any] {
        let message = LynxSendMessage.paramsErrorMessage(with: containerID, errorMsg: errorMsg)
        return message.toDict()
    }

    func noHandlerMessageDict() -> [String: Any] {
        let message = LynxSendMessage.noHandlerErrorMessage(with: containerID)
        return message.toDict()
    }
}
