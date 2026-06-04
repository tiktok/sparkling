// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// Snapshot describing one Sparkling Method invocation. The fields mirror the
/// Android `SparklingMethodInvocationCenter.Event` so debug surfaces can be
/// kept consistent across platforms.
public struct SparklingMethodInvocationEvent {
    public let id: String
    public let name: String
    public let namespace: String?
    public let platform: String
    public let params: [String: Any]?
    public let result: [String: Any]?
    public let statusCode: Int?
    public let statusMessage: String?
    public let startTime: Date
    public let endTime: Date?

    public var durationMs: Int? {
        guard let end = endTime else { return nil }
        return Int((end.timeIntervalSince(startTime)) * 1000)
    }
}

/// Observer protocol for [SparklingMethodInvocationCenter].
public protocol SparklingMethodInvocationObserver: AnyObject {
    func sparklingMethodInvocationDidStart(_ event: SparklingMethodInvocationEvent)
    func sparklingMethodInvocationDidEnd(_ event: SparklingMethodInvocationEvent)
}

public extension SparklingMethodInvocationObserver {
    func sparklingMethodInvocationDidStart(_ event: SparklingMethodInvocationEvent) {}
}

/// Central hub the Sparkling Method pipe uses to broadcast every invocation
/// lifecycle event. Debug tools attach themselves at app startup; the pipe
/// only walks observers when at least one is registered.
public final class SparklingMethodInvocationCenter {
    public static let shared = SparklingMethodInvocationCenter()

    /// Notification posted whenever a method invocation starts. The
    /// `Notification.object` is the `SparklingMethodInvocationCenter` shared
    /// instance and the underlying `SparklingMethodInvocationEvent` is
    /// available under `userInfo[SparklingMethodInvocationCenter.eventKey]`.
    /// Debug surfaces that don't want to take a hard dependency on
    /// `SparklingMethod` can subscribe by name string instead of importing
    /// this module.
    public static let didStartNotification = Notification.Name("SparklingMethodInvocationCenter.didStart")
    public static let didEndNotification = Notification.Name("SparklingMethodInvocationCenter.didEnd")
    public static let eventKey = "event"

    private let lock = NSLock()
    private var observers = NSHashTable<AnyObject>.weakObjects()

    private init() {}

    public func addObserver(_ observer: SparklingMethodInvocationObserver) {
        lock.lock(); defer { lock.unlock() }
        observers.add(observer as AnyObject)
    }

    public func removeObserver(_ observer: SparklingMethodInvocationObserver) {
        lock.lock(); defer { lock.unlock() }
        observers.remove(observer as AnyObject)
    }

    public var hasObservers: Bool {
        lock.lock(); defer { lock.unlock() }
        return observers.allObjects.contains { _ in true }
    }

    func notifyStart(_ event: SparklingMethodInvocationEvent) {
        for case let observer as SparklingMethodInvocationObserver in snapshot() {
            observer.sparklingMethodInvocationDidStart(event)
        }
        NotificationCenter.default.post(
            name: SparklingMethodInvocationCenter.didStartNotification,
            object: self,
            userInfo: makeNotificationUserInfo(for: event)
        )
    }

    func notifyEnd(_ event: SparklingMethodInvocationEvent) {
        for case let observer as SparklingMethodInvocationObserver in snapshot() {
            observer.sparklingMethodInvocationDidEnd(event)
        }
        NotificationCenter.default.post(
            name: SparklingMethodInvocationCenter.didEndNotification,
            object: self,
            userInfo: makeNotificationUserInfo(for: event)
        )
    }

    func notifyNativeToJsEvent(name: String, params: [String: Any]?, containerID: String?) {
        let now = Date()
        let payload: [String: Any] = [
            "event": name,
            "params": params ?? [:],
            "containerID": containerID ?? NSNull(),
        ]
        let result: [String: Any] = [
            "status": "sent",
            "direction": "Native -> JS",
        ]
        notifyEnd(
            SparklingMethodInvocationEvent(
                id: UUID().uuidString,
                name: name,
                namespace: "native-to-js",
                platform: "lynx",
                params: payload,
                result: result,
                statusCode: 1,
                statusMessage: "sent",
                startTime: now,
                endTime: now
            )
        )
    }

    func notifyCompletedInvocation(
        name: String,
        namespace: String?,
        platform: String,
        params: [String: Any]?,
        result: [String: Any]?,
        statusCode: Int?,
        statusMessage: String?
    ) {
        let now = Date()
        notifyEnd(
            SparklingMethodInvocationEvent(
                id: UUID().uuidString,
                name: name,
                namespace: namespace,
                platform: platform,
                params: params,
                result: result,
                statusCode: statusCode,
                statusMessage: statusMessage,
                startTime: now,
                endTime: now
            )
        )
    }

    /// Build a stable, type-erased dictionary representation of the event so
    /// debug surfaces can subscribe via `Notification.Name` strings without
    /// importing this module. Keys mirror the struct field names.
    private func makeNotificationUserInfo(for event: SparklingMethodInvocationEvent) -> [AnyHashable: Any] {
        var info: [AnyHashable: Any] = [
            SparklingMethodInvocationCenter.eventKey: event,
            "id": event.id,
            "name": event.name,
            "platform": event.platform,
            "startTime": event.startTime,
        ]
        if let namespace = event.namespace { info["namespace"] = namespace }
        if let params = event.params { info["params"] = params }
        if let result = event.result { info["result"] = result }
        if let statusCode = event.statusCode { info["statusCode"] = statusCode }
        if let statusMessage = event.statusMessage { info["statusMessage"] = statusMessage }
        if let endTime = event.endTime { info["endTime"] = endTime }
        return info
    }

    private func snapshot() -> [AnyObject] {
        lock.lock(); defer { lock.unlock() }
        return observers.allObjects
    }
}
