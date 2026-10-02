package sak.sdk.config

import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigMergerTest {
    @Test
    fun mergeLicenseOverlay_updatesFeatures() {
        val default = SakConfig(
            features = mapOf(
                "analytics" to FeatureAccess(enabled = true, plan = "trial"),
                "billing" to FeatureAccess(enabled = false, plan = "none"),
            ),
        )
        val overlay = SakConfig(
            features = mapOf(
                "analytics" to FeatureAccess(plan = "pro", grants = setOf("analytics.reports")),
                "billing" to FeatureAccess(enabled = true, plan = "pro"),
            ),
        )
        val active = default.mergeLicenseOverlay(overlay)
        assertEquals("pro", active.features["analytics"]?.plan)
        assertEquals(setOf("analytics.reports"), active.features["analytics"]?.grants)
        assertEquals(true, active.features["billing"]?.enabled)
    }

    @Test
    fun licenseOverlay_mergesAccess() {
        val overlay = SakConfig(features = mapOf("analytics" to FeatureAccess(plan = "pro")))
        assertEquals("pro", overlay.features["analytics"]?.plan)
    }

    @Test
    fun mergeLicenseOverlay_revokesGrants() {
        val default = SakConfig(
            features = mapOf(
                "analytics" to FeatureAccess(enabled = true, plan = "pro", grants = setOf("analytics.reports", "analytics.export")),
            ),
        )
        val revokedOverlay = SakConfig(
            features = mapOf(
                "analytics" to FeatureAccess(enabled = true, plan = "trial", grants = emptySet()),
            ),
        )
        val active = default.mergeLicenseOverlay(revokedOverlay)
        assertEquals(emptySet(), active.features["analytics"]?.grants)
        assertEquals("trial", active.features["analytics"]?.plan)
    }
}
