package sak.sdk.config

import kotlinx.serialization.json.Json

/** Shared JSON parser for config and license payloads. */
val SakJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

/**
 * Merge a signed license overlay into default config.
 */
fun SakConfig.mergeLicenseOverlay(overlay: SakConfig): SakConfig {
    val mergedFeatures = features.toMutableMap()
    overlay.features.forEach { (id, access) ->
        val base = mergedFeatures[id] ?: FeatureAccess()
        mergedFeatures[id] = mergeAccess(base, access)
    }
    return copy(
        version = overlay.version,
        features = mergedFeatures,
    )
}

private fun mergeAccess(base: FeatureAccess, overlay: FeatureAccess): FeatureAccess =
    FeatureAccess(
        enabled = overlay.enabled,
        visible = overlay.visible,
        plan = overlay.plan,
        grants = overlay.grants,
        entries = overlay.entries ?: base.entries,
        routes = overlay.routes ?: base.routes,
        attrs = base.attrs + overlay.attrs,
    )
