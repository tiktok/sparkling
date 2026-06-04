// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import UIKit

/// Optional callbacks that override the debugTag default actions.
/// Return `true` to consume the event; the default action is then skipped.
@objc public protocol SparklingFloatingBallActionHandler: AnyObject {
    @objc optional func sparklingFloatingBallDidTap(presenter: UIViewController?) -> Bool
    /// Legacy callback kept for source compatibility. Sparkling no longer
    /// installs a long-press gesture to invoke this hook.
    @objc optional func sparklingFloatingBallDidLongPress(presenter: UIViewController?) -> Bool
}

/// Owns the overlay window that hosts the bottom-left debugTag, routing taps
/// to the registered handler or the default in-app debug entry point.
@objcMembers
public final class SparklingFloatingBallManager: NSObject {
    public static let shared = SparklingFloatingBallManager()

    private var overlayWindow: UIWindow?
    private weak var actionHandler: SparklingFloatingBallActionHandler?
    private var debugTag: UILabel?
    private var enabled: Bool = false

    private override init() {
        super.init()
        if #available(iOS 13.0, *) {
            NotificationCenter.default.addObserver(
                self,
                selector: #selector(sceneDidActivate),
                name: UIScene.didActivateNotification,
                object: nil
            )
        }
    }

    @available(iOS 13.0, *)
    @objc private func sceneDidActivate(_ note: Notification) {
        guard enabled, overlayWindow == nil else { return }
        DispatchQueue.main.async { [weak self] in self?.installIfNeeded() }
    }

    public func setEnabled(_ enabled: Bool) {
        if self.enabled == enabled { return }
        self.enabled = enabled
        if enabled {
            DispatchQueue.main.async { self.installIfNeeded() }
        } else {
            DispatchQueue.main.async { self.uninstall() }
        }
    }

    public var isEnabled: Bool { enabled }

    public func setActionHandler(_ handler: SparklingFloatingBallActionHandler?) {
        actionHandler = handler
    }

    private func installIfNeeded() {
        guard overlayWindow == nil else { return }

        let window: PassthroughWindow
        if #available(iOS 13.0, *) {
            guard let scene = Self.activeWindowScene() else {
                // No scene yet (e.g. very early in launch). Retry next runloop.
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { [weak self] in
                    guard let self, self.enabled else { return }
                    self.installIfNeeded()
                }
                return
            }
            window = PassthroughWindow(windowScene: scene)
        } else {
            window = PassthroughWindow(frame: UIScreen.main.bounds)
        }
        window.windowLevel = UIWindow.Level.alert + 1
        window.backgroundColor = .clear

        let host = UIViewController()
        host.view.backgroundColor = .clear
        host.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        window.rootViewController = host
        // Force the host view into the window's bounds before positioning the
        // tag (otherwise the view stays at its zero/intrinsic size on some
        // SwiftUI hosts and the tag lands off-screen).
        host.view.frame = window.bounds
        host.view.layoutIfNeeded()

        let tag = UILabel()
        tag.text = "sparkling"
        tag.font = .systemFont(ofSize: 12, weight: .semibold)
        tag.textColor = .white
        tag.backgroundColor = UIColor(red: 0.0, green: 0.43, blue: 1.0, alpha: 1.0)
        tag.layer.cornerRadius = 0
        tag.layer.masksToBounds = true
        tag.textAlignment = .center
        tag.isUserInteractionEnabled = true
        tag.translatesAutoresizingMaskIntoConstraints = false
        tag.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(debugTagTapped)))
        host.view.addSubview(tag)
        NSLayoutConstraint.activate([
            tag.leadingAnchor.constraint(equalTo: host.view.leadingAnchor),
            tag.bottomAnchor.constraint(equalTo: host.view.bottomAnchor),
            tag.widthAnchor.constraint(greaterThanOrEqualToConstant: 72),
            tag.heightAnchor.constraint(equalToConstant: 28),
        ])

        window.isHidden = false

        self.debugTag = tag
        self.overlayWindow = window
    }

    private func uninstall() {
        debugTag?.removeFromSuperview()
        debugTag = nil
        overlayWindow?.isHidden = true
        overlayWindow?.rootViewController = nil
        overlayWindow = nil
    }

    @available(iOS 13.0, *)
    private static func activeWindowScene() -> UIWindowScene? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        return scenes.first { $0.activationState == .foregroundActive } ?? scenes.first
    }

    private func topPresentedViewController() -> UIViewController? {
        guard let window = keyWindowForPresentation() else { return nil }
        var top = window.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }

    private func keyWindowForPresentation() -> UIWindow? {
        let candidates: [UIWindow]
        if #available(iOS 13.0, *) {
            candidates = UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .flatMap { $0.windows }
        } else {
            candidates = UIApplication.shared.windows
        }
        return candidates.first { $0 !== overlayWindow && $0.isKeyWindow }
            ?? candidates.first { $0 !== overlayWindow }
    }

    private func handleTap() {
        let presenter = topPresentedViewController()
        if actionHandler?.sparklingFloatingBallDidTap?(presenter: presenter) == true {
            return
        }
        defaultTap(presenter: presenter)
    }

    @objc private func debugTagTapped() {
        handleTap()
    }

    private func defaultTap(presenter: UIViewController?) {
        guard let presenter else { return }
        SparklingDebugTool.presentConsolePanel(from: presenter)
    }
}

/// A UIWindow whose empty regions allow touches to pass through to the
/// host app's regular windows, so the debugTag doesn't intercept user
/// interaction outside its own bounds.
private final class PassthroughWindow: UIWindow {
    override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        guard let hit = super.hitTest(point, with: event) else { return nil }
        return hit === rootViewController?.view ? nil : hit
    }
}
