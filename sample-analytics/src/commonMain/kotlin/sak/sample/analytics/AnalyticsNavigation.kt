package sak.sample.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsTodayScreen(
    onOpenReport: (String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Real-Time Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Total Gross Revenue", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$128,450.00", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(12.dp))
                        Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) {
                            Text("↑ 18.4%", style = MaterialTheme.typography.labelMedium, color = Color(0xFF2E7D32), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    
                    // Simple Simulated Chart Bars
                    Row(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val barHeights = listOf(0.3f, 0.5f, 0.4f, 0.7f, 0.6f, 0.85f, 1.0f)
                        barHeights.forEachIndexed { index, fraction ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(fraction)
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (index == barHeights.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
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
                    trend = "↑ 12%",
                    isPositive = true,
                    modifier = Modifier.weight(1f)
                )
                MetricWidget(
                    title = "Avg Session",
                    value = "4m 12s",
                    trend = "↑ 5%",
                    isPositive = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricWidget(
                    title = "Conversions",
                    value = "3,480",
                    trend = "↑ 8.2%",
                    isPositive = true,
                    modifier = Modifier.weight(1f)
                )
                MetricWidget(
                    title = "Bounce Rate",
                    value = "24.1%",
                    trend = "↓ 2.4%",
                    isPositive = false, // Lower bounce rate is better, but styling it red for demo
                    modifier = Modifier.weight(1f)
                )
            }

            // Reports Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.width(8.dp))
                        Text("Weekly Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Your cohort analysis and CSV exports are ready for review.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onOpenReport("weekly") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("View Full Report")
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
                title = { Text("Report: ${reportId.replaceFirstChar { it.uppercase() }}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Executive Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Generated Real-Time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    Text("• Customer Retention: 84.5%", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("• Top Traffic Channel: Organic Search (45%)", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("• Server Response Latency: 120ms avg", style = MaterialTheme.typography.bodyLarge)
                }
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("Back to Dashboard")
            }
        }
    }
}

@Composable
private fun MetricWidget(
    title: String,
    value: String,
    trend: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(
                trend, 
                style = MaterialTheme.typography.labelMedium, 
                color = if (isPositive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error, 
                fontWeight = FontWeight.Bold
            )
        }
    }
}
