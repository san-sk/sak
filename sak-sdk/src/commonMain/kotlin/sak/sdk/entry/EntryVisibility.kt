package sak.sdk.entry

/** When an entry point should appear relative to feature/config state. */
enum class EntryVisibility {
    /** Show whenever the feature is active. */
    Always,
    /** Show when the feature is enabled in ActiveConfig. */
    WhenEnabled,
    /** Show when enabled and [sak.sdk.config.FeatureToggle.visible] is true. */
    WhenVisible,
}
