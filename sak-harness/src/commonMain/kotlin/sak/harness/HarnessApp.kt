package sak.harness

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import sak.compose.ui.SakApp
import sak.compose.ui.navContributors
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
    val deps = SakDependencies(licenseProvider = licenseProvider)

    LaunchedEffect(defaultConfig, licenseProvider) {
        if (runtime == null) {
            val launcher = SakLauncher(appScope)
            runtime = launcher.launch(
                defaultConfig = defaultConfig,
                featureModules = featureModules,
                deps = deps,
            )
        }
    }

    runtime?.let { sakRuntime ->
        SakApp(
            runtime = sakRuntime,
            featureNavContributors = featureModules.navContributors(deps),
        )
    }
}
