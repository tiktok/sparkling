// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import SparklingMethod
import XCTest

@testable import Sparkling_Media

class SPKMediaTest: XCTestCase {

    // MARK: - SPKChooseMediaMethod Tests

    func testChooseMediaMethodName() {
        let method = SPKChooseMediaMethod()
        XCTAssertEqual(method.methodName, "media.chooseMedia")
        XCTAssertEqual(SPKChooseMediaMethod.methodName(), "media.chooseMedia")
    }

    func testChooseMediaMethodModels() {
        let method = SPKChooseMediaMethod()
        XCTAssertTrue(method.paramsModelClass is SPKChooseMediaMethodParamModel.Type)
        XCTAssertTrue(method.resultModelClass is SPKChooseMediaMethodResultModel.Type)
    }

    func testChooseMediaParamModelJsonMapping() {
        let paramModel = SPKChooseMediaMethodParamModel()
        paramModel.mediaTypes = [1, 2]  // image and video
        paramModel.sourceType = 1  // album
        paramModel.cameraType = 1  // front
        paramModel.maxCount = 5
        paramModel.compressOption = 1  // both

        let keyPaths = type(of: paramModel).jsonKeyPathsByPropertyKey()
        XCTAssertNotNil(keyPaths["mediaTypes"], "mediaTypes key path should exist")
        XCTAssertNotNil(keyPaths["sourceType"], "sourceType key path should exist")
        XCTAssertNotNil(keyPaths["cameraType"], "cameraType key path should exist")
        XCTAssertNotNil(keyPaths["maxCount"], "maxCount key path should exist")
        XCTAssertNotNil(keyPaths["compressOption"], "compressOption key path should exist")
    }

    func testChooseMediaResultModelJsonMapping() {
        let resultModel = SPKChooseMediaMethodResultModel()

        let keyPaths = type(of: resultModel).jsonKeyPathsByPropertyKey()
        XCTAssertNotNil(keyPaths["tempFiles"], "tempFiles key path should exist")
    }

    func testChooseMediaTempFileModelJsonMapping() {
        let tempFileModel = SPKChooseMediaMethodResultTempFileModel()

        let keyPaths = type(of: tempFileModel).jsonKeyPathsByPropertyKey()
        XCTAssertNotNil(keyPaths["tempFilePath"], "tempFilePath key path should exist")
        XCTAssertNotNil(keyPaths["mediaType"], "mediaType key path should exist")
        XCTAssertNotNil(keyPaths["size"], "size key path should exist")
        XCTAssertNotNil(keyPaths["base64Data"], "base64Data key path should exist")
        XCTAssertNotNil(keyPaths["tempFileAbsolutePath"], "tempFileAbsolutePath key path should exist")
        XCTAssertNotNil(keyPaths["mimeType"], "mimeType key path should exist")
    }

    // MARK: - SPKUploadFileMethod Tests

    func testUploadFileMethodName() {
        let method = SPKUploadFileMethod()
        XCTAssertEqual(method.methodName, "media.uploadFile")
        XCTAssertEqual(SPKUploadFileMethod.methodName(), "media.uploadFile")
    }

    func testUploadFileMethodModels() {
        let method = SPKUploadFileMethod()
        XCTAssertTrue(method.paramsModelClass is SPKUploadFileMethodParamModel.Type)
        XCTAssertTrue(method.resultModelClass is SPKUploadFileMethodResultModel.Type)
    }

    func testUploadFileParamModelJsonMapping() {
        let paramModel = SPKUploadFileMethodParamModel()

        let keyPaths = type(of: paramModel).jsonKeyPathsByPropertyKey()
        XCTAssertNotNil(keyPaths["url"], "url key path should exist")
        XCTAssertNotNil(keyPaths["filePath"], "filePath key path should exist")
        XCTAssertNotNil(keyPaths["name"], "name key path should exist")
        XCTAssertNotNil(keyPaths["fileName"], "fileName key path should exist")
        XCTAssertNotNil(keyPaths["mimeType"], "mimeType key path should exist")
        XCTAssertNotNil(keyPaths["header"], "header key path should exist")
        XCTAssertNotNil(keyPaths["params"], "params key path should exist")
    }

