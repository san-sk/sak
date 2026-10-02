package sak.runtime

import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureId

/**
 * Registry of available feature modules.
 */
class FeatureRegistry(
    private val factories: MutableMap<FeatureId, () -> Feature> = mutableMapOf()
) {
    /** Register a feature instance. */
    fun register(feature: Feature): FeatureRegistry {
        factories[feature.id] = { feature }
        return this
    }

    /** Register a feature factory. */
    fun register(id: FeatureId, factory: () -> Feature): FeatureRegistry {
        factories[id] = factory
        return this
    }

    /** Returns all registered feature factories. */
    fun factories(): Map<FeatureId, () -> Feature> = factories

    companion object {
        /** Create a feature registry with explicit feature list. */
        fun of(vararg features: Feature): FeatureRegistry {
            val registry = FeatureRegistry()
            features.forEach { registry.register(it) }
            return registry
        }
    }
}
