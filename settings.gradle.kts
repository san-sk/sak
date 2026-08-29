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
    ":sak-compose-ui",
    ":sak-ios-bridge",
    ":sak-ksp-processor",
    ":sak-library-debug",
    ":sample-analytics",
    ":sample-licensing",
    ":sak-harness",
)
