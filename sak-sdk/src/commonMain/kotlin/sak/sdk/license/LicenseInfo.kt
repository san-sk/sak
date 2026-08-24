package sak.sdk.license

import kotlinx.serialization.Serializable
import sak.sdk.config.AppConfig

/**
 * Account-level metadata in [AppConfig.license].
 *
 * @property type Display label (guest, trial, pro). **Not used for gating** — features may read for UI only.
 * @property rev Optional backend revision for cache and staleness checks.
 */
@Serializable
data class LicenseInfo(
    val type: String = "guest",
    val rev: String? = null,
)
