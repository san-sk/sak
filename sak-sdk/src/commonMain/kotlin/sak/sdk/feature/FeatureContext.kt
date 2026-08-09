package sak.sdk.feature

import sak.sdk.api.ApiLookup
import sak.sdk.config.FeatureToggle
import sak.sdk.navigation.Route
import sak.sdk.service.AppServiceLookup

/**
 * What an active feature can do at runtime: read access, navigate, use APIs, register previews.
 * Provided by [sak.runtime.FeatureRuntimeContext] on [Feature.onActivated].
 */
interface FeatureContext {
    /** Current access snapshot (plan, grants, entries) from ActiveConfig. */
    val access: FeatureToggle

    /** Request navigation on the host NavHost (same graph as scaffold + features). */
    fun navigate(route: Route)

    /** Look up APIs registered by other active features. */
    val apis: ApiLookup

    /** Host-provided services (analytics, auth, storage, …). */
    val services: AppServiceLookup

    /** Register entry-point preview composables by [sak.sdk.entry.EntryPoint.previewKey]. */
    val screens: ScreenRegistry
}

/** Maps preview keys to UI registrations (rendered by sak-compose-ui). */
interface ScreenRegistry {
    fun register(previewKey: String, registration: ScreenRegistration)
    fun unregister(previewKey: String)
}

/** UI binding is implemented in sak-compose-ui; SDK holds the contract only. */
interface ScreenRegistration
