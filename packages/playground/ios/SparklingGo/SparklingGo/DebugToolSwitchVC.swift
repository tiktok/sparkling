// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import Lynx
import UIKit

private let kPrefsSuite = "sparkling_debug_tool_switches"
private let kLynxDebug = "lynx_debug"
private let kDevtool = "devtool"
private let kLogBox = "logbox"

class DebugToolSwitchViewController: UIViewController {

    private let lynxDebugSwitch = UISwitch()
    private let devtoolSwitch = UISwitch()
    private let logBoxSwitch = UISwitch()
    private var defaults: UserDefaults { UserDefaults(suiteName: kPrefsSuite) ?? .standard }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .white
        title = "Debug tool switches"

        let scroll = UIScrollView()
        scroll.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(scroll)

        let stack = UIStackView()
        stack.axis = .vertical
        stack.spacing = 16
        stack.translatesAutoresizingMaskIntoConstraints = false
        scroll.addSubview(stack)

        let intro = UILabel()
        intro.numberOfLines = 0
        intro.textColor = UIColor(white: 0.4, alpha: 1)
        intro.font = .systemFont(ofSize: 14)
        intro.text = "Toggle Lynx debugging features at runtime (debug builds)."
        stack.addArrangedSubview(intro)

        stack.addArrangedSubview(makeRow(title: "Lynx debug", toggle: lynxDebugSwitch, action: #selector(lynxDebugChanged)))
        stack.addArrangedSubview(makeRow(title: "DevTool", toggle: devtoolSwitch, action: #selector(devtoolChanged)))
        stack.addArrangedSubview(makeRow(title: "LogBox", toggle: logBoxSwitch, action: #selector(logBoxChanged)))

        navigationItem.rightBarButtonItem = UIBarButtonItem(barButtonSystemItem: .done, target: self, action: #selector(doneTapped))

        NSLayoutConstraint.activate([
            scroll.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            scroll.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scroll.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scroll.bottomAnchor.constraint(equalTo: view.bottomAnchor),

            stack.topAnchor.constraint(equalTo: scroll.contentLayoutGuide.topAnchor, constant: 16),
            stack.leadingAnchor.constraint(equalTo: scroll.frameLayoutGuide.leadingAnchor, constant: 16),
            stack.trailingAnchor.constraint(equalTo: scroll.frameLayoutGuide.trailingAnchor, constant: -16),
            stack.bottomAnchor.constraint(equalTo: scroll.contentLayoutGuide.bottomAnchor, constant: -16),
            stack.widthAnchor.constraint(equalTo: scroll.frameLayoutGuide.widthAnchor, constant: -32),
        ])

        lynxDebugSwitch.isOn = defaults.bool(forKey: kLynxDebug, defaultValue: true)
        devtoolSwitch.isOn = defaults.bool(forKey: kDevtool, defaultValue: true)
        logBoxSwitch.isOn = defaults.bool(forKey: kLogBox, defaultValue: true)
        applyLynxEnv()
    }

    private func makeRow(title: String, toggle: UISwitch, action: Selector) -> UIView {
        let row = UIView()
        let label = UILabel()
        label.text = title
        label.textColor = UIColor(white: 0.13, alpha: 1)
        label.font = .systemFont(ofSize: 16)
        label.translatesAutoresizingMaskIntoConstraints = false
        toggle.translatesAutoresizingMaskIntoConstraints = false
        toggle.addTarget(self, action: action, for: .valueChanged)
        row.addSubview(label)
        row.addSubview(toggle)
        NSLayoutConstraint.activate([
            label.leadingAnchor.constraint(equalTo: row.leadingAnchor),
            label.centerYAnchor.constraint(equalTo: row.centerYAnchor),
            toggle.trailingAnchor.constraint(equalTo: row.trailingAnchor),
            toggle.centerYAnchor.constraint(equalTo: row.centerYAnchor),
            label.trailingAnchor.constraint(lessThanOrEqualTo: toggle.leadingAnchor, constant: -8),
            row.heightAnchor.constraint(greaterThanOrEqualToConstant: 44),
        ])
        return row
    }

    private func applyLynxEnv() {
        let env = LynxEnv.sharedInstance()
        env.lynxDebugEnabled = lynxDebugSwitch.isOn
        env.devtoolEnabled = devtoolSwitch.isOn
        env.logBoxEnabled = logBoxSwitch.isOn
    }

    @objc private func lynxDebugChanged() {
        LynxEnv.sharedInstance().lynxDebugEnabled = lynxDebugSwitch.isOn
        defaults.set(lynxDebugSwitch.isOn, forKey: kLynxDebug)
    }

    @objc private func devtoolChanged() {
        LynxEnv.sharedInstance().devtoolEnabled = devtoolSwitch.isOn
        defaults.set(devtoolSwitch.isOn, forKey: kDevtool)
    }

    @objc private func logBoxChanged() {
        LynxEnv.sharedInstance().logBoxEnabled = logBoxSwitch.isOn
        defaults.set(logBoxSwitch.isOn, forKey: kLogBox)
    }

    @objc private func doneTapped() {
        if let nav = navigationController, nav.viewControllers.count > 1 {
            nav.popViewController(animated: true)
        } else {
            dismiss(animated: true)
        }
    }
}

private extension UserDefaults {
    func bool(forKey key: String, defaultValue: Bool) -> Bool {
        if object(forKey: key) == nil { return defaultValue }
        return bool(forKey: key)
    }
}
