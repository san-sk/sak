package sak.runtime

import sak.sdk.feature.FeatureId
import sak.sdk.navigation.NavigationResult
import sak.sdk.navigation.Route
import sak.sdk.navigation.RouteGuard

/**
 * Evaluates [RouteGuard]s declared by loaded features against live capabilities.
 */
class NavigationGate(
    private val capabilitiesFor: (FeatureId) -> Set<String>,
    private val guardsFor: (FeatureId) -> List<RouteGuard>,
    private val activeFeatureIds: () -> Set<FeatureId>,
) {
  fun evaluate(route: Route): NavigationResult {
        for (featureId in activeFeatureIds()) {
            for (guard in guardsFor(featureId)) {
                if (routeMatches(guard.pattern, route.value) && !hasCapabilities(featureId, guard.capabilities)) {
                    return NavigationResult.Denied(route = route, featureId = featureId, guard = guard)
                }
            }
        }
        return NavigationResult.Allowed
    }

    private fun hasCapabilities(featureId: FeatureId, required: Set<String>): Boolean {
        if (required.isEmpty()) return true
        val live = capabilitiesFor(featureId)
        return required.all { it in live }
    }

    private fun routeMatches(pattern: String, route: String): Boolean =
        route == pattern || route.startsWith("$pattern/")
}
