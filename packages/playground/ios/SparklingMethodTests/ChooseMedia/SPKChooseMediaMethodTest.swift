// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import AVFoundation
import Foundation
import Photos
import UIKit
import XCTest

@testable import Sparkling_Media
@testable import Sparkling_Router

class SPKChooseMediaMethodTest: XCTestCase {

    var chooseMediaMethod: SPKChooseMediaMethod!

    override func setUp() {
        super.setUp()
        chooseMediaMethod = SPKChooseMediaMethod()
    }

    override func tearDown() {
        chooseMediaMethod = nil
        super.tearDown()
    }

    func testMethodName() {
        XCTAssertEqual(SPKChooseMediaMethod.methodName(), "media.chooseMedia")
        XCTAssertEqual(chooseMediaMethod.methodName, "media.chooseMedia")
    }

    func testParamsModelClass() {
        XCTAssertEqual(
            String(describing: chooseMediaMethod.paramsModelClass),
            String(describing: SPKChooseMediaMethodParamModel.self)
        )
    }

    func testResultModelClass() {
        XCTAssertEqual(
            String(describing: chooseMediaMethod.resultModelClass),
            String(describing: SPKChooseMediaMethodResultModel.self)
        )
    }

    func testParamModelJsonMapping() {
        let paramModel = SPKChooseMediaMethodParamModel()
        paramModel.mediaTypes = ["image", "video"]
        paramModel.sourceType = "album"
        paramModel.cameraType = "front"
        paramModel.maxCount = 5
        paramModel.compressOption = 1

        let keyPaths = type(of: paramModel).jsonKeyPathsByPropertyKey()
        for key in ["mediaTypes", "sourceType", "cameraType", "maxCount", "compressOption"] {
            XCTAssertNotNil(keyPaths[key])
        }
    }

    func testResultModelJsonMapping() {
        let keyPaths = type(of: SPKChooseMediaMethodResultModel()).jsonKeyPathsByPropertyKey()
        XCTAssertNotNil(keyPaths["tempFiles"])
    }

    func testTempFileModelJsonMapping() {
        let keyPaths = type(of: SPKChooseMediaMethodResultTempFileModel()).jsonKeyPathsByPropertyKey()
        for key in ["tempFilePath", "mediaType", "size", "base64Data", "tempFileAbsolutePath", "mimeType"] {
            XCTAssertNotNil(keyPaths[key])
        }
    }

    func testPermissionMethodExistence() {
        XCTAssertTrue(SPKChooseMediaPermissionDenyAction.self is Any.Type)
        XCTAssertTrue(SPKChooseMediaMediaSourceType.self is Any.Type)
    }

    func testMediaTypesMapping() {
        let mediaPicker = SPKDefaultMediaPicker()
        let paramModel = SPKChooseMediaMethodParamModel()

        paramModel.mediaTypes = ["image"]
        XCTAssertNotNil(mediaPicker.mediaPicker(with: paramModel) { _, _ in })

        paramModel.mediaTypes = ["video"]
        XCTAssertNotNil(mediaPicker.mediaPicker(with: paramModel) { _, _ in })

        paramModel.mediaTypes = ["image", "video"]
        XCTAssertNotNil(mediaPicker.mediaPicker(with: paramModel) { _, _ in })
    }

    func testImageCompression() {
        let mediaPicker = SPKDefaultMediaPicker()
        let paramModel = SPKChooseMediaMethodParamModel()
        paramModel.compressOption = 1
        paramModel.compressWidth = 100
        paramModel.compressHeight = 100
        paramModel.compressionQuality = 0.5

        let setParamsSelector = NSSelectorFromString("params=")
        if mediaPicker.responds(to: setParamsSelector) {
            mediaPicker.perform(setParamsSelector, with: paramModel)

            let testImage = UIImage(systemName: "photo") ?? UIImage()
            let compressedDataSelector = NSSelectorFromString("imageDataForImage:")
            if mediaPicker.responds(to: compressedDataSelector) {
                let imageData = mediaPicker.perform(compressedDataSelector, with: testImage)?.takeRetainedValue() as? Data
                XCTAssertNotNil(imageData)
            }
        }
    }

    func testCameraTypeSetting() {
        let mediaPicker = SPKDefaultMediaPicker()
        let paramModel = SPKChooseMediaMethodParamModel()
        paramModel.sourceType = "camera"

        paramModel.cameraType = "front"
        XCTAssertNotNil(mediaPicker.mediaPicker(with: paramModel) { _, _ in })

        paramModel.cameraType = "back"
        XCTAssertNotNil(mediaPicker.mediaPicker(with: paramModel) { _, _ in })
    }
}
