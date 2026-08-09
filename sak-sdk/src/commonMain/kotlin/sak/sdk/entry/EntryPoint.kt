package sak.sdk.entry

/**
 * A small widget on a scaffold destination (card, row, banner) that can open a feature screen.
 *
 * Publish via [sak.sdk.feature.Feature.entryPoints]. Placement is driven by config scaffold sections
 * (explicit refs or category query on destinations like Home / Alerts).
 */
/** Stable id for an entry widget (e.g. `summary-card`, `unlock-card`). */
data class EntryPointId(val value: String)

/** Runtime entry widget published by a feature and placed by scaffold config. */
data class EntryPoint(
    val id: EntryPointId,
    val style: EntryStyle,
    /** Key for [sak.sdk.feature.ScreenRegistry] preview composable. */
    val previewKey: String,
    val action: EntryAction,
    /** Higher priority sorts first within a destination section. */
    val priority: Int = 0,
    /** Used by scaffold query sections (e.g. `alerts`, `home`). */
    val categories: Set<String> = emptySet(),
    val visibility: EntryVisibility = EntryVisibility.WhenEnabled,
)
