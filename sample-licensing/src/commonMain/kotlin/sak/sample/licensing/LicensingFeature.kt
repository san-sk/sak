package sak.sample.licensing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import sak.sdk.config.FeatureAccess
import sak.sdk.entry.EntryAction
import sak.sdk.entry.EntryPoint
import sak.sdk.entry.EntryPointId
import sak.sdk.entry.EntryPointType
import sak.sdk.entry.EntryStyle
import sak.sdk.feature.BaseFeature
import sak.sdk.feature.FeatureContext
import sak.sdk.feature.FeatureId
import sak.sdk.feature.FeatureState
import sak.sdk.license.LicenseProvider
import sak.sdk.navigation.Route
import sak.sdk.service.FeatureService
import sak.sdk.service.featureService

/**
 * Production-ready sample licensing & entitlement feature.
 */
class LicensingFeature(
    private val licenseProvider: SampleLicenseProvider,
) : BaseFeature() {
    override val id = FeatureId("licensing")

    private val _access = MutableStateFlow<FeatureAccess?>(null)
    val access: StateFlow<FeatureAccess?> = _access.asStateFlow()

    override val routes = listOf(Route("licensing/upgrade"))

    override fun services(): Set<FeatureService<*>> =
        setOf(featureService(LicenseProvider::class, licenseProvider))

    override suspend fun onCreate(context: FeatureContext) {
        super.onCreate(context)
        _access.value = context.access
        publishEntries(context.access)
        updateStatus(FeatureState.Ready)
    }

    override suspend fun onConfigChanged(access: FeatureAccess) {
        _access.value = access
        publishEntries(access)
    }

    private fun publishEntries(access: FeatureAccess) {
        val pro = access.plan == "pro"
        val entries = mutableListOf<EntryPoint>()

        // 1. Account Subscription Row for Settings/Home
        entries += EntryPoint(
            id = EntryPointId("plan-row"),
            type = EntryPointType.Settings,
            style = EntryStyle.Row,
            action = EntryAction.Navigate("licensing/upgrade"),
            priority = 80,
            categories = setOf("home", "settings"),
            metadata = mapOf(
                "title" to buildJsonObject { put("v", "Subscription & Entitlements") },
                "subtitle" to buildJsonObject { put("v", if (pro) "Pro Enterprise Tier Active • All Features Granted" else "Free Trial Tier • Tap to Upgrade") },
                "icon" to buildJsonObject { put("v", "key") },
                "badge" to buildJsonObject { put("v", if (pro) "PRO" else "TRIAL") }
            )
        )

        // 2. Pro Widget vs Upgrade Banner for Home
        if (pro) {
            entries += EntryPoint(
                id = EntryPointId("pro-widget"),
                type = EntryPointType.Summary,
                style = EntryStyle.Widget,
                action = EntryAction.Navigate("licensing/upgrade"),
                priority = 85,
                categories = setOf("home"),
                metadata = mapOf(
                    "title" to buildJsonObject { put("v", "Enterprise License Verified") },
                    "subtitle" to buildJsonObject { put("v", "Cryptographic signature: dev-pro • Valid through 2027") },
                    "icon" to buildJsonObject { put("v", "verified") },
                    "badge" to buildJsonObject { put("v", "ACTIVE") }
                )
            )
        } else {
            entries += EntryPoint(
                id = EntryPointId("unlock-banner"),
                type = EntryPointType.Alert,
                style = EntryStyle.Banner,
                action = EntryAction.Navigate("licensing/upgrade"),
                priority = 95,
                categories = setOf("home"),
                metadata = mapOf(
                    "title" to buildJsonObject { put("v", "Upgrade to SAK Enterprise Pro") },
                    "subtitle" to buildJsonObject { put("v", "Unlock advanced analytics reports, unlimited exports, and high-priority SLA") },
                    "icon" to buildJsonObject { put("v", "star") },
                    "badge" to buildJsonObject { put("v", "PROMO 50% OFF") }
                )
            )
        }
        setEntryPoints(entries)
    }
}
