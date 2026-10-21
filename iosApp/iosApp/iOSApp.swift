import SwiftUI
import AVFoundation
import ComposeApp
#if canImport(GoogleSignIn)
import GoogleSignIn
#endif

@main
struct iOSApp: App {
    init() {
        // Play the app's sounds and speech even when the ring/silent switch is on silent.
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: [.mixWithOthers])
        try? AVAudioSession.sharedInstance().setActive(true)

        // Subscriptions (StoreKit)
        IosPlatformBridge.shared.billing = StoreKitBridge()

        #if canImport(GoogleSignIn)
        // "Continue with Google" support (only when the GoogleSignIn package is added in Xcode).
        IosPlatformBridge.shared.googleSignIn = GoogleSignInBridge()
        #endif
    }

    var body: some Scene {
        WindowGroup {
            #if canImport(GoogleSignIn)
            ContentView()
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
            #else
            ContentView()
            #endif
        }
    }
}
