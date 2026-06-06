// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import UIKit

/// Unified Sparkling debug inspector. Half-sheet with three tabs at the top:
/// `Console` / `GlobalProps` / `Method`, plus a close icon in the top-left
/// corner. Each tab is a self-contained child view controller; switching tabs
/// keeps the previously-shown VC alive so the user can hop back and forth
/// without losing scroll position.
@objc public final class SparklingInspectorViewController: UIViewController {

    @objc public enum Tab: Int {
        case console
        case globalProps
        case methods
    }

    private let topBar = UIView()
    private let closeButton = UIButton(type: .system)
    private let titleLabel = UILabel()
    private let tabBar = UIStackView()
    private let contentContainer = UIView()
    private var tabButtons: [Tab: UIButton] = [:]
    private var tabUnderlines: [Tab: UIView] = [:]

    private lazy var consoleVC: SparklingConsoleViewController = {
        let vc = SparklingConsoleViewController()
        vc.embedsWithoutCloseButton = true
        return vc
    }()
    private lazy var globalPropsVC: SparklingGlobalPropsViewController = {
        let vc = SparklingGlobalPropsViewController()
        vc.embedsWithoutCloseButton = true
        return vc
    }()
    private lazy var methodsVC: SparklingMethodInvocationViewController = {
        let vc = SparklingMethodInvocationViewController()
        vc.embedsWithoutCloseButton = true
        return vc
    }()

    private var currentTab: Tab = .console
    private let initialTab: Tab

    @objc public init(initialTab: Tab = .console) {
        self.initialTab = initialTab
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    public override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = DebugColor.background
        configureTopBar()
        configureTabBar()
        configureContent()
        applyConstraints()
        select(tab: initialTab, animated: false)
    }

    // MARK: - Setup

    private func configureTopBar() {
        topBar.translatesAutoresizingMaskIntoConstraints = false
        topBar.backgroundColor = DebugColor.background

        // Close icon (top-right). Use `.close` on iOS 13+, fall back to a
        // tinted "✕" symbol on iOS 12.
        if #available(iOS 13.0, *) {
            let symbol = UIImage(systemName: "xmark")?
                .withConfiguration(UIImage.SymbolConfiguration(pointSize: 16, weight: .semibold))
            closeButton.setImage(symbol, for: .normal)
            closeButton.tintColor = DebugColor.primary
        } else {
            closeButton.setTitle("✕", for: .normal)
            closeButton.titleLabel?.font = .systemFont(ofSize: 18, weight: .semibold)
            closeButton.setTitleColor(DebugColor.primary, for: .normal)
        }
        closeButton.translatesAutoresizingMaskIntoConstraints = false
        closeButton.addTarget(self, action: #selector(handleClose), for: .touchUpInside)
        closeButton.accessibilityLabel = "Close"
        closeButton.layer.cornerRadius = 0
        closeButton.layer.masksToBounds = true
        topBar.addSubview(closeButton)

        titleLabel.text = "Sparkling Debug Tool - \(resolvePageName())"
        titleLabel.font = .systemFont(ofSize: 16, weight: .semibold)
        titleLabel.textColor = DebugColor.primary
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        topBar.addSubview(titleLabel)

        view.addSubview(topBar)
    }

    private func configureTabBar() {
        tabBar.translatesAutoresizingMaskIntoConstraints = false
        tabBar.axis = .horizontal
        tabBar.distribution = .fillEqually
        tabBar.spacing = 0
        tabBar.layoutMargins = .zero
        tabBar.isLayoutMarginsRelativeArrangement = true
        view.addSubview(tabBar)

        let entries: [(Tab, String)] = [
            (.console, "Log"),
            (.methods, "Sparkling Method"),
            (.globalProps, "GlobalProps"),
        ]
        for (tab, label) in entries {
            let button = UIButton(type: .system)
            button.setTitle(label, for: .normal)
            button.titleLabel?.font = .systemFont(ofSize: 13, weight: .regular)
            button.backgroundColor = .clear
            button.setTitleColor(DebugColor.secondary, for: .normal)
            button.setTitleColor(DebugColor.secondary, for: .highlighted)
            button.setTitleColor(DebugColor.secondary, for: .selected)
            button.tintColor = DebugColor.secondary
            button.layer.cornerRadius = 0
            button.layer.masksToBounds = true
            button.layer.borderWidth = 0.5
            button.layer.borderColor = DebugColor.separator.cgColor
            button.heightAnchor.constraint(equalToConstant: 38).isActive = true
            let underline = UIView()
            underline.backgroundColor = DebugColor.accent
            underline.translatesAutoresizingMaskIntoConstraints = false
            button.addSubview(underline)
            NSLayoutConstraint.activate([
                underline.heightAnchor.constraint(equalToConstant: 2),
                underline.leadingAnchor.constraint(equalTo: button.leadingAnchor),
                underline.trailingAnchor.constraint(equalTo: button.trailingAnchor),
                underline.bottomAnchor.constraint(equalTo: button.bottomAnchor),
            ])
            tabUnderlines[tab] = underline
            button.addAction(for: tab, in: self)
            tabBar.addArrangedSubview(button)
            tabButtons[tab] = button
        }

        let separator = UIView()
        separator.backgroundColor = DebugColor.separator
        separator.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(separator)
        NSLayoutConstraint.activate([
            separator.topAnchor.constraint(equalTo: tabBar.bottomAnchor),
            separator.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            separator.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            separator.heightAnchor.constraint(equalToConstant: 1.0 / UIScreen.main.scale),
        ])
    }

