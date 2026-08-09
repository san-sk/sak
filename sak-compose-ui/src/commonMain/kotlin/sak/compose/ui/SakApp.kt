package sak.compose.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import sak.runtime.ResolvedEntry
import sak.runtime.SakRuntime
import sak.sdk.entry.EntryAction
import sak.sdk.navigation.Route

/**
 * Reference Compose shell: bottom bar, scaffold destinations, and feature NavHost routes.
 *
 * Observe [sak.runtime.SakRuntime.renderPlan] and [sak.runtime.SakRuntime.navigationRoute].
 * Pass [featureNavContributors] from [navContributors] on bundled modules.
 */
@Composable
fun SakApp(
    runtime: SakRuntime,
    theme: SakTheme = { content -> MaterialTheme { content() } },
    featureNavContributors: List<FeatureNavContributor> = emptyList(),
) {
    val navController = rememberNavController()
    val renderPlan by runtime.renderPlan.collectAsState()
    val pendingRoute by runtime.navigationRoute.collectAsState()

    pendingRoute?.let { route ->
        navController.navigate(route.value) {
            launchSingleTop = true
        }
        runtime.clearNavigation()
    }

    theme {
        Scaffold(
            bottomBar = {
                val items = renderPlan.scaffold.bottomBar.items
                if (items.isNotEmpty()) {
                    NavigationBar {
                        items.forEach { item ->
                            NavigationBarItem(
                                selected = false,
                                onClick = { navController.navigate(scaffoldRoute(item.destination)) },
                                icon = { Text(item.icon.take(1)) },
                                label = { Text(item.label) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = scaffoldRoute("home"),
                modifier = Modifier.padding(padding),
            ) {
                renderPlan.scaffold.destinations.keys.forEach { destinationId ->
                    composable(scaffoldRoute(destinationId)) {
                        ScaffoldDestination(
                            destinationId = destinationId,
                            entries = renderPlan.destinations[destinationId] ?: emptyList(),
                            onNavigate = { route -> navController.navigate(route.value) },
                        )
                    }
                }
                featureNavContributors.forEach { contributor ->
                    contributor(this, navController)
                }
            }
        }
    }
}

private fun scaffoldRoute(destinationId: String): String = "scaffold/$destinationId"

@Composable
private fun ScaffoldDestination(
    destinationId: String,
    entries: List<ResolvedEntry>,
    onNavigate: (Route) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = destinationId.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.headlineSmall,
        )
        entries.forEach { resolved ->
            EntryPointCard(
                resolved = resolved,
                onNavigate = onNavigate,
            )
        }
    }
}

@Composable
private fun EntryPointCard(
    resolved: ResolvedEntry,
    onNavigate: (Route) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                when (val action = resolved.entry.action) {
                    is EntryAction.Navigate -> onNavigate(Route(action.route))
                    is EntryAction.DeepLink -> onNavigate(Route(action.uri))
                    EntryAction.None -> Unit
                }
            },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${resolved.featureId.value} · ${resolved.entry.id.value}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = resolved.entry.style.name,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
