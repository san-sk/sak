package sak.runtime

import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sak.sdk.config.FeatureAccess
import sak.sdk.config.SakConfig
import sak.sdk.config.merge
import sak.sdk.entry.EntryPoint
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureId
import sak.sdk.feature.FeatureStatus
import sak.sdk.license.AllFeaturesLicensed
import sak.sdk.license.FeatureEntitlements
import sak.sdk.navigation.Route

/**
 * The Application-scoped SAK engine: manages feature gating, lifecycle synchronization, 
 * exactly-once navigation routing, and O(1) flow aggregation.
 *
 * The Runtime ensures that a feature only boots up when all three gates agree:
 *  1. **Available** - The lazy factory is present in the [featureRegistry].
 *  2. **Licensed** - The ID is permitted by the [entitlements] functional interface.
 *  3. **Enabled** - The feature is not explicitly set to `false` in the [activeConfig].
 */
class SakRuntime(
    private val baseConfig: SakConfig = SakConfig(),
    private val featureRegistry: FeatureRegistry = FeatureRegistry(),
    private val serviceRegistry: ServiceRegistry = ServiceRegistry(),
    private var entitlements: FeatureEntitlements = AllFeaturesLicensed,
    private val scope: CoroutineScope,
    hostLifecycle: Lifecycle,
) {
    private val _activeConfig = MutableStateFlow(baseConfig)

    /** The accumulated runtime overlay, merged onto [baseConfig] to form the final active config. */
    private var appliedOverlay: SakConfig = SakConfig()

    /** 
     * The master configuration map (Base Defaults + Network Overlay). 
     * Features and Host Apps observe this to detect live configuration toggles.
     */
    val activeConfig: StateFlow<SakConfig> = _activeConfig.asStateFlow()

    private val manager = FeatureManager(serviceRegistry, ::navigate, hostLifecycle, scope)

    private val _allStatuses = MutableStateFlow<Map<FeatureId, FeatureStatus>>(emptyMap())
    /** 
     * Observable map of all enabled features and their health/urgency status.
     * Host apps use this to render observability dashboards or global notification dots.
     */
    val allStatuses: StateFlow<Map<FeatureId, FeatureStatus>> = _allStatuses.asStateFlow()

    private val _allEntryPoints = MutableStateFlow<List<Pair<FeatureId, EntryPoint>>>(emptyList())
    /** 
     * The flattened, priority-sorted stream of Micro-Frontend descriptors from all active features.
     * Host apps (Compose/SwiftUI) collect this flow natively to render the app's Dashboards and Settings.
     */
    val allEntryPoints: StateFlow<List<Pair<FeatureId, EntryPoint>>> = _allEntryPoints.asStateFlow()

    private val aggregationLock = Mutex()
    private val featureJobs = mutableMapOf<FeatureId, Job>()
    private val statusMap = mutableMapOf<FeatureId, FeatureStatus>()
    private val entryPointsMap = mutableMapOf<FeatureId, List<EntryPoint>>()

    init {
        // Observers feature activation to manage targeted O(1) aggregation jobs without array re-allocation overhead.
        scope.launch {
            manager.activeFeatures.collect { activeFeatures ->
                aggregationLock.withLock {
                    val currentIds = activeFeatures.keys
                    val obsoleteIds = featureJobs.keys - currentIds

                    // Cleanup removed features
                    obsoleteIds.forEach { id ->
                        featureJobs.remove(id)?.cancel()
                        statusMap.remove(id)
                        entryPointsMap.remove(id)
                    }

                    // Start observing new features
                    val newIds = currentIds - featureJobs.keys
                    newIds.forEach { id ->
                        val feature = activeFeatures[id]!!
                        val job = scope.launch {
                            launch {
                                feature.status.collect { status ->
                                    aggregationLock.withLock {
                                        statusMap[id] = status
                                        _allStatuses.value = statusMap.toMap()
                                    }
                                }
                            }
                            launch {
                                feature.entryPoints.collect { points ->
                                    aggregationLock.withLock {
                                        entryPointsMap[id] = points
                                        recalculateEntryPoints()
                                    }
                                }
                            }
                        }
                        featureJobs[id] = job
                    }

                    // Force an emission update if features were removed
                    if (obsoleteIds.isNotEmpty()) {
                        _allStatuses.value = statusMap.toMap()
                        recalculateEntryPoints()
                    }
                }
            }
        }
    }

    private fun recalculateEntryPoints() {
        val flatList = entryPointsMap.flatMap { (id, points) ->
            points.map { point -> id to point }
        }.sortedByDescending { it.second.priority }
        _allEntryPoints.value = flatList
    }

    /** Starts the runtime engine, evaluating the 3 Gates and concurrently bootstrapping allowed features. */
    suspend fun start() {
        apply(_activeConfig.value)
    }

    /** 
     * Shuts down the runtime, gracefully destroying all active features, cancelling coroutine scopes, 
     * and tearing down all Flow aggregation channels. 
     */
    suspend fun stop() {
        manager.shutdown()
        aggregationLock.withLock {
            featureJobs.values.forEach { it.cancel() }
            featureJobs.clear()
            statusMap.clear()
            entryPointsMap.clear()
            _allStatuses.value = emptyMap()
            _allEntryPoints.value = emptyList()
        }
    }

    /**
     * Applies a remote configuration overlay (e.g., from Firebase Remote Config) on top of the base config.
     *
     * Overlays accumulate safely. Features that were newly enabled by the payload are concurrently instantiated, 
     * synced to the Host OS lifecycle, and started. Features explicitly disabled are immediately destroyed.
     */
    suspend fun updateConfig(overlay: SakConfig) {
        appliedOverlay = appliedOverlay.merge(overlay)
        _activeConfig.value = baseConfig.merge(appliedOverlay)
        apply(_activeConfig.value)
    }

    /** Convenience helper to dynamically enable or disable a single feature at runtime. */
    suspend fun setFeatureEnabled(featureId: String, enabled: Boolean) {
        val nextFeatures = appliedOverlay.features.toMutableMap()
        nextFeatures[featureId] = FeatureAccess(enabled = enabled)
        updateConfig(SakConfig(version = appliedOverlay.version, features = nextFeatures))
    }

    /**
     * Updates the licensing state (e.g., when the user completes an In-App Purchase).
     *
     * Features that just became licensed are immediately instantiated and started. Features that
     * just lost their license are gracefully destroyed.
     */
    suspend fun updateEntitlements(entitlements: FeatureEntitlements) {
        this.entitlements = entitlements
        apply(_activeConfig.value)
    }

    private suspend fun apply(config: SakConfig) {
        manager.apply(config, featureRegistry.snapshot(), entitlements, serviceRegistry.lookup())
    }

    /** Returns `true` only if [featureId] is available, licensed, and strictly enabled. */
    fun isActive(featureId: FeatureId): Boolean =
        featureRegistry.availableIds().contains(featureId) &&
            entitlements.isLicensed(featureId) &&
            _activeConfig.value.isFeatureEnabled(featureId.value)

    /** 
     * Requests on-demand access to a specific Feature class. 
     * Returns null if the Feature fails any of the 3 Gates.
     */
    suspend fun getFeature(id: FeatureId): Feature? =
        manager.getOrInstantiate(id, featureRegistry.snapshot(), _activeConfig.value, entitlements)

    /** Synchronous check to retrieve an already active feature instance. */
    fun getFeatureOrNull(id: FeatureId): Feature? = manager.activeFeaturesSnapshot()[id]

    private val _navigationChannel = Channel<Route>(Channel.BUFFERED)

    /** 
     * Exactly-once navigation event stream.
     * Features emit route strings here via `FeatureContext.navigate()`. The Host App explicitly collects 
     * from this Hot Channel to execute the transitions natively, ensuring no "double-navigation" crashes occur.
     */
    val navigationEvents: Flow<Route> = _navigationChannel.receiveAsFlow()

    /** Internally requests an exactly-once navigation event on the Host App's Native Router. */
    fun navigate(route: Route) {
        scope.launch {
            _navigationChannel.send(route)
        }
    }
}
