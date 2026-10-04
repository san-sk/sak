package sak.sdk.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Root configuration structure for the SAK runtime engine.
 *
 * This data class represents the entire remote or local configuration payload for the application.
 * It is easily serializable to JSON, making it ideal for integration with Firebase Remote Config,
 * AWS AppConfig, or a custom configuration backend.
 *
 * Maps feature IDs to their corresponding [FeatureAccess] enablement and parameter states.
 *
 * @param version Configuration schema version, useful for triggering migrations if the JSON structure changes.
 * @param features Dictionary mapping [sak.sdk.feature.FeatureId] strings to their [FeatureAccess] rules.
 */
@Serializable
data class SakConfig(
    val version: Int = 1,
    val features: Map<String, FeatureAccess> = emptyMap(),
) {
    /**
     * Checks whether a specific feature is enabled.
     * Defaults to `true` if the feature is not explicitly listed in [features].
     *
     * @param featureId The string value of the FeatureId to check.
     */
    fun isFeatureEnabled(featureId: String): Boolean =
        features[featureId]?.enabled ?: true
}

/**
 * Shared JSON parser instance configured specifically for SAK configuration payloads.
 *
 * Configured to silently ignore unknown keys (for forward compatibility with older app versions)
 * and encode default values to ensure predictable serialization.
 */
val SakJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

/**
 * Merges a configuration [overlay] on top of this base [SakConfig].
 *
 * Feature enablement entries in [overlay] overwrite or add to existing entries in this config.
 * This is typically used to apply live server configurations over hardcoded local defaults.
 *
 * @param overlay The new configuration to apply on top of the current one.
 * @return A new [SakConfig] instance containing the merged result.
 */
fun SakConfig.merge(overlay: SakConfig): SakConfig {
    val mergedFeatures = features.toMutableMap()
    overlay.features.forEach { (id, access) ->
        mergedFeatures[id] = access
    }
    return copy(
        version = overlay.version,
        features = mergedFeatures,
    )
}
