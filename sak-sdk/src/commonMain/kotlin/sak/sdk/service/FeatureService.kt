package sak.sdk.service

import kotlin.reflect.KClass

/**
 * A public Service or API exported by a feature module for Inter-Process Communication (IPC).
 *
 * In the SAK architecture, features must not hold direct Gradle dependencies on each other.
 * This class functions similarly to `ContentProvider`s or `SystemService`s in Android:
 * it allows a feature to securely expose a capability interface to the host app or sibling 
 * feature modules without coupling their concrete implementation classes.
 *
 * @param T The public Kotlin interface type defining the API contract.
 */
interface FeatureService<T : Any> {
    /** The interface class key used for `ServiceRegistry` lookups. */
    val type: KClass<T>

    /** The concrete service provider implementation instance. */
    val provider: T
}

/**
 * A helper factory to wrap an API interface class and its concrete provider instance into a [FeatureService].
 *
 * Example:
 * ```kotlin
 * override fun services(): Set<FeatureService<*>> = setOf(
 *     featureService(AnalyticsApi::class, analyticsApiImpl)
 * )
 * ```
 */
fun <T : Any> featureService(type: KClass<T>, provider: T): FeatureService<T> =
    object : FeatureService<T> {
        override val type: KClass<T> = type
        override val provider: T = provider
    }
