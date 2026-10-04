package sak.sdk.entry

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.jvm.JvmInline

/**
 * Stable unique identifier for an entry point widget (e.g. `EntryPointId("summary-card")`).
 *
 * Uses `@JvmInline value class` to ensure strict compile-time type safety while eliminating
 * runtime object allocations on the heap.
 */
@JvmInline
value class EntryPointId(val value: String)

/**
 * High-level semantic classification of a published `EntryPoint` widget.
 * Host apps read this type to decide exactly *where* to render the widget in their UI
 * (e.g., placing `Settings` widgets inside a scrollable Preferences view).
 */
enum class EntryPointType {
    /** Primary dashboard tile. Usually reserved for the top of a home screen. */
    Dashboard,
    /** Standard card representation. */
    Card,
    /** Urgent alert or prominent notification block. */
    Alert,
    /** Deep-linked settings, preferences, or account item. */
    Settings,
    /** A direct Call-To-Action (CTA) or action button. */
    Action,
    /** A compact numeric summary tile or metric widget. */
    Summary,
    /** A Host-defined custom type not covered by standard layout logic. */
    Custom,
}

/**
 * Visual layout hint suggesting to the Host App *how* it should shape this entry point.
 * Note: The Host App ultimately controls the design system and is free to ignore this hint.
 */
enum class EntryStyle {
    /** Full-width card widget with high visual weight. */
    Card,
    /** A simple single-line list row (e.g., `[Icon] Title ->`). */
    Row,
    /** A compact grid tile (often used for categories). */
    Tile,
    /** A prominent full-width banner with background color (often used for alerts). */
    Banner,
    /** A massive dashboard-style interactive block (e.g., a Line Chart view). */
    Widget,
    /** A custom styling hook defined by the host. */
    Custom,
}

/**
 * Interaction behavior dispatched to the SAK Runtime when a user taps an entry point widget.
 */
@Serializable
sealed class EntryAction {
    /** 
     * Instructs the SAK Engine to emit a [sak.sdk.navigation.Route] to the Host OS Router.
     * @param route The exact string path (e.g., "chat/home") to push onto the native NavigationStack.
     */
    @Serializable
    @SerialName("navigate")
    data class Navigate(val route: String) : EntryAction()

    /** Instructs the Host App to open an external web URL or internal App Deep Link URI. */
    @Serializable
    @SerialName("deepLink")
    data class DeepLink(val uri: String) : EntryAction()

    /** Display-only widget; tapping it performs no action. */
    @Serializable
    @SerialName("none")
    data object None : EntryAction()

    /** 
     * Dispatches a custom interaction handled exclusively by the Host App. 
     * Useful for triggering native APIs (like Apple Pay) from a KMP feature widget.
     */
    @Serializable
    @SerialName("custom")
    data class Custom(
        val id: String,
        val params: Map<String, JsonElement> = emptyMap()
    ) : EntryAction()
}
