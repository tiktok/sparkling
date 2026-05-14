// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

extension MethodPipe {
    func executeMethod(methodName: String, params: [String: Any]?, thread: MethodThread = .mainThread, completion: CommonPipeCompletion?) {
        guard let method = self.method(forName: methodName) else {
            completion?(.notFound(), nil)
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
            completion?(fStatus, resultDict)
        }
        let paramsModelType = method.paramsModelClass
        guard var params = params else {
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
            completion?(.invalidParameter(message: error.localizedDescription), nil)
            return
        }
        guard var paramModel = paramModel else {
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
