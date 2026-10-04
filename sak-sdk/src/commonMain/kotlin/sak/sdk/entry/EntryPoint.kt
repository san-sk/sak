package sak.sdk.entry

import kotlinx.serialization.json.JsonElement

/**
 * A Micro-Frontend Data Descriptor published by a feature module to expose UI capabilities.
 *
 * In the SAK architecture, feature modules do **not** render host views (Compose/SwiftUI) directly,
 * ensuring strict KMP boundaries. Instead, features publish `EntryPoint` data structures.
 * 
 * The Host Shell constantly observes the aggregated flow of `EntryPoint`s from all active features.
 * When it receives these descriptors, the Host dynamically draws Native UI components (Cards, Banners)
 * matching the provided text, icons, and badges.
 *
 * @param id Stable, zero-allocation unique identifier for this entry point.
 * @param type Semantic classification hint telling the Host *where* to put it ([EntryPointType.Dashboard], [EntryPointType.Settings], etc.).
 * @param style Visual layout hint telling the Host *how* it should look ([EntryStyle.Card], [EntryStyle.Row], etc.).
 * @param priority Relative sorting priority. Higher numbers are sorted to the top of the UI list.
 * @param action The behavior dispatched to `SakRuntime` when the user taps this widget natively ([EntryAction.Navigate], etc.).
 * @param title Primary title label.
 * @param subtitle Secondary or supporting description text.
 * @param icon Icon key identifier (e.g., "chat_bubble", "settings"). The Host maps this string to native Vector Drawables or SF Symbols.
 * @param badge Optional badge indicator string (e.g., "NEW", "LIVE", "3").
 * @param metadata An arbitrary JSON payload. Used to pass complex data (like line-graph data points) to Custom Host renderers.
 */
data class EntryPoint(
    val id: EntryPointId,
    val type: EntryPointType = EntryPointType.Card,
    val style: EntryStyle? = null,
    val priority: Int = 0,
    val action: EntryAction = EntryAction.None,
    val title: String = "",
    val subtitle: String? = null,
    val icon: String? = null,
    val badge: String? = null,
    val metadata: Map<String, JsonElement> = emptyMap(),
)
