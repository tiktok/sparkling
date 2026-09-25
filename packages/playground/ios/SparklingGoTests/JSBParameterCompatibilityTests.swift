// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Mantle
import Network
import SparklingMethod
@testable import Sparkling
import Sparkling_Router
import Sparkling_Storage
import UIKit
import XCTest
@testable import Sparkling_Media

/// Contracts taken from the open-source methods and Playground services at c4ce8d2.
final class JSBParameterCompatibilityTests: XCTestCase {
    private func decode<T: SPKMethodModel>(_ type: T.Type, _ json: [String: Any] = [:]) throws -> T {
        try XCTUnwrap(MTLJSONAdapter.model(of: type, fromJSONDictionary: json) as? T)
    }

    func testNavigationOptionsSurviveJSONParsing() throws {
        let open = try decode(OpenMethodParamModel.self, [
            "scheme": "hybrid://lynxview_page", "replace": true, "replaceType": "alwaysCloseAfterOpen",
            "useSysBrowser": true, "animated": true, "interceptor": "example", "extra": ["id": 12]
        ])
        XCTAssertEqual(open.scheme, "hybrid://lynxview_page")
        XCTAssertTrue(open.replace)
        XCTAssertEqual(open.replaceType, "alwaysCloseAfterOpen")
        XCTAssertTrue(open.useSysBrowser)
        XCTAssertTrue(open.animated)
        XCTAssertEqual(open.interceptor, "example")
        XCTAssertEqual(open.extra?["id"] as? Int, 12)
        let close = try decode(CloseMethodParamModel.self, ["containerID": "page", "animated": true])
        XCTAssertEqual(close.containerID, "page")
        XCTAssertTrue(close.animated)
        XCTAssertFalse(try decode(OpenMethodParamModel.self).animated)
        XCTAssertFalse(try decode(CloseMethodParamModel.self).animated)
    }

    func testOriginalHostThreadPolicy() {
        for currentThread in [false, true] {
            let done = expectation(description: "host thread callback")
            let runtime = SPKMethodRuntime()
            let method = SPKMethod(handler: { _, completion in
                XCTAssertEqual(Thread.isMainThread, !currentThread)
                completion?(nil, nil)
            })
            runtime.registerLocalMethod(method, forName: "test.thread")
            let handler = SPKMethodHostHandler(router: SPKMethodCallRouter(runtime: runtime))
            let message = SPKMethodCallMessage()
            message.methodName = "test.thread"
            message.engineType = .lynx
            message.params = currentThread ? ["threadType": "CURRENT_THREAD"] : [:]
            DispatchQueue.global().async {
                handler.handle(message) { _ in done.fulfill() }
            }
            wait(for: [done], timeout: 3)
        }
    }

    @MainActor
    func testCloseMatchesOriginalRouterForCoveredCallingContainer() throws {
        let first = UIViewController()
        let foreground = UIViewController()
        let navigation = UINavigationController()
        navigation.setViewControllers([first, foreground], animated: false)
        let view = SPKWrapperLynxView(withFrame: .zero, params: nil)
        first.view.addSubview(view)
        let runtime = try XCTUnwrap(view.methodRuntime)
        runtime.registerLocalMethod(CloseMethod())
        var completed = false
        let hooks = view.methodInvocationHooks()
        let injectContext = hooks.willInvoke
        hooks.willInvoke = { params, model in
            injectContext?(params, model)
            XCTAssertTrue(model?.spk_callingContainer === view)
        }
        runtime.invokeMethodNamed("router.close", params: [:], engineType: .lynx,
                                  hooks: hooks) { code, _, _ in
            XCTAssertEqual(code, .succeeded)
            completed = true
        }
        XCTAssertTrue(completed)
        // Compare with the original router instead of silently fixing its existing
        // isTopViewController bug as part of the framework migration.
        let baselineFirst = UIViewController()
        let baselineForeground = UIViewController()
        let baselineNavigation = UINavigationController()
        baselineNavigation.setViewControllers([baselineFirst, baselineForeground], animated: false)
        XCTAssertTrue(SPKRouter.close(container: baselineFirst.view))
        XCTAssertEqual(navigation.viewControllers.count, baselineNavigation.viewControllers.count)
        XCTAssertEqual(first.parent == nil, baselineFirst.parent == nil)
        XCTAssertEqual(foreground.parent == nil, baselineForeground.parent == nil)
        XCTAssertEqual(navigation.topViewController === foreground,
                       baselineNavigation.topViewController === baselineForeground)
    }

