package sak.compose.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import sak.compose.ui.shell.DefaultHostShell
import sak.compose.ui.shell.HostShellDestination
import sak.runtime.ResolvedEntry
import sak.runtime.SakRuntime
import sak.sdk.entry.EntryAction
import sak.sdk.navigation.Route

/**
 * Reference Compose shell: bottom bar, scaffold destinations, and feature NavHost routes.
 *
 * Shell tabs are [HostShellDestination] ids — entry points with matching [tags][sak.sdk.entry.EntryPoint.tags]
 * are shown on each tab via [sak.runtime.RenderPlan.entriesForTag].
 */
@Composable
fun SakApp(
    runtime: SakRuntime,
    theme: SakTheme = { content -> MaterialTheme { content() } },
    featureNavContributors: List<FeatureNavContributor> = emptyList(),
    shellDestinations: List<HostShellDestination> = DefaultHostShell.destinations,
) {
    val navController = rememberNavController()
    val renderPlan by runtime.renderPlan.collectAsState()
    val pendingRoute by runtime.navigationRoute.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(pendingRoute) {
        pendingRoute?.let { route ->
            navController.navigate(route.value) {
                launchSingleTop = true
            }
            runtime.clearNavigation()
        }
    }

    theme {
        Scaffold(
            bottomBar = {
                if (shellDestinations.isNotEmpty()) {
                    NavigationBar {
                        shellDestinations.forEach { dest ->
                            NavigationBarItem(
                                selected = currentRoute == scaffoldRoute(dest.id),
                                onClick = {
                                    navController.navigate(scaffoldRoute(dest.id)) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    if (sakBarIconSupported(dest.icon)) {
                                        SakBarIcon(name = dest.icon)
                                    } else {
                                        Text(dest.label.take(1))
                                    }
                                },
                                label = { Text(dest.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = scaffoldRoute(shellDestinations.firstOrNull()?.id ?: "home"),
                modifier = Modifier.padding(padding),
            ) {
                shellDestinations.forEach { dest ->
                    composable(scaffoldRoute(dest.id)) {
                        ScaffoldDestination(
                            destinationId = dest.id,
                            entries = renderPlan.entriesForTag(dest.id),
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
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Entry points tagged for this destination",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No entry points for tag \"$destinationId\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            entries.forEach { resolved ->
                SakEntryWidget(
                    featureId = resolved.featureId,
                    entry = resolved.entry,
                    onClick = {
                        when (val action = resolved.entry.action) {
                            is EntryAction.Navigate -> onNavigate(Route(action.route))
                            is EntryAction.DeepLink -> onNavigate(Route(action.uri))
                            is EntryAction.Command -> Unit
                            EntryAction.None -> Unit
                        }
                    },
                )
            }
        }
    }
}
