package sak.runtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import sak.sdk.api.ApiLookup
import sak.sdk.config.AppConfig
import sak.sdk.config.FeatureToggle
import sak.sdk.config.mergeLicenseOverlay
import sak.sdk.entry.EntryPoint
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureContext
import sak.sdk.feature.FeatureId
import sak.sdk.license.LicenseProvider
import sak.sdk.license.LicenseVerifier
import sak.sdk.license.SignedLicensePayload
import sak.sdk.navigation.Route
import sak.sdk.service.AppServiceLookup

/** Runtime implementation of [FeatureContext] passed to features on activate. */
class FeatureRuntimeContext(
    private val featureId: FeatureId,
    private val accessFlow: StateFlow<sak.sdk.config.FeatureToggle>,
    private val apiLookup: ApiLookup,
    private val serviceLookup: AppServiceLookup,
    private val navigator: (Route) -> Unit,
    private val screenRegistry: sak.sdk.feature.ScreenRegistry,
) : FeatureContext {
    override val access: sak.sdk.config.FeatureToggle
        get() = accessFlow.value

    override fun navigate(route: Route) = navigator(route)

    override val apis: ApiLookup = apiLookup

    override val services: AppServiceLookup = serviceLookup

    override val screens: sak.sdk.feature.ScreenRegistry = screenRegistry
}

/**
 * Activates/deactivates features when ActiveConfig changes.
 * One feature instance per id for the app process lifetime (not per Activity rotation).
 */
class FeatureLifecycle(
    private val apiRegistry: ApiRegistry,
    private val onNavigate: (Route) -> Unit,
) {
    private val active = mutableMapOf<FeatureId, Feature>()
    private val accessFlows = mutableMapOf<FeatureId, MutableStateFlow<sak.sdk.config.FeatureToggle>>()
    private val screenRegistries = mutableMapOf<FeatureId, ScreenRegistryImpl>()

    /** Snapshot of currently activated features. */
    fun activeFeatures(): Map<FeatureId, Feature> = active.toMap()

    /**
     * Reconcile active features with [activeConfig]: activate new, update access, deactivate removed.
     */
    suspend fun apply(
        activeConfig: AppConfig,
        factories: Map<FeatureId, () -> Feature>,
        serviceLookup: AppServiceLookup,
    ) {
        val desired = activeConfig.features.filter { (_, toggle) -> toggle.enabled }.keys
        val desiredIds = desired.map { FeatureId(it) }

        val toDeactivate = active.keys.filter { it !in desiredIds }
        toDeactivate.forEach { id -> deactivate(id) }

        desiredIds.forEach { id ->
            val toggle = activeConfig.features[id.value] ?: return@forEach
            if (id in active) {
                accessFlows[id]?.value = toggle
                active[id]?.onAccessChanged(contextFor(id, serviceLookup))
            } else {
                val factory = factories[id] ?: return@forEach
                activate(id, factory(), toggle, serviceLookup)
            }
        }
    }

    private suspend fun activate(
        id: FeatureId,
        feature: Feature,
        toggle: sak.sdk.config.FeatureToggle,
        serviceLookup: AppServiceLookup,
    ) {
        val accessFlow = MutableStateFlow(toggle)
        accessFlows[id] = accessFlow
        val registry = ScreenRegistryImpl()
        screenRegistries[id] = registry
        active[id] = feature
        feature.onActivated(contextFor(id, serviceLookup, accessFlow, registry))
        apiRegistry.register(id, feature.apis())
    }

    private suspend fun deactivate(id: FeatureId) {
        active[id]?.onDeactivated()
        active.remove(id)
        accessFlows.remove(id)
        screenRegistries.remove(id)
        apiRegistry.unregister(id)
    }

    /** Screen registry for entry preview composables (Compose hosts). */
    fun screenRegistry(featureId: FeatureId): ScreenRegistryImpl? = screenRegistries[featureId]

    private fun contextFor(
        id: FeatureId,
        serviceLookup: AppServiceLookup,
        accessFlow: MutableStateFlow<sak.sdk.config.FeatureToggle> = accessFlows[id]!!,
        registry: ScreenRegistryImpl = screenRegistries[id]!!,
    ): FeatureContext = FeatureRuntimeContext(
        featureId = id,
        accessFlow = accessFlow,
        apiLookup = apiRegistry.lookup(),
        serviceLookup = serviceLookup,
        navigator = onNavigate,
        screenRegistry = registry,
    )
}

/** Per-feature screen preview registry (entry point composables). */
class ScreenRegistryImpl : sak.sdk.feature.ScreenRegistry {
    private val registrations = mutableMapOf<String, sak.sdk.feature.ScreenRegistration>()

    override fun register(previewKey: String, registration: sak.sdk.feature.ScreenRegistration) {
        registrations[previewKey] = registration
    }

    override fun unregister(previewKey: String) {
        registrations.remove(previewKey)
    }

    /** Resolve a preview registration for [sak.compose.ui.SakApp] entry cards. */
    fun get(previewKey: String): sak.sdk.feature.ScreenRegistration? = registrations[previewKey]
}