    @MainActor
    func testCloseWithoutCallingContextDoesNotGuessCurrentPage() throws {
        var code: SPKMethodStatusCode?
        CloseMethod().invoke(withParamModel: try decode(CloseMethodParamModel.self)) { _, status in
            code = status?.statusCode
        }
        XCTAssertEqual(code, .failed)
        let params = try decode(CloseMethodParamModel.self)
        var source: UIView? = UIView()
        params.spk_callingContainer = source
        XCTAssertTrue(params.spk_callingContainer === source)
        source = nil
        XCTAssertNil(params.spk_callingContainer)
    }

    @MainActor
    func testOriginalPickerAndUnmappedParameters() throws {
        let params = try decode(SPKChooseMediaParams.self, [
            "maxCount": 5, "isMultiSelect": true, "compressImage": true,
            "needBase64Data": true, "saveToPhotoAlbum": true
        ])
        XCTAssertFalse(params.compressImage)
        XCTAssertFalse(params.needBase64Data)
        XCTAssertFalse(params.saveToPhotoAlbum)
        let picker = SPKDefaultMediaPicker()
        let controller = try XCTUnwrap(picker.mediaPicker(with: params) { _, _ in })
        let imagePicker = try XCTUnwrap(controller as? UIImagePickerController)
        XCTAssertEqual(imagePicker.sourceType, .photoLibrary)
        XCTAssertFalse(imagePicker.allowsEditing)
    }

    func testOriginalMediaDefaultsAndOptionalDownloadExtension() throws {
        let choose = try decode(SPKChooseMediaParams.self)
        XCTAssertEqual(choose.sourceType, "album")
        XCTAssertEqual(choose.cameraType, "back")
        XCTAssertEqual(choose.maxCount, 1)
        XCTAssertEqual(choose.compressionQuality, 0.8, accuracy: 0.0001)
        XCTAssertEqual(choose.compressOption, 0)
        XCTAssertTrue(choose.needPreview)
        XCTAssertTrue(choose.needTempFilePath)
        XCTAssertNil(SPKChooseMediaParams.requiredKeyPaths())
        let download = try decode(SPKDownloadFileParams.self, ["url": "https://example.invalid/file"])
        XCTAssertNil(download.fileExtension)
        XCTAssertTrue(download.needCommonParams)
        XCTAssertEqual(download.timeoutInterval, 0)
        XCTAssertEqual(SPKDownloadFileParams.requiredKeyPaths(), Set(["url"]))
        XCTAssertEqual(SPKUploadImageParams.requiredKeyPaths(), Set(["url"]))
        XCTAssertEqual(SPKUploadParams.requiredKeyPaths(), Set(["url", "filePath"]))
    }

    func testLegacyCompressionAndPermissionKeys() throws {
        let params = try decode(SPKChooseMediaParams.self, [
            "compressWidth": 120, "compressHeight": 80, "compressionQuality": 0.4,
            "compressOption": 3, "cameraPermissionDenyAction": 1, "albumPermissionDenyAction": 1,
            "isNeedCut": true, "compressQuality": 100
        ])
        XCTAssertEqual(params.compressWidth, 120)
        XCTAssertEqual(params.compressHeight, 80)
        XCTAssertEqual(params.compressionQuality, 0.4, accuracy: 0.0001)
        XCTAssertEqual(params.cameraPermissionDenyAction, 1)
        XCTAssertEqual(params.albumPermissionDenyAction, 1)
        // The JS-only crop/quality keys did not override native compressionQuality.
        XCTAssertEqual(try decode(SPKChooseMediaParams.self, ["compressQuality": 100]).compressionQuality, 0.8)
    }

