// Copyright (c) 2022 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.
import Foundation

public extension SPKKitWrapper where Base: UIWindow {
    static var keyWindow: UIWindow? {
        if #available(iOS 13, *) {
            let oldKeyWindow = UIApplication.shared.keyWindow
            let activeWindowScenes = UIApplication.shared
                .connectedScenes
                .filter { $0.activationState == .foregroundActive }
                .compactMap { $0 as? UIWindowScene }

            var result: UIWindow?
            if let scene = activeWindowScenes.first {
                result = keyWindow(from: scene)
            }
            if activeWindowScenes.count > 1 {
                if let scene = oldKeyWindow?.windowScene {
                    result = keyWindow(from: scene)
                }
            }
            if result == nil {
                result = UIApplication.shared.windows.first { $0.isKeyWindow }
            }
            if result == nil,
               let oldKeyWindows = oldKeyWindow,
               oldKeyWindows.isKeyWindow {
                result = oldKeyWindows
            }
            if result == nil,
               let delegate = UIApplication.shared.delegate,
               delegate.responds(to: #selector(getter: UIApplicationDelegate.window)) {
                result = UIApplication.shared.delegate?.window ?? nil
            }
            return result
        } else {
            return UIApplication.shared.keyWindow
        }
    }

    @available(iOS 13, *)
    private static func keyWindow(from windowScene: UIWindowScene) -> UIWindow? {
        windowScene.windows.first { $0.isKeyWindow }
    }
}
