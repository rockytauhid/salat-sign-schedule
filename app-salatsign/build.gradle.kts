// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
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

    androidResources {
        localeFilters += listOf("en")
    }

    val isReleaseTaskRequested = gradle.startParameter.taskNames.any { task ->
        val name = task.substringAfterLast(':')
        (name.startsWith("assemble", ignoreCase = true) || name.startsWith("bundle", ignoreCase = true)) &&
            name.endsWith("Release", ignoreCase = true)
    }

    val storeFilePath = project.findProperty("SALATSIGN_STORE_FILE")?.toString()
        ?: System.getenv("SALATSIGN_STORE_FILE")
        ?: project.findProperty("RELEASE_STORE_FILE")?.toString()
        ?: project.findProperty("KEYSTORE_PATH")?.toString()
        ?: System.getenv("RELEASE_STORE_FILE")
        ?: System.getenv("KEYSTORE_PATH")
    val storePassword = project.findProperty("SALATSIGN_STORE_PASSWORD")?.toString()
        ?: System.getenv("SALATSIGN_STORE_PASSWORD")
        ?: project.findProperty("RELEASE_STORE_PASSWORD")?.toString()
        ?: project.findProperty("KEYSTORE_PASSWORD")?.toString()
        ?: System.getenv("RELEASE_STORE_PASSWORD")
        ?: System.getenv("KEYSTORE_PASSWORD")
    val keyAlias = project.findProperty("SALATSIGN_KEY_ALIAS")?.toString()
        ?: System.getenv("SALATSIGN_KEY_ALIAS")
        ?: project.findProperty("RELEASE_KEY_ALIAS")?.toString()
        ?: project.findProperty("KEY_ALIAS")?.toString()
        ?: System.getenv("RELEASE_KEY_ALIAS")
        ?: System.getenv("KEY_ALIAS")
    val keyPassword = project.findProperty("SALATSIGN_KEY_PASSWORD")?.toString()
        ?: System.getenv("SALATSIGN_KEY_PASSWORD")
        ?: project.findProperty("RELEASE_KEY_PASSWORD")?.toString()
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
            "SalatSign release keystore is not configured. Define SALATSIGN_STORE_FILE (or RELEASE_STORE_FILE), " +
            "SALATSIGN_STORE_PASSWORD, SALATSIGN_KEY_ALIAS, and SALATSIGN_KEY_PASSWORD as Gradle properties or environment variables."
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
    implementation(project(":core"))
    implementation(project(":core-tv-ui"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
