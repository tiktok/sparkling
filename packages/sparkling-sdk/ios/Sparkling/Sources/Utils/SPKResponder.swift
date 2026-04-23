// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

//@objcMembers
class SPKResponder: NSObject {
    
    static var topViewController: UIViewController? {
        return self.topViewControllerForController(rootViewController: UIApplication.shared.keyWindow?.rootViewController)
    }
    
    static func topViewControllerForController(rootViewController: UIViewController?) -> UIViewController? {
        guard rootViewController != nil else {
            return nil
        }
        
        if let navigationCotnroller = rootViewController as? UINavigationController {
            return self.topViewControllerForController(rootViewController: navigationCotnroller.viewControllers.last)
        }
        
        if let tabController = rootViewController as? UITabBarController {
            return self.topViewControllerForController(rootViewController: tabController.selectedViewController)
        }
        
        if let presentedViewController = rootViewController?.presentedViewController {
            return self.topViewControllerForController(rootViewController: presentedViewController)
        }
        return rootViewController
    }
    
    public static func topViewController(for viewController: UIViewController) -> UIViewController? {
        if let navVC = viewController as? UINavigationController,
           let lastVC = navVC.viewControllers.last {
            return topViewController(for: lastVC)
        } else if let barVC = viewController as? UITabBarController,
                  let selectedVC = barVC.selectedViewController {
            return topViewController(for: selectedVC)
        } else if let presentedVC = viewController.presentedViewController {
            return topViewController(for: presentedVC)
        } else {
            return viewController
        }
    }

    public static func topViewController(for view: UIView) -> UIViewController? {
        var responder: UIResponder? = view
        while responder != nil, !(responder is UIViewController) {
            responder = responder?.next
        }
        guard let vc: UIViewController = (responder as? UIViewController) ?? UIWindow.spk.keyWindow?.rootViewController else { return nil }
        return topViewController(for: vc)
    }
    
    public static func topViewController(for responder: UIResponder) -> UIViewController? {
        switch responder {
        case let controller as UIViewController: return topViewController(for: controller)
        case let view as UIView: return topViewController(for: view)
        default: return topViewController
        }
    }

    static func isTopViewController(viewController: UIViewController?) -> Bool {
        guard let topViewController = topViewController else {
            return false
        }
        return self.topViewController == topViewController
    }
}
