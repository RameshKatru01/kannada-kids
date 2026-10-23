// Optional: "Continue with Google" on iPhone.
// This file only does something after you add the GoogleSignIn Swift package in Xcode
// (File > Add Package Dependencies… > https://github.com/google/GoogleSignIn-iOS) and fill in
// GOOGLE_IOS_CLIENT_ID / GOOGLE_REVERSED_CLIENT_ID in Configuration/Config.xcconfig.
// Without it, the app still works: users sign in with their Gmail address or as a guest.

#if canImport(GoogleSignIn)
import Foundation
import UIKit
import GoogleSignIn
import ComposeApp

final class GoogleSignInBridge: NSObject, IosGoogleSignInHandler {

    func signIn(callback_ callback: GoogleSignInCallback) {
        DispatchQueue.main.async {
            let clientId = (Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String) ?? ""
            if clientId.isEmpty || clientId.hasPrefix("$(") {
                callback.onResult(email: nil, displayName: nil, idToken: nil,
                                  errorMessage: "Google sign-in is not configured yet. You can explore as a guest instead!")
                return
            }
            guard let presenter = GoogleSignInBridge.topViewController() else {
                callback.onResult(email: nil, displayName: nil, idToken: nil,
                                  errorMessage: "Unable to show Google sign-in right now.")
                return
            }
            GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
                if let error = error {
                    callback.onResult(email: nil, displayName: nil, idToken: nil,
                                      errorMessage: error.localizedDescription)
                    return
                }
                let user = result?.user
                callback.onResult(email: user?.profile?.email,
                                  displayName: user?.profile?.name,
                                  idToken: user?.idToken?.tokenString,
                                  errorMessage: nil)
            }
        }
    }

    func signOut() {
        GIDSignIn.sharedInstance.signOut()
    }

    private static func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let window = scenes.flatMap { $0.windows }.first { $0.isKeyWindow } ?? scenes.first?.windows.first
        var top = window?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}
#endif
