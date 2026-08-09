package sak.compose.ui

import androidx.compose.runtime.Composable

/** Wraps Compose Material theme (or host branding) around SAK UI. */
typealias SakTheme = @Composable (content: @Composable () -> Unit) -> Unit

/**
 * Registers feature routes on the shared [androidx.navigation.NavHost].
 * Parameters: NavGraphBuilder extension + NavHostController for back stack.
 */
typealias FeatureNavContributor = (
    androidx.navigation.NavGraphBuilder,
    androidx.navigation.NavHostController,
) -> Unit
