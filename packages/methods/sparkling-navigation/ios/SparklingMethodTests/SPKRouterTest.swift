// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import XCTest
import SparklingMethod
@testable import Sparkling_Router

class SPKRouterTest: XCTestCase {

    // MARK: - CloseMethod Tests

    func testCloseMethodName() {
        let method = CloseMethod()
        XCTAssertEqual(method.methodName, "router.close")
        XCTAssertEqual(CloseMethod.methodName(), "router.close")
    }

    func testCloseMethodModels() {
        let method = CloseMethod()
        XCTAssertTrue(method.paramsModelClass is CloseMethodParamModel.Type)
        XCTAssertTrue(method.resultModelClass is EmptyMethodModelClass.Type)
    }

    func testCloseMethodParamModelValidation() {
        let paramModel = CloseMethodParamModel()

        XCTAssertTrue(type(of: paramModel).requiredKeyPaths()?.isEmpty ?? false)

        paramModel.containerID = "test-container"
        XCTAssertNoThrow(try paramModel.validate())
    }

    func testCloseMethodParamModelJSONMapping() {
        let paramModel = CloseMethodParamModel()
        paramModel.containerID = "test-container"
        paramModel.animated = true

        XCTAssertEqual(type(of: paramModel).jsonKeyPathsByPropertyKey() as? [String: String], [
            "containerID": "containerID",
            "animated": "animated"
        ])
    }

    // MARK: - OpenMethod Tests

    func testOpenMethodName() {
        let method = OpenMethod()
        XCTAssertEqual(method.methodName, "router.open")
        XCTAssertEqual(OpenMethod.methodName(), "router.open")
    }

    func testOpenMethodModels() {
        let method = OpenMethod()
        XCTAssertTrue(method.paramsModelClass is OpenMethodParamModel.Type)
        XCTAssertTrue(method.resultModelClass is EmptyMethodModelClass.Type)
    }

    func testOpenMethodParamModelValidation() throws {
        XCTAssertThrowsError(try OpenMethodParamModel(dictionary: [:]))

        let paramModel = try XCTUnwrap(try OpenMethodParamModel.from(dict: ["scheme": "https://example.com"]))
        paramModel.scheme = "https://example.com"
        XCTAssertNoThrow(try paramModel.validate())
    }

    func testOpenMethodParamModelJSONMapping() {
        let paramModel = OpenMethodParamModel()

        XCTAssertEqual(type(of: paramModel).jsonKeyPathsByPropertyKey() as? [String: String], [
            "scheme": "scheme",
            "replace": "replace",
            "replaceType": "replaceType",
            "useSysBrowser": "useSysBrowser",
            "animated": "animated",
            "interceptor": "interceptor",
            "extra": "extra"
        ])
    }

    func testOpenMethodParamModelDefaultValues() {
        let paramModel = OpenMethodParamModel()
        paramModel.scheme = "test-scheme"

        XCTAssertFalse(paramModel.replace)
        XCTAssertFalse(paramModel.useSysBrowser)
        XCTAssertFalse(paramModel.animated)
    }

    func testOpenMethodParamModelAllFields() {
        let paramModel = OpenMethodParamModel()
        paramModel.scheme = "test-scheme"
        paramModel.replace = true
        paramModel.replaceType = "all"
        paramModel.useSysBrowser = true
        paramModel.animated = true
        paramModel.interceptor = "test-interceptor"
        let extraDict = NSDictionary(dictionary: ["key": "value"])
        paramModel.extra = extraDict

        XCTAssertEqual(paramModel.scheme, "test-scheme")
        XCTAssertTrue(paramModel.replace)
        XCTAssertEqual(paramModel.replaceType, "all")
        XCTAssertTrue(paramModel.useSysBrowser)
        XCTAssertTrue(paramModel.animated)
        XCTAssertEqual(paramModel.interceptor, "test-interceptor")
        XCTAssertEqual(paramModel.extra, extraDict)
    }

    func testOpenMethodParamModelFromDictParsesExtraObject() throws {
        let dict: [String: Any] = [
            "scheme": "hybrid://lynxview_page?bundle=a.lynx.bundle",
            "extra": ["foo": "bar"],
        ]
        let model = try XCTUnwrap(try OpenMethodParamModel.from(dict: dict))
        XCTAssertNotNil(model.extra)
        XCTAssertEqual(model.extra?["foo"] as? String, "bar")
    }
}
