package sak.sdk.feature

import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.StateFlow
import sak.sdk.config.FeatureAccess
import sak.sdk.entry.EntryPoint
import sak.sdk.navigation.Route
import sak.sdk.service.FeatureService

/**
 * Core contract for a pluggable business capability in SAK.
 *
 * Implements [LifecycleOwner]. Bound to host platform lifecycle (e.g. Android ComponentActivity/Compose
 * or SwiftUI scene phase).
 *
 * Features can be implemented directly on any class (`class AnalyticsFeature : Feature`).
 */
interface Feature : LifecycleOwner {
    /**
     * Stable, unique identifier for this feature module (e.g. `FeatureId("analytics")`).
     * Must match the key in [sak.sdk.config.SakConfig.features].
     */
    val id: FeatureId

    /**
     * Observable health and readiness status of this feature.
     * Emits state transitions (e.g. `Initializing`, `Ready`, `Error`).
     */
    val status: StateFlow<FeatureStatus>

    /**
     * Observable list of UI entry points (cards, banners, rows) published by this feature to fill host slots.
     */
    val entryPoints: StateFlow<List<EntryPoint>>

    /**
     * Internal navigation destinations owned by this feature (e.g. `Route("analytics/today")`).
     */
    val routes: List<Route> get() = emptyList()

    /**
     * Services or APIs exported by this feature for consumption by other features or the host app.
     */
    fun services(): Set<FeatureService<*>> = emptySet()

    /**
     * Called when the feature is instantiated and initialized.
     * Perform heavy initialization (DB setup, long-lived connections) here.
     *
     * @param context Runtime handle providing access to services, configuration, and coroutine scopes.
     */
    suspend fun onCreate(context: FeatureContext) {}

    /**
     * Called when the feature's configuration or licensing entitlements update.
     * Use this hook to reactively re-evaluate published entry points or internal capabilities.
     *
     * @param access Updated entitlement snapshot containing plans, capability grants, and attributes.
     */
    suspend fun onConfigChanged(access: FeatureAccess) {}

    /**
     * Called when the feature is deactivated or runtime shuts down.
     * Release all long-lived resources, clear flows, and cancel tasks.
     */
    suspend fun onDestroy() {}
}
