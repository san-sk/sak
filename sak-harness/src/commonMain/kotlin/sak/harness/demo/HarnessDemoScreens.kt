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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.coroutines.launch
import sak.compose.ui.FieldRow
import sak.compose.ui.InfoCard
import sak.compose.ui.SakEntryWidget
import sak.compose.ui.SakFeatureScreen
import sak.compose.ui.SectionCard
import sak.sdk.entry.EntryAction
import sak.sdk.entry.EntryPoint
import sak.sdk.entry.EntryPointId
import sak.sdk.entry.EntryStyle
import sak.sdk.feature.Attention
import sak.sdk.feature.FeatureId
import sak.sdk.navigation.Route
import sak.sample.licensing.SampleLicenseProvider

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

    SakFeatureScreen(
        title = "Config-driven scaffold",
        subtitle = "DefaultConfig JSON drives chrome & widgets",
    ) {
        SectionCard("Bottom bar (scaffold.bottomBar)") {
            activeConfig.scaffold?.bottomBar?.items?.forEach { item ->
                FieldRow(item.label, "\u2192 scaffold/${item.destination}")
            }
        }
        SectionCard("Destinations (scaffold.destinations)") {
            activeConfig.scaffold?.destinations?.forEach { (id, layout) ->
                FieldRow(id, "${layout.sections.size} section(s)")
            }
        }
        SectionCard("Active features (features)") {
            activeConfig.features.forEach { (id, toggle) ->
                FieldRow(id, "enabled=${toggle.enabled} \u00B7 visible=${toggle.visible} \u00B7 plan=${toggle.plan}")
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
        subtitle = "onActivated / onAccessChanged / onReset + FeatureStatus",
    ) {
        SectionCard("FeatureStatus") {
            FieldRow("enabled", status.enabled.toString())
            FieldRow("visible", status.visible.toString())
            FieldRow("setupComplete", status.setupComplete.toString())
            FieldRow("attention", status.attention.toString())
            FieldRow("introSeen", status.introSeen.toString())
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
                Attention.entries.forEach { attention ->
                    OutlinedButton(
                        onClick = { feature.setAttention(attention) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(attention.name)
                    }
                }
            }
        }
        SectionCard("Callback log") {
            if (log.isEmpty()) {
                Text("No callbacks yet", style = MaterialTheme.typography.bodySmall)
            } else {
                log.forEach { line -> Text("\u00B7 $line", style = MaterialTheme.typography.bodySmall) }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
fun EntryStylesDemoScreen(onBack: () -> Unit) {
    val featureId = HarnessDemoFeatureModule.feature?.id ?: FeatureId("demos")

    SakFeatureScreen(
        title = "Entry-point styles",
        subtitle = "EntryStyle drives widget rendering on destinations",
    ) {
        InfoCard(
            "SakEntryWidget renders each EntryStyle differently: Card, Row, Banner, " +
                "Tile, Widget, Custom (falls back to Card).",
        )
        EntryStyle.entries.forEach { style ->
            SakEntryWidget(
                featureId = featureId,
                entry = sampleEntry("sample-${style.name.lowercase()}", style),
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
    val feature = HarnessDemoFeatureModule.feature ?: return
    val access by feature.access.collectAsState()

    SakFeatureScreen(
        title = "Feature access",
        subtitle = "FeatureToggle from ActiveConfig",
    ) {
        val toggle = access ?: return@SakFeatureScreen
        SectionCard("Plan & visibility") {
            FieldRow("enabled", toggle.enabled.toString())
            FieldRow("visible", toggle.visible.toString())
            FieldRow("plan", toggle.plan)
        }
        SectionCard("grants") {
            if (toggle.grants.isEmpty()) {
                Text("none", style = MaterialTheme.typography.bodySmall)
            } else {
                toggle.grants.forEach { grant -> Text("\u00B7 $grant", style = MaterialTheme.typography.bodySmall) }
            }
        }
        SectionCard("entries") {
            FieldRow("allowed", toggle.entries?.allowed?.joinToString(", ") ?: "all")
            FieldRow("blocked", toggle.entries?.blocked?.joinToString(", ") ?: "none")
        }
        SectionCard("attrs (opaque JSON)") {
            toggle.attrs.forEach { (key, value) ->
                FieldRow(key, value.toString())
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
            "Licensing exposes LicenseProvider as a FeatureApi. The demos feature looks " +
                "it up at runtime via context.apis.find(...).",
        )
        Button(
            onClick = {
                val provider = feature.findLicenseProvider()
                result = if (provider != null) {
                    "Found LicenseProvider \u2192 ${provider::class.simpleName}"
                } else {
                    "Not found — licensing feature not active"
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
        InfoCard(
            "The host registered InMemoryHarnessLogService via AppServiceRegistry. " +
                "Features consume it through context.services.",
        )
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
                messages.forEach { message -> Text("\u00B7 $message", style = MaterialTheme.typography.bodySmall) }
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
        subtitle = "signed config merge into ActiveConfig",
    ) {
        InfoCard(
            "DefaultConfig is merged with the license overlay. Only features change; " +
                "the scaffold is preserved. Verifier = AcceptAllLicenseVerifier (dev).",
        )
        SectionCard("Current analytics access") {
            FieldRow("plan", activeConfig.features["analytics"]?.plan ?: "\u2014")
            FieldRow(
                "grants",
                (activeConfig.features["analytics"]?.grants ?: emptySet()).joinToString(", ").ifEmpty { "\u2014" },
            )
        }
        SectionCard("Current licensing access") {
            FieldRow("plan", activeConfig.features["licensing"]?.plan ?: "\u2014")
            FieldRow(
                "grants",
                (activeConfig.features["licensing"]?.grants ?: emptySet()).joinToString(", ").ifEmpty { "\u2014" },
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { scope.launch { runtime.applyDevLicenseOverlay(proAnalyticsOverlay) } },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Apply dev overlay (analytics \u2192 pro)")
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
        InfoCard(
            "The licensing feature's Unlock Pro (licensing/upgrade) emits the same overlay. " +
                "After a pro overlay analytics gains analytics.reports (report-row on Alerts, " +
                "trial-tile disappears from Home) and licensing swaps unlock-banner for a pro-widget.",
        )
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
        subtitle = "one shared NavHost + route table",
    ) {
        InfoCard(
            "All scaffold and feature routes live in one graph. runtime.navigate() emits " +
                "a one-shot navigationRoute that SakApp consumes.",
        )
        SectionCard("routeTable (${routeTable.routes.size} routes)") {
            routeTable.routes.sorted().forEach { route ->
                Text("\u00B7 $route", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { runtime.navigate(Route("analytics/today")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Navigate \u2192 analytics/today")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { runtime.navigate(Route("licensing/upgrade")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Navigate \u2192 licensing/upgrade")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

private fun sampleEntry(id: String, style: EntryStyle) = EntryPoint(
    id = EntryPointId(id),
    style = style,
    previewKey = "demo.$id",
    action = EntryAction.None,
    priority = 0,
)

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
