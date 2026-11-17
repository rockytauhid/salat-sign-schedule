plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.salatsign.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.salatsign.tv"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val isReleaseTaskRequested = gradle.startParameter.taskNames.any {
        it.contains("Release", ignoreCase = true)
    }

    val storeFilePath = project.findProperty("RELEASE_STORE_FILE")?.toString()
        ?: project.findProperty("KEYSTORE_PATH")?.toString()
        ?: System.getenv("RELEASE_STORE_FILE")
        ?: System.getenv("KEYSTORE_PATH")
    val storePassword = project.findProperty("RELEASE_STORE_PASSWORD")?.toString()
        ?: project.findProperty("KEYSTORE_PASSWORD")?.toString()
        ?: System.getenv("RELEASE_STORE_PASSWORD")
        ?: System.getenv("KEYSTORE_PASSWORD")
    val keyAlias = project.findProperty("RELEASE_KEY_ALIAS")?.toString()
        ?: project.findProperty("KEY_ALIAS")?.toString()
        ?: System.getenv("RELEASE_KEY_ALIAS")
        ?: System.getenv("KEY_ALIAS")
    val keyPassword = project.findProperty("RELEASE_KEY_PASSWORD")?.toString()
        ?: project.findProperty("KEY_PASSWORD")?.toString()
        ?: System.getenv("RELEASE_KEY_PASSWORD")
        ?: System.getenv("KEY_PASSWORD")

    val hasReleaseKeystore = storeFilePath != null &&
        file(storeFilePath).exists() &&
        !storePassword.isNullOrBlank() &&
        !keyAlias.isNullOrBlank() &&
        !keyPassword.isNullOrBlank()

    if (isReleaseTaskRequested && !hasReleaseKeystore) {
        throw GradleException(
            "Release keystore is not configured. Define RELEASE_STORE_FILE (or KEYSTORE_PATH), " +
            "RELEASE_STORE_PASSWORD (or KEYSTORE_PASSWORD), RELEASE_KEY_ALIAS (or KEY_ALIAS), and " +
            "RELEASE_KEY_PASSWORD (or KEY_PASSWORD) as Gradle properties or environment variables."
        )
    }

    signingConfigs {
        create("release") {
            if (hasReleaseKeystore) {
                storeFile = file(storeFilePath!!)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isPseudoLocalesEnabled = true
        }
    }

    lint {
        error.add("NewApi")
        error.add("MissingTranslation")
        abortOnError = true
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Jetpack Compose BOM & Core
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material.icons)

    // Jetpack Compose for TV (10-foot UI)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)

    // Lifecycle & Compose Integration
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Offline Prayer Time Calculation Engine
    implementation(libs.adhan)

    // DataStore (Preferences Storage)
    implementation(libs.androidx.datastore.preferences)

    // Coil 3 (Mosque Logos & Signage Slide Image Loading - Internal storage & assets)
    implementation(libs.coil.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Kotlinx Serialization JSON
    implementation(libs.kotlinx.serialization.json)

    // Background Scheduling & Resilience
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}