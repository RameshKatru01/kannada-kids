// Keychain storage for the sign-in tokens (used by PlatformAuth.ios.kt through IosPlatformBridge).
// Items are readable after the first unlock and stay on this device only, so they are not in
// iCloud / computer backups and don't move to a new phone.

import Foundation
import Security
import ComposeApp

final class KeychainStore: NSObject, IosSecureStore {

    private let service = (Bundle.main.bundleIdentifier ?? "KannadaKids") + ".auth"

    private func query(_ key: String) -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key
        ]
    }

    func read(key: String) -> String? {
        var q = query(key)
        q[kSecReturnData as String] = true
        q[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: AnyObject?
        guard SecItemCopyMatching(q as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    func write(key: String, value: String) {
        let data = Data(value.utf8)
        let update: [String: Any] = [
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        ]
        let status = SecItemUpdate(query(key) as CFDictionary, update as CFDictionary)
        if status == errSecItemNotFound {
            var add = query(key)
            add.merge(update) { _, new in new }
            SecItemAdd(add as CFDictionary, nil)
        }
    }

    func remove(key: String) {
        SecItemDelete(query(key) as CFDictionary)
    }
}
