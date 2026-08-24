package sak.runtime

import sak.sdk.config.AppConfig
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureId
import sak.sdk.feature.shouldLoad

/**
 * Collects [ResolvedEntry] widgets from all **loaded** features.
 *
 * Entries are grouped by [sak.sdk.entry.EntryPoint.tags] for host shell placement.
 * Untagged entries appear under the empty-string key in [RenderPlan.byTag].
 */
object EntryResolver {
    fun build(
        activeConfig: AppConfig,
        activeFeatures: Map<FeatureId, Feature>,
    ): RenderPlan {
        val all = mutableListOf<ResolvedEntry>()
        val byTag = mutableMapOf<String, MutableList<ResolvedEntry>>()

        activeFeatures.forEach { (featureId, feature) ->
            val row = activeConfig.features[featureId.value]
            if (row == null || !row.state.shouldLoad()) return@forEach

            feature.entryPoints.value
                .sortedByDescending { it.priority }
                .forEach { entry ->
                    val resolved = ResolvedEntry(featureId, entry)
                    all += resolved
                    if (entry.tags.isEmpty()) {
                        byTag.getOrPut("") { mutableListOf() }.add(resolved)
                    } else {
                        entry.tags.forEach { tag ->
                            byTag.getOrPut(tag) { mutableListOf() }.add(resolved)
                        }
                    }
                }
        }

        return RenderPlan(entries = all, byTag = byTag)
    }
}
