plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.sl.parknote"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sl.parknote"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Keystore and passwords are supplied only through the manual release-signing job.
    // CI/PR builds stay unsigned. No signing information is stored in source control.
    val releaseSigningNames = listOf(
        "PARKNOTE_SIGNING_STORE_FILE",
        "PARKNOTE_SIGNING_STORE_PASSWORD",
        "PARKNOTE_SIGNING_KEY_ALIAS",
        "PARKNOTE_SIGNING_KEY_PASSWORD",
    )
    val releaseSigningEnabled = releaseSigningNames.any { !System.getenv(it).isNullOrBlank() }
    if (releaseSigningEnabled) {
        require(releaseSigningNames.all { !System.getenv(it).isNullOrBlank() }) {
            "Incomplete ParkNote release signing environment"
        }
        signingConfigs {
            create("environmentRelease") {
                val storePath = checkNotNull(System.getenv("PARKNOTE_SIGNING_STORE_FILE"))
                require(file(storePath).isFile) { "ParkNote release keystore does not exist" }
                storeFile = file(storePath)
                storePassword = System.getenv("PARKNOTE_SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("PARKNOTE_SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("PARKNOTE_SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (releaseSigningEnabled) {
                signingConfig = signingConfigs.getByName("environmentRelease")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.core:core-ktx:1.19.1")
    // The Android 17 Location Button has no stable artifact as of 2026-10-22.
    // Its sole alpha dependency is deliberately isolated to the permission UI.
    implementation("androidx.core.locationbutton:locationbutton-compose:1.0.0-alpha01")
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
