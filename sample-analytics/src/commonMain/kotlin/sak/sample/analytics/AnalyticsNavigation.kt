package sak.sample.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read

/** Registers analytics routes on the shared SAK NavHost. */
fun NavGraphBuilder.analyticsNavigation(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
) {
    composable("analytics/today") {
        AnalyticsTodayScreen(
            onOpenReport = { reportId -> onNavigate("analytics/report/$reportId") },
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
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Analytics · Today", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Trial summary: 42 events today")
        Button(onClick = { onOpenReport("weekly") }) {
            Text("Open weekly report")
        }
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}

@Composable
fun AnalyticsReportScreen(
    reportId: String,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Report: $reportId", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Pro analytics report content")
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}
