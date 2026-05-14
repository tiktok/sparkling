// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

// Parameter model
@objc(SPKSaveDataURLMethodParamModel)
public class SPKSaveDataURLMethodParamModel: SPKMethodModel {
    @objc public override static func requiredKeyPaths() -> Set<String>? {
        return ["dataURL", "filename", "extension"]
    }

    @objc public var dataURL: String?
    @objc public var filename: String?
    @objc public var extensions: String?
    @objc public var saveToAlbum: String?

    @objc public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        return [
            "dataURL": "dataURL",
            "filename": "filename",
            "extensions": "extension",
            "saveToAlbum": "saveToAlbum",
        ]
    }
}

// Result model
@objc(SPKSaveDataURLMethodResultModel)
class SPKSaveDataURLMethodResultModel: SPKMethodModel {
    @objc public var filePath: String?

    @objc public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        return [
            "filePath": "filePath"
        ]
    }
}

// Main method class
@objc(SPKSaveDataURLMethod)
public class SPKSaveDataURLMethod: PipeMethod {
    @objc public override var paramsModelClass: AnyClass {
        return SPKSaveDataURLMethodParamModel.self
    }

    @objc public override var resultModelClass: AnyClass {
        return SPKSaveDataURLMethodResultModel.self
    }

    public override var methodName: String {
        return "media.saveDataURL"
    }

    public override class func methodName() -> String {
        return "media.saveDataURL"
    }
}
