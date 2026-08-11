// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

public enum SPKThemePreference: String {
    case followSystem = "follow-system"
    case light
    case dark

    public init?(value: String?) {
        guard let value = value?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() else {
            return nil
        }
        self.init(rawValue: value)
    }
}

public final class SPKThemePreferenceManager {
    public static let shared = SPKThemePreferenceManager()
    public static let globalPropsKey = "preferredTheme"

    static let userDefaultsKey = "com.tiktok.sparkling.theme.preference"

    private let userDefaults: UserDefaults
    private let containers = NSHashTable<AnyObject>.weakObjects()
    private let lock = NSLock()

    init(userDefaults: UserDefaults = .standard) {
        self.userDefaults = userDefaults
    }

    public var preference: SPKThemePreference {
        SPKThemePreference(value: self.userDefaults.string(forKey: Self.userDefaultsKey)) ?? .followSystem
    }

    public func setPreference(_ preference: SPKThemePreference) {
        self.userDefaults.set(preference.rawValue, forKey: Self.userDefaultsKey)

        let update = { [weak self] in
            guard let self else { return }
            let globalProps = [Self.globalPropsKey: preference.rawValue]
            for container in self.activeContainers() {
                container.update(withGlobalProps: globalProps)
            }
        }
        if Thread.isMainThread {
            update()
        } else {
            DispatchQueue.main.async(execute: update)
        }
    }

    func register(_ container: SPKWrapperViewProtocol) {
        self.lock.lock()
        self.containers.add(container)
        self.lock.unlock()
    }

    func unregister(_ container: SPKWrapperViewProtocol) {
        self.lock.lock()
        self.containers.remove(container)
        self.lock.unlock()
    }

    private func activeContainers() -> [SPKWrapperViewProtocol] {
        self.lock.lock()
        defer { self.lock.unlock() }
        return self.containers.allObjects.compactMap { $0 as? SPKWrapperViewProtocol }
    }
}
