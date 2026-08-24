package sak.sdk.feature

/** Stable feature identifier matching [sak.sdk.config.AppConfig.features] keys (e.g. `analytics`). */
data class FeatureId(val value: String)

/**
 * Error reported by a feature to the host.
 *
 * @property featureId Originating feature.
 * @property message User- or log-facing description.
 * @property cause Optional underlying exception.
 * @property recoverable When `true`, host may retry or show recoverable UI instead of failing hard.
 */
data class FeatureError(
    val featureId: FeatureId,
    val message: String,
    val cause: Throwable? = null,
    val recoverable: Boolean = false,
)
