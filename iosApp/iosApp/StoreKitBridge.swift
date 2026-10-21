// Subscriptions on iPhone with StoreKit 2.
// Product IDs (premium_monthly, premium_halfyearly, premium_yearly) must exist in
// App Store Connect > your app > Subscriptions, in one subscription group.

import Foundation
import StoreKit
import ComposeApp

final class StoreKitBridge: NSObject, IosBillingHandler {

    private var updatesTask: Task<Void, Never>?

    override init() {
        super.init()
        // Finish renewals / purchases made on other devices or while the app was closed.
        updatesTask = Task.detached {
            for await update in Transaction.updates {
                if case .verified(let transaction) = update {
                    await transaction.finish()
                }
            }
        }
    }

    deinit { updatesTask?.cancel() }

    func loadProducts(productIds: [String], callback: IosJsonCallback) {
        Task {
            do {
                let products = try await Product.products(for: productIds)
                let list = products.map { ["id": $0.id, "price": $0.displayPrice] }
                callback.onResult(json: Self.json(list), errorMessage: nil)
            } catch {
                callback.onResult(json: nil, errorMessage: error.localizedDescription)
            }
        }
    }

    func purchase(productId: String, callback: IosJsonCallback) {
        Task {
            do {
                guard let product = try await Product.products(for: [productId]).first else {
                    callback.onResult(json: nil, errorMessage: "This plan isn't available in the App Store yet.")
                    return
                }
                let result = try await product.purchase()
                switch result {
                case .success(let verification):
                    guard case .verified(let transaction) = verification else {
                        callback.onResult(json: nil, errorMessage: "The App Store couldn't verify this purchase.")
                        return
                    }
                    await transaction.finish()
                    let info = await Self.describe(transaction, product: product)
                    callback.onResult(json: Self.json(info), errorMessage: nil)
                case .pending:
                    callback.onResult(json: nil, errorMessage: "Waiting for approval (for example Ask to Buy). Premium starts once it's approved.")
                case .userCancelled:
                    callback.onResult(json: nil, errorMessage: nil)
                @unknown default:
                    callback.onResult(json: nil, errorMessage: nil)
                }
            } catch {
                callback.onResult(json: nil, errorMessage: error.localizedDescription)
            }
        }
    }

    func activePurchases(callback: IosJsonCallback) {
        Task {
            var list: [[String: Any]] = []
            for await entitlement in Transaction.currentEntitlements {
                guard case .verified(let transaction) = entitlement,
                      transaction.productType == .autoRenewable,
                      transaction.revocationDate == nil else { continue }
                let product = try? await Product.products(for: [transaction.productID]).first
                list.append(await Self.describe(transaction, product: product))
            }
            callback.onResult(json: Self.json(list), errorMessage: nil)
        }
    }

    // MARK: - Helpers

    private static func describe(_ t: Transaction, product: Product?) async -> [String: Any] {
        var autoRenew = true
        if let statuses = try? await product?.subscription?.status {
            for status in statuses {
                if case .verified(let renewal) = status.renewalInfo,
                   case .verified(let tx) = status.transaction,
                   tx.originalID == t.originalID {
                    autoRenew = renewal.willAutoRenew
                }
            }
        }
        var info: [String: Any] = [
            "id": t.productID,
            "orderId": String(t.id),
            "purchased": Int64(t.purchaseDate.timeIntervalSince1970 * 1000),
            "autoRenew": autoRenew
        ]
        if let exp = t.expirationDate {
            info["expires"] = Int64(exp.timeIntervalSince1970 * 1000)
        }
        return info
    }

    private static func json(_ object: Any) -> String {
        guard let data = try? JSONSerialization.data(withJSONObject: object),
              let text = String(data: data, encoding: .utf8) else { return "[]" }
        return text
    }
}
