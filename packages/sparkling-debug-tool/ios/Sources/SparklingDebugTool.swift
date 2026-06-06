// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import DebugRouter
import Foundation
import Lynx
import UIKit

@objcMembers
public class SparklingDebugTool: NSObject {
    private static let devURLKey = "sparkling.debug.dev_url"

    public static func setup() {
        // Preset values must be set BEFORE LynxEnv flags so the DevTool
        // service picks them up during initialization.

        if let devtool = LynxServices.getInstanceWith(LynxServiceDevToolProtocol.self)
            as? LynxServiceDevToolProtocol
        {
            devtool.logBoxPresetValue = true
            devtool.lynxDebugPresetValue = true
        }

        let env = LynxEnv.sharedInstance()
        env.lynxDebugEnabled = true
        env.devtoolEnabled = true
        env.logBoxEnabled = true

        // Enable DebugRouter so the DevTool desktop app can discover and
        // connect to this app (via USB or network).
        DebugRouter.instance().enableAllSessions()
    }

    public static func devURL(fallback: String) -> String {
        let stored = UserDefaults.standard.string(forKey: devURLKey)?.trimmingCharacters(in: .whitespacesAndNewlines)
        if let stored, !stored.isEmpty {
            return stored
        }
        return fallback
    }

    public static func setDevURL(_ url: String) {
        let normalized = url.trimmingCharacters(in: .whitespacesAndNewlines)
        UserDefaults.standard.set(normalized, forKey: devURLKey)
    }

    /// Attach the JS console sink to the given LynxView so that any
    /// `console.*` payload it emits flows into [SparklingConsoleStore].
    @discardableResult
    public static func attachConsoleSink(to lynxView: LynxView) -> Bool {
        return SparklingLynxConsoleSink.attach(to: lynxView)
    }

    /// Detach the JS console sink previously attached via [attachConsoleSink].
    public static func detachConsoleSink(from lynxView: LynxView) {
        SparklingLynxConsoleSink.detach(from: lynxView)
    }

    /// Show or hide the Sparkling bottom-left debugTag. The tag is hosted in
    /// a dedicated overlay window so it follows the foreground scene.
    /// Enabling the tag also auto-installs the LynxView lifecycle tracker so
    /// the JS console + GlobalProps panels start receiving data without any
    /// host wiring.
    public static func setFloatingBallEnabled(_ enabled: Bool) {
        SparklingFloatingBallManager.shared.setEnabled(enabled)
        if enabled {
            SparklingDebugAutoWiring.installOnce()
            // The default tap presents the unified inspector.
            SparklingFloatingBallManager.shared.setActionHandler(SparklingDefaultFloatingBallHandler.shared)
        }
    }

    /// Whether the debugTag entry is currently enabled.
    public static var isFloatingBallEnabled: Bool {
        SparklingFloatingBallManager.shared.isEnabled
    }

    /// Override the default tap behaviour of the debugTag entry.
    public static func setFloatingBallActionHandler(_ handler: SparklingFloatingBallActionHandler?) {
        SparklingFloatingBallManager.shared.setActionHandler(handler)
    }

    /// Present the unified inspector panel (Console / GlobalProps / Method
    /// invocations) as a half-sheet modal.
    public static func presentInspectorPanel(
        from presenter: UIViewController,
        initialTab: SparklingInspectorViewController.Tab = .console
    ) {
        DispatchQueue.main.async {
            let inspector = SparklingInspectorViewController(initialTab: initialTab)
            Self.applyHalfSheetPresentation(to: inspector)
            presenter.present(inspector, animated: true)
        }
    }

    /// Present the inspector with the Console tab selected.
    public static func presentConsolePanel(from presenter: UIViewController) {
        presentInspectorPanel(from: presenter, initialTab: .console)
    }

    /// Present the inspector with the GlobalProps tab selected.
    public static func presentGlobalPropsPanel(from presenter: UIViewController) {
        presentInspectorPanel(from: presenter, initialTab: .globalProps)
    }

    /// Present the inspector with the Sparkling Method tab selected.
    public static func presentMethodInvocationPanel(from presenter: UIViewController) {
        presentInspectorPanel(from: presenter, initialTab: .methods)
    }

    /// Configure a navigation controller to be presented as a draggable
    /// half-height sheet (medium detent by default, expandable to large).
    /// Falls back to system page sheet on iOS 13/14.
    public static func applyHalfSheetPresentation(to viewController: UIViewController) {
        viewController.modalPresentationStyle = .pageSheet
        if #available(iOS 15.0, *) {
            if let sheet = viewController.sheetPresentationController {
                sheet.detents = [.medium(), .large()]
                sheet.prefersGrabberVisible = true
                sheet.prefersScrollingExpandsWhenScrolledToEdge = false
                sheet.prefersEdgeAttachedInCompactHeight = true
                sheet.widthFollowsPreferredContentSizeWhenEdgeAttached = true
                sheet.preferredCornerRadius = 0
            }
        }
    }

    /// Register a closure that returns one snapshot per live LynxView. The
    /// inspector panel calls this on demand.
    public static func setGlobalPropsProvider(_ provider: (() -> [SparklingGlobalPropsSnapshot])?) {
        SparklingGlobalPropsRegistry.shared.setProvider(provider)
    }

    /// Append a method invocation record (or update an existing one with the
    /// same id) into the in-memory store. The host normally registers a
    /// `SparklingMethodInvocationObserver` that calls these helpers.
    public static func recordMethodInvocation(_ invocation: SparklingMethodInvocation) {
        SparklingMethodInvocationStore.shared.add(invocation)
    }

    public static func updateMethodInvocation(_ invocation: SparklingMethodInvocation) {
        SparklingMethodInvocationStore.shared.update(invocation)
    }

    public static func clearMethodInvocations() {
        SparklingMethodInvocationStore.shared.clear()
    }

    public static func showDevURLDialog(
        from viewController: UIViewController,
        initialURL: String?,
        onSaved: ((String) -> Void)? = nil
    ) {
        DispatchQueue.main.async {
            let alert = UIAlertController(
                title: "Set Sparkling Dev URL",
                message: "Update the main Lynx bundle URL for debug mode.",
                preferredStyle: .alert
            )

            alert.addTextField { textField in
                textField.placeholder = "http://localhost:5969/main.lynx.bundle"
                textField.keyboardType = .URL
                textField.text = initialURL
                textField.clearButtonMode = .whileEditing
            }

            alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
            alert.addAction(
                UIAlertAction(
                    title: "Save", style: .default,
                    handler: { _ in
                        let value = alert.textFields?.first?.text?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                        guard !value.isEmpty else {
                            return
                        }
                        setDevURL(value)
                        onSaved?(value)
                    }))

            viewController.present(alert, animated: true)
        }
    }
}
