// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import AVFoundation
import Foundation
import Photos
import SparklingMethod
import UIKit

@objc(SPKChooseMediaParams)
public final class SPKChooseMediaParams: SPKMethodModel {
    @objc public var mediaTypes: [String]?
    @objc public var sourceType: String = "album"
    @objc public var cameraType: String = "back"
    @objc public var maxCount: Int = 1
    @objc public var quality: Float = 1
    @objc public var videoMaxDuration: Double = 60
    @objc public var compressOption: Int = 0
    @objc public var needPreview: Bool = true
    @objc public var cameraPermissionDenyAction: Int = 0
    @objc public var albumPermissionDenyAction: Int = 0
    @objc public var compressWidth: CGFloat = 0
    @objc public var compressHeight: CGFloat = 0
    @objc public var compressionQuality: CGFloat = 0.8
    @objc public var type: String?
    @objc public var needTempFilePath: Bool = true
    @objc public var needBase64: Bool = false
    @objc public var needSaveToAlbum: Bool = false
    @objc public var compressImage: Bool = false
    @objc public var needBase64Data: Bool = false
    @objc public var saveToPhotoAlbum: Bool = false
    @objc public var showTakePhotoButton: Bool = true
    @objc public var showCameraRoll: Bool = true
    @objc public var showPhotoLibrary: Bool = true

    public override class func requiredKeyPaths() -> Set<String>? { nil }
    public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        [
            "mediaTypes": "mediaTypes",
            "sourceType": "sourceType",
            "cameraType": "cameraType",
            "maxCount": "maxCount",
            "quality": "quality",
            "videoMaxDuration": "videoMaxDuration",
            "compressOption": "compressOption",
            "needPreview": "needPreview",
            "cameraPermissionDenyAction": "cameraPermissionDenyAction",
            "albumPermissionDenyAction": "albumPermissionDenyAction",
            "compressWidth": "compressWidth",
            "compressHeight": "compressHeight",
            "compressionQuality": "compressionQuality",
            "type": "type",
            "needTempFilePath": "needTempFilePath",
            "needBase64": "needBase64",
            "needSaveToAlbum": "needSaveToAlbum",
            "showTakePhotoButton": "showTakePhotoButton",
            "showCameraRoll": "showCameraRoll",
            "showPhotoLibrary": "showPhotoLibrary",
        ]
    }

    // The original iOS picker uses compressionQuality (0...1), not the JS-only
    // compressQuality option (0...100). Cropping and preview flags remain no-ops.
    func imageData(for image: UIImage) -> Data? {
        if compressOption == 4 { return image.jpegData(compressionQuality: 1) }
        guard [0, 1, 3].contains(compressOption) else { return image.jpegData(compressionQuality: 0.8) }
        var output = image
        if compressWidth > 0 && compressHeight > 0 {
            let widthScale = image.size.width / compressWidth
            let heightScale = image.size.height / compressHeight
            var size = image.size
            if widthScale > heightScale && widthScale > 1 {
                size = CGSize(width: compressWidth, height: image.size.height / widthScale)
            } else if heightScale > widthScale && heightScale > 1 {
                size = CGSize(width: image.size.width / heightScale, height: compressHeight)
            }
            if size != image.size {
                UIGraphicsBeginImageContextWithOptions(size, false, 0)
                image.draw(in: CGRect(origin: .zero, size: size))
                output = UIGraphicsGetImageFromCurrentImageContext() ?? image
                UIGraphicsEndImageContext()
            }
        }
        return output.jpegData(compressionQuality: compressionQuality)
    }
}

@objc(SPKChooseMediaMethod)
public final class SPKChooseMediaMethod: SPKMethod {
    public override class func methodName() -> String? { "media.chooseMedia" }
    public override var methodName: String? { Self.methodName() }
    public override var paramModelClass: (any SPKMethodModelProtocol.Type)? { SPKChooseMediaParams.self }
    public override var resultModelClass: AnyClass? { SPKChooseMediaResult.self }

