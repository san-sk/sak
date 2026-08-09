plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "SakIosBridge"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":sak-sdk"))
            implementation(project(":sak-runtime"))
            implementation(project(":sample-analytics"))
            implementation(project(":sample-licensing"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
