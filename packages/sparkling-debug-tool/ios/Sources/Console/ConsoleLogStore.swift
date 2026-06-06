// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// Thread-safe ring buffer of [SparklingConsoleLog]s shared by every
/// [SparklingConsoleViewController] instance and the [LynxConsoleSink] that
/// feeds it.
public final class SparklingConsoleStore {
    public static let shared = SparklingConsoleStore()

    public static let didChangeNotification = Notification.Name("SparklingConsoleStore.didChange")

    private let queue = DispatchQueue(label: "com.tiktok.sparkling.debugtool.console.store")
    private var buffer: [SparklingConsoleLog] = []
    private var capacity: Int = 300

    public func setCapacity(_ newCapacity: Int) {
        let value = max(1, newCapacity)
        queue.sync {
            capacity = value
            if buffer.count > value {
                buffer.removeFirst(buffer.count - value)
            }
        }
        notifyChanged()
    }

    public func add(_ log: SparklingConsoleLog) {
        queue.sync {
            if buffer.count >= capacity {
                buffer.removeFirst(buffer.count - capacity + 1)
            }
            buffer.append(log)
        }
        notifyChanged()
    }

    public func snapshot() -> [SparklingConsoleLog] {
        return queue.sync { buffer }
    }

    public func update(_ log: SparklingConsoleLog) {
        queue.sync {
            if let idx = buffer.firstIndex(where: { $0.id == log.id }) {
                buffer[idx] = log
            }
        }
    }

    public func clear() {
        queue.sync { buffer.removeAll() }
        notifyChanged()
    }

    private func notifyChanged() {
        let post = {
            NotificationCenter.default.post(name: SparklingConsoleStore.didChangeNotification, object: self)
        }
        if Thread.isMainThread {
            post()
        } else {
            DispatchQueue.main.async(execute: post)
        }
    }
}
