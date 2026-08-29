plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.devtools.ksp")
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
            implementation(project(":sak-harness")) // For demo features
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", project(":sak-ksp-processor"))
}
