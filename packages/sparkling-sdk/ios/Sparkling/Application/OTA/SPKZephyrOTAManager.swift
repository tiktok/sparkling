// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

struct SPKZephyrOTARuntimeConfig: Decodable {
    struct Polling: Decodable {
        let enabled: Bool?
        let intervalMs: Double?
    }

    let enabled: Bool?
    let versionUrl: String?
    let polling: Polling?
}

struct SPKZephyrVersionInfo {
    let snapshotId: String
    let versionUrl: String
}

final class SPKZephyrOTAManager {
    static let updateReadyNotification = Notification.Name("com.tiktok.sparkling.zephyr-ota.update-ready")
    static let shared = SPKZephyrOTAManager()

    private let defaults = UserDefaults.standard
    private let queue = DispatchQueue(label: "com.tiktok.sparkling.zephyr-ota", qos: .utility)
    private let decoder = JSONDecoder()

    private let configFileName = "sparkling.zephyr.json"
    private let versionInfoPath = "__get_version_info__"
    private let defaultPollingIntervalMs = 15 * 60 * 1000.0
    private let minimumPollingIntervalMs = 1_000.0
    private let appliedSnapshotIdKey = "sparkling_zephyr_ota.applied_snapshot_id"
    private let appliedVersionUrlKey = "sparkling_zephyr_ota.applied_version_url"
    private let pendingSnapshotIdKey = "sparkling_zephyr_ota.pending_snapshot_id"
    private let pendingVersionUrlKey = "sparkling_zephyr_ota.pending_version_url"
    private let pendingUpdateAvailableKey = "sparkling_zephyr_ota.pending_update_available"
    private let lastCheckAtKey = "sparkling_zephyr_ota.last_check_at"

    private var refreshInFlight = false
    private var pollingTimer: DispatchSourceTimer?
    private var pollingIntervalMs: Double?

    private init() {}

    private func stopPollingLocked() {
        pollingTimer?.cancel()
        pollingTimer = nil
        pollingIntervalMs = nil
    }

    func resolveBundle(_ bundle: String?) -> String? {
        guard let bundle, !bundle.isEmpty else {
            return bundle
        }
        if Self.isRemoteUrl(bundle) {
            return bundle
        }

        guard let config = readRuntimeConfig(), config.enabled ?? false else {
            return bundle
        }

        return Self.joinBundleUrl(baseUrl: defaults.string(forKey: appliedVersionUrlKey), bundle: bundle) ?? bundle
    }

    func hasPendingUpdate() -> Bool {
        return defaults.bool(forKey: pendingUpdateAvailableKey)
    }

    @discardableResult
    func applyPendingUpdateIfNeeded() -> Bool {
        guard defaults.bool(forKey: pendingUpdateAvailableKey),
              let snapshotId = defaults.string(forKey: pendingSnapshotIdKey),
              let versionUrl = defaults.string(forKey: pendingVersionUrlKey) else {
            return false
        }

        defaults.set(snapshotId, forKey: appliedSnapshotIdKey)
        defaults.set(versionUrl, forKey: appliedVersionUrlKey)
        defaults.removeObject(forKey: pendingSnapshotIdKey)
        defaults.removeObject(forKey: pendingVersionUrlKey)
        defaults.set(false, forKey: pendingUpdateAvailableKey)
        return true
    }

