// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import SparklingMethod
import Sparkling_Storage
import Testing
import XCTest

class SPKStorageTest: XCTestCase {
    var setStorageItemMethod: SetStorageItemMethod!
    var getStorageItemMethod: GetStorageItemMethod!
    var removeStorageItemMethod: RemoveStorageItemMethod!

    var setStorageItemModelJSONDict: [String: Any] = [
        "key": "storage_test",
        "data": ["test_key": "test_value", "number": 123] as [String: Any],
    ]

    var getStorageItemModelJSONDict: [String: Any] = [
        "key": "storage_test"
    ]

    var removeStorageItemModelJSONDict: [String: Any] = [
        "key": "storage_test"
    ]

    override func setUp() {
        SPKServiceRegister.registerAll()
        setStorageItemMethod = SetStorageItemMethod()
        getStorageItemMethod = GetStorageItemMethod()
        removeStorageItemMethod = RemoveStorageItemMethod()
    }

    override func tearDown() {
        super.tearDown()
    }

    // MARK: - SetStorageItemMethod Tests

    func testSetStorageItemMethodParamModelConversion() {
        do {
            let model = try SetStorageItemMethodParamModel(dictionary: setStorageItemModelJSONDict)
            XCTAssertNotNil(model)
            XCTAssertEqual(model.key, "storage_test")
            XCTAssertNotNil(model.data)

            let toDict = try model.toDict()
            XCTAssertNotNil(toDict)
            XCTAssertEqual(toDict?["key"] as? String, setStorageItemModelJSONDict["key"] as? String)
        } catch {
            XCTFail("Failed to create model: \(error)")
        }
    }

    func testSetStorageItemMethodInvoke() {
        let expectation = self.expectation(description: "SetStorageItem invoke")

        do {
            let model = try SetStorageItemMethodParamModel(dictionary: setStorageItemModelJSONDict)
            setStorageItemMethod.call(withParamModel: model) { status, result in
                #expect(status.code == MethodStatusCode.succeeded)
                XCTAssertNil(result)
                expectation.fulfill()
            }
        } catch {
            XCTFail("Failed to invoke method: \(error)")
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 5.0)
    }

    func testSetStorageItemMethodWithEmptyKey() {
        let expectation = self.expectation(description: "SetStorageItem with empty key")

        do {
            var invalidParams = setStorageItemModelJSONDict
            invalidParams["key"] = ""
            let model = try SetStorageItemMethodParamModel(dictionary: invalidParams)

            guard let method = MethodRegistry.global.method(forName: SetStorageItemMethod.methodName()) as? SetStorageItemMethod else {
                XCTFail("Method not found")
                expectation.fulfill()
                return
            }

            method.call(withParamModel: model) { status, _ in
                XCTAssertEqual(status.code, MethodStatusCode.invalidInputParameter)
                expectation.fulfill()
            }
        } catch {
            XCTFail("Failed to invoke method: \(error)")
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 5.0)
    }

    // MARK: - GetStorageItemMethod Tests

    func testGetStorageItemMethodParamModelConversion() {
        do {
            let model = try GetStorageItemMethodParamModel(dictionary: getStorageItemModelJSONDict)
            XCTAssertNotNil(model)
            XCTAssertEqual(model.key, "storage_test")

            let toDict = try model.toDict()
            XCTAssertNotNil(toDict)
            XCTAssertEqual(toDict?["key"] as? String, getStorageItemModelJSONDict["key"] as? String)
        } catch {
            XCTFail("Failed to create model: \(error)")
        }
    }

    func testGetStorageItemMethodInvoke() {
        let expectation = self.expectation(description: "GetStorageItem invoke")

        do {
            testSetStorageItemMethodInvoke()

            let model = try GetStorageItemMethodParamModel(dictionary: getStorageItemModelJSONDict)
            getStorageItemMethod.call(withParamModel: model) { status, result in
                #expect(status.code == MethodStatusCode.succeeded)
                if let resultModel = result as? GetStorageItemMethodResultModel {
                    XCTAssertNotNil(resultModel.data)
                } else {
                    XCTFail("Result should be GetStorageItemMethodResultModel")
                }
                expectation.fulfill()
            }
        } catch {
            XCTFail("Failed to invoke method: \(error)")
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 5.0)
    }

    func testGetStorageItemMethodWithEmptyKey() {
        let expectation = self.expectation(description: "GetStorageItem with empty key")

        do {
            var invalidParams = getStorageItemModelJSONDict
            invalidParams["key"] = ""
            let model = try GetStorageItemMethodParamModel(dictionary: invalidParams)

            getStorageItemMethod.call(withParamModel: model) { status, _ in
                XCTAssertEqual(status.code, MethodStatusCode.invalidInputParameter)
                expectation.fulfill()
            }
        } catch {
            XCTFail("Failed to invoke method: \(error)")
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 5.0)
    }

    // MARK: - RemoveStorageItemMethod Tests

