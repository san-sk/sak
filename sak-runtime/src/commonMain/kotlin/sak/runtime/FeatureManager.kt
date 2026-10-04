package sak.runtime

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sak.sdk.config.FeatureAccess
import sak.sdk.config.SakConfig
import sak.sdk.feature.BaseFeature
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureContext
import sak.sdk.feature.FeatureId
import sak.sdk.license.FeatureEntitlements
import sak.sdk.navigation.Route
import sak.sdk.service.ServiceLookup

/**
 * The internal Engine Orchestrator responsible for calculating activation diffs, dispatching
 * concurrent bootstraps, and synchronizing Host OS lifecycle transitions to individual Feature modules.
 */
internal class FeatureManager(
    private val serviceRegistry: ServiceRegistry,
    private val onNavigate: (Route) -> Unit,
    private val hostLifecycle: Lifecycle,
    private val scope: CoroutineScope,
) {
    private val lock = Mutex()
    private val allFeatures = mutableMapOf<FeatureId, Feature>()
    private val enabledIds = mutableSetOf<FeatureId>()
    private val accessFlows = mutableMapOf<FeatureId, MutableStateFlow<FeatureAccess>>()

    private val _activeFeatures = MutableStateFlow<Map<FeatureId, Feature>>(emptyMap())

    /** Observable map of currently active (enabled and instantiated) features. */
    val activeFeatures: StateFlow<Map<FeatureId, Feature>> = _activeFeatures.asStateFlow()

    private fun publishActiveFeatures() {
        _activeFeatures.value = allFeatures.filterKeys { it in enabledIds }
    }

    init {
        hostLifecycle.addObserver(LifecycleEventObserver { _, event ->
            syncAllToHost(event)
        })
    }

    private fun syncAllToHost(event: Lifecycle.Event) {
        val activeFeaturesList = allFeatures.filterKeys { it in enabledIds }.values.toList()
        activeFeaturesList.forEach { feature ->
            if (feature is BaseFeature) {
                feature.registry.handleLifecycleEvent(event)
            }
        }
    }

    /** Snapshot of currently active features. */
    fun activeFeaturesSnapshot(): Map<FeatureId, Feature> = _activeFeatures.value

    /**
     * Reconciles active features against the 3 runtime gates (Available, Licensed, Enabled).
     * Calculates the "Diff" under a highly optimized fast-lock, then executes heavy side-effects
     * (`onCreate` or `onDestroy`) concurrently via `scope.launch` to prevent Thread/Mutex starvation.
     */
    suspend fun apply(
        activeConfig: SakConfig,
        factories: Map<FeatureId, () -> Feature>,
        entitlements: FeatureEntitlements,
        serviceLookup: ServiceLookup,
    ) {
        val featuresToDeactivate = mutableListOf<FeatureId>()
        val featuresToActivate = mutableListOf<Feature>()
        val accessUpdates = mutableMapOf<FeatureId, FeatureAccess>()

        lock.withLock {
            val desiredEnabled = factories.keys
                .filter { id -> activeConfig.isFeatureEnabled(id.value) && entitlements.isLicensed(id) }
                .toSet()

            // Identify features to deactivate
            featuresToDeactivate.addAll(enabledIds.filter { it !in desiredEnabled })

            // Identify newly enabled features to instantiate and start
            desiredEnabled.filter { it !in enabledIds }.forEach { id ->
                val feature = allFeatures[id] ?: instantiateFeatureInternal(
                    id,
                    factories,
                    activeConfig.features[id.value] ?: FeatureAccess()
                ) ?: return@forEach

                enabledIds.add(id)
                featuresToActivate.add(feature)
            }

            // Capture config updates for all enabled features
            enabledIds.forEach { id ->
                val newAccess = activeConfig.features[id.value] ?: FeatureAccess()
                accessFlows[id]?.value = newAccess
                accessUpdates[id] = newAccess
            }

            publishActiveFeatures()
        }

        // Execute teardown concurrently outside the lock
        featuresToDeactivate.forEach { id ->
            scope.launch {
                deactivate(id)
            }
        }

        // Execute startup concurrently outside the lock
        featuresToActivate.forEach { feature ->
            scope.launch {
                activateInternal(feature.id, serviceLookup)
                safeLifecycleCall(feature.id) {
                    feature.onConfigChanged(accessUpdates[feature.id] ?: FeatureAccess())
                }
            }
        }
        
        // Push config updates to already active features
        val alreadyActive = accessUpdates.keys - featuresToActivate.map { it.id }.toSet()
        alreadyActive.forEach { id ->
            scope.launch {
                safeLifecycleCall(id) {
                    allFeatures[id]?.onConfigChanged(accessUpdates[id] ?: FeatureAccess())
                }
            }
        }
    }

    /** On-demand access to a feature that passes every gate. */
    suspend fun getOrInstantiate(
        id: FeatureId,
        factories: Map<FeatureId, () -> Feature>,
        activeConfig: SakConfig,
        entitlements: FeatureEntitlements,
    ): Feature? = lock.withLock {
        if (!activeConfig.isFeatureEnabled(id.value)) return null
        if (!entitlements.isLicensed(id)) return null
        allFeatures[id] ?: instantiateFeatureInternal(
            id,
            factories,
            activeConfig.features[id.value] ?: FeatureAccess(),
        )
    }

    private fun instantiateFeatureInternal(
        id: FeatureId,
        factories: Map<FeatureId, () -> Feature>,
        access: FeatureAccess
    ): Feature? {
        val factory = factories[id] ?: return null
        
        return try {
            val feature = factory()
            allFeatures[id] = feature
            accessFlows[id] = MutableStateFlow(access)
            feature
        } catch (e: Exception) {
            println("Feature ${id.value} instantiation error: ${e.message}")
            null
        }
    }

    /** Shutdown all features and release resources. */
    suspend fun shutdown() {
        val activeIds = lock.withLock { enabledIds.toList() }
        
        // Deactivate outside lock
        activeIds.forEach { deactivate(it) }

        lock.withLock {
            allFeatures.keys.toList().forEach { id ->
                val feature = allFeatures[id]
                scope.launch {
                    safeLifecycleCall(id) {
                        feature?.onDestroy()
                    }
                }
                if (feature is BaseFeature) {
                    feature.registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                }
            }
            allFeatures.clear()
            accessFlows.clear()
            enabledIds.clear()
            publishActiveFeatures()
        }
    }

    private suspend fun activateInternal(id: FeatureId, serviceLookup: ServiceLookup) {
        val feature = lock.withLock {
            val f = allFeatures[id] ?: return
            f
        }

        val targetState = hostLifecycle.currentState

        // onCreate must run first: context and lifecycle handles are provided to the feature here.
        safeLifecycleCall(id) {
            feature.onCreate(contextFor(id, serviceLookup))
            lock.withLock {
                serviceRegistry.registerFeatureServices(id, feature.services())
            }
        }

        // Advance feature lifecycle to match the host container state.
        if (feature is BaseFeature) {
            val registry = feature.registry
            if (registry.currentState == Lifecycle.State.INITIALIZED) {
                registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            }
            if (targetState.isAtLeast(Lifecycle.State.STARTED) && registry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            }
            if (targetState.isAtLeast(Lifecycle.State.RESUMED) && registry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            }
        }
    }

    private suspend fun deactivate(id: FeatureId) {
        val feature = lock.withLock {
            val f = allFeatures[id] ?: return
            f
        }

        // Wind down in standard Android lifecycle order: pause -> stop -> destroy.
        if (feature is BaseFeature) {
            val registry = feature.registry
            if (registry.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            }
            if (registry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            }
        }

        safeLifecycleCall(id) {
            feature.onDestroy()
            lock.withLock {
                serviceRegistry.unregisterFeatureServices(id)
            }
        }

        if (feature is BaseFeature) {
            feature.registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }

        lock.withLock {
            enabledIds.remove(id)
            allFeatures.remove(id)
            accessFlows.remove(id)
            publishActiveFeatures()
        }
    }

    private suspend fun <T> safeLifecycleCall(id: FeatureId, block: suspend () -> T): T? {
        return try {
            block()
        } catch (e: Exception) {
            println("Feature ${id.value} error: ${e.message}")
            null
        }
    }

    private fun contextFor(
        id: FeatureId,
        serviceLookup: ServiceLookup,
    ): FeatureContext {
        val accessFlow = accessFlows[id]!!
        val feature = allFeatures[id]!!
        val lifecycle = if (feature is BaseFeature) feature.registry else feature.lifecycle
        
        return FeatureRuntimeContext(
            accessFlow = accessFlow,
            services = serviceLookup,
            navigator = onNavigate,
            coroutineScope = lifecycle.coroutineScope,
            lifecycle = lifecycle
        )
    }
}

/**
 * Concrete [FeatureContext] implementation passed to active features during [Feature.onCreate].
 */
internal class FeatureRuntimeContext(
    private val accessFlow: StateFlow<FeatureAccess>,
    override val services: ServiceLookup,
    private val navigator: (Route) -> Unit,
    override val coroutineScope: CoroutineScope,
    override val lifecycle: Lifecycle,
) : FeatureContext {
    override val access: FeatureAccess
        get() = accessFlow.value

    override fun navigate(route: Route) = navigator(route)
}
