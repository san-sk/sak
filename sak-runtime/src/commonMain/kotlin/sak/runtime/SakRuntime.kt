package sak.runtime

import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sak.sdk.config.SakConfig
import sak.sdk.config.SakJson
import sak.sdk.config.mergeLicenseOverlay
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureId
import sak.sdk.feature.FeatureStatus
import sak.sdk.license.LicenseProvider
import sak.sdk.license.LicenseVerifier
import sak.sdk.license.SignedLicensePayload
import sak.sdk.navigation.Route

/**
 * Application-scoped SAK engine: config merge, feature lifecycle, navigation.
 */
class SakRuntime(
    private val defaultConfig: SakConfig = SakConfig(),
    private val featureRegistry: FeatureRegistry = FeatureRegistry(),
    private val serviceRegistry: ServiceRegistry = ServiceRegistry(),
    private val licenseVerifier: LicenseVerifier = AcceptAllLicenseVerifier,
    private val scope: CoroutineScope,
    hostLifecycle: Lifecycle,
) {
    private val _activeConfig = MutableStateFlow(defaultConfig)

    /** DefaultConfig merged with current license overlay. */
    val activeConfig: StateFlow<SakConfig> = _activeConfig.asStateFlow()

    private val lifecycle = FeatureLifecycle(serviceRegistry, ::navigate, hostLifecycle)
    private var licenseJob: Job? = null

    /**
     * Start the runtime and subscribe to license updates.
     */
    suspend fun start(licenseProvider: LicenseProvider? = null) {
        applyLicense(licenseProvider?.resolveLicense())
        licenseProvider?.let { provider ->
            licenseJob?.cancel()
            licenseJob = scope.launch {
                provider.updates.collectLatest { payload ->
                    if (licenseVerifier.verify(payload)) {
                        mergeAndApply(payload.config)
                    }
                }
            }
        }
    }

    /** Shutdown the runtime and all features. */
    suspend fun stop() {
        licenseJob?.cancel()
        licenseJob = null
        lifecycle.shutdown()
    }

    /** Access active feature by ID. Triggers lazy load if needed. */
    suspend fun getFeature(id: FeatureId): Feature? =
        lifecycle.getOrInstantiate(
            id,
            featureRegistry.factories(),
            activeConfig.value
        )

    /** Synchronous check for an active feature instance. */
    fun getFeatureOrNull(id: FeatureId): Feature? = lifecycle.activeFeatures()[id]

    /** Observable map of all features and their health status. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val allStatuses: StateFlow<Map<FeatureId, FeatureStatus>> =
        activeConfig.flatMapLatest { _ ->
            val activeFeatures = lifecycle.activeFeatures()
            if (activeFeatures.isEmpty()) return@flatMapLatest flowOf(emptyMap<FeatureId, FeatureStatus>())

            combine(activeFeatures.map { (id, feature) ->
                feature.status.map { status -> id to status }
            }) { pairs ->
                pairs.toMap()
            }
        }.stateIn(scope, SharingStarted.WhileSubscribed(), emptyMap())

    /** Development helper: apply raw JSON overlay. */
    suspend fun applyDevLicenseOverlay(overlayJson: String) {
        val overlay = SakJson.decodeFromString(SakConfig.serializer(), overlayJson)
        mergeAndApply(overlay)
    }

    private suspend fun applyLicense(payload: SignedLicensePayload?) {
        if (payload != null && licenseVerifier.verify(payload)) {
            mergeAndApply(payload.config)
        } else {
            mergeAndApply(null)
        }
    }

    private suspend fun mergeAndApply(overlay: SakConfig?) {
        val merged = if (overlay != null) {
            defaultConfig.mergeLicenseOverlay(overlay)
        } else {
            defaultConfig
        }
        _activeConfig.value = merged
        lifecycle.apply(merged, featureRegistry.factories(), serviceRegistry.lookup())
    }

    /** Request navigation on the host NavHost if route is allowed. */
    fun navigate(route: Route) {
        val featureId = route.value.substringBefore('/')
        val access = _activeConfig.value.features[featureId]
        if (access != null && !access.isRouteAllowed(route.value)) {
            return
        }
        _navigationRoute.value = route
    }

    private val _navigationRoute = MutableStateFlow<Route?>(null)

    /** One-shot navigation signal. */
    val navigationRoute: StateFlow<Route?> = _navigationRoute.asStateFlow()

    /** Clear navigation signal after host consumes it. */
    fun clearNavigation() {
        _navigationRoute.value = null
    }
}

/** Development verifier — accepts all licenses. */
object AcceptAllLicenseVerifier : LicenseVerifier {
    override fun verify(payload: SignedLicensePayload): Boolean = true
}
