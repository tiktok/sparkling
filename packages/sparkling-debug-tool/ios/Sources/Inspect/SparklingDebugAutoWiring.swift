// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Lynx
import UIKit

/// Reflectively wires the Sparkling SDK + method invocation infrastructure
/// to the debug tool so the host needs no manual setup beyond
/// `SparklingDebugTool.setFloatingBallEnabled(true)`.
///
/// The notification names and accessors we use are defined by the SDK as
/// stable strings, so the debug tool stays decoupled from a hard
/// `Sparkling` import. When the SDK is missing at runtime the corresponding
/// hook silently no-ops.
internal enum SparklingDebugAutoWiring {
    private static var installed = false
    private static var liveLynxRegistry = LiveWrapperLynxRegistry()
    private static var didCreateObserver: NSObjectProtocol?
    private static var willDestroyObserver: NSObjectProtocol?
    private static var methodStartObserver: NSObjectProtocol?
    private static var methodEndObserver: NSObjectProtocol?

    static func installOnce() {
        if installed { return }
        installed = true
        installLynxLifecycleHook()
        installGlobalPropsProvider()
        installMethodInvocationObserver()
        refreshLiveLynxViews()
    }

    static func refreshLiveLynxViews() {
        guard installed else { return }
        activeWindows().forEach { scan(view: $0) }
    }

    // MARK: - LynxView lifecycle (notification-based)

    private static func installLynxLifecycleHook() {
        let didCreate = Notification.Name("SPKWrapperLynxView.didCreate")
        let willDestroy = Notification.Name("SPKWrapperLynxView.willDestroy")
        didCreateObserver = NotificationCenter.default.addObserver(
            forName: didCreate,
            object: nil,
            queue: .main
        ) { note in
            guard let view = note.object else { return }
            trackIfSupported(view)
            SparklingGlobalPropsRegistry.shared.notifyChanged()
        }
        willDestroyObserver = NotificationCenter.default.addObserver(
            forName: willDestroy,
            object: nil,
            queue: .main
        ) { note in
            guard let view = note.object else { return }
            if let lynxView = view as? LynxView {
                SparklingLynxConsoleSink.detach(from: lynxView)
            }
            liveLynxRegistry.remove(view as AnyObject)
            SparklingGlobalPropsRegistry.shared.notifyChanged()
        }
    }

    // MARK: - GlobalProps provider

    private static func installGlobalPropsProvider() {
        SparklingGlobalPropsRegistry.shared.setProvider { [registry = liveLynxRegistry] in
            registry.snapshot()
        }
    }

    private static func activeWindows() -> [UIWindow] {
        if #available(iOS 13.0, *) {
            return UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .flatMap { $0.windows }
        }
        return UIApplication.shared.windows
    }

    private static func scan(view: UIView) {
        trackIfSupported(view)
        view.subviews.forEach { scan(view: $0) }
    }

    private static func trackIfSupported(_ view: Any) {
        if let lynxView = view as? LynxView {
            SparklingLynxConsoleSink.attach(to: lynxView)
        }
        let object = view as AnyObject
        if object.responds(to: NSSelectorFromString("globalPropsDictionary")) {
            liveLynxRegistry.add(object)
        }
    }

    // MARK: - Sparkling Method observer (Notification-based)

    /// Subscribe to the notifications posted by
    /// `SparklingMethodInvocationCenter.notifyStart/End`. Using the
    /// notification names as raw strings keeps the debug-tool decoupled from
    /// the `SparklingMethod` module; the SDK posts these unconditionally.
    private static func installMethodInvocationObserver() {
        let didStart = Notification.Name("SparklingMethodInvocationCenter.didStart")
        let didEnd = Notification.Name("SparklingMethodInvocationCenter.didEnd")
        methodStartObserver = NotificationCenter.default.addObserver(
            forName: didStart,
            object: nil,
            queue: .main
        ) { note in
            guard let info = note.userInfo else { return }
            let record = makeRecord(from: info, completed: false)
            SparklingMethodInvocationStore.shared.add(record)
        }
        methodEndObserver = NotificationCenter.default.addObserver(
            forName: didEnd,
            object: nil,
            queue: .main
        ) { note in
            guard let info = note.userInfo else { return }
            let record = makeRecord(from: info, completed: true)
            SparklingMethodInvocationStore.shared.update(record)
        }
    }

    private static func makeRecord(from info: [AnyHashable: Any], completed: Bool) -> SparklingMethodInvocation {
        let id = (info["id"] as? String) ?? UUID().uuidString
        let name = (info["name"] as? String) ?? "(unknown)"
        let namespace = info["namespace"] as? String
        let platform = (info["platform"] as? String) ?? "lynx"
        let params = SparklingMethodInvocation.format(info["params"])
        let result = completed ? SparklingMethodInvocation.format(info["result"]) : nil
        let statusCode = info["statusCode"] as? Int
        let statusMessage = completed ? info["statusMessage"] as? String : nil
        let startTime = (info["startTime"] as? Date) ?? Date()
        let endTime = completed ? info["endTime"] as? Date : nil
        return SparklingMethodInvocation(
            id: id,
            name: name,
            namespace: namespace,
            platform: platform,
            params: params,
            result: result,
            statusCode: statusCode,
            statusMessage: statusMessage,
            startTime: startTime,
            endTime: endTime
        )
    }
}

