package sak.sdk.license

import sak.sdk.feature.FeatureId

/**
 * Decides whether the current license permits a feature.
 *
 * This is deliberately a single predicate with no cryptography. SAK owns *when* a
 * licensed feature starts and stops; the host owns *how* a license is obtained and
 * validated (StoreKit, Play Billing, a signed server payload, a hardcoded dev list).
 *
 * A feature is active only when all three gates agree:
 *  1. **Available** - the module is registered in the host's `FeatureRegistry`.
 *  2. **Licensed** - [isLicensed] returns `true` for the feature ID.
 *  3. **Enabled** - not explicitly disabled in `SakConfig`.
 *
 * ```kotlin
 * // Production: whatever your license backend reports.
 * val entitlements = FeatureEntitlements { featureId -> myLicense.allows(featureId) }
 *
 * // Development / tests: everything licensed (the default).
 * SakRuntime(entitlements = AllFeaturesLicensed)
 *
 * // Fixed list, e.g. read from your own config file.
 * SakRuntime(entitlements = LicensedFeatures(setOf(FeatureId("analytics"))))
 * ```
 */
fun interface FeatureEntitlements {
    /** Returns whether [featureId] is permitted by the current license. */
    fun isLicensed(featureId: FeatureId): Boolean
}

/** Licenses every discovered feature. The default, for development and tests. */
object AllFeaturesLicensed : FeatureEntitlements {
    override fun isLicensed(featureId: FeatureId): Boolean = true
}

/** Licenses only the given feature IDs. */
class LicensedFeatures(featureIds: Collection<FeatureId>) : FeatureEntitlements {
    private val licensed: Set<FeatureId> = featureIds.toSet()

    /** The licensed feature IDs. */
    val featureIds: Set<FeatureId> get() = licensed

    override fun isLicensed(featureId: FeatureId): Boolean = featureId in licensed
}

/** Licenses nothing, disabling every discovered feature. */
object NoFeaturesLicensed : FeatureEntitlements {
    override fun isLicensed(featureId: FeatureId): Boolean = false
}
