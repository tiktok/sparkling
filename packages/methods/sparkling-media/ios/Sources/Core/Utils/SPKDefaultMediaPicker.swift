// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import AVFoundation
import CoreServices
import MobileCoreServices
import Photos
import UIKit
import SparklingMethod
import Mantle

enum SPKChooseMediaMediaType: Int {
    case image = 1
    case video = 2
}

enum SPKChooseMediaMediaSourceType: Int {
    case album = 1
    case camera = 2
    case unknown = 0
}

enum SPKChooseMediaCameraType: Int {
    case front = 1
    case back = 2
}

enum SPKChooseMediaCompressOption: Int {
    case `default` = 0
    case both = 1
    case onlyBase64 = 2
    case onlyImage = 3
    case none = 4
}

enum SPKChooseMediaPermissionDenyAction: Int {
    case `default` = 0
    case noAlert = 1
}

enum SPKChooseMediaParamValue {
    static let image = "image"
    static let video = "video"
    static let album = "album"
    static let camera = "camera"
    static let front = "front"
    static let back = "back"
}

typealias SPKChooseMediaCompletionHandler = (SPKChooseMediaResult?, SPKStatus?) -> Void

protocol SPKChooseMediaPicker {
    func supported(with paramModel: SPKChooseMediaParams) -> Bool
    func mediaPicker(with paramModel: SPKChooseMediaParams, completionHandler: @escaping SPKChooseMediaCompletionHandler) -> UIViewController?
}

class SPKDefaultMediaPicker: NSObject, SPKChooseMediaPicker, UINavigationControllerDelegate, UIImagePickerControllerDelegate {

    private var params: SPKChooseMediaParams?
    private var completionHandler: SPKChooseMediaCompletionHandler?
    private weak var imagePicker: UIImagePickerController?

    func supported(with paramModel: SPKChooseMediaParams) -> Bool {
        return true
    }

    private func imagePickerSourceType(for sourceType: String) -> UIImagePickerController.SourceType? {
        switch sourceType.lowercased() {
        case SPKChooseMediaParamValue.album:
            return .photoLibrary
        case SPKChooseMediaParamValue.camera:
            return .camera
        default:
            return nil
        }
    }

    private func mappedMediaTypes(from mediaTypes: [String]?) -> [String] {
        guard let mediaTypes = mediaTypes else {
            return [kUTTypeImage as String]
        }

        var mappedMediaTypes: [String] = []
        let normalizedMediaTypes = mediaTypes.map { $0.lowercased() }
        if normalizedMediaTypes.contains(SPKChooseMediaParamValue.image) {
            mappedMediaTypes.append(kUTTypeImage as String)
        }
        if normalizedMediaTypes.contains(SPKChooseMediaParamValue.video) {
            mappedMediaTypes.append(kUTTypeMovie as String)
        }
        if mappedMediaTypes.isEmpty {
            mappedMediaTypes.append(kUTTypeImage as String)
        }
        return mappedMediaTypes
    }

    private func cameraDevice(for cameraType: String) -> UIImagePickerController.CameraDevice? {
        switch cameraType.lowercased() {
        case SPKChooseMediaParamValue.front:
            return .front
        case SPKChooseMediaParamValue.back:
            return .rear
        default:
            return nil
        }
    }

