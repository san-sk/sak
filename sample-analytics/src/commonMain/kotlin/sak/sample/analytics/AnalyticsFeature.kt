package sak.sample.analytics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sak.sdk.config.FeatureToggle
import sak.sdk.entry.EntryAction
import sak.sdk.entry.EntryPoint
import sak.sdk.entry.EntryPointId
import sak.sdk.entry.EntryStyle
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureContext
import sak.sdk.feature.FeatureId
import sak.sdk.feature.FeatureStatus
import sak.sdk.navigation.Route

/**
 * Sample analytics feature demonstrating plan-driven entry points:
 * - `summary-card` (Card) always shown on Home.
 * - `trial-tile` (Tile) shown on Home while on the trial plan.
 * - `report-row` (Row) shown on Alerts once the pro license grants `analytics.reports`.
 */
class AnalyticsFeature : Feature {
    override val id = FeatureId("analytics")

    private val _status = MutableStateFlow(FeatureStatus())
    override val status: StateFlow<FeatureStatus> = _status.asStateFlow()

    private val _entryPoints = MutableStateFlow<List<EntryPoint>>(emptyList())
    override val entryPoints: StateFlow<List<EntryPoint>> = _entryPoints.asStateFlow()

    private val _access = MutableStateFlow<FeatureToggle?>(null)
    val access: StateFlow<FeatureToggle?> = _access.asStateFlow()

    override val routes = listOf(
        Route("analytics/today"),
        Route("analytics/report/{reportId}"),
    )

    override suspend fun onActivated(context: FeatureContext) {
        _access.value = context.access
        publishEntries(context.access)
    }

    override suspend fun onDeactivated() {
        _entryPoints.value = emptyList()
    }

    override suspend fun onReset() {
        _status.value = FeatureStatus()
    }

    override suspend fun onAccessChanged(context: FeatureContext) {
        _access.value = context.access
        publishEntries(context.access)
    }

    private fun publishEntries(access: FeatureToggle) {
        val pro = access.plan == "pro" || access.grants.contains("analytics.reports")
        val entries = mutableListOf(
            EntryPoint(
                id = EntryPointId("summary-card"),
                style = EntryStyle.Card,
                previewKey = "analytics.summary",
                action = EntryAction.Navigate("analytics/today"),
                priority = 10,
                categories = setOf("home"),
            ),
        )
        if (!pro) {
            entries += EntryPoint(
                id = EntryPointId("trial-tile"),
                style = EntryStyle.Tile,
                previewKey = "analytics.trial",
                action = EntryAction.Navigate("analytics/today"),
                priority = 3,
                categories = setOf("home"),
            )
        }
        if (pro) {
            entries += EntryPoint(
                id = EntryPointId("report-row"),
                style = EntryStyle.Row,
                previewKey = "analytics.report",
                action = EntryAction.Navigate("analytics/report/weekly"),
                priority = 5,
                categories = setOf("alerts"),
            )
        }
        _entryPoints.value = entries
    }
}
