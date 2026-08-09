package sak.runtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import sak.sdk.config.AppConfig
import sak.sdk.config.SakJson
import sak.sdk.config.mergeLicenseOverlay
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureId
import sak.sdk.license.LicenseProvider
import sak.sdk.license.LicenseVerifier
import sak.sdk.license.SignedLicensePayload
import sak.sdk.navigation.Route

/**
 * Application-scoped SAK engine: config merge, feature lifecycle, scaffold render plan, navigation.
 *
 * Observe [renderPlan] and [navigationRoute] from Compose; call [start] once from the host.
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

    private val _renderPlan = MutableStateFlow(RenderPlan(sak.sdk.layout.ScaffoldConfig(), emptyMap()))
    /** Scaffold bottom bar + resolved entry widgets per destination. Drives [sak.compose.ui.SakApp]. */
    val renderPlan: StateFlow<RenderPlan> = _renderPlan.asStateFlow()

    private val _routeTable = MutableStateFlow(RouteTable(emptySet()))
    /** All routes from active features (deep links, validation). */
    val routeTable: StateFlow<RouteTable> = _routeTable.asStateFlow()

    private val apiRegistry = ApiRegistry()
    private val lifecycle = FeatureLifecycle(apiRegistry, ::navigate)

    private var licenseProvider: LicenseProvider? = null

    /** Compose entry-preview registry for a feature (null if not active). */
    fun screenRegistry(featureId: FeatureId): ScreenRegistryImpl? = lifecycle.screenRegistry(featureId)

    /**
     * Apply initial license, subscribe to [LicenseProvider.updates], and activate features.
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
        refreshDerivedState(merged)
    }

    private fun refreshDerivedState(config: AppConfig) {
        val features = lifecycle.activeFeatures()
        _renderPlan.value = ScaffoldRenderer.build(config.scaffold, config, features)
        _routeTable.value = RouteMerger.build(features)
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