    func testRemoveStorageItemMethodParamModelConversion() {
        do {
            let model = try RemoveStorageItemMethodParamModel(dictionary: removeStorageItemModelJSONDict)
            XCTAssertNotNil(model)
            XCTAssertEqual(model.key, "storage_test")

            let toDict = try model.toDict()
            XCTAssertNotNil(toDict)
            XCTAssertEqual(toDict?["key"] as? String, removeStorageItemModelJSONDict["key"] as? String)
        } catch {
            XCTFail("Failed to create model: \(error)")
        }
    }

    func testRemoveStorageItemMethodInvoke() {
        let expectation = self.expectation(description: "RemoveStorageItem invoke")

        do {
            testSetStorageItemMethodInvoke()
            let model = try RemoveStorageItemMethodParamModel(dictionary: removeStorageItemModelJSONDict)

            removeStorageItemMethod.call(withParamModel: model) { status, result in
                #expect(status.code == MethodStatusCode.succeeded)
                expectation.fulfill()
            }
        } catch {
            XCTFail("Failed to invoke method: \(error)")
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 5.0)
    }

    func testRemoveStorageItemMethodWithEmptyKey() {
        let expectation = self.expectation(description: "RemoveStorageItem with empty key")

        do {
            var invalidParams = removeStorageItemModelJSONDict
            invalidParams["key"] = ""
            let model = try RemoveStorageItemMethodParamModel(dictionary: invalidParams)

            removeStorageItemMethod.call(withParamModel: model) { status, _ in
                XCTAssertEqual(status.code, MethodStatusCode.invalidInputParameter)
                expectation.fulfill()
            }
        } catch {
            XCTFail("Failed to invoke method: \(error)")
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 5.0)
    }

    // MARK: - Integration Tests

    func testStorageOperationSequence() {
        let expectation = self.expectation(description: "Storage operation sequence")
        let group = DispatchGroup()

        group.enter()
        DispatchQueue.global().async {
            self.testSetStorageItemMethodInvoke()
            group.leave()
        }

        group.enter()
        DispatchQueue.global().asyncAfter(deadline: .now() + 0.5) {
            self.testGetStorageItemMethodInvoke()
            group.leave()
        }

        group.enter()
        DispatchQueue.global().asyncAfter(deadline: .now() + 1.0) {
            self.testRemoveStorageItemMethodInvoke()
            group.leave()
        }

        group.enter()
        DispatchQueue.global().asyncAfter(deadline: .now() + 1.5) {
            let getExpectation = self.expectation(description: "Get after removal")

            do {
                let model = try GetStorageItemMethodParamModel(dictionary: self.getStorageItemModelJSONDict)
                self.getStorageItemMethod.call(withParamModel: model) { status, result in
                    #expect(status.code == MethodStatusCode.succeeded)
                    if let resultModel = result as? GetStorageItemMethodResultModel {
                        XCTAssertNil(resultModel.data)
                    }
                    getExpectation.fulfill()
                    group.leave()
                }
            } catch {
                XCTFail("Failed to invoke method: \(error)")
                getExpectation.fulfill()
                group.leave()
            }

            self.wait(for: [getExpectation], timeout: 5.0)
        }

        group.notify(queue: .main) {
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 20.0)
    }

    func testMissingRequiredParameters() {
        XCTAssertThrowsError(try SetStorageItemMethodParamModel(dictionary: ["data": ["test_key": "test_value"]]))
        XCTAssertThrowsError(try SetStorageItemMethodParamModel(dictionary: ["key": "test_key"]))
        XCTAssertThrowsError(try GetStorageItemMethodParamModel(dictionary: [:]))
        XCTAssertThrowsError(try RemoveStorageItemMethodParamModel(dictionary: [:]))
    }

    func testMethodNameAccess() {
        let setMethod = SetStorageItemMethod()
        let getMethod = GetStorageItemMethod()
        let removeMethod = RemoveStorageItemMethod()

        XCTAssertEqual(setMethod.methodName, "storage.setItem")
        XCTAssertEqual(getMethod.methodName, "storage.getItem")
        XCTAssertEqual(removeMethod.methodName, "storage.removeItem")

        XCTAssertEqual(SetStorageItemMethod.methodName(), "storage.setItem")
        XCTAssertEqual(GetStorageItemMethod.methodName(), "storage.getItem")
        XCTAssertEqual(RemoveStorageItemMethod.methodName(), "storage.removeItem")
    }

    func testModelClassAccess() {
        let setMethod = SetStorageItemMethod()
        let getMethod = GetStorageItemMethod()
        let removeMethod = RemoveStorageItemMethod()

        XCTAssertEqual(String(describing: setMethod.paramsModelClass), String(describing: SetStorageItemMethodParamModel.self))
        XCTAssertEqual(String(describing: setMethod.resultModelClass), String(describing: EmptyMethodModelClass.self))

        XCTAssertEqual(String(describing: getMethod.paramsModelClass), String(describing: GetStorageItemMethodParamModel.self))
        XCTAssertEqual(String(describing: getMethod.resultModelClass), String(describing: GetStorageItemMethodResultModel.self))

        XCTAssertEqual(String(describing: removeMethod.paramsModelClass), String(describing: RemoveStorageItemMethodParamModel.self))
        XCTAssertEqual(String(describing: removeMethod.resultModelClass), String(describing: EmptyMethodModelClass.self))
    }
}
