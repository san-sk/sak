package sak.sdk.feature

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.decodeFromJsonElement
import sak.sdk.api.ApiLookup
import sak.sdk.config.FeatureToggle
import sak.sdk.config.SakJson
import sak.sdk.navigation.Route
import sak.sdk.service.AppServiceLookup

/**
 * What an active feature can do at runtime: read access, navigate, use APIs.
 * Provided by the runtime on [Feature.onCreate] and [Feature.onActivated].
 */
interface FeatureContext {
    /** Current access snapshot (plan, grants, entries) from ActiveConfig. */
    val access: FeatureToggle

    /** Request navigation on the host NavHost. */
    fun navigate(route: Route)

    /** Look up APIs registered by other active features. */
    val apis: ApiLookup

    /** Host-provided services (analytics, auth, storage, …). */
    val services: AppServiceLookup

    /**
     * Managed scope for this feature.
     * Cancelled automatically when the feature is destroyed or deactivated.
     */
    val coroutineScope: CoroutineScope
}

/**
 * Get a typed attribute from the feature's configuration.
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
 * Get a typed attribute or throw if missing or malformed.
 */
inline fun <reified T> FeatureContext.requireAttr(key: String): T {
    val element = access.attrs[key] ?: error("Attribute '$key' missing for feature")
    return SakJson.decodeFromJsonElement<T>(element)
}
