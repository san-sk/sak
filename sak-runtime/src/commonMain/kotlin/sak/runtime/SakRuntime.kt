package sak.runtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import sak.sdk.config.AppConfig
import sak.sdk.config.ConfigJson
import sak.sdk.config.mergeEntitlementOverlay
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureId
import sak.sdk.license.EntitlementConfigProvider
import sak.sdk.license.LicenseProvider
import sak.sdk.license.LicenseVerifier
import sak.sdk.license.SignedLicensePayload
import sak.sdk.navigation.NavigationResult
import sak.sdk.navigation.Route

/**
 * Application-scoped SAK engine: config merge, feature lifecycle, entry catalog, navigation.
 *
 * Observe [activeConfig], [renderPlan], and [navigationRoute] from the host UI.
 */
class SakRuntime(
    private val defaultConfig: AppConfig,
    private val featureFactories: Map<FeatureId, () -> Feature>,
    private val appServiceRegistry: AppServiceRegistry = AppServiceRegistry(),
    private val licenseVerifier: LicenseVerifier = AcceptAllLicenseVerifier,
    private val scope: CoroutineScope,
) {
    private val _activeConfig = MutableStateFlow(defaultConfig)
    /** Bundled default merged with the current entitlement overlay. */
    val activeConfig: StateFlow<AppConfig> = _activeConfig.asStateFlow()

    private val _renderPlan = MutableStateFlow(RenderPlan())
    /** Entry-point catalog from loaded features — host decides shell placement. */
    val renderPlan: StateFlow<RenderPlan> = _renderPlan.asStateFlow()

    private val _routeTable = MutableStateFlow(RouteTable(emptySet()))
    /** All routes from loaded features (deep links, validation). */
    val routeTable: StateFlow<RouteTable> = _routeTable.asStateFlow()

    private val apiRegistry = ApiRegistry()
    private val lifecycle = FeatureLifecycle(apiRegistry, ::navigateInternal)
    private val navigationGate = NavigationGate(
        capabilitiesFor = lifecycle::capabilities,
        guardsFor = lifecycle::routeGuards,
        activeFeatureIds = { lifecycle.activeFeatureIds() },
    )

    private var licenseProvider: LicenseProvider? = null
    private var entitlementProvider: EntitlementConfigProvider? = null

    /**
     * Subscribe to entitlement sources and reconcile feature load policy.
     *
     * @param licenseProvider Signed entitlement overlays (legacy).
     * @param entitlementProvider Unsigned JSON overlays (login / IAP / backend) — preferred.
     */
    suspend fun start(
        licenseProvider: LicenseProvider? = null,
        entitlementProvider: EntitlementConfigProvider? = null,
    ) {
        this.licenseProvider = licenseProvider
        this.entitlementProvider = entitlementProvider

        entitlementProvider?.current()?.let { mergeAndApply(it) }
            ?: applyLicense(licenseProvider?.resolveLicense())

        entitlementProvider?.let { provider ->
            scope.launch {
                provider.updates.collectLatest { config -> mergeAndApply(config) }
            }
        }

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

    /** Dev helper: apply raw entitlement JSON without a provider. */
    suspend fun applyDevEntitlementOverlay(overlayJson: String) {
        val overlay = ConfigJson.decodeFromString(AppConfig.serializer(), overlayJson)
        mergeAndApply(overlay)
    }

    private suspend fun applyLicense(payload: SignedLicensePayload?) {
        if (payload != null && licenseVerifier.verify(payload)) {
            mergeAndApply(payload.config)
        } else if (entitlementProvider?.current() == null) {
            mergeAndApply(null)
        }
    }

    private suspend fun mergeAndApply(overlay: AppConfig?) {
        val merged = overlay?.let { defaultConfig.mergeEntitlementOverlay(it) } ?: defaultConfig
        _activeConfig.value = merged
        lifecycle.apply(merged, featureFactories, appServiceRegistry.lookup())
        refreshDerivedState(merged)
    }

    private fun refreshDerivedState(config: AppConfig) {
        val features = lifecycle.activeFeatures()
        _renderPlan.value = EntryResolver.build(config, features)
        _routeTable.value = RouteMerger.build(features)
    }

    /** Request navigation — denied when a [sak.sdk.navigation.RouteGuard] blocks the route. */
    fun navigate(route: Route) {
        when (val result = navigationGate.evaluate(route)) {
            is NavigationResult.Allowed -> _navigationRoute.value = route
            is NavigationResult.Denied -> Unit // host may observe and show upsell
        }
    }

    private fun navigateInternal(route: Route) = navigate(route)

    private val _navigationRoute = MutableStateFlow<Route?>(null)
    /** One-shot navigation signal — consumed by host UI then cleared via [clearNavigation]. */
    val navigationRoute: StateFlow<Route?> = _navigationRoute.asStateFlow()

    fun clearNavigation() {
        _navigationRoute.value = null
    }

    /** Loaded feature instances keyed by id. */
    fun loadedFeatures(): Map<FeatureId, Feature> = lifecycle.activeFeatures()
}

/** Development verifier — accepts all signed payloads. Replace in production hosts. */
object AcceptAllLicenseVerifier : LicenseVerifier {
    override fun verify(payload: SignedLicensePayload): Boolean = true
}
