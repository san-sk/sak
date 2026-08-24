package sak.sdk.feature

import kotlinx.coroutines.flow.StateFlow
import sak.sdk.api.FeatureApi
import sak.sdk.entry.EntryPoint
import sak.sdk.navigation.Route
import sak.sdk.navigation.RouteGuard

/**
 * Pluggable business module: routes, entry-point descriptors, optional APIs.
 *
 * Runtime calls [onCreate] when [FeatureState] is [FeatureState.Enabled] and [onDestroy] when
 * policy changes to [FeatureState.Disabled] or [FeatureState.Hidden].
 */
interface Feature {
    val id: FeatureId

    /** Feature-reported UI readiness — not entitlement ([FeatureConfig]). */
    val status: StateFlow<FeatureStatus>

    /**
     * UI descriptors for the host to place and render.
     * Empty when the feature has nothing to expose.
     */
    val entryPoints: StateFlow<List<EntryPoint>>

    /** Routes this feature registers in the shared navigation graph. */
    val routes: List<Route>

    /** Optional route guards evaluated against live capabilities. */
    fun routeGuards(): List<RouteGuard> = emptyList()

    /** Typed APIs exported to other features while this instance is loaded. */
    fun apis(): Set<FeatureApi<*>> = emptySet()

    /** Called when entitlement policy loads this feature. */
    suspend fun onCreate(context: FeatureContext)

    /** Called when entitlement policy unloads this feature. */
    suspend fun onDestroy()

    /** Optional user-driven reset (e.g. clear local state) without unload. */
    suspend fun onReset() {}

    /** Called when capabilities or settings change while the instance stays loaded. */
    suspend fun onCapabilitiesChanged(context: FeatureContext) {}
}
