package sak.runtime

import sak.sdk.config.AppConfig
import sak.sdk.config.FeatureToggle
import sak.sdk.entry.EntryPoint
import sak.sdk.entry.EntryPointId
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureId
import sak.sdk.layout.EntryRef
import sak.sdk.layout.LayoutSection
import sak.sdk.layout.ScaffoldConfig

/**
 * Builds [RenderPlan] from scaffold config + active features.
 * Filters entry points by enabled/visible, config entries allow-list, and section type (refs vs query).
 */
object ScaffoldRenderer {
    /** Build a [RenderPlan] from scaffold config, ActiveConfig, and currently active features. */
    fun build(
        scaffold: ScaffoldConfig?,
        activeConfig: AppConfig,
        activeFeatures: Map<FeatureId, Feature>,
    ): RenderPlan {
        val scaffoldConfig = scaffold ?: ScaffoldConfig()
        val destinations = scaffoldConfig.destinations.mapValues { (_, layout) ->
            resolveSections(layout.sections, activeConfig, activeFeatures)
        }
        return RenderPlan(scaffold = scaffoldConfig, destinations = destinations)
    }

    private fun resolveSections(
        sections: List<LayoutSection>,
        activeConfig: AppConfig,
        activeFeatures: Map<FeatureId, Feature>,
    ): List<ResolvedEntry> {
        val resolved = mutableListOf<ResolvedEntry>()
        sections.forEach { section ->
            when (section) {
                is LayoutSection.Entries -> section.refs.forEach { ref ->
                    resolveRef(ref, activeConfig, activeFeatures)?.let { resolved += it }
                }
                is LayoutSection.Query -> {
                    activeFeatures.forEach { (featureId, feature) ->
                        val toggle = activeConfig.features[featureId.value] ?: return@forEach
                        if (!toggle.enabled || !toggle.visible) return@forEach
                        feature.entryPoints.value
                            .filter { entry ->
                                section.categories.any { it in entry.categories } &&
                                    (section.styles == null || section.styles!!.contains(entry.style.name))
                            }
                            .filter { entry -> entryAllowed(toggle, entry) }
                            .sortedByDescending { it.priority }
                            .forEach { entry ->
                                resolved += ResolvedEntry(featureId, entry)
                            }
                    }
                }
            }
        }
        return resolved
    }

    private fun resolveRef(
        ref: EntryRef,
        activeConfig: AppConfig,
        activeFeatures: Map<FeatureId, Feature>,
    ): ResolvedEntry? {
        val featureId = FeatureId(ref.feature)
        val feature = activeFeatures[featureId] ?: return null
        val toggle = activeConfig.features[ref.feature] ?: return null
        if (!toggle.enabled || !toggle.visible) return null
        val entry = feature.entryPoints.value.find { it.id == EntryPointId(ref.entry) } ?: return null
        if (!entryAllowed(toggle, entry)) return null
        return ResolvedEntry(featureId, entry)
    }

    private fun entryAllowed(toggle: FeatureToggle, entry: EntryPoint): Boolean {
        val access = toggle.entries
        val allowed = access?.allowed
        if (allowed != null) {
            return allowed.contains(entry.id.value)
        }
        val blocked = access?.blocked
        if (blocked != null && blocked.contains(entry.id.value)) {
            return false
        }
        return true
    }
}

/** Collects route strings from all active features. */
object RouteMerger {
    fun build(activeFeatures: Map<FeatureId, Feature>): RouteTable {
        val routes = activeFeatures.values
            .flatMap { feature -> feature.routes.map { it.value } }
            .toSet()
        return RouteTable(routes = routes)
    }
}
