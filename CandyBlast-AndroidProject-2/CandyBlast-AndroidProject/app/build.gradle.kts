plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.techarenastudio.candyblast"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.techarenastudio.candyblast"
        minSdk = 23
        targetSdk = 36
        // If an older version is already on Play Console, make versionCode HIGHER than the old one.
        versionCode = 1
        versionName = "1.0"
    }

    // Signing is read from environment variables (used by the GitHub build).
    // Without them, the release bundle is simply built unsigned.
    signingConfigs {
        create("release") {
            val ksFile = System.getenv("KEYSTORE_FILE")
            if (!ksFile.isNullOrEmpty()) {
                storeFile = file(ksFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (!System.getenv("KEYSTORE_FILE").isNullOrEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.webkit:webkit:1.12.1")
    // AdMob only. Google UMP consent library comes along with this automatically.
    implementation("com.google.android.gms:play-services-ads:24.9.0")
}
