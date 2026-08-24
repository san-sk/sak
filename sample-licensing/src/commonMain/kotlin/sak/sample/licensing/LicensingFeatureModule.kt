package sak.sample.licensing

import sak.compose.ui.FeatureNavigationModule
import sak.compose.ui.FeatureNavContributor
import sak.sdk.bundle.FeatureModule
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureId

/**
 * Licensing feature module.
 *
 * Host should assign [licenseProvider] before launch so it matches
 * [sak.runtime.SakHostSetup.licenseProvider] (same instance for runtime + UI).
 */
object LicensingFeatureModule : FeatureModule, FeatureNavigationModule {
    var feature: LicensingFeature? = null
        private set

    var licenseProvider: SampleLicenseProvider = SampleLicenseProvider()

    override val id = FeatureId("licensing")

    override fun create(): Feature =
        LicensingFeature(licenseProvider).also { feature = it }

    override val featureId = id

    override fun navContributor(): FeatureNavContributor =
        { builder, navController ->
            builder.licensingNavigation(
                licenseProvider = licenseProvider,
                onBack = { navController.popBackStack() },
            )
        }
}
