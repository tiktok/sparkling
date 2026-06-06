// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import SparklingMethod
import UIKit
import MobileCoreServices
import XCTest
@testable import Sparkling_Media

private class MediaCompletionRecorder: NSObject, PipeMethod.CompletionHandlerProtocol {
    var status: MethodStatus?
    var result: SPKMethodModel?

    func handleCompletion(status: MethodStatus, result: SPKMethodModel?) {
        self.status = status
        self.result = result
    }
}

class SPKMediaTest: XCTestCase {

    private func makeTestImage(size: CGSize = CGSize(width: 16, height: 16)) -> UIImage {
        UIGraphicsImageRenderer(size: size).image { context in
            UIColor.red.setFill()
            context.fill(CGRect(origin: .zero, size: size))
        }
    }

    private func tempFileURL(fileExtension: String, contents: Data = Data([1, 2, 3, 4])) -> URL {
        let fileName = "spk-media-\(UUID().uuidString).\(fileExtension)"
        let url = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent(fileName)
        try? contents.write(to: url, options: .atomic)
        return url
    }

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
        paramModel.mediaTypes = ["image", "video"]
        paramModel.sourceType = "album"
        paramModel.cameraType = "front"
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

    func testChooseMediaCallRejectsInvalidParametersSynchronously() {
        let method = SPKChooseMediaMethod()
        let invalidTypeRecorder = MediaCompletionRecorder()
        method.call(withParamModel: "invalid", completionHandler: invalidTypeRecorder)
        XCTAssertEqual(invalidTypeRecorder.status?.rawCode, MethodStatusCode.invalidInputParameter.rawValue)
        XCTAssertEqual(invalidTypeRecorder.status?.message, "Invalid parameter model type")

        let invalidSource = SPKChooseMediaMethodParamModel()
        invalidSource.sourceType = "invalid"
        let invalidSourceRecorder = MediaCompletionRecorder()
        method.call(withParamModel: invalidSource, completionHandler: invalidSourceRecorder)
        XCTAssertEqual(invalidSourceRecorder.status?.rawCode, MethodStatusCode.invalidInputParameter.rawValue)
        XCTAssertEqual(invalidSourceRecorder.status?.message, "Invalid source type")
    }

    func testDownloadFileCallRejectsMissingUrl() {
        let method = SPKDownloadFileMethod()

        let invalidTypeRecorder = MediaCompletionRecorder()
        method.call(withParamModel: ["url": "https://example.com/file"], completionHandler: invalidTypeRecorder)
        XCTAssertEqual(invalidTypeRecorder.status?.rawCode, MethodStatusCode.invalidInputParameter.rawValue)
        XCTAssertEqual(invalidTypeRecorder.status?.message, "Invalid parameter model type")

        let missingURL = SPKDownloadFileMethodParamModel()
        let missingURLRecorder = MediaCompletionRecorder()
        method.call(withParamModel: missingURL, completionHandler: missingURLRecorder)
        XCTAssertEqual(missingURLRecorder.status?.rawCode, MethodStatusCode.invalidInputParameter.rawValue)
        XCTAssertEqual(missingURLRecorder.status?.message, "The URL should not be empty.")
    }

