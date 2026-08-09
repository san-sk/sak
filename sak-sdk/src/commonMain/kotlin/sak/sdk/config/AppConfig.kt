package sak.sdk.config

import kotlinx.serialization.Serializable
import sak.sdk.layout.ScaffoldConfig

/**
 * Root JSON config document.
 *
 * - **DefaultConfig**: scaffold + features (shipped with the app).
 * - **License overlay**: features only (signed); merged into ActiveConfig.
 * - Scaffold is never changed by the license overlay.
 */
@Serializable
data class AppConfig(
    val version: Int = 1,
    val scaffold: ScaffoldConfig? = null,
    val features: Map<String, FeatureToggle> = emptyMap(),
)

typealias DefaultConfig = AppConfig

/** DefaultConfig after license overlay merge ([sak.sdk.config.mergeLicenseOverlay]). */
typealias ActiveConfig = AppConfig
