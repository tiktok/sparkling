// Copyright (c) 2025 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import AVFoundation
import CoreServices
import MobileCoreServices
import Photos
import UIKit

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

typealias SPKChooseMediaCompletionHandler = (SPKChooseMediaMethodResultModel?, SPKStatus?) -> Void

protocol SPKChooseMediaPicker {
    func supported(with paramModel: SPKChooseMediaMethodParamModel) -> Bool
    func mediaPicker(with paramModel: SPKChooseMediaMethodParamModel, completionHandler: @escaping SPKChooseMediaCompletionHandler) -> UIViewController?
}

class SPKDefaultMediaPicker: NSObject, SPKChooseMediaPicker, UINavigationControllerDelegate, UIImagePickerControllerDelegate {

    private var params: SPKChooseMediaMethodParamModel?
    private var completionHandler: SPKChooseMediaCompletionHandler?
    private weak var imagePicker: UIImagePickerController?

    func supported(with paramModel: SPKChooseMediaMethodParamModel) -> Bool {
        return true
    }

    func mediaPicker(with paramModel: SPKChooseMediaMethodParamModel, completionHandler: @escaping SPKChooseMediaCompletionHandler) -> UIViewController? {
        self.params = paramModel
        if params?.compressOption == nil {
            params?.compressOption = SPKChooseMediaCompressOption.default.rawValue
        }

        var sourceType = UIImagePickerController.SourceType.camera
        if paramModel.sourceType == SPKChooseMediaMediaSourceType.album.rawValue {
            sourceType = .photoLibrary
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
            var mappedMediaTypes: [String] = []
            if let mediaTypes = paramModel.mediaTypes {
                if mediaTypes.contains(SPKChooseMediaMediaType.image.rawValue) {
                    mappedMediaTypes.append(kUTTypeImage as String)
                }
                if mediaTypes.contains(SPKChooseMediaMediaType.video.rawValue) {
                    mappedMediaTypes.append(kUTTypeMovie as String)
                }
                if mappedMediaTypes.isEmpty {
                    mappedMediaTypes.append(kUTTypeImage as String)
                }
            } else {
                mappedMediaTypes.append(kUTTypeImage as String)
            }

            let imagePicker = UIImagePickerController()
            imagePicker.sourceType = sourceType
            imagePicker.allowsEditing = false
            imagePicker.mediaTypes = mappedMediaTypes

            if sourceType == .camera {
                let cameraType = paramModel.cameraType
                let cameraDevice: UIImagePickerController.CameraDevice
                if let cameraTypeEnum = SPKChooseMediaCameraType(rawValue: cameraType) {
                    switch cameraTypeEnum {
                    case .front:
                        cameraDevice = .front
                    case .back:
                        cameraDevice = .rear
                    }
                } else {
                    finish(with: nil, status: SPKStatus(code: SPKStatusCode.invalidParameter, message: "Unknown camera type: \(cameraType)"))
                    return nil
                }
                imagePicker.cameraDevice = cameraDevice
            }

            if let mediaTypes = paramModel.mediaTypes, mediaTypes.contains(SPKChooseMediaMediaType.video.rawValue) {
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
        let tempFileModel = SPKChooseMediaMethodResultTempFileModel()

        if let mediaType = info[.mediaType] as? String {
            if UTTypeConformsTo(mediaType as CFString, kUTTypeMovie as CFString) {
                if let mediaURL = info[.mediaURL] as? URL {
                    tempFileModel.mediaType = SPKChooseMediaMediaType.video.rawValue

                    tempFileModel.tempFilePath = mediaURL.path.spk_stringByStrippingSandboxPath()
                    tempFileModel.tempFileAbsolutePath = tempFileModel.tempFilePath?.spk_stringFromProcessFile()

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
                            tempFileModel.tempFilePath = filePath.spk_stringByStrippingSandboxPath()
                            tempFileModel.tempFileAbsolutePath = tempFileModel.tempFilePath?.spk_stringFromProcessFile()
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

        let resultModel = SPKChooseMediaMethodResultModel()
        resultModel.tempFiles = [tempFileModel]
        finish(with: resultModel, status: nil)
    }

    func isCameraDenied() -> Bool {
        let status = AVCaptureDevice.authorizationStatus(for: .video)
        return status == .restricted || status == .denied
    }

    func imageDataForImage(_ image: UIImage) -> Data? {
        guard let params = params else { return nil }

        if params.compressOption == SPKChooseMediaCompressOption.none.rawValue {
            return image.jpegData(compressionQuality: 1.0)
        }

        if params.compressOption == SPKChooseMediaCompressOption.both.rawValue || params.compressOption == SPKChooseMediaCompressOption.onlyImage.rawValue
            || params.compressOption == SPKChooseMediaCompressOption.default.rawValue
        {

            let compressWidth = params.compressWidth ?? 0
            let compressHeight = params.compressHeight ?? 0

            if compressWidth > 0 && compressHeight > 0 {
                let imageWidth = image.size.width
                let imageHeight = image.size.height
                let wScale = imageWidth / CGFloat(compressWidth)
                let hScale = imageHeight / CGFloat(compressHeight)

                var newSize = image.size
                var needRedraw = false

                if wScale > hScale && wScale > 1 {
                    newSize.width = CGFloat(compressWidth)
                    newSize.height = imageHeight / wScale
                    needRedraw = true
                } else if hScale > wScale && hScale > 1 {
                    newSize.width = imageWidth / hScale
                    newSize.height = CGFloat(compressHeight)
                    needRedraw = true
                }

                if needRedraw {
                    UIGraphicsBeginImageContextWithOptions(newSize, false, 0.0)
                    image.draw(in: CGRect(x: 0, y: 0, width: newSize.width, height: newSize.height))
                    let resizedImage = UIGraphicsGetImageFromCurrentImageContext()
                    UIGraphicsEndImageContext()

                    if let resizedImage = resizedImage {
                        let compressionQuality = params.compressionQuality ?? 0.8
                        return resizedImage.jpegData(compressionQuality: compressionQuality)
                    }
                }
            }

            let compressionQuality = params.compressionQuality ?? 0.8
            return image.jpegData(compressionQuality: compressionQuality)
        }

        return image.jpegData(compressionQuality: 0.8)
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

    func finish(with resultModel: SPKChooseMediaMethodResultModel?, status: SPKStatus?) {
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
