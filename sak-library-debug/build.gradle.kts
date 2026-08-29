plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    id("com.android.library")
}

kotlin {
    androidTarget()
    
    sourceSets {
        commonMain.dependencies {
            implementation(project(":sak-sdk"))
            implementation(project(":sak-runtime"))
            implementation(project(":sak-compose-ui"))
            implementation(libs.compose.multiplatform.material3)
        }
    }
}

android {
    namespace = "sak.debug"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
}
