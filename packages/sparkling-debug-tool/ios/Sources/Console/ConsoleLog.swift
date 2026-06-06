// Copyright (c) 2026 TikTok Pte. Ltd.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// One JS console message captured from a Lynx view via the inspector console
/// delegate, or from any other source feeding [SparklingConsoleStore].
///
/// Lynx 3.6 emits a JSON payload of the form
/// `{"type":"log|debug|info|warn|error","data":[ ... ]}`. We flatten the data
/// array into a single human-readable string while retaining the level so the
/// UI can color/filter by it.
@objc public enum SparklingConsoleLevel: Int {
    case verbose = 0
    case debug = 1
    case info = 2
    case warn = 3
    case error = 4

    public var label: String {
        switch self {
        case .verbose: return "V"
        case .debug: return "D"
        case .info: return "I"
        case .warn: return "W"
        case .error: return "E"
        }
    }

    public static func from(type: String) -> SparklingConsoleLevel {
        switch type.lowercased() {
        case "debug": return .debug
        case "info": return .info
        case "warn", "warning": return .warn
        case "error": return .error
        case "log": return .verbose
        default: return .verbose
        }
    }

    public static func from(name: String) -> SparklingConsoleLevel {
        switch name.uppercased() {
        case "V", "VERBOSE": return .verbose
        case "D", "DEBUG": return .debug
        case "I", "INFO": return .info
        case "W", "WARN", "WARNING": return .warn
        case "E", "ERROR": return .error
        default: return .info
        }
    }
}

public struct SparklingConsoleLog: Equatable {
    public let id: UUID
    public let type: String
    public let level: SparklingConsoleLevel
    public let message: String
    public let tag: String
    public let timestamp: Date
    public var expanded: Bool

    public init(
        type: String,
        level: SparklingConsoleLevel,
        message: String,
        tag: String = "",
        timestamp: Date = Date(),
        expanded: Bool = false
    ) {
        self.id = UUID()
        self.type = type
        self.level = level
        self.message = message
        self.tag = tag
        self.timestamp = timestamp
        self.expanded = expanded
    }

    public func matches(keyword: String) -> Bool {
        let needle = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
        if needle.isEmpty { return true }
        return message.localizedCaseInsensitiveContains(needle)
            || tag.localizedCaseInsensitiveContains(needle)
            || type.localizedCaseInsensitiveContains(needle)
    }

    /// Parse one Lynx inspector console message JSON payload.
    public static func fromJSON(_ raw: String) -> SparklingConsoleLog? {
        guard !raw.isEmpty, let data = raw.data(using: .utf8) else {
            return nil
        }
        if let json = (try? JSONSerialization.jsonObject(with: data, options: [])) as? [String: Any] {
            let type = (json["type"] as? String) ?? "log"
            let payload = json["data"]
            let text = format(payload: payload) ?? (json["message"] as? String) ?? raw
            return SparklingConsoleLog(type: type, level: .from(type: type), message: text)
        }
        return SparklingConsoleLog(type: "log", level: .verbose, message: raw)
    }

    public static func fromPlain(level: String, tag: String, message: String) -> SparklingConsoleLog {
        let lvl = SparklingConsoleLevel.from(name: level)
        return SparklingConsoleLog(type: typeFromLevel(lvl), level: lvl, message: message, tag: tag)
    }

    private static func typeFromLevel(_ level: SparklingConsoleLevel) -> String {
        switch level {
        case .verbose: return "log"
        case .debug: return "debug"
        case .info: return "info"
        case .warn: return "warn"
        case .error: return "error"
        }
    }

    private static func format(payload: Any?) -> String? {
        guard let array = payload as? [Any] else { return nil }
        if array.isEmpty { return "" }
        var parts: [String] = []
        for item in array {
            switch item {
            case let str as String: parts.append(str)
            case let num as NSNumber: parts.append(num.description)
            case is NSNull: parts.append("null")
            default:
                if let data = try? JSONSerialization.data(
                    withJSONObject: item,
                    options: [.prettyPrinted, .sortedKeys]
                ), let s = String(data: data, encoding: .utf8) {
                    parts.append(s)
                } else {
                    parts.append("\(item)")
                }
            }
        }
        return parts.joined(separator: "\t")
    }
}
