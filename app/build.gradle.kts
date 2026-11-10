import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Fixed signing key: every build (debug + release, local + GitHub Actions) is signed the same
// way, so a new APK installs over the old one without uninstalling.
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun signingValue(envName: String, propName: String): String? =
    System.getenv(envName)?.takeIf { it.isNotBlank() } ?: keystoreProps.getProperty(propName)

android {
    namespace = "com.privacyshield.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.privacyshield.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "1.2.2"
    }

    signingConfigs {
        create("shield") {
            storeFile = file(signingValue("SIGNING_STORE_FILE", "storeFile") ?: "release.keystore")
            storePassword = signingValue("SIGNING_STORE_PASSWORD", "storePassword")
            keyAlias = signingValue("SIGNING_KEY_ALIAS", "keyAlias")
            keyPassword = signingValue("SIGNING_KEY_PASSWORD", "keyPassword")
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("shield")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("shield")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.01.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.security:security-crypto:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
