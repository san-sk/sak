package sak.harness.demo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sak.sdk.api.FeatureApiDescriptor
import sak.sdk.entry.EntryAction
import sak.sdk.entry.EntryKind
import sak.sdk.entry.EntryPoint
import sak.sdk.entry.EntryPointId
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureContext
import sak.sdk.feature.FeatureId
import sak.sdk.feature.FeatureStatus
import sak.sdk.feature.IntroState
import sak.sdk.license.LicenseProvider
import sak.sdk.navigation.Route

/** Harness demo feature: publishes one entry point per SAK capability. */
class HarnessDemoFeature : Feature {
    override val id = FeatureId("demos")

    private val _status = MutableStateFlow(FeatureStatus())
    override val status: StateFlow<FeatureStatus> = _status.asStateFlow()

    private val _entryPoints = MutableStateFlow<List<EntryPoint>>(emptyList())
    override val entryPoints: StateFlow<List<EntryPoint>> = _entryPoints.asStateFlow()

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log.asStateFlow()

    override val routes = listOf(
        Route("demos/config"),
        Route("demos/lifecycle"),
        Route("demos/entrystyles"),
        Route("demos/access"),
        Route("demos/api"),
        Route("demos/service"),
        Route("demos/license"),
        Route("demos/routes"),
    )

    private var context: FeatureContext? = null

    override suspend fun onCreate(context: FeatureContext) {
        this.context = context
        append("onCreate (license=${context.licenseType.value})")
        _entryPoints.value = demoEntries()
    }

    override suspend fun onCapabilitiesChanged(context: FeatureContext) {
        append("onCapabilitiesChanged (license=${context.licenseType.value})")
        _entryPoints.value = demoEntries()
    }

    override suspend fun onDestroy() {
        append("onDestroy")
        _entryPoints.value = emptyList()
        context = null
    }

    override suspend fun onReset() {
        _status.value = FeatureStatus()
        _log.value = listOf("onReset")
    }

    fun setSetupComplete(done: Boolean) {
        _status.value = _status.value.copy(setupComplete = done)
        append("status.setupComplete = $done")
    }

    fun setIntroState(intro: IntroState) {
        _status.value = _status.value.copy(intro = intro)
        append("status.intro = $intro")
    }

    fun findLicenseProvider(): LicenseProvider? = context?.apis?.find(LicenseProvider::class)

    fun apiCatalog(): List<FeatureApiDescriptor> = context?.apis?.catalog() ?: emptyList()

    fun logService(): HarnessLogService? = context?.services?.find(HarnessLogService::class)

    private fun append(line: String) {
        _log.value = (_log.value + line).takeLast(12)
    }

    private fun demoEntries(): List<EntryPoint> =
        listOf(
            demoEntry("config-demo", "config", priority = 10),
            demoEntry("lifecycle-demo", "lifecycle", priority = 9),
            demoEntry("entry-styles-demo", "entrystyles", priority = 8),
            demoEntry("access-demo", "access", priority = 7),
            demoEntry("api-demo", "api", priority = 6),
            demoEntry("service-demo", "service", priority = 5),
            demoEntry("license-demo", "license", priority = 4),
            demoEntry("routes-demo", "routes", priority = 3),
        )

    private fun demoEntry(id: String, route: String, priority: Int) = EntryPoint(
        id = EntryPointId(id),
        kind = EntryKind.Card,
        tags = setOf("demo"),
        action = EntryAction.Navigate("demos/$route"),
        priority = priority,
    )
}
