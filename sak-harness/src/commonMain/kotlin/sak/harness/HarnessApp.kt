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
import sak.runtime.SakHost
import sak.runtime.SakHostSetup
import sak.runtime.SakRuntime
import sak.sample.licensing.SampleLicenseProvider
import sak.sdk.config.AppConfig

/** Demo harness: [SakHost] + [SakApp]. */
@Composable
fun HarnessApp(
    defaultConfig: AppConfig,
    licenseProvider: SampleLicenseProvider = remember { SampleLicenseProvider() },
    appScope: CoroutineScope = rememberCoroutineScope(),
) {
    var runtime by remember { mutableStateOf<SakRuntime?>(null) }
    val logService = remember { InMemoryHarnessLogService() }
    val host = remember(licenseProvider, logService, defaultConfig) {
        SakHost(
            scope = appScope,
            setup = SakHostSetup(
                defaultConfig = defaultConfig,
                bundledModules = harnessBundledFeatureModules(licenseProvider),
                licenseProvider = licenseProvider,
                appServices = AppServiceRegistry(mapOf(HarnessLogService::class to logService)),
            ),
        )
    }

    LaunchedEffect(host) {
        if (runtime == null) {
            val sakRuntime = host.start()
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
            featureNavContributors = host.modules().navContributors(),
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SakLogo(modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
            Text("Starting SAK…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
