package sak.sample.licensing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import sak.sdk.config.AppConfig
import sak.sdk.feature.FeatureConfig
import sak.sdk.feature.FeatureState
import sak.sdk.license.LicenseInfo
import sak.sdk.license.LicenseProvider
import sak.sdk.license.SignedLicensePayload

/**
 * In-memory demo [LicenseProvider] for the harness.
 * [unlockPro] / [resetToTrial] emit signed overlays that change license type and capabilities.
 */
class SampleLicenseProvider : LicenseProvider {
    private val proOverlay = SignedLicensePayload(
        config = AppConfig(
            license = LicenseInfo(type = "pro"),
            features = mapOf(
                "analytics" to FeatureConfig(
                    state = FeatureState.Enabled,
                    capabilities = setOf("analytics.summary", "analytics.reports"),
                ),
                "licensing" to FeatureConfig(
                    state = FeatureState.Enabled,
                    capabilities = setOf("licensing.pro"),
                ),
            ),
        ),
        signature = "dev-pro",
    )

    private val trialOverlay = SignedLicensePayload(
        config = AppConfig(
            license = LicenseInfo(type = "trial"),
            features = mapOf(
                "analytics" to FeatureConfig(
                    state = FeatureState.Enabled,
                    capabilities = setOf("analytics.summary"),
                ),
                "licensing" to FeatureConfig(
                    state = FeatureState.Enabled,
                    capabilities = emptySet(),
                ),
            ),
        ),
        signature = "dev-trial",
    )

    private val updatesFlow = MutableSharedFlow<SignedLicensePayload>(extraBufferCapacity = 1)

    override val updates: Flow<SignedLicensePayload> = updatesFlow.asSharedFlow()

    override suspend fun resolveLicense(): SignedLicensePayload? = null

    fun unlockPro() {
        updatesFlow.tryEmit(proOverlay)
    }

    fun resetToTrial() {
        updatesFlow.tryEmit(trialOverlay)
    }
}
