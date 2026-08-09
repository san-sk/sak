package sak.runtime

import sak.sdk.service.AppServiceLookup
import kotlin.reflect.KClass

/** Host-provided services passed into features via [FeatureRuntimeContext]. */
class AppServiceRegistry(
    private val services: Map<KClass<*>, Any> = emptyMap(),
) {
    /** Lookup adapter passed into [FeatureRuntimeContext]. */
    fun lookup(): AppServiceLookup = object : AppServiceLookup {
        override fun <T : Any> require(type: KClass<T>): T =
            find(type) ?: error("AppService not registered: ${type.qualifiedName ?: type.toString()}")

        override fun <T : Any> find(type: KClass<T>): T? {
            val service = services[type]
            @Suppress("UNCHECKED_CAST")
            return if (service != null && type.isInstance(service)) service as T else null
        }
    }
}