    func refreshIfNeeded(force: Bool = false) {
        guard let config = readRuntimeConfig(),
              config.enabled ?? false,
              config.polling?.enabled ?? true,
              let versionUrl = Self.normalizeVersionUrl(config.versionUrl) else {
            return
        }

        let now = Date().timeIntervalSince1970 * 1000
        let lastCheckAt = defaults.double(forKey: lastCheckAtKey)
        let intervalMs = config.polling?.intervalMs ?? defaultPollingIntervalMs
        if !force, now - lastCheckAt < max(intervalMs, 0) {
            return
        }

        queue.async {
            if self.refreshInFlight {
                return
            }
            self.refreshInFlight = true
            self.defaults.set(now, forKey: self.lastCheckAtKey)
            defer {
                self.refreshInFlight = false
            }

            do {
                guard let versionInfo = try self.fetchVersionInfo(baseUrl: versionUrl) else {
                    return
                }
                let appliedSnapshotId = self.defaults.string(forKey: self.appliedSnapshotIdKey)
                let pendingSnapshotId = self.defaults.string(forKey: self.pendingSnapshotIdKey)
                let normalizedNextSnapshotId = versionInfo.snapshotId.trimmingCharacters(in: .whitespacesAndNewlines)

                let shouldNotify: Bool
                if appliedSnapshotId?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty != false {
                    self.defaults.set(normalizedNextSnapshotId, forKey: self.appliedSnapshotIdKey)
                    self.defaults.set(versionInfo.versionUrl, forKey: self.appliedVersionUrlKey)
                    self.defaults.removeObject(forKey: self.pendingSnapshotIdKey)
                    self.defaults.removeObject(forKey: self.pendingVersionUrlKey)
                    self.defaults.set(false, forKey: self.pendingUpdateAvailableKey)
                    shouldNotify = false
                } else if Self.shouldMarkUpdateReady(previousSnapshotId: appliedSnapshotId, nextSnapshotId: normalizedNextSnapshotId) {
                    self.defaults.set(normalizedNextSnapshotId, forKey: self.pendingSnapshotIdKey)
                    self.defaults.set(versionInfo.versionUrl, forKey: self.pendingVersionUrlKey)
                    self.defaults.set(true, forKey: self.pendingUpdateAvailableKey)
                    shouldNotify = pendingSnapshotId?.trimmingCharacters(in: .whitespacesAndNewlines) != normalizedNextSnapshotId
                } else {
                    self.defaults.removeObject(forKey: self.pendingSnapshotIdKey)
                    self.defaults.removeObject(forKey: self.pendingVersionUrlKey)
                    self.defaults.set(false, forKey: self.pendingUpdateAvailableKey)
                    shouldNotify = false
                }
                self.defaults.set(Date().timeIntervalSince1970 * 1000, forKey: self.lastCheckAtKey)
                NSLog("SPKZephyrOTAManager refresh: applied=%@ pending=%@ next=%@ shouldNotify=%@ versionUrl=%@",
                      appliedSnapshotId ?? "nil",
                      pendingSnapshotId ?? "nil",
                      versionInfo.snapshotId,
                      shouldNotify.description,
                      versionInfo.versionUrl)
                if shouldNotify {
                    DispatchQueue.main.async {
                        NotificationCenter.default.post(name: Self.updateReadyNotification, object: versionInfo)
                    }
                }
            } catch {
                NSLog("SPKZephyrOTAManager refresh failed: %@", error.localizedDescription)
            }
        }
    }

    func startPolling() {
        guard let config = readRuntimeConfig(),
              config.enabled ?? false,
              config.polling?.enabled ?? true else {
            stopPolling()
            return
        }

        let intervalMs = max(config.polling?.intervalMs ?? defaultPollingIntervalMs, minimumPollingIntervalMs)
        queue.async {
            if let currentInterval = self.pollingIntervalMs,
               abs(currentInterval - intervalMs) < 0.0001,
               self.pollingTimer != nil {
                self.refreshIfNeeded(force: true)
                return
            }

            self.stopPollingLocked()
            self.pollingIntervalMs = intervalMs
            let timer = DispatchSource.makeTimerSource(queue: self.queue)
            timer.schedule(deadline: .now(), repeating: .milliseconds(Int(intervalMs)))
            timer.setEventHandler { [weak self] in
                self?.refreshIfNeeded(force: true)
            }
            self.pollingTimer = timer
            timer.resume()
        }
    }

    func stopPolling() {
        queue.async {
            self.stopPollingLocked()
        }
    }