    func testImageCompressionMatchesOriginalOptions() throws {
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        let source = UIGraphicsImageRenderer(size: CGSize(width: 1200, height: 600), format: format).image { renderer in
            UIColor.red.setFill()
            renderer.fill(CGRect(x: 0, y: 0, width: 1200, height: 600))
        }
        let params = try decode(SPKChooseMediaParams.self, ["compressWidth": 120, "compressHeight": 80])
        let compressedData = try XCTUnwrap(params.imageData(for: source))
        let compressed = try XCTUnwrap(UIImage(data: compressedData))
        XCTAssertEqual(compressed.size.width, 120 * UIScreen.main.scale)
        XCTAssertEqual(compressed.size.height, 60 * UIScreen.main.scale)
        XCTAssertEqual(Array(compressedData.prefix(2)), [0xff, 0xd8])
        params.compressOption = 4
        let unscaled = try XCTUnwrap(UIImage(data: XCTUnwrap(params.imageData(for: source))))
        XCTAssertEqual(unscaled.size, source.size)
        params.compressOption = 2
        let base64Only = try XCTUnwrap(UIImage(data: XCTUnwrap(params.imageData(for: source))))
        XCTAssertEqual(base64Only.size, source.size)
    }

    func testStorageReadsExistingDataAndKeepsOldKeySemantics() throws {
        let key = " spk-compat-\(UUID().uuidString) "
        let defaults = try XCTUnwrap(UserDefaults(suiteName: "com.SPK.custom.userdefault"))
        defaults.set(["old": "value"], forKey: key)
        defer { defaults.removeObject(forKey: key) }
        let get = try decode(SPKStorageKeyModel.self, ["key": key, "biz": "different"])
        GetStorageItemMethod().invoke(withParamModel: get) { result, status in
            XCTAssertNil(status)
            XCTAssertEqual((result as? SPKStorageGetResult)?.data as? [String: String], ["old": "value"])
        }
        let set = try decode(SPKStorageSetModel.self, ["key": key, "data": "new", "biz": "other", "validDuration": 0])
        SetStorageItemMethod().invoke(withParamModel: set) { _, status in XCTAssertNil(status) }
        XCTAssertEqual(defaults.string(forKey: key), "new")
        GetStorageItemMethod().invoke(withParamModel: get) { result, status in
            XCTAssertNil(status)
            XCTAssertEqual((result as? SPKStorageGetResult)?.data as? String, "new")
        }
        RemoveStorageItemMethod().invoke(withParamModel: get) { _, status in XCTAssertNil(status) }
        XCTAssertNil(defaults.object(forKey: key))
    }

    func testSaveDataURLAcceptsOriginalBase64Whitespace() throws {
        let filename = "spk-compat-\(UUID().uuidString)"
        let params = try decode(SPKSaveDataURLParams.self, [
            "dataURL": "data:text/plain;base64,aGVs\nbG8=", "filename": filename, "extension": "txt"
        ])
        var path: String?
        SPKSaveDataURLMethod().invoke(withParamModel: params) { result, status in
            XCTAssertNil(status)
            path = (result as? SPKSaveDataURLResult)?.filePath
        }
        let file = URL(fileURLWithPath: try XCTUnwrap(path))
        defer { try? FileManager.default.removeItem(at: file) }
        XCTAssertEqual(try String(contentsOf: file, encoding: .utf8), "hello")
    }

    func testResponseKeysMatchEachOriginalMethod() throws {
        let uploadFile = try XCTUnwrap(SPKUploadFileResult())
        uploadFile.responseData = ["id": 17]
        let fileJSON = try XCTUnwrap(MTLJSONAdapter.jsonDictionary(fromModel: uploadFile))
        XCTAssertEqual((fileJSON["responseData"] as? [String: Int])?["id"], 17)
        XCTAssertNil(fileJSON["response"])
        XCTAssertNil(fileJSON["tempFiles"])
        XCTAssertEqual(fileJSON["clientCode"] as? Int, 0)
        let uploadImage = try XCTUnwrap(SPKUploadImageResult())
        uploadImage.responseData = ["id": 17]
        let imageJSON = try XCTUnwrap(MTLJSONAdapter.jsonDictionary(fromModel: uploadImage))
        XCTAssertNotNil(imageJSON["response"])
        XCTAssertNil(imageJSON["responseData"])
        let saved = try XCTUnwrap(SPKSaveDataURLResult())
        saved.filePath = "/tmp/example.txt"
        XCTAssertEqual(Set(try MTLJSONAdapter.jsonDictionary(fromModel: saved).keys.compactMap { $0 as? String }), Set(["filePath"]))
    }

