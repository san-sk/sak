package sak.ios.bridge

import sak.sample.analytics.AnalyticsFeatureModule
import sak.sample.licensing.LicensingFeatureModule
import sak.sample.licensing.SampleLicenseProvider
import sak.sdk.bundle.FeatureModule

internal fun iosHarnessBundledFeatureModules(licenseProvider: SampleLicenseProvider): List<FeatureModule> {
    LicensingFeatureModule.licenseProvider = licenseProvider
    return listOf(
        AnalyticsFeatureModule,
        LicensingFeatureModule,
    )
}
