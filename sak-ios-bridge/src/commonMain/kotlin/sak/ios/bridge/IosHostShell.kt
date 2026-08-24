package sak.ios.bridge

/** Native shell tab — not part of entitlement JSON. */
data class IosHostShellDestination(
    val id: String,
    val label: String,
    val icon: String = "",
)

internal val defaultIosHostShellDestinations: List<IosHostShellDestination> = listOf(
    IosHostShellDestination(id = "home", label = "Home", icon = "home"),
    IosHostShellDestination(id = "alerts", label = "Alerts", icon = "notifications"),
)
