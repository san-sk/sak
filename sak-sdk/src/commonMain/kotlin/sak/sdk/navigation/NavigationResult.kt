package sak.sdk.navigation

import sak.sdk.feature.FeatureId

/**
 * Result of evaluating a navigation request against [RouteGuard]s and live capabilities.
 */
sealed class NavigationResult {
    /** Navigation may proceed. */
    data object Allowed : NavigationResult()

    /**
     * Navigation blocked by a feature guard.
     *
     * @property route Requested route.
     * @property featureId Feature that declared the matching guard.
     * @property guard Guard that denied access.
     */
    data class Denied(
        val route: Route,
        val featureId: FeatureId,
        val guard: RouteGuard,
    ) : NavigationResult()
}
