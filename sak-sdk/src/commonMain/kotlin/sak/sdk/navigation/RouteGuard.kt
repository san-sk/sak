package sak.sdk.navigation

/**
 * Route protection declared by a [sak.sdk.feature.Feature].
 *
 * Runtime matches [pattern] against the requested route and requires [capabilities]
 * from the live [sak.sdk.license.FeatureEntitlement].
 *
 * @property pattern Route pattern (exact or prefix — runtime defines matching rules).
 * @property capabilities All required capability strings; empty means guard applies to pattern only.
 */
data class RouteGuard(
    val pattern: String,
    val capabilities: Set<String> = emptySet(),
)
