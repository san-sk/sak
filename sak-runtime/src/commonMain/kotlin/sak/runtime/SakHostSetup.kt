package sak.runtime

import sak.sdk.bundle.FeatureModule
import sak.sdk.config.AppConfig
import sak.sdk.license.EntitlementConfigProvider
import sak.sdk.license.LicenseProvider
import sak.sdk.license.LicenseVerifier

/**
 * Everything the host app supplies to start SAK.
 *
 * Single bag for launch wiring — distinct from [sak.sdk.config.AppConfig] (entitlement data)
 * and from [sak.sdk.feature.FeatureContext] (live feature instance).
 */
data class SakHostSetup(
    /** Bundled entitlement JSON (guest / trial defaults). */
    val defaultConfig: AppConfig,
    /**
     * Extra modules for iOS or to override ServiceLoader on Android.
     * Merged with platform discovery; same [sak.sdk.feature.FeatureId] → manual wins.
     */
    val bundledModules: List<FeatureModule> = emptyList(),
    /** Unsigned entitlement updates (login, IAP, backend). */
    val entitlementProvider: EntitlementConfigProvider? = null,
    /** Signed entitlement updates. */
    val licenseProvider: LicenseProvider? = null,
    /** Verifies signed overlays before merge. Replace in production. */
    val licenseVerifier: LicenseVerifier = AcceptAllLicenseVerifier,
    /** Host app services exposed to features via [sak.sdk.feature.FeatureContext.services]. */
    val appServices: AppServiceRegistry = AppServiceRegistry(),
)
