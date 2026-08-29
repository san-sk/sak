package sak.sdk.bundle

import sak.sdk.feature.FeatureId

/** Metadata for a feature listed in a host manifest (optional tooling). */
data class FeatureRegistration(
    val id: FeatureId,
    /** JVM class name of the [FeatureModule] implementation. */
    val factoryClass: String,
    val sdkVersion: String = "1.0.0",
)
