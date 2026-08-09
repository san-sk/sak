package sak.harness

import sak.sample.analytics.AnalyticsFeatureModule
import sak.sample.licensing.LicensingFeatureModule
import sak.sdk.bundle.FeatureModule

/**
 * iOS cannot use ServiceLoader — list bundled modules here.
 * On Android, META-INF/services still auto-discovers; this list merges as fallback.
 */
internal val harnessBundledFeatureModules: List<FeatureModule> = listOf(
    AnalyticsFeatureModule,
    LicensingFeatureModule,
)
