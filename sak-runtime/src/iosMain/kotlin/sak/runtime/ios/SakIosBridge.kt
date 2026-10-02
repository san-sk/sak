package sak.runtime.ios

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
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
import sak.sdk.feature.FeatureId
import sak.sdk.navigation.Route

/** Data representation of an EntryPoint exported for iOS SwiftUI host rendering. */
data class IosEntryPoint(
    val id: String,
    val featureId: String,
    val type: String,
    val style: String,
    val actionRoute: String?,
    val priority: Int,
    val categories: List<String>,
)

/** Minimal iOS platform lifecycle owner. */
class IosHostLifecycle : LifecycleOwner {
    override val lifecycle: Lifecycle = object : Lifecycle() {
        override fun addObserver(observer: LifecycleObserver) {}
        override fun removeObserver(observer: LifecycleObserver) {}
        override val currentState: State = State.RESUMED
    }
}

/**
 * Swift-friendly host bridge for initializing and interacting with SakRuntime on iOS.
 */
class SakIosHost(
    private val defaultConfigJson: String = "{}",
    private val features: List<Feature> = emptyList(),
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
            defaultConfig = config,
            featureRegistry = featureRegistry,
            serviceRegistry = ServiceRegistry(),
            scope = scope,
            hostLifecycle = hostLifecycle.lifecycle
        )
        runtime = sakRuntime

        scope.launch {
            sakRuntime.start()
            onReady()
        }
    }

    /** Observes active navigation route changes for SwiftUI NavigationStack. */
    fun observeNavigation(callback: (String?) -> Unit) {
        val sakRuntime = runtime ?: return
        scope.launch {
            sakRuntime.navigationRoute.collectLatest { route ->
                callback(route?.value)
            }
        }
    }

    /** Triggers a navigation route request. */
    fun navigate(route: String) {
        runtime?.navigate(Route(route))
    }

    /** Clears active navigation state. */
    fun clearNavigation() {
        runtime?.clearNavigation()
    }

    /** Resolves entry point widgets for a given host slot category. */
    fun queryCategoryEntries(category: String, callback: (List<IosEntryPoint>) -> Unit) {
        val sakRuntime = runtime ?: run {
            callback(emptyList())
            return
        }

        scope.launch {
            val config = sakRuntime.activeConfig.value
            val activeFeatureIds = config.features.filterValues { it.enabled && it.visible }.keys.map { FeatureId(it) }
            val resolved = mutableListOf<IosEntryPoint>()

            for (id in activeFeatureIds) {
                val feature = sakRuntime.getFeature(id) ?: continue
                for (entry in feature.entryPoints.value) {
                    if (entry.categories.contains(category)) {
                        val route = when (val action = entry.action) {
                            is EntryAction.Navigate -> action.route
                            is EntryAction.DeepLink -> action.uri
                            else -> null
                        }
                        resolved.add(
                            IosEntryPoint(
                                id = entry.id.value,
                                featureId = id.value,
                                type = entry.type.name,
                                style = entry.style?.name ?: "Default",
                                actionRoute = route,
                                priority = entry.priority,
                                categories = entry.categories.toList()
                            )
                        )
                    }
                }
            }

            resolved.sortByDescending { it.priority }
            callback(resolved)
        }
    }

    /** Stops SakRuntime engine and cleans up coroutines. */
    fun stop() {
        scope.launch {
            runtime?.stop()
            scope.cancel()
        }
    }
}
