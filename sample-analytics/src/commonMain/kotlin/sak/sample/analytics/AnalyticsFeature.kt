package sak.sample.analytics

import sak.sdk.entry.EntryAction
import sak.sdk.entry.EntryPoint
import sak.sdk.entry.EntryPointId
import sak.sdk.entry.EntryPointType
import sak.sdk.entry.EntryStyle
import sak.sdk.feature.BaseFeature
import sak.sdk.feature.FeatureContext
import sak.sdk.feature.FeatureId
import sak.sdk.feature.FeatureState

/**
 * Sample analytics feature.
 *
 * Publishes entry point descriptors with descriptive details. The host app decides
 * where and how these are rendered.
 */
class AnalyticsFeature : BaseFeature() {
    override val id = FeatureId("analytics")

    override suspend fun onCreate(context: FeatureContext) {
        super.onCreate(context)
        publishEntries()
        updateStatus(FeatureState.Ready)
    }

    override suspend fun onDestroy() {
        super.onDestroy()
    }

    private fun publishEntries() {
        setEntryPoints(
            listOf(
                EntryPoint(
                    id = EntryPointId("summary-card"),
                    type = EntryPointType.Dashboard,
                    style = EntryStyle.Card,
                    action = EntryAction.Navigate("analytics/today"),
                    priority = 100,
                    title = "Real-Time Traffic & Analytics",
                    subtitle = "14.2k active sessions • +18.4% growth",
                    icon = "chart_bar",
                    badge = "LIVE",
                ),
                EntryPoint(
                    id = EntryPointId("revenue-card"),
                    type = EntryPointType.Summary,
                    style = EntryStyle.Widget,
                    action = EntryAction.Navigate("analytics/today"),
                    priority = 90,
                    title = "Revenue Today",
                    subtitle = "Across all channels",
                    icon = "payments",
                ),
                EntryPoint(
                    id = EntryPointId("report-row"),
                    type = EntryPointType.Card,
                    style = EntryStyle.Row,
                    action = EntryAction.Navigate("analytics/report/weekly"),
                    priority = 70,
                    title = "Weekly Performance Report",
                    subtitle = "Conversion rate increased by +4.2% this week",
                    icon = "document_chart",
                    badge = "NEW",
                ),
                EntryPoint(
                    id = EntryPointId("alerts-row"),
                    type = EntryPointType.Alert,
                    style = EntryStyle.Row,
                    action = EntryAction.Navigate("analytics/today"),
                    priority = 95,
                    title = "Traffic Spike Detected",
                    subtitle = "Unusual activity detected in the last hour",
                    icon = "warning",
                    badge = "ALERT",
                ),
            )
        )
    }
}
