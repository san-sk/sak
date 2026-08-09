package sak.runtime

import kotlinx.coroutines.CoroutineScope
import sak.runtime.bundle.FeatureModuleRegistry
import sak.sdk.bundle.FeatureModule
import sak.sdk.bundle.SakDependencies
import sak.sdk.config.AppConfig
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureId
import sak.sdk.license.LicenseProvider
import sak.sdk.license.LicenseVerifier

/**
 * Host entry point: wires config + feature modules into a running [SakRuntime].
 */
class SakLauncher(
    private val scope: CoroutineScope,
) {
    private var runtime: SakRuntime? = null

    /** Running runtime after [launch], or null before first launch. */
    fun runtimeOrNull(): SakRuntime? = runtime

    /** Launch with explicit feature factories (manual wiring). */
    suspend fun launch(
        defaultConfig: AppConfig,
        featureFactories: Map<FeatureId, () -> Feature>,
        licenseProvider: LicenseProvider? = null,
        licenseVerifier: LicenseVerifier = AcceptAllLicenseVerifier,
        appServices: AppServiceRegistry = AppServiceRegistry(),
    ): SakRuntime {
        val sakRuntime = SakRuntime(
            defaultConfig = defaultConfig,
            featureFactories = featureFactories,
            appServiceRegistry = appServices,
            licenseVerifier = licenseVerifier,
            scope = scope,
        )
        sakRuntime.start(licenseProvider)
        runtime = sakRuntime
        return sakRuntime
    }

    /** Launch with discovered [FeatureModule] list (recommended). */
    suspend fun launch(
        defaultConfig: AppConfig,
        featureModules: List<FeatureModule>,
        deps: SakDependencies = SakDependencies(),
        licenseProvider: LicenseProvider? = deps.licenseProvider,
        licenseVerifier: LicenseVerifier = AcceptAllLicenseVerifier,
        appServices: AppServiceRegistry = AppServiceRegistry(),
    ): SakRuntime {
        val resolvedDeps = deps.copy(licenseProvider = licenseProvider ?: deps.licenseProvider)
        val registry = FeatureModuleRegistry(featureModules)
        return launch(
            defaultConfig = defaultConfig,
            featureFactories = registry.factories(resolvedDeps),
            licenseProvider = resolvedDeps.licenseProvider,
            licenseVerifier = licenseVerifier,
            appServices = appServices,
        )
    }
}
