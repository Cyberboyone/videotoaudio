plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.devtools.ksp") version "1.9.22-1.0.17"
}

// ---- AdMob configuration (environment-separated) ----
// Production AdMob IDs are supplied via a project-root `admob.properties` file
// (which should be gitignored). When the file or a particular key is absent, the
// official Google TEST ad IDs are used, so the app builds and runs with test ads
// by default. This keeps real IDs out of source control and out of the codebase.
// Keys: appId, bannerHome, bannerHistory, interstitial
val admobProps = mutableMapOf<String, String>().apply {
    val propsFile = rootProject.file("admob.properties")
    if (propsFile.exists()) {
        propsFile.readLines().forEach { raw ->
            val line = raw.trim()
            if (line.isNotEmpty() && !line.startsWith("#") && line.contains("=")) {
                val parts = line.split("=", limit = 2)
                put(parts[0].trim(), parts[1].trim())
            }
        }
    }
}

val ADMOB_TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
val ADMOB_TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
val ADMOB_TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"

fun admobRaw(key: String, testDefault: String): String =
    admobProps[key]?.takeIf { it.isNotBlank() } ?: testDefault

android {
    namespace = "com.nakudin.videotoaudio"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nakudin.videotoaudio"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        // Release signing is only configured when the environment provides a
        // keystore (e.g. CI / GitHub Actions secrets). Builds without these
        // variables stay unsigned-by-config and fall back to the debug
        // keystore, so local development is unaffected.
        create("release") {
            System.getenv("SIGNING_STORE_FILE")?.let { path ->
                storeFile = file(path)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        // DEBUG: always uses Google TEST ad IDs so development/testing never
        // serves real ads (AdMob policy). Independent of admob.properties.
        debug {
            buildConfigField("String", "ADMOB_APP_ID", "\"$ADMOB_TEST_APP_ID\"")
            buildConfigField("String", "ADMOB_BANNER_HOME", "\"$ADMOB_TEST_BANNER\"")
            buildConfigField("String", "ADMOB_BANNER_HISTORY", "\"$ADMOB_TEST_BANNER\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL", "\"$ADMOB_TEST_INTERSTITIAL\"")
            manifestPlaceholders["com.google.android.gms.ads.APPLICATION_ID"] = ADMOB_TEST_APP_ID
        }

        // RELEASE: uses production IDs from admob.properties (with test fallback
        // if the file is missing — PRODUCTION BUILDS MUST provide it). R8
        // minification + resource shrinking produce a lean, optimized bundle.
        // ProGuard rules live in proguard-rules.pro.
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val relAppId = admobRaw("appId", ADMOB_TEST_APP_ID)
            val relBannerHome = admobRaw("bannerHome", ADMOB_TEST_BANNER)
            val relBannerHistory = admobRaw("bannerHistory", ADMOB_TEST_BANNER)
            val relInterstitial = admobRaw("interstitial", ADMOB_TEST_INTERSTITIAL)
            buildConfigField("String", "ADMOB_APP_ID", "\"$relAppId\"")
            buildConfigField("String", "ADMOB_BANNER_HOME", "\"$relBannerHome\"")
            buildConfigField("String", "ADMOB_BANNER_HISTORY", "\"$relBannerHistory\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL", "\"$relInterstitial\"")
            manifestPlaceholders["com.google.android.gms.ads.APPLICATION_ID"] = relAppId

            // Attach the release signing config only when a keystore is provided.
            if (System.getenv("SIGNING_STORE_FILE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi"
        )
    }
}

dependencies {
    val composeBom = "androidx.compose:compose-bom:2024.10.00"

    implementation(platform(composeBom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // Android Material Components provides the Material3 Android theme/widget
    // styles (Theme.Material3.*, Widget.Material3.*) referenced by res/values/themes.xml.
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // Room (local conversion history).
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore (modern local preferences).
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Google Mobile Ads SDK (AdMob).
    implementation("com.google.android.gms:play-services-ads:22.6.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
