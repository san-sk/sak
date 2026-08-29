package sak.sdk.entry

import kotlinx.serialization.json.JsonElement

/**
 * A small widget published by a feature (card, row, banner) that can open a screen or trigger an action.
 *
 * Publish via [sak.sdk.feature.Feature.entryPoints].
 */
/** Stable id for an entry widget (e.g. `summary-card`, `unlock-card`). */
data class EntryPointId(val value: String)

/** Runtime entry widget published by a feature. */
data class EntryPoint(
    val id: EntryPointId,
    val type: String,
    val style: EntryStyle,
    val action: EntryAction,
    val metadata: Map<String, JsonElement> = emptyMap(),
    /** Higher priority sorts first within a destination section. */
    val priority: Int = 0,
    /** Used by the host app to group entries (e.g. `alerts`, `home`). */
    val categories: Set<String> = emptySet(),
)
