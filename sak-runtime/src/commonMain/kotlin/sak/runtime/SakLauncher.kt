package sak.runtime

import kotlinx.coroutines.CoroutineScope
import sak.runtime.bundle.FeatureModuleRegistry
import sak.sdk.bundle.FeatureModule
import sak.sdk.config.AppConfig
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureId
import sak.sdk.license.EntitlementConfigProvider
import sak.sdk.license.LicenseProvider
import sak.sdk.license.LicenseVerifier

/** Wires [SakHostSetup] and discovered modules into a running [SakRuntime]. */
class SakLauncher(
    private val scope: CoroutineScope,
) {
    private var runtime: SakRuntime? = null

    fun runtimeOrNull(): SakRuntime? = runtime

    suspend fun launch(
        setup: SakHostSetup,
        featureModules: List<FeatureModule>,
    ): SakRuntime = launch(
        defaultConfig = setup.defaultConfig,
        featureFactories = FeatureModuleRegistry(featureModules).factories(),
        licenseProvider = setup.licenseProvider,
        entitlementProvider = setup.entitlementProvider,
        licenseVerifier = setup.licenseVerifier,
        appServices = setup.appServices,
    )

    suspend fun launch(
        defaultConfig: AppConfig,
        featureFactories: Map<FeatureId, () -> Feature>,
        licenseProvider: LicenseProvider? = null,
        entitlementProvider: EntitlementConfigProvider? = null,
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
        sakRuntime.start(
            licenseProvider = licenseProvider,
            entitlementProvider = entitlementProvider,
        )
        runtime = sakRuntime
        return sakRuntime
    }
}
