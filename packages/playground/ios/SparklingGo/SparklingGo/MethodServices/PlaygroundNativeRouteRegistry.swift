// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License, Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import UIKit
import SparklingMethod

/// Playground-only registry for `router.open({ scheme: "hybrid://native_route=<id>" })`.
/// Add new native pages with `register(_:handler:)`.
final class PlaygroundNativeRouteRegistry {

    static let shared = PlaygroundNativeRouteRegistry()

    static let nativeRouteSchemePrefix = "hybrid://native_route="

    typealias NativeRouteHandler = (@escaping SparklingMethod.PipeMethod.CompletionBlock) -> Void

    private var routes: [String: NativeRouteHandler] = [:]

    private init() {
        installDefaultRoutes()
    }

    func installDefaultRoutes() {
        register("card_demo") { [weak self] completion in
            self?.presentRoot(CardViewDemoViewController(), completion: completion)
        }
        register("debug_tool_switch") { [weak self] completion in
            self?.presentRoot(DebugToolSwitchViewController(), completion: completion)
        }
    }

    func register(_ routeId: String, handler: @escaping NativeRouteHandler) {
        routes[routeId] = handler
    }

    func unregister(_ routeId: String) {
        routes.removeValue(forKey: routeId)
    }

    /// - Returns: `true` if the scheme is a native-route URL and `routeId` was registered.
    func tryOpen(scheme: String, completion: @escaping SparklingMethod.PipeMethod.CompletionBlock) -> Bool {
        guard scheme.hasPrefix(Self.nativeRouteSchemePrefix) else {
            return false
        }
        let rest = String(scheme.dropFirst(Self.nativeRouteSchemePrefix.count))
        let routeId = rest.split(separator: "&", maxSplits: 1, omittingEmptySubsequences: false).first.map(String.init) ?? ""
        guard let handler = routes[routeId] else {
            return false
        }
        handler(completion)
        return true
    }

    private func presentRoot(_ viewController: UIViewController, completion: @escaping SparklingMethod.PipeMethod.CompletionBlock) {
        guard let topVC = Self.topViewController() else {
            completion(.failed(message: "No view controller to present from"), nil)
            return
        }
        if let nav = topVC.navigationController {
            nav.pushViewController(viewController, animated: true)
        } else {
            let nav = UINavigationController(rootViewController: viewController)
            nav.modalPresentationStyle = .fullScreen
            topVC.present(nav, animated: true)
        }
        completion(.succeeded(), nil)
    }

    private static func topViewController(base: UIViewController? = nil) -> UIViewController? {
        let root = base ?? UIApplication.shared.connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow?.rootViewController }
            .first
        if let nav = root as? UINavigationController {
            return topViewController(base: nav.visibleViewController)
        }
        if let presented = root?.presentedViewController {
            return topViewController(base: presented)
        }
        return root
    }
}
