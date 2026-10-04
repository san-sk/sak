package sak.sdk.feature

import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.StateFlow
import sak.sdk.config.FeatureAccess
import sak.sdk.entry.EntryPoint
import sak.sdk.service.FeatureService

/**
 * The core contract for a pluggable business capability in the SAK framework.
 *
 * Features are highly decoupled, lifecycle-aware ([LifecycleOwner]) state machines. They contain
 * domain logic but do **not** render Host UI frameworks directly. 
 *
 * Instead, they interact with the Host App via Data and Events:
 * 1. They publish UI metadata descriptors via [entryPoints].
 * 2. They communicate with sibling modules by exporting [services].
 * 3. They trigger UI transitions via `FeatureContext.navigate()`.
 */
interface Feature : LifecycleOwner {
    /**
     * The stable, unique identifier for this feature module (e.g. `FeatureId("analytics")`).
     * Must match the key in [sak.sdk.config.SakConfig] and [sak.sdk.license.FeatureEntitlements].
     */
    val id: FeatureId

    /**
     * The observable health ([FeatureState]) and urgency ([Attention]) status of this module.
     * Starts as `Initializing` and should transition to `Ready` when the feature is usable.
     */
    val status: StateFlow<FeatureStatus>

    /**
     * An observable stream of Micro-Frontend UI descriptors.
     * The Host App observes this flow and natively renders Jetpack Compose or SwiftUI views based
     * on the data (Titles, Icons, Actions) provided here.
     */
    val entryPoints: StateFlow<List<EntryPoint>>

    /**
     * Public Service Locator APIs exported by this feature for consumption by other sibling features.
     * This acts as the Inter-Process Communication (IPC) boundary, allowing features to share logic
     * without direct Gradle/build dependencies.
     */
    fun services(): Set<FeatureService<*>> = emptySet()

    /**
     * Bootstraps the feature. Called automatically by `SakRuntime` concurrently when the feature
     * passes the Registry, License, and Config gates.
     *
     * This is a `suspend` function. Features should perform database setup or network authentication here.
     *
     * @param context The runtime sandbox providing Coroutine scopes, navigation, and service lookups.
     */
    suspend fun onCreate(context: FeatureContext) {}

    /**
     * Called when the remote configuration for this specific feature updates while it is active.
     * Use this to hot-swap themes, sampling rates, or feature toggles without restarting the app.
     *
     * @param access The updated configuration snapshot, including custom JSON `params`.
     */
    suspend fun onConfigChanged(access: FeatureAccess) {}

    /**
     * Called when the feature is torn down (e.g., the remote config flipped its status to disabled, 
     * the user's license expired, or the Host App is shutting down).
     * 
     * Release database connections here. Note: The `FeatureContext.coroutineScope` is automatically 
     * cancelled for you by the runtime engine.
     */
    suspend fun onDestroy() {}
}
