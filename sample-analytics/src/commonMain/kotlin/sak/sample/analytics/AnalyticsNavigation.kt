package sak.sample.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import sak.sdk.config.FeatureAccess

fun NavGraphBuilder.analyticsNavigation(
    access: FeatureAccess?,
    onNavigate: (String) -> Unit,
    onUpgrade: () -> Unit,
    onBack: () -> Unit,
) {
    composable("analytics/today") {
        AnalyticsTodayScreen(
            access = access,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsTodayScreen(
    access: FeatureAccess?,
    onOpenReport: (String) -> Unit,
    onUpgrade: () -> Unit,
    onBack: () -> Unit,
) {
    val pro = access?.plan == "pro" || access?.grants?.contains("analytics.reports") == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Real-Time Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                },
                actions = {
                    Surface(
                        color = if (pro) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = if (pro) "PRO" else "TRIAL",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (pro) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Revenue Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Total Gross Revenue", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$128,450.00", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text("+18.4%", style = MaterialTheme.typography.labelLarge, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(16.dp))
                    
                    // Simple Simulated Chart Bars
                    Row(
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val barHeights = listOf(0.4f, 0.6f, 0.3f, 0.85f, 0.55f, 0.95f, 0.7f)
                        barHeights.forEach { fraction ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(fraction)
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }

            // Metric Cards Grid
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricWidget(
                    title = "Active Users",
                    value = "14,210",
                    change = "+12%",
                    modifier = Modifier.weight(1f)
                )
                MetricWidget(
                    title = "Avg Session",
                    value = "4m 12s",
                    change = "+5%",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricWidget(
                    title = "Conversions",
                    value = "3,480",
                    change = "+8.2%",
                    modifier = Modifier.weight(1f)
                )
                MetricWidget(
                    title = "Bounce Rate",
                    value = "24.1%",
                    change = "-2.4%",
                    modifier = Modifier.weight(1f)
                )
            }

            // Pro Features Section
            if (pro) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Pro Analytics Suite Active", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("You have unlocked full cohort analysis, weekly reports, and CSV export capabilities.", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onOpenReport("weekly") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Weekly Performance Report")
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Upgrade to Pro Tier", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Unlock granular breakdown reports, export raw metrics, and observe real-time cohort retention.", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onUpgrade,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Upgrade License via SAK Licensing")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsReportScreen(
    reportId: String,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report: $reportId") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Weekly Performance Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Report ID: $reportId • Generated Real-Time", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("• Customer Retention: 84.5%", style = MaterialTheme.typography.bodyMedium)
                    Text("• Top Traffic Channel: Organic Search (45%)", style = MaterialTheme.typography.bodyMedium)
                    Text("• Server Response Latency: 120ms avg", style = MaterialTheme.typography.bodyMedium)
                }
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Dashboard")
            }
        }
    }
}

@Composable
private fun MetricWidget(
    title: String,
    value: String,
    change: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(change, style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
        }
    }
}
