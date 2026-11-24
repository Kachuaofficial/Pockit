/*
 * Android app build configuration: SDK support, variants, signing, dependencies and generated Room schemas.
 * Build settings do not grant runtime permissions or deploy Firebase rules/functions.
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.invatech.pockit"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.invatech.pockit"
        minSdk = 24
        targetSdk = 37
        versionCode = 12
        versionName = "2.3"

        testInstrumentationRunner = if (providers.gradleProperty("releaseSmoke").orNull == "true") {
            "com.invatech.pockit.ReleaseSmokeInstrumentation"
        } else {
            "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFile = rootProject.file("upload-keystore.jks")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: (project.findProperty("KEYSTORE_PASSWORD") as? String) ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: (project.findProperty("KEY_ALIAS") as? String) ?: "upload"
                keyPassword = System.getenv("KEY_PASSWORD") ?: (project.findProperty("KEY_PASSWORD") as? String) ?: ""
            }
        }
    }

    buildTypes {
        debug {
            // Use the registered Pockit app in internal/debug builds as well. Developers
            // must register their handset as an AdMob test device to avoid invalid traffic.
            manifestPlaceholders["admobAppId"] = "ca-app-pub-5858375161867194~8387330202"
        }
        release {
            manifestPlaceholders["admobAppId"] = "ca-app-pub-5858375161867194~8387330202"
            isMinifyEnabled = true
            isShrinkResources = true
            ndk {
                debugSymbolLevel = "FULL"
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile?.exists() == true && !releaseSigning.storePassword.isNullOrEmpty()) {
                signingConfig = releaseSigning
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation("io.coil-kt.coil3:coil-compose:3.6.3")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.3")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation("com.google.firebase:firebase-functions:23.0.0")
    implementation("com.google.firebase:firebase-appcheck-playintegrity:20.1.0")
    debugImplementation("com.google.firebase:firebase-appcheck-debug:20.1.0")
    implementation(libs.googleid)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // QR Code generation and barcode scanning
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")

    // Google ML Kit Text Recognition (On-device OCR for Receipts & Bills)
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")

    // Google AdMob & User Messaging Platform (GDPR/UMP consent)
    implementation(libs.google.mobile.ads)
    implementation(libs.user.messaging.platform)

    // Google Play In-App Updates
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // Jetpack Glance AppWidget (Home Screen Quick Actions & Stats)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // AndroidX Room Database (Offline-first Local Persistence)
    val roomVersion = "2.7.0"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // AndroidX WorkManager (Reliable, Battery-efficient Local Background Scheduling)
    implementation("androidx.work:work-runtime-ktx:2.10.0")
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

// Next-Gen contains mediation compatibility classes; never package both SDK runtimes.
configurations.configureEach {
    exclude(group = "com.google.android.gms", module = "play-services-ads")
    exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
}
