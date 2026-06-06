import Foundation

#if canImport(Sparkling_Storage)
import Sparkling_Storage

private let storageSuiteName = "com.sparkling.template.storage"

class StorageServiceImpl: StorageService {
    private let userDefaults = UserDefaults(suiteName: storageSuiteName) ?? UserDefaults.standard

    func setObject(key: String, value: Any) {
        userDefaults.set(value, forKey: key)
    }

    func object(forKey key: String) -> Any? {
        return userDefaults.object(forKey: key)
    }

    func removeObject(forKey key: String) {
        userDefaults.removeObject(forKey: key)
    }
}
#endif
