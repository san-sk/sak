package sak.sdk.layout

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/** Points to a specific entry point on a scaffold destination. */
@Serializable
data class EntryRef(
    /** Feature id from config `features` (e.g. `analytics`). */
    val feature: String,
    /** Entry id from [sak.sdk.entry.EntryPointId] (e.g. `summary-card`). */
    val entry: String,
)

/** Legacy query shape; prefer [LayoutSection.Query] in scaffold JSON. */
@Serializable
data class LayoutQuery(
    val categories: Set<String>,
    val styles: Set<String>? = null,
)

/**
 * How widgets are placed on a scaffold destination (Home, Alerts, …).
 * - **entries**: explicit feature + entry refs from config.
 * - **query**: auto-collect entry points matching categories/styles from active features.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class LayoutSection {
    /** Place specific entry refs listed in config. */
    @Serializable
    @SerialName("entries")
    data class Entries(val refs: List<EntryRef>) : LayoutSection()

    /** Collect entry points whose [sak.sdk.entry.EntryPoint.categories] match. */
    @Serializable
    @SerialName("query")
    data class Query(
        val categories: Set<String>,
        val styles: Set<String>? = null,
    ) : LayoutSection()
}

/** Layout of one bottom-bar destination (e.g. `home`, `alerts`). */
@Serializable
data class DestinationLayout(
    val sections: List<LayoutSection> = emptyList(),
)

/** One tab in the scaffold bottom navigation bar. */
@Serializable
data class BottomBarItem(
    val id: String,
    val label: String,
    val icon: String,
    /** Destination key — maps to [ScaffoldConfig.destinations] and NavHost `scaffold/{destination}`. */
    val destination: String,
)

/** Bottom navigation items from DefaultConfig `scaffold.bottomBar`. */
@Serializable
data class BottomBarConfig(
    val items: List<BottomBarItem> = emptyList(),
)

/**
 * App chrome from DefaultConfig: bottom navigation + per-tab widget layouts.
 * Routes in the NavHost are `scaffold/{destination}`.
 */
@Serializable
data class ScaffoldConfig(
    val bottomBar: BottomBarConfig = BottomBarConfig(),
    val destinations: Map<String, DestinationLayout> = emptyMap(),
)
