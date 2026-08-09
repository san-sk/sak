package sak.sample.analytics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sak.sdk.entry.EntryAction
import sak.sdk.entry.EntryPoint
import sak.sdk.entry.EntryPointId
import sak.sdk.entry.EntryStyle
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureContext
import sak.sdk.feature.FeatureId
import sak.sdk.feature.FeatureStatus
import sak.sdk.navigation.Route

/** Demo analytics feature: summary entry, today/report routes, trial vs pro grants. */
class AnalyticsFeature : Feature {
    override val id = FeatureId("analytics")

    private val _status = MutableStateFlow(FeatureStatus())
    override val status: StateFlow<FeatureStatus> = _status.asStateFlow()

    private val _entryPoints = MutableStateFlow<List<EntryPoint>>(emptyList())
    override val entryPoints: StateFlow<List<EntryPoint>> = _entryPoints.asStateFlow()

    override val routes = listOf(
        Route("analytics/today"),
        Route("analytics/report/{reportId}"),
    )

    override suspend fun onActivated(context: FeatureContext) {
        publishEntries(context.access)
    }

    override suspend fun onDeactivated() {
        _entryPoints.value = emptyList()
    }

    override suspend fun onReset() {
        _status.value = FeatureStatus()
    }

    override suspend fun onAccessChanged(context: FeatureContext) {
        publishEntries(context.access)
    }

    private fun publishEntries(access: sak.sdk.config.FeatureToggle) {
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
        if (access.plan == "pro" || access.grants.contains("analytics.reports")) {
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
