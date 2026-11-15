# Fun Learn – R8 rules for release builds.
# Most libraries (Firebase, Play Billing, Media3, Compose, Coil, kotlinx) ship their own rules;
# these cover the gaps.

# Readable crash reports in Play Console (Android vitals) – keep line numbers, hide file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# Google sign-in through Credential Manager (classes are loaded by name).
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Ktor + OkHttp: optional platform classes that aren't on Android.
-keep class io.ktor.client.engine.okhttp.** { *; }
-dontwarn io.ktor.**
-dontwarn org.slf4j.**
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn java.lang.management.**

# Firebase Auth helpers loaded by name.
-keep class com.google.firebase.auth.internal.** { *; }

# The app's own platform layer (expect/actual, iOS-shaped bridges) – small, keep it whole.
-keep class com.example.platform.** { *; }
