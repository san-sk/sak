package sak.sdk.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Access control policy defining allowed or blocked entry point widgets for a feature.
 */
@Serializable
data class EntryAccess(
    val allowed: Set<String>? = null,
    val blocked: Set<String>? = null,
) {
    fun isAllowed(entryId: String): Boolean {
        if (blocked?.contains(entryId) == true) return false
        if (allowed != null) return allowed.contains(entryId)
        return true
    }
}

/**
 * Access control policy defining allowed or blocked internal navigation routes for a feature.
 */
@Serializable
data class RouteAccess(
    val allowed: Set<String>? = null,
    val blocked: Set<String>? = null,
) {
    fun isAllowed(route: String): Boolean {
        if (blocked?.contains(route) == true) return false
        if (allowed != null) return allowed.contains(route)
        return true
    }
}

/**
 * Entitlement snapshot for a feature module.
 *
 * Defines activation state, business tier plan, capability grant tokens, and custom JSON attributes.
 */
@Serializable
data class FeatureAccess(
    /** If `false`, the feature is deactivated and its entry points are removed. */
    val enabled: Boolean = true,
    /** If `false`, feature remains active in memory but its entry points may be hidden from host slots. */
    val visible: Boolean = true,
    /** Business plan tier identifier (e.g. `"trial"`, `"pro"`, `"complete"`). */
    val plan: String = "complete",
    /** Set of capability grant tokens unlocked by signed license overlays. */
    val grants: Set<String> = emptySet(),
    /** Entry point widget access filters. */
    val entries: EntryAccess? = null,
    /** Internal route navigation filters. */
    val routes: RouteAccess? = null,
    /** Opaque JSON key-value configuration attributes. */
    val attrs: Map<String, JsonElement> = emptyMap(),
) {
    fun hasGrant(grant: String): Boolean = grants.contains(grant)
    fun isEntryAllowed(entryId: String): Boolean = visible && (entries?.isAllowed(entryId) ?: true)
    fun isRouteAllowed(route: String): Boolean = enabled && (routes?.isAllowed(route) ?: true)
}
