package sak.runtime

import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import sak.sdk.config.FeatureAccess
import sak.sdk.feature.FeatureContext
import sak.sdk.feature.FeatureId
import sak.sdk.navigation.Route
import sak.sdk.service.ServiceLookup

/**
 * Runtime implementation of [FeatureContext] passed to active feature modules.
 *
 * Provides real-time entitlement access state, capability grant checks, cross-feature
 * service lookups, managed coroutine scope, platform lifecycle, and navigation dispatch.
 */
class FeatureRuntimeContext(
    private val accessFlow: StateFlow<FeatureAccess>,
    private val serviceLookup: ServiceLookup,
    private val navigator: (Route) -> Unit,
    override val coroutineScope: CoroutineScope,
    override val lifecycle: Lifecycle,
) : FeatureContext {
    override val access: FeatureAccess
        get() = accessFlow.value

    override fun hasGrant(grant: String): Boolean = access.grants.contains(grant)

    override fun observeGrant(grant: String): Flow<Boolean> = accessFlow.map { it.grants.contains(grant) }

    override fun navigate(route: Route) = navigator(route)

    override val services: ServiceLookup = serviceLookup
}
