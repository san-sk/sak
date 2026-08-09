package sak.sample.licensing

import sak.compose.ui.FeatureNavigationModule
import sak.compose.ui.FeatureNavContributor
import sak.sdk.bundle.FeatureModule
import sak.sdk.bundle.SakDependencies
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureId

/**
 * Convention: package `sak.sample.licensing` → feature id `licensing`.
 * Registered for Android via META-INF/services.
 */
object LicensingFeatureModule : FeatureModule, FeatureNavigationModule {
    override val id = FeatureId("licensing")

    override fun create(deps: SakDependencies): Feature {
        val provider = deps.licenseProvider as? SampleLicenseProvider
            ?: SampleLicenseProvider()
        return LicensingFeature(provider)
    }

    override val featureId = id

    override fun navContributor(deps: SakDependencies): FeatureNavContributor {
        val provider = deps.licenseProvider as? SampleLicenseProvider
            ?: SampleLicenseProvider()
        return { builder, navController ->
            builder.licensingNavigation(
                licenseProvider = provider,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
