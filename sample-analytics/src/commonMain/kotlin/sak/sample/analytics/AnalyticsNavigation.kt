package sak.sample.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import sak.compose.ui.FieldRow
import sak.compose.ui.InfoCard
import sak.compose.ui.SakFeatureScreen
import sak.compose.ui.SectionCard

/** Registers analytics routes on the shared SAK NavHost. */
fun NavGraphBuilder.analyticsNavigation(
    onNavigate: (String) -> Unit,
    onUpgrade: () -> Unit,
    onBack: () -> Unit,
) {
    composable("analytics/today") {
        AnalyticsTodayScreen(
            onOpenReport = { reportId -> onNavigate("analytics/report/$reportId") },
            onUpgrade = onUpgrade,
            onBack = onBack,
        )
    }
    composable(
        route = "analytics/report/{reportId}",
        arguments = listOf(navArgument("reportId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val reportId = backStackEntry.arguments?.read { getString("reportId") } ?: "unknown"
        AnalyticsReportScreen(reportId = reportId, onBack = onBack)
    }
}

@Composable
fun AnalyticsTodayScreen(
    onOpenReport: (String) -> Unit,
    onUpgrade: () -> Unit,
    onBack: () -> Unit,
) {
    val feature = AnalyticsFeatureModule.feature ?: return
    val entries by feature.entryPoints.collectAsState()
    val pro = entries.any { it.id.value == "report-row" }

    SakFeatureScreen(
        title = "Analytics",
        subtitle = "Feature route · analytics/today",
    ) {
        InfoCard(
            "Plan-driven feature: Home shows the summary-card (always) and the trial-tile " +
                "(trial only); Alerts shows report-row once the license grants analytics.reports.",
        )
        SectionCard("Capabilities (from entry points)") {
            FieldRow("loaded entries", entries.map { it.id.value }.joinToString(", "))
            FieldRow("reports", if (pro) "yes" else "no")
        }
        MetricCard(label = "Events today", value = "42")
        MetricCard(label = "Active users", value = "1.2k")
        Spacer(Modifier.height(4.dp))
        if (pro) {
            Button(
                onClick = { onOpenReport("weekly") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open weekly report")
            }
        } else {
            OutlinedButton(
                onClick = onUpgrade,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Upgrade to Pro for reports")
            }
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Back")
        }
    }
}

@Composable
fun AnalyticsReportScreen(
    reportId: String,
    onBack: () -> Unit,
) {
    SakFeatureScreen(
        title = "Weekly report",
        subtitle = "Feature route · analytics/report/$reportId",
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Report: $reportId",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Pro analytics report content — available because the license grants " +
                        "the analytics.reports capability.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
