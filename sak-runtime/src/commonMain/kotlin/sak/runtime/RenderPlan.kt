package sak.runtime

import sak.sdk.entry.EntryPoint
import sak.sdk.feature.FeatureId
import sak.sdk.layout.ScaffoldConfig

/** One resolved widget: which feature owns it and the entry point definition. */
data class ResolvedEntry(
    val featureId: FeatureId,
    val entry: EntryPoint,
)

/**
 * Output of [ScaffoldRenderer]: bottom bar config + entry widgets per scaffold destination.
 */
data class RenderPlan(
    val scaffold: ScaffoldConfig,
    /** Destination id → resolved entry widgets (e.g. `home` → summary cards). */
    val destinations: Map<String, List<ResolvedEntry>>,
)

/** Union of all feature route strings for deep-link validation. */
data class RouteTable(
    val routes: Set<String>,
)
