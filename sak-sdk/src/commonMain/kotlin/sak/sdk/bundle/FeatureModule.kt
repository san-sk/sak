package sak.sdk.bundle

import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureId
import sak.sdk.license.LicenseProvider

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
    /** Create a new feature instance (one per app process when activated). */
    fun create(deps: SakDependencies): Feature
}
