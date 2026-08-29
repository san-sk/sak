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
 * Lifecycle states reported by the feature to the host.
 */
sealed class FeatureState {
    /** Feature is performing internal initialization (onCreate). */
    object Initializing : FeatureState()
    /** Feature is ready for user interaction and has published EntryPoints. */
    object Ready : FeatureState()
    /** Feature encountered a terminal error and cannot function. */
    data class Error(val message: String) : FeatureState()
}

/**
 * Feature-reported status. Observed by the host shell for readiness and health.
 */
data class FeatureStatus(
    val state: FeatureState = FeatureState.Initializing,
    val attention: Attention = Attention.None,
    val introSeen: IntroState = IntroState.Done,
)
