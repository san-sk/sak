package sak.compose.ui.shell

/**
 * Host shell tab / destination — not part of entitlement JSON.
 */
data class HostShellDestination(
    val id: String,
    val label: String,
    val icon: String = "",
)

/** Reference demo shell matching common entry [tags][sak.sdk.entry.EntryPoint.tags]. */
object DefaultHostShell {
    val destinations: List<HostShellDestination> = listOf(
        HostShellDestination(id = "home", label = "Home", icon = "home"),
        HostShellDestination(id = "alerts", label = "Alerts", icon = "notifications"),
    )
}
