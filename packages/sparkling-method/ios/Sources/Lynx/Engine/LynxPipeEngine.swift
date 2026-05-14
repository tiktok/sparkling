// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Lynx

protocol LynxPipeEngineExecutor: AnyObject {
    func executeMethod(with recvMessage: LynxRecvMessage, onEngine engine: LynxPipeEngine, completion: LynxPipeCompletion?)
}

class LynxPipeEngine: PipeEngine {
    private(set) weak var lynxView: LynxView?
    var namescope: String?
    weak var executor: LynxPipeEngineExecutor?

    var pipeContainer: PipeContainer? {
        return lynxView
    }

    required init?(withLynxView lynxView: LynxView, executor: LynxPipeEngineExecutor? = nil) {
        self.lynxView = lynxView
        self.executor = executor
    }

    func attch(to lynxView: LynxView) {
        self.lynxView = lynxView
    }

    func fireEvent(name: String, params: [String: Any]?) {
        guard let lynxView = self.lynxView else {
            return
        }
        var sendMsg = LynxSendMessage(containerID: self.lynxView?.containerID)
        sendMsg.data = params
        sendMsg.code = .succeeded
        lynxView.sendGlobalEvent(name, withParams: [sendMsg.toDict()])
    }
}

extension LynxPipeEngine {
    func executeMethod(with recvMessage: LynxRecvMessage, completion: LynxCallbackBlock?) {
        let containerID = recvMessage.containerID ?? lynxView?.containerID
        self.executor?.executeMethod(with: recvMessage, onEngine: self) { code, data in
            var sendMsg = LynxSendMessage(containerID: containerID)
            sendMsg.recvMessage = recvMessage
            sendMsg.code = code
            sendMsg.data = data
            completion?(sendMsg.toDict())
        }
    }
}
