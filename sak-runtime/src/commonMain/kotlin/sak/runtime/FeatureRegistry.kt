package sak.runtime

import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureId

/**
 * Registry of the feature modules available in this build.
 *
 * Populated by the host, usually in one place when the runtime is created. Keep the list
 * short and explicit: it is the one place that says which feature modules this app ships.
 *
 * Registration provides a factory lambda (`() -> Feature`), meaning the feature object is 
 * only constructed if it passes every gate: available, licensed, and enabled. 
 * An unlicensed/disabled feature costs nothing in memory beyond a map entry.
 */
class FeatureRegistry {
    private val factories = mutableMapOf<FeatureId, () -> Feature>()

    /**
     * Registers a pre-instantiated [feature]. 
     * Note: This defeats lazy loading since the feature is already in memory.
     * Prefer [registerFactory] for optimal memory usage in production apps.
     */
    fun register(feature: Feature): FeatureRegistry {
        factories[feature.id] = { feature }
        return this
    }

    /** 
     * Registers a feature to be produced lazily on demand.
     * This is the recommended approach for optimal memory usage.
     */
    fun registerFactory(id: FeatureId, factory: () -> Feature): FeatureRegistry {
        factories[id] = factory
        return this
    }

    /** Immutable snapshot of all available feature factories. */
    fun snapshot(): Map<FeatureId, () -> Feature> = factories.toMap()

    /** IDs of all available features, whether or not they are licensed or enabled. */
    fun availableIds(): Set<FeatureId> = factories.keys.toSet()

    companion object {
        /** Creates a registry pre-populated with the given feature instances. */
        fun of(vararg features: Feature): FeatureRegistry =
            FeatureRegistry().apply { features.forEach { register(it) } }
    }
}