    func testSaveDataURLCallValidationAndSuccess() {
        let method = SPKSaveDataURLMethod()

        let invalidTypeRecorder = MediaCompletionRecorder()
        method.call(withParamModel: 1, completionHandler: invalidTypeRecorder)
        XCTAssertEqual(invalidTypeRecorder.status?.rawCode, MethodStatusCode.invalidInputParameter.rawValue)
        XCTAssertEqual(invalidTypeRecorder.status?.message, "Invalid parameter model type")

        let invalidBase64 = SPKSaveDataURLMethodParamModel()
        invalidBase64.dataURL = "data:text/plain;base64,%%%"
        invalidBase64.filename = "invalid"
        invalidBase64.extensions = "txt"
        let invalidBase64Recorder = MediaCompletionRecorder()
        method.call(withParamModel: invalidBase64, completionHandler: invalidBase64Recorder)
        XCTAssertEqual(invalidBase64Recorder.status?.rawCode, MethodStatusCode.invalidInputParameter.rawValue)
        XCTAssertEqual(invalidBase64Recorder.status?.message, "Invalid base64 data in dataURL.")

        let fileName = "spk-media-\(UUID().uuidString)"
        let expectedPath = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent("\(fileName).txt")
        defer { try? FileManager.default.removeItem(at: expectedPath) }

        let success = SPKSaveDataURLMethodParamModel()
        success.dataURL = "data:text/plain;base64,\("hello".data(using: .utf8)!.base64EncodedString())"
        success.filename = fileName
        success.extensions = "txt"

        let successRecorder = MediaCompletionRecorder()
        method.call(withParamModel: success, completionHandler: successRecorder)
        XCTAssertEqual(successRecorder.status?.rawCode, MethodStatusCode.succeeded.rawValue)
        let result = successRecorder.result as? SPKSaveDataURLMethodResultModel
        XCTAssertNotNil(result?.filePath)
        XCTAssertTrue(FileManager.default.fileExists(atPath: expectedPath.path))
    }

    func testSaveDataURLCallRejectsMissingFieldsAndSupportsRawBase64() {
        let method = SPKSaveDataURLMethod()

        let missingDataURL = SPKSaveDataURLMethodParamModel()
        missingDataURL.filename = "missing-data"
        missingDataURL.extensions = "txt"
        let missingDataURLRecorder = MediaCompletionRecorder()
        method.call(withParamModel: missingDataURL, completionHandler: missingDataURLRecorder)
        XCTAssertEqual(missingDataURLRecorder.status?.message, "The dataURL should not be empty.")

        let missingFileName = SPKSaveDataURLMethodParamModel()
        missingFileName.dataURL = "aGVsbG8="
        missingFileName.extensions = "txt"
        let missingFileNameRecorder = MediaCompletionRecorder()
        method.call(withParamModel: missingFileName, completionHandler: missingFileNameRecorder)
        XCTAssertEqual(missingFileNameRecorder.status?.message, "The filename should not be empty.")

        let missingExtension = SPKSaveDataURLMethodParamModel()
        missingExtension.dataURL = "aGVsbG8="
        missingExtension.filename = "missing-extension"
        let missingExtensionRecorder = MediaCompletionRecorder()
        method.call(withParamModel: missingExtension, completionHandler: missingExtensionRecorder)
        XCTAssertEqual(missingExtensionRecorder.status?.message, "The extension should not be empty.")

        let rawBase64FileName = "spk-media-raw-\(UUID().uuidString)"
        let expectedPath = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent("\(rawBase64FileName).txt")
        defer { try? FileManager.default.removeItem(at: expectedPath) }

        let rawBase64 = SPKSaveDataURLMethodParamModel()
        rawBase64.dataURL = "raw".data(using: .utf8)!.base64EncodedString()
        rawBase64.filename = rawBase64FileName
        rawBase64.extensions = "txt"
        rawBase64.saveToAlbum = "unsupported"
        let rawBase64Recorder = MediaCompletionRecorder()
        method.call(withParamModel: rawBase64, completionHandler: rawBase64Recorder)
        XCTAssertEqual(rawBase64Recorder.status?.rawCode, MethodStatusCode.succeeded.rawValue)
        XCTAssertTrue(FileManager.default.fileExists(atPath: expectedPath.path))
    }

