package sak.sdk.service

import kotlin.reflect.KClass

/**
 * Marker pairing a host service type with its [KClass] for registration.
 */
interface AppService<T : Any> {
    val type: KClass<T>
}

/**
 * Host services available to loaded features via [sak.sdk.feature.FeatureContext.services].
 */
interface AppServiceLookup {
    /** Returns the service or throws if the host did not register [type]. */
    fun <T : Any> require(type: KClass<T>): T

    /** Returns the service or `null` if not registered. */
    fun <T : Any> find(type: KClass<T>): T?
}