    func testUploadFileResultModelJsonMapping() {
        let resultModel = SPKUploadFileMethodResultModel()

        let keyPaths = type(of: resultModel).jsonKeyPathsByPropertyKey()
        XCTAssertNotNil(keyPaths["clientCode"], "clientCode key path should exist")
        XCTAssertNotNil(keyPaths["httpCode"], "httpCode key path should exist")
        XCTAssertNotNil(keyPaths["header"], "header key path should exist")
        XCTAssertNotNil(keyPaths["responseData"], "responseData key path should exist")
    }

    // MARK: - SPKDownloadFileMethod Tests

    func testDownloadFileMethodName() {
        let method = SPKDownloadFileMethod()
        XCTAssertEqual(method.methodName, "media.downloadFile")
        XCTAssertEqual(SPKDownloadFileMethod.methodName(), "media.downloadFile")
    }

    func testDownloadFileMethodModels() {
        let method = SPKDownloadFileMethod()
        XCTAssertTrue(method.paramsModelClass is SPKDownloadFileMethodParamModel.Type)
        XCTAssertTrue(method.resultModelClass is SPKDownloadFileMethodResultModel.Type)
    }

    func testDownloadFileParamModelJsonMapping() {
        let paramModel = SPKDownloadFileMethodParamModel()

        let keyPaths = type(of: paramModel).jsonKeyPathsByPropertyKey()
        XCTAssertNotNil(keyPaths["url"], "url key path should exist")
        XCTAssertNotNil(keyPaths["extensions"], "extensions key path should exist")
        XCTAssertNotNil(keyPaths["header"], "header key path should exist")
        XCTAssertNotNil(keyPaths["params"], "params key path should exist")
        XCTAssertNotNil(keyPaths["saveToAlbum"], "saveToAlbum key path should exist")
    }

    func testDownloadFileResultModelJsonMapping() {
        let resultModel = SPKDownloadFileMethodResultModel()

        let keyPaths = type(of: resultModel).jsonKeyPathsByPropertyKey()
        XCTAssertNotNil(keyPaths["clientCode"], "clientCode key path should exist")
        XCTAssertNotNil(keyPaths["httpCode"], "httpCode key path should exist")
        XCTAssertNotNil(keyPaths["header"], "header key path should exist")
        XCTAssertNotNil(keyPaths["filePath"], "filePath key path should exist")
    }

    // MARK: - SPKSaveDataURLMethod Tests

    func testSaveDataURLMethodName() {
        let method = SPKSaveDataURLMethod()
        XCTAssertEqual(method.methodName, "media.saveDataURL")
        XCTAssertEqual(SPKSaveDataURLMethod.methodName(), "media.saveDataURL")
    }

    func testSaveDataURLMethodModels() {
        let method = SPKSaveDataURLMethod()
        XCTAssertTrue(method.paramsModelClass is SPKSaveDataURLMethodParamModel.Type)
        XCTAssertTrue(method.resultModelClass is SPKSaveDataURLMethodResultModel.Type)
    }

    func testSaveDataURLParamModelJsonMapping() {
        let paramModel = SPKSaveDataURLMethodParamModel()

        let keyPaths = type(of: paramModel).jsonKeyPathsByPropertyKey()
        XCTAssertNotNil(keyPaths["dataURL"], "dataURL key path should exist")
        XCTAssertNotNil(keyPaths["filename"], "filename key path should exist")
        XCTAssertNotNil(keyPaths["extensions"], "extensions key path should exist")
        XCTAssertNotNil(keyPaths["saveToAlbum"], "saveToAlbum key path should exist")
    }

    func testSaveDataURLResultModelJsonMapping() {
        let resultModel = SPKSaveDataURLMethodResultModel()

        let keyPaths = type(of: resultModel).jsonKeyPathsByPropertyKey()
        XCTAssertNotNil(keyPaths["filePath"], "filePath key path should exist")
    }

