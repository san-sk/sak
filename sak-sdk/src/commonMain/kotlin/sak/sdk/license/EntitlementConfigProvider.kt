package sak.sdk.license

import kotlinx.coroutines.flow.Flow
import sak.sdk.config.AppConfig
import sak.sdk.config.DefaultConfig

/**
 * Host supplies entitlement JSON after login, IAP, cache, or backend push.
 *
 * Runtime subscribes to [updates] and reconciles feature
 * [sak.sdk.feature.Feature.onCreate] / [sak.sdk.feature.Feature.onDestroy].
 *
 * Features do **not** use this directly — they receive entitlement via [sak.sdk.feature.FeatureContext].
 */
interface EntitlementConfigProvider {
    /**
     * Latest config snapshot.
     * @return `null` when only the bundled [DefaultConfig] should apply.
     */
    suspend fun current(): AppConfig?

    /** Emits whenever entitlement changes (login, logout, purchase, refresh). */
    val updates: Flow<AppConfig>
}
