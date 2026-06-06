// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import XCTest
import SparklingMethod
@testable import Sparkling_Storage

private final class InMemoryStorageService: StorageService {
    private var values: [String: Any] = [:]

    func object(forKey key: String) -> Any? {
        values[key]
    }

    func removeObject(forKey key: String) {
        values.removeValue(forKey: key)
    }

    func setObject(key: String, value: Any) {
        values[key] = value
    }
}

final class SPKStorageTest: XCTestCase {
    private var storageService: InMemoryStorageService!

    override func setUp() {
        super.setUp()
        DefaultDIContainerProvider.inject()
        storageService = InMemoryStorageService()
        DIProviderRegistry.provider.pipeShared().register(StorageService.self) { self.storageService }
    }

    override func tearDown() {
        storageService = nil
        super.tearDown()
    }

    func testMethodNames() {
        XCTAssertEqual(SetStorageItemMethod().methodName, "storage.setItem")
        XCTAssertEqual(GetStorageItemMethod().methodName, "storage.getItem")
        XCTAssertEqual(RemoveStorageItemMethod().methodName, "storage.removeItem")
        XCTAssertEqual(SetStorageItemMethod.methodName(), "storage.setItem")
        XCTAssertEqual(GetStorageItemMethod.methodName(), "storage.getItem")
        XCTAssertEqual(RemoveStorageItemMethod.methodName(), "storage.removeItem")
    }

    func testModelClasses() {
        XCTAssertTrue(SetStorageItemMethod().paramsModelClass is SetStorageItemMethodParamModel.Type)
        XCTAssertTrue(SetStorageItemMethod().resultModelClass is EmptyMethodModelClass.Type)
        XCTAssertTrue(GetStorageItemMethod().paramsModelClass is GetStorageItemMethodParamModel.Type)
        XCTAssertTrue(GetStorageItemMethod().resultModelClass is GetStorageItemMethodResultModel.Type)
        XCTAssertTrue(RemoveStorageItemMethod().paramsModelClass is RemoveStorageItemMethodParamModel.Type)
        XCTAssertTrue(RemoveStorageItemMethod().resultModelClass is EmptyMethodModelClass.Type)
    }

    func testParamModelConversionAndValidation() throws {
        let setModel = try SetStorageItemMethodParamModel(dictionary: [
            "key": "storage_test",
            "data": ["value": "stored"] as [String: Any],
        ])
        XCTAssertEqual(setModel.key, "storage_test")
        XCTAssertEqual(try setModel.toDict()?["key"] as? String, "storage_test")

        let getModel = try GetStorageItemMethodParamModel(dictionary: ["key": "storage_test"])
        XCTAssertEqual(getModel.key, "storage_test")
        XCTAssertEqual(try getModel.toDict()?["key"] as? String, "storage_test")

        let removeModel = try RemoveStorageItemMethodParamModel(dictionary: ["key": "storage_test"])
        XCTAssertEqual(removeModel.key, "storage_test")
        XCTAssertEqual(try removeModel.toDict()?["key"] as? String, "storage_test")

        XCTAssertThrowsError(try SetStorageItemMethodParamModel(dictionary: ["data": "missing key"]))
        XCTAssertThrowsError(try SetStorageItemMethodParamModel(dictionary: ["key": "missing data"]))
        XCTAssertThrowsError(try GetStorageItemMethodParamModel(dictionary: [:]))
        XCTAssertThrowsError(try RemoveStorageItemMethodParamModel(dictionary: [:]))
    }

    func testSetGetAndRemoveStorageItemMethods() throws {
        let key = "storage_test"
        let payload = ["value": "stored", "count": 3] as [String: Any]

        let setExpectation = expectation(description: "set storage item")
        let setModel = try SetStorageItemMethodParamModel(dictionary: ["key": key, "data": payload])
        SetStorageItemMethod().call(withParamModel: setModel) { status, result in
            XCTAssertEqual(status.code, MethodStatusCode.succeeded)
            XCTAssertNil(result)
            setExpectation.fulfill()
        }
        wait(for: [setExpectation], timeout: 5.0)

        let getExpectation = expectation(description: "get storage item")
        let getModel = try GetStorageItemMethodParamModel(dictionary: ["key": key])
        GetStorageItemMethod().call(withParamModel: getModel) { status, result in
            XCTAssertEqual(status.code, MethodStatusCode.succeeded)
            let resultModel = result as? GetStorageItemMethodResultModel
            let resultPayload: [String: Any]? = resultModel?.data?.dictionaryValue()
            XCTAssertEqual(resultPayload?["value"] as? String, "stored")
            XCTAssertEqual(resultPayload?["count"] as? Int, 3)
            getExpectation.fulfill()
        }
        wait(for: [getExpectation], timeout: 5.0)

        let removeExpectation = expectation(description: "remove storage item")
        let removeModel = try RemoveStorageItemMethodParamModel(dictionary: ["key": key])
        RemoveStorageItemMethod().call(withParamModel: removeModel) { status, result in
            XCTAssertEqual(status.code, MethodStatusCode.succeeded)
            XCTAssertNil(result)
            removeExpectation.fulfill()
        }
        wait(for: [removeExpectation], timeout: 5.0)

        XCTAssertNil(storageService.object(forKey: key))
    }

    func testEmptyKeysReturnInvalidInput() throws {
        let setExpectation = expectation(description: "set empty key")
        let setModel = try SetStorageItemMethodParamModel(dictionary: [
            "key": "",
            "data": ["value": "stored"] as [String: Any],
        ])
        SetStorageItemMethod().call(withParamModel: setModel) { status, _ in
            XCTAssertEqual(status.code, MethodStatusCode.invalidInputParameter)
            setExpectation.fulfill()
        }
        wait(for: [setExpectation], timeout: 5.0)

        let getExpectation = expectation(description: "get empty key")
        let getModel = try GetStorageItemMethodParamModel(dictionary: ["key": ""])
        GetStorageItemMethod().call(withParamModel: getModel) { status, _ in
            XCTAssertEqual(status.code, MethodStatusCode.invalidInputParameter)
            getExpectation.fulfill()
        }
        wait(for: [getExpectation], timeout: 5.0)

        let removeExpectation = expectation(description: "remove empty key")
        let removeModel = try RemoveStorageItemMethodParamModel(dictionary: ["key": ""])
        RemoveStorageItemMethod().call(withParamModel: removeModel) { status, _ in
            XCTAssertEqual(status.code, MethodStatusCode.invalidInputParameter)
            removeExpectation.fulfill()
        }
        wait(for: [removeExpectation], timeout: 5.0)
    }
}
