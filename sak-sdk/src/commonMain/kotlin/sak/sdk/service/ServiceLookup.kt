package sak.sdk.service

import kotlin.reflect.KClass

/**
 * A unified lookup container representing the Service Locator pattern.
 * 
 * Provides access to Host-provided services and Feature-exported APIs (registered via [FeatureService]).
 * This interface is injected into every feature inside the [sak.sdk.feature.FeatureContext].
 */
interface ServiceLookup {
    /**
     * Finds and returns an active service implementing [type], or `null` if the feature
     * providing that service is disabled, not licensed, or not registered.
     * 
     * The nullable return type enforces graceful degradation instead of runtime crashes.
     */
    fun <T : Any> find(type: KClass<T>): T?
}

/**
 * Convenience inline extension to query the [ServiceLookup] registry using reified generic type parameters.
 *
 * Example:
 * ```kotlin
 * val logger = context.services.get<LoggerApi>()
 * logger?.log("Executing safely without direct coupling!")
 * ```
 */
inline fun <reified T : Any> ServiceLookup.get(): T? = find(T::class)
