// Copyright 2025 The Sparkling Authors. All rights reserved.
// Licensed under the Apache License Version 2.0 that can be found in the
// LICENSE file in the root directory of this source tree.

import Foundation

private struct ServiceKey: Hashable {
    let typeIdentifier: ObjectIdentifier
    let name: AnyHashable?

    init<T>(_ serviceType: T.Type, name: AnyHashable? = nil) {
        self.typeIdentifier = ObjectIdentifier(serviceType)
        self.name = name
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(typeIdentifier)
        hasher.combine(name)
    }

    static func == (lhs: ServiceKey, rhs: ServiceKey) -> Bool {
        return lhs.typeIdentifier == rhs.typeIdentifier && lhs.name == rhs.name
    }
}

internal final class DIContainer: DIContainerProtocol {
    private var factories: [ServiceKey: Any] = [:]
    private var lock = ReadWriteLock()

    func register<Service>(_ serviceType: Service.Type, name: AnyHashable?, scope: ServiceScope, factory: @escaping Factory<Service>) {
        let key = ServiceKey(serviceType, name: name)
        let entry = ServiceEntry<Service>(scope: scope, factory: factory)
        lock.write {
            factories[key] = entry
        }
    }

    public func resolve<Service>(_ serviceType: Service.Type, name: AnyHashable?) -> Service? {
        let key = ServiceKey(serviceType, name: name)
        var entry: ServiceEntry<Service>?
        var instance: Service?
        lock.read {
            entry = factories[key] as? ServiceEntry<Service>
            instance = entry?.instance
        }
        if let cachedInstance = instance {
            return cachedInstance
        }
        guard let serviceEntry = entry else {
            return nil
        }

        return lock.write {
            if let instance = serviceEntry.instance {
                return instance
            }

            let service = serviceEntry.createService()
            if serviceEntry.scope == .container {
                serviceEntry.instance = service
            }
            return service
        }
    }
}

private class ServiceEntry<Service> {
    var scope: ServiceScope
    var factory: Factory<Service>
    var instance: Service?

    init(scope: ServiceScope, factory: @escaping Factory<Service>) {
        self.scope = scope
        self.factory = factory
    }

    func createService() -> Service {
        return factory()
    }
}
