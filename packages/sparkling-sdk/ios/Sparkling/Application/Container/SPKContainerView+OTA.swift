// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import UIKit
import SnapKit
import ObjectiveC

private var standaloneOtaControllerKey: UInt8 = 0

final class SPKStandaloneOtaController {
    weak var container: SPKContainerView?
    weak var badge: UIButton?
    var observer: NSObjectProtocol?

    init(container: SPKContainerView) {
        self.container = container
    }

    func sync() {
        guard let container else {
            return
        }
        guard !isManagedByViewController(container) else {
            teardownBadge()
            return
        }
        guard container.window != nil else {
            teardownBadge()
            SPKZephyrOTAManager.shared.stopPolling()
            return
        }
        guard let config = SPKZephyrOTAManager.shared.readRuntimeConfig(),
              config.enabled ?? false else {
            teardownBadge()
            return
        }

        installObserverIfNeeded()
        installBadgeIfNeeded()
        SPKZephyrOTAManager.shared.startPolling()
        updateBadgeVisibility()
    }

    func cleanup() {
        teardownBadge()
        if let observer {
            NotificationCenter.default.removeObserver(observer)
            self.observer = nil
        }
    }

    func installObserverIfNeeded() {
        guard observer == nil else {
            return
        }
        observer = NotificationCenter.default.addObserver(
            forName: SPKZephyrOTAManager.updateReadyNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            guard let self else {
                return
            }
            NSLog("SPKContainerView OTA update ready for container=%@", self.container?.containerID ?? "")
            self.updateBadgeVisibility()
        }
    }

    func installBadgeIfNeeded() {
        guard let container else {
            return
        }
        if let badge {
            container.bringSubviewToFront(badge)
            return
        }

        let button = UIButton(type: .system)
        button.setTitle("Update ready · tap", for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.backgroundColor = UIColor(white: 0.12, alpha: 0.92)
        button.titleLabel?.font = UIFont.systemFont(ofSize: 12.0, weight: .semibold)
        button.contentEdgeInsets = UIEdgeInsets(top: 6, left: 10, bottom: 6, right: 10)
        button.layer.cornerRadius = 12.0
        button.layer.masksToBounds = true
        button.isHidden = true
        button.addTarget(self, action: #selector(handleBadgeTap), for: .touchUpInside)
        container.addSubview(button)
        button.snp.makeConstraints { make in
            make.top.equalTo(container.safeAreaLayoutGuide.snp.top).offset(12)
            make.trailing.equalTo(container).offset(-16)
        }
        badge = button
    }

    func teardownBadge() {
        badge?.removeFromSuperview()
        badge = nil
    }

    func updateBadgeVisibility() {
        let shouldShow = SPKZephyrOTAManager.shared.hasPendingUpdate()
        badge?.isHidden = !shouldShow
        if shouldShow, let badge, let container {
            container.bringSubviewToFront(badge)
        }
    }

    @objc
    func handleBadgeTap() {
        guard let container,
              let originURL = container.originURL else {
            return
        }
        guard SPKZephyrOTAManager.shared.applyPendingUpdateIfNeeded() else {
            updateBadgeVisibility()
            return
        }

        let reloadURL = container.config?.originURL ?? originURL
        let context = container.context as? SPKContext ?? SPKContext()
        let resolved = SPKScheme.resolver(withScheme: reloadURL, context: context, paramClass: SPKSchemeParam.self)
        NSLog("SPKContainerView applying OTA: origin=%@ reload=%@ resolved=%@",
              originURL.absoluteString,
              reloadURL.absoluteString,
              resolved?.resolvedURL?.absoluteString ?? "nil")
        container.load(withParams: resolved ?? SPKSchemeParam.resolver(withScheme: reloadURL, context: context), context, forceInitKitView: false)
        updateBadgeVisibility()
    }

    func isManagedByViewController(_ container: SPKContainerView) -> Bool {
        var responder: UIResponder? = container
        while let current = responder {
            if current is SPKViewController {
                return true
            }
            responder = current.next
        }
        return false
    }
}

extension SPKContainerView {
    var spk_standaloneOtaController: SPKStandaloneOtaController {
        if let controller = objc_getAssociatedObject(self, &standaloneOtaControllerKey) as? SPKStandaloneOtaController {
            return controller
        }
        let controller = SPKStandaloneOtaController(container: self)
        objc_setAssociatedObject(self, &standaloneOtaControllerKey, controller, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return controller
    }
}
