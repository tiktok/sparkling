// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import AVFoundation
import Foundation
import Photos
import SparklingMethod
import UIKit

extension SPKChooseMediaMethod {
    @objc public override func call(withParamModel paramModel: Any, completionHandler: CompletionHandlerProtocol) {
        guard let typedParamModel = paramModel as? SPKChooseMediaMethodParamModel else {
            completionHandler.handleCompletion(status: .invalidParameter(message: "Invalid parameter model type"), result: nil)
            return
        }

        switch SPKChooseMediaMediaSourceType(rawValue: typedParamModel.sourceType) {
        case .album:
            checkAlbumPermission(with: typedParamModel) { [weak self] hasPermission in
                guard let self = self else { return }
                if hasPermission {
                    self.openMediaPicker(with: typedParamModel, completionHandler: completionHandler)
                } else {
                    self.handleAlbumDenyAction(with: typedParamModel, completionHandler: completionHandler)
                }
            }
        case .camera:
            guard AVCaptureDevice.default(for: .video) != nil else {
                completionHandler.handleCompletion(status: .failed(message: "Camera is not available on this device."), result: nil)
                return
            }
            checkCameraPermission(with: typedParamModel) { [weak self] hasPermission in
                guard let self = self else { return }
                if hasPermission {
                    self.openMediaPicker(with: typedParamModel, completionHandler: completionHandler)
                } else {
                    self.handleCameraDenyAction(with: typedParamModel, completionHandler: completionHandler)
                }
            }
        default:
            completionHandler.handleCompletion(status: .invalidParameter(message: "Invalid source type"), result: nil)
        }
    }

