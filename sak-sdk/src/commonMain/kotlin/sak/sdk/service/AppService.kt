package sak.sdk.service

import kotlin.reflect.KClass

/** Marker for a host-provided service type. */
interface AppService<T : Any> {
    val type: KClass<T>
}

/** Host services injected into features via [sak.sdk.feature.FeatureContext.services]. */
interface AppServiceLookup {
    /** Throws if the service type was not registered by the host. */
    fun <T : Any> require(type: KClass<T>): T
    fun <T : Any> find(type: KClass<T>): T?
}
