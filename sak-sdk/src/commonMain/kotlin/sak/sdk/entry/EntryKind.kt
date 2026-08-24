package sak.sdk.entry

/**
 * UI kind for an [EntryPoint] — host maps each value to a renderer using [EntryPoint.payload].
 */
enum class EntryKind {
    Card,
    Row,
    Tile,
    Banner,
    Switch,
    Fab,
    ActionItem,
    Progress,
    Dashboard,
    Alert,
    Custom,
}
