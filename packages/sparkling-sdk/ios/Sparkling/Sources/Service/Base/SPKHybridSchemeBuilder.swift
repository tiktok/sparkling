// Copyright 2026 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

/// A Lynx resource target accepted by the canonical Sparkling page scheme.
public enum SPKHybridLynxResource: Equatable, Sendable {
    /// A bundle name or path resolved by Sparkling's template provider.
    case bundle(String)

    /// An absolute resource URL resolved by Sparkling's resource loader.
    case url(String)

    fileprivate var queryItem: URLQueryItem {
        switch self {
        case .bundle(let bundle):
            return URLQueryItem(name: "bundle", value: bundle)
        case .url(let url):
            return URLQueryItem(name: "url", value: url)
        }
    }

    fileprivate var value: String {
        switch self {
        case .bundle(let bundle):
            return bundle
        case .url(let url):
            return url
        }
    }
}

/// Errors returned while constructing a canonical Sparkling page scheme.
public enum SPKHybridSchemeBuilderError: Error, Equatable, Sendable {
    /// The bundle or URL target is empty.
    case emptyResource

    /// A URL-style target is not an absolute URL.
    case invalidResourceURL(String)

    /// An additional item attempts to replace the builder-owned resource target.
    case reservedQueryItem(String)

    /// Foundation could not represent the requested values as a valid URL.
    case unableToBuild
}

extension SPKHybridSchemeBuilderError: LocalizedError {
    public var errorDescription: String? {
        switch self {
        case .emptyResource:
            return "The Sparkling Lynx resource must not be empty."
        case .invalidResourceURL(let value):
            return "The Sparkling Lynx resource URL must be absolute: '\(value)'."
        case .reservedQueryItem(let name):
            return "The '\(name)' query item is owned by the Sparkling scheme builder."
        case .unableToBuild:
            return "The canonical Sparkling page scheme could not be built."
        }
    }
}

extension SPKHybridSchemeParam {
    /// Builds a canonical `hybrid://lynxview_page` URL.
    ///
    /// The resource target is always the first query item. The encoded URL preserves
    /// the order and duplicates of additional query items, while Foundation performs
    /// exactly one layer of percent encoding. A resolver that exposes query items as
    /// a dictionary may still apply last-value-wins semantics. Callers cannot provide
    /// a second `bundle` or `url` target because Sparkling's resolver requires one
    /// unambiguous resource owner.
    ///
    /// - Parameters:
    ///   - resource: The Lynx bundle or absolute URL to load.
    ///   - queryItems: Additional page and container configuration.
    /// - Returns: A canonical URL accepted by `SPKHybridSchemeParam.resolver`.
    public static func buildLynxPageScheme(
        resource: SPKHybridLynxResource,
        queryItems: [URLQueryItem] = []
    ) throws -> URL {
        guard !resource.value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            throw SPKHybridSchemeBuilderError.emptyResource
        }

        if case .url(let value) = resource {
            guard let innerURL = URL(string: value), innerURL.scheme?.isEmpty == false else {
                throw SPKHybridSchemeBuilderError.invalidResourceURL(value)
            }
        }

        if let reservedItem = queryItems.first(where: {
            let name = $0.name.lowercased()
            return name == "bundle" || name == "url"
        }) {
            throw SPKHybridSchemeBuilderError.reservedQueryItem(reservedItem.name)
        }

        var components = URLComponents()
        components.scheme = "hybrid"
        components.host = Self.lynxViewPageScheme
        components.queryItems = [resource.queryItem] + queryItems

        guard let url = components.url,
            Self.canResolve(url: url),
            Self.engineType(withURL: url) == .SPKHybridEngineTypeLynx
        else {
            throw SPKHybridSchemeBuilderError.unableToBuild
        }
        return url
    }
}
