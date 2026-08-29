package sak.sdk.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConfigMergerTest {
    @Test
    fun mergeLicenseOverlay_updatesFeatures() {
        val default = AppConfig(
            features = mapOf(
                "analytics" to FeatureToggle(enabled = true, plan = "trial"),
                "billing" to FeatureToggle(enabled = false, plan = "none"),
            ),
        )
        val overlay = AppConfig(
            features = mapOf(
                "analytics" to FeatureToggle(plan = "pro", grants = setOf("analytics.reports")),
                "billing" to FeatureToggle(enabled = true, plan = "pro"),
            ),
        )
        val active = default.mergeLicenseOverlay(overlay)
        assertEquals("pro", active.features["analytics"]?.plan)
        assertEquals(setOf("analytics.reports"), active.features["analytics"]?.grants)
        assertEquals(true, active.features["billing"]?.enabled)
    }

    @Test
    fun licenseOverlay_mergesToggles() {
        val overlay = AppConfig(features = mapOf("analytics" to FeatureToggle(plan = "pro")))
        assertEquals("pro", overlay.features["analytics"]?.plan)
    }
}
