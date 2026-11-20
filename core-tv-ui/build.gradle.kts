// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.salatsign.core.tv"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        error.add("NewApi")
        error.add("MissingTranslation")
        abortOnError = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    api(project(":core"))

    api(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)
    api(libs.material)

    // Jetpack Compose
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    api(libs.androidx.compose.material.icons)

    // Jetpack Compose for TV (10-foot UI)
    api(libs.androidx.tv.foundation)
    api(libs.androidx.tv.material)

    // Lifecycle & Compose Integration
    api(libs.androidx.activity.compose)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.lifecycle.runtime.compose)
    api(libs.androidx.lifecycle.viewmodel.compose)

    // Coil 3 (Local internal storage & assets)
    api(libs.coil.compose)

    testImplementation(libs.junit)
}
