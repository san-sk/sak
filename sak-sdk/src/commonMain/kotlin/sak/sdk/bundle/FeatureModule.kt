package sak.sdk.bundle

import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureId
import sak.sdk.license.LicenseProvider

/**
 * Annotation for auto-discovering feature modules on iOS via KSP.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class SakModule

/**
 * Priority for when a feature should be instantiated.
 */
enum class LoadingPriority {
    /** Instantiated as soon as the runtime starts (if enabled). */
    Startup,
    /** Instantiated only when requested via [sak.runtime.SakRuntime.getFeature]. */
    Lazy
}

/** Dependencies the host passes when creating bundled features. */
data class SakDependencies(
    /** Optional license source; often the licensing feature's [sak.sdk.license.LicenseProvider]. */
    val licenseProvider: LicenseProvider? = null,
)

/**
 * One installable feature module: stable id + factory.
 *
 * Register for auto-discovery on Android:
 * `META-INF/services/sak.sdk.bundle.FeatureModule` → your module object class name.
 * On iOS, list modules manually in the host app.
 */
interface FeatureModule {
    val id: FeatureId

    /** When this module should be instantiated. Defaults to [LoadingPriority.Startup]. */
    val priority: LoadingPriority get() = LoadingPriority.Startup

    /** Create a new feature instance (one per app process when activated). */
    fun create(deps: SakDependencies): Feature
}
