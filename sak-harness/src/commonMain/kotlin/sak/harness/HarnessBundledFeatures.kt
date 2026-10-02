package sak.harness

import sak.harness.demo.DebugFeature
import sak.harness.demo.HarnessDemoFeature
import sak.runtime.FeatureRegistry
import sak.sample.analytics.AnalyticsFeature
import sak.sample.licensing.LicensingFeature
import sak.sample.licensing.SampleLicenseProvider

/**
 * Registry of bundled sample features for harness app.
 */
internal fun createHarnessRegistry(licenseProvider: SampleLicenseProvider): FeatureRegistry =
    FeatureRegistry.of(
        AnalyticsFeature(),
        LicensingFeature(licenseProvider),
        HarnessDemoFeature(),
        DebugFeature(),
    )
