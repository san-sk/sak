package sak.sdk

import sak.sdk.config.AppConfig

/**
 * Entry point for starting SAK from a host app.
 * The concrete launcher lives in [sak.runtime.SakLauncher].
 */
interface SakLauncher {
    /** Load config, activate features, and return a running runtime instance. */
    suspend fun launch(defaultConfig: AppConfig)
}
