package sak.sdk.config

import kotlinx.serialization.Serializable

/**
 * Root JSON config document.
 *
 * - **DefaultConfig**: features only (shipped with the app).
 * - **License overlay**: features only (signed); merged into ActiveConfig.
 */
@Serializable
data class AppConfig(
    val version: Int = 1,
    val features: Map<String, FeatureToggle> = emptyMap(),
)

typealias DefaultConfig = AppConfig

/** DefaultConfig after license overlay merge ([sak.sdk.config.mergeLicenseOverlay]). */
typealias ActiveConfig = AppConfig
