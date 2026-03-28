// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Sparkling
import UIKit
#if canImport(DebugTool)
import DebugTool
#endif

private let defaultMainDevBundleURL = "http://localhost:5969/main.lynx.bundle"

enum DebugDevURLSupport {
    static func mainScheme() -> String {
        #if DEBUG
        let url = storedDevURL(fallback: defaultMainDevBundleURL)
        return mainScheme(withDebugURL: url)
        #else
        return "hybrid://lynxview?bundle=.%2Fmain.lynx.bundle&hide_status_bar=1&hide_nav_bar=1"
        #endif
    }

    static func mainScheme(withDebugURL url: String) -> String {
        let encodedURL = url.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? url
        return "hybrid://lynxview?url=\(encodedURL)&hide_status_bar=1&hide_nav_bar=1"
    }

    static func networkBundleURL(fromScheme scheme: String?) -> String? {
        guard let scheme, !scheme.isEmpty,
              let components = URLComponents(string: scheme),
              let urlValue = components.queryItems?.first(where: { $0.name == "url" })?.value else {
            return nil
        }
        let normalized = urlValue.removingPercentEncoding ?? urlValue
        guard normalized.hasPrefix("http://") || normalized.hasPrefix("https://") else {
            return nil
        }
        return normalized
    }

    static func makeContext(delegate: SPKContainerLifecycleProtocol? = nil) -> SPKContext {
        let context = SPKContext()
        let element = SparklingLynxElement(lynxElementName: "input", lynxElementClassName: LynxInput.self)
        context.customUIElements = [element]
        context.containerLifecycleDelegate = delegate
        return context
    }

    static func storedDevURL(fallback: String) -> String {
        #if canImport(DebugTool)
        SparklingDebugTool.devURL(fallback: fallback)
        #else
        fallback
        #endif
    }

    static func saveDevURL(_ url: String) {
        #if canImport(DebugTool)
        SparklingDebugTool.setDevURL(url)
        #endif
    }

    static func showDevURLDialog(from controller: UIViewController, initialURL: String?, onSaved: @escaping (String) -> Void) {
        #if canImport(DebugTool)
        SparklingDebugTool.showDevURLDialog(from: controller, initialURL: initialURL, onSaved: onSaved)
        #else
        onSaved(initialURL ?? defaultMainDevBundleURL)
        #endif
    }
}

final class DevURLLoadFailedDelegate: NSObject, SPKContainerLifecycleProtocol {
    private weak var navigationController: UINavigationController?
    private let frameProvider: () -> CGRect
    private var isPrompting = false

    init(navigationController: UINavigationController, frameProvider: @escaping () -> CGRect) {
        self.navigationController = navigationController
        self.frameProvider = frameProvider
        super.init()
    }

    func container(_ container: SPKContainerProtocol, didLoadFailedWithURL url: URL?, error: Error?) {
        #if DEBUG
        guard !isPrompting,
              let navigationController,
              let failedScheme = container.originURL?.absoluteString,
              let currentURL = DebugDevURLSupport.networkBundleURL(fromScheme: failedScheme) else {
            return
        }
        isPrompting = true

        DebugDevURLSupport.showDevURLDialog(from: navigationController, initialURL: currentURL) { [weak self] updatedURL in
            guard let self, let navigationController = self.navigationController else {
                return
            }
            self.isPrompting = false
            let nextScheme = DebugDevURLSupport.mainScheme(withDebugURL: updatedURL)
            let nextContext = DebugDevURLSupport.makeContext(delegate: self)
            let nextVC = SPKRouter.create(withURL: nextScheme, context: nextContext, frame: self.frameProvider())
            navigationController.setViewControllers([nextVC], animated: false)
        }
        #endif
    }
}
