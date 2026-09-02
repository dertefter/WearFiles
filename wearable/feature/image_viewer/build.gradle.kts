import org.jetbrains.kotlin.gradle.dsl.JvmTarget

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.dertefter.wearable.image_viewer"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(project(":wearable:core:data"))
    implementation(project(":wearable:core:design"))

    implementation(libs.zoomable)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.ui.tooling)
    implementation(libs.horologist.compose.layout)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.coil.compose)
}