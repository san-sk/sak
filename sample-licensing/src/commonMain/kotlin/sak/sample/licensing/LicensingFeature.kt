package sak.sample.licensing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sak.sdk.api.FeatureApi
import sak.sdk.api.featureApi
import sak.sdk.config.FeatureToggle
import sak.sdk.entry.EntryAction
import sak.sdk.entry.EntryPoint
import sak.sdk.entry.EntryPointId
import sak.sdk.entry.EntryStyle
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureContext
import sak.sdk.feature.FeatureId
import sak.sdk.feature.FeatureState
import sak.sdk.feature.FeatureStatus
import sak.sdk.license.LicenseProvider
import sak.sdk.navigation.Route

/**
 * Sample licensing feature demonstrating plan-driven upgrade entry points:
 * - `unlock-banner` (Banner) on Home while on trial — nudges to upgrade.
 * - `plan-row` (Row) on Home always — shows the current plan.
 * - `pro-widget` (Widget) on Home once the license is pro.
 *
 * Exposes [SampleLicenseProvider] as a [LicenseProvider] API for other features.
 */
class LicensingFeature(
    private val licenseProvider: SampleLicenseProvider,
) : Feature {
    override val id = FeatureId("licensing")

    private val _status = MutableStateFlow(FeatureStatus())
    override val status: StateFlow<FeatureStatus> = _status.asStateFlow()

    private val _entryPoints = MutableStateFlow<List<EntryPoint>>(emptyList())
    override val entryPoints: StateFlow<List<EntryPoint>> = _entryPoints.asStateFlow()

    private val _access = MutableStateFlow<FeatureToggle?>(null)
    val access: StateFlow<FeatureToggle?> = _access.asStateFlow()

    override val routes = listOf(Route("licensing/upgrade"))

    override fun apis(): Set<FeatureApi<*>> =
        setOf(featureApi(LicenseProvider::class, licenseProvider))

    override suspend fun onCreate(context: FeatureContext) {
    }

    override suspend fun onDestroy() {
    }

    override suspend fun onActivated(context: FeatureContext) {
        _access.value = context.access
        publishEntries(context.access)
        _status.value = _status.value.copy(state = FeatureState.Ready)
    }

    override suspend fun onDeactivated() {
        _entryPoints.value = emptyList()
    }

    override suspend fun onAccessChanged(context: FeatureContext) {
        _access.value = context.access
        publishEntries(context.access)
    }

    private fun publishEntries(access: FeatureToggle) {
        val pro = access.plan == "pro"
        val entries = mutableListOf(
            EntryPoint(
                id = EntryPointId("plan-row"),
                type = "summary",
                style = EntryStyle.Row,
                action = EntryAction.Navigate("licensing/upgrade"),
                priority = 5,
                categories = setOf("home"),
            ),
        )
        entries += if (pro) {
            EntryPoint(
                id = EntryPointId("pro-widget"),
                type = "summary",
                style = EntryStyle.Widget,
                action = EntryAction.Navigate("licensing/upgrade"),
                priority = 8,
                categories = setOf("home"),
            )
        } else {
            EntryPoint(
                id = EntryPointId("unlock-banner"),
                type = "summary",
                style = EntryStyle.Banner,
                action = EntryAction.Navigate("licensing/upgrade"),
                priority = 9,
                categories = setOf("home"),
            )
        }
        _entryPoints.value = entries
    }
}
