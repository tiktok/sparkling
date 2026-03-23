// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SwiftUI
import Sparkling
import UIKit

class CardViewDemoViewController: UIViewController {

    private var sparklingContainerView: SPKContainerView?
    private let containerWrapper = UIView()

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .white
        title = "Card View Demo"

        setupNativeUI()
    }

    private func setupNativeUI() {
        let buttonBar = UIView()
        buttonBar.backgroundColor = UIColor(red: 0.96, green: 0.96, blue: 0.96, alpha: 1.0)
        buttonBar.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(buttonBar)

        let showButton = UIButton(type: .system)
        showButton.setTitle("Show Card View", for: .normal)
        showButton.setTitleColor(.white, for: .normal)
        showButton.titleLabel?.font = UIFont.systemFont(ofSize: 15, weight: .semibold)
        showButton.backgroundColor = UIColor(red: 0.4, green: 0.49, blue: 0.92, alpha: 1.0)
        showButton.layer.cornerRadius = 8
        showButton.translatesAutoresizingMaskIntoConstraints = false
        showButton.addTarget(self, action: #selector(showCardView), for: .touchUpInside)
        buttonBar.addSubview(showButton)

        let closeButton = UIButton(type: .system)
        closeButton.setTitle("Close", for: .normal)
        closeButton.setTitleColor(.white, for: .normal)
        closeButton.titleLabel?.font = UIFont.systemFont(ofSize: 15, weight: .semibold)
        closeButton.backgroundColor = UIColor(red: 0.9, green: 0.22, blue: 0.21, alpha: 1.0)
        closeButton.layer.cornerRadius = 8
        closeButton.translatesAutoresizingMaskIntoConstraints = false
        closeButton.addTarget(self, action: #selector(closeTapped), for: .touchUpInside)
        buttonBar.addSubview(closeButton)

        containerWrapper.backgroundColor = .white
        containerWrapper.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(containerWrapper)

        NSLayoutConstraint.activate([
            buttonBar.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            buttonBar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            buttonBar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            buttonBar.heightAnchor.constraint(equalToConstant: 72),

            showButton.leadingAnchor.constraint(equalTo: buttonBar.leadingAnchor, constant: 12),
            showButton.centerYAnchor.constraint(equalTo: buttonBar.centerYAnchor),
            showButton.heightAnchor.constraint(equalToConstant: 48),

            closeButton.leadingAnchor.constraint(equalTo: showButton.trailingAnchor, constant: 8),
            closeButton.trailingAnchor.constraint(equalTo: buttonBar.trailingAnchor, constant: -12),
            closeButton.centerYAnchor.constraint(equalTo: buttonBar.centerYAnchor),
            closeButton.widthAnchor.constraint(equalToConstant: 80),
            closeButton.heightAnchor.constraint(equalToConstant: 48),

            showButton.trailingAnchor.constraint(equalTo: closeButton.leadingAnchor, constant: -8),

            containerWrapper.topAnchor.constraint(equalTo: buttonBar.bottomAnchor),
            containerWrapper.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            containerWrapper.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            containerWrapper.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
    }

    @objc private func showCardView() {
        sparklingContainerView?.removeFromSuperview()
        sparklingContainerView = nil

        let spkView = SPKContainerView(frame: containerWrapper.bounds)
        spkView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        containerWrapper.addSubview(spkView)
        sparklingContainerView = spkView

        let context = SPKContext()
        let elements = SparklingLynxElement(lynxElementName: "input", lynxElementClassName: LynxInput.self)
        context.customUIElements = [elements]

        let url = "hybrid://lynxview?bundle=.%2Fcard-view-demo.lynx.bundle"
        spkView.load(withURL: url, context)
    }

    @objc private func closeTapped() {
        if let nav = navigationController {
            nav.popViewController(animated: true)
        } else {
            dismiss(animated: true)
        }
    }

    deinit {
        sparklingContainerView?.removeFromSuperview()
    }
}

struct CardViewDemoVCWrapper: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> CardViewDemoViewController {
        return CardViewDemoViewController()
    }

    func updateUIViewController(_ uiViewController: CardViewDemoViewController, context: Context) {}
}
