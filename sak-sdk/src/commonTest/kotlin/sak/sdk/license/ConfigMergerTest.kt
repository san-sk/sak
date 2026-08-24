package sak.sdk.license

import sak.sdk.config.AppConfig
import sak.sdk.config.ConfigJson
import sak.sdk.config.mergeEntitlementOverlay
import sak.sdk.feature.FeatureConfig
import sak.sdk.feature.FeatureState
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigMergerTest {
    @Test
    fun mergeEntitlementOverlay_patchesListedFeatures() {
        val default = AppConfig(
            license = LicenseInfo(type = "guest"),
            features = mapOf(
                "analytics" to FeatureConfig(
                    state = FeatureState.Enabled,
                    capabilities = setOf("analytics.summary"),
                ),
                "vpn" to FeatureConfig(state = FeatureState.Hidden),
            ),
        )
        val overlay = AppConfig(
            license = LicenseInfo(type = "pro", rev = "rev-1"),
            features = mapOf(
                "analytics" to FeatureConfig(
                    state = FeatureState.Enabled,
                    capabilities = setOf("analytics.summary", "analytics.reports"),
                ),
            ),
        )
        val active = default.mergeEntitlementOverlay(overlay)
        assertEquals("pro", active.license.type)
        assertEquals(setOf("analytics.reports"), active.features["analytics"]?.capabilities?.minus("analytics.summary"))
        assertEquals(FeatureState.Hidden, active.features["vpn"]?.state)
    }

    @Test
    fun mergeEntitlementOverlay_mergesSettings() {
        val default = AppConfig(
            features = mapOf(
                "analytics" to FeatureConfig(settings = mapOf("a" to ConfigJson.parseToJsonElement("1"))),
            ),
        )
        val overlay = AppConfig(
            features = mapOf(
                "analytics" to FeatureConfig(settings = mapOf("b" to ConfigJson.parseToJsonElement("2"))),
            ),
        )
        val active = default.mergeEntitlementOverlay(overlay)
        assertEquals(2, active.features["analytics"]?.settings?.size)
    }

    @Test
    fun overlay_canDisableFeature() {
        val default = AppConfig(
            features = mapOf("analytics" to FeatureConfig(state = FeatureState.Enabled)),
        )
        val overlay = AppConfig(
            features = mapOf("analytics" to FeatureConfig(state = FeatureState.Disabled)),
        )
        val active = default.mergeEntitlementOverlay(overlay)
        assertEquals(FeatureState.Disabled, active.features["analytics"]?.state)
    }
}
