package sak.sdk.license

import kotlinx.serialization.json.JsonElement
import sak.sdk.feature.FeatureConfig

/**
 * Runtime entitlement snapshot for one **loaded** feature.
 *
 * Built from [FeatureConfig] + [LicenseInfo.type] — features should not parse raw JSON themselves.
 *
 * Compare [sak.sdk.feature.FeatureContext]: one-shot data at [sak.sdk.feature.Feature.onCreate],
 * versus live [kotlinx.coroutines.flow.StateFlow]s while the instance runs.
 *
 * @property capabilities Capability strings granted to this feature.
 * @property licenseType Copy of [LicenseInfo.type] for display-only UI.
 * @property settings Opaque per-feature settings from config — feature interprets keys.
 */
data class FeatureEntitlement(
    val capabilities: Set<String> = emptySet(),
    val licenseType: String = "guest",
    val settings: Map<String, JsonElement> = emptyMap(),
)

/** Build runtime entitlement from a config row and account license type. */
fun FeatureConfig.toEntitlement(licenseType: String): FeatureEntitlement =
    FeatureEntitlement(
        capabilities = capabilities,
        licenseType = licenseType,
        settings = settings,
    )
