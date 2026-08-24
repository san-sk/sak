package sak.runtime

import sak.sdk.entry.EntryPoint
import sak.sdk.feature.FeatureId

/** One entry-point descriptor bound to its owning feature. */
data class ResolvedEntry(
    val featureId: FeatureId,
    val entry: EntryPoint,
)

/**
 * Snapshot of entry points from all loaded features.
 *
 * @property entries Flat list sorted by feature publish order (tags applied separately).
 * @property byTag Entries grouped by [sak.sdk.entry.EntryPoint.tags]; untagged → key `""`.
 */
data class RenderPlan(
    val entries: List<ResolvedEntry> = emptyList(),
    val byTag: Map<String, List<ResolvedEntry>> = emptyMap(),
) {
    /** Entries for a shell destination id when tags match destination names (e.g. `home`, `alerts`). */
    fun entriesForTag(tag: String): List<ResolvedEntry> = byTag[tag] ?: emptyList()
}

/** Union of route strings from loaded features. */
data class RouteTable(
    val routes: Set<String>,
)

/** Collects route strings from all loaded features. */
object RouteMerger {
    fun build(activeFeatures: Map<FeatureId, sak.sdk.feature.Feature>): RouteTable {
        val routes = activeFeatures.values
            .flatMap { feature -> feature.routes.map { it.value } }
            .toSet()
        return RouteTable(routes = routes)
    }
}
