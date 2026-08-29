package sak.ios.bridge

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import sak.sdk.entry.EntryPoint
import sak.sdk.feature.FeatureId
import sak.runtime.SakLauncher
import sak.runtime.SakRuntime
import sak.runtime.bundle.discoverFeatureModules
import sak.sample.licensing.SampleLicenseProvider
import sak.sdk.bundle.SakDependencies
import sak.sdk.bundle.sakDiscoveredModules
import sak.sdk.config.AppConfig
import sak.sdk.config.SakJson
import sak.sdk.navigation.Route

/**
 * Resolved entry for iOS consumption.
 */
data class IosResolvedEntry(
    val featureId: FeatureId,
    val entry: EntryPoint
)

/**
 * Headless SAK host for native iOS UI (SwiftUI, UIKit, …).
 *
 * Observes [AppConfig] from [SakRuntime] and exposes data for the shell to render.
 */
class SakIosHost {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var runtime: SakRuntime? = null
    private var observeJob: Job? = null
    private val licenseProvider = SampleLicenseProvider()

    private var configListener: ((AppConfig) -> Unit)? = null
    private var navigationListener: ((String) -> Unit)? = null

    private var latestConfig: AppConfig? = null

    /** Route to open when the user taps an entry widget. */
    fun navigationTargetFor(entry: IosResolvedEntry): String? = entry.navigationTarget()

    /** Start runtime with the demo harness config and bundled sample features. */
    fun start() {
        if (runtime != null) return
        scope.launch {
            val config = loadHarnessConfig()
            val modules = discoverFeatureModules(manualModules = sakDiscoveredModules)
            val deps = SakDependencies(licenseProvider = licenseProvider)
            val launcher = SakLauncher(scope)
            val sakRuntime = launcher.launch(
                defaultConfig = config,
                featureModules = modules,
                deps = deps,
            )
            runtime = sakRuntime
            observeJob = scope.launch {
                sakRuntime.activeConfig.collectLatest { config ->
                    latestConfig = config
                    configListener?.invoke(config)
                }
            }
            scope.launch {
                sakRuntime.navigationRoute.collectLatest { route ->
                    if (route != null) {
                        navigationListener?.invoke(route.value)
                        sakRuntime.clearNavigation()
                    }
                }
            }
        }
    }

    /** Subscribe to config updates. */
    fun setConfigListener(listener: ((AppConfig) -> Unit)?) {
        configListener = listener
        latestConfig?.let { listener?.invoke(it) }
    }

    /** Subscribe to one-shot navigation requests. */
    fun setNavigationListener(listener: ((String) -> Unit)?) {
        navigationListener = listener
    }

    /** Resolved entry widgets for a category (e.g. `home`, `alerts`). */
    suspend fun entriesForCategory(category: String): List<IosResolvedEntry> {
        val config = latestConfig ?: return emptyList()
        val runtime = runtime ?: return emptyList()
        
        val resolved = mutableListOf<IosResolvedEntry>()
        config.features.keys.forEach { idVal ->
            val id = FeatureId(idVal)
            val feature = runtime.getFeature(id)
            if (feature != null) {
                feature.entryPoints.value
                    .filter { it.categories.contains(category) }
                    .forEach { resolved.add(IosResolvedEntry(id, it)) }
            }
        }
        return resolved.sortedByDescending { it.entry.priority }
    }

    /** Apply entry tap. */
    fun onEntrySelected(entry: IosResolvedEntry) {
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
