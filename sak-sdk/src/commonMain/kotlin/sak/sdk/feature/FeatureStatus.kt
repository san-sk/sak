package sak.sdk.feature

/** How urgently the user should notice this feature (badges, alerts tab, banners). */
enum class Attention {
    /** No badge or highlight. */
    None,
    /** Subtle indicator (dot, muted badge). */
    Low,
    /** Standard attention (badge count, banner). */
    Medium,
    /** Urgent (persistent banner, high-priority alert). */
    High,
}

/** First-run / onboarding state for this feature. */
enum class IntroState {
    /** User must complete onboarding before normal use. */
    Required,
    Done,
    NotApplicable,
}

/**
 * Feature-reported state. Observed by the host shell for badges, setup gates, and readiness.
 *
 * Config `enabled`/`visible` control activation; [setupComplete] is the feature's own init signal.
 */
data class FeatureStatus(
    val enabled: Boolean = true,
    val visible: Boolean = true,
    /** True when internal init finished (SDK connected, cache loaded, permissions granted). */
    val setupComplete: Boolean = true,
    val attention: Attention = Attention.None,
    val introSeen: IntroState = IntroState.Done,
)
