package sak.harness.demo

import sak.compose.ui.FeatureNavigationModule
import sak.compose.ui.FeatureNavContributor
import sak.sdk.bundle.FeatureModule
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureId

object HarnessDemoFeatureModule : FeatureModule, FeatureNavigationModule {
    var feature: HarnessDemoFeature? = null
        private set

    override val id = FeatureId("demos")
    override val featureId = id

    override fun create(): Feature =
        HarnessDemoFeature().also { feature = it }

    override fun navContributor(): FeatureNavContributor =
        { builder, navController ->
            builder.harnessDemoNavigation(onBack = { navController.popBackStack() })
        }
}
