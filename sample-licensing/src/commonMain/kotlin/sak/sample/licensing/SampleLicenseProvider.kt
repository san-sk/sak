package sak.sample.licensing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import sak.sdk.api.featureApi
import sak.sdk.config.AppConfig
import sak.sdk.config.FeatureToggle
import sak.sdk.license.LicenseProvider
import sak.sdk.license.SignedLicensePayload

/**
 * In-memory demo [LicenseProvider] for the harness.
 * [unlockPro] emits a signed overlay that upgrades analytics to pro plan.
 */
class SampleLicenseProvider : LicenseProvider {
    private val proOverlay = SignedLicensePayload(
        config = AppConfig(
            version = 1,
            features = mapOf(
                "analytics" to FeatureToggle(
                    enabled = true,
                    visible = true,
                    plan = "pro",
                    grants = setOf("analytics.summary", "analytics.reports"),
                    entries = sak.sdk.config.EntryAccess(
                        allowed = setOf("summary-card", "report-row"),
                    ),
                ),
            ),
        ),
        signature = "dev-pro",
    )

    private val updatesFlow = MutableSharedFlow<SignedLicensePayload>(extraBufferCapacity = 1)

    override val updates: Flow<SignedLicensePayload> = updatesFlow.asSharedFlow()

    override suspend fun resolveLicense(): SignedLicensePayload? = null

    /** Harness/demo: emit pro license overlay (triggers runtime config merge). */
    fun unlockPro() {
        updatesFlow.tryEmit(proOverlay)
    }
}
