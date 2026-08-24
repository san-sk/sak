package sak.runtime

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonElement
import sak.sdk.api.ApiLookup
import sak.sdk.feature.FeatureContext
import sak.sdk.feature.FeatureId
import sak.sdk.navigation.Route
import sak.sdk.service.AppServiceLookup

/**
 * Runtime implementation of [FeatureContext] passed to [sak.sdk.feature.Feature.onCreate].
 *
 * Flows mirror the current [sak.sdk.license.FeatureEntitlement] and update on config refresh
 * without unload when capabilities or settings change.
 */
class FeatureRuntimeContext(
    private val featureId: FeatureId,
    override val capabilities: StateFlow<Set<String>>,
    override val licenseType: StateFlow<String>,
    override val settings: StateFlow<Map<String, JsonElement>>,
    private val apiLookup: ApiLookup,
    private val serviceLookup: AppServiceLookup,
    private val navigator: (Route) -> Unit,
) : FeatureContext {
    override fun navigate(route: Route) = navigator(route)

    override val apis: ApiLookup = apiLookup

    override val services: AppServiceLookup = serviceLookup
}
