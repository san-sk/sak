package sak.sample.licensing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import sak.sdk.config.FeatureAccess

fun NavGraphBuilder.licensingNavigation(
    access: FeatureAccess?,
    licenseProvider: SampleLicenseProvider,
    onBack: () -> Unit,
) {
    composable("licensing/upgrade") {
        LicensingUpgradeScreen(
            access = access,
            onUnlock = { licenseProvider.unlockPro() },
            onReset = { licenseProvider.resetToTrial() },
            onBack = onBack,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensingUpgradeScreen(
    access: FeatureAccess?,
    onUnlock: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    val pro = access?.plan == "pro"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("License & Entitlements Hub") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Active Tier Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (pro) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Current Plan Tier", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = if (pro) "PRO ENTERPRISE TIER" else "STANDARD TRIAL TIER",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = if (pro) Color(0xFF4CAF50) else MaterialTheme.colorScheme.tertiary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (pro) "ACTIVE" else "TRIAL",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (pro)
                            "Full access to advanced analytics reports, custom micro-frontend widgets, and priority API grants."
                        else
                            "Limited trial access. Upgrade to Pro to unlock advanced reporting and export features.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Feature Grants Checklist
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Granted Capabilities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    
                    GrantRow(title = "Basic Dashboard Analytics", granted = true)
                    GrantRow(title = "Pro Analytics Reports (`analytics.reports`)", granted = pro)
                    GrantRow(title = "Enterprise Licensing API (`licensing.pro`)", granted = pro)
                    GrantRow(title = "Live Signed License Overlay Updates", granted = true)
                }
            }

            // Interactive Dev Controls
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Interactive Entitlement Control", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Simulate live remote license overlay emissions from server.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    
                    if (pro) {
                        OutlinedButton(
                            onClick = onReset,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Downgrade / Reset to Trial Overlay")
                        }
                    } else {
                        Button(
                            onClick = onUnlock,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Simulate Pro Upgrade (Emit Pro License Overlay)")
                        }
                    }
                }
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Main Shell")
            }
        }
    }
}

@Composable
private fun GrantRow(title: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodySmall)
        Text(
            text = if (granted) "✓ Granted" else "✗ Locked",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (granted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
        )
    }
}
