// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import UIKit
#if canImport(Sparkling_DebugTool)
import Sparkling_DebugTool
#endif

/// Wires the Sparkling debug tool into the app lifecycle. Only active when
/// the DebugTool pod is linked (i.e. debug builds). The debug tool itself
/// auto-installs the JS console sink, GlobalProps provider, Sparkling Method
/// observer, and floating-ball tap action — the host only needs to flip the
/// floating ball on.
enum DebugConsoleSupport {
    static func setup() {
        #if canImport(Sparkling_DebugTool)
        SparklingDebugTool.setFloatingBallEnabled(true)
        #endif
    }

    static func presentConsole(from presenter: UIViewController) {
        #if canImport(Sparkling_DebugTool)
        SparklingDebugTool.presentConsolePanel(from: presenter)
        #else
        let alert = UIAlertController(
            title: "JS Console",
            message: "DebugTool is not available in this build.",
            preferredStyle: .alert
        )
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        presenter.present(alert, animated: true)
        #endif
    }
}
