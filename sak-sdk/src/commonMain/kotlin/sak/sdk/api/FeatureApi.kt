package sak.sdk.api

import kotlin.reflect.KClass
import sak.sdk.feature.FeatureId

/**
 * A typed capability exported by a feature (e.g. LicenseProvider from the licensing feature).
 * Registered in [sak.runtime.ApiRegistry] when the feature activates.
 */
interface FeatureApi<T : Any> {
    val type: KClass<T>
    val provider: T
}

data class FeatureApiDescriptor(
    val apiType: String,
    val featureId: FeatureId,
)

/** Wrap a concrete provider as a [FeatureApi] for [Feature.apis]. */
fun <T : Any> featureApi(type: KClass<T>, provider: T): FeatureApi<T> =
    object : FeatureApi<T> {
        override val type: KClass<T> = type
        override val provider: T = provider
    }

/** Look up APIs from active features. */
interface ApiLookup {
    /** Returns null if no active feature registered this API type. */
    fun <T : Any> find(type: KClass<T>): T?
    /** All registered APIs for tooling / debug menus. */
    fun catalog(): List<FeatureApiDescriptor>
}
