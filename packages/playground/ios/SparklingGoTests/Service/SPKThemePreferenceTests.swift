// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import SparklingMethod
import Testing
import UIKit

@testable import Sparkling

@Suite(.serialized)
@MainActor
struct SPKThemePreferenceTests {
    @Test func defaultsToFollowSystemAndPersistsPreference() {
        let userDefaults = self.makeUserDefaults()
        let manager = SPKThemePreferenceManager(userDefaults: userDefaults)

        #expect(manager.preference == .followSystem)

        manager.setPreference(.dark)

        #expect(manager.preference == .dark)
        #expect(userDefaults.string(forKey: SPKThemePreferenceManager.userDefaultsKey) == SPKThemePreference.dark.rawValue)
    }

    @Test func defaultGlobalPropsInjectPersistedPreference() {
        self.resetSharedPreference()
        defer { self.resetSharedPreference() }

        SPKThemePreferenceManager.shared.setPreference(.light)

        let globalProps = SPKGlobalPropsUtils.defaultGlobalProps()

        #expect(globalProps[SPKThemePreferenceManager.globalPropsKey] as? String == SPKThemePreference.light.rawValue)
    }

    @Test func defaultGlobalPropsInjectSystemTheme() {
        let globalProps = SPKGlobalPropsUtils.defaultGlobalProps()

        #expect(globalProps["theme"] as? String == SPKGlobalPropsUtils.systemTheme())
        #expect(SPKGlobalPropsUtils.systemTheme(for: .light) == "light")
        #expect(SPKGlobalPropsUtils.systemTheme(for: .dark) == "dark")
        #expect(SPKGlobalPropsUtils.systemTheme(for: .unspecified) == "light")
    }

    @Test func themeMethodIsGloballyAutoDiscovered() {
        let methodName = SPKSetThemePreferenceMethod.methodName()
        MethodRegistry.global.unregister(methodName: methodName)
        defer { MethodRegistry.global.unregister(methodName: methodName) }

        MethodRegistry.autoRegisterGlobalMethods()

        #expect(MethodRegistry.global.respondTo(methodName: methodName))
        #expect(MethodRegistry.global.method(forName: methodName) is SPKSetThemePreferenceMethod)
    }

    @Test func broadcastsPreferenceToAllActiveContainers() {
        let manager = SPKThemePreferenceManager(userDefaults: self.makeUserDefaults())
        let firstContainer = RecordingWrapperView(containerID: "theme-container-1")
        let secondContainer = RecordingWrapperView(containerID: "theme-container-2")
        manager.register(firstContainer)
        manager.register(secondContainer)

        manager.setPreference(.dark)

        let expected = [SPKThemePreferenceManager.globalPropsKey: SPKThemePreference.dark.rawValue]
        #expect(firstContainer.globalPropsUpdates == [expected])
        #expect(secondContainer.globalPropsUpdates == [expected])
    }

    @Test func setThemePreferenceMethodPersistsAndReturnsNormalizedPreference() {
        self.resetSharedPreference()
        defer { self.resetSharedPreference() }

        let paramModel = SPKSetThemePreferenceParamModel()
        paramModel.preference = " DARK "
        var status: MethodStatus?
        var result: SPKSetThemePreferenceResultModel?

        SPKSetThemePreferenceMethod().call(withParamModel: paramModel) { callbackStatus, callbackResult in
            status = callbackStatus
            result = callbackResult as? SPKSetThemePreferenceResultModel
        }

        #expect(SPKSetThemePreferenceMethod.methodName() == "sparkling.setThemePreference")
        #expect(status?.code == .succeeded)
        #expect(result?.preference == SPKThemePreference.dark.rawValue)
        #expect(SPKThemePreferenceManager.shared.preference == .dark)
    }

    @Test func setThemePreferenceMethodRejectsInvalidPreference() {
        self.resetSharedPreference()
        defer { self.resetSharedPreference() }

        let paramModel = SPKSetThemePreferenceParamModel()
        paramModel.preference = "sepia"
        var status: MethodStatus?
        var result: SPKMethodModel?

        SPKSetThemePreferenceMethod().call(withParamModel: paramModel) { callbackStatus, callbackResult in
            status = callbackStatus
            result = callbackResult
        }

        #expect(status?.code == .invalidInputParameter)
        #expect(result == nil)
        #expect(SPKThemePreferenceManager.shared.preference == .followSystem)
    }

    private func makeUserDefaults() -> UserDefaults {
        let suiteName = "SPKThemePreferenceTests.\(UUID().uuidString)"
        let userDefaults = UserDefaults(suiteName: suiteName)!
        userDefaults.removePersistentDomain(forName: suiteName)
        return userDefaults
    }

    private func resetSharedPreference() {
        UserDefaults.standard.removeObject(forKey: SPKThemePreferenceManager.userDefaultsKey)
    }
}

@MainActor
private final class RecordingWrapperView: UIView, SPKWrapperViewProtocol {
    let containerID: String
    var context: SPKHybridContext?
    var loadState: SPKLoadState = .SPKLoadStateNotLoad
    var rawView: UIView? { self }
    var params: SPKHybridParams?
    weak var lifeCycleDelegate: SPKWrapperViewLifecycleProtocol?
    var anyMethodPipe: Any?
    var estimatedProgress: Float = 0
    var globalPropsUpdates: [[String: String]] = []

    init(containerID: String) {
        self.containerID = containerID
        super.init(frame: .zero)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func load() {}

    func reload(_ context: SPKHybridContext?) {
        self.context = context
    }

    func send(event event: String, params: [String: Any]?, callback: ((Any?) -> Void)?) {}

    func config(withParams params: SPKHybridParams?) {
        self.params = params
    }

    func onshow(params: [AnyHashable: Any]) {}

    func onHide(params: [AnyHashable: Any]) {}

    func update(withGlobalProps globalProps: Any?) {
        guard let globalProps = globalProps as? [String: String] else { return }
        self.globalPropsUpdates.append(globalProps)
    }

    func config(withGlobalProps globalProps: Any?) {}
}
