package sak.harness.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sak.runtime.SakRuntime
import sak.sdk.config.FeatureAccess
import sak.sdk.entry.*
import sak.sdk.feature.*
import sak.sdk.navigation.Route

/**
 * A sample feature providing an observability dashboard for SAK features.
 */
class DebugFeature : BaseFeature() {
    override val id = FeatureId("sak-debug")
    override val routes = listOf(Route("sak/debug"))

    override suspend fun onCreate(context: FeatureContext) {
        super.onCreate(context)
        updateStatus(FeatureState.Ready)
        setEntryPoints(listOf(
            EntryPoint(
                id = EntryPointId("debug-dashboard"),
                type = EntryPointType.Action,
                style = EntryStyle.Row,
                action = EntryAction.Navigate("sak/debug"),
                priority = -100,
                categories = setOf("home", "settings")
            )
        ))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugDashboardScreen(runtime: SakRuntime, onBack: () -> Unit) {
    val statuses by runtime.allStatuses.collectAsState()
    val activeConfig by runtime.activeConfig.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SAK Architecture Observability") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Overall System Status Overview
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("System Architecture Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Active Modules: ${statuses.size}", style = MaterialTheme.typography.bodySmall)
                            Text("Config Schema v${activeConfig.version}", style = MaterialTheme.typography.bodySmall)
                            Text("Engine: ONLINE", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Developer Quick Actions
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Dev Controls & Overlay Injector", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        runtime.applyDevLicenseOverlay(
                                            """{"version":1,"features":{"analytics":{"enabled":true,"visible":true,"plan":"pro","grants":["analytics.reports"]}}}"""
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Inject Pro License")
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        runtime.applyDevLicenseOverlay(
                                            """{"version":1,"features":{"analytics":{"enabled":true,"visible":true,"plan":"trial","grants":[]}}}"""
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Inject Trial License")
                            }
                        }
                    }
                }
            }

            item {
                Text("Registered Feature Modules (${statuses.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(statuses.toList()) { (id, status) ->
                FeatureStatusCard(id, status, activeConfig.features[id.value])
            }
        }
    }
}

@Composable
fun FeatureStatusCard(id: FeatureId, status: FeatureStatus, access: FeatureAccess?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when(status.state) {
                is FeatureState.Ready -> MaterialTheme.colorScheme.surfaceVariant
                is FeatureState.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = id.value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                StatusBadge(status.state)
            }
            
            Spacer(Modifier.height(8.dp))
            access?.let {
                Text("Plan: ${it.plan.uppercase()} • Enabled: ${it.enabled} • Visible: ${it.visible}", style = MaterialTheme.typography.bodySmall)
                if (it.grants.isNotEmpty()) {
                    Text("Capability Grants: ${it.grants.joinToString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(state: FeatureState) {
    val color = when(state) {
        is FeatureState.Ready -> Color(0xFF4CAF50)
        is FeatureState.Initializing -> Color(0xFFFFC107)
        is FeatureState.Error -> Color(0xFFF44336)
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = when(state) {
                is FeatureState.Ready -> "READY"
                is FeatureState.Initializing -> "INIT"
                is FeatureState.Error -> "ERROR"
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
