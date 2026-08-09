package sak.sdk.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Which entry-point widgets are allowed or blocked for this feature. */
@Serializable
data class EntryAccess(
    val allowed: Set<String>? = null,
    val blocked: Set<String>? = null,
)

/** Optional route-level guard (feature internal routes). */
@Serializable
data class RouteAccess(
    val allowed: Set<String>? = null,
    val blocked: Set<String>? = null,
)

/**
 * Per-feature access in config. Runtime enforces [enabled], [visible], [entries], [routes].
 * [plan], [grants], and [attrs] are opaque — the feature interprets them.
 */
@Serializable
data class FeatureToggle(
    /** When false, feature is deactivated ([Feature.onDeactivated]). */
    val enabled: Boolean = true,
    /** When false, feature stays active but entry points may be hidden. */
    val visible: Boolean = true,
    /** Opaque plan tier (e.g. `trial`, `pro`, `complete`) — feature interprets. */
    val plan: String = "complete",
    /** Capability strings granted by license overlay. */
    val grants: Set<String> = emptySet(),
    val entries: EntryAccess? = null,
    val routes: RouteAccess? = null,
    /** Feature-specific JSON attrs from config. */
    val attrs: Map<String, JsonElement> = emptyMap(),
)
