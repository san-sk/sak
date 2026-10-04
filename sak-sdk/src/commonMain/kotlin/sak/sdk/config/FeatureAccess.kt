package sak.sdk.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Entitlement, enablement, and configuration state for an individual feature module.
 *
 * Controls whether the feature should be active in the host application shell, and provides
 * optional feature-specific parameters driven by remote configuration.
 *
 * When [enabled] is `true`, the feature is instantiated, connected to the host lifecycle,
 * and publishes its entry points.
 * When [enabled] is `false`, the feature is stopped, destroyed, and all published entry points are removed.
 *
 * @param enabled True if the feature should be active and running; false to destroy it.
 * @param params Optional key-value dictionary for remote configuration parameters specific to this feature
 *               (e.g., {"samplingRate": 0.5}, {"themeVariant": "dark"}).
 */
@Serializable
data class FeatureAccess(
    val enabled: Boolean = true,
    val params: Map<String, JsonElement> = emptyMap(),
)
