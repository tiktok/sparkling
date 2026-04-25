// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

extension URL: SPKKitCompatibleValue {}

public extension SPKKitWrapper where Base == URL {
    static func url(string: String, relativeTo url: URL? = nil) -> URL? {
        return _url(string: string, relativeTo: url)
    }
    static func url(string: String, queryItems: [AnyHashable: Any]? = nil, fragment: String? = nil) -> URL? {
        return _url(string: string, queryItems: queryItems, fragment: fragment)
    }
    internal static let urlValid: CharacterSet = CharacterSet.urlUserAllowed
        .union(.urlPathAllowed)
        .union(.urlHostAllowed)
        .union(.urlQueryAllowed)
        .union(.urlFragmentAllowed)
        .union(.urlPasswordAllowed)

    private static func _url(string str: String, relativeTo url: URL?) -> URL? {
        if isEmptyString(str) {
            return nil
        }
        let fixStr = str.trimmingCharacters(in: .whitespacesAndNewlines)
        var u: URL?
        if let url = url {
            u = URL(string: fixStr, relativeTo: url)
        } else {
            u = URL(string: fixStr)
        }
        if u == nil {
            //
            // Fail to construct a URL directly. Try to construct a URL with a encodedQuery.
            var sourceString = fixStr
            let fragmentRange = fixStr.range(of: "#")
            var fragment: String?
            if let fragmentRange = fragmentRange {
                sourceString = String(fixStr[..<fragmentRange.lowerBound])
                fragment = String(fixStr[fragmentRange.lowerBound...])
            }
            let substrings = sourceString.components(separatedBy: "?")
            if substrings.count > 1 {
                let beforeQuery = substrings[0]
                let queryString = substrings[1]
                let paramsList = queryString.components(separatedBy: "&")
                var encodedQueryParams: [AnyHashable: Any] = [:]
                paramsList.forEach { param in
                    let keyAndValue = param.components(separatedBy: "=")
                    if keyAndValue.count > 1 {
                        let key = keyAndValue[0]
                        var value = keyAndValue[1]

                        if value.range(of: "%") != nil {
                            value = value.removingPercentEncoding ?? value
                        }
                        let allowedCharacterSet = urlValid.subtracting(.init(charactersIn: ":/?#@!$&'(){}*+="))
                        let encodedValue = value.addingPercentEncoding(withAllowedCharacters: allowedCharacterSet)
                        encodedQueryParams[key] = encodedValue
                    }
                }

                let encodedQuery = encodedQueryParams.spk.urlQueryString
                let encodedURLString = beforeQuery.appending("?").appending(encodedQuery ?? "").appending(fragment ?? "")

                if let url = url {
                    u = URL(string: encodedURLString, relativeTo: url)
                } else {
                    u = URL(string: encodedURLString)
                }
            }

            if u == nil {
                let fixStr2 = fixStr.addingPercentEncoding(withAllowedCharacters: urlValid) ?? ""
                if let url = url {
                    u = URL(string: fixStr2, relativeTo: url)
                } else {
                    u = URL(string: fixStr2)
                }
            }
            assert(u != nil, "Fail to construct a URL.Please be sure that url is legal and contact with the professionals.")
        }
        return u
    }

    private static func _url(string str: String, queryItems: [AnyHashable: Any]? = nil, fragment: String? = nil) -> URL? {
        if isEmptyString(str) {
            return nil
        }
        var querys: String = ""
        if let queryItems = queryItems, queryItems.count > 0 {
            queryItems.forEach { key, value in
                guard value != nil else { return }
                let encodedKey = "\(key)".spk.urlEncoded
                let encodedValue = "\(value)".spk.urlEncoded
                if let encodedKey = encodedKey, let encodedValue = encodedValue {
                    querys.append("\(encodedKey)=\(encodedValue)")
                    querys.append("&")
                }
            }
            if querys.hasSuffix("&") {
                querys.removeLast()
            }
        }

        var resultURL: String = str
        if querys.count > 0 {
            if resultURL.range(of: "?") == nil {
                resultURL.append("?")
            } else if !resultURL.hasSuffix("?") && !resultURL.hasSuffix("&") {
                resultURL.append("&")
            }
            resultURL.append(querys)
        }

        if let fragment = fragment, fragment.count > 0 {
            resultURL.append("#\(fragment)")
        }

        let url = self.url(string: resultURL)
        return url
    }
    var queryItems: [String: String]? {
        guard let query = base.query, query.count > 0 else {
            return nil
        }
        var queries: [String: String] = [:]
        let params = query.components(separatedBy: "&")
        params.forEach { param in
            let keyAndValue = param.components(separatedBy: "=")
            if keyAndValue.count > 1 {
                let key = keyAndValue[0]
                let value = keyAndValue[1]
                queries[key] = value
            }
        }
        return queries
    }
    var decodedQueryItems: [String: String]? {
        let components = URLComponents(string: base.absoluteString)
        guard let queryItems = components?.queryItems, queryItems.count > 0 else {
            return nil
        }
        var queries: [String: String] = [:]
        queryItems.forEach { queryItem in
            queries[queryItem.name] = queryItem.value
        }
        return queries
    }
    func merging(query key: String, value: String, encode: Bool = false) -> URL {
        return merging(queries: [key: value], encode: encode)
    }
    func merging(queries: [String: String], encode: Bool = false) -> URL {
        guard var components = URLComponents(string: base.absoluteString) else { return base }
        let oldPairs = decodedQueryItems ?? [:]
        let newPairs = queries
        let mergeQueries = mergeOrderedPairs(oldPairs, newPairs)
        if encode {
            let encodedQueryArray = mergeQueries.map { "\($0.spk.urlEncoded ?? $0)=\($1.spk.urlEncoded ?? $1)" }
            let encodedQuery = encodedQueryArray.joined(separator: "&")
            components.percentEncodedQuery = encodedQuery
        } else {
            let queryItems = mergeQueries.map { URLQueryItem(name: $0, value: $1) }
            components.queryItems = queryItems
        }
        return components.url ?? base
    }
    
    internal func mergeOrderedPairs<K: Hashable, V>(_ oldPairs: [K: V], _ newPairs: [K: V]) -> [(K, V)] {
        var newPairs = newPairs
        var mergePairs: [(K, V)] = []
        for oldPair in oldPairs {
            if let newValue = newPairs[oldPair.key] {
                mergePairs.append((oldPair.key, newValue))
                newPairs[oldPair.key] = nil
            } else {
                mergePairs.append((oldPair.key, oldPair.value))
            }
        }
        for newPair in newPairs {
            mergePairs.append((newPair.key, newPair.value))
        }
        return mergePairs
    }
}
