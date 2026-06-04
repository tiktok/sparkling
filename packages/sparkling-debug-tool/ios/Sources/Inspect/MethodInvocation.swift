// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// One observed Sparkling Method invocation rendered in the debug panel.
public struct SparklingMethodInvocation {
    public let id: String
    public let name: String
    public let namespace: String?
    public let platform: String
    public let params: String
    public var result: String?
    public var statusCode: Int?
    public var statusMessage: String?
    public let startTime: Date
    public var endTime: Date?

    public var durationMs: Int? {
        guard let endTime else { return nil }
        return Int(endTime.timeIntervalSince(startTime) * 1000)
    }

    public var success: Bool? {
        if endTime == nil { return nil }
        if let code = statusCode { return code == 0 || code == 1 }
        if let message = statusMessage?.lowercased(), message.contains("fail") || message.contains("error") || message.contains("invalid") || message.contains("notfound") { return false }
        return true
    }

    public init(
        id: String,
        name: String,
        namespace: String?,
        platform: String,
        params: String,
        result: String? = nil,
        statusCode: Int? = nil,
        statusMessage: String? = nil,
        startTime: Date,
        endTime: Date? = nil
    ) {
        self.id = id
        self.name = name
        self.namespace = namespace
        self.platform = platform
        self.params = params
        self.result = result
        self.statusCode = statusCode
        self.statusMessage = statusMessage
        self.startTime = startTime
        self.endTime = endTime
    }

    public static func format(_ payload: Any?) -> String {
        guard let payload else { return "" }
        if let string = payload as? String { return string }
        if JSONSerialization.isValidJSONObject(payload),
           let data = try? JSONSerialization.data(withJSONObject: payload, options: [.prettyPrinted, .sortedKeys]),
           let json = String(data: data, encoding: .utf8) {
            return json
        }
        return "\(payload)"
    }
}

public final class SparklingMethodInvocationStore {
    public static let shared = SparklingMethodInvocationStore()
    public static let didChangeNotification = Notification.Name("SparklingMethodInvocationStore.didChange")

    private let queue = DispatchQueue(label: "sparkling.debug.method-store", attributes: .concurrent)
    private var buffer: [SparklingMethodInvocation] = []
    private var capacity: Int = 300

    private init() {}

    public func setCapacity(_ value: Int) {
        queue.async(flags: .barrier) {
            self.capacity = max(1, value)
            self.trim()
        }
        notify()
    }

    public func add(_ record: SparklingMethodInvocation) {
        queue.async(flags: .barrier) {
            self.buffer.append(record)
            self.trim()
        }
        notify()
    }

    public func update(_ record: SparklingMethodInvocation) {
        queue.async(flags: .barrier) {
            guard let index = self.buffer.firstIndex(where: { $0.id == record.id }) else {
                self.buffer.append(record)
                self.trim()
                return
            }
            self.buffer[index] = record
        }
        notify()
    }

    public func snapshot() -> [SparklingMethodInvocation] {
        queue.sync { buffer }
    }

    public func clear() {
        queue.async(flags: .barrier) { self.buffer.removeAll() }
        notify()
    }

    private func trim() {
        if buffer.count > capacity {
            buffer.removeFirst(buffer.count - capacity)
        }
    }

    private func notify() {
        let post = {
            NotificationCenter.default.post(name: Self.didChangeNotification, object: self)
        }
        if Thread.isMainThread { post() } else { DispatchQueue.main.async(execute: post) }
    }
}
