package sak.compose.ui

import sak.sdk.bundle.SakDependencies

/**
 * Optional navigation wiring for a [sak.sdk.bundle.FeatureModule].
 *
 * Implement alongside [FeatureModule] to register feature routes in [SakApp]'s NavHost.
 */
interface FeatureNavigationModule {
    val featureId: sak.sdk.feature.FeatureId
    /** NavGraphBuilder extension for this feature's screens. */
    fun navContributor(deps: SakDependencies): FeatureNavContributor
}
