// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SparklingMethod

extension SPKRouter {
    public static func close(container: PipeContainer?) -> Bool {
        guard let uiResponder = container as? UIResponder else {
            return false
        }

        guard let targetVC = viewController(for: uiResponder) else {
            return false
        }

        if let naviVC = targetVC.navigationController {
            if SPKResponder.isTopViewController(viewController: targetVC) {
                if naviVC.viewControllers.count == 1 && naviVC.presentingViewController != nil {
                    naviVC.dismiss(animated: true)
                } else {
                    naviVC.popViewController(animated: true)
                }
            } else {
                let removeTargetFromStack = {
                    let currentStack = naviVC.viewControllers
                    let stack = currentStack.filter { $0 !== targetVC }
                    guard stack.count != currentStack.count else { return }
                    naviVC.setViewControllers(stack, animated: false)
                }

                if let transitionCoordinator = naviVC.transitionCoordinator {
                    // Route replacement can close the source while the push is animating.
                    let transitionScheduled = transitionCoordinator.animate(alongsideTransition: nil) { context in
                        guard !context.isCancelled else { return }
                        removeTargetFromStack()
                    }
                    if !transitionScheduled {
                        removeTargetFromStack()
                    }
                } else {
                    removeTargetFromStack()
                }
            }
        } else {
            targetVC.dismiss(animated: true)
        }
        return true
    }

    public static func viewController(for responder: UIResponder) -> UIViewController? {
        var nextRepsonder: UIResponder? = responder
        while let responder = nextRepsonder {
            if let vc = responder as? UIViewController {
                return vc
            }
            nextRepsonder = responder.next
        }
        return nil
    }
}