    func testDownloadWithoutExtensionSendsParametersAndHeaders() throws {
        let server = try CompatibilityHTTPServer(body: Data("download fixture".utf8))
        defer { server.stop() }
        let params = try decode(SPKDownloadFileParams.self, [
            "url": server.url + "/file?existing=yes", "params": ["value": "a b"],
            "header": ["X-Compatibility": "download"], "timeoutInterval": 3
        ])
        let done = expectation(description: "download callback")
        let method = SPKDownloadFileMethod()
        method.invoke(withParamModel: params) { result, status in
            defer { done.fulfill() }
            XCTAssertNil(status)
            guard let result = result as? SPKDownloadResult, let path = result.filePath else {
                XCTFail("Missing download result")
                return
            }
            let file = URL(fileURLWithPath: path)
            defer { try? FileManager.default.removeItem(at: file) }
            XCTAssertEqual(file.pathExtension, "")
            XCTAssertEqual(try? String(contentsOf: file, encoding: .utf8), "download fixture")
            XCTAssertEqual(result.clientCode, 0)
            XCTAssertEqual(result.httpCode, 200)
        }
        wait(for: [done], timeout: 6)
        XCTAssertTrue(server.request.contains("existing=yes"))
        XCTAssertTrue(server.request.contains("value=a%20b"))
        XCTAssertTrue(server.request.lowercased().contains("x-compatibility: download"))
    }

