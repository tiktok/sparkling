// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

public enum ServiceScope {
    case transient
    case container
}

public typealias Factory<T> = () -> T

public protocol DIContainerProtocol {
    func register<Service>(_ serviceType: Service.Type, name: AnyHashable?, scope: ServiceScope, factory: @escaping Factory<Service>)
    func resolve<Service>(_ serviceType: Service.Type, name: AnyHashable?) -> Service?
}

extension DIContainerProtocol {
    public func register<Service>(_ serviceType: Service.Type, name: AnyHashable? = nil, scope: ServiceScope = .container, factory: @escaping Factory<Service>) {
        self.register(serviceType, name: name, scope: scope, factory: factory)
    }

    public func resolve<Service>(_ serviceType: Service.Type, name: AnyHashable? = nil) -> Service? {
        return self.resolve(serviceType, name: name)
    }
}
