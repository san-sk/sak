package sak.compose.ui

/**
 * Optional navigation wiring for a [sak.sdk.bundle.FeatureModule].
 *
 * Implement alongside [FeatureModule] to register feature routes in [SakApp]'s NavHost.
 */
interface FeatureNavigationModule {
    val featureId: sak.sdk.feature.FeatureId

    fun navContributor(): FeatureNavContributor
}
