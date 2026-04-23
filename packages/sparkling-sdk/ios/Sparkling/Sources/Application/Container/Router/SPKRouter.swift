// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// A router class that manages navigation and page creation within the SPK framework.
/// 
/// SPKRouter provides static methods for creating, opening, and managing SPK pages
/// and view controllers. It handles URL-based navigation, system browser integration,
/// and view controller lifecycle management. The @objcMembers attribute ensures
/// Objective-C compatibility for all properties and methods.
@objcMembers
public class SPKRouter: NSObject {
    
    /// Creates a new SPK page container with the specified URL and context.
    /// 
    /// This method resolves the provided URL using the scheme resolver, creates appropriate
    /// configuration parameters, and instantiates a SPKViewController as the page container.
    /// 
    /// - Parameters:
    ///   - url: The URL string for the page to create.
    ///   - context: The context object containing page configuration. Creates a new context if nil.
    ///   - frame: The frame for the view controller. Defaults to screen bounds.
    /// - Returns: A view controller that conforms to SPKPageContainerProtocol.
    public static func create(withURL url: String?, context: SPKContext? = nil, frame: CGRect = UIScreen.main.bounds) -> (UIViewController & SPKContainerProtocol) {
        let context = context ?? SPKContext()
        let config = SPKSchemeParam.resolver(withScheme: URL.spk.url(string: url ?? ""), context: context) as? SPKSchemeParam
        
        let pageContainer = SPKViewController(withURL: config?.resolvedURL, config: config, context: context, frame: frame)
        return pageContainer
    }
    
    /// Opens a SPK page by pushing it onto the current navigation stack.
    /// 
    /// This method creates a new page container and attempts to push it onto the current
    /// navigation controller. It requires a valid navigation controller in the view hierarchy.
    /// 
    /// - Parameters:
    ///   - urlString: The URL string for the page to open.
    ///   - context: The context object containing page configuration.
    /// - Returns: A tuple containing the created container and success status, or nil if failed.
    public static func open(withURL urlString: String?, context: SPKContext?) -> ((UIViewController & SPKContainerProtocol)?, Bool)? {
        guard let urlString = urlString else {
            return nil
        }
        context?.originURL = urlString
        let container = self.create(withURL: urlString, context: context)
        if let container = container as? (UIViewController & SPKContainerProtocol),
           let topVC = SPKResponder.topViewController {
            if let naviVC = topVC.navigationController as? UINavigationController {
                naviVC.pushViewController(container, animated: true)
            } else if let naviVC = topVC.children.last as? UINavigationController {
                // Currently, we only support returning a NavigationController as in SwiftUI.
                // In this situation, the topVC should be the UIHostingController, and it its navigationController is null.
                // We have to use topVC.children.last to get the navigationController.
                naviVC.pushViewController(container, animated: true)
            } else {
                return (nil, false)
            }
            return (container, true)
        }
        return (nil, false)
    }
    
    /// Opens a URL in the system's default web browser.
    ///
    /// This method validates the URL and opens it using the system browser if it's a valid
    /// HTTP or HTTPS URL. Only web URLs are supported for security reasons.
    ///
    /// - Parameter urlString: The URL string to open in the system browser.
    /// - Returns: Boolean indicating whether the URL was successfully opened.
    public static func openInSystemBrowser(withURL urlString: String?) -> Bool {
        guard let urlString = urlString, !urlString.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return false
        }

        guard let url = URL(string: urlString) else {
            return false
        }

        guard let scheme = url.scheme?.lowercased(), scheme == "http" || scheme == "https" else {
            return false
        }

        if UIApplication.shared.canOpenURL(url) {
            UIApplication.shared.open(url, options: [:]) { success in
                if !success {
                    print("[SPKRouter] Failed to open URL in system browser: \(urlString)")
                }
            }
            return true
        }

        return false
    }
    
    /// Closes the top view controller in the current navigation stack.
    /// 
    /// This method intelligently determines how to close the current view controller:
    /// - If there are multiple view controllers in the navigation stack, it pops the top one.
    /// - If the navigation controller is presented modally, it dismisses the entire stack.
    /// - Does nothing if no appropriate navigation context is found.
    public static func closeTopViewController() {
        guard let topVC = SPKResponder.topViewController, let naviVC = topVC.children.last as? UINavigationController else {
            return
        }
        if naviVC.viewControllers.count > 1 {
            naviVC.popViewController(animated: true)
            return
        }
        if naviVC.presentingViewController != nil {
            naviVC.dismiss(animated: true, completion: nil)
            return
        }
    }
}
