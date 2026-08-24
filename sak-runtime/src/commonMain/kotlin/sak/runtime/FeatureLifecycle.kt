package sak.runtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonElement
import sak.sdk.config.AppConfig
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureContext
import sak.sdk.feature.FeatureId
import sak.sdk.feature.FeatureState
import sak.sdk.license.toEntitlement
import sak.sdk.navigation.Route
import sak.sdk.navigation.RouteGuard
import sak.sdk.service.AppServiceLookup

/**
 * Loads and unloads [Feature] instances when [AppConfig] load policy changes.
 *
 * One instance per feature id for the app process (survives Activity rotation).
 */
class FeatureLifecycle(
    private val apiRegistry: ApiRegistry,
    private val onNavigate: (Route) -> Unit,
) {
    private val active = mutableMapOf<FeatureId, Feature>()
    private val capabilityFlows = mutableMapOf<FeatureId, MutableStateFlow<Set<String>>>()
    private val licenseTypeFlows = mutableMapOf<FeatureId, MutableStateFlow<String>>()
    private val settingsFlows = mutableMapOf<FeatureId, MutableStateFlow<Map<String, JsonElement>>>()

    fun activeFeatures(): Map<FeatureId, Feature> = active.toMap()

    fun activeFeatureIds(): Set<FeatureId> = active.keys.toSet()

    fun capabilities(featureId: FeatureId): Set<String> =
        capabilityFlows[featureId]?.value ?: emptySet()

    fun routeGuards(featureId: FeatureId): List<RouteGuard> =
        active[featureId]?.routeGuards() ?: emptyList()

    /**
     * Reconcile loaded features with [activeConfig]:
     * [FeatureState.Enabled] → [Feature.onCreate] or [Feature.onCapabilitiesChanged];
     * [FeatureState.Disabled] / [FeatureState.Hidden] → [Feature.onDestroy].
     */
    suspend fun apply(
        activeConfig: AppConfig,
        factories: Map<FeatureId, () -> Feature>,
        serviceLookup: AppServiceLookup,
    ) {
        val licenseType = activeConfig.license.type
        val desiredIds = activeConfig.features
            .filter { (_, row) -> row.state.shouldLoad() }
            .keys
            .map { FeatureId(it) }

        val toUnload = active.keys.filter { it !in desiredIds }
        toUnload.forEach { unload(it) }

        desiredIds.forEach { id ->
            val row = activeConfig.features[id.value] ?: return@forEach
            if (id in active) {
                updateEntitlement(id, row, licenseType, serviceLookup)
            } else {
                val factory = factories[id] ?: return@forEach
                load(id, factory(), row, licenseType, serviceLookup)
            }
        }
    }

    private suspend fun load(
        id: FeatureId,
        feature: Feature,
        row: sak.sdk.feature.FeatureConfig,
        licenseType: String,
        serviceLookup: AppServiceLookup,
    ) {
        val entitlement = row.toEntitlement(licenseType)
        val capsFlow = MutableStateFlow(entitlement.capabilities)
        val licenseFlow = MutableStateFlow(entitlement.licenseType)
        val settingsFlow = MutableStateFlow(entitlement.settings)
        capabilityFlows[id] = capsFlow
        licenseTypeFlows[id] = licenseFlow
        settingsFlows[id] = settingsFlow
        active[id] = feature
        feature.onCreate(contextFor(id, capsFlow, licenseFlow, settingsFlow, serviceLookup))
        apiRegistry.register(id, feature.apis())
    }

    private suspend fun updateEntitlement(
        id: FeatureId,
        row: sak.sdk.feature.FeatureConfig,
        licenseType: String,
        serviceLookup: AppServiceLookup,
    ) {
        val entitlement = row.toEntitlement(licenseType)
        val capsFlow = capabilityFlows[id]!!
        val licenseFlow = licenseTypeFlows[id]!!
        val settingsFlow = settingsFlows[id]!!
        val capsChanged = capsFlow.value != entitlement.capabilities
        val settingsChanged = settingsFlow.value != entitlement.settings
        capsFlow.value = entitlement.capabilities
        licenseFlow.value = entitlement.licenseType
        settingsFlow.value = entitlement.settings
        if (capsChanged || settingsChanged) {
            active[id]?.onCapabilitiesChanged(
                contextFor(id, capsFlow, licenseFlow, settingsFlow, serviceLookup),
            )
        }
    }

    private suspend fun unload(id: FeatureId) {
        active[id]?.onDestroy()
        active.remove(id)
        capabilityFlows.remove(id)
        licenseTypeFlows.remove(id)
        settingsFlows.remove(id)
        apiRegistry.unregister(id)
    }

    private fun contextFor(
        id: FeatureId,
        capabilities: StateFlow<Set<String>>,
        licenseType: StateFlow<String>,
        settings: StateFlow<Map<String, JsonElement>>,
        serviceLookup: AppServiceLookup,
    ): FeatureContext = FeatureRuntimeContext(
        featureId = id,
        capabilities = capabilities,
        licenseType = licenseType,
        settings = settings,
        apiLookup = apiRegistry.lookup(),
        serviceLookup = serviceLookup,
        navigator = onNavigate,
    )
}
