package sak.runtime

import sak.sdk.api.ApiLookup
import sak.sdk.api.FeatureApi
import sak.sdk.api.FeatureApiDescriptor
import sak.sdk.feature.FeatureId
import kotlin.reflect.KClass

/**
 * Registers [sak.sdk.api.FeatureApi] instances when features activate.
 * Other features resolve APIs via [FeatureContext.apis].
 */
class ApiRegistry {
    private val byType = mutableMapOf<KClass<*>, Pair<FeatureId, Any>>()

    /** Register all APIs exported by a feature on activation. */
    fun register(featureId: FeatureId, apis: Set<FeatureApi<*>>) {
        apis.forEach { api ->
            byType[api.type] = featureId to api.provider
        }
    }

    /** Remove APIs when a feature deactivates. */
    fun unregister(featureId: FeatureId) {
        val keysToRemove = byType.filter { (_, pair) -> pair.first == featureId }.keys
        keysToRemove.forEach { key -> byType.remove(key) }
    }

    /** [ApiLookup] for [FeatureContext.apis]. */
    fun lookup(): ApiLookup = object : ApiLookup {
        override fun <T : Any> find(type: KClass<T>): T? {
            val provider = byType[type]?.second
            @Suppress("UNCHECKED_CAST")
            return if (provider != null && type.isInstance(provider)) provider as T else null
        }

        override fun catalog(): List<FeatureApiDescriptor> =
            byType.map { (type, pair) ->
                FeatureApiDescriptor(apiType = type.qualifiedName ?: type.toString(), featureId = pair.first)
            }
    }
}
