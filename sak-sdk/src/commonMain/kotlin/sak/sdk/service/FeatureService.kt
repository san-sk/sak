package sak.sdk.service

import kotlin.reflect.KClass

/**
 * A service/API exported by a feature for use by other features or the host app.
 */
interface FeatureService<T : Any> {
    val type: KClass<T>
    val provider: T
}

/** Wrap a concrete provider as a [FeatureService]. */
fun <T : Any> featureService(type: KClass<T>, provider: T): FeatureService<T> =
    object : FeatureService<T> {
        override val type: KClass<T> = type
        override val provider: T = provider
    }
