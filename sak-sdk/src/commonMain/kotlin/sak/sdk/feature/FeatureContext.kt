package sak.sdk.feature

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonElement
import sak.sdk.api.ApiLookup
import sak.sdk.navigation.Route
import sak.sdk.service.AppServiceLookup

/**
 * Live runtime surface for a loaded [Feature] instance.
 *
 * Flows update when entitlement is refreshed without unload/reload.
 * Compare [sak.sdk.license.FeatureEntitlement]: snapshot at [Feature.onCreate] only.
 */
interface FeatureContext {
    /** Current capability strings for this feature. */
    val capabilities: StateFlow<Set<String>>

    /** [sak.sdk.license.LicenseInfo.type] — display label only, not for gating. */
    val licenseType: StateFlow<String>

    /** [FeatureConfig.settings] — opaque to runtime. */
    val settings: StateFlow<Map<String, JsonElement>>

    /** Request navigation in the host NavHost. */
    fun navigate(route: Route)

    /** Look up APIs registered by other loaded features. */
    val apis: ApiLookup

    /** Host-injected services (analytics, logging, etc.). */
    val services: AppServiceLookup
}
