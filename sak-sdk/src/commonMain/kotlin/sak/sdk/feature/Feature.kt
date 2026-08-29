package sak.sdk.feature

import kotlinx.coroutines.flow.StateFlow
import sak.sdk.api.FeatureApi
import sak.sdk.entry.EntryPoint
import sak.sdk.navigation.NavigationContract
import sak.sdk.navigation.Route

/**
 * A pluggable business capability (analytics, licensing, messaging, …).
 *
 * Lifecycle is owned by [sak.runtime.FeatureLifecycle], not the Android Activity.
 * The host activates features based on config; features publish widgets and routes reactively.
 */
interface Feature {
    /** Stable id — must match the key in config `features` and [FeatureModule.id]. */
    val id: FeatureId

    /**
     * Runtime-visible health and readiness (setup done, needs attention, intro state).
     * Update this as async initialization completes.
     */
    val status: StateFlow<FeatureStatus>

    /**
     * Widgets shown on scaffold destinations (Home, Alerts, …).
     * Publish in [onActivated] / [onAccessChanged]; clear in [onDeactivated].
     */
    val entryPoints: StateFlow<List<EntryPoint>>

    /** Internal screens this feature owns (e.g. `analytics/today`). */
    val routes: List<Route>

    /** Optional navigation contract for typed access to this feature's screens. */
    val navContract: NavigationContract? get() = null

    /** Optional APIs other features or the host can look up (e.g. [sak.sdk.license.LicenseProvider]). */
    fun apis(): Set<FeatureApi<*>> = emptySet()

    /**
     * Initial setup when the runtime starts. Called once per app session.
     * Heavy initialization (DB, long-lived connections) should start here.
     */
    suspend fun onCreate(context: FeatureContext)

    /**
     * Runtime is shutting down or feature is being permanently removed.
     * Release all long-lived resources.
     */
    suspend fun onDestroy()

    /** Feature became enabled in ActiveConfig — register screens, publish entry points. */
    suspend fun onActivated(context: FeatureContext)

    /** Feature disabled in ActiveConfig — clear flows and stop UI updates. */
    suspend fun onDeactivated()

    /** Plan, grants, or entries changed (usually after a license update). */
    suspend fun onAccessChanged(context: FeatureContext) {}
}