    func testUploadFileCallValidation() {
        let method = SPKUploadFileMethod()

        let invalidTypeRecorder = MediaCompletionRecorder()
        method.call(withParamModel: "invalid", completionHandler: invalidTypeRecorder)
        XCTAssertEqual(invalidTypeRecorder.status?.rawCode, MethodStatusCode.invalidInputParameter.rawValue)
        XCTAssertEqual(invalidTypeRecorder.status?.message, "Invalid parameter model type")

        let missingURL = SPKUploadFileMethodParamModel()
        missingURL.filePath = "/tmp/file.jpg"
        let missingURLRecorder = MediaCompletionRecorder()
        method.call(withParamModel: missingURL, completionHandler: missingURLRecorder)
        XCTAssertEqual(missingURLRecorder.status?.rawCode, MethodStatusCode.invalidInputParameter.rawValue)
        XCTAssertEqual(missingURLRecorder.status?.message, "The URL should not be empty.")

        let missingFilePath = SPKUploadFileMethodParamModel()
        missingFilePath.url = "https://example.com/upload"
        let missingFilePathRecorder = MediaCompletionRecorder()
        method.call(withParamModel: missingFilePath, completionHandler: missingFilePathRecorder)
        XCTAssertEqual(missingFilePathRecorder.status?.rawCode, MethodStatusCode.invalidInputParameter.rawValue)
        XCTAssertEqual(missingFilePathRecorder.status?.message, "The filePath should not be empty.")

        let nonExistent = SPKUploadFileMethodParamModel()
        nonExistent.url = "https://example.com/upload"
        nonExistent.filePath = "/non/existent/spk-media-file.jpg"
        let nonExistentRecorder = MediaCompletionRecorder()
        method.call(withParamModel: nonExistent, completionHandler: nonExistentRecorder)
        XCTAssertEqual(nonExistentRecorder.status?.rawCode, MethodStatusCode.invalidInputParameter.rawValue)
        XCTAssertEqual(nonExistentRecorder.status?.message, "The file does not exist at the specified path.")
    }

    func testUploadImageCallValidation() {
        let method = SPKUploadImageMethod()

        let invalidTypeRecorder = MediaCompletionRecorder()
        method.call(withParamModel: "invalid", completionHandler: invalidTypeRecorder)
        XCTAssertEqual(invalidTypeRecorder.status?.rawCode, MethodStatusCode.invalidInputParameter.rawValue)
        XCTAssertEqual(invalidTypeRecorder.status?.message, "Invalid parameter model type")

        let missingURL = SPKUploadImageMethodParamModel()
        missingURL.filePath = "/tmp/image.png"
        let missingURLRecorder = MediaCompletionRecorder()
        method.call(withParamModel: missingURL, completionHandler: missingURLRecorder)
        XCTAssertEqual(missingURLRecorder.status?.rawCode, MethodStatusCode.invalidInputParameter.rawValue)
        XCTAssertEqual(missingURLRecorder.status?.message, "The URL should not be empty.")

        let missingFilePath = SPKUploadImageMethodParamModel()
        missingFilePath.url = "https://example.com/upload"
        let missingFilePathRecorder = MediaCompletionRecorder()
        method.call(withParamModel: missingFilePath, completionHandler: missingFilePathRecorder)
        XCTAssertEqual(missingFilePathRecorder.status?.rawCode, MethodStatusCode.invalidInputParameter.rawValue)
        XCTAssertEqual(missingFilePathRecorder.status?.message, "The filePath should not be empty.")
    }

