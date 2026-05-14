// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// Network task protocol
public protocol SPKHttpTaskProtocol {
    var protectTimeout: TimeInterval { get set }
    var timeoutInterval: TimeInterval { get set }
    func resume()
}

/// Simple network task implementation
public class SPKHttpTask: NSObject, SPKHttpTaskProtocol {
    public var protectTimeout: TimeInterval = 30
    public var timeoutInterval: TimeInterval = 30

    private let dataTask: URLSessionDataTask?
    private let downloadTask: URLSessionDownloadTask?

    init(dataTask: URLSessionDataTask? = nil, downloadTask: URLSessionDownloadTask? = nil) {
        self.dataTask = dataTask
        self.downloadTask = downloadTask
    }

    public func resume() {
        dataTask?.resume()
        downloadTask?.resume()
    }
}

/// Network manager
public class TTNetworkManager: NSObject {
    public static let shared = TTNetworkManager()

    private let session: URLSession

    private override init() {
        let configuration = URLSessionConfiguration.default
        session = URLSession(configuration: configuration)
    }

    /// Download task
    public func downloadTaskWithRequest(
        _ urlString: String,
        parameters: [String: Any]?,
        headerField: [String: Any]?,
        needCommonParams: Bool,
        progress: ((Float) -> Void)?,
        destination: URL,
        autoResume: Bool,
        completionHandler: @escaping (SPKHttpResponse?, URL?, Error?) -> Void
    ) -> SPKHttpTask {

        // Simplified download implementation
        guard let url = URL(string: urlString) else {
            let error = NSError(domain: "TTNetworkError", code: -1000, userInfo: [NSLocalizedDescriptionKey: "Invalid URL"])
            DispatchQueue.main.async {
                completionHandler(nil, nil, error)
            }
            return SPKHttpTask()
        }

        var request = URLRequest(url: url)
        request.httpMethod = "GET"

        // Add request headers
        if let headers = headerField as? [String: String] {
            for (key, value) in headers {
                request.setValue(value, forHTTPHeaderField: key)
            }
        }

        // Download task
        let downloadTask = session.downloadTask(with: request) { [weak self] location, response, error in
            guard let self = self else { return }

            if let error = error {
                DispatchQueue.main.async {
                    completionHandler(nil, nil, error)
                }
                return
            }

            guard let location = location else {
                let error = NSError(domain: "TTNetworkError", code: -1001, userInfo: [NSLocalizedDescriptionKey: "Download location not found"])
                DispatchQueue.main.async {
                    completionHandler(nil, nil, error)
                }
                return
            }

            do {
                // Move file to destination
                let fileManager = FileManager.default
                if fileManager.fileExists(atPath: destination.path) {
                    try fileManager.removeItem(at: destination)
                }
                try fileManager.moveItem(at: location, to: destination)

                // Construct response
                var httpResponse: SPKHttpResponseChromium?
                if let urlResponse = response as? HTTPURLResponse {
                    httpResponse = SPKHttpResponseChromium(
                        statusCode: urlResponse.statusCode,
                        allHeaderFields: urlResponse.allHeaderFields
                    )
                }

                DispatchQueue.main.async {
                    completionHandler(httpResponse, destination, nil)
                }
            } catch {
                DispatchQueue.main.async {
                    completionHandler(nil, nil, error)
                }
            }
        }

        return SPKHttpTask(downloadTask: downloadTask)
    }

    class func shareInstance() -> TTNetworkManager {
        return TTNetworkManager.shared
    }

    /// Upload task with multipart form data
    public func uploadTaskWithRequest(
        _ url: String,
        fileURL: URL,
        name: String,
        fileName: String,
        mimeType: String,
        parameters: [String: Any]?,
        headerField: [String: Any]?,
        needCommonParams: Bool,
        progress: ((Float) -> Void)?,
        completionHandler: @escaping (SPKHttpResponse?, Any?, Error?) -> Void
    ) -> SPKHttpTask {

        // Validate URL
        guard let uploadUrl = URL(string: url) else {
            let error = NSError(domain: "TTNetworkError", code: -1000, userInfo: [NSLocalizedDescriptionKey: "Invalid URL"])
            DispatchQueue.main.async {
                completionHandler(nil, nil, error)
            }
            return SPKHttpTask()
        }

        // Create URLRequest
        var request = URLRequest(url: uploadUrl)
        request.httpMethod = "POST"

        // Add request headers
        if let headers = headerField as? [String: String] {
            for (key, value) in headers {
                request.setValue(value, forHTTPHeaderField: key)
            }
        }

        // Create multipart form data upload task
        let boundary = "Boundary-\(UUID().uuidString)"
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

        // Create upload task with multipart form data
        let uploadTask = session.uploadTask(with: request, fromFile: fileURL) { [weak self] data, response, error in
            guard let self = self else { return }

            if let error = error {
                DispatchQueue.main.async {
                    completionHandler(nil, nil, error)
                }
                return
            }

            // Construct response
            var httpResponse: SPKHttpResponseChromium?
            if let urlResponse = response as? HTTPURLResponse {
                httpResponse = SPKHttpResponseChromium(
                    statusCode: urlResponse.statusCode,
                    allHeaderFields: urlResponse.allHeaderFields
                )
            }

            // Parse response data if available
            var responseObject: Any? = nil
            if let data = data {
                do {
                    responseObject = try JSONSerialization.jsonObject(with: data, options: [])
                } catch {
                    // If JSON parsing fails, return the data as string
                    responseObject = String(data: data, encoding: .utf8)
                }
            }

            DispatchQueue.main.async {
                completionHandler(httpResponse, responseObject, nil)
            }
        }

        return SPKHttpTask(dataTask: uploadTask)
    }
}
