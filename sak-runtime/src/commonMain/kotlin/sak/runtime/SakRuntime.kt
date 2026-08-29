package sak.runtime

import kotlinx.coroutines.CoroutineScope
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
import sak.sdk.config.AppConfig
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
 *
 * Observe [activeConfig] and [navigationRoute] from the Host UI.
 */
class SakRuntime(
    private val defaultConfig: AppConfig,
    private val featureFactories: Map<FeatureId, () -> Feature>,
    private val appServiceRegistry: AppServiceRegistry = AppServiceRegistry(),
    private val licenseVerifier: LicenseVerifier = AcceptAllLicenseVerifier,
    private val scope: CoroutineScope,
) {
    private val _activeConfig = MutableStateFlow(defaultConfig)
    /** DefaultConfig merged with the current license overlay. */
    val activeConfig: StateFlow<AppConfig> = _activeConfig.asStateFlow()

    private val apiRegistry = ApiRegistry()
    private val lifecycle = FeatureLifecycle(apiRegistry, ::navigate, scope)

    private var licenseProvider: LicenseProvider? = null

    /**
     * Apply initial license, subscribe to [LicenseProvider.updates], and initialize features.
     */
    suspend fun start(licenseProvider: LicenseProvider? = null) {
        this.licenseProvider = licenseProvider
        applyLicense(licenseProvider?.resolveLicense())
        licenseProvider?.let { provider ->
            scope.launch {
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
        lifecycle.shutdown()
    }

    /** Access to active features for platform UI bridge. Trigger lazy load if needed. */
    suspend fun getFeature(id: FeatureId): Feature? = 
        lifecycle.getOrInstantiate(id, featureFactories, appServiceRegistry.lookup(), activeConfig.value)

    /**
     * Synchronous check for a feature. Does NOT trigger lazy loading.
     */
    fun getFeatureOrNull(id: FeatureId): Feature? = lifecycle.activeFeatures()[id]

    /**
     * Observable map of all features (instantiated) and their current status.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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

    /** Dev helper: apply a raw JSON overlay without going through [LicenseProvider]. */
    suspend fun applyDevLicenseOverlay(overlayJson: String) {
        val overlay = SakJson.decodeFromString(AppConfig.serializer(), overlayJson)
        mergeAndApply(overlay)
    }

    private suspend fun applyLicense(payload: SignedLicensePayload?) {
        if (payload != null && licenseVerifier.verify(payload)) {
            mergeAndApply(payload.config)
        } else {
            mergeAndApply(null)
        }
    }

    private suspend fun mergeAndApply(overlay: AppConfig?) {
        val merged = if (overlay != null) {
            defaultConfig.mergeLicenseOverlay(overlay)
        } else {
            defaultConfig
        }
        _activeConfig.value = merged
        lifecycle.apply(merged, featureFactories, appServiceRegistry.lookup())
    }

    /** Request navigation on the host NavHost (notifications, deep links, feature callbacks). */
    fun navigate(route: Route) {
        _navigationRoute.value = route
    }

    private val _navigationRoute = MutableStateFlow<Route?>(null)
    /** One-shot navigation signal — consumed by SakApp then cleared via [clearNavigation]. */
    val navigationRoute: StateFlow<Route?> = _navigationRoute.asStateFlow()

    /** Consume the one-shot [navigationRoute] after handling in the host UI. */
    fun clearNavigation() {
        _navigationRoute.value = null
    }
}

/** Development verifier — accepts all licenses. Replace in production hosts. */
object AcceptAllLicenseVerifier : LicenseVerifier {
    override fun verify(payload: SignedLicensePayload): Boolean = true
}
