package sak.sdk.service

import kotlin.reflect.KClass

/**
 * Unified lookup for host-provided services and feature-provided APIs.
 */
interface ServiceLookup {
    /** Returns the requested service/API, or null if not registered. */
    fun <T : Any> find(type: KClass<T>): T?

    /** Returns the requested service/API, or throws an error if missing. */
    fun <T : Any> require(type: KClass<T>): T =
        find(type) ?: error("Required service ${type.simpleName ?: type.toString()} was not registered")
}

/** Get a typed service or null. */
inline fun <reified T : Any> ServiceLookup.get(): T? = find(T::class)

/** Get a typed service or throw. */
inline fun <reified T : Any> ServiceLookup.requireService(): T = require(T::class)
