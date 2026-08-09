package sak.sdk.feature

/** Stable feature identifier (e.g. `analytics`, `licensing`). */
data class FeatureId(val value: String)

/** Recoverable or fatal error reported by a feature to the host. */
data class FeatureError(
    val featureId: FeatureId,
    val message: String,
    val cause: Throwable? = null,
    /** If true, the host may retry or show a recoverable UI instead of crashing. */
    val recoverable: Boolean = false,
)
