// "Sign in with Apple" for the iPhone / iPad app.
// Needs the "Sign in with Apple" capability (iosApp.entitlements) and the Apple provider turned on in
// Firebase console > Authentication > Sign-in method.

import Foundation
import UIKit
import AuthenticationServices
import CryptoKit
import ComposeApp

final class AppleSignInBridge: NSObject, IosAppleSignInHandler,
    ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {

    private var callback: AppleSignInCallback?
    private var rawNonce: String?

    func signIn(callback_ callback: AppleSignInCallback) {
        DispatchQueue.main.async {
            self.callback = callback
            let nonce = AppleSignInBridge.randomNonce()
            self.rawNonce = nonce

            let request = ASAuthorizationAppleIDProvider().createRequest()
            request.requestedScopes = [.fullName, .email]
            request.nonce = AppleSignInBridge.sha256(nonce)

            let controller = ASAuthorizationController(authorizationRequests: [request])
            controller.delegate = self
            controller.presentationContextProvider = self
            controller.performRequests()
        }
    }

    // MARK: ASAuthorizationControllerDelegate

    func authorizationController(controller: ASAuthorizationController,
                                 didCompleteWithAuthorization authorization: ASAuthorization) {
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let tokenData = credential.identityToken,
              let idToken = String(data: tokenData, encoding: .utf8) else {
            finish(idToken: nil, code: nil, email: nil, name: nil, error: "Sign in with Apple failed")
            return
        }
        let code = credential.authorizationCode.flatMap { String(data: $0, encoding: .utf8) }
        let name = [credential.fullName?.givenName, credential.fullName?.familyName]
            .compactMap { $0 }
            .joined(separator: " ")
        finish(idToken: idToken, code: code, email: credential.email, name: name.isEmpty ? nil : name, error: nil)
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        let cancelled = (error as? ASAuthorizationError)?.code == .canceled
        finish(idToken: nil, code: nil, email: nil, name: nil,
               error: cancelled ? "Sign in was cancelled." : error.localizedDescription)
    }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        return scenes.flatMap { $0.windows }.first { $0.isKeyWindow } ?? scenes.first?.windows.first ?? UIWindow()
    }

    private func finish(idToken: String?, code: String?, email: String?, name: String?, error: String?) {
        let cb = callback
        callback = nil
        cb?.onResult(idToken: idToken, rawNonce: rawNonce, authorizationCode: code,
                     email: email, fullName: name, errorMessage: error)
    }

    // MARK: Nonce

    private static func randomNonce(length: Int = 32) -> String {
        let charset = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        var result = ""
        var remaining = length
        while remaining > 0 {
            var bytes = [UInt8](repeating: 0, count: 16)
            _ = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
            for b in bytes where remaining > 0 && Int(b) < charset.count {
                result.append(charset[Int(b)])
                remaining -= 1
            }
        }
        return result
    }

    private static func sha256(_ input: String) -> String {
        SHA256.hash(data: Data(input.utf8)).map { String(format: "%02x", $0) }.joined()
    }
}
