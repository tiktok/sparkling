// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Lynx

extension MethodPipe {
    private static let setupOnce: Void = {
        let service = LynxServices.getInstanceWith(LynxServiceModuleProtocol.self)
        if (service as? LynxServiceModuleProtocol) == nil {
            LynxServices.registerService(withProtocol: SPKLynxModuleService.self, protocol: LynxServiceModuleProtocol.self)
        }
    }()

    public convenience init(withLynxView lynxView: LynxView) {
        let _ = Self.setupOnce

        self.init()
        let lynxEngine = LynxPipeEngine(withLynxView: lynxView, executor: self)
        self.engine = lynxEngine
        LynxPipeEnginePool.setEngine(engine: lynxEngine, containerID: lynxView.containerID)
    }

    public class func setupLynxPipe(config: LynxConfig) {
        let _ = Self.setupOnce

        config.register(
            SPKLynxNativeModule.self,
            param: [
                LynxKeys.containerID: config.spk_containerID ?? "",
                LynxKeys.namespace: config.spk_namescope ?? "",
            ])
    }
}

extension MethodPipe: LynxPipeEngineExecutor {
    func executeMethod(with recvMessage: LynxRecvMessage, onEngine engine: LynxPipeEngine, completion: LynxPipeCompletion?) {
        let params = recvMessage.data as? [String: Any] ?? [:]
        var thread = MethodThread.mainThread
        if let pageThreaddMode = params[LynxKeys.threadType] as? String {
            if pageThreaddMode == LynxKeys.mainThread {
                thread = .mainThread
            } else if pageThreaddMode == LynxKeys.currentThread {
                thread = .currentThread
            }
        }
        if engine.lynxView?.spk_isLynxViewDestorying == true {
            thread = .mainThread
        }
        executeMethod(methodName: recvMessage.methodName, params: params, thread: thread) { status, response in
            completion?(LynxPipeStatusCode(status), response)
        }
    }
}
