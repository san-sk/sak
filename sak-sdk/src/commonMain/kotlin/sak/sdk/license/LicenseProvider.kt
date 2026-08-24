package sak.sdk.license

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import sak.sdk.config.AppConfig

/**
 * Signed entitlement payload — config plus verification metadata.
 *
 * @property config Entitlement JSON to merge when [EntitlementVerifier.verify] succeeds.
 * @property signature Host or backend signature bytes (encoding defined by verifier).
 * @property expiresAt Optional expiry hint for cache (ISO-8601 string).
 */
@Serializable
data class SignedEntitlementPayload(
    val config: AppConfig,
    val signature: String,
    val expiresAt: String? = null,
)

/** Verifies [SignedEntitlementPayload] before runtime merges [SignedEntitlementPayload.config]. */
interface EntitlementVerifier {
    fun verify(payload: SignedEntitlementPayload): Boolean
}

/** @deprecated Use [EntitlementVerifier]. */
typealias LicenseVerifier = EntitlementVerifier

/** @deprecated Use [SignedEntitlementPayload]. */
typealias SignedLicensePayload = SignedEntitlementPayload

/**
 * Legacy signed-config provider.
 *
 * Prefer [EntitlementConfigProvider] for login/IAP JSON that does not require signing.
 */
interface LicenseProvider {
    /** Current signed payload, or `null` if none. */
    suspend fun resolveLicense(): SignedEntitlementPayload?

    /** Emits on entitlement changes (same contract as [EntitlementConfigProvider.updates]). */
    val updates: Flow<SignedEntitlementPayload>
}
