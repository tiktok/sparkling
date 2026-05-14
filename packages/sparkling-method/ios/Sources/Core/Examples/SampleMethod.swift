// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// Sample method parameter model
@objc(SampleMethodParamModel)
class SampleMethodParamModel: SPKMethodModel {
    var sampleParam: String?

    // Required parameter key paths
    @objc public static func requiredKeyPaths() -> Set<String>? {
        return ["sampleParam"]
    }

    override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        return ["sampleParam": "sample_param"]
    }
}

/// Sample method result model
@objc(SampleMethodResultModel)
public final class SampleMethodResultModel: SPKMethodModel {
    var sampleResult: String?

    // Required parameter key paths
    @objc public static func requiredKeyPaths() -> Set<String>? {
        return ["sampleResult"]
    }

    override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        return ["sampleResult": "sample_result"]
    }
}

/// Sample method implementation demonstrating how to inherit from PipeMethod
@objc(SampleMethod)
public final class SampleMethod: PipeMethod {
    /// Method name
    public override var methodName: String {
        return "sample_method"
    }

    public override class func methodName() -> String {
        return "sample_method"
    }

    /// Parameter model type
    @objc override var paramsModelType: AnyClass {
        return SampleMethodParamModel.self
    }

    /// Result model type
    @objc override var resultModelType: AnyClass {
        return SampleMethodResultModel.self
    }
}
