package sak.harness.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.coroutines.launch
import sak.compose.ui.FieldRow
import sak.compose.ui.InfoCard
import sak.compose.ui.SakEntryWidget
import sak.compose.ui.SakFeatureScreen
import sak.compose.ui.SectionCard
import sak.compose.ui.shell.DefaultHostShell
import sak.sdk.entry.EntryAction
import sak.sdk.entry.EntryKind
import sak.sdk.entry.EntryPoint
import sak.sdk.entry.EntryPointId
import sak.sdk.feature.FeatureId
import sak.sdk.feature.IntroState
import sak.sdk.navigation.Route
import sak.sample.licensing.SampleLicenseProvider

/** Registers all demo routes on the shared SAK NavHost. */
fun NavGraphBuilder.harnessDemoNavigation(onBack: () -> Unit) {
    composable("demos/config") { ConfigDemoScreen(onBack) }
    composable("demos/lifecycle") { LifecycleDemoScreen(onBack) }
    composable("demos/entrystyles") { EntryKindsDemoScreen(onBack) }
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
    val renderPlan by runtime.renderPlan.collectAsState()

    SakFeatureScreen(
        title = "Entitlement config",
        subtitle = "AppConfig JSON drives load policy & capabilities",
    ) {
        SectionCard("License (display only)") {
            FieldRow("type", activeConfig.license.type)
            FieldRow("rev", activeConfig.license.rev ?: "—")
        }
        SectionCard("Host shell (not in JSON)") {
            DefaultHostShell.destinations.forEach { dest ->
                FieldRow(dest.id, dest.label)
            }
        }
        SectionCard("Entry catalog") {
            FieldRow("total entries", renderPlan.entries.size.toString())
            renderPlan.byTag.forEach { (tag, entries) ->
                FieldRow(tag.ifEmpty { "(untagged)" }, entries.size.toString())
            }
        }
        SectionCard("Feature rows") {
            activeConfig.features.forEach { (id, row) ->
                FieldRow(id, "state=${row.state.name} · caps=${row.capabilities.size}")
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
fun LifecycleDemoScreen(onBack: () -> Unit) {
    val feature = HarnessDemoFeatureModule.feature ?: return
    val status by feature.status.collectAsState()
    val log by feature.log.collectAsState()

    SakFeatureScreen(
        title = "Feature lifecycle",
        subtitle = "onCreate / onCapabilitiesChanged / onDestroy / onReset",
    ) {
        SectionCard("FeatureStatus") {
            FieldRow("setupComplete", status.setupComplete.toString())
            FieldRow("intro", status.intro.toString())
        }
        SectionCard("Controls") {
            Button(
                onClick = { feature.setSetupComplete(!status.setupComplete) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (status.setupComplete) "Mark setup incomplete" else "Mark setup complete")
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntroState.entries.forEach { intro ->
                    OutlinedButton(
                        onClick = { feature.setIntroState(intro) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(intro.name)
                    }
                }
            }
        }
        SectionCard("Callback log") {
            if (log.isEmpty()) {
                Text("No callbacks yet", style = MaterialTheme.typography.bodySmall)
            } else {
                log.forEach { line -> Text("· $line", style = MaterialTheme.typography.bodySmall) }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
fun EntryKindsDemoScreen(onBack: () -> Unit) {
    val featureId = HarnessDemoFeatureModule.feature?.id ?: FeatureId("demos")

    SakFeatureScreen(
        title = "Entry kinds",
        subtitle = "EntryKind drives SakEntryWidget rendering",
    ) {
        InfoCard("SakEntryWidget maps each EntryKind to a reference widget shape.")
        EntryKind.entries.forEach { kind ->
            SakEntryWidget(
                featureId = featureId,
                entry = sampleEntry("sample-${kind.name.lowercase()}", kind),
                onClick = {},
            )
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
fun AccessDemoScreen(onBack: () -> Unit) {
    val runtime = HarnessDemoRuntimeRef.runtime ?: return
    val activeConfig by runtime.activeConfig.collectAsState()
    val demosRow = activeConfig.features["demos"]

    SakFeatureScreen(
        title = "FeatureConfig row",
        subtitle = "Per-feature entitlement from ActiveConfig",
    ) {
        if (demosRow == null) return@SakFeatureScreen
        SectionCard("demos feature row") {
            FieldRow("state", demosRow.state.name)
            FieldRow("capabilities", demosRow.capabilities.joinToString(", ").ifEmpty { "—" })
        }
        SectionCard("settings (opaque)") {
            if (demosRow.settings.isEmpty()) {
                Text("none", style = MaterialTheme.typography.bodySmall)
            } else {
                demosRow.settings.forEach { (key, value) ->
                    FieldRow(key, value.toString())
                }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
fun ApiDemoScreen(onBack: () -> Unit) {
    val feature = HarnessDemoFeatureModule.feature ?: return
    var result by remember { mutableStateOf("") }

    SakFeatureScreen(
        title = "Feature APIs",
        subtitle = "cross-feature ApiLookup",
    ) {
        InfoCard(
            "Licensing exposes LicenseProvider as a FeatureApi. Demos looks it up via context.apis.",
        )
        Button(
            onClick = {
                val provider = feature.findLicenseProvider()
                result = if (provider != null) {
                    "Found LicenseProvider → ${provider::class.simpleName}"
                } else {
                    "Not found — licensing feature not loaded"
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Look up LicenseProvider")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                result = feature.apiCatalog().joinToString("\n") { api ->
                    "${api.apiType} (from ${api.featureId.value})"
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Catalog registered APIs")
        }
        if (result.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            SectionCard("Result") {
                Text(result, style = MaterialTheme.typography.bodySmall)
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
fun ServiceDemoScreen(onBack: () -> Unit) {
    val feature = HarnessDemoFeatureModule.feature ?: return
    val service = feature.logService()
    val messages by (service?.messages?.collectAsState() ?: return)

    SakFeatureScreen(
        title = "App services",
        subtitle = "host-injected AppService",
    ) {
        InfoCard("Host registered InMemoryHarnessLogService via AppServiceRegistry.")
        Button(
            onClick = { service.log("log entry @ ${System.nanoTime().toString().takeLast(6)}") },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Write log entry")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { service.clear() }, modifier = Modifier.fillMaxWidth()) {
            Text("Clear log")
        }
        Spacer(Modifier.height(8.dp))
        SectionCard("Service messages (${messages.size})") {
            if (messages.isEmpty()) {
                Text("empty", style = MaterialTheme.typography.bodySmall)
            } else {
                messages.forEach { message -> Text("· $message", style = MaterialTheme.typography.bodySmall) }
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
    val feature = HarnessDemoFeatureModule.feature

    SakFeatureScreen(
        title = "License overlay",
        subtitle = "merge entitlement into ActiveConfig",
    ) {
        InfoCard("DefaultConfig merges with signed or dev overlays. Shell tabs are host-owned.")
        SectionCard("Account license type") {
            FieldRow("type", activeConfig.license.type)
        }
        SectionCard("analytics row") {
            val row = activeConfig.features["analytics"]
            FieldRow("state", row?.state?.name ?: "—")
            FieldRow("capabilities", row?.capabilities?.joinToString(", ") ?: "—")
        }
        SectionCard("licensing row") {
            val row = activeConfig.features["licensing"]
            FieldRow("state", row?.state?.name ?: "—")
            FieldRow("capabilities", row?.capabilities?.joinToString(", ") ?: "—")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { scope.launch { runtime.applyDevEntitlementOverlay(proAnalyticsOverlay) } },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Apply dev overlay (analytics → pro reports)")
        }
        Spacer(Modifier.height(8.dp))
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
    val routeTable by runtime.routeTable.collectAsState()

    SakFeatureScreen(
        title = "Routes & navigation",
        subtitle = "shared NavHost + route table",
    ) {
        InfoCard("runtime.navigate() emits navigationRoute; RouteGuards may deny access.")
        SectionCard("routeTable (${routeTable.routes.size} routes)") {
            routeTable.routes.sorted().forEach { route ->
                Text("· $route", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { runtime.navigate(Route("analytics/today")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Navigate → analytics/today")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { runtime.navigate(Route("licensing/upgrade")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Navigate → licensing/upgrade")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

private fun sampleEntry(id: String, kind: EntryKind) = EntryPoint(
    id = EntryPointId(id),
    kind = kind,
    action = EntryAction.None,
    priority = 0,
)

private val proAnalyticsOverlay =
    """
    {
      "schema": 1,
      "license": { "type": "pro" },
      "features": {
        "analytics": {
          "state": "enabled",
          "capabilities": ["analytics.summary", "analytics.reports"]
        }
      }
    }
    """.trimIndent()
