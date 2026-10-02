package sak.sdk.entry

import kotlinx.serialization.json.JsonElement

/**
 * Stable unique identifier for an entry point widget (e.g. `EntryPointId("summary-card")`).
 */
data class EntryPointId(val value: String)

/**
 * Standard classification types for published entry point widgets.
 */
enum class EntryPointType {
    /** Urgent alert or notification widget. */
    Alert,
    /** Informational card or status banner. */
    Info,
    /** Main dashboard tile or summary widget. */
    Dashboard,
    /** Settings or configuration option item. */
    Settings,
    /** Interactive action button or CTA card. */
    Action,
    /** Feature summary tile. */
    Summary,
    /** Host-defined or custom type. */
    Custom,
}

/**
 * Runtime entry point published by a feature module to fill host application UI slots.
 *
 * @param id Stable identifier for this entry point.
 * @param type Entry point classification type ([EntryPointType.Alert], [EntryPointType.Info], [EntryPointType.Dashboard], [EntryPointType.Settings]). Defaults to [EntryPointType.Info].
 * @param style Optional visual layout hint ([EntryStyle]). Defaults to `null`.
 * @param action Action executed when user interacts with this entry point. Defaults to [EntryAction.None].
 * @param metadata Custom JSON metadata payload.
 * @param priority Relative sorting priority (higher priority items sort first within host slots).
 * @param categories Host slot categories where this entry point should appear (e.g. `setOf("home", "alerts")`).
 */
data class EntryPoint(
    val id: EntryPointId,
    val type: EntryPointType = EntryPointType.Info,
    val style: EntryStyle? = null,
    val action: EntryAction = EntryAction.None,
    val metadata: Map<String, JsonElement> = emptyMap(),
    val priority: Int = 0,
    val categories: Set<String> = emptySet(),
)