    // MARK: - SPKUploadImageMethod Tests

    func testUploadImageMethodName() {
        let method = SPKUploadImageMethod()
        XCTAssertEqual(method.methodName, "media.uploadImage")
        XCTAssertEqual(SPKUploadImageMethod.methodName(), "media.uploadImage")
    }

    func testUploadImageMethodModels() {
        let method = SPKUploadImageMethod()
        XCTAssertTrue(method.paramsModelClass is SPKUploadImageMethodParamModel.Type)
        XCTAssertTrue(method.resultModelClass is SPKUploadImageMethodResultModel.Type)
    }

    func testUploadImageParamModelJsonMapping() {
        let paramModel = SPKUploadImageMethodParamModel()

        let keyPaths = type(of: paramModel).jsonKeyPathsByPropertyKey()
        XCTAssertNotNil(keyPaths["url"], "url key path should exist")
        XCTAssertNotNil(keyPaths["filePath"], "filePath key path should exist")
        XCTAssertNotNil(keyPaths["name"], "name key path should exist")
        XCTAssertNotNil(keyPaths["fileName"], "fileName key path should exist")
        XCTAssertNotNil(keyPaths["header"], "header key path should exist")
        XCTAssertNotNil(keyPaths["params"], "params key path should exist")
    }

    func testUploadImageResultModelJsonMapping() {
        let resultModel = SPKUploadImageMethodResultModel()

        let keyPaths = type(of: resultModel).jsonKeyPathsByPropertyKey()
        XCTAssertNotNil(keyPaths["clientCode"], "clientCode key path should exist")
        XCTAssertNotNil(keyPaths["httpCode"], "httpCode key path should exist")
        XCTAssertNotNil(keyPaths["url"], "url key path should exist")
        XCTAssertNotNil(keyPaths["uri"], "uri key path should exist")
        XCTAssertNotNil(keyPaths["header"], "header key path should exist")
        XCTAssertNotNil(keyPaths["responseData"], "responseData key path should exist")
    }

    // MARK: - SPKDefaultMediaPicker Tests

    func testMediaPickerSupportCheck() {
        let mediaPicker = SPKDefaultMediaPicker()
        let paramModel = SPKChooseMediaMethodParamModel()

        XCTAssertTrue(mediaPicker.supported(with: paramModel), "Default media picker should support all param models")
    }

    func testMediaTypesEnumerations() {
        // Test media type values
        XCTAssertEqual(SPKChooseMediaMediaType.image.rawValue, 1)
        XCTAssertEqual(SPKChooseMediaMediaType.video.rawValue, 2)

        // Test source type values
        XCTAssertEqual(SPKChooseMediaMediaSourceType.album.rawValue, 1)
        XCTAssertEqual(SPKChooseMediaMediaSourceType.camera.rawValue, 2)
        XCTAssertEqual(SPKChooseMediaMediaSourceType.unknown.rawValue, 0)

        // Test camera type values
        XCTAssertEqual(SPKChooseMediaCameraType.front.rawValue, 1)
        XCTAssertEqual(SPKChooseMediaCameraType.back.rawValue, 2)

        // Test compress option values
        XCTAssertEqual(SPKChooseMediaCompressOption.default.rawValue, 0)
        XCTAssertEqual(SPKChooseMediaCompressOption.both.rawValue, 1)
        XCTAssertEqual(SPKChooseMediaCompressOption.onlyBase64.rawValue, 2)
        XCTAssertEqual(SPKChooseMediaCompressOption.onlyImage.rawValue, 3)
        XCTAssertEqual(SPKChooseMediaCompressOption.none.rawValue, 4)

        // Test permission deny action values
        XCTAssertEqual(SPKChooseMediaPermissionDenyAction.default.rawValue, 0)
        XCTAssertEqual(SPKChooseMediaPermissionDenyAction.noAlert.rawValue, 1)
    }
}
