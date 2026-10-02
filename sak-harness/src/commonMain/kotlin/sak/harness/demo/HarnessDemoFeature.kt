package sak.harness.demo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sak.sdk.config.FeatureAccess
import sak.sdk.entry.EntryAction
import sak.sdk.entry.EntryPoint
import sak.sdk.entry.EntryPointId
import sak.sdk.entry.EntryPointType
import sak.sdk.entry.EntryStyle
import sak.sdk.feature.Attention
import sak.sdk.feature.BaseFeature
import sak.sdk.feature.FeatureContext
import sak.sdk.feature.FeatureId
import sak.sdk.feature.FeatureState
import sak.sdk.license.LicenseProvider
import sak.sdk.navigation.Route

/** Harness demo feature: publishes entry points for SAK capabilities. */
class HarnessDemoFeature : BaseFeature() {
    override val id = FeatureId("demos")

    private val _access = MutableStateFlow<FeatureAccess?>(null)
    val access: StateFlow<FeatureAccess?> = _access.asStateFlow()

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

    override suspend fun onCreate(context: FeatureContext) {
        super.onCreate(context)
        _access.value = context.access
        append("onCreate")
        setEntryPoints(demoEntries())
        updateStatus(FeatureState.Ready)
    }

    override suspend fun onConfigChanged(access: FeatureAccess) {
        _access.value = access
        append("onConfigChanged (plan=${access.plan})")
        setEntryPoints(demoEntries())
    }

    override suspend fun onDestroy() {
        super.onDestroy()
        append("onDestroy")
    }

    fun setSetupComplete(done: Boolean) {
        val newState = if (done) FeatureState.Ready else FeatureState.Initializing
        updateStatus(newState)
        append("status.state = $newState")
    }

    fun setAttention(attention: Attention) {
        updateAttention(attention)
        append("status.attention = $attention")
    }

    fun findLicenseProvider(): LicenseProvider? = context.services.find(LicenseProvider::class)

    fun logService(): HarnessLogService? = context.services.find(HarnessLogService::class)

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
        type = EntryPointType.Dashboard,
        style = EntryStyle.Card,
        action = EntryAction.Navigate("demos/$route"),
        priority = priority,
        categories = setOf("demo"),
    )
}
