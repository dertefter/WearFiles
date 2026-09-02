import org.jetbrains.kotlin.gradle.dsl.JvmTarget

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.oss.licenses)
}

android {
    namespace = "com.dertefter.wearfiles"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.dertefter.wearfiles"
        minSdk = 26
        targetSdk = 37
        versionCode = project.property("appVersionCode").toString().toInt() + 1
        versionName = project.property("appVersionName").toString()
    }

    signingConfigs {
        create("release") {
            val envKeystoreFile = System.getenv("ANDROID_KEYSTORE_FILE")
            val envKeystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            val envKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
            val envKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")

            if (envKeystoreFile != null && envKeystorePassword != null && envKeyAlias != null && envKeyPassword != null) {
                storeFile = file(envKeystoreFile)
                storePassword = envKeystorePassword
                keyAlias = envKeyAlias
                keyPassword = envKeyPassword
            } else {
                val debugConfig = signingConfigs.getByName("debug")
                storeFile = debugConfig.storeFile
                storePassword = debugConfig.storePassword
                keyAlias = debugConfig.keyAlias
                keyPassword = debugConfig.keyPassword
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            ndk {
                debugSymbolLevel = "FULL"
            }
            optimization {
                enable = true
            }
        }
        debug {
            optimization {
                enable = true
            }
        }
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
    implementation(project(":wearable:core:navigation"))

    implementation(project(":wearable:feature:home"))
    implementation(project(":wearable:feature:images"))
    implementation(project(":wearable:feature:music"))
    implementation(project(":wearable:feature:onboarding"))
    implementation(project(":wearable:feature:file_list"))
    implementation(project(":wearable:feature:rename"))
    implementation(project(":wearable:feature:delete"))
    implementation(project(":wearable:feature:new_directory"))
    implementation(project(":wearable:feature:video"))
    implementation(project(":wearable:feature:theming"))
    implementation(project(":wearable:feature:settings"))
    implementation(project(":wearable:feature:text_viewer"))
    implementation(project(":wearable:feature:image_viewer"))
    implementation(project(":wearable:feature:pdf_viewer"))

    implementation(libs.material.kolor)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.wear.compose.navigation)
    implementation(libs.horologist.compose.layout)
    implementation(libs.androidx.wear.remote.interactions)
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.gson)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.oss.licenses.droibit)
}