    func readRuntimeConfig() -> SPKZephyrOTARuntimeConfig? {
        guard let configURL = findConfigURL(),
              let data = try? Data(contentsOf: configURL) else {
            return nil
        }

        return try? decoder.decode(SPKZephyrOTARuntimeConfig.self, from: data)
    }

    func fetchVersionInfo(baseUrl: String) throws -> SPKZephyrVersionInfo? {
        let endpoint = Self.versionInfoEndpoint(baseUrl: baseUrl)
        let data = try Data(contentsOf: endpoint)
        return try parseVersionInfo(data: data, fallbackBaseUrl: baseUrl)
    }

    func parseVersionInfo(data: Data, fallbackBaseUrl: String) throws -> SPKZephyrVersionInfo? {
        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        let snapshotId = (json?["snapshot_id"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !snapshotId.isEmpty else {
            return nil
        }

        let rawVersionUrl = json?["version_url"] as? String
        guard let versionUrl = Self.normalizeVersionUrl(rawVersionUrl) ?? Self.normalizeVersionUrl(fallbackBaseUrl) else {
            return nil
        }

        return SPKZephyrVersionInfo(
            snapshotId: snapshotId,
            versionUrl: versionUrl
        )
    }

    private func findConfigURL() -> URL? {
        guard let root = Bundle.main.resourceURL else {
            return nil
        }

        let prefixes = ["LynxResources/Assets", "LynxResources", "Assets", ""]
        for prefix in prefixes {
            let candidate = prefix.isEmpty
                ? root.appendingPathComponent(configFileName)
                : root.appendingPathComponent(prefix).appendingPathComponent(configFileName)
            if FileManager.default.fileExists(atPath: candidate.path) {
                return candidate
            }
        }

        return Bundle.main.url(forResource: "sparkling.zephyr", withExtension: "json")
    }

    static func normalizeVersionUrl(_ value: String?) -> String? {
        guard let trimmed = value?.trimmingCharacters(in: .whitespacesAndNewlines),
              !trimmed.isEmpty else {
            return nil
        }

        let withoutTrailingSlash = trimmed.replacingOccurrences(of: "/+$", with: "", options: .regularExpression)
        if withoutTrailingSlash.lowercased().hasPrefix("http://") || withoutTrailingSlash.lowercased().hasPrefix("https://") {
            return withoutTrailingSlash
        }

        let withoutLeadingSlash = withoutTrailingSlash.replacingOccurrences(of: "^/+", with: "", options: .regularExpression)
        return "https://\(withoutLeadingSlash)"
    }

    static func versionInfoEndpoint(baseUrl: String) -> URL {
        let normalized = baseUrl.replacingOccurrences(of: "/+$", with: "", options: .regularExpression)
        return URL(string: "\(normalized)/__get_version_info__")!
    }

    static func joinBundleUrl(baseUrl: String?, bundle: String?) -> String? {
        guard let normalizedBase = normalizeVersionUrl(baseUrl),
              let rawBundle = bundle?.trimmingCharacters(in: .whitespacesAndNewlines),
              !rawBundle.isEmpty else {
            return nil
        }

        let normalizedBundle = rawBundle
            .replacingOccurrences(of: "^\\./", with: "", options: .regularExpression)
            .replacingOccurrences(of: "^/+", with: "", options: .regularExpression)

        guard !normalizedBundle.isEmpty else {
            return nil
        }

        return "\(normalizedBase)/\(normalizedBundle)"
    }

    static func isRemoteUrl(_ value: String) -> Bool {
        let lowercased = value.lowercased()
        return lowercased.hasPrefix("http://") || lowercased.hasPrefix("https://")
    }

    static func shouldMarkUpdateReady(previousSnapshotId: String?, nextSnapshotId: String) -> Bool {
        guard let previousSnapshotId = previousSnapshotId?.trimmingCharacters(in: .whitespacesAndNewlines),
              !previousSnapshotId.isEmpty else {
            return false
        }
        return previousSnapshotId != nextSnapshotId.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
