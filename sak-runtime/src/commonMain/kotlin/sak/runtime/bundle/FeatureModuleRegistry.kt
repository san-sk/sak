package sak.runtime.bundle

import sak.sdk.bundle.FeatureModule
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureId

/** Turns discovered [FeatureModule]s into lazy factories for [sak.runtime.SakRuntime]. */
class FeatureModuleRegistry(
    private val modules: List<FeatureModule>,
) {
    fun factories(): Map<FeatureId, () -> Feature> =
        modules.associate { module ->
            module.id to { module.create() }
        }
}
