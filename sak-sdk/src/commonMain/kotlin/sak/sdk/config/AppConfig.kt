package sak.sdk.config

import kotlinx.serialization.Serializable
import sak.sdk.feature.FeatureConfig
import sak.sdk.license.LicenseInfo

/**
 * Root entitlement JSON document.
 *
 * Same shape for bundled default, login response, IAP result, and backend refresh.
 * Runtime merges overlays into the bundled default to produce [ActiveConfig].
 */
@Serializable
data class AppConfig(
    /** Schema version for forward-compatible parsing. */
    val schema: Int = 1,
    /** Account-level metadata — [LicenseInfo.type] is display-only, not used for gating. */
    val license: LicenseInfo = LicenseInfo(),
    /** Per-feature entitlement rows keyed by feature id. */
    val features: Map<String, FeatureConfig> = emptyMap(),
)

/** Bundled default entitlement shipped with the app. */
typealias DefaultConfig = AppConfig

/** Effective entitlement after merge (default + overlay). */
typealias ActiveConfig = AppConfig
