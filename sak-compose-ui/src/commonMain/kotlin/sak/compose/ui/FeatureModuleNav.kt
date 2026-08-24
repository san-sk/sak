package sak.compose.ui

import sak.sdk.bundle.FeatureModule

/** Collect [FeatureNavContributor] lambdas from modules that implement [FeatureNavigationModule]. */
fun List<FeatureModule>.navContributors(): List<FeatureNavContributor> =
    mapNotNull { module ->
        (module as? FeatureNavigationModule)?.navContributor()
    }
