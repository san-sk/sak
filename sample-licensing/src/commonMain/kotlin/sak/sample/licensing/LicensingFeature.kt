package sak.sample.licensing

import sak.sdk.entry.EntryAction
import sak.sdk.entry.EntryPoint
import sak.sdk.entry.EntryPointId
import sak.sdk.entry.EntryPointType
import sak.sdk.entry.EntryStyle
import sak.sdk.feature.BaseFeature
import sak.sdk.feature.FeatureContext
import sak.sdk.feature.FeatureId
import sak.sdk.feature.FeatureState

/**
 * Sample feature controls feature.
 *
 * Publishes a settings entry point that lets the host open the feature controls screen,
 * where features can be enabled or disabled at runtime to demonstrate lifecycle behaviour.
 */
class LicensingFeature : BaseFeature() {
    override val id = FeatureId("licensing")

    override suspend fun onCreate(context: FeatureContext) {
        super.onCreate(context)
        publishEntries()
        updateStatus(FeatureState.Ready)
    }

    override suspend fun onDestroy() {
        super.onDestroy()
    }

    private fun publishEntries() {
        setEntryPoints(
            listOf(
                EntryPoint(
                    id = EntryPointId("controls-row"),
                    type = EntryPointType.Settings,
                    style = EntryStyle.Row,
                    action = EntryAction.Navigate("licensing/upgrade"),
                    priority = 80,
                    title = "Feature Controls",
                    subtitle = "Enable or disable features at runtime",
                    icon = "tune",
                ),
            )
        )
    }
}