    private func checkAlbumPermission(with paramModel: SPKChooseMediaMethodParamModel, completionHandler: @escaping (Bool) -> Void) {
        let authorizationStatus = PHPhotoLibrary.authorizationStatus()

        switch authorizationStatus {
        case .authorized, .limited:
            completionHandler(true)
        case .notDetermined:
            PHPhotoLibrary.requestAuthorization { status in
                DispatchQueue.main.async {
                    if #available(iOS 14, *) {
                        completionHandler(status == .authorized || status == .limited)
                    } else {
                        completionHandler(status == .authorized)
                    }
                }
            }
        default:
            completionHandler(false)
        }
    }

    private func checkCameraPermission(with paramModel: SPKChooseMediaMethodParamModel, completionHandler: @escaping (Bool) -> Void) {
        let authorizationStatus = AVCaptureDevice.authorizationStatus(for: .video)

        switch authorizationStatus {
        case .authorized:
            completionHandler(true)
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                DispatchQueue.main.async {
                    completionHandler(granted)
                }
            }
        default:
            completionHandler(false)
        }
    }

    private func handleAlbumDenyAction(with paramModel: SPKChooseMediaMethodParamModel, completionHandler: CompletionHandlerProtocol) {
        if paramModel.albumPermissionDenyAction == SPKChooseMediaPermissionDenyAction.default.rawValue {
            let alert = UIAlertController(
                title: "Photo library access required",
                message: "Please allow photo library access in Settings",
                preferredStyle: .alert)

            let cancelAction = UIAlertAction(title: "Cancel", style: .cancel) {
                _ in
                completionHandler.handleCompletion(status: .failed(message: "User cancelled authorization"), result: nil)
            }

            let settingsAction = UIAlertAction(title: "Open Settings", style: .default) {
                _ in
                if let settingsURL = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(settingsURL)
                }
                completionHandler.handleCompletion(status: .failed(message: "Please grant permission in Settings"), result: nil)
            }

            alert.addAction(cancelAction)
            alert.addAction(settingsAction)

            if let rootViewController = UIApplication.shared.keyWindow?.rootViewController {
                rootViewController.present(alert, animated: true, completion: nil)
            }
        } else {
            completionHandler.handleCompletion(status: .failed(message: "Photo library permission denied"), result: nil)
        }
    }

    private func handleCameraDenyAction(with paramModel: SPKChooseMediaMethodParamModel, completionHandler: CompletionHandlerProtocol) {
        if paramModel.cameraPermissionDenyAction == SPKChooseMediaPermissionDenyAction.default.rawValue {
            let alert = UIAlertController(
                title: "Camera access required",
                message: "Please allow camera access in Settings",
                preferredStyle: .alert)

            let cancelAction = UIAlertAction(title: "Cancel", style: .cancel) {
                _ in
                completionHandler.handleCompletion(status: .failed(message: "User cancelled authorization"), result: nil)
            }

            let settingsAction = UIAlertAction(title: "Open Settings", style: .default) {
                _ in
                if let settingsURL = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(settingsURL)
                }
                completionHandler.handleCompletion(status: .failed(message: "Please grant permission in Settings"), result: nil)
            }

            alert.addAction(cancelAction)
            alert.addAction(settingsAction)

            if let rootViewController = UIApplication.shared.keyWindow?.rootViewController {
                rootViewController.present(alert, animated: true, completion: nil)
            }
        } else {
            completionHandler.handleCompletion(status: .failed(message: "Camera permission denied"), result: nil)
        }
    }

    // Retain the media picker so it (and its delegate) stay alive while the picker is presented
    private static var _activeMediaPicker: SPKDefaultMediaPicker?

    private func openMediaPicker(with paramModel: SPKChooseMediaMethodParamModel, completionHandler: CompletionHandlerProtocol) {
        let mediaPicker = SPKDefaultMediaPicker()
        SPKChooseMediaMethod._activeMediaPicker = mediaPicker

        let pickerVC = mediaPicker.mediaPicker(with: paramModel) { [weak self] resultModel, error in
            SPKChooseMediaMethod._activeMediaPicker = nil
            guard self != nil else { return }

            if let error = error {
                completionHandler.handleCompletion(status: .failed(message: error.message), result: nil)
            } else if let resultModel = resultModel {
                completionHandler.handleCompletion(status: .succeeded(), result: resultModel)
            } else {
                completionHandler.handleCompletion(status: .failed(message: "No media file selected"), result: nil)
            }
        }

        if let pickerVC = pickerVC {
            DispatchQueue.main.async {
                var presentingVC: UIViewController?

                if let rootVC = UIApplication.shared.keyWindow?.rootViewController {
                    presentingVC = rootVC
                    while let presented = presentingVC?.presentedViewController {
                        presentingVC = presented
                    }
                }

                if presentingVC == nil, #available(iOS 13.0, *) {
                    let scene = UIApplication.shared.connectedScenes
                        .filter { $0.activationState == .foregroundActive }
                        .compactMap { $0 as? UIWindowScene }
                        .first
                    if let rootVC = scene?.windows.first(where: { $0.isKeyWindow })?.rootViewController {
                        presentingVC = rootVC
                        while let presented = presentingVC?.presentedViewController {
                            presentingVC = presented
                        }
                    }
                }

                if presentingVC == nil {
                    if let rootVC = UIApplication.shared.windows.first?.rootViewController {
                        presentingVC = rootVC
                        while let presented = presentingVC?.presentedViewController {
                            presentingVC = presented
                        }
                    }
                }

                if let presentingVC = presentingVC {
                    presentingVC.present(pickerVC, animated: true, completion: nil)
                } else {
                    completionHandler.handleCompletion(status: .failed(message: "No view controller available to present picker"), result: nil)
                    SPKChooseMediaMethod._activeMediaPicker = nil
                }
            }
        }
    }

    private var defaultMediaPicker: SPKDefaultMediaPicker {
        return SPKDefaultMediaPicker()
    }

    private func isCameraDenied() -> Bool {
        let authorizationStatus = AVCaptureDevice.authorizationStatus(for: .video)
        return authorizationStatus == .denied || authorizationStatus == .restricted
    }

    private func isAlbumDenied() -> Bool {
        let authorizationStatus = PHPhotoLibrary.authorizationStatus()
        return authorizationStatus == .denied || authorizationStatus == .restricted
    }
}
