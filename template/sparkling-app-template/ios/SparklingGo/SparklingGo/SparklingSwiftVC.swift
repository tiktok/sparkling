// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation
import SwiftUI
import Sparkling
import UIKit

class SparklingLynxElement: SPKLynxElement {
    var lynxElementName: String

    var lynxElementClassName: AnyClass

    init(lynxElementName: String, lynxElementClassName: AnyClass) {
        self.lynxElementName = lynxElementName
        self.lynxElementClassName = lynxElementClassName
    }
}

struct SPKSwiftVC: UIViewControllerRepresentable {
    var frame: CGRect
    
    init(frame: CGRect = .zero) {
        self.frame = frame
    }

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }
    
    func makeUIViewController(context: Context) -> UINavigationController {
        let url = DebugDevURLSupport.mainScheme()
        let spkContext = DebugDevURLSupport.makeContext()
        let resolved = Self.resolvedFrame(frame)
        let vc = SPKRouter.create(withURL: url, context: spkContext, frame: resolved)
        let naviVC = UINavigationController(rootViewController: vc)
        naviVC.isNavigationBarHidden = true
        context.coordinator.loadFailedDelegate = DevURLLoadFailedDelegate(
            navigationController: naviVC,
            frameProvider: { Self.resolvedFrame(self.frame) }
        )
        spkContext.containerLifecycleDelegate = context.coordinator.loadFailedDelegate
        return naviVC
    }

    func updateUIViewController(_ uiViewController: UINavigationController, context: Context) {
        let resolved = Self.resolvedFrame(frame)
        uiViewController.view.frame = CGRect(origin: .zero, size: resolved.size)
        uiViewController.view.setNeedsLayout()
        uiViewController.view.layoutIfNeeded()
    }

    private static func resolvedFrame(_ frame: CGRect) -> CGRect {
        if frame.width > 0, frame.height > 0 {
            return CGRect(origin: .zero, size: frame.size)
        }
        return CGRect(origin: .zero, size: UIScreen.main.bounds.size)
    }

    final class Coordinator {
        var loadFailedDelegate: DevURLLoadFailedDelegate?
    }
}

struct DemoVC: View {
    var body: some View {
        GeometryReader { geometry in
            SPKSwiftVC(frame: CGRect(origin: .zero, size: geometry.size))
        }
        .background(Color.black)
        .ignoresSafeArea()
    }
}
