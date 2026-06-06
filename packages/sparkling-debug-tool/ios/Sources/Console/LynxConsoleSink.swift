// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Lynx
import LynxDevtool
import ObjectiveC.runtime

private var sparklingConsoleSinkKey: UInt8 = 0
private let sparklingConsoleSinkMaxRetries = 10

/// Forwards every `console.*` payload from a [LynxView]'s inspector console
/// to [SparklingConsoleStore].
@objc public final class SparklingLynxConsoleSink: NSObject, LynxInspectorConsoleDelegate {

    public func onConsoleMessage(_ msg: String) {
        guard !msg.isEmpty else { return }
        guard let log = SparklingConsoleLog.fromJSON(msg) else { return }
        SparklingConsoleStore.shared.add(log)
    }

    @discardableResult
    @objc public static func attach(to lynxView: LynxView) -> Bool {
        return attach(to: lynxView, retriesRemaining: sparklingConsoleSinkMaxRetries)
    }

    @discardableResult
    private static func attach(to lynxView: LynxView, retriesRemaining: Int) -> Bool {
        if objc_getAssociatedObject(lynxView, &sparklingConsoleSinkKey) is SparklingLynxConsoleSink {
            return true
        }
        guard let owner = lynxView.baseInspectorOwner else {
            guard retriesRemaining > 0 else { return false }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak lynxView] in
                guard let view = lynxView else { return }
                _ = attach(to: view, retriesRemaining: retriesRemaining - 1)
            }
            return false
        }
        let sink = SparklingLynxConsoleSink()
        owner.setLynxInspectorConsoleDelegate(sink)
        objc_setAssociatedObject(lynxView, &sparklingConsoleSinkKey, sink, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return true
    }

    @objc public static func detach(from lynxView: LynxView) {
        objc_setAssociatedObject(lynxView, &sparklingConsoleSinkKey, nil, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
    }
}
