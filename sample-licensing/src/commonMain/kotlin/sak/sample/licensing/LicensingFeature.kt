package sak.sample.licensing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sak.sdk.api.FeatureApi
import sak.sdk.api.featureApi
import sak.sdk.entry.EntryAction
import sak.sdk.entry.EntryKind
import sak.sdk.entry.EntryPoint
import sak.sdk.entry.EntryPointId
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureContext
import sak.sdk.feature.FeatureId
import sak.sdk.feature.FeatureStatus
import sak.sdk.license.LicenseProvider
import sak.sdk.navigation.Route

/**
 * Sample licensing feature with plan-driven upgrade entry points on tag `home`.
 * Exposes [SampleLicenseProvider] as a [LicenseProvider] API.
 */
class LicensingFeature(
    private val licenseProvider: SampleLicenseProvider,
) : Feature {
    override val id = FeatureId("licensing")

    private val _status = MutableStateFlow(FeatureStatus())
    override val status: StateFlow<FeatureStatus> = _status.asStateFlow()

    private val _entryPoints = MutableStateFlow<List<EntryPoint>>(emptyList())
    override val entryPoints: StateFlow<List<EntryPoint>> = _entryPoints.asStateFlow()

    override val routes = listOf(Route("licensing/upgrade"))

    override fun apis(): Set<FeatureApi<*>> =
        setOf(featureApi(LicenseProvider::class, licenseProvider))

    override suspend fun onCreate(context: FeatureContext) {
        publishEntries(context.licenseType.value)
    }

    override suspend fun onDestroy() {
        _entryPoints.value = emptyList()
    }

    override suspend fun onReset() {
        _status.value = FeatureStatus()
    }

    override suspend fun onCapabilitiesChanged(context: FeatureContext) {
        publishEntries(context.licenseType.value)
    }

    private fun publishEntries(licenseType: String) {
        val pro = licenseType == "pro"
        val entries = mutableListOf(
            EntryPoint(
                id = EntryPointId("plan-row"),
                kind = EntryKind.Row,
                tags = setOf("home"),
                action = EntryAction.Navigate("licensing/upgrade"),
                priority = 5,
            ),
        )
        entries += if (pro) {
            EntryPoint(
                id = EntryPointId("pro-widget"),
                kind = EntryKind.Dashboard,
                tags = setOf("home"),
                action = EntryAction.Navigate("licensing/upgrade"),
                priority = 8,
            )
        } else {
            EntryPoint(
                id = EntryPointId("unlock-banner"),
                kind = EntryKind.Banner,
                tags = setOf("home"),
                action = EntryAction.Navigate("licensing/upgrade"),
                priority = 9,
            )
        }
        _entryPoints.value = entries
    }
}
