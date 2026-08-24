package sak.sdk.feature

/**
 * Feature-reported UI state — distinct from entitlement ([FeatureConfig]).
 *
 * Alerts and sorting use [sak.sdk.entry.EntryPoint.priority] and payload, not this type.
 */
data class FeatureStatus(
    /** Technical init finished (SDK wired, cache warm, permissions granted, …). */
    val setupComplete: Boolean = true,
    /** First-run / what's-new state for this feature. */
    val intro: IntroState = IntroState.NotApplicable,
)

/**
 * Intro / what's-new gating for a feature.
 *
 * Distinct from [FeatureStatus.setupComplete] (technical readiness).
 */
enum class IntroState {
    /** No intro flow for this feature. */
    NotApplicable,
    /** User should see intro or what's-new. */
    Required,
    /** User completed or dismissed intro. */
    Completed,
}
