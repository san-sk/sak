package sak.runtime

import sak.sdk.feature.FeatureId
import sak.sdk.service.FeatureService
import sak.sdk.service.ServiceLookup
import kotlin.reflect.KClass

/**
 * Unified registry managing host-provided services and feature-provided services/APIs.
 */
class ServiceRegistry(
    hostServices: Map<KClass<*>, Any> = emptyMap()
) {
    private val hostServicesMap = hostServices.toMutableMap()
    private val featureServicesMap = mutableMapOf<KClass<*>, Pair<FeatureId, Any>>()

    /** Register a host service. */
    fun registerHostService(type: KClass<*>, service: Any) {
        hostServicesMap[type] = service
    }

    /** Register services exported by a feature when activated. */
    fun registerFeatureServices(featureId: FeatureId, services: Set<FeatureService<*>>) {
        services.forEach { service ->
            featureServicesMap[service.type] = featureId to service.provider
        }
    }

    /** Unregister services when a feature is deactivated. */
    fun unregisterFeatureServices(featureId: FeatureId) {
        val keysToRemove = featureServicesMap.filter { (_, pair) -> pair.first == featureId }.keys
        keysToRemove.forEach { key -> featureServicesMap.remove(key) }
    }

    /** Returns unified [ServiceLookup]. */
    fun lookup(): ServiceLookup = object : ServiceLookup {
        override fun <T : Any> find(type: KClass<T>): T? {
            val featureProvider = featureServicesMap[type]?.second
            if (featureProvider != null && type.isInstance(featureProvider)) {
                @Suppress("UNCHECKED_CAST")
                return featureProvider as T
            }
            val hostService = hostServicesMap[type]
            if (hostService != null && type.isInstance(hostService)) {
                @Suppress("UNCHECKED_CAST")
                return hostService as T
            }
            return null
        }
    }
}