    public override func invoke(withParamModel paramModel: SPKMethodModel?, completionHandler: SPKMethodCompletionHandler?) {
        let completionHandler = completionHandler ?? { _, _ in }
        guard let typedParamModel = paramModel as? SPKChooseMediaParams else {
            completionHandler(nil, SPKMedia.error(.invalidParameter, "Invalid parameter model type"))
            return
        }

        switch typedParamModel.sourceType.lowercased() {
        case SPKChooseMediaParamValue.album:
            checkAlbumPermission(with: typedParamModel) { [weak self] hasPermission in
                guard let self = self else { return }
                if hasPermission {
                    self.openMediaPicker(with: typedParamModel, completionHandler: completionHandler)
                } else {
                    self.handleAlbumDenyAction(with: typedParamModel, completionHandler: completionHandler)
                }
            }
        case SPKChooseMediaParamValue.camera:
            guard AVCaptureDevice.default(for: .video) != nil else {
                completionHandler(nil, SPKMedia.error(.failed, "Camera is not available on this device."))
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
            completionHandler(nil, SPKMedia.error(.invalidParameter, "Invalid source type"))
        }
    }

    private func checkAlbumPermission(with paramModel: SPKChooseMediaParams, completionHandler: @escaping (Bool) -> Void) {
        let authorizationStatus = PHPhotoLibrary.authorizationStatus()

        switch authorizationStatus {
        case .authorized, .limited:
            completionHandler(true)
        case .notDetermined:
            PHPhotoLibrary.requestAuthorization { status in
                DispatchQueue.main.async {
                    completionHandler(status == .authorized || status == .limited)
                }
            }
        default:
            completionHandler(false)
        }
    }

    private func checkCameraPermission(with paramModel: SPKChooseMediaParams, completionHandler: @escaping (Bool) -> Void) {
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

    private func handleAlbumDenyAction(with paramModel: SPKChooseMediaParams, completionHandler: @escaping SPKMethodCompletionHandler) {
        if paramModel.albumPermissionDenyAction == SPKChooseMediaPermissionDenyAction.default.rawValue {
            let alert = UIAlertController(
                title: "Photo library access required",
                message: "Please allow photo library access in Settings",
                preferredStyle: .alert)

            let cancelAction = UIAlertAction(title: "Cancel", style: .cancel) {
                _ in
                completionHandler(nil, SPKMedia.error(.failed, "User cancelled authorization"))
            }

            let settingsAction = UIAlertAction(title: "Open Settings", style: .default) {
                _ in
                if let settingsURL = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(settingsURL)
                }
                completionHandler(nil, SPKMedia.error(.failed, "Please grant permission in Settings"))
            }

            alert.addAction(cancelAction)
            alert.addAction(settingsAction)

            if let rootViewController = UIApplication.shared.keyWindow?.rootViewController {
                rootViewController.present(alert, animated: true, completion: nil)
            }
        } else {
            completionHandler(nil, SPKMedia.error(.failed, "Photo library permission denied"))
        }
    }

    private func handleCameraDenyAction(with paramModel: SPKChooseMediaParams, completionHandler: @escaping SPKMethodCompletionHandler) {
        if paramModel.cameraPermissionDenyAction == SPKChooseMediaPermissionDenyAction.default.rawValue {
            let alert = UIAlertController(
                title: "Camera access required",
                message: "Please allow camera access in Settings",
                preferredStyle: .alert)

            let cancelAction = UIAlertAction(title: "Cancel", style: .cancel) {
                _ in
                completionHandler(nil, SPKMedia.error(.failed, "User cancelled authorization"))
            }

            let settingsAction = UIAlertAction(title: "Open Settings", style: .default) {
                _ in
                if let settingsURL = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(settingsURL)
                }
                completionHandler(nil, SPKMedia.error(.failed, "Please grant permission in Settings"))
            }

            alert.addAction(cancelAction)
            alert.addAction(settingsAction)

            if let rootViewController = UIApplication.shared.keyWindow?.rootViewController {
                rootViewController.present(alert, animated: true, completion: nil)
            }
        } else {
            completionHandler(nil, SPKMedia.error(.failed, "Camera permission denied"))
        }
    }

    // Retain the media picker so it (and its delegate) stay alive while the picker is presented
    private static var _activeMediaPicker: SPKDefaultMediaPicker?

    private func openMediaPicker(with paramModel: SPKChooseMediaParams, completionHandler: @escaping SPKMethodCompletionHandler) {
        let mediaPicker = SPKDefaultMediaPicker()
        SPKChooseMediaMethod._activeMediaPicker = mediaPicker

        let pickerVC = mediaPicker.mediaPicker(with: paramModel) { resultModel, error in
            SPKChooseMediaMethod._activeMediaPicker = nil

            if let error = error {
                completionHandler(nil, SPKMedia.error(.failed, error.message))
            } else if let resultModel = resultModel {
                completionHandler(resultModel, nil)
            } else {
                completionHandler(nil, SPKMedia.error(.failed, "No media file selected"))
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

                if presentingVC == nil {
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
                    completionHandler(nil, SPKMedia.error(.failed, "No view controller available to present picker"))
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

@SPKGlobalMethod
extension SPKChooseMediaMethod {}

@objc(SPKChooseMediaMethodResultTempFileModel)
class SPKChooseMediaMethodResultTempFileModel: SPKMethodModel {
    @objc public var tempFilePath: String?
    @objc public var tempFileAbsolutePath: String?
    @objc public var base64Data: String?
    @objc public var fileName: String?
    @objc public var mimeType: String?
    @objc public var size: Int64 = 0
    @objc public var width: Int = 0
    @objc public var height: Int = 0
    @objc public var mediaType: Int = 0

    @objc public override class func jsonKeyPathsByPropertyKey() -> [AnyHashable: Any] {
        return [
            "tempFilePath": "tempFilePath",
            "tempFileAbsolutePath": "tempFileAbsolutePath",
            "base64Data": "base64Data",
            "fileName": "fileName",
            "mimeType": "mimeType",
            "size": "size",
            "width": "width",
            "height": "height",
            "mediaType": "mediaType",
        ]
    }
}
