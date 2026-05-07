// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import UIKit

public extension SPKKitWrapper where Base: UIView {
    var viewController: UIViewController? {
        var view: UIView? = base
        while view != nil {
            let nextResponsder = view?.next
            if let nextResponsder = nextResponsder as? UIViewController {
                return nextResponsder
            }
            view = view?.superview
        }
        return nil
    }
    var safeAreaInsets: UIEdgeInsets {
        if #available(iOS 13.0, *) {
            return base.safeAreaInsets
        }

        var safeInset = UIEdgeInsets.zero

        if #available(iOS 11.0, *) {
            safeInset = base.safeAreaInsets
        }
        
        let viewFrameInWindow = base.convert(base.bounds, to: nil)

        let statusBarView = UIApplication.shared.value(forKey: "statusBar") as? UIView
        if (viewFrameInWindow.origin.y < 30 && !UIApplication.shared.isStatusBarHidden) || statusBarView?.frame.size.height ?? 0 <= 0 {
            if safeInset.top <= 0 {
                safeInset.top = UIApplication.shared.statusBarFrame.size.height - viewFrameInWindow.origin.y
            }
            if safeInset.top <= 0 {
                safeInset.top = 20 - viewFrameInWindow.origin.y
            }
        }
        return safeInset
    }
}