    func mediaPicker(with paramModel: SPKChooseMediaParams, completionHandler: @escaping SPKChooseMediaCompletionHandler) -> UIViewController? {
        self.params = paramModel

        guard let sourceType = imagePickerSourceType(for: paramModel.sourceType) else {
            completionHandler(nil, SPKStatus(code: SPKStatusCode.invalidParameter, message: "Unknown source type: \(paramModel.sourceType)"))
            return nil
        }

        if !UIImagePickerController.isSourceTypeAvailable(sourceType) {
            completionHandler(nil, SPKStatus(code: SPKStatusCode.invalidParameter, message: "Source type \(sourceType.rawValue) is not available on this device."))
            return nil
        }

        if sourceType == .camera && isCameraDenied() {
            let message = "Cannot access camera. Please go to Settings > Privacy and grant the permission for \(UIApplication.shared.btd_appDisplayName ?? "")"

            let alertView = UIAlertController(title: "tip", message: message, preferredStyle: .alert)
            alertView.addAction(UIAlertAction(title: "cancel", style: .cancel, handler: nil))
            alertView.addAction(
                UIAlertAction(
                    title: "go_to_settings", style: .default,
                    handler: { _ in
                        UIApplication.shared.open(URL(string: UIApplication.openSettingsURLString)!, options: [:], completionHandler: nil)
                    }))

            completionHandler(nil, SPKStatus(code: SPKStatusCode.unauthorizedAccess, message: "The access to camera is unauthorized."))
            UIApplication.shared.keyWindow?.topViewController()?.present(alertView, animated: true, completion: nil)
            return nil
        } else {
            let mappedMediaTypes = mappedMediaTypes(from: paramModel.mediaTypes)

            let imagePicker = UIImagePickerController()
            imagePicker.sourceType = sourceType
            imagePicker.allowsEditing = false
            imagePicker.mediaTypes = mappedMediaTypes

            if sourceType == .camera {
                let cameraType = paramModel.cameraType
                guard let cameraDevice = cameraDevice(for: cameraType) else {
                    completionHandler(nil, SPKStatus(code: SPKStatusCode.invalidParameter, message: "Unknown camera type: \(cameraType)"))
                    return nil
                }
                imagePicker.cameraDevice = cameraDevice
            }

            if paramModel.mediaTypes?.map({ $0.lowercased() }).contains(SPKChooseMediaParamValue.video) ?? false {
                imagePicker.videoQuality = .typeHigh
            }

            imagePicker.delegate = self

            self.imagePicker = imagePicker
            self.completionHandler = completionHandler

            return imagePicker
        }
    }

    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        finish(with: nil, status: SPKStatus(code: SPKStatusCode.operationCancelled, message: "The user has cancelled the operation."))
    }

    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
        guard let tempFileModel = SPKChooseMediaMethodResultTempFileModel() else { return }

        if let mediaType = info[.mediaType] as? String {
            if UTTypeConformsTo(mediaType as CFString, kUTTypeMovie as CFString) {
                if let mediaURL = info[.mediaURL] as? URL {
                    tempFileModel.mediaType = SPKChooseMediaMediaType.video.rawValue

                    tempFileModel.tempFilePath = mediaURL.path
                    tempFileModel.tempFileAbsolutePath = tempFileModel.tempFilePath

                    do {
                        let resourceValues = try mediaURL.resourceValues(forKeys: [.fileSizeKey])
                        if let fileSize = resourceValues.fileSize {
                            tempFileModel.size = Int64(fileSize)
                        }
                    } catch {
                        completionHandler?(nil, SPKStatus(code: SPKStatusCode.invalidResult, message: error.localizedDescription))
                        return
                    }

                    if params?.saveToPhotoAlbum ?? false {
                        if UIVideoAtPathIsCompatibleWithSavedPhotosAlbum(mediaURL.path) {
                            UISaveVideoAtPathToSavedPhotosAlbum(mediaURL.path, nil, nil, nil)
                        } else {
                            finish(with: nil, status: SPKStatus(code: SPKStatusCode.invalidResult, message: "Failed to save the video to photo album."))
                            return
                        }
                    }
                } else {
                    finish(with: nil, status: SPKStatus(code: SPKStatusCode.invalidResult, message: "The video URL is nil when taking from camera."))
                    return
                }
            } else if UTTypeConformsTo(mediaType as CFString, kUTTypeImage as CFString) {
                if let image = info[.originalImage] as? UIImage {
                    tempFileModel.mediaType = SPKChooseMediaMediaType.image.rawValue

                    if let imageData = imageDataForImage(image) {
                        if let filePath = writeImageDataToDisk(imageData) {
                            tempFileModel.tempFilePath = filePath
                            tempFileModel.tempFileAbsolutePath = tempFileModel.tempFilePath
                            tempFileModel.size = Int64(imageData.count)
                            tempFileModel.mimeType = "image/jpeg"

                            if params?.needBase64Data ?? false {
                                tempFileModel.base64Data = imageData.base64EncodedString(options: .lineLength64Characters)
                            }

                            if params?.saveToPhotoAlbum ?? false {
                                UIImageWriteToSavedPhotosAlbum(image, nil, nil, nil)
                            }
                        } else {
                            finish(with: nil, status: SPKStatus(code: SPKStatusCode.invalidResult, message: "Failed to save JPEG to disk."))
                            return
                        }
                    } else {
                        finish(with: nil, status: SPKStatus(code: SPKStatusCode.invalidResult, message: "Failed to convert to JPEG."))
                        return
                    }
                } else {
                    finish(with: nil, status: SPKStatus(code: SPKStatusCode.invalidResult, message: "The image is nil when taking from camera."))
                    return
                }
            } else {
                completionHandler?(nil, SPKStatus(code: SPKStatusCode.invalidResult, message: "Unknown media type: \(mediaType)"))
                return
            }
        }

        guard let resultModel = SPKChooseMediaResult(),
              let json = try? MTLJSONAdapter.jsonDictionary(fromModel: tempFileModel) else { return }
        resultModel.tempFiles = [json]
        finish(with: resultModel, status: nil)
    }

    func isCameraDenied() -> Bool {
        let status = AVCaptureDevice.authorizationStatus(for: .video)
        return status == .restricted || status == .denied
    }

    func imageDataForImage(_ image: UIImage) -> Data? {
        return params?.imageData(for: image)
    }

    func writeImageDataToDisk(_ imageData: Data) -> String? {
        let fileName = "\(UUID().uuidString).JPEG"
        let filePath = NSTemporaryDirectory().appending(fileName)
        let fileURL = URL(fileURLWithPath: filePath)

        do {
            try imageData.write(to: fileURL, options: .atomic)
            return filePath
        } catch {
            return nil
        }
    }

    func finish(with resultModel: SPKChooseMediaResult?, status: SPKStatus?) {
        imagePicker?.dismiss(animated: true, completion: nil)
        completionHandler?(resultModel, status)
        completionHandler = nil
    }
}

extension UIApplication {
    fileprivate var btd_appDisplayName: String? {
        return Bundle.main.infoDictionary?["CFBundleDisplayName"] as? String ?? Bundle.main.infoDictionary?["CFBundleName"] as? String
    }
}

extension UIWindow {
    fileprivate func topViewController() -> UIViewController? {
        var topViewController: UIViewController? = rootViewController

        while let presentedViewController = topViewController?.presentedViewController {
            topViewController = presentedViewController
        }

        return topViewController
    }
}

enum SPKStatusCode: Int {
    case success = 0
    case invalidParameter = 1
    case invalidResult = 2
    case operationCancelled = 3
    case unauthorizedAccess = 4
    case other = 99
}

class SPKStatus {
    let code: SPKStatusCode
    let message: String

    init(code: SPKStatusCode, message: String) {
        self.code = code
        self.message = message
    }
}
