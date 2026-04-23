// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import UIKit

extension UIApplication: SPKKitCompatible {}

public extension SPKKitWrapper where Base == UIApplication {
    static var mainWindow: UIWindow? {
        if #available(iOS 13.0, *) {
            let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
            if let key = scenes.flatMap({ $0.windows }).first(where: { $0.isKeyWindow }) {
                return key
            }
            if let any = scenes.flatMap({ $0.windows }).first {
                return any
            }
        }
        var window: UIWindow? = UIApplication.shared.delegate?.window ?? nil
        if !(window is UIView) {
            window = UIApplication.shared.keyWindow
        }
        if window == nil {
            window = UIApplication.shared.windows.first
        }
        return window
    }
}
