package sak.sdk.entry

/** Visual layout hint for scaffold widgets. SakApp may style Banner vs Row differently. */
enum class EntryStyle {
    /** Full-width card widget. */
    Card,
    /** Single-line list row. */
    Row,
    /** Compact grid tile. */
    Tile,
    /** Prominent banner (often used for alerts). */
    Banner,
    /** Dashboard-style widget block. */
    Widget,
    /** Host-defined styling via [ScreenRegistry]. */
    Custom,
}