/// Tracks every live `SPKWrapperLynxView` (held weakly via `AnyObject`) so the
/// GlobalProps panel can collect snapshots on demand without a hard SDK
/// dependency. Property access goes through KVC, which works because
/// `SPKWrapperLynxView` is annotated with `@objcMembers`.
private final class LiveWrapperLynxRegistry {
    private let lock = NSLock()
    private var entries: [Weak] = []

    private struct Weak { weak var ref: AnyObject? }

    func add(_ obj: AnyObject) {
        lock.lock(); defer { lock.unlock() }
        entries.removeAll { $0.ref === obj || $0.ref == nil }
        entries.append(Weak(ref: obj))
    }

    func remove(_ obj: AnyObject) {
        lock.lock(); defer { lock.unlock() }
        entries.removeAll { $0.ref === obj || $0.ref == nil }
    }

    func snapshot() -> [SparklingGlobalPropsSnapshot] {
        lock.lock()
        let live = entries.compactMap { $0.ref }
        entries = live.map { Weak(ref: $0) }
        lock.unlock()
        return live.compactMap { obj -> SparklingGlobalPropsSnapshot? in
            let mirror = obj as AnyObject
            guard mirror.responds(to: NSSelectorFromString("globalPropsDictionary")) else {
                return nil
            }
            let props = (mirror.value(forKey: "globalPropsDictionary") as? [String: Any]) ?? [:]
            var rest = props
            rest.removeValue(forKey: "queryItems")
            let containerID = (mirror.value(forKey: "containerID") as? String)
                ?? (mirror.value(forKey: "containerId") as? String)
                ?? "(unknown)"
            let url: String? = {
                let sel = NSSelectorFromString("loadURLString")
                if mirror.responds(to: sel),
                   let unmanaged = mirror.perform(sel),
                   let str = unmanaged.takeUnretainedValue() as? String {
                    return str
                }
                return nil
            }()
            let queryItems = (props["queryItems"] as? [String: Any]).flatMap { $0.isEmpty ? nil : $0 }
                ?? Self.parseQueryItems(from: url)
            return SparklingGlobalPropsSnapshot(
                containerId: containerID,
                templateUrl: url,
                globalProps: rest,
                queryItems: queryItems
            )
        }
    }

    private static func parseQueryItems(from url: String?) -> [String: Any] {
        guard let url, !url.isEmpty, let components = URLComponents(string: url) else { return [:] }
        var result: [String: Any] = [:]
        components.queryItems?.forEach { item in
            result[item.name] = item.value ?? ""
        }
        return result
    }
}

/// Default debugTag action handler installed by the debug tool when no custom
/// handler is set; opens the unified inspector panel.
internal final class SparklingDefaultFloatingBallHandler: NSObject, SparklingFloatingBallActionHandler {
    static let shared = SparklingDefaultFloatingBallHandler()

    func sparklingFloatingBallDidTap(presenter: UIViewController?) -> Bool {
        guard let presenter = presenter else { return false }
        SparklingDebugTool.presentInspectorPanel(from: presenter)
        return true
    }
}
