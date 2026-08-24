package sak.sdk.bundle

import sak.sdk.config.AppConfig
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureId
import sak.sdk.feature.FeatureState

/**
 * Installable feature: stable [id] and a factory for [Feature] instances.
 *
 * Registered at build time; runtime calls [create] when entitlement [FeatureState] is [FeatureState.Enabled].
 *
 * - **Android:** optional ServiceLoader — `META-INF/services/sak.sdk.bundle.FeatureModule`
 * - **iOS:** host passes modules in `SakHostSetup.bundledModules` at launch
 *
 * Host dependencies (providers, services) are **not** passed here — configure
 * [sak.runtime.SakHostSetup] and inject via module constructor when a factory needs them.
 */
interface FeatureModule {
    /** Stable id (e.g. `analytics`). Must match [AppConfig.features] keys. */
    val id: FeatureId

    fun create(): Feature
}
