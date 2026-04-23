// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

public extension SPKKitWrapper where Base: DispatchQueue {
    @discardableResult
    static func syncGlobal(timeout: TimeInterval = 0.01, flags: DispatchWorkItemFlags = [], execute work: @escaping @convention(block) () -> Void) -> DispatchTimeoutResult {
        var timeout = timeout
        if Thread.isMainThread {
            if timeout <= 0 {
                timeout = 0.01
            }
            let qos = DispatchQoS(qosClass: .userInitiated, relativePriority: -8)
            let task_work = DispatchWorkItem(qos: qos, flags: flags, block: work)
            DispatchQueue.global(qos: .default).async(execute: task_work)
            return task_work.wait(timeout: .now() + timeout)
        } else {
            work()
            return .success
        }
    }
    
    static func syncMain(flags: DispatchWorkItemFlags = [], execute work: @escaping @convention(block) () -> Void) {
        if isMain {
            work()
        } else {
            DispatchQueue.main.sync(flags: flags, execute: work)
        }
    }

    static func asyncMain(group: DispatchGroup? = nil, qos: DispatchQoS = .unspecified, flags: DispatchWorkItemFlags = [], execute work: @escaping @convention(block) () -> Void) {
        if isMain {
            work()
        } else {
            DispatchQueue.main.async(group: group, qos: qos, flags: flags, execute: work)
        }
    }

    static var isMain: Bool {
        return currentLabel == DispatchQueue.main.label
    }
    
    private static var currentLabel: String {
        return String(validatingUTF8: __dispatch_queue_get_label(nil)) ?? ""
    }
    
}
