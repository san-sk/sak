package sak.sdk.license

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import sak.sdk.config.AppConfig

/** Signed license payload: feature overlay JSON + signature metadata. */
@Serializable
data class SignedLicensePayload(
    val config: AppConfig,
    val signature: String,
    val expiresAt: String? = null,
)

/** Verifies license signatures before merging overlay into ActiveConfig. */
interface LicenseVerifier {
    fun verify(payload: SignedLicensePayload): Boolean
}

/**
 * Supplies the current license and streams updates (e.g. after in-app purchase).
 * Often exposed by the licensing feature as a [sak.sdk.api.FeatureApi].
 */
interface LicenseProvider {
    /** Current license at startup; null means use DefaultConfig only. */
    suspend fun resolveLicense(): SignedLicensePayload?
    /** Emits when license changes (purchase, refresh, revoke). */
    val updates: Flow<SignedLicensePayload>
}