    func testUploadWireParametersAndOriginalResponseKeys() throws {
        let file = FileManager.default.temporaryDirectory.appendingPathComponent("spk-compat-\(UUID().uuidString).jpg")
        try Data("upload fixture".utf8).write(to: file)
        defer { try? FileManager.default.removeItem(at: file) }
        for image in [false, true] {
            let server = try CompatibilityHTTPServer(body: Data(#"{"url":"fixture","uri":"17"}"#.utf8))
            defer { server.stop() }
            let json: [String: Any] = [
                "url": server.url + "/upload", "filePath": file.path, "name": "photo",
                "fileName": "avatar.jpg", "header": ["X-Compatibility": "upload"],
                "params": ["label": "fixture"], "timeoutInterval": 3, "paramsOption": 2
            ]
            let params: SPKUploadParams = image ? try decode(SPKUploadImageParams.self, json) : try decode(SPKUploadParams.self, json)
            let method: SPKMethod = image ? SPKUploadImageMethod() : SPKUploadFileMethod()
            let done = expectation(description: "upload callback")
            method.invoke(withParamModel: params) { result, status in
                defer { done.fulfill() }
                XCTAssertNil(status)
                guard let result else { XCTFail("Missing upload result"); return }
                let json = try? MTLJSONAdapter.jsonDictionary(fromModel: result)
                XCTAssertEqual(json?["clientCode"] as? Int, 0)
                XCTAssertEqual(json?["httpCode"] as? Int, 200)
                let response = json?[image ? "response" : "responseData"] as? [String: String]
                XCTAssertEqual(response?["uri"], "17")
            }
            wait(for: [done], timeout: 6)
            XCTAssertTrue(server.request.contains("POST /upload HTTP/1.1"))
            XCTAssertTrue(server.request.contains("name=\"photo\"; filename=\"avatar.jpg\""))
            XCTAssertTrue(server.request.contains("Content-Type: image/jpeg"))
            XCTAssertTrue(server.request.contains("name=\"label\""))
            XCTAssertTrue(server.request.contains("upload fixture"))
            XCTAssertTrue(server.request.lowercased().contains("x-compatibility: upload"))
        }
    }

    func testUploadDefaultsHeadersAndMIMETypes() throws {
        let params = try decode(SPKUploadImageParams.self, [
            "url": "https://example.invalid/upload", "filePath": "/tmp/a.jpg", "paramsOption": 2,
            "needCommonParams": false, "timeoutInterval": 5, "name": "photo", "fileName": "avatar.jpg",
            "header": ["X-Test": "value"], "params": ["id": 12]
        ])
        XCTAssertFalse(params.needCommonParams)
        XCTAssertEqual(params.paramsOption, 2)
        XCTAssertEqual(params.timeoutInterval, 5)
        XCTAssertEqual(params.name, "photo")
        XCTAssertEqual(params.fileName, "avatar.jpg")
        XCTAssertEqual(params.header?["X-Test"] as? String, "value")
        XCTAssertEqual(params.params?["id"] as? Int, 12)
        XCTAssertEqual(SPKMedia.mimeType(for: URL(fileURLWithPath: "/tmp/a.JPG")), "image/jpeg")
        XCTAssertEqual(SPKMedia.mimeType(for: URL(fileURLWithPath: "/tmp/a.mov")), "video/quicktime")
        XCTAssertEqual(SPKMedia.mimeType(for: URL(fileURLWithPath: "/tmp/a.unknown")), "application/octet-stream")
        XCTAssertEqual(SPKMedia.mimeType(for: URL(fileURLWithPath: "/tmp/a.unknown"), image: true), "image/jpeg")
    }
}

/// A loopback-only HTTP fixture, so network parameter tests do not contact external services.
private final class CompatibilityHTTPServer {
    private let listener: NWListener
    private let queue = DispatchQueue(label: "spk.compatibility.http")
    private let lock = NSLock()
    private var received = ""
    private let body: Data
    private(set) var url = ""

    var request: String {
        lock.lock()
        defer { lock.unlock() }
        return received
    }

    init(body: Data) throws {
        let parameters = NWParameters.tcp
        parameters.requiredLocalEndpoint = .hostPort(host: "127.0.0.1", port: .any)
        listener = try NWListener(using: parameters)
        self.body = body
        let ready = DispatchSemaphore(value: 0)
        listener.stateUpdateHandler = { state in
            if case .ready = state { ready.signal() }
            if case .failed = state { ready.signal() }
        }
        listener.newConnectionHandler = { [weak self] connection in
            guard let self else { connection.cancel(); return }
            connection.start(queue: self.queue)
            self.receive(connection, accumulated: Data())
        }
        listener.start(queue: queue)
        guard ready.wait(timeout: .now() + 3) == .success, let port = listener.port else {
            listener.cancel()
            throw NSError(domain: "SPKCompatibilityTest", code: 1)
        }
        url = "http://127.0.0.1:\(port.rawValue)"

    }

    func stop() { listener.cancel() }

    private func receive(_ connection: NWConnection, accumulated: Data) {
        connection.receive(minimumIncompleteLength: 1, maximumLength: 1 << 20) { [weak self] data, _, complete, failure in
            guard let self else { connection.cancel(); return }
            var bytes = accumulated
            if let data { bytes.append(data) }
            if let separator = bytes.range(of: Data("\r\n\r\n".utf8)) {
                let header = String(decoding: bytes[..<separator.lowerBound], as: UTF8.self)
                let length = header.components(separatedBy: "\r\n").first { $0.lowercased().hasPrefix("content-length:") }
                    .flatMap { Int($0.split(separator: ":", maxSplits: 1)[1].trimmingCharacters(in: .whitespaces)) } ?? 0
                if bytes.count >= separator.upperBound + length {
                    self.lock.lock()
                    self.received = String(decoding: bytes, as: UTF8.self)
                    self.lock.unlock()
                    var response = Data("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: \(self.body.count)\r\nConnection: close\r\n\r\n".utf8)
                    response.append(self.body)
                    connection.send(content: response, completion: .contentProcessed { _ in connection.cancel() })
                    return
                }
            }
            if complete || failure != nil { connection.cancel() }
            else { self.receive(connection, accumulated: bytes) }
        }
    }
}
