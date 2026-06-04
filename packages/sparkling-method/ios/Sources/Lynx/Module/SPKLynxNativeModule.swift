// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Lynx

@objcMembers
public class SPKLynxNativeModule: NSObject, LynxModule {
    public static var name: String { return "spkPipe" }
    public static var methodLookup: [String: String] {
        let sel = NSStringFromSelector(#selector(call(name:params:callback:)))
        return ["call": sel]
    }

    private var containerID: String?

    public required init(param: Any) {
        if let param = param as? [String: Any], let containerID = param["containerID"] as? String {
            self.containerID = containerID
        }
        super.init()
    }

    public override required init() {
        super.init()
    }

    public func call(name: String, params: [String: Any], callback: LynxCallbackBlock?) {
        let message = LynxRecvMessage(methodName: name, rawData: params)
        guard let containerID = message.containerID?.isEmpty == false ?  message.containerID : containerID else {
            let result = LynxSendMessage.paramsErrorMessage(
                with: message.containerID,
                errorMsg: "missing container id"
            ).toDict()
            SparklingMethodInvocationCenter.shared.notifyCompletedInvocation(
                name: name,
                namespace: "spkPipe",
                platform: "lynx",
                params: params,
                result: result,
                statusCode: LynxPipeStatusCode.parameterError.rawValue,
                statusMessage: "missing container id"
            )
            callback?(result)
            return
        }
        guard let engine = LynxPipeEnginePool.engine(for: containerID) else {
            var errorSendMsg = LynxSendMessage.paramsErrorMessage(with: containerID, errorMsg: "error container id")
            errorSendMsg.recvMessage = message
            let result = errorSendMsg.toDict()
            SparklingMethodInvocationCenter.shared.notifyCompletedInvocation(
                name: name,
                namespace: "spkPipe",
                platform: "lynx",
                params: params,
                result: result,
                statusCode: LynxPipeStatusCode.parameterError.rawValue,
                statusMessage: "error container id"
            )
            callback?(result)
            return
        }

        engine.executeMethod(with: message, completion: callback)
    }
}
