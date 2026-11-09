import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.secrets)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    compilerOptions {
        // expect/actual classes are used for the small platform layer in com.example.platform
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.ktor.client.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.ui)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.auth)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services)
            implementation(libs.googleid)
            implementation(libs.android.billing.ktx)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        // Only used by the "Generate voice" action to export the list of spoken phrases
        androidUnitTest.dependencies {
            implementation("junit:junit:4.13.2")
        }
        // Apple (cinterop) APIs used by the iOS platform code
        all {
            if (name.startsWith("ios")) {
                languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
                languageSettings.optIn("kotlinx.cinterop.BetaInteropApi")
            }
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.example.resources"
    generateResClass = always
}

android {
    namespace = "com.example"
    compileSdk = 36

    defaultConfig {
        // Same application id as the original Android app so Play Store updates keep working.
        applicationId = "com.funlearn.studio"
        minSdk = 24
        targetSdk = 36
        // Release builds (GitHub "Build release" workflow) pass a higher number each time;
        // Google Play needs versionCode to go up with every upload.
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 2
        versionName = System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "1.1"
    }

    signingConfigs {
        // A fixed debug key (committed on purpose) so every debug APK, on any machine or on GitHub,
        // has the same SHA-1 fingerprint. Google Sign-In only works for fingerprints registered in Firebase.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
            storeFile = file(keystorePath)
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = "upload"
            keyPassword = System.getenv("KEY_PASSWORD") ?: System.getenv("STORE_PASSWORD")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val releaseKeystore = file(System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks")
            if (releaseKeystore.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

// Reads WEB_CLIENT_ID / GEMINI_API_KEY from .env (falls back to .env.example) into BuildConfig.
secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }
