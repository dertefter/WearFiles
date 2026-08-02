pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.google.android.gms.oss-licenses-plugin") {
                useModule("com.google.android.gms:oss-licenses-plugin:0.13.0")
            }
        }
    }

}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "WearFiles"
include(":wearable")
include(":wearable:core:design")
include(":wearable:feature:onboarding")
include(":wearable:core:data")
include(":wearable:feature:file_list")
include(":wearable:core:navigation")
include(":wearable:feature:text_viewer")
include(":wearable:feature:menu")
include(":wearable:feature:rename")
include(":wearable:feature:new_directory")
include(":wearable:feature:delete")
include(":wearable:feature:home")
include(":wearable:feature:images")
include(":wearable:feature:music")
include(":wearable:feature:video")
include(":wearable:feature:image_viewer")
include(":wearable:feature:theming")
include(":wearable:feature:settings")
include(":wearable:feature:pdf_viewer")

include(":mobile")
