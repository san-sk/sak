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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import sak.harness.demo.DebugDashboardScreen
import sak.harness.demo.HarnessDemoRuntimeRef
import sak.harness.demo.HarnessLogService
import sak.harness.demo.InMemoryHarnessLogService
import sak.harness.demo.harnessDemoNavigation
import sak.runtime.SakRuntime
import sak.runtime.ServiceRegistry
import sak.sample.analytics.analyticsNavigation
import sak.sample.licensing.SampleLicenseProvider
import sak.sample.licensing.licensingNavigation
import sak.sdk.config.SakConfig
import sak.sdk.entry.EntryAction
import sak.sdk.entry.EntryPoint
import sak.sdk.entry.EntryStyle
import sak.sdk.feature.FeatureId
import sak.sdk.navigation.Route

/**
 * Android Reference Harness: Boots SakRuntime and renders features using Jetpack Compose.
 */
@Composable
fun HarnessApp(
    defaultConfig: SakConfig,
    licenseProvider: SampleLicenseProvider = remember { SampleLicenseProvider() },
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

    LaunchedEffect(defaultConfig, licenseProvider) {
        if (runtime == null) {
            val registry = createHarnessRegistry(licenseProvider)
            val sakRuntime = SakRuntime(
                defaultConfig = defaultConfig,
                featureRegistry = registry,
                serviceRegistry = serviceRegistry,
                scope = appScope,
                hostLifecycle = hostLifecycle,
            )
            sakRuntime.start(licenseProvider)
            runtime = sakRuntime
        }
    }

    val sakRuntime = runtime
    if (sakRuntime == null) {
        LoadingScreen()
    } else {
        HarnessShell(sakRuntime, licenseProvider)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HarnessShell(
    runtime: SakRuntime,
    licenseProvider: SampleLicenseProvider,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val pendingRoute by runtime.navigationRoute.collectAsState()
    LaunchedEffect(pendingRoute) {
        val route = pendingRoute ?: return@LaunchedEffect
        navController.navigate(route.value) {
            launchSingleTop = true
        }
        runtime.clearNavigation()
    }

    val items = listOf(
        Triple("home", "Home", "🏠"),
        Triple("alerts", "Alerts", "🔔"),
        Triple("demos", "Demos", "⚡"),
        Triple("settings", "Settings", "⚙️"),
    )

    val activeConfig by runtime.activeConfig.collectAsState()
    val analyticsPlan = activeConfig.features["analytics"]?.plan ?: "trial"
    val isPro = analyticsPlan == "pro"

    LaunchedEffect(runtime) {
        HarnessDemoRuntimeRef.runtime = runtime
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SAK SuperApp Shell", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Protocol-First KMP Micro-Frontend Architecture", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    Surface(
                        color = if (isPro) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (isPro) "PRO ACTIVE" else "TRIAL",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isPro) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    IconButton(onClick = { navController.navigate("sak/debug") }) {
                        Text("🛠️")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        icon = { Text(icon) },
                        label = { Text(label) },
                        selected = currentDestination?.hierarchy?.any { it.route == route } == true,
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                ComposeFeatureSlot(runtime = runtime, category = "home")
            }
            composable("alerts") {
                ComposeFeatureSlot(runtime = runtime, category = "alerts")
            }
            composable("demos") {
                ComposeFeatureSlot(runtime = runtime, category = "demo")
            }
            composable("settings") {
                ComposeFeatureSlot(runtime = runtime, category = "settings")
            }
            composable("sak/debug") {
                DebugDashboardScreen(
                    runtime = runtime,
                    onBack = { navController.popBackStack() }
                )
            }

            // Register feature navigation graphs
            analyticsNavigation(
                access = activeConfig.features["analytics"],
                onNavigate = { route -> runtime.navigate(Route(route)) },
                onUpgrade = { runtime.navigate(Route("licensing/upgrade")) },
                onBack = { navController.popBackStack() },
            )
            licensingNavigation(
                access = activeConfig.features["licensing"],
                licenseProvider = licenseProvider,
                onBack = { navController.popBackStack() },
            )
            harnessDemoNavigation(
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/**
 * Example of how a host app consumes SAK EntryPoints and renders custom Compose UI cards.
 */
@Composable
fun ComposeFeatureSlot(
    runtime: SakRuntime,
    category: String,
    modifier: Modifier = Modifier,
) {
    val activeConfig by runtime.activeConfig.collectAsState()

    val entries by produceState<List<Pair<FeatureId, EntryPoint>>>(initialValue = emptyList(), activeConfig) {
        val activeFeatureIds = activeConfig.features.filterValues { it.enabled && it.visible }.keys.map { FeatureId(it) }
        val activeFeatures = activeFeatureIds.mapNotNull { runtime.getFeature(it) }

        if (activeFeatures.isEmpty()) {
            value = emptyList()
            return@produceState
        }

        combine(activeFeatures.map { feature ->
            feature.entryPoints.map { list -> feature.id to list }
        }) { flowsArray ->
            val resolved = mutableListOf<Pair<FeatureId, EntryPoint>>()
            flowsArray.forEach { (featureId, list) ->
                list.filter { it.categories.contains(category) }
                    .forEach { resolved.add(featureId to it) }
            }
            resolved.sortedByDescending { it.second.priority }
        }.collect { combinedList ->
            value = combinedList
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (entries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No active micro-frontend widgets for '$category'",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Features publish entry points dynamically based on entitlement grants.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(entries) { (featureId, entry) ->
                MicroFrontendCard(
                    featureId = featureId,
                    entry = entry,
                    onTap = {
                        when (val action = entry.action) {
                            is EntryAction.Navigate -> runtime.navigate(Route(action.route))
                            is EntryAction.DeepLink -> runtime.navigate(Route(action.uri))
                            else -> Unit
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MicroFrontendCard(
    featureId: FeatureId,
    entry: EntryPoint,
    onTap: () -> Unit,
) {
    val title = entry.metadata["title"]?.metaString() ?: entry.id.value
    val subtitle = entry.metadata["subtitle"]?.metaString() ?: "Feature: ${featureId.value}"
    val badge = entry.metadata["badge"]?.metaString()
    val valText = entry.metadata["value"]?.metaString()

    when (entry.style) {
        EntryStyle.Banner -> {
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            badge?.let {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiary,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = it,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(subtitle, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = onTap, modifier = Modifier.padding(start = 8.dp)) {
                        Text("Explore")
                    }
                }
            }
        }

        EntryStyle.Row -> {
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            badge?.let {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = it,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("→", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        else -> { // Default Card / Tile / Widget
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        badge?.let {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = it,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    valText?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(text = it, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

private fun JsonElement.metaString(): String? {
    return try {
        if (this is JsonObject) {
            this["v"]?.jsonPrimitive?.content
        } else if (this is JsonPrimitive) {
            this.content
        } else null
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Starting SAK Harness...")
        }
    }
}
