package sak.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import sak.sdk.entry.EntryPointId
import sak.sdk.feature.Feature

/**
 * Interface for features that provide custom Compose UI for their entry points.
 *
 * Implemented in the feature's `androidMain` or `commonMain` (if using Compose Multiplatform).
 */
interface ComposeFeature : Feature {
    /**
     * Render the custom UI widget for a given [EntryPointId].
     */
    @Composable
    fun EntryPointView(id: EntryPointId, modifier: Modifier)
}
