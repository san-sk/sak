package sak.sample.licensing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sak.sdk.api.FeatureApi
import sak.sdk.api.featureApi
import sak.sdk.entry.EntryAction
import sak.sdk.entry.EntryPoint
import sak.sdk.entry.EntryPointId
import sak.sdk.entry.EntryStyle
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureContext
import sak.sdk.feature.FeatureId
import sak.sdk.feature.FeatureStatus
import sak.sdk.license.LicenseProvider
import sak.sdk.navigation.Route

/** Demo licensing feature: unlock entry, upgrade route, exposes [SampleLicenseProvider] as API. */
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

    override suspend fun onActivated(context: FeatureContext) {
        _entryPoints.value = listOf(
            EntryPoint(
                id = EntryPointId("unlock-card"),
                style = EntryStyle.Card,
                previewKey = "licensing.unlock",
                action = EntryAction.Navigate("licensing/upgrade"),
                priority = 5,
                categories = setOf("home"),
            ),
        )
    }

    override suspend fun onDeactivated() {
        _entryPoints.value = emptyList()
    }

    override suspend fun onReset() {
        _status.value = FeatureStatus()
    }
}
