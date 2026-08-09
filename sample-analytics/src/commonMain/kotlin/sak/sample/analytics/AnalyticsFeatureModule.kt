package sak.sample.analytics

import sak.compose.ui.FeatureNavigationModule
import sak.compose.ui.FeatureNavContributor
import sak.sdk.bundle.FeatureModule
import sak.sdk.bundle.SakDependencies
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureId

/**
 * Convention: package `sak.sample.analytics` → feature id `analytics`.
 * Registered for Android via META-INF/services.
 */
object AnalyticsFeatureModule : FeatureModule, FeatureNavigationModule {
    /** Last created analytics feature instance — screens read its access StateFlow. */
    var feature: AnalyticsFeature? = null
        private set

    override val id = FeatureId("analytics")

    override fun create(deps: SakDependencies): Feature =
        AnalyticsFeature().also { feature = it }

    override val featureId = id

    override fun navContributor(deps: SakDependencies): FeatureNavContributor =
        { builder, navController ->
            builder.analyticsNavigation(
                onNavigate = { route -> navController.navigate(route) },
                onUpgrade = { navController.navigate("licensing/upgrade") },
                onBack = { navController.popBackStack() },
            )
        }
}
