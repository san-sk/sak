package sak.sample.analytics

import sak.compose.ui.FeatureNavigationModule
import sak.compose.ui.FeatureNavContributor
import sak.sdk.bundle.FeatureModule
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureId

/** Convention: package `sak.sample.analytics` → feature id `analytics`. */
object AnalyticsFeatureModule : FeatureModule, FeatureNavigationModule {
    var feature: AnalyticsFeature? = null
        private set

    override val id = FeatureId("analytics")

    override fun create(): Feature = AnalyticsFeature().also { feature = it }

    override val featureId = id

    override fun navContributor(): FeatureNavContributor =
        { builder, navController ->
            builder.analyticsNavigation(
                onNavigate = { route -> navController.navigate(route) },
                onUpgrade = { navController.navigate("licensing/upgrade") },
                onBack = { navController.popBackStack() },
            )
        }
}
