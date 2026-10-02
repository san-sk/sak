pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "sak"

include(
    ":sak-sdk",
    ":sak-runtime",
    ":sample-analytics",
    ":sample-licensing",
    ":harness-android",
)

project(":harness-android").projectDir = file("sak-harness")