    private func configureContent() {
        contentContainer.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(contentContainer)

        for childVC in [consoleVC as UIViewController, globalPropsVC, methodsVC] {
            addChild(childVC)
            contentContainer.addSubview(childVC.view)
            childVC.view.translatesAutoresizingMaskIntoConstraints = false
            NSLayoutConstraint.activate([
                childVC.view.topAnchor.constraint(equalTo: contentContainer.topAnchor),
                childVC.view.leadingAnchor.constraint(equalTo: contentContainer.leadingAnchor),
                childVC.view.trailingAnchor.constraint(equalTo: contentContainer.trailingAnchor),
                childVC.view.bottomAnchor.constraint(equalTo: contentContainer.bottomAnchor),
            ])
            childVC.didMove(toParent: self)
        }
    }

    private func applyConstraints() {
        NSLayoutConstraint.activate([
            topBar.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            topBar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            topBar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            topBar.heightAnchor.constraint(equalToConstant: 40),

            closeButton.trailingAnchor.constraint(equalTo: topBar.trailingAnchor, constant: -8),
            closeButton.centerYAnchor.constraint(equalTo: topBar.centerYAnchor),
            closeButton.widthAnchor.constraint(equalToConstant: 32),
            closeButton.heightAnchor.constraint(equalToConstant: 32),

            titleLabel.leadingAnchor.constraint(equalTo: topBar.leadingAnchor, constant: 16),
            titleLabel.trailingAnchor.constraint(lessThanOrEqualTo: closeButton.leadingAnchor, constant: -8),
            titleLabel.centerYAnchor.constraint(equalTo: topBar.centerYAnchor),

            tabBar.topAnchor.constraint(equalTo: topBar.bottomAnchor),
            tabBar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tabBar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tabBar.heightAnchor.constraint(equalToConstant: 38),

            contentContainer.topAnchor.constraint(equalTo: tabBar.bottomAnchor),
            contentContainer.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            contentContainer.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            contentContainer.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
    }

    // MARK: - Actions

    @objc private func handleClose() {
        dismiss(animated: true)
    }

    fileprivate func select(tab: Tab, animated: Bool) {
        currentTab = tab
        consoleVC.view.isHidden = tab != .console
        globalPropsVC.view.isHidden = tab != .globalProps
        methodsVC.view.isHidden = tab != .methods
        if tab == .globalProps {
            globalPropsVC.refresh()
        }
        for (key, button) in tabButtons {
            let selected = (key == tab)
            tabUnderlines[key]?.isHidden = !selected
            UIView.animate(withDuration: animated ? 0.15 : 0) {
                button.backgroundColor = .clear
                let titleColor = selected ? DebugColor.primary : DebugColor.secondary
                button.setTitleColor(titleColor, for: .normal)
                button.setTitleColor(titleColor, for: .highlighted)
            }
        }
    }

    private func resolvePageName() -> String {
        let url = SparklingGlobalPropsRegistry.shared.collectAll().first?.templateUrl ?? ""
        guard !url.isEmpty else { return "Unknown" }
        if let components = URLComponents(string: url),
           let bundle = components.queryItems?.first(where: { $0.name == "bundle" })?.value,
           !bundle.isEmpty {
            return bundle
        }
        let withoutQuery = url.components(separatedBy: "?").first?.trimmingCharacters(in: CharacterSet(charactersIn: "/")) ?? url
        return withoutQuery.components(separatedBy: "/").last?.nilIfEmpty ?? withoutQuery
    }
}

private extension String {
    var nilIfEmpty: String? { isEmpty ? nil : self }
}

private enum DebugColor {
    static var background: UIColor {
        if #available(iOS 13.0, *) { return .systemBackground }
        return .white
    }
    static var secondaryBackground: UIColor {
        if #available(iOS 13.0, *) { return .secondarySystemBackground }
        return UIColor(white: 0.97, alpha: 1)
    }
    static var primary: UIColor {
        if #available(iOS 13.0, *) { return .label }
        return UIColor(white: 0.1, alpha: 1)
    }
    static var secondary: UIColor {
        if #available(iOS 13.0, *) { return .secondaryLabel }
        return UIColor(white: 0.48, alpha: 1)
    }
    static var separator: UIColor {
        if #available(iOS 13.0, *) { return .separator }
        return UIColor(white: 0, alpha: 0.1)
    }
    static var accent: UIColor {
        if #available(iOS 13.0, *) { return .systemBlue }
        return UIColor(red: 0.0, green: 0.43, blue: 1.0, alpha: 1.0)
    }
}

private extension UIButton {
    /// Adds a tap target without storing the closure on the responder chain.
    /// Captures `self` weakly so the inspector VC can be deinitialized.
    func addAction(for tab: SparklingInspectorViewController.Tab, in target: SparklingInspectorViewController) {
        let action = UITapTarget(tab: tab, target: target)
        // Retain the strategy on the button itself via associated object.
        objc_setAssociatedObject(self, &UITapTarget.key, action, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        addTarget(action, action: #selector(UITapTarget.handle), for: .touchUpInside)
    }
}

private final class UITapTarget {
    static var key: UInt8 = 0
    let tab: SparklingInspectorViewController.Tab
    weak var target: SparklingInspectorViewController?

    init(tab: SparklingInspectorViewController.Tab, target: SparklingInspectorViewController) {
        self.tab = tab
        self.target = target
    }

    @objc func handle() {
        target?.select(tab: tab, animated: true)
    }
}
