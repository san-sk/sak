package sak.sdk.api

import kotlin.reflect.KClass
import sak.sdk.feature.FeatureId

/**
 * Typed capability exported by a loaded [sak.sdk.feature.Feature] (e.g. a license API object).
 *
 * Registered in runtime when the feature is created; looked up via [ApiLookup].
 */
interface FeatureApi<T : Any> {
    val type: KClass<T>
    val provider: T
}

/**
 * Catalog entry for tooling or debug menus.
 *
 * @property apiType Stable type name string.
 * @property featureId Feature that registered this API.
 */
data class FeatureApiDescriptor(
    val apiType: String,
    val featureId: FeatureId,
)

/** Wrap a concrete provider as a [FeatureApi] for [sak.sdk.feature.Feature.apis]. */
fun <T : Any> featureApi(type: KClass<T>, provider: T): FeatureApi<T> =
    object : FeatureApi<T> {
        override val type: KClass<T> = type
        override val provider: T = provider
    }

/** Look up APIs from currently loaded features. */
interface ApiLookup {
    /** Returns `null` if no loaded feature registered this type. */
    fun <T : Any> find(type: KClass<T>): T?

    /** All registered APIs for tooling / debug surfaces. */
    fun catalog(): List<FeatureApiDescriptor>
}
