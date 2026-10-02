package sak.sdk.feature

import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.decodeFromJsonElement
import sak.sdk.config.FeatureAccess
import sak.sdk.config.SakJson
import sak.sdk.navigation.Route
import sak.sdk.service.ServiceLookup

/**
 * Runtime environment handle provided to an active feature module during initialization.
 *
 * Grants access to feature entitlements, cross-feature service lookups, navigation triggers,
 * managed coroutine scopes, and platform lifecycle binding.
 */
interface FeatureContext {
    /**
     * Current feature access entitlement snapshot from [sak.sdk.config.SakConfig].
     */
    val access: FeatureAccess

    /**
     * Checks if the specified capability grant string is present in current [access].
     *
     * @param grant Capability token string (e.g. `"analytics.reports"`).
     * @return `true` if granted, `false` otherwise.
     */
    fun hasGrant(grant: String): Boolean

    /**
     * Observes real-time grant transitions when license overlays are applied.
     *
     * @param grant Capability token string to observe.
     * @return [Flow] emitting `true`/`false` whenever grant status transitions.
     */
    fun observeGrant(grant: String): Flow<Boolean>

    /**
     * Emits a navigation request signal to the host application shell.
     *
     * @param route Target destination route (e.g. `Route("analytics/today")`).
     */
    fun navigate(route: Route)

    /**
     * Container for looking up host-provided services and feature-exported APIs.
     */
    val services: ServiceLookup

    /**
     * Managed coroutine scope bound to this feature's lifecycle.
     * Automatically cancelled when the feature is deactivated or destroyed.
     */
    val coroutineScope: CoroutineScope

    /**
     * Platform [Lifecycle] reference synced with host container (Activity or SwiftUI scene phase).
     */
    val lifecycle: Lifecycle
}

/**
 * Parses an opaque JSON attribute from feature configuration into a typed object.
 *
 * @param key Attribute key in [FeatureAccess.attrs].
 * @return Parsed object of type [T], or `null` if key is missing or malformed.
 */
inline fun <reified T> FeatureContext.typedAttr(key: String): T? {
    val element = access.attrs[key] ?: return null
    return try {
        SakJson.decodeFromJsonElement<T>(element)
    } catch (e: Exception) {
        null
    }
}

/**
 * Parses an opaque JSON attribute from feature configuration or throws an error if missing.
 *
 * @param key Attribute key in [FeatureAccess.attrs].
 * @return Parsed object of type [T].
 * @throws IllegalStateException if attribute is missing.
 */
inline fun <reified T> FeatureContext.requireAttr(key: String): T {
    val element = access.attrs[key] ?: error("Attribute '$key' missing for feature")
    return SakJson.decodeFromJsonElement<T>(element)
}
