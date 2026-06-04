// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import UIKit

/// Draggable, edge-snapping floating ball that exposes the Sparkling debug
/// tool entry points. Visual style mirrors the Android implementation.
final class SparklingFloatingBall: UIView {
    /// Tap / long-press callbacks. Setters are not retained beyond the ball
    /// itself; the manager owns both this view and the closures.
    var onTap: (() -> Void)?
    var onLongPress: (() -> Void)?

    private let backgroundView = UIView()
    private let imageView = UIImageView()
    private var dragOriginCenter: CGPoint = .zero

    init() {
        super.init(frame: CGRect(x: 0, y: 0, width: 52, height: 52))
        backgroundColor = .clear
        setupContent()
        setupGestures()
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        backgroundView.layer.cornerRadius = backgroundView.bounds.height / 2
        layer.shadowPath = UIBezierPath(ovalIn: backgroundView.frame).cgPath
    }

    private func setupContent() {
        // Drop shadow lives on the host view's layer so the white circle below
        // can clip its own contents without losing the shadow halo. Kept
        // intentionally light so the ball doesn't feel "stuck" to the page.
        layer.shadowColor = UIColor.black.cgColor
        layer.shadowOpacity = 0.10
        layer.shadowRadius = 4
        layer.shadowOffset = CGSize(width: 0, height: 1)
        layer.masksToBounds = false

        backgroundView.backgroundColor = .white
        backgroundView.layer.borderWidth = 1.0 / UIScreen.main.scale
        backgroundView.layer.borderColor = UIColor(white: 0, alpha: 0.06).cgColor
        backgroundView.layer.masksToBounds = true
        backgroundView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(backgroundView)

        imageView.image = SparklingFloatingBall.loadLogoImage()
        imageView.contentMode = .scaleAspectFit
        imageView.translatesAutoresizingMaskIntoConstraints = false
        backgroundView.addSubview(imageView)

        NSLayoutConstraint.activate([
            backgroundView.topAnchor.constraint(equalTo: topAnchor),
            backgroundView.bottomAnchor.constraint(equalTo: bottomAnchor),
            backgroundView.leadingAnchor.constraint(equalTo: leadingAnchor),
            backgroundView.trailingAnchor.constraint(equalTo: trailingAnchor),
            // Inset the logo so the white ring around it is clearly visible.
            imageView.topAnchor.constraint(equalTo: backgroundView.topAnchor, constant: 9),
            imageView.bottomAnchor.constraint(equalTo: backgroundView.bottomAnchor, constant: -9),
            imageView.leadingAnchor.constraint(equalTo: backgroundView.leadingAnchor, constant: 9),
            imageView.trailingAnchor.constraint(equalTo: backgroundView.trailingAnchor, constant: -9),
        ])
        isAccessibilityElement = true
        accessibilityLabel = "Sparkling debug tool"
        accessibilityTraits = .button
    }

    private func setupGestures() {
        let tap = UITapGestureRecognizer(target: self, action: #selector(handleTap))
        addGestureRecognizer(tap)

        let longPress = UILongPressGestureRecognizer(target: self, action: #selector(handleLongPress))
        longPress.minimumPressDuration = 0.55
        addGestureRecognizer(longPress)

        let pan = UIPanGestureRecognizer(target: self, action: #selector(handlePan))
        pan.maximumNumberOfTouches = 1
        addGestureRecognizer(pan)
    }

    @objc private func handleTap() {
        onTap?()
    }

    @objc private func handleLongPress(_ gesture: UILongPressGestureRecognizer) {
        guard gesture.state == .began else { return }
        let generator = UIImpactFeedbackGenerator(style: .medium)
        generator.impactOccurred()
        onLongPress?()
    }

    @objc private func handlePan(_ gesture: UIPanGestureRecognizer) {
        guard let host = superview else { return }
        switch gesture.state {
        case .began:
            dragOriginCenter = center
        case .changed:
            let translation = gesture.translation(in: host)
            center = CGPoint(
                x: dragOriginCenter.x + translation.x,
                y: dragOriginCenter.y + translation.y
            )
        case .ended, .cancelled, .failed:
            snapToEdge(in: host)
        default:
            break
        }
    }

    private func snapToEdge(in host: UIView) {
        let safeArea = host.safeAreaInsets
        let halfWidth = bounds.width / 2
        let halfHeight = bounds.height / 2
        let minX = safeArea.left + halfWidth
        let maxX = host.bounds.width - safeArea.right - halfWidth
        let minY = safeArea.top + halfHeight
        let maxY = host.bounds.height - safeArea.bottom - halfHeight
        let middleX = host.bounds.midX
        let targetX: CGFloat = center.x < middleX ? minX : maxX
        let clampedY = max(minY, min(center.y, maxY))
        UIView.animate(withDuration: 0.25, delay: 0, options: [.curveEaseOut]) {
            self.center = CGPoint(x: targetX, y: clampedY)
        }
    }

    private static func loadLogoImage() -> UIImage? {
        // 1) Pod resource bundle (preferred when shipped via cocoapods).
        let resourceName = "sparkling_floating_logo"
        let podBundle = Bundle(for: SparklingFloatingBall.self)
        if let image = UIImage(named: resourceName, in: podBundle, compatibleWith: nil) {
            return image
        }
        if let bundleURL = podBundle.url(forResource: "SparklingDebugToolAssets", withExtension: "bundle"),
           let assetBundle = Bundle(url: bundleURL),
           let image = UIImage(named: resourceName, in: assetBundle, compatibleWith: nil) {
            return image
        }
        // 2) Fall back to the host app bundle (e.g. SwiftPM / direct integration).
        if let image = UIImage(named: resourceName) {
            return image
        }
        // 3) Last-resort placeholder so the ball remains visible.
        return placeholderImage()
    }

    private static func placeholderImage() -> UIImage? {
        let size = CGSize(width: 48, height: 48)
        UIGraphicsBeginImageContextWithOptions(size, false, UIScreen.main.scale)
        defer { UIGraphicsEndImageContext() }
        let rect = CGRect(origin: .zero, size: size)
        let path = UIBezierPath(ovalIn: rect.insetBy(dx: 2, dy: 2))
        UIColor(red: 0.13, green: 0.55, blue: 0.95, alpha: 1.0).setFill()
        path.fill()
        let attrs: [NSAttributedString.Key: Any] = [
            .foregroundColor: UIColor.white,
            .font: UIFont.systemFont(ofSize: 18, weight: .bold),
        ]
        let text = "S"
        let textSize = (text as NSString).size(withAttributes: attrs)
        let textRect = CGRect(
            x: (size.width - textSize.width) / 2,
            y: (size.height - textSize.height) / 2,
            width: textSize.width,
            height: textSize.height
        )
        (text as NSString).draw(in: textRect, withAttributes: attrs)
        return UIGraphicsGetImageFromCurrentImageContext()
    }
}
