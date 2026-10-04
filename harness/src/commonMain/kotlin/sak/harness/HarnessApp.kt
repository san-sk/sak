package sak.harness

import androidx.compose.foundation.clickable
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import sak.harness.demo.DebugDashboardScreen
import sak.harness.demo.HarnessDemoRuntimeRef
import sak.harness.demo.HarnessLogService
import sak.harness.demo.InMemoryHarnessLogService
import sak.harness.demo.harnessDemoNavigation
import sak.runtime.SakRuntime
import sak.runtime.ServiceRegistry
import sak.sample.analytics.analyticsNavigation
import sak.sample.licensing.licensingNavigation
import sak.sdk.config.SakConfig
import sak.sdk.entry.EntryAction
import sak.sdk.entry.EntryPoint
import sak.sdk.entry.EntryPointType
import sak.sdk.entry.EntryStyle
import sak.sdk.feature.Attention
import sak.sdk.feature.FeatureId
import sak.sdk.navigation.Route

/** Feature ID backing the Messages tab. */
private const val DEMO_FEATURE_ID = "messages"

/**
 * Android Reference Harness: Boots SakRuntime and renders features using Jetpack Compose.
 */
@Composable
fun HarnessApp(
    defaultConfig: SakConfig,
    appScope: CoroutineScope = rememberCoroutineScope(),
) {
    var runtime by remember { mutableStateOf<SakRuntime?>(null) }
    val hostLifecycle = LocalLifecycleOwner.current.lifecycle
    val logService = remember { InMemoryHarnessLogService() }

    val serviceRegistry = remember(logService) {
        ServiceRegistry(mapOf(
            HarnessLogService::class to logService,
        ))
    }

    LaunchedEffect(defaultConfig) {
        if (runtime == null) {
            val sakRuntime = SakRuntime(
                baseConfig = defaultConfig,
                featureRegistry = createHarnessRegistry(),
                serviceRegistry = serviceRegistry,
                scope = appScope,
                hostLifecycle = hostLifecycle,
            )
            sakRuntime.start()
            runtime = sakRuntime
        }
    }

    val sakRuntime = runtime
    if (sakRuntime == null) {
        LoadingScreen()
    } else {
        HarnessShell(sakRuntime, appScope)
    }
}

