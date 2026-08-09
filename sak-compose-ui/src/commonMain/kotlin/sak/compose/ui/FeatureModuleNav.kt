package sak.compose.ui

import sak.sdk.bundle.FeatureModule
import sak.sdk.bundle.SakDependencies

/** Collect [FeatureNavContributor] lambdas from modules that implement [FeatureNavigationModule]. */
fun List<FeatureModule>.navContributors(deps: SakDependencies): List<FeatureNavContributor> =
    mapNotNull { module ->
        (module as? FeatureNavigationModule)?.navContributor(deps)
    }
