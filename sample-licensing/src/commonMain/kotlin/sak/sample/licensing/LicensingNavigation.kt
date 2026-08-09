package sak.sample.licensing

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import sak.compose.ui.FieldRow
import sak.compose.ui.InfoCard
import sak.compose.ui.SakFeatureScreen
import sak.compose.ui.SectionCard

/** Registers licensing routes on the shared SAK NavHost. */
fun NavGraphBuilder.licensingNavigation(
    licenseProvider: SampleLicenseProvider,
    onBack: () -> Unit,
) {
    composable("licensing/upgrade") {
        LicensingUpgradeScreen(
            onUnlock = { licenseProvider.unlockPro() },
            onReset = { licenseProvider.resetToTrial() },
            onBack = onBack,
        )
    }
}

@Composable
fun LicensingUpgradeScreen(
    onUnlock: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    val feature = LicensingFeatureModule.feature ?: return
    val access by feature.access.collectAsState()
    val pro = access?.plan == "pro"

    SakFeatureScreen(
        title = "Licensing",
        subtitle = "Feature route · licensing/upgrade",
    ) {
        InfoCard(
            "Tapping Unlock emits a signed license overlay. The runtime merges it into " +
                "ActiveConfig and re-activates features: analytics gains the analytics.reports " +
                "grant (report-row on Alerts) and licensing swaps its unlock-banner for a pro-widget.",
        )
        SectionCard("License state") {
            FieldRow("plan", access?.plan ?: "\u2014")
            FieldRow("grants", (access?.grants ?: emptySet()).joinToString(", ").ifEmpty { "\u2014" })
        }
        Spacer(Modifier.height(4.dp))
        if (pro) {
            InfoCard(
                "Pro is active — entry points and flow have changed. Reset to trial to " +
                    "revert the overlay and watch the widgets swap back.",
            )
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Reset to trial")
            }
        } else {
            Button(
                onClick = onUnlock,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Unlock Pro")
            }
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Back")
        }
    }
}