@Composable
fun HarnessShell(
    runtime: SakRuntime,
    appScope: CoroutineScope,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(runtime) {
        runtime.navigationEvents
            .onEach { route ->
                runCatching {
                    navController.navigate(route.value) {
                        launchSingleTop = true
                    }
                }
            }
            .launchIn(this)
    }

    val items = listOf(
        Triple("home", "Home", "🏠"),
        Triple("alerts", "Alerts", "🔔"),
        Triple("settings", "Settings", "⚙️"),
    )

    val topLevelRoutes = setOf("home", "alerts", "settings")
    val showBottomBar = currentDestination?.route in topLevelRoutes

    val activeConfig by runtime.activeConfig.collectAsState()
    val allStatuses by runtime.allStatuses.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { (route, label, iconText) ->
                        val selected = currentDestination?.hierarchy?.any { it.route == route } == true
                        
                        // Check if messages feature has an active Attention badge request (shown under Settings or Home)
                        val attentionLevel = if (route == "settings") {
                            allStatuses[FeatureId(DEMO_FEATURE_ID)]?.attention ?: Attention.None
                        } else {
                            Attention.None
                        }
                        
                        NavigationBarItem(
                            icon = { 
                                BadgedBox(
                                    badge = {
                                        if (attentionLevel == Attention.High || attentionLevel == Attention.Medium) {
                                            Badge { Text("!") }
                                        } else if (attentionLevel == Attention.Low) {
                                            Badge()
                                        }
                                    }
                                ) {
                                    Text(iconText) 
                                }
                            },
                            label = { Text(label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeDashboard(runtime) }
            composable("alerts") { AlertsFeed(runtime) }
            composable("settings") { SettingsScreen(runtime) }

            // Developer tools route backing the debug feature
            composable("sak/debug") {
                DebugDashboardScreen(
                    runtime = runtime,
                    onBack = { navController.popBackStack() }
                )
            }

            analyticsNavigation(
                onNavigate = { runtime.navigate(Route(it)) },
                onBack = { navController.popBackStack() }
            )

            licensingNavigation(
                runtime = runtime,
                allFeatureIds = HarnessFeatureIds.all,
                onBack = { navController.popBackStack() },
            )
            
            // Bridge the Real-World Chat Demo routes
            harnessDemoNavigation(
                runtimeRef = HarnessDemoRuntimeRef(
                    onToggleFeature = { id, enabled ->
                        appScope.launch { runtime.setFeatureEnabled(id, enabled) }
                    },
                    isFeatureEnabled = { id -> activeConfig.isFeatureEnabled(id) }
                ),
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboard(runtime: SakRuntime) {
    val entries by runtime.allEntryPoints.collectAsState()
    val homeWidgets = entries.filter { it.second.type == EntryPointType.Dashboard || it.second.type == EntryPointType.Summary }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Dashboard", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (homeWidgets.isEmpty()) {
                item { EmptyState("Your dashboard is empty", "Enable some plugins in Settings to see widgets here.") }
            } else {
                items(homeWidgets) { (featureId, entry) ->
                    WidgetCard(featureId, entry, onClick = { handleAction(runtime, entry.action) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsFeed(runtime: SakRuntime) {
    val entries by runtime.allEntryPoints.collectAsState()
    val alerts = entries.filter { it.second.type == EntryPointType.Alert }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Activity Feed", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (alerts.isEmpty()) {
                item { EmptyState("No active alerts", "Everything looks good.") }
            } else {
                items(alerts) { (featureId, entry) ->
                    AlertCard(featureId, entry, onClick = { handleAction(runtime, entry.action) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(runtime: SakRuntime) {
    val entries by runtime.allEntryPoints.collectAsState()
    val settings = entries.filter { it.second.type == EntryPointType.Settings || it.second.type == EntryPointType.Action }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (settings.isEmpty()) {
                item { EmptyState("No settings available", "Enable plugins to configure them.") }
            } else {
                items(settings) { (featureId, entry) ->
                    SettingsRow(featureId, entry, onClick = { handleAction(runtime, entry.action) })
                }
            }
        }
    }
}

private fun handleAction(runtime: SakRuntime, action: EntryAction) {
    when (action) {
        is EntryAction.Navigate -> runtime.navigate(Route(action.route))
        is EntryAction.DeepLink -> println("Handling deep link: ${action.uri}")
        is EntryAction.Custom -> println("Handling custom action: ${action.id}")
        EntryAction.None -> {}
    }
}

@Composable
fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📭", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetCard(featureId: FeatureId, entry: EntryPoint, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.style == EntryStyle.Banner) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (entry.icon != null) {
                    Text(emojiFor(entry.icon), modifier = Modifier.padding(end = 8.dp))
                }
                Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (entry.badge != null) {
                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                        Text(entry.badge!!, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
            }
            if (entry.subtitle != null) {
                Spacer(Modifier.height(8.dp))
                Text(entry.subtitle!!, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertCard(featureId: FeatureId, entry: EntryPoint, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emojiFor(entry.icon ?: "warning"), modifier = Modifier.padding(end = 12.dp))
            Column {
                Text(entry.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (entry.subtitle != null) {
                    Text(entry.subtitle!!, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRow(featureId: FeatureId, entry: EntryPoint, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (entry.icon != null) {
                Text(emojiFor(entry.icon), modifier = Modifier.padding(end = 16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (entry.subtitle != null) {
                    Text(entry.subtitle!!, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Text("→", color = Color.Gray)
        }
    }
}

/** Stub native icon mapping for the demo harness. */
private fun emojiFor(iconName: String?): String = when(iconName) {
    "chart_bar" -> "📊"
    "payments" -> "💰"
    "document_chart" -> "📄"
    "warning" -> "⚠️"
    "tune" -> "🎛️"
    "settings" -> "⚙️"
    "bug_report" -> "🐞"
    "extension" -> "🧩"
    "chat_bubble" -> "💬"
    else -> "💠"
}
