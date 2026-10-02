package sak.runtime

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sak.sdk.config.FeatureAccess
import sak.sdk.config.SakConfig
import sak.sdk.feature.BaseFeature
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureContext
import sak.sdk.feature.FeatureId
import sak.sdk.feature.FeatureState
import sak.sdk.navigation.Route
import sak.sdk.service.ServiceLookup

/**
 * Manages feature lifecycles and syncs them with host platform lifecycle.
 */
class FeatureLifecycle(
    private val serviceRegistry: ServiceRegistry,
    private val onNavigate: (Route) -> Unit,
    private val hostLifecycle: Lifecycle,
) {
    private val lock = Mutex()
    private val allFeatures = mutableMapOf<FeatureId, Feature>()
    private val enabledIds = mutableSetOf<FeatureId>()
    private val accessFlows = mutableMapOf<FeatureId, MutableStateFlow<FeatureAccess>>()
    private val featureLifecycles = mutableMapOf<FeatureId, LifecycleRegistry>()

    init {
        hostLifecycle.addObserver(LifecycleEventObserver { _, event ->
            syncAllToHost(event)
        })
    }

    private fun syncAllToHost(event: Lifecycle.Event) {
        val activeRegistries = featureLifecycles.filterKeys { it in enabledIds }.values.toList()
        activeRegistries.forEach { registry ->
            registry.handleLifecycleEvent(event)
        }
    }

    /** Snapshot of currently active features. */
    fun activeFeatures(): Map<FeatureId, Feature> =
        allFeatures.filterKeys { it in enabledIds }

    /** Reconcile features with [activeConfig]. */
    suspend fun apply(
        activeConfig: SakConfig,
        factories: Map<FeatureId, () -> Feature>,
        serviceLookup: ServiceLookup,
    ) {
        lock.withLock {
            val configFeatures = activeConfig.features
            val desiredEnabled = configFeatures.filter { it.value.enabled }.keys.map { FeatureId(it) }

            // Deactivate features no longer enabled
            enabledIds.filter { it !in desiredEnabled }.forEach { id ->
                deactivate(id)
            }

            // Reconcile features
            desiredEnabled.forEach { id ->
                val access = configFeatures[id.value]!!
                val feature = allFeatures[id] ?: instantiateFeatureInternal(id, factories, access) ?: return@forEach

                val accessFlow = accessFlows[id]!!
                val oldAccess = accessFlow.value
                accessFlow.value = access

                if (id !in enabledIds) {
                    activateInternal(id, serviceLookup)
                } else if (oldAccess != access) {
                    safeLifecycleCall(id) {
                        feature.onConfigChanged(access)
                    }
                }
            }
        }
    }

    /** On-demand feature instantiation (Lazy Loading). */
    suspend fun getOrInstantiate(
        id: FeatureId,
        factories: Map<FeatureId, () -> Feature>,
        activeConfig: SakConfig
    ): Feature? = lock.withLock {
        val feature = allFeatures[id]
        if (feature != null) return feature

        val access = activeConfig.features[id.value] ?: return null
        return instantiateFeatureInternal(id, factories, access)
    }

    private suspend fun instantiateFeatureInternal(
        id: FeatureId,
        factories: Map<FeatureId, () -> Feature>,
        access: FeatureAccess
    ): Feature? {
        val factory = factories[id] ?: return null

        return safeLifecycleCall(id) {
            val feature = factory()
            allFeatures[id] = feature
            val accessFlow = MutableStateFlow(access)
            accessFlows[id] = accessFlow

            val registry = LifecycleRegistry.createUnsafe(feature)
            featureLifecycles[id] = registry
            feature
        }
    }

    /** Shutdown all features. */
    suspend fun shutdown() {
        lock.withLock {
            enabledIds.toList().forEach { deactivate(it) }
            allFeatures.keys.toList().forEach { id ->
                val feature = allFeatures[id]
                safeLifecycleCall(id) {
                    feature?.onDestroy()
                }
                featureLifecycles[id]?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            }
            allFeatures.clear()
            accessFlows.clear()
            featureLifecycles.clear()
            enabledIds.clear()
        }
    }

    private suspend fun activateInternal(id: FeatureId, serviceLookup: ServiceLookup) {
        val feature = allFeatures[id] ?: return
        enabledIds.add(id)

        val registry = featureLifecycles[id]!!
        val targetState = hostLifecycle.currentState

        if (registry.currentState == Lifecycle.State.INITIALIZED) {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }
        if (targetState.isAtLeast(Lifecycle.State.STARTED) && registry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }
        if (targetState.isAtLeast(Lifecycle.State.RESUMED) && registry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        safeLifecycleCall(id) {
            feature.onCreate(contextFor(id, serviceLookup))
            serviceRegistry.registerFeatureServices(id, feature.services())
        }
    }

    private suspend fun deactivate(id: FeatureId) {
        val feature = allFeatures[id] ?: return

        featureLifecycles[id]?.handleLifecycleEvent(Lifecycle.Event.ON_STOP)

        safeLifecycleCall(id) {
            feature.onDestroy()
            serviceRegistry.unregisterFeatureServices(id)
        }
        enabledIds.remove(id)
    }

    private suspend fun <T> safeLifecycleCall(id: FeatureId, block: suspend () -> T): T? {
        return try {
            block()
        } catch (e: Exception) {
            println("Feature $id error: ${e.message}")
            null
        }
    }

    private fun contextFor(
        id: FeatureId,
        serviceLookup: ServiceLookup,
        accessFlow: MutableStateFlow<FeatureAccess> = accessFlows[id]!!,
    ): FeatureContext {
        val lifecycle = featureLifecycles[id]!!
        return FeatureRuntimeContext(
            accessFlow = accessFlow,
            serviceLookup = serviceLookup,
            navigator = onNavigate,
            coroutineScope = lifecycle.coroutineScope,
            lifecycle = lifecycle
        )
    }
}
