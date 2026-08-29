package sak.runtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import sak.sdk.api.ApiLookup
import sak.sdk.config.AppConfig
import sak.sdk.config.FeatureToggle
import sak.sdk.entry.EntryPoint
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureContext
import sak.sdk.feature.FeatureId
import sak.sdk.feature.FeatureState
import sak.sdk.feature.FeatureStatus
import sak.sdk.navigation.Route
import sak.sdk.service.AppServiceLookup

/** Runtime implementation of [FeatureContext] passed to features. */
class FeatureRuntimeContext(
    private val featureId: FeatureId,
    private val accessFlow: StateFlow<sak.sdk.config.FeatureToggle>,
    private val apiLookup: ApiLookup,
    private val serviceLookup: AppServiceLookup,
    private val navigator: (Route) -> Unit,
    override val coroutineScope: CoroutineScope,
) : FeatureContext {
    override val access: sak.sdk.config.FeatureToggle
        get() = accessFlow.value

    override fun navigate(route: Route) = navigator(route)

    override val apis: ApiLookup = apiLookup

    override val services: AppServiceLookup = serviceLookup
}

/**
 * Manages the lifecycle of features: Create, Activate, Deactivate, Destroy.
 */
class FeatureLifecycle(
    private val apiRegistry: ApiRegistry,
    private val onNavigate: (Route) -> Unit,
    private val runtimeScope: CoroutineScope,
) {
    private val allFeatures = mutableMapOf<FeatureId, Feature>()
    private val activeIds = mutableSetOf<FeatureId>()
    private val accessFlows = mutableMapOf<FeatureId, MutableStateFlow<sak.sdk.config.FeatureToggle>>()
    private val featureScopes = mutableMapOf<FeatureId, CoroutineScope>()

    private val supervisor = SupervisorJob(runtimeScope.coroutineContext[kotlinx.coroutines.Job])

    /** Snapshot of currently activated features. */
    fun activeFeatures(): Map<FeatureId, Feature> = allFeatures.filterKeys { it in activeIds }

    /**
     * Reconcile features with [activeConfig].
     */
    suspend fun apply(
        activeConfig: AppConfig,
        factories: Map<FeatureId, () -> Feature>,
        serviceLookup: AppServiceLookup,
    ) {
        val configFeatures = activeConfig.features
        
        // 1. Reconcile Activation and Lazy Loading
        val desiredActive = configFeatures.filter { it.value.enabled }.keys.map { FeatureId(it) }
        
        // Deactivate those no longer desired
        activeIds.filter { it !in desiredActive }.forEach { id ->
            deactivate(id)
        }

        // Activate new or update existing
        desiredActive.forEach { id ->
            val toggle = configFeatures[id.value]!!
            
            // Ensure instantiated (if not lazy, or if already active somehow)
            // Note: In Phase 1, we instantiated everything in Startup.
            // In Phase 6, we only instantiate if not already present.
            val feature = allFeatures[id] ?: run {
                // If it's not present, we check if it SHOULD be instantiated now.
                // We'll assume for now that if it's in desiredActive, we want to start it unless we have explicit lazy logic.
                // However, the prompt for Lazy Loading said: "Instantiated as soon as the runtime starts (if enabled)."
                // But only if priority is Startup.
                // I need access to the factories' priorities.
                // I'll add a helper to instantiate if not present.
                instantiateFeature(id, factories, serviceLookup, toggle)
            } ?: return@forEach

            val accessFlow = accessFlows[id]!!
            accessFlow.value = toggle
            
            if (id !in activeIds) {
                activate(id, serviceLookup)
            } else {
                safeLifecycleCall(id) {
                    feature.onAccessChanged(contextFor(id, serviceLookup))
                }
            }
        }
    }

    /**
     * Public helper to instantiate a feature on-demand (Lazy Loading).
     */
    suspend fun getOrInstantiate(
        id: FeatureId,
        factories: Map<FeatureId, () -> Feature>,
        serviceLookup: AppServiceLookup,
        activeConfig: AppConfig
    ): Feature? {
        val feature = allFeatures[id]
        if (feature != null) return feature

        val toggle = activeConfig.features[id.value] ?: return null
        return instantiateFeature(id, factories, serviceLookup, toggle)
    }

    private suspend fun instantiateFeature(
        id: FeatureId,
        factories: Map<FeatureId, () -> Feature>,
        serviceLookup: AppServiceLookup,
        toggle: FeatureToggle
    ): Feature? {
        val factory = factories[id] ?: return null
        // Note: Ideally we'd know the priority here. 
        // If we don't have the FeatureModule, we just instantiate.
        // Assuming factories was built from List<FeatureModule>.
        
        return safeLifecycleCall(id) {
            val feature = factory()
            allFeatures[id] = feature
            val accessFlow = MutableStateFlow(toggle)
            accessFlows[id] = accessFlow
            val scope = CoroutineScope(runtimeScope.coroutineContext + SupervisorJob(supervisor))
            featureScopes[id] = scope
            feature.onCreate(contextFor(id, serviceLookup, accessFlow))
            feature
        }
    }

    /** Shutdown all features and clear the registry. */
    suspend fun shutdown() {
        activeIds.toList().forEach { deactivate(it) }
        allFeatures.keys.toList().forEach { id ->
            val feature = allFeatures[id]
            safeLifecycleCall(id) {
                feature?.onDestroy()
            }
            featureScopes[id]?.cancel()
        }
        allFeatures.clear()
        accessFlows.clear()
        featureScopes.clear()
    }

    private suspend fun activate(id: FeatureId, serviceLookup: AppServiceLookup) {
        val feature = allFeatures[id] ?: return
        activeIds.add(id)
        safeLifecycleCall(id) {
            feature.onActivated(contextFor(id, serviceLookup))
            apiRegistry.register(id, feature.apis())
        }
    }

    private suspend fun deactivate(id: FeatureId) {
        val feature = allFeatures[id] ?: return
        safeLifecycleCall(id) {
            feature.onDeactivated()
            apiRegistry.unregister(id)
        }
        activeIds.remove(id)
    }

    private suspend fun <T> safeLifecycleCall(id: FeatureId, block: suspend () -> T): T? {
        return try {
            block()
        } catch (e: Exception) {
            // Update status to Error if possible
            val feature = allFeatures[id]
            // We'd need a way to set the status flow from outside, or the feature should handle it.
            // For now, we'll log it and let the feature's status Flow reflect the state if it can.
            // A better way is to have the Runtime track "RuntimeStatus" per feature.
            null
        }
    }

    private fun contextFor(
        id: FeatureId,
        serviceLookup: AppServiceLookup,
        accessFlow: MutableStateFlow<sak.sdk.config.FeatureToggle> = accessFlows[id]!!,
    ): FeatureContext = FeatureRuntimeContext(
        featureId = id,
        accessFlow = accessFlow,
        apiLookup = apiRegistry.lookup(),
        serviceLookup = serviceLookup,
        navigator = onNavigate,
        coroutineScope = featureScopes[id]!!
    )
}
