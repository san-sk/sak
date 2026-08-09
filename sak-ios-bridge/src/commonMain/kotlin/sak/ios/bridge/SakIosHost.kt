package sak.ios.bridge

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import sak.runtime.RenderPlan
import sak.runtime.ResolvedEntry
import sak.runtime.SakLauncher
import sak.runtime.SakRuntime
import sak.runtime.bundle.discoverFeatureModules
import sak.sample.licensing.SampleLicenseProvider
import sak.sdk.bundle.SakDependencies
import sak.sdk.config.AppConfig
import sak.sdk.config.SakJson
import sak.sdk.layout.BottomBarItem
import sak.sdk.navigation.Route

/**
 * Headless SAK host for native iOS UI (SwiftUI, UIKit, …).
 *
 * Observes [RenderPlan] from [SakRuntime] and exposes data for the shell to render.
 * Does not use Compose — see [sak.compose.ui.SakApp] for the Compose reference shell.
 */
class SakIosHost {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var runtime: SakRuntime? = null
    private var observeJob: Job? = null
    private val licenseProvider = SampleLicenseProvider()

    private var renderPlanListener: ((RenderPlan) -> Unit)? = null
    private var navigationListener: ((String) -> Unit)? = null

    private var latestPlan: RenderPlan? = null

    /** Route to open when the user taps an entry widget. */
    fun navigationTargetFor(entry: ResolvedEntry): String? = entry.navigationTarget()

    /** Start runtime with the demo harness config and bundled sample features. */
    fun start() {
        if (runtime != null) return
        scope.launch {
            val config = loadHarnessConfig()
            val modules = discoverFeatureModules(manualModules = iosHarnessBundledFeatureModules)
            val deps = SakDependencies(licenseProvider = licenseProvider)
            val launcher = SakLauncher(scope)
            val sakRuntime = launcher.launch(
                defaultConfig = config,
                featureModules = modules,
                deps = deps,
            )
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

    /** Subscribe to scaffold updates (bottom bar + destination entries). Replacing listener re-delivers latest plan. */
    fun setRenderPlanListener(listener: ((RenderPlan) -> Unit)?) {
        renderPlanListener = listener
        latestPlan?.let { listener?.invoke(it) }
    }

    /** Subscribe to one-shot navigation requests (entry taps, feature callbacks). Route is a string path. */
    fun setNavigationListener(listener: ((String) -> Unit)?) {
        navigationListener = listener
    }

    /** Last [RenderPlan] snapshot, or null before [start] completes. */
    fun currentRenderPlan(): RenderPlan? = latestPlan

    /** Bottom navigation items from the active render plan. */
    fun bottomBarItems(): List<BottomBarItem> =
        latestPlan?.scaffold?.bottomBar?.items ?: emptyList()

    /** Resolved entry widgets for a scaffold destination id (e.g. `home`, `alerts`). */
    fun entriesForDestination(destinationId: String): List<ResolvedEntry> =
        latestPlan?.destinations[destinationId] ?: emptyList()

    /** Apply entry tap — navigates via runtime when the entry has a route action. */
    fun onEntrySelected(entry: ResolvedEntry) {
        val target = entry.navigationTarget()
        if (target != null) {
            runtime?.navigate(Route(target))
        }
    }

    /** Request navigation on the runtime (in-feature pushes from native UI). */
    fun navigate(route: String) {
        runtime?.navigate(Route(route))
    }

    /** Demo: apply pro license overlay (same as Compose harness unlock flow). */
    fun unlockPro() {
        licenseProvider.unlockPro()
    }

    /** Embedded harness DefaultConfig JSON (no iOS bundle asset loading). */
    fun loadHarnessConfig(): AppConfig =
        SakJson.decodeFromString(AppConfig.serializer(), harnessDefaultConfigJson)
}
