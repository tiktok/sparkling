// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// One Sparkling instance's runtime props as displayed in the debug panel.
public struct SparklingGlobalPropsSnapshot {
    public let containerId: String
    public let templateUrl: String?
    public let globalProps: [String: Any]
    public let queryItems: [String: Any]

    public init(containerId: String, templateUrl: String?, globalProps: [String: Any], queryItems: [String: Any]) {
        self.containerId = containerId
        self.templateUrl = templateUrl
        self.globalProps = globalProps
        self.queryItems = queryItems
    }
}

/// Bridge between the host application's view registry and the GlobalProps
/// debug panel. The host sets a [Provider] that walks every live Lynx view
/// and returns one snapshot per instance.
public final class SparklingGlobalPropsRegistry {
    public typealias Provider = () -> [SparklingGlobalPropsSnapshot]

    public static let shared = SparklingGlobalPropsRegistry()
    public static let didChangeNotification = Notification.Name("SparklingGlobalPropsRegistry.didChange")

    private let lock = NSLock()
    private var provider: Provider?

    private init() {}

    public func setProvider(_ provider: Provider?) {
        lock.lock(); defer { lock.unlock() }
        self.provider = provider
        notifyChanged()
    }

    public func collectAll() -> [SparklingGlobalPropsSnapshot] {
        lock.lock()
        let snapshot = provider
        lock.unlock()
        return snapshot?() ?? []
    }

    public func notifyChanged() {
        DispatchQueue.main.async {
            NotificationCenter.default.post(name: Self.didChangeNotification, object: self)
        }
    }
}
