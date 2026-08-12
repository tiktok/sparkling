// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

@objc(SPKSetThemePreferenceMethod)
public final class SPKSetThemePreferenceMethod: PipeMethod {
    public override var methodName: String {
        Self.methodName()
    }

    public override class func methodName() -> String {
        "sparkling.setThemePreference"
    }

    @objc public override var paramsModelClass: AnyClass {
        SPKSetThemePreferenceParamModel.self
    }

    @objc public override var resultModelClass: AnyClass {
        SPKSetThemePreferenceResultModel.self
    }

    @objc public override func call(withParamModel paramModel: Any, completionHandler: CompletionHandlerProtocol) {
        guard let paramModel = paramModel as? SPKSetThemePreferenceParamModel else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "Invalid parameter model type"), result: nil)
            return
        }
        guard let preference = SPKThemePreference(value: paramModel.preference) else {
            completionHandler.handleCompletion(
                status: .invalidParameter(message: "preference must be follow-system, light, or dark"),
                result: nil)
            return
        }

        SPKThemePreferenceManager.shared.setPreference(preference)
        let result = SPKSetThemePreferenceResultModel()
        result.preference = preference.rawValue
        completionHandler.handleCompletion(status: .succeeded(), result: result)
    }
}

@objc(SPKSetThemePreferenceParamModel)
public final class SPKSetThemePreferenceParamModel: SPKMethodModel {
    @objc public var preference: String?

    public override class func requiredKeyPaths() -> Set<String>? {
        ["preference"]
    }

    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        ["preference": "preference"]
    }
}

@objc(SPKSetThemePreferenceResultModel)
public final class SPKSetThemePreferenceResultModel: SPKMethodModel {
    @objc public var preference: String?

    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        ["preference": "preference"]
    }
}
