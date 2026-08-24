package sak.sample.analytics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sak.sdk.entry.EntryAction
import sak.sdk.entry.EntryKind
import sak.sdk.entry.EntryPoint
import sak.sdk.entry.EntryPointId
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureContext
import sak.sdk.feature.FeatureId
import sak.sdk.feature.FeatureStatus
import sak.sdk.navigation.Route

/**
 * Sample analytics feature demonstrating capability-driven entry points:
 * - `summary-card` (Card) on tag `home`.
 * - `trial-tile` (Tile) on `home` until pro / reports capability.
 * - `report-row` (Row) on tag `alerts` when `analytics.reports` is granted.
 */
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

    override suspend fun onCreate(context: FeatureContext) {
        publishEntries(context.capabilities.value, context.licenseType.value)
    }

    override suspend fun onDestroy() {
        _entryPoints.value = emptyList()
    }

    override suspend fun onReset() {
        _status.value = FeatureStatus()
    }

    override suspend fun onCapabilitiesChanged(context: FeatureContext) {
        publishEntries(context.capabilities.value, context.licenseType.value)
    }

    private fun publishEntries(capabilities: Set<String>, licenseType: String) {
        val pro = licenseType == "pro" || capabilities.contains("analytics.reports")
        val entries = mutableListOf(
            EntryPoint(
                id = EntryPointId("summary-card"),
                kind = EntryKind.Card,
                tags = setOf("home"),
                action = EntryAction.Navigate("analytics/today"),
                priority = 10,
            ),
        )
        if (!pro) {
            entries += EntryPoint(
                id = EntryPointId("trial-tile"),
                kind = EntryKind.Tile,
                tags = setOf("home"),
                action = EntryAction.Navigate("analytics/today"),
                priority = 3,
            )
        }
        if (pro) {
            entries += EntryPoint(
                id = EntryPointId("report-row"),
                kind = EntryKind.Row,
                tags = setOf("alerts"),
                action = EntryAction.Navigate("analytics/report/weekly"),
                priority = 5,
            )
        }
        _entryPoints.value = entries
    }
}
