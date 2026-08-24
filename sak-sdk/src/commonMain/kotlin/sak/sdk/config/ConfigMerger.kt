package sak.sdk.config

import sak.sdk.feature.FeatureConfig

/**
 * Merge [overlay] into this default config.
 *
 * Overlay wins for [FeatureConfig.state], [FeatureConfig.capabilities], and [license];
 * [FeatureConfig.settings] are shallow-merged per feature id.
 */
fun AppConfig.mergeEntitlementOverlay(overlay: AppConfig): AppConfig {
    val mergedFeatures = features.toMutableMap()
    overlay.features.forEach { (id, patch) ->
        val base = mergedFeatures[id] ?: FeatureConfig()
        mergedFeatures[id] = mergeFeatureConfig(base, patch)
    }
    return copy(
        schema = overlay.schema,
        license = overlay.license,
        features = mergedFeatures,
    )
}

private fun mergeFeatureConfig(base: FeatureConfig, overlay: FeatureConfig): FeatureConfig =
    FeatureConfig(
        state = overlay.state,
        capabilities = overlay.capabilities,
        settings = base.settings + overlay.settings,
    )

/** @deprecated Use [mergeEntitlementOverlay]. */
fun AppConfig.mergeLicenseOverlay(overlay: AppConfig): AppConfig = mergeEntitlementOverlay(overlay)
