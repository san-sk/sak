package sak.harness

import sak.harness.demo.HarnessDemoFeature
import sak.runtime.FeatureRegistry
import sak.sample.analytics.AnalyticsFeature
import sak.sample.licensing.LicensingFeature
import sak.sdk.feature.FeatureId

/**
 * Registry of bundled sample features for harness app.
 * Uses lazy factory registration to ensure feature classes are only allocated
 * when explicitly enabled by configuration and licensing.
 */
internal fun createHarnessRegistry(): FeatureRegistry =
    FeatureRegistry()
        .registerFactory(FeatureId("analytics")) { AnalyticsFeature() }
        .registerFactory(FeatureId("licensing")) { LicensingFeature() }
        .registerFactory(FeatureId("messages")) { HarnessDemoFeature() }

/**
 * Feature IDs bundled in the harness, used by the feature controls screen.
 */
internal object HarnessFeatureIds {
    val all: Set<String> = setOf("analytics", "licensing", "messages")
}
