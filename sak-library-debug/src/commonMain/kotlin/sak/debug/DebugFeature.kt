package sak.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sak.compose.ui.ComposeFeature
import sak.sdk.entry.*
import sak.sdk.feature.*
import sak.sdk.navigation.Route
import sak.runtime.SakRuntime

/**
 * A feature that provides an observability dashboard for all loaded SAK features.
 */
class DebugFeature : Feature, ComposeFeature {
    override val id = FeatureId("sak-debug")

    private val _status = MutableStateFlow(FeatureStatus(state = FeatureState.Ready))
    override val status: StateFlow<FeatureStatus> = _status.asStateFlow()

    private val _entryPoints = MutableStateFlow<List<EntryPoint>>(emptyList())
    override val entryPoints: StateFlow<List<EntryPoint>> = _entryPoints.asStateFlow()

    override val routes = listOf(Route("sak/debug"))

    private var runtime: SakRuntime? = null

    override suspend fun onCreate(context: FeatureContext) {
        // We expect the host to provide SakRuntime as a service for the debug feature
        runtime = context.services.find(SakRuntime::class)
    }

    override suspend fun onActivated(context: FeatureContext) {
        _entryPoints.value = listOf(
            EntryPoint(
                id = EntryPointId("debug-dashboard"),
                type = "action",
                style = EntryStyle.Row,
                action = EntryAction.Navigate("sak/debug"),
                priority = -100, // Show at the bottom
                categories = setOf("home", "settings")
            )
        )
    }

    override suspend fun onDeactivated() {
        _entryPoints.value = emptyList()
    }

    override suspend fun onDestroy() {
        runtime = null
    }

    @Composable
    override fun EntryPointView(id: EntryPointId, modifier: Modifier) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🛠 SAK Debug Dashboard", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text("Open", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun DebugDashboardScreen(runtime: SakRuntime, onBack: () -> Unit) {
    val statuses by runtime.allStatuses.collectAsState()
    val activeConfig by runtime.activeConfig.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SAK Observability") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Global Config Version: ${activeConfig.version}", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
            }

            items(statuses.toList()) { (id, status) ->
                FeatureStatusCard(id, status, activeConfig.features[id.value])
            }
        }
    }
}

@Composable
fun FeatureStatusCard(id: FeatureId, status: FeatureStatus, toggle: sak.sdk.config.FeatureToggle?) {
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
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = id.value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                StatusBadge(status.state)
            }
            
            toggle?.let {
                Text("Plan: ${it.plan}", style = MaterialTheme.typography.bodySmall)
                if (it.grants.isNotEmpty()) {
                    Text("Grants: ${it.grants.joinToString()}", style = MaterialTheme.typography.bodySmall)
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
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = when(state) {
                is FeatureState.Ready -> "READY"
                is FeatureState.Initializing -> "INIT"
                is FeatureState.Error -> "ERROR"
            },
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
