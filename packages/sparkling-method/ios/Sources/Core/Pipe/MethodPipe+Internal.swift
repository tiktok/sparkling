// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

private enum MethodInvocationPayloadKeys {
    static let code = "code"
    static let data = "data"
    static let message = "message"
    static let msg = "msg"
    static let containerID = "containerID"
    static let protocolVersion = "protocolVersion"
}

private func methodInvocationStatusCode(_ status: MethodStatus) -> Int {
    status.code == .notFound ? MethodStatusCode.unregisteredMethod.rawValue : status.rawCode
}

extension MethodPipe {
    func executeMethod(methodName: String, params: [String: Any]?, thread: MethodThread = .mainThread, completion: CommonPipeCompletion?) {
        let invocationId = UUID().uuidString
        let invocationStart = Date()
        let invocationCenter = SparklingMethodInvocationCenter.shared
        let invocationNamespace: String? = "method-pipe"
        let callbackPayload: (MethodStatus, [String: Any]?) -> [String: Any] = { status, data in
            var payload: [String: Any] = [
                MethodInvocationPayloadKeys.code: methodInvocationStatusCode(status),
                MethodInvocationPayloadKeys.data: data ?? NSNull(),
                MethodInvocationPayloadKeys.protocolVersion: "1.1.0",
            ]
            payload[MethodInvocationPayloadKeys.msg] = status.message ?? NSNull()
            if let containerID = params?[MethodInvocationPayloadKeys.containerID] as? String {
                payload[MethodInvocationPayloadKeys.containerID] = containerID
            }
            return payload
        }

        invocationCenter.notifyStart(
            SparklingMethodInvocationEvent(
                id: invocationId,
                name: methodName,
                namespace: invocationNamespace,
                platform: "core",
                params: params,
                result: nil,
                statusCode: nil,
                statusMessage: nil,
                startTime: invocationStart,
                endTime: nil
            )
        )

        guard let method = self.method(forName: methodName) else {
            let status = MethodStatus.notFound()
            let notFound = SparklingMethodInvocationEvent(
                id: invocationId,
                name: methodName,
                namespace: invocationNamespace,
                platform: "core",
                params: params,
                result: callbackPayload(status, nil),
                statusCode: methodInvocationStatusCode(status),
                statusMessage: status.message ?? "notFound",
                startTime: invocationStart,
                endTime: Date()
            )
            invocationCenter.notifyEnd(notFound)
            completion?(status, nil)
            return
        }

        let resultModelType = method.resultModelClass
        let resultBlk: PipeMethod.CompletionBlock = { status, result in
            if let result = result, type(of: result) != EmptyMethodModel.self {
                let resultActualType = type(of: result)
                if resultModelType != resultActualType && resultModelType != EmptyMethodModel.self {
                    completion?(.resultModelTypeWrong(message: "\(type(of: method).methodName()).Result type mismatch: expected \(resultModelType),got \(resultActualType)"), nil)
                    return
                }
            }

            var fStatus = status
            var resultDict: [String: Any] = [:]
            do {
                if let result2Dict = try result?.toDict() {
                    resultDict = result2Dict
                }
            } catch {
                fStatus = .invalidResult(message: error.localizedDescription)
            }

            resultDict[DictKeys.statusMessage] = fStatus.message
            let callbackResult = callbackPayload(fStatus, resultDict)

            invocationCenter.notifyEnd(
                SparklingMethodInvocationEvent(
                    id: invocationId,
                    name: methodName,
                    namespace: invocationNamespace,
                    platform: "core",
                    params: params,
                    result: callbackResult,
                    statusCode: methodInvocationStatusCode(fStatus),
                    statusMessage: fStatus.message,
                    startTime: invocationStart,
                    endTime: Date()
                )
            )

            completion?(fStatus, resultDict)
        }

        // Notify observers of an early-out failure (mirrors `resultBlk`).
        let notifyEarlyFailure: (String) -> Void = { message in
            let status = MethodStatus.invalidParameter(message: message)
            invocationCenter.notifyEnd(
                SparklingMethodInvocationEvent(
                    id: invocationId,
                    name: methodName,
                    namespace: invocationNamespace,
                    platform: "core",
                    params: params,
                    result: callbackPayload(status, [MethodInvocationPayloadKeys.message: message]),
                    statusCode: methodInvocationStatusCode(status),
                    statusMessage: message,
                    startTime: invocationStart,
                    endTime: Date()
                )
            )
        }

        let paramsModelType = method.paramsModelClass
        guard var params = params else {
            notifyEarlyFailure("Pipe inner error: empty params")
            completion?(.invalidParameter(message: "Pipe inner error: empty params"), nil)
            return
        }

        // Safely get requiredKeyPaths
        if let methodModelType = paramsModelType as? MethodModel.Type,
            let requiredKeyPaths = methodModelType.requiredKeyPaths
        {
            let paramsKeys: Set<String> = Set(params.keys)
            let missingKeys = requiredKeyPaths.subtracting(paramsKeys).sorted().joined(separator: ", ")
            if missingKeys.count > 0 {
                notifyEarlyFailure("Missing required parameter(s): \(missingKeys)")
                completion?(.invalidParameter(message: "Missing required parameter(s): \(missingKeys)"), nil)
                return
            }
        }

        var paramModel: MethodModel?
        do {
            if let methodModelType = paramsModelType as? MethodModel.Type {
                paramModel = try methodModelType.from(dict: params)
            } else {
                throw NSError(domain: "MethodPipe", code: -1, userInfo: [NSLocalizedDescriptionKey: "Invalid params model type: \(paramsModelType)"])
            }
        } catch {
            notifyEarlyFailure(error.localizedDescription)
            completion?(.invalidParameter(message: error.localizedDescription), nil)
            return
        }
        guard var paramModel = paramModel else {
            notifyEarlyFailure("Param model conversion failed")
            completion?(.invalidParameter(message: "Param model conversion failed: \(type(of: paramsModelType)) from dict: \(params)"), nil)
            return
        }
        paramModel.context = {
            let context = MethodContext()
            context.pipeContainer = self.engine?.pipeContainer
            return context
        }()

        let invokeBlk: () -> Void = {
            method.invokeErased(withParams: paramModel, completion: resultBlk)
        }
        switch thread {
        case .mainThread:
            CoreUtils.onMain {
                invokeBlk()
            }
        case .currentThread:
            invokeBlk()
        }
    }
}
