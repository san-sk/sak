package sak.runtime.ios

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import sak.runtime.FeatureRegistry
import sak.runtime.SakRuntime
import sak.runtime.ServiceRegistry
import sak.sdk.config.SakConfig
import sak.sdk.config.SakJson
import sak.sdk.entry.EntryAction
import sak.sdk.feature.Feature
import sak.sdk.navigation.Route

/** Entry point descriptor exported for iOS SwiftUI host rendering. */
data class IosEntryPoint(
    val id: String,
    val featureId: String,
    val type: String,
    val style: String,
    val title: String,
    val subtitle: String?,
    val icon: String?,
    val badge: String?,
    val priority: Int,
    val actionRoute: String?,
)

/**
 * iOS host lifecycle driven by SwiftUI scene phase.
 * Swift calls [onResume], [onPause], [onStop], and [onDestroy] to mirror UIKit/Compose semantics.
 */
class IosHostLifecycle : LifecycleOwner {
    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle get() = registry

    init {
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun onPause() = registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)

    fun onStop() = registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)

    fun onResume() = registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

    fun onDestroy() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        registry.currentState = Lifecycle.State.DESTROYED
    }
}

/**
 * Swift-friendly host bridge for initializing and interacting with SakRuntime on iOS.
 */
class SakIosHost(
    private val defaultConfigJson: String = "{}",
    private val features: List<Feature> = emptyList(), // Note: Pre-instantiated for Swift convenience, but ideally use factories in Swift
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val hostLifecycle = IosHostLifecycle()
    private val featureRegistry = FeatureRegistry.of(*features.toTypedArray())

    private var runtime: SakRuntime? = null

    /** Starts SakRuntime engine on iOS. */
    fun start(onReady: () -> Unit = {}) {
        val config = try {
            SakJson.decodeFromString(SakConfig.serializer(), defaultConfigJson)
        } catch (e: Exception) {
            SakConfig()
        }

        val sakRuntime = SakRuntime(
            baseConfig = config,
            featureRegistry = featureRegistry,
            serviceRegistry = ServiceRegistry(),
            scope = scope,
            hostLifecycle = hostLifecycle.lifecycle,
        )
        runtime = sakRuntime

        scope.launch {
            sakRuntime.start()
            onReady()
        }
    }

    /** Applies a config overlay enabling or disabling features at runtime. */
    fun applyConfig(json: String) {
        val sakRuntime = runtime ?: return
        scope.launch {
            val overlay = try {
                SakJson.decodeFromString(SakConfig.serializer(), json)
            } catch (e: Exception) {
                return@launch
            }
            sakRuntime.updateConfig(overlay)
        }
    }

    /** Convenience helper to enable or disable a single feature. */
    fun setFeatureEnabled(featureId: String, enabled: Boolean) {
        scope.launch {
            runtime?.setFeatureEnabled(featureId, enabled)
        }
    }

    /** Observes active navigation route changes for SwiftUI NavigationStack. */
    fun observeNavigation(callback: (String?) -> Unit) {
        val sakRuntime = runtime ?: return
        scope.launch {
            sakRuntime.navigationEvents.collectLatest { route ->
                callback(route.value)
            }
        }
    }

    /** Triggers a navigation route request. */
    fun navigate(route: String) {
        runtime?.navigate(Route(route))
    }

    /** Returns all entry points published by enabled features, sorted by descending priority. */
    fun queryEntryPoints(callback: (List<IosEntryPoint>) -> Unit) {
        val sakRuntime = runtime ?: run {
            callback(emptyList())
            return
        }

        scope.launch {
            callback(sakRuntime.allEntryPoints.value.map { (featureId, entry) ->
                val route = when (val action = entry.action) {
                    is EntryAction.Navigate -> action.route
                    is EntryAction.DeepLink -> action.uri
                    else -> null
                }
                IosEntryPoint(
                    id = entry.id.value,
                    featureId = featureId.value,
                    type = entry.type.name,
                    style = entry.style?.name ?: "Default",
                    title = entry.title,
                    subtitle = entry.subtitle,
                    icon = entry.icon,
                    badge = entry.badge,
                    priority = entry.priority,
                    actionRoute = route,
                )
            })
        }
    }

    /** Returns whether a feature is currently enabled. */
    fun isFeatureEnabled(featureId: String): Boolean =
        runtime?.activeConfig?.value?.let { it.isFeatureEnabled(featureId) } ?: true

    /** Observes feature enabled/disabled state changes. */
    fun observeFeatureEnabled(featureId: String, callback: (Boolean) -> Unit) {
        val sakRuntime = runtime ?: run {
            callback(true)
            return
        }
        scope.launch {
            sakRuntime.activeConfig.collectLatest { config ->
                callback(config.features[featureId]?.enabled ?: true)
            }
        }
    }

    /** Forwards SwiftUI scene phase changes to the host lifecycle. */
    fun onScenePhaseActive() = hostLifecycle.onResume()

    fun onScenePhaseInactive() = hostLifecycle.onPause()

    fun onScenePhaseBackground() = hostLifecycle.onStop()

    /** Stops SakRuntime engine and cleans up coroutines. */
    fun stop() {
        scope.launch {
            runtime?.stop()
            hostLifecycle.onDestroy()
            scope.cancel()
        }
    }
}