    func testUploadFileCompletionMappingAndMimeTypes() {
        let method = SPKUploadFileMethod()

        let successRecorder = MediaCompletionRecorder()
        method.finishUploadFile(
            httpCode: 201,
            header: ["X-Test": "ok"],
            responseData: ["uploaded": true],
            error: nil,
            completionHandler: successRecorder
        )
        XCTAssertEqual(successRecorder.status?.rawCode, MethodStatusCode.succeeded.rawValue)
        let successResult = successRecorder.result as? SPKUploadFileMethodResultModel
        XCTAssertEqual(successResult?.httpCode, 201)
        XCTAssertEqual(successResult?.header?["X-Test"], "ok")
        XCTAssertEqual(successResult?.responseData?["uploaded"] as? Bool, true)

        let malformedRecorder = MediaCompletionRecorder()
        method.finishUploadFile(httpCode: nil, header: nil, responseData: nil, error: nil, completionHandler: malformedRecorder)
        XCTAssertEqual(malformedRecorder.status?.rawCode, MethodStatusCode.failed.rawValue)
        XCTAssertEqual(malformedRecorder.status?.message, "The response returned from server is malformed.")

        let error = NSError(domain: "SPKMediaTest", code: 42, userInfo: [NSLocalizedDescriptionKey: "upload failed"])
        let errorRecorder = MediaCompletionRecorder()
        method.finishUploadFile(httpCode: 500, header: nil, responseData: nil, error: error, completionHandler: errorRecorder)
        XCTAssertEqual(errorRecorder.status?.message, "upload failed")
        XCTAssertEqual((errorRecorder.result as? SPKUploadFileMethodResultModel)?.clientCode, 42)

        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/photo.JPG")), "image/jpeg")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/photo.png")), "image/png")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/photo.gif")), "image/gif")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/photo.webp")), "image/webp")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/photo.heic")), "image/heic")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/photo.heif")), "image/heif")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/video.mp4")), "video/mp4")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/video.mov")), "video/quicktime")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/video.avi")), "video/x-msvideo")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/video.mkv")), "video/x-matroska")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/video.wmv")), "video/x-ms-wmv")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/audio.mp3")), "audio/mpeg")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/audio.wav")), "audio/wav")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/audio.aac")), "audio/aac")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/audio.m4a")), "audio/mp4")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/doc.pdf")), "application/pdf")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/doc.docx")), "application/msword")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/sheet.xlsx")), "application/vnd.ms-excel")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/slides.pptx")), "application/vnd.ms-powerpoint")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/readme.txt")), "text/plain")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/archive.zip")), "application/zip")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/archive.rar")), "application/x-rar-compressed")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/archive.7z")), "application/x-7z-compressed")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/data.json")), "application/json")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/data.xml")), "application/xml")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/index.html")), "text/html")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/styles.css")), "text/css")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/app.js")), "text/javascript")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/file.bin")), "application/octet-stream")
    }

    func testUploadImageCompletionMappingAndMimeTypes() {
        let method = SPKUploadImageMethod()

        let successRecorder = MediaCompletionRecorder()
        method.finishUploadImage(
            httpCode: 200,
            header: ["Content-Type": "application/json"],
            responseData: ["url": "https://example.com/image.jpg", "uri": "spk://image"],
            error: nil,
            completionHandler: successRecorder
        )
        XCTAssertEqual(successRecorder.status?.rawCode, MethodStatusCode.succeeded.rawValue)
        let successResult = successRecorder.result as? SPKUploadImageMethodResultModel
        XCTAssertEqual(successResult?.httpCode, 200)
        XCTAssertEqual(successResult?.url, "https://example.com/image.jpg")
        XCTAssertEqual(successResult?.uri, "spk://image")

        let malformedRecorder = MediaCompletionRecorder()
        method.finishUploadImage(httpCode: nil, header: nil, responseData: nil, error: nil, completionHandler: malformedRecorder)
        XCTAssertEqual(malformedRecorder.status?.message, "The response returned from server is malformed.")

        let error = NSError(domain: "SPKMediaTest", code: 7, userInfo: [NSLocalizedDescriptionKey: "image upload failed"])
        let errorRecorder = MediaCompletionRecorder()
        method.finishUploadImage(httpCode: 500, header: nil, responseData: nil, error: error, completionHandler: errorRecorder)
        XCTAssertEqual(errorRecorder.status?.message, "image upload failed")
        XCTAssertEqual((errorRecorder.result as? SPKUploadImageMethodResultModel)?.clientCode, 7)

        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/photo.jpeg")), "image/jpeg")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/photo.png")), "image/png")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/photo.gif")), "image/gif")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/photo.webp")), "image/webp")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/photo.heic")), "image/heic")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/photo.heif")), "image/heif")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/photo.bmp")), "image/bmp")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/photo.tif")), "image/tiff")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/photo.svg")), "image/svg+xml")
        XCTAssertEqual(method.guessMimeType(for: URL(fileURLWithPath: "/tmp/photo.unknown")), "image/jpeg")
    }

    func testDownloadFileCompletionMapping() {
        let method = SPKDownloadFileMethod()
        let downloadedFile = tempFileURL(fileExtension: "txt", contents: Data("download".utf8))
        defer { try? FileManager.default.removeItem(at: downloadedFile) }

        let successRecorder = MediaCompletionRecorder()
        method.finishDownloadFile(
            httpCode: 206,
            header: ["Accept-Ranges": "bytes"],
            fileURL: downloadedFile,
            error: nil,
            completionHandler: successRecorder
        )
        XCTAssertEqual(successRecorder.status?.rawCode, MethodStatusCode.succeeded.rawValue)
        let successResult = successRecorder.result as? SPKDownloadFileMethodResultModel
        XCTAssertEqual(successResult?.httpCode, 206)
        XCTAssertEqual(successResult?.header?["Accept-Ranges"], "bytes")
        XCTAssertNotNil(successResult?.filePath)

        let malformedRecorder = MediaCompletionRecorder()
        method.finishDownloadFile(httpCode: nil, header: nil, fileURL: downloadedFile, error: nil, completionHandler: malformedRecorder)
        XCTAssertEqual(malformedRecorder.status?.message, "The response returned from server is malformed.")

        let error = NSError(domain: "SPKMediaTest", code: 9, userInfo: [NSLocalizedDescriptionKey: "download failed"])
        let errorRecorder = MediaCompletionRecorder()
        method.finishDownloadFile(httpCode: 404, header: nil, fileURL: downloadedFile, error: error, completionHandler: errorRecorder)
        XCTAssertEqual(errorRecorder.status?.message, "download failed")
        XCTAssertEqual((errorRecorder.result as? SPKDownloadFileMethodResultModel)?.clientCode, 9)
    }

    func testDefaultMediaPickerAlbumPickerAndCancelFlow() {
        let picker = SPKDefaultMediaPicker()
        let paramModel = SPKChooseMediaMethodParamModel()
        paramModel.sourceType = "album"
        paramModel.mediaTypes = ["image", "video"]

        var completedStatus: SPKStatus?
        let controller = picker.mediaPicker(with: paramModel) { _, status in
            completedStatus = status
        }

        if let imagePicker = controller as? UIImagePickerController {
            XCTAssertEqual(imagePicker.sourceType, .photoLibrary)
            XCTAssertTrue(imagePicker.mediaTypes.contains(kUTTypeImage as String))
            XCTAssertTrue(imagePicker.mediaTypes.contains(kUTTypeMovie as String))

            picker.imagePickerControllerDidCancel(imagePicker)
            XCTAssertEqual(completedStatus?.code, SPKStatusCode.operationCancelled)
        } else {
            XCTAssertEqual(completedStatus?.code, SPKStatusCode.invalidParameter)
        }
    }

    func testDefaultMediaPickerImageDataHelpers() {
        let picker = SPKDefaultMediaPicker()
        let paramModel = SPKChooseMediaMethodParamModel()
        paramModel.sourceType = "album"
        paramModel.compressOption = SPKChooseMediaCompressOption.both.rawValue
        paramModel.compressWidth = 8
        paramModel.compressHeight = 8
        paramModel.compressionQuality = 0.5
        _ = picker.mediaPicker(with: paramModel) { _, _ in }

        let image = UIGraphicsImageRenderer(size: CGSize(width: 16, height: 16)).image { context in
            UIColor.red.setFill()
            context.fill(CGRect(x: 0, y: 0, width: 16, height: 16))
        }

        let imageData = picker.imageDataForImage(image)
        XCTAssertNotNil(imageData)

        let path = picker.writeImageDataToDisk(imageData ?? Data())
        XCTAssertNotNil(path)
        if let path = path {
            XCTAssertTrue(FileManager.default.fileExists(atPath: path))
            try? FileManager.default.removeItem(atPath: path)
        }
    }

    func testDefaultMediaPickerFinishesImageSelection() {
        let picker = SPKDefaultMediaPicker()
        let paramModel = SPKChooseMediaMethodParamModel()
        paramModel.sourceType = "album"
        paramModel.mediaTypes = ["image"]
        paramModel.compressOption = SPKChooseMediaCompressOption.none.rawValue
        paramModel.needBase64Data = true

        var resultModel: SPKChooseMediaMethodResultModel?
        var completedStatus: SPKStatus?
        guard let imagePicker = picker.mediaPicker(with: paramModel, completionHandler: { result, status in
            resultModel = result
            completedStatus = status
        }) as? UIImagePickerController else {
            XCTFail("Expected a photo library picker")
            return
        }

        picker.imagePickerController(imagePicker, didFinishPickingMediaWithInfo: [
            .mediaType: kUTTypeImage as String,
            .originalImage: makeTestImage()
        ])

        XCTAssertNil(completedStatus)
        let tempFile = resultModel?.tempFiles?.first
        XCTAssertEqual(tempFile?.mediaType, SPKChooseMediaMediaType.image.rawValue)
        XCTAssertEqual(tempFile?.mimeType, "image/jpeg")
        XCTAssertGreaterThan(tempFile?.size ?? 0, 0)
        XCTAssertNotNil(tempFile?.base64Data)
        if let path = tempFile?.tempFileAbsolutePath {
            try? FileManager.default.removeItem(atPath: path)
        }
    }

    func testDefaultMediaPickerFinishesVideoSelection() {
        let picker = SPKDefaultMediaPicker()
        let paramModel = SPKChooseMediaMethodParamModel()
        paramModel.sourceType = "album"
        paramModel.mediaTypes = ["video"]
        let videoURL = tempFileURL(fileExtension: "mov", contents: Data([0, 1, 2, 3, 4, 5]))
        defer { try? FileManager.default.removeItem(at: videoURL) }

        var resultModel: SPKChooseMediaMethodResultModel?
        var completedStatus: SPKStatus?
        guard let imagePicker = picker.mediaPicker(with: paramModel, completionHandler: { result, status in
            resultModel = result
            completedStatus = status
        }) as? UIImagePickerController else {
            XCTFail("Expected a photo library picker")
            return
        }

        picker.imagePickerController(imagePicker, didFinishPickingMediaWithInfo: [
            .mediaType: kUTTypeMovie as String,
            .mediaURL: videoURL
        ])

        XCTAssertNil(completedStatus)
        let tempFile = resultModel?.tempFiles?.first
        XCTAssertEqual(tempFile?.mediaType, SPKChooseMediaMediaType.video.rawValue)
        XCTAssertEqual(tempFile?.size, 6)
        XCTAssertNotNil(tempFile?.tempFilePath)
    }

    func testDefaultMediaPickerReportsInvalidMediaSelection() {
        let picker = SPKDefaultMediaPicker()
        let paramModel = SPKChooseMediaMethodParamModel()
        paramModel.sourceType = "album"
        paramModel.mediaTypes = ["image"]

        var completedStatus: SPKStatus?
        guard let imagePicker = picker.mediaPicker(with: paramModel, completionHandler: { _, status in
            completedStatus = status
        }) as? UIImagePickerController else {
            XCTFail("Expected a photo library picker")
            return
        }

        picker.imagePickerController(imagePicker, didFinishPickingMediaWithInfo: [
            .mediaType: kUTTypeImage as String
        ])
        XCTAssertEqual(completedStatus?.code, SPKStatusCode.invalidResult)
        XCTAssertEqual(completedStatus?.message, "The image is nil when taking from camera.")

        _ = picker.mediaPicker(with: paramModel, completionHandler: { _, status in
            completedStatus = status
        })
        picker.imagePickerController(imagePicker, didFinishPickingMediaWithInfo: [
            .mediaType: kUTTypeAudio as String
        ])
        XCTAssertEqual(completedStatus?.code, SPKStatusCode.invalidResult)
        XCTAssertEqual(completedStatus?.message, "Unknown media type: \(kUTTypeAudio as String)")
    }
}
