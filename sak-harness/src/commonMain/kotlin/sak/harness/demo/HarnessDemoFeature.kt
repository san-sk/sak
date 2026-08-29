package sak.harness.demo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sak.sdk.api.FeatureApiDescriptor
import sak.sdk.config.FeatureToggle
import sak.sdk.entry.EntryAction
import sak.sdk.entry.EntryPoint
import sak.sdk.entry.EntryPointId
import sak.sdk.entry.EntryStyle
import sak.sdk.feature.Attention
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureContext
import sak.sdk.feature.FeatureId
import sak.sdk.feature.FeatureState
import sak.sdk.feature.FeatureStatus
import sak.sdk.license.LicenseProvider
import sak.sdk.navigation.Route

/** Harness demo feature: publishes one entry point per SAK capability. */
class HarnessDemoFeature : Feature {
    override val id = FeatureId("demos")

    private val _status = MutableStateFlow(FeatureStatus())
    override val status: StateFlow<FeatureStatus> = _status.asStateFlow()

    private val _entryPoints = MutableStateFlow<List<EntryPoint>>(emptyList())
    override val entryPoints: StateFlow<List<EntryPoint>> = _entryPoints.asStateFlow()

    private val _access = MutableStateFlow<FeatureToggle?>(null)
    val access: StateFlow<FeatureToggle?> = _access.asStateFlow()

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
        append("onCreate")
    }

    override suspend fun onDestroy() {
        append("onDestroy")
    }

    override suspend fun onActivated(context: FeatureContext) {
        this.context = context
        _access.value = context.access
        append("onActivated (plan=${context.access.plan})")
        _entryPoints.value = demoEntries()
        _status.value = _status.value.copy(state = FeatureState.Ready)
    }

    override suspend fun onAccessChanged(context: FeatureContext) {
        _access.value = context.access
        append("onAccessChanged (plan=${context.access.plan})")
        _entryPoints.value = demoEntries()
    }

    override suspend fun onDeactivated() {
        append("onDeactivated")
        _entryPoints.value = emptyList()
    }

    fun setSetupComplete(done: Boolean) {
        val newState = if (done) FeatureState.Ready else FeatureState.Initializing
        _status.value = _status.value.copy(state = newState)
        append("status.state = $newState")
    }

    fun setAttention(attention: sak.sdk.feature.Attention) {
        _status.value = _status.value.copy(attention = attention)
        append("status.attention = $attention")
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
        type = "demo",
        style = EntryStyle.Card,
        action = EntryAction.Navigate("demos/$route"),
        priority = priority,
        categories = setOf("demo"),
    )
}
