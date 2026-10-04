package sak.sdk.feature

import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CoroutineScope
import sak.sdk.config.FeatureAccess
import sak.sdk.navigation.Route
import sak.sdk.service.ServiceLookup

/**
 * The runtime environment sandbox injected into an active feature during [Feature.onCreate].
 *
 * The `FeatureContext` provides the feature module with safely scoped tools required to execute its logic.
 * By injecting these tools, the feature remains decoupled from the `SakRuntime` engine and the Host OS.
 */
interface FeatureContext {
    /** 
     * The current remote configuration snapshot for this specific feature.
     * Contains the enablement status and custom JSON parameters (e.g., sampling rates, theme overrides).
     */
    val access: FeatureAccess

    /**
     * The Service Locator registry.
     * Features use this to look up public APIs exported by other sibling features or the Host OS,
     * ensuring zero compile-time coupling between plugin modules.
     */
    val services: ServiceLookup

    /**
     * A managed CoroutineScope bound strictly to this feature's lifecycle.
     * When the feature is destroyed (due to config changes or app exit), all jobs launched
     * in this scope are automatically and instantly cancelled.
     */
    val coroutineScope: CoroutineScope

    /**
     * The platform [Lifecycle] registry synchronized with the Host container (Android Activity or SwiftUI Scene).
     * Features should use this to pause background work (like WebSockets) when the Host OS goes to the background.
     */
    val lifecycle: Lifecycle

    /**
     * Emits a navigation request over the exact-once Event Channel to the host application shell.
     *
     * @param route Target destination route (e.g. `Route("analytics/today")`).
     */
    fun navigate(route: Route)
}
