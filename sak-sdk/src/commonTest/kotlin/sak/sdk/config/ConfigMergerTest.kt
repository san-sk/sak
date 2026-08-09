package sak.sdk.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConfigMergerTest {
    @Test
    fun mergeLicenseOverlay_updatesFeatures_keepsScaffold() {
        val default = AppConfig(
            scaffold = sak.sdk.layout.ScaffoldConfig(
                bottomBar = sak.sdk.layout.BottomBarConfig(
                    items = listOf(
                        sak.sdk.layout.BottomBarItem("home", "Home", "home", "home"),
                    ),
                ),
            ),
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
        assertEquals(1, active.scaffold?.bottomBar?.items?.size)
    }

    @Test
    fun licenseOverlay_hasNoScaffold() {
        val overlay = AppConfig(features = mapOf("analytics" to FeatureToggle(plan = "pro")))
        assertNull(overlay.scaffold)
    }
}
