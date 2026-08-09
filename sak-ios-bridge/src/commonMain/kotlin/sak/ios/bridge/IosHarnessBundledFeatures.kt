package sak.ios.bridge

import sak.sample.analytics.AnalyticsFeatureModule
import sak.sample.licensing.LicensingFeatureModule
import sak.sdk.bundle.FeatureModule

/** Manual feature module list for iOS Swift harness (no ServiceLoader). */
internal val iosHarnessBundledFeatureModules: List<FeatureModule> = listOf(
    AnalyticsFeatureModule,
    LicensingFeatureModule,
)
