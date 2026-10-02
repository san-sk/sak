package sak.sample.licensing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import sak.sdk.config.EntryAccess
import sak.sdk.config.FeatureAccess
import sak.sdk.config.SakConfig
import sak.sdk.license.LicenseProvider
import sak.sdk.license.SignedLicensePayload

/**
 * In-memory demo [LicenseProvider] for the harness.
 */
class SampleLicenseProvider : LicenseProvider {
    private val proOverlay = SignedLicensePayload(
        config = SakConfig(
            version = 1,
            features = mapOf(
                "analytics" to FeatureAccess(
                    enabled = true,
                    visible = true,
                    plan = "pro",
                    grants = setOf("analytics.summary", "analytics.reports"),
                    entries = EntryAccess(
                        allowed = setOf("summary-card", "report-row"),
                    ),
                ),
                "licensing" to FeatureAccess(
                    enabled = true,
                    visible = true,
                    plan = "pro",
                    grants = setOf("licensing.pro"),
                    entries = EntryAccess(
                        allowed = setOf("plan-row", "pro-widget"),
                    ),
                ),
            ),
        ),
        signature = "dev-pro",
    )

    private val trialOverlay = SignedLicensePayload(
        config = SakConfig(
            version = 1,
            features = mapOf(
                "analytics" to FeatureAccess(
                    enabled = true,
                    visible = true,
                    plan = "trial",
                    grants = setOf("analytics.summary"),
                    entries = EntryAccess(
                        allowed = setOf("summary-card", "trial-tile"),
                    ),
                ),
                "licensing" to FeatureAccess(
                    enabled = true,
                    visible = true,
                    plan = "trial",
                    grants = emptySet(),
                    entries = EntryAccess(
                        allowed = setOf("plan-row", "unlock-banner"),
                    ),
                ),
            ),
        ),
        signature = "dev-trial",
    )

    private val updatesFlow = MutableSharedFlow<SignedLicensePayload>(extraBufferCapacity = 1)

    override val updates: Flow<SignedLicensePayload> = updatesFlow.asSharedFlow()

    override suspend fun resolveLicense(): SignedLicensePayload? = null

    /** Harness/demo: emit pro license overlay. */
    fun unlockPro() {
        updatesFlow.tryEmit(proOverlay)
    }

    /** Harness/demo: emit trial overlay. */
    fun resetToTrial() {
        updatesFlow.tryEmit(trialOverlay)
    }
}
