// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

extension String: SPKKitCompatibleValue {}

public extension SPKKitWrapper where Base == String {
    var urlEncoded: String? {
        let allowedCharacterSet = URL.spk.urlValid.subtracting(.init(charactersIn: ":!*();@/&?+$,='"))
        return base.addingPercentEncoding(withAllowedCharacters: allowedCharacterSet)
    }
    
    /// Parses the string as a URL query string and returns a dictionary of key-value pairs.
    /// 
    /// This method extracts query parameters from a URL string and converts them into a dictionary.
    /// It handles various edge cases including empty values, missing equals signs, and URL encoding.
    /// 
    /// - Parameter isEscapes: Whether to decode percent-encoded characters in keys and values.
    /// - Returns: A dictionary containing the parsed query parameters, or `nil` if no valid parameters found.
    func queryDict(isEscapes: Bool) -> [String: String]? {
        guard let queryString = base.spk.queryString() else {
            return nil
        }
        var queryDict: [String: String] = [:]
        var queryArray = queryString.components(separatedBy: "&")
        queryArray.forEach({ queryItem in
            var pair = queryItem.components(separatedBy: "=")
            if pair.count < 2 || isEmptyString(pair[0]) || isEmptyString(pair[1]) {
                if let range = queryItem.range(of: "=") {
                    let keyString = String(queryItem[..<range.lowerBound])
                    let valueString = String(queryItem[range.upperBound...])
                    
                    if !isEmptyString(keyString), !isEmptyString(valueString) {
                        pair = [keyString, valueString]
                    } else {
                        return
                    }
                } else {
                    return
                }
            }
            var keyString: String? = pair[0]
            var valueString: String? = pair[1]
            
            if isEscapes {
                keyString = keyString?.removingPercentEncoding
                valueString = valueString?.removingPercentEncoding
            }
            
            if !isEmptyString(keyString), !isEmptyString(valueString) {
                queryDict.updateValue(valueString ?? "", forKey: keyString ?? "")
            }
        })
        return queryDict.isEmpty ? nil : queryDict
    }
    func queryString() -> String? {
        guard let questionMarkRange = base.range(of: "?") else {
            return nil
        }
        
        let queryStart = questionMarkRange.upperBound
        var remaining = String(base[queryStart...])
        
        while remaining.hasPrefix("?") {
            remaining.removeFirst()
        }
        
        if let fragmentRange = remaining.range(of: "#") {
            let query = String(remaining[..<fragmentRange.lowerBound])
            return query.isEmpty ? nil : query
        }
        
        return remaining.isEmpty ? nil : remaining
    }
}
