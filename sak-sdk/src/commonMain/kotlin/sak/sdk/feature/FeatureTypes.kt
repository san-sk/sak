package sak.sdk.feature

import kotlin.jvm.JvmInline

/**
 * Stable, unique identifier for a feature module (e.g. `FeatureId("analytics")`).
 *
 * This `@JvmInline value class` provides strict compile-time type safety to prevent passing
 * arbitrary strings to SAK APIs, while compiling down to a raw String at runtime to ensure
 * zero heap allocation overhead.
 *
 * The string value MUST match the key used in the [sak.sdk.config.SakConfig.features] remote config
 * and the [sak.sdk.license.FeatureEntitlements] license registry.
 */
@JvmInline
value class FeatureId(val value: String)

/**
 * Represents the urgency level of a feature's current state, used by the host application
 * to render indicators like notification badges, red dots, or sticky banners.
 */
enum class Attention {
    /** Standard operation. No badge or highlight required. */
    None,
    /** Subtle indicator (e.g., a muted dot indicating background activity). */
    Low,
    /** Standard attention requirement (e.g., a badge with a number like "3 unread messages"). */
    Medium,
    /** Urgent intervention required (e.g., a persistent red banner for "Payment Failed"). */
    High,
}

/**
 * Represents the lifecycle health and readiness state of a feature module.
 * Host applications observe this to render appropriate UI (like loading spinners or error fallbacks)
 * instead of crashing the app if a plugin fails.
 */
sealed class FeatureState {
    /** 
     * The feature has passed all activation gates and is performing internal initialization 
     * (e.g., executing network calls or opening SQLite databases in `Feature.onCreate`).
     */
    data object Initializing : FeatureState()

    /** The feature is initialized, healthy, and ready for user interaction. */
    data object Ready : FeatureState()

    /** 
     * The feature encountered a terminal error and cannot function normally.
     * @param message A localized, human-readable error description for the Host UI to display.
     */
    data class Error(val message: String) : FeatureState()
}

/**
 * The observable health and readiness status published by a feature module.
 *
 * Observed by the Host shell to drive UI rendering (Entry Points, Badges) and observability dashboards.
 *
 * @param state Current lifecycle state ([FeatureState.Initializing], [FeatureState.Ready], or [FeatureState.Error]).
 * @param attention Visual attention request level ([Attention.None], [Attention.Medium], etc.).
 */
data class FeatureStatus(
    val state: FeatureState = FeatureState.Initializing,
    val attention: Attention = Attention.None,
)
