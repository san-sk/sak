package sak.sample.analytics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
import sak.sdk.navigation.Route

/**
 * Production-ready sample analytics micro-frontend feature.
 */
class AnalyticsFeature : BaseFeature() {
    override val id = FeatureId("analytics")

    private val _access = MutableStateFlow<FeatureAccess?>(null)
    val access: StateFlow<FeatureAccess?> = _access.asStateFlow()

    override val routes = listOf(
        Route("analytics/today"),
        Route("analytics/report/{reportId}"),
    )

    override suspend fun onCreate(context: FeatureContext) {
        super.onCreate(context)
        _access.value = context.access
        updateStatus(FeatureState.Ready)
        publishEntries(context.access)

        observeGrant("analytics.reports")
            .onEach { publishEntries(context.access) }
            .launchIn(featureScope)
    }

    override suspend fun onConfigChanged(access: FeatureAccess) {
        _access.value = access
        publishEntries(access)
    }

    private fun publishEntries(access: FeatureAccess) {
        val pro = access.plan == "pro" || access.grants.contains("analytics.reports")
        val entries = mutableListOf<EntryPoint>()

        // 1. Core Summary Card for Home Slot
        entries += EntryPoint(
            id = EntryPointId("summary-card"),
            type = EntryPointType.Dashboard,
            style = EntryStyle.Card,
            action = EntryAction.Navigate("analytics/today"),
            priority = 100,
            categories = setOf("home"),
            metadata = mapOf(
                "title" to buildJsonObject { put("v", "Real-Time Traffic & Analytics") },
                "subtitle" to buildJsonObject { put("v", "14.2k active sessions • +18.4% growth") },
                "icon" to buildJsonObject { put("v", "chart_bar") },
                "badge" to buildJsonObject { put("v", if (pro) "PRO UNLOCKED" else "TRIAL TIER") },
                "value" to buildJsonObject { put("v", "$128,450.00") }
            )
        )

        // 2. Trial Banner / Tile when on trial
        if (!pro) {
            entries += EntryPoint(
                id = EntryPointId("trial-tile"),
                type = EntryPointType.Info,
                style = EntryStyle.Tile,
                action = EntryAction.Navigate("analytics/today"),
                priority = 40,
                categories = setOf("home"),
                metadata = mapOf(
                    "title" to buildJsonObject { put("v", "Unlock Pro Reports") },
                    "subtitle" to buildJsonObject { put("v", "Get instant access to weekly revenue breakdowns & automated exports") },
                    "icon" to buildJsonObject { put("v", "sparkles") }
                )
            )
        }

        // 3. Pro Alert Row for Alerts Slot
        if (pro) {
            entries += EntryPoint(
                id = EntryPointId("report-row"),
                type = EntryPointType.Alert,
                style = EntryStyle.Row,
                action = EntryAction.Navigate("analytics/report/weekly"),
                priority = 90,
                categories = setOf("alerts"),
                metadata = mapOf(
                    "title" to buildJsonObject { put("v", "Weekly Performance Report Ready") },
                    "subtitle" to buildJsonObject { put("v", "Conversion rate increased by +4.2% this week") },
                    "icon" to buildJsonObject { put("v", "document_chart") },
                    "badge" to buildJsonObject { put("v", "NEW REPORT") }
                )
            )
        }

        setEntryPoints(entries)
    }
}
