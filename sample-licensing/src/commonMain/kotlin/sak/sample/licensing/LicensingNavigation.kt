package sak.sample.licensing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** Registers licensing routes on the shared SAK NavHost. */
fun NavGraphBuilder.licensingNavigation(
    licenseProvider: SampleLicenseProvider,
    onBack: () -> Unit,
) {
    composable("licensing/upgrade") {
        LicensingUpgradeScreen(
            onUnlock = { licenseProvider.unlockPro() },
            onBack = onBack,
        )
    }
}

@Composable
fun LicensingUpgradeScreen(
    onUnlock: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Upgrade license", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Unlock pro analytics via signed license overlay")
        Button(onClick = onUnlock) {
            Text("Unlock Pro")
        }
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}
