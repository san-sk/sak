package sak.sdk.config

import kotlinx.serialization.json.Json

/** Shared JSON parser for config and license payloads (lenient unknown keys). */
val SakJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

/**
 * Merge a signed license overlay into default config.
 * Only [AppConfig.features] are merged; scaffold stays from the base config.
 */
fun AppConfig.mergeLicenseOverlay(overlay: AppConfig): AppConfig {
    val mergedFeatures = features.toMutableMap()
    overlay.features.forEach { (id, toggle) ->
        val base = mergedFeatures[id] ?: FeatureToggle()
        mergedFeatures[id] = mergeToggle(base, toggle)
    }
    return copy(
        version = overlay.version,
        features = mergedFeatures,
    )
}

private fun mergeToggle(base: FeatureToggle, overlay: FeatureToggle): FeatureToggle =
    FeatureToggle(
        enabled = overlay.enabled,
        visible = overlay.visible,
        plan = overlay.plan,
        grants = overlay.grants.ifEmpty { base.grants },
        entries = overlay.entries ?: base.entries,
        routes = overlay.routes ?: base.routes,
        attrs = base.attrs + overlay.attrs,
    )
