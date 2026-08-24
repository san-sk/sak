package sak.sdk.feature

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Config load policy for a feature id in [sak.sdk.config.AppConfig.features].
 *
 * Distinct from [FeatureStatus] (feature-reported UI state) and from
 * [onCreate][Feature.onCreate] / [onDestroy][Feature.onDestroy] naming — this enum only
 * answers whether runtime should instantiate the feature.
 */
@Serializable
enum class FeatureState {
    /** Load feature — runtime calls [Feature.onCreate]. */
    @SerialName("enabled")
    Enabled,

    /** Do not load; host may show locked or upsell UI for this id. */
    @SerialName("disabled")
    Disabled,

    /** Do not load; host should hide all surfaces for this id. */
    @SerialName("hide")
    Hidden,
}

/** `true` when runtime should call [Feature.onCreate] for this config row. */
fun FeatureState.shouldLoad(): Boolean = this == FeatureState.Enabled
