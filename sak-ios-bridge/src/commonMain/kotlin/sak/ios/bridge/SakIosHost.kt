package sak.ios.bridge

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import sak.runtime.RenderPlan
import sak.runtime.ResolvedEntry
import sak.runtime.SakHost
import sak.runtime.SakHostSetup
import sak.runtime.SakRuntime
import sak.runtime.shell.DefaultHostShell
import sak.runtime.shell.HostShellDestination
import sak.sample.licensing.SampleLicenseProvider
import sak.sdk.config.AppConfig
import sak.sdk.config.ConfigJson
import sak.sdk.navigation.Route

/**
 * Headless SAK host for native iOS UI (SwiftUI, UIKit, …).
 */
class SakIosHost {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val licenseProvider = SampleLicenseProvider()
    private val host = SakHost(
        scope = scope,
        setup = SakHostSetup(
            defaultConfig = loadHarnessConfig(),
            bundledModules = iosHarnessBundledFeatureModules(licenseProvider),
            licenseProvider = licenseProvider,
        ),
    )

    private var runtime: SakRuntime? = null
    private var observeJob: Job? = null

    private var renderPlanListener: ((RenderPlan) -> Unit)? = null
    private var navigationListener: ((String) -> Unit)? = null

    private var latestPlan: RenderPlan? = null

    fun navigationTargetFor(entry: ResolvedEntry): String? = entry.navigationTarget()

    fun start() {
        if (runtime != null) return
        scope.launch {
            val sakRuntime = host.start()
            runtime = sakRuntime
            observeJob = scope.launch {
                sakRuntime.renderPlan.collect { plan ->
                    latestPlan = plan
                    renderPlanListener?.invoke(plan)
                }
            }
            scope.launch {
                sakRuntime.navigationRoute.collect { route ->
                    if (route != null) {
                        navigationListener?.invoke(route.value)
                        sakRuntime.clearNavigation()
                    }
                }
            }
        }
    }

    fun setRenderPlanListener(listener: ((RenderPlan) -> Unit)?) {
        renderPlanListener = listener
        latestPlan?.let { listener?.invoke(it) }
    }

    fun setNavigationListener(listener: ((String) -> Unit)?) {
        navigationListener = listener
    }

    fun currentRenderPlan(): RenderPlan? = latestPlan

    fun shellDestinations(): List<HostShellDestination> = DefaultHostShell.destinations

    fun bottomBarItems(): List<HostShellDestination> = shellDestinations()

    fun entriesForDestination(destinationId: String): List<ResolvedEntry> =
        latestPlan?.entriesForTag(destinationId) ?: emptyList()

    fun onEntrySelected(entry: ResolvedEntry) {
        val target = entry.navigationTarget()
        if (target != null) {
            runtime?.navigate(Route(target))
        }
    }

    fun navigate(route: String) {
        runtime?.navigate(Route(route))
    }

    fun unlockPro() {
        licenseProvider.unlockPro()
    }

    fun loadHarnessConfig(): AppConfig =
        ConfigJson.decodeFromString(AppConfig.serializer(), harnessDefaultConfigJson)
}
