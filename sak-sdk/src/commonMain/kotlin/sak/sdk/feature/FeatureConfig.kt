package sak.sdk.feature

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Serializable per-feature block inside [sak.sdk.config.AppConfig.features].
 *
 * @property state Load policy — see [FeatureState].
 * @property capabilities Strings the loaded feature may use (UI locks, [sak.sdk.navigation.RouteGuard]).
 * @property settings Opaque JSON map — runtime pass-through; only the feature interprets keys.
 */
@Serializable
data class FeatureConfig(
    val state: FeatureState = FeatureState.Enabled,
    val capabilities: Set<String> = emptySet(),
    val settings: Map<String, JsonElement> = emptyMap(),
)
