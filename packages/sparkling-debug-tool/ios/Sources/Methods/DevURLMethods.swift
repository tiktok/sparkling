// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

private let devURLErrorMessage = "The url must be a valid http:// or https:// URL"

private func normalizedDevURL(_ value: String?) -> String? {
    let normalized = value?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    guard
        !normalized.isEmpty,
        let components = URLComponents(string: normalized),
        let scheme = components.scheme?.lowercased(),
        scheme == "http" || scheme == "https",
        let host = components.host,
        !host.isEmpty
    else {
        return nil
    }
    return normalized
}

@objc(GetDevUrlMethod)
public final class GetDevUrlMethod: PipeMethod {
    public override var methodName: String {
        Self.methodName()
    }

    public override class func methodName() -> String {
        "debugtool.getDevUrl"
    }

    public override var paramsModelClass: AnyClass {
        EmptyMethodModelClass.self
    }

    public override var resultModelClass: AnyClass {
        GetDevUrlMethodResultModel.self
    }

    public override func call(
        withParamModel paramModel: Any,
        completionHandler: CompletionHandlerProtocol
    ) {
        guard paramModel is EmptyMethodModelClass else {
            completionHandler.handleCompletion(
                status: .invalidParameter(message: "Invalid parameter model type"),
                result: nil
            )
            return
        }

        let result = GetDevUrlMethodResultModel()
        result.url = SparklingDebugTool.devURL(fallback: "")
        completionHandler.handleCompletion(status: .succeeded(), result: result)
    }
}

@objc(GetDevUrlMethodResultModel)
public final class GetDevUrlMethodResultModel: SPKMethodModel {
    @objc public var url: String?

    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        [
            "url": "url"
        ]
    }
}

@objc(SetDevUrlMethod)
public final class SetDevUrlMethod: PipeMethod {
    public override var methodName: String {
        Self.methodName()
    }

    public override class func methodName() -> String {
        "debugtool.setDevUrl"
    }

    public override var paramsModelClass: AnyClass {
        SetDevUrlMethodParamModel.self
    }

    public override var resultModelClass: AnyClass {
        EmptyMethodModelClass.self
    }

    public override func call(
        withParamModel paramModel: Any,
        completionHandler: CompletionHandlerProtocol
    ) {
        guard let params = paramModel as? SetDevUrlMethodParamModel else {
            completionHandler.handleCompletion(
                status: .invalidParameter(message: "Invalid parameter model type"),
                result: nil
            )
            return
        }
        guard let url = normalizedDevURL(params.url) else {
            completionHandler.handleCompletion(
                status: .invalidParameter(message: devURLErrorMessage),
                result: nil
            )
            return
        }

        SparklingDebugTool.setDevURL(url)
        completionHandler.handleCompletion(status: .succeeded(), result: nil)
    }
}

@objc(SetDevUrlMethodParamModel)
public final class SetDevUrlMethodParamModel: SPKMethodModel {
    @objc public var url: String?

    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        [
            "url": "url"
        ]
    }
}
