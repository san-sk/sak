package sak.runtime.bundle

import sak.sdk.bundle.FeatureModule
import sak.sdk.bundle.SakDependencies
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureId

/** Turns a list of [FeatureModule] into factories for [sak.runtime.SakRuntime]. */
class FeatureModuleRegistry(
    private val modules: List<FeatureModule>,
) {
    /** Map feature id → lazy factory for [SakRuntime]. */
    fun factories(deps: SakDependencies): Map<FeatureId, () -> Feature> =
        modules.associate { module ->
            module.id to { module.create(deps) }
        }
}
