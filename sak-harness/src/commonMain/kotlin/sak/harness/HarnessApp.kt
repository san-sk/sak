package sak.harness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import sak.compose.ui.SakApp
import sak.compose.ui.SakLogo
import sak.compose.ui.navContributors
import sak.harness.demo.HarnessDemoRuntimeRef
import sak.harness.demo.HarnessLogService
import sak.harness.demo.InMemoryHarnessLogService
import sak.runtime.AppServiceRegistry
import sak.runtime.SakLauncher
import sak.runtime.SakRuntime
import sak.runtime.bundle.discoverFeatureModules
import sak.sample.licensing.SampleLicenseProvider
import sak.sdk.bundle.SakDependencies
import sak.sdk.config.AppConfig

/** Demo harness root: discovers modules, launches runtime, renders [SakApp]. */
@Composable
fun HarnessApp(
    defaultConfig: AppConfig,
    licenseProvider: SampleLicenseProvider = remember { SampleLicenseProvider() },
    appScope: CoroutineScope = rememberCoroutineScope(),
) {
    var runtime by remember { mutableStateOf<SakRuntime?>(null) }
    val featureModules = remember {
        discoverFeatureModules(manualModules = harnessBundledFeatureModules)
    }
    val logService = remember { InMemoryHarnessLogService() }
    val appServices = remember(logService) {
        AppServiceRegistry(mapOf(HarnessLogService::class to logService))
    }
    val deps = SakDependencies(licenseProvider = licenseProvider)

    LaunchedEffect(defaultConfig, licenseProvider) {
        if (runtime == null) {
            val launcher = SakLauncher(appScope)
            val sakRuntime = launcher.launch(
                defaultConfig = defaultConfig,
                featureModules = featureModules,
                deps = deps,
                appServices = appServices,
            )
            HarnessDemoRuntimeRef.runtime = sakRuntime
            runtime = sakRuntime
        }
    }

    val sakRuntime = runtime
    if (sakRuntime == null) {
        LoadingScreen()
    } else {
        SakApp(
            runtime = sakRuntime,
            featureNavContributors = featureModules.navContributors(deps),
        )
    }
}

/** Branded splash shown while the runtime boots (config merge + feature activation). */
@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SakLogo(modifier = Modifier.size(96.dp))
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Starting SAK harness",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
