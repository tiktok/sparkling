// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Lynx
import UIKit

@objcMembers
public class SparklingDebugTool: NSObject {
    private static let devURLKey = "sparkling.debug.dev_url"

    public static func setup() {
        // Preset values must be set BEFORE LynxEnv flags so the DevTool
        // service picks them up during initialization.
        if let devtool = LynxServices.getInstanceWith(
            NSProtocolFromString("LynxServiceDevToolProtocol")!
        ) as? LynxServiceDevToolProtocol {
            devtool.logBoxPresetValue = true
            devtool.lynxDebugPresetValue = true
        }

        let env = LynxEnv.sharedInstance()
        env.lynxDebugEnabled = true
        env.devtoolEnabled = true
        env.logBoxEnabled = true

        // Enable DebugRouter so the DevTool desktop app can discover and
        // connect to this app (via USB or network).
        if let routerClass = NSClassFromString("DebugRouter"),
           let instance = routerClass.perform(NSSelectorFromString("instance"))?.takeUnretainedValue(),
           instance.responds(to: NSSelectorFromString("enableAllSessions")) {
            instance.perform(NSSelectorFromString("enableAllSessions"))
        }
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
            alert.addAction(UIAlertAction(title: "Save", style: .default, handler: { _ in
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
