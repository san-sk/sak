package sak.harness

import sak.harness.demo.HarnessDemoFeatureModule
import sak.sample.analytics.AnalyticsFeatureModule
import sak.sample.licensing.LicensingFeatureModule
import sak.sample.licensing.SampleLicenseProvider
import sak.sdk.bundle.FeatureModule

/**
 * Manual module list (required on iOS; merges with ServiceLoader on Android).
 * Call [wireHarnessModules] so licensing shares the host's [SampleLicenseProvider].
 */
internal fun harnessBundledFeatureModules(licenseProvider: SampleLicenseProvider): List<FeatureModule> {
    LicensingFeatureModule.licenseProvider = licenseProvider
    return listOf(
        AnalyticsFeatureModule,
        LicensingFeatureModule,
        HarnessDemoFeatureModule,
    )
}
