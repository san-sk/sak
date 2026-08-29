package sak.compose.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import sak.runtime.SakRuntime
import sak.sdk.entry.EntryAction
import sak.sdk.entry.EntryPoint
import sak.sdk.feature.FeatureId
import sak.sdk.navigation.Route

/**
 * Basic shell helper for features.
 *
 * Observe [sak.runtime.SakRuntime.navigationRoute] and handle feature navigation.
 */
@Composable
fun SakApp(
    runtime: SakRuntime,
    navController: NavHostController = rememberNavController(),
    theme: SakTheme = { content -> MaterialTheme { content() } },
    content: @Composable (padding: androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    val pendingRoute by runtime.navigationRoute.collectAsState()

    LaunchedEffect(pendingRoute) {
        val route = pendingRoute ?: return@LaunchedEffect

        // Wait for graph to be set to avoid "Navigation graph has not been set" crash
        snapshotFlow {
            runCatching { navController.graph }.getOrNull()
        }.filterNotNull().first()

        navController.navigate(route.value) {
            launchSingleTop = true
        }
        runtime.clearNavigation()
    }

    theme {
        Scaffold { padding ->
            content(padding)
        }
    }
}

@Composable
fun SakFeaturePlaceHolder(
    runtime: SakRuntime,
    category: String,
    onNavigate: (Route) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeConfig by runtime.activeConfig.collectAsState()
    
    // Resolve entries reactively. For Lazy features, this will update when they load.
    val entries by produceState<List<Pair<FeatureId, EntryPoint>>>(initialValue = emptyList(), activeConfig) {
        val resolved = mutableListOf<Pair<FeatureId, EntryPoint>>()
        activeConfig.features.keys.forEach { idVal ->
            val id = FeatureId(idVal)
            // Trigger lazy load and wait if enabled
            val feature = runtime.getFeature(id)
            if (feature != null) {
                feature.entryPoints.value
                    .filter { it.categories.contains(category) }
                    .forEach { resolved.add(id to it) }
            }
        }
        value = resolved.sortedByDescending { it.second.priority }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No entry points for $category",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            entries.forEach { pair ->
                val featureId = pair.first
                val entry = pair.second
                val feature = runtime.getFeatureOrNull(featureId)
                SakEntryWidget(
                    feature = feature,
                    entry = entry,
                    onClick = {
                        when (val action = entry.action) {
                            is EntryAction.Navigate -> onNavigate(Route(action.route))
                            is EntryAction.DeepLink -> onNavigate(Route(action.uri))
                            is EntryAction.Custom -> { /* Host handles custom */ }
                            EntryAction.None -> Unit
                        }
                    },
                )
            }
        }
    }
}
