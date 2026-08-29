package sak.harness.demo

import sak.compose.ui.FeatureNavigationModule
import sak.compose.ui.FeatureNavContributor
import sak.sdk.bundle.FeatureModule
import sak.sdk.bundle.SakDependencies
import sak.sdk.bundle.SakModule
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureId

/** Registers the SAK capability demos as a bundled feature module. */
@SakModule
object HarnessDemoFeatureModule : FeatureModule, FeatureNavigationModule {
    /** Last created demo feature instance — demo screens read its StateFlows. */
    var feature: HarnessDemoFeature? = null
        private set

    override val id = FeatureId("demos")
    override val featureId = id

    override fun create(deps: SakDependencies): Feature =
        HarnessDemoFeature().also { feature = it }

    override fun navContributor(deps: SakDependencies): FeatureNavContributor =
        { builder, navController ->
            builder.harnessDemoNavigation(onBack = { navController.popBackStack() })
        }
}
