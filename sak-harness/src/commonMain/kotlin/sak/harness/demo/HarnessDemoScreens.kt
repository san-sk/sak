package sak.harness.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.coroutines.launch
import sak.sdk.entry.EntryAction
import sak.sdk.entry.EntryPoint
import sak.sdk.entry.EntryPointId
import sak.sdk.entry.EntryStyle
import sak.sdk.feature.Attention
import sak.sdk.feature.FeatureState
import sak.sdk.navigation.Route
import sak.sample.licensing.SampleLicenseProvider
import sak.sdk.feature.FeatureId

/** Registers all demo routes on the shared SAK NavHost. */
fun NavGraphBuilder.harnessDemoNavigation(onBack: () -> Unit) {
    composable("demos/config") { ConfigDemoScreen(onBack) }
    composable("demos/lifecycle") { LifecycleDemoScreen(onBack) }
    composable("demos/entrystyles") { EntryStylesDemoScreen(onBack) }
    composable("demos/access") { AccessDemoScreen(onBack) }
    composable("demos/api") { ApiDemoScreen(onBack) }
    composable("demos/service") { ServiceDemoScreen(onBack) }
    composable("demos/license") { LicenseDemoScreen(onBack) }
    composable("demos/routes") { RoutesDemoScreen(onBack) }
}

@Composable
fun ConfigDemoScreen(onBack: () -> Unit) {
    val runtime = HarnessDemoRuntimeRef.runtime ?: return
    val activeConfig by runtime.activeConfig.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Config-driven logic", style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                activeConfig.features.forEach { (id, access) ->
                    Text("$id: enabled=${access.enabled} · plan=${access.plan}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

private fun getDemoFeature(): HarnessDemoFeature? =
    HarnessDemoRuntimeRef.runtime?.getFeatureOrNull(FeatureId("demos")) as? HarnessDemoFeature

@Composable
fun LifecycleDemoScreen(onBack: () -> Unit) {
    val feature = getDemoFeature() ?: return
    val status by feature.status.collectAsState()
    val log by feature.log.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Feature Lifecycle", style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("state: ${status.state}", style = MaterialTheme.typography.bodySmall)
                Text("attention: ${status.attention}", style = MaterialTheme.typography.bodySmall)
            }
        }
        Button(
            onClick = { feature.setSetupComplete(status.state !is FeatureState.Ready) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (status.state is FeatureState.Ready) "Mark setup incomplete" else "Mark setup complete")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Attention.entries.forEach { attention ->
                OutlinedButton(
                    onClick = { feature.setAttention(attention) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(attention.name)
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Callback log:", style = MaterialTheme.typography.titleSmall)
                if (log.isEmpty()) {
                    Text("No callbacks yet", style = MaterialTheme.typography.bodySmall)
                } else {
                    log.forEach { line -> Text("· $line", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
fun EntryStylesDemoScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Entry-point styles", style = MaterialTheme.typography.headlineMedium)
        EntryStyle.entries.forEach { style ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text("EntryStyle.${style.name}", modifier = Modifier.padding(16.dp))
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
fun AccessDemoScreen(onBack: () -> Unit) {
    val feature = getDemoFeature() ?: return
    val access by feature.access.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Feature Access", style = MaterialTheme.typography.headlineMedium)
        val acc = access ?: return@Column
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("enabled: ${acc.enabled}", style = MaterialTheme.typography.bodySmall)
                Text("plan: ${acc.plan}", style = MaterialTheme.typography.bodySmall)
                Text("grants: ${acc.grants.joinToString()}", style = MaterialTheme.typography.bodySmall)
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
fun ApiDemoScreen(onBack: () -> Unit) {
    val feature = getDemoFeature() ?: return
    var result by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Feature APIs", style = MaterialTheme.typography.headlineMedium)
        Button(
            onClick = {
                val provider = feature.findLicenseProvider()
                result = if (provider != null) {
                    "Found LicenseProvider → ${provider::class.simpleName}"
                } else {
                    "Not found — licensing feature not active"
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Look up LicenseProvider")
        }
        if (result.isNotEmpty()) {
            Text(result, style = MaterialTheme.typography.bodyMedium)
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
fun ServiceDemoScreen(onBack: () -> Unit) {
    val feature = getDemoFeature() ?: return
    val service = feature.logService()
    val messages by (service?.messages?.collectAsState() ?: remember { mutableStateOf(emptyList()) })

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("App Services", style = MaterialTheme.typography.headlineMedium)
        Button(
            onClick = { service?.log("log entry") },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Write log entry")
        }
        OutlinedButton(onClick = { service?.clear() }, modifier = Modifier.fillMaxWidth()) {
            Text("Clear log")
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Log messages (${messages.size}):", style = MaterialTheme.typography.titleSmall)
                messages.forEach { msg -> Text("· $msg", style = MaterialTheme.typography.bodySmall) }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
fun LicenseDemoScreen(onBack: () -> Unit) {
    val runtime = HarnessDemoRuntimeRef.runtime ?: return
    val scope = rememberCoroutineScope()
    val activeConfig by runtime.activeConfig.collectAsState()
    val feature = getDemoFeature()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("License Overlay", style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Analytics plan: ${activeConfig.features["analytics"]?.plan ?: "-"}", style = MaterialTheme.typography.bodySmall)
                Text("Licensing plan: ${activeConfig.features["licensing"]?.plan ?: "-"}", style = MaterialTheme.typography.bodySmall)
            }
        }
        Button(
            onClick = { scope.launch { runtime.applyDevLicenseOverlay(proAnalyticsOverlay) } },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Apply dev overlay (analytics → pro)")
        }
        OutlinedButton(
            onClick = {
                (feature?.findLicenseProvider() as? SampleLicenseProvider)?.unlockPro()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Unlock Pro via licensing feature API")
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
fun RoutesDemoScreen(onBack: () -> Unit) {
    val runtime = HarnessDemoRuntimeRef.runtime ?: return

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Routes & Navigation", style = MaterialTheme.typography.headlineMedium)
        Button(
            onClick = { runtime.navigate(Route("analytics/today")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Navigate → analytics/today")
        }
        Button(
            onClick = { runtime.navigate(Route("licensing/upgrade")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Navigate → licensing/upgrade")
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

private val proAnalyticsOverlay =
    """
    {
      "version": 1,
      "features": {
        "analytics": {
          "enabled": true,
          "visible": true,
          "plan": "pro",
          "grants": ["analytics.summary", "analytics.reports"],
          "entries": { "allowed": ["summary-card", "report-row"] }
        }
      }
    }
    """.trimIndent()
