package sak.runtime

import kotlinx.coroutines.CoroutineScope
import sak.runtime.bundle.discoverFeatureModules
import sak.sdk.feature.FeatureId

/**
 * Single host entry: discovery, entitlement providers, launch, runtime access.
 */
class SakHost(
    private val scope: CoroutineScope,
    private val setup: SakHostSetup,
) {
    private val launcher = SakLauncher(scope)
    private val featureModules: List<sak.sdk.bundle.FeatureModule> =
        discoverFeatureModules(setup.bundledModules)

    private var runtime: SakRuntime? = null

    fun setup(): SakHostSetup = setup

    fun modules(): List<sak.sdk.bundle.FeatureModule> = featureModules

    fun runtimeOrNull(): SakRuntime? = runtime ?: launcher.runtimeOrNull()

    suspend fun start(): SakRuntime {
        val existing = runtimeOrNull()
        if (existing != null) return existing
        val sakRuntime = launcher.launch(setup, featureModules)
        runtime = sakRuntime
        return sakRuntime
    }

    suspend fun applyEntitlementOverlay(json: String) {
        runtimeOrNull()?.applyDevEntitlementOverlay(json)
    }
}
