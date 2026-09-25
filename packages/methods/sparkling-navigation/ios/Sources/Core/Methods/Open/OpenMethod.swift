// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Sparkling
import SparklingMethod
import UIKit

@objc(OpenMethod)
public final class OpenMethod: SPKMethod {
    public override class func methodName() -> String? { "router.open" }
    public override var methodName: String? { Self.methodName() }
    public override var paramModelClass: (any SPKMethodModelProtocol.Type)? { OpenMethodParamModel.self }

    public override func invoke(withParamModel paramModel: SPKMethodModel?, completionHandler: SPKMethodCompletionHandler?) {
        guard let params = paramModel as? OpenMethodParamModel,
              let scheme = params.scheme, !scheme.isEmpty else {
            Self.complete(completionHandler, code: .invalidParameter, message: "The scheme should not be empty.")
            return
        }

        let context = SPKContext()
        if let extra = params.extra as? [String: Any] {
            context.extra = extra.mapValues { value in
                (value as? AnyHashable) ?? AnyHashable(String(describing: value))
            }
        }

        DispatchQueue.main.async {
            if params.useSysBrowser {
                let success = SPKRouter.openInSystemBrowser(withURL: scheme)
                Self.complete(completionHandler, code: success ? .succeeded : .failed,
                              message: success ? nil : "Failed to open URL in system browser")
                return
            }

            func openPage(afterOpen: ((Bool) -> Void)? = nil) {
                let success = SPKRouter.open(withURL: scheme, context: context)?.1 == true
                afterOpen?(success)
                Self.complete(completionHandler, code: success ? .succeeded : .failed,
                              message: success ? nil : "Failed to open URL")
            }

            if params.replace && params.replaceType == "alwaysCloseBeforeOpen" {
                _ = SPKRouter.close(container: params.spk_callingContainer)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { openPage() }
            } else if params.replace {
                openPage { success in
                    if params.replaceType == "alwaysCloseAfterOpen" ||
                        (params.replaceType == "onlyCloseAfterOpenSucceed" && success) {
                        _ = SPKRouter.close(container: params.spk_callingContainer)
                    }
                }
            } else {
                openPage()
            }
        }
    }

    private static func complete(_ completion: SPKMethodCompletionHandler?, code: SPKMethodStatusCode, message: String?) {
        guard let completion else { return }
        if code == .succeeded {
            completion(nil, nil)
        } else {
            let status = SPKMethodStatus(statusCode: code)
            status.message = message
            completion(nil, status)
        }
    }
}

@objc(OpenMethodParamModel)
public final class OpenMethodParamModel: SPKMethodModel {
    @objc public var scheme: String?
    @objc public var replace = false
    @objc public var replaceType: String?
    @objc public var useSysBrowser = false
    @objc public var extra: NSDictionary?
    // Accepted by the original iOS API; SPKRouter controls animation and interception.
    @objc public var animated = false
    @objc public var interceptor: String?

    public override class func requiredKeyPaths() -> Set<String>? { ["scheme"] }

    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        ["scheme": "scheme", "replace": "replace", "replaceType": "replaceType",
         "useSysBrowser": "useSysBrowser", "extra": "extra",
         "animated": "animated", "interceptor": "interceptor"]
    }
}

@SPKGlobalMethod
extension OpenMethod {}